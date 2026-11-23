package com.pulse.player.ui.themes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRegistryTest {

    @Test
    fun playerThemesContainsConceptAndAtmosphericThemes() {
        assertEquals("PlayerThemes must register all existing, concept, and studio themes", 17, PlayerThemes.size)
    }

    @Test
    fun defaultThemeIsClassic() {
        assertEquals("classic", PlayerThemes[0].id)
        assertEquals(PlayerLayout.CLASSIC, PlayerThemes[0].layout)
    }

    @Test
    fun allCanonicalThemesArePresent() {
        val canonicalIds = setOf(
            "classic",
            "mountain",
            "mesh",
            "minimal",
            "acoustic",
            "vinyl",
            "coast",
            "starry",
            "immersive"
        )
        val studioIds = setOf(
            "vinyl_turntable",
            "minimal_typography",
            "split_artwork_left",
            "compact_card",
            "waveform_studio"
        )
        val registeredIds = PlayerThemes.map { it.id }.toSet()
        assertTrue(registeredIds.containsAll(canonicalIds))
        assertTrue(registeredIds.contains("pixel"))
        assertTrue(registeredIds.contains("namida"))
        assertTrue(registeredIds.contains("atmospheric_queue"))
        assertTrue(registeredIds.containsAll(studioIds))
        assertEquals("All theme IDs must be unique", PlayerThemes.size, registeredIds.size)
    }

    @Test
    fun atmosphericQueueThemeIsRegisteredCorrectly() {
        val atmospheric = PlayerThemes.firstOrNull { it.id == "atmospheric_queue" }
        assertNotNull("Atmospheric Queue theme must be registered", atmospheric)
        assertEquals("Atmospheric Queue", atmospheric!!.name)
        assertEquals(PlayerLayout.ATMOSPHERIC_QUEUE, atmospheric.layout)
    }

    @Test
    fun studioLayoutsAreRegisteredWithExpectedLayouts() {
        val turntable = PlayerThemes.firstOrNull { it.id == "vinyl_turntable" }
        val minimalTypo = PlayerThemes.firstOrNull { it.id == "minimal_typography" }
        val splitLeft = PlayerThemes.firstOrNull { it.id == "split_artwork_left" }
        val compact = PlayerThemes.firstOrNull { it.id == "compact_card" }
        val waveform = PlayerThemes.firstOrNull { it.id == "waveform_studio" }

        assertEquals(PlayerLayout.VINYL_TURNTABLE, turntable?.layout)
        assertEquals(PlayerLayout.MINIMAL_TYPOGRAPHY, minimalTypo?.layout)
        assertEquals(PlayerLayout.SPLIT_ARTWORK_LEFT, splitLeft?.layout)
        assertEquals(PlayerLayout.COMPACT_CARD, compact?.layout)
        assertEquals(PlayerLayout.WAVEFORM_STUDIO, waveform?.layout)
    }

    @Test
    fun immersiveThemeIsRegisteredCorrectly() {
        val immersive = PlayerThemes.firstOrNull { it.id == "immersive" }
        assertNotNull("Immersive theme must be registered", immersive)
        assertEquals("Immersive Artwork Player", immersive!!.name)
        assertEquals(PlayerLayout.IMMERSIVE, immersive.layout)
    }

    @Test
    fun unknownThemeIdFallsBackToClassic() {
        val unknownId = "non_existent_theme_xyz"
        val resolved = PlayerThemes.firstOrNull { it.id == unknownId } ?: PlayerThemes[0]
        assertEquals("classic", resolved.id)
    }
}
