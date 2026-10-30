package com.pulse.player.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LrcParserTest {

    @Test
    fun parseStandardSyncedLrc() {
        val lrcText = """
            [ti:Test Title]
            [ar:Test Artist]
            [00:05.100]First line
            [00:15.500]Second line
            [01:02.000]Third line
        """.trimIndent()

        val parsed = LrcParser.parse(lrcText)
        assertNotNull(parsed)
        assertEquals(3, parsed!!.size)

        assertEquals(5_100L, parsed[0].timeMs)
        assertEquals("First line", parsed[0].text)

        assertEquals(15_500L, parsed[1].timeMs)
        assertEquals("Second line", parsed[1].text)

        assertEquals(62_000L, parsed[2].timeMs)
        assertEquals("Third line", parsed[2].text)
    }

    @Test
    fun parseMultiTimestampLine() {
        val lrcText = "[00:10.00][00:20.00]Repeated chorus line"
        val parsed = LrcParser.parse(lrcText)
        assertNotNull(parsed)
        assertEquals(2, parsed!!.size)

        assertEquals(10_000L, parsed[0].timeMs)
        assertEquals("Repeated chorus line", parsed[0].text)

        assertEquals(20_000L, parsed[1].timeMs)
        assertEquals("Repeated chorus line", parsed[1].text)
    }

    @Test
    fun parseTwoDigitAndThreeDigitFractions() {
        val lrcText = """
            [00:01.50]Two digit fraction
            [00:02.755]Three digit fraction
            [00:03]No fraction
        """.trimIndent()

        val parsed = LrcParser.parse(lrcText)
        assertNotNull(parsed)
        assertEquals(3, parsed!!.size)

        assertEquals(1_500L, parsed[0].timeMs)
        assertEquals(2_755L, parsed[1].timeMs)
        assertEquals(3_000L, parsed[2].timeMs)
    }

    @Test
    fun nonSyncedPlainTextReturnsNull() {
        val plainText = """
            Just a regular song lyrics
            Without any timestamp tags
            Like a normal poem or text file
        """.trimIndent()

        val parsed = LrcParser.parse(plainText)
        assertNull("Plain text without timestamps must return null", parsed)
    }

    @Test
    fun activeIndexBinarySearch() {
        val lines = listOf(
            LyricLine(1000L, "Line 1"),
            LyricLine(3000L, "Line 2"),
            LyricLine(5000L, "Line 3"),
            LyricLine(8000L, "Line 4")
        )

        // Before any line starts
        assertEquals(-1, LrcParser.activeIndex(lines, 500L))

        // At exact start
        assertEquals(0, LrcParser.activeIndex(lines, 1000L))

        // In between
        assertEquals(0, LrcParser.activeIndex(lines, 2500L))
        assertEquals(1, LrcParser.activeIndex(lines, 3000L))
        assertEquals(1, LrcParser.activeIndex(lines, 4000L))
        assertEquals(2, LrcParser.activeIndex(lines, 6000L))

        // Past the last line
        assertEquals(3, LrcParser.activeIndex(lines, 8000L))
        assertEquals(3, LrcParser.activeIndex(lines, 20000L))
    }
}

