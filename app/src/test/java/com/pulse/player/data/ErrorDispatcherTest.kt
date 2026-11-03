package com.pulse.player.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorDispatcherTest {

    @Before
    fun setUp() {
        ErrorDispatcher.clearLog()
    }

    @Test
    fun postError_addsToRecentErrors() = runTest {
        ErrorDispatcher.post(
            category = ErrorCategory.PLAYBACK,
            title = "Playback Error",
            message = "Track not found"
        )

        val errors = ErrorDispatcher.recentErrors.value
        assertEquals(1, errors.size)
        assertEquals("Playback Error", errors[0].title)
        assertEquals("Track not found", errors[0].message)
        assertEquals(ErrorCategory.PLAYBACK, errors[0].category)
    }

    @Test
    fun postError_emitsEvent() = runTest {
        ErrorDispatcher.post(
            category = ErrorCategory.SCAN,
            title = "Scan Warning",
            message = "Permission revoked"
        )

        val emitted = ErrorDispatcher.events.first()
        assertEquals(ErrorCategory.SCAN, emitted.category)
        assertEquals("Scan Warning", emitted.title)
    }

    @Test
    fun clearLog_emptiesRecentErrors() = runTest {
        ErrorDispatcher.post(ErrorCategory.STORAGE, "Storage Error", "Disk full")
        assertTrue(ErrorDispatcher.recentErrors.value.isNotEmpty())

        ErrorDispatcher.clearLog()
        assertTrue(ErrorDispatcher.recentErrors.value.isEmpty())
    }

    @Test
    fun maxLogSize_isEnforced() = runTest {
        repeat(60) { i ->
            ErrorDispatcher.post(ErrorCategory.AUDIO_FX, "Error $i", "Details $i")
        }

        val errors = ErrorDispatcher.recentErrors.value
        assertEquals(50, errors.size)
        assertEquals("Error 59", errors[0].title)
    }
}
