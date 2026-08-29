package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.data.local.BookmarkDao
import com.jdluu.leafline.library.data.local.BookmarkEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BookmarkRepositoryImpl(
    private val bookmarkDao: BookmarkDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) : BookmarkRepository {

    override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> {
        return bookmarkDao.observeForBook(bookId).map { entities ->
            entities.map { it.toBookmark() }
        }
    }

    override suspend fun getBookmarks(bookId: String): List<Bookmark> = withContext(ioDispatcher) {
        bookmarkDao.getForBook(bookId).map { it.toBookmark() }
    }

    override suspend fun toggleBookmark(
        bookId: String,
        locatorJson: String,
        label: String?
    ): BookmarkToggleResult = withContext(ioDispatcher) {
        val key = LocatorIdentity.key(locatorJson)
        val existing = bookmarkDao.getForBook(bookId)
            .firstOrNull { LocatorIdentity.key(it.locatorJson) == key }
        if (existing != null) {
            bookmarkDao.deleteById(existing.id)
            BookmarkToggleResult.Removed
        } else {
            bookmarkDao.insert(
                BookmarkEntity(
                    bookId = bookId,
                    locatorJson = locatorJson,
                    createdAt = clock(),
                    label = label
                )
            )
            BookmarkToggleResult.Added
        }
    }

    override suspend fun removeBookmark(id: Long) = withContext(ioDispatcher) {
        bookmarkDao.deleteById(id)
        Unit
    }

    override suspend fun removeBookmarksForBook(bookId: String) = withContext(ioDispatcher) {
        bookmarkDao.deleteForBook(bookId)
        Unit
    }
}
