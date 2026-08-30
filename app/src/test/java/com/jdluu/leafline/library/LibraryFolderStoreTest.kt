package com.jdluu.leafline.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class LibraryFolderStoreTest {
    private lateinit var context: Context
    private lateinit var store: LibraryFolderStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(LibraryFolderStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = LibraryFolderStore.fromContext(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(LibraryFolderStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `empty store has no folders`() {
        assertEquals(emptyList<String>(), store.load())
    }

    @Test
    fun `save replaces duplicates`() {
        store.save(listOf("content://books", "content://archive", "content://books"))

        assertEquals(listOf("content://archive", "content://books"), store.load())
    }

    @Test
    fun `remove deletes only the requested folder`() {
        store.save(listOf("content://books", "content://archive"))

        assertTrue(store.remove("content://books"))
        assertEquals(listOf("content://archive"), store.load())
    }

    @Test
    fun `remove reports false for an unknown folder`() {
        store.save(listOf("content://books"))

        assertEquals(false, store.remove("content://missing"))
        assertEquals(listOf("content://books"), store.load())
    }
}
