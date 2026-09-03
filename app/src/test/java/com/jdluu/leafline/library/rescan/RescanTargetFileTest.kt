package com.jdluu.leafline.library.rescan

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RescanTargetFileTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dir: File
        get() = tempFolder.root

    @Test
    fun `free display name path is used as-is`() {
        val target = RescanTargetFile.resolve(dir, "book.epub", "hash-a", emptyMap())
        assertEquals(File(dir, "book.epub"), target)
    }

    @Test
    fun `occupied path with same content hash is reused for unchanged content`() {
        val occupied = mapOf(File(dir, "book.epub").absolutePath to "hash-a")
        val target = RescanTargetFile.resolve(dir, "book.epub", "hash-a", occupied)
        assertEquals(File(dir, "book.epub"), target)
    }

    @Test
    fun `occupied path with different content hash never overwrites and gets a new suffix`() {
        val occupied = mapOf(File(dir, "book.epub").absolutePath to "hash-old")
        val target = RescanTargetFile.resolve(dir, "book.epub", "hash-new", occupied)
        assertEquals(File(dir, "book-2.epub"), target)
        assertFalse(target.exists())
    }

    @Test
    fun `distinct documents with the same display name get distinct deterministic paths`() {
        val occupied = mapOf(File(dir, "book.epub").absolutePath to "hash-a")
        val first = RescanTargetFile.resolve(dir, "book.epub", "hash-b", occupied)
        assertEquals(File(dir, "book-2.epub"), first)

        val occupied2 = occupied + (File(dir, "book-2.epub").absolutePath to "hash-b")
        val second = RescanTargetFile.resolve(dir, "book.epub", "hash-c", occupied2)
        assertEquals(File(dir, "book-3.epub"), second)
    }

    @Test
    fun `changed content keeps the old file referenced path untouched`() {
        val oldPath = File(dir, "book.epub").absolutePath
        val occupied = mapOf(oldPath to "hash-old")
        val target = RescanTargetFile.resolve(dir, "book.epub", "hash-new", occupied)
        assertFalse("old file path must not be reused for changed content", target.absolutePath == oldPath)
        assertEquals(File(dir, "book-2.epub"), target)
    }

    @Test
    fun `path traversal display name cannot escape files dir`() {
        val target = RescanTargetFile.resolve(dir, "../../etc/passwd.epub", "hash-a", emptyMap())
        assertTrue(target.canonicalPath.startsWith(dir.canonicalPath + File.separator))
    }

    @Test
    fun `differently cased names stay distinct on a case-sensitive filesystem`() {
        val occupied = mapOf(File(dir, "book.epub").absolutePath to "hash-a")
        val target = RescanTargetFile.resolve(dir, "Book.epub", "hash-b", occupied)
        assertEquals(File(dir, "Book.epub"), target)
    }

    @Test
    fun `existing untracked file is never overwritten`() {
        val existing = File(dir, "orphan.epub")
        existing.parentFile?.mkdirs()
        existing.writeText("untracked bytes")
        try {
            val target = RescanTargetFile.resolve(dir, "orphan.epub", "hash-new", emptyMap())
            assertEquals(File(dir, "orphan-2.epub"), target)
            assertEquals("untracked bytes", existing.readText())
        } finally {
            existing.delete()
        }
    }
}
