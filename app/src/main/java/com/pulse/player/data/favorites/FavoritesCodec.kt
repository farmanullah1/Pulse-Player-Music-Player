package com.pulse.player.data.favorites

import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.norm
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

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
    const val MAX_BYTES = 10 * 1024 * 1024
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
            val o = arr.optJSONObject(i)
            if (o == null) {
                skipped++
                continue
            }
            val title = o.optString("title").take(MAX_STR)
            if (title.isBlank()) {
                skipped++
                continue
            }
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
