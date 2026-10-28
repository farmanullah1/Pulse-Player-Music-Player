package com.pulse.player.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class RoomMigrationTest {

    @Test
    fun testMigration1To2SqlStatements() {
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

        PulseDb.MIGRATION_1_2.migrate(dbProxy)

        assertEquals(6, executedSql.size)

        // 1. tracks.dataPath
        assertTrue(executedSql[0].contains("ALTER TABLE tracks ADD COLUMN dataPath TEXT"))

        // 2. tracks.scanGen
        assertTrue(executedSql[1].contains("ALTER TABLE tracks ADD COLUMN scanGen INTEGER NOT NULL DEFAULT 0"))

        // 3. playlists.sortKey
        assertTrue(executedSql[2].contains("ALTER TABLE playlists ADD COLUMN sortKey TEXT"))

        // 4. playlists.sortOrder
        assertTrue(executedSql[3].contains("ALTER TABLE playlists ADD COLUMN sortOrder INTEGER"))

        // 5. Deduplicate play_events
        assertTrue(executedSql[4].contains("DELETE FROM play_events WHERE id NOT IN"))
        assertTrue(executedSql[4].contains("GROUP BY trackId, playedAtMs"))

        // 6. Unique index on play_events
        assertTrue(
            executedSql[5].contains("CREATE UNIQUE INDEX IF NOT EXISTS index_play_events_trackId_playedAtMs")
        )
        assertTrue(
            executedSql[5].contains("ON play_events(trackId, playedAtMs)")
        )
    }
}

