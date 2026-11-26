package com.jdluu.leafline.library

import com.jdluu.leafline.library.cover.CoverCache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deletes local EPUB and cached cover artifacts for removed books when those files
 * live under [filesDir] and are no longer referenced by any remaining library entry.
 */
object BookArtifactCleanup {

    suspend fun cleanUp(
        filesDir: File,
        deletedBooks: List<LibraryBook>,
        remainingBooks: List<LibraryBook>,
        evictMemoryCache: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        if (deletedBooks.isEmpty()) return@withContext

        val coversDir = CoverCache.coversDirectory(filesDir)
        val coverCache = CoverCache(coversDir)
        val filesRoot = filesDir.canonicalFile
        val coversRoot = coversDir.canonicalFile

        val remainingBookIds = remainingBooks.mapTo(mutableSetOf()) { it.stableId }
        val remainingPaths = remainingBooks.mapTo(mutableSetOf()) { it.filePath }
        val remainingCanonicalPaths = remainingBooks.mapNotNullTo(mutableSetOf()) {
            runCatching { File(it.filePath).canonicalPath }.getOrNull()
        }
        val remainingCoverPaths = remainingBooks.mapNotNullTo(mutableSetOf()) { it.coverPath }

        for (deleted in deletedBooks) {
            val coverPath = deleted.coverPath
            if (!coverPath.isNullOrBlank()) {
                evictMemoryCache(coverPath)
                if (coverPath !in remainingCoverPaths) {
                    deleteIfInsideDirectory(File(coverPath), coversRoot)
                }
            }

            if (deleted.stableId !in remainingBookIds) {
                val cachedCoverFile = coverCache.coverFileFor(deleted.stableId)
                evictMemoryCache(cachedCoverFile.absolutePath)
                if (cachedCoverFile.absolutePath !in remainingCoverPaths) {
                    coverCache.deleteCover(deleted.stableId)
                }
            }

            val epubFile = File(deleted.filePath)
            val epubCanonical = runCatching { epubFile.canonicalFile }.getOrNull()
            val stillReferenced = deleted.filePath in remainingPaths ||
                (epubCanonical != null && epubCanonical.path in remainingCanonicalPaths)
            if (!stillReferenced && epubCanonical != null) {
                val insideFilesDir = epubCanonical.toPath().startsWith(filesRoot.toPath()) &&
                    epubCanonical != filesRoot
                val insideCoversDir = epubCanonical.toPath().startsWith(coversRoot.toPath())
                if (insideFilesDir && !insideCoversDir) {
                    runCatching { epubFile.delete() }
                }
            }
        }
    }

    private fun deleteIfInsideDirectory(file: File, rootDir: File) {
        val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return
        if (canonical.toPath().startsWith(rootDir.toPath()) && canonical != rootDir) {
            runCatching { file.delete() }
        }
    }
}
