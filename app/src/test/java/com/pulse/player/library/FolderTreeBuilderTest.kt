package com.pulse.player.library

import com.pulse.player.playback.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderTreeBuilderTest {

    private fun track(id: String, folder: String) = Track(
        id = id,
        uri = "content://media/$id",
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        isFavorite = false,
        folder = folder
    )

    @Test
    fun `commonRoot finds shared ancestor directory`() {
        val root = FolderTreeBuilder.commonRoot(
            listOf(
                "/storage/emulated/0/Music/Rock/Classic",
                "/storage/emulated/0/Music/Jazz",
                "/storage/emulated/0/Music/"
            )
        )
        assertEquals("/storage/emulated/0/Music", root)
    }

    @Test
    fun `browseAt builds subfolders recursive totals and breadcrumbs`() {
        val tracks = listOf(
            track("1", "/Music"),
            track("2", "/Music/Electronic/House"),
            track("3", "/Music/Electronic/Ambient"),
            track("4", "/Music/Electronic"),
            track("5", "/Music/Acoustic")
        )

        val rootState = FolderTreeBuilder.browseAt(tracks, null)
        assertEquals("/Music", rootState.currentPath)
        assertEquals(1, rootState.directTracks.size)
        assertEquals(2, rootState.subfolders.size)

        val acoustic = rootState.subfolders.first { it.name == "Acoustic" }
        assertEquals(1, acoustic.directCount)
        assertEquals(1, acoustic.totalCount)

        val electronic = rootState.subfolders.first { it.name == "Electronic" }
        assertEquals(1, electronic.directCount)
        assertEquals(3, electronic.totalCount)

        val childState = FolderTreeBuilder.browseAt(tracks, "/Music/Electronic")
        assertEquals("/Music/Electronic", childState.currentPath)
        assertEquals(listOf("Music", "Electronic"), childState.breadcrumbs.map { it.label })
        assertEquals(1, childState.directTracks.size)
        assertEquals(2, childState.subfolders.size)
    }

    @Test
    fun `tracksUnderFolder respects recursive toggle`() {
        val tracks = listOf(
            track("1", "/Music/Electronic"),
            track("2", "/Music/Electronic/Synthwave"),
            track("3", "/Music/Rock")
        )

        val recursive = FolderTreeBuilder.tracksUnderFolder(tracks, "/Music/Electronic", recursive = true)
        assertEquals(listOf("1", "2"), recursive.map { it.id })

        val directOnly = FolderTreeBuilder.tracksUnderFolder(tracks, "/Music/Electronic", recursive = false)
        assertEquals(listOf("1"), directOnly.map { it.id })
        assertTrue(FolderTreeBuilder.browseAt(emptyList(), null).subfolders.isEmpty())
    }
}
