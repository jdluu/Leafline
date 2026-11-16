package com.jdluu.leafline.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReadingGoalStoreTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ReadingGoalStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `loadGoals returns default goals when nothing saved`() {
        val store = ReadingGoalStore.fromContext(context)
        val goals = store.loadGoals()

        assertEquals(20, goals.dailyMinutes)
        assertEquals(12, goals.booksTarget)
    }

    @Test
    fun `saveGoals and loadGoals round trips custom goals`() {
        val store = ReadingGoalStore.fromContext(context)
        val customGoals = ReadingGoals(dailyMinutes = 45, booksTarget = 24)

        store.saveGoals(customGoals)
        val loaded = store.loadGoals()

        assertEquals(45, loaded.dailyMinutes)
        assertEquals(24, loaded.booksTarget)
    }

    @Test
    fun `saveGoals and loadGoals coerces non-positive values to at least 1`() {
        val store = ReadingGoalStore.fromContext(context)
        val invalidGoals = ReadingGoals(dailyMinutes = -10, booksTarget = 0)

        store.saveGoals(invalidGoals)
        val loaded = store.loadGoals()

        assertEquals(1, loaded.dailyMinutes)
        assertEquals(1, loaded.booksTarget)
    }
}
