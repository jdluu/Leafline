package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory data source for LibraryBook.
 *
 * Duplicate Semantics: When inserting a book with an existing stableId,
 * the new book replaces the existing one (upsert behavior).
 */
class InMemoryBookDataSource : BookDataSource {
    private val books = mutableMapOf<String, LibraryBook>()
    private var idCounter = 0L
    private val mutex = Mutex()
    private val _booksFlow = MutableStateFlow<List<LibraryBook>>(emptyList())

    override fun getAllBooks(): kotlinx.coroutines.flow.Flow<List<LibraryBook>> = _booksFlow.asStateFlow()

    private fun refreshFlow() {
        _booksFlow.value = books.values.sortedByDescending { it.addedAtEpochMillis }
    }

    override suspend fun getBookByStableId(stableId: String): LibraryBook? = mutex.withLock {
        books[stableId]
    }

    override suspend fun getBookByFileHash(fileHash: String): LibraryBook? = mutex.withLock {
        books.values.find { it.fileHash == fileHash }
    }

    override suspend fun insert(book: LibraryBook): Long = mutex.withLock {
        val id = idCounter++
        books[book.stableId] = book
        refreshFlow()
        id
    }

    override suspend fun update(book: LibraryBook) = mutex.withLock {
        books[book.stableId] = book
        refreshFlow()
    }

    override suspend fun delete(stableId: String) = mutex.withLock {
        books.remove(stableId)
        refreshFlow()
        Unit
    }
}