package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.data.local.ReadingSessionDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class ReadingSessionRepositoryImpl(
    private val readingSessionDao: ReadingSessionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) : ReadingSessionRepository {

    override suspend fun recordSession(session: ReadingSession): Long = withContext(ioDispatcher) {
        readingSessionDao.insert(session.toEntity())
    }

    override suspend fun updateSession(session: ReadingSession): Int = withContext(ioDispatcher) {
        readingSessionDao.update(session.toEntity())
    }

    override suspend fun getSessionById(id: Long): ReadingSession? = withContext(ioDispatcher) {
        readingSessionDao.getById(id)?.toReadingSession()
    }

    override suspend fun getAllSessions(): List<ReadingSession> = withContext(ioDispatcher) {
        readingSessionDao.getAll().map { it.toReadingSession() }
    }

    override fun observeAllSessions(): Flow<List<ReadingSession>> {
        return readingSessionDao.observeAll().map { list ->
            list.map { it.toReadingSession() }
        }
    }

    override suspend fun getSessionsForBook(bookId: String): List<ReadingSession> = withContext(ioDispatcher) {
        readingSessionDao.getForBook(bookId).map { it.toReadingSession() }
    }

    override suspend fun getSessionsInTimeRange(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<ReadingSession> = withContext(ioDispatcher) {
        readingSessionDao.getInTimeRange(startEpochMillis, endEpochMillis).map { it.toReadingSession() }
    }

    override suspend fun getAggregate(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): ReadingAggregate = withContext(ioDispatcher) {
        val dbResult = readingSessionDao.queryAggregate(startEpochMillis, endEpochMillis)
        ReadingAggregate(
            sessionCount = dbResult.sessionCount,
            activeDurationMillis = dbResult.totalActiveDurationMillis,
            activeMinutes = dbResult.totalActiveDurationMillis / 60_000L,
            booksOpened = dbResult.booksOpenedCount
        )
    }

    override suspend fun getDailyAggregate(
        date: LocalDate,
        zoneId: ZoneId
    ): ReadingAggregate {
        val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val startOfNextDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return getAggregate(startOfDay, startOfNextDay)
    }

    override suspend fun getWeeklyAggregate(
        date: LocalDate,
        zoneId: ZoneId
    ): ReadingAggregate {
        val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val startOfNextWeek = startOfWeek.plusWeeks(1)
        val startEpoch = startOfWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endEpoch = startOfNextWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
        return getAggregate(startEpoch, endEpoch)
    }

    override suspend fun pruneOldSessions(retentionDays: Long): Int = withContext(ioDispatcher) {
        val cutoff = clock() - TimeUnit.DAYS.toMillis(retentionDays)
        readingSessionDao.deleteOlderThan(cutoff)
    }

    override suspend fun deleteAllSessions(): Int = withContext(ioDispatcher) {
        readingSessionDao.deleteAll()
    }

    override suspend fun deleteSessionsForBook(bookId: String): Int = withContext(ioDispatcher) {
        readingSessionDao.deleteForBook(bookId)
    }
}
