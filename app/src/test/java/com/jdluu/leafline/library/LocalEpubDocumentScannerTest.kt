package com.jdluu.leafline.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalEpubDocumentScannerTest {
    @Test
    fun `EPUB matching is case insensitive`() {
        assertTrue(LocalEpubDocumentScanner.isEpubName("book.epub"))
        assertTrue(LocalEpubDocumentScanner.isEpubName("BOOK.EPUB"))
    }

    @Test
    fun `non EPUB names are ignored`() {
        assertFalse(LocalEpubDocumentScanner.isEpubName("notes.txt"))
        assertFalse(LocalEpubDocumentScanner.isEpubName("book.epub.bak"))
        assertFalse(LocalEpubDocumentScanner.isEpubName(null))
    }
}

