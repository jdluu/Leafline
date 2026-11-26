package com.jdluu.leafline.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibrarySelectionTopBar(
    selectedCount: Int,
    hasCollections: Boolean,
    hasExporter: Boolean,
    onExitSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onSetReadingStatus: (ReadingStatus) -> Unit,
    onOpenBatchCollection: () -> Unit,
    onOpenBatchTags: () -> Unit,
    onExportNotes: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    var showBatchStatusMenu by remember { mutableStateOf(false) }
    var showSelectionOverflow by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                "$selectedCount selected",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onExitSelection) {
                Icon(Icons.Default.Close, contentDescription = "Exit selection")
            }
        },
        actions = {
            IconButton(onClick = onToggleSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select or deselect all")
            }
            if (selectedCount > 0) {
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
                                    showBatchStatusMenu = false
                                    onSetReadingStatus(status)
                                }
                            )
                        }
                    }
                }
                if (hasCollections) {
                    IconButton(onClick = onOpenBatchCollection) {
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
                                onOpenBatchTags()
                            }
                        )
                        if (hasExporter) {
                            DropdownMenuItem(
                                text = { Text("Export notes") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showSelectionOverflow = false
                                    onExportNotes()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete books") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showSelectionOverflow = false
                                onDeleteSelected()
                            }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryMainTopBar(
    layout: LibraryLayout,
    progressFilter: ProgressFilter,
    hasBackupManager: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSortSheet: () -> Unit,
    onToggleLayout: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenFolders: () -> Unit
) {
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
            IconButton(onClick = onToggleSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search library")
            }
            // A progress filter hides books, and the sheet is the only place it is
            // shown, so the button itself has to say when one is on: the accent tint
            // plus the spoken description, never the tint alone.
            val progressFilterActive = progressFilter != ProgressFilter.ALL
            IconButton(onClick = onOpenSortSheet) {
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
            IconButton(onClick = onToggleLayout) {
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
                            onEnterSelectionMode()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reading insights") },
                        leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        onClick = {
                            overflowMenuExpanded = false
                            onOpenInsights()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Collections") },
                        leadingIcon = { Icon(Icons.Default.CollectionsBookmark, contentDescription = null) },
                        onClick = {
                            overflowMenuExpanded = false
                            onOpenCollections()
                        }
                    )
                    if (hasBackupManager) {
                        DropdownMenuItem(
                            text = { Text("Backup & restore") },
                            leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null) },
                            onClick = {
                                overflowMenuExpanded = false
                                onOpenBackup()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Manage folders") },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        onClick = {
                            overflowMenuExpanded = false
                            onOpenFolders()
                        }
                    )
                }
            }
        }
    )
}
