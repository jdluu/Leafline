package com.jdluu.leafline.opds.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.opds.OpdsUiState
import com.jdluu.leafline.opds.OpdsViewModel
import com.jdluu.leafline.opds.model.BreadcrumbItem
import com.jdluu.leafline.opds.model.DownloadStatus
import com.jdluu.leafline.opds.model.OpdsBook
import com.jdluu.leafline.opds.model.OpdsFeedSource
import com.jdluu.leafline.opds.splitHttpStatus
import com.jdluu.leafline.theme.LeaflineEmptyState
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing

/**
 * Seeds the add-feed form from the catalog the reader subscribed to. A 401 can arrive
 * from a sub-feed, but the account belongs to the server, and saving a mid-hierarchy
 * URL as a catalog would leave one that cannot be browsed from its root.
 */
internal fun credentialSeed(
    breadcrumbs: List<BreadcrumbItem>,
    source: OpdsFeedSource
): OpdsFeedSource {
    val root = breadcrumbs.firstOrNull()
    return OpdsFeedSource(
        id = "",
        title = root?.title ?: source.title,
        url = root?.url ?: source.url
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpdsScreen(
    viewModel: OpdsViewModel,
    gridState: LazyGridState = rememberLazyGridState(),
    searchActive: Boolean? = null,
    onSearchActiveChange: ((Boolean) -> Unit)? = null,
    onOpenBook: (LibraryBook) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val feedSources by viewModel.feedSources.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val downloadStatuses by viewModel.downloadStatuses.collectAsState()

    var showAddFeedDialog by remember { mutableStateOf(false) }
    // Non-null when the dialog should open pre-filled, which is how the 401 screen turns
    // "this catalog needs an account" into something the reader can finish.
    var addFeedSeed by remember { mutableStateOf<OpdsFeedSource?>(null) }
    var selectedBookForDetails by remember { mutableStateOf<OpdsBook?>(null) }
    var localSearchActive by rememberSaveable { mutableStateOf(false) }
    val isSearchActive = searchActive ?: localSearchActive
    val setSearchActive: (Boolean) -> Unit = onSearchActiveChange ?: { localSearchActive = it }
    // Non-null while the confirmation for removing a catalog's stored account is open.
    var removeCredentialsFor by remember { mutableStateOf<OpdsFeedSource?>(null) }

    // Intercept hardware back button to navigate backwards in catalog hierarchy
    BackHandler(enabled = breadcrumbs.size > 1) {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            OpdsTopBar(
                selectedSource = selectedSource,
                sources = feedSources,
                onSelectSource = { source ->
                    setSearchActive(false)
                    viewModel.selectSource(source)
                },
                onAddFeedClick = {
                    addFeedSeed = null
                    showAddFeedDialog = true
                },
                onRefreshClick = { viewModel.refresh() },
                onDeleteSource = { sourceId -> viewModel.deleteCustomFeed(sourceId) },
                onRemoveCredentials = { source -> removeCredentialsFor = source },
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onSearchToggle = { setSearchActive(!isSearchActive) },
                onSearchQueryChange = { query -> viewModel.search(query) },
                onSearchClear = {
                    viewModel.clearSearch()
                    setSearchActive(false)
                },
                canNavigateBack = breadcrumbs.size > 1,
                onNavigateBack = { viewModel.navigateBack() }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (breadcrumbs.size > 1) {
                OpdsBreadcrumbBar(
                    breadcrumbs = breadcrumbs,
                    onBreadcrumbClick = { index -> viewModel.navigateToBreadcrumb(index) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is OpdsUiState.NoCatalogs -> {
                        LeaflineEmptyState(
                            title = "No catalogs yet",
                            body = "Add the address of a Calibre, Kavita, or other OPDS catalog to browse it here.",
                            actionLabel = "Add catalog",
                            onAction = {
                                addFeedSeed = null
                                showAddFeedDialog = true
                            }
                        )
                    }
                    is OpdsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = state.message ?: "Loading catalog...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is OpdsUiState.Error -> {
                        OpdsErrorContent(
                            message = state.message,
                            selectedSource = selectedSource,
                            breadcrumbs = breadcrumbs,
                            onAddWithCredentials = { seed ->
                                addFeedSeed = seed
                                showAddFeedDialog = true
                            },
                            onRetry = { viewModel.refresh() },
                            onAddCatalog = {
                                addFeedSeed = null
                                showAddFeedDialog = true
                            }
                        )
                    }
                    is OpdsUiState.Success -> {
                        FeedContentView(
                            feed = state.feed,
                            viewModel = viewModel,
                            downloadStatuses = downloadStatuses,
                            gridState = gridState,
                            onSubfeedClick = { link ->
                                viewModel.navigateSubfeed(link.title, link.href)
                            },
                            onBookClick = { book ->
                                selectedBookForDetails = book
                            },
                            onDownloadClick = { book ->
                                viewModel.downloadBook(
                                    book = book,
                                    onSuccess = {},
                                    onError = {}
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    selectedBookForDetails?.let { book ->
        val status = downloadStatuses[book.id] ?: DownloadStatus.Idle
        val isInLibrary = viewModel.isBookInLibrary(book)

        OpdsBookDetailDialog(
            book = book,
            downloadStatus = status,
            isInLibrary = isInLibrary,
            onDismiss = { selectedBookForDetails = null },
            onDownloadClick = {
                viewModel.downloadBook(
                    book = book,
                    onSuccess = {},
                    onError = {}
                )
            },
            onOpenInReader = {
                val libraryBook = viewModel.libraryBooks.value.firstOrNull {
                    it.title.trim().equals(book.title.trim(), ignoreCase = true)
                }
                if (libraryBook != null) {
                    selectedBookForDetails = null
                    onOpenBook(libraryBook)
                }
            }
        )
    }

    if (showAddFeedDialog) {
        AddOpdsFeedDialog(
            onDismiss = {
                showAddFeedDialog = false
                addFeedSeed = null
            },
            initialTitle = addFeedSeed?.title.orEmpty(),
            initialUrl = addFeedSeed?.url.orEmpty(),
            onAdd = { title, url, username, password, authToken ->
                viewModel.addCustomFeed(title, url, username, password, authToken)
                showAddFeedDialog = false
                addFeedSeed = null
            }
        )
    }

    // Removing a stored account is its own action: blank fields preserve credentials now,
    // so this is the only path and the reader gets told what happens next.
    removeCredentialsFor?.let { source ->
        OpdsRemoveCredentialsDialog(
            source = source,
            onConfirm = {
                viewModel.clearCredentials(source.id)
                removeCredentialsFor = null
            },
            onDismiss = { removeCredentialsFor = null }
        )
    }
}

@Composable
private fun OpdsErrorContent(
    message: String,
    selectedSource: OpdsFeedSource?,
    breadcrumbs: List<BreadcrumbItem>,
    onAddWithCredentials: (OpdsFeedSource) -> Unit,
    onRetry: () -> Unit,
    onAddCatalog: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Padding.screen),
        contentAlignment = Alignment.Center
    ) {
        val (detail, statusCode) = remember(message) {
            splitHttpStatus(message)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "Failed to load catalog",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                // The 401 sentence the client builds already names the recovery,
                // so echo it rather than inventing a settings screen this app does not have.
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (statusCode == "HTTP 401" && selectedSource != null) {
                // One action, and it carries the failing feed's name and
                // address into the add-feed form so the reader only has
                // to type the account.
                Button(onClick = { onAddWithCredentials(credentialSeed(breadcrumbs, selectedSource)) }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Add with credentials")
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onRetry) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Retry")
                    }
                    OutlinedButton(onClick = onAddCatalog) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Add catalog")
                    }
                }
            }
            if (statusCode != null) {
                Text(
                    text = statusCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
