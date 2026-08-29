package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.local.CollectionDao
import com.jdluu.leafline.library.data.local.CollectionEntity
import com.jdluu.leafline.library.data.local.BookCollectionCrossRef
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class LibraryRepositoryImpl(
    private val dataSource: BookDataSource = InMemoryBookDataSource(),
    private val collectionDao: CollectionDao? = null,
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

    override suspend fun getBookByFilePath(filePath: String): LibraryBook? = withContext(ioDispatcher) {
        dataSource.getBookByFilePath(filePath)
    }

    override suspend fun saveLastLocator(stableId: String, locatorJson: String?, readAtEpochMillis: Long?) = withContext(ioDispatcher) {
        dataSource.saveLastLocator(stableId, locatorJson, readAtEpochMillis)
    }

    override suspend fun setKoreaderHash(stableId: String, koreaderHash: String?) = withContext(ioDispatcher) {
        dataSource.setKoreaderHash(stableId, koreaderHash)
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

    // -- Collections --

    override fun getAllCollections(): Flow<List<Collection>> {
        val dao = collectionDao ?: return emptyFlow()
        return dao.getAllCollections().map { entities ->
            entities.map { it.toCollection() }
        }
    }

    override suspend fun createCollection(name: String): Long = withContext(ioDispatcher) {
        val dao = collectionDao ?: return@withContext -1L
        dao.insert(CollectionEntity(name = name, createdAtEpochMillis = System.currentTimeMillis()))
    }

    override suspend fun deleteCollection(id: Long) {
        withContext(ioDispatcher) { collectionDao?.deleteById(id) }
    }

    override suspend fun renameCollection(id: Long, name: String) {
        withContext(ioDispatcher) { collectionDao?.rename(id, name) }
    }

    override suspend fun addBookToCollection(collectionId: Long, bookStableId: String) {
        withContext(ioDispatcher) { collectionDao?.addBookToCollection(BookCollectionCrossRef(collectionId, bookStableId)) }
    }

    override suspend fun removeBookFromCollection(collectionId: Long, bookStableId: String) {
        withContext(ioDispatcher) { collectionDao?.removeBookFromCollection(collectionId, bookStableId) }
    }

    override fun getCollectionsForBook(bookStableId: String): Flow<List<Collection>> {
        val dao = collectionDao ?: return emptyFlow()
        return dao.getCollectionIdsForBook(bookStableId).map { ids ->
            ids.mapNotNull { id -> runCatching { dao.getCollectionById(id) }.getOrNull() }
                .map { it.toCollection() }
        }
    }

    override suspend fun getBookIdsForCollection(collectionId: Long): List<String> = withContext(ioDispatcher) {
        val dao = collectionDao ?: return@withContext emptyList()
        val flow = dao.getBookIdsForCollection(collectionId)
        // We need a snapshot, so collect the first emission
        flow.first()
    }

    // -- Reading status --

    override suspend fun setReadingStatus(stableId: String, status: ReadingStatus) = withContext(ioDispatcher) {
        dataSource.setReadingStatus(stableId, status)
    }

    override fun getBooksByReadingStatus(status: ReadingStatus): Flow<List<LibraryBook>> {
        return dataSource.getBooksByReadingStatus(status)
    }

    override fun getRecentlyReadBooks(limit: Int): Flow<List<LibraryBook>> {
        return dataSource.getRecentlyReadBooks(limit)
    }

    private fun <T> emptyFlow(): Flow<List<T>> = kotlinx.coroutines.flow.flowOf(emptyList())
}