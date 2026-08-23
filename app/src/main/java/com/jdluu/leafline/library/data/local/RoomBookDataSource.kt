package com.jdluu.leafline.library.data.local

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.BookDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomBookDataSource(
    private val database: LeaflineDatabase
) : BookDataSource {

    override fun getAllBooks(): Flow<List<LibraryBook>> {
        return database.bookDao().getAllBooks().map { entities ->
            entities.map { BookEntity.toLibraryBook(it) }
        }
    }

    override suspend fun getBookByStableId(stableId: String): LibraryBook? {
        return database.bookDao().getBookByStableId(stableId)?.let { BookEntity.toLibraryBook(it) }
    }

    override suspend fun getBookByFileHash(fileHash: String): LibraryBook? {
        return database.bookDao().getBookByFileHash(fileHash)?.let { BookEntity.toLibraryBook(it) }
    }

    override suspend fun getBookLocatorByFilePath(filePath: String): Pair<String, String?>? {
        return database.bookDao().getBookByFilePath(filePath)?.let { it.stableId to it.lastLocatorJson }
    }

    override suspend fun saveLastLocator(stableId: String, locatorJson: String?) {
        database.bookDao().updateLastLocator(stableId, locatorJson)
    }

    override suspend fun setCoverPath(stableId: String, coverPath: String?) {
        database.bookDao().setCoverPath(stableId, coverPath)
    }

    override suspend fun insert(book: LibraryBook): Long {
        return database.bookDao().insert(BookEntity.fromLibraryBook(book))
    }

    override suspend fun update(book: LibraryBook) {
        database.bookDao().update(BookEntity.fromLibraryBook(book))
    }

    override suspend fun delete(stableId: String) {
        database.bookDao().delete(stableId)
    }
}