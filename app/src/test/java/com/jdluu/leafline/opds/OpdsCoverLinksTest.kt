package com.jdluu.leafline.opds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner

/**
 * Readium's OPDS1 parser keeps only the acquisition link, so covers have to be
 * recovered from the raw Atom. These fixtures mirror what a self-hosted server
 * actually emits: relative hrefs, a full image and a thumbnail on the same entry.
 */
@RunWith(RobolectricTestRunner::class)
class OpdsCoverLinksTest {

    private val base = Url("http://127.0.0.1:6061/api/v1/opds/catalog")!!

    /**
     * A spec-valid Atom feed. Built by concatenation, not `trimIndent()`: that runs
     * after interpolation, so the un-indented entry lines would drag the common
     * indent to zero and leave whitespace before the XML declaration.
     */
    private fun feed(entries: String) = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<feed xmlns="http://www.w3.org/2005/Atom">""")
        appendLine("<id>urn:uuid:catalog</id>")
        appendLine("<title>Test catalog</title>")
        appendLine("<updated>2026-01-01T00:00:00Z</updated>")
        appendLine(entries)
        appendLine("</feed>")
    }.toByteArray()

    private fun entry(title: String, href: String, links: String) = buildString {
        appendLine("<entry>")
        appendLine("<id>urn:uuid:${title.hashCode()}</id>")
        appendLine("<title>$title</title>")
        appendLine("<updated>2026-01-01T00:00:00Z</updated>")
        appendLine("""<link rel="http://opds-spec.org/acquisition" type="application/epub+zip" href="$href"/>""")
        appendLine(links)
        appendLine("</entry>")
    }

    @Test
    fun `a relative image href is resolved and paired with its acquisition`() {
        val xml = feed(
            entry(
                title = "Blackflame",
                href = "/api/v1/opds/934/download?fileId=934",
                links = """<link rel="http://opds-spec.org/image" type="image/jpeg" href="/api/v1/opds/934/cover"/>"""
            )
        )

        val covers = OpdsCoverLinks.extract(xml, base)

        assertEquals(
            "http://127.0.0.1:6061/api/v1/opds/934/cover",
            covers["http://127.0.0.1:6061/api/v1/opds/934/download?fileId=934"]
        )
    }

    @Test
    fun `the full image wins over the thumbnail`() {
        val xml = feed(
            entry(
                title = "Both",
                href = "/dl/1",
                links = """
                    <link rel="http://opds-spec.org/image/thumbnail" type="image/jpeg" href="/thumb/1.jpg"/>
                    <link rel="http://opds-spec.org/image" type="image/jpeg" href="/full/1.jpg"/>
                """
            )
        )

        val covers = OpdsCoverLinks.extract(xml, base)

        assertEquals("http://127.0.0.1:6061/full/1.jpg", covers["http://127.0.0.1:6061/dl/1"])
    }

    @Test
    fun `a thumbnail alone is still used`() {
        val xml = feed(
            entry(
                title = "Thumb only",
                href = "/dl/2",
                links = """<link rel="http://opds-spec.org/image/thumbnail" type="image/jpeg" href="/thumb/2.jpg"/>"""
            )
        )

        assertEquals(
            "http://127.0.0.1:6061/thumb/2.jpg",
            OpdsCoverLinks.extract(xml, base)["http://127.0.0.1:6061/dl/2"]
        )
    }

    @Test
    fun `an entry without a cover contributes nothing`() {
        val xml = feed(
            entry(title = "No art", href = "/dl/3", links = "")
        )

        assertTrue(OpdsCoverLinks.extract(xml, base).isEmpty())
    }

    @Test
    fun `covers from several entries do not leak into each other`() {
        val xml = feed(
            entry("A", "/dl/a", """<link rel="http://opds-spec.org/image" href="/c/a.jpg"/>""") +
                entry("B", "/dl/b", """<link rel="http://opds-spec.org/image" href="/c/b.jpg"/>""")
        )

        val covers = OpdsCoverLinks.extract(xml, base)

        assertEquals(2, covers.size)
        assertEquals("http://127.0.0.1:6061/c/a.jpg", covers["http://127.0.0.1:6061/dl/a"])
        assertEquals("http://127.0.0.1:6061/c/b.jpg", covers["http://127.0.0.1:6061/dl/b"])
    }

    @Test
    fun `a malformed feed yields no covers instead of throwing`() {
        assertTrue(OpdsCoverLinks.extract("<feed><entry>".toByteArray(), base).isEmpty())
    }

    @Test
    fun `the parser attaches a recovered cover to the book`() {
        val xml = feed(
            entry(
                title = "Blackflame",
                href = "/api/v1/opds/934/download?fileId=934",
                links = """<link rel="http://opds-spec.org/image" type="image/jpeg" href="/api/v1/opds/934/cover"/>"""
            )
        )

        val result = OpdsParser.parse(xml, base.toString())

        val book = result.books.single()
        assertEquals("Blackflame", book.title)
        assertEquals(
            "the cover must reach the book, not just the map",
            "http://127.0.0.1:6061/api/v1/opds/934/cover",
            book.coverUrl
        )
    }
}
