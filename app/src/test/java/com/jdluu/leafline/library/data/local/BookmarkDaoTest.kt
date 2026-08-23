package com.jdluu.leafline.library.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BookmarkDaoTest {

    private lateinit var database: LeaflineDatabase
    private lateinit var bookmarkDao: BookmarkDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bookmarkDao = database.bookmarkDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun entity(
        id: Long = 0,
        bookId: String = "book-1",
        createdAt: Long = 1000L,
        label: String? = null
    ): BookmarkEntity {
        return BookmarkEntity(
            id = id,
            bookId = bookId,
            locatorJson = """{"href": "/OEBPS/chapter01.xhtml"}""",
            createdAt = createdAt,
            label = label
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun insert_round_trips_all_fields() = runTest {
        bookmarkDao.insert(
            BookmarkEntity(
                bookId = "book-1",
                locatorJson = """{"href": "/a.xhtml", "locations": {"progression": 0.25}}""",
                createdAt = 42L,
                label = "Quote"
            )
        )

        val stored = bookmarkDao.getForBook("book-1")
        assertEquals(1, stored.size)
        assertEquals("book-1", stored[0].bookId)
        assertEquals("""{"href": "/a.xhtml", "locations": {"progression": 0.25}}""", stored[0].locatorJson)
        assertEquals(42L, stored[0].createdAt)
        assertEquals("Quote", stored[0].label)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun insert_assigns_incrementing_ids_when_unset() = runTest {
        val first = bookmarkDao.insert(entity())
        val second = bookmarkDao.insert(entity())

        assertTrue(first > 0)
        assertTrue(second > first)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getForBook_filters_by_book_id_and_orders_newest_first() = runTest {
        bookmarkDao.insert(entity(bookId = "book-1", createdAt = 1000L))
        bookmarkDao.insert(entity(bookId = "book-2", createdAt = 2000L))
        bookmarkDao.insert(entity(bookId = "book-1", createdAt = 3000L))

        val stored = bookmarkDao.getForBook("book-1")

        assertEquals(2, stored.size)
        assertEquals(3000L, stored[0].createdAt)
        assertEquals(1000L, stored[1].createdAt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun observeForBook_emits_current_rows() = runTest {
        bookmarkDao.insert(entity())

        val stored = bookmarkDao.observeForBook("book-1").first()

        assertEquals(1, stored.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun deleteById_removes_only_target_row() = runTest {
        val targetId = bookmarkDao.insert(entity())
        bookmarkDao.insert(entity())

        val removed = bookmarkDao.deleteById(targetId)

        assertEquals(1, removed)
        assertEquals(1, bookmarkDao.getForBook("book-1").size)
        assertTrue(bookmarkDao.getForBook("book-1").none { it.id == targetId })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun deleteById_returns_zero_for_missing_row() = runTest {
        assertEquals(0, bookmarkDao.deleteById(999L))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun deleteForBook_removes_only_that_books_rows() = runTest {
        bookmarkDao.insert(entity(bookId = "book-1"))
        bookmarkDao.insert(entity(bookId = "book-1"))
        bookmarkDao.insert(entity(bookId = "book-2"))

        val removed = bookmarkDao.deleteForBook("book-1")

        assertEquals(2, removed)
        assertTrue(bookmarkDao.getForBook("book-1").isEmpty())
        assertEquals(1, bookmarkDao.getForBook("book-2").size)
    }
}
