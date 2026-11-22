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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Edit
import com.jdluu.leafline.library.ProgressFilter
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
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import com.jdluu.leafline.export.BookExportData
import com.jdluu.leafline.export.NotesExporter
import com.jdluu.leafline.export.ui.NotesExportSheet
import com.jdluu.leafline.library.backup.BackupRestoreSheet
import com.jdluu.leafline.library.backup.LibraryBackupManager
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    foldersViewModel: LibraryFolderViewModel? = null,
    folderRescanViewModel: FolderRescanViewModel? = null,
    backupManager: LibraryBackupManager? = null,
    notesExporter: NotesExporter? = null,
    onImportEpub: () -> Unit,
    onAddLibraryFolder: () -> Unit = {},
    onReselectFolder: (String) -> Unit = {},
    onOpenBook: (LibraryBook) -> Unit
) {
    val books by viewModel.sortedBooks.collectAsStateWithLifecycle()
    // Unfiltered count: filters over an empty library are noise, and the empty state
    // already carries the import action.
    val allBooks by viewModel.books.collectAsStateWithLifecycle()
    val libraryIsEmpty = allBooks.isEmpty()
    val isLibraryLoaded by viewModel.isLibraryLoaded.collectAsStateWithLifecycle()
    val currentSort by viewModel.sort.collectAsStateWithLifecycle()
    val currentSortAscending by viewModel.sortAscending.collectAsStateWithLifecycle()
    val progressFilter by viewModel.progressFilter.collectAsStateWithLifecycle()
    val collectionCounts by viewModel.collectionCounts.collectAsStateWithLifecycle()
    var renameCollectionTarget by remember { mutableStateOf<Collection?>(null) }
    var renameCollectionName by remember { mutableStateOf("") }
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedCollection.collectAsStateWithLifecycle()
    val readingStatusFilter by viewModel.readingStatusFilter.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val continueReadingBooks by viewModel.continueReadingBooks.collectAsStateWithLifecycle()
    var searchActive by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var collectionMenuExpanded by remember { mutableStateOf(false) }
    var detailSheetBook by remember { mutableStateOf<LibraryBook?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<LibraryBook?>(null) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedBookIds by remember { mutableStateOf(emptySet<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchStatusMenu by remember { mutableStateOf(false) }
    var showBatchCollectionDialog by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    val tagCounts by viewModel.tagCounts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var exportBookTarget by remember { mutableStateOf<LibraryBook?>(null) }
    var exportBookData by remember { mutableStateOf<BookExportData?>(null) }
    var exportBatchTarget by remember { mutableStateOf<List<BookExportData>?>(null) }

    LaunchedEffect(exportBookTarget) {
        val target = exportBookTarget
        if (target != null && notesExporter != null) {
            exportBookData = notesExporter.getExportDataForBook(target)
        } else {
            exportBookData = null
        }
    }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedBookIds = emptySet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectionMode) {
                var showSelectionOverflow by remember { mutableStateOf(false) }
                TopAppBar(
                    title = {
                        Text(
                            "${selectedBookIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            if (collections.isNotEmpty()) {
                                IconButton(onClick = { showBatchCollectionDialog = true }) {
                                    Icon(Icons.Default.CollectionsBookmark, contentDescription = "Add selected to collection")
                                }
                            }
                            Box {
                                IconButton(onClick = { showSelectionOverflow = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More bulk actions")
                                }
                                DropdownMenu(
                                    expanded = showSelectionOverflow,
                                    onDismissRequest = { showSelectionOverflow = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit tags") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                                        onClick = {
                                            showSelectionOverflow = false
                                            showBatchTagDialog = true
                                        }
                                    )
                                    if (notesExporter != null) {
                                        DropdownMenuItem(
                                            text = { Text("Export notes") },
                                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                            onClick = {
                                                showSelectionOverflow = false
                                                scope.launch {
                                                    val selectedBooks = books.filter { it.stableId in selectedBookIds }
                                                    val batchData = selectedBooks.map { book ->
                                                        notesExporter.getExportDataForBook(book)
                                                    }
                                                    exportBatchTarget = batchData
                                                }
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Delete books") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                        onClick = {
                                            showSelectionOverflow = false
                                            showBatchDeleteConfirm = true
                                        }
                                    )
                                }
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
                // A progress filter hides books, and the sheet is the only place it is
                // shown, so the button itself has to say when one is on: the accent tint
                // plus the spoken description, never the tint alone.
                val progressFilterActive = progressFilter != ProgressFilter.ALL
                IconButton(onClick = { showSortSheet = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = if (progressFilterActive) {
                            "Sort and filter library. Progress filter: ${progressFilter.displayName}"
                        } else {
                            "Sort and filter library"
                        },
                        tint = if (progressFilterActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        }
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
                        if (backupManager != null) {
                            DropdownMenuItem(
                                text = { Text("Backup & restore") },
                                leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    showBackupSheet = true
                                }
                            )
                        }
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

        val sectionTitle = when {
            selectedCollection != null ->
                collections.firstOrNull { it.id == selectedCollection }?.name ?: "Collection"
            readingStatusFilter != null -> readingStatusFilter!!.displayName
            else -> "All books"
        }

        if (!libraryIsEmpty) {
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
            collectionCounts = collectionCounts,
            onSelectCollection = { viewModel.selectCollection(it) },
            readingStatusFilter = readingStatusFilter,
            onSelectReadingStatus = { viewModel.setReadingStatusFilter(it) },
            allTags = allTags,
            selectedTag = selectedTag,
            tagCounts = tagCounts,
            onSelectTag = { viewModel.selectTag(it) }
        )

        }
        if (showSortSheet) {
            LibrarySortSheet(
                selected = currentSort,
                ascending = currentSortAscending,
                progressFilter = progressFilter,
                onSelectSort = { viewModel.setSort(it) },
                onToggleDirection = { viewModel.toggleSortDirection() },
                onSelectProgress = { viewModel.setProgressFilter(it) },
                onDismiss = { showSortSheet = false }
            )
        }

        if (books.isEmpty()) {
            when {
                !isLibraryLoaded -> LibraryLoadingState()
                // Each filtered-empty state clears its own filter: once the last book is
                // gone the filter bar hides, so without an action here the reader is left
                // staring at a reason with no way out of it.
                query.isNotBlank() -> LibraryEmptyState(
                    title = "No books match",
                    body = "Try a different search term",
                    actionLabel = "Clear search",
                    onAction = {
                        viewModel.setQuery("")
                        searchActive = false
                    }
                )
                selectedCollection != null -> LibraryEmptyState(
                    title = "Collection is empty",
                    body = "Books added to this collection will appear here",
                    actionLabel = "Show all books",
                    onAction = { viewModel.selectCollection(null) }
                )
                readingStatusFilter != null -> LibraryEmptyState(
                    title = "No books with this status",
                    body = "Status filter: ${readingStatusFilter!!.displayName}",
                    actionLabel = "Clear filter",
                    onAction = { viewModel.setReadingStatusFilter(null) }
                )
                progressFilter != ProgressFilter.ALL -> LibraryEmptyState(
                    title = "No books in this range",
                    body = "Progress filter: ${progressFilter.displayName}",
                    actionLabel = "Clear filter",
                    onAction = { viewModel.setProgressFilter(ProgressFilter.ALL) }
                )
                selectedTag != null -> LibraryEmptyState(
                    title = "No books with this tag",
                    body = "Tag filter: $selectedTag",
                    actionLabel = "Clear filter",
                    onAction = { viewModel.selectTag(null) }
                )
                else -> LibraryEmptyState(
                    title = "Your library awaits",
                    body = "Import an EPUB to start reading. Folder scanning lives in the menu.",
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
                        text = sectionTitle,
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
                        text = sectionTitle,
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

    // Import FAB — always in reach, never blocks content. Hidden on an empty library:
    // the empty state already offers the same action, and two add controls compete.
    if (!libraryIsEmpty) {
        FloatingActionButton(
            onClick = onImportEpub,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Padding.screen)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Import EPUB")
        }
    }
    }

    // Collection management sheet
    if (showCollectionSheet) {
        CollectionManagementSheet(
            collections = collections,
            collectionCounts = collectionCounts,
            onDismiss = { showCollectionSheet = false },
            onCreate = { showCreateDialog = true },
            onRename = { collection ->
                renameCollectionName = collection.name
                renameCollectionTarget = collection
            },
            onDelete = { collection -> viewModel.deleteCollection(collection.id) }
        )
    }

    renameCollectionTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameCollectionTarget = null },
            title = { Text("Rename collection") },
            text = {
                OutlinedTextField(
                    value = renameCollectionName,
                    onValueChange = { renameCollectionName = it },
                    placeholder = { Text("Collection name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameCollectionName.isNotBlank()) {
                            viewModel.renameCollection(target.id, renameCollectionName.trim())
                            renameCollectionTarget = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameCollectionTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Backup & restore sheet
    if (showBackupSheet && backupManager != null) {
        BackupRestoreSheet(
            backupManager = backupManager,
            onDismiss = { showBackupSheet = false }
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
            onUpdateMetadata = { title, authors, series, seriesIndex, publisher, description ->
                viewModel.updateBookMetadata(
                    stableId = book.stableId,
                    title = title,
                    authors = authors,
                    series = series,
                    seriesIndex = seriesIndex,
                    publisher = publisher,
                    description = description
                )
            },
            onExportNotes = if (notesExporter != null) {
                { exportBookTarget = currentBook }
            } else null,
            onGetReadingStats = { viewModel.getReadingAggregateForBook(book.stableId) }
        )
    }

    if (exportBookData != null && notesExporter != null) {
        NotesExportSheet(
            notesExporter = notesExporter,
            singleBookData = exportBookData,
            onDismiss = {
                exportBookTarget = null
                exportBookData = null
            }
        )
    }

    if (exportBatchTarget != null && notesExporter != null) {
        NotesExportSheet(
            notesExporter = notesExporter,
            batchBooksData = exportBatchTarget,
            onDismiss = {
                exportBatchTarget = null
            }
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
    collectionCounts: Map<Long, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onRename: (Collection) -> Unit,
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
                        val count = collectionCounts[collection.id] ?: 0
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    collection.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    if (count == 1) "1 book" else "$count books",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onRename(collection) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Rename ${collection.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySortSheet(
    selected: LibrarySort,
    ascending: Boolean,
    progressFilter: ProgressFilter,
    onSelectSort: (LibrarySort) -> Unit,
    onToggleDirection: () -> Unit,
    onSelectProgress: (ProgressFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Text("Sort and filter", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.sm))
            // Only the options scroll. Done stays pinned below them, the same shape the
            // export sheet uses, so the action is reachable without scrolling past 12 rows.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {

            Text(
                "Sort by",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.selectableGroup()) {
                LibrarySort.entries.forEach { option ->
                    SheetOptionRow(
                        label = option.label,
                        selected = option == selected,
                        onClick = { onSelectSort(option) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onToggleDirection)
                    .padding(vertical = Spacing.xs)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Order", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (ascending) "Ascending" else "Descending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (ascending) {
                        Icons.Default.ArrowUpward
                    } else {
                        Icons.Default.ArrowDownward
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

            Text(
                "Progress filter",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.selectableGroup()) {
                ProgressFilter.entries.forEach { filter ->
                    SheetOptionRow(
                        label = filter.displayName,
                        selected = filter == progressFilter,
                        onClick = { onSelectProgress(filter) }
                    )
                }
            }

            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(top = Spacing.sm)
            ) {
                Text("Done")
            }
        }
    }
}

/** One single-choice row in the sort and filter sheet. */
@Composable
private fun SheetOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = Spacing.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
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
                    // Two lines, like the title: at a large system font scale a single
                    // line clipped real names to initials plus an ellipsis.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            book.formattedSeries()?.let { seriesText ->
                Text(
                    text = seriesText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
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
    onUpdateMetadata: ((title: String, authors: List<String>, series: String?, seriesIndex: Float?, publisher: String?, description: String?) -> Unit)? = null,
    onExportNotes: (() -> Unit)? = null,
    onGetReadingStats: suspend () -> ReadingAggregate = { ReadingAggregate.EMPTY }
) {
    var optimisticStatus by remember(book.readingStatus) { mutableStateOf(book.readingStatus) }
    var optimisticRating by remember(book.rating) { mutableStateOf(book.rating) }
    var optimisticTags by remember(book.tags) { mutableStateOf(book.tags) }
    var optimisticCollectionIds by remember(bookCollectionIds) { mutableStateOf(bookCollectionIds) }
    var optimisticTitle by remember(book.title) { mutableStateOf(book.title) }
    var optimisticAuthors by remember(book.authors) { mutableStateOf(book.authors) }
    var optimisticSeries by remember(book.series) { mutableStateOf(book.series) }
    var optimisticSeriesIndex by remember(book.seriesIndex) { mutableStateOf(book.seriesIndex) }
    var optimisticPublisher by remember(book.publisher) { mutableStateOf(book.publisher) }
    var optimisticDescription by remember(book.description) { mutableStateOf(book.description) }
    var showEditMetadataDialog by remember { mutableStateOf(false) }

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
                    Text(optimisticTitle, style = MaterialTheme.typography.titleLarge)
                    if (optimisticAuthors.isNotEmpty()) {
                        Text(
                            optimisticAuthors.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    formatSeriesText(optimisticSeries, optimisticSeriesIndex)?.let { seriesText ->
                        Text(
                            text = seriesText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
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

            if (!optimisticDescription.isNullOrBlank()) {
                var descriptionExpanded by remember { mutableStateOf(false) }
                Spacer(Modifier.height(Spacing.sm))
                HorizontalDivider()
                Spacer(Modifier.height(Padding.compact))
                Text("Description", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = optimisticDescription!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (optimisticDescription!!.length > 180) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Metadata", style = MaterialTheme.typography.titleSmall)
                if (onUpdateMetadata != null) {
                    TextButton(onClick = { showEditMetadataDialog = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp) // touch-target-ok: button decorative icon
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            MetadataRow("Status", optimisticStatus.displayName)
            optimisticRating?.let { MetadataRow("Rating", "$it / 5 stars") }
            if (optimisticTags.isNotEmpty()) {
                MetadataRow("Tags", optimisticTags.joinToString(", ") { "#$it" })
            }
            optimisticPublisher?.let { MetadataRow("Publisher", it) }
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

            if (onExportNotes != null) {
                Spacer(Modifier.height(Spacing.sm))
                OutlinedButton(
                    onClick = onExportNotes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp) // touch-target-ok: button decorative icon
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Export notes & highlights")
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

    if (showEditMetadataDialog && onUpdateMetadata != null) {
        EditBookMetadataDialog(
            initialTitle = optimisticTitle,
            initialAuthors = optimisticAuthors,
            initialSeries = optimisticSeries,
            initialSeriesIndex = optimisticSeriesIndex,
            initialPublisher = optimisticPublisher,
            initialDescription = optimisticDescription,
            initialReadingStatus = optimisticStatus,
            onDismiss = { showEditMetadataDialog = false },
            onSave = { newTitle, newAuthors, newSeries, newSeriesIndex, newPublisher, newDescription, newReadingStatus ->
                optimisticTitle = newTitle
                optimisticAuthors = newAuthors
                optimisticSeries = newSeries
                optimisticSeriesIndex = newSeriesIndex
                optimisticPublisher = newPublisher
                optimisticDescription = newDescription
                if (newReadingStatus != optimisticStatus) {
                    optimisticStatus = newReadingStatus
                    onSetReadingStatus(newReadingStatus)
                }
                onUpdateMetadata(newTitle, newAuthors, newSeries, newSeriesIndex, newPublisher, newDescription)
            }
        )
    }
}

@Composable
private fun EditBookMetadataDialog(
    initialTitle: String,
    initialAuthors: List<String>,
    initialSeries: String?,
    initialSeriesIndex: Float?,
    initialPublisher: String?,
    initialDescription: String?,
    initialReadingStatus: ReadingStatus,
    onDismiss: () -> Unit,
    onSave: (title: String, authors: List<String>, series: String?, seriesIndex: Float?, publisher: String?, description: String?, readingStatus: ReadingStatus) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var authors by remember { mutableStateOf(initialAuthors.joinToString(", ")) }
    var series by remember { mutableStateOf(initialSeries.orEmpty()) }
    var seriesIndex by remember {
        mutableStateOf(
            initialSeriesIndex?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }.orEmpty()
        )
    }
    var publisher by remember { mutableStateOf(initialPublisher.orEmpty()) }
    var description by remember { mutableStateOf(initialDescription.orEmpty()) }
    var readingStatus by remember { mutableStateOf(initialReadingStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit metadata", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Status belongs here: it was only settable in bulk, so a book
                // marked Finished while a few percent in could not be corrected
                // from the one screen that shows its details. The chips scroll
                // because three of them plus the label overflow a narrow dialog
                // and the last status would otherwise be unreachable.
                Text(
                    "Status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReadingStatus.entries.forEach { status ->
                        FilterChip(
                            selected = readingStatus == status,
                            onClick = { readingStatus = status },
                            label = { Text(status.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    isError = title.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = authors,
                    onValueChange = { authors = it },
                    label = { Text("Authors (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    OutlinedTextField(
                        value = series,
                        onValueChange = { series = it },
                        label = { Text("Series (optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seriesIndex,
                        onValueChange = { seriesIndex = it },
                        label = { Text("Volume #") },
                        singleLine = true,
                        modifier = Modifier.width(100.dp)
                    )
                }
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = { Text("Publisher (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedAuthors = authors.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val parsedSeriesIndex = seriesIndex.trim().toFloatOrNull()
                        onSave(
                            title.trim(),
                            parsedAuthors,
                            series.trim().takeIf { it.isNotEmpty() },
                            parsedSeriesIndex,
                            publisher.trim().takeIf { it.isNotEmpty() },
                            description.trim().takeIf { it.isNotEmpty() },
                            readingStatus
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatSeriesText(series: String?, seriesIndex: Float?): String? {
    val s = series?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val idx = seriesIndex ?: return s
    val formattedIndex = if (idx % 1f == 0f) idx.toInt().toString() else idx.toString()
    return "$s #$formattedIndex"
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

@Composable
private fun InsightMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = true
) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        durationMillis <= 0L -> "0 min"
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
    val readingGoals by viewModel.readingGoals.collectAsState()
    val allBooks by viewModel.books.collectAsState()
    val finishedBooksCount = remember(allBooks) { allBooks.count { it.readingStatus == ReadingStatus.FINISHED } }
    var showEditGoalsDialog by remember { mutableStateOf(false) }

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
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            InsightMetric(
                                value = formatReadingDuration(todayStats.activeDurationMillis),
                                label = "Time read",
                                modifier = Modifier.weight(1f)
                            )
                            InsightMetric(
                                value = "${todayStats.sessionCount}",
                                label = "Sessions",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            InsightMetric(
                                value = "${todayStats.booksOpened}",
                                label = "Books opened",
                                modifier = Modifier.weight(1f)
                            )
                            InsightMetric(
                                value = if (currentStreak > 0) "$currentStreak d" else "0 d",
                                label = "Streak",
                                emphasised = currentStreak > 0,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            val todayMinutes = todayStats.activeMinutes
            val dailyTarget = readingGoals.dailyMinutes
            val dailyProgress = if (dailyTarget > 0) (todayMinutes.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f) else 0f
            val dailyGoalReached = todayMinutes >= dailyTarget

            val booksTarget = readingGoals.booksTarget
            val booksProgress = if (booksTarget > 0) (finishedBooksCount.toFloat() / booksTarget.toFloat()).coerceIn(0f, 1f) else 0f
            val booksGoalReached = finishedBooksCount >= booksTarget

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reading Goals",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = { showEditGoalsDialog = true },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Text("Edit")
                        }
                    }

                    Spacer(Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily reading target",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$todayMinutes / $dailyTarget min",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (dailyGoalReached) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            ) {
                                Text(
                                    text = "Goal met! 🎉",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "${(dailyProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    LinearProgressIndicator(
                        progress = { dailyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(Modifier.height(Spacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Books finished target",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$finishedBooksCount / $booksTarget books",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (booksGoalReached) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            ) {
                                Text(
                                    text = "Goal met! 🏆",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "${(booksProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    LinearProgressIndicator(
                        progress = { booksProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
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

        if (showEditGoalsDialog) {
            EditReadingGoalsDialog(
                initialDailyMinutes = readingGoals.dailyMinutes,
                initialBooksTarget = readingGoals.booksTarget,
                onDismiss = { showEditGoalsDialog = false },
                onSave = { dailyMins, booksTarget ->
                    viewModel.updateReadingGoals(dailyMins, booksTarget)
                }
            )
        }
    }
}

@Composable
private fun EditReadingGoalsDialog(
    initialDailyMinutes: Int,
    initialBooksTarget: Int,
    onDismiss: () -> Unit,
    onSave: (dailyMinutes: Int, booksTarget: Int) -> Unit
) {
    var dailyMinutesText by remember { mutableStateOf(initialDailyMinutes.toString()) }
    var booksTargetText by remember { mutableStateOf(initialBooksTarget.toString()) }

    val parsedDailyMinutes = dailyMinutesText.trim().toIntOrNull()
    val parsedBooksTarget = booksTargetText.trim().toIntOrNull()
    val isValid = parsedDailyMinutes != null && parsedDailyMinutes > 0 &&
            parsedBooksTarget != null && parsedBooksTarget > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Reading Goals",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Set your personal reading targets. All goals and stats remain entirely private on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = dailyMinutesText,
                    onValueChange = { dailyMinutesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Daily target (minutes)") },
                    singleLine = true,
                    isError = parsedDailyMinutes == null || parsedDailyMinutes <= 0,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    listOf(15, 20, 30, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = parsedDailyMinutes == mins,
                            onClick = { dailyMinutesText = mins.toString() },
                            label = { Text("${mins}m") },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                OutlinedTextField(
                    value = booksTargetText,
                    onValueChange = { booksTargetText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Books target") },
                    singleLine = true,
                    isError = parsedBooksTarget == null || parsedBooksTarget <= 0,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    listOf(5, 12, 24, 52).forEach { count ->
                        FilterChip(
                            selected = parsedBooksTarget == count,
                            onClick = { booksTargetText = count.toString() },
                            label = { Text("$count") },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onSave(parsedDailyMinutes!!, parsedBooksTarget!!)
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
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
                                shape = MaterialTheme.shapes.small,
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
