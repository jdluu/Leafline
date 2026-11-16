package com.jdluu.leafline.opds.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.opds.OpdsUiState
import com.jdluu.leafline.opds.OpdsViewModel
import com.jdluu.leafline.opds.model.BreadcrumbItem
import com.jdluu.leafline.opds.model.DownloadStatus
import com.jdluu.leafline.opds.model.OpdsBook
import com.jdluu.leafline.opds.model.OpdsFeedResult
import com.jdluu.leafline.opds.model.OpdsFeedSource
import com.jdluu.leafline.opds.model.OpdsNavigationLink
import com.jdluu.leafline.opds.rememberOpdsCoverBitmap
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing

private const val COVER_ASPECT_RATIO = 1f / 1.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpdsScreen(
    viewModel: OpdsViewModel,
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
    var selectedBookForDetails by remember { mutableStateOf<OpdsBook?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }

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
                    isSearchActive = false
                    viewModel.selectSource(source)
                },
                onAddFeedClick = { showAddFeedDialog = true },
                onRefreshClick = { viewModel.refresh() },
                onDeleteSource = { sourceId -> viewModel.deleteCustomFeed(sourceId) },
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onSearchToggle = { isSearchActive = !isSearchActive },
                onSearchQueryChange = { query -> viewModel.search(query) },
                onSearchClear = {
                    viewModel.clearSearch()
                    isSearchActive = false
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
            // Breadcrumbs Bar
            if (breadcrumbs.size > 1) {
                OpdsBreadcrumbBar(
                    breadcrumbs = breadcrumbs,
                    onBreadcrumbClick = { index -> viewModel.navigateToBreadcrumb(index) }
                )
            }

            // Main Content Area
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Padding.screen),
                            contentAlignment = Alignment.Center
                        ) {
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
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = { viewModel.refresh() }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    is OpdsUiState.Success -> {
                        FeedContentView(
                            feed = state.feed,
                            viewModel = viewModel,
                            downloadStatuses = downloadStatuses,
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
                            },
                            onOpenInReader = onOpenBook
                        )
                    }
                }
            }
        }
    }

    // Book Detail Sheet / Dialog
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

    // Add Feed Dialog
    if (showAddFeedDialog) {
        AddOpdsFeedDialog(
            onDismiss = { showAddFeedDialog = false },
            onAdd = { title, url, username, password, authToken ->
                viewModel.addCustomFeed(title, url, username, password, authToken)
                showAddFeedDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpdsTopBar(
    selectedSource: OpdsFeedSource,
    sources: List<OpdsFeedSource>,
    onSelectSource: (OpdsFeedSource) -> Unit,
    onAddFeedClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteSource: (String) -> Unit,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit
) {
    var showSourceDropdown by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (isSearchActive) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search catalog...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            navigationIcon = {
                IconButton(onClick = onSearchClear) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit search"
                    )
                }
            },
            actions = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onSearchClear) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            }
        )
    } else {
        TopAppBar(
            title = {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { showSourceDropdown = true }
                            .padding(horizontal = Spacing.xs, vertical = 4.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedSource.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (selectedSource.hasAuth) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Authenticated catalog",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp) // touch-target-ok
                                    )
                                }
                            }
                            Text(
                                text = "Tap to switch catalog",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSourceDropdown,
                        onDismissRequest = { showSourceDropdown = false }
                    ) {
                        sources.forEach { source ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = source.title,
                                                fontWeight = if (source.id == selectedSource.id) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (source.hasAuth) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Authenticated catalog",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp) // touch-target-ok
                                                )
                                            }
                                        }
                                        if (source.description != null) {
                                            Text(
                                                text = source.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                trailingIcon = if (!source.isBuiltIn) {
                                    {
                                        IconButton(onClick = {
                                            showSourceDropdown = false
                                            onDeleteSource(source.id)
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete custom feed",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                } else null,
                                onClick = {
                                    showSourceDropdown = false
                                    onSelectSource(source)
                                }
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (canNavigateBack) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onSearchToggle) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search catalog")
                }
                IconButton(onClick = onAddFeedClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom feed")
                }
                IconButton(onClick = onRefreshClick) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh catalog")
                }
            }
        )
    }
}

@Composable
private fun OpdsBreadcrumbBar(
    breadcrumbs: List<BreadcrumbItem>,
    onBreadcrumbClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, item ->
            val isLast = index == breadcrumbs.lastIndex
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(enabled = !isLast) { onBreadcrumbClick(index) }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp) // touch-target-ok
                )
            }
        }
    }
}

@Composable
private fun FeedContentView(
    feed: OpdsFeedResult,
    viewModel: OpdsViewModel,
    downloadStatuses: Map<String, DownloadStatus>,
    onSubfeedClick: (OpdsNavigationLink) -> Unit,
    onBookClick: (OpdsBook) -> Unit,
    onDownloadClick: (OpdsBook) -> Unit,
    onOpenInReader: (LibraryBook) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val columns = when {
        screenWidth < 600 -> 2
        screenWidth < 840 -> 3
        else -> 4
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize()
    ) {
        // Navigation subsections (e.g. Categories, Genres)
        if (feed.navigationLinks.isNotEmpty()) {
            item(span = { GridItemSpan(columns) }) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = "Categories & Shelves",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        feed.navigationLinks.forEach { navLink ->
                            SuggestionChip(
                                onClick = { onSubfeedClick(navLink) },
                                label = { Text(navLink.title) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp) // touch-target-ok
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Direct books
        if (feed.books.isNotEmpty()) {
            item(span = { GridItemSpan(columns) }) {
                Text(
                    text = "Books (${feed.books.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }

            items(feed.books, key = { it.id }) { book ->
                val status = downloadStatuses[book.id] ?: DownloadStatus.Idle
                val isInLibrary = viewModel.isBookInLibrary(book)

                OpdsBookTile(
                    book = book,
                    downloadStatus = status,
                    isInLibrary = isInLibrary,
                    authHeader = viewModel.currentAuthHeader,
                    onClick = { onBookClick(book) },
                    onDownloadClick = { onDownloadClick(book) }
                )
            }
        }

        // Groups / Collections in feed
        feed.groups.forEach { group ->
            if (group.books.isNotEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }

                items(group.books, key = { "${group.title}_${it.id}" }) { book ->
                    val status = downloadStatuses[book.id] ?: DownloadStatus.Idle
                    val isInLibrary = viewModel.isBookInLibrary(book)

                    OpdsBookTile(
                        book = book,
                        downloadStatus = status,
                        isInLibrary = isInLibrary,
                        authHeader = viewModel.currentAuthHeader,
                        onClick = { onBookClick(book) },
                        onDownloadClick = { onDownloadClick(book) }
                    )
                }
            }
        }

        // Empty state when nothing returned
        if (feed.books.isEmpty() && feed.groups.isEmpty() && feed.navigationLinks.isEmpty()) {
            item(span = { GridItemSpan(columns) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Padding.screen),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp) // touch-target-ok
                        )
                        Text(
                            text = "No books found in this section",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpdsBookTile(
    book: OpdsBook,
    downloadStatus: DownloadStatus,
    isInLibrary: Boolean,
    authHeader: String? = null,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(Spacing.xs)) {
            val coverBitmap = rememberOpdsCoverBitmap(book.coverUrl, authHeader)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(Spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp) // touch-target-ok
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // In-library badge
                if (isInLibrary) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.xs),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        SurfaceBadge(label = "In Library", color = MaterialTheme.colorScheme.primaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!book.author.isNullOrBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (downloadStatus) {
                is DownloadStatus.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { downloadStatus.progressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Downloading ${(downloadStatus.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is DownloadStatus.Importing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp)) // touch-target-ok
                        Text(
                            text = "Importing...",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                is DownloadStatus.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp) // touch-target-ok
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Added",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is DownloadStatus.Error -> {
                    Text(
                        text = "Download failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                DownloadStatus.Idle -> {
                    if (!isInLibrary && !book.downloadUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp) // touch-target-ok
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Get EPUB", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurfaceBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color)
            .padding(horizontal = Spacing.xs, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OpdsBookDetailDialog(
    book: OpdsBook,
    downloadStatus: DownloadStatus,
    isInLibrary: Boolean,
    onDismiss: () -> Unit,
    onDownloadClick: () -> Unit,
    onOpenInReader: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = book.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (!book.author.isNullOrBlank()) {
                    Text(
                        text = "By ${book.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (book.categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        book.categories.forEach { category ->
                            AssistChip(
                                onClick = {},
                                label = { Text(category) }
                            )
                        }
                    }
                }

                if (!book.summary.isNullOrBlank()) {
                    Text(
                        text = book.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (downloadStatus) {
                    is DownloadStatus.Downloading -> {
                        LinearProgressIndicator(
                            progress = { downloadStatus.progressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Downloading... ${(downloadStatus.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    is DownloadStatus.Importing -> {
                        Text(
                            text = "Importing into your Leafline library...",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    is DownloadStatus.Error -> {
                        Text(
                            text = "Download error: ${downloadStatus.message}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (isInLibrary) {
                Button(onClick = onOpenInReader) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Read Book")
                }
            } else if (!book.downloadUrl.isNullOrBlank()) {
                Button(
                    onClick = onDownloadClick,
                    enabled = downloadStatus !is DownloadStatus.Downloading && downloadStatus !is DownloadStatus.Importing
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Download & Add to Library")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private enum class OpdsAuthType(val label: String) {
    NONE("None"),
    BASIC("Basic Auth"),
    BEARER("API Token")
}

@Composable
private fun AddOpdsFeedDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String?, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var authType by remember { mutableStateOf(OpdsAuthType.NONE) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Catalog Feed") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Add an OPDS catalog URL from Calibre-Web, Kavita, or any open catalog feed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Feed Name") },
                    placeholder = { Text("My Calibre Library") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        isError = false
                    },
                    label = { Text("OPDS URL") },
                    placeholder = { Text("http://192.168.1.10:8080/opds") },
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("URL must start with http:// or https://") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Authentication",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.xs)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    OpdsAuthType.entries.forEach { type ->
                        FilterChip(
                            selected = authType == type,
                            onClick = { authType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                when (authType) {
                    OpdsAuthType.NONE -> { /* No additional fields */ }
                    OpdsAuthType.BASIC -> {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            placeholder = { Text("admin") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OpdsAuthType.BEARER -> {
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = { Text("API / Bearer Token") },
                            placeholder = { Text("kavita-api-key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = url.trim()
                    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                        val finalUsername = if (authType == OpdsAuthType.BASIC) username.trim().ifBlank { null } else null
                        val finalPassword = if (authType == OpdsAuthType.BASIC) password.ifBlank { null } else null
                        val finalToken = if (authType == OpdsAuthType.BEARER) token.trim().ifBlank { null } else null
                        onAdd(
                            title.trim().ifEmpty { "Custom Catalog" },
                            trimmed,
                            finalUsername,
                            finalPassword,
                            finalToken
                        )
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Add Feed")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
