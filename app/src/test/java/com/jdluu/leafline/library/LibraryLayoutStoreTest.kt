package com.jdluu.leafline.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryLayoutStoreTest {

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
    fun `load defaults to GRID when nothing saved`() {
        val store = LibraryLayoutStore.fromContext(context)

        assertEquals(LibraryLayout.GRID, store.load())
    }

    @Test
    fun `save then load round trips the chosen layout`() {
        val store = LibraryLayoutStore.fromContext(context)

        store.save(LibraryLayout.LIST)
        assertEquals(LibraryLayout.LIST, store.load())

        store.save(LibraryLayout.GRID)
        assertEquals(LibraryLayout.GRID, store.load())
    }

    @Test
    fun `unknown name falls back to GRID`() {
        assertEquals(LibraryLayout.GRID, LibraryLayout.fromNameOrDefault("UNKNOWN_VALUE"))
        assertEquals(LibraryLayout.GRID, LibraryLayout.fromNameOrDefault(null))
    }
}
