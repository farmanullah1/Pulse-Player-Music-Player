# 04 — Playback, Controller, Equalizer, Sleep Timer, Fades

Rule: **one** ExoPlayer, owned by the service. The UI talks to it only through `MediaController` (class `PlayerController`).

## 1. UI-facing models
```kotlin
data class Track(val id: String, val uri: String, val title: String, val artist: String, val album: String,
                 val durationMs: Long, val isFavorite: Boolean, val folder: String = "", val dateAddedMs: Long = 0)
fun TrackWithFav.toTrack() = Track(track.id, track.uri, track.title, track.artist, track.album, track.durationMs, isFavorite, track.folderPath, track.dateAddedMs)

data class PlaybackUiState(
    val mediaId: String? = null, val title: String = "", val artist: String = "", val album: String = "", val artworkUri: String? = null,
    val isPlaying: Boolean = false, val playWhenReady: Boolean = false, val buffering: Boolean = false,
    val durationMs: Long = 0, val index: Int = -1, val queueSize: Int = 0,
    val shuffle: Boolean = false, val repeat: Int = Player.REPEAT_MODE_OFF, val speed: Float = 1f,
    val sleepRemainingMs: Long = 0, val sleepEndOfTrack: Boolean = false, val error: String? = null
)
```

## 2. Playback service
```kotlin
package com.pulse.player.playback

class MusicPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)          // pause on headset/Bluetooth disconnect
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) { AudioFx.attach(audioSessionId) }
            override fun onEvents(p: Player, events: Player.Events) { WidgetStateStore.write(this@MusicPlaybackService, p, scope) }
            override fun onPlayerError(error: PlaybackException) {
                // Skip a broken file but never loop forever
                if (player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.prepare() }
            }
        })
        AudioFx.attach(player.audioSessionId)
        session = MediaSession.Builder(this, player).build()
    }
    override fun onGetSession(info: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()   // keep playing if user is listening
    }
    override fun onDestroy() {
        scope.cancel(); AudioFx.release()
        session?.run { player.release(); release() }; session = null
        super.onDestroy()
    }
}
```
Notification: Media3 `MediaSessionService` creates it automatically (prev / play-pause / next) and lock-screen controls come from the session. Provide a small notification icon `@drawable/ic_notification` (vector note). Request `POST_NOTIFICATIONS` on API 33+.

