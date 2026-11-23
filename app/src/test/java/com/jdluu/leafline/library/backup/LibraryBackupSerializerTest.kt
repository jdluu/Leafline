package com.jdluu.leafline.library.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryBackupSerializerTest {

    @Test
    fun `serialize and deserialize round trip preserves all library data`() {
        val backup = LibraryBackup(
            version = 1,
            exportedAt = 1700000000000L,
            appVersion = "1.0",
            books = listOf(
                BookBackup(
                    stableId = "book-1",
                    title = "The Time Machine",
                    authors = listOf("H.G. Wells"),
                    tags = listOf("Sci-Fi", "Classic"),
                    rating = 5,
                    readingStatus = "READING",
                    lastLocatorJson = """{"href":"chapter1.html","locations":{"progression":0.42}}"""
                ),
                BookBackup(
                    stableId = "book-2",
                    title = "War of the Worlds",
                    authors = listOf("H.G. Wells"),
                    tags = listOf("Sci-Fi"),
                    rating = null,
                    readingStatus = null,
                    lastLocatorJson = null
                )
            ),
            collections = listOf(
                CollectionBackup(
                    name = "Favorites",
                    bookStableIds = listOf("book-1")
                )
            ),
            bookmarks = listOf(
                BookmarkBackup(
                    bookStableId = "book-1",
                    locatorJson = """{"href":"ch1.html"}""",
                    label = "Chapter 1",
                    createdAt = 1700000050000L
                )
            ),
            annotations = listOf(
                AnnotationBackup(
                    bookStableId = "book-1",
                    locatorJson = """{"href":"ch1.html","text":{"exact":"Great quote"}}""",
                    colorHex = "#FFE082",
                    note = "Thought-provoking",
                    createdAt = 1700000060000L
                )
            )
        )

        val json = LibraryBackupSerializer.serialize(backup)
        assertNotNull(json)
        assert(json.contains("The Time Machine"))
        assert(json.contains("Favorites"))

        val restored = LibraryBackupSerializer.deserialize(json)
        assertEquals(1, restored.version)
        assertEquals(1700000000000L, restored.exportedAt)
        assertEquals("1.0", restored.appVersion)

        assertEquals(2, restored.books.size)
        val book1 = restored.books[0]
        assertEquals("book-1", book1.stableId)
        assertEquals("The Time Machine", book1.title)
        assertEquals(listOf("H.G. Wells"), book1.authors)
        assertEquals(listOf("Sci-Fi", "Classic"), book1.tags)
        assertEquals(5, book1.rating)
        assertEquals("READING", book1.readingStatus)
        assertEquals("""{"href":"chapter1.html","locations":{"progression":0.42}}""", book1.lastLocatorJson)

        assertEquals(1, restored.collections.size)
        assertEquals("Favorites", restored.collections[0].name)
        assertEquals(listOf("book-1"), restored.collections[0].bookStableIds)

        assertEquals(1, restored.bookmarks.size)
        assertEquals("book-1", restored.bookmarks[0].bookStableId)
        assertEquals("Chapter 1", restored.bookmarks[0].label)

        assertEquals(1, restored.annotations.size)
        assertEquals("book-1", restored.annotations[0].bookStableId)
        assertEquals("#FFE082", restored.annotations[0].colorHex)
        assertEquals("Thought-provoking", restored.annotations[0].note)
    }

    @Test
    fun `deserialize handles empty or missing sections gracefully`() {
        val emptyJson = """{"version":1,"books":[],"collections":[]}"""
        val backup = LibraryBackupSerializer.deserialize(emptyJson)
        assertEquals(1, backup.version)
        assertEquals(0, backup.books.size)
        assertEquals(0, backup.collections.size)
        assertEquals(0, backup.bookmarks.size)
        assertEquals(0, backup.annotations.size)
    }

    @Test
    fun `per-book hardcover choices survive a round trip`() {
        val backup = LibraryBackup(
            version = 2,
            books = listOf(BookBackup(stableId = "book-1", title = "T", authors = emptyList())),
            hardcoverTracking = mapOf(
                "book-1" to "IGNORE",
                "urn:isbn:9780141439471" to "TRACK"
            )
        )

        val restored = LibraryBackupSerializer.deserialize(LibraryBackupSerializer.serialize(backup))

        assertEquals(
            mapOf("book-1" to "IGNORE", "urn:isbn:9780141439471" to "TRACK"),
            restored.hardcoverTracking
        )
    }

    @Test
    fun `a version 1 file with no choices still loads`() {
        val v1 = """
            {"version":1,"exportedAt":1700000000000,"appVersion":"1.0",
             "books":[{"stableId":"book-1","title":"T","authors":[]}],
             "collections":[],"bookmarks":[],"annotations":[]}
        """.trimIndent()

        val restored = LibraryBackupSerializer.deserialize(v1)

        assertEquals(1, restored.version)
        assertEquals(1, restored.books.size)
        assertTrue(
            "a v1 file must not invent choices",
            restored.hardcoverTracking.isEmpty()
        )
    }
}
