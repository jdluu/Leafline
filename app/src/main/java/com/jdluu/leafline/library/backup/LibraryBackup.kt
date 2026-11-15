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
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val books: List<BookBackup> = emptyList(),
    val collections: List<CollectionBackup> = emptyList(),
    val bookmarks: List<BookmarkBackup> = emptyList(),
    val annotations: List<AnnotationBackup> = emptyList()
)

data class BackupRestoreSummary(
    val booksMatched: Int = 0,
    val tagsUpdated: Int = 0,
    val collectionsRestored: Int = 0,
    val bookmarksRestored: Int = 0,
    val annotationsRestored: Int = 0
)
