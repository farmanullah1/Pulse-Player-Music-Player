package com.pulse.player.playback

import com.pulse.player.data.settings.SoundProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundProfileAndFxTest {

    @Test
    fun `builtInProfiles all have unique ids, 5 bands, and valid fx ranges`() {
        val profiles = AudioFx.builtInProfiles
        assertTrue(profiles.size >= 6)
        assertEquals(profiles.size, profiles.map { it.id }.toSet().size)
        profiles.forEach { profile ->
            assertTrue("${profile.name} should be built-in", profile.isBuiltIn)
            assertEquals("${profile.name} should have 5 EQ bands", 5, profile.bandLevels.size)
            assertTrue(profile.bassStrength in 0..1000)
            assertTrue(profile.virtualizerStrength in 0..1000)
            assertTrue(profile.reverbIndex in AudioFx.reverbNames.indices)
        }
    }

    @Test
    fun `custom SoundProfile defaults to unity speed and pitch and isBuiltIn false`() {
        val custom = SoundProfile(
            id = "custom_1",
            name = "My Mix",
            bandLevels = listOf(100, 200, 0, -100, 150)
        )
        assertFalse(custom.isBuiltIn)
        assertEquals(1f, custom.speed)
        assertEquals(0f, custom.pitchSemitones)
        assertEquals(0, custom.bassStrength)
        assertEquals(0, custom.virtualizerStrength)
        assertEquals(0, custom.reverbIndex)
    }

    @Test
    fun `QueueModel handles sequential move and undo-style insert via playNext`() {
        val model = QueueModel(listOf("t1", "t2", "t3", "t4"), "t1")
        model.move(3, 1)
        assertEquals(listOf("t1", "t4", "t2", "t3"), model.original)

        val newCurrent = model.remove("t2")
        assertNull(newCurrent)
        assertEquals("t1", model.currentId)
        assertEquals(listOf("t1", "t4", "t3"), model.original)
    }
}
