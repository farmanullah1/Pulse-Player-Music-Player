package com.pulse.player.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStateTest {

    private fun resolvePermissionState(
        isGranted: Boolean,
        hasSafAccess: Boolean,
        wasPreviouslyAsked: Boolean,
        canShowRationale: Boolean
    ): PermissionState {
        return when {
            isGranted -> PermissionState.Granted
            hasSafAccess -> PermissionState.Limited
            wasPreviouslyAsked && !canShowRationale -> PermissionState.PermanentlyDenied
            else -> PermissionState.Denied
        }
    }

    @Test
    fun initialState_isDenied() {
        val state = resolvePermissionState(
            isGranted = false,
            hasSafAccess = false,
            wasPreviouslyAsked = false,
            canShowRationale = true
        )
        assertEquals(PermissionState.Denied, state)
    }

    @Test
    fun granted_whenAudioPermissionGranted() {
        val state = resolvePermissionState(
            isGranted = true,
            hasSafAccess = false,
            wasPreviouslyAsked = true,
            canShowRationale = false
        )
        assertEquals(PermissionState.Granted, state)
    }

    @Test
    fun limited_whenSafGrantedWithoutAudioPermission() {
        val state = resolvePermissionState(
            isGranted = false,
            hasSafAccess = true,
            wasPreviouslyAsked = false,
            canShowRationale = true
        )
        assertEquals(PermissionState.Limited, state)
    }

    @Test
    fun permanentlyDenied_whenAskedAndCannotShowRationale() {
        val state = resolvePermissionState(
            isGranted = false,
            hasSafAccess = false,
            wasPreviouslyAsked = true,
            canShowRationale = false
        )
        assertEquals(PermissionState.PermanentlyDenied, state)
    }

    @Test
    fun fullTransitionCycle() {
        // 1. Initial install
        var state = resolvePermissionState(isGranted = false, hasSafAccess = false, wasPreviouslyAsked = false, canShowRationale = true)
        assertEquals(PermissionState.Denied, state)

        // 2. Denied once with rationale allowed
        state = resolvePermissionState(isGranted = false, hasSafAccess = false, wasPreviouslyAsked = true, canShowRationale = true)
        assertEquals(PermissionState.Denied, state)

        // 3. User chooses SAF folder without system-wide audio permission
        state = resolvePermissionState(isGranted = false, hasSafAccess = true, wasPreviouslyAsked = true, canShowRationale = true)
        assertEquals(PermissionState.Limited, state)

        // 4. Denied permanently (Don't ask again)
        state = resolvePermissionState(isGranted = false, hasSafAccess = false, wasPreviouslyAsked = true, canShowRationale = false)
        assertEquals(PermissionState.PermanentlyDenied, state)

        // 5. User grants in App Settings
        state = resolvePermissionState(isGranted = true, hasSafAccess = false, wasPreviouslyAsked = true, canShowRationale = false)
        assertEquals(PermissionState.Granted, state)
    }

    @Test
    fun isAudio_supportsExpectedExtensions() {
        assertTrue(SafScanner.isAudio("song.mp3", null))
        assertTrue(SafScanner.isAudio("track.m4a", null))
        assertTrue(SafScanner.isAudio("lossless.flac", null))
        assertTrue(SafScanner.isAudio("sound.wav", null))
        assertTrue(SafScanner.isAudio("stream.ogg", null))
        assertTrue(SafScanner.isAudio("voice.aac", null))
        assertTrue(SafScanner.isAudio("file.opus", null))
        assertTrue(SafScanner.isAudio("unknown_format", "audio/mpeg"))

        assertFalse(SafScanner.isAudio("picture.jpg", null))
        assertFalse(SafScanner.isAudio("video.mp4", "video/mp4"))
        assertFalse(SafScanner.isAudio("document.pdf", "application/pdf"))
    }
}

