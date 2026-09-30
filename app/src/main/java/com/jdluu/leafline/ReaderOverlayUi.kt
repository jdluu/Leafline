package com.jdluu.leafline

import androidx.compose.material3.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import com.jdluu.leafline.reader.tts.ReaderTtsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.library.data.BookmarkRepository
import com.jdluu.leafline.library.data.BookmarkToggleResult
import com.jdluu.leafline.library.data.LocatorIdentity
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.reader.BRIGHTNESS_MAX
import com.jdluu.leafline.reader.BRIGHTNESS_MIN
import com.jdluu.leafline.reader.PAGE_MARGINS_DEFAULT
import com.jdluu.leafline.reader.PAGE_MARGINS_MAX
import com.jdluu.leafline.reader.PAGE_MARGINS_MIN
import com.jdluu.leafline.reader.PAGE_MARGINS_STEP
import com.jdluu.leafline.reader.READER_FONT_FAMILIES
import com.jdluu.leafline.reader.PageTurnAnimation
import com.jdluu.leafline.reader.ReaderPreferencesStore
import com.jdluu.leafline.reader.ReaderSettings
import com.jdluu.leafline.reader.StyleMode
import com.jdluu.leafline.reader.TapZoneAction
import com.jdluu.leafline.reader.TapZoneConfig
import com.jdluu.leafline.reader.clampBrightness
import com.jdluu.leafline.reader.effectiveTapZoneAction
import com.jdluu.leafline.reader.snapPageMargins
import com.jdluu.leafline.reader.styleModeFor
import com.jdluu.leafline.reader.pageTurnIsAnimated
import com.jdluu.leafline.reader.toggleSepia
import com.jdluu.leafline.reader.annotations.HighlightUndoCoordinator
import com.jdluu.leafline.reader.annotations.confirmHighlightDismiss
import com.jdluu.leafline.reader.search.BookSearchQuery
import com.jdluu.leafline.theme.DEFAULT_HIGHLIGHT_TINT
import com.jdluu.leafline.theme.HIGHLIGHT_TINTS
import com.jdluu.leafline.theme.HighlightTint
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.reader.search.BookSearchResult
import com.jdluu.leafline.reader.search.BookSearchState
import com.jdluu.leafline.reader.search.BookSearchStatus
import com.jdluu.leafline.reader.search.BookSearcher
import com.jdluu.leafline.reader.tapZoneAt
import com.jdluu.leafline.reader.theme.ReaderTheme
import com.jdluu.leafline.reader.withStyleMode
import com.jdluu.leafline.sync.BookRef
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import com.jdluu.leafline.sync.ProgressSyncer
import com.jdluu.leafline.sync.PullOutcome
import com.jdluu.leafline.sync.PushOutcome
import com.jdluu.leafline.sync.ReadingProgressMath
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderOverlay(
    title: String,
    toolbarVisible: Boolean,
    tocLinks: List<Pair<Link, Int>>,
    currentSettings: ReaderSettings,
    drawerState: DrawerState,
    settingsSheetVisible: Boolean,
    bookmarks: List<Bookmark>,
    bookmarkActionsEnabled: Boolean,
    currentLocatorJson: String?,
    bookmarkSheetVisible: Boolean,
    annotations: List<Annotation>,
    highlightsSheetVisible: Boolean,
    searchSheetVisible: Boolean,
    searchState: BookSearchState,
    pageAnnouncement: String?,
    snackbarHostState: SnackbarHostState,
    syncConflict: SyncConflictState?,
    onJumpToRemote: () -> Unit,
    onDismissSyncConflict: () -> Unit,
    onBack: () -> Unit,
    onOpenToc: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onDismissBookmarks: () -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onOpenHighlights: () -> Unit,
    onDismissHighlights: () -> Unit,
    onAnnotationClick: (Annotation) -> Unit,
    onDeleteAnnotation: (Annotation) -> Unit,
    onRestoreAnnotation: (Annotation) -> Unit,
    onEditAnnotation: (Annotation, String?) -> Unit,
    onChangeColor: (Annotation, HighlightTint) -> Unit,
    onExportAnnotations: (List<Annotation>) -> Unit,
    onTocClick: (Link) -> Unit,
    onSettingsChange: (ReaderSettings) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onBrightnessReset: () -> Unit,
    onToggleSepia: () -> Unit,
    onOpenSearch: () -> Unit,
    onDismissSearch: () -> Unit,
    onSubmitSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResultClick: (BookSearchResult) -> Unit,
    highlightTintSheetVisible: Boolean,
    onHighlightTintSelected: (HighlightTint) -> Unit,
    ttsState: ReaderTtsState = ReaderTtsState.UNAVAILABLE,
    onTtsPlay: () -> Unit = {},
    onTtsPause: () -> Unit = {},
    onTtsStop: () -> Unit = {}
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Contents",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(Padding.screen)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn {
                    items(tocLinks) { (link, depth) ->
                        TocItem(link = link, depth = depth, onClick = { onTocClick(link) })
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (toolbarVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 6.dp
                    ) {
                        ReaderTopBar(
                            title = title,
                            bookmarks = bookmarks,
                            bookmarkActionsEnabled = bookmarkActionsEnabled,
                            currentLocatorJson = currentLocatorJson,
                            onBack = onBack,
                            onOpenToc = onOpenToc,
                            onOpenSearch = onOpenSearch,
                            onOpenSettings = onOpenSettings,
                            onToggleBookmark = onToggleBookmark,
                            onOpenBookmarks = onOpenBookmarks,
                            onOpenHighlights = onOpenHighlights
                        )
                    }
                    BrightnessControl(
                        brightness = currentSettings.brightness,
                        onBrightnessChange = onBrightnessChange,
                        onReset = onBrightnessReset
                    )
                    QuickControls(
                        sepiaSelected = currentSettings.theme == ReaderTheme.SEPIA,
                        onToggleSepia = onToggleSepia,
                        ttsState = ttsState,
                        onTtsPlay = onTtsPlay,
                        onTtsPause = onTtsPause,
                        onTtsStop = onTtsStop
                    )
                }
            }

            if (settingsSheetVisible) {
                ReaderSettingsSheet(
                    settings = currentSettings,
                    onSettingsChange = onSettingsChange,
                    onDismiss = onDismissSettings
                )
            }

            if (bookmarkSheetVisible) {
                BookmarkListSheet(
                    bookmarks = bookmarks,
                    onBookmarkClick = onBookmarkClick,
                    onDeleteBookmark = onDeleteBookmark,
                    onDismiss = onDismissBookmarks
                )
            }

            if (highlightsSheetVisible) {
                AnnotationListSheet(
                    annotations = annotations,
                    onAnnotationClick = onAnnotationClick,
                    onDeleteAnnotation = onDeleteAnnotation,
                    onRestoreAnnotation = onRestoreAnnotation,
                    onEditAnnotation = onEditAnnotation,
                    onChangeColor = onChangeColor,
                    onExportAnnotations = onExportAnnotations,
                    onDismiss = onDismissHighlights
                )
            }

            if (searchSheetVisible) {
                BookSearchSheet(
                    state = searchState,
                    onDismiss = onDismissSearch,
                    onSubmitSearch = onSubmitSearch,
                    onClearSearch = onClearSearch,
                    onResultClick = onSearchResultClick
                )
            }

            if (highlightTintSheetVisible) {
                HighlightTintPickerSheet(
                    onDismiss = { onHighlightTintSelected(DEFAULT_HIGHLIGHT_TINT) },
                    onTintSelected = onHighlightTintSelected
                )
            }

            syncConflict?.let { conflict ->
                SyncConflictSheet(
                    conflict = conflict,
                    onJump = onJumpToRemote,
                    onDismiss = onDismissSyncConflict
                )
            }

            pageAnnouncement?.let { announcement ->
                Text(
                    text = announcement,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyncConflictSheet(
    conflict: SyncConflictState,
    onJump: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("Progress sync", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Local", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = conflict.localPercentage?.let { "%.0f%%".format(it) } ?: "—",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Remote", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = conflict.remotePercentage?.let { "%.0f%%".format(it) } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(Padding.compact))

            if (conflict.remoteDevice != null) {
                MetadataRow("Device", conflict.remoteDevice)
            }
            conflict.remoteTimestamp?.let { ts ->
                MetadataRow("Last synced", formatEpochMillis(ts))
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Padding.compact)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("Stay") }
                Button(
                    onClick = onJump,
                    modifier = Modifier.weight(1f)
                ) { Text("Jump to remote") }
            }

            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrightnessControl(
    brightness: Float?,
    onBrightnessChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = Padding.screen)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Brightness6,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = Spacing.xs)
                )
                Text(
                    text = "Brightness",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = brightness?.let { "%.0f%%".format(it * 100) } ?: "System",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.semantics {
                        contentDescription = "Reset brightness to system default"
                    }
                ) {
                    Text("Reset")
                }
            }
            Slider(
                value = brightness ?: BRIGHTNESS_MAX,
                onValueChange = { onBrightnessChange(clampBrightness(it)) },
                valueRange = BRIGHTNESS_MIN..BRIGHTNESS_MAX,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Brightness" }
            )
        }
    }
}

/**
 * Quick controls row in the reader overlay toolbar, providing one-tap
 * theme switching (sepia) and explicit text-to-speech controls (play, pause, stop).
 */
@Composable
internal fun QuickControls(
    sepiaSelected: Boolean,
    onToggleSepia: () -> Unit,
    ttsState: ReaderTtsState = ReaderTtsState.UNAVAILABLE,
    onTtsPlay: () -> Unit = {},
    onTtsPause: () -> Unit = {},
    onTtsStop: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.padding(horizontal = Padding.screen, vertical = Spacing.xs)
        ) {
            FilterChip(
                selected = sepiaSelected,
                onClick = onToggleSepia,
                label = { Text("Sepia") },
                leadingIcon = {
                    Icon(Icons.Default.WbSunny, contentDescription = null)
                },
                modifier = Modifier.semantics {
                    contentDescription = if (sepiaSelected) "Disable sepia theme" else "Enable sepia theme"
                }
            )
            when (ttsState) {
                ReaderTtsState.PLAYING -> {
                    FilterChip(
                        selected = true,
                        onClick = onTtsPause,
                        label = { Text("Pause") },
                        leadingIcon = {
                            Icon(Icons.Default.Pause, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Pause text-to-speech"
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onTtsStop,
                        label = { Text("Stop") },
                        leadingIcon = {
                            Icon(Icons.Default.Stop, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Stop text-to-speech"
                        }
                    )
                }
                ReaderTtsState.PAUSED -> {
                    FilterChip(
                        selected = false,
                        onClick = onTtsPlay,
                        label = { Text("Resume") },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Resume text-to-speech"
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onTtsStop,
                        label = { Text("Stop") },
                        leadingIcon = {
                            Icon(Icons.Default.Stop, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Stop text-to-speech"
                        }
                    )
                }
                ReaderTtsState.ERROR -> {
                    FilterChip(
                        selected = false,
                        onClick = onTtsPlay,
                        label = { Text("Retry TTS") },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Retry text-to-speech"
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onTtsStop,
                        label = { Text("Stop") },
                        leadingIcon = {
                            Icon(Icons.Default.Stop, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Stop text-to-speech"
                        }
                    )
                }
                ReaderTtsState.IDLE -> {
                    FilterChip(
                        selected = false,
                        onClick = onTtsPlay,
                        label = { Text("Read aloud") },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Read aloud with text-to-speech"
                        }
                    )
                }
                ReaderTtsState.UNAVAILABLE -> {
                    // Hidden when TTS is not supported for the publication
                }
            }
        }
    }
}

/**
 * Warm/sepia quick control shown next to the brightness slider in the reader
 * overlay. Selected means the publication renders with the sepia theme;
 * toggling swaps between sepia and the previously active theme, persisted by
 * [ReaderPreferencesStore] across restarts. The full light, sepia, and dark
 * choice stays in the settings sheet.
 */
@Composable
internal fun SepiaQuickControl(
    selected: Boolean,
    onToggle: () -> Unit
) {
    QuickControls(
        sepiaSelected = selected,
        onToggleSepia = onToggle
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderTopBar(
    title: String,
    bookmarks: List<Bookmark>,
    bookmarkActionsEnabled: Boolean,
    currentLocatorJson: String?,
    onBack: () -> Unit,
    onOpenToc: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHighlights: () -> Unit
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val currentKey = remember(currentLocatorJson) {
        currentLocatorJson?.let { LocatorIdentity.key(it) }
    }
    val isCurrentLocationBookmarked = currentKey != null && bookmarks.any {
        LocatorIdentity.key(it.locatorJson) == currentKey
    }
    TopAppBar(
        title = { 
            Text(
                title, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onOpenToc) {
                Icon(Icons.Default.Menu, contentDescription = "Contents")
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search in book")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Reader settings")
            }
            IconButton(onClick = { moreMenuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(if (isCurrentLocationBookmarked) "Remove bookmark" else "Add bookmark")
                    },
                    onClick = {
                        moreMenuExpanded = false
                        onToggleBookmark()
                    },
                    leadingIcon = {
                        Icon(
                            if (isCurrentLocationBookmarked) Icons.Default.Bookmark
                            else Icons.Default.BookmarkBorder,
                            contentDescription = null
                        )
                    },
                    enabled = bookmarkActionsEnabled && currentLocatorJson != null
                )
                DropdownMenuItem(
                    text = { Text("Bookmarks") },
                    onClick = {
                        moreMenuExpanded = false
                        onOpenBookmarks()
                    },
                    leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                    enabled = bookmarkActionsEnabled
                )
                DropdownMenuItem(
                    text = { Text("Highlights") },
                    onClick = {
                        moreMenuExpanded = false
                        onOpenHighlights()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.FormatColorFill, contentDescription = null)
                    },
                    enabled = bookmarkActionsEnabled
                )
            }
        }
    )
}

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
    return try {
        val text = org.json.JSONObject(annotation.locatorJson).optJSONObject("text")
        text?.optString("exact")?.takeIf { it.isNotBlank() }
            ?: LocatorIdentity.displayTitle(annotation.locatorJson)
            ?: "Highlight"
    } catch (e: Exception) {
        LocatorIdentity.displayTitle(annotation.locatorJson) ?: "Highlight"
    }
}

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
        Text(
            "Highlights",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(Padding.screen)
        )
        if (annotations.isNotEmpty()) {
            OutlinedButton(
                onClick = { onExportAnnotations(annotations) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Padding.screen)
            ) {
                Text("Export highlights")
            }
        }
        if (annotations.isEmpty()) {
            Text(
                "No highlights yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Padding.screen)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(annotations, key = { it.id }) { annotation ->
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
                OutlinedTextField(
                    value = editNoteText,
                    onValueChange = { editNoteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Padding.screen)
                        .focusRequester(focusRequester),
                    singleLine = false,
                    maxLines = 5,
                    placeholder = { Text("Add a note...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    label = { Text("Note") }
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
internal fun BookSearchSheet(
    state: BookSearchState,
    onDismiss: () -> Unit,
    onSubmitSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onResultClick: (BookSearchResult) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var query by remember { mutableStateOf(state.query.orEmpty()) }
        var lastSubmitted by remember { mutableStateOf(state.query) }
        val focusRequester = remember { FocusRequester() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.screen)
        ) {
            Text(
                "Search in book",
                style = MaterialTheme.typography.titleMedium
            )
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            // Debounced auto search: restarting this effect on each keystroke
            // cancels the pending submission until typing settles.
            LaunchedEffect(query) {
                val normalized = BookSearchQuery.normalize(query)
                when {
                    normalized == null -> {
                        lastSubmitted = null
                        onClearSearch()
                    }
                    normalized == lastSubmitted -> Unit
                    else -> {
                        delay(SEARCH_DEBOUNCE_MS)
                        lastSubmitted = normalized
                        onSubmitSearch(normalized)
                    }
                }
            }

            SearchStatusText(state, Modifier.padding(top = 12.dp, bottom = 4.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            items(state.results, key = { it.id }) { result ->
                SearchResultRow(result = result, onClick = { onResultClick(result) })
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
    return buildAnnotatedString {
        append(result.excerptBefore)
        if (result.excerptMatch.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(result.excerptMatch)
            }
        }
        append(result.excerptAfter)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val preferences = settings.epub
    // Typography knobs imply custom styles: touching them while publisher
    // styles are active switches the mode so the preview matches the choice.
    val applyCustomPreference: ((EpubPreferences) -> EpubPreferences) -> Unit = { transform ->
        val base =
            if (styleModeFor(preferences.publisherStyles) == StyleMode.PUBLISHER) {
                preferences.withStyleMode(StyleMode.CUSTOM)
            } else {
                preferences
            }
        onSettingsChange(settings.copy(epub = transform(base)))
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                "Reader Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text("Theme", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm)) {
                listOf(
                    ReaderTheme.LIGHT to "Light",
                    ReaderTheme.SEPIA to "Sepia",
                    ReaderTheme.DARK to "Dark"
                ).forEach { (theme, label) ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = {
                            onSettingsChange(settings.copy(theme = theme))
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
            }

            Text("Font", style = MaterialTheme.typography.labelLarge)
            FlowRow(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                READER_FONT_FAMILIES.forEach { (family, label) ->
                    FilterChip(
                        selected = preferences.fontFamily == family,
                        onClick = {
                            applyCustomPreference { it.copy(fontFamily = family) }
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
            }

            Text("Page margins", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            ) {
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.pageMargins ?: PAGE_MARGINS_DEFAULT
                        if (current > PAGE_MARGINS_MIN) {
                            prefs.copy(
                                pageMargins = snapPageMargins(current - PAGE_MARGINS_STEP)
                            )
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease page margins")
                }
                Text(
                    text = "%.2f".format(preferences.pageMargins ?: PAGE_MARGINS_DEFAULT),
                    modifier = Modifier
                        .padding(horizontal = Padding.compact)
                        .semantics {
                            contentDescription = "Page margin: ${"%.2f".format(preferences.pageMargins ?: PAGE_MARGINS_DEFAULT)}"
                        }
                )
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.pageMargins ?: PAGE_MARGINS_DEFAULT
                        if (current < PAGE_MARGINS_MAX) {
                            prefs.copy(
                                pageMargins = snapPageMargins(current + PAGE_MARGINS_STEP)
                            )
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase page margins")
                }
            }

            Text("Line height", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            ) {
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.lineHeight ?: 1.2
                        if (current > 1.0) {
                            prefs.copy(lineHeight = current - 0.2)
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease line height")
                }
                Text(
                    text = "%.1f".format(preferences.lineHeight ?: 1.2),
                    modifier = Modifier
                        .padding(horizontal = Padding.compact)
                        .semantics {
                            contentDescription = "Line height: ${"%.1f".format(preferences.lineHeight ?: 1.2)}"
                        }
                )
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.lineHeight ?: 1.2
                        if (current < 2.5) {
                            prefs.copy(lineHeight = current + 0.2)
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase line height")
                }
            }

            Text("Styles", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                listOf(
                    StyleMode.PUBLISHER to "Publisher",
                    StyleMode.CUSTOM to "Custom"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = styleModeFor(preferences.publisherStyles) == mode,
                        onClick = {
                            onSettingsChange(
                                settings.copy(epub = preferences.withStyleMode(mode))
                            )
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
            }
            Text(
                text = when (styleModeFor(preferences.publisherStyles)) {
                    StyleMode.PUBLISHER -> "The publication's own typography is used."
                    StyleMode.CUSTOM -> "Your font, line height, and margins are applied."
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferences.scroll ?: false,
                        role = Role.Switch,
                        onValueChange = { checked ->
                            onSettingsChange(settings.copy(epub = preferences.copy(scroll = checked)))
                        }
                    )
            ) {
                Text(
                    "Scroll mode",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = preferences.scroll ?: false,
                    onCheckedChange = null
                )
            }
            Text(
                text = if (preferences.scroll == true) {
                    "Content scrolls continuously. Swipe up or down to turn; page-turn taps are inactive."
                } else {
                    "Content is paginated and tap zones can turn pages."
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            )

            Text("Tap zones", style = MaterialTheme.typography.labelLarge)
            TapZoneActionRow(
                label = "Left zone",
                selected = settings.tapZoneConfig.leftZone,
                onSelect = { action ->
                    onSettingsChange(
                        settings.copy(
                            tapZoneConfig = settings.tapZoneConfig.copy(leftZone = action)
                        )
                    )
                }
            )
            TapZoneActionRow(
                label = "Center zone",
                selected = settings.tapZoneConfig.centerZone,
                onSelect = { action ->
                    onSettingsChange(
                        settings.copy(
                            tapZoneConfig = settings.tapZoneConfig.copy(centerZone = action)
                        )
                    )
                }
            )
            TapZoneActionRow(
                label = "Right zone",
                selected = settings.tapZoneConfig.rightZone,
                onSelect = { action ->
                    onSettingsChange(
                        settings.copy(
                            tapZoneConfig = settings.tapZoneConfig.copy(rightZone = action)
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Text("Page turn animation", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm)) {
                listOf(
                    PageTurnAnimation.SLIDE to "Slide",
                    PageTurnAnimation.NONE to "None"
                ).forEach { (animation, label) ->
                    FilterChip(
                        selected = settings.pageTurnAnimation == animation,
                        onClick = { onSettingsChange(settings.copy(pageTurnAnimation = animation)) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.reduceMotion,
                        role = Role.Switch,
                        onValueChange = { reduce ->
                            onSettingsChange(settings.copy(reduceMotion = reduce))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reduce motion",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.reduceMotion,
                    onCheckedChange = null
                )
            }
            Text(
                "Disable page-turn animations and sheet transitions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

/** Offered actions for a configurable tap zone in the settings sheet. */
internal val TAP_ZONE_ACTION_OPTIONS: List<Pair<TapZoneAction, String>> = listOf(
    TapZoneAction.PREVIOUS_PAGE to "Previous page",
    TapZoneAction.NEXT_PAGE to "Next page",
    TapZoneAction.TOGGLE_MENU to "Toggle menu",
    TapZoneAction.NONE to "None"
)

@Composable
internal fun TapZoneActionRow(
    label: String,
    selected: TapZoneAction,
    onSelect: (TapZoneAction) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        FlowRow(modifier = Modifier.padding(top = 4.dp)) {
            TAP_ZONE_ACTION_OPTIONS.forEach { (action, optionLabel) ->
                FilterChip(
                    selected = selected == action,
                    onClick = { onSelect(action) },
                    label = { Text(optionLabel) },
                    modifier = Modifier
                        .padding(end = Spacing.xs)
                        .semantics { contentDescription = "$label: $optionLabel" }
                )
            }
        }
    }
}

@Composable
internal fun TocItem(link: Link, depth: Int, onClick: () -> Unit) {
    val indent = (depth * 16).dp
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClickLabel = "Open section", onClick = onClick)
    ) {
        Text(
            text = link.title ?: link.href.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = indent + 16.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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

@Composable
internal fun MetadataRow(label: String, value: String) {
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
            modifier = Modifier.width(100.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

internal fun formatEpochMillis(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.US)
    return sdf.format(java.util.Date(millis))
}
