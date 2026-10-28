# 08 — Widgets (Glance), Monthly Recap, README, Icon & Favicon

## 1. Home-screen widget (one responsive widget = 2x2, 4x1, 4x2)

### 1.1 State bridge (service writes, widget reads)
```kotlin
object WidgetStateStore {
    private const val PREFS = "widget_state"
    fun write(ctx: Context, p: Player, scope: CoroutineScope) {
        val md = p.mediaMetadata
        val artPath = File(ctx.cacheDir, "widget_art.png")
        scope.launch(Dispatchers.IO) {
            val uri = p.currentMediaItem?.localConfiguration?.uri?.toString()
            if (uri != null) ArtworkLoader.load(ctx, uri, 160)?.let { b -> artPath.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 90, it) } } else artPath.delete()
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", md.title?.toString()).putString("artist", md.artist?.toString())
                .putBoolean("playing", p.isPlaying).putBoolean("has", p.mediaItemCount > 0)
                .putLong("pos", p.currentPosition).putLong("dur", p.duration.coerceAtLeast(0)).apply()
            PlayerWidget().updateAll(ctx)
        }
    }
    fun read(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
```
Throttle: the service listener already fires only on player events (not every frame). Do not update the widget on position ticks.

### 1.2 Widget
```kotlin
class PlayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 200.dp)))
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sp = WidgetStateStore.read(context)
        val art = File(context.cacheDir, "widget_art.png").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        provideContent {
            val size = LocalSize.current
            val has = sp.getBoolean("has", false); val playing = sp.getBoolean("playing", false)
            val title = sp.getString("title", null) ?: "Nothing playing"; val artist = sp.getString("artist", null).orEmpty()
            val bg = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF14101F))).cornerRadius(24.dp).padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
            when {
                size.width < 200.dp -> Column(bg, horizontalAlignment = Alignment.CenterHorizontally) {         // 2x2
                    Art(art, 64.dp); Spacer(GlanceModifier.height(6.dp)); Row { Ctl((if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play), PlayPauseCb::class); Ctl(R.drawable.ic_w_next, NextCb::class) } }
                size.height < 150.dp -> Row(bg, verticalAlignment = Alignment.CenterVertically) {               // 4x1
                    Art(art, 56.dp); Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) { Title(title); Sub(artist) }
                    Ctl(R.drawable.ic_w_prev, PrevCb::class); Ctl((if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play), PlayPauseCb::class); Ctl(R.drawable.ic_w_next, NextCb::class) }
                else -> Column(bg) {                                                                             // 4x2
                    Row(verticalAlignment = Alignment.CenterVertically) { Art(art, 84.dp); Spacer(GlanceModifier.width(12.dp)); Column { Title(title); Sub(artist) } }
                    Spacer(GlanceModifier.defaultWeight())
                    Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Ctl(R.drawable.ic_w_prev, PrevCb::class); Ctl((if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play), PlayPauseCb::class); Ctl(R.drawable.ic_w_next, NextCb::class) } }
            }
        }
    }
}
```
Helpers (write them so they compile with Glance 1.1): `Art(bitmap?, size)` → `Image(ImageProvider(bitmap))` with `cornerRadius(12.dp)` or a gradient fallback `ic_w_note`; `Title/Sub` → `Text` with `TextStyle(color = ColorProvider(Color.White), fontWeight = Bold, fontSize = 14.sp)` and maxLines 1; `Ctl(iconRes, callback)` → `Image(provider = ImageProvider(iconRes), contentDescription = ..., modifier = GlanceModifier.size(40.dp).padding(6.dp).clickable(actionRunCallback(callback.java)))`. Replace `(if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play)` with `if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play`.
Create vector drawables via Android Studio: `ic_w_play`, `ic_w_pause`, `ic_w_next`, `ic_w_prev`, `ic_w_note` (white, 24dp). When `has == false` show only the title "Nothing playing" and a "Open app" tap — truthful empty state.

