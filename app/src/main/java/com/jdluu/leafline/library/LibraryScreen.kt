package com.jdluu.leafline.library

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.leafline.LibraryViewModel
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Collection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onImportEpub: () -> Unit,
    onOpenBook: (LibraryBook) -> Unit
) {
    val books by viewModel.sortedBooks.collectAsStateWithLifecycle()
    val currentSort by viewModel.sort.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedCollection.collectAsStateWithLifecycle()
    val readingStatusFilter by viewModel.readingStatusFilter.collectAsStateWithLifecycle()
    var searchActive by remember { mutableStateOf(false) }
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var collectionMenuExpanded by remember { mutableStateOf(false) }
    var detailSheetBook by remember { mutableStateOf<LibraryBook?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<LibraryBook?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Library") },
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
                        Icon(Icons.Default.Sort, contentDescription = "Sort library")
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
                IconButton(onClick = { showCollectionSheet = true }) {
                    Icon(
                        Icons.Default.CollectionsBookmark,
                        contentDescription = "Manage collections"
                    )
                }
            }
        )
        if (searchActive) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search title or author") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            viewModel.setQuery("")
                            searchActive = false
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            )
        }
        Button(
            onClick = onImportEpub,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Import EPUB")
        }

        // Collection filter chips
        if (collections.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCollection == null,
                        onClick = { viewModel.selectCollection(null) },
                        label = { Text("All") }
                    )
                }
                items(collections) { collection ->
                    FilterChip(
                        selected = selectedCollection == collection.id,
                        onClick = { viewModel.selectCollection(collection.id) },
                        label = { Text(collection.name) },
                        trailingIcon = if (selectedCollection == collection.id) {
                            {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        // Reading status filter chips
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = readingStatusFilter == null,
                    onClick = { viewModel.setReadingStatusFilter(null) },
                    label = { Text("All") }
                )
            }
            ReadingStatus.entries.forEach { status ->
                item {
                    FilterChip(
                        selected = readingStatusFilter == status,
                        onClick = { viewModel.setReadingStatusFilter(status) },
                        label = {
                            Text(
                                when (status) {
                                    ReadingStatus.UNREAD -> "Unread"
                                    ReadingStatus.READING -> "Reading"
                                    ReadingStatus.FINISHED -> "Finished"
                                }
                            )
                        }
                    )
                }
            }
        }

        // Continue reading shelf
        val continueReadingBooks by viewModel.continueReadingBooks.collectAsStateWithLifecycle()
        if (continueReadingBooks.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "Continue reading",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(continueReadingBooks, key = { it.stableId }) { book ->
                        ContinueReadingTile(book = book, onClick = { onOpenBook(book) })
                    }
                }
            }
        }

        if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (query.isNotBlank()) {
                    Text("No books match")
                } else if (selectedCollection != null) {
                    Text("No books in this collection")
                } else if (readingStatusFilter != null) {
                    Text("No books with this status")
                } else {
                    Text("No books imported yet")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(rememberLibraryGridColumns()),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = books, key = { it.stableId }) { book ->
                    BookGridTile(
                        book = book,
                        onClick = { onOpenBook(book) },
                        onLongClick = { detailSheetBook = book },
                        collections = collections,
                        bookCollections = emptyList(), // simplified: collection state per book
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

    // Collection management sheet
    if (showCollectionSheet) {
        CollectionManagementSheet(
            collections = collections,
            onDismiss = { showCollectionSheet = false },
            onCreate = { showCreateDialog = true },
            onDelete = { collection -> viewModel.deleteCollection(collection.id) }
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
        BookDetailSheet(
            book = book,
            collections = collections,
            onDismiss = { detailSheetBook = null },
            onDelete = {
                detailSheetBook = null
                showDeleteConfirm = book
            },
            onSetReadingStatus = { status -> viewModel.setBookReadingStatus(book.stableId, status) },
            onAddToCollection = { id -> viewModel.addBookToCollection(book.stableId, id) },
            onRemoveFromCollection = { id -> viewModel.removeBookFromCollection(book.stableId, id) },
            onOpen = {
                detailSheetBook = null
                onOpenBook(book)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionManagementSheet(
    collections: List<Collection>,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onDelete: (Collection) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (collections.isEmpty()) {
                Text(
                    "No collections yet. Create one to start organizing your books.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn {
                    items(collections) { collection ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CollectionsBookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
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
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookCollectionPickerSheet(
    bookStableId: String,
    collections: List<Collection>,
    bookCollectionIds: Set<Long>,
    onDismiss: () -> Unit,
    onAddToCollection: (Long) -> Unit,
    onRemoveFromCollection: (Long) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Collections",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (collections.isEmpty()) {
                Text(
                    "No collections yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(collections) { collection ->
                        val isMember = collection.id in bookCollectionIds
                        FilterChip(
                            selected = isMember,
                            onClick = {
                                if (isMember) onRemoveFromCollection(collection.id)
                                else onAddToCollection(collection.id)
                            },
                            label = { Text(collection.name) },
                            leadingIcon = {
                                Icon(
                                    if (isMember) Icons.Default.Bookmark
                                    else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
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
                        Icon(Icons.Default.Sort, contentDescription = null)
                    }
                }
            )
            if (index != LibrarySort.entries.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun rememberLibraryGridColumns(): Int {
    val configuration = LocalConfiguration.current
    return if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookGridTile(
    book: LibraryBook,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    collections: List<Collection> = emptyList(),
    bookCollections: List<Collection> = emptyList(),
    onAddToCollection: (Long) -> Unit = {},
    onRemoveFromCollection: (Long) -> Unit = {}
) {
    var showCollectionPicker by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = "Open book",
                onClick = onClick,
                onLongClickLabel = "Book details",
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val bitmap = rememberCoverBitmap(book.coverPath)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CoverPlaceholder(title = book.title)
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
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (book.readingStatus) {
                            ReadingStatus.READING -> "Reading"
                            ReadingStatus.FINISHED -> "Finished"
                            ReadingStatus.UNREAD -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
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
                        onClick = { showCollectionPicker = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.CollectionsBookmark,
                            contentDescription = "Collections",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
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
        }
    }

    if (showCollectionPicker) {
        BookCollectionPickerSheet(
            bookStableId = book.stableId,
            collections = collections,
            bookCollectionIds = emptySet(), // simplified per-book collection state
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
private fun CoverPlaceholder(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
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
    return produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                path?.let { BitmapFactory.decodeFile(it) }
            } catch (e: Exception) {
                null
            }
        }
    }.value
}

@Composable
private fun ContinueReadingTile(book: LibraryBook, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClickLabel = "Continue reading", onClick = onClick)
    ) {
        Column {
            val bitmap = rememberCoverBitmap(book.coverPath)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CoverPlaceholder(title = book.title)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookDetailSheet(
    book: LibraryBook,
    collections: List<Collection>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSetReadingStatus: (ReadingStatus) -> Unit,
    onAddToCollection: (Long) -> Unit,
    onRemoveFromCollection: (Long) -> Unit,
    onOpen: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
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
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleLarge)
                    if (book.authors.isNotEmpty()) {
                        Text(
                            book.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpen) { Text("Open book") }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Metadata section
            Text("Metadata", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            MetadataRow("Status", when (book.readingStatus) {
                ReadingStatus.UNREAD -> "Unread"
                ReadingStatus.READING -> "Reading"
                ReadingStatus.FINISHED -> "Finished"
            })
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

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // File info section
            Text("File info", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            MetadataRow("Path", book.filePath)
            MetadataRow("Hash", book.fileHash.take(12) + "…")
            book.koreaderHash?.let { MetadataRow("KOReader hash", it.take(12) + "…") }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Actions section
            Text("Actions", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            // Reading status buttons
            Text("Mark as:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingStatus.entries.forEach { status ->
                    FilterChip(
                        selected = book.readingStatus == status,
                        onClick = { onSetReadingStatus(status) },
                        label = {
                            Text(when (status) {
                                ReadingStatus.UNREAD -> "Unread"
                                ReadingStatus.READING -> "Reading"
                                ReadingStatus.FINISHED -> "Finished"
                            })
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Collection management
            if (collections.isNotEmpty()) {
                Text("Collections:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                // Simple toggle row for each collection
                collections.forEach { collection ->
                    FilterChip(
                        selected = false, // simplified: no per-book collection tracking here
                        onClick = { onAddToCollection(collection.id) },
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

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
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