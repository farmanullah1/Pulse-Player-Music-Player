# 13 — Fix: Coordinated Audio Transitions, Sleep Timer, Controller Lifecycle, Errors, Seek, Ringtone

> Replaces `04_PLAYBACK_AND_AUDIO.md` §2 (service), §3 (controller) and the fade/sleep code in it. `AudioFx` (04 §4), the equalizer screen (04 §5) and the ringtone helper idea (04 §6) stay, except where changed below. Blueprint code: fix imports/signatures for the installed Media3.

## 1. The design in one picture
- The **service side owns everything that changes sound or ends a session**: `AudioTransitionController` (all fades), `SleepTimerEngine`, `ErrorPolicy`, `SessionTracker` (file 15) — held in a process-wide `PlaybackEngine` singleton, because the UI and the service share one process (same pattern as `AudioFx`).
- Every command from every source (app, notification, lock screen, headset button, widget) reaches ExoPlayer through a `FadingPlayer` wrapper handed to `MediaSession`. So **one code path** decides fades; nothing else writes `volume`.
- The UI `PlayerController` is a thin, disposable client: it attaches/detaches with the activity and reconnects if the service dies.
- Volume is **never persisted**. A fresh ExoPlayer always starts at user volume 1.0 with fade multiplier 1.0, so killing the service mid-fade can never leave the player muted.

## 2. AudioTransitionController
```kotlin
package com.pulse.player.playback

enum class TransitionReason(val defaultMs: Long) {
    PlayStart(350), PauseEnd(250), SleepTimer(10_000), AudioFocusLoss(150), AudioFocusGain(300), TrackChange(200) }

/** The ONLY writer of player volume. effective = userVolume * fadeMultiplier. */
class AudioTransitionController(
    private val scope: CoroutineScope,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
    private val sink: (Float) -> Unit,                       // sink = { exo.volume = it }
) {
    var userVolume = 1f; private set                         // what the user chose; fades never touch it
    var fadeMultiplier = 1f; private set                     // temporary, 0..1
    var activeReason: TransitionReason? = null; private set
    val effectiveVolume get() = (userVolume * fadeMultiplier).coerceIn(0f, 1f)
    private var job: Job? = null
    private fun publish() = sink(effectiveVolume)

    fun setUserVolume(v: Float) { userVolume = v.coerceIn(0f, 1f); publish() }   // does not restart or cancel a fade

    /** One transition at a time. A new request cancels the old one and starts from the CURRENT audible level. */
    fun fadeTo(target: Float, durationMs: Long, reason: TransitionReason, onDone: (() -> Unit)? = null) {
        job?.cancel()
        val from = fadeMultiplier; val to = target.coerceIn(0f, 1f)
        if (durationMs <= 0 || from == to) { fadeMultiplier = to; activeReason = null; publish(); onDone?.invoke(); return }
        activeReason = reason
        job = scope.launch {
            val start = clock()
            while (true) {
                val t = ((clock() - start) / durationMs.toFloat()).coerceIn(0f, 1f)
                fadeMultiplier = from + (to - from) * (t * t * (3 - 2 * t))   // smoothstep
                publish()
                if (t >= 1f) break
                delay(16)
            }
            activeReason = null
            onDone?.invoke()                                  // only runs if NOT cancelled
        }
    }
    fun snapTo(multiplier: Float) { job?.cancel(); activeReason = null; fadeMultiplier = multiplier.coerceIn(0f, 1f); publish() }
    /** Call on completion, cancel, error, pause, stop, focus loss, service destroy. Always leaves effective == userVolume. */
    fun restore() = snapTo(1f)
}
```
Rules: a fade-out that ends in "pause" passes the pause as `onDone`, so if a newer transition cancels it the pause is **not** executed (no half-faded stop). After any actual pause/stop, call `restore()`.

