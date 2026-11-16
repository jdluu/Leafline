package com.jdluu.leafline.library

import android.content.Context
import android.content.SharedPreferences

data class ReadingGoals(
    val dailyMinutes: Int = DEFAULT_DAILY_MINUTES,
    val booksTarget: Int = DEFAULT_BOOKS_TARGET
) {
    companion object {
        const val DEFAULT_DAILY_MINUTES = 20
        const val DEFAULT_BOOKS_TARGET = 12
    }
}

class ReadingGoalStore(private val preferences: SharedPreferences) {

    fun loadGoals(): ReadingGoals {
        val dailyMinutes = preferences.getInt(KEY_DAILY_MINUTES, ReadingGoals.DEFAULT_DAILY_MINUTES)
            .coerceAtLeast(1)
        val booksTarget = preferences.getInt(KEY_BOOKS_TARGET, ReadingGoals.DEFAULT_BOOKS_TARGET)
            .coerceAtLeast(1)
        return ReadingGoals(
            dailyMinutes = dailyMinutes,
            booksTarget = booksTarget
        )
    }

    fun saveGoals(goals: ReadingGoals) {
        preferences.edit()
            .putInt(KEY_DAILY_MINUTES, goals.dailyMinutes.coerceAtLeast(1))
            .putInt(KEY_BOOKS_TARGET, goals.booksTarget.coerceAtLeast(1))
            .apply()
    }

    companion object {
        private const val KEY_DAILY_MINUTES = "reading_goal_daily_minutes"
        private const val KEY_BOOKS_TARGET = "reading_goal_books_target"
        internal const val PREFS_NAME = "leafline_prefs"

        fun fromContext(context: Context): ReadingGoalStore {
            return ReadingGoalStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
        }
    }
}
