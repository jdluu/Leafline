package com.jdluu.leafline.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.library.data.LocatorIdentity
import com.jdluu.leafline.reader.annotations.HighlightUndoCoordinator
import com.jdluu.leafline.reader.annotations.confirmHighlightDismiss
import com.jdluu.leafline.theme.HIGHLIGHT_TINTS
import com.jdluu.leafline.theme.HighlightTint
import com.jdluu.leafline.theme.LeaflineTextField
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarkListSheet(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Bookmarks",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(Padding.screen)
        )
        if (bookmarks.isEmpty()) {
            Text(
                "No bookmarks yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Padding.screen)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onDeleteBookmark(bookmark)
                                    true
                                } else {
                                    false
                                }
                            }
                        ),
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(end = Spacing.md)
                                )
                            }
                        }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClickLabel = "Open bookmark",
                                    onClick = { onBookmarkClick(bookmark) }
                                )
                                .semantics {
                                    customActions = listOf(
                                        CustomAccessibilityAction("Delete bookmark") {
                                            onDeleteBookmark(bookmark)
                                            true
                                        }
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = Padding.screen, vertical = Padding.compact)
                            ) {
                                Text(
                                    text = bookmark.label
                                        ?: LocatorIdentity.displayTitle(bookmark.locatorJson)
                                        ?: "Bookmark",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatCreatedAt(bookmark.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

internal fun formatCreatedAt(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

internal fun annotationExcerpt(annotation: Annotation): String {
    val text = runCatching {
        org.json.JSONObject(annotation.locatorJson).optJSONObject("text")
    }.getOrNull()
    val exact = text?.optString("exact")?.takeIf { it.isNotBlank() }
    val highlight = text?.optString("highlight")?.takeIf { it.isNotBlank() }
    return when {
        exact != null -> collapseWhitespace(exact)
        text != null && highlight != null -> excerptAround(
            before = text.optString("before"),
            highlight = highlight,
            after = text.optString("after")
        )
        else -> LocatorIdentity.displayTitle(annotation.locatorJson) ?: "Highlight"
    }
}

/** Readium omits `text.exact` for some locators, so quote the highlight with its context. */
private fun excerptAround(before: String, highlight: String, after: String): String {
    // Slice raw text first and collapse whitespace only after joining, otherwise the
    // boundary spaces around the highlight are lost ("monopolisingthebusiness").
    // Cut on word boundaries, but only when the slice was actually truncated: a short
    // context starts and ends on real word boundaries already.
    val leadSlice = before.takeLast(EXCERPT_CONTEXT_CHARS)
    val trailSlice = after.take(EXCERPT_CONTEXT_CHARS)
    val lead = if (before.length > EXCERPT_CONTEXT_CHARS) trimToLastWord(leadSlice) else leadSlice
    val trail = if (after.length > EXCERPT_CONTEXT_CHARS) trimToFirstWord(trailSlice) else trailSlice
    val body = collapseWhitespace(lead + highlight + trail).take(EXCERPT_MAX_CHARS).trimEnd()
    return buildString {
        if (lead.isNotBlank()) append("…")
        append(body)
        if (trail.isNotBlank()) append("…")
    }
}

/** Drops a partial leading word left by a mid-sentence slice. */
private fun trimToLastWord(text: String): String =
    text.indexOf(' ').let { if (it >= 0) text.substring(it + 1) else text }

/** Drops a partial trailing word left by a mid-sentence slice. */
private fun trimToFirstWord(text: String): String =
    text.lastIndexOf(' ').let { if (it >= 0) text.substring(0, it) else text }

private fun collapseWhitespace(value: String): String =
    value.replace(Regex("\\s+"), " ").trim()

private const val EXCERPT_CONTEXT_CHARS = 60
private const val EXCERPT_MAX_CHARS = 90

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnnotationListSheet(
    annotations: List<Annotation>,
    onAnnotationClick: (Annotation) -> Unit,
    onDeleteAnnotation: (Annotation) -> Unit,
    onRestoreAnnotation: (Annotation) -> Unit,
    onEditAnnotation: (Annotation, String?) -> Unit,
    onChangeColor: (Annotation, HighlightTint) -> Unit,
    onExportAnnotations: (List<Annotation>) -> Unit,
    onDismiss: () -> Unit,
    excerptFor: (Annotation) -> String = { annotationExcerpt(it) }
) {
    var editDialogAnnotation by remember { mutableStateOf<Annotation?>(null) }
    var editNoteText by remember { mutableStateOf("") }
    var colorPickerAnnotation by remember { mutableStateOf<Annotation?>(null) }
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoCoordinator = remember {
        HighlightUndoCoordinator(
            delete = onDeleteAnnotation,
            restore = onRestoreAnnotation,
            showUndoSnackbar = {
                snackbarHostState.showSnackbar(
                    message = "Highlight deleted",
                    actionLabel = "Undo"
                )
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        var searchQuery by remember { mutableStateOf("") }
        var selectedTint by remember { mutableStateOf<HighlightTint?>(null) }

        Text(
            "Highlights",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(Padding.screen)
        )
        if (annotations.isNotEmpty()) {
            LeaflineTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search highlights or notes",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Padding.screen, vertical = Spacing.xs)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Padding.screen, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = selectedTint == null,
                    onClick = { selectedTint = null },
                    label = { Text("All (${annotations.size})") }
                )
                HIGHLIGHT_TINTS.forEach { tint ->
                    val count = annotations.count { it.colorHex.equals(tint.hex, ignoreCase = true) }
                    if (count > 0) {
                        FilterChip(
                            selected = selectedTint == tint,
                            onClick = { selectedTint = if (selectedTint == tint) null else tint },
                            label = { Text("${tint.label} ($count)") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp) // touch-target-ok: decorative swatch inside FilterChip
                                        .clip(CircleShape)
                                        .background(tint.swatchColor)
                                )
                            }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { onExportAnnotations(annotations) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Padding.screen)
            ) {
                Text("Export highlights")
            }
        }

        val currentTint = selectedTint
        val filteredAnnotations = remember(annotations, searchQuery, currentTint) {
            annotations.filter { annotation ->
                val matchesTint = currentTint == null || annotation.colorHex.equals(currentTint.hex, ignoreCase = true)
                val matchesQuery = searchQuery.isBlank() ||
                    excerptFor(annotation).contains(searchQuery, ignoreCase = true) ||
                    (annotation.note?.contains(searchQuery, ignoreCase = true) == true)
                matchesTint && matchesQuery
            }
        }

        if (annotations.isEmpty()) {
            Text(
                "No highlights yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Padding.screen)
            )
        } else if (filteredAnnotations.isEmpty()) {
            Text(
                "No matching highlights found",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Padding.screen)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filteredAnnotations, key = { it.id }) { annotation ->
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (confirmHighlightDismiss(value)) {
                                    scope.launch { undoCoordinator.deleteWithUndo(annotation) }
                                    true
                                } else {
                                    false
                                }
                            }
                        ),
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(end = Spacing.md)
                                )
                            }
                        }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClickLabel = "Open highlight",
                                    onClick = { onAnnotationClick(annotation) }
                                )
                                .semantics {
                                    customActions = listOf(
                                        CustomAccessibilityAction("Delete highlight") {
                                            scope.launch { undoCoordinator.deleteWithUndo(annotation) }
                                            true
                                        }
                                    )
                                }
                        ) {
                            val excerpt = excerptFor(annotation)
                            val excerptShort = excerpt.take(30)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    start = Padding.screen,
                                    top = Padding.compact,
                                    bottom = Padding.compact,
                                    end = 4.dp
                                )
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = excerpt,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatCreatedAt(annotation.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editDialogAnnotation = annotation
                                        editNoteText = annotation.note.orEmpty()
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit note for highlight: $excerptShort")
                                }
                                IconButton(
                                    onClick = {
                                        colorPickerAnnotation = annotation
                                    }
                                ) {
                                    Icon(Icons.Default.FormatColorFill, contentDescription = "Change color for highlight: $excerptShort")
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch { undoCoordinator.deleteWithUndo(annotation) }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete highlight: $excerptShort")
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        // The sheet renders in its own window above the overlay, so its undo
        // snackbar must be hosted here to stay visible over the sheet scrim.
        SnackbarHost(hostState = snackbarHostState)
    }

    editDialogAnnotation?.let { annotation ->
        AlertDialog(
            onDismissRequest = { editDialogAnnotation = null },
            title = { Text("Edit note") },
            text = {
                LeaflineTextField(
                    value = editNoteText,
                    onValueChange = { editNoteText = it },
                    label = "Note",
                    placeholder = "Add a note...",
                    imeAction = ImeAction.Done,
                    singleLine = false,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Padding.screen)
                        .focusRequester(focusRequester)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditAnnotation(annotation, editNoteText)
                    editDialogAnnotation = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editDialogAnnotation = null }) { Text("Cancel") }
            }
        )
    }

    colorPickerAnnotation?.let { annotation ->
        HighlightTintPickerSheet(
            onDismiss = { colorPickerAnnotation = null },
            onTintSelected = { tint ->
                onChangeColor(annotation, tint)
                colorPickerAnnotation = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HighlightTintPickerSheet(
    onDismiss: () -> Unit,
    onTintSelected: (HighlightTint) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(Padding.screen)) {
            Text(
                "Choose highlight color",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HIGHLIGHT_TINTS.forEach { tint ->
                    FilterChip(
                        selected = false,
                        onClick = { onTintSelected(tint) },
                        label = { Text(tint.label) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp) // touch-target-ok: decorative color swatch in leading icon
                                    .background(
                                        tint.swatchColor,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    )
                }
            }
        }
    }
}
