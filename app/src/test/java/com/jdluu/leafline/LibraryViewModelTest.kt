package com.jdluu.leafline

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.library.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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

    @Test
    fun `adding an existing file hash updates its path instead of inserting a duplicate`() =
        runTest(dispatcher) {
            val existing = testBook(
                stableId = "legacy-id",
                filePath = "/old/book.epub",
                fileHash = "same-hash",
                lastLocatorJson = "{saved-locator}",
                readingStatus = ReadingStatus.READING
            )
            val imported = testBook(
                stableId = "identifier:book:hash:same-hash",
                filePath = "/new/book.epub",
                fileHash = "same-hash"
            )
            val repository = FakeLibraryRepository(
                books = flowOf(listOf(existing)),
                existingByHash = existing
            )
            val viewModel = LibraryViewModel(repository)

            viewModel.addBook(imported)
            advanceUntilIdle()

            assertEquals(0, repository.addedBooks.size)
            assertEquals(1, repository.updatedBooks.size)
            assertEquals("legacy-id", repository.updatedBooks.single().stableId)
            assertEquals("/new/book.epub", repository.updatedBooks.single().filePath)
            assertEquals("{saved-locator}", repository.updatedBooks.single().lastLocatorJson)
            assertEquals(ReadingStatus.READING, repository.updatedBooks.single().readingStatus)
            assertTrue(repository.lookupHashes.contains("same-hash"))
        }

    @Test
    fun `collectionIdsForBook returns known membership IDs`() =
        runTest(dispatcher) {
            val col1 = Collection(id = 10L, name = "Favorites", createdAtEpochMillis = 1L)
            val col2 = Collection(id = 20L, name = "To Read", createdAtEpochMillis = 2L)
            val repository = FakeLibraryRepository(
                books = flowOf(emptyList()),
                collectionsForBook = mapOf("book-1" to flowOf(listOf(col1, col2)))
            )
            val viewModel = LibraryViewModel(repository)

            val ids = mutableListOf<Set<Long>>()
            val job = launch {
                viewModel.collectionIdsForBook("book-1").collect { ids += it }
            }
            advanceUntilIdle()
            job.cancel()

            assertEquals(1, ids.size)
            assertEquals(setOf(10L, 20L), ids.single())
        }

    @Test
    fun `collectionIdsForBook reuses the same flow for a book`() =
        runTest(dispatcher) {
            val viewModel = LibraryViewModel(FakeLibraryRepository(books = flowOf(emptyList())))

            assertSame(
                viewModel.collectionIdsForBook("book-1"),
                viewModel.collectionIdsForBook("book-1")
            )
        }

    @Test
    fun `collectionIdsForBook returns empty set when book has no collections`() =
        runTest(dispatcher) {
            val repository = FakeLibraryRepository(books = flowOf(emptyList()))
            val viewModel = LibraryViewModel(repository)

            val ids = mutableListOf<Set<Long>>()
            val job = launch {
                viewModel.collectionIdsForBook("unaffiliated").collect { ids += it }
            }
            advanceUntilIdle()
            job.cancel()

            assertEquals(1, ids.size)
            assertTrue(ids.single().isEmpty())
        }

    @Test
    fun `collectionIdsForBook reacts to repository membership changes`() =
        runTest(dispatcher) {
            val bookFlow = MutableStateFlow(listOf<Collection>())
            val repository = FakeLibraryRepository(
                books = flowOf(emptyList()),
                collectionsForBook = mapOf("book-1" to bookFlow)
            )
            val viewModel = LibraryViewModel(repository)

            val emissions = mutableListOf<Set<Long>>()
            val job = launch {
                viewModel.collectionIdsForBook("book-1").collect { emissions += it }
            }
            advanceUntilIdle()
            assertEquals(1, emissions.size)
            assertTrue(emissions.last().isEmpty())

            bookFlow.value = listOf(Collection(id = 5L, name = "Sci-Fi", createdAtEpochMillis = 1L))
            advanceUntilIdle()
            assertEquals(2, emissions.size)
            assertEquals(setOf(5L), emissions.last())

            job.cancel()
        }

    @Test
    fun `collection membership actions reach the repository`() =
        runTest(dispatcher) {
            val repository = FakeLibraryRepository(books = flowOf(emptyList()))
            val viewModel = LibraryViewModel(repository)

            viewModel.addBookToCollection(bookStableId = "book-1", collectionId = 10L)
            viewModel.removeBookFromCollection(bookStableId = "book-1", collectionId = 20L)
            advanceUntilIdle()

            assertEquals(listOf(10L to "book-1"), repository.addedToCollection)
            assertEquals(listOf(20L to "book-1"), repository.removedFromCollection)
        }
}