## 3. PlayerController (UI side)
```kotlin
class PlayerController(private val ctx: Context, private val db: PulseDb) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mc: MediaController? = null
    private val _state = MutableStateFlow(PlaybackUiState()); val state = _state.asStateFlow()
    private val _pos = MutableStateFlow(0L); val position = _pos.asStateFlow()          // only seek bars collect this
    private val _queue = MutableStateFlow<List<Track>>(emptyList()); val queue = _queue.asStateFlow()
    private var sleepJob: Job? = null; private var fadeJob: Job? = null
    private var listenedMs = 0L; private var lastId: String? = null

    init {
        val token = SessionToken(ctx, ComponentName(ctx, MusicPlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).buildAsync()
        f.addListener({
            mc = f.get().also { it.addListener(listener); sync(it) }
        }, ContextCompat.getMainExecutor(ctx))
        scope.launch { while (true) { delay(250)
            mc?.let { if (it.isPlaying) { _pos.value = it.currentPosition; listenedMs += 250 } } } }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(p: Player, e: Player.Events) { sync(p as MediaController) }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            logEvent(completed = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            lastId = item?.mediaId; listenedMs = 0
            if (_state.value.sleepEndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) { mc?.pause(); cancelSleep() }
        }
        override fun onPlayerError(e: PlaybackException) { _state.update { it.copy(error = "This file can't be played on this device.") } }
    }
    private fun logEvent(completed: Boolean) {
        val id = lastId ?: return; val ms = listenedMs
        if (ms >= 30_000 || completed) scope.launch(Dispatchers.IO) { db.stats().insert(PlayEventEntity(trackId = id, playedAtMs = System.currentTimeMillis(), listenedMs = ms, completed = completed)) }
    }
    private fun sync(c: MediaController) {
        val md = c.mediaMetadata
        _state.update { it.copy(
            mediaId = c.currentMediaItem?.mediaId, title = md.title?.toString().orEmpty(), artist = md.artist?.toString().orEmpty(),
            album = md.albumTitle?.toString().orEmpty(), artworkUri = c.currentMediaItem?.localConfiguration?.uri?.toString(),
            isPlaying = c.isPlaying, playWhenReady = c.playWhenReady, buffering = c.playbackState == Player.STATE_BUFFERING,
            durationMs = if (c.duration == C.TIME_UNSET) 0 else c.duration, index = c.currentMediaItemIndex, queueSize = c.mediaItemCount,
            shuffle = c.shuffleModeEnabled, repeat = c.repeatMode, speed = c.playbackParameters.speed) }
        _queue.value = (0 until c.mediaItemCount).map { i -> c.getMediaItemAt(i).let { m ->
            Track(m.mediaId, m.localConfiguration?.uri.toString(), m.mediaMetadata.title.toString(), m.mediaMetadata.artist.toString(),
                  m.mediaMetadata.albumTitle.toString(), m.mediaMetadata.durationMs ?: 0, false) } }
        _pos.value = c.currentPosition
    }

    private fun Track.toItem() = MediaItem.Builder().setMediaId(id).setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setDurationMs(durationMs).build()).build()

    // ---- commands (all main-thread) ----
    fun playQueue(tracks: List<Track>, startIndex: Int, shuffle: Boolean = false) {
        val c = mc ?: return; if (tracks.isEmpty()) return
        c.setMediaItems(tracks.map { it.toItem() }, startIndex.coerceIn(0, tracks.lastIndex), 0L)
        c.shuffleModeEnabled = shuffle; c.prepare(); fadeIn(c)
    }
    fun togglePlay() { val c = mc ?: return; if (c.isPlaying) fadePause(c) else { if (c.playbackState == Player.STATE_IDLE) c.prepare(); fadeIn(c) } }
    fun next() { mc?.seekToNextMediaItem() }
    fun previous() { mc?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem() } }
    fun seekTo(ms: Long) { mc?.seekTo(ms); _pos.value = ms }
    fun skipBy(ms: Long) { mc?.let { seekTo((it.currentPosition + ms).coerceIn(0, it.duration.coerceAtLeast(0))) } }
    fun toggleShuffle() { mc?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() { mc?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF } } }
    fun setSpeed(s: Float) { mc?.playbackParameters = PlaybackParameters(s, 1f) }          // pitch preserved
    fun playNext(t: Track) { mc?.let { it.addMediaItem((it.currentMediaItemIndex + 1).coerceAtMost(it.mediaItemCount), t.toItem()) } }
    fun addToQueue(t: Track) { mc?.addMediaItem(t.toItem()) }
    fun removeFromQueue(i: Int) { mc?.removeMediaItem(i) }
    fun moveInQueue(from: Int, to: Int) { mc?.moveMediaItem(from, to) }
    fun clearQueue() { mc?.clearMediaItems() }
    fun skipToQueueIndex(i: Int) { mc?.seekTo(i, 0L) }

    // ---- real audio fades (volume ramp) ----
    private fun ramp(c: MediaController, from: Float, to: Float, ms: Long, done: () -> Unit = {}) {
        fadeJob?.cancel(); fadeJob = scope.launch {
            val steps = 12; for (i in 0..steps) { c.volume = from + (to - from) * i / steps; delay(ms / steps) }; done() }
    }
    private fun fadeIn(c: MediaController) { if (fadeEnabled) { c.volume = 0f; c.play(); ramp(c, 0f, 1f, 350) } else { c.volume = 1f; c.play() } }
    private fun fadePause(c: MediaController) { if (fadeEnabled) ramp(c, c.volume, 0f, 250) { c.pause(); c.volume = 1f } else c.pause() }
    var fadeEnabled = true

    // ---- sleep timer ----
    fun startSleep(minutes: Int) {
        cancelSleep(); val end = SystemClock.elapsedRealtime() + minutes * 60_000L
        sleepJob = scope.launch {
            while (true) { val left = end - SystemClock.elapsedRealtime()
                _state.update { it.copy(sleepRemainingMs = left.coerceAtLeast(0)) }
                if (left <= 0) break
                if (left <= 10_000) mc?.let { it.volume = (left / 10_000f).coerceIn(0f, 1f) }   // 10 s fade-out
                delay(500) }
            mc?.pause(); mc?.volume = 1f; _state.update { it.copy(sleepRemainingMs = 0) }
        }
    }
    fun sleepAtEndOfTrack() { cancelSleep(); _state.update { it.copy(sleepEndOfTrack = true) } }
    fun cancelSleep() { sleepJob?.cancel(); mc?.volume = 1f; _state.update { it.copy(sleepRemainingMs = 0, sleepEndOfTrack = false) } }

    // ---- queue persistence ----
    fun snapshot(): Triple<List<String>, Int, Long>? = mc?.let { c -> Triple((0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }, c.currentMediaItemIndex, c.currentPosition) }
    suspend fun restore(ids: List<String>, index: Int, pos: Long, resolve: suspend (List<String>) -> List<Track>) {
        val tracks = resolve(ids); val c = mc ?: return; if (tracks.isEmpty()) return
        c.setMediaItems(tracks.map { it.toItem() }, index.coerceIn(0, tracks.lastIndex), pos); c.prepare()   // stays paused
    }
}
```
Mini-note: `MediaMetadata.durationMs` is a `Long?`. Use `?: 0L`.