## 3. FadingPlayer (every command goes through it)
```kotlin
class FadingPlayer(private val raw: ExoPlayer, private val t: AudioTransitionController,
                   private val sleep: SleepTimerEngine, private val fadeEnabled: () -> Boolean) : ForwardingPlayer(raw) {

    override fun play() = startPlayback()
    override fun setPlayWhenReady(playWhenReady: Boolean) { if (playWhenReady) startPlayback() else pause() }

    private fun startPlayback() {
        if (t.activeReason == TransitionReason.SleepTimer) sleep.cancel()           // user wants to keep listening: cancels the timer and restores volume
        if (!fadeEnabled()) { t.restore(); raw.play(); return }
        when {
            t.activeReason == TransitionReason.PauseEnd -> { raw.play(); t.fadeTo(1f, TransitionReason.PlayStart.defaultMs, TransitionReason.PlayStart) } // fade back in from the audible level, never from zero
            !raw.isPlaying -> { t.snapTo(0f); raw.play(); t.fadeTo(1f, TransitionReason.PlayStart.defaultMs, TransitionReason.PlayStart) }
            else -> t.fadeTo(1f, TransitionReason.PlayStart.defaultMs, TransitionReason.PlayStart)
        }
    }
    override fun pause() {
        if (!fadeEnabled() || !raw.isPlaying) { raw.pause(); t.restore(); return }
        t.fadeTo(0f, TransitionReason.PauseEnd.defaultMs, TransitionReason.PauseEnd) { raw.pause(); t.restore() }
    }
    override fun stop() { raw.stop(); t.restore() }
    override fun seekToNextMediaItem() = cut { raw.seekToNextMediaItem() }
    override fun seekToPreviousMediaItem() = cut { raw.seekToPreviousMediaItem() }
    /** Executes the skip immediately (never drops a tap), starting silent to avoid a pop, then fades in. */
    private fun cut(op: () -> Unit) {
        if (!fadeEnabled() || !raw.isPlaying) { op(); return }
        t.snapTo(0f); op(); t.fadeTo(1f, TransitionReason.TrackChange.defaultMs, TransitionReason.TrackChange)
    }
    override fun setVolume(volume: Float) = t.setUserVolume(volume)
    override fun getVolume(): Float = t.userVolume
}
```
If the installed Media3 reports that some `ForwardingPlayer` members are `final`/different, adapt, but keep the rule: **play, pause, setPlayWhenReady, stop, next, previous, volume all go through the controller.**

## 4. Sleep timer (service side, same controller)
```kotlin
class SleepTimerEngine(private val scope: CoroutineScope, private val raw: ExoPlayer,
                       private val t: AudioTransitionController, private val clock: () -> Long = { SystemClock.elapsedRealtime() }) {
    data class State(val remainingMs: Long = 0, val endOfTrack: Boolean = false)
    val state = MutableStateFlow(State())
    private var job: Job? = null
    companion object { const val FADE_MS = 10_000L }

    fun start(minutes: Int) {
        cancel()
        val end = clock() + minutes * 60_000L
        job = scope.launch {
            var faded = false
            while (true) {
                val left = end - clock(); state.value = State(left.coerceAtLeast(0))
                if (left <= 0) break
                if (!faded && left <= FADE_MS) { faded = true; t.fadeTo(0f, left, TransitionReason.SleepTimer) }   // same controller as every other fade
                delay(250)
            }
            raw.pause(); t.restore(); state.value = State()
        }
    }
    /** "End of this track": ExoPlayer itself pauses when the current item ends, so the next song never starts. */
    fun startEndOfTrack() { cancel(); raw.pauseAtEndOfMediaItems = true; state.value = State(endOfTrack = true) }
    fun onPausedAtEndOfItem() { if (state.value.endOfTrack) { raw.pauseAtEndOfMediaItems = false; t.restore(); state.value = State() } }
    fun cancel() { job?.cancel(); job = null; raw.pauseAtEndOfMediaItems = false; t.restore(); state.value = State() }   // volume back to full immediately
}
```
`onPausedAtEndOfItem()` is called from the engine listener when `onPlayWhenReadyChanged(false, PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM)` fires. Policy: pressing play while the final fade runs **cancels** the timer (documented in the UI as "Sleep timer cancelled").

## 5. PlaybackEngine and service (replaces 04 §2)
```kotlin
object PlaybackEngine {
    var scope: CoroutineScope? = null; private set
    var exo: ExoPlayer? = null; private set
    lateinit var transitions: AudioTransitionController private set
    lateinit var sleep: SleepTimerEngine private set
    lateinit var errors: ErrorPolicy private set
    lateinit var sessions: SessionTracker private set                       // file 15 §1
    var fadingPlayer: FadingPlayer? = null; private set
    @Volatile var fadeEnabled = true                                          // set from SettingsStore

    fun attach(ctx: Context, db: PulseDb, player: ExoPlayer) {
        detach()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate); scope = s; exo = player
        transitions = AudioTransitionController(s) { player.volume = it }
        sleep = SleepTimerEngine(s, player, transitions)
        errors = ErrorPolicy(player)
        sessions = SessionTracker(ctx.applicationContext, db, player)
        fadingPlayer = FadingPlayer(player, transitions, sleep) { fadeEnabled }
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(id: Int) { AudioFx.attach(id) }
            override fun onPlayWhenReadyChanged(pwr: Boolean, reason: Int) {
                if (!pwr) transitions.restore()                                 // a stopped/paused player never stays faded (focus loss, noisy, end of item, user)
                if (!pwr && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) sleep.onPausedAtEndOfItem()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) { errors.onPlaying(); if (transitions.activeReason == null) transitions.restore() }   // plays that bypassed the wrapper are not faded
            }
            override fun onPlayerError(e: PlaybackException) { errors.onError(e) }
            override fun onEvents(p: Player, events: Player.Events) { WidgetStateStore.write(ctx, p, s) }
        })
        AudioFx.attach(player.audioSessionId)
    }
    fun detach() {
        if (exo == null) return
        transitions.restore()                                                  // before release; also never persisted anywhere
        sessions.closeNow("service-destroyed")
        scope?.cancel(); scope = null
        AudioFx.release(); exo?.release(); exo = null; fadingPlayer = null
    }
}

class MusicPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true).setWakeMode(C.WAKE_MODE_LOCAL).build()
        PlaybackEngine.attach(this, (application as PulseApp).db, exo)
        session = MediaSession.Builder(this, PlaybackEngine.fadingPlayer!!).build()
    }
    override fun onGetSession(info: MediaSession.ControllerInfo) = session
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = PlaybackEngine.exo
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }
    override fun onDestroy() { session?.release(); session = null; PlaybackEngine.detach(); super.onDestroy() }
}
```
Hooks the review asked for → where they are handled: player attached/detached = `attach/detach`; service created/destroyed = `onCreate/onDestroy`; audio focus gained/lost = `onPlayWhenReadyChanged` + `onIsPlayingChanged`; activity stopped = only the UI client detaches, the engine keeps running; user volume = `setVolume → setUserVolume`.

