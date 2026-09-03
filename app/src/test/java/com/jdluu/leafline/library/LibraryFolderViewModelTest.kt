package com.jdluu.leafline.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LibraryFolderViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var store: LibraryFolderStore

    private class FakeFolderMetadata(
        private val labels: Map<String, String?>,
        private val grants: MutableMap<String, Boolean> = mutableMapOf()
    ) : FolderMetadata {
        val released = mutableListOf<String>()

        override fun labelFor(uri: String): String? = labels[uri]

        override fun hasPersistedReadGrant(uri: String): Boolean = grants[uri] == true

        override fun releasePersistedReadGrant(uri: String) {
            released += uri
            grants[uri] = false
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(LibraryFolderStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = LibraryFolderStore.fromContext(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        context.getSharedPreferences(LibraryFolderStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `lists saved folders with labels and accessibility from grants`() = runTest(dispatcher) {
        store.save(listOf("content://books", "content://archive"))
        val metadata = FakeFolderMetadata(
            labels = mapOf(
                "content://books" to "Books",
                "content://archive" to null
            ),
            grants = mutableMapOf("content://books" to true)
        )
        val viewModel = LibraryFolderViewModel(store, metadata)

        val folders = viewModel.folders.value
        assertEquals(2, folders.size)
        assertEquals("Books", folders.first { it.uri == "content://books" }.label)
        assertEquals(true, folders.first { it.uri == "content://books" }.accessible)
        assertEquals(null, folders.first { it.uri == "content://archive" }.label)
        assertEquals(false, folders.first { it.uri == "content://archive" }.accessible)
    }

    @Test
    fun `removing an accessible folder releases the grant and removes it from the store`() =
        runTest(dispatcher) {
            store.save(listOf("content://books", "content://archive"))
            val metadata = FakeFolderMetadata(
                labels = mapOf("content://archive" to "Archive"),
                grants = mutableMapOf("content://books" to true)
            )
            val viewModel = LibraryFolderViewModel(store, metadata)

            viewModel.remove(uri = "content://books")

            assertTrue(metadata.released.contains("content://books"))
            assertEquals(listOf("content://archive"), store.load())
            assertEquals(1, viewModel.folders.value.size)
            assertFalse(viewModel.folders.value.any { it.uri == "content://books" })
        }

    @Test
    fun `removing a permission-lost folder still attempts the idempotent release`() =
        runTest(dispatcher) {
            store.save(listOf("content://archive"))
            val metadata = FakeFolderMetadata(
                labels = mapOf("content://archive" to null),
                grants = mutableMapOf()
            )
            val viewModel = LibraryFolderViewModel(store, metadata)

            viewModel.remove(uri = "content://archive")

            // The release is attempted unconditionally so a folder whose accessibility query
            // previously failed still has its persisted grant released defensively.
            assertTrue(metadata.released.contains("content://archive"))
            assertEquals(emptyList<String>(), store.load())
        }

    @Test
    fun `refresh picks up folders added through the picker flow`() = runTest(dispatcher) {
        store.save(listOf("content://books"))
        val metadata = FakeFolderMetadata(labels = mapOf(), grants = mutableMapOf())
        val viewModel = LibraryFolderViewModel(store, metadata)

        store.add("content://archive")

        assertEquals(1, viewModel.folders.value.size)
        viewModel.refresh()
        assertEquals(listOf("content://archive", "content://books"), store.load())
        assertEquals(2, viewModel.folders.value.size)
    }

    @Test
    fun `reselect replaces the stale uri via remove releasing its held grant`() = runTest(dispatcher) {
        store.save(listOf("content://stale"))
        val metadata = FakeFolderMetadata(
            labels = mapOf("content://stale" to null, "content://fresh" to "Fresh"),
            grants = mutableMapOf("content://stale" to true)
        )
        val viewModel = LibraryFolderViewModel(store, metadata)

        // Reselect flow routes through ViewModel.remove so the stale grant is released, then the
        // replacement is added and the list refreshed.
        viewModel.remove("content://stale")
        store.add("content://fresh")
        viewModel.refresh()

        assertTrue(metadata.released.contains("content://stale"))
        assertEquals(listOf("content://fresh"), store.load())
        val folders = viewModel.folders.value
        assertEquals(1, folders.size)
        assertEquals("content://fresh", folders.single().uri)
        assertEquals("Fresh", folders.single().label)
        assertEquals(false, folders.single().accessible)
    }
}
