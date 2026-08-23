package com.jdluu.leafline.library.data

import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun observeBookmarks(bookId: String): Flow<List<Bookmark>>
    suspend fun getBookmarks(bookId: String): List<Bookmark>
    suspend fun toggleBookmark(
        bookId: String,
        locatorJson: String,
        label: String? = null
    ): BookmarkToggleResult
    suspend fun removeBookmark(id: Long)
    suspend fun removeBookmarksForBook(bookId: String)
}