private fun testBook(
    stableId: String,
    filePath: String,
    fileHash: String,
    lastLocatorJson: String? = null,
    readingStatus: ReadingStatus = ReadingStatus.UNREAD
) = LibraryBook(
    stableId = stableId,
    title = "Book",
    authors = listOf("Author"),
    language = "en",
    description = null,
    publisher = null,
    publishedAtEpochMillis = null,
    filePath = filePath,
    fileHash = fileHash,
    addedAtEpochMillis = 1L,
    pageCount = null,
    lastLocatorJson = lastLocatorJson,
    readingStatus = readingStatus
)

/** Minimal [LibraryRepository] for ViewModel tests; only reads used here. */
private class FakeLibraryRepository(
    private val books: Flow<List<LibraryBook>>,
    private val existingByHash: LibraryBook? = null,
    private val collectionsForBook: Map<String, Flow<List<Collection>>> = emptyMap()
) : LibraryRepository {
    val addedBooks = mutableListOf<LibraryBook>()
    val updatedBooks = mutableListOf<LibraryBook>()
    val lookupHashes = mutableListOf<String>()
    val addedToCollection = mutableListOf<Pair<Long, String>>()
    val removedFromCollection = mutableListOf<Pair<Long, String>>()
    override fun getAllBooks(): Flow<List<LibraryBook>> = books

    override fun searchBooks(query: String): Flow<List<LibraryBook>> = books

    override fun getAllCollections(): Flow<List<Collection>> = flowOf(emptyList())

    override fun getRecentlyReadBooks(limit: Int): Flow<List<LibraryBook>> = flowOf(emptyList())

    override fun getBooksByReadingStatus(status: ReadingStatus): Flow<List<LibraryBook>> =
        flowOf(emptyList())

    override fun getCollectionsForBook(bookStableId: String): Flow<List<Collection>> =
        collectionsForBook[bookStableId] ?: flowOf(emptyList())

    override suspend fun getBookByStableId(stableId: String): LibraryBook? = error("not used")

    override suspend fun getBookByFileHash(fileHash: String): LibraryBook? {
        lookupHashes += fileHash
        return existingByHash?.takeIf { it.fileHash == fileHash }
    }

    override suspend fun getBookByFilePath(filePath: String): LibraryBook? = error("not used")

    override suspend fun saveLastLocator(
        stableId: String,
        locatorJson: String?,
        readAtEpochMillis: Long?
    ) = error("not used")

    override suspend fun setKoreaderHash(stableId: String, koreaderHash: String?) =
        error("not used")

    override suspend fun setCoverPath(stableId: String, coverPath: String?) = error("not used")

    override suspend fun addBook(book: LibraryBook): Long {
        addedBooks += book
        return 1L
    }

    override suspend fun updateBook(book: LibraryBook) {
        updatedBooks += book
    }

    override suspend fun deleteBook(stableId: String) = error("not used")

    override suspend fun createCollection(name: String): Long = error("not used")

    override suspend fun deleteCollection(id: Long) = error("not used")

    override suspend fun renameCollection(id: Long, name: String) = error("not used")

    override suspend fun addBookToCollection(collectionId: Long, bookStableId: String) {
        addedToCollection += collectionId to bookStableId
    }

    override suspend fun removeBookFromCollection(collectionId: Long, bookStableId: String) {
        removedFromCollection += collectionId to bookStableId
    }

    override suspend fun getBookIdsForCollection(collectionId: Long): List<String> =
        error("not used")

    override suspend fun setReadingStatus(stableId: String, status: ReadingStatus) =
        error("not used")
}