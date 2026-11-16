package com.jdluu.leafline

import com.jdluu.leafline.library.data.Annotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnnotationExcerptTest {

    private fun annotation(locatorJson: String) = Annotation(
        id = 1L,
        bookId = "book",
        locatorJson = locatorJson,
        colorHex = Annotation.DEFAULT_COLOR_HEX,
        createdAt = 0L
    )

    private val withExact = """
        {"href": "OEBPS/ch1.xhtml",
         "text": {"exact": "Call me   Ishmael.\n", "before": "", "highlight": "", "after": ""}}
    """.trimIndent()

    private val highlightOnly = """
        {"href": "OEBPS/4851588220423269_2701-h-1.htm.html",
         "text": {"before": "monopolising ", "highlight": "the", "after": " business of whaling"}}
    """.trimIndent()

    @Test
    fun `exact text is preferred and whitespace is collapsed`() {
        assertEquals("Call me Ishmael.", annotationExcerpt(annotation(withExact)))
    }

    @Test
    fun `a locator without exact text quotes the highlight with its context`() {
        val excerpt = annotationExcerpt(annotation(highlightOnly))

        assertTrue("should contain the highlighted word: $excerpt", excerpt.contains("the"))
        assertTrue("should include trailing context: $excerpt", excerpt.contains("business of whaling"))
        assertTrue("should include leading ellipsis: $excerpt", excerpt.startsWith("…"))
        assertTrue("should include trailing ellipsis: $excerpt", excerpt.endsWith("…"))
        assertFalse("must not surface the raw resource filename", excerpt.contains(".htm"))
    }

    @Test
    fun `context is trimmed to keep the row readable`() {
        val long = """
            {"href": "OEBPS/ch1.xhtml",
             "text": {"before": "${"a".repeat(200)} ", "highlight": "word", "after": " ${"b".repeat(200)}"}}
        """.trimIndent()
        val excerpt = annotationExcerpt(annotation(long))

        assertTrue("excerpt too long: ${excerpt.length}", excerpt.length <= 90 + 2)
        assertTrue(excerpt.contains("word"))
    }

    @Test
    fun `a truncated context is cut on word boundaries`() {
        val long = """
            {"href": "OEBPS/ch1.xhtml",
             "text": {"before": "...the whale was a monster of the sea and the sailors feared him greatly whenever the wind blew hard from the north", "highlight": " whaling", "after": " industry grew across the islands of the pacific ocean and beyond the cape of good hope"}}
        """.trimIndent()
        val excerpt = annotationExcerpt(annotation(long))

        val body = excerpt.removePrefix("\u2026").removeSuffix("\u2026")
        assertFalse("starts mid-word: $excerpt", body.startsWith("orth") || body.startsWith("rth"))
        assertTrue("no leading context: $excerpt", excerpt.startsWith("\u2026"))
        assertTrue("highlight missing: $excerpt", body.contains("whaling"))
    }

    @Test
    fun `spaces around the highlight survive the join`() {
        val excerpt = annotationExcerpt(annotation(highlightOnly))

        assertTrue("missing space before highlight: $excerpt", excerpt.contains("monopolising the"))
        assertTrue("missing space after highlight: $excerpt", excerpt.contains("the business"))
        assertFalse("words fused together: $excerpt", excerpt.contains("monopolisingthe"))
    }

    @Test
    fun `a locator with no text falls back to the href file name`() {
        assertEquals(
            "chapter01.xhtml",
            annotationExcerpt(annotation("""{"href": "/OEBPS/chapter01.xhtml"}"""))
        )
    }

    @Test
    fun `malformed json never crashes the list`() {
        assertEquals("Highlight", annotationExcerpt(annotation("not json")))
    }
}
