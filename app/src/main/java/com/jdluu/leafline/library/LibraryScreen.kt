package com.jdluu.leafline.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import com.jdluu.leafline.library.insights.ReadingStreakHelper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jdluu.leafline.library.cover.BitmapMemoryCache
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.InputChip
import androidx.compose.material3.AssistChip
import androidx.compose.material.icons.filled.BarChart
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ListItem
import androidx.compose.material3.CardDefaults
import com.jdluu.leafline.library.data.ReadingAggregate
import java.time.LocalDate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.leafline.LibraryViewModel
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.library.rescan.FolderRescanResult
import com.jdluu.leafline.library.rescan.FolderRescanUiState
import com.jdluu.leafline.library.rescan.FolderRescanViewModel
import com.jdluu.leafline.library.rescan.RescanSummary
import com.jdluu.leafline.sync.ReadingProgressMath
import com.jdluu.leafline.theme.IconSize
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    foldersViewModel: LibraryFolderViewModel? = null,
    folderRescanViewModel: FolderRescanViewModel? = null,
    onImportEpub: () -> Unit,
    onAddLibraryFolder: () -> Unit = {},
    onReselectFolder: (String) -> Unit = {},
    onOpenBook: (LibraryBook) -> Unit
) {
    val books by viewModel.sortedBooks.collectAsStateWithLifecycle()
    val isLibraryLoaded by viewModel.isLibraryLoaded.collectAsStateWithLifecycle()
    val currentSort by viewModel.sort.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedCollection.collectAsStateWithLifecycle()
    val readingStatusFilter by viewModel.readingStatusFilter.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val continueReadingBooks by viewModel.continueReadingBooks.collectAsStateWithLifecycle()
    var searchActive by remember { mutableStateOf(false) }
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var collectionMenuExpanded by remember { mutableStateOf(false) }
    var detailSheetBook by remember { mutableStateOf<LibraryBook?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<LibraryBook?>(null) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedBookIds by remember { mutableStateOf(emptySet<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchStatusMenu by remember { mutableStateOf(false) }
    var showBatchCollectionDialog by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    val tagCounts by viewModel.tagCounts.collectAsStateWithLifecycle()

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedBookIds = emptySet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            "${selectedBookIds.size} selected",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedBookIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedBookIds = if (selectedBookIds.size == books.size) emptySet() else books.map { it.stableId }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select or deselect all")
                        }
                        if (selectedBookIds.isNotEmpty()) {
                            Box {
                                IconButton(onClick = { showBatchStatusMenu = true }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Set reading status")
                                }
                                DropdownMenu(
                                    expanded = showBatchStatusMenu,
                                    onDismissRequest = { showBatchStatusMenu = false }
                                ) {
                                    ReadingStatus.entries.forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(status.displayName) },
                                            onClick = {
                                                viewModel.setBooksReadingStatus(selectedBookIds, status)
                                                showBatchStatusMenu = false
                                                selectionMode = false
                                                selectedBookIds = emptySet()
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { showBatchTagDialog = true }) {
                                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Edit tags for selected books")
                            }
                            if (collections.isNotEmpty()) {
                                IconButton(onClick = { showBatchCollectionDialog = true }) {
                                    Icon(Icons.Default.CollectionsBookmark, contentDescription = "Add selected to collection")
                                }
                            }
                            IconButton(onClick = { showBatchDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete selected books")
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Eco,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp) // touch-target-ok: decorative leaf glyph
                            .padding(end = 4.dp)
                    )
                    Text(
                        "Leafline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        if (searchActive) {
                            viewModel.setQuery("")
                        }
                        searchActive = !searchActive
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search library")
                }
                Box {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort library")
                    }
                    LibrarySortMenu(
                        expanded = menuExpanded,
                        selected = currentSort,
                        onSelect = { sort ->
                            viewModel.setSort(sort)
                            menuExpanded = false
                        },
                        onDismiss = { menuExpanded = false }
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleLayout() }
                ) {
                    Icon(
                        imageVector = if (layout == LibraryLayout.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = if (layout == LibraryLayout.GRID) "Switch to list view" else "Switch to grid view"
                    )
                }
                Box {
                    var overflowMenuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { overflowMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = overflowMenuExpanded,
                        onDismissRequest = { overflowMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select books") },
                            leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                            onClick = {
                                overflowMenuExpanded = false
                                selectionMode = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reading insights") },
                            leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                            onClick = {
                                overflowMenuExpanded = false
                                showInsightsSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Collections") },
                            leadingIcon = { Icon(Icons.Default.CollectionsBookmark, contentDescription = null) },
                            onClick = {
                                overflowMenuExpanded = false
                                showCollectionSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Manage folders") },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                            onClick = {
                                overflowMenuExpanded = false
                                foldersViewModel?.refresh()
                                showFolderSheet = true
                            }
                        )
                    }
                }
            }
        )
        }

        LibraryFilterBar(
            searchActive = searchActive,
            query = query,
            onQueryChange = viewModel::setQuery,
            onCloseSearch = {
                viewModel.setQuery("")
                searchActive = false
            },
            collections = collections,
            selectedCollection = selectedCollection,
            onSelectCollection = { viewModel.selectCollection(it) },
            readingStatusFilter = readingStatusFilter,
            onSelectReadingStatus = { viewModel.setReadingStatusFilter(it) },
            allTags = allTags,
            selectedTag = selectedTag,
            tagCounts = tagCounts,
            onSelectTag = { viewModel.selectTag(it) }
        )

        if (books.isEmpty()) {
            when {
                !isLibraryLoaded -> LibraryLoadingState()
                query.isNotBlank() -> LibraryEmptyState(
                    title = "No books match",
                    body = "Try a different search term"
                )
                selectedCollection != null -> LibraryEmptyState(
                    title = "Collection is empty",
                    body = "Books added to this collection will appear here"
                )
                readingStatusFilter != null -> LibraryEmptyState(
                    title = "No books with this status",
                    body = "Try a different filter"
                )
                selectedTag != null -> LibraryEmptyState(
                    title = "No books with this tag",
                    body = "Try selecting a different tag or clear the filter"
                )
                else -> LibraryEmptyState(
                    title = "Your library awaits",
                    body = "Import an EPUB or add a local folder to start reading",
                    actionLabel = "Import EPUB",
                    onAction = onImportEpub
                )
            }
        } else if (layout == LibraryLayout.LIST) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Padding.screen, end = Padding.screen, top = Spacing.xs, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(Padding.compact)
            ) {
                if (continueReadingBooks.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = Spacing.xs)) {
                            Text(
                                "Continue reading",
                                style = MaterialTheme.typography.titleMedium
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.padding(vertical = Spacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(Padding.compact)
                            ) {
                                items(continueReadingBooks, key = { it.stableId }) { book ->
                                    ContinueReadingTile(book = book, onClick = { onOpenBook(book) })
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = if (readingStatusFilter != null) readingStatusFilter!!.displayName else "All books",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }

                items(items = books, key = { it.stableId }) { book ->
                    val isSelected = selectedBookIds.contains(book.stableId)
                    BookListRow(
                        book = book,
                        isSelected = isSelected,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                selectedBookIds = if (isSelected) selectedBookIds - book.stableId else selectedBookIds + book.stableId
                            } else {
                                onOpenBook(book)
                            }
                        },
                        onLongClick = {
                            if (selectionMode) {
                                selectedBookIds = if (isSelected) selectedBookIds - book.stableId else selectedBookIds + book.stableId
                            } else {
                                detailSheetBook = book
                            }
                        },
                        collections = collections,
                        bookCollectionIds = viewModel
                            .collectionIdsForBook(book.stableId)
                            .collectAsStateWithLifecycle(emptySet())
                            .value,
                        onAddToCollection = { collectionId ->
                            viewModel.addBookToCollection(book.stableId, collectionId)
                        },
                        onRemoveFromCollection = { collectionId ->
                            viewModel.removeBookFromCollection(book.stableId, collectionId)
                        }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(rememberLibraryGridColumns()),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Padding.screen, end = Padding.screen, top = Spacing.xs, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(Padding.compact),
                horizontalArrangement = Arrangement.spacedBy(Padding.compact)
            ) {
                if (continueReadingBooks.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.padding(top = Spacing.xs)) {
                            Text(
                                "Continue reading",
                                style = MaterialTheme.typography.titleMedium
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.padding(vertical = Spacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(Padding.compact)
                            ) {
                                items(continueReadingBooks, key = { it.stableId }) { book ->
                                    ContinueReadingTile(book = book, onClick = { onOpenBook(book) })
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = if (readingStatusFilter != null) readingStatusFilter!!.displayName else "All books",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }

                items(items = books, key = { it.stableId }) { book ->
                    val isSelected = selectedBookIds.contains(book.stableId)
                    BookGridTile(
                        book = book,
                        isSelected = isSelected,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                selectedBookIds = if (isSelected) selectedBookIds - book.stableId else selectedBookIds + book.stableId
                            } else {
                                onOpenBook(book)
                            }
                        },
                        onLongClick = {
                            if (selectionMode) {
                                selectedBookIds = if (isSelected) selectedBookIds - book.stableId else selectedBookIds + book.stableId
                            } else {
                                detailSheetBook = book
                            }
                        },
                        collections = collections,
                        bookCollectionIds = viewModel
                            .collectionIdsForBook(book.stableId)
                            .collectAsStateWithLifecycle(emptySet())
                            .value,
                        onAddToCollection = { collectionId ->
                            viewModel.addBookToCollection(book.stableId, collectionId)
                        },
                        onRemoveFromCollection = { collectionId ->
                            viewModel.removeBookFromCollection(book.stableId, collectionId)
                        }
                    )
                }
            }
        }
    }

    // Import FAB — always in reach, never blocks content
    FloatingActionButton(
        onClick = onImportEpub,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(Padding.screen)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Import EPUB")
    }
    }

    // Collection management sheet
    if (showCollectionSheet) {
        CollectionManagementSheet(
            collections = collections,
            onDismiss = { showCollectionSheet = false },
            onCreate = { showCreateDialog = true },
            onDelete = { collection -> viewModel.deleteCollection(collection.id) }
        )
    }

    // Folder management sheet
    if (showFolderSheet && foldersViewModel != null) {
        val rescanState by folderRescanViewModel?.state?.collectAsStateWithLifecycle()
            ?: remember { mutableStateOf<FolderRescanUiState>(FolderRescanUiState.Idle) }
        LibraryFoldersSheet(
            folders = foldersViewModel.folders.collectAsStateWithLifecycle().value,
            onDismiss = { showFolderSheet = false },
            onAddFolder = { onAddLibraryFolder() },
            onRemove = { uri -> foldersViewModel.remove(uri) },
            onReselect = { uri -> onReselectFolder(uri) },
            rescanState = rescanState,
            onRescanAll = {
                folderRescanViewModel?.rescanAll(
                    foldersViewModel.folders.value.map { it.uri }
                )
            },
            onRescanOne = { uri -> folderRescanViewModel?.rescanOne(uri) }
        )
    }

    // Reading insights sheet
    if (showInsightsSheet) {
        ReadingInsightsSheet(
            viewModel = viewModel,
            onDismiss = { showInsightsSheet = false }
        )
    }

    // Create collection dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New collection") },
            text = {
                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    placeholder = { Text("Collection name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            viewModel.createCollection(newCollectionName.trim())
                            newCollectionName = ""
                            showCreateDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Book detail sheet (long-press)
    detailSheetBook?.let { book ->
        val currentBook = books.find { it.stableId == book.stableId } ?: book
        BookDetailSheet(
            book = currentBook,
            collections = collections,
            availableTags = allTags,
            bookCollectionIds = viewModel
                .collectionIdsForBook(book.stableId)
                .collectAsStateWithLifecycle(emptySet())
                .value,
            onDismiss = { detailSheetBook = null },
            onDelete = {
                detailSheetBook = null
                showDeleteConfirm = book
            },
            onSetReadingStatus = { status -> viewModel.setBookReadingStatus(book.stableId, status) },
            onSetRating = { rating -> viewModel.setRating(book.stableId, rating) },
            onAddTag = { tag -> viewModel.addTag(book.stableId, tag) },
            onRemoveTag = { tag -> viewModel.removeTag(book.stableId, tag) },
            onAddToCollection = { id -> viewModel.addBookToCollection(book.stableId, id) },
            onRemoveFromCollection = { id -> viewModel.removeBookFromCollection(book.stableId, id) },
            onOpen = {
                detailSheetBook = null
                onOpenBook(book)
            },
            onGetReadingStats = { viewModel.getReadingAggregateForBook(book.stableId) }
        )
    }

    // Batch delete confirmation dialog
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Delete books?") },
            text = { Text("Remove ${selectedBookIds.size} books from your library? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBooks(selectedBookIds)
                        showBatchDeleteConfirm = false
                        selectionMode = false
                        selectedBookIds = emptySet()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Batch tag dialog
    if (showBatchTagDialog && selectedBookIds.isNotEmpty()) {
        BatchTagDialog(
            selectedBookCount = selectedBookIds.size,
            allTags = allTags,
            onAddTag = { tag -> viewModel.addTagToBooks(selectedBookIds, tag) },
            onRemoveTag = { tag -> viewModel.removeTagFromBooks(selectedBookIds, tag) },
            onDismiss = { showBatchTagDialog = false }
        )
    }

    // Batch add to collection dialog
    if (showBatchCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showBatchCollectionDialog = false },
            title = { Text("Add to collection") },
            text = {
                LazyColumn {
                    items(collections) { collection ->
                        ListItem(
                            headlineContent = { Text(collection.name) },
                            modifier = Modifier.clickable {
                                viewModel.addBooksToCollection(selectedBookIds, collection.id)
                                showBatchCollectionDialog = false
                                selectionMode = false
                                selectedBookIds = emptySet()
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBatchCollectionDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete confirmation dialog
    showDeleteConfirm?.let { book ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete book?") },
            text = { Text("Remove \"${book.title}\" from your library? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBook(book.stableId)
                        showDeleteConfirm = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Loading surface shown until the library store has produced its first book
 * list. Kept separate from the empty state so a real empty library is never
 * confused with one that is still being read.
 */
@Composable
private fun LibraryLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading library" }
        )
    }
}

/**
 * Branded empty surface: the leaf mark on the soft container tone, a short
 * heading, a one-line explanation, and one optional primary action.
 */
@Composable
private fun LibraryEmptyState(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Filled.Eco,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp) // touch-target-ok: decorative leaf glyph
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(Spacing.md))
                FilledTonalButton(onClick = onAction) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.small) // touch-target-ok: decorative icon in button
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(actionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionManagementSheet(
    collections: List<Collection>,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onDelete: (Collection) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(Padding.screen)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Collections",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onCreate) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon in button
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }
            Spacer(Modifier.height(Padding.compact))
            if (collections.isEmpty()) {
                Text(
                    "No collections yet. Create one to start organizing your books.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.md)
                )
            } else {
                LazyColumn {
                    items(collections) { collection ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CollectionsBookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp) // touch-target-ok: decorative icon in row
                            )
                            Spacer(Modifier.width(Padding.compact))
                            Text(
                                collection.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDelete(collection) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete ${collection.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryFoldersSheet(
    folders: List<LibraryFolder>,
    onDismiss: () -> Unit,
    onAddFolder: () -> Unit,
    onRemove: (String) -> Unit,
    onReselect: (String) -> Unit,
    rescanState: FolderRescanUiState = FolderRescanUiState.Idle,
    onRescanAll: () -> Unit = {},
    onRescanOne: (String) -> Unit = {}
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(Padding.screen)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Saved folders",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onRescanAll,
                        enabled = folders.isNotEmpty() &&
                            rescanState !is FolderRescanUiState.Running
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.small) // touch-target-ok: decorative icon in button
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (rescanState is FolderRescanUiState.Running) "Rescanning…" else "Rescan")
                    }
                    TextButton(onClick = onAddFolder) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon in button
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
            Text(
                "Books in these local folders are available in your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )
            Spacer(Modifier.height(Padding.compact))
            Box(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                when (val state = rescanState) {
                    is FolderRescanUiState.Running -> {
                        Column {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xs)
                            )
                            Text(
                                "Rescanning saved folders…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Padding.compact)
                            )
                        }
                    }
                    is FolderRescanUiState.Done -> {
                        RescanSummaryRow(summary = state.summary)
                    }
                    is FolderRescanUiState.Error -> {
                        Text(
                            text = state.message.ifBlank { "Rescan failed unexpectedly" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = Padding.compact)
                        )
                    }
                    FolderRescanUiState.Idle -> {}
                }
            }
            if (folders.isEmpty()) {
                Text(
                    "No saved folders. Add a folder to import its EPUBs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.md)
                )
            } else {
                LazyColumn {
                    items(folders, key = { it.uri }) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = if (folder.accessible) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(24.dp) // touch-target-ok: decorative icon in row
                            )
                            Spacer(Modifier.width(Padding.compact))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    folder.label ?: "Unnamed folder",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    if (folder.accessible) {
                                        "Accessible"
                                    } else {
                                        "Permission lost, reselect to restore"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (folder.accessible) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            if (!folder.accessible) {
                                TextButton(onClick = { onReselect(folder.uri) }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon in button
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reselect")
                                }
                            } else {
                                TextButton(
                                    onClick = { onRescanOne(folder.uri) },
                                    enabled = rescanState !is FolderRescanUiState.Running
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon in button
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Rescan")
                                }
                            }
                            IconButton(onClick = { onRemove(folder.uri) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove ${folder.label ?: "folder"}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

/**
 * Accessible summary of the most recent folder rescan, including per-folder
 * failures.
 */
@Composable
private fun RescanSummaryRow(
    summary: RescanSummary
) {
    val totals = summary.totals
    val total = totals.total
    val failureCount = summary.folderResults.count { it is FolderRescanResult.Failed }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Padding.compact),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(Padding.compact)) {
            Text(
                text = if (total == 0 && failureCount == 0) {
                    "No EPUBs found to rescan"
                } else {
                    "Rescan complete: $total file(s)"
                },
                style = MaterialTheme.typography.titleSmall
            )
            if (total > 0 || failureCount > 0) {
                Text(
                    text = buildList {
                        if (totals.added > 0) add("${totals.added} added")
                        if (totals.updated > 0) add("${totals.updated} updated")
                        if (totals.skipped > 0) add("${totals.skipped} unchanged")
                        if (totals.failedFiles > 0) add("${totals.failedFiles} failed")
                        if (failureCount > 0) add("${failureCount} folder(s) unreachable")
                    }.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (failureCount > 0) {
                summary.folderResults.filterIsInstance<FolderRescanResult.Failed>()
                    .forEach { failed ->
                        Text(
                            text = failed.reason.ifBlank { "Folder unreachable" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = Spacing.xs)
                        )
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookCollectionPickerSheet(
    bookStableId: String,
    collections: List<Collection>,
    bookCollectionIds: Set<Long>,
    onDismiss: () -> Unit,
    onAddToCollection: (Long) -> Unit,
    onRemoveFromCollection: (Long) -> Unit
) {
    var optimisticIds by remember(bookCollectionIds) { mutableStateOf(bookCollectionIds) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(Padding.screen)) {
            Text(
                "Collections",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Padding.compact)
            )
            if (collections.isEmpty()) {
                Text(
                    "No collections yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(collections, key = { it.id }) { collection ->
                        val isMember = collection.id in optimisticIds
                        FilterChip(
                            selected = isMember,
                            onClick = {
                                optimisticIds = if (isMember) {
                                    optimisticIds - collection.id
                                } else {
                                    optimisticIds + collection.id
                                }
                                if (isMember) onRemoveFromCollection(collection.id)
                                else onAddToCollection(collection.id)
                            },
                            label = { Text(collection.name) },
                            leadingIcon = {
                                Icon(
                                    if (isMember) Icons.Default.Bookmark
                                    else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp) // touch-target-ok: decorative chip icon
                                )
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySortMenu(
    expanded: Boolean,
    selected: LibrarySort,
    onSelect: (LibrarySort) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        LibrarySort.entries.forEachIndexed { index, option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                onClick = { onSelect(option) },
                leadingIcon = {
                    if (option == selected) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                    }
                }
            )
            if (index != LibrarySort.entries.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

internal fun libraryGridColumnsForWidth(widthDp: Int): Int {
    return when {
        widthDp < 600 -> 2
        widthDp < 840 -> 3
        else -> 5
    }
}

@Composable
fun rememberLibraryGridColumns(): Int {
    val configuration = LocalConfiguration.current
    return libraryGridColumnsForWidth(configuration.screenWidthDp)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookGridTile(
    book: LibraryBook,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    collections: List<Collection> = emptyList(),
    bookCollectionIds: Set<Long> = emptySet(),
    onAddToCollection: (Long) -> Unit = {},
    onRemoveFromCollection: (Long) -> Unit = {}
) {
    var showCollectionPicker by remember { mutableStateOf(false) }
    Card(
        border = if (selectionMode && isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = if (selectionMode) "Toggle selection" else "Open book",
                onClick = onClick,
                onLongClickLabel = if (selectionMode) "Toggle selection" else "Book details",
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(Padding.compact)) {
            val bitmap = rememberCoverBitmap(book.coverPath)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                Crossfade(
                    targetState = bitmap,
                    animationSpec = tween(durationMillis = 200),
                    label = "gridCoverCrossfade"
                ) { bmp ->
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CoverPlaceholder(title = book.title)
                    }
                }

                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .padding(Spacing.xs),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Reading status badge
            if (book.readingStatus != ReadingStatus.UNREAD) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(
                            when (book.readingStatus) {
                                ReadingStatus.READING -> MaterialTheme.colorScheme.primary
                                ReadingStatus.FINISHED -> MaterialTheme.colorScheme.secondary
                                ReadingStatus.UNREAD -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = Spacing.xs, vertical = 2.dp)
                ) {
                    Text(
                        text = book.readingStatus.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (collections.isNotEmpty()) {
                    IconButton(
                        onClick = { showCollectionPicker = true }
                    ) {
                        Icon(
                            Icons.Default.CollectionsBookmark,
                            contentDescription = "Collections for ${book.title}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp) // touch-target-ok: decorative glyph inside icon button
                        )
                    }
                }
            }
            if (book.authors.isNotEmpty()) {
                Text(
                    text = book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if ((book.rating != null && book.rating > 0) || book.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (book.rating != null && book.rating > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = Spacing.xs)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "${book.rating} stars",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp) // touch-target-ok: decorative star icon
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "${book.rating}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (book.tags.isNotEmpty()) {
                        Text(
                            text = book.tags.joinToString(" ") { "#$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }

    if (showCollectionPicker) {
        BookCollectionPickerSheet(
            bookStableId = book.stableId,
            collections = collections,
            bookCollectionIds = bookCollectionIds,
            onDismiss = { showCollectionPicker = false },
            onAddToCollection = { id ->
                onAddToCollection(id)
                showCollectionPicker = false
            },
            onRemoveFromCollection = { id ->
                onRemoveFromCollection(id)
                showCollectionPicker = false
            }
        )
    }
}

@Composable
internal fun CoverPlaceholder(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Padding.compact),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun rememberCoverBitmap(path: String?): Bitmap? {
    if (path == null) return null
    val cached = remember(path) { BitmapMemoryCache.get(path) }
    if (cached != null) return cached

    return produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            BitmapMemoryCache.decodeSampledBitmap(path)
        }
    }.value
}

@Composable
private fun ContinueReadingTile(book: LibraryBook, onClick: () -> Unit) {
    val progressFraction = ReadingProgressMath.percentageFromLocator(book.lastLocatorJson)
        ?.div(ReadingProgressMath.FULL_PERCENT)
        ?.coerceIn(0.0, 1.0)
        ?.toFloat()
    val progressLabel = ReadingProgressMath.progressLabel(book.lastLocatorJson)

    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClickLabel = "Continue reading", onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (progressLabel != null) {
                    contentDescription = "${book.title}, $progressLabel"
                }
            }
    ) {
        Column {
            val bitmap = rememberCoverBitmap(book.coverPath)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                Crossfade(
                    targetState = bitmap,
                    animationSpec = tween(durationMillis = 200),
                    label = "continueReadingCoverCrossfade"
                ) { bmp ->
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CoverPlaceholder(title = book.title)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )
            if (progressFraction != null && progressLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xs)
                )
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.xs)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookDetailSheet(
    book: LibraryBook,
    collections: List<Collection>,
    bookCollectionIds: Set<Long>,
    availableTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSetReadingStatus: (ReadingStatus) -> Unit,
    onSetRating: (Int?) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onAddToCollection: (Long) -> Unit,
    onRemoveFromCollection: (Long) -> Unit,
    onOpen: () -> Unit,
    onGetReadingStats: suspend () -> ReadingAggregate = { ReadingAggregate.EMPTY }
) {
    var optimisticStatus by remember(book.readingStatus) { mutableStateOf(book.readingStatus) }
    var optimisticRating by remember(book.rating) { mutableStateOf(book.rating) }
    var optimisticTags by remember(book.tags) { mutableStateOf(book.tags) }
    var optimisticCollectionIds by remember(bookCollectionIds) { mutableStateOf(bookCollectionIds) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState())
        ) {
            // Cover + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                val bitmap = rememberCoverBitmap(book.coverPath)
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(COVER_ASPECT_RATIO)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Crossfade(
                        targetState = bitmap,
                        animationSpec = tween(durationMillis = 200),
                        label = "detailCoverCrossfade"
                    ) { bmp ->
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleLarge)
                    if (book.authors.isNotEmpty()) {
                        Text(
                            book.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val progressFraction = ReadingProgressMath.percentageFromLocator(book.lastLocatorJson)
                        ?.div(ReadingProgressMath.FULL_PERCENT)
                    val progressLabel = ReadingProgressMath.progressLabel(book.lastLocatorJson)
                    if (progressFraction != null) {
                        Spacer(Modifier.height(Spacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (progressLabel != null) {
                                Text(
                                    text = progressLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Button(onClick = onOpen) { Text("Open book") }
                }
            }

            if (!book.description.isNullOrBlank()) {
                var descriptionExpanded by remember { mutableStateOf(false) }
                Spacer(Modifier.height(Spacing.sm))
                HorizontalDivider()
                Spacer(Modifier.height(Padding.compact))
                Text("Description", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.description.length > 180) {
                    TextButton(
                        onClick = { descriptionExpanded = !descriptionExpanded },
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(if (descriptionExpanded) "Show less" else "Read more")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(Padding.compact))

            // Metadata section
            Text("Metadata", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.xs))
            MetadataRow("Status", optimisticStatus.displayName)
            optimisticRating?.let { MetadataRow("Rating", "$it / 5 stars") }
            if (optimisticTags.isNotEmpty()) {
                MetadataRow("Tags", optimisticTags.joinToString(", ") { "#$it" })
            }
            book.publisher?.let { MetadataRow("Publisher", it) }
            book.pageCount?.let { MetadataRow("Pages", it.toString()) }
            book.language?.let { MetadataRow("Language", it) }
            book.publishedAtEpochMillis?.let { millis ->
                MetadataRow("Published", formatEpochMillis(millis))
            }
            book.addedAtEpochMillis?.let { millis ->
                MetadataRow("Added", formatEpochMillis(millis))
            }
            book.lastReadAtEpochMillis?.let { millis ->
                MetadataRow("Last read", formatEpochMillis(millis))
            }

            var readingStats by remember(book.stableId) { mutableStateOf<ReadingAggregate?>(null) }
            LaunchedEffect(book.stableId) {
                readingStats = onGetReadingStats()
            }
            readingStats?.let { stats ->
                if (stats.sessionCount > 0) {
                    Spacer(Modifier.height(Spacing.sm))
                    HorizontalDivider()
                    Spacer(Modifier.height(Padding.compact))
                    Text("Reading activity", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(Spacing.xs))
                    MetadataRow("Time read", formatReadingDuration(stats.activeDurationMillis))
                    MetadataRow("Sessions", "${stats.sessionCount}")
                    val avgMillis = stats.activeDurationMillis / stats.sessionCount
                    MetadataRow("Average session", formatReadingDuration(avgMillis))
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider()
            Spacer(Modifier.height(Padding.compact))

            // File info section
            Text("File info", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.xs))
            MetadataRow("Path", book.filePath)
            MetadataRow("Hash", book.fileHash.take(12) + "…")
            book.koreaderHash?.let { MetadataRow("KOReader hash", it.take(12) + "…") }

            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider()
            Spacer(Modifier.height(Padding.compact))

            // Actions section
            Text("Actions", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.xs))

            // Reading status buttons
            Text("Mark as:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                ReadingStatus.entries.forEach { status ->
                    FilterChip(
                        selected = optimisticStatus == status,
                        onClick = {
                            optimisticStatus = status
                            onSetReadingStatus(status)
                        },
                        label = {
                            Text(status.displayName)
                        }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Rating
            Text("Rating:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.padding(top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (star in 1..5) {
                    val isFilled = (optimisticRating ?: 0) >= star
                    IconButton(
                        onClick = {
                            val newRating = if (optimisticRating == star) null else star
                            optimisticRating = newRating
                            onSetRating(newRating)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star of 5 stars",
                            tint = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = if (optimisticRating != null && optimisticRating!! > 0) "${optimisticRating}/5" else "Unrated",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (optimisticRating != null) {
                    Spacer(Modifier.width(Spacing.xs))
                    TextButton(
                        onClick = {
                            optimisticRating = null
                            onSetRating(null)
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Tags
            Text("Tags:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(Spacing.xs))

            if (optimisticTags.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    optimisticTags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = {
                                optimisticTags = optimisticTags - tag
                                onRemoveTag(tag)
                            },
                            label = { Text("#$tag") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove tag $tag",
                                    modifier = Modifier.size(16.dp) // touch-target-ok: decorative chip icon
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
            }

            var newTagInput by remember { mutableStateOf("") }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("Add a tag…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Spacing.xs))
                IconButton(
                    onClick = {
                        val trimmed = newTagInput.trim()
                        if (trimmed.isNotEmpty() && !optimisticTags.any { it.equals(trimmed, ignoreCase = true) }) {
                            optimisticTags = optimisticTags + trimmed
                            onAddTag(trimmed)
                            newTagInput = ""
                        }
                    },
                    enabled = newTagInput.isNotBlank(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add tag")
                }
            }

            val unassignedLibraryTags = availableTags.filterNot { tag ->
                optimisticTags.any { it.equals(tag, ignoreCase = true) }
            }
            if (unassignedLibraryTags.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Suggested:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    unassignedLibraryTags.take(8).forEach { tag ->
                        AssistChip(
                            onClick = {
                                optimisticTags = optimisticTags + tag
                                onAddTag(tag)
                            },
                            label = { Text("+$tag") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Collection management
            if (collections.isNotEmpty()) {
                Text("Collections:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                // Toggle row for each collection
                collections.forEach { collection ->
                    val isMember = collection.id in optimisticCollectionIds
                    FilterChip(
                        selected = isMember,
                        onClick = {
                            optimisticCollectionIds = if (isMember) {
                                optimisticCollectionIds - collection.id
                            } else {
                                optimisticCollectionIds + collection.id
                            }
                            if (isMember) onRemoveFromCollection(collection.id)
                            else onAddToCollection(collection.id)
                        },
                        label = { Text(collection.name) },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Delete button
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(4.dp))
                Text("Delete from library", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatEpochMillis(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
    return sdf.format(java.util.Date(millis))
}

private const val COVER_ASPECT_RATIO = 0.7f
internal fun formatReadingDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        totalMinutes < 1L -> "< 1 min"
        hours == 0L -> "$minutes min"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingInsightsSheet(
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    var todayStats by remember { mutableStateOf(ReadingAggregate.EMPTY) }
    var weeklyStats by remember { mutableStateOf(ReadingAggregate.EMPTY) }
    var recentDailyStats by remember { mutableStateOf<List<Pair<LocalDate, ReadingAggregate>>>(emptyList()) }
    val currentStreak = remember(recentDailyStats) {
        ReadingStreakHelper.calculateCurrentStreak(recentDailyStats)
    }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        todayStats = viewModel.getTodayAggregate()
        weeklyStats = viewModel.getWeeklyAggregate()
        recentDailyStats = viewModel.getRecentDailyAggregates(7)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .padding(bottom = Spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Reading Insights",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "100% private and on-device. No analytics or tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.md))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = formatReadingDuration(todayStats.activeDurationMillis),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Time read",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Column {
                            Text(
                                text = "${todayStats.sessionCount}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sessions",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Column {
                            Text(
                                text = "${todayStats.booksOpened}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Books opened",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Column {
                            Text(
                                text = if (currentStreak > 0) "$currentStreak d" else "0 d",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (currentStreak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Streak",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = "Past 7 Days",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Total: ${formatReadingDuration(weeklyStats.activeDurationMillis)} across ${weeklyStats.sessionCount} sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.md))

            AnimatedVisibility(visible = selectedDayIndex != null) {
                selectedDayIndex?.let { idx ->
                    if (idx in recentDailyStats.indices) {
                        val (selectedDate, selectedAgg) = recentDailyStats[idx]
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.xs)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.dayOfMonth}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${formatReadingDuration(selectedAgg.activeDurationMillis)} • ${selectedAgg.sessionCount} session${if (selectedAgg.sessionCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            val maxMinutes = (recentDailyStats.maxOfOrNull { it.second.activeMinutes } ?: 0L).coerceAtLeast(15L)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                recentDailyStats.forEachIndexed { index, (date, agg) ->
                    val isSelected = selectedDayIndex == index
                    val isToday = date == LocalDate.now()
                    val targetRatio = (agg.activeMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.06f, 1f)
                    val animatedRatio by animateFloatAsState(
                        targetValue = targetRatio,
                        animationSpec = tween(durationMillis = 400),
                        label = "chartBarHeight"
                    )
                    val dayLabel = date.dayOfWeek.name.take(3)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                selectedDayIndex = if (selectedDayIndex == index) null else index
                            }
                            .padding(vertical = 4.dp)
                            .semantics {
                                contentDescription = "$dayLabel: ${agg.activeMinutes} minutes, ${agg.sessionCount} sessions"
                            }
                    ) {
                        Text(
                            text = if (agg.activeMinutes > 0) "${agg.activeMinutes}m" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 20.dp else 16.dp)
                                .height((70 * animatedRatio).dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        agg.activeMinutes > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                                .then(
                                    if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                                    else Modifier
                                )
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}


@Composable
private fun BatchTagDialog(
    selectedBookCount: Int,
    allTags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTagInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit tags ($selectedBookCount books)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Add or remove tags across all $selectedBookCount selected books.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it.take(30) },
                        placeholder = { Text("Add tag to all…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    IconButton(
                        onClick = {
                            val trimmed = newTagInput.trim().removePrefix("#")
                            if (trimmed.isNotEmpty()) {
                                onAddTag(trimmed)
                                newTagInput = ""
                            }
                        },
                        enabled = newTagInput.isNotBlank(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag to selected books")
                    }
                }

                if (allTags.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Library tags:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allTags.forEach { tag ->
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 10.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { onAddTag(tag) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add tag #$tag to selected books",
                                            modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon in 48dp IconButton
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveTag(tag) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove tag #$tag from selected books",
                                            modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon in 48dp IconButton
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Done")
            }
        }
    )
}
