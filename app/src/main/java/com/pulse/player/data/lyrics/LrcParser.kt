package com.pulse.player.data.lyrics

data class LyricLine(val timeMs: Long, val text: String)

object LrcParser {
    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(raw: String): List<LyricLine>? {
        val out = ArrayList<LyricLine>()
        raw.lineSequence().forEach { line ->
            val stamps = tag.findAll(line).toList()
            if (stamps.isEmpty()) return@forEach
            val text = line.substring(stamps.last().range.last + 1).trim()
            stamps.forEach { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
                out += LyricLine(min * 60_000 + sec * 1000 + ms, text)
            }
        }
        return out.sortedBy { it.timeMs }.takeIf { it.isNotEmpty() }
    }

    fun activeIndex(lines: List<LyricLine>, posMs: Long): Int {
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
}

