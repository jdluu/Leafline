package com.jdluu.leafline.reader.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.reader.ui.HighlightTintPickerSheet
import com.jdluu.leafline.reader.ui.TocItem
import com.jdluu.leafline.reader.ui.annotationExcerpt
import com.jdluu.leafline.reader.ui.formatCreatedAt
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.library.data.LocatorIdentity
import com.jdluu.leafline.sync.ReadingProgressMath
import com.jdluu.leafline.reader.ui.matchesChapter
import com.jdluu.leafline.reader.annotations.HighlightUndoCoordinator
import com.jdluu.leafline.reader.annotations.confirmHighlightDismiss
import com.jdluu.leafline.theme.HIGHLIGHT_TINTS
import com.jdluu.leafline.theme.HighlightTint
import com.jdluu.leafline.theme.LeaflineTextField
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Link

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderNavigationDrawerContent(
    selectedTab: ReaderNavigationTab,
    onTabSelected: (ReaderNavigationTab) -> Unit,
    // TOC
    tocLinks: List<Pair<Link, Int>>,
    currentChapterHref: String?,
    onTocClick: (Link) -> Unit,
    // Bookmarks
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    // Highlights
    annotations: List<Annotation>,
    onAnnotationClick: (Annotation) -> Unit,
    onDeleteAnnotation: (Annotation) -> Unit,
    onRestoreAnnotation: (Annotation) -> Unit,
    onEditAnnotation: (Annotation, String?) -> Unit,
    onChangeColor: (Annotation, HighlightTint) -> Unit,
    onExportAnnotations: (List<Annotation>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Scrollable rather than fixed: a fixed row splits the width three ways and
        // ellipsises the longest label ("Bookma…"). Tabs now size to their text.
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            ReaderNavigationTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        // Counts live inside each pane; appending them to the title only
                        // widened the labels.
                        Text(
                            text = tab.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            ReaderNavigationTab.CONTENTS -> {
                TocTabContent(
                    tocLinks = tocLinks,
                    currentChapterHref = currentChapterHref,
                    onTocClick = onTocClick,
                    modifier = Modifier.weight(1f)
                )
            }
            ReaderNavigationTab.BOOKMARKS -> {
                BookmarkTabContent(
                    bookmarks = bookmarks,
                    onBookmarkClick = onBookmarkClick,
                    onDeleteBookmark = onDeleteBookmark,
                    modifier = Modifier.weight(1f)
                )
            }
            ReaderNavigationTab.HIGHLIGHTS -> {
                HighlightTabContent(
                    annotations = annotations,
                    onAnnotationClick = onAnnotationClick,
                    onDeleteAnnotation = onDeleteAnnotation,
                    onRestoreAnnotation = onRestoreAnnotation,
                    onEditAnnotation = onEditAnnotation,
                    onChangeColor = onChangeColor,
                    onExportAnnotations = onExportAnnotations,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TocTabContent(
    tocLinks: List<Pair<Link, Int>>,
    currentChapterHref: String?,
    onTocClick: (Link) -> Unit,
    modifier: Modifier = Modifier
) {
    val tocListState = rememberLazyListState()
    val activeTocIndex = remember(tocLinks, currentChapterHref) {
        tocLinks.indexOfFirst { (link, _) ->
            matchesChapter(link.href.toString(), currentChapterHref)
        }
    }
    var collapsedTocKeys by remember { mutableStateOf(setOf<String>()) }
    val allParentKeys = remember(tocLinks) {
        tocLinks.filter { (link, _) -> link.children.isNotEmpty() }
            .map { (link, _) -> tocLinkKey(link) }
            .toSet()
    }
    val hasNestedTocItems = allParentKeys.isNotEmpty()

    LaunchedEffect(activeTocIndex) {
        if (activeTocIndex >= 0) {
            val ancestors = findAncestorKeysForIndex(tocLinks, activeTocIndex)
            if (ancestors.any { it in collapsedTocKeys }) {
                collapsedTocKeys = collapsedTocKeys - ancestors
            }
        }
    }

    val visibleTocLinks = remember(tocLinks, collapsedTocKeys) {
        filterVisibleTocLinks(tocLinks, collapsedTocKeys)
    }

    LaunchedEffect(activeTocIndex) {
        if (activeTocIndex >= 0) {
            val visibleIndex = visibleTocLinks.indexOfFirst { (link, _) ->
                matchesChapter(link.href.toString(), currentChapterHref)
            }
            if (visibleIndex >= 0) {
                tocListState.animateScrollToItem(visibleIndex)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.screen, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sections",
                    style = MaterialTheme.typography.titleMedium
                )
                if (tocLinks.isNotEmpty()) {
                    Text(
                        text = if (tocLinks.size == 1) "1 section" else "${tocLinks.size} sections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (hasNestedTocItems) {
                TextButton(
                    onClick = {
                        collapsedTocKeys = if (collapsedTocKeys.isEmpty()) {
                            allParentKeys
                        } else {
                            emptySet()
                        }
                    }
                ) {
                    Text(if (collapsedTocKeys.isEmpty()) "Collapse all" else "Expand all")
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = Padding.screen),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (tocLinks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Padding.screen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No table of contents available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(state = tocListState, modifier = Modifier.fillMaxSize()) {
                items(
                    items = visibleTocLinks,
                    key = { (link, _) -> tocLinkKey(link) }
                ) { (link, depth) ->
                    val isCurrent = matchesChapter(link.href.toString(), currentChapterHref)
                    val key = tocLinkKey(link)
                    val hasChildren = link.children.isNotEmpty()
                    val isExpanded = hasChildren && key !in collapsedTocKeys
                    TocItem(
                        link = link,
                        depth = depth,
                        isCurrent = isCurrent,
                        hasChildren = hasChildren,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            collapsedTocKeys = if (isExpanded) {
                                collapsedTocKeys + key
                            } else {
                                collapsedTocKeys - key
                            }
                        },
                        onClick = { onTocClick(link) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkTabContent(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bookmarks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(Padding.screen),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Spacing.lg)
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp) // touch-target-ok: decorative empty state icon
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "No bookmarks yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Add bookmarks while reading to quickly jump back to important pages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            item {
                Text(
                    text = if (bookmarks.size == 1) "1 bookmark" else "${bookmarks.size} bookmarks",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Padding.screen, vertical = Spacing.xs)
                )
            }
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
                    val progressLabel = ReadingProgressMath.progressLabel(bookmark.locatorJson)
                    val title = bookmark.label
                        ?: LocatorIdentity.displayTitle(bookmark.locatorJson)
                        ?: "Bookmark"

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Padding.screen, vertical = Padding.compact)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitle = buildString {
                                    if (progressLabel != null) {
                                        append(progressLabel)
                                        append(" • ")
                                    }
                                    append(formatCreatedAt(bookmark.createdAt))
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteBookmark(bookmark) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete bookmark $title",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HighlightTabContent(
    annotations: List<Annotation>,
    onAnnotationClick: (Annotation) -> Unit,
    onDeleteAnnotation: (Annotation) -> Unit,
    onRestoreAnnotation: (Annotation) -> Unit,
    onEditAnnotation: (Annotation, String?) -> Unit,
    onChangeColor: (Annotation, HighlightTint) -> Unit,
    onExportAnnotations: (List<Annotation>) -> Unit,
    modifier: Modifier = Modifier,
    excerptFor: (Annotation) -> String = { annotationExcerpt(it) }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTint by remember { mutableStateOf<HighlightTint?>(null) }
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

    Column(modifier = modifier.fillMaxSize()) {
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
                onClick = { onExportAnnotations(filteredAnnotations) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Padding.screen, vertical = Spacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp) // touch-target-ok: decorative button icon
                )
                Spacer(Modifier.size(Spacing.xs)) // touch-target-ok: layout spacing
                Text("Export highlights")
            }
        }

        if (annotations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Padding.screen),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(Spacing.lg)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatColorFill,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp) // touch-target-ok: decorative empty state icon
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "No highlights yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Select text while reading to create highlights, notes, and exportable quotes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (filteredAnnotations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Padding.screen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching highlights found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
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
                                .defaultMinSize(minHeight = 48.dp)
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
                                    if (!annotation.note.isNullOrBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Note: ${annotation.note}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
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
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit note for highlight: $excerptShort"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        colorPickerAnnotation = annotation
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.FormatColorFill,
                                        contentDescription = "Change color for highlight: $excerptShort"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch { undoCoordinator.deleteWithUndo(annotation) }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete highlight: $excerptShort"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

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
