package com.pulse.player.data.lyrics

import java.io.File
import java.util.Locale

data class LrcLine(val timeMs: Long, val text: String)

object Lrc {
    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(src: String): List<LrcLine> = buildList {
        for (raw in src.lineSequence()) {
            val ms = tag.findAll(raw).toList()
            if (ms.isEmpty()) continue
            val text = raw.substring(ms.last().range.last + 1).trim()
            for (m in ms) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val msv = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
                add(LrcLine(min * 60_000 + sec * 1000 + msv, text))
            }
        }
    }.sortedBy { it.timeMs }

    fun activeIndex(lines: List<LrcLine>, posMs: Long): Int {
        var lo = 0
        var hi = lines.lastIndex
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= posMs) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return ans
    }

    fun formatTimestamp(timeMs: Long): String {
        val safe = timeMs.coerceAtLeast(0L)
        val totalSeconds = safe / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val centis = (safe % 1000L) / 10L
        return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, centis)
    }

    fun shiftTimestamps(lines: List<LrcLine>, deltaMs: Long): List<LrcLine> {
        if (deltaMs == 0L) return lines
        return lines.map { line ->
            if (line.timeMs < 0L) line
            else line.copy(timeMs = (line.timeMs + deltaMs).coerceAtLeast(0L))
        }.sortedBy { if (it.timeMs < 0L) Long.MAX_VALUE else it.timeMs }
    }

    fun applyPlaybackLeadOffset(lines: List<LrcLine>, playbackLeadOffsetMs: Long): List<LrcLine> =
        shiftTimestamps(lines, -playbackLeadOffsetMs)

    fun serialize(lines: List<LrcLine>): String {
        val sorted = lines.sortedWith(
            compareBy<LrcLine> { if (it.timeMs < 0L) Long.MAX_VALUE else it.timeMs }
        )
        return sorted.joinToString("\n") { line ->
            if (line.timeMs >= 0L) "${formatTimestamp(line.timeMs)}${line.text}" else line.text
        }
    }

    fun discoverSidecarLrc(trackDataPath: String?): File? {
        if (trackDataPath.isNullOrBlank()) return null
        return runCatching {
            val audioFile = File(trackDataPath)
            val parent = audioFile.parentFile ?: return@runCatching null
            if (!parent.exists() || !parent.isDirectory) return@runCatching null
            val stem = audioFile.nameWithoutExtension
            if (stem.isBlank()) return@runCatching null
            listOf(
                File(parent, "$stem.lrc"),
                File(parent, "$stem.LRC"),
                File(parent, "${audioFile.name}.lrc")
            ).firstOrNull { it.exists() && it.isFile && it.length() in 1L..(512 * 1024L) }
        }.getOrNull()
    }
}

