package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class LibraryRepositoryImpl(
    private val dataSource: BookDataSource = InMemoryBookDataSource(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LibraryRepository {

    override fun getAllBooks(): Flow<List<LibraryBook>> = dataSource.getAllBooks()

    override fun searchBooks(query: String): Flow<List<LibraryBook>> = dataSource.searchBooks(query)

    override suspend fun getBookByStableId(stableId: String): LibraryBook? = withContext(ioDispatcher) {
        dataSource.getBookByStableId(stableId)
    }

    override suspend fun getBookByFileHash(fileHash: String): LibraryBook? = withContext(ioDispatcher) {
        dataSource.getBookByFileHash(fileHash)
    }

    override suspend fun getBookLocatorByFilePath(filePath: String): Pair<String, String?>? = withContext(ioDispatcher) {
        dataSource.getBookLocatorByFilePath(filePath)
    }

    override suspend fun saveLastLocator(stableId: String, locatorJson: String?) = withContext(ioDispatcher) {
        dataSource.saveLastLocator(stableId, locatorJson)
    }

    override suspend fun setCoverPath(stableId: String, coverPath: String?) = withContext(ioDispatcher) {
        dataSource.setCoverPath(stableId, coverPath)
    }

    override suspend fun addBook(book: LibraryBook): Long = withContext(ioDispatcher) {
        dataSource.insert(book)
    }

    override suspend fun updateBook(book: LibraryBook) = withContext(ioDispatcher) {
        dataSource.update(book)
    }

    override suspend fun deleteBook(stableId: String) = withContext(ioDispatcher) {
        dataSource.delete(stableId)
    }
}