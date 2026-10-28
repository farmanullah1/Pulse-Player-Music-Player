# 03 — Project Setup, Room Database, MediaStore Scan, Artwork

> Blueprint code. Keep package `com.pulse.player` (or the existing package). Fix imports/versions to match the installed Android Studio. Use the **latest stable** versions of each library; the ones below are minimum-known-good.

## 1. Gradle (app/build.gradle.kts essentials)
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")   // Kotlin 2.x
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.pulse.player"
    compileSdk = 35
    defaultConfig { applicationId = "com.pulse.player"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0.0" }
    buildFeatures { compose = true }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    val media3 = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room"); implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")
}
```
No Coil, Hilt or Retrofit: fewer moving parts for a smaller model. Dependencies are created by a tiny manual graph (`PulseApp`).

## 2. AndroidManifest.xml
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.WRITE_SETTINGS" /> <!-- ringtone only -->
    <!-- NO INTERNET PERMISSION -->
    <application
        android:name=".PulseApp" android:label="Pulse Player"
        android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.Pulse" android:supportsRtl="true">
        <activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTask"
            android:windowSoftInputMode="adjustResize"
            android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden">
            <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
        </activity>
        <service android:name=".playback.MusicPlaybackService" android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter><action android:name="androidx.media3.session.MediaSessionService"/></intent-filter>
        </service>
        <receiver android:name=".ui.widget.PlayerWidgetReceiver" android:exported="true">
            <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/player_widget_info"/>
        </receiver>
    </application>
</manifest>
```
`res/values/themes.xml`: `<style name="Theme.Pulse" parent="android:Theme.Material.NoActionBar"><item name="android:windowBackground">#000000</item><item name="android:statusBarColor">@android:color/transparent</item><item name="android:navigationBarColor">@android:color/transparent</item></style>`

## 3. Room entities
```kotlin
package com.pulse.player.data.db
import androidx.room.*

@Entity(tableName = "tracks", indices = [Index("title"), Index("artist"), Index("album"), Index("folderPath"), Index("dateAddedMs")])
data class TrackEntity(
    @PrimaryKey val id: String,              // "ms:<mediaStoreId>" or "saf:<hash>"
    val uri: String, val displayName: String,
    val title: String, val artist: String, val album: String, val albumArtist: String?,
    val durationMs: Long, val sizeBytes: Long, val mimeType: String?,
    val folderPath: String, val dateAddedMs: Long, val dateModifiedMs: Long,
    val trackNumber: Int, val year: Int,
    val normTitle: String, val normArtist: String, val normAlbum: String,
    val available: Boolean = true
)
@Entity(tableName = "favorites") data class FavoriteEntity(@PrimaryKey val trackId: String, val addedAtMs: Long)
@Entity(tableName = "playlists") data class PlaylistEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val createdAtMs: Long)
@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"], indices = [Index("trackId")])
data class PlaylistTrackEntity(val playlistId: Long, val trackId: String, val position: Int, val addedAtMs: Long)
@Entity(tableName = "play_events", indices = [Index("playedAtMs"), Index("trackId")])
data class PlayEventEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val trackId: String, val playedAtMs: Long, val listenedMs: Long, val completed: Boolean)

data class TrackWithFav(@Embedded val track: TrackEntity, val isFavorite: Boolean)
data class TrackStat(val trackId: String, val plays: Int, val ms: Long)
data class ArtistStat(val artist: String, val plays: Int, val ms: Long)
```

