package com.jdluu.leafline.opds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.opds.model.BreadcrumbItem
import com.jdluu.leafline.opds.model.OpdsFeedSource
import com.jdluu.leafline.theme.LeaflineTextField
import com.jdluu.leafline.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OpdsTopBar(
    selectedSource: OpdsFeedSource?,
    sources: List<OpdsFeedSource>,
    onSelectSource: (OpdsFeedSource) -> Unit,
    onAddFeedClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteSource: (String) -> Unit,
    onRemoveCredentials: (OpdsFeedSource) -> Unit,
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

    // With no catalog there is nothing to switch between and nothing to search, so the
    // bar keeps a neutral title and offers the only action that exists.
    if (selectedSource == null) {
        TopAppBar(
            title = {
                Text(
                    text = "Catalogs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            actions = {
                IconButton(onClick = onAddFeedClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add catalog")
                }
            }
        )
        return
    }

    if (isSearchActive) {
        TopAppBar(
            title = {
                LeaflineTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = "Search catalog…",
                    imeAction = ImeAction.Search,
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                                        Row {
                                            // Removal is an explicit action, never a blank
                                            // field, so a reader can tell the stored account
                                            // is gone rather than merely hidden.
                                            if (source.hasAuth) {
                                                IconButton(onClick = {
                                                    showSourceDropdown = false
                                                    onRemoveCredentials(source)
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Default.LockOpen,
                                                        contentDescription = "Remove stored credentials"
                                                    )
                                                }
                                            }
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
internal fun OpdsBreadcrumbBar(
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