## 4. Audio effects (equalizer, bass, virtualizer, reverb) — capability-checked
```kotlin
object AudioFx {
    private var eq: Equalizer? = null; private var bass: BassBoost? = null
    private var virt: Virtualizer? = null; private var reverb: PresetReverb? = null
    var sessionId = 0; private set
    val reverbNames = listOf("None", "Small room", "Medium room", "Large room", "Medium hall", "Large hall", "Plate")
    private val reverbPresets = shortArrayOf(PresetReverb.PRESET_NONE, PresetReverb.PRESET_SMALLROOM, PresetReverb.PRESET_MEDIUMROOM,
        PresetReverb.PRESET_LARGEROOM, PresetReverb.PRESET_MEDIUMHALL, PresetReverb.PRESET_LARGEHALL, PresetReverb.PRESET_PLATE)

    fun attach(id: Int) {
        if (id == 0 || id == sessionId) return; release(); sessionId = id
        eq = runCatching { Equalizer(0, id).apply { enabled = true } }.getOrNull()
        bass = runCatching { BassBoost(0, id).apply { enabled = true } }.getOrNull()
        virt = runCatching { Virtualizer(0, id).apply { enabled = true } }.getOrNull()
        reverb = runCatching { PresetReverb(0, id).apply { enabled = true } }.getOrNull()
        saved?.let { applySaved(it) }
    }
    fun release() { listOf(eq, bass, virt, reverb).forEach { runCatching { it?.release() } }; eq = null; bass = null; virt = null; reverb = null; sessionId = 0 }

    val eqSupported get() = eq != null
    val bandCount get() = eq?.numberOfBands?.toInt() ?: 0
    val levelRange get() = eq?.bandLevelRange?.let { it[0].toInt() to it[1].toInt() } ?: (-1500 to 1500)   // millibels
    fun bandFreqHz(b: Int) = (eq?.getCenterFreq(b.toShort()) ?: 0) / 1000
    fun bandLevel(b: Int) = eq?.getBandLevel(b.toShort())?.toInt() ?: 0
    fun setBand(b: Int, mb: Int) { eq?.setBandLevel(b.toShort(), mb.toShort()) }
    val bassSupported get() = bass?.strengthSupported == true
    fun setBass(permille: Int) { bass?.setStrength(permille.toShort()) }          // 0..1000
    val virtSupported get() = virt?.strengthSupported == true
    fun setVirtualizer(permille: Int) { virt?.setStrength(permille.toShort()) }
    val reverbSupported get() = reverb != null
    fun setReverb(index: Int) { reverb?.preset = reverbPresets[index] }

    // Presets: levels expressed as fractions (-1..1) of the supported range, mapped to however many bands exist.
    val presets: Map<String, FloatArray> = mapOf(
        "Normal" to floatArrayOf(0f, 0f, 0f, 0f, 0f), "Classical" to floatArrayOf(0.4f, 0.2f, -0.1f, 0.3f, 0.4f),
        "Dance" to floatArrayOf(0.6f, 0.3f, 0f, 0.3f, 0.5f), "Flat" to floatArrayOf(0f, 0f, 0f, 0f, 0f),
        "Folk" to floatArrayOf(0.2f, 0f, 0.1f, 0.3f, 0.1f), "Heavy Metal" to floatArrayOf(0.5f, 0.2f, 0.6f, 0.4f, 0.2f),
        "Hip Hop" to floatArrayOf(0.6f, 0.4f, 0f, 0.1f, 0.3f), "Jazz" to floatArrayOf(0.3f, 0.1f, -0.2f, 0.2f, 0.4f),
        "Pop" to floatArrayOf(-0.2f, 0.2f, 0.4f, 0.2f, -0.2f), "Rock" to floatArrayOf(0.5f, 0.2f, -0.1f, 0.3f, 0.5f))
    fun applyPreset(name: String) {
        val p = presets[name] ?: return; val (lo, hi) = levelRange; val n = bandCount; if (n == 0) return
        for (b in 0 until n) { val f = p[(b * (p.size - 1)) / (n - 1).coerceAtLeast(1)]; setBand(b, (if (f >= 0) f * hi else -f * lo).toInt()) }
    }
    var saved: FxSettings? = null
    data class FxSettings(val bands: List<Int>, val bass: Int, val virt: Int, val reverb: Int)
    private fun applySaved(s: FxSettings) { s.bands.forEachIndexed { i, v -> if (i < bandCount) setBand(i, v) }; setBass(s.bass); setVirtualizer(s.virt); setReverb(s.reverb) }
}
```
Because the service and UI run in the same process, `AudioFx` is a shared singleton. The Equalizer screen **must** hide any section whose `...Supported` flag is false and show "Not supported on this device". Persist settings (bands list, bass, virt, reverb, preset name) in DataStore and set `AudioFx.saved` at app start so it re-applies on attach.

