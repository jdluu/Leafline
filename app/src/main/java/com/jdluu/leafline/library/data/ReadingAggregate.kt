package com.jdluu.leafline.library.data

/**
 * Summary aggregate of reading activity over a time window (e.g. daily, weekly).
 *
 * @property sessionCount Total number of reading sessions started within the window.
 * @property activeDurationMillis Total active reading time in milliseconds (excludes background time).
 * @property activeMinutes Active reading time in whole minutes.
 * @property booksOpened Total number of distinct books opened within the window.
 */
data class ReadingAggregate(
    val sessionCount: Int = 0,
    val activeDurationMillis: Long = 0L,
    val activeMinutes: Long = activeDurationMillis / 60_000L,
    val booksOpened: Int = 0
) {
    companion object {
        val EMPTY = ReadingAggregate(
            sessionCount = 0,
            activeDurationMillis = 0L,
            activeMinutes = 0L,
            booksOpened = 0
        )
    }
}
