package com.pulse.player.data.backup.classic

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.pulse.player.data.backup.common.BackupKind
import com.pulse.player.data.backup.common.BackupSniffer
import com.pulse.player.data.backup.common.LimitedStream
import com.pulse.player.data.db.FavoriteEntity
import com.pulse.player.data.db.PulseDb
import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.norm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import kotlin.math.abs

data class FavItem(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val addedAtMs: Long,
    val normTitle: String,
    val normArtist: String,
    val normAlbum: String
)

object FavoritesCodec {
    const val SCHEMA = "pulseplayer.favorites"
    const val VERSION = 1
    const val MAX_BYTES = 10 * 1024 * 1024L
    const val MAX_ITEMS = 50_000
    private const val MAX_STR = 500

    fun encode(tracks: List<TrackEntity>, addedAt: Map<String, Long>): String {
        val arr = JSONArray()
        tracks.forEach { t ->
            arr.put(JSONObject().apply {
                put("title", t.title)
                put("artist", t.artist)
                put("album", t.album)
                put("durationMs", t.durationMs)
                put("displayName", t.displayName)
                put("mimeType", t.mimeType ?: JSONObject.NULL)
                put("sizeBytes", t.sizeBytes)
                put("addedAtMs", addedAt[t.id] ?: 0L)
                put("normTitle", t.normTitle)
                put("normArtist", t.normArtist)
                put("normAlbum", t.normAlbum)
            })
        }
        return JSONObject().apply {
            put("schema", SCHEMA)
            put("schemaVersion", VERSION)
            put("app", "Pulse Player")
            put("exportedAt", Instant.now().toString())
            put("count", tracks.size)
            put("items", arr)
        }.toString(2)
    }

    sealed interface Decoded {
        data class Ok(val items: List<FavItem>, val exportedAt: String?, val skipped: Int) : Decoded
        data class Fail(val reason: String) : Decoded
    }

    fun decode(text: String): Decoded {
        val root = runCatching { JSONObject(text) }.getOrElse {
            return Decoded.Fail("This file is not valid JSON.")
        }
        if (root.optString("schema") != SCHEMA) {
            return Decoded.Fail("This is not a Pulse Player favorites file.")
        }
        if (root.optInt("schemaVersion", -1) != VERSION) {
            return Decoded.Fail("Unsupported file version. Update the app and try again.")
        }
        val arr = root.optJSONArray("items") ?: return Decoded.Fail("The file has no items list.")
        if (arr.length() > MAX_ITEMS) return Decoded.Fail("The file has too many items.")
        val out = ArrayList<FavItem>(arr.length())
        var skipped = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: run { skipped++; continue }
            val title = o.optString("title").take(MAX_STR)
            if (title.isBlank()) { skipped++; continue }
            val artist = o.optString("artist").take(MAX_STR)
            val album = o.optString("album").take(MAX_STR)
            out += FavItem(
                title = title,
                artist = artist,
                album = album,
                durationMs = o.optLong("durationMs", 0).coerceAtLeast(0),
                displayName = o.optString("displayName").take(MAX_STR),
                mimeType = o.optString("mimeType").takeIf { it.isNotBlank() && it != "null" },
                sizeBytes = o.optLong("sizeBytes", 0).coerceAtLeast(0),
                addedAtMs = o.optLong("addedAtMs", 0),
                normTitle = norm(title),
                normArtist = norm(artist),
                normAlbum = norm(album)
            )
        }
        return Decoded.Ok(out, root.optString("exportedAt").takeIf { it.isNotBlank() }, skipped)
    }
}

sealed interface MatchResult {
    data class Matched(val item: FavItem, val track: TrackEntity, val level: String) : MatchResult
    data class AlreadyFavorite(val item: FavItem, val track: TrackEntity) : MatchResult
    data class Ambiguous(val item: FavItem, val candidates: List<TrackEntity>) : MatchResult
    data class Missing(val item: FavItem) : MatchResult
    data class Duplicate(val item: FavItem) : MatchResult
}

class FavoritesMatcher(
    private val library: List<TrackEntity>,
    private val favoriteIds: Set<String>
) {
    private val byTitleArtist = library.groupBy { it.normTitle to it.normArtist }
    private val byTitle = library.groupBy { it.normTitle }

    fun match(items: List<FavItem>): List<MatchResult> {
        val seen = HashSet<String>()
        val out = ArrayList<MatchResult>(items.size)

        for (it in items) {
            val key = "${it.normTitle}|${it.normArtist}|${it.durationMs / 2000}"
            if (!seen.add(key)) {
                out += MatchResult.Duplicate(it)
                continue
            }

            // 1) strong: same file name + size + near duration
            val strong = library.filter { t ->
                t.sizeBytes == it.sizeBytes && it.sizeBytes > 0 &&
                        t.displayName.equals(it.displayName, true) &&
                        abs(t.durationMs - it.durationMs) <= 2000
            }

            // 2) high: title + artist + duration within 2s
            val ta = byTitleArtist[it.normTitle to it.normArtist].orEmpty()
            val high = ta.filter { t -> abs(t.durationMs - it.durationMs) <= 2000 }

            // 3) medium: title + album, duration within 5s
            val medium = byTitle[it.normTitle].orEmpty().filter { t ->
                t.normAlbum == it.normAlbum && abs(t.durationMs - it.durationMs) <= 5000
            }

            val (cands, level) = when {
                strong.size == 1 -> strong to "strong"
                high.size == 1 -> high to "high"
                high.isEmpty() && medium.size == 1 -> medium to "medium"
                else -> (strong.ifEmpty { high.ifEmpty { medium } }) to "multi"
            }

            out += when {
                cands.isEmpty() -> MatchResult.Missing(it)
                cands.size > 1 || level == "multi" -> MatchResult.Ambiguous(it, cands)
                cands[0].id in favoriteIds -> MatchResult.AlreadyFavorite(it, cands[0])
                else -> MatchResult.Matched(it, cands[0], level)
            }
        }
        return out
    }
}

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
            val sniffed = BackupSniffer.sniff(bytes)
            if (sniffed == BackupKind.Full) {
                error("This looks like a Full backup — open the Full backup importer?")
            }
            if (sniffed == BackupKind.RealWav) {
                error("This is an audio WAV file, not a Pulse Player favorites backup.")
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

    private fun readLimited(i: InputStream, max: Long): ByteArray {
        val o = ByteArrayOutputStream()
        LimitedStream(i, max).use { limited ->
            val b = ByteArray(8192)
            var n: Int
            while (limited.read(b).also { n = it } > 0) {
                o.write(b, 0, n)
            }
        }
        return o.toByteArray()
    }
}

