package com.jdluu.leafline.library

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.LeaflineApplication
import com.jdluu.leafline.hardcover.HardcoverBookPreference
import com.jdluu.leafline.hardcover.HardcoverIsbn
import com.jdluu.leafline.hardcover.HardcoverTrackingDecision
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.library.data.ReadingAggregate
import com.jdluu.leafline.sync.ReadingProgressMath
import com.jdluu.leafline.theme.LeaflineTextField
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.theme.leaflineTween
import com.jdluu.leafline.theme.rememberReduceMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDetailSheet(
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

    // Hardcover tracking for this one book. Read straight from the container rather
    // than threading two more parameters through this sheet: it is a single switch, and
    // the surrounding callbacks chain is already long.
    val trackingContext = LocalContext.current
    val container = remember(trackingContext) {
        (trackingContext.applicationContext as LeaflineApplication).appContainer
    }
    val tracker = container.hardcoverTracking
    val hardcoverEnabled = remember(tracker) {
        container.hardcoverConfigStore.load()?.enabled == true
    }
    val tracksThisBook = remember(book.stableId) {
        tracker.decide(book.stableId, BookIdentity.publicationOf(book.stableId)) ==
            HardcoverTrackingDecision.Track
    }
    var sendToHardcover by remember(book.stableId) { mutableStateOf(tracksThisBook) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState())
        ) {
            if (hardcoverEnabled &&
                HardcoverIsbn.isbn13From(BookIdentity.publicationOf(book.stableId)) != null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Send to Hardcover", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (sendToHardcover) {
                                "Progress is synced for this book."
                            } else {
                                "Not synced. Turn on to start sending it."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = sendToHardcover,
                        onCheckedChange = { on ->
                            sendToHardcover = on
                            tracker.remember(
                                book.stableId,
                                if (on) {
                                    HardcoverBookPreference.TRACK
                                } else {
                                    HardcoverBookPreference.IGNORE
                                }
                            )
                        }
                    )
                }
            }

            // Cover + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                val reduceMotion = rememberReduceMotion()
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
                        animationSpec = leaflineTween(200, reduceMotion),
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

            optimisticDescription?.takeIf { it.isNotBlank() }?.let { descriptionText ->
                var descriptionExpanded by remember { mutableStateOf(false) }
                Spacer(Modifier.height(Spacing.sm))
                HorizontalDivider()
                Spacer(Modifier.height(com.jdluu.leafline.theme.Padding.compact))
                Text("Description", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (descriptionText.length > 180) {
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
            Spacer(Modifier.height(com.jdluu.leafline.theme.Padding.compact))

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
                    Spacer(Modifier.height(com.jdluu.leafline.theme.Padding.compact))
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
            Spacer(Modifier.height(com.jdluu.leafline.theme.Padding.compact))

            // File info section
            Text("File info", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.xs))
            MetadataRow("Path", book.filePath)
            MetadataRow("Hash", book.fileHash.take(12) + "…")
            book.koreaderHash?.let { MetadataRow("KOReader hash", it.take(12) + "…") }

            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider()
            Spacer(Modifier.height(com.jdluu.leafline.theme.Padding.compact))

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
                    text = optimisticRating?.takeIf { it > 0 }?.let { "$it/5" } ?: "Unrated",
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
                FlowRow(
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
                LeaflineTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = "Add a tag…",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                FlowRow(
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
                LeaflineTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title *",
                    isError = title.isBlank()
                )
                LeaflineTextField(
                    value = authors,
                    onValueChange = { authors = it },
                    label = "Authors (comma-separated)"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    LeaflineTextField(
                        value = series,
                        onValueChange = { series = it },
                        label = "Series (optional)",
                        modifier = Modifier.weight(1f)
                    )
                    LeaflineTextField(
                        value = seriesIndex,
                        onValueChange = { seriesIndex = it },
                        label = "Volume #",
                        modifier = Modifier.weight(0.45f)
                    )
                }
                LeaflineTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = "Publisher (optional)"
                )
                LeaflineTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description (optional)",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 6
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

private fun formatEpochMillis(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return sdf.format(Date(millis))
}

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