## 4. DAOs
```kotlin
@Dao interface TrackDao {
    @Query("SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM tracks t LEFT JOIN favorites f ON f.trackId = t.id WHERE t.available = 1 AND t.durationMs >= :minMs ORDER BY t.title COLLATE NOCASE")
    fun observeAll(minMs: Long): Flow<List<TrackWithFav>>
    @Query("SELECT * FROM tracks WHERE id IN (:ids)") suspend fun byIds(ids: List<String>): List<TrackEntity>
    @Query("SELECT * FROM tracks WHERE id = :id") suspend fun byId(id: String): TrackEntity?
    @Query("SELECT * FROM tracks") suspend fun allOnce(): List<TrackEntity>
    @Query("SELECT COUNT(*) FROM tracks WHERE available = 1") fun observeCount(): Flow<Int>
    @Upsert suspend fun upsertAll(items: List<TrackEntity>)
    @Query("UPDATE tracks SET available = 0 WHERE id LIKE 'ms:%'") suspend fun markMediaStoreUnavailable()
    @Query("UPDATE tracks SET available = 0 WHERE id LIKE 'saf:%'") suspend fun markSafUnavailable()
}
@Dao interface FavoriteDao {
    @Query("SELECT trackId FROM favorites") fun observeIds(): Flow<List<String>>
    @Query("SELECT t.*, 1 AS isFavorite FROM favorites f JOIN tracks t ON t.id = f.trackId ORDER BY f.addedAtMs DESC") fun observeTracks(): Flow<List<TrackWithFav>>
    @Query("SELECT t.* FROM favorites f JOIN tracks t ON t.id = f.trackId ORDER BY f.addedAtMs DESC") suspend fun allTracksOnce(): List<TrackEntity>
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :id)") suspend fun isFavorite(id: String): Boolean
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun add(f: FavoriteEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addAll(f: List<FavoriteEntity>)
    @Query("DELETE FROM favorites WHERE trackId = :id") suspend fun remove(id: String)
    @Query("DELETE FROM favorites") suspend fun clear()
    @Transaction suspend fun toggle(id: String, now: Long) { if (isFavorite(id)) remove(id) else add(FavoriteEntity(id, now)) }
}
@Dao interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAtMs") fun observePlaylists(): Flow<List<PlaylistEntity>>
    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :id") fun observeCount(id: Long): Flow<Int>
    @Query("SELECT t.*, (f.trackId IS NOT NULL) AS isFavorite FROM playlist_tracks p JOIN tracks t ON t.id = p.trackId LEFT JOIN favorites f ON f.trackId = t.id WHERE p.playlistId = :id ORDER BY p.position")
    fun observeTracks(id: Long): Flow<List<TrackWithFav>>
    @Insert suspend fun create(p: PlaylistEntity): Long
    @Query("UPDATE playlists SET name = :name WHERE id = :id") suspend fun rename(id: Long, name: String)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: Long)
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id") suspend fun clearTracks(id: Long)
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :id") suspend fun nextPos(id: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addTrack(x: PlaylistTrackEntity)
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id AND trackId = :trackId") suspend fun removeTrack(id: Long, trackId: String)
    @Transaction suspend fun delete(id: Long) { clearTracks(id); deletePlaylist(id) }
}
@Dao interface StatsDao {
    @Insert suspend fun insert(e: PlayEventEntity)
    @Query("SELECT COALESCE(SUM(listenedMs),0) FROM play_events WHERE playedAtMs BETWEEN :from AND :to") suspend fun totalMs(from: Long, to: Long): Long
    @Query("SELECT trackId, COUNT(*) AS plays, SUM(listenedMs) AS ms FROM play_events WHERE playedAtMs BETWEEN :from AND :to GROUP BY trackId ORDER BY plays DESC LIMIT :limit")
    suspend fun topTracks(from: Long, to: Long, limit: Int): List<TrackStat>
    @Query("SELECT t.artist AS artist, COUNT(*) AS plays, SUM(e.listenedMs) AS ms FROM play_events e JOIN tracks t ON t.id = e.trackId WHERE e.playedAtMs BETWEEN :from AND :to GROUP BY t.artist ORDER BY ms DESC LIMIT :limit")
    suspend fun topArtists(from: Long, to: Long, limit: Int): List<ArtistStat>
    @Query("SELECT trackId FROM play_events GROUP BY trackId ORDER BY MAX(playedAtMs) DESC LIMIT :limit") fun observeRecentIds(limit: Int): Flow<List<String>>
    @Query("SELECT trackId FROM play_events GROUP BY trackId ORDER BY COUNT(*) DESC LIMIT :limit") fun observeTopIds(limit: Int): Flow<List<String>>
}
@Database(entities = [TrackEntity::class, FavoriteEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class, PlayEventEntity::class], version = 1, exportSchema = true)
abstract class PulseDb : RoomDatabase() {
    abstract fun tracks(): TrackDao; abstract fun favorites(): FavoriteDao
    abstract fun playlists(): PlaylistDao; abstract fun stats(): StatsDao
}
```
Never use `fallbackToDestructiveMigration()`. Add real migrations when the version changes.

## 5. App graph (manual DI)
```kotlin
class PulseApp : Application() {
    lateinit var db: PulseDb; lateinit var settings: SettingsStore
    lateinit var scanner: LibraryScanner; lateinit var player: PlayerController
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, PulseDb::class.java, "pulse.db").build()
        settings = SettingsStore(this)
        scanner = LibraryScanner(this, db)
        player = PlayerController(this, db)
    }
}
val Context.app get() = applicationContext as PulseApp
```

## 6. Normalization helper
```kotlin
import java.text.Normalizer
fun norm(s: String?): String =
    Normalizer.normalize((s ?: "").trim(), Normalizer.Form.NFKC).lowercase().replace(Regex("\\s+"), " ")
```

