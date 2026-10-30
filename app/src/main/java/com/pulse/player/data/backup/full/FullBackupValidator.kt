package com.pulse.player.data.backup.full

import com.pulse.player.data.backup.common.LimitedStream
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object BackupLimits {
    const val MAX_ARCHIVE_BYTES = 200L shl 20        // 200 MB whole file
    const val MAX_ENTRIES = 64
    const val MAX_ENTRY_BYTES = 50L shl 20           // 50 MB uncompressed, per entry
    const val MAX_TOTAL_UNCOMPRESSED = 150L shl 20   // 150 MB
    const val MAX_RATIO = 200                         // uncompressed/compressed (zip-bomb guard)
    const val MAX_JSON_ITEMS = 500_000

    val ALLOWED_NAMES = setOf(
        "metadata.inf",
        "pulse.manifest.json",
        "settings.bak",
        "Covers/MusicCoversBackupConfig.conf",
        "Covers/AlbumCoversBackupConfig.conf",
        "MusicTagsBackupConfig.conf",
        "Lyrics/LyricsBackupConfig.conf",
        "PlayLists/PlayListBackupConfig.conf",
        "SortBackupConfig.conf",
        "ScanHiddenBackupConfig.conf",
        "Statistics/StatisticsSnapshotConfig.json",
        "Statistics/PlaybackRecords.json",
        "PlaybackRecords.json",
        "PlaybackRecords_MF.json",
        "SongCoversBackupConfig.json",
        "AlbumCoversBackupConfig.json",
        "LyricsBackupConfig.json",
        "PlaylistBackupConfig.json",
        "ScanBackupConfig.json",
        "StatisticsSnapshotConfig.json"
    )

    val SNAPSHOT_REGEXES = listOf(
        Regex("^Statistics/snapshot_monthly_(\\d{6})\\.db$"),
        Regex("^StatisticsSnapshot_(\\d{6})\\.db$")
    )

    fun isAllowedName(name: String): Boolean =
        name in ALLOWED_NAMES || SNAPSHOT_REGEXES.any { it.matches(name) }
}

fun validateEntryName(n: String): Boolean =
    n.isNotEmpty() &&
            n.length <= 128 &&
            !n.startsWith("/") &&
            !n.startsWith("\\") &&
            !n.contains("..") &&
            !n.contains('\\') &&
            !n.contains(':') &&
            !n.contains('\u0000') &&
            n.none { it.code < 32 } &&
            !n.endsWith("/")

fun checkSnapshotEntry(zip: ZipFile, e: ZipEntry): Boolean {
    if (e.size !in 16L..BackupLimits.MAX_ENTRY_BYTES) return false
    val head = ByteArray(16)
    val read = runCatching {
        zip.getInputStream(e).use { stream ->
            var n = 0
            while (n < 16) {
                val r = stream.read(head, n, 16 - n)
                if (r < 0) break
                n += r
            }
            n
        }
    }.getOrDefault(-1)
    if (read < 16) return false
    return String(head, Charsets.US_ASCII) == "SQLite format 3\u0000"
}

fun checkSnapshotStream(stream: InputStream, size: Long): Boolean {
    if (size !in 16L..BackupLimits.MAX_ENTRY_BYTES) return false
    val head = ByteArray(16)
    var n = 0
    while (n < 16) {
        val r = stream.read(head, n, 16 - n)
        if (r < 0) break
        n += r
    }
    if (n < 16) return false
    return String(head, Charsets.US_ASCII) == "SQLite format 3\u0000"
}

enum class ValidationTier {
    Reference,
    OwnExport
}

data class ManifestEntry(val name: String, val size: Long, val sha256: String)
data class FullManifest(val format: String, val createdAt: String, val entries: Map<String, ManifestEntry>, val counts: Map<String, Int>)

sealed interface ValidationResult {
    data class Valid(val tier: ValidationTier, val manifest: FullManifest?, val warnings: List<String>) : ValidationResult
    data class Invalid(val reason: String) : ValidationResult
}

object FullBackupValidator {

