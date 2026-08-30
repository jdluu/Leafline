package com.jdluu.leafline.library

/** Merges a newly imported copy of an existing EPUB without losing local state. */
object ImportedBookReconciler {
    fun merge(existing: LibraryBook, imported: LibraryBook): LibraryBook? {
        if (existing.fileHash != imported.fileHash) return null

        return imported.copy(
            stableId = existing.stableId,
            addedAtEpochMillis = existing.addedAtEpochMillis,
            lastLocatorJson = existing.lastLocatorJson,
            coverPath = existing.coverPath ?: imported.coverPath,
            koreaderHash = existing.koreaderHash ?: imported.koreaderHash,
            lastReadAtEpochMillis = existing.lastReadAtEpochMillis,
            readingStatus = existing.readingStatus
        )
    }
}

