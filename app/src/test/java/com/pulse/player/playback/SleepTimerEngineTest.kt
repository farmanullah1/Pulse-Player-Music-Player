package com.pulse.player.playback

import androidx.media3.common.Player
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerEngineTest {

    private fun createFakePlayer(): Pair<Player, MutableList<String>> {
        val calls = mutableListOf<String>()
        val proxy = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                    if (method != null) {
                        calls.add(method.name)
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

    @Test
    fun sleepTimer_cancelMidFade_restoresVolume() = runTest {
        var currentVolume = 1f
        val controller = AudioTransitionController(
            scope = this,
            sink = { currentVolume = it }
        )
        val (fakePlayer, _) = createFakePlayer()
        var virtualTime = 0L

        val engine = SleepTimerEngine(
            scope = this,
            raw = fakePlayer,
            t = controller,
            clock = { virtualTime }
        )

        engine.start(minutes = 1)
        assertEquals(60_000L, engine.state.value.remainingMs)

        // Advance into the final 10s fade window (at 55s, remaining = 5s)
        virtualTime = 55_000L
        controller.fadeTo(0f, 5000L, TransitionReason.SleepTimer)

        // Cancel mid fade
        engine.cancel()
        assertEquals(0L, engine.state.value.remainingMs)
        assertEquals(1.0f, controller.fadeMultiplier, 0.001f)
        assertEquals(1.0f, controller.effectiveVolume, 0.001f)
    }

    @Test
    fun sleepTimer_endOfTrackMode_configuresState() = runTest {
        val controller = AudioTransitionController(
            scope = this,
            sink = {}
        )
        val (fakePlayer, _) = createFakePlayer()

        val engine = SleepTimerEngine(
            scope = this,
            raw = fakePlayer,
            t = controller
        )

        engine.startEndOfTrack()
        assertTrue(engine.state.value.endOfTrack)

        engine.onPausedAtEndOfItem()
        assertFalse(engine.state.value.endOfTrack)
    }
}

