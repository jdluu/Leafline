package com.jdluu.leafline.hardcover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HardcoverIsbnTest {

    @Test
    fun `reads a urn isbn identifier`() {
        assertEquals("9780141439471", HardcoverIsbn.isbn13From("urn:isbn:9780141439471"))
    }

    @Test
    fun `accepts hyphens and an ISBN prefix`() {
        assertEquals("9780141439471", HardcoverIsbn.isbn13From("ISBN 978-0-14-143947-1"))
    }

    @Test
    fun `converts a valid isbn10 to isbn13`() {
        assertEquals("9780141439471", HardcoverIsbn.isbn13From("urn:isbn:0141439475"))
    }

    @Test
    fun `accepts an isbn10 with an X check digit`() {
        // 043942089X is a valid ISBN-10; its ISBN-13 form is 9780439420891.
        assertEquals("9780439420891", HardcoverIsbn.isbn13From("URN:ISBN:043942089X"))
    }

    @Test
    fun `rejects a bad checksum`() {
        assertNull(HardcoverIsbn.isbn13From("urn:isbn:9780141439472"))
    }

    @Test
    fun `rejects a url identifier that is not an isbn`() {
        assertNull(HardcoverIsbn.isbn13From("http://www.gutenberg.org/1952"))
        assertNull(HardcoverIsbn.isbn13From("https://example.com/1234567890123"))
    }

    @Test
    fun `rejects null and blank`() {
        assertNull(HardcoverIsbn.isbn13From(null))
        assertNull(HardcoverIsbn.isbn13From("   "))
    }
}
