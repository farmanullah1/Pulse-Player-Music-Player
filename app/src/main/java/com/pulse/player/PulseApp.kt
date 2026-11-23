package com.pulse.player

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.pulse.player.data.CrashLogStore
import com.pulse.player.data.ErrorCategory
import com.pulse.player.data.ErrorDispatcher
import com.pulse.player.data.backup.classic.FavoritesRepository
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.scan.ArtworkLoader
import com.pulse.player.data.scan.LibraryScanner
import com.pulse.player.data.scan.MediaStoreContentObserver
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.playback.AudioDeviceMonitor
import com.pulse.player.playback.PlayerController
import com.pulse.player.data.backup.ArchiveBackupService
import com.pulse.player.data.backup.ArchiveRestoreService
import com.pulse.player.net.NetworkGateway
import com.pulse.player.net.NetworkGatewayFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class PulseApp : Application() {
    lateinit var db: PulseDb
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var scanner: LibraryScanner
        private set
    lateinit var player: PlayerController
        private set
    lateinit var favoritesRepo: com.pulse.player.data.favorites.FavoritesRepository
        private set
    lateinit var archiveBackup: ArchiveBackupService
        private set
    lateinit var archiveRestore: ArchiveRestoreService
        private set
    lateinit var networkGateway: NetworkGateway
        private set

    lateinit var mediaStoreObserver: MediaStoreContentObserver
        private set

    private val appInitScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CrashLogStore.install(this)
        // Back up before any migration runs; covers both 3→4 and any future path.
        try {
            PulseDb.backupDatabaseBeforeMigration(this, 3)
        } catch (e: Exception) {
            Log.w("PulseApp", "Could not create the pre-migration database backup", e)
            ErrorDispatcher.post(
                ErrorCategory.STORAGE,
                "Database Backup Warning",
                "Pre-migration safety backup could not be created.",
                e.stackTraceToString()
            )
        }
        db = Room.databaseBuilder(this, PulseDb::class.java, "pulse.db")
            .addMigrations(
                PulseDb.MIGRATION_1_2,
                PulseDb.MIGRATION_2_3,
                PulseDb.MIGRATION_3_4,
                PulseDb.MIGRATION_4_5,
                PulseDb.MIGRATION_5_6
            )
            .build()
        settings = SettingsStore(this)
        networkGateway = NetworkGatewayFactory.create(this, settings)
        scanner = LibraryScanner(this, db, settings)
        player = PlayerController(this)
        favoritesRepo = com.pulse.player.data.favorites.FavoritesRepository(this, db)
        archiveBackup = ArchiveBackupService(this, db, settings)
        archiveRestore = ArchiveRestoreService(this, db, settings)
        AudioDeviceMonitor.start(this)
        mediaStoreObserver = MediaStoreContentObserver(this, appInitScope, scanner, settings).apply {
            register()
        }
        appInitScope.launch {
            runCatching { ArtworkLoader.trimDiskCacheOnStartup(this@PulseApp) }
            runCatching { FavoritesRepository.ensureDefaultPulseFolder(this@PulseApp) }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        ArtworkLoader.trim(level)
        com.pulse.player.ui.theme.immersive.BlurCache.clear()
    }
}

val Context.app: PulseApp
    get() = applicationContext as PulseApp
