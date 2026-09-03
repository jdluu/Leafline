package com.jdluu.leafline.library.rescan

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderRescanCoordinatorTest {

    @Test
    fun `rescan one folder returns counts on success`() = runBlocking {
        val engine = RescanEngine(
            loadExisting = { null },
            persistAdded = {},
            persistUpdated = {}
        )
        val coordinator = FolderRescanCoordinator(engine) { uri, _ ->
            if (uri == "content://books") {
                FolderScan.Scanned(listOf(ScannedFile.Imported(book("a", fileHash = "h1"))))
            } else {
                FolderScan.NoPermission
            }
        }

        val result = coordinator.rescanOne("content://books")

        assertEquals(FolderRescanResult.Success(RescanCounts(added = 1)), result)
    }

    @Test
    fun `missing or revoked permission is a recoverable failure without throwing`() = runBlocking {
        val engine = RescanEngine(
            loadExisting = { null },
            persistAdded = {},
            persistUpdated = {}
        )
        val coordinator = FolderRescanCoordinator(engine) { _, _ -> FolderScan.NoPermission }

        val result = coordinator.rescanOne("content://revoked")

        val failed = result as FolderRescanResult.Failed
        assertEquals("content://revoked", failed.folderUri)
        assertEquals(true, failed.recoverable)
    }

    @Test
    fun `scanner exceptions become recoverable folder failures`() = runBlocking {
        val engine = RescanEngine(
            loadExisting = { null },
            persistAdded = {},
            persistUpdated = {}
        )
        val coordinator = FolderRescanCoordinator(engine) { _, _ ->
            throw SecurityException("no access")
        }

        val result = coordinator.rescanOne("content://boom")

        val failed = result as FolderRescanResult.Failed
        assertEquals("content://boom", failed.folderUri)
        assertEquals(true, failed.recoverable)
    }

    @Test
    fun `rescan all aggregates per-folder results and totals`() = runBlocking {
        val engine = RescanEngine(
            loadExisting = { if (it == "h1") null else book("b", fileHash = it) },
            persistAdded = {},
            persistUpdated = {}
        )
        val coordinator = FolderRescanCoordinator(engine) { uri, _ ->
            when (uri) {
                "content://one" -> FolderScan.Scanned(
                    listOf(ScannedFile.Imported(book("a", fileHash = "h1")))
                )
                "content://two" -> FolderScan.Scanned(
                    listOf(
                        ScannedFile.Imported(book("b", fileHash = "h2")),
                        ScannedFile.Failed("/bad.epub", "bad")
                    )
                )
                else -> FolderScan.NoPermission
            }
        }

        val summary = coordinator.rescanAll(listOf("content://one", "content://two", "content://revoked"))

        assertEquals(3, summary.folderResults.size)
        assertEquals(FolderRescanResult.Success(RescanCounts(added = 1)), summary.folderResults[0])
        assertEquals(FolderRescanResult.Success(RescanCounts(skipped = 1, failedFiles = 1)), summary.folderResults[1])
        val failed = summary.folderResults[2] as FolderRescanResult.Failed
        assertEquals("content://revoked", failed.folderUri)
        assertEquals(true, failed.recoverable)
        assertEquals(RescanCounts(added = 1, skipped = 1, failedFiles = 1), summary.totals)
    }

    @Test
    fun `rescan all over empty list yields empty summary`() = runBlocking {
        val engine = RescanEngine(
            loadExisting = { null },
            persistAdded = {},
            persistUpdated = {}
        )
        val coordinator = FolderRescanCoordinator(engine) { _, _ -> FolderScan.NoPermission }

        val summary = coordinator.rescanAll(emptyList())

        assertEquals(emptyList<FolderRescanResult>(), summary.folderResults)
        assertEquals(RescanCounts(), summary.totals)
    }

    @Test
    fun `same content in two folders shares one occupied path and one persisted record`() = runBlocking {
        // Existing record already owns book.epub with hash X. Both folders
        // contain the identical EPUB (hash X). Resolving within one top-level
        // rescan must keep both occurrences pinned to the same path so the
        // record is not retargeted and the old file is not orphaned.
        val existing = book("b1", filePath = "/files/book.epub", fileHash = "X")
        val added = mutableListOf<LibraryBook>()
        val updated = mutableListOf<LibraryBook>()
        val engine = RescanEngine(
            loadExisting = { if (it == "X") existing else null },
            persistAdded = { added += it },
            persistUpdated = { updated += it }
        )

        val occupied = mutableMapOf("/files/book.epub" to "X")
        val coordinator = FolderRescanCoordinator(engine) { _, occ ->
            val target = RescanTargetFile.resolve(File("/files"), "book.epub", "X", occ)
            FolderScan.Scanned(
                listOf(
                    ScannedFile.Imported(
                        book("b1", filePath = target.absolutePath, fileHash = "X")
                    )
                )
            )
        }

        val summary = coordinator.rescanAll(listOf("content://a", "content://b"), occupied)

        assertEquals(2, summary.folderResults.size)
        summary.folderResults.forEach { result ->
            assertEquals(FolderRescanResult.Success(RescanCounts(skipped = 1)), result)
        }
        assertEquals(emptyList<LibraryBook>(), added)
        assertEquals(emptyList<LibraryBook>(), updated)
        // The single shared occupied path is still the original one.
        assertEquals("/files/book.epub", occupied.keys.single())
    }

    @Test
    fun `each top-level rescan one gets an isolated occupied map`() = runBlocking {
        val engine = RescanEngine(
            loadExisting = { if (it == "X") book("b1", filePath = "/files/book.epub", fileHash = "X") else null },
            persistAdded = {},
            persistUpdated = {}
        )
        val seen = mutableListOf<Map<String, String>>()
        val coordinator = FolderRescanCoordinator(engine) { _, occ ->
            seen += occ
            FolderScan.Scanned(
                listOf(ScannedFile.Imported(book("a", filePath = "/files/book.epub", fileHash = "X")))
            )
        }

        coordinator.rescanOne("content://a")
        coordinator.rescanOne("content://b")

        // Two sequential top-level operations must not share runner state: each
        // gets its own occupied map instance seeded fresh from the library.
        assertEquals(2, seen.size)
        assertTrue("occupied maps must be distinct instances", seen[0] !== seen[1])
    }

    private fun book(
        stableId: String,
        filePath: String = "/$stableId.epub",
        fileHash: String = "hash-$stableId"
    ) = LibraryBook(
        stableId = stableId,
        title = stableId,
        authors = listOf("Author"),
        language = "en",
        description = null,
        publisher = null,
        publishedAtEpochMillis = null,
        filePath = filePath,
        fileHash = fileHash,
        addedAtEpochMillis = 1L,
        pageCount = null,
        coverPath = null,
        koreaderHash = null,
        lastReadAtEpochMillis = null,
        lastLocatorJson = null,
        readingStatus = ReadingStatus.UNREAD
    )
}
