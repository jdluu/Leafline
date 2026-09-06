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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ReadingSessionDaoTest {

    private lateinit var database: LeaflineDatabase
    private lateinit var sessionDao: ReadingSessionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = database.readingSessionDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun sampleEntity(
        id: Long = 0,
        bookId: String = "book-1",
        startTime: Long = 1000L,
        endTime: Long = 2000L,
        activeDuration: Long = 1000L,
        startProgression: Double? = 0.1,
        endProgression: Double? = 0.2,
        startLocatorJson: String? = """{"href":"/ch1.xhtml"}""",
        endLocatorJson: String? = """{"href":"/ch2.xhtml"}"""
    ): ReadingSessionEntity {
        return ReadingSessionEntity(
            id = id,
            bookId = bookId,
            startTimeEpochMillis = startTime,
            endTimeEpochMillis = endTime,
            activeDurationMillis = activeDuration,
            startProgression = startProgression,
            endProgression = endProgression,
            startLocatorJson = startLocatorJson,
            endLocatorJson = endLocatorJson
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `insert and getById round-trips all fields`() = runTest {
        val entity = sampleEntity(bookId = "book-test")
        val id = sessionDao.insert(entity)
        assertTrue(id > 0)

        val retrieved = sessionDao.getById(id)
        assertNotNull(retrieved)
        assertEquals(id, retrieved!!.id)
        assertEquals("book-test", retrieved.bookId)
        assertEquals(1000L, retrieved.startTimeEpochMillis)
        assertEquals(2000L, retrieved.endTimeEpochMillis)
        assertEquals(1000L, retrieved.activeDurationMillis)
        assertEquals(0.1, retrieved.startProgression!!, 0.001)
        assertEquals(0.2, retrieved.endProgression!!, 0.001)
        assertEquals("""{"href":"/ch1.xhtml"}""", retrieved.startLocatorJson)
        assertEquals("""{"href":"/ch2.xhtml"}""", retrieved.endLocatorJson)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `insert assigns incrementing ids`() = runTest {
        val id1 = sessionDao.insert(sampleEntity())
        val id2 = sessionDao.insert(sampleEntity())
        assertTrue(id1 > 0)
        assertTrue(id2 > id1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `update modifies existing record`() = runTest {
        val id = sessionDao.insert(sampleEntity(activeDuration = 500L, endTime = 1500L))
        val initial = sessionDao.getById(id)!!

        val updated = initial.copy(
            activeDurationMillis = 1500L,
            endTimeEpochMillis = 2500L,
            endProgression = 0.5
        )
        val rowsAffected = sessionDao.update(updated)
        assertEquals(1, rowsAffected)

        val retrieved = sessionDao.getById(id)!!
        assertEquals(1500L, retrieved.activeDurationMillis)
        assertEquals(2500L, retrieved.endTimeEpochMillis)
        assertEquals(0.5, retrieved.endProgression!!, 0.001)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAll and observeAll order newest first`() = runTest {
        sessionDao.insert(sampleEntity(startTime = 1000L))
        sessionDao.insert(sampleEntity(startTime = 3000L))
        sessionDao.insert(sampleEntity(startTime = 2000L))

        val all = sessionDao.getAll()
        assertEquals(3, all.size)
        assertEquals(3000L, all[0].startTimeEpochMillis)
        assertEquals(2000L, all[1].startTimeEpochMillis)
        assertEquals(1000L, all[2].startTimeEpochMillis)

        val observed = sessionDao.observeAll().first()
        assertEquals(all, observed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getForBook and observeForBook filter by bookId`() = runTest {
        sessionDao.insert(sampleEntity(bookId = "book-A", startTime = 1000L))
        sessionDao.insert(sampleEntity(bookId = "book-B", startTime = 2000L))
        sessionDao.insert(sampleEntity(bookId = "book-A", startTime = 3000L))

        val bookASessions = sessionDao.getForBook("book-A")
        assertEquals(2, bookASessions.size)
        assertEquals(3000L, bookASessions[0].startTimeEpochMillis)
        assertEquals(1000L, bookASessions[1].startTimeEpochMillis)

        val observed = sessionDao.observeForBook("book-A").first()
        assertEquals(bookASessions, observed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getInTimeRange filters and sorts ascending`() = runTest {
        sessionDao.insert(sampleEntity(startTime = 100L))
        sessionDao.insert(sampleEntity(startTime = 200L))
        sessionDao.insert(sampleEntity(startTime = 300L))
        sessionDao.insert(sampleEntity(startTime = 400L))

        val inRange = sessionDao.getInTimeRange(200L, 400L)
        assertEquals(2, inRange.size)
        assertEquals(200L, inRange[0].startTimeEpochMillis)
        assertEquals(300L, inRange[1].startTimeEpochMillis)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `queryAggregate computes aggregate statistics`() = runTest {
        sessionDao.insert(sampleEntity(bookId = "book-1", startTime = 100L, activeDuration = 1000L))
        sessionDao.insert(sampleEntity(bookId = "book-1", startTime = 200L, activeDuration = 2000L))
        sessionDao.insert(sampleEntity(bookId = "book-2", startTime = 300L, activeDuration = 3000L))
        // Outside range:
        sessionDao.insert(sampleEntity(bookId = "book-3", startTime = 500L, activeDuration = 5000L))

        val aggregate = sessionDao.queryAggregate(100L, 400L)
        assertEquals(3, aggregate.sessionCount)
        assertEquals(6000L, aggregate.totalActiveDurationMillis)
        assertEquals(2, aggregate.booksOpenedCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `queryAggregate on empty range returns zeros`() = runTest {
        val aggregate = sessionDao.queryAggregate(100L, 200L)
        assertEquals(0, aggregate.sessionCount)
        assertEquals(0L, aggregate.totalActiveDurationMillis)
        assertEquals(0, aggregate.booksOpenedCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteById removes single record`() = runTest {
        val id1 = sessionDao.insert(sampleEntity())
        val id2 = sessionDao.insert(sampleEntity())

        val deletedCount = sessionDao.deleteById(id1)
        assertEquals(1, deletedCount)
        assertNull(sessionDao.getById(id1))
        assertNotNull(sessionDao.getById(id2))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteOlderThan prunes sessions before cutoff`() = runTest {
        sessionDao.insert(sampleEntity(startTime = 1000L))
        sessionDao.insert(sampleEntity(startTime = 2000L))
        sessionDao.insert(sampleEntity(startTime = 3000L))

        val deleted = sessionDao.deleteOlderThan(2500L)
        assertEquals(2, deleted)

        val remaining = sessionDao.getAll()
        assertEquals(1, remaining.size)
        assertEquals(3000L, remaining[0].startTimeEpochMillis)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteForBook removes only matching book sessions`() = runTest {
        sessionDao.insert(sampleEntity(bookId = "book-1"))
        sessionDao.insert(sampleEntity(bookId = "book-1"))
        sessionDao.insert(sampleEntity(bookId = "book-2"))

        val deleted = sessionDao.deleteForBook("book-1")
        assertEquals(2, deleted)

        val remaining = sessionDao.getAll()
        assertEquals(1, remaining.size)
        assertEquals("book-2", remaining[0].bookId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteAll and getCount`() = runTest {
        assertEquals(0, sessionDao.getCount())

        sessionDao.insert(sampleEntity())
        sessionDao.insert(sampleEntity())
        assertEquals(2, sessionDao.getCount())

        val deleted = sessionDao.deleteAll()
        assertEquals(2, deleted)
        assertEquals(0, sessionDao.getCount())
    }
}