### 1.3 Callbacks and receiver
```kotlin
object WidgetControls {
    suspend fun send(ctx: Context, block: (MediaController) -> Unit) = withContext(Dispatchers.Main) {
        val token = SessionToken(ctx, ComponentName(ctx, MusicPlaybackService::class.java))
        val c = MediaController.Builder(ctx, token).buildAsync().await()
        block(c); delay(300); c.release()
    }
}
class PlayPauseCb : ActionCallback { override suspend fun onAction(c: Context, g: GlanceId, p: ActionParameters) { WidgetControls.send(c) { if (it.isPlaying) it.pause() else it.play() } } }
class NextCb : ActionCallback { override suspend fun onAction(c: Context, g: GlanceId, p: ActionParameters) { WidgetControls.send(c) { it.seekToNextMediaItem() } } }
class PrevCb : ActionCallback { override suspend fun onAction(c: Context, g: GlanceId, p: ActionParameters) { WidgetControls.send(c) { it.seekToPreviousMediaItem() } } }
class PlayerWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = PlayerWidget() }
```
`res/xml/player_widget_info.xml`:
```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp" android:minHeight="110dp" android:minResizeWidth="110dp" android:minResizeHeight="40dp"
    android:targetCellWidth="4" android:targetCellHeight="2" android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="0" android:widgetCategory="home_screen" android:previewLayout="@layout/widget_preview" />
```
(Provide a simple `widget_preview.xml` or drop `previewLayout`.) Sizes are launcher-dependent: say "target configuration", not a guarantee. Settings has a "Widgets" card explaining: long-press home screen → Widgets → Pulse Player.

## 2. Monthly recap (real data only)
```kotlin
data class Recap(val totalMs: Long, val topTracks: List<Pair<TrackEntity, Int>>, val topArtists: List<ArtistStat>, val badges: List<String>)

suspend fun buildRecap(db: PulseDb, month: YearMonth): Recap? {
    val zone = ZoneId.systemDefault()
    val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    val total = db.stats().totalMs(from, to); if (total <= 0) return null
    val top = db.stats().topTracks(from, to, 5); val tracks = db.tracks().byIds(top.map { it.trackId }).associateBy { it.id }
    val badges = buildList {
        if (total >= 100 * 3_600_000L) add("100 Hours Club")
        if (db.stats().nightMs(from, to) >= total * 0.4) add("Night Owl Listener")     // add DAO: SUM over events where hour(playedAtMs) in 0..4 or >=22
        if (top.firstOrNull()?.plays ?: 0 >= 30) add("On Repeat") }
    return Recap(total, top.mapNotNull { t -> tracks[t.trackId]?.let { it to t.plays } }, db.stats().topArtists(from, to, 5), badges)
}
```
Night-owl DAO sketch: `SELECT COALESCE(SUM(listenedMs),0) FROM play_events WHERE playedAtMs BETWEEN :from AND :to AND (CAST(strftime('%H', playedAtMs/1000, 'unixepoch', 'localtime') AS INTEGER) >= 22 OR CAST(strftime('%H', playedAtMs/1000, 'unixepoch', 'localtime') AS INTEGER) < 5)`.

**Recap screen:** month switcher (< Oct 2026 >), big total ("12 h 40 min"), Top 5 songs with art and play counts, Top 5 artists, badges as gradient chips, **Share** button: render a 1080x1350 bitmap of the card with `Canvas`/`ImageBitmap` (gradient + text), save to `cacheDir/recap.png`, share via `FileProvider` + `ACTION_SEND` (`image/png`). If no events for the month: empty state "Listen to some music and your recap will appear here."
FileProvider needs `<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.files" android:exported="false" android:grantUriPermissions="true">` with `res/xml/file_paths.xml` (`<cache-path name="cache" path="."/>`).

