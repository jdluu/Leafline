package com.jdluu.leafline.library

import com.jdluu.leafline.EpubImporter
import com.jdluu.leafline.FileHashUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SafEpubStagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val filesDir: File
        get() = tempFolder.root

    private fun fakeImporter(
        stableIdPrefix: String = "id",
        accept: (File) -> Boolean = { true }
    ): EpubImporter = object : EpubImporter {
        override suspend fun importEpub(file: File, fileHash: String): LibraryBook? {
            if (!accept(file)) return null
            return LibraryBook(
                stableId = "$stableIdPrefix:$fileHash",
                title = file.nameWithoutExtension,
                authors = emptyList(),
                language = "en",
                description = null,
                publisher = null,
                publishedAtEpochMillis = null,
                filePath = file.absolutePath,
                fileHash = fileHash,
                addedAtEpochMillis = 1_000L,
                pageCount = null
            )
        }
    }

    @Test
    fun `stages parses and moves EPUB into free target path`() = runTest {
        val bytes = "book-a-bytes".toByteArray()
        val occupied = mutableMapOf<String, String>()

        val result = SafEpubStager.stageAndImport(
            filesDir = filesDir,
            displayName = "book.epub",
            openStream = { ByteArrayInputStream(bytes) },
            importer = fakeImporter(),
            occupied = occupied
        )

        assertTrue(result is SafStageResult.Imported)
        val book = (result as SafStageResult.Imported).book
        val target = File(filesDir, "book.epub")
        assertEquals(target.absolutePath, book.filePath)
        assertTrue(target.isFile)
        assertEquals("book-a-bytes", target.readText())
        assertEquals(book.fileHash, occupied[target.absolutePath])
        assertEquals(listOf("book.epub"), filesDir.list()?.toList())
    }

    @Test
    fun `colliding display name with different content never overwrites existing file`() = runTest {
        val existing = File(filesDir, "book.epub").apply { writeText("existing-bytes") }
        val existingHash = FileHashUtil.computeSha256(existing)
        val occupied = mutableMapOf(existing.absolutePath to existingHash)

        val result = SafEpubStager.stageAndImport(
            filesDir = filesDir,
            displayName = "book.epub",
            openStream = { ByteArrayInputStream("new-different-bytes".toByteArray()) },
            importer = fakeImporter(),
            occupied = occupied
        )

        assertTrue(result is SafStageResult.Imported)
        val book = (result as SafStageResult.Imported).book
        val suffixed = File(filesDir, "book-2.epub")
        assertEquals(suffixed.absolutePath, book.filePath)
        assertEquals("existing-bytes", existing.readText())
        assertEquals("new-different-bytes", suffixed.readText())
        assertEquals(2, filesDir.list()?.size)
    }

    @Test
    fun `reimporting identical bytes reuses existing path without creating duplicate file`() = runTest {
        val bytes = "same-book-bytes".toByteArray()
        val existing = File(filesDir, "original.epub").apply { writeBytes(bytes) }
        val existingHash = FileHashUtil.computeSha256(existing)
        val occupied = mutableMapOf(existing.absolutePath to existingHash)

        val result = SafEpubStager.stageAndImport(
            filesDir = filesDir,
            displayName = "renamed-copy.epub",
            openStream = { ByteArrayInputStream(bytes) },
            importer = fakeImporter(),
            occupied = occupied
        )

        assertTrue(result is SafStageResult.Imported)
        val book = (result as SafStageResult.Imported).book
        assertEquals(existing.absolutePath, book.filePath)
        assertEquals(listOf("original.epub"), filesDir.list()?.toList())
    }

    @Test
    fun `invalid EPUB deletes temporary staged file and leaves existing files intact`() = runTest {
        val existing = File(filesDir, "book.epub").apply { writeText("original-bytes") }
        val occupied = mutableMapOf(existing.absolutePath to "hash-original")

        val result = SafEpubStager.stageAndImport(
            filesDir = filesDir,
            displayName = "book.epub",
            openStream = { ByteArrayInputStream("corrupt-bytes".toByteArray()) },
            importer = fakeImporter(accept = { false }),
            occupied = occupied
        )

        assertTrue(result is SafStageResult.Failed)
        assertEquals("original-bytes", existing.readText())
        assertEquals(listOf("book.epub"), filesDir.list()?.toList())
    }

    @Test
    fun `empty or null stream cleans up staged temp files`() = runTest {
        val emptyResult = SafEpubStager.stageAndImport(
            filesDir = filesDir,
            displayName = "empty.epub",
            openStream = { ByteArrayInputStream(ByteArray(0)) },
            importer = fakeImporter(),
            occupied = mutableMapOf()
        )
        assertTrue(emptyResult is SafStageResult.Failed)
        assertTrue(filesDir.list().isNullOrEmpty())

        val nullResult = SafEpubStager.stageAndImport(
            filesDir = filesDir,
            displayName = "missing.epub",
            openStream = { null },
            importer = fakeImporter(),
            occupied = mutableMapOf()
        )
        assertTrue(nullResult is SafStageResult.Failed)
        assertTrue(filesDir.list().isNullOrEmpty())
    }

    @Test
    fun `stream read failure deletes staged temp file`() = runTest {
        val throwingStream = object : InputStream() {
            override fun read(): Int = throw IOException("boom")
        }

        val result = SafEpubStager.stageAndImport(
            filesDir = filesDir,
            displayName = "broken.epub",
            openStream = { throwingStream },
            importer = fakeImporter(),
            occupied = mutableMapOf()
        )

        assertTrue(result is SafStageResult.Failed)
        assertTrue(filesDir.list().isNullOrEmpty())
    }

    @Test
    fun `path traversal filename stays inside filesDir`() = runTest {
        val occupied = mutableMapOf<String, String>()

        val result = SafEpubStager.stageAndImport(
            filesDir = filesDir,
            displayName = "../../etc/passwd.epub",
            openStream = { ByteArrayInputStream("safe-bytes".toByteArray()) },
            importer = fakeImporter(),
            occupied = occupied
        )

        assertTrue(result is SafStageResult.Imported)
        val book = (result as SafStageResult.Imported).book
        val target = File(book.filePath)
        assertTrue(target.canonicalPath.startsWith(filesDir.canonicalPath + File.separator))
    }
}