## 7. Library scanner (MediaStore)
```kotlin
class LibraryScanner(private val ctx: Context, private val db: PulseDb) {
    private val _scanning = MutableStateFlow(false); val scanning = _scanning.asStateFlow()
    private val _progress = MutableStateFlow(0); val progress = _progress.asStateFlow()

    suspend fun scanMediaStore(minDurationMs: Long, excluded: Set<String>): Int = withContext(Dispatchers.IO) {
        if (_scanning.value) return@withContext 0
        _scanning.value = true; _progress.value = 0
        try {
            val col = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                      else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val p = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.RELATIVE_PATH)   // API 29+; on 26-28 the column is missing → handled below
            val sel = "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.SIZE} > 0 AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND ${MediaStore.Audio.Media.IS_ALARM} = 0"
            val batch = ArrayList<TrackEntity>(500); var count = 0
            val projection = if (Build.VERSION.SDK_INT >= 29) p else p.dropLast(1).toTypedArray()
            ctx.contentResolver.query(col, projection, sel, arrayOf(minDurationMs.toString()), null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iT = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iAr = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAl = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iD = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iS = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val iM = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val iN = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val iDa = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iDm = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val iTr = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val iY = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val iR = c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                db.tracks().markMediaStoreUnavailable()
                while (c.moveToNext()) {
                    val folder = if (iR >= 0) c.getString(iR).orEmpty().trimEnd('/') else "Music"
                    if (excluded.any { folder.startsWith(it, ignoreCase = true) }) continue
                    val id = c.getLong(iId)
                    val title = c.getString(iT).orEmpty().ifBlank { c.getString(iN).orEmpty().substringBeforeLast('.') }
                    val artist = c.getString(iAr).orEmpty().let { if (it == "<unknown>" || it.isBlank()) "Unknown artist" else it }
                    val album = c.getString(iAl).orEmpty().ifBlank { "Unknown album" }
                    batch += TrackEntity(
                        id = "ms:$id", uri = ContentUris.withAppendedId(col, id).toString(), displayName = c.getString(iN).orEmpty(),
                        title = title, artist = artist, album = album, albumArtist = null,
                        durationMs = c.getLong(iD), sizeBytes = c.getLong(iS), mimeType = c.getString(iM), folderPath = folder,
                        dateAddedMs = c.getLong(iDa) * 1000, dateModifiedMs = c.getLong(iDm) * 1000,
                        trackNumber = c.getInt(iTr) % 1000, year = c.getInt(iY),
                        normTitle = norm(title), normArtist = norm(artist), normAlbum = norm(album))
                    count++
                    if (batch.size >= 500) { db.tracks().upsertAll(batch.toList()); batch.clear(); _progress.value = count }
                }
                if (batch.isNotEmpty()) db.tracks().upsertAll(batch.toList())
                _progress.value = count
            }
            count
        } finally { _scanning.value = false }
    }
}
```
Notes for the model: `IS_ALARM` etc. are valid MediaStore columns. `markMediaStoreUnavailable()` runs before upserts so deleted files become unavailable while favorites stay. If `query` returns null, report an error state (permission likely missing) and do not mark everything unavailable (guard: only call `markMediaStoreUnavailable()` after a non-null cursor, as above).

## 8. Permissions helper (Compose)
```kotlin
fun audioPermission() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable fun rememberAudioPermissionLauncher(onResult: (Boolean) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
```
Flow: user taps **Scan** → show rationale text → request → on grant call `scanMediaStore()` → on deny show "Open settings" button (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`). Request `POST_NOTIFICATIONS` (API 33+) the first time playback starts.

## 9. Artwork loader (no library needed, cached, off main thread)
```kotlin
object ArtworkLoader {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) { override fun sizeOf(k: String, v: Bitmap) = v.byteCount }
    suspend fun load(ctx: Context, uri: String, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri@$px"; cache.get(key)?.let { return@withContext it }
        val bmp = runCatching {
            if (Build.VERSION.SDK_INT >= 29) ctx.contentResolver.loadThumbnail(Uri.parse(uri), Size(px, px), null)
            else MediaMetadataRetriever().use { r ->
                r.setDataSource(ctx, Uri.parse(uri))
                r.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }
        }.getOrNull()
        if (bmp != null) cache.put(key, bmp); bmp
    }
    fun dominant(bmp: Bitmap): Int { val p = Palette.from(bmp).maximumColorCount(12).generate()
        return p.getDarkMutedColor(p.getMutedColor(p.getDominantColor(0xFF2A0826.toInt()))) }
}
```
(`MediaMetadataRetriever.use` requires API 29 `AutoCloseable`; for API 26–28 wrap with `try/finally { r.release() }`.)

## 9b. Acceptance for this part
App launches, asks permission only on Scan, real songs appear (log the count), favorites table exists, rescan keeps favorites, nothing on the main thread, `brain.md` updated.
