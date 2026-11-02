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
    fun unknownLayoutUsesPremierAtmosphericMode() {
        assertEquals(PlayerLayoutMode.ATMOSPHERIC_QUEUE, PlayerLayoutMode.fromId("missing"))
    }
}
