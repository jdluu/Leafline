package com.jdluu.leafline.library.data

import kotlinx.coroutines.flow.Flow
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
    suspend fun getDailyAggregate(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ReadingAggregate
    suspend fun getWeeklyAggregate(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ReadingAggregate
    suspend fun pruneOldSessions(retentionDays: Long = DEFAULT_RETENTION_DAYS): Int
    suspend fun deleteAllSessions(): Int
    suspend fun deleteSessionsForBook(bookId: String): Int
}
