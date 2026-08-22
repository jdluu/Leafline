package com.jdluu.leafline

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

class ImportUtilsTest {

    @Test
    fun `sanitizeFileName removes path traversal sequences`() {
        val result = sanitizeFileName("../../../etc/passwd")
        assertFalse("Should not contain path traversal", result.contains(".."))
        assertFalse("Should not contain slash", result.contains("/"))
        assertFalse("Should not start with slash", result.startsWith("/"))
    }

    @Test
    fun `sanitizeFileName replaces forward slashes`() {
        val result = sanitizeFileName("path/to/file.epub")
        assertFalse("Should not contain forward slash", result.contains("/"))
    }

    @Test
    fun `sanitizeFileName replaces backslashes`() {
        val result = sanitizeFileName("path\\to\\file.epub")
        assertFalse("Should not contain backslash", result.contains("\\"))
    }

    @Test
    fun `sanitizeFileName removes null characters`() {
        val result = sanitizeFileName("file\u0000.epub")
        assertFalse("Should not contain null character", result.contains("\u0000"))
    }

    @Test
    fun `sanitizeFileName returns default for blank input`() {
        val result = sanitizeFileName("")
        assertEquals("imported.epub", result)
    }

    @Test
    fun `sanitizeFileName returns default for whitespace only`() {
        val result = sanitizeFileName("   ")
        assertEquals("imported.epub", result)
    }

    @Test
    fun `sanitizeFileName handles normal filename`() {
        val result = sanitizeFileName("my-book.epub")
        assertFalse("Should be a valid filename", result.startsWith("/") || result.startsWith(".."))
    }

    @Test
    fun `sanitizeFileName returns default for path traversal only`() {
        val result = sanitizeFileName("../")
        assertEquals("imported.epub", result)
    }

    @Test
    fun `sanitizeFileName removes control characters`() {
        val result = sanitizeFileName("file\u0001\u0002.epub")
        assertFalse("Should not contain control characters", result.contains("\u0001") || result.contains("\u0002"))
    }
}
