package com.jdluu.leafline.reader.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchHistoryStoreTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(SearchHistoryStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun store(maxHistory: Int = 10): SearchHistoryStore {
        val prefs = context.getSharedPreferences(SearchHistoryStore.PREFS_NAME, Context.MODE_PRIVATE)
        return SearchHistoryStore(prefs, maxHistory)
    }

    @Test
    fun `initial search history is empty`() {
        val store = store()
        assertTrue(store.history.value.isEmpty())
    }

    @Test
    fun `recordQuery adds query to top of history`() {
        val store = store()
        store.recordQuery("Pride and Prejudice")
        store.recordQuery("Elizabeth")

        assertEquals(listOf("Elizabeth", "Pride and Prejudice"), store.history.value)
    }

    @Test
    fun `recordQuery deduplicates case-insensitively and promotes to top`() {
        val store = store()
        store.recordQuery("First")
        store.recordQuery("Second")
        store.recordQuery("first")

        assertEquals(listOf("first", "Second"), store.history.value)
    }

    @Test
    fun `recordQuery ignores blank queries`() {
        val store = store()
        store.recordQuery("")
        store.recordQuery("   ")

        assertTrue(store.history.value.isEmpty())
    }

    @Test
    fun `recordQuery trims history to maxHistory`() {
        val store = store(maxHistory = 3)
        store.recordQuery("one")
        store.recordQuery("two")
        store.recordQuery("three")
        store.recordQuery("four")

        assertEquals(listOf("four", "three", "two"), store.history.value)
    }

    @Test
    fun `removeQuery removes specific item`() {
        val store = store()
        store.recordQuery("one")
        store.recordQuery("two")
        store.recordQuery("three")

        store.removeQuery("two")

        assertEquals(listOf("three", "one"), store.history.value)
    }

    @Test
    fun `clear removes all history entries`() {
        val store = store()
        store.recordQuery("one")
        store.recordQuery("two")

        store.clear()

        assertTrue(store.history.value.isEmpty())
    }
}
