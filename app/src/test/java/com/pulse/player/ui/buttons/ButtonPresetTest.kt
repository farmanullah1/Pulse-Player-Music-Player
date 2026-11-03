package com.pulse.player.ui.buttons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonPresetTest {

    @Test
    fun atLeast12PresetsRegistered() {
        assertTrue("Must have at least 12 button presets", ButtonPresets.size >= 12)
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
    fun allCanonicalPresetsArePresent() {
        val expectedIds = setOf(
            "centered", "split", "minimal", "waveform_below", "queue_inline",
            "expanded", "compact", "floating", "symmetric", "asymmetric",
            "two_row", "radial_inspired"
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
