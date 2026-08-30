package com.jdluu.leafline.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ImportedBookReconcilerTest {
    @Test
    fun `matching content updates location while preserving reading state`() {
        val existing = book(
            stableId = "legacy-id",
            filePath = "/old/book.epub",
            lastLocatorJson = "{locator}",
            readingStatus = ReadingStatus.READING,
            coverPath = "/covers/existing.cover"
        )
        val imported = book(
            stableId = "identifier:new:hash:same",
            filePath = "/new/book.epub",
            coverPath = null
        )

        val result = ImportedBookReconciler.merge(existing, imported)

        assertNotNull(result)
        assertEquals("legacy-id", result?.stableId)
        assertEquals("/new/book.epub", result?.filePath)
        assertEquals("{locator}", result?.lastLocatorJson)
        assertEquals(ReadingStatus.READING, result?.readingStatus)
        assertEquals("/covers/existing.cover", result?.coverPath)
    }

    @Test
    fun `different content is not reconciled`() {
        val existing = book(stableId = "existing", fileHash = "old-hash")
        val imported = book(stableId = "imported", fileHash = "new-hash")

        assertEquals(null, ImportedBookReconciler.merge(existing, imported))
    }

    private fun book(
        stableId: String,
        filePath: String = "/book.epub",
        fileHash: String = "same-hash",
        lastLocatorJson: String? = null,
        readingStatus: ReadingStatus = ReadingStatus.UNREAD,
        coverPath: String? = null
    ) = LibraryBook(
        stableId = stableId,
        title = "Book",
        authors = listOf("Author"),
        language = "en",
        description = null,
        publisher = null,
        publishedAtEpochMillis = null,
        filePath = filePath,
        fileHash = fileHash,
        addedAtEpochMillis = 1L,
        pageCount = null,
        coverPath = coverPath,
        lastLocatorJson = lastLocatorJson,
        readingStatus = readingStatus
    )
}
