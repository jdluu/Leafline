package com.jdluu.leafline.reader.navigation

import kotlin.math.roundToInt

/**
 * Pure helper functions for resolving interactive scrubber seeking and
 * chapter skipping indices and preview labels.
 */
object ReaderScrubberHelper {

    /**
     * Resolves the target chapter index (0-based) for a given seeking percentage in [0.0..100.0].
     * Returns null if [chapterCount] is 0 or less.
     */
    fun findChapterTarget(percent: Double, chapterCount: Int): Int? {
        if (chapterCount <= 0) return null
        val clamped = percent.coerceIn(0.0, 100.0)
        return ((clamped / 100.0) * (chapterCount - 1)).roundToInt().coerceIn(0, chapterCount - 1)
    }

    /**
     * Whether skipping backward to a previous chapter is permitted.
     */
    fun canSkipPrevious(currentIndex: Int): Boolean = currentIndex > 0

    /**
     * Whether skipping forward to a next chapter is permitted.
     */
    fun canSkipNext(currentIndex: Int, totalCount: Int): Boolean =
        currentIndex in 0 until (totalCount - 1)

    /**
     * Resolves the previous chapter index, or null if already at the first chapter or index is invalid.
     */
    fun resolvePreviousChapterIndex(currentIndex: Int): Int? =
        if (canSkipPrevious(currentIndex)) currentIndex - 1 else null

    /**
     * Resolves the next chapter index, or null if already at the last chapter or index is invalid.
     */
    fun resolveNextChapterIndex(currentIndex: Int, totalCount: Int): Int? =
        if (canSkipNext(currentIndex, totalCount)) currentIndex + 1 else null

    /**
     * Produces a clean preview string for the scrubber thumb / readout while the user is actively dragging.
     */
    fun resolveScrubberPreview(
        percent: Double,
        chapterTitles: List<String> = emptyList()
    ): String {
        val roundedPercent = percent.coerceIn(0.0, 100.0).roundToInt()
        if (chapterTitles.isEmpty()) {
            return "$roundedPercent%"
        }
        val targetIndex = findChapterTarget(percent, chapterTitles.size) ?: 0
        val title = chapterTitles.getOrNull(targetIndex)?.trim()?.takeIf { it.isNotBlank() }
        return if (title != null) {
            "$title \u2022 $roundedPercent%"
        } else {
            "Chapter ${targetIndex + 1} \u2022 $roundedPercent%"
        }
    }
}
