package com.pulse.player.playback

import android.content.SharedPreferences
import android.os.SystemClock
import com.pulse.player.data.db.PlayEventEntity
import com.pulse.player.data.db.PulseDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

object StatsPolicy {
    const val COUNT_MIN_REAL_MS = 30_000L            // a play counts after 30 s of real listening…
    const val COUNT_MIN_FRACTION = 0.5f              // …or after half the track, whichever is smaller
    const val SKIP_MAX_FRACTION = 0.25f              // left via next/previous/pick before 25% and before 30 s → skipped
    const val IGNORE_BELOW_REAL_MS = 2_000L          // sessions shorter than this are discarded (accidental taps)

    fun counted(real: Long, media: Long, dur: Long): Boolean =
        real >= minOf(COUNT_MIN_REAL_MS, (dur * COUNT_MIN_FRACTION).toLong().coerceAtLeast(5_000L))

    fun skipped(real: Long, media: Long, dur: Long, userLeft: Boolean): Boolean =
        userLeft && !counted(real, media, dur) && (dur <= 0 || media < dur * SKIP_MAX_FRACTION)
}

class SessionTracker(
    private val db: PulseDb? = null,
    private val prefs: SharedPreferences,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val elapsedClock: () -> Long = { SystemClock.elapsedRealtime() },
    private val wallClock: () -> Long = { System.currentTimeMillis() },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + NonCancellable),
    private val eventWriter: (suspend (PlayEventEntity) -> Unit)? = null
) {
    private var trackId: String? = null
    private var durationMs = 0L
    private var segStart = 0L
    private var segSpeed = 1f
    private var running = false
    private var realMs = 0L
    private var mediaMs = 0L
    private var startedWall = 0L
    private var speedWeighted = 0.0

    /** Called from the service's Player.Listener. `userAction` is true when the change came from next/previous/pick. */
    fun onTrackStarted(id: String, dur: Long, playing: Boolean, speed: Float) {
        close(userLeft = false)
        trackId = id
        durationMs = dur
        realMs = 0
        mediaMs = 0
        speedWeighted = 0.0
        startedWall = wallClock()
        if (playing) resume(speed)
        checkpoint()
    }

    fun onPlayingChanged(playing: Boolean, speed: Float) {
        if (playing) resume(speed) else pause()
        checkpoint()
    }

    fun onSpeedChanged(speed: Float) {
        if (running) {
            pause()
            resume(speed)
        } else {
            segSpeed = speed
        }
    }

    fun onTrackLeft(userAction: Boolean) = close(userLeft = userAction)
    fun onDestroy() = close(userLeft = false)

    private fun resume(speed: Float) {
        if (running) return
        running = true
        segSpeed = speed
        segStart = elapsedClock()
    }

    private fun pause() {
        if (!running) return
        val dt = (elapsedClock() - segStart).coerceAtLeast(0)
        realMs += dt
        mediaMs += (dt * segSpeed).toLong()
        speedWeighted += dt * segSpeed
        running = false
    }

    private suspend fun recordEvent(e: PlayEventEntity) {
        if (eventWriter != null) {
            eventWriter.invoke(e)
        } else {
            db?.stats()?.insert(e)
        }
    }

    private fun close(userLeft: Boolean) {
        val id = trackId ?: return
        pause()
        val real = realMs
        val media = mediaMs
        trackId = null
        clearCheckpoint()
        if (real < StatsPolicy.IGNORE_BELOW_REAL_MS) return
        val z = zone()
        val dt = Instant.ofEpochMilli(startedWall).atZone(z)
        val e = PlayEventEntity(
            trackId = id,
            playedAtMs = startedWall,
            listenedMs = real,
            completed = durationMs > 0 && media >= durationMs * 0.95f,
            zoneId = z.id,
            monthKey = "%04d%02d".format(dt.year, dt.monthValue),
            playedFraction = if (durationMs > 0) (media.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            wasSkipped = StatsPolicy.skipped(real, media, durationMs, userLeft),
            speedAvg = if (real > 0) (media.toFloat() / real) else 1f,
            counted = StatsPolicy.counted(real, media, durationMs)
        )
        scope.launch {
            recordEvent(e)
        }
    }

    // ---- process-death recovery ----
    fun checkpoint() {
        val id = trackId ?: return
        if (running) {
            val dt = elapsedClock() - segStart
            prefs.edit().putString(
                "open",
                "$id|$startedWall|$durationMs|${realMs + dt}|${mediaMs + (dt * segSpeed).toLong()}"
            ).apply()
        } else {
            prefs.edit().putString(
                "open",
                "$id|$startedWall|$durationMs|$realMs|$mediaMs"
            ).apply()
        }
    }

    private fun clearCheckpoint() = prefs.edit().remove("open").apply()

    /** Call once at service/app start. A checkpoint that is still present means the process died mid-session. */
    suspend fun recoverOpenSession() {
        val s = prefs.getString("open", null) ?: return
        clearCheckpoint()
        val p = s.split("|")
        if (p.size != 5) return
        val real = p[3].toLongOrNull() ?: return
        val media = p[4].toLongOrNull() ?: return
        val dur = p[2].toLongOrNull() ?: 0L
        val start = p[1].toLongOrNull() ?: return
        if (real < StatsPolicy.IGNORE_BELOW_REAL_MS) return
        val z = zone()
        val dt = Instant.ofEpochMilli(start).atZone(z)
        recordEvent(
            PlayEventEntity(
                trackId = p[0],
                playedAtMs = start,
                listenedMs = real,
                completed = dur > 0 && media >= dur * 0.95f,
                zoneId = z.id,
                monthKey = "%04d%02d".format(dt.year, dt.monthValue),
                playedFraction = if (dur > 0) (media.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                wasSkipped = false,
                speedAvg = if (real > 0) media.toFloat() / real else 1f,
                counted = StatsPolicy.counted(real, media, dur)
            )
        )
    }
}
