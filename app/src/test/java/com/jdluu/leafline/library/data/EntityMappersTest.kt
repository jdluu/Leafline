package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.data.local.AnnotationEntity
import com.jdluu.leafline.library.data.local.BookmarkEntity
import com.jdluu.leafline.library.data.local.CollectionEntity
import com.jdluu.leafline.library.data.local.ReadingSessionEntity
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

    // -- ReadingSession --

    @Test
    fun `reading session maps all fields`() {
        val entity = ReadingSessionEntity(
            id = 10L,
            bookId = "book-42",
            startTimeEpochMillis = 1000L,
            endTimeEpochMillis = 2000L,
            activeDurationMillis = 500L,
            startProgression = 0.1,
            endProgression = 0.3,
            startLocatorJson = """{"href":"/ch1.xhtml"}""",
            endLocatorJson = """{"href":"/ch2.xhtml"}"""
        )

        val domain = entity.toReadingSession()
        assertEquals(10L, domain.id)
        assertEquals("book-42", domain.bookId)
        assertEquals(1000L, domain.startTimeEpochMillis)
        assertEquals(2000L, domain.endTimeEpochMillis)
        assertEquals(500L, domain.activeDurationMillis)
        assertEquals(0.1, domain.startProgression!!, 0.001)
        assertEquals(0.3, domain.endProgression!!, 0.001)
        assertEquals("""{"href":"/ch1.xhtml"}""", domain.startLocatorJson)
        assertEquals("""{"href":"/ch2.xhtml"}""", domain.endLocatorJson)

        val roundTripped = domain.toEntity()
        assertEquals(entity, roundTripped)
    }

    @Test
    fun `reading session maps nullable fields`() {
        val entity = ReadingSessionEntity(
            id = 11L,
            bookId = "book-43",
            startTimeEpochMillis = 1000L,
            endTimeEpochMillis = 1500L,
            activeDurationMillis = 500L,
            startProgression = null,
            endProgression = null,
            startLocatorJson = null,
            endLocatorJson = null
        )

        val domain = entity.toReadingSession()
        assertNull(domain.startProgression)
        assertNull(domain.endProgression)
        assertNull(domain.startLocatorJson)
        assertNull(domain.endLocatorJson)

        val roundTripped = domain.toEntity()
        assertEquals(entity, roundTripped)
    }
}