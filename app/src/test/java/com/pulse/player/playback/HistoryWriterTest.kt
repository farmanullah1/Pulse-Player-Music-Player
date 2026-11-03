package com.pulse.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for HistoryWriter origin enforcement.
 * Full DB integration is tested in instrumented tests; these cover the model constraints.
 */
class HistoryWriterTest {

    @Test
    fun importOriginCodesAreDistinctFromLive() {
        val liveCodes = setOf("LIVE")
        val importCodes = HistoryWriter.ImportOrigin.entries.map { it.code }.toSet()

        // No import origin should use the LIVE code — that is reserved for SessionTracker
        importCodes.forEach { code ->
            assertNotEquals(
                "ImportOrigin must not use 'LIVE' code: found $code",
                "LIVE", code
            )
        }
    }

    @Test
    fun allImportOriginsHaveNonBlankCodes() {
        HistoryWriter.ImportOrigin.entries.forEach { origin ->
            assert(origin.code.isNotBlank()) {
                "ImportOrigin.${origin.name} has a blank code"
            }
        }
    }

    @Test
    fun importOriginsMatchExpectedValues() {
        assertEquals("IMPORT_LASTFM",      HistoryWriter.ImportOrigin.LAST_FM.code)
        assertEquals("IMPORT_SPOTIFY",     HistoryWriter.ImportOrigin.SPOTIFY.code)
        assertEquals("IMPORT_LISTENBRAINZ",HistoryWriter.ImportOrigin.LISTEN_BRAINZ.code)
        assertEquals("IMPORT_YOUTUBE",     HistoryWriter.ImportOrigin.YOUTUBE.code)
        assertEquals("MANUAL",             HistoryWriter.ImportOrigin.MANUAL.code)
    }

    @Test
    fun importResultContainsAllCountFields() {
        val result = HistoryWriter.ImportResult(
            inserted = 10,
            skipped = 2,
            unmatched = 1,
            errors = emptyList()
        )
        assertEquals(10, result.inserted)
        assertEquals(2, result.skipped)
        assertEquals(1, result.unmatched)
        assertEquals(0, result.errors.size)
    }
}
