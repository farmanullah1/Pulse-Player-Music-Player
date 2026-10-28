package com.pulse.player.data.backup

import android.util.JsonReader
import android.util.JsonToken
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

fun readValue(r: JsonReader): Any? = when (r.peek()) {
    JsonToken.NULL -> {
        r.nextNull()
        null
    }
    JsonToken.BOOLEAN -> r.nextBoolean()
    JsonToken.NUMBER -> r.nextString().let { it.toLongOrNull() ?: it.toDoubleOrNull() }
    JsonToken.STRING -> r.nextString()
    JsonToken.BEGIN_ARRAY -> {
        val list = ArrayList<Any?>()
        r.beginArray()
        while (r.hasNext()) list += readValue(r)
        r.endArray()
        list
    }
    JsonToken.BEGIN_OBJECT -> {
        val map = LinkedHashMap<String, Any?>()
        r.beginObject()
        while (r.hasNext()) map[r.nextName()] = readValue(r)
        r.endObject()
        map
    }
    else -> {
        r.skipValue()
        null
    }
}

/** Calls [onItem] for each object of a top-level JSON array, one at a time. Throws on non-array input. */
fun InputStream.forEachObject(onItem: (Map<String, Any?>) -> Unit): Int {
    var count = 0
    JsonReader(InputStreamReader(this, Charsets.UTF_8)).use { r ->
        r.beginArray()
        while (r.hasNext()) {
            @Suppress("UNCHECKED_CAST")
            val item = readValue(r) as? Map<String, Any?> ?: throw IOException("Unexpected array item")
            onItem(item)
            count++
        }
        r.endArray()
    }
    return count
}

fun InputStream.readObject(): Map<String, Any?> =
    JsonReader(InputStreamReader(this, Charsets.UTF_8)).use {
        @Suppress("UNCHECKED_CAST")
        (readValue(it) as? Map<String, Any?> ?: throw IOException("Expected a JSON object"))
    }

fun Map<String, Any?>.str(k: String): String? = this[k] as? String
fun Map<String, Any?>.int(k: String, d: Int = 0): Int = (this[k] as? Number)?.toInt() ?: d
fun Map<String, Any?>.lng(k: String, d: Long = 0): Long = (this[k] as? Number)?.toLong() ?: d

