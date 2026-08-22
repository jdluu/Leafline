package com.jdluu.leafline.library.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    @Test
    fun `fromAuthors converts list to JSON string`() {
        val converters = Converters()
        val authors = listOf("Jane Austen", "Charles Dickens")

        val result = converters.fromAuthors(authors)

        assertEquals("[\"Jane Austen\",\"Charles Dickens\"]", result)
    }

    @Test
    fun `toAuthors converts JSON string to list`() {
        val converters = Converters()
        val json = "[\"Jane Austen\",\"Charles Dickens\"]"

        val result = converters.toAuthors(json)

        assertEquals(listOf("Jane Austen", "Charles Dickens"), result)
    }

    @Test
    fun `round trip preserves author list`() {
        val converters = Converters()
        val original = listOf("Author One", "Author Two", "Author Three")

        val json = converters.fromAuthors(original)
        val result = converters.toAuthors(json)

        assertEquals(original, result)
    }

    @Test
    fun `fromAuthors handles empty list`() {
        val converters = Converters()
        val authors = emptyList<String>()

        val result = converters.fromAuthors(authors)

        assertEquals("[]", result)
    }

    @Test
    fun `toAuthors handles empty array`() {
        val converters = Converters()
        val json = "[]"

        val result = converters.toAuthors(json)

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `fromAuthors handles empty string`() {
        val converters = Converters()
        val authors = emptyList<String>()

        val result = converters.fromAuthors(authors)

        assertEquals("[]", result)
    }

    @Test
    fun `toAuthors handles empty string`() {
        val converters = Converters()
        val json = ""

        val result = converters.toAuthors(json)

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `authors with special characters preserved through round trip`() {
        val converters = Converters()
        val original = listOf("O'Brien, John", "Müller, Jürgen", "日本語, 太郎", "李, 华")

        val json = converters.fromAuthors(original)
        val result = converters.toAuthors(json)

        assertEquals(original, result)
    }

    @Test
    fun `authors with quotes preserved through round trip`() {
        val converters = Converters()
        val original = listOf("Author with \"quotes\"", "Another 'quoted' name")

        val json = converters.fromAuthors(original)
        val result = converters.toAuthors(json)

        assertEquals(original, result)
    }
}