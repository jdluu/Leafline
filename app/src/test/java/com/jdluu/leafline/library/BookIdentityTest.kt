package com.jdluu.leafline.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `publication of an identified book is its identifier`() {
        val id = BookIdentity.from(identifier = "edition-1", fileHash = "abc123")

        assertEquals("edition-1", BookIdentity.publicationOf(id))
    }

    @Test
    fun `two files of one publication share a publication but not an id`() {
        val first = BookIdentity.from(identifier = "edition-1", fileHash = "abc123")
        val second = BookIdentity.from(identifier = "edition-1", fileHash = "def456")

        assertNotEquals(first, second)
        assertEquals(BookIdentity.publicationOf(first), BookIdentity.publicationOf(second))
    }

    @Test
    fun `a hash-only identity has no publication`() {
        assertNull(BookIdentity.publicationOf(BookIdentity.from(identifier = null, fileHash = "abc123")))
        assertNull(BookIdentity.publicationOf(BookIdentity.from(identifier = " ", fileHash = "abc123")))
    }

    @Test
    fun `an identifier containing the hash marker is read whole`() {
        // A pathological identifier must not be truncated at an inner ":hash:".
        assertEquals(
            "weird:hash:value",
            BookIdentity.publicationOf(BookIdentity.from(identifier = "weird:hash:value", fileHash = "deadbeef"))
        )
    }
}
