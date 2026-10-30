package com.pulse.player.playback

import org.json.JSONArray
import org.json.JSONObject

data class QueueSnapshot(
    val original: List<String>,
    val shuffled: List<String>,
    val currentId: String?,
    val positionMs: Long,
    val repeat: Int,
    val shuffleEnabled: Boolean,
    val seed: Long,
    val savedAtMs: Long
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("original", JSONArray(original))
        obj.put("shuffled", JSONArray(shuffled))
        if (currentId != null) {
            obj.put("currentId", currentId)
        } else {
            obj.put("currentId", JSONObject.NULL)
        }
        obj.put("positionMs", positionMs)
        obj.put("repeat", repeat)
        obj.put("shuffleEnabled", shuffleEnabled)
        obj.put("seed", seed)
        obj.put("savedAtMs", savedAtMs)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String?): QueueSnapshot? {
            if (json.isNullOrBlank()) return null
            return runCatching {
                val obj = JSONObject(json)
                val origArray = obj.getJSONArray("original")
                val orig = ArrayList<String>(origArray.length())
                for (i in 0 until origArray.length()) {
                    orig.add(origArray.getString(i))
                }
                val shufArray = obj.optJSONArray("shuffled")
                val shuf = ArrayList<String>()
                if (shufArray != null) {
                    for (i in 0 until shufArray.length()) {
                        shuf.add(shufArray.getString(i))
                    }
                }
                val curId = if (obj.isNull("currentId")) null else obj.optString("currentId", null)
                QueueSnapshot(
                    original = orig,
                    shuffled = shuf,
                    currentId = curId,
                    positionMs = obj.optLong("positionMs", 0L),
                    repeat = obj.optInt("repeat", 0),
                    shuffleEnabled = obj.optBoolean("shuffleEnabled", false),
                    seed = obj.optLong("seed", 0L),
                    savedAtMs = obj.optLong("savedAtMs", 0L)
                )
            }.getOrNull()
        }
    }
}

data class Restored(val model: QueueModel, val positionMs: Long, val startPaused: Boolean)

fun restoreQueue(s: QueueSnapshot, availableIds: Set<String>): Restored? {
    val orig = s.original.filter { it in availableIds }
    if (orig.isEmpty()) return null
    val m = QueueModel(orig, null)
    val activeBefore = if (s.shuffleEnabled) s.shuffled else s.original
    val stillThere = s.currentId in availableIds
    val newCurrent = if (stillThere) s.currentId else {
        // nearest surviving track at or after the old index in the old active order, else the last survivor before it
        val i = activeBefore.indexOf(s.currentId).coerceAtLeast(0)
        activeBefore.drop(i).firstOrNull { it in availableIds }
            ?: activeBefore.take(i).lastOrNull { it in availableIds }
            ?: orig.first()
    }
    m.enableShuffleFrom(s.shuffled.filter { it in availableIds }, s.seed, newCurrent)
    if (!s.shuffleEnabled) {
        m.disableShuffle()
    }
    return Restored(m, if (stillThere) s.positionMs else 0L, startPaused = true)
}

fun formatDroppedTracksNotice(droppedCount: Int): String? {
    if (droppedCount <= 0) return null
    val s = if (droppedCount == 1) "" else "s"
    val verb = if (droppedCount == 1) "is" else "are"
    return "$droppedCount song$s in your last queue $verb unavailable"
}

