package com.jdluu.leafline.library.backup

import com.jdluu.leafline.library.data.local.AnnotationDao
import com.jdluu.leafline.library.data.local.AnnotationEntity
import com.jdluu.leafline.library.data.local.BookCollectionCrossRef
import com.jdluu.leafline.library.data.local.BookDao
import com.jdluu.leafline.library.data.local.BookmarkDao
import com.jdluu.leafline.library.data.local.BookmarkEntity
import com.jdluu.leafline.library.data.local.CollectionDao
import com.jdluu.leafline.library.data.local.CollectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class LibraryBackupManager(
    private val bookDao: BookDao,
    private val collectionDao: CollectionDao,
    private val bookmarkDao: BookmarkDao,
    private val annotationDao: AnnotationDao
) {

    suspend fun createBackup(): LibraryBackup = withContext(Dispatchers.IO) {
        val books = bookDao.getAllBooksList().map { entity ->
            BookBackup(
                stableId = entity.stableId,
                title = entity.title,
                authors = entity.authors,
                tags = entity.tags,
                rating = entity.rating,
                readingStatus = entity.readingStatus,
                lastLocatorJson = entity.lastLocatorJson
            )
        }

        val collections = collectionDao.getAllCollectionsList().map { col ->
            val bookIds = collectionDao.getBookIdsForCollection(col.id).first()
            CollectionBackup(
                name = col.name,
                bookStableIds = bookIds
            )
        }

        val bookmarks = bookmarkDao.getAllBookmarks().map { bm ->
            BookmarkBackup(
                bookStableId = bm.bookId,
                locatorJson = bm.locatorJson,
                label = bm.label,
                createdAt = bm.createdAt
            )
        }

        val annotations = annotationDao.getAllAnnotations().map { ann ->
            AnnotationBackup(
                bookStableId = ann.bookId,
                locatorJson = ann.locatorJson,
                colorHex = ann.colorHex,
                note = ann.note,
                createdAt = ann.createdAt
            )
        }

        LibraryBackup(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            appVersion = "1.0",
            books = books,
            collections = collections,
            bookmarks = bookmarks,
            annotations = annotations
        )
    }

    suspend fun exportBackup(outputStream: OutputStream): Unit = withContext(Dispatchers.IO) {
        val backup = createBackup()
        val json = LibraryBackupSerializer.serialize(backup)
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json)
            writer.flush()
        }
    }

    suspend fun restoreBackup(backup: LibraryBackup): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val currentBooks = bookDao.getAllBooksList()
        val bookByStableId = currentBooks.associateBy { it.stableId }.toMutableMap()
        val bookByTitleAuthors = currentBooks.associateBy { "${it.title.trim().lowercase()}|${it.authors.joinToString(",").trim().lowercase()}" }

        var booksMatched = 0
        var tagsUpdated = 0
        var collectionsRestored = 0
        var bookmarksRestored = 0
        var annotationsRestored = 0

        // Map backup stable IDs to actual current library stable IDs
        val backupToCurrentIdMap = mutableMapOf<String, String>()

        for (backupBook in backup.books) {
            val localBook = bookByStableId[backupBook.stableId]
                ?: bookByTitleAuthors["${backupBook.title.trim().lowercase()}|${backupBook.authors.joinToString(",").trim().lowercase()}"]

            if (localBook != null) {
                booksMatched++
                backupToCurrentIdMap[backupBook.stableId] = localBook.stableId

                // Merge tags
                if (backupBook.tags.isNotEmpty()) {
                    val mergedTags = (localBook.tags + backupBook.tags).distinct()
                    if (mergedTags.size != localBook.tags.size) {
                        bookDao.setTags(localBook.stableId, mergedTags)
                        tagsUpdated++
                    }
                }

                // Restore rating if not set locally
                if (localBook.rating == null && backupBook.rating != null) {
                    bookDao.setRating(localBook.stableId, backupBook.rating)
                }

                // Restore reading status if not set locally
                if ((localBook.readingStatus.isBlank() || localBook.readingStatus == "unread") && !backupBook.readingStatus.isNullOrBlank()) {
                    bookDao.setReadingStatus(localBook.stableId, backupBook.readingStatus)
                }

                // Restore last reading locator if not present locally
                if (localBook.lastLocatorJson == null && backupBook.lastLocatorJson != null) {
                    bookDao.updateLastLocator(localBook.stableId, backupBook.lastLocatorJson, System.currentTimeMillis())
                }
            }
        }

        // Restore Collections
        for (colBackup in backup.collections) {
            val existingCol = collectionDao.getCollectionByName(colBackup.name)
            val collectionId = existingCol?.id ?: collectionDao.insert(
                CollectionEntity(name = colBackup.name, createdAtEpochMillis = System.currentTimeMillis())
            )
            collectionsRestored++

            for (backupBookId in colBackup.bookStableIds) {
                val currentBookId = backupToCurrentIdMap[backupBookId]
                if (currentBookId != null) {
                    collectionDao.addBookToCollection(
                        BookCollectionCrossRef(
                            bookStableId = currentBookId,
                            collectionId = collectionId
                        )
                    )
                }
            }
        }

        // Restore Bookmarks
        for (bmBackup in backup.bookmarks) {
            val currentBookId = backupToCurrentIdMap[bmBackup.bookStableId]
            if (currentBookId != null) {
                val existingBookmarks = bookmarkDao.getForBook(currentBookId)
                val alreadyExists = existingBookmarks.any { it.locatorJson == bmBackup.locatorJson }
                if (!alreadyExists) {
                    bookmarkDao.insert(
                        BookmarkEntity(
                            bookId = currentBookId,
                            locatorJson = bmBackup.locatorJson,
                            label = bmBackup.label,
                            createdAt = bmBackup.createdAt
                        )
                    )
                    bookmarksRestored++
                }
            }
        }

        // Restore Annotations
        for (annBackup in backup.annotations) {
            val currentBookId = backupToCurrentIdMap[annBackup.bookStableId]
            if (currentBookId != null) {
                val existingAnnotations = annotationDao.getForBook(currentBookId)
                val alreadyExists = existingAnnotations.any { it.locatorJson == annBackup.locatorJson }
                if (!alreadyExists) {
                    annotationDao.insert(
                        AnnotationEntity(
                            bookId = currentBookId,
                            locatorJson = annBackup.locatorJson,
                            colorHex = annBackup.colorHex,
                            note = annBackup.note,
                            createdAt = annBackup.createdAt
                        )
                    )
                    annotationsRestored++
                }
            }
        }

        BackupRestoreSummary(
            booksMatched = booksMatched,
            tagsUpdated = tagsUpdated,
            collectionsRestored = collectionsRestored,
            bookmarksRestored = bookmarksRestored,
            annotationsRestored = annotationsRestored
        )
    }

    suspend fun importBackup(inputStream: InputStream): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val backup = LibraryBackupSerializer.deserialize(json)
        restoreBackup(backup)
    }
}
