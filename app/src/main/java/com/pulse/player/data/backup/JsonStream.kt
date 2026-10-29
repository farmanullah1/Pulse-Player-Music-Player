package com.pulse.player.data.backup

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.io.InputStream

private fun convertJsonValue(v: Any?): Any? = when (v) {
    null, JSONObject.NULL -> null
    is JSONObject -> {
        val map = LinkedHashMap<String, Any?>()
        val keys = v.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = convertJsonValue(v.opt(key))
        }
        map
    }
    is JSONArray -> {
        val list = ArrayList<Any?>()
        for (i in 0 until v.length()) {
            list += convertJsonValue(v.opt(i))
        }
        list
    }
    else -> v
}

/** Calls [onItem] for each object of a top-level JSON array, one at a time. Throws on non-array input. */
fun InputStream.forEachObject(onItem: (Map<String, Any?>) -> Unit): Int {
    val text = bufferedReader(Charsets.UTF_8).use { it.readText() }
    val tokener = JSONTokener(text)
    val nextVal = try {
        tokener.nextValue()
    } catch (e: Exception) {
        throw IOException("Failed to parse JSON array", e)
    }
    if (nextVal !is JSONArray) throw IOException("Expected a JSON array")
    var count = 0
    for (i in 0 until nextVal.length()) {
        val obj = nextVal.optJSONObject(i) ?: throw IOException("Unexpected array item")
        @Suppress("UNCHECKED_CAST")
        val map = convertJsonValue(obj) as? Map<String, Any?> ?: throw IOException("Unexpected array item")
        onItem(map)
        count++
    }
    return count
}

fun InputStream.readObject(): Map<String, Any?> {
    val text = bufferedReader(Charsets.UTF_8).use { it.readText() }
    val tokener = JSONTokener(text)
    val nextVal = try {
        tokener.nextValue()
    } catch (e: Exception) {
        throw IOException("Failed to parse JSON object", e)
    }
    if (nextVal !is JSONObject) throw IOException("Expected a JSON object")
    @Suppress("UNCHECKED_CAST")
    return convertJsonValue(nextVal) as? Map<String, Any?> ?: throw IOException("Expected a JSON object")
}

fun Map<String, Any?>.str(k: String): String? = this[k] as? String
fun Map<String, Any?>.int(k: String, d: Int = 0): Int = (this[k] as? Number)?.toInt() ?: d
fun Map<String, Any?>.lng(k: String, d: Long = 0): Long = (this[k] as? Number)?.toLong() ?: d
