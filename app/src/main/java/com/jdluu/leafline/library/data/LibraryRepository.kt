package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface LibraryRepository {
    fun getAllBooks(): Flow<List<LibraryBook>>
    fun searchBooks(query: String): Flow<List<LibraryBook>>
    suspend fun getBookByStableId(stableId: String): LibraryBook?
    suspend fun getBookByFileHash(fileHash: String): LibraryBook?
    suspend fun getBookByFilePath(filePath: String): LibraryBook?
    suspend fun saveLastLocator(stableId: String, locatorJson: String?, readAtEpochMillis: Long?)
    suspend fun setKoreaderHash(stableId: String, koreaderHash: String?)
    suspend fun setCoverPath(stableId: String, coverPath: String?)
    suspend fun addBook(book: LibraryBook): Long
    suspend fun updateBook(book: LibraryBook)
    suspend fun deleteBook(stableId: String)
    suspend fun setRating(stableId: String, rating: Int?)
    suspend fun setTags(stableId: String, tags: List<String>)

    // -- Collections --
    fun getAllCollections(): Flow<List<Collection>>
    suspend fun createCollection(name: String): Long
    suspend fun deleteCollection(id: Long)
    suspend fun renameCollection(id: Long, name: String)
    suspend fun addBookToCollection(collectionId: Long, bookStableId: String)
    suspend fun removeBookFromCollection(collectionId: Long, bookStableId: String)
    fun getCollectionsForBook(bookStableId: String): Flow<List<Collection>>
    suspend fun getBookIdsForCollection(collectionId: Long): List<String>
    fun observeBookIdsForCollection(collectionId: Long): Flow<List<String>> = emptyFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCollectionCounts(): Flow<Map<Long, Int>> =
        getAllCollections().flatMapLatest { collections ->
            if (collections.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(collections.map { col ->
                    observeBookIdsForCollection(col.id).map { ids -> col.id to ids.size }
                }) { pairs ->
                    pairs.toMap()
                }
            }
        }

    // -- Reading status --
    suspend fun setReadingStatus(stableId: String, status: ReadingStatus)
    fun getBooksByReadingStatus(status: ReadingStatus): Flow<List<LibraryBook>>
    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<LibraryBook>>
}
