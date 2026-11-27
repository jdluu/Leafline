package com.jdluu.leafline.opds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.opds.model.BreadcrumbItem
import com.jdluu.leafline.opds.model.DownloadStatus
import com.jdluu.leafline.opds.model.OpdsBook
import com.jdluu.leafline.opds.model.OpdsFeedResult
import com.jdluu.leafline.opds.model.OpdsFeedSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OpdsUiState {
    /** No catalog is saved. Browsing has not started, and nothing has failed. */
    data object NoCatalogs : OpdsUiState
    data class Loading(val message: String? = null) : OpdsUiState
    data class Success(val feed: OpdsFeedResult) : OpdsUiState
    data class Error(val message: String) : OpdsUiState
}

class OpdsViewModel(
    private val feedStore: OpdsFeedStore,
    private val opdsClient: OpdsClient,
    private val downloadManager: OpdsDownloadManager,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _feedSources = MutableStateFlow<List<OpdsFeedSource>>(feedStore.loadFeeds())
    val feedSources: StateFlow<List<OpdsFeedSource>> = _feedSources.asStateFlow()

    private val _selectedSource = MutableStateFlow(resolveSelectedSource())
    val selectedSource: StateFlow<OpdsFeedSource?> = _selectedSource.asStateFlow()

    /**
     * Restores the remembered catalog, or the first saved one. A remembered id that no
     * longer resolves (the row was deleted, or it named a catalog this version no longer
     * ships) is dropped rather than carried forever.
     */
    private fun resolveSelectedSource(): OpdsFeedSource? {
        val savedId = feedStore.loadSelectedFeedId() ?: return _feedSources.value.firstOrNull()
        val saved = _feedSources.value.firstOrNull { it.id == savedId }
        if (saved == null) feedStore.clearSelectedFeedId()
        return saved ?: _feedSources.value.firstOrNull()
    }

    private val _breadcrumbs = MutableStateFlow<List<BreadcrumbItem>>(emptyList())
    val breadcrumbs: StateFlow<List<BreadcrumbItem>> = _breadcrumbs.asStateFlow()

    private val _uiState = MutableStateFlow<OpdsUiState>(OpdsUiState.Loading())
    val uiState: StateFlow<OpdsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var activeSearchTemplate: String? = null
    private var feedLoadJob: Job? = null

    val downloadStatuses: StateFlow<Map<String, DownloadStatus>> = downloadManager.downloadStatuses

    val libraryBooks: StateFlow<List<LibraryBook>> = libraryRepository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        val source = _selectedSource.value
        if (source == null) {
            _uiState.value = OpdsUiState.NoCatalogs
        } else {
            loadRootFeed(source)
        }
    }

    fun selectSource(source: OpdsFeedSource) {
        if (_selectedSource.value?.id == source.id && _breadcrumbs.value.size <= 1) return
        _selectedSource.value = source
        feedStore.saveSelectedFeedId(source.id)
        _searchQuery.value = ""
        loadRootFeed(source)
    }

    private fun loadRootFeed(source: OpdsFeedSource) {
        _breadcrumbs.value = listOf(BreadcrumbItem(title = source.title, url = source.url))
        loadFeedUrl(source.url)
    }

    fun navigateSubfeed(title: String, url: String) {
        val current = _breadcrumbs.value
        _breadcrumbs.value = current + BreadcrumbItem(title = title, url = url)
        _searchQuery.value = ""
        loadFeedUrl(url)
    }

    fun navigateBack(): Boolean {
        val current = _breadcrumbs.value
        if (current.size <= 1) return false
        val updated = current.dropLast(1)
        _breadcrumbs.value = updated
        _searchQuery.value = ""
        val targetUrl = updated.last().url
        loadFeedUrl(targetUrl)
        return true
    }

    fun navigateToBreadcrumb(index: Int) {
        val current = _breadcrumbs.value
        if (index < 0 || index >= current.size || index == current.lastIndex) return
        val updated = current.take(index + 1)
        _breadcrumbs.value = updated
        _searchQuery.value = ""
        val targetUrl = updated.last().url
        loadFeedUrl(targetUrl)
    }

    val currentAuthHeader: String?
        get() = _selectedSource.value?.authHeader()

    fun search(query: String) {
        _searchQuery.value = query
        val template = activeSearchTemplate
        if (template == null || query.isBlank()) {
            refresh()
            return
        }

        feedLoadJob?.cancel()
        feedLoadJob = viewModelScope.launch {
            _uiState.value = OpdsUiState.Loading("Searching...")
            val result = opdsClient.searchFeed(template, query.trim(), currentAuthHeader)
            result.fold(
                onSuccess = { feed ->
                    _uiState.value = OpdsUiState.Success(feed)
                },
                onFailure = { error ->
                    _uiState.value = OpdsUiState.Error(error.message ?: "Search failed")
                }
            )
        }
    }

    fun clearSearch() {
        if (_searchQuery.value.isNotEmpty()) {
            _searchQuery.value = ""
            refresh()
        }
    }

    fun refresh() {
        // With no catalog there is no address to retry; say that rather than fetch "".
        val currentUrl = _breadcrumbs.value.lastOrNull()?.url ?: _selectedSource.value?.url
        if (currentUrl == null) {
            _uiState.value = OpdsUiState.NoCatalogs
            return
        }
        loadFeedUrl(currentUrl)
    }

    private fun loadFeedUrl(url: String) {
        feedLoadJob?.cancel()
        feedLoadJob = viewModelScope.launch {
            _uiState.value = OpdsUiState.Loading()
            val result = opdsClient.fetchFeed(url, currentAuthHeader)
            result.fold(
                onSuccess = { feed ->
                    if (!feed.searchTemplateUrl.isNullOrBlank()) {
                        activeSearchTemplate = feed.searchTemplateUrl
                    }
                    _uiState.value = OpdsUiState.Success(feed)
                },
                onFailure = { error ->
                    _uiState.value = OpdsUiState.Error(
                        error.message ?: "Failed to connect to OPDS catalog"
                    )
                }
            )
        }
    }

    fun downloadBook(
        book: OpdsBook,
        onSuccess: (LibraryBook) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        downloadManager.downloadAndImport(book, currentAuthHeader, onSuccess, onError)
    }

    fun addCustomFeed(
        title: String,
        url: String,
        username: String? = null,
        password: String? = null,
        authToken: String? = null
    ) {
        val newSource = feedStore.addCustomFeed(title, url, username, password, authToken)
        _feedSources.value = feedStore.loadFeeds()
        selectSource(newSource)
    }

    fun updateCustomFeed(
        id: String,
        title: String,
        url: String,
        username: String? = null,
        password: String? = null,
        authToken: String? = null
    ) {
        val updated = feedStore.updateCustomFeed(id, title, url, username, password, authToken)
        _feedSources.value = feedStore.loadFeeds()
        if (updated != null && _selectedSource.value?.id == id) {
            _selectedSource.value = updated
            refresh()
        }
    }

    fun clearCredentials(id: String) {
        if (!feedStore.clearCredentials(id)) return
        _feedSources.value = feedStore.loadFeeds()
        val refreshed = _feedSources.value.firstOrNull { it.id == id } ?: return
        if (_selectedSource.value?.id == id) {
            _selectedSource.value = refreshed
            refresh()
        }
    }

    fun deleteCustomFeed(id: String) {
        val wasSelected = _selectedSource.value?.id == id
        feedStore.removeCustomFeed(id)
        _feedSources.value = feedStore.loadFeeds()
        if (!wasSelected) return

        val next = _feedSources.value.firstOrNull()
        if (next != null) {
            selectSource(next)
            return
        }
        // The last catalog is gone, so the screen goes back to the empty state instead of
        // holding a source that no longer exists.
        _selectedSource.value = null
        _breadcrumbs.value = emptyList()
        _searchQuery.value = ""
        feedStore.clearSelectedFeedId()
        _uiState.value = OpdsUiState.NoCatalogs
    }

    fun isBookInLibrary(book: OpdsBook): Boolean {
        val normalizedTitle = book.title.trim().lowercase()
        return libraryBooks.value.any { it.title.trim().lowercase() == normalizedTitle }
    }

    fun reloadFeeds() {
        _feedSources.value = feedStore.loadFeeds()
        if (_selectedSource.value == null) {
            val first = _feedSources.value.firstOrNull()
            if (first != null) {
                selectSource(first)
            }
        }
    }
}

class OpdsViewModelFactory(
    private val feedStore: OpdsFeedStore,
    private val opdsClient: OpdsClient,
    private val downloadManager: OpdsDownloadManager,
    private val libraryRepository: LibraryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OpdsViewModel(
            feedStore = feedStore,
            opdsClient = opdsClient,
            downloadManager = downloadManager,
            libraryRepository = libraryRepository
        ) as T
    }
}
