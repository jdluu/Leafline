package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryBookDataSource : BookDataSource {
    private val books = mutableMapOf<String, LibraryBook>()
    private val idCounter = 0L
    private val mutex = Mutex()

    override fun getAllBooks(): kotlinx.coroutines.flow.Flow<List<LibraryBook>> = kotlinx.coroutines.flow.flow {
        emit(books.values.sortedByDescending { it.addedAtEpochMillis })
    }

    override suspend fun getBookByStableId(stableId: String): LibraryBook? = mutex.withLock {
        books[stableId]
    }

    override suspend fun getBookByFileHash(fileHash: String): LibraryBook? = mutex.withLock {
        books.values.find { it.fileHash == fileHash }
    }

    override suspend fun insert(book: LibraryBook): Long = mutex.withLock {
        val id = idCounter
        books[book.stableId] = book
        id
    }

    override suspend fun update(book: LibraryBook) = mutex.withLock {
        books[book.stableId] = book
    }

    override suspend fun delete(stableId: String) = mutex.withLock {
        books.remove(stableId)
        Unit
    }
}