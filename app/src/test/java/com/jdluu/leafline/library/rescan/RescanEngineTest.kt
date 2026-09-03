package com.jdluu.leafline.library.rescan

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RescanEngineTest {

    @Test
    fun `unseen file is added and persists`() = runBlocking {
        val added = mutableListOf<LibraryBook>()
        val updated = mutableListOf<LibraryBook>()
        val engine = engine(
            loadExisting = { null },
            persistAdded = { added += it },
            persistUpdated = { updated += it }
        )

        val counts = engine.reconcile(
            listOf(ScannedFile.Imported(book("b1", fileHash = "hash-a")))
        )

        assertEquals(RescanCounts(added = 1), counts)
        assertEquals(1, added.size)
        assertEquals(emptyList<LibraryBook>(), updated)
    }

    @Test
    fun `same content at a different path preserves the existing path and is skipped`() = runBlocking {
        val added = mutableListOf<LibraryBook>()
        val updated = mutableListOf<LibraryBook>()
        val existing = book(
            stableId = "legacy-id",
            filePath = "/old/b.epub",
            fileHash = "hash-a",
            lastLocatorJson = "{locator}",
            readingStatus = ReadingStatus.READING
        )
        val engine = engine(
            loadExisting = { if (it == "hash-a") existing else null },
            persistAdded = { added += it },
            persistUpdated = { updated += it }
        )

        // Identical content hash surfaced under a different display name/path.
        // Importing the same document yields the same title; only the resolved
        // target path differs, which must not retarget the existing record.
        val imported = book("b1", filePath = "/new/b.epub", fileHash = "hash-a")
            .copy(title = existing.title)
        val counts = engine.reconcile(listOf(ScannedFile.Imported(imported)))

        assertEquals(RescanCounts(skipped = 1), counts)
        assertEquals(emptyList<LibraryBook>(), added)
        assertEquals("existing path must not be retargeted by unchanged content", "/old/b.epub", existing.filePath)
        assertEquals(emptyList<LibraryBook>(), updated)
    }

    @Test
    fun `identical existing and merged content is skipped`() = runBlocking {
        var persisted = 0
        val existing = book("b1", filePath = "/b.epub", fileHash = "hash-a")
        val engine = engine(
            loadExisting = { existing },
            persistAdded = { persisted++ },
            persistUpdated = { persisted++ }
        )

        val counts = engine.reconcile(listOf(ScannedFile.Imported(existing)))

        assertEquals(RescanCounts(skipped = 1), counts)
        assertEquals(0, persisted)
    }

    @Test
    fun `genuine metadata update at a different path keeps the existing path`() = runBlocking {
        // Same content hash, but the imported copy carries a real metadata delta
        // (new title) alongside a different path. The record updates its
        // metadata while preserving the existing path: content identity wins.
        val added = mutableListOf<LibraryBook>()
        val updated = mutableListOf<LibraryBook>()
        val existing = book(
            stableId = "b1",
            filePath = "/old/book.epub",
            fileHash = "hash-a"
        ).copy(title = "Old Title")
        val engine = engine(
            loadExisting = { if (it == "hash-a") existing else null },
            persistAdded = { added += it },
            persistUpdated = { updated += it }
        )

        val imported = book("b1", filePath = "/new/book.epub", fileHash = "hash-a")
            .copy(title = "New Title")
        val counts = engine.reconcile(listOf(ScannedFile.Imported(imported)))

        assertEquals(RescanCounts(updated = 1), counts)
        assertEquals(emptyList<LibraryBook>(), added)
        val persisted = updated.single()
        assertEquals("New Title", persisted.title)
        assertEquals("existing path must survive a metadata-only update", "/old/book.epub", persisted.filePath)
        assertEquals("hash-a", persisted.fileHash)
    }

    @Test
    fun `continues after a failed file and counts it`() = runBlocking {
        val added = mutableListOf<LibraryBook>()
        val engine = engine(
            loadExisting = { throw IllegalStateException("lookup blowup") },
            persistAdded = { added += it }
        )

        val counts = engine.reconcile(
            listOf(
                ScannedFile.Imported(book("good", fileHash = "good")),
                ScannedFile.Failed("/bad.epub", "unreadable"),
                ScannedFile.Imported(book("after", fileHash = "after"))
            )
        )

        assertEquals(RescanCounts(failedFiles = 3, added = 0), counts)
        assertEquals(emptyList<LibraryBook>(), added)
    }

    @Test
    fun `does not stop after a persistence failure`() = runBlocking {
        val added = mutableListOf<LibraryBook>()
        var persistCalls = 0
        val engine = engine(
            loadExisting = { null },
            persistAdded = {
                persistCalls++
                if (it.fileHash == "bad") throw IllegalStateException("persist fail")
                added += it
            },
            persistUpdated = {}
        )

        val counts = engine.reconcile(
            listOf(
                ScannedFile.Imported(book("a", fileHash = "bad")),
                ScannedFile.Imported(book("b", fileHash = "ok"))
            )
        )

        assertEquals(2, persistCalls)
        assertEquals(RescanCounts(added = 1, failedFiles = 1), counts)
        assertEquals(listOf("b"), added.map { it.stableId })
    }

    @Test
    fun `changed content is added, not skipped, and keeps the old record coherent`() = runBlocking {
        val filesDir = File("/data/files")
        val oldPath = File(filesDir, "book.epub")
        val existing = book("legacy", filePath = oldPath.absolutePath, fileHash = "old-hash")
        val added = mutableListOf<LibraryBook>()
        val engine = engine(
            loadExisting = { if (it == "new-hash") null else throw AssertionError("unexpected lookup $it") },
            persistAdded = { added += it },
            persistUpdated = {}
        )

        val occupied = mapOf(oldPath.absolutePath to "old-hash")
        val target = RescanTargetFile.resolve(filesDir, "book.epub", "new-hash", occupied)
        val imported = book("new", filePath = target.absolutePath, fileHash = "new-hash")

        val counts = engine.reconcile(listOf(ScannedFile.Imported(imported)))

        assertEquals(RescanCounts(added = 1), counts)
        val persisted = added.single()
        assertEquals("new-hash", persisted.fileHash)
        assertNotEquals("old record path must not be reused after content changed", oldPath.absolutePath, persisted.filePath)
        assertEquals("legacy", existing.stableId)
        assertEquals("old-hash", existing.fileHash)
    }

    @Test
    fun `name collision does not affect classification`() = runBlocking {
        val added = mutableListOf<LibraryBook>()
        val engine = engine(
            loadExisting = { null },
            persistAdded = { added += it }
        )

        val counts = engine.reconcile(
            listOf(
                ScannedFile.Imported(book("a", fileHash = "h1")),
                ScannedFile.Imported(book("a", fileHash = "h2"))
            )
        )

        assertEquals(RescanCounts(added = 2), counts)
        assertEquals(2, added.distinctBy { it.fileHash }.size)
    }

    @Test
    fun `changed content with a known stableId updates in place, preserves reading state, and adds no duplicate`() = runBlocking {
        val updated = mutableListOf<LibraryBook>()
        val added = mutableListOf<LibraryBook>()
        val prior = book(
            stableId = "stable-id",
            filePath = "/files/book.epub",
            fileHash = "old-hash",
            lastLocatorJson = "{locator}",
            readingStatus = ReadingStatus.READING
        )
        val engine = engine(
            loadExisting = { null },
            persistAdded = { added += it },
            persistUpdated = { updated += it },
            lookupByStableId = { if (it == "stable-id") prior else null }
        )

        // Content changed: new hash, but the same stable identity already exists.
        val imported = book(
            stableId = "stable-id",
            filePath = "/files/book-2.epub",
            fileHash = "new-hash"
        )
        val counts = engine.reconcile(listOf(ScannedFile.Imported(imported)))

        assertEquals(RescanCounts(updated = 1), counts)
        assertEquals(emptyList<LibraryBook>(), added)
        val persisted = updated.single()
        assertEquals("stable-id", persisted.stableId)
        assertEquals("/files/book-2.epub", persisted.filePath)
        assertEquals("new-hash", persisted.fileHash)
        // Reading state survives the content change.
        assertEquals("{locator}", persisted.lastLocatorJson)
        assertEquals(ReadingStatus.READING, persisted.readingStatus)
    }

    @Test
    fun `same content in two scanned occurrences persists as one existing record`() = runBlocking {
        val updated = mutableListOf<LibraryBook>()
        val added = mutableListOf<LibraryBook>()
        val existing = book("b1", filePath = "/files/book.epub", fileHash = "X")
        // Second occurrence resolves to the same path because the shared
        // occupied map pins identical content to the same target.
        val engine = engine(
            loadExisting = { if (it == "X") existing else null },
            persistAdded = { added += it },
            persistUpdated = { updated += it }
        )

        val counts = engine.reconcile(
            listOf(
                ScannedFile.Imported(book("b1", filePath = "/files/book.epub", fileHash = "X")),
                ScannedFile.Imported(book("b1", filePath = "/files/book.epub", fileHash = "X"))
            )
        )

        assertEquals(RescanCounts(skipped = 2), counts)
        assertEquals(emptyList<LibraryBook>(), added)
        assertEquals(emptyList<LibraryBook>(), updated)
    }

    private fun engine(
        loadExisting: suspend (String) -> LibraryBook?,
        persistAdded: suspend (LibraryBook) -> Unit,
        persistUpdated: suspend (LibraryBook) -> Unit = {},
        lookupByStableId: (suspend (String) -> LibraryBook?)? = null
    ) = RescanEngine(
        loadExisting = loadExisting,
        persistAdded = persistAdded,
        persistUpdated = persistUpdated,
        lookupByStableId = lookupByStableId
    )

    private fun book(
        stableId: String,
        filePath: String = "/$stableId.epub",
        fileHash: String = "hash-$stableId",
        lastLocatorJson: String? = null,
        readingStatus: ReadingStatus = ReadingStatus.UNREAD
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
        lastLocatorJson = lastLocatorJson,
        readingStatus = readingStatus
    )
}
