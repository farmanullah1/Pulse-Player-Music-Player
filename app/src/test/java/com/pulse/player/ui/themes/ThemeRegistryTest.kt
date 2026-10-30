package com.pulse.player.ui.themes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRegistryTest {

    @Test
    fun playerThemesContainsExactlyNineThemes() {
        assertEquals("PlayerThemes must register exactly 9 themes", 9, PlayerThemes.size)
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
        val registeredIds = PlayerThemes.map { it.id }.toSet()
        assertEquals(canonicalIds, registeredIds)
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