## 6. Error policy (one retry, then an actionable card)
```kotlin
class ErrorPolicy(private val raw: ExoPlayer) {
    enum class Category { MISSING_FILE, NO_PERMISSION, UNSUPPORTED, OTHER }
    data class TrackError(val mediaId: String?, val title: String, val category: Category, val message: String)
    val current = MutableStateFlow<TrackError?>(null)
    private val retries = HashMap<String, Int>()

    private fun category(e: PlaybackException) = when (e.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> Category.MISSING_FILE
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> Category.NO_PERMISSION
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> Category.UNSUPPORTED
        else -> Category.OTHER }
    fun onError(e: PlaybackException) {
        val item = raw.currentMediaItem; val id = item?.mediaId ?: "?"; val cat = category(e)
        val n = retries.getOrDefault(id, 0)
        if (n < 1 && cat == Category.OTHER) { retries[id] = n + 1; raw.prepare(); return }      // at most ONE automatic retry, only for unknown errors
        raw.pause(); PlaybackEngine.transitions.restore()
        current.value = TrackError(id, item?.mediaMetadata?.title?.toString().orEmpty(), cat, when (cat) {
            Category.MISSING_FILE -> "This file can't be found. It may have been moved or the SD card removed."
            Category.NO_PERMISSION -> "Pulse Player can't read this file. Check the audio permission."
            Category.UNSUPPORTED -> "This format can't be played on this device."
            Category.OTHER -> "This track failed to play." })
    }
    fun onPlaying() { raw.currentMediaItem?.mediaId?.let { retries.remove(it) }; current.value = null }
    fun retry() { current.value?.mediaId?.let { retries.remove(it) }; current.value = null; raw.prepare(); raw.play() }
    fun skip() { current.value = null; if (raw.hasNextMediaItem()) { raw.seekToNextMediaItem(); raw.prepare(); raw.play() } }
}
```
No auto-skip loops: after the card appears nothing advances until the user taps **Retry** or **Skip**. UI: an error card above the controls showing track title, message and the two buttons (collected from `PlaybackEngine.errors.current`).

