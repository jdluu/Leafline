package com.jdluu.leafline.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySortTest {

    private fun book(
        id: String,
        title: String,
        authors: List<String> = emptyList(),
        addedAt: Long? = null,
        lastReadAt: Long? = null,
        progression: Double? = null
    ): LibraryBook {
        val locatorJson = progression?.let {
            """{"locations":{"progression":$it},"href":"chapter1.html","type":"application/xhtml+xml"}"""
        }
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
            lastReadAtEpochMillis = lastReadAt,
            lastLocatorJson = locatorJson,
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
    fun `RECENT ascending orders by addedAt ascending with nulls last`() {
        val books = listOf(
            book("a", "A", addedAt = 100),
            book("b", "B", addedAt = 300),
            book("c", "C", addedAt = null),
            book("d", "D", addedAt = 200)
        )

        val sorted = LibrarySort.RECENT.sorted(books, ascending = true)

        assertEquals(listOf("a", "d", "b", "c"), sorted.map { it.stableId })
    }

    @Test
    fun `RECENTLY_READ sorts by lastReadAt descending then addedAt descending`() {
        val books = listOf(
            book("1", "A", addedAt = 100, lastReadAt = 500),
            book("2", "B", addedAt = 200, lastReadAt = 900),
            book("3", "C", addedAt = 300, lastReadAt = null),
            book("4", "D", addedAt = 400, lastReadAt = 500)
        )

        val sorted = LibrarySort.RECENTLY_READ.sorted(books)

        // 2 has lastRead 900. 4 and 1 tie at 500, but 4 has addedAt 400 > 100. 3 is null.
        assertEquals(listOf("2", "4", "1", "3"), sorted.map { it.stableId })
    }

    @Test
    fun `PROGRESS sorts by reading progress fraction descending`() {
        val books = listOf(
            book("1", "A", progression = 0.25),
            book("2", "B", progression = 0.90),
            book("3", "C", progression = null),
            book("4", "D", progression = 0.50)
        )

        val sorted = LibrarySort.PROGRESS.sorted(books)

        assertEquals(listOf("2", "4", "1", "3"), sorted.map { it.stableId })
    }

    @Test
    fun `PROGRESS ascending sorts lowest progress first`() {
        val books = listOf(
            book("1", "A", progression = 0.25),
            book("2", "B", progression = 0.90),
            book("3", "C", progression = null),
            book("4", "D", progression = 0.50)
        )

        val sorted = LibrarySort.PROGRESS.sorted(books, ascending = true)

        assertEquals(listOf("3", "1", "4", "2"), sorted.map { it.stableId })
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
    fun `TITLE descending sorts reverse alphabetically`() {
        val books = listOf(
            book("1", "banana"),
            book("2", "Apple"),
            book("3", "cherry")
        )

        val sorted = LibrarySort.TITLE.sorted(books, ascending = false)

        assertEquals(listOf("3", "1", "2"), sorted.map { it.stableId })
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
    fun `RATING sorts by rating descending then addedAt descending`() {
        val books = listOf(
            book("1", "A", addedAt = 100).copy(rating = 3),
            book("2", "B", addedAt = 200).copy(rating = 5),
            book("3", "C", addedAt = 300).copy(rating = null),
            book("4", "D", addedAt = 400).copy(rating = 3)
        )

        val sorted = LibrarySort.RATING.sorted(books)

        assertEquals(listOf("2", "4", "1", "3"), sorted.map { it.stableId })
    }

    @Test
    fun `fromNameOrDefault parses stored names`() {
        assertEquals(LibrarySort.TITLE, LibrarySort.fromNameOrDefault("TITLE"))
        assertEquals(LibrarySort.AUTHOR, LibrarySort.fromNameOrDefault("AUTHOR"))
        assertEquals(LibrarySort.RECENT, LibrarySort.fromNameOrDefault("RECENT"))
        assertEquals(LibrarySort.RECENTLY_READ, LibrarySort.fromNameOrDefault("RECENTLY_READ"))
        assertEquals(LibrarySort.PROGRESS, LibrarySort.fromNameOrDefault("PROGRESS"))
        assertEquals(LibrarySort.RATING, LibrarySort.fromNameOrDefault("RATING"))
    }

    @Test
    fun `fromNameOrDefault falls back to RECENT for unknown or missing values`() {
        assertEquals(LibrarySort.RECENT, LibrarySort.fromNameOrDefault(null))
        assertEquals(LibrarySort.RECENT, LibrarySort.fromNameOrDefault(""))
        assertEquals(LibrarySort.RECENT, LibrarySort.fromNameOrDefault("NOPE"))
    }
}
