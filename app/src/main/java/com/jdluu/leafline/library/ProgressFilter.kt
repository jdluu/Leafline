package com.jdluu.leafline.library

import com.jdluu.leafline.sync.ReadingProgressMath

/**
 * Filter options for book reading progress ranges in the library view.
 */
enum class ProgressFilter(val displayName: String) {
    ALL("All progress"),
    UNREAD("Not started (0%)"),
    IN_PROGRESS("In progress (1–99%)"),
    NEAR_FINISH("Almost done (≥75%)"),
    FINISHED("Finished (100%)");

    fun matches(book: LibraryBook): Boolean {
        if (this == ALL) return true
        val progress = ReadingProgressMath.percentageFromLocator(book.lastLocatorJson)
        val isExplicitFinished = book.readingStatus == ReadingStatus.FINISHED

        return when (this) {
            ALL -> true
            UNREAD -> (progress == null || progress <= 0.1) && !isExplicitFinished
            IN_PROGRESS -> {
                if (isExplicitFinished) false
                else if (progress != null) progress > 0.1 && progress < 99.9
                else book.readingStatus == ReadingStatus.READING
            }
            NEAR_FINISH -> {
                if (isExplicitFinished) false
                else (progress != null && progress >= 75.0 && progress < 99.9)
            }
            FINISHED -> isExplicitFinished || (progress != null && progress >= 99.9)
        }
    }
}
