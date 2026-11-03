package com.pulse.player.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class Migration3To4Test {

    private fun collectSql(): Pair<List<String>, SupportSQLiteDatabase> {
        val sql = mutableListOf<String>()
        val proxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                    if (method?.name == "execSQL" && args != null && args.isNotEmpty()) {
                        sql.add(args[0] as String)
                    }
                    return null
                }
            }
        ) as SupportSQLiteDatabase
        return sql to proxy
    }

    @Test
    fun migration3To4AltersTracksWithAllV4Columns() {
        val (sql, proxy) = collectSql()
        PulseDb.MIGRATION_3_4.migrate(proxy)

        val alters = sql.filter { it.startsWith("ALTER TABLE tracks") }
        val alterColumns = alters.map { it.substringAfter("ADD COLUMN").trim().substringBefore(" ") }

        assertTrue("genre column missing", alterColumns.contains("genre"))
        assertTrue("genresRaw column missing", alterColumns.contains("genresRaw"))
        assertTrue("albumArtistNorm column missing", alterColumns.contains("albumArtistNorm"))
        assertTrue("bitrate column missing", alterColumns.contains("bitrate"))
        assertTrue("sampleRate column missing", alterColumns.contains("sampleRate"))
        assertTrue("bitDepth column missing", alterColumns.contains("bitDepth"))
        assertTrue("codec column missing", alterColumns.contains("codec"))
        assertTrue("replayGainTrackDb column missing", alterColumns.contains("replayGainTrackDb"))
        assertTrue("replayGainAlbumDb column missing", alterColumns.contains("replayGainAlbumDb"))
        assertTrue("bpm column missing", alterColumns.contains("bpm"))
        assertTrue("language column missing", alterColumns.contains("language"))
        assertTrue("hasEmbeddedLyrics column missing", alterColumns.contains("hasEmbeddedLyrics"))
    }

    @Test
    fun migration3To4AltersLyricsTable() {
        val (sql, proxy) = collectSql()
        PulseDb.MIGRATION_3_4.migrate(proxy)

        val lyricsAlters = sql.filter { it.startsWith("ALTER TABLE lyrics") }
        assertEquals("lyrics table should gain 2 new columns", 2, lyricsAlters.size)
        assertTrue(lyricsAlters.any { it.contains("source") })
        assertTrue(lyricsAlters.any { it.contains("format") })
    }

    @Test
    fun migration3To4AddsOriginColumnToPlayEvents() {
        val (sql, proxy) = collectSql()
        PulseDb.MIGRATION_3_4.migrate(proxy)

        val origin = sql.filter { it.startsWith("ALTER TABLE play_events") && it.contains("origin") }
        assertEquals("play_events must gain exactly one origin column", 1, origin.size)
        assertTrue(origin[0].contains("DEFAULT 'LIVE'"))
    }

    @Test
    fun migration3To4CreatesAllNewTables() {
        val (sql, proxy) = collectSql()
        PulseDb.MIGRATION_3_4.migrate(proxy)

        val creates = sql.filter { it.startsWith("CREATE TABLE IF NOT EXISTS") }
            .map { it.substringAfter("CREATE TABLE IF NOT EXISTS ").substringBefore(" ").trim() }

        val expectedTables = listOf(
            "track_tags", "track_moods", "track_meta", "track_audio_profile",
            "bookmarks", "chapters", "saved_queues", "saved_queue_items",
            "smart_playlists", "artist_images", "sources", "cache_entries", "waveforms"
        )
        expectedTables.forEach { table ->
            assertTrue("$table table not created in MIGRATION_3_4", creates.contains(table))
        }
        assertEquals("Expected exactly ${expectedTables.size} new tables", expectedTables.size, creates.size)
    }

    @Test
    fun migration3To4CreatesIndexesForNewColumns() {
        val (sql, proxy) = collectSql()
        PulseDb.MIGRATION_3_4.migrate(proxy)

        val indexes = sql.filter { it.startsWith("CREATE INDEX") }
        val indexNames = indexes.map { it.substringAfter("CREATE INDEX IF NOT EXISTS ").substringBefore(" ") }

        assertTrue("genre index missing", indexNames.contains("index_tracks_genre"))
        assertTrue("language index missing", indexNames.contains("index_tracks_language"))
        assertTrue("track_tags.tag index missing", indexNames.contains("index_track_tags_tag"))
        assertTrue("track_moods.mood index missing", indexNames.contains("index_track_moods_mood"))
        assertTrue("bookmarks.trackId index missing", indexNames.contains("index_bookmarks_trackId"))
        assertTrue("artist_images.artistNorm index missing", indexNames.contains("index_artist_images_artistNorm"))
        assertTrue("cache_entries.lastUsedMs index missing", indexNames.contains("index_cache_entries_lastUsedMs"))
    }

    @Test
    fun migration3To4DoesNotDropOrRenameAnyExistingColumn() {
        val (sql, proxy) = collectSql()
        PulseDb.MIGRATION_3_4.migrate(proxy)

        // No DROP or RENAME statements are allowed (non-destructive migration gate)
        val destructive = sql.filter {
            it.contains("DROP COLUMN", ignoreCase = true) ||
            it.contains("DROP TABLE", ignoreCase = true) ||
            it.contains("RENAME COLUMN", ignoreCase = true)
        }
        assertTrue(
            "MIGRATION_3_4 must not drop or rename anything: $destructive",
            destructive.isEmpty()
        )
    }
}
