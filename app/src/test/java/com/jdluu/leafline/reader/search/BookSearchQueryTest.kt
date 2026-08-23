package com.jdluu.leafline.reader.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookSearchQueryTest {

    @Test
    fun `normalize trims leading and trailing whitespace`() {
        assertEquals("hello", BookSearchQuery.normalize("  hello  "))
    }

    @Test
    fun `normalize collapses internal whitespace runs`() {
        assertEquals("hello world", BookSearchQuery.normalize("hello   \t world"))
    }

    @Test
    fun `normalize returns null for blank input`() {
        assertNull(BookSearchQuery.normalize(""))
        assertNull(BookSearchQuery.normalize("   "))
    }

    @Test
    fun `normalize preserves non empty single word`() {
        assertEquals("pride", BookSearchQuery.normalize("pride"))
    }
}
