package com.jdluu.leafline.library.insights

import com.jdluu.leafline.library.data.ReadingAggregate
import java.time.LocalDate

/**
 * Pure helper for calculating reading streaks from daily reading aggregates.
 */
object ReadingStreakHelper {

    /**
     * Calculates the current active reading streak in consecutive days.
     * The streak is considered active if the user read on [referenceDate] (e.g. today)
     * or on the day prior (e.g. yesterday, meaning today is still in progress).
     *
     * @param dailyStats List of (Date, Aggregate) pairs.
     * @param referenceDate Base date to evaluate from (defaults to today).
     * @return Number of consecutive active reading days.
     */
    fun calculateCurrentStreak(
        dailyStats: List<Pair<LocalDate, ReadingAggregate>>,
        referenceDate: LocalDate = LocalDate.now()
    ): Int {
        val statsMap = dailyStats.associate { it.first to it.second.activeMinutes }

        val readToday = (statsMap[referenceDate] ?: 0L) > 0L
        val readYesterday = (statsMap[referenceDate.minusDays(1)] ?: 0L) > 0L

        if (!readToday && !readYesterday) {
            return 0
        }

        var streak = 0
        var currentDate = if (readToday) referenceDate else referenceDate.minusDays(1)

        while (true) {
            val minutes = statsMap[currentDate] ?: 0L
            if (minutes > 0L) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }
}
