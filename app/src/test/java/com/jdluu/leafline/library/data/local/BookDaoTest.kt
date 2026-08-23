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
class BookDaoTest {

    private lateinit var database: LeaflineDatabase
    private lateinit var bookDao: BookDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bookDao = database.bookDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun entity(
        stableId: String,
        title: String,
        authors: List<String>,
        addedAt: Long? = 1000L
    ): BookEntity {
        return BookEntity(
            stableId = stableId,
            title = title,
            authors = authors,
            language = "en",
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/$stableId.epub",
            fileHash = "hash-$stableId",
            addedAtEpochMillis = addedAt,
            pageCount = null
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun searchBooks_matches_partial_title_case_insensitively() = runTest {
        bookDao.insert(entity(stableId = "book-1", title = "The War of the Worlds", authors = listOf("H. G. Wells")))
        bookDao.insert(entity(stableId = "book-2", title = "Northanger Abbey", authors = listOf("Jane Austen")))

        val results = bookDao.searchBooks("war OF the").first()

        assertEquals(1, results.size)
        assertEquals("book-1", results[0].stableId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun searchBooks_matches_partial_author_case_insensitively() = runTest {
        bookDao.insert(entity(stableId = "book-1", title = "Emma", authors = listOf("Jane Austen")))
        bookDao.insert(entity(stableId = "book-2", title = "The Time Machine", authors = listOf("H. G. Wells")))

        val results = bookDao.searchBooks("austen").first()

        assertEquals(1, results.size)
        assertEquals("book-1", results[0].stableId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun searchBooks_matches_any_author_in_list() = runTest {
        bookDao.insert(
            entity(
                stableId = "book-1",
                title = "Good Omens",
                authors = listOf("Terry Pratchett", "Neil Gaiman")
            )
        )

        val results = bookDao.searchBooks("gaiman").first()

        assertEquals(1, results.size)
        assertEquals("book-1", results[0].stableId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun searchBooks_orders_newest_first() = runTest {
        bookDao.insert(entity(stableId = "book-old", title = "Moby Dick", authors = listOf("Herman Melville"), addedAt = 1000L))
        bookDao.insert(entity(stableId = "book-new", title = "Persuasion", authors = listOf("Jane Austen"), addedAt = 2000L))

        val results = bookDao.searchBooks("").first()

        assertEquals(listOf("book-new", "book-old"), results.map { it.stableId })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun searchBooks_returns_empty_list_when_nothing_matches() = runTest {
        bookDao.insert(entity(stableId = "book-1", title = "Emma", authors = listOf("Jane Austen")))

        val results = bookDao.searchBooks("tolkien").first()

        assertTrue(results.isEmpty())
    }
}
