package com.jdluu.leafline.library.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AnnotationDaoTest {

    private lateinit var database: LeaflineDatabase
    private lateinit var annotationDao: AnnotationDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        annotationDao = database.annotationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun entity(
        id: Long = 0,
        bookId: String = "book-1",
        createdAt: Long = 1000L,
        colorHex: String = "#55E65100",
        note: String? = null
    ): AnnotationEntity {
        return AnnotationEntity(
            id = id,
            bookId = bookId,
            locatorJson = """{"href": "/OEBPS/chapter01.xhtml"}""",
            colorHex = colorHex,
            note = note,
            createdAt = createdAt
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun insert_round_trips_all_fields() = runTest {
        annotationDao.insert(
            AnnotationEntity(
                bookId = "book-1",
                locatorJson = """{"href": "/a.xhtml", "locations": {"progression": 0.25}}""",
                colorHex = "#80FFAB91",
                note = "Remember this",
                createdAt = 42L
            )
        )

        val stored = annotationDao.getForBook("book-1")
        assertEquals(1, stored.size)
        assertEquals("book-1", stored[0].bookId)
        assertEquals("""{"href": "/a.xhtml", "locations": {"progression": 0.25}}""", stored[0].locatorJson)
        assertEquals("#80FFAB91", stored[0].colorHex)
        assertEquals("Remember this", stored[0].note)
        assertEquals(42L, stored[0].createdAt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun insert_allows_null_note() = runTest {
        annotationDao.insert(entity())

        val stored = annotationDao.getForBook("book-1")
        assertEquals(1, stored.size)
        assertEquals(null, stored[0].note)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun insert_assigns_incrementing_ids_when_unset() = runTest {
        val first = annotationDao.insert(entity())
        val second = annotationDao.insert(entity())

        assertTrue(first > 0)
        assertTrue(second > first)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getForBook_filters_by_book_id_and_orders_newest_first() = runTest {
        annotationDao.insert(entity(bookId = "book-1", createdAt = 1000L))
        annotationDao.insert(entity(bookId = "book-2", createdAt = 2000L))
        annotationDao.insert(entity(bookId = "book-1", createdAt = 3000L))

        val stored = annotationDao.getForBook("book-1")

        assertEquals(2, stored.size)
        assertEquals(3000L, stored[0].createdAt)
        assertEquals(1000L, stored[1].createdAt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun observeForBook_emits_current_rows() = runTest {
        annotationDao.insert(entity())

        val stored = annotationDao.observeForBook("book-1").first()

        assertEquals(1, stored.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun deleteById_removes_only_target_row() = runTest {
        val targetId = annotationDao.insert(entity())
        annotationDao.insert(entity())

        val removed = annotationDao.deleteById(targetId)

        assertEquals(1, removed)
        assertEquals(1, annotationDao.getForBook("book-1").size)
        assertTrue(annotationDao.getForBook("book-1").none { it.id == targetId })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun deleteById_returns_zero_for_missing_row() = runTest {
        assertEquals(0, annotationDao.deleteById(999L))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateNote_changes_only_note_field() = runTest {
        val id = annotationDao.insert(entity(note = "original"))
        annotationDao.insert(entity())

        val updated = annotationDao.updateNote(id, "new note")

        assertEquals(1, updated)
        val stored = annotationDao.getForBook("book-1")
        assertEquals(2, stored.size)
        val target = stored.first { it.id == id }
        assertEquals("new note", target.note)
        assertEquals("#55E65100", target.colorHex)
        assertEquals("book-1", target.bookId)
        assertEquals("""{"href": "/OEBPS/chapter01.xhtml"}""", target.locatorJson)
        assertEquals(1000L, target.createdAt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateNote_clears_note_when_null() = runTest {
        val id = annotationDao.insert(entity(note = "existing"))

        annotationDao.updateNote(id, null)

        val stored = annotationDao.getForBook("book-1")
        assertEquals(null, stored[0].note)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateNote_returns_zero_for_missing_id() = runTest {
        annotationDao.insert(entity())

        assertEquals(0, annotationDao.updateNote(999L, "note"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateColor_changes_only_color_field() = runTest {
        val id = annotationDao.insert(entity(colorHex = "#55E65100", note = "Keep this note"))

        assertEquals(1, annotationDao.updateColor(id, "#5543A047"))

        val updated = annotationDao.getForBook("book-1").single()
        assertEquals("#5543A047", updated.colorHex)
        assertEquals("Keep this note", updated.note)
        assertEquals("book-1", updated.bookId)
        assertEquals(1000L, updated.createdAt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateColor_returns_zero_for_missing_row() = runTest {
        assertEquals(0, annotationDao.updateColor(999L, "#5543A047"))
    }
}
