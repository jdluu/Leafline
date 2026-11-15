package com.jdluu.leafline.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing

/**
 * Reusable filter and search header for the library view.
 * Encapsulates search bar, collection filters, reading status chips, and tag chips.
 */
@Composable
fun LibraryFilterBar(
    searchActive: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    collections: List<Collection>,
    selectedCollection: Long?,
    onSelectCollection: (Long?) -> Unit,
    readingStatusFilter: ReadingStatus?,
    onSelectReadingStatus: (ReadingStatus?) -> Unit,
    allTags: List<String>,
    selectedTag: String?,
    tagCounts: Map<String, Int> = emptyMap(),
    onSelectTag: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (searchActive) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Padding.screen, vertical = 4.dp)
                    .semantics { contentDescription = "Search title or author" },
                placeholder = { Text("Search title or author") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = onCloseSearch
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            )
        }

        if (collections.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(horizontal = Padding.screen, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                item {
                    FilterChip(
                        selected = selectedCollection == null,
                        onClick = { onSelectCollection(null) },
                        label = { Text("All") },
                        modifier = Modifier.semantics { contentDescription = "All collections" }
                    )
                }
                items(collections, key = { it.id }) { collection ->
                    FilterChip(
                        selected = selectedCollection == collection.id,
                        onClick = { onSelectCollection(collection.id) },
                        label = { Text(collection.name) },
                        trailingIcon = if (selectedCollection == collection.id) {
                            {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp) // touch-target-ok: decorative chip icon
                                )
                            }
                        } else null
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier.padding(horizontal = Padding.screen, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            item {
                FilterChip(
                    selected = readingStatusFilter == null,
                    onClick = { onSelectReadingStatus(null) },
                    label = { Text("All") },
                    modifier = Modifier.semantics { contentDescription = "All reading statuses" }
                )
            }
            ReadingStatus.entries.forEach { status ->
                item {
                    FilterChip(
                        selected = readingStatusFilter == status,
                        onClick = { onSelectReadingStatus(status) },
                        label = {
                            Text(status.displayName)
                        }
                    )
                }
            }
        }

        if (allTags.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(horizontal = Padding.screen, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                item {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { onSelectTag(null) },
                        label = { Text("All tags") },
                        modifier = Modifier.semantics { contentDescription = "All tags" }
                    )
                }
                items(allTags, key = { it }) { tag ->
                    val isSelected = selectedTag.equals(tag, ignoreCase = true)
                    val count = tagCounts[tag.trim().lowercase()] ?: 0
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectTag(if (isSelected) null else tag)
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("#$tag")
                                if (count > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp) // touch-target-ok: decorative chip icon
                                )
                            }
                        } else null,
                        modifier = Modifier.semantics { contentDescription = "Tag $tag, $count books" }
                    )
                }
            }
        }
    }
}