    fun validate(file: File): ValidationResult {
        // Rule 1: File size <= MAX_ARCHIVE_BYTES; first 4 bytes are PK\x03\x04
        if (file.length() > BackupLimits.MAX_ARCHIVE_BYTES) {
            return ValidationResult.Invalid("Archive exceeds maximum size limit (200 MB).")
        }
        if (file.length() < 4) {
            return ValidationResult.Invalid("File is too small to be a backup archive.")
        }
        val header = ByteArray(4)
        file.inputStream().use {
            if (it.read(header) < 4) return ValidationResult.Invalid("Could not read archive header.")
        }
        if (header[0] != 0x50.toByte() || header[1] != 0x4B.toByte() || header[2] != 0x03.toByte() || header[3] != 0x04.toByte()) {
            return ValidationResult.Invalid("Not a valid ZIP archive (missing PK\\x03\\x04 signature).")
        }

        // Rule 2: Open with ZipFile on temp copy. Entry count <= MAX_ENTRIES.
        val zip = runCatching { ZipFile(file) }.getOrElse {
            return ValidationResult.Invalid("Archive is corrupt or not a valid ZIP file.")
        }
        zip.use { z ->
            val entries = z.entries().toList()
            if (entries.size > BackupLimits.MAX_ENTRIES) {
                return ValidationResult.Invalid("Archive contains too many entries (${entries.size} > ${BackupLimits.MAX_ENTRIES}).")
            }

            var totalUncompressed = 0L
            val entryNames = mutableSetOf<String>()
            val warnings = mutableListOf<String>()

            // Rule 3: Entry names, size limits, compression ratios, running totals
            for (e in entries) {
                val name = e.name
                if (!validateEntryName(name)) {
                    return ValidationResult.Invalid("Invalid entry name in archive: $name")
                }
                if (e.isDirectory) {
                    return ValidationResult.Invalid("Archive must not contain directory entries: $name")
                }
                if (!BackupLimits.isAllowedName(name)) {
                    warnings += "Ignored unknown entry: $name"
                    continue
                }
                entryNames.add(name)

                val uncompressedSize = e.size
                if (uncompressedSize > BackupLimits.MAX_ENTRY_BYTES) {
                    return ValidationResult.Invalid("Entry $name exceeds maximum uncompressed size (50 MB).")
                }
                totalUncompressed += uncompressedSize
                if (totalUncompressed > BackupLimits.MAX_TOTAL_UNCOMPRESSED) {
                    return ValidationResult.Invalid("Total uncompressed size exceeds limit (150 MB).")
                }

                val compressedSize = e.compressedSize
                if (compressedSize > 0 && uncompressedSize > 0) {
                    val ratio = uncompressedSize / compressedSize
                    if (ratio > BackupLimits.MAX_RATIO) {
                        return ValidationResult.Invalid("Compression ratio for $name exceeds safety limit (potential zip-bomb).")
                    }
                }
            }

            // Rule 4: Snapshot entries check
            for (e in entries) {
                if (BackupLimits.SNAPSHOT_REGEXES.any { it.matches(e.name) }) {
                    if (!checkSnapshotEntry(z, e)) {
                        warnings += "Snapshot ${e.name} failed SQLite header validation and was skipped."
                    }
                }
            }

            // Rule 5: CRC check by reading entries
            val entryHashes = mutableMapOf<String, String>()
            for (e in entries) {
                if (e.name !in entryNames) continue
                try {
                    val md = MessageDigest.getInstance("SHA-256")
                    val buf = ByteArray(8192)
                    LimitedStream(z.getInputStream(e), BackupLimits.MAX_ENTRY_BYTES).use { stream ->
                        var read: Int
                        while (stream.read(buf).also { read = it } > 0) {
                            md.update(buf, 0, read)
                        }
                    }
                    entryHashes[e.name] = md.digest().joinToString("") { "%02x".format(it) }
                } catch (ze: ZipException) {
                    return ValidationResult.Invalid("CRC check failed for entry ${e.name}: corrupt archive.")
                } catch (e: Exception) {
                    return ValidationResult.Invalid("Error reading entry ${e.message}")
                }
            }

            // Rule 6: metadata.inf check
            val metaEntry = z.getEntry("metadata.inf")
                ?: return ValidationResult.Invalid("Archive is missing metadata.inf.")
            val metaText = z.getInputStream(metaEntry).use { it.reader(Charsets.UTF_8).readText() }
            val metaLines = metaText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val metaMap = metaLines.mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim() else null
            }.toMap()

            if (!metaMap.containsKey("version") && !metaMap.containsKey("backupTime")) {
                warnings += "metadata.inf missing standard version or backupTime keys."
            }

            // Rule 7: Count sanity for JSON files
            for (e in entries) {
                if (e.name.endsWith(".json") && e.name != "pulse.manifest.json") {
                    val text = z.getInputStream(e).use { it.reader(Charsets.UTF_8).readText() }
                    if (text.contains("\u0000")) {
                        return ValidationResult.Invalid("File ${e.name} contains invalid null character.")
                    }
                    val jsonTrimmed = text.trim()
                    if (jsonTrimmed.startsWith("[")) {
                        val arr = runCatching { JSONArray(jsonTrimmed) }.getOrNull()
                        if (arr != null && arr.length() > BackupLimits.MAX_JSON_ITEMS) {
                            return ValidationResult.Invalid("Array in ${e.name} exceeds maximum item limit.")
                        }
                    }
                }
            }

            // Rule 8: Manifest and tier determination
            val manifestEntry = z.getEntry("pulse.manifest.json")
            if (manifestEntry == null) {
                return ValidationResult.Valid(ValidationTier.Reference, null, warnings)
            }

            // Parse manifest
            val manifestJson = runCatching {
                val raw = z.getInputStream(manifestEntry).use { it.reader(Charsets.UTF_8).readText() }
                JSONObject(raw)
            }.getOrElse {
                return ValidationResult.Invalid("pulse.manifest.json is malformed.")
            }

            val format = manifestJson.optString("format", "")
            val createdAt = manifestJson.optString("createdAt", "")
            val entriesArray = manifestJson.optJSONArray("entries") ?: JSONArray()
            val manifestEntries = mutableMapOf<String, ManifestEntry>()
            for (i in 0 until entriesArray.length()) {
                val obj = entriesArray.optJSONObject(i) ?: continue
                val name = obj.optString("name")
                val size = obj.optLong("size", 0L)
                val sha = obj.optString("sha256", "")
                manifestEntries[name] = ManifestEntry(name, size, sha)
            }

            // Verify SHA-256 for all entries listed in manifest
            for ((name, me) in manifestEntries) {
                val actualSha = entryHashes[name]
                if (actualSha == null || !actualSha.equals(me.sha256, ignoreCase = true)) {
                    return ValidationResult.Invalid("File was modified or damaged: checksum mismatch on $name.")
                }
            }

            val countsJson = manifestJson.optJSONObject("counts")
            val counts = mutableMapOf<String, Int>()
            if (countsJson != null) {
                countsJson.keys().forEach { k -> counts[k] = countsJson.optInt(k, 0) }
            }

            val parsedManifest = FullManifest(format, createdAt, manifestEntries, counts)
            return ValidationResult.Valid(ValidationTier.OwnExport, parsedManifest, warnings)
        }
    }
}

