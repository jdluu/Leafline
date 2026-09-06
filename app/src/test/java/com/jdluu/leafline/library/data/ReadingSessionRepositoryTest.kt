package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.data.local.AggregateDbResult
import com.jdluu.leafline.library.data.local.ReadingSessionDao
import com.jdluu.leafline.library.data.local.ReadingSessionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingSessionRepositoryTest {

    private class FakeReadingSessionDao : ReadingSessionDao {
        val stored = MutableStateFlow<List<ReadingSessionEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insert(session: ReadingSessionEntity): Long {
            val id = if (session.id > 0) session.id else nextId++
            val entity = session.copy(id = id)
            stored.value = stored.value.filterNot { it.id == id } + entity
            return id
        }

        override suspend fun update(session: ReadingSessionEntity): Int {
            val current = stored.value
            val exists = current.any { it.id == session.id }
            if (!exists) return 0
            stored.value = current.map { if (it.id == session.id) session else it }
            return 1
        }

        override suspend fun getById(id: Long): ReadingSessionEntity? {
            return stored.value.firstOrNull { it.id == id }
        }

        override fun observeAll(): Flow<List<ReadingSessionEntity>> {
            return stored.map { list ->
                list.sortedWith(
                    compareByDescending<ReadingSessionEntity> { it.startTimeEpochMillis }
                        .thenByDescending { it.id }
                )
            }
        }

        override suspend fun getAll(): List<ReadingSessionEntity> {
            return observeAll().first()
        }

        override fun observeForBook(bookId: String): Flow<List<ReadingSessionEntity>> {
            return stored.map { list ->
                list.filter { it.bookId == bookId }
                    .sortedWith(
                        compareByDescending<ReadingSessionEntity> { it.startTimeEpochMillis }
                            .thenByDescending { it.id }
                    )
            }
        }

        override suspend fun getForBook(bookId: String): List<ReadingSessionEntity> {
            return observeForBook(bookId).first()
        }

        override suspend fun getInTimeRange(
            startEpochMillis: Long,
            endEpochMillis: Long
        ): List<ReadingSessionEntity> {
            return stored.value.filter {
                it.startTimeEpochMillis in startEpochMillis until endEpochMillis
            }.sortedWith(
                compareBy<ReadingSessionEntity> { it.startTimeEpochMillis }
                    .thenBy { it.id }
            )
        }

        override suspend fun queryAggregate(
            startEpochMillis: Long,
            endEpochMillis: Long
        ): AggregateDbResult {
            val matching = stored.value.filter {
                it.startTimeEpochMillis in startEpochMillis until endEpochMillis
            }
            val count = matching.size
            val totalDuration = matching.sumOf { it.activeDurationMillis }
            val distinctBooks = matching.map { it.bookId }.distinct().size
            return AggregateDbResult(
                sessionCount = count,
                totalActiveDurationMillis = totalDuration,
                booksOpenedCount = distinctBooks
            )
        }

        override suspend fun deleteById(id: Long): Int {
            val before = stored.value.size
            stored.value = stored.value.filterNot { it.id == id }
            return before - stored.value.size
        }

        override suspend fun deleteOlderThan(cutoffEpochMillis: Long): Int {
            val before = stored.value.size
            stored.value = stored.value.filterNot { it.startTimeEpochMillis < cutoffEpochMillis }
            return before - stored.value.size
        }

        override suspend fun deleteForBook(bookId: String): Int {
            val before = stored.value.size
            stored.value = stored.value.filterNot { it.bookId == bookId }
            return before - stored.value.size
        }

        override suspend fun deleteAll(): Int {
            val count = stored.value.size
            stored.value = emptyList()
            return count
        }

        override suspend fun getCount(): Int {
            return stored.value.size
        }
    }

    private fun TestScope.newRepository(
        dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
        clock: () -> Long = { 100_000_000L }
    ): Pair<ReadingSessionRepositoryImpl, FakeReadingSessionDao> {
        val dao = FakeReadingSessionDao()
        val repository = ReadingSessionRepositoryImpl(
            readingSessionDao = dao,
            ioDispatcher = dispatcher,
            clock = clock
        )
        return repository to dao
    }

    private fun sampleSession(
        id: Long = 0L,
        bookId: String = "book-1",
        startTime: Long = 1000L,
        endTime: Long = 2000L,
        activeDuration: Long = 60_000L
    ) = ReadingSession(
        id = id,
        bookId = bookId,
        startTimeEpochMillis = startTime,
        endTimeEpochMillis = endTime,
        activeDurationMillis = activeDuration,
        startProgression = 0.1,
        endProgression = 0.3,
        startLocatorJson = """{"href":"/ch1.xhtml"}""",
        endLocatorJson = """{"href":"/ch2.xhtml"}"""
    )

    @Test
    fun `recordSession inserts and returns assigned id`() = runTest {
        val (repository, dao) = newRepository()
        val id = repository.recordSession(sampleSession())

        assertTrue(id > 0)
        val entity = dao.stored.value.first()
        assertEquals(id, entity.id)
        assertEquals("book-1", entity.bookId)
        assertEquals(60_000L, entity.activeDurationMillis)
    }

    @Test
    fun `updateSession updates existing record`() = runTest {
        val (repository, _) = newRepository()
        val id = repository.recordSession(sampleSession())

        val updated = sampleSession(id = id, activeDuration = 120_000L)
        val rows = repository.updateSession(updated)
        assertEquals(1, rows)

        val retrieved = repository.getSessionById(id)
        assertNotNull(retrieved)
        assertEquals(120_000L, retrieved!!.activeDurationMillis)
    }

    @Test
    fun `getSessionById returns null for missing id`() = runTest {
        val (repository, _) = newRepository()
        val result = repository.getSessionById(999L)
        assertNull(result)
    }

    @Test
    fun `getAllSessions and observeAllSessions emit mapped domain sessions`() = runTest {
        val (repository, _) = newRepository()
        repository.recordSession(sampleSession(startTime = 1000L))
        repository.recordSession(sampleSession(startTime = 2000L))

        val all = repository.getAllSessions()
        assertEquals(2, all.size)
        assertEquals(2000L, all[0].startTimeEpochMillis)
        assertEquals(1000L, all[1].startTimeEpochMillis)

        val observed = repository.observeAllSessions().first()
        assertEquals(all, observed)
    }

    @Test
    fun `getSessionsForBook returns only sessions for specified book`() = runTest {
        val (repository, _) = newRepository()
        repository.recordSession(sampleSession(bookId = "book-A"))
        repository.recordSession(sampleSession(bookId = "book-B"))

        val forA = repository.getSessionsForBook("book-A")
        assertEquals(1, forA.size)
        assertEquals("book-A", forA[0].bookId)
    }

    @Test
    fun `getSessionsInTimeRange filters range`() = runTest {
        val (repository, _) = newRepository()
        repository.recordSession(sampleSession(startTime = 100L))
        repository.recordSession(sampleSession(startTime = 200L))
        repository.recordSession(sampleSession(startTime = 300L))

        val inRange = repository.getSessionsInTimeRange(150L, 250L)
        assertEquals(1, inRange.size)
        assertEquals(200L, inRange[0].startTimeEpochMillis)
    }

    @Test
    fun `getAggregate derives activeMinutes from activeDurationMillis`() = runTest {
        val (repository, _) = newRepository()
        repository.recordSession(sampleSession(bookId = "b1", startTime = 100L, activeDuration = 120_000L)) // 2 min
        repository.recordSession(sampleSession(bookId = "b2", startTime = 200L, activeDuration = 90_000L))  // 1.5 min

        val aggregate = repository.getAggregate(0L, 500L)
        assertEquals(2, aggregate.sessionCount)
        assertEquals(210_000L, aggregate.activeDurationMillis)
        assertEquals(3L, aggregate.activeMinutes) // 210_000 / 60_000 = 3
        assertEquals(2, aggregate.booksOpened)
    }

    @Test
    fun `getDailyAggregate bounds queries to 24h of specified date`() = runTest {
        val zone = ZoneId.of("UTC")
        val date = LocalDate.of(2026, 3, 15)
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val noon = startOfDay + TimeUnit.HOURS.toMillis(12)
        val nextDay = startOfDay + TimeUnit.DAYS.toMillis(1) + 1000L

        val (repository, _) = newRepository()
        repository.recordSession(sampleSession(startTime = noon, activeDuration = 60_000L))
        repository.recordSession(sampleSession(startTime = nextDay, activeDuration = 60_000L))

        val daily = repository.getDailyAggregate(date, zone)
        assertEquals(1, daily.sessionCount)
        assertEquals(60_000L, daily.activeDurationMillis)
    }

    @Test
    fun `getWeeklyAggregate bounds queries to Monday through Sunday of week`() = runTest {
        val zone = ZoneId.of("UTC")
        // 2026-03-18 is Wednesday
        val wednesday = LocalDate.of(2026, 3, 18)
        val startOfWeek = LocalDate.of(2026, 3, 16).atStartOfDay(zone).toInstant().toEpochMilli() // Monday
        val insideWeek = startOfWeek + TimeUnit.DAYS.toMillis(2)
        val previousWeek = startOfWeek - TimeUnit.DAYS.toMillis(1)

        val (repository, _) = newRepository()
        repository.recordSession(sampleSession(startTime = insideWeek, activeDuration = 30_000L))
        repository.recordSession(sampleSession(startTime = previousWeek, activeDuration = 30_000L))

        val weekly = repository.getWeeklyAggregate(wednesday, zone)
        assertEquals(1, weekly.sessionCount)
        assertEquals(30_000L, weekly.activeDurationMillis)
    }

    @Test
    fun `pruneOldSessions deletes sessions older than retention cutoff`() = runTest {
        val now = 100_000_000_000L
        val (repository, _) = newRepository(clock = { now })

        val recentTime = now - TimeUnit.DAYS.toMillis(10)
        val oldTime = now - TimeUnit.DAYS.toMillis(35) // > DEFAULT_RETENTION_DAYS (30)

        repository.recordSession(sampleSession(startTime = recentTime))
        repository.recordSession(sampleSession(startTime = oldTime))

        val pruned = repository.pruneOldSessions(DEFAULT_RETENTION_DAYS)
        assertEquals(1, pruned)

        val remaining = repository.getAllSessions()
        assertEquals(1, remaining.size)
        assertEquals(recentTime, remaining[0].startTimeEpochMillis)
    }

    @Test
    fun `deleteSessionsForBook and deleteAllSessions`() = runTest {
        val (repository, _) = newRepository()
        repository.recordSession(sampleSession(bookId = "book-A"))
        repository.recordSession(sampleSession(bookId = "book-A"))
        repository.recordSession(sampleSession(bookId = "book-B"))

        val deletedA = repository.deleteSessionsForBook("book-A")
        assertEquals(2, deletedA)
        assertEquals(1, repository.getAllSessions().size)

        val deletedAll = repository.deleteAllSessions()
        assertEquals(1, deletedAll)
        assertTrue(repository.getAllSessions().isEmpty())
    }
}
