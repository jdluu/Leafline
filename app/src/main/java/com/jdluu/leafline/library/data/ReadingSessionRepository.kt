package com.jdluu.leafline.library.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val DEFAULT_RETENTION_DAYS = 30L

interface ReadingSessionRepository {
    suspend fun recordSession(session: ReadingSession): Long
    suspend fun updateSession(session: ReadingSession): Int
    suspend fun getSessionById(id: Long): ReadingSession?
    suspend fun getAllSessions(): List<ReadingSession>
    fun observeAllSessions(): Flow<List<ReadingSession>>
    suspend fun getSessionsForBook(bookId: String): List<ReadingSession>
    suspend fun getSessionsInTimeRange(startEpochMillis: Long, endEpochMillis: Long): List<ReadingSession>
    suspend fun getAggregate(startEpochMillis: Long, endEpochMillis: Long): ReadingAggregate
    suspend fun getAggregateForBook(bookId: String): ReadingAggregate
    suspend fun getDailyAggregate(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ReadingAggregate

    suspend fun getDailyAggregates(
        startDate: LocalDate,
        days: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<Pair<LocalDate, ReadingAggregate>> {
        if (days <= 0) return emptyList()
        val endDate = startDate.plusDays(days.toLong())
        val startEpoch = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endEpoch = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val byDate = getSessionsInTimeRange(startEpoch, endEpoch)
            .groupBy { Instant.ofEpochMilli(it.startTimeEpochMillis).atZone(zoneId).toLocalDate() }
        return (0 until days).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val sessions = byDate[date].orEmpty()
            if (sessions.isEmpty()) {
                date to ReadingAggregate.EMPTY
            } else {
                val totalDuration = sessions.sumOf { it.activeDurationMillis }
                date to ReadingAggregate(
                    sessionCount = sessions.size,
                    activeDurationMillis = totalDuration,
                    activeMinutes = totalDuration / 60_000L,
                    booksOpened = sessions.mapTo(HashSet()) { it.bookId }.size
                )
            }
        }
    }

    suspend fun getWeeklyAggregate(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ReadingAggregate
    suspend fun pruneOldSessions(retentionDays: Long = DEFAULT_RETENTION_DAYS): Int
    suspend fun deleteAllSessions(): Int
    suspend fun deleteSessionsForBook(bookId: String): Int
}
