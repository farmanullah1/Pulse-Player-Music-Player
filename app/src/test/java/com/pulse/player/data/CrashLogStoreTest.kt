package com.pulse.player.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CrashLogStoreTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("crash_log_test").toFile()
        CrashLogStore.clearCrashes()
    }

    @After
    fun tearDown() {
        CrashLogStore.clearCrashes()
        tempDir.deleteRecursively()
    }

    @Test
    fun computeSignature_prefersAppFrame() {
        val trace = """
            java.lang.IllegalStateException: Boom
                at android.os.Handler.handleCallback(Handler.java:942)
                at com.pulse.player.ui.viewmodel.MainViewModel.playTracks(MainViewModel.kt:142)
                at com.pulse.player.MainActivity.onCreate(MainActivity.kt:88)
        """.trimIndent()

        val sig = CrashLogStore.computeSignature("java.lang.IllegalStateException", trace)
        assertEquals(
            "java.lang.IllegalStateException|at com.pulse.player.ui.viewmodel.MainViewModel.playTracks(MainViewModel.kt:142)",
            sig
        )
    }

    @Test
    fun mergeCrashRecord_deduplicatesAndIncrementsCount() {
        val first = CrashLogStore.buildCrashRecord(
            exceptionClass = "java.lang.NullPointerException",
            message = "Missing track",
            threadName = "main",
            stackTrace = "java.lang.NullPointerException\n\tat com.pulse.player.Test.run(Test.kt:10)",
            nowMs = 1000L,
            versionName = "239.983.4",
            versionCode = 3,
            deviceInfo = "TestDevice"
        )
        val second = CrashLogStore.buildCrashRecord(
            exceptionClass = "java.lang.NullPointerException",
            message = "Missing track",
            threadName = "main",
            stackTrace = "java.lang.NullPointerException\n\tat com.pulse.player.Test.run(Test.kt:10)",
            nowMs = 5000L,
            versionName = "239.983.4",
            versionCode = 3,
            deviceInfo = "TestDevice"
        )

        val mergedOnce = CrashLogStore.mergeCrashRecord(emptyList(), first)
        val mergedTwice = CrashLogStore.mergeCrashRecord(mergedOnce, second)

        assertEquals(1, mergedTwice.size)
        assertEquals(2, mergedTwice[0].count)
        assertEquals(1000L, mergedTwice[0].firstSeenMs)
        assertEquals(5000L, mergedTwice[0].lastSeenMs)
        assertTrue(mergedTwice[0].unacknowledged)
    }

    @Test
    fun recordUncaughtException_persistsAndSetsPendingRecovery() {
        val ex = IllegalArgumentException("Corrupt URI")
        CrashLogStore.recordUncaughtException(
            threadName = "PlaybackThread",
            throwable = ex,
            filesDir = tempDir,
            nowMs = 2000L
        )

        val persistedFile = File(tempDir, "pulse_crash_logs.json")
        assertTrue(persistedFile.exists())
        assertEquals(1, CrashLogStore.crashes.value.size)
        assertNotNull(CrashLogStore.pendingRecoveryCrash.value)
        assertEquals("PlaybackThread", CrashLogStore.crashes.value[0].threadName)

        CrashLogStore.acknowledgeRecovery()
        assertNull(CrashLogStore.pendingRecoveryCrash.value)
        assertEquals(false, CrashLogStore.crashes.value[0].unacknowledged)
    }

    @Test
    fun formatCrashReport_includesAppNameVersionAndDetails() {
        val record = CrashLogStore.buildCrashRecord(
            exceptionClass = "java.lang.RuntimeException",
            message = "Audio decoder failed",
            threadName = "ExoPlayer:Playback",
            stackTrace = "java.lang.RuntimeException: Audio decoder failed\n\tat com.pulse.player.playback.PlaybackEngine.init(PlaybackEngine.kt:50)",
            nowMs = 1700000000000L,
            versionName = "239.983.4",
            versionCode = 3,
            deviceInfo = "Pixel Test"
        )

        val report = CrashLogStore.formatCrashReport(record)
        assertTrue(report.contains("Pulse Music Player Crash Report"))
        assertTrue(report.contains("239.983.4 (3)"))
        assertTrue(report.contains("Pixel Test"))
        assertTrue(report.contains("Audio decoder failed"))
        assertTrue(report.contains("PlaybackEngine.kt:50"))
    }
}
