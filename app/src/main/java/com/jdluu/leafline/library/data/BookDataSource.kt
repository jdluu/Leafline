package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import kotlinx.coroutines.flow.Flow

interface BookDataSource {
    fun getAllBooks(): Flow<List<LibraryBook>>
    suspend fun getBookByStableId(stableId: String): LibraryBook?
    suspend fun getBookByFileHash(fileHash: String): LibraryBook?
    suspend fun getBookLocatorByFilePath(filePath: String): Pair<String, String?>?
    suspend fun saveLastLocator(stableId: String, locatorJson: String?)
    suspend fun insert(book: LibraryBook): Long
    suspend fun update(book: LibraryBook)
    suspend fun delete(stableId: String)
}