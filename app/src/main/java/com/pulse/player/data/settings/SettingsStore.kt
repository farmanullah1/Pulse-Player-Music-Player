package com.pulse.player.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

val Context.dataStore by preferencesDataStore("pulse_settings")

enum class ThemeMode { SYSTEM, DARK, AMOLED, LIGHT }

/** Controls where the accent/seed color comes from. */
enum class ColorSource { PULSE_PALETTE, MATERIAL_YOU, ARTWORK_DRIVEN, CUSTOM_SEED }

/** Controls typography display & accent pairing across the app. */
enum class FontStylePreset(val id: String, val label: String, val subtitle: String) {
    MODERN("modern", "Modern Sans", "Geometric display headers with crisp UI sans"),
    EDITORIAL("editorial", "Editorial Serif", "Classic serif titles paired with clean body text"),
    TECHNICAL("technical", "Studio Mono", "Monospace accents with high-clarity sans")
}

enum class ReplayGainMode(val label: String) {
    OFF("Off"),
    TRACK("Track"),
    ALBUM("Album")
}

/** Controls Now Playing progress seekbar visualization. */
enum class SeekStylePreset(val id: String, val label: String) {
    WAVEFORM("waveform", "Waveform"),
    WAVY("wavy", "Wavy"),
    LINEAR("classic", "Classic")
}

data class SoundProfile(
    val id: String,
    val name: String,
    val bandLevels: List<Int>,
    val bassStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val reverbIndex: Int = 0,
    val speed: Float = 1f,
    val pitchSemitones: Float = 0f,
    val isBuiltIn: Boolean = false,
    val limiterEnabled: Boolean = true,
    val channelBalance: Float = 0f,
    val monoMode: Boolean = false
)

