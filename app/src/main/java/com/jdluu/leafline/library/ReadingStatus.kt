package com.jdluu.leafline.library

/**
 * Tracks the user's reading progress through a book.
 *
 * Values are stored as their lowercase name strings in the database
 * (e.g., "reading", "finished").
 */
enum class ReadingStatus(val dbValue: String, val displayName: String) {
    UNREAD("unread", "Unread"),
    READING("reading", "Reading"),
    FINISHED("finished", "Finished");

    companion object {
        /** Parses a database value, defaulting to UNREAD for unknown values. */
        fun fromDb(value: String?): ReadingStatus {
            return entries.firstOrNull { it.dbValue == value } ?: UNREAD
        }
    }
}
