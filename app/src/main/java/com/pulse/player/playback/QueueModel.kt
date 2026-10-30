package com.pulse.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlin.random.Random

class QueueModel(original: List<String>, startId: String?) {
    var original: List<String> = original.distinct(); private set          // each track at most once in a queue
    var shuffled: List<String> = emptyList(); private set
    var shuffleEnabled = false; private set
    var seed = 0L; private set
    var currentId: String? = startId ?: original.firstOrNull()
    val active: List<String> get() = if (shuffleEnabled) shuffled else original
    val currentIndex: Int get() = active.indexOf(currentId)

    fun enableShuffle(newSeed: Long) {                                       // deterministic for a given seed
        seed = newSeed; shuffleEnabled = true
        shuffled = listOfNotNull(currentId) + (original - setOfNotNull(currentId)).shuffled(Random(newSeed))
    }

    fun disableShuffle() { shuffleEnabled = false; shuffled = emptyList() }  // current track is found again in `original`; playback never restarts

    fun nextId(repeat: Int): String? {
        val a = active; val i = currentIndex; if (a.isEmpty() || i < 0) return null
        return when {
            repeat == Player.REPEAT_MODE_ONE -> currentId
            i + 1 < a.size -> a[i + 1]
            repeat == Player.REPEAT_MODE_ALL -> a.first()
            else -> null
        }
    }

    fun previousId(): String? = active.getOrNull(currentIndex - 1)

    fun playNext(id: String) {
        original = original.insertAfter(currentId, id)
        if (shuffleEnabled) shuffled = shuffled.insertAfter(currentId, id)
    }

    fun append(id: String): Boolean {
        if (id in original) return false
        original = original + id
        if (shuffleEnabled) shuffled = shuffled + id
        return true
    }

    fun remove(id: String): String? {                                         // returns the new current id when the current track was removed
        val wasCurrent = id == currentId; val idx = currentIndex
        original = original - id; shuffled = shuffled - id
        if (wasCurrent) currentId = active.getOrNull(idx) ?: active.lastOrNull()
        return if (wasCurrent) currentId else null
    }

    fun move(from: Int, to: Int) {
        if (shuffleEnabled) shuffled = shuffled.moved(from, to) else original = original.moved(from, to)
    }

    fun enableShuffleFrom(savedShuffled: List<String>, savedSeed: Long, current: String?) {
        seed = savedSeed
        shuffleEnabled = true
        val inOrig = savedShuffled.distinct().filter { it in original }
        val withoutCurrent = inOrig.filter { it != current }
        val remaining = original - setOfNotNull(current) - withoutCurrent.toSet()
        shuffled = listOfNotNull(current) + withoutCurrent + remaining
        currentId = current
    }

    private fun List<String>.insertAfter(anchor: String?, id: String): List<String> {
        val l = this - id
        val i = l.indexOf(anchor)
        return l.toMutableList().apply { add(if (i < 0) 0 else i + 1, id) }
    }

    private fun List<String>.moved(f: Int, t: Int): List<String> {
        if (f !in indices) return this
        val m = toMutableList()
        val item = m.removeAt(f)
        m.add(t.coerceIn(0, m.size), item)
        return m
    }
}

fun MediaController.reorderAroundCurrent(order: List<MediaItem>, currentId: String) {
    val cur = currentMediaItemIndex
    val idx = order.indexOfFirst { it.mediaId == currentId }
    require(idx >= 0) { "currentId $currentId not found in order" }
    if (cur < 0 || mediaItemCount == 0) {
        setMediaItems(order, idx, currentPosition.coerceAtLeast(0L))
        prepare()
        return
    }
    if (cur < mediaItemCount - 1) removeMediaItems(cur + 1, mediaItemCount)
    if (cur > 0) removeMediaItems(0, cur)
    if (idx > 0) addMediaItems(0, order.subList(0, idx))
    if (idx + 1 < order.size) addMediaItems(order.subList(idx + 1, order.size))      // the playing item is never touched
}

