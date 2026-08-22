package com.jdluu.leafline.library.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.BookDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class RoomBookDataSourceTest {
    private lateinit var database: LeaflineDatabase
    private lateinit var dataSource: BookDataSource
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataSource = RoomBookDataSource(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBook stores book and returns id`() = runTest {
        val book = LibraryBook(
            stableId = "test-id",
            title = "Test Book",
            authors = listOf("Jane Austen"),
            language = "en",
            description = "A test book",
            publisher = "Test Pub",
            publishedAtEpochMillis = 1000L,
            filePath = "/books/test.epub",
            fileHash = "hash123",
            addedAtEpochMillis = 2000L,
            pageCount = 100
        )

        val id = dataSource.insert(book)

        assertTrue(id >= 0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBookByStableId retrieves book`() = runTest {
        val book = LibraryBook(
            stableId = "stable-id-1",
            title = "Book One",
            authors = listOf("Author A", "Author B"),
            language = "en",
            description = "Description",
            publisher = "Publisher",
            publishedAtEpochMillis = 1000L,
            filePath = "/books/book1.epub",
            fileHash = "hash1",
            addedAtEpochMillis = 1000L,
            pageCount = 50
        )
        dataSource.insert(book)

        val result = dataSource.getBookByStableId("stable-id-1")

        assertNotNull(result)
        assertEquals("Book One", result?.title)
        assertEquals("stable-id-1", result?.stableId)
        assertEquals(listOf("Author A", "Author B"), result?.authors)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBookByFileHash retrieves book`() = runTest {
        val book = LibraryBook(
            stableId = "hash-id",
            title = "Hash Book",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/hash.epub",
            fileHash = "unique-file-hash",
            addedAtEpochMillis = 1000L,
            pageCount = null
        )
        dataSource.insert(book)

        val result = dataSource.getBookByFileHash("unique-file-hash")

        assertNotNull(result)
        assertEquals("Hash Book", result?.title)
        assertEquals("unique-file-hash", result?.fileHash)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBooks returns all books ordered by addedAt descending`() = runTest {
        val book1 = LibraryBook(
            stableId = "id1",
            title = "Book 1",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/book1.epub",
            fileHash = "hash1",
            addedAtEpochMillis = 1000L,
            pageCount = null
        )
        val book2 = LibraryBook(
            stableId = "id2",
            title = "Book 2",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/book2.epub",
            fileHash = "hash2",
            addedAtEpochMillis = 3000L,
            pageCount = null
        )
        val book3 = LibraryBook(
            stableId = "id3",
            title = "Book 3",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/book3.epub",
            fileHash = "hash3",
            addedAtEpochMillis = 2000L,
            pageCount = null
        )

        dataSource.insert(book1)
        dataSource.insert(book2)
        dataSource.insert(book3)

        val books = dataSource.getAllBooks().first()

        assertEquals(3, books.size)
        assertEquals("Book 2", books[0].title)
        assertEquals("Book 3", books[1].title)
        assertEquals("Book 1", books[2].title)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBooks returns empty list initially`() = runTest {
        val books = dataSource.getAllBooks().first()
        assertTrue(books.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `updateBook modifies existing book`() = runTest {
        val book = LibraryBook(
            stableId = "update-id",
            title = "Original Title",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/update.epub",
            fileHash = "hash-update",
            addedAtEpochMillis = 1000L,
            pageCount = 50
        )
        dataSource.insert(book)

        val updatedBook = book.copy(title = "Updated Title")
        dataSource.update(updatedBook)

        val result = dataSource.getBookByStableId("update-id")
        assertEquals("Updated Title", result?.title)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteBook removes book`() = runTest {
        val book = LibraryBook(
            stableId = "delete-id",
            title = "Book to Delete",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/delete.epub",
            fileHash = "hash-delete",
            addedAtEpochMillis = 1000L,
            pageCount = 50
        )
        dataSource.insert(book)

        dataSource.delete("delete-id")

        val result = dataSource.getBookByStableId("delete-id")
        assertNull(result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBook with same stableId replaces existing book`() = runTest {
        val book1 = LibraryBook(
            stableId = "replace-id",
            title = "Original",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/original.epub",
            fileHash = "hash-original",
            addedAtEpochMillis = 1000L,
            pageCount = 50
        )
        dataSource.insert(book1)

        val book2 = LibraryBook(
            stableId = "replace-id",
            title = "Replaced",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/replaced.epub",
            fileHash = "hash-replaced",
            addedAtEpochMillis = 2000L,
            pageCount = 100
        )
        dataSource.insert(book2)

        val books = dataSource.getAllBooks().first()
        assertEquals(1, books.size)
        assertEquals("Replaced", books[0].title)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `persistence round trip preserves all fields`() = runTest {
        val book = LibraryBook(
            stableId = "roundtrip-id",
            title = "Round Trip Book",
            authors = listOf("Author One", "Author Two"),
            language = "en-US",
            description = "A detailed description",
            publisher = "Publisher A; Publisher B",
            publishedAtEpochMillis = 1609459200000L,
            filePath = "/books/roundtrip.epub",
            fileHash = "sha256-hash-abc123",
            addedAtEpochMillis = 1609545600000L,
            pageCount = 250
        )
        dataSource.insert(book)

        val result = dataSource.getBookByStableId("roundtrip-id")

        assertNotNull(result)
        assertEquals("roundtrip-id", result?.stableId)
        assertEquals("Round Trip Book", result?.title)
        assertEquals(listOf("Author One", "Author Two"), result?.authors)
        assertEquals("en-US", result?.language)
        assertEquals("A detailed description", result?.description)
        assertEquals("Publisher A; Publisher B", result?.publisher)
        assertEquals(1609459200000L, result?.publishedAtEpochMillis)
        assertEquals("/books/roundtrip.epub", result?.filePath)
        assertEquals("sha256-hash-abc123", result?.fileHash)
        assertEquals(1609545600000L, result?.addedAtEpochMillis)
        assertEquals(250, result?.pageCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `authors with special characters preserved correctly`() = runTest {
        val book = LibraryBook(
            stableId = "special-chars-id",
            title = "Special Characters",
            authors = listOf("O'Brien, John", "Müller, Jürgen", "日本語, 太郎"),
            language = "en",
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/special.epub",
            fileHash = "hash-special",
            addedAtEpochMillis = 1000L,
            pageCount = null
        )
        dataSource.insert(book)

        val result = dataSource.getBookByStableId("special-chars-id")

        assertNotNull(result)
        assertEquals(
            listOf("O'Brien, John", "Müller, Jürgen", "日本語, 太郎"),
            result?.authors
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBooks emits updated list on insert`() = runTest {
        val booksBefore = dataSource.getAllBooks().first()
        assertTrue(booksBefore.isEmpty())

        val book = LibraryBook(
            stableId = "new-id",
            title = "New Book",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/new.epub",
            fileHash = "hash-new",
            addedAtEpochMillis = 1000L,
            pageCount = null
        )
        dataSource.insert(book)

        val booksAfter = dataSource.getAllBooks().first()
        assertEquals(1, booksAfter.size)
        assertEquals("New Book", booksAfter[0].title)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBooks emits updated list on delete`() = runTest {
        val book = LibraryBook(
            stableId = "to-delete",
            title = "To Delete",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/delete.epub",
            fileHash = "hash-delete",
            addedAtEpochMillis = 1000L,
            pageCount = null
        )
        dataSource.insert(book)
        assertEquals(1, dataSource.getAllBooks().first().size)

        dataSource.delete("to-delete")

        val booksAfter = dataSource.getAllBooks().first()
        assertTrue(booksAfter.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBooks emits updated list on update`() = runTest {
        val book = LibraryBook(
            stableId = "update-test",
            title = "Original Title",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/update.epub",
            fileHash = "hash-update",
            addedAtEpochMillis = 1000L,
            pageCount = null
        )
        dataSource.insert(book)

        val updatedBook = book.copy(title = "Updated Title")
        dataSource.update(updatedBook)

        val books = dataSource.getAllBooks().first()
        assertEquals(1, books.size)
        assertEquals("Updated Title", books[0].title)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBookByStableId returns null for non-existent id`() = runTest {
        val result = dataSource.getBookByStableId("non-existent-id")
        assertNull(result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBookByFileHash returns null for non-existent hash`() = runTest {
        val result = dataSource.getBookByFileHash("non-existent-hash")
        assertNull(result)
    }
}