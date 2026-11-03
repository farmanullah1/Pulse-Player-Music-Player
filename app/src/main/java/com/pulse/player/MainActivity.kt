package com.pulse.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pulse.player.ui.screens.AppShell
import com.pulse.player.ui.theme.Pulse
import com.pulse.player.ui.theme.PulseTheme
import com.pulse.player.ui.theme.Skins
import com.pulse.player.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.pulse.player.data.scan.audioPermission

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val firstRunPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioPerm = audioPermission()
        val audioGranted = results[audioPerm] == true
        if (audioGranted) {
            vm.scan()
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

        // Periodic snapshot of queue & position
        lifecycleScope.launch {
            while (true) {
                delay(5000)
                val ps = vm.player.state.value
                if (ps.isPlaying) {
                    vm.player.snapshot()?.let { (ids, idx, pos) ->
                        vm.settings.saveQueue(ids, idx, pos)
                    }
                }
            }
        }

        setContent {
            val windowSize = calculateWindowSizeClass(this)
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val skinId by vm.skinId.collectAsStateWithLifecycle()
            val reducedMotion by vm.reducedMotion.collectAsStateWithLifecycle()
            val activeSkin = Skins.firstOrNull { it.id == skinId } ?: Skins[0]

            PulseTheme(
                mode = themeMode,
                skin = activeSkin,
                reducedMotion = reducedMotion,
                accent = Pulse.Cyan
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
            val hasNotification = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val hasRecord = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

            if (hasAudio && hasNotification && hasRecord) {
                vm.settings.setAudioPermissionAsked(true)
                vm.scan()
                return@launch
            }

            val permissions = buildList {
                if (!hasAudio) add(audioPerm)
                if (Build.VERSION.SDK_INT >= 33 && !hasNotification) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (!hasRecord) {
                    add(Manifest.permission.RECORD_AUDIO)
                }
            }

            if (permissions.isNotEmpty()) {
                firstRunPermissionLauncher.launch(permissions.toTypedArray())
            } else {
                vm.settings.setAudioPermissionAsked(true)
                vm.scan()
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
        }
    }

    override fun onPause() {
        super.onPause()
        vm.player.snapshot()?.let { (ids, idx, pos) ->
            lifecycleScope.launch {
                vm.settings.saveQueue(ids, idx, pos)
            }
        }
    }
}
