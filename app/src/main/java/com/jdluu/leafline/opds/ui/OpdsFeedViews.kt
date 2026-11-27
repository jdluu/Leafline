package com.jdluu.leafline.opds.ui

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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.opds.OpdsViewModel
import com.jdluu.leafline.opds.model.DownloadStatus
import com.jdluu.leafline.opds.model.OpdsBook
import com.jdluu.leafline.opds.model.OpdsFeedResult
import com.jdluu.leafline.opds.model.OpdsNavigationLink
import com.jdluu.leafline.opds.rememberOpdsCoverBitmap
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing

private const val COVER_ASPECT_RATIO = 1f / 1.5f

@Composable
internal fun FeedContentView(
    feed: OpdsFeedResult,
    viewModel: OpdsViewModel,
    downloadStatuses: Map<String, DownloadStatus>,
    gridState: LazyGridState = rememberLazyGridState(),
    onSubfeedClick: (OpdsNavigationLink) -> Unit,
    onBookClick: (OpdsBook) -> Unit,
    onDownloadClick: (OpdsBook) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val columns = when {
        screenWidth < 600 -> 2
        screenWidth < 840 -> 3
        else -> 4
    }

    // Catalogs sometimes expose a "Home" link that points back at the feed itself; treat
    // those as noise so the reader is not sent in a loop and the empty state can appear.
    val navigationLinks = feed.navigationLinks.filterNot { it.href == feed.feedUrl }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = PaddingValues(Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize()
    ) {
        // Navigation subsections (e.g. Categories, Genres)
        if (navigationLinks.isNotEmpty()) {
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
                        navigationLinks.forEach { navLink ->
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
        if (feed.books.isEmpty() && feed.groups.isEmpty() && navigationLinks.isEmpty()) {
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
private fun SurfaceBadge(label: String, color: Color) {
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
