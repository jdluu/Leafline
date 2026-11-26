package com.jdluu.leafline.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.reader.ReaderSettings
import com.jdluu.leafline.reader.font.CustomFont
import com.jdluu.leafline.reader.footnotes.FootnoteContent
import com.jdluu.leafline.reader.navigation.JumpNotice
import com.jdluu.leafline.reader.navigation.ReaderNavigationDrawerContent
import com.jdluu.leafline.reader.navigation.ReaderNavigationTab
import com.jdluu.leafline.reader.search.BookSearchResult
import com.jdluu.leafline.reader.search.BookSearchState
import com.jdluu.leafline.reader.search.SearchOptions
import com.jdluu.leafline.reader.theme.ReaderTheme
import com.jdluu.leafline.reader.theme.chromeTextColor
import com.jdluu.leafline.reader.tts.ReaderTtsState
import com.jdluu.leafline.reader.tts.TtsSleepOption
import com.jdluu.leafline.reader.tts.TtsSleepState
import com.jdluu.leafline.theme.DEFAULT_HIGHLIGHT_TINT
import com.jdluu.leafline.theme.HighlightTint
import com.jdluu.leafline.theme.Spacing
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Link

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
