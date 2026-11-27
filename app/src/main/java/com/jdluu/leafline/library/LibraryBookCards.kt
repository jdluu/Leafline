package com.jdluu.leafline.library

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.library.cover.BitmapMemoryCache
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.sync.ReadingProgressMath
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.theme.leaflineTween
import com.jdluu.leafline.theme.rememberReduceMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val COVER_ASPECT_RATIO = 0.7f

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
    val reduceMotion = rememberReduceMotion()
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
                    animationSpec = leaflineTween(200, reduceMotion),
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
                                else Color.Transparent
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
internal fun ContinueReadingTile(book: LibraryBook, onClick: () -> Unit) {
    val reduceMotion = rememberReduceMotion()
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
                    animationSpec = leaflineTween(200, reduceMotion),
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
