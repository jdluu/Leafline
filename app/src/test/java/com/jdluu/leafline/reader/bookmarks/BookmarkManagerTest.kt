package com.jdluu.leafline.reader.bookmarks

import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.library.data.BookmarkRepository
import com.jdluu.leafline.library.data.BookmarkToggleResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkManagerTest {

    private class FakeBookmarkRepository : BookmarkRepository {
        var recordedLabel: String? = "unset"
        override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> = flowOf(emptyList())
        override suspend fun getBookmarks(bookId: String): List<Bookmark> = emptyList()
        override suspend fun toggleBookmark(
            bookId: String,
            locatorJson: String,
            label: String?
        ): BookmarkToggleResult {
            recordedLabel = label
            return BookmarkToggleResult.Added
        }
        override suspend fun removeBookmark(id: Long) = Unit
        override suspend fun removeBookmarksForBook(bookId: String) = Unit
    }

    private fun manager(repository: BookmarkRepository, scope: kotlinx.coroutines.CoroutineScope) =
        BookmarkManager(
            scope = scope,
            repository = repository,
            navigatorLocator = { """{"href":"/OEBPS/ch1.xhtml"}""" },
            navigatorProvider = { null }
        )

    @Test
    fun `toggle forwards the chapter title as the bookmark label`() = runTest {
        val repository = FakeBookmarkRepository()

        manager(repository, this).toggle("book-id", "CHAPTER 1. Loomings.")
        advanceUntilIdle()

        assertEquals("CHAPTER 1. Loomings.", repository.recordedLabel)
    }

    @Test
    fun `a blank label is stored as null rather than an empty string`() = runTest {
        val repository = FakeBookmarkRepository()

        manager(repository, this).toggle("book-id", "   ")
        advanceUntilIdle()

        assertNull(repository.recordedLabel)
    }

    @Test
    fun `omitting the label still toggles with no label`() = runTest {
        val repository = FakeBookmarkRepository()

        manager(repository, this).toggle("book-id")
        advanceUntilIdle()

        assertNull(repository.recordedLabel)
    }

    @Test
    fun `a missing locator does not touch the repository`() = runTest {
        val repository = FakeBookmarkRepository()
        val withoutLocator = BookmarkManager(
            scope = this,
            repository = repository,
            navigatorLocator = { null },
            navigatorProvider = { null }
        )

        withoutLocator.toggle("book-id", "CHAPTER 1. Loomings.")
        advanceUntilIdle()

        assertEquals("unset", repository.recordedLabel)
    }
}
