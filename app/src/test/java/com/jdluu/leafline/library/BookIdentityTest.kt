package com.jdluu.leafline.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BookIdentityTest {
    @Test
    fun `identity combines publication identifier with content hash`() {
        assertEquals(
            "identifier:edition-1:hash:abc123",
            BookIdentity.from(identifier = " edition-1 ", fileHash = "abc123")
        )
    }

    @Test
    fun `identity falls back to content hash when identifier is blank`() {
        assertEquals(
            "hash:abc123",
            BookIdentity.from(identifier = "  ", fileHash = "abc123")
        )
    }

    @Test
    fun `different content with same publication identifier has different identity`() {
        val first = BookIdentity.from(identifier = "edition-1", fileHash = "abc123")
        val second = BookIdentity.from(identifier = "edition-1", fileHash = "def456")

        assertNotEquals(first, second)
    }
}
