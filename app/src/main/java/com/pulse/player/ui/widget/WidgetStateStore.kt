package com.pulse.player.ui.widget

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.glance.appwidget.updateAll
import androidx.media3.common.Player
import com.pulse.player.data.scan.ArtworkLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object WidgetStateStore {
    private const val PREFS = "widget_state"
    private const val POS_THROTTLE_MS = 2000L

    @Volatile private var lastFingerprint: String? = null
    @Volatile private var lastArtworkUri: String? = null
    @Volatile private var lastPosWriteMs: Long = 0L
    @Volatile private var cachedFavIds: Set<String> = emptySet()

    internal fun buildFingerprint(
        mediaId: String?,
        uri: String?,
        title: String?,
        artist: String?,
        isPlaying: Boolean,
        has: Boolean,
        isFavorite: Boolean
    ): String = "${mediaId.orEmpty()}|${uri.orEmpty()}|${title.orEmpty()}|${artist.orEmpty()}|$isPlaying|$has|$isFavorite"

    internal fun buildFingerprint(
        uri: String?,
        title: String?,
        artist: String?,
        isPlaying: Boolean,
        has: Boolean
    ): String = buildFingerprint(null, uri, title, artist, isPlaying, has, false)

    internal fun shouldRefreshArtwork(prevUri: String?, newUri: String?): Boolean = prevUri != newUri

    fun updateFavoriteIds(ctx: Context, favoriteIds: Set<String>, scope: CoroutineScope) {
        cachedFavIds = favoriteIds
        val sp = read(ctx)
        val currentMediaId = sp.getString("mediaId", null) ?: return
        val newFav = currentMediaId in favoriteIds
        val prevFav = sp.getBoolean("isFavorite", false)
        if (newFav == prevFav) return
        sp.edit().putBoolean("isFavorite", newFav).apply()
        lastFingerprint = null
        scope.launch(Dispatchers.IO) {
            runCatching { PlayerWidget().updateAll(ctx) }
        }
    }

    fun write(ctx: Context, p: Player, scope: CoroutineScope) {
        val md = p.mediaMetadata
        val mediaId = p.currentMediaItem?.mediaId
        val title = md.title?.toString()
        val artist = md.artist?.toString()
        val isPlaying = p.isPlaying
        val has = p.mediaItemCount > 0
        val pos = p.currentPosition.coerceAtLeast(0L)
        val dur = if (p.duration < 0) 0L else p.duration
        val uri = p.currentMediaItem?.localConfiguration?.uri?.toString()
        val isFavorite = mediaId != null && mediaId in cachedFavIds

        val fingerprint = buildFingerprint(mediaId, uri, title, artist, isPlaying, has, isFavorite)
        val now = System.currentTimeMillis()
        val fingerprintChanged = fingerprint != lastFingerprint
        val artChanged = shouldRefreshArtwork(lastArtworkUri, uri)
        if (!fingerprintChanged && now - lastPosWriteMs < POS_THROTTLE_MS) {
            return
        }
        lastFingerprint = fingerprint
        lastPosWriteMs = now
        val prevArtUri = lastArtworkUri
        if (artChanged) {
            lastArtworkUri = uri
        }

        val artPath = File(ctx.cacheDir, "widget_art.png")

        scope.launch(Dispatchers.IO) {
            if (artChanged || (uri != null && !artPath.exists() && prevArtUri == null)) {
                if (uri != null) {
                    ArtworkLoader.load(ctx, uri, 160)?.let { b ->
                        runCatching {
                            artPath.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 90, it) }
                        }
                    }
                } else {
                    artPath.delete()
                }
            }
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("mediaId", mediaId)
                .putString("title", title)
                .putString("artist", artist)
                .putBoolean("playing", isPlaying)
                .putBoolean("has", has)
                .putBoolean("isFavorite", isFavorite)
                .putLong("pos", pos)
                .putLong("dur", dur)
                .apply()
            if (fingerprintChanged) {
                runCatching {
                    PlayerWidget().updateAll(ctx)
                }
            }
        }
    }

    fun read(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
