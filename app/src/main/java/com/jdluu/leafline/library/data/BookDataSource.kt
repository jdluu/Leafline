package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import kotlinx.coroutines.flow.Flow

interface BookDataSource {
    fun getAllBooks(): Flow<List<LibraryBook>>
    fun searchBooks(query: String): Flow<List<LibraryBook>>
    suspend fun getBookByStableId(stableId: String): LibraryBook?
    suspend fun getBookByFileHash(fileHash: String): LibraryBook?
    suspend fun getBookByFilePath(filePath: String): LibraryBook?
    suspend fun saveLastLocator(stableId: String, locatorJson: String?, readAtEpochMillis: Long?)
    suspend fun setKoreaderHash(stableId: String, koreaderHash: String?)
    suspend fun setCoverPath(stableId: String, coverPath: String?)
    suspend fun insert(book: LibraryBook): Long
    suspend fun update(book: LibraryBook)
    suspend fun delete(stableId: String)
    suspend fun setReadingStatus(stableId: String, status: ReadingStatus)
    fun getBooksByReadingStatus(status: ReadingStatus): Flow<List<LibraryBook>>
    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<LibraryBook>>
}