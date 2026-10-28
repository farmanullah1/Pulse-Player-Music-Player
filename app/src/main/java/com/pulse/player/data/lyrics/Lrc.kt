package com.pulse.player.data.lyrics

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
}

