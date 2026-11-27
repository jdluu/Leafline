package com.jdluu.leafline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.LibraryLayout
import com.jdluu.leafline.library.LibrarySort
import com.jdluu.leafline.library.ProgressFilter
import com.jdluu.leafline.library.LibraryLayoutStore
import com.jdluu.leafline.library.ReadingGoalStore
import com.jdluu.leafline.library.ReadingGoals
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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
    fun `layout defaults to GRID and toggles between LIST and GRID`() =
        runTest(dispatcher) {
            val viewModel = LibraryViewModel(FakeLibraryRepository(books = flowOf(emptyList())))

            assertEquals(LibraryLayout.GRID, viewModel.layout.value)

            viewModel.toggleLayout()
            assertEquals(LibraryLayout.LIST, viewModel.layout.value)

            viewModel.toggleLayout()
            assertEquals(LibraryLayout.GRID, viewModel.layout.value)
        }

    @Test
    fun `setLayout persists to store when present`() =
        runTest(dispatcher) {
            val context: Context = ApplicationProvider.getApplicationContext()
            val store = LibraryLayoutStore.fromContext(context)
            val viewModel = LibraryViewModel(
                repository = FakeLibraryRepository(books = flowOf(emptyList())),
                layoutStore = store
            )

            assertEquals(LibraryLayout.GRID, viewModel.layout.value)

            viewModel.setLayout(LibraryLayout.LIST)
            assertEquals(LibraryLayout.LIST, viewModel.layout.value)
            assertEquals(LibraryLayout.LIST, store.load())
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
    fun `the same publication imported as a second file updates the existing book`() =
        runTest(dispatcher) {
            // Different bytes, so the hash lookup misses; the publication
            // identifier is what proves these are one book.
            val existing = testBook(
                stableId = "identifier:urn:isbn:9780141439471:hash:first-hash",
                filePath = "/old/pride.epub",
                fileHash = "first-hash",
                lastLocatorJson = "{saved-locator}",
                readingStatus = ReadingStatus.READING
            )
            val imported = testBook(
                stableId = "identifier:urn:isbn:9780141439471:hash:second-hash",
                filePath = "/calibre/Pride and Prejudice (12)/pride.epub",
                fileHash = "second-hash"
            )
            val repository = FakeLibraryRepository(
                books = flowOf(listOf(existing)),
                existingByHash = null
            )
            val viewModel = LibraryViewModel(repository)

            viewModel.addBook(imported)
            advanceUntilIdle()

            assertEquals(0, repository.addedBooks.size)
            assertEquals(1, repository.updatedBooks.size)
            val updated = repository.updatedBooks.single()
            assertEquals(existing.stableId, updated.stableId)
            assertEquals(imported.filePath, updated.filePath)
            assertEquals("second-hash", updated.fileHash)
            // The reader keeps their place in the book they were already reading.
            assertEquals("{saved-locator}", updated.lastLocatorJson)
            assertEquals(ReadingStatus.READING, updated.readingStatus)
        }

    @Test
    fun `a different publication with a new hash is added separately`() =
        runTest(dispatcher) {
            val existing = testBook(
                stableId = "identifier:urn:isbn:1:hash:first-hash",
                filePath = "/old/one.epub",
                fileHash = "first-hash"
            )
            val imported = testBook(
                stableId = "identifier:urn:isbn:2:hash:second-hash",
                filePath = "/new/two.epub",
                fileHash = "second-hash"
            )
            val repository = FakeLibraryRepository(
                books = flowOf(listOf(existing)),
                existingByHash = null
            )
            val viewModel = LibraryViewModel(repository)

            viewModel.addBook(imported)
            advanceUntilIdle()

            assertEquals(1, repository.addedBooks.size)
            assertEquals(0, repository.updatedBooks.size)
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

    @Test
    fun `batch actions execute correctly on repository`() =
        runTest(dispatcher) {
            val repository = FakeLibraryRepository(books = flowOf(emptyList()))
            val viewModel = LibraryViewModel(repository)

            viewModel.setBooksReadingStatus(setOf("b1", "b2"), ReadingStatus.FINISHED)
            viewModel.deleteBooks(setOf("b1", "b2"))
            viewModel.addBooksToCollection(setOf("b1", "b2"), 42L)
            advanceUntilIdle()

            assertEquals(setOf("b1" to ReadingStatus.FINISHED, "b2" to ReadingStatus.FINISHED), repository.updatedReadingStatuses.toSet())
            assertEquals(setOf("b1", "b2"), repository.deletedBooks.toSet())
            assertEquals(setOf(42L to "b1", 42L to "b2"), repository.addedToCollection.toSet())
        }

    @Test
    fun `setRating invokes repository setRating`() = runTest(dispatcher) {
        val repository = FakeLibraryRepository(books = flowOf(emptyList()))
        val viewModel = LibraryViewModel(repository)

        viewModel.setRating("book-1", 5)
        advanceUntilIdle()

        assertEquals(listOf("book-1" to 5), repository.updatedRatings)
    }

    @Test
    fun `setTags invokes repository setTags`() = runTest(dispatcher) {
        val repository = FakeLibraryRepository(books = flowOf(emptyList()))
        val viewModel = LibraryViewModel(repository)

        viewModel.setTags("book-1", listOf("sci-fi", "favorite"))
        advanceUntilIdle()

        assertEquals(listOf("book-1" to listOf("sci-fi", "favorite")), repository.updatedTags)
    }

    @Test
    fun `addTag appends new tag to existing book tags`() = runTest(dispatcher) {
        val book = testBook("book-1", "/p1", "h1", tags = listOf("sci-fi"))
        val repository = FakeLibraryRepository(
            books = flowOf(listOf(book)),
            booksById = mapOf("book-1" to book)
        )
        val viewModel = LibraryViewModel(repository)

        viewModel.addTag("book-1", "dystopian")
        advanceUntilIdle()

        assertEquals(listOf("book-1" to listOf("sci-fi", "dystopian")), repository.updatedTags)
    }

    @Test
    fun `removeTag removes matching tag case-insensitively`() = runTest(dispatcher) {
        val book = testBook("book-1", "/p1", "h1", tags = listOf("Sci-Fi", "Classic"))
        val repository = FakeLibraryRepository(
            books = flowOf(listOf(book)),
            booksById = mapOf("book-1" to book)
        )
        val viewModel = LibraryViewModel(repository)

        viewModel.removeTag("book-1", "sci-fi")
        advanceUntilIdle()

        assertEquals(listOf("book-1" to listOf("Classic")), repository.updatedTags)
    }

    @Test
    fun `allTags emits sorted distinct tags from books`() = runTest(dispatcher) {
        val book1 = testBook("book-1", "/p1", "h1", tags = listOf("Sci-Fi", "Novel"))
        val book2 = testBook("book-2", "/p2", "h2", tags = listOf("Fantasy", "Novel", "  "))
        val repository = FakeLibraryRepository(books = flowOf(listOf(book1, book2)))
        val viewModel = LibraryViewModel(repository)

        val emissions = mutableListOf<List<String>>()
        val job = launch {
            viewModel.allTags.collect { emissions += it }
        }
        advanceUntilIdle()

        assertEquals(listOf("Fantasy", "Novel", "Sci-Fi"), emissions.last())
        job.cancel()
    }

    @Test
    fun `tag filter restricts sortedBooks`() = runTest(dispatcher) {
        val book1 = testBook("b1", "/p1", "h1", tags = listOf("history"))
        val book2 = testBook("b2", "/p2", "h2", tags = listOf("science"))
        val repository = FakeLibraryRepository(books = flowOf(listOf(book1, book2)))
        val viewModel = LibraryViewModel(repository)

        val emissions = mutableListOf<List<LibraryBook>>()
        val job = launch {
            viewModel.sortedBooks.collect { emissions += it }
        }
        advanceUntilIdle()
        assertEquals(2, emissions.last().size)

        viewModel.selectTag("history")
        advanceUntilIdle()
        assertEquals(1, emissions.last().size)
        assertEquals("b1", emissions.last().first().stableId)

        viewModel.selectTag(null)
        advanceUntilIdle()
        assertEquals(2, emissions.last().size)

        job.cancel()
    }

    @Test
    fun `tagCounts calculates frequency for distinct tags`() = runTest(dispatcher) {
        val book1 = testBook("b1", "/p1", "h1", tags = listOf("sci-fi", "Favorite"))
        val book2 = testBook("b2", "/p2", "h2", tags = listOf("Sci-Fi", "Classic"))
        val repository = FakeLibraryRepository(books = flowOf(listOf(book1, book2)))
        val viewModel = LibraryViewModel(repository)

        val emissions = mutableListOf<Map<String, Int>>()
        val job = launch {
            viewModel.tagCounts.collect { emissions += it }
        }
        advanceUntilIdle()

        assertEquals(mapOf("sci-fi" to 2, "favorite" to 1, "classic" to 1), emissions.last())
        job.cancel()
    }

    @Test
    fun `addTagToBooks appends tag to selected books that lack it`() = runTest(dispatcher) {
        val book1 = testBook("b1", "/p1", "h1", tags = listOf("fiction"))
        val book2 = testBook("b2", "/p2", "h2", tags = listOf("fiction", "favorite"))
        val repository = FakeLibraryRepository(
            books = flowOf(listOf(book1, book2)),
            booksById = mapOf("b1" to book1, "b2" to book2)
        )
        val viewModel = LibraryViewModel(repository)

        viewModel.addTagToBooks(setOf("b1", "b2"), "favorite")
        advanceUntilIdle()

        assertEquals(listOf("b1" to listOf("fiction", "favorite")), repository.updatedTags)
    }

    @Test
    fun `removeTagFromBooks removes tag from selected books that have it`() = runTest(dispatcher) {
        val book1 = testBook("b1", "/p1", "h1", tags = listOf("fiction", "favorite"))
        val book2 = testBook("b2", "/p2", "h2", tags = listOf("history"))
        val repository = FakeLibraryRepository(
            books = flowOf(listOf(book1, book2)),
            booksById = mapOf("b1" to book1, "b2" to book2)
        )
        val viewModel = LibraryViewModel(repository)

        viewModel.removeTagFromBooks(setOf("b1", "b2"), "favorite")
        advanceUntilIdle()

        assertEquals(listOf("b1" to listOf("fiction")), repository.updatedTags)
    }

    @Test
    fun `setSort updates sort and tapping same sort toggles sortAscending`() = runTest(dispatcher) {
        val repository = FakeLibraryRepository(books = flowOf(emptyList()))
        val viewModel = LibraryViewModel(repository)

        viewModel.setSort(LibrarySort.TITLE)
        assertEquals(LibrarySort.TITLE, viewModel.sort.value)
        assertTrue(viewModel.sortAscending.value)

        // Tapping same sort toggles direction
        viewModel.setSort(LibrarySort.TITLE)
        assertEquals(LibrarySort.TITLE, viewModel.sort.value)
        org.junit.Assert.assertFalse(viewModel.sortAscending.value)
    }

    @Test
    fun `setProgressFilter filters books according to reading progress`() = runTest(dispatcher) {
        val locatorMid = """{"locations":{"progression":0.5},"href":"c1.html"}"""
        val locatorDone = """{"locations":{"progression":1.0},"href":"c1.html"}"""
        val bookUnread = testBook("b1", "/p1", "h1")
        val bookReading = testBook("b2", "/p2", "h2", lastLocatorJson = locatorMid, readingStatus = ReadingStatus.READING)
        val bookFinished = testBook("b3", "/p3", "h3", lastLocatorJson = locatorDone, readingStatus = ReadingStatus.FINISHED)
        val repository = FakeLibraryRepository(books = flowOf(listOf(bookUnread, bookReading, bookFinished)))
        val viewModel = LibraryViewModel(repository)

        val emissions = mutableListOf<List<LibraryBook>>()
        val job = launch {
            viewModel.sortedBooks.collect { emissions += it }
        }
        advanceUntilIdle()
        assertEquals(3, emissions.last().size)

        viewModel.setProgressFilter(ProgressFilter.IN_PROGRESS)
        advanceUntilIdle()
        assertEquals(1, emissions.last().size)
        assertEquals("b2", emissions.last().first().stableId)

        viewModel.setProgressFilter(ProgressFilter.FINISHED)
        advanceUntilIdle()
        assertEquals(1, emissions.last().size)
        assertEquals("b3", emissions.last().first().stableId)

        viewModel.setProgressFilter(ProgressFilter.ALL)
        advanceUntilIdle()
        assertEquals(3, emissions.last().size)

        job.cancel()
    }

    @Test
    fun `renameCollection calls repository renameCollection`() = runTest(dispatcher) {
        val repository = FakeLibraryRepository(books = flowOf(emptyList()))
        val viewModel = LibraryViewModel(repository)

        viewModel.renameCollection(42L, "Classics")
        advanceUntilIdle()

        assertEquals(listOf(42L to "Classics"), repository.renamedCollections)
    }

    @Test
    fun `updateBookMetadata trims fields, parses authors and series, and updates repository`() =
        runTest(dispatcher) {
            val initialBook = LibraryBook(
                stableId = "book-1",
                title = "Original Title",
                authors = listOf("Original Author"),
                series = null,
                seriesIndex = null,
                publisher = "Original Publisher",
                description = "Original Description"
            )
            val fakeRepo = FakeLibraryRepository(
                books = flowOf(listOf(initialBook)),
                booksById = mapOf("book-1" to initialBook)
            )
            val viewModel = LibraryViewModel(repository = fakeRepo)

            viewModel.updateBookMetadata(
                stableId = "book-1",
                title = "  New Title  ",
                authors = listOf(" Author 1 ", "Author 2 "),
                series = "  Dune  ",
                seriesIndex = 2.0f,
                publisher = "  Acme Books  ",
                description = "  New Description  "
            )
            advanceUntilIdle()

            assertEquals(1, fakeRepo.updatedBooks.size)
            val updated = fakeRepo.updatedBooks.first()
            assertEquals("New Title", updated.title)
            assertEquals(listOf("Author 1", "Author 2"), updated.authors)
            assertEquals("Dune", updated.series)
            assertEquals(2.0f, updated.seriesIndex)
            assertEquals("Acme Books", updated.publisher)
            assertEquals("New Description", updated.description)
        }

    @Test
    fun `readingGoals initializes with default values when no store provided`() =
        runTest(dispatcher) {
            val fakeRepo = FakeLibraryRepository(books = flowOf(emptyList()))
            val viewModel = LibraryViewModel(repository = fakeRepo)

            assertEquals(ReadingGoals.DEFAULT_DAILY_MINUTES, viewModel.readingGoals.value.dailyMinutes)
            assertEquals(ReadingGoals.DEFAULT_BOOKS_TARGET, viewModel.readingGoals.value.booksTarget)
        }

    @Test
    fun `updateReadingGoals updates state flow and persists to readingGoalStore`() =
        runTest(dispatcher) {
            val context: Context = ApplicationProvider.getApplicationContext()
            context.getSharedPreferences(ReadingGoalStore.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()

            val store = ReadingGoalStore.fromContext(context)
            val fakeRepo = FakeLibraryRepository(books = flowOf(emptyList()))
            val viewModel = LibraryViewModel(
                repository = fakeRepo,
                readingGoalStore = store
            )

            viewModel.updateReadingGoals(dailyMinutes = 35, booksTarget = 25)
            advanceUntilIdle()

            assertEquals(35, viewModel.readingGoals.value.dailyMinutes)
            assertEquals(25, viewModel.readingGoals.value.booksTarget)

            val loadedFromStore = store.loadGoals()
            assertEquals(35, loadedFromStore.dailyMinutes)
            assertEquals(25, loadedFromStore.booksTarget)
        }

    @Test
    fun `deleteBook passes deleted book and remaining books to cleanup callback`() =
        runTest(dispatcher) {
            val victim = testBook("victim", "/files/victim.epub", "h1")
            val survivor = testBook("survivor", "/files/survivor.epub", "h2")
            val booksFlow = MutableStateFlow(listOf(victim, survivor))
            val repository = FakeLibraryRepository(
                books = booksFlow,
                booksById = mapOf("victim" to victim, "survivor" to survivor)
            )
            val cleaned = mutableListOf<Pair<List<LibraryBook>, List<LibraryBook>>>()
            val viewModel = LibraryViewModel(
                repository = repository,
                onBooksDeleted = { deleted, remaining ->
                    cleaned += deleted to remaining
                }
            )

            // Simulate repository state update when deleteBook is called
            booksFlow.value = listOf(survivor)
            viewModel.deleteBook("victim")
            advanceUntilIdle()

            assertEquals(listOf("victim"), repository.deletedBooks)
            assertEquals(1, cleaned.size)
            assertEquals(listOf(victim), cleaned.single().first)
            assertEquals(listOf(survivor), cleaned.single().second)
        }

    @Test
    fun `multiple library observers share a single upstream getAllBooks subscription`() =
        runTest(dispatcher) {
            val book = testBook(
                stableId = "b1",
                filePath = "/files/b1.epub",
                fileHash = "h1",
                tags = listOf("Sci-Fi")
            )
            val booksFlow = MutableStateFlow(listOf(book))
            val repository = FakeLibraryRepository(books = booksFlow)
            val viewModel = LibraryViewModel(repository)

            val jobs = listOf(
                launch { viewModel.books.collect {} },
                launch { viewModel.isLibraryLoaded.collect {} },
                launch { viewModel.allTags.collect {} },
                launch { viewModel.tagCounts.collect {} },
                launch { viewModel.sortedBooks.collect {} }
            )
            advanceUntilIdle()
            jobs.forEach { it.cancel() }

            assertEquals(1, repository.getAllBooksCalls)
        }

    @Test
    fun `cover extraction bounds concurrency and updates repository cover path`() =
        runTest(dispatcher) {
            val books = (1..5).map { idx ->
                testBook("b$idx", "/files/b$idx.epub", "h$idx")
            }
            val booksFlow = MutableStateFlow(books)
            val repository = FakeLibraryRepository(books = booksFlow)
            var currentConcurrent = 0
            var maxConcurrent = 0
            val loader = com.jdluu.leafline.library.cover.CoverLoader { book ->
                currentConcurrent += 1
                maxConcurrent = maxOf(maxConcurrent, currentConcurrent)
                kotlinx.coroutines.delay(50L)
                currentConcurrent -= 1
                "/covers/${book.stableId}.jpg"
            }

            val viewModel = LibraryViewModel(
                repository = repository,
                coverLoader = loader,
                ioDispatcher = dispatcher
            )

            val collectJob = launch { viewModel.books.collect {} }
            advanceUntilIdle()
            collectJob.cancel()

            assertEquals(5, repository.updatedCoverPaths.size)
            assertTrue("Expected bounded concurrency <= 3, was $maxConcurrent", maxConcurrent in 1..3)
        }
}

private fun testBook(
    stableId: String,
    filePath: String,
    fileHash: String,
    lastLocatorJson: String? = null,
    readingStatus: ReadingStatus = ReadingStatus.UNREAD,
    rating: Int? = null,
    tags: List<String> = emptyList()
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
    readingStatus = readingStatus,
    rating = rating,
    tags = tags
)

/** Minimal [LibraryRepository] for ViewModel tests; only reads used here. */
private class FakeLibraryRepository(
    private val books: Flow<List<LibraryBook>>,
    private val existingByHash: LibraryBook? = null,
    private val collectionsForBook: Map<String, Flow<List<Collection>>> = emptyMap(),
    private val booksById: Map<String, LibraryBook> = emptyMap()
) : LibraryRepository {
    val addedBooks = mutableListOf<LibraryBook>()
    val updatedBooks = mutableListOf<LibraryBook>()
    val lookupHashes = mutableListOf<String>()
    val addedToCollection = mutableListOf<Pair<Long, String>>()
    val removedFromCollection = mutableListOf<Pair<Long, String>>()
    val deletedBooks = mutableListOf<String>()
    val updatedReadingStatuses = mutableListOf<Pair<String, ReadingStatus>>()
    val updatedRatings = mutableListOf<Pair<String, Int?>>()
    val updatedTags = mutableListOf<Pair<String, List<String>>>()
    val renamedCollections = mutableListOf<Pair<Long, String>>()
    val updatedCoverPaths = mutableListOf<Pair<String, String?>>()
    var getAllBooksCalls: Int = 0
        private set

    override fun getAllBooks(): Flow<List<LibraryBook>> = kotlinx.coroutines.flow.flow {
        getAllBooksCalls += 1
        books.collect { emit(it) }
    }

    override fun searchBooks(query: String): Flow<List<LibraryBook>> = books

    override fun getAllCollections(): Flow<List<Collection>> = flowOf(emptyList())

    override fun getRecentlyReadBooks(limit: Int): Flow<List<LibraryBook>> = flowOf(emptyList())

    override fun getBooksByReadingStatus(status: ReadingStatus): Flow<List<LibraryBook>> =
        flowOf(emptyList())

    override fun getCollectionsForBook(bookStableId: String): Flow<List<Collection>> =
        collectionsForBook[bookStableId] ?: flowOf(emptyList())

    override suspend fun getBookByStableId(stableId: String): LibraryBook? = booksById[stableId]

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

    override suspend fun setCoverPath(stableId: String, coverPath: String?) {
        updatedCoverPaths += stableId to coverPath
    }

    override suspend fun addBook(book: LibraryBook): Long {
        addedBooks += book
        return 1L
    }

    override suspend fun updateBook(book: LibraryBook) {
        updatedBooks += book
    }

    override suspend fun deleteBook(stableId: String) {
        deletedBooks += stableId
    }

    override suspend fun createCollection(name: String): Long = error("not used")

    override suspend fun deleteCollection(id: Long) = error("not used")

    override suspend fun renameCollection(id: Long, name: String) { renamedCollections += id to name }

    override suspend fun addBookToCollection(collectionId: Long, bookStableId: String) {
        addedToCollection += collectionId to bookStableId
    }

    override suspend fun removeBookFromCollection(collectionId: Long, bookStableId: String) {
        removedFromCollection += collectionId to bookStableId
    }

    override suspend fun getBookIdsForCollection(collectionId: Long): List<String> =
        error("not used")

    override suspend fun setReadingStatus(stableId: String, status: ReadingStatus) {
        updatedReadingStatuses += stableId to status
    }

    override suspend fun setRating(stableId: String, rating: Int?) {
        updatedRatings += stableId to rating
    }

    override suspend fun setTags(stableId: String, tags: List<String>) {
        updatedTags += stableId to tags
    }
}