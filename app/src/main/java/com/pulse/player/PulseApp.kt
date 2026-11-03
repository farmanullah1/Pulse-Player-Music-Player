package com.pulse.player

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.favorites.FavoritesRepository
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.data.scan.LibraryScanner
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.playback.PlayerController
import com.pulse.player.data.backup.ArchiveBackupService
import com.pulse.player.data.backup.ArchiveRestoreService
import com.pulse.player.net.NetworkGateway
import com.pulse.player.net.NetworkGatewayFactory

open class PulseApp : Application() {
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
    lateinit var networkGateway: NetworkGateway
        private set

    override fun onCreate() {
        super.onCreate()
        // Back up before any migration runs; covers both 3→4 and any future path.
        try {
            PulseDb.backupDatabaseBeforeMigration(this, 3)
        } catch (e: Exception) {
            Log.w("PulseApp", "Could not create the pre-migration database backup", e)
        }
        db = Room.databaseBuilder(this, PulseDb::class.java, "pulse.db")
            .addMigrations(PulseDb.MIGRATION_1_2, PulseDb.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
        settings = SettingsStore(this)
        networkGateway = NetworkGatewayFactory.create(this, settings)
        scanner = LibraryScanner(this, db, settings)
        player = PlayerController(this)
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
