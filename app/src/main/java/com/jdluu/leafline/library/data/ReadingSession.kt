package com.jdluu.leafline.library.data

/**
 * Domain representation of an active reading session in Leafline.
 *
 * All state is local-only; sessions are never transmitted or exported.
 */
data class ReadingSession(
    val id: Long = 0,
    val bookId: String,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long,
    val activeDurationMillis: Long,
    val startProgression: Double? = null,
    val endProgression: Double? = null,
    val startLocatorJson: String? = null,
    val endLocatorJson: String? = null
)
