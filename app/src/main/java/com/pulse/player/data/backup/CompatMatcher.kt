package com.pulse.player.data.backup

import com.pulse.player.data.db.TrackEntity
import com.pulse.player.data.scan.norm
import java.text.Normalizer
import java.util.Locale

object PathKey {
    private val volume = Regex("^/(storage/emulated/\\d+|storage/[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}|sdcard|mnt/sdcard)/")

    fun nfc(s: String): String = Normalizer.normalize(s.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
    fun abs(p: String): String = nfc(p)
    fun rel(p: String): String = nfc(volume.replaceFirst(p.trim(), ""))
    fun name(p: String): String = nfc(p.substringAfterLast('/'))
    fun parentName(p: String): String {
        val s = p.trim().split('/')
        return nfc(s.takeLast(2).joinToString("/"))
    }
}

fun unk(a: String?): String = if (a.isNullOrBlank() || a == "<unknown>") "Unknown artist" else a
fun unkAlbum(a: String?): String = if (a.isNullOrBlank() || a == "<unknown>") "Unknown album" else a

data class SrcTag(val path: String, val title: String, val artist: String, val album: String)

sealed interface Resolve {
    data class Hit(val track: TrackEntity, val tier: Int) : Resolve
    data object Ambiguous : Resolve
    data object Missing : Resolve
}

class LibraryIndex(tracks: List<TrackEntity>) {
    private val paths = tracks.filter { !it.dataPath.isNullOrBlank() }
    private val byAbs = paths.groupBy { PathKey.abs(it.dataPath!!) }
    private val byRel = paths.groupBy { PathKey.rel(it.dataPath!!) }
    private val byParent = paths.groupBy { PathKey.parentName(it.dataPath!!) }
    private val byName = paths.groupBy { PathKey.name(it.dataPath!!) }
    private val byMeta = tracks.groupBy { Triple(it.normTitle, it.normArtist, it.normAlbum) }

    fun resolve(srcPath: String, tag: SrcTag?): Resolve {
        val tiers = listOf(
            1 to byAbs[PathKey.abs(srcPath)],
            2 to byRel[PathKey.rel(srcPath)],
            3 to byParent[PathKey.parentName(srcPath)],
            4 to tag?.let { byMeta[Triple(norm(it.title), norm(unk(it.artist)), norm(unkAlbum(it.album)))] },
            5 to byName[PathKey.name(srcPath)]
        )
        for ((tier, list) in tiers) {
            if (list == null || list.isEmpty()) continue
            return if (list.size == 1) Resolve.Hit(list[0], tier) else Resolve.Ambiguous // never guess among multiple matches
        }
        return Resolve.Missing
    }
}

