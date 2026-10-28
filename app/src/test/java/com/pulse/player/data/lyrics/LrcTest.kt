package com.pulse.player.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcTest {

    @Test
    fun parse_emptyString_returnsEmptyList() {
        val lines = Lrc.parse("")
        assertTrue(lines.isEmpty())
    }

    @Test
    fun parse_standardTimestamps() {
        val lrc = """
            [00:01.00]First line
            [00:04.50]Second line with fraction
            [01:10.000]One minute ten seconds
        """.trimIndent()

        val parsed = Lrc.parse(lrc)
        assertEquals(3, parsed.size)
        assertEquals(1000L, parsed[0].timeMs)
        assertEquals("First line", parsed[0].text)

        assertEquals(4500L, parsed[1].timeMs)
        assertEquals("Second line with fraction", parsed[1].text)

        assertEquals(70000L, parsed[2].timeMs)
        assertEquals("One minute ten seconds", parsed[2].text)
    }

    @Test
    fun parse_multiTagLines_expandedAndSorted() {
        val lrc = """
            [00:02.00][00:10.00]Chorus repeats
        """.trimIndent()

        val parsed = Lrc.parse(lrc)
        assertEquals(2, parsed.size)
        assertEquals(2000L, parsed[0].timeMs)
        assertEquals("Chorus repeats", parsed[0].text)
        assertEquals(10000L, parsed[1].timeMs)
        assertEquals("Chorus repeats", parsed[1].text)
    }

    @Test
    fun activeIndex_findsCurrentLineAccurately() {
        val lines = listOf(
            LrcLine(1000L, "Line 1"),
            LrcLine(4000L, "Line 2"),
            LrcLine(8000L, "Line 3")
        )

        assertEquals(-1, Lrc.activeIndex(lines, 500L))
        assertEquals(0, Lrc.activeIndex(lines, 1000L))
        assertEquals(0, Lrc.activeIndex(lines, 2500L))
        assertEquals(1, Lrc.activeIndex(lines, 4000L))
        assertEquals(1, Lrc.activeIndex(lines, 7999L))
        assertEquals(2, Lrc.activeIndex(lines, 8000L))
        assertEquals(2, Lrc.activeIndex(lines, 99999L))
    }
}

