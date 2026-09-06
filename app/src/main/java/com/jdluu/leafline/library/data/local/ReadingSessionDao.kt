package com.jdluu.leafline.library.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class AggregateDbResult(
    val sessionCount: Int,
    val totalActiveDurationMillis: Long,
    val booksOpenedCount: Int
)

@Dao
interface ReadingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReadingSessionEntity): Long

    @Update
    suspend fun update(session: ReadingSessionEntity): Int

    @Query("SELECT * FROM reading_sessions WHERE id = :id")
    suspend fun getById(id: Long): ReadingSessionEntity?

    @Query("SELECT * FROM reading_sessions ORDER BY startTimeEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY startTimeEpochMillis DESC, id DESC")
    suspend fun getAll(): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTimeEpochMillis DESC, id DESC")
    fun observeForBook(bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTimeEpochMillis DESC, id DESC")
    suspend fun getForBook(bookId: String): List<ReadingSessionEntity>

    @Query(
        "SELECT * FROM reading_sessions " +
            "WHERE startTimeEpochMillis >= :startEpochMillis AND startTimeEpochMillis < :endEpochMillis " +
            "ORDER BY startTimeEpochMillis ASC, id ASC"
    )
    suspend fun getInTimeRange(startEpochMillis: Long, endEpochMillis: Long): List<ReadingSessionEntity>

    @Query(
        "SELECT COUNT(*) AS sessionCount, " +
            "COALESCE(SUM(activeDurationMillis), 0) AS totalActiveDurationMillis, " +
            "COUNT(DISTINCT bookId) AS booksOpenedCount " +
            "FROM reading_sessions " +
            "WHERE startTimeEpochMillis >= :startEpochMillis AND startTimeEpochMillis < :endEpochMillis"
    )
    suspend fun queryAggregate(startEpochMillis: Long, endEpochMillis: Long): AggregateDbResult

    @Query("DELETE FROM reading_sessions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM reading_sessions WHERE startTimeEpochMillis < :cutoffEpochMillis")
    suspend fun deleteOlderThan(cutoffEpochMillis: Long): Int

    @Query("DELETE FROM reading_sessions WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String): Int

    @Query("DELETE FROM reading_sessions")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM reading_sessions")
    suspend fun getCount(): Int
}
