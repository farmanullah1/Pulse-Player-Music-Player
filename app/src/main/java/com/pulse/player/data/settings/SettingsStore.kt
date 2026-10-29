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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.UUID

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
        val sortRaw = stringPreferencesKey("sort_raw")
        val songSortKey = stringPreferencesKey("song_sort_key")
        val songSortOrder = intPreferencesKey("song_sort_order")
        val deviceId = stringPreferencesKey("install_id")
        val lastBackup = stringPreferencesKey("last_backup_json")
        val geminiApiKey = stringPreferencesKey("gemini_api_key")
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
    val songSortKey: Flow<String> = ctx.dataStore.data.map { it[K.songSortKey] ?: "dateAdded" }
    val songSortOrder: Flow<Int> = ctx.dataStore.data.map { it[K.songSortOrder] ?: -1 }
    val geminiApiKey: Flow<String?> = ctx.dataStore.data.map { it[K.geminiApiKey] }

    suspend fun setGeminiApiKey(key: String) = ctx.dataStore.edit { it[K.geminiApiKey] = key.trim() }

    suspend fun effectiveGeminiApiKey(): String {
        val custom = geminiApiKey.first()?.trim().orEmpty()
        if (custom.isNotBlank()) return custom
        return com.pulse.player.BuildConfig.GEMINI_API_KEY.trim()
    }

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

    suspend fun installId(): String {
        val existing = ctx.dataStore.data.first()[K.deviceId]
        if (!existing.isNullOrBlank()) return existing
        val newId = UUID.randomUUID().toString().replace("-", "").take(16)
        ctx.dataStore.edit { it[K.deviceId] = newId }
        return newId
    }

    suspend fun excludedFoldersNow(): Set<String> = ctx.dataStore.data.first()[K.excluded] ?: emptySet()

    suspend fun addExcludedFolders(folders: Set<String>) {
        ctx.dataStore.edit {
            val cur = it[K.excluded] ?: emptySet()
            it[K.excluded] = cur + folders
        }
    }

    suspend fun lastBackup(): String? = ctx.dataStore.data.first()[K.lastBackup]
    suspend fun setLastBackup(json: String) {
        ctx.dataStore.edit { it[K.lastBackup] = json }
    }

    suspend fun sortRawOrNull(): Map<String, Any?>? {
        val raw = ctx.dataStore.data.first()[K.sortRaw] ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, Any?>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val sub = json.optJSONObject(k)
                if (sub != null) {
                    val subMap = mutableMapOf<String, Any?>()
                    val subKeys = sub.keys()
                    while (subKeys.hasNext()) {
                        val sk = subKeys.next()
                        subMap[sk] = sub.get(sk)
                    }
                    map[k] = subMap
                }
            }
            map
        }.getOrNull()
    }

    suspend fun saveSortRaw(map: Map<String, Any?>) {
        val json = JSONObject(map).toString()
        ctx.dataStore.edit {
            it[K.sortRaw] = json
            val allSong = map["All_Song"] as? Map<*, *>
            if (allSong != null) {
                val key = allSong["key"] as? String
                val order = (allSong["order"] as? Number)?.toInt()
                if (key != null) it[K.songSortKey] = key
                if (order != null) it[K.songSortOrder] = order
            }
        }
    }

    fun sortFor(screen: String, raw: Map<String, Any?>?): Pair<String, Int> {
        val o = raw?.get(screen) as? Map<*, *>
        val key = o?.get("key") as? String ?: when (screen) {
            "Album_" -> "album"
            "Album_Song" -> "sortTrackNumber"
            "Artist_" -> "artist"
            "Artist_Song" -> "dateAdded"
            "Genre_" -> "name"
            "Genre_Song" -> "dateAdded"
            else -> "dateAdded"
        }
        val order = (o?.get("order") as? Number)?.toInt() ?: when (screen) {
            "Album_", "Album_Song", "Artist_", "Genre_" -> 1
            else -> -1
        }
        return key to order
    }
}
