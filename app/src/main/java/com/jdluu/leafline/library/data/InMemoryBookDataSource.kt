package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

    override fun searchBooks(query: String): kotlinx.coroutines.flow.Flow<List<LibraryBook>> {
        val normalized = query.trim()
        return _booksFlow.asStateFlow().map { all ->
            if (normalized.isEmpty()) {
                all
            } else {
                val lowerQuery = normalized.lowercase()
                all.filter { book ->
                    book.title.lowercase().contains(lowerQuery) ||
                        book.authors.any { author -> author.lowercase().contains(lowerQuery) }
                }
            }
        }
    }

    private fun refreshFlow() {
        _booksFlow.value = books.values.sortedByDescending { it.addedAtEpochMillis }
    }

    override suspend fun getBookByStableId(stableId: String): LibraryBook? = mutex.withLock {
        books[stableId]
    }

    override suspend fun getBookByFileHash(fileHash: String): LibraryBook? = mutex.withLock {
        books.values.find { it.fileHash == fileHash }
    }

    override suspend fun getBookLocatorByFilePath(filePath: String): Pair<String, String?>? =
        mutex.withLock {
            books.values.find { it.filePath == filePath }?.let { it.stableId to null }
        }

    override suspend fun saveLastLocator(stableId: String, locatorJson: String?) {
        // No-op for the in-memory interim source; locator persistence is Room-only.
    }

    override suspend fun setCoverPath(stableId: String, coverPath: String?) {
        mutex.withLock {
            books[stableId]?.let { current ->
                books[stableId] = current.copy(coverPath = coverPath)
                refreshFlow()
            }
        }
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