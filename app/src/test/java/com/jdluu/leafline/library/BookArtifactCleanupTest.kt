package com.jdluu.leafline.library

import com.jdluu.leafline.library.cover.CoverCache
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BookArtifactCleanupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val filesDir: File
        get() = tempFolder.root

    private fun testBook(
        stableId: String,
        filePath: String,
        coverPath: String? = null
    ) = LibraryBook(
        stableId = stableId,
        title = "Title",
        authors = emptyList(),
        language = "en",
        description = null,
        publisher = null,
        publishedAtEpochMillis = null,
        filePath = filePath,
        fileHash = "hash-$stableId",
        addedAtEpochMillis = 1_000L,
        pageCount = null,
        coverPath = coverPath
    )

    @Test
    fun `deletes unreferenced internal epub cached cover and evicts memory cache`() = runTest {
        val coverCache = CoverCache(CoverCache.coversDirectory(filesDir))
        val epub = File(filesDir, "book-1.epub").apply { writeText("epub-bytes") }
        val coverPath = coverCache.storeCoverBytes("book-1", byteArrayOf(1, 2, 3))!!
        val evicted = mutableListOf<String>()
        val deleted = testBook("book-1", epub.absolutePath, coverPath)

        BookArtifactCleanup.cleanUp(
            filesDir = filesDir,
            deletedBooks = listOf(deleted),
            remainingBooks = emptyList(),
            evictMemoryCache = { evicted += it }
        )

        assertFalse(epub.exists())
        assertFalse(File(coverPath).exists())
        assertTrue(coverPath in evicted)
    }

    @Test
    fun `keeps shared epub and cover files when referenced by another remaining book`() = runTest {
        val coverCache = CoverCache(CoverCache.coversDirectory(filesDir))
        val sharedEpub = File(filesDir, "shared.epub").apply { writeText("shared-bytes") }
        val coverPath = coverCache.storeCoverBytes("book-1", byteArrayOf(4, 5, 6))!!
        val deleted = testBook("book-1", sharedEpub.absolutePath, coverPath)
        val remaining = testBook("book-2", sharedEpub.absolutePath, coverPath)

        BookArtifactCleanup.cleanUp(
            filesDir = filesDir,
            deletedBooks = listOf(deleted),
            remainingBooks = listOf(remaining)
        )

        assertTrue(sharedEpub.exists())
        assertTrue(File(coverPath).exists())
    }

    @Test
    fun `never deletes epub files outside filesDir`() = runTest {
        val externalDir = tempFolder.newFolder("external")
        val appFilesDir = tempFolder.newFolder("app-files")
        val externalEpub = File(externalDir, "user-book.epub").apply { writeText("keep-me") }
        val deleted = testBook("ext-1", externalEpub.absolutePath)

        BookArtifactCleanup.cleanUp(
            filesDir = appFilesDir,
            deletedBooks = listOf(deleted),
            remainingBooks = emptyList()
        )

        assertTrue(externalEpub.exists())
        assertEquals("keep-me", externalEpub.readText())
    }
}