## 7. PlayerController (UI client) with explicit lifecycle (replaces 04 §3 plumbing)
```kotlin
class PlayerController(private val ctx: Context, private val db: PulseDb, private val scope: CoroutineScope) {
    private var future: ListenableFuture<MediaController>? = null
    private var mc: MediaController? = null
    private var tickJob: Job? = null; private var reconnectJob: Job? = null; private var attempts = 0
    private val _state = MutableStateFlow(PlaybackUiState()); val state = _state.asStateFlow()
    private val _pos = MutableStateFlow(0L); val position = _pos.asStateFlow()
    // _queue, sync(), Track.toItem() and the queue commands stay as in file 04 §3 (fades/sleep/logEvent removed)

    fun attach() {                                             // call from Activity.onStart
        if (future != null) return
        val token = SessionToken(ctx, ComponentName(ctx, MusicPlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).setListener(object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) { onLost() } }).buildAsync()
        future = f
        f.addListener({
            runCatching { f.get() }.onSuccess { c ->
                if (future === f) { mc = c; attempts = 0; c.addListener(listener); sync(c); updateTicker(c.isPlaying) } else c.release()
            }.onFailure { if (future === f) onLost() }
        }, ContextCompat.getMainExecutor(ctx))
        scope.launch { PlaybackEngine.sleep.state.collect { s -> _state.update { it.copy(sleepRemainingMs = s.remainingMs, sleepEndOfTrack = s.endOfTrack) } } }   // see note
    }
    fun detach() {                                             // call from Activity.onStop; playback continues in the service
        reconnectJob?.cancel(); tickJob?.cancel()
        mc?.removeListener(listener); future?.let { MediaController.releaseFuture(it) }; future = null; mc = null
    }
    private fun onLost() { detach(); scheduleReconnect() }
    private fun scheduleReconnect() {
        if (attempts >= 5) { _state.update { it.copy(error = "Lost connection to the player. Reopen the app.") }; return }
        val wait = 500L shl attempts++; reconnectJob = scope.launch { delay(wait); attach() }
    }
    private fun updateTicker(playing: Boolean) {               // runs only while playing and attached
        tickJob?.cancel()
        if (!playing) return
        tickJob = scope.launch { while (isActive) { delay(250); mc?.let { _pos.value = it.currentPosition } } }
    }
    private val listener = object : Player.Listener {
        override fun onEvents(p: Player, e: Player.Events) { sync(p as MediaController); if (e.contains(Player.EVENT_IS_PLAYING_CHANGED)) updateTicker(p.isPlaying) }
    }
}
```
Note: collect the sleep/error flows once per controller (start the collectors in `init`, not in every `attach`) — keep a `Job` and cancel it in a `release()` called from `PulseApp` shutdown, so reconnects don't create duplicate collectors. `PulseApp` creates the controller but no longer connects in `init`. `MainActivity`: `onStart { app.player.attach() }`, `onStop { app.player.detach() }`.
`togglePlay/next/previous` simply call `mc.play()/pause()/seekToNext…`; the service-side `FadingPlayer` does the fades. Remove `fadeEnabled`, `ramp`, sleep code and `listenedMs` from the controller. Settings → `PlaybackEngine.fadeEnabled`.

## 8. Seek handling
```kotlin
// PlaybackUiState gets: val canSeek: Boolean = false   (sync(): c.isCurrentMediaItemSeekable && c.duration != C.TIME_UNSET && c.duration > 0)
fun seekTo(ms: Long): Boolean {
    val c = mc ?: return false; val d = c.duration
    if (!c.isCurrentMediaItemSeekable || d == C.TIME_UNSET || ms < 0 || ms > d) return false      // reject invalid targets
    c.seekTo(ms); _pos.value = ms; return true
}
fun skipBy(deltaMs: Long): Boolean {
    val c = mc ?: return false; val d = c.duration; if (d == C.TIME_UNSET) return false
    return seekTo((c.currentPosition + deltaMs).coerceIn(0, d))                                    // relative seeks clamp, then validate
}
```
UI: when `!state.canSeek`, show the position only, disable the slider and the −10 s/+10 s buttons (with a content description "Seeking unavailable"); the seek bar never divides by a zero duration.

## 9. Ringtone as a pending user action (replaces the Boolean helper in 04 §6)
```kotlin
sealed interface RingtoneResult {
    data object Done : RingtoneResult
    data class NeedsPermission(val uri: String, val type: Int) : RingtoneResult     // the user was sent to the system screen: NOT a failure
    data class Failed(val reason: String) : RingtoneResult
}
fun trySetRingtone(ctx: Context, uri: Uri, type: Int): RingtoneResult {
    if (!Settings.System.canWrite(ctx)) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return RingtoneResult.NeedsPermission(uri.toString(), type) }
    return runCatching { RingtoneManager.setActualDefaultRingtoneUri(ctx, type, uri); RingtoneResult.Done }
        .getOrElse { RingtoneResult.Failed("Couldn't use this file as a ringtone.") }
}
```
ViewModel keeps `pendingRingtone` (saved in `SavedStateHandle`) and shows a banner "Allow 'Modify system settings' to finish setting your ringtone · Cancel". In the activity/screen: `LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.recheckPendingRingtone() }`: if pending exists and `canWrite` is now true → apply it, clear it, toast the real result; if still not allowed → keep the banner. A SAF URI that cannot be a ringtone returns `Failed` with that message.

## 10. Acceptance (all must be shown by tests in file 17)
- Starting a track while a pause fade-out runs ends with exactly one fade-in from the audible level; volume never reaches 0 because of it.
- Cancelling the sleep timer during its fade returns effective volume to `userVolume` immediately.
- Destroying the service during a fade leaves nothing persisted; a new service starts at 1.0.
- Rapid next/next/next executes every skip.
- A missing file shows the error card with no skip loop; a transient error retries once.
- `controller.detach()` → no leaked listeners/jobs; killing the service → controller reconnects (≤ 5 tries) and shows an error after that.
- No file other than `AudioTransitionController` assigns `volume`.
