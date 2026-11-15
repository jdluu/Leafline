package com.jdluu.leafline.opds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpdsParserTest {

    private val sampleOpds1Xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom"
              xmlns:opds="http://opds-spec.org/2010/catalog">
          <id>https://example.com/opds</id>
          <title>Sample Catalog</title>
          <updated>2026-11-15T12:00:00Z</updated>
          <link rel="self" href="https://example.com/opds" type="application/atom+xml;profile=opds-catalog"/>
          <link rel="search" href="https://example.com/search?q={searchTerms}" type="application/atom+xml"/>
          <link rel="next" href="https://example.com/opds?page=2" type="application/atom+xml"/>
          <link rel="subsection" href="https://example.com/fiction.atom" type="application/atom+xml;profile=opds-catalog" title="Fiction"/>

          <entry>
            <id>urn:uuid:12345</id>
            <title>The Great Gatsby</title>
            <author><name>F. Scott Fitzgerald</name></author>
            <summary>A classic novel of the Jazz Age.</summary>
            <link rel="http://opds-spec.org/image" href="https://example.com/covers/gatsby.jpg" type="image/jpeg"/>
            <link rel="http://opds-spec.org/acquisition" href="https://example.com/epubs/gatsby.epub" type="application/epub+zip"/>
            <category term="Fiction" label="Fiction"/>
          </entry>
        </feed>
    """.trimIndent()

    private val sampleOpds2Json = """
        {
          "metadata": {
            "title": "OPDS 2 Catalog"
          },
          "links": [
            { "rel": "self", "href": "https://example.com/opds2", "type": "application/opds+json" }
          ],
          "navigation": [
            { "title": "Science Fiction", "href": "https://example.com/opds2/scifi", "type": "application/opds+json" }
          ],
          "publications": [
            {
              "metadata": {
                "title": "Dune",
                "author": "Frank Herbert",
                "description": "Epic science fiction masterpiece."
              },
              "links": [
                { "rel": "http://opds-spec.org/acquisition", "href": "https://example.com/dune.epub", "type": "application/epub+zip" }
              ],
              "images": [
                { "href": "https://example.com/dune_cover.jpg", "type": "image/jpeg" }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parseOpds1Feed_extractsCatalogFieldsAccurately() {
        val result = OpdsParser.parse(
            data = sampleOpds1Xml.toByteArray(Charsets.UTF_8),
            feedUrlString = "https://example.com/opds",
            isJson = false
        )

        assertEquals("Sample Catalog", result.title)
        assertEquals(1, result.books.size)

        val book = result.books[0]
        assertEquals("The Great Gatsby", book.title)
        assertEquals("F. Scott Fitzgerald", book.author)
        assertEquals("A classic novel of the Jazz Age.", book.summary)
        assertEquals("https://example.com/covers/gatsby.jpg", book.coverUrl)
        assertEquals("https://example.com/epubs/gatsby.epub", book.downloadUrl)
        assertTrue(book.categories.contains("Fiction"))

        assertEquals(1, result.navigationLinks.size)
        assertEquals("Fiction", result.navigationLinks[0].title)
        assertEquals("https://example.com/fiction.atom", result.navigationLinks[0].href)

        

        assertEquals("https://example.com/search?q={searchTerms}", result.searchTemplateUrl)
        assertEquals("https://example.com/opds?page=2", result.nextPageUrl)
    }

    @Test
    fun parseOpds2Feed_extractsJsonCatalogCorrectly() {
        val result = OpdsParser.parse(
            data = sampleOpds2Json.toByteArray(Charsets.UTF_8),
            feedUrlString = "https://example.com/opds2",
            isJson = true
        )

        assertEquals("OPDS 2 Catalog", result.title)
        assertEquals(1, result.navigationLinks.size)
        assertEquals("Science Fiction", result.navigationLinks[0].title)

        assertEquals(1, result.books.size)
        val book = result.books[0]
        assertEquals("Dune", book.title)
        assertEquals("https://example.com/dune.epub", book.downloadUrl)
    }
}
