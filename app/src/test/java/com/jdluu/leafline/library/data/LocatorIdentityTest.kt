package com.jdluu.leafline.library.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocatorIdentityTest {

    private val locator = """
        {"href": "/OEBPS/chapter01.xhtml", "type": "application/xhtml+xml",
         "title": "Chapter One", "locations": {"progression": 0.5},
         "text": {"after": "some words"}}
    """.trimIndent()

    @Test
    fun `identical payloads produce identical keys`() {
        assertEquals(LocatorIdentity.key(locator), LocatorIdentity.key(locator))
    }

    @Test
    fun `title and text differences do not change the key`() {
        val scrolled = """
            {"href": "/OEBPS/chapter01.xhtml", "type": "application/xhtml+xml",
             "title": "Renamed", "locations": {"progression": 0.5},
             "text": {"after": "other words"}}
        """.trimIndent()

        assertEquals(LocatorIdentity.key(locator), LocatorIdentity.key(scrolled))
    }

    @Test
    fun `progression differences change the key`() {
        val later = """
            {"href": "/OEBPS/chapter01.xhtml", "locations": {"progression": 0.9}}
        """.trimIndent()

        assertEquals(false, LocatorIdentity.key(locator) == LocatorIdentity.key(later))
    }

    @Test
    fun `href differences change the key`() {
        val other = """
            {"href": "/OEBPS/chapter02.xhtml", "locations": {"progression": 0.5}}
        """.trimIndent()

        assertEquals(false, LocatorIdentity.key(locator) == LocatorIdentity.key(other))
    }

    @Test
    fun `malformed json falls back to raw payload`() {
        assertEquals("not json", LocatorIdentity.key("not json"))
    }

    @Test
    fun `displayTitle prefers locator title`() {
        assertEquals("Chapter One", LocatorIdentity.displayTitle(locator))
    }

    @Test
    fun `displayTitle falls back to href file name`() {
        val noTitle = """{"href": "/Text/section-2.xhtml#p3"}"""

        assertEquals("section-2.xhtml", LocatorIdentity.displayTitle(noTitle))
    }

    @Test
    fun `displayTitle returns null for malformed json`() {
        assertNull(LocatorIdentity.displayTitle("not json"))
    }
}
