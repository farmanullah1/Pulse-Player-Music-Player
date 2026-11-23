package com.pulse.player.library

import com.pulse.player.playback.Track

data class FolderBreadcrumb(
    val label: String,
    val path: String
)

data class FolderTreeNode(
    val path: String,
    val name: String,
    val directCount: Int,
    val totalCount: Int
)

data class FolderTreeState(
    val rootPath: String,
    val currentPath: String,
    val breadcrumbs: List<FolderBreadcrumb>,
    val subfolders: List<FolderTreeNode>,
    val directTracks: List<Track>
)

object FolderTreeBuilder {
    fun normalizeFolder(raw: String): String {
        val clean = raw.trim().replace('\\', '/').trimEnd('/')
        return if (clean.isBlank()) "Music" else clean
    }

    fun commonRoot(paths: Collection<String>): String {
        val normalized = paths.map { normalizeFolder(it) }.distinct()
        if (normalized.isEmpty()) return ""
        if (normalized.size == 1) {
            val single = normalized.first()
            val parent = single.substringBeforeLast('/', "")
            return parent.ifBlank { single }
        }
        val splitPaths = normalized.map { path -> path.split('/').filter { it.isNotEmpty() } }
        val minLen = splitPaths.minOf { it.size }
        val commonSegments = mutableListOf<String>()
        for (i in 0 until minLen) {
            val seg = splitPaths[0][i]
            if (splitPaths.all { it[i].equals(seg, ignoreCase = true) }) {
                commonSegments.add(seg)
            } else {
                break
            }
        }
        if (commonSegments.isEmpty()) return ""
        val prefix = if (normalized.first().startsWith("/")) "/" else ""
        return prefix + commonSegments.joinToString("/")
    }

    fun tracksUnderFolder(tracks: List<Track>, folderPath: String, recursive: Boolean = true): List<Track> {
        val target = normalizeFolder(folderPath)
        val prefix = "$target/"
        return tracks.filter { track ->
            val folder = normalizeFolder(track.folder)
            folder.equals(target, ignoreCase = true) ||
                (recursive && folder.startsWith(prefix, ignoreCase = true))
        }
    }

    fun browseAt(tracks: List<Track>, requestedPath: String?): FolderTreeState {
        if (tracks.isEmpty()) {
            return FolderTreeState(
                rootPath = "",
                currentPath = "",
                breadcrumbs = emptyList(),
                subfolders = emptyList(),
                directTracks = emptyList()
            )
        }

        val normalizedTracks = tracks.map { it to normalizeFolder(it.folder) }
        val rootPath = commonRoot(normalizedTracks.map { it.second })

        val reqNorm = requestedPath?.trim()?.takeIf { it.isNotBlank() }?.let(::normalizeFolder)
        val currentPath = if (reqNorm != null && (
                reqNorm.equals(rootPath, ignoreCase = true) ||
                    reqNorm.startsWith("$rootPath/", ignoreCase = true) ||
                    rootPath.isEmpty()
                )
        ) {
            reqNorm
        } else {
            rootPath
        }

        val currentPrefix = if (currentPath.isEmpty()) "" else "$currentPath/"
        val childCounts = LinkedHashMap<String, Pair<Int, Int>>() // path -> (direct, total)
        val directTracks = mutableListOf<Track>()

        for ((track, folder) in normalizedTracks) {
            if (currentPath.isNotEmpty() && folder.equals(currentPath, ignoreCase = true)) {
                directTracks.add(track)
                continue
            }
            val remainder = when {
                currentPath.isEmpty() -> folder
                folder.startsWith(currentPrefix, ignoreCase = true) -> folder.substring(currentPrefix.length)
                else -> continue
            }.trimStart('/')

            if (remainder.isEmpty()) {
                directTracks.add(track)
            } else {
                val firstChildName = remainder.substringBefore('/')
                val childPath = if (currentPath.isEmpty()) firstChildName else "$currentPath/$firstChildName"
                val prev = childCounts[childPath] ?: (0 to 0)
                val isDirect = !remainder.contains('/')
                childCounts[childPath] = (prev.first + if (isDirect) 1 else 0) to (prev.second + 1)
            }
        }

        val subfolders = childCounts.entries.map { (path, counts) ->
            FolderTreeNode(
                path = path,
                name = path.substringAfterLast('/'),
                directCount = counts.first,
                totalCount = counts.second
            )
        }.sortedBy { it.name.lowercase() }

        val breadcrumbs = buildBreadcrumbs(rootPath, currentPath)

        return FolderTreeState(
            rootPath = rootPath,
            currentPath = currentPath,
            breadcrumbs = breadcrumbs,
            subfolders = subfolders,
            directTracks = directTracks
        )
    }

    private fun buildBreadcrumbs(rootPath: String, currentPath: String): List<FolderBreadcrumb> {
        val result = mutableListOf<FolderBreadcrumb>()
        val rootLabel = rootPath.substringAfterLast('/').ifBlank { "Storage" }
        result.add(FolderBreadcrumb(label = rootLabel, path = rootPath))
        if (currentPath == rootPath || currentPath.isBlank()) return result

        val tail = if (rootPath.isEmpty()) {
            currentPath
        } else {
            currentPath.removePrefix(rootPath).trimStart('/')
        }
        if (tail.isBlank()) return result

        var running = rootPath
        for (segment in tail.split('/').filter { it.isNotBlank() }) {
            running = if (running.isEmpty()) segment else "$running/$segment"
            result.add(FolderBreadcrumb(label = segment, path = running))
        }
        return result
    }
}
