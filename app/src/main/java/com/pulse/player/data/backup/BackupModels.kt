package com.pulse.player.data.backup

enum class BackupCategory(val id: String, val displayName: String, val internalPath: String) {
    FAVORITES("favorites", "Favourites", "metadata/favorites.json"),
    PLAYLISTS("playlists", "Playlists & Queue", "metadata/playlists.json"),
    STATISTICS("statistics", "Playback Records & Recap", "metadata/listening_statistics.json"),
    SETTINGS("settings", "App Settings & Skins", "metadata/settings.json"),
    MUSIC_TAGS("music_tags", "Track Metadata & Tags", "metadata/music_tags.json")
}

enum class RestoreConflictMode {
    MERGE,
    REPLACE_SELECTED_CATEGORIES
}

data class ArchiveCategoryInfo(
    val included: Boolean,
    val path: String? = null,
    val recordCount: Long = 0,
    val required: Boolean = false
)

data class ArchiveManifest(
    val formatId: String = FORMAT_ID,
    val formatVersion: Int = CURRENT_VERSION,
    val appName: String = "Pulse Player",
    val appVersionName: String? = "1.0.0",
    val appVersionCode: Long? = 1L,
    val createdAtUtc: String,
    val backupMode: String = "MANUAL_GLOBAL",
    val backupScope: String = "GLOBAL",
    val containsAudioFiles: Boolean = false,
    val deviceLabel: String = "Android Device",
    val counts: Map<String, Long> = emptyMap(),
    val categories: Map<String, ArchiveCategoryInfo> = emptyMap(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        const val FORMAT_ID = "pulse-player-archive-backup"
        const val CURRENT_VERSION = 1
        const val MINIMUM_RESTORE_VERSION = 1
    }
}

data class RestorePreview(
    val fileName: String,
    val manifest: ArchiveManifest,
    val isValid: Boolean,
    val validationMessage: String,
    val detectedFormat: String,
    val checksumsVerified: Boolean,
    val totalUncompressedBytes: Long,
    val entryCount: Int,
    val warnings: List<String>
)

data class RestoreReport(
    val success: Boolean,
    val fileName: String,
    val conflictMode: RestoreConflictMode,
    val importedCount: Map<String, Int>,
    val updatedCount: Map<String, Int>,
    val skippedCount: Map<String, Int>,
    val unmatchedTracks: Int,
    val totalRestored: Int,
    val warnings: List<String>,
    val errors: List<String>
)

data class BackupOperationState(
    val inProgress: Boolean = false,
    val stage: String = "",
    val progressFraction: Float = 0f,
    val completedMessage: String? = null,
    val errorMessage: String? = null
)

data class LastBackupRecord(
    val timestampMs: Long,
    val fileName: String,
    val fileSizeBytes: Long,
    val recordCounts: Map<String, Long>
)
