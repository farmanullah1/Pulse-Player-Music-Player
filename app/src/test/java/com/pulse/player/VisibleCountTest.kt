package com.pulse.player

import com.pulse.player.data.db.TrackDao
import com.pulse.player.data.db.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class VisibleCountTest {

    private fun isVisible(
        t: TrackEntity,
        minMs: Long,
        allTracks: List<TrackEntity>
    ): Boolean {
        if (!t.available) return false
        if (t.durationMs < minMs) return false
        if (t.sourceType == "COPIED") {
            val hasEqualMediaStore = allTracks.any { m ->
                m.sourceType == "MEDIASTORE" &&
                        m.available &&
                        m.sizeBytes == t.sizeBytes &&
                        m.normTitle == t.normTitle &&
                        abs(m.durationMs - t.durationMs) < 2000
            }
            if (hasEqualMediaStore) return false
        }
        return true
    }

    private fun createTrack(
        id: String,
        title: String,
        durationMs: Long,
        sizeBytes: Long,
        available: Boolean = true,
        sourceType: String = "MEDIASTORE"
    ) = TrackEntity(
        id = id,
        uri = "content://media/$id",
        displayName = "$title.mp3",
        title = title,
        artist = "Artist",
        album = "Album",
        albumArtist = null,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        mimeType = "audio/mpeg",
        folderPath = "Music",
        dateAddedMs = 1000L,
        dateModifiedMs = 1000L,
        trackNumber = 1,
        year = 2026,
        normTitle = title.lowercase(),
        normArtist = "artist",
        normAlbum = "album",
        available = available,
        dataPath = null,
        scanGen = 0L,
        sourceType = sourceType,
        stableId = id,
        volumeId = "external_primary",
        folderUri = null
    )

    @Test
    fun visibleConstant_isDefinedInTrackDao() {
        assertTrue(TrackDao.VISIBLE.contains("t.available = 1"))
        assertTrue(TrackDao.VISIBLE.contains("t.durationMs >= :minMs"))
        assertTrue(TrackDao.VISIBLE.contains("t.sourceType = 'COPIED'"))
    }

    @Test
    fun visibleCount_filtersByMinDuration() {
        val tracks = listOf(
            createTrack("1", "Short", 15_000L, 500_000L),
            createTrack("2", "Medium", 45_000L, 1_500_000L),
            createTrack("3", "Long", 180_000L, 5_000_000L)
        )

        val countAt0s = tracks.count { isVisible(it, 0L, tracks) }
        val countAt30s = tracks.count { isVisible(it, 30_000L, tracks) }
        val countAt60s = tracks.count { isVisible(it, 60_000L, tracks) }

        assertEquals(3, countAt0s)
        assertEquals(2, countAt30s)
        assertEquals(1, countAt60s)
    }

    @Test
    fun visibleCount_hidesCopiedDuplicatesWhenMediaStoreExists() {
        val msTrack = createTrack("ms:1", "My Song", 200_000L, 4_000_000L, sourceType = "MEDIASTORE")
        val copiedTrack = createTrack("copy:1", "My Song", 200_500L, 4_000_000L, sourceType = "COPIED")
        val uniqueCopiedTrack = createTrack("copy:2", "Unique Song", 150_000L, 3_000_000L, sourceType = "COPIED")

        val all = listOf(msTrack, copiedTrack, uniqueCopiedTrack)

        assertTrue(isVisible(msTrack, 0L, all))
        assertFalse("Duplicate copied track must be hidden", isVisible(copiedTrack, 0L, all))
        assertTrue("Unique copied track must remain visible", isVisible(uniqueCopiedTrack, 0L, all))

        val visibleCount = all.count { isVisible(it, 0L, all) }
        assertEquals(2, visibleCount)
    }

    @Test
    fun visibleCount_unavailableTracksAreExcluded() {
        val track = createTrack("1", "Missing Song", 120_000L, 2_000_000L, available = false)
        assertFalse(isVisible(track, 0L, listOf(track)))
    }
}

