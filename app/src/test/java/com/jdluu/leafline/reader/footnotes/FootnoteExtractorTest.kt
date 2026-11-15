package com.jdluu.leafline.reader.footnotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FootnoteExtractorTest {

    @Test
    fun `extractFootnote extracts EPUB3 semantic aside footnote`() {
        val html = """
            <html xmlns="http://www.w3.org/1999/xhtml">
            <body>
                <aside id="fn1" epub:type="footnote">
                    <h4>Historical Note</h4>
                    <p>This is the explanation of the event.</p>
                </aside>
            </body>
            </html>
        """.trimIndent()

        val result = FootnoteExtractor.extractFootnote(html, "fn1")
        assertNotNull(result)
        assertEquals("fn1", result?.id)
        assertEquals("Historical Note", result?.title)
        assertEquals("Historical Note\n\nThis is the explanation of the event.", result?.text)
    }

    @Test
    fun `extractFootnote removes backlink anchors cleanly`() {
        val html = """
            <div id="fn-2" role="doc-footnote">
                <p><sup>2</sup> Here is footnote text with a <a href="#ref-2">↩</a> backlink.</p>
            </div>
        """.trimIndent()

        val result = FootnoteExtractor.extractFootnote(html, "fn-2")
        assertNotNull(result)
        assertEquals("fn-2", result?.id)
        assertEquals("Note 2", result?.title)
        assertEquals("2 Here is footnote text with a backlink.", result?.text)
    }

    @Test
    fun `extractFootnote decodes HTML entities and numeric codes`() {
        val html = """
            <p id="fn3">
                <sup>3</sup> &ldquo;Quotes&rdquo; &amp; &mdash; dashes &amp; &#39;apostrophe&#39; &#x2014;
            </p>
        """.trimIndent()

        val result = FootnoteExtractor.extractFootnote(html, "fn3")
        assertNotNull(result)
        assertEquals("Note 3", result?.title)
        assertEquals("3 “Quotes” & — dashes & 'apostrophe' —", result?.text)
    }

    @Test
    fun `extractFootnote finds adjacent block element following empty anchor tag`() {
        val html = """
            <div>
                <a id="fn4"></a>
                <p>This paragraph immediately follows the empty anchor tag.</p>
            </div>
        """.trimIndent()

        val result = FootnoteExtractor.extractFootnote(html, "fn4")
        assertNotNull(result)
        assertEquals("Note 4", result?.title)
        assertEquals("This paragraph immediately follows the empty anchor tag.", result?.text)
    }

    @Test
    fun `extractFootnote handles nested elements properly`() {
        val html = """
            <div id="note-5">
                <div><div>Nested inside divs</div></div>
                <p>Second paragraph</p>
            </div>
        """.trimIndent()

        val result = FootnoteExtractor.extractFootnote(html, "note-5")
        assertNotNull(result)
        assertEquals("Note 5", result?.title)
        assertEquals("Nested inside divs\n\nSecond paragraph", result?.text)
    }

    @Test
    fun `extractFootnote returns null when fragment not found or empty`() {
        val html = "<p id=\"existing\">Some text</p>"

        assertNull(FootnoteExtractor.extractFootnote(html, "nonexistent"))
        assertNull(FootnoteExtractor.extractFootnote(html, ""))
        assertNull(FootnoteExtractor.extractFootnote("", "fn1"))
    }
}
