package com.pulse.player

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pulse.player.data.settings.ThemeMode
import com.pulse.player.ui.screens.AppShell
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.PulseTheme
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pulse.player.data.scan.audioPermission

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val firstRunPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioPerm = audioPermission()
        val audioGranted = results[audioPerm] == true
        vm.setAudioPermissionDenied(!audioGranted)
        if (audioGranted) {
            lifecycleScope.launch {
                val hasCached = withContext(Dispatchers.IO) {
                    vm.settings.lastGoodScanAt() != null && vm.db.tracks().availableCountOnce() > 0
                }
                if (!hasCached) vm.scan()
            }
        }
        lifecycleScope.launch {
            vm.settings.setAudioPermissionAsked(true)
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val windowSize = calculateWindowSizeClass(this)
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val skinId by vm.skinId.collectAsStateWithLifecycle()
            val reducedMotion by vm.reducedMotion.collectAsStateWithLifecycle()
            val colorSource by vm.colorSource.collectAsStateWithLifecycle()
            val seedColor by vm.seedColor.collectAsStateWithLifecycle()
            val cornerRadius by vm.cornerRadiusDp.collectAsStateWithLifecycle()
            val artworkAccent by vm.artworkAccent.collectAsStateWithLifecycle()
            val fontStyle by vm.fontStyle.collectAsStateWithLifecycle()
            val listDensity by vm.listDensity.collectAsStateWithLifecycle()
            val albumGridColumns by vm.albumGridColumns.collectAsStateWithLifecycle()
            val activeSkin = Skins.firstOrNull { it.id == skinId } ?: Skins[0]

            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT  -> false
                else             -> true
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
            }

            PulseTheme(
                mode = themeMode,
                skin = activeSkin,
                reducedMotion = reducedMotion,
                accent = Pulse.StudioAmber,
                colorSource = colorSource,
                seedColor = seedColor,
                artworkAccent = artworkAccent,
                cornerRadiusDp = cornerRadius,
                fontStyle = fontStyle,
                listDensity = listDensity,
                albumGridColumns = albumGridColumns
            ) {
                AppShell(vm = vm, windowSize = windowSize)
            }
        }

        requestFirstRunPermissions()
    }

    private fun requestFirstRunPermissions() {
        lifecycleScope.launch {
            val audioPerm = audioPermission()
            val hasAudio = ContextCompat.checkSelfPermission(this@MainActivity, audioPerm) == PackageManager.PERMISSION_GRANTED

            if (hasAudio) {
                vm.setAudioPermissionDenied(false)
                vm.settings.setAudioPermissionAsked(true)
                val hasCachedLibrary = withContext(Dispatchers.IO) {
                    vm.settings.lastGoodScanAt() != null && vm.db.tracks().availableCountOnce() > 0
                }
                if (!hasCachedLibrary) {
                    vm.scan()
                }
                return@launch
            }

            // Just-in-time: request only the minimum audio permission needed to build the library.
            val permissions = buildList {
                if (!hasAudio) add(audioPerm)
            }

            if (permissions.isNotEmpty()) {
                firstRunPermissionLauncher.launch(permissions.toTypedArray())
            } else {
                vm.settings.setAudioPermissionAsked(true)
                val hasCachedLibrary = withContext(Dispatchers.IO) {
                    vm.settings.lastGoodScanAt() != null && vm.db.tracks().availableCountOnce() > 0
                }
                if (!hasCachedLibrary) {
                    vm.scan()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            intent.data?.let { vm.playExternal(it) }
            setIntent(null)
        } else if (intent.action == "pulse.PLAY_TRACK") {
            intent.getStringExtra("id")?.let { vm.playTrackById(it) }
            setIntent(null)
        } else if (intent.action == "pulse.OPEN_NOW_PLAYING" || intent.getBooleanExtra("open_now_playing", false)) {
            vm.openNowPlaying()
            setIntent(null)
        }
    }

    override fun onPause() {
        super.onPause()
        vm.player.snapshot()?.let { (ids, idx, pos) ->
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching { vm.settings.saveQueue(ids, idx, pos) }
            }
        }
    }
}
