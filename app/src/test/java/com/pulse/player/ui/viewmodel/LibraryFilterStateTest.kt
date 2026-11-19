package com.pulse.player.ui.viewmodel

import com.pulse.player.playback.PlaybackUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFilterStateTest {

    @Test
    fun activeCount_reflectsEnabledFilters() {
        val empty = LibraryFilterState()
        assertEquals(0, empty.activeCount)

        val twoActive = LibraryFilterState(favoritesOnly = true, hiResOnly = true)
        assertEquals(2, twoActive.activeCount)

        val allActive = LibraryFilterState(
            favoritesOnly = true,
            hiResOnly = true,
            longOnly = true,
            unplayedOnly = true
        )
        assertEquals(4, allActive.activeCount)
    }

    @Test
    fun playbackUiState_defaultsAreConsistent() {
        val state = PlaybackUiState()
        assertEquals(1f, state.speed, 0.001f)
        assertTrue(state.pitchLocked)
        assertEquals(0f, state.pitchSemitones, 0.001f)
        assertEquals(10, state.sleepFadeSec)
        assertNull(state.abLoopStartMs)
        assertNull(state.abLoopEndMs)
        assertNull(state.trackError)
    }
}
