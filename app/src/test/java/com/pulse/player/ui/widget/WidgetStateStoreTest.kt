package com.pulse.player.ui.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateStoreTest {

    @Test
    fun buildFingerprint_staysStableForSameTrackAndPlayState() {
        val a = WidgetStateStore.buildFingerprint(
            uri = "content://media/external/audio/media/42",
            title = "Midnight City",
            artist = "M83",
            isPlaying = true,
            has = true
        )
        val b = WidgetStateStore.buildFingerprint(
            uri = "content://media/external/audio/media/42",
            title = "Midnight City",
            artist = "M83",
            isPlaying = true,
            has = true
        )
        assertEquals(a, b)
    }

    @Test
    fun buildFingerprint_changesOnTrackOrPlayStateChange() {
        val playing = WidgetStateStore.buildFingerprint(
            uri = "content://media/external/audio/media/42",
            title = "Midnight City",
            artist = "M83",
            isPlaying = true,
            has = true
        )
        val paused = WidgetStateStore.buildFingerprint(
            uri = "content://media/external/audio/media/42",
            title = "Midnight City",
            artist = "M83",
            isPlaying = false,
            has = true
        )
        val nextTrack = WidgetStateStore.buildFingerprint(
            uri = "content://media/external/audio/media/43",
            title = "Wait",
            artist = "M83",
            isPlaying = true,
            has = true
        )
        assertNotEquals(playing, paused)
        assertNotEquals(playing, nextTrack)
    }

    @Test
    fun shouldRefreshArtwork_onlyReturnsTrueWhenUriChanges() {
        val uri = "content://media/external/audio/media/42"
        assertFalse(WidgetStateStore.shouldRefreshArtwork(uri, uri))
        assertTrue(WidgetStateStore.shouldRefreshArtwork(null, uri))
        assertTrue(WidgetStateStore.shouldRefreshArtwork(uri, "content://media/external/audio/media/99"))
        assertTrue(WidgetStateStore.shouldRefreshArtwork(uri, null))
    }
}
