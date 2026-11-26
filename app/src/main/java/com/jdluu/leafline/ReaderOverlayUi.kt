package com.jdluu.leafline

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.InputChip
import com.jdluu.leafline.reader.ReaderOrientation
import com.jdluu.leafline.reader.search.SearchOptions
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.jdluu.leafline.theme.HIGHLIGHT_TINTS
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilledTonalButton
import com.jdluu.leafline.reader.footnotes.FootnoteContent
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.ButtonDefaults
import com.jdluu.leafline.reader.navigation.JumpNotice
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
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.jdluu.leafline.reader.navigation.ReaderNavigationDrawerContent
import com.jdluu.leafline.reader.ReaderStylePreset
import com.jdluu.leafline.reader.navigation.ReaderNavigationTab
import com.jdluu.leafline.reader.navigation.filterVisibleTocLinks
import com.jdluu.leafline.reader.navigation.findAncestorKeysForIndex
import com.jdluu.leafline.reader.MinimalHudMode
import com.jdluu.leafline.reader.navigation.ReaderScrubberHelper
import com.jdluu.leafline.reader.navigation.tocLinkKey
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import com.jdluu.leafline.reader.tts.ReaderTtsState
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.RadioButton
import com.jdluu.leafline.reader.tts.TtsSleepOption
import com.jdluu.leafline.reader.tts.TtsSleepState
import com.jdluu.leafline.reader.tts.formatSleepTimerLabel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import kotlin.math.roundToInt
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import com.jdluu.leafline.reader.font.CustomFont
import org.readium.r2.navigator.preferences.FontFamily
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
import androidx.compose.ui.graphics.Color as ComposeColor
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
import androidx.compose.ui.text.style.TextAlign
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
import com.jdluu.leafline.reader.FONT_SIZE_DEFAULT
import com.jdluu.leafline.reader.FONT_SIZE_MAX
import com.jdluu.leafline.reader.FONT_SIZE_MIN
import com.jdluu.leafline.reader.FONT_SIZE_STEP
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
import com.jdluu.leafline.reader.TapZoneLayout
import com.jdluu.leafline.reader.clampBrightness
import com.jdluu.leafline.reader.effectiveTapZoneAction
import com.jdluu.leafline.reader.snapFontSize
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
import com.jdluu.leafline.reader.theme.chromeTextColor
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
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
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
    onDismissToolbar: () -> Unit = {},
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
    onSubmitSearch: (String, SearchOptions) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResultClick: (BookSearchResult) -> Unit,
    currentChapterTitle: String? = null,
    currentChapterHref: String? = null,
    recentSearches: List<String> = emptyList(),
    onRemoveRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    highlightTintSheetVisible: Boolean,
    onHighlightTintSelected: (HighlightTint) -> Unit,
    ttsState: ReaderTtsState = ReaderTtsState.UNAVAILABLE,
    ttsRate: Float = 1.0f,
    ttsSleepState: TtsSleepState = TtsSleepState(),
    onTtsPlay: () -> Unit = {},
    onTtsPause: () -> Unit = {},
    onTtsStop: () -> Unit = {},
    onTtsSkipPrevious: () -> Unit = {},
    onTtsSkipNext: () -> Unit = {},
    onTtsRateChange: (Float) -> Unit = {},
    onSetTtsSleepTimer: (TtsSleepOption) -> Unit = {},
    bookActiveDurationMillis: Long = 0L,
    canNavigateHistoryBack: Boolean = false,
    canNavigateHistoryForward: Boolean = false,
    jumpNotice: JumpNotice? = null,
    onNavigateHistoryBack: () -> Unit = {},
    onNavigateHistoryForward: () -> Unit = {},
    onDismissJumpNotice: () -> Unit = {},
    activeFootnote: FootnoteContent? = null,
    onDismissFootnote: () -> Unit = {},
    onJumpToFootnote: (String) -> Unit = {},
    chapterTitles: List<String> = emptyList(),
    canSkipPreviousChapter: Boolean = false,
    canSkipNextChapter: Boolean = false,
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onSeekToPercentage: ((Double) -> Unit)? = null,
    onCycleMinimalHudMode: () -> Unit = {},
    customFonts: List<CustomFont> = emptyList(),
    onImportFont: () -> Unit = {},
    onDeleteFont: (CustomFont) -> Unit = {}
) {
    BackHandler(enabled = !toolbarVisible && jumpNotice != null) {
        onNavigateHistoryBack()
    }
    BackHandler(enabled = activeFootnote != null) {
        onDismissFootnote()
    }
    BackHandler(enabled = toolbarVisible) {
        onDismissToolbar()
    }

    var sleepTimerSheetVisible by remember { mutableStateOf(false) }
    var selectedNavigationTab by remember { mutableStateOf(ReaderNavigationTab.CONTENTS) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(bookmarkSheetVisible) {
        if (bookmarkSheetVisible) {
            selectedNavigationTab = ReaderNavigationTab.BOOKMARKS
            drawerState.open()
            onDismissBookmarks()
        }
    }

    LaunchedEffect(highlightsSheetVisible) {
        if (highlightsSheetVisible) {
            selectedNavigationTab = ReaderNavigationTab.HIGHLIGHTS
            drawerState.open()
            onDismissHighlights()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                ReaderNavigationDrawerContent(
                    selectedTab = selectedNavigationTab,
                    onTabSelected = { selectedNavigationTab = it },
                    tocLinks = tocLinks,
                    currentChapterHref = currentChapterHref,
                    onTocClick = { link ->
                        scope.launch { drawerState.close() }
                        onTocClick(link)
                    },
                    bookmarks = bookmarks,
                    onBookmarkClick = { bookmark ->
                        scope.launch { drawerState.close() }
                        onBookmarkClick(bookmark)
                    },
                    onDeleteBookmark = onDeleteBookmark,
                    annotations = annotations,
                    onAnnotationClick = { annotation ->
                        scope.launch { drawerState.close() }
                        onAnnotationClick(annotation)
                    },
                    onDeleteAnnotation = onDeleteAnnotation,
                    onRestoreAnnotation = onRestoreAnnotation,
                    onEditAnnotation = onEditAnnotation,
                    onChangeColor = onChangeColor,
                    onExportAnnotations = onExportAnnotations
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = toolbarVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 150)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                // Tapping outside the toolbar dismisses it
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onDismissToolbar()
                        }
                )
            }

            AnimatedVisibility(
                visible = toolbarVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 150)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
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
                            onOpenToc = {
                                selectedNavigationTab = ReaderNavigationTab.CONTENTS
                                onOpenToc()
                            },
                            onOpenSearch = onOpenSearch,
                            onOpenSettings = onOpenSettings,
                            onToggleBookmark = onToggleBookmark,
                            onOpenBookmarks = {
                                selectedNavigationTab = ReaderNavigationTab.BOOKMARKS
                                scope.launch { drawerState.open() }
                            },
                            onOpenHighlights = {
                                selectedNavigationTab = ReaderNavigationTab.HIGHLIGHTS
                                scope.launch { drawerState.open() }
                            },
                            canNavigateHistoryBack = canNavigateHistoryBack,
                            canNavigateHistoryForward = canNavigateHistoryForward,
                            onNavigateHistoryBack = onNavigateHistoryBack,
                            onNavigateHistoryForward = onNavigateHistoryForward
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
                        ttsRate = ttsRate,
                        ttsSleepState = ttsSleepState,
                        onTtsPlay = onTtsPlay,
                        onTtsPause = onTtsPause,
                        onTtsStop = onTtsStop,
                        onTtsSkipPrevious = onTtsSkipPrevious,
                        onTtsSkipNext = onTtsSkipNext,
                        onTtsRateChange = onTtsRateChange,
                        onOpenSleepTimer = { sleepTimerSheetVisible = true }
                    )
                }
            }

            if (settingsSheetVisible) {
                ReaderSettingsSheet(
                    settings = currentSettings,
                    onSettingsChange = onSettingsChange,
                    onDismiss = onDismissSettings,
                    customFonts = customFonts,
                    onImportFont = onImportFont,
                    onDeleteFont = onDeleteFont
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
                    currentChapterTitle = currentChapterTitle,
                    currentChapterHref = currentChapterHref,
                    recentSearches = recentSearches,
                    onDismiss = onDismissSearch,
                    onSubmitSearch = onSubmitSearch,
                    onClearSearch = onClearSearch,
                    onResultClick = onSearchResultClick,
                    onRemoveRecentSearch = onRemoveRecentSearch,
                    onClearRecentSearches = onClearRecentSearches
                )
            }

            if (highlightTintSheetVisible) {
                HighlightTintPickerSheet(
                    onDismiss = { onHighlightTintSelected(DEFAULT_HIGHLIGHT_TINT) },
                    onTintSelected = onHighlightTintSelected
                )
            }

            if (sleepTimerSheetVisible) {
                TtsSleepTimerSheet(
                    currentState = ttsSleepState,
                    onSelectOption = onSetTtsSleepTimer,
                    onDismiss = { sleepTimerSheetVisible = false }
                )
            }

            syncConflict?.let { conflict ->
                SyncConflictSheet(
                    conflict = conflict,
                    onJump = onJumpToRemote,
                    onDismiss = onDismissSyncConflict
                )
            }

            if (activeFootnote != null) {
                FootnotePreviewSheet(
                    footnote = activeFootnote,
                    onDismiss = onDismissFootnote,
                    onJump = onJumpToFootnote
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

            AnimatedVisibility(
                visible = !toolbarVisible && currentSettings.showMinimalHud,
                enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                MinimalReadingFooter(
                    currentChapterTitle = currentChapterTitle,
                    currentLocatorJson = currentLocatorJson,
                    activeDurationMillis = bookActiveDurationMillis,
                    mode = currentSettings.minimalHudMode,
                    onCycleMode = onCycleMinimalHudMode,
                    pageTextColor = ComposeColor(currentSettings.theme.chromeTextColor()),
                    modifier = Modifier.padding(bottom = 6.dp, start = Spacing.md, end = Spacing.md)
                )
            }

            AnimatedVisibility(
                visible = toolbarVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 150)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                ReaderBottomBar(
                    currentLocatorJson = currentLocatorJson,
                    activeDurationMillis = bookActiveDurationMillis,
                    currentChapterTitle = currentChapterTitle,
                    chapterTitles = chapterTitles,
                    canSkipPreviousChapter = canSkipPreviousChapter,
                    canSkipNextChapter = canSkipNextChapter,
                    onPreviousChapter = onPreviousChapter,
                    onNextChapter = onNextChapter,
                    onSeekToPercentage = onSeekToPercentage,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                )
            }
            if (jumpNotice != null) {
                JumpNoticeBanner(
                    notice = jumpNotice,
                    toolbarVisible = toolbarVisible,
                    onReturn = onNavigateHistoryBack,
                    onDismiss = onDismissJumpNotice,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
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
    ttsRate: Float = 1.0f,
    ttsSleepState: TtsSleepState = TtsSleepState(),
    onTtsPlay: () -> Unit = {},
    onTtsPause: () -> Unit = {},
    onTtsStop: () -> Unit = {},
    onTtsSkipPrevious: () -> Unit = {},
    onTtsSkipNext: () -> Unit = {},
    onTtsRateChange: (Float) -> Unit = {},
    onOpenSleepTimer: () -> Unit = {}
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
                        selected = false,
                        onClick = onTtsSkipPrevious,
                        label = { Text("Prev") },
                        leadingIcon = {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Previous sentence"
                        }
                    )
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
                        onClick = onTtsSkipNext,
                        label = { Text("Next") },
                        leadingIcon = {
                            Icon(Icons.Default.SkipNext, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Next sentence"
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
                    val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    FilterChip(
                        selected = false,
                        onClick = {
                            val idx = speedOptions.indexOf(ttsRate).let { if (it == -1) 1 else it }
                            val next = speedOptions[(idx + 1) % speedOptions.size]
                            onTtsRateChange(next)
                        },
                        label = { Text("${ttsRate}x") },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Text-to-speech speed ${ttsRate}x"
                        }
                    )
                    val timerLabel = formatSleepTimerLabel(ttsSleepState)
                    FilterChip(
                        selected = ttsSleepState.isActive,
                        onClick = onOpenSleepTimer,
                        label = { Text(timerLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (ttsSleepState.isActive) {
                                "Sleep timer active: $timerLabel. Tap to adjust"
                            } else {
                                "Set sleep timer"
                            }
                        }
                    )
                }
                ReaderTtsState.PAUSED -> {
                    FilterChip(
                        selected = false,
                        onClick = onTtsSkipPrevious,
                        label = { Text("Prev") },
                        leadingIcon = {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Previous sentence"
                        }
                    )
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
                        onClick = onTtsSkipNext,
                        label = { Text("Next") },
                        leadingIcon = {
                            Icon(Icons.Default.SkipNext, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Next sentence"
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
                    val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    FilterChip(
                        selected = false,
                        onClick = {
                            val idx = speedOptions.indexOf(ttsRate).let { if (it == -1) 1 else it }
                            val next = speedOptions[(idx + 1) % speedOptions.size]
                            onTtsRateChange(next)
                        },
                        label = { Text("${ttsRate}x") },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Text-to-speech speed ${ttsRate}x"
                        }
                    )
                    val timerLabel = formatSleepTimerLabel(ttsSleepState)
                    FilterChip(
                        selected = ttsSleepState.isActive,
                        onClick = onOpenSleepTimer,
                        label = { Text(timerLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (ttsSleepState.isActive) {
                                "Sleep timer active: $timerLabel. Tap to adjust"
                            } else {
                                "Set sleep timer"
                            }
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
                    val timerLabel = formatSleepTimerLabel(ttsSleepState)
                    FilterChip(
                        selected = ttsSleepState.isActive,
                        onClick = onOpenSleepTimer,
                        label = { Text(timerLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (ttsSleepState.isActive) {
                                "Sleep timer active: $timerLabel. Tap to adjust"
                            } else {
                                "Set sleep timer"
                            }
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
    onOpenHighlights: () -> Unit,
    canNavigateHistoryBack: Boolean = false,
    canNavigateHistoryForward: Boolean = false,
    onNavigateHistoryBack: () -> Unit = {},
    onNavigateHistoryForward: () -> Unit = {}
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (canNavigateHistoryBack) {
                IconButton(onClick = onNavigateHistoryBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Back to previous location"
                    )
                }
            }
            if (canNavigateHistoryForward) {
                IconButton(onClick = onNavigateHistoryForward) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Forward to next location"
                    )
                }
            }
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search highlights or notes") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Padding.screen, vertical = Spacing.xs),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit,
    customFonts: List<CustomFont> = emptyList(),
    onImportFont: () -> Unit = {},
    onDeleteFont: (CustomFont) -> Unit = {}
) {
    val preferences = settings.epub
    var showFontManagerDialog by remember { mutableStateOf(false) }
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
                    ReaderTheme.DARK to "Dark",
                    ReaderTheme.EINK to "E-Ink"
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

            Text("Presets", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = Spacing.xs, bottom = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                val activePreset = ReaderStylePreset.matchingPreset(preferences)
                ReaderStylePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = activePreset == preset,
                        onClick = {
                            applyCustomPreference { current ->
                                preset.applyTo(current)
                            }
                        },
                        label = { Text(preset.label) }
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
                customFonts.forEach { customFont ->
                    val fontFam = FontFamily(customFont.name)
                    FilterChip(
                        selected = preferences.fontFamily == fontFam,
                        onClick = {
                            applyCustomPreference { it.copy(fontFamily = fontFam) }
                        },
                        label = { Text(customFont.name) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
                AssistChip(
                    onClick = onImportFont,
                    label = { Text("Import font…") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon inside chip
                        )
                    },
                    modifier = Modifier.padding(end = Spacing.xs)
                )
            }

            if (customFonts.isNotEmpty()) {
                TextButton(
                    onClick = { showFontManagerDialog = true },
                    modifier = Modifier.padding(bottom = Spacing.xs)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon inside text button
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Manage imported fonts (${customFonts.size})")
                }
            }

            if (showFontManagerDialog) {
                AlertDialog(
                    onDismissRequest = { showFontManagerDialog = false },
                    title = { Text("Imported Fonts") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            customFonts.forEach { font ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(font.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(font.fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = {
                                            onDeleteFont(font)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete ${font.name}")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFontManagerDialog = false }) {
                            Text("Done")
                        }
                    }
                )
            }

            Text("Text size", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            ) {
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.fontSize ?: FONT_SIZE_DEFAULT
                        if (current > FONT_SIZE_MIN) {
                            prefs.copy(fontSize = snapFontSize(current - FONT_SIZE_STEP))
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease text size")
                }
                val fontSizeValue = preferences.fontSize ?: FONT_SIZE_DEFAULT
                Text(
                    text = "${(fontSizeValue * 100).toInt()}%",
                    modifier = Modifier
                        .padding(horizontal = Padding.compact)
                        .semantics {
                            contentDescription = "Text size: ${(fontSizeValue * 100).toInt()} percent"
                        }
                )
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.fontSize ?: FONT_SIZE_DEFAULT
                        if (current < FONT_SIZE_MAX) {
                            prefs.copy(fontSize = snapFontSize(current + FONT_SIZE_STEP))
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase text size")
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

            Text("Columns", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs, bottom = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                val currentColumnCount = preferences.columnCount ?: ColumnCount.AUTO
                listOf(
                    ColumnCount.AUTO to "Auto",
                    ColumnCount.ONE to "1 Column",
                    ColumnCount.TWO to "2 Columns"
                ).forEach { (count, label) ->
                    FilterChip(
                        selected = currentColumnCount == count,
                        onClick = {
                            applyCustomPreference { it.copy(columnCount = count) }
                        },
                        label = { Text(label) }
                    )
                }
            }
            Text(
                text = when (preferences.columnCount ?: ColumnCount.AUTO) {
                    ColumnCount.AUTO -> "Single column in portrait, dual columns on wide screens or landscape."
                    ColumnCount.ONE -> "Single column layout across all orientations."
                    ColumnCount.TWO -> "Two-column side-by-side spread."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferences.hyphens ?: true,
                        role = Role.Switch,
                        onValueChange = { checked ->
                            applyCustomPreference { it.copy(hyphens = checked) }
                        }
                    )
            ) {
                Text(
                    "Hyphenation",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = preferences.hyphens ?: true,
                    onCheckedChange = null
                )
            }
            Text(
                text = if (preferences.hyphens != false) {
                    "Words are hyphenated across line breaks to balance text justification."
                } else {
                    "Words are kept intact at line ends without hyphen breaks."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            )

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
            Text(
                "Zone layout",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            FlowRow(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                TapZoneLayout.values().forEach { layout ->
                    FilterChip(
                        selected = settings.tapZoneConfig.layout == layout,
                        onClick = {
                            onSettingsChange(
                                settings.copy(
                                    tapZoneConfig = settings.tapZoneConfig.copy(layout = layout)
                                )
                            )
                        },
                        label = { Text(layout.label) },
                        modifier = Modifier
                            .padding(end = Spacing.xs)
                            .semantics { contentDescription = "Zone layout: ${layout.description}" }
                    )
                }
            }
            Text(
                settings.tapZoneConfig.layout.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs)
            )

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

            Text("Screen orientation", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm)) {
                listOf(
                    ReaderOrientation.AUTO to "Auto",
                    ReaderOrientation.PORTRAIT to "Portrait",
                    ReaderOrientation.LANDSCAPE to "Landscape"
                ).forEach { (orient, label) ->
                    FilterChip(
                        selected = settings.orientation == orient,
                        onClick = { onSettingsChange(settings.copy(orientation = orient)) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))
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

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.previewFootnotes,
                        role = Role.Switch,
                        onValueChange = { preview ->
                            onSettingsChange(settings.copy(previewFootnotes = preview))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Preview footnotes & notes",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.previewFootnotes,
                    onCheckedChange = null
                )
            }
            Text(
                "Display a popup preview when tapping footnotes instead of jumping to the end of the chapter",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .toggleable(
                        value = settings.keepScreenOn,
                        role = Role.Switch,
                        onValueChange = { keepOn ->
                            onSettingsChange(settings.copy(keepScreenOn = keepOn))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Keep screen on",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = null
                )
            }
            Text(
                "Prevent screen from timing out while reading",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .toggleable(
                        value = settings.showMinimalHud,
                        role = Role.Switch,
                        onValueChange = { showHud ->
                            onSettingsChange(settings.copy(showMinimalHud = showHud))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Minimal reading HUD",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.showMinimalHud,
                    onCheckedChange = null
                )
            }
            Text(
                "Show chapter and progress subtly when menus are hidden",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .toggleable(
                        value = settings.hapticFeedback,
                        role = Role.Switch,
                        onValueChange = { haptic ->
                            onSettingsChange(settings.copy(hapticFeedback = haptic))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Haptic feedback",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.hapticFeedback,
                    onCheckedChange = null
                )
            }
            Text(
                "Subtle vibration when turning pages via edge taps or volume keys",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .toggleable(
                        value = settings.volumeKeyNavigation,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            onSettingsChange(settings.copy(volumeKeyNavigation = enabled))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Volume key navigation",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.volumeKeyNavigation,
                    onCheckedChange = null
                )
            }
            Text(
                "Turn pages using physical volume buttons when not scrolling",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (settings.volumeKeyNavigation) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .toggleable(
                            value = settings.invertVolumeKeys,
                            role = Role.Switch,
                            onValueChange = { inverted ->
                                onSettingsChange(settings.copy(invertVolumeKeys = inverted))
                            }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Invert volume buttons",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Switch(
                        checked = settings.invertVolumeKeys,
                        onCheckedChange = null
                    )
                }
                Text(
                    "Volume Up turns forward, Volume Down turns back",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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

internal fun matchesChapter(linkHref: String, currentHref: String?): Boolean {
    if (currentHref == null) return false
    val cleanLink = linkHref.substringBefore("#").trimStart('/')
    val cleanCurrent = currentHref.substringBefore("#").trimStart('/')
    if (cleanLink.isEmpty() || cleanCurrent.isEmpty()) return false
    return cleanLink == cleanCurrent || cleanCurrent.endsWith(cleanLink) || cleanLink.endsWith(cleanCurrent)
}

@Composable
internal fun TocItem(
    link: Link,
    depth: Int,
    isCurrent: Boolean = false,
    hasChildren: Boolean = false,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    onClick: () -> Unit
) {
    val indent = (depth * 16).dp
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(
                onClickLabel = if (isCurrent) "Current section" else "Open section",
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.small,
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = indent + 12.dp,
                    top = if (hasChildren) 4.dp else 12.dp,
                    bottom = if (hasChildren) 4.dp else 12.dp,
                    end = if (hasChildren) 4.dp else 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon in 48dp Surface
                )
            }
            Text(
                text = link.title ?: link.href.toString(),
                style = if (isCurrent) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        else MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (hasChildren) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse section" else "Expand section",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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

@Composable
internal fun ReaderBottomBar(
    currentLocatorJson: String?,
    activeDurationMillis: Long = 0L,
    currentChapterTitle: String? = null,
    chapterTitles: List<String> = emptyList(),
    canSkipPreviousChapter: Boolean = false,
    canSkipNextChapter: Boolean = false,
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onSeekToPercentage: ((Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val percent = remember(currentLocatorJson) {
        ReadingProgressMath.percentageFromLocator(currentLocatorJson)
    }
    val label = remember(currentLocatorJson) {
        ReadingProgressMath.progressLabel(currentLocatorJson)
    }
    val remaining = remember(percent, activeDurationMillis) {
        ReadingProgressMath.estimatedRemainingTimeLabel(percent, activeDurationMillis)
    }
    val displayText = remember(label, remaining, currentChapterTitle) {
        val chapter = currentChapterTitle?.trim()?.takeIf { it.isNotBlank() }
        val progressInfo = if (label == null) null
        else if (remaining != null) "$label • $remaining"
        else label
        when {
            chapter != null && progressInfo != null -> "$chapter • $progressInfo"
            progressInfo != null -> progressInfo
            else -> chapter
        }
    }

    var draggingPercent by remember { mutableStateOf<Double?>(null) }
    val activePercent = draggingPercent ?: percent ?: 0.0

    if (displayText != null || percent != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (draggingPercent != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = ReaderScrubberHelper.resolveScrubberPreview(activePercent, chapterTitles),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                        )
                    }
                } else if (displayText != null) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousChapter,
                        enabled = canSkipPreviousChapter,
                        modifier = Modifier.semantics {
                            contentDescription = "Previous chapter"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null
                        )
                    }

                    Slider(
                        value = activePercent.toFloat().coerceIn(0f, 100f),
                        onValueChange = { draggingPercent = it.toDouble() },
                        onValueChangeFinished = {
                            draggingPercent?.let { onSeekToPercentage?.invoke(it) }
                            draggingPercent = null
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "Reading progress: ${activePercent.toInt()}%"
                            }
                    )

                    IconButton(
                        onClick = onNextChapter,
                        enabled = canSkipNextChapter,
                        modifier = Modifier.semantics {
                            contentDescription = "Next chapter"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

internal fun formatEpochMillis(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.US)
    return sdf.format(java.util.Date(millis))
}


@Composable
internal fun JumpNoticeBanner(
    notice: JumpNotice,
    toolbarVisible: Boolean,
    onReturn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = modifier
            .padding(
                horizontal = 16.dp,
                vertical = if (toolbarVisible) 110.dp else 16.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to " + notice.sourceDescription,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onReturn,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.inversePrimary
                )
            ) {
                Text(
                    text = "Return",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss jump notice"
                )
            }
        }
    }
}


/**
 * Bottom sheet for configuring text-to-speech sleep timers (off, timed durations, or end of chapter).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TtsSleepTimerSheet(
    currentState: TtsSleepState,
    onSelectOption: (TtsSleepOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.screen)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sleep timer",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close sleep timer")
                }
            }

            Text(
                text = "Automatically pause reading aloud",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (currentState.isActive) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = when {
                                currentState.option == TtsSleepOption.END_OF_CHAPTER -> {
                                    "Pausing at the end of the chapter"
                                }
                                currentState.isFadingOut -> {
                                    "Gradually fading audio: ${formatSleepTimerLabel(currentState)}"
                                }
                                else -> {
                                    "Time remaining: ${formatSleepTimerLabel(currentState)}"
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            val options = listOf(
                TtsSleepOption.OFF to ("Off" to "Do not pause automatically"),
                TtsSleepOption.MINUTES_5 to ("5 minutes" to "Pause after 5 minutes of reading"),
                TtsSleepOption.MINUTES_10 to ("10 minutes" to "Pause after 10 minutes of reading"),
                TtsSleepOption.MINUTES_15 to ("15 minutes" to "Pause after 15 minutes of reading"),
                TtsSleepOption.MINUTES_30 to ("30 minutes" to "Pause after 30 minutes of reading"),
                TtsSleepOption.MINUTES_45 to ("45 minutes" to "Pause after 45 minutes of reading"),
                TtsSleepOption.MINUTES_60 to ("60 minutes" to "Pause after 60 minutes of reading"),
                TtsSleepOption.END_OF_CHAPTER to ("End of chapter" to "Pause when current chapter ends")
            )

            options.forEach { (option, labels) ->
                val (title, subtitle) = labels
                val isSelected = currentState.option == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable {
                            onSelectOption(option)
                            onDismiss()
                        }
                        .padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        modifier = Modifier.padding(end = Spacing.sm)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

/**
 * Non-disruptive bottom sheet preview for footnotes and endnotes, allowing
 * the user to inspect note content without leaving their reading position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FootnotePreviewSheet(
    footnote: FootnoteContent,
    onDismiss: () -> Unit,
    onJump: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.screen)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp) // touch-target-ok: non-interactive decorative icon
                    )
                    Text(
                        text = footnote.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss footnote preview"
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = footnote.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = Spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(end = Spacing.xs)
                ) {
                    Text("Close")
                }
                if (footnote.targetLocatorJson != null) {
                    FilledTonalButton(
                        onClick = {
                            val locJson = footnote.targetLocatorJson
                            onDismiss()
                            onJump(locJson)
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp) // touch-target-ok: non-interactive icon inside button
                                .padding(end = 4.dp)
                        )
                        Text("Jump to note")
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}

/**
 * An unobtrusive single-line footer rendered at the bottom edge while menus
 * are hidden. Displays reading progress according to the selected [MinimalHudMode].
 */
internal fun formatMinimalFooterText(
    chapterTitle: String?,
    percent: Int?,
    remainingTime: String? = null,
    mode: MinimalHudMode = MinimalHudMode.CHAPTER_AND_PERCENT
): String? {
    val chapter = chapterTitle?.trim()?.takeIf { it.isNotBlank() }
    val progressText = percent?.let { "$it%" }
    val remaining = remainingTime?.trim()?.takeIf { it.isNotBlank() }

    return when (mode) {
        MinimalHudMode.CHAPTER_AND_PERCENT -> when {
            chapter != null && progressText != null -> "$chapter • $progressText"
            chapter != null -> chapter
            progressText != null -> progressText
            else -> null
        }
        MinimalHudMode.PERCENT_ONLY -> progressText ?: chapter
        MinimalHudMode.TIME_REMAINING -> remaining ?: progressText ?: chapter
        MinimalHudMode.CHAPTER_ONLY -> chapter ?: progressText
    }
}

/**
 * Height reserved at the bottom of the reading view for the minimal footer: its 48dp
 * minimum touch target plus the 6dp bottom margin it is laid out with.
 */
internal const val READER_FOOTER_INSET_DP = 54

@Composable
internal fun MinimalReadingFooter(
    currentChapterTitle: String?,
    currentLocatorJson: String?,
    activeDurationMillis: Long = 0L,
    mode: MinimalHudMode = MinimalHudMode.CHAPTER_AND_PERCENT,
    onCycleMode: () -> Unit = {},
    pageTextColor: ComposeColor,
    modifier: Modifier = Modifier
) {
    val percent = remember(currentLocatorJson) {
        ReadingProgressMath.percentageFromLocator(currentLocatorJson)?.let {
            it.roundToInt()
        }
    }
    val remainingTime = remember(percent, activeDurationMillis) {
        percent?.let {
            ReadingProgressMath.estimatedRemainingTimeLabel(it.toDouble(), activeDurationMillis)
        }
    }
    val displayText = remember(currentChapterTitle, percent, remainingTime, mode) {
        formatMinimalFooterText(currentChapterTitle, percent, remainingTime, mode)
    }

    if (displayText != null) {
        Box(
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCycleMode
                )
                .semantics { contentDescription = "Reading status: $displayText. Tap to cycle format." },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelSmall,
                // The page's own ink, not the app scheme: the app can be dark while
                // the book page is white, where a 45% light gray disappears.
                color = pageTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Displayed while an EPUB archive is opened asynchronously in the background.
 * Provides a clean back button to allow exiting if the file is large.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderLoadingScreen(
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp) // touch-target-ok: non-interactive progress indicator
                )
                Text(
                    text = "Opening book…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
