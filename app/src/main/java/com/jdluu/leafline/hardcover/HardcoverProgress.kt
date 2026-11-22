package com.jdluu.leafline.hardcover

import com.jdluu.leafline.library.ReadingStatus
import kotlin.math.roundToInt

/**
 * Hardcover's library status ids, from docs.hardcover.app ("Setting a library
 * entry's status to Currently Reading (2) starts a read... Setting it to Read (3)
 * finishes it").
 */
object HardcoverStatus {
    const val WANT_TO_READ = 1
    const val CURRENTLY_READING = 2
    const val READ = 3

    fun idFor(status: ReadingStatus): Int = when (status) {
        ReadingStatus.UNREAD -> WANT_TO_READ
        ReadingStatus.READING -> CURRENTLY_READING
        ReadingStatus.FINISHED -> READ
    }
}

/**
 * Hardcover records progress as pages of a specific edition, while Leafline knows
 * only a 0..1 progression, so the edition's page count is the only honest bridge.
 * Without a page count nothing is sent rather than a guessed number.
 */
object HardcoverProgress {

    fun pages(progression: Double?, editionPages: Int?): Int? {
        if (progression == null || editionPages == null || editionPages <= 0) return null
        val clamped = progression.coerceIn(0.0, 1.0)
        return (clamped * editionPages).roundToInt().coerceIn(1, editionPages)
    }
}
