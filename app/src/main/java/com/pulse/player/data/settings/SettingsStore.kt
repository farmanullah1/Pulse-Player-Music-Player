package com.pulse.player.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("pulse_settings")

enum class ThemeMode { SYSTEM, DARK, AMOLED, LIGHT }

class SettingsStore(private val ctx: Context) {
    private object K {
        val theme = stringPreferencesKey("theme_mode")
        val skin = stringPreferencesKey("skin_id")
        val player = stringPreferencesKey("player_theme")
        val wallpaper = stringPreferencesKey("wallpaper_path")
        val reduced = booleanPreferencesKey("reduced_motion")
        val dynamic = booleanPreferencesKey("dynamic_accent")
        val minDur = intPreferencesKey("min_duration_s")
        val fade = booleanPreferencesKey("fade_play_pause")
        val excluded = stringSetPreferencesKey("excluded_folders")
        val safTrees = stringSetPreferencesKey("saf_trees")
        val queueIds = stringPreferencesKey("queue_ids")
        val queueIndex = intPreferencesKey("queue_index")
        val queuePos = longPreferencesKey("queue_pos")
        val eqPreset = stringPreferencesKey("eq_preset")
    }

    val themeMode: Flow<ThemeMode> = ctx.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[K.theme] ?: "DARK") }.getOrDefault(ThemeMode.DARK)
    }

    val skinId: Flow<String> = ctx.dataStore.data.map { it[K.skin] ?: "plum" }
    val playerThemeId: Flow<String> = ctx.dataStore.data.map { it[K.player] ?: "classic" }
    val wallpaperPath: Flow<String?> = ctx.dataStore.data.map { it[K.wallpaper] }
    val reducedMotion: Flow<Boolean> = ctx.dataStore.data.map { it[K.reduced] ?: false }
    val dynamicAccent: Flow<Boolean> = ctx.dataStore.data.map { it[K.dynamic] ?: true }
    val minDurationSec: Flow<Int> = ctx.dataStore.data.map { it[K.minDur] ?: 30 }
    val fadeEnabled: Flow<Boolean> = ctx.dataStore.data.map { it[K.fade] ?: true }
    val excludedFolders: Flow<Set<String>> = ctx.dataStore.data.map { it[K.excluded] ?: emptySet() }
    val safTrees: Flow<Set<String>> = ctx.dataStore.data.map { it[K.safTrees] ?: emptySet() }

    suspend fun setTheme(m: ThemeMode) = ctx.dataStore.edit { it[K.theme] = m.name }
    suspend fun setSkin(id: String) = ctx.dataStore.edit {
        it[K.skin] = id
        it.remove(K.wallpaper)
    }
    suspend fun setPlayerTheme(id: String) = ctx.dataStore.edit { it[K.player] = id }
    suspend fun setWallpaper(path: String?) = ctx.dataStore.edit {
        if (path == null) it.remove(K.wallpaper) else it[K.wallpaper] = path
    }
    suspend fun setReduced(v: Boolean) = ctx.dataStore.edit { it[K.reduced] = v }
    suspend fun setDynamic(v: Boolean) = ctx.dataStore.edit { it[K.dynamic] = v }
    suspend fun setMinDuration(s: Int) = ctx.dataStore.edit { it[K.minDur] = s }
    suspend fun setFade(v: Boolean) = ctx.dataStore.edit { it[K.fade] = v }
    suspend fun addSafTree(uri: String) = ctx.dataStore.edit {
        it[K.safTrees] = (it[K.safTrees] ?: emptySet()) + uri
    }
    suspend fun saveQueue(ids: List<String>, index: Int, pos: Long) = ctx.dataStore.edit {
        it[K.queueIds] = ids.joinToString("\n")
        it[K.queueIndex] = index
        it[K.queuePos] = pos
    }

    val queue: Flow<Triple<List<String>, Int, Long>> = ctx.dataStore.data.map {
        val ids = (it[K.queueIds] ?: "").split("\n").filter(String::isNotBlank)
        val idx = it[K.queueIndex] ?: 0
        val pos = it[K.queuePos] ?: 0L
        Triple(ids, idx, pos)
    }
}
