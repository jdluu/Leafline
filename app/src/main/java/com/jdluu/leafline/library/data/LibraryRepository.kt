package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
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
}