package com.pulse.player.ui.buttons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonPresetTest {

    @Test
    fun exactly15PresetsRegistered() {
        assertEquals("Must have exactly 15 button presets", 15, ButtonPresets.size)
    }

    @Test
    fun allPresetIdsAreUnique() {
        val ids = ButtonPresets.map { it.id }
        assertEquals("All preset IDs must be unique", ids.toSet().size, ids.size)
    }

    @Test
    fun allLayoutValuesAreCovered() {
        val coveredLayouts = ButtonPresets.map { it.layout }.toSet()
        val allLayouts = ButtonLayout.entries.toSet()
        assertEquals("Every ButtonLayout value must have a preset", allLayouts, coveredLayouts)
    }

    @Test
    fun defaultPresetIsCentered() {
        assertEquals("centered", ButtonPresetRegistry.default.id)
        assertEquals(ButtonLayout.CENTERED, ButtonPresetRegistry.default.layout)
    }

    @Test
    fun unknownIdFallsBackToDefault() {
        val resolved = ButtonPresetRegistry.fromId("non_existent_xyz")
        assertEquals("centered", resolved.id)
    }

    @Test
    fun aliasesMapToCanonicalPresets() {
        assertEquals("two_row", ButtonPresetRegistry.fromId("two_row_grid").id)
        assertEquals("radial_inspired", ButtonPresetRegistry.fromId("radial").id)
    }

    @Test
    fun allCanonicalPresetsArePresent() {
        val expectedIds = setOf(
            "centered", "split", "minimal", "waveform_below", "queue_inline",
            "expanded", "compact", "floating", "symmetric", "asymmetric",
            "two_row", "radial_inspired", "thumb_zone", "side_rail", "chips"
        )
        val registeredIds = ButtonPresets.map { it.id }.toSet()
        assertTrue(
            "Missing canonical presets: ${expectedIds - registeredIds}",
            registeredIds.containsAll(expectedIds)
        )
    }

    @Test
    fun eachPresetHasNonBlankNameAndDescription() {
        ButtonPresets.forEach { preset ->
            assertTrue("Preset ${preset.id} has blank name", preset.name.isNotBlank())
            assertTrue("Preset ${preset.id} has blank description", preset.description.isNotBlank())
        }
    }

    @Test
    fun fromIdReturnsCorrectPreset() {
        ButtonPresets.forEach { preset ->
            val resolved = ButtonPresetRegistry.fromId(preset.id)
            assertEquals("fromId('${preset.id}') must return ${preset.id}", preset.id, resolved.id)
        }
    }
}