## 5. Equalizer screen requirements
Master on/off switch · preset chips (horizontal scroll) · one vertical slider per real band (label e.g. "250 Hz", range in dB = mb/100) · Bass boost knob/slider · Virtualizer slider · Reverb chips (names above) · Reset button. Changes apply live. Slider state hoisted in a ViewModel; do not write to DataStore on every drag tick (write on `onValueChangeFinished`).

## 6. Ringtone helper
```kotlin
fun setRingtone(ctx: Context, uri: Uri, type: Int /* RingtoneManager.TYPE_RINGTONE | TYPE_NOTIFICATION | TYPE_ALARM */): Boolean {
    if (!Settings.System.canWrite(ctx)) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return false }
    return runCatching { RingtoneManager.setActualDefaultRingtoneUri(ctx, type, uri); true }.getOrDefault(false)
}
```
Show a toast ("Ringtone set" / "Couldn't set ringtone"). Content URIs from MediaStore work; SAF URIs may not — then show "Can't use this file as ringtone".

## 7. Saving the queue
In `MainActivity`, `lifecycleScope` collects `player.state` and every 5 s while `isPlaying` plus on pause calls `player.snapshot()` and stores ids/index/position in DataStore. On launch, if the library is loaded, call `restore(...)` with `resolve = { ids -> db.tracks().byIds(ids) ordered as ids }`.

## 8. Acceptance for this part
Song plays from the list; plays with screen off; notification and lock-screen controls work; unplug headset → pauses; sleep timer fades and pauses; speed change works; EQ hides unsupported controls; only one player instance; `brain.md` updated.
