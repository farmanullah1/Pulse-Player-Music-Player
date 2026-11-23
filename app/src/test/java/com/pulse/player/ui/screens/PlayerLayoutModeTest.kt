package com.pulse.player.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLayoutModeTest {
    @Test
    fun atmosphericQueueHasStablePresentation() {
        val mode = PlayerLayoutMode.fromId("atmospheric_queue")

        assertEquals("Atmospheric Queue", mode.displayName)
        assertEquals("Dark mist overlay & light queue card", mode.subtitle)
    }

    @Test
    fun allStudioLayoutsResolveCorrectly() {
        assertEquals(17, PlayerLayoutMode.entries.size)
        assertEquals(PlayerLayoutMode.VINYL_TURNTABLE, PlayerLayoutMode.fromId("vinyl_turntable"))
        assertEquals(PlayerLayoutMode.MINIMAL_TYPOGRAPHY, PlayerLayoutMode.fromId("minimal_typography"))
        assertEquals(PlayerLayoutMode.SPLIT_ARTWORK_LEFT, PlayerLayoutMode.fromId("split_artwork_left"))
        assertEquals(PlayerLayoutMode.COMPACT_CARD, PlayerLayoutMode.fromId("compact_card"))
        assertEquals(PlayerLayoutMode.WAVEFORM_STUDIO, PlayerLayoutMode.fromId("waveform_studio"))
    }

    @Test
    fun nextAndPreviousCycleThroughAllModes() {
        val first = PlayerLayoutMode.entries.first()
        val last = PlayerLayoutMode.entries.last()
        assertEquals(first, PlayerLayoutMode.next(last))
        assertEquals(last, PlayerLayoutMode.previous(first))
    }

    @Test
    fun unknownLayoutUsesPremierAtmosphericMode() {
        assertEquals(PlayerLayoutMode.ATMOSPHERIC_QUEUE, PlayerLayoutMode.fromId("missing"))
    }
}
