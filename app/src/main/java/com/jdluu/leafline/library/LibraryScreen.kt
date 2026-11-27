package com.jdluu.leafline.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.leafline.LibraryViewModel
import com.jdluu.leafline.export.BookExportData
import com.jdluu.leafline.export.NotesExporter
import com.jdluu.leafline.export.ui.NotesExportSheet
import com.jdluu.leafline.library.backup.BackupRestoreSheet
import com.jdluu.leafline.library.backup.LibraryBackupManager
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.library.rescan.FolderRescanUiState
import com.jdluu.leafline.library.rescan.FolderRescanViewModel
import com.jdluu.leafline.theme.LeaflineEmptyState
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    foldersViewModel: LibraryFolderViewModel? = null,
    folderRescanViewModel: FolderRescanViewModel? = null,
    backupManager: LibraryBackupManager? = null,
    notesExporter: NotesExporter? = null,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    searchActive: Boolean? = null,
    onSearchActiveChange: ((Boolean) -> Unit)? = null,
    onImportEpub: () -> Unit,
    onAddLibraryFolder: () -> Unit = {},
    onReselectFolder: (String) -> Unit = {},
    onBackupRestored: () -> Unit = {},
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
    var localSearchActive by rememberSaveable { mutableStateOf(false) }
    val isSearchActive = searchActive ?: localSearchActive
    val setSearchActive: (Boolean) -> Unit = onSearchActiveChange ?: { localSearchActive = it }
    var showSortSheet by remember { mutableStateOf(false) }
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var detailSheetBook by remember { mutableStateOf<LibraryBook?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<LibraryBook?>(null) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedBookIds by remember { mutableStateOf(emptySet<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchCollectionDialog by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    val tagCounts by viewModel.tagCounts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var exportBookTarget by remember { mutableStateOf<LibraryBook?>(null) }
    var exportBookData by remember { mutableStateOf<BookExportData?>(null) }
    var exportBatchTarget by remember { mutableStateOf<List<BookExportData>?>(null) }

    LaunchedEffect(exportBookTarget) {
        val target = exportBookTarget
        exportBookData = if (target != null && notesExporter != null) {
            notesExporter.getExportDataForBook(target)
        } else {
            null
        }
    }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedBookIds = emptySet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectionMode) {
                LibrarySelectionTopBar(
                    selectedCount = selectedBookIds.size,
                    hasCollections = collections.isNotEmpty(),
                    hasExporter = notesExporter != null,
                    onExitSelection = {
                        selectionMode = false
                        selectedBookIds = emptySet()
                    },
                    onToggleSelectAll = {
                        selectedBookIds = if (selectedBookIds.size == books.size) {
                            emptySet()
                        } else {
                            books.map { it.stableId }.toSet()
                        }
                    },
                    onSetReadingStatus = { status ->
                        viewModel.setBooksReadingStatus(selectedBookIds, status)
                        selectionMode = false
                        selectedBookIds = emptySet()
                    },
                    onOpenBatchCollection = { showBatchCollectionDialog = true },
                    onOpenBatchTags = { showBatchTagDialog = true },
                    onExportNotes = {
                        notesExporter?.let { exporter ->
                            scope.launch {
                                val selectedBooks = books.filter { it.stableId in selectedBookIds }
                                exportBatchTarget = selectedBooks.map { book ->
                                    exporter.getExportDataForBook(book)
                                }
                            }
                        }
                    },
                    onDeleteSelected = { showBatchDeleteConfirm = true }
                )
            } else {
                LibraryMainTopBar(
                    layout = layout,
                    progressFilter = progressFilter,
                    hasBackupManager = backupManager != null,
                    onToggleSearch = {
                        if (isSearchActive) {
                            viewModel.setQuery("")
                        }
                        setSearchActive(!isSearchActive)
                    },
                    onOpenSortSheet = { showSortSheet = true },
                    onToggleLayout = { viewModel.toggleLayout() },
                    onEnterSelectionMode = { selectionMode = true },
                    onOpenInsights = { showInsightsSheet = true },
                    onOpenCollections = { showCollectionSheet = true },
                    onOpenBackup = { showBackupSheet = true },
                    onOpenFolders = {
                        foldersViewModel?.refresh()
                        showFolderSheet = true
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
                    searchActive = isSearchActive,
                    query = query,
                    onQueryChange = viewModel::setQuery,
                    onCloseSearch = {
                        viewModel.setQuery("")
                        setSearchActive(false)
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

            val activeFilterCount = listOf(
                query.isNotBlank(),
                selectedCollection != null,
                readingStatusFilter != null,
                progressFilter != ProgressFilter.ALL,
                selectedTag != null
            ).count { it }

            if (books.isEmpty()) {
                when {
                    !isLibraryLoaded -> LibraryLoadingState()
                    // Each filtered-empty state clears its own filter: once the last book is
                    // gone the filter bar hides, so without an action here the reader is left
                    // staring at a reason with no way out of it.
                    activeFilterCount > 1 -> LeaflineEmptyState(
                        title = "No books match these filters",
                        body = "Try clearing filters to see your library",
                        actionLabel = "Clear all filters",
                        onAction = {
                            viewModel.clearAllFilters()
                            setSearchActive(false)
                        }
                    )
                    query.isNotBlank() -> LeaflineEmptyState(
                        title = "No books match",
                        body = "Try a different search term",
                        actionLabel = "Clear search",
                        onAction = {
                            viewModel.setQuery("")
                            setSearchActive(false)
                        }
                    )
                    selectedCollection != null -> LeaflineEmptyState(
                        title = "Collection is empty",
                        body = "Books added to this collection will appear here",
                        actionLabel = "Show all books",
                        onAction = { viewModel.selectCollection(null) }
                    )
                    readingStatusFilter != null -> LeaflineEmptyState(
                        title = "No books with this status",
                        body = "Status filter: ${readingStatusFilter!!.displayName}",
                        actionLabel = "Clear filter",
                        onAction = { viewModel.setReadingStatusFilter(null) }
                    )
                    progressFilter != ProgressFilter.ALL -> LeaflineEmptyState(
                        title = "No books in this range",
                        body = "Progress filter: ${progressFilter.displayName}",
                        actionLabel = "Clear filter",
                        onAction = { viewModel.setProgressFilter(ProgressFilter.ALL) }
                    )
                    selectedTag != null -> LeaflineEmptyState(
                        title = "No books with this tag",
                        body = "Tag filter: $selectedTag",
                        actionLabel = "Clear filter",
                        onAction = { viewModel.selectTag(null) }
                    )
                    else -> LeaflineEmptyState(
                        title = "Your library awaits",
                        body = "Import an EPUB to start reading. Folder scanning lives in the menu.",
                        actionLabel = "Import EPUB",
                        onAction = onImportEpub
                    )
                }
            } else if (layout == LibraryLayout.LIST) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = Padding.screen, end = Padding.screen, top = Spacing.xs, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(Padding.compact)
                ) {
                    if (continueReadingBooks.isNotEmpty()) {
                        item {
                            ContinueReadingSection(
                                books = continueReadingBooks,
                                onOpenBook = onOpenBook
                            )
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
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = Padding.screen, end = Padding.screen, top = Spacing.xs, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(Padding.compact),
                    horizontalArrangement = Arrangement.spacedBy(Padding.compact)
                ) {
                    if (continueReadingBooks.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ContinueReadingSection(
                                books = continueReadingBooks,
                                onOpenBook = onOpenBook
                            )
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
        RenameCollectionDialog(
            name = renameCollectionName,
            onNameChange = { renameCollectionName = it },
            onSave = {
                if (renameCollectionName.isNotBlank()) {
                    viewModel.renameCollection(target.id, renameCollectionName.trim())
                    renameCollectionTarget = null
                }
            },
            onDismiss = { renameCollectionTarget = null }
        )
    }

    if (showBackupSheet && backupManager != null) {
        BackupRestoreSheet(
            backupManager = backupManager,
            onDismiss = { showBackupSheet = false },
            onBackupRestored = onBackupRestored
        )
    }

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

    if (showInsightsSheet) {
        ReadingInsightsSheet(
            viewModel = viewModel,
            onDismiss = { showInsightsSheet = false }
        )
    }

    if (showCreateDialog) {
        CreateCollectionDialog(
            name = newCollectionName,
            onNameChange = { newCollectionName = it },
            onCreate = {
                if (newCollectionName.isNotBlank()) {
                    viewModel.createCollection(newCollectionName.trim())
                    newCollectionName = ""
                    showCreateDialog = false
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    detailSheetBook?.let { book ->
        val currentBook = books.find { it.stableId == book.stableId } ?: book
        val nextBook = remember(currentBook, allBooks) { currentBook.nextInSeries(allBooks) }
        BookDetailSheet(
            book = currentBook,
            collections = collections,
            availableTags = allTags,
            bookCollectionIds = viewModel
                .collectionIdsForBook(book.stableId)
                .collectAsStateWithLifecycle(emptySet())
                .value,
            nextInSeries = nextBook,
            onSelectBook = { next -> detailSheetBook = next },
            onOpenNextInSeries = { next ->
                detailSheetBook = null
                onOpenBook(next)
            },
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

    if (showBatchDeleteConfirm) {
        BatchDeleteBooksDialog(
            selectedCount = selectedBookIds.size,
            onConfirmDelete = {
                viewModel.deleteBooks(selectedBookIds)
                showBatchDeleteConfirm = false
                selectionMode = false
                selectedBookIds = emptySet()
            },
            onDismiss = { showBatchDeleteConfirm = false }
        )
    }

    if (showBatchTagDialog && selectedBookIds.isNotEmpty()) {
        BatchTagDialog(
            selectedBookCount = selectedBookIds.size,
            allTags = allTags,
            onAddTag = { tag -> viewModel.addTagToBooks(selectedBookIds, tag) },
            onRemoveTag = { tag -> viewModel.removeTagFromBooks(selectedBookIds, tag) },
            onDismiss = { showBatchTagDialog = false }
        )
    }

    if (showBatchCollectionDialog) {
        BatchCollectionDialog(
            collections = collections,
            onSelectCollection = { collection ->
                viewModel.addBooksToCollection(selectedBookIds, collection.id)
                showBatchCollectionDialog = false
                selectionMode = false
                selectedBookIds = emptySet()
            },
            onDismiss = { showBatchCollectionDialog = false }
        )
    }

    showDeleteConfirm?.let { book ->
        DeleteBookDialog(
            bookTitle = book.title,
            onConfirmDelete = {
                viewModel.deleteBook(book.stableId)
                showDeleteConfirm = null
            },
            onDismiss = { showDeleteConfirm = null }
        )
    }
}

@Composable
private fun ContinueReadingSection(
    books: List<LibraryBook>,
    onOpenBook: (LibraryBook) -> Unit
) {
    Column(modifier = Modifier.padding(top = Spacing.xs)) {
        Text(
            "Continue reading",
            style = MaterialTheme.typography.titleMedium
        )
        LazyRow(
            modifier = Modifier.padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Padding.compact)
        ) {
            items(books, key = { it.stableId }) { book ->
                ContinueReadingTile(book = book, onClick = { onOpenBook(book) })
            }
        }
    }
}
