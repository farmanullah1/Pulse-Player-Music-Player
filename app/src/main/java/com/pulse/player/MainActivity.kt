package com.pulse.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

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
