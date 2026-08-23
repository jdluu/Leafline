package com.jdluu.leafline.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibrarySortStoreTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("leafline_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `load defaults to RECENT when nothing saved`() {
        val store = LibrarySortStore.fromContext(context)

        assertEquals(LibrarySort.RECENT, store.load())
    }

    @Test
    fun `save then load round trips the chosen sort`() {
        val store = LibrarySortStore.fromContext(context)

        store.save(LibrarySort.TITLE)

        assertEquals(LibrarySort.TITLE, store.load())
    }

    @Test
    fun `load falls back to RECENT for corrupted stored values`() {
        context.getSharedPreferences("leafline_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("library_sort", "GARBAGE")
            .commit()

        val store = LibrarySortStore.fromContext(context)

        assertEquals(LibrarySort.RECENT, store.load())
    }

    @Test
    fun `second save overwrites the first choice`() {
        val store = LibrarySortStore.fromContext(context)

        store.save(LibrarySort.AUTHOR)
        store.save(LibrarySort.RECENT)

        assertEquals(LibrarySort.RECENT, store.load())
    }
}
