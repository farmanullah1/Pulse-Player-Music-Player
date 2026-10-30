package com.pulse.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class ErrorPolicyTest {

    private fun createFakePlayer(item: MediaItem?, hasNext: Boolean = false): Pair<Player, MutableList<String>> {
        val calls = mutableListOf<String>()
        val proxy = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                    if (method != null) {
                        calls.add(method.name)
                        if (method.name == "getCurrentMediaItem") return item
                        if (method.name == "hasNextMediaItem") return hasNext
                        if (method.returnType == Boolean::class.javaPrimitiveType) return false
                        if (method.returnType == Int::class.javaPrimitiveType) return 0
                        if (method.returnType == Long::class.javaPrimitiveType) return 0L
                    }
                    return null
                }
            }
        ) as Player
        return proxy to calls
    }

    private fun sampleItem(id: String, title: String) = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .build()

    @Test
    fun nonRetryableErrors_goStraightToErrorCard() {
        val item = sampleItem("1", "Missing Track")
        val (fakePlayer, calls) = createFakePlayer(item)
        val errorPolicy = ErrorPolicy(fakePlayer)

        val missingEx = PlaybackException(
            "File missing",
            null,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        )

        errorPolicy.onError(missingEx)

        val err = errorPolicy.current.value
        assertNotNull(err)
        assertEquals(ErrorPolicy.Category.MISSING_FILE, err?.category)
        assertTrue(calls.contains("pause"))
        // Should not have prepared/retried
        assertEquals(0, calls.count { it == "prepare" })
    }

    @Test
    fun genericError_retriesOnceAutomatically() {
        val item = sampleItem("2", "Flaky Track")
        val (fakePlayer, calls) = createFakePlayer(item)
        val errorPolicy = ErrorPolicy(fakePlayer)

        val genericEx = PlaybackException(
            "Unknown glitch",
            null,
            PlaybackException.ERROR_CODE_UNSPECIFIED
        )

        // 1st occurrence -> auto retry
        errorPolicy.onError(genericEx)
        assertNull(errorPolicy.current.value)
        assertEquals(1, calls.count { it == "prepare" })

        // 2nd occurrence -> halts and surfaces error card
        errorPolicy.onError(genericEx)
        val err = errorPolicy.current.value
        assertNotNull(err)
        assertEquals(ErrorPolicy.Category.OTHER, err?.category)
        assertTrue(calls.contains("pause"))
    }

    @Test
    fun skip_advancesToNextTrackWhenAvailable() {
        val item = sampleItem("3", "Corrupt Track")
        val (fakePlayer, calls) = createFakePlayer(item, hasNext = true)
        val errorPolicy = ErrorPolicy(fakePlayer)

        val corruptEx = PlaybackException(
            "Decoder failed",
            null,
            PlaybackException.ERROR_CODE_DECODING_FAILED
        )

        errorPolicy.onError(corruptEx)
        assertNotNull(errorPolicy.current.value)

        errorPolicy.skip()
        assertNull(errorPolicy.current.value)
        assertTrue(calls.contains("seekToNextMediaItem"))
        assertTrue(calls.contains("play"))
    }
}

