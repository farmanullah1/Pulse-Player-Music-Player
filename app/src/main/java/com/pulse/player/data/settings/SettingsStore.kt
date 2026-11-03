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

/** Controls where the accent/seed color comes from. */
enum class ColorSource { PULSE_PALETTE, MATERIAL_YOU, ARTWORK_DRIVEN, CUSTOM_SEED }

class SettingsStore(private val ctx: Context) {
    private object K {
        val theme = stringPreferencesKey("theme_mode")
        val skin = stringPreferencesKey("skin_id")
        val player = stringPreferencesKey("player_theme")
        val playerLayout = stringPreferencesKey("player_layout_mode")
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
        val lastGoodScanAt = longPreferencesKey("last_good_scan_at")
        val audioAsked = booleanPreferencesKey("audio_permission_asked")
        // Schema v4 / Phase M2 additions
        val colorSource = stringPreferencesKey("color_source")
        val seedColor = intPreferencesKey("seed_color")           // ARGB int for custom seed
        val cornerRadius = intPreferencesKey("corner_radius_dp")  // 0–32 dp
        val buttonPreset = stringPreferencesKey("button_preset_id")
        val onlineFeaturesEnabled = booleanPreferencesKey("online_features_enabled")
        val offlineOnlyMode = booleanPreferencesKey("offline_only_mode")
        val animationIntensity = intPreferencesKey("animation_intensity") // 0=off, 1=reduced, 2=full
        val listDensity = intPreferencesKey("list_density")               // 0=compact, 1=normal, 2=comfortable
        val albumGridColumns = intPreferencesKey("album_grid_columns")    // 2..4
        val minFileSizeKb = intPreferencesKey("min_file_size_kb")         // 0 = no filter
    }

    val themeMode: Flow<ThemeMode> = ctx.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[K.theme] ?: "DARK") }.getOrDefault(ThemeMode.DARK)
    }

    val skinId: Flow<String> = ctx.dataStore.data.map { it[K.skin] ?: "plum" }
    val playerThemeId: Flow<String> = ctx.dataStore.data.map { it[K.player] ?: "classic" }
    val playerLayoutId: Flow<String> = ctx.dataStore.data.map { it[K.playerLayout] ?: "atmospheric_queue" }
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

    suspend fun lastGoodScanAt(): Long? = ctx.dataStore.data.first()[K.lastGoodScanAt]

    suspend fun setLastGoodScanAt(ms: Long) {
        ctx.dataStore.edit { it[K.lastGoodScanAt] = ms }
    }

    suspend fun audioPermissionAsked(): Boolean = ctx.dataStore.data.first()[K.audioAsked] ?: false

    suspend fun setAudioPermissionAsked(asked: Boolean) {
        ctx.dataStore.edit { it[K.audioAsked] = asked }
    }

    suspend fun setGeminiApiKey(key: String) = ctx.dataStore.edit { it[K.geminiApiKey] = key.trim() }

    suspend fun effectiveGeminiApiKey(): String {
        val custom = geminiApiKey.first()?.trim().orEmpty()
        if (custom.isNotBlank()) return custom
        return ""
    }

    suspend fun setTheme(m: ThemeMode) = ctx.dataStore.edit { it[K.theme] = m.name }
    suspend fun setSkin(id: String) = ctx.dataStore.edit {
        it[K.skin] = id
        it.remove(K.wallpaper)
    }
    suspend fun setPlayerTheme(id: String) = ctx.dataStore.edit { it[K.player] = id }
    suspend fun setPlayerLayout(id: String) = ctx.dataStore.edit { it[K.playerLayout] = id }
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
    suspend fun removeSafTree(uri: String) = ctx.dataStore.edit {
        it[K.safTrees] = (it[K.safTrees] ?: emptySet()) - uri
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

    // ─── Phase M2 new settings flows ─────────────────────────────────────────

    val colorSource: Flow<ColorSource> = ctx.dataStore.data.map {
        runCatching { ColorSource.valueOf(it[K.colorSource] ?: "PULSE_PALETTE") }
            .getOrDefault(ColorSource.PULSE_PALETTE)
    }
    /** Custom seed color as ARGB int; 0 means "use default Cyan". */
    val seedColor: Flow<Int> = ctx.dataStore.data.map { it[K.seedColor] ?: 0 }
    val cornerRadiusDp: Flow<Int> = ctx.dataStore.data.map { (it[K.cornerRadius] ?: 16).coerceIn(0, 32) }
    val buttonPresetId: Flow<String> = ctx.dataStore.data.map { it[K.buttonPreset] ?: "centered" }
    val onlineFeaturesEnabled: Flow<Boolean> = ctx.dataStore.data.map { it[K.onlineFeaturesEnabled] ?: false }
    val offlineOnlyMode: Flow<Boolean> = ctx.dataStore.data.map { it[K.offlineOnlyMode] ?: false }
    val animationIntensity: Flow<Int> = ctx.dataStore.data.map { (it[K.animationIntensity] ?: 2).coerceIn(0, 2) }
    val listDensity: Flow<Int> = ctx.dataStore.data.map { (it[K.listDensity] ?: 1).coerceIn(0, 2) }
    val albumGridColumns: Flow<Int> = ctx.dataStore.data.map { (it[K.albumGridColumns] ?: 2).coerceIn(2, 4) }
    val minFileSizeKb: Flow<Int> = ctx.dataStore.data.map { (it[K.minFileSizeKb] ?: 0).coerceAtLeast(0) }

    suspend fun setColorSource(s: ColorSource) = ctx.dataStore.edit { it[K.colorSource] = s.name }
    suspend fun setSeedColor(argb: Int) = ctx.dataStore.edit { it[K.seedColor] = argb }
    suspend fun setCornerRadius(dp: Int) = ctx.dataStore.edit { it[K.cornerRadius] = dp.coerceIn(0, 32) }
    suspend fun setButtonPreset(id: String) = ctx.dataStore.edit { it[K.buttonPreset] = id }
    suspend fun setOnlineFeaturesEnabled(v: Boolean) = ctx.dataStore.edit { it[K.onlineFeaturesEnabled] = v }
    suspend fun setOfflineOnlyMode(v: Boolean) = ctx.dataStore.edit { it[K.offlineOnlyMode] = v }
    suspend fun setAnimationIntensity(v: Int) = ctx.dataStore.edit { it[K.animationIntensity] = v.coerceIn(0, 2) }
    suspend fun setListDensity(v: Int) = ctx.dataStore.edit { it[K.listDensity] = v.coerceIn(0, 2) }
    suspend fun setAlbumGridColumns(v: Int) = ctx.dataStore.edit { it[K.albumGridColumns] = v.coerceIn(2, 4) }
    suspend fun setMinFileSizeKb(v: Int) = ctx.dataStore.edit { it[K.minFileSizeKb] = v.coerceAtLeast(0) }

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