data class DeviceDspRule(
    val deviceKey: String,
    val profileId: String? = null,
    val crossfadeSecOverride: Int? = null,
    val replayGainModeOverride: ReplayGainMode? = null,
    val monoModeOverride: Boolean? = null,
    val channelBalanceOverride: Float? = null
)

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
        val crossfadeSecs = intPreferencesKey("crossfade_secs")            // 0..12 seconds
        val homeTabs = stringPreferencesKey("home_tabs_csv")
        val fontStyle = stringPreferencesKey("font_style_preset")
        val seekStyle = stringPreferencesKey("seek_style_preset")
        val hiddenPlayerElements = stringSetPreferencesKey("hidden_player_elements")
        val customButtonOrder = stringPreferencesKey("custom_button_order_csv")
        val forYouOrder = stringPreferencesKey("for_you_order_csv")
        val hiddenForYouSections = stringSetPreferencesKey("hidden_for_you_sections")
        val sleepFadeSec = intPreferencesKey("sleep_fade_sec")
        val splitPlayerUpperRatio = floatPreferencesKey("split_player_upper_ratio")
        val playerCoverShape = stringPreferencesKey("player_cover_shape")
        val playerBackdropBlur = floatPreferencesKey("player_backdrop_blur")
        val playerBackdropDim = floatPreferencesKey("player_backdrop_dim")
        val playerArtworkGlow = booleanPreferencesKey("player_artwork_glow")
        val playerSwipeSkip = booleanPreferencesKey("player_swipe_skip")
        val playerSeekbarHeight = stringPreferencesKey("player_seekbar_height")
        val recentSearchesJson = stringPreferencesKey("recent_searches_json")
        val customSoundProfilesJson = stringPreferencesKey("custom_sound_profiles_json")
        val activeSoundProfileId = stringPreferencesKey("active_sound_profile_id")
        // Audio upgrades & Device Routing
        val replayGainMode = stringPreferencesKey("replay_gain_mode")
        val replayGainPreampDb = floatPreferencesKey("replay_gain_preamp_db")
        val crossfadeEqualPower = booleanPreferencesKey("crossfade_equal_power")
        val deviceProfilesJson = stringPreferencesKey("device_profiles_json")
        val autoResumeOnDeviceConnect = booleanPreferencesKey("auto_resume_device_connect")
        val smartAlbumGapless = booleanPreferencesKey("smart_album_gapless")
        val eqChannelBalance = floatPreferencesKey("eq_channel_balance")
        val eqMonoMode = booleanPreferencesKey("eq_mono_mode")
        val eqLimiterEnabled = booleanPreferencesKey("eq_limiter_enabled")
        val drivingKeepScreenOn = booleanPreferencesKey("driving_keep_screen_on")
        val splitMultiArtists = booleanPreferencesKey("split_multi_artists")
        val artistSplitExceptionsJson = stringPreferencesKey("artist_split_exceptions_json")
        val folderViewTreeMode = booleanPreferencesKey("folder_view_tree_mode")
        val deviceDspRulesJson = stringPreferencesKey("device_dsp_rules_json")
    }

    val themeMode: Flow<ThemeMode> = ctx.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[K.theme] ?: "DARK") }.getOrDefault(ThemeMode.DARK)
    }

    val skinId: Flow<String> = ctx.dataStore.data.map { it[K.skin] ?: "studio_amber" }
    val playerThemeId: Flow<String> = ctx.dataStore.data.map { it[K.player] ?: "atmospheric_queue" }
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
        val capped = if (ids.size > 2500) {
            val start = (index - 500).coerceIn(0, (ids.size - 2500).coerceAtLeast(0))
            val end = (start + 2500).coerceAtMost(ids.size)
            val sliced = ids.subList(start, end)
            it[K.queueIndex] = (index - start).coerceIn(0, (sliced.lastIndex).coerceAtLeast(0))
            sliced
        } else {
            it[K.queueIndex] = index
            ids
        }
        it[K.queueIds] = capped.joinToString("\n")
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

    suspend fun removeExcludedFolder(folder: String) {
        ctx.dataStore.edit {
            val cur = it[K.excluded] ?: emptySet()
            it[K.excluded] = cur - folder
        }
    }

    suspend fun clearExcludedFolders() {
        ctx.dataStore.edit { it.remove(K.excluded) }
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
    val crossfadeSeconds: Flow<Int> = ctx.dataStore.data.map { (it[K.crossfadeSecs] ?: 0).coerceIn(0, 12) }
    val homeTabsList: Flow<List<String>> = ctx.dataStore.data.map {
        val raw = it[K.homeTabs]?.split(",")?.map { s -> s.trim() }?.filter { s -> s.isNotBlank() }
        if (!raw.isNullOrEmpty()) raw else listOf("For you", "Songs", "Playlists", "Folders", "Albums", "Artists")
    }
    val fontStyle: Flow<FontStylePreset> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[K.fontStyle]
        FontStylePreset.entries.firstOrNull { it.id == raw } ?: FontStylePreset.MODERN
    }
    val seekStyle: Flow<SeekStylePreset> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[K.seekStyle]
        SeekStylePreset.entries.firstOrNull { it.id == raw } ?: SeekStylePreset.WAVEFORM
    }
    val hiddenPlayerElements: Flow<Set<String>> = ctx.dataStore.data.map {
        it[K.hiddenPlayerElements] ?: emptySet()
    }
    val customButtonOrder: Flow<List<String>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[K.customButtonOrder]?.split(",")?.map { s -> s.trim() }?.filter { s -> s.isNotBlank() }
        if (!raw.isNullOrEmpty()) raw else listOf(
            "Shuffle", "Previous", "Play/Pause", "Next", "Repeat", "Favorite", "Queue", "More"
        )
    }
    val forYouSectionOrder: Flow<List<String>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[K.forYouOrder]?.split(",")?.map { s -> s.trim() }?.filter { s -> s.isNotBlank() }
        if (!raw.isNullOrEmpty()) raw else listOf(
            "resume", "actions", "widgets", "recent", "top_picks"
        )
    }
    val hiddenForYouSections: Flow<Set<String>> = ctx.dataStore.data.map {
        it[K.hiddenForYouSections] ?: emptySet()
    }
    val sleepFadeSec: Flow<Int> = ctx.dataStore.data.map { (it[K.sleepFadeSec] ?: 10).coerceIn(0, 30) }
    val splitPlayerUpperRatio: Flow<Float> = ctx.dataStore.data.map {
        (it[K.splitPlayerUpperRatio] ?: 0.40f).coerceIn(0.35f, 0.72f)
    }
    val playerCoverShape: Flow<String> = ctx.dataStore.data.map {
        it[K.playerCoverShape] ?: "ROUNDED"
    }
    val playerBackdropBlur: Flow<Float> = ctx.dataStore.data.map {
        (it[K.playerBackdropBlur] ?: 0.60f).coerceIn(0f, 1f)
    }
    val playerBackdropDim: Flow<Float> = ctx.dataStore.data.map {
        (it[K.playerBackdropDim] ?: 0.55f).coerceIn(0.10f, 0.90f)
    }
    val playerArtworkGlow: Flow<Boolean> = ctx.dataStore.data.map {
        it[K.playerArtworkGlow] ?: true
    }
    val playerSwipeSkip: Flow<Boolean> = ctx.dataStore.data.map {
        it[K.playerSwipeSkip] ?: true
    }
    val playerSeekbarHeight: Flow<String> = ctx.dataStore.data.map {
        it[K.playerSeekbarHeight] ?: "MEDIUM"
    }

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
    suspend fun setCrossfadeSeconds(sec: Int) = ctx.dataStore.edit { it[K.crossfadeSecs] = sec.coerceIn(0, 12) }
    suspend fun setSleepFadeSec(sec: Int) = ctx.dataStore.edit { it[K.sleepFadeSec] = sec.coerceIn(0, 30) }
    suspend fun setSplitPlayerUpperRatio(ratio: Float) = ctx.dataStore.edit {
        it[K.splitPlayerUpperRatio] = ratio.coerceIn(0.35f, 0.72f)
    }
    suspend fun setPlayerCoverShape(shape: String) = ctx.dataStore.edit { it[K.playerCoverShape] = shape }
    suspend fun setPlayerBackdropBlur(blur: Float) = ctx.dataStore.edit { it[K.playerBackdropBlur] = blur.coerceIn(0f, 1f) }
    suspend fun setPlayerBackdropDim(dim: Float) = ctx.dataStore.edit { it[K.playerBackdropDim] = dim.coerceIn(0.10f, 0.90f) }
    suspend fun setPlayerArtworkGlow(enabled: Boolean) = ctx.dataStore.edit { it[K.playerArtworkGlow] = enabled }
    suspend fun setPlayerSwipeSkip(enabled: Boolean) = ctx.dataStore.edit { it[K.playerSwipeSkip] = enabled }
    suspend fun setPlayerSeekbarHeight(height: String) = ctx.dataStore.edit { it[K.playerSeekbarHeight] = height }
    suspend fun setHomeTabs(tabs: List<String>) = ctx.dataStore.edit { it[K.homeTabs] = tabs.joinToString(",") }
    suspend fun setFontStyle(preset: FontStylePreset) = ctx.dataStore.edit { it[K.fontStyle] = preset.id }
    suspend fun setSeekStyle(preset: SeekStylePreset) = ctx.dataStore.edit { it[K.seekStyle] = preset.id }
    suspend fun setPlayerElementVisible(key: String, visible: Boolean) = ctx.dataStore.edit { prefs ->
        val cur = prefs[K.hiddenPlayerElements] ?: emptySet()
        prefs[K.hiddenPlayerElements] = if (visible) cur - key else cur + key
    }
    suspend fun clearHiddenPlayerElements() = ctx.dataStore.edit { it.remove(K.hiddenPlayerElements) }
    suspend fun setCustomButtonOrder(order: List<String>) = ctx.dataStore.edit {
        it[K.customButtonOrder] = order.joinToString(",")
    }
    suspend fun setForYouSectionOrder(order: List<String>) = ctx.dataStore.edit {
        it[K.forYouOrder] = order.joinToString(",")
    }
    suspend fun setForYouSectionVisible(key: String, visible: Boolean) = ctx.dataStore.edit { prefs ->
        val cur = prefs[K.hiddenForYouSections] ?: emptySet()
        prefs[K.hiddenForYouSections] = if (visible) cur - key else cur + key
    }
    suspend fun resetForYouLayout() = ctx.dataStore.edit {
        it.remove(K.forYouOrder)
        it.remove(K.hiddenForYouSections)
    }

    val recentSearches: Flow<List<String>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[K.recentSearchesJson] ?: return@map emptyList()
        runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i).trim()
                    if (s.isNotBlank()) add(s)
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        ctx.dataStore.edit { prefs ->
            val current = runCatching {
                val arr = JSONArray(prefs[K.recentSearchesJson] ?: "[]")
                buildList {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i).trim()
                        if (s.isNotBlank()) add(s)
                    }
                }
            }.getOrDefault(emptyList())
            val next = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
            prefs[K.recentSearchesJson] = JSONArray(next).toString()
        }
    }

    suspend fun removeRecentSearch(query: String) {
        ctx.dataStore.edit { prefs ->
            val current = runCatching {
                val arr = JSONArray(prefs[K.recentSearchesJson] ?: "[]")
                buildList {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i).trim()
                        if (s.isNotBlank() && !s.equals(query, ignoreCase = true)) add(s)
                    }
                }
            }.getOrDefault(emptyList())
            prefs[K.recentSearchesJson] = JSONArray(current).toString()
        }
    }

    suspend fun clearRecentSearches() = ctx.dataStore.edit { it.remove(K.recentSearchesJson) }

    val customSoundProfiles: Flow<List<SoundProfile>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[K.customSoundProfilesJson] ?: return@map emptyList()
        runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val bandsArr = o.optJSONArray("bands")
                    val bands = buildList {
                        if (bandsArr != null) {
                            for (b in 0 until bandsArr.length()) {
                                add(bandsArr.optInt(b, 0))
                            }
                        }
                    }
                    add(
                        SoundProfile(
                            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = o.optString("name").ifBlank { "Custom Profile" },
                            bandLevels = bands,
                            bassStrength = o.optInt("bass", 0).coerceIn(0, 1000),
                            virtualizerStrength = o.optInt("virt", 0).coerceIn(0, 1000),
                            reverbIndex = o.optInt("reverb", 0).coerceIn(0, 6),
                            speed = o.optDouble("speed", 1.0).toFloat().coerceIn(0.5f, 2.0f),
                            pitchSemitones = o.optDouble("pitch", 0.0).toFloat().coerceIn(-6f, 6f),
                            isBuiltIn = false,
                            limiterEnabled = o.optBoolean("limiter", true),
                            channelBalance = o.optDouble("balance", 0.0).toFloat().coerceIn(-1f, 1f),
                            monoMode = o.optBoolean("mono", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    val activeSoundProfileId: Flow<String> = ctx.dataStore.data.map {
        it[K.activeSoundProfileId] ?: "builtin_flat"
    }

    suspend fun setActiveSoundProfileId(id: String) = ctx.dataStore.edit {
        it[K.activeSoundProfileId] = id
    }

    suspend fun saveCustomSoundProfile(profile: SoundProfile) {
        ctx.dataStore.edit { prefs ->
            val current = runCatching {
                val arr = JSONArray(prefs[K.customSoundProfilesJson] ?: "[]")
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            }.getOrDefault(emptyList())
            val obj = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("bands", JSONArray(profile.bandLevels))
                put("bass", profile.bassStrength)
                put("virt", profile.virtualizerStrength)
                put("reverb", profile.reverbIndex)
                put("speed", profile.speed.toDouble())
                put("pitch", profile.pitchSemitones.toDouble())
                put("limiter", profile.limiterEnabled)
                put("balance", profile.channelBalance.toDouble())
                put("mono", profile.monoMode)
            }
            val next = JSONArray().apply {
                put(obj)
                current.filterNot { it.optString("id") == profile.id }.take(11).forEach { put(it) }
            }
            prefs[K.customSoundProfilesJson] = next.toString()
            prefs[K.activeSoundProfileId] = profile.id
        }
    }

    suspend fun deleteCustomSoundProfile(id: String) {
        ctx.dataStore.edit { prefs ->
            val current = runCatching {
                val arr = JSONArray(prefs[K.customSoundProfilesJson] ?: "[]")
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            }.getOrDefault(emptyList())
            val next = JSONArray().apply {
                current.filterNot { it.optString("id") == id }.forEach { put(it) }
            }
            prefs[K.customSoundProfilesJson] = next.toString()
            if (prefs[K.activeSoundProfileId] == id) {
                prefs[K.activeSoundProfileId] = "builtin_flat"
            }
        }
    }

    val replayGainMode: Flow<ReplayGainMode> = ctx.dataStore.data.map { prefs ->
        runCatching { ReplayGainMode.valueOf(prefs[K.replayGainMode] ?: "OFF") }
            .getOrDefault(ReplayGainMode.OFF)
    }
    val replayGainPreampDb: Flow<Float> = ctx.dataStore.data.map { prefs ->
        (prefs[K.replayGainPreampDb] ?: 0f).coerceIn(-6f, 6f)
    }
    val crossfadeEqualPower: Flow<Boolean> = ctx.dataStore.data.map { prefs ->
        prefs[K.crossfadeEqualPower] ?: true
    }
    val autoResumeOnDeviceConnect: Flow<Boolean> = ctx.dataStore.data.map { prefs ->
        prefs[K.autoResumeOnDeviceConnect] ?: false
    }
    val smartAlbumGapless: Flow<Boolean> = ctx.dataStore.data.map { prefs ->
        prefs[K.smartAlbumGapless] ?: true
    }
    val eqChannelBalance: Flow<Float> = ctx.dataStore.data.map { prefs ->
        (prefs[K.eqChannelBalance] ?: 0f).coerceIn(-1f, 1f)
    }
    val eqMonoMode: Flow<Boolean> = ctx.dataStore.data.map { prefs ->
        prefs[K.eqMonoMode] ?: false
    }
    val eqLimiterEnabled: Flow<Boolean> = ctx.dataStore.data.map { prefs ->
        prefs[K.eqLimiterEnabled] ?: true
    }
    val drivingKeepScreenOn: Flow<Boolean> = ctx.dataStore.data.map { prefs ->
        prefs[K.drivingKeepScreenOn] ?: true
    }
    val deviceProfileMap: Flow<Map<String, String>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[K.deviceProfilesJson] ?: return@map emptyMap()
        runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.optString(k)
                    if (k.isNotBlank() && v.isNotBlank()) put(k, v)
                }
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun setReplayGainMode(mode: ReplayGainMode) = ctx.dataStore.edit {
        it[K.replayGainMode] = mode.name
    }
    suspend fun setReplayGainPreampDb(db: Float) = ctx.dataStore.edit {
        it[K.replayGainPreampDb] = db.coerceIn(-6f, 6f)
    }
    suspend fun setCrossfadeEqualPower(enabled: Boolean) = ctx.dataStore.edit {
        it[K.crossfadeEqualPower] = enabled
    }
    suspend fun setAutoResumeOnDeviceConnect(enabled: Boolean) = ctx.dataStore.edit {
        it[K.autoResumeOnDeviceConnect] = enabled
    }
    suspend fun setSmartAlbumGapless(enabled: Boolean) = ctx.dataStore.edit {
        it[K.smartAlbumGapless] = enabled
    }
    suspend fun setEqChannelBalance(balance: Float) = ctx.dataStore.edit {
        it[K.eqChannelBalance] = balance.coerceIn(-1f, 1f)
    }
    suspend fun setEqMonoMode(enabled: Boolean) = ctx.dataStore.edit {
        it[K.eqMonoMode] = enabled
    }
    suspend fun setEqLimiterEnabled(enabled: Boolean) = ctx.dataStore.edit {
        it[K.eqLimiterEnabled] = enabled
    }
    suspend fun setDrivingKeepScreenOn(enabled: Boolean) = ctx.dataStore.edit {
        it[K.drivingKeepScreenOn] = enabled
    }
    suspend fun setDeviceProfile(deviceKey: String, profileId: String?) {
        val cleanKey = deviceKey.trim()
        if (cleanKey.isBlank()) return
        ctx.dataStore.edit { prefs ->
            val obj = runCatching {
                JSONObject(prefs[K.deviceProfilesJson] ?: "{}")
            }.getOrElse { JSONObject() }
            if (profileId.isNullOrBlank()) {
                obj.remove(cleanKey)
            } else {
                obj.put(cleanKey, profileId)
            }
            prefs[K.deviceProfilesJson] = obj.toString()

            val rulesObj = runCatching {
                JSONObject(prefs[K.deviceDspRulesJson] ?: "{}")
            }.getOrElse { JSONObject() }
            val existingRule = rulesObj.optJSONObject(cleanKey) ?: JSONObject()
            if (profileId.isNullOrBlank()) {
                existingRule.remove("profileId")
            } else {
                existingRule.put("profileId", profileId)
            }
            if (existingRule.length() == 0) rulesObj.remove(cleanKey) else rulesObj.put(cleanKey, existingRule)
            prefs[K.deviceDspRulesJson] = rulesObj.toString()
        }
    }

    val splitMultiArtists: Flow<Boolean> = ctx.dataStore.data.map { prefs ->
        prefs[K.splitMultiArtists] ?: true
    }

    suspend fun setSplitMultiArtists(enabled: Boolean) = ctx.dataStore.edit {
        it[K.splitMultiArtists] = enabled
    }

    val artistSplitExceptions: Flow<Set<String>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[K.artistSplitExceptionsJson] ?: return@map emptySet()
        runCatching {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i).trim().lowercase()
                    if (v.isNotBlank()) add(v)
                }
            }
        }.getOrDefault(emptySet())
    }

    suspend fun setArtistSplitExceptions(exceptions: Set<String>) {
        val normalized = exceptions.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        ctx.dataStore.edit { prefs ->
            prefs[K.artistSplitExceptionsJson] = JSONArray(normalized.toList()).toString()
        }
    }

    suspend fun addArtistSplitException(name: String) {
        val clean = name.trim().lowercase()
        if (clean.isBlank()) return
        ctx.dataStore.edit { prefs ->
            val arr = runCatching { JSONArray(prefs[K.artistSplitExceptionsJson] ?: "[]") }.getOrElse { JSONArray() }
            val current = buildSet {
                for (i in 0 until arr.length()) add(arr.optString(i).trim().lowercase())
            }
            prefs[K.artistSplitExceptionsJson] = JSONArray((current + clean).filter { it.isNotBlank() }).toString()
        }
    }

    suspend fun removeArtistSplitException(name: String) {
        val clean = name.trim().lowercase()
        ctx.dataStore.edit { prefs ->
            val arr = runCatching { JSONArray(prefs[K.artistSplitExceptionsJson] ?: "[]") }.getOrElse { JSONArray() }
            val current = buildSet {
                for (i in 0 until arr.length()) add(arr.optString(i).trim().lowercase())
            }
            prefs[K.artistSplitExceptionsJson] = JSONArray((current - clean).filter { it.isNotBlank() }).toString()
        }
    }

    val folderViewTreeMode: Flow<Boolean> = ctx.dataStore.data.map { prefs ->
        prefs[K.folderViewTreeMode] ?: false
    }

    suspend fun setFolderViewTreeMode(enabled: Boolean) = ctx.dataStore.edit {
        it[K.folderViewTreeMode] = enabled
    }

    val deviceDspRules: Flow<Map<String, DeviceDspRule>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[K.deviceDspRulesJson] ?: return@map emptyMap()
        runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val r = obj.optJSONObject(k) ?: continue
                    val rgName = r.optString("replayGainMode", "")
                    val rgMode = if (rgName.isNotBlank()) {
                        runCatching { ReplayGainMode.valueOf(rgName) }.getOrNull()
                    } else null
                    put(
                        k,
                        DeviceDspRule(
                            deviceKey = k,
                            profileId = r.optString("profileId", "").takeIf { it.isNotBlank() },
                            crossfadeSecOverride = if (r.has("crossfadeSec")) r.optInt("crossfadeSec").coerceIn(0, 12) else null,
                            replayGainModeOverride = rgMode,
                            monoModeOverride = if (r.has("monoMode")) r.optBoolean("monoMode") else null,
                            channelBalanceOverride = if (r.has("channelBalance")) r.optDouble("channelBalance").toFloat().coerceIn(-1f, 1f) else null
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun setDeviceDspRule(rule: DeviceDspRule) {
        val cleanKey = rule.deviceKey.trim()
        if (cleanKey.isBlank()) return
        ctx.dataStore.edit { prefs ->
            val rulesObj = runCatching {
                JSONObject(prefs[K.deviceDspRulesJson] ?: "{}")
            }.getOrElse { JSONObject() }
            val r = JSONObject().apply {
                rule.profileId?.takeIf { it.isNotBlank() }?.let { put("profileId", it) }
                rule.crossfadeSecOverride?.let { put("crossfadeSec", it.coerceIn(0, 12)) }
                rule.replayGainModeOverride?.let { put("replayGainMode", it.name) }
                rule.monoModeOverride?.let { put("monoMode", it) }
                rule.channelBalanceOverride?.let { put("channelBalance", it.coerceIn(-1f, 1f).toDouble()) }
            }
            if (r.length() == 0) rulesObj.remove(cleanKey) else rulesObj.put(cleanKey, r)
            prefs[K.deviceDspRulesJson] = rulesObj.toString()

            val legacyObj = runCatching {
                JSONObject(prefs[K.deviceProfilesJson] ?: "{}")
            }.getOrElse { JSONObject() }
            if (rule.profileId.isNullOrBlank()) legacyObj.remove(cleanKey) else legacyObj.put(cleanKey, rule.profileId)
            prefs[K.deviceProfilesJson] = legacyObj.toString()
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
