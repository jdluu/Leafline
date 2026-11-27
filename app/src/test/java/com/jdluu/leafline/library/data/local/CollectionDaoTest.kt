package com.jdluu.leafline.library.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.LibraryRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CollectionDaoTest {

    private lateinit var database: LeaflineDatabase
    private lateinit var collectionDao: CollectionDao
    private lateinit var repository: LibraryRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        collectionDao = database.collectionDao()
        repository = LibraryRepositoryImpl(
            dataSource = RoomBookDataSource(database),
            collectionDao = collectionDao
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun testBook(id: String) = LibraryBook(
        stableId = id,
        title = "Title $id",
        authors = listOf("Author"),
        language = "en",
        description = null,
        publisher = null,
        publishedAtEpochMillis = null,
        filePath = "/books/$id.epub",
        fileHash = "hash-$id",
        addedAtEpochMillis = 1_000L,
        pageCount = 100
    )

    @Test
    fun observeCollectionsForBook_returns_sorted_joined_collections() = runTest {
        repository.addBook(testBook("b1"))
        val sciFiId = repository.createCollection("Sci-Fi")
        val classicsId = repository.createCollection("Classics")
        repository.createCollection("Empty")

        repository.addBookToCollection(sciFiId, "b1")
        repository.addBookToCollection(classicsId, "b1")

        val collections = repository.getCollectionsForBook("b1").first()
        assertEquals(listOf("Classics", "Sci-Fi"), collections.map { it.name })
    }

    @Test
    fun observeCollectionCounts_includes_empty_collections_and_updates_reactively() = runTest {
        repository.addBook(testBook("b1"))
        repository.addBook(testBook("b2"))
        val colA = repository.createCollection("Alpha")
        val colB = repository.createCollection("Beta")

        val initialCounts = repository.observeCollectionCounts().first()
        assertEquals(mapOf(colA to 0, colB to 0), initialCounts)

        repository.addBookToCollection(colA, "b1")
        repository.addBookToCollection(colA, "b2")
        repository.addBookToCollection(colB, "b1")

        val populatedCounts = repository.observeCollectionCounts().first()
        assertEquals(mapOf(colA to 2, colB to 1), populatedCounts)

        repository.removeBookFromCollection(colA, "b1")
        val updatedCounts = repository.observeCollectionCounts().first()
        assertEquals(mapOf(colA to 1, colB to 1), updatedCounts)
    }

    @Test
    fun observeCollectionsForBook_returns_empty_for_unassigned_book() = runTest {
        repository.addBook(testBook("lonely"))
        repository.createCollection("Favorites")

        assertTrue(repository.getCollectionsForBook("lonely").first().isEmpty())
    }
}