## 3. App icon and favicon
- **Android adaptive icon** (required): `mipmap-anydpi-v26/ic_launcher.xml` with `ic_launcher_foreground` (vector) and `ic_launcher_background` (vector gradient) + monochrome layer for themed icons. Design: rounded dark square, a stylized sound-wave of 5 vertical rounded bars in a violet→cyan→pink gradient. Keep within the 66dp safe zone.
- **Foreground vector** (`res/drawable/ic_launcher_foreground.xml`, 108dp):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#9B7BFF" android:pathData="M34,48 h6 a3,3 0 0 1 3,3 v6 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-6 a3,3 0 0 1 3,-3z"/>
    <path android:fillColor="#4DD9F5" android:pathData="M44,38 h6 a3,3 0 0 1 3,3 v26 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-26 a3,3 0 0 1 3,-3z"/>
    <path android:fillColor="#FF6DAA" android:pathData="M54,30 h6 a3,3 0 0 1 3,3 v42 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-42 a3,3 0 0 1 3,-3z"/>
    <path android:fillColor="#4DD9F5" android:pathData="M64,38 h6 a3,3 0 0 1 3,3 v26 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-26 a3,3 0 0 1 3,-3z"/>
    <path android:fillColor="#9B7BFF" android:pathData="M74,48 h6 a3,3 0 0 1 3,3 v6 a3,3 0 0 1 -3,3 h-6 a3,3 0 0 1 -3,-3 v-6 a3,3 0 0 1 3,-3z"/>
</vector>
```
Background: `#0B0816` solid (or a vector gradient `#2A0826`→`#0B0816`). Monochrome layer: same shapes in one color.
- **`favicon.svg`** (for README/GitHub Pages; not a replacement for launcher icons):
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#9B7BFF"/><stop offset="0.5" stop-color="#4DD9F5"/><stop offset="1" stop-color="#FF6DAA"/></linearGradient></defs>
  <rect width="64" height="64" rx="14" fill="#0B0816"/>
  <g fill="url(#g)"><rect x="12" y="26" width="6" height="12" rx="3"/><rect x="21" y="18" width="6" height="28" rx="3"/><rect x="30" y="10" width="6" height="44" rx="3"/><rect x="39" y="18" width="6" height="28" rx="3"/><rect x="48" y="26" width="6" height="12" rx="3"/></g>
</svg>
```
Also generate `docs/assets/banner.png` placeholder only if the model can create a real image; otherwise omit it from the README.

## 4. README.md template (fill with verified facts only)
````markdown
<p align="center"><img src="docs/assets/favicon.svg" width="96" alt="Pulse Player icon"></p>

# Pulse Player

A fast, colorful, 100% offline music player for Android. Built with Kotlin, Jetpack Compose, Media3, Room and Glance.

## Highlights
- Plays local audio (formats supported by your device), background playback, notification and lock-screen controls
- Tabs: For you, Songs, Playlists, Folders, Albums, Artists; global search
- 8 player themes with a swipeable preview carousel; skin themes and custom wallpapers
- Equalizer, bass boost, virtualizer, reverb (only where the device supports them)
- Sleep timer with fade-out, playback speed, queue management
- Favourites and playlists stored on-device
- **Favourites export/import (JSON)** for moving to a new phone or tablet
- Home-screen widget, monthly recap with shareable card
- Phone and tablet layouts; reduced-motion mode
- No ads, no accounts, **no internet permission**

## Move to a new phone or tablet
1. Settings → Backup & Transfer → Export favourites
2. Copy your music and the JSON file to the new device (into a `Music` folder)
3. Open the app → Scan (or Choose music folder)
4. Settings → Backup & Transfer → Import favourites → review → Apply
The JSON contains song metadata only, not audio.

## Build
Open in Android Studio (latest stable), sync Gradle, run on a device/emulator with Android 8.0+ (API 26).
`./gradlew assembleDebug` · `./gradlew testDebugUnitTest` · `./gradlew lintDebug`

## Permissions
Audio read access (to find your music), notifications (playback controls), optional "Modify system settings" (ringtone only). No network.

## Verified status / known limitations
(Fill from `brain.md`.) Effects depend on device support; some formats need device decoders.

## License
Choose a license before publishing.
````

## 5. Other repo files to create
`brain.md`, `CHANGELOG.md`, `.gitignore` (build/, .gradle/, local.properties, *.jks, exported JSON, music files), `docs/PRIVACY.md` (offline statement), `docs/TESTING.md`.

## 6. Acceptance for this part
Widget can be added, shows real current track, buttons control playback, shows "Nothing playing" otherwise; recap computed from real events; launcher icon renders in round/square/themed masks; README claims only what was verified; `brain.md` updated.
