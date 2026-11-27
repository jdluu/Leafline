package com.jdluu.leafline.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.library.rescan.FolderRescanResult
import com.jdluu.leafline.library.rescan.FolderRescanUiState
import com.jdluu.leafline.library.rescan.RescanSummary
import com.jdluu.leafline.theme.IconSize
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing

/**
 * Loading surface shown until the library store has produced its first book
 * list. Kept separate from the empty state so a real empty library is never
 * confused with one that is still being read.
 */
@Composable
internal fun LibraryLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading library" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionManagementSheet(
    collections: List<Collection>,
    collectionCounts: Map<Long, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onRename: (Collection) -> Unit,
    onDelete: (Collection) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
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
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon in button
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryFoldersSheet(
    folders: List<LibraryFolder>,
    onDismiss: () -> Unit,
    onAddFolder: () -> Unit,
    onRemove: (String) -> Unit,
    onReselect: (String) -> Unit,
    rescanState: FolderRescanUiState = FolderRescanUiState.Idle,
    onRescanAll: () -> Unit = {},
    onRescanOne: (String) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
    @Suppress("UNUSED_PARAMETER") bookStableId: String,
    collections: List<Collection>,
    bookCollectionIds: Set<Long>,
    onDismiss: () -> Unit,
    onAddToCollection: (Long) -> Unit,
    onRemoveFromCollection: (Long) -> Unit
) {
    var optimisticIds by remember(bookCollectionIds) { mutableStateOf(bookCollectionIds) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
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
internal fun LibrarySortSheet(
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

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.xs),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

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
