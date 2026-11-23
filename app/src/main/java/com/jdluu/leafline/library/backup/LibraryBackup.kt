package com.jdluu.leafline.library.backup

data class BookBackup(
    val stableId: String,
    val title: String,
    val authors: List<String>,
    val tags: List<String> = emptyList(),
    val rating: Int? = null,
    val readingStatus: String? = null,
    val lastLocatorJson: String? = null
)

data class CollectionBackup(
    val name: String,
    val bookStableIds: List<String> = emptyList()
)

/**
 * One reading session, as it travels in a backup.
 *
 * Identity for de-duplication is the triple (bookStableId, start, end): a session's
 * start and end are recorded once, when the reader stops, so the same session imported
 * twice is byte-identical on those three fields. Its duration and progression are
 * carried but not compared, so a file written by a slightly different build cannot
 * resurrect the same hour twice.
 */
data class ReadingSessionBackup(
    val bookStableId: String,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long,
    val activeDurationMillis: Long,
    val startProgression: Double? = null,
    val endProgression: Double? = null,
    val startLocatorJson: String? = null,
    val endLocatorJson: String? = null
) {
    /** The de-duplication key; see the class comment. */
    val identity: Triple<String, Long, Long>
        get() = Triple(bookStableId, startTimeEpochMillis, endTimeEpochMillis)
}

data class BookmarkBackup(
    val bookStableId: String,
    val locatorJson: String,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AnnotationBackup(
    val bookStableId: String,
    val locatorJson: String,
    val colorHex: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class LibraryBackup(
    val version: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val books: List<BookBackup> = emptyList(),
    val collections: List<CollectionBackup> = emptyList(),
    val bookmarks: List<BookmarkBackup> = emptyList(),
    val annotations: List<AnnotationBackup> = emptyList(),
    /**
     * Per-book Hardcover choices, keyed by stable id, valued "TRACK" or "IGNORE".
     *
     * Added in version 2. A version 1 file simply has no entry here, so restoring an
     * older backup leaves these decisions alone rather than clearing them.
     */
    val hardcoverTracking: Map<String, String> = emptyMap(),
    /**
     * Reading sessions, so statistics survive a move between devices. Added in version
     * 3; a version 1 or 2 file has none and restores as before.
     */
    val readingSessions: List<ReadingSessionBackup> = emptyList()
)

data class BackupRestoreSummary(
    val booksMatched: Int = 0,
    val tagsUpdated: Int = 0,
    val collectionsRestored: Int = 0,
    val bookmarksRestored: Int = 0,
    val annotationsRestored: Int = 0,
    val hardcoverChoicesRestored: Int = 0,
    val readingSessionsRestored: Int = 0,
    val readingSessionsSkipped: Int = 0
)
