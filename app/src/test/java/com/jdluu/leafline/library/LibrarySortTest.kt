package com.jdluu.leafline.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySortTest {

    private fun book(
        id: String,
        title: String,
        authors: List<String> = emptyList(),
        addedAt: Long? = null
    ): LibraryBook {
        return LibraryBook(
            stableId = id,
            title = title,
            authors = authors,
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/$id.epub",
            fileHash = "hash-$id",
            addedAtEpochMillis = addedAt,
            pageCount = null
        )
    }

    @Test
    fun `RECENT orders by addedAt descending with nulls last`() {
        val books = listOf(
            book("a", "A", addedAt = 100),
            book("b", "B", addedAt = 300),
            book("c", "C", addedAt = null),
            book("d", "D", addedAt = 200)
        )

        val sorted = LibrarySort.RECENT.sorted(books)

        assertEquals(listOf("b", "d", "a", "c"), sorted.map { it.stableId })
    }

    @Test
    fun `TITLE sorts alphabetically ignoring case`() {
        val books = listOf(
            book("1", "banana"),
            book("2", "Apple"),
            book("3", "cherry")
        )

        val sorted = LibrarySort.TITLE.sorted(books)

        assertEquals(listOf("2", "1", "3"), sorted.map { it.stableId })
    }

    @Test
    fun `AUTHOR sorts by first author then title`() {
        val books = listOf(
            book("1", "Zebra", authors = listOf("Austen")),
            book("2", "Apple", authors = listOf("Dickens")),
            book("3", "Yak", authors = listOf("Austen"))
        )

        val sorted = LibrarySort.AUTHOR.sorted(books)

        assertEquals(listOf("3", "1", "2"), sorted.map { it.stableId })
    }

    @Test
    fun `AUTHOR places books without authors before titled fallback order`() {
        val books = listOf(
            book("1", "Beta", authors = listOf("Author")),
            book("2", "Alpha", authors = emptyList())
        )

        val sorted = LibrarySort.AUTHOR.sorted(books)

        assertEquals(listOf("2", "1"), sorted.map { it.stableId })
    }

    @Test
    fun `sorted does not mutate the original list`() {
        val books = mutableListOf(
            book("1", "B", addedAt = 100),
            book("2", "A", addedAt = 200)
        )

        LibrarySort.TITLE.sorted(books)

        assertEquals(listOf("1", "2"), books.map { it.stableId })
    }

    @Test
    fun `fromNameOrDefault parses stored names`() {
        assertEquals(LibrarySort.TITLE, LibrarySort.fromNameOrDefault("TITLE"))
        assertEquals(LibrarySort.AUTHOR, LibrarySort.fromNameOrDefault("AUTHOR"))
        assertEquals(LibrarySort.RECENT, LibrarySort.fromNameOrDefault("RECENT"))
    }

    @Test
    fun `fromNameOrDefault falls back to RECENT for unknown or missing values`() {
        assertEquals(LibrarySort.RECENT, LibrarySort.fromNameOrDefault(null))
        assertEquals(LibrarySort.RECENT, LibrarySort.fromNameOrDefault(""))
        assertEquals(LibrarySort.RECENT, LibrarySort.fromNameOrDefault("NOPE"))
    }
}
