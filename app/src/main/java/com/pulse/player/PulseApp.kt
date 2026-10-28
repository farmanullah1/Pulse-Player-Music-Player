package com.pulse.player

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.favorites.FavoritesRepository
import com.pulse.player.data.scan.LibraryScanner
import com.pulse.player.data.settings.SettingsStore
import com.pulse.player.playback.PlayerController

import com.pulse.player.data.backup.ArchiveBackupService
import com.pulse.player.data.backup.ArchiveRestoreService
import com.pulse.player.data.scan.SampleMusicHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        db = Room.databaseBuilder(this, PulseDb::class.java, "pulse.db").build()
        settings = SettingsStore(this)
        scanner = LibraryScanner(this, db)
        player = PlayerController(this, db)
        favoritesRepo = FavoritesRepository(this, db)
        archiveBackup = ArchiveBackupService(this, db, settings)
        archiveRestore = ArchiveRestoreService(this, db, settings)

        // Seed sample music track so offline playback can be used immediately
        CoroutineScope(Dispatchers.IO).launch {
            SampleMusicHelper.installSampleTrack(this@PulseApp, db)
        }
    }
}

val Context.app: PulseApp
    get() = applicationContext as PulseApp
