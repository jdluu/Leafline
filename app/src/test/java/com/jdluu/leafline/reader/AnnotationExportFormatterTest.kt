package com.jdluu.leafline.reader

import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.reader.annotations.AnnotationExportFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationExportFormatterTest {
    private val annotation = Annotation(
        id = 4,
        bookId = "book-1",
        locatorJson = "{\"href\":\"chapter.xhtml\"}",
        colorHex = "#55E65100",
        note = "A useful note",
        createdAt = 123L
    )

    @Test
    fun plainTextIncludesExcerptAndNote() {
        assertEquals(
            "A quote\nNote: A useful note",
            AnnotationExportFormatter.plainText(listOf(annotation)) { "A quote" }
        )
    }

    @Test
    fun markdownPreservesMultilineExcerptAndMetadata() {
        val output = AnnotationExportFormatter.markdown(listOf(annotation)) { "Line one\nLine two" }
        assertTrue(output.contains("> Line one\n> Line two"))
        assertTrue(output.contains("A useful note"))
        assertTrue(output.contains("chapter.xhtml"))
    }

    @Test
    fun jsonContainsStableExportFields() {
        val output = AnnotationExportFormatter.json(listOf(annotation)) { "A quote" }
        assertTrue(output.startsWith("[{\"id\":4"))
        assertTrue(output.contains("\"excerpt\":\"A quote\""))
        assertTrue(output.contains("\"color\":\"#55E65100\""))
        assertTrue(output.contains("\"note\":\"A useful note\""))
    }

    @Test
    fun emptyInputProducesEmptyFormats() {
        assertEquals("", AnnotationExportFormatter.plainText(emptyList()) { "unused" })
        assertEquals("", AnnotationExportFormatter.markdown(emptyList()) { "unused" })
        assertEquals("[]", AnnotationExportFormatter.json(emptyList()) { "unused" })
    }
}
