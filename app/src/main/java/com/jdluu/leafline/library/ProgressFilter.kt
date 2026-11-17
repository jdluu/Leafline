package com.jdluu.leafline.library

import com.jdluu.leafline.sync.ReadingProgressMath

/**
 * Filter options for book reading progress ranges in the library view.
 */
/**
 * Progress ranges offered by the library filter bar.
 *
 * [displayName] stays short because five chips share one row and the parenthesised
 * ranges pushed the last one off the edge; [description] carries the full meaning for
 * screen readers and the accessibility tree.
 */
enum class ProgressFilter(val displayName: String, val description: String) {
    ALL("All progress", "All progress"),
    UNREAD("Not started", "Not started, 0 percent"),
    IN_PROGRESS("In progress", "In progress, 1 to 99 percent"),
    NEAR_FINISH("Almost done", "Almost done, 75 percent or more"),
    FINISHED("Finished", "Finished, 100 percent");

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
