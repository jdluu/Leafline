package com.jdluu.leafline.opds

import androidx.test.core.app.ApplicationProvider
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.opds.model.DownloadStatus
import com.jdluu.leafline.opds.model.OpdsBook
import com.jdluu.leafline.opds.model.OpdsFeedResult
import com.jdluu.leafline.opds.model.OpdsFeedSource
import com.jdluu.leafline.opds.model.OpdsNavigationLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class OpdsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeLibraryRepository : LibraryRepository {
        private val booksFlow = MutableStateFlow<List<LibraryBook>>(emptyList())
        override fun getAllBooks(): Flow<List<LibraryBook>> = booksFlow.asStateFlow()
        fun setBooks(books: List<LibraryBook>) { booksFlow.value = books }

        override fun searchBooks(query: String): Flow<List<LibraryBook>> = booksFlow.asStateFlow()
        override fun getAllCollections(): Flow<List<Collection>> = flowOf(emptyList())
        override fun getRecentlyReadBooks(limit: Int): Flow<List<LibraryBook>> = flowOf(emptyList())
        override fun getBooksByReadingStatus(status: ReadingStatus): Flow<List<LibraryBook>> = flowOf(emptyList())
        override fun getCollectionsForBook(bookStableId: String): Flow<List<Collection>> = flowOf(emptyList())

        override suspend fun getBookByStableId(stableId: String): LibraryBook? = booksFlow.value.firstOrNull { it.stableId == stableId }
        override suspend fun getBookByFileHash(fileHash: String): LibraryBook? = booksFlow.value.firstOrNull { it.fileHash == fileHash }
        override suspend fun getBookByFilePath(filePath: String): LibraryBook? = booksFlow.value.firstOrNull { it.filePath == filePath }
        override suspend fun saveLastLocator(stableId: String, locatorJson: String?, readAtEpochMillis: Long?) {}
        override suspend fun setKoreaderHash(stableId: String, koreaderHash: String?) {}
        override suspend fun setCoverPath(stableId: String, coverPath: String?) {}
        override suspend fun addBook(book: LibraryBook): Long = 1L
        override suspend fun updateBook(book: LibraryBook) {}
        override suspend fun deleteBook(stableId: String) {}
        override suspend fun createCollection(name: String): Long = 1L
        override suspend fun deleteCollection(id: Long) {}
        override suspend fun renameCollection(id: Long, name: String) {}
        override suspend fun addBookToCollection(collectionId: Long, bookStableId: String) {}
        override suspend fun removeBookFromCollection(collectionId: Long, bookStableId: String) {}
        override suspend fun getBookIdsForCollection(collectionId: Long): List<String> = emptyList()
        override suspend fun setReadingStatus(stableId: String, status: ReadingStatus) {}
        override suspend fun setRating(stableId: String, rating: Int?) {}
        override suspend fun setTags(stableId: String, tags: List<String>) {}
    }

    private class FakeOpdsClient : OpdsClient() {
        var lastFetchedUrl: String? = null
        var lastSearchTemplate: String? = null
        var lastSearchQuery: String? = null
        var lastAuthHeader: String? = null

        override suspend fun fetchFeed(url: String, authHeader: String?): Result<OpdsFeedResult> {
            lastFetchedUrl = url
            lastAuthHeader = authHeader
            return Result.success(
                OpdsFeedResult(
                    title = "Feed for $url",
                    feedUrl = url,
                    books = listOf(
                        OpdsBook(
                            id = "book-1",
                            title = "Moby Dick",
                            author = "Herman Melville",
                            summary = "Call me Ishmael.",
                            downloadUrl = "https://example.com/moby.epub"
                        )
                    ),
                    navigationLinks = listOf(
                        OpdsNavigationLink(
                            title = "Adventure",
                            href = "https://example.com/adventure"
                        )
                    ),
                    searchTemplateUrl = "https://example.com/search?q={searchTerms}"
                )
            )
        }

        override suspend fun searchFeed(searchTemplateUrl: String, query: String, authHeader: String?): Result<OpdsFeedResult> {
            lastSearchTemplate = searchTemplateUrl
            lastSearchQuery = query
            lastAuthHeader = authHeader
            return Result.success(
                OpdsFeedResult(
                    title = "Search Results for $query",
                    feedUrl = "https://example.com/search?q=$query",
                    books = listOf(
                        OpdsBook(
                            id = "search-result-1",
                            title = "Result: $query",
                            author = "Author",
                            downloadUrl = "https://example.com/result.epub"
                        )
                    )
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsRootFeedAndInitializesBreadcrumbs() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val feedStore = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val opdsClient = FakeOpdsClient()
        val repo = FakeLibraryRepository()
        val downloadManager = OpdsDownloadManager(
            context = context,
            opdsClient = opdsClient,
            importer = com.jdluu.leafline.ReadiumEpubImporter(context),
            repository = repo,
            scope = this
        )

        val viewModel = OpdsViewModel(
            feedStore = feedStore,
            opdsClient = opdsClient,
            downloadManager = downloadManager,
            libraryRepository = repo
        )

        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.breadcrumbs.value.size)
        assertEquals("Standard Ebooks", viewModel.breadcrumbs.value[0].title)

        val state = viewModel.uiState.value
        assertTrue("UI state should be success", state is OpdsUiState.Success)
        val successState = state as OpdsUiState.Success
        assertEquals(1, successState.feed.books.size)
        assertEquals("Moby Dick", successState.feed.books[0].title)
    }

    @Test
    fun navigateSubfeedAndBack_managesBreadcrumbsCorrectly() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val feedStore = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val opdsClient = FakeOpdsClient()
        val repo = FakeLibraryRepository()
        val downloadManager = OpdsDownloadManager(
            context = context,
            opdsClient = opdsClient,
            importer = com.jdluu.leafline.ReadiumEpubImporter(context),
            repository = repo,
            scope = this
        )

        val viewModel = OpdsViewModel(
            feedStore = feedStore,
            opdsClient = opdsClient,
            downloadManager = downloadManager,
            libraryRepository = repo
        )
        testScheduler.advanceUntilIdle()

        // Navigate to subfeed
        viewModel.navigateSubfeed("Adventure", "https://example.com/adventure")
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.breadcrumbs.value.size)
        assertEquals("Adventure", viewModel.breadcrumbs.value.last().title)
        assertEquals("https://example.com/adventure", opdsClient.lastFetchedUrl)

        // Navigate back
        val handled = viewModel.navigateBack()
        testScheduler.advanceUntilIdle()

        assertTrue(handled)
        assertEquals(1, viewModel.breadcrumbs.value.size)
        assertEquals("Standard Ebooks", viewModel.breadcrumbs.value.last().title)
    }

    @Test
    fun search_usesDiscoveredSearchTemplate() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val feedStore = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val opdsClient = FakeOpdsClient()
        val repo = FakeLibraryRepository()
        val downloadManager = OpdsDownloadManager(
            context = context,
            opdsClient = opdsClient,
            importer = com.jdluu.leafline.ReadiumEpubImporter(context),
            repository = repo,
            scope = this
        )

        val viewModel = OpdsViewModel(
            feedStore = feedStore,
            opdsClient = opdsClient,
            downloadManager = downloadManager,
            libraryRepository = repo
        )
        testScheduler.advanceUntilIdle()

        viewModel.search("whale")
        testScheduler.advanceUntilIdle()

        assertEquals("whale", opdsClient.lastSearchQuery)
        val state = viewModel.uiState.value as OpdsUiState.Success
        assertEquals("Result: whale", state.feed.books[0].title)
    }

    @Test
    fun isBookInLibrary_detectsExistingBookTitle() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val feedStore = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val opdsClient = FakeOpdsClient()
        val repo = FakeLibraryRepository()
        repo.setBooks(
            listOf(
                LibraryBook(
                    stableId = "book-moby-10",
                    title = "Moby Dick",
                    authors = listOf("Herman Melville"),
                    filePath = "/path/moby.epub",
                    fileHash = "abc",
                    language = "en",
                    description = null,
                    publisher = null,
                    publishedAtEpochMillis = null,
                    addedAtEpochMillis = 1L,
                    pageCount = 400
                )
            )
        )

        val downloadManager = OpdsDownloadManager(
            context = context,
            opdsClient = opdsClient,
            importer = com.jdluu.leafline.ReadiumEpubImporter(context),
            repository = repo,
            scope = this
        )

        val viewModel = OpdsViewModel(
            feedStore = feedStore,
            opdsClient = opdsClient,
            downloadManager = downloadManager,
            libraryRepository = repo
        )
        testScheduler.advanceUntilIdle()

        val matchingBook = OpdsBook(id = "1", title = "Moby Dick")
        val otherBook = OpdsBook(id = "2", title = "War and Peace")

        assertTrue(viewModel.isBookInLibrary(matchingBook))
        assertFalse(viewModel.isBookInLibrary(otherBook))
    }

    @Test
    fun authenticatedFeed_propagatesAuthHeaderToFetchAndSearch() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val feedStore = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val opdsClient = FakeOpdsClient()
        val repo = FakeLibraryRepository()
        val downloadManager = OpdsDownloadManager(
            context = context,
            opdsClient = opdsClient,
            importer = com.jdluu.leafline.ReadiumEpubImporter(context),
            repository = repo,
            scope = this
        )

        val viewModel = OpdsViewModel(
            feedStore = feedStore,
            opdsClient = opdsClient,
            downloadManager = downloadManager,
            libraryRepository = repo
        )
        testScheduler.advanceUntilIdle()

        // Built-in feeds have no auth header
        assertEquals(null, opdsClient.lastAuthHeader)

        // Add custom feed with Basic Auth
        viewModel.addCustomFeed(
            title = "Secured Calibre",
            url = "https://calibre.local/opds",
            username = "myuser",
            password = "mypassword"
        )
        testScheduler.advanceUntilIdle()

        val expectedHeader = "Basic " + java.util.Base64.getEncoder().encodeToString("myuser:mypassword".toByteArray(Charsets.UTF_8))
        assertEquals(expectedHeader, viewModel.currentAuthHeader)
        assertEquals(expectedHeader, opdsClient.lastAuthHeader)
        assertEquals("https://calibre.local/opds", opdsClient.lastFetchedUrl)

        // Search within authenticated feed
        viewModel.search("Isaac Asimov")
        testScheduler.advanceUntilIdle()

        assertEquals("Isaac Asimov", opdsClient.lastSearchQuery)
        assertEquals(expectedHeader, opdsClient.lastAuthHeader)
    }

    @Test
    fun addCustomFeed_fromFailedBuiltInCatalog_selectsTheSavedFeed() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val feedStore = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val opdsClient = FakeOpdsClient()
        val repo = FakeLibraryRepository()
        val downloadManager = OpdsDownloadManager(
            context = context,
            opdsClient = opdsClient,
            importer = com.jdluu.leafline.ReadiumEpubImporter(context),
            repository = repo,
            scope = this
        )

        val viewModel = OpdsViewModel(
            feedStore = feedStore,
            opdsClient = opdsClient,
            downloadManager = downloadManager,
            libraryRepository = repo
        )
        testScheduler.advanceUntilIdle()

        viewModel.addCustomFeed(
            title = "Standard Ebooks",
            url = "https://standardebooks.org/feeds/opds",
            username = "patron",
            password = "hunter2"
        )
        testScheduler.advanceUntilIdle()

        assertEquals("https://standardebooks.org/feeds/opds", viewModel.selectedSource.value.url)
        assertEquals(1, viewModel.feedSources.value.count { !it.isBuiltIn })
        assertEquals("https://standardebooks.org/feeds/opds", opdsClient.lastFetchedUrl)
    }
}
