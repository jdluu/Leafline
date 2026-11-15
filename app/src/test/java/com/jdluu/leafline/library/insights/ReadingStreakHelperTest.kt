package com.jdluu.leafline.library.insights

import com.jdluu.leafline.library.data.ReadingAggregate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReadingStreakHelperTest {

    private fun aggregateWithMinutes(minutes: Long) = ReadingAggregate(
        activeDurationMillis = minutes * 60_000L,
        sessionCount = if (minutes > 0) 1 else 0,
        booksOpened = if (minutes > 0) 1 else 0
    )

    @Test
    fun `empty stats returns zero streak`() {
        val today = LocalDate.of(2026, 11, 15)
        assertEquals(0, ReadingStreakHelper.calculateCurrentStreak(emptyList(), today))
    }

    @Test
    fun `no reading today and no reading yesterday returns zero streak`() {
        val today = LocalDate.of(2026, 11, 15)
        val stats = listOf(
            today.minusDays(2) to aggregateWithMinutes(30),
            today.minusDays(1) to aggregateWithMinutes(0),
            today to aggregateWithMinutes(0)
        )
        assertEquals(0, ReadingStreakHelper.calculateCurrentStreak(stats, today))
    }

    @Test
    fun `reading today counts current streak`() {
        val today = LocalDate.of(2026, 11, 15)
        val stats = listOf(
            today.minusDays(3) to aggregateWithMinutes(20),
            today.minusDays(2) to aggregateWithMinutes(15),
            today.minusDays(1) to aggregateWithMinutes(45),
            today to aggregateWithMinutes(10)
        )
        assertEquals(4, ReadingStreakHelper.calculateCurrentStreak(stats, today))
    }

    @Test
    fun `reading yesterday but not yet today maintains streak`() {
        val today = LocalDate.of(2026, 11, 15)
        val stats = listOf(
            today.minusDays(2) to aggregateWithMinutes(30),
            today.minusDays(1) to aggregateWithMinutes(25),
            today to aggregateWithMinutes(0)
        )
        assertEquals(2, ReadingStreakHelper.calculateCurrentStreak(stats, today))
    }

    @Test
    fun `broken streak stops at first day with zero minutes`() {
        val today = LocalDate.of(2026, 11, 15)
        val stats = listOf(
            today.minusDays(4) to aggregateWithMinutes(50),
            today.minusDays(3) to aggregateWithMinutes(0),
            today.minusDays(2) to aggregateWithMinutes(30),
            today.minusDays(1) to aggregateWithMinutes(20),
            today to aggregateWithMinutes(15)
        )
        assertEquals(3, ReadingStreakHelper.calculateCurrentStreak(stats, today))
    }
}
