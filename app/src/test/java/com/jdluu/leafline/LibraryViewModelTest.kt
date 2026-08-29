package com.jdluu.leafline

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.library.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isLibraryLoaded starts false and turns true after the library emits`() =
        runTest(dispatcher) {
            val viewModel = LibraryViewModel(FakeLibraryRepository(books = flowOf(emptyList())))

            val observed = mutableListOf<Boolean>()
            val job = launch { viewModel.isLibraryLoaded.collect { observed += it } }

            advanceUntilIdle()
            job.cancel()

            assertEquals(
                "not loaded before the first emission, loaded after",
                listOf(false, true),
                observed
            )
        }
}

/** Minimal [LibraryRepository] for ViewModel tests; only reads used here. */
private class FakeLibraryRepository(
    private val books: Flow<List<LibraryBook>>
) : LibraryRepository {
    override fun getAllBooks(): Flow<List<LibraryBook>> = books

    override fun searchBooks(query: String): Flow<List<LibraryBook>> = books

    override fun getAllCollections(): Flow<List<Collection>> = flowOf(emptyList())

    override fun getRecentlyReadBooks(limit: Int): Flow<List<LibraryBook>> = flowOf(emptyList())

    override fun getBooksByReadingStatus(status: ReadingStatus): Flow<List<LibraryBook>> =
        flowOf(emptyList())

    override fun getCollectionsForBook(bookStableId: String): Flow<List<Collection>> =
        flowOf(emptyList())

    override suspend fun getBookByStableId(stableId: String): LibraryBook? = error("not used")

    override suspend fun getBookByFileHash(fileHash: String): LibraryBook? = error("not used")

    override suspend fun getBookByFilePath(filePath: String): LibraryBook? = error("not used")

    override suspend fun saveLastLocator(
        stableId: String,
        locatorJson: String?,
        readAtEpochMillis: Long?
    ) = error("not used")

    override suspend fun setKoreaderHash(stableId: String, koreaderHash: String?) =
        error("not used")

    override suspend fun setCoverPath(stableId: String, coverPath: String?) = error("not used")

    override suspend fun addBook(book: LibraryBook): Long = error("not used")

    override suspend fun updateBook(book: LibraryBook) = error("not used")

    override suspend fun deleteBook(stableId: String) = error("not used")

    override suspend fun createCollection(name: String): Long = error("not used")

    override suspend fun deleteCollection(id: Long) = error("not used")

    override suspend fun renameCollection(id: Long, name: String) = error("not used")

    override suspend fun addBookToCollection(collectionId: Long, bookStableId: String) =
        error("not used")

    override suspend fun removeBookFromCollection(collectionId: Long, bookStableId: String) =
        error("not used")

    override suspend fun getBookIdsForCollection(collectionId: Long): List<String> =
        error("not used")

    override suspend fun setReadingStatus(stableId: String, status: ReadingStatus) =
        error("not used")
}