package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.LibraryBook
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRepositoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBook stores book and returns id`() = runTest {
        val repository = LibraryRepositoryImpl()

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

        val id = repository.addBook(book)

        assertTrue(id >= 0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBookByStableId retrieves book`() = runTest {
        val repository = LibraryRepositoryImpl()

        val book = LibraryBook(
            stableId = "stable-id-1",
            title = "Book One",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/book1.epub",
            fileHash = "hash1",
            addedAtEpochMillis = 1000L,
            pageCount = 50
        )
        repository.addBook(book)

        val result = repository.getBookByStableId("stable-id-1")

        assertNotNull(result)
        assertEquals("Book One", result?.title)
        assertEquals("stable-id-1", result?.stableId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBookByStableId returns null for non-existent id`() = runTest {
        val repository = LibraryRepositoryImpl()

        val result = repository.getBookByStableId("non-existent-id")
        assertNull(result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBooks returns all books ordered by addedAt descending`() = runTest {
        val repository = LibraryRepositoryImpl()

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

        repository.addBook(book1)
        repository.addBook(book2)
        repository.addBook(book3)

        val books = repository.getAllBooks().first()

        assertEquals(3, books.size)
        assertEquals("Book 2", books[0].title)
        assertEquals("Book 3", books[1].title)
        assertEquals("Book 1", books[2].title)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBooks returns empty list initially`() = runTest {
        val repository = LibraryRepositoryImpl()

        val books = repository.getAllBooks().first()
        assertTrue(books.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `updateBook modifies existing book`() = runTest {
        val repository = LibraryRepositoryImpl()

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
        repository.addBook(book)

        val updatedBook = book.copy(title = "Updated Title")
        repository.updateBook(updatedBook)

        val result = repository.getBookByStableId("update-id")
        assertEquals("Updated Title", result?.title)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteBook removes book from database`() = runTest {
        val repository = LibraryRepositoryImpl()

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
        repository.addBook(book)

        repository.deleteBook("delete-id")

        val result = repository.getBookByStableId("delete-id")
        assertNull(result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBookByFileHash retrieves book`() = runTest {
        val repository = LibraryRepositoryImpl()

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
        repository.addBook(book)

        val result = repository.getBookByFileHash("unique-file-hash")

        assertNotNull(result)
        assertEquals("Hash Book", result?.title)
        assertEquals("unique-file-hash", result?.fileHash)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBook with same stableId replaces existing book`() = runTest {
        val repository = LibraryRepositoryImpl()

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
        repository.addBook(book1)

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
        repository.addBook(book2)

        val books = repository.getAllBooks().first()
        assertEquals(1, books.size)
        assertEquals("Replaced", books[0].title)
    }
}