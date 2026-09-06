package com.jdluu.leafline.library.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Durable record of an active reading session in Leafline.
 *
 * Stored locally in Room; never synced, exported, or transmitted.
 */
@Entity(
    tableName = "reading_sessions",
    indices = [
        Index("bookId"),
        Index("startTimeEpochMillis")
    ]
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long,
    val activeDurationMillis: Long,
    val startProgression: Double? = null,
    val endProgression: Double? = null,
    val startLocatorJson: String? = null,
    val endLocatorJson: String? = null
)
