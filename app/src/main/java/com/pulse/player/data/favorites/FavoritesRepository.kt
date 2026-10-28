package com.pulse.player.data.favorites

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.pulse.player.data.db.FavoriteEntity
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class FavoritesRepository(private val ctx: Context, private val db: PulseDb) {
    suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val tracks = db.favorites().allTracksOnce()
            if (tracks.isEmpty()) error("You have no favourites to export yet.")
            val added = db.favorites().addedAtList().associate { it.trackId to it.addedAtMs }
            val json = FavoritesCodec.encode(tracks, added)
            ctx.contentResolver.openOutputStream(uri, "wt")!!.use {
                it.write(json.toByteArray(Charsets.UTF_8))
                it.flush()
            }
            tracks.size
        }
    }

    data class Preview(
        val fileName: String,
        val exportedAt: String?,
        val results: List<MatchResult>,
        val invalid: Int
    )

    suspend fun preview(uri: Uri, fileName: String): Result<Preview> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = ctx.contentResolver.openInputStream(uri)!!.use {
                readLimited(it, FavoritesCodec.MAX_BYTES)
            }
            when (val d = FavoritesCodec.decode(bytes.toString(Charsets.UTF_8))) {
                is FavoritesCodec.Decoded.Fail -> error(d.reason)
                is FavoritesCodec.Decoded.Ok -> {
                    val lib = db.tracks().allOnce().filter { it.available }
                    val favIds = db.favorites().observeIds().first().toSet()
                    Preview(fileName, d.exportedAt, FavoritesMatcher(lib, favIds).match(d.items), d.skipped)
                }
            }
        }
    }

    /** Applies only confident matches + user-resolved ambiguous picks. Atomic and idempotent. */
    suspend fun apply(preview: Preview, resolved: Map<FavItem, TrackEntity>, replace: Boolean): Int =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val ids = preview.results.mapNotNull { (it as? MatchResult.Matched)?.track?.id } +
                    resolved.values.map { it.id }
            val distinctIds = ids.distinct()
            db.withTransaction {
                if (replace) db.favorites().clear()
                db.favorites().addAll(distinctIds.map { FavoriteEntity(it, now) })
            }
            distinctIds.size
        }

    private fun readLimited(i: InputStream, max: Int): ByteArray {
        val o = ByteArrayOutputStream()
        val b = ByteArray(8192)
        var n: Int
        var t = 0
        while (i.read(b).also { n = it } > 0) {
            t += n
            if (t > max) error("The file is too large.")
            o.write(b, 0, n)
        }
        return o.toByteArray()
    }
}
