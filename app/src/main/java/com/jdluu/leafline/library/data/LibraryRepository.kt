package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import kotlinx.coroutines.flow.Flow

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

    // -- Collections --
    fun getAllCollections(): Flow<List<Collection>>
    suspend fun createCollection(name: String): Long
    suspend fun deleteCollection(id: Long)
    suspend fun renameCollection(id: Long, name: String)
    suspend fun addBookToCollection(collectionId: Long, bookStableId: String)
    suspend fun removeBookFromCollection(collectionId: Long, bookStableId: String)
    fun getCollectionsForBook(bookStableId: String): Flow<List<Collection>>
    suspend fun getBookIdsForCollection(collectionId: Long): List<String>

    // -- Reading status --
    suspend fun setReadingStatus(stableId: String, status: ReadingStatus)
    fun getBooksByReadingStatus(status: ReadingStatus): Flow<List<LibraryBook>>
    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<LibraryBook>>
}