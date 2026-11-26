package com.jdluu.leafline.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.reader.search.BookSearchQuery
import com.jdluu.leafline.reader.search.BookSearchResult
import com.jdluu.leafline.reader.search.BookSearchState
import com.jdluu.leafline.reader.search.BookSearchStatus
import com.jdluu.leafline.reader.search.SearchOptions
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import kotlinx.coroutines.delay

internal const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BookSearchSheet(
    state: BookSearchState,
    currentChapterTitle: String? = null,
    currentChapterHref: String? = null,
    recentSearches: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSubmitSearch: (String, SearchOptions) -> Unit,
    onClearSearch: () -> Unit,
    onResultClick: (BookSearchResult) -> Unit,
    onRemoveRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {}
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var query by remember { mutableStateOf(state.query.orEmpty()) }
        var options by remember { mutableStateOf(state.options) }
        var lastSubmittedQuery by remember { mutableStateOf(state.query) }
        var lastSubmittedOptions by remember { mutableStateOf(state.options) }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.screen)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Search in book",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close search")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs)
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = "Search in book" },
                singleLine = true,
                placeholder = { Text("Find in this book") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            lastSubmittedQuery = null
                            onClearSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search query")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        val normalized = BookSearchQuery.normalize(query)
                        if (normalized != null) {
                            lastSubmittedQuery = normalized
                            lastSubmittedOptions = options
                            onSubmitSearch(normalized, options)
                        }
                    }
                )
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            // Search options filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = options.caseSensitive,
                    onClick = { options = options.copy(caseSensitive = !options.caseSensitive) },
                    label = { Text("Match case") },
                    leadingIcon = if (options.caseSensitive) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
                FilterChip(
                    selected = options.wholeWord,
                    onClick = { options = options.copy(wholeWord = !options.wholeWord) },
                    label = { Text("Whole word") },
                    leadingIcon = if (options.wholeWord) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
                FilterChip(
                    selected = options.regex,
                    onClick = { options = options.copy(regex = !options.regex) },
                    label = { Text("Regex") },
                    leadingIcon = if (options.regex) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
                if (currentChapterHref != null) {
                    val isChapterScoped = options.chapterHref != null
                    FilterChip(
                        selected = isChapterScoped,
                        onClick = {
                            options = options.copy(
                                chapterHref = if (isChapterScoped) null else currentChapterHref
                            )
                        },
                        label = {
                            Text(
                                if (isChapterScoped) {
                                    "Chapter: ${currentChapterTitle ?: "Current"}"
                                } else {
                                    "All chapters"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = if (isChapterScoped) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }

            // Debounced auto search
            LaunchedEffect(query, options) {
                val normalized = BookSearchQuery.normalize(query)
                when {
                    normalized == null -> {
                        lastSubmittedQuery = null
                        lastSubmittedOptions = options
                        onClearSearch()
                    }
                    normalized == lastSubmittedQuery && options == lastSubmittedOptions -> Unit
                    else -> {
                        delay(SEARCH_DEBOUNCE_MS)
                        lastSubmittedQuery = normalized
                        lastSubmittedOptions = options
                        onSubmitSearch(normalized, options)
                    }
                }
            }

            SearchStatusText(state, Modifier.padding(top = 4.dp, bottom = 4.dp))
        }

        if (query.isBlank()) {
            if (recentSearches.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Padding.screen, vertical = Spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent searches",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onClearRecentSearches) {
                            Text("Clear all")
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        recentSearches.forEach { recentQuery ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    query = recentQuery
                                    keyboardController?.hide()
                                    val normalized = BookSearchQuery.normalize(recentQuery)
                                    if (normalized != null) {
                                        lastSubmittedQuery = normalized
                                        lastSubmittedOptions = options
                                        onSubmitSearch(normalized, options)
                                    }
                                },
                                label = { Text(recentQuery) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove $recentQuery from history",
                                        modifier = Modifier.clickable { onRemoveRecentSearch(recentQuery) }
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Padding.screen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Search across chapters, footnotes, and text",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (state.status is BookSearchStatus.Completed && state.results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(Padding.screen),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp) // touch-target-ok: decorative icon
                    )
                    Text(
                        text = "No matches found for \"$query\"",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Check spelling or adjust search filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(state.results, key = { it.id }) { result ->
                    SearchResultRow(result = result, onClick = { onResultClick(result) })
                }
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
        Spacer(modifier = Modifier.height(Spacing.sm))
    }
}

@Composable
internal fun SearchStatusText(state: BookSearchState, modifier: Modifier = Modifier) {
    when (val status = state.status) {
        is BookSearchStatus.Idle -> Unit
        is BookSearchStatus.Searching -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 2.dp)
                    .semantics { contentDescription = "Searching" }
            )
        }
        is BookSearchStatus.Completed -> Text(
            text = if (status.resultCount == 0) "No matches found" else matchCountLabel(status.resultCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        is BookSearchStatus.Failed -> Text(
            text = status.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

internal fun matchCountLabel(count: Int): String {
    val total = count.toString()
    return if (count == 1) "$total match" else "$total matches"
}

@Composable
internal fun SearchResultRow(
    result: BookSearchResult,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClickLabel = "Open search result", onClick = onClick)
            .semantics(mergeDescendants = true) {}
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Padding.screen, vertical = Padding.compact)
        ) {
            if (!result.sectionTitle.isNullOrBlank()) {
                Text(
                    text = result.sectionTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = searchExcerpt(result),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
internal fun searchExcerpt(result: BookSearchResult): AnnotatedString {
    val highlightBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val highlightColor = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        append(result.excerptBefore)
        if (result.excerptMatch.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = highlightColor,
                    background = highlightBg
                )
            ) {
                append(result.excerptMatch)
            }
        }
        append(result.excerptAfter)
    }
}
