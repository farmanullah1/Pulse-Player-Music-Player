package com.pulse.player.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class Migration2To3Test {

    @Test
    fun testMigration2To3SqlStatements() {
        val executedSql = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                    if (method?.name == "execSQL" && args != null && args.isNotEmpty()) {
                        executedSql.add(args[0] as String)
                    }
                    return null
                }
            }
        ) as SupportSQLiteDatabase

        PulseDb.MIGRATION_2_3.migrate(dbProxy)

        assertEquals(17, executedSql.size)

        // 1. tracks.sourceType
        assertTrue(executedSql[0].contains("ALTER TABLE tracks ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'MEDIASTORE'"))
        // 2. tracks.stableId
        assertTrue(executedSql[1].contains("ALTER TABLE tracks ADD COLUMN stableId TEXT NOT NULL DEFAULT ''"))
        // 3. tracks.volumeId
        assertTrue(executedSql[2].contains("ALTER TABLE tracks ADD COLUMN volumeId TEXT NOT NULL DEFAULT 'external_primary'"))
        // 4. tracks.folderUri
        assertTrue(executedSql[3].contains("ALTER TABLE tracks ADD COLUMN folderUri TEXT"))
        // 5. backfill stableId for ms:
        assertTrue(executedSql[4].contains("UPDATE tracks SET stableId = substr(id, 4) WHERE id LIKE 'ms:%'"))
        // 6. backfill sourceType and stableId for saf:
        assertTrue(executedSql[5].contains("UPDATE tracks SET sourceType = 'SAF', stableId = substr(id, 5) WHERE id LIKE 'saf:%'"))
        // 7. index tracks(sourceType, volumeId)
        assertTrue(executedSql[6].contains("CREATE INDEX IF NOT EXISTS index_tracks_sourceType_volumeId ON tracks(sourceType, volumeId)"))
        // 8. scan_seen table
        assertTrue(executedSql[7].contains("CREATE TABLE IF NOT EXISTS scan_seen (id TEXT NOT NULL, PRIMARY KEY(id))"))
        // 9. lyrics table
        assertTrue(executedSql[8].contains("CREATE TABLE IF NOT EXISTS lyrics (trackId TEXT NOT NULL, text TEXT NOT NULL, synced INTEGER NOT NULL, importedAtMs INTEGER NOT NULL, PRIMARY KEY(trackId))"))
        // 10-15. play_events additions
        assertTrue(executedSql[9].contains("ALTER TABLE play_events ADD COLUMN zoneId TEXT NOT NULL DEFAULT ''"))
        assertTrue(executedSql[10].contains("ALTER TABLE play_events ADD COLUMN monthKey TEXT NOT NULL DEFAULT ''"))
        assertTrue(executedSql[11].contains("ALTER TABLE play_events ADD COLUMN playedFraction REAL NOT NULL DEFAULT 0"))
        assertTrue(executedSql[12].contains("ALTER TABLE play_events ADD COLUMN wasSkipped INTEGER NOT NULL DEFAULT 0"))
        assertTrue(executedSql[13].contains("ALTER TABLE play_events ADD COLUMN speedAvg REAL NOT NULL DEFAULT 1"))
        assertTrue(executedSql[14].contains("ALTER TABLE play_events ADD COLUMN counted INTEGER NOT NULL DEFAULT 1"))
        // 16. backfill monthKey
        assertTrue(executedSql[15].contains("UPDATE play_events SET monthKey = strftime('%Y%m', playedAtMs/1000, 'unixepoch', 'localtime')"))
        // 17. create index
        assertTrue(executedSql[16].contains("CREATE INDEX IF NOT EXISTS index_play_events_monthKey ON play_events(monthKey)"))
    }
}
