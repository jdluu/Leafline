package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun getAllBooks(): Flow<List<LibraryBook>>
    suspend fun getBookByStableId(stableId: String): LibraryBook?
    suspend fun getBookByFileHash(fileHash: String): LibraryBook?
    suspend fun addBook(book: LibraryBook): Long
    suspend fun updateBook(book: LibraryBook)
    suspend fun deleteBook(stableId: String)
}