package com.pulse.player.ui.theme.immersive

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class ImmersivePlayerTest {

    @Test
    fun queueItemModelProperties() {
        val item = QueueItem(
            mediaId = "track_123",
            title = "Midnight Sun",
            artist = "Retro Wave",
            artUri = "content://media/external/audio/albumart/5",
            durationMs = 215_000L
        )

        assertEquals("track_123", item.mediaId)
        assertEquals("Midnight Sun", item.title)
        assertEquals("Retro Wave", item.artist)
        assertEquals("content://media/external/audio/albumart/5", item.artUri)
        assertEquals(215_000L, item.durationMs)
    }

    @Test
    fun repeatLabelFormatting() {
        assertEquals("one", repeatLabel(Player.REPEAT_MODE_ONE))
        assertEquals("all", repeatLabel(Player.REPEAT_MODE_ALL))
        assertEquals("off", repeatLabel(Player.REPEAT_MODE_OFF))
        assertEquals("off", repeatLabel(999)) // Unknown mode falls back to off
    }

    @Test
    fun timeFormatting() {
        assertEquals("0:00", fmt(0L))
        assertEquals("0:05", fmt(5_000L))
        assertEquals("1:05", fmt(65_000L))
        assertEquals("3:45", fmt(225_000L))
        assertEquals("10:00", fmt(600_000L))
    }
}

