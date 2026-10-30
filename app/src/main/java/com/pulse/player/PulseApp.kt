package com.pulse.player

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.favorites.FavoritesRepository
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.data.scan.LibraryScanner
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.playback.PlayerController
import com.pulse.player.data.backup.ArchiveBackupService
import com.pulse.player.data.backup.ArchiveRestoreService

class PulseApp : Application() {
    lateinit var db: PulseDb
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var scanner: LibraryScanner
        private set
    lateinit var player: PlayerController
        private set
    lateinit var favoritesRepo: FavoritesRepository
        private set
    lateinit var archiveBackup: ArchiveBackupService
        private set
    lateinit var archiveRestore: ArchiveRestoreService
        private set

    override fun onCreate() {
        super.onCreate()
        PulseDb.backupDatabaseBeforeMigration(this, 2)
        db = Room.databaseBuilder(this, PulseDb::class.java, "pulse.db")
            .addMigrations(PulseDb.MIGRATION_1_2, PulseDb.MIGRATION_2_3)
            .build()
        settings = SettingsStore(this)
        scanner = LibraryScanner(this, db, settings)
        player = PlayerController(this, db)
        favoritesRepo = FavoritesRepository(this, db)
        archiveBackup = ArchiveBackupService(this, db, settings)
        archiveRestore = ArchiveRestoreService(this, db, settings)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        ArtworkLoader.trim(level)
        com.pulse.player.ui.theme.immersive.BlurCache.clear()
    }
}

val Context.app: PulseApp
    get() = applicationContext as PulseApp
