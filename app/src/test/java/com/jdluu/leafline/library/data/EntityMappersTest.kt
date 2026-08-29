package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.data.local.AnnotationEntity
import com.jdluu.leafline.library.data.local.BookmarkEntity
import com.jdluu.leafline.library.data.local.CollectionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityMappersTest {

    // -- Bookmark --

    @Test
    fun `bookmark maps all fields`() {
        val entity = BookmarkEntity(
            id = 41L,
            bookId = "book-1",
            locatorJson = """{"href": "/OEBPS/ch1.xhtml"}""",
            createdAt = 1234L,
            label = "Key quote"
        )

        assertEquals(
            Bookmark(
                id = 41L,
                bookId = "book-1",
                locatorJson = """{"href": "/OEBPS/ch1.xhtml"}""",
                createdAt = 1234L,
                label = "Key quote"
            ),
            entity.toBookmark()
        )
    }

    @Test
    fun `bookmark maps null label`() {
        val entity = BookmarkEntity(
            id = 7L,
            bookId = "book-2",
            locatorJson = """{"href": "/OEBPS/ch2.xhtml"}""",
            createdAt = 999L,
            label = null
        )

        val bookmark = entity.toBookmark()

        assertNull(bookmark.label)
        assertEquals(7L, bookmark.id)
        assertEquals("book-2", bookmark.bookId)
        assertEquals("""{"href": "/OEBPS/ch2.xhtml"}""", bookmark.locatorJson)
        assertEquals(999L, bookmark.createdAt)
    }

    // -- Annotation --

    @Test
    fun `annotation maps all fields`() {
        val entity = AnnotationEntity(
            id = 51L,
            bookId = "book-1",
            locatorJson = """{"href": "/OEBPS/ch1.xhtml", "locations": {"progression": 0.5}}""",
            colorHex = "#80B39DDB",
            note = "Key passage",
            createdAt = 4321L
        )

        assertEquals(
            Annotation(
                id = 51L,
                bookId = "book-1",
                locatorJson = """{"href": "/OEBPS/ch1.xhtml", "locations": {"progression": 0.5}}""",
                colorHex = "#80B39DDB",
                note = "Key passage",
                createdAt = 4321L
            ),
            entity.toAnnotation()
        )
    }

    @Test
    fun `annotation maps null note`() {
        val entity = AnnotationEntity(
            id = 9L,
            bookId = "book-2",
            locatorJson = """{"href": "/OEBPS/ch2.xhtml"}""",
            colorHex = Annotation.DEFAULT_COLOR_HEX,
            note = null,
            createdAt = 777L
        )

        val annotation = entity.toAnnotation()

        assertNull(annotation.note)
        assertEquals(9L, annotation.id)
        assertEquals("book-2", annotation.bookId)
        assertEquals("""{"href": "/OEBPS/ch2.xhtml"}""", annotation.locatorJson)
        assertEquals(Annotation.DEFAULT_COLOR_HEX, annotation.colorHex)
        assertEquals(777L, annotation.createdAt)
    }

    // -- Collection --

    @Test
    fun `collection maps all fields`() {
        val entity = CollectionEntity(
            id = 3L,
            name = "Favorites",
            createdAtEpochMillis = 20241010L
        )

        assertEquals(
            Collection(id = 3L, name = "Favorites", createdAtEpochMillis = 20241010L),
            entity.toCollection()
        )
    }
}