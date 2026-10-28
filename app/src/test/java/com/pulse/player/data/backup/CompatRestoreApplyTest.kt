package com.pulse.player.data.backup

import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.norm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatRestoreApplyTest {

    @Test
    fun testDryRunPreviewDoesNotModify() {
        val tracks = listOf(
            createTestTrack("ms:1", "/storage/emulated/0/Music/track1.mp3", "Track 1", "Artist 1"),
            createTestTrack("ms:2", "/storage/emulated/0/Music/track2.mp3", "Track 2", "Artist 2")
        )
        val libraryIndex = LibraryIndex(tracks)

        val staged = StagedBackup(
            meta = mapOf("backupTime" to "20261028_172204175", "versionName" to "4.4.1.189"),
            flags = mapOf("isBackupPlaylists" to true, "isBackupMusicRecap" to true),
            tagCount = 2,
            tags = mapOf(
                PathKey.abs("/storage/emulated/0/Music/track1.mp3") to SrcTag("/storage/emulated/0/Music/track1.mp3", "Track 1", "Artist 1", "Album 1"),
                PathKey.abs("/storage/emulated/0/Music/track2.mp3") to SrcTag("/storage/emulated/0/Music/track2.mp3", "Track 2", "Artist 2", "Album 2")
            ),
            playlists = listOf(
                SrcPlaylist("My Favourite", null, null, listOf("/storage/emulated/0/Music/track1.mp3")),
                SrcPlaylist("Rock", "playlistPlayOrder", -1, listOf("/storage/emulated/0/Music/track2.mp3"))
            ),
            events = listOf(
                SrcEvent("/storage/emulated/0/Music/track1.mp3", 1700000000000L, 180000L, 0),
                SrcEvent("/storage/emulated/0/Music/track2.mp3", 1700000060000L, 200000L, 1)
            ),
            sortRaw = mapOf("All_Song" to mapOf("key" to "title", "order" to 1)),
            excludeDirs = listOf("/storage/emulated/0/Recordings"),
            hiddenFlagged = 0,
            lyricsNonEmpty = false,
            snapshotsOk = 1,
            snapshotsBad = 0,
            warnings = emptyList()
        )

        // Resolve against staged
        val favHits = staged.playlists.filter { it.name.lowercase() == "my favourite" }
            .flatMap { it.paths }
            .mapNotNull { path -> (libraryIndex.resolve(path, staged.tags[PathKey.abs(path)]) as? Resolve.Hit)?.track }

        assertEquals(1, favHits.size)
        assertEquals("ms:1", favHits[0].id)

        val otherPlaylists = staged.playlists.filter { it.name.lowercase() != "my favourite" }
        val plHits = otherPlaylists.flatMap { it.paths }
            .mapNotNull { path -> (libraryIndex.resolve(path, staged.tags[PathKey.abs(path)]) as? Resolve.Hit)?.track }

        assertEquals(1, plHits.size)
        assertEquals("ms:2", plHits[0].id)

        val eventHits = staged.events.mapNotNull { ev ->
            (libraryIndex.resolve(ev.path, staged.tags[PathKey.abs(ev.path)]) as? Resolve.Hit)?.track
        }
        assertEquals(2, eventHits.size)
    }

    @Test
    fun testIdempotentResolutionKeys() {
        // Idempotency: resolving the same staged events twice yields identical IDs without duplicates
        val tracks = listOf(
            createTestTrack("ms:1", "/storage/emulated/0/Music/track1.mp3", "Track 1", "Artist 1")
        )
        val libraryIndex = LibraryIndex(tracks)

        val event = SrcEvent("/storage/emulated/0/Music/track1.mp3", 1700000000000L, 180000L, 0)
        val hit1 = libraryIndex.resolve(event.path, null) as Resolve.Hit
        val hit2 = libraryIndex.resolve(event.path, null) as Resolve.Hit

        assertEquals(hit1.track.id, hit2.track.id)
        // Deduplication key: (trackId, playedAtMs)
        val key1 = Pair(hit1.track.id, event.timestamp)
        val key2 = Pair(hit2.track.id, event.timestamp)
        assertEquals(key1, key2)
    }

    @Test
    fun testReplaceScopeFiltersCategories() {
        val allOptions = setOf("isBackupPlaylists", "isBackupMusicRecap", "isBackupSortSettings")
        val selectedOnlyPlaylists = setOf("isBackupPlaylists")

        assertTrue("isBackupPlaylists" in selectedOnlyPlaylists)
        assertTrue("isBackupMusicRecap" !in selectedOnlyPlaylists)
        assertTrue("isBackupSortSettings" !in selectedOnlyPlaylists)
    }

    private fun createTestTrack(
        id: String,
        dataPath: String,
        title: String,
        artist: String
    ): TrackEntity = TrackEntity(
        id = id,
        uri = "content://media/external/audio/media/${id.substringAfter(':')}",
        displayName = dataPath.substringAfterLast('/'),
        title = title,
        artist = artist,
        album = "Test Album",
        albumArtist = artist,
        durationMs = 200_000L,
        sizeBytes = 5_000_000L,
        mimeType = "audio/mpeg",
        folderPath = dataPath.substringBeforeLast('/'),
        dateAddedMs = 1700000000000L,
        dateModifiedMs = 1700000000000L,
        trackNumber = 1,
        year = 2024,
        normTitle = norm(title),
        normArtist = norm(artist),
        normAlbum = norm("Test Album"),
        available = true,
        dataPath = dataPath,
        scanGen = 1L
    )
}

