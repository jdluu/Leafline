package com.jdluu.leafline.library

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalEpubScannerTest {
    @Test
    fun `scan returns EPUB files recursively and ignores other formats`() {
        val root = Files.createTempDirectory("leafline-scan").toFile()
        try {
            File(root, "book.epub").writeText("book")
            File(root, "notes.txt").writeText("notes")
            File(root.resolve("nested").apply { mkdirs() }, "nested.EPUB").writeText("nested")

            val result = LocalEpubScanner.scan(root)

            assertEquals(
                listOf(
                    File(root, "book.epub").canonicalFile,
                    File(root, "nested/nested.EPUB").canonicalFile
                ),
                result
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `scan skips unreadable and empty directories without failing`() {
        val root = Files.createTempDirectory("leafline-scan").toFile()
        try {
            File(root, "empty").mkdirs()

            assertEquals(emptyList<File>(), LocalEpubScanner.scan(root.resolve("missing")))
            assertEquals(emptyList<File>(), LocalEpubScanner.scan(root))
        } finally {
            root.deleteRecursively()
        }
    }
}
