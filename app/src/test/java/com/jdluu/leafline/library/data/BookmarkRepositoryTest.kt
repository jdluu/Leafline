package com.jdluu.leafline.library.data

import com.google.gson.JsonParser
import com.jdluu.leafline.library.data.local.BookmarkDao
import com.jdluu.leafline.library.data.local.BookmarkEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkRepositoryTest {

    private class FakeBookmarkDao : BookmarkDao {
        val stored = MutableStateFlow<List<BookmarkEntity>>(emptyList())
        private var nextId = 1L

        override fun observeForBook(bookId: String): Flow<List<BookmarkEntity>> {
            return stored.map { list ->
                list.filter { it.bookId == bookId }
                    .sortedWith(
                        compareByDescending<BookmarkEntity> { it.createdAt }
                            .thenByDescending { it.id }
                    )
            }
        }

        override suspend fun getForBook(bookId: String): List<BookmarkEntity> {
            return observeForBook(bookId).first()
        }

        override suspend fun insert(bookmark: BookmarkEntity): Long {
            val id = bookmark.id.takeIf { it != 0L } ?: nextId++
            stored.value = stored.value + bookmark.copy(id = id)
            return id
        }

        override suspend fun deleteById(id: Long): Int {
            val current = stored.value
            val removed = current.count { it.id == id }
            stored.value = current.filterNot { it.id == id }
            return removed
        }

        override suspend fun deleteForBook(bookId: String): Int {
            val current = stored.value
            stored.value = current.filterNot { it.bookId == bookId }
            return current.size - stored.value.size
        }
    }

    private fun newRepository(ticks: MutableList<Long>): Pair<BookmarkRepositoryImpl, FakeBookmarkDao> {
        val dao = FakeBookmarkDao()
        val repository = BookmarkRepositoryImpl(
            bookmarkDao = dao,
            clock = { ticks.removeAt(0) }
        )
        return repository to dao
    }

    private fun clockTicks(): MutableList<Long> {
        return mutableListOf(1000L, 2000L, 3000L, 4000L, 5000L, 6000L, 7000L, 8000L)
    }

    private val chapterOne = """
        {"href": "/OEBPS/chapter01.xhtml", "type": "application/xhtml+xml",
         "title": "Chapter One", "locations": {"progression": 0.5},
         "text": {"after": "some words"}}
    """.trimIndent()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleBookmark inserts bookmark and returns added`() = runTest {
        val (repository, dao) = newRepository(clockTicks())

        val result = repository.toggleBookmark("book-1", chapterOne)

        assertEquals(BookmarkToggleResult.Added, result)
        assertEquals(1, dao.stored.value.size)
        assertEquals("book-1", dao.stored.value[0].bookId)
        assertEquals(chapterOne, dao.stored.value[0].locatorJson)
        assertEquals(1000L, dao.stored.value[0].createdAt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleBookmark twice with same locator removes bookmark`() = runTest {
        val (repository, dao) = newRepository(clockTicks())

        repository.toggleBookmark("book-1", chapterOne)
        val result = repository.toggleBookmark("book-1", chapterOne)

        assertEquals(BookmarkToggleResult.Removed, result)
        assertTrue(dao.stored.value.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleBookmark treats changed title or text as same position`() = runTest {
        val (repository, dao) = newRepository(clockTicks())
        val scrolled = """
            {"href": "/OEBPS/chapter01.xhtml", "type": "application/xhtml+xml",
             "title": "Chapter One Renamed", "locations": {"progression": 0.5},
             "text": {"after": "different words entirely"}}
        """.trimIndent()

        repository.toggleBookmark("book-1", chapterOne)
        val result = repository.toggleBookmark("book-1", scrolled)

        assertEquals(BookmarkToggleResult.Removed, result)
        assertTrue(dao.stored.value.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleBookmark keeps separate bookmarks for different hrefs`() = runTest {
        val (repository, dao) = newRepository(clockTicks())
        val chapterTwo = """
            {"href": "/OEBPS/chapter02.xhtml", "type": "application/xhtml+xml",
             "title": "Chapter Two", "locations": {}, "text": {}}
        """.trimIndent()

        repository.toggleBookmark("book-1", chapterOne)
        val result = repository.toggleBookmark("book-1", chapterTwo)

        assertEquals(BookmarkToggleResult.Added, result)
        assertEquals(2, dao.stored.value.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleBookmark keeps separate bookmarks for different progressions`() = runTest {
        val (repository, dao) = newRepository(clockTicks())
        val later = """
            {"href": "/OEBPS/chapter01.xhtml", "type": "application/xhtml+xml",
             "title": "Chapter One", "locations": {"progression": 0.9}, "text": {}}
        """.trimIndent()

        repository.toggleBookmark("book-1", chapterOne)
        val result = repository.toggleBookmark("book-1", later)

        assertEquals(BookmarkToggleResult.Added, result)
        assertEquals(2, dao.stored.value.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleBookmark isolates bookmarks per book`() = runTest {
        val (repository, dao) = newRepository(clockTicks())

        repository.toggleBookmark("book-1", chapterOne)
        val result = repository.toggleBookmark("book-2", chapterOne)

        assertEquals(BookmarkToggleResult.Added, result)
        assertEquals(2, dao.stored.value.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observeBookmarks returns newest first`() = runTest {
        val (repository, _) = newRepository(clockTicks())

        repository.toggleBookmark("book-1", chapterOne)
        repository.toggleBookmark(
            "book-1",
            """{"href": "/OEBPS/chapter02.xhtml", "locations": {}}""".trimIndent()
        )

        val bookmarks = repository.observeBookmarks("book-1").first()
        assertEquals(2, bookmarks.size)
        assertEquals("/OEBPS/chapter02.xhtml", extractHref(bookmarks[0].locatorJson))
        assertEquals(2000L, bookmarks[0].createdAt)
        assertEquals(1000L, bookmarks[1].createdAt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observeBookmarks breaks createdAt ties by newest insertion`() = runTest {
        val ticks = mutableListOf(1000L, 1000L, 1000L, 1000L)
        val (repository, _) = newRepository(ticks)

        repository.toggleBookmark("book-1", chapterOne)
        repository.toggleBookmark(
            "book-1",
            """{"href": "/OEBPS/chapter02.xhtml", "locations": {}}""".trimIndent()
        )

        val bookmarks = repository.observeBookmarks("book-1").first()
        assertEquals(2, bookmarks.size)
        assertTrue(bookmarks[0].id > bookmarks[1].id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `removeBookmark deletes only the targeted bookmark`() = runTest {
        val (repository, dao) = newRepository(clockTicks())
        repository.toggleBookmark("book-1", chapterOne)
        repository.toggleBookmark(
            "book-1",
            """{"href": "/OEBPS/chapter02.xhtml", "locations": {}}""".trimIndent()
        )
        val first = repository.getBookmarks("book-1")[0]

        repository.removeBookmark(first.id)

        assertEquals(1, dao.stored.value.size)
        assertTrue(repository.getBookmarks("book-1").none { it.id == first.id })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `removeBookmarksForBook leaves other books untouched`() = runTest {
        val (repository, dao) = newRepository(clockTicks())
        repository.toggleBookmark("book-1", chapterOne)
        repository.toggleBookmark("book-2", chapterOne)

        repository.removeBookmarksForBook("book-1")

        assertEquals(1, dao.stored.value.size)
        assertEquals("book-2", dao.stored.value[0].bookId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `label persists through repository mapping`() = runTest {
        val (repository, _) = newRepository(clockTicks())

        repository.toggleBookmark("book-1", chapterOne, label = "Important quote")

        val bookmarks = repository.getBookmarks("book-1")
        assertEquals("Important quote", bookmarks[0].label)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBookmarks returns empty list for unknown book`() = runTest {
        val (repository, _) = newRepository(clockTicks())

        assertTrue(repository.getBookmarks("missing").isEmpty())
    }

    private fun extractHref(locatorJson: String): String {
        return JsonParser.parseString(locatorJson).asJsonObject.get("href").asString
    }
}
