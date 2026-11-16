package com.jdluu.leafline

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.util.Log
import com.jdluu.leafline.reader.ReaderOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import android.view.ActionMode
import android.view.KeyEvent
import android.view.View
import android.view.MotionEvent
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.leafline.export.BookExportData
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.export.ui.NotesExportSheet
import com.jdluu.leafline.di.appContainer
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.library.data.BookmarkRepository
import com.jdluu.leafline.library.data.BookmarkToggleResult
import com.jdluu.leafline.library.data.LibraryRepository
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
import com.jdluu.leafline.reader.ReaderSettingsController
import com.jdluu.leafline.reader.theme.ReaderTheme
import com.jdluu.leafline.reader.theme.backgroundColor
import com.jdluu.leafline.reader.StyleMode
import com.jdluu.leafline.reader.TapZoneAction
import com.jdluu.leafline.reader.TapZoneConfig
import com.jdluu.leafline.reader.effectiveTapZoneAction
import com.jdluu.leafline.reader.snapPageMargins
import com.jdluu.leafline.reader.styleModeFor
import com.jdluu.leafline.reader.pageTurnIsAnimated
import com.jdluu.leafline.reader.search.BookSearchQuery
import com.jdluu.leafline.theme.DEFAULT_HIGHLIGHT_TINT
import com.jdluu.leafline.theme.HIGHLIGHT_TINTS
import com.jdluu.leafline.theme.HighlightTint
import com.jdluu.leafline.reader.search.BookSearchResult
import com.jdluu.leafline.reader.search.BookSearchState
import com.jdluu.leafline.reader.search.BookSearchStatus
import com.jdluu.leafline.reader.search.BookSearcher
import com.jdluu.leafline.reader.search.SearchHistoryStore
import com.jdluu.leafline.reader.search.SearchOptions
import org.readium.r2.shared.publication.services.search.SearchService
import com.jdluu.leafline.reader.search.SearchDecorationPlanner
import com.jdluu.leafline.reader.session.ReadingSessionRecorder
import com.jdluu.leafline.reader.tapZoneAt
import com.jdluu.leafline.reader.theme.toReadiumTheme
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import org.readium.navigator.media.common.MediaNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.VisualNavigator
import com.jdluu.leafline.reader.tts.ReaderTtsController
import com.jdluu.leafline.reader.tts.ReaderTtsState
import com.jdluu.leafline.reader.tts.TtsSleepOption
import com.jdluu.leafline.reader.tts.TtsSleepState
import com.jdluu.leafline.reader.tts.TtsAudioFocusManager
import com.jdluu.leafline.reader.tts.AudioBecomingNoisyReceiver
import com.jdluu.leafline.reader.tts.TtsMediaSessionManager
import com.jdluu.leafline.reader.tts.throttleLatest
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.HyperlinkNavigator
import com.jdluu.leafline.reader.footnotes.FootnoteContent
import com.jdluu.leafline.reader.footnotes.FootnoteDetector
import com.jdluu.leafline.reader.footnotes.FootnoteResolver
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import com.jdluu.leafline.reader.navigation.NavigationHistoryManager
import com.jdluu.leafline.reader.navigation.ReaderScrubberHelper
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import com.jdluu.leafline.reader.font.CustomFont
import com.jdluu.leafline.reader.font.CustomFontInjector
import com.jdluu.leafline.reader.font.FontStore
import org.readium.r2.navigator.preferences.FontFamily
import java.io.File
import java.io.IOException

/** UI state for the sync conflict bottom sheet. */
data class SyncConflictState(
    val localPercentage: Double?,
    val remotePercentage: Double?,
    val remoteDevice: String?,
    val remoteTimestamp: Long?,
    val onJump: () -> Unit
)

internal const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : FragmentActivity(), EpubNavigatorFragment.Listener {

    companion object {
        private const val TAG = "ReaderActivity"
        private const val NAVIGATOR_TAG = "EpubNavigatorFragment"
        private const val EXTRA_FILE_PATH = "extra_file_path"
        private const val SEARCH_DECORATION_GROUP = "leafline-search"
        private const val ANNOTATION_DECORATION_GROUP = "leafline-annotations"
        private const val ANNOTATION_DECORATION_PREFIX = "annotation-"
        private const val DEFAULT_ANNOTATION_TINT = 0x55E65100.toInt()
        private const val MENU_ITEM_HIGHLIGHT_ID = 1
        private const val MENU_ITEM_COPY_ID = 2
        private const val KEY_SYNC_DEVICE_ID = "sync_device_id"
        private const val PAGE_TURN_ANNOUNCE_DEBOUNCE_MS = 300L
        private const val TTS_DECORATION_GROUP = "leafline-tts"
        private const val TTS_UTTERANCE_DECORATION_ID = "tts-utterance"
        private val TTS_UTTERANCE_TINT = 0x55E65100.toInt()

        const val EXTRA_SHOW_TOOLBAR = "extra_show_toolbar"

        fun newIntent(context: Context, filePath: String, showToolbar: Boolean = false): Intent {
            return Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
                putExtra(EXTRA_SHOW_TOOLBAR, showToolbar)
            }
        }
    }

    private var navigator: EpubNavigatorFragment? = null
    private var publication: Publication? = null
    private val bookStableIdState = androidx.compose.runtime.mutableStateOf<String?>(null)
    private var bookStableId: String?
        get() = bookStableIdState.value
        set(value) { bookStableIdState.value = value }
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var annotationRepository: AnnotationRepository
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var readerPreferencesStore: ReaderPreferencesStore
    private lateinit var bookSearcher: BookSearcher
    private lateinit var searchHistoryStore: SearchHistoryStore
    private val searchDecorationPlanner = SearchDecorationPlanner()
    private lateinit var syncManager: com.jdluu.leafline.reader.sync.ReaderSyncManager
    private lateinit var annotationManager: com.jdluu.leafline.reader.annotations.AnnotationManager
    private lateinit var textSelectionManager: com.jdluu.leafline.reader.selection.TextSelectionManager
    private val navigationHistory = NavigationHistoryManager()
    private var jumpNoticeDismissJob: Job? = null
    private var ttsFactory: AndroidTtsNavigatorFactory? = null
    private var ttsNavigator: AndroidTtsNavigator? = null
    private var ttsJob: Job? = null
    private val ttsObserveJobs = mutableListOf<Job>()
    private lateinit var ttsController: ReaderTtsController
    private lateinit var ttsAudioFocusManager: TtsAudioFocusManager
    private val audioBecomingNoisyReceiver = AudioBecomingNoisyReceiver {
        if (this::ttsController.isInitialized) {
            ttsController.pause()
        }
    }
    private var ttsMediaSessionManager: TtsMediaSessionManager? = null

    /** Lazily wired once the navigator exists; see onCreate. */
    private val goToLocator: (org.readium.r2.shared.publication.Locator) -> Unit = { locator ->
        currentLocation.value?.let { from ->
            navigationHistory.recordJump(
                from = from,
                to = locator,
                description = "Bookmark / Highlight"
            )
        }
        navigator?.go(locator, false)
    }

    /** Locator JSON captured when the user selects text and taps Highlight, pending tint selection. */
    private var pendingHighlightLocator: String? = null

    /** Currently selected highlight tint shown in the picker sheet. */
    private var selectedHighlightTint: HighlightTint = DEFAULT_HIGHLIGHT_TINT
    private var currentBook: com.jdluu.leafline.library.LibraryBook? = null
    private var toolbarVisible = mutableStateOf(false)
    private var isDrawerOpen = mutableStateOf(false)
    private var settingsSheetVisible = mutableStateOf(false)
    private val activeFootnote = mutableStateOf<FootnoteContent?>(null)
    private var highlightTintSheetVisible = mutableStateOf(false)
    private var bookmarkSheetVisible = mutableStateOf(false)
    private var highlightsSheetVisible = mutableStateOf(false)
    private var exportSheetVisible = mutableStateOf(false)
    private var exportSheetData = mutableStateOf<BookExportData?>(null)
    private var searchSheetVisible = mutableStateOf(false)
    private var bookTitle = mutableStateOf("")
    private var bookAuthor = mutableStateOf<String?>(null)

    /** Sync conflict state for the bottom sheet. */
    private val syncConflictState = MutableStateFlow<SyncConflictState?>(null)
    private var tocLinks = mutableStateOf<List<Pair<Link, Int>>>(emptyList())
    private lateinit var settingsController: ReaderSettingsController
    private var bookmarks = mutableStateOf<List<Bookmark>>(emptyList())
    private var annotations = mutableStateOf<List<Annotation>>(emptyList())
    private var currentLocation = mutableStateOf<Locator?>(null)
    private val isLoadingPublication = mutableStateOf(true)
    private var pageAnnouncement = mutableStateOf<String?>(null)
    @Volatile
    private var activeSearchResultId: Int? = null
    private val snackbarHostState = SnackbarHostState()
    private lateinit var tapZoneHandler: com.jdluu.leafline.reader.navigation.TapZoneHandler
    private lateinit var bookmarkManager: com.jdluu.leafline.reader.bookmarks.BookmarkManager
    private lateinit var sessionRecorder: ReadingSessionRecorder
    private var sessionRestored: Boolean = false
    private var lifetimeActiveDurationMillis: Long = 0L
    private lateinit var fontStore: FontStore
    private val customFontsState = mutableStateOf<List<CustomFont>>(emptyList())
    private val fontPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importFontFromUri(uri)
        }
    }

    @OptIn(ExperimentalReadiumApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        bookmarkRepository = appContainer.bookmarkRepository
        annotationRepository = appContainer.annotationRepository
        libraryRepository = appContainer.libraryRepository
        fontStore = FontStore(this)
        customFontsState.value = fontStore.getCustomFonts()
        readerPreferencesStore = ReaderPreferencesStore.fromContext(this)

        sessionRecorder = ReadingSessionRecorder(
            repository = appContainer.readingSessionRepository,
            scope = lifecycleScope
        )
        lifecycle.addObserver(sessionRecorder)
        sessionRestored = sessionRecorder.restoreState(savedInstanceState)

        if (intent?.getBooleanExtra(EXTRA_SHOW_TOOLBAR, false) == true) {
            toolbarVisible.value = true
        }
        if (intent?.getBooleanExtra("extra_show_highlights", false) == true) {
            toolbarVisible.value = true
            highlightsSheetVisible.value = true
        }
        if (intent?.getBooleanExtra("extra_show_bookmarks", false) == true) {
            toolbarVisible.value = true
            bookmarkSheetVisible.value = true
        }

        val intentBookId = intent?.getStringExtra("extra_book_id")
        if (intentBookId != null) {
            bookStableId = intentBookId
        }
        val importedPath = intent?.getStringExtra(EXTRA_FILE_PATH)
        var epubFile: File? = null

        if (!importedPath.isNullOrEmpty()) {
            val file = File(importedPath)
            try {
                val canonicalPath = file.canonicalPath
                val allowedPath = filesDir.canonicalPath
                if (canonicalPath.startsWith(allowedPath + File.separator)) {
                    epubFile = file
                }
            } catch (e: IOException) {
                Log.w(TAG, "Cannot resolve imported file canonical path", e)
            }
        }

        if (epubFile == null || !epubFile.exists()) {
            Log.e(TAG, "EPUB file does not exist: ${epubFile?.absolutePath}")
            Toast.makeText(this, "EPUB file not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(
            contentResolver = contentResolver,
            httpClient = httpClient
        )
        val publicationOpener = PublicationOpener(
            publicationParser = DefaultPublicationParser(
                this,
                httpClient = httpClient,
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )

        val animatorScale = android.provider.Settings.Global.getFloat(
            contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        val savedSettings = readerPreferencesStore.load(initialReduceMotion = animatorScale == 0f)
        settingsController = ReaderSettingsController(
            initialSettings = savedSettings,
            submitToNavigator = { navigator?.submitPreferences(it.toNavigatorPreferences()) },
            applyWindowBrightness = ::applyBrightnessToWindow,
            applyKeepScreenOn = ::applyKeepScreenOn,
            applyOrientation = ::applyOrientation,
            applyThemeBackground = { applyThemeBackground(it, animate = true) },
            save = readerPreferencesStore::save
        )
        applyBrightnessToWindow(savedSettings.brightness)
        applyKeepScreenOn(savedSettings.keepScreenOn)
        applyOrientation(savedSettings.orientation)
        applyThemeBackground(savedSettings.theme, animate = false)

        annotationManager = com.jdluu.leafline.reader.annotations.AnnotationManager(
            context = this,
            scope = lifecycleScope,
            repository = annotationRepository,
            navigatorProvider = { navigator },
            bookStableIdProvider = { bookStableId },
            annotationsProvider = { annotations.value },
            onPendingHighlightChanged = { pendingHighlightLocator = it },
            onShowHighlightTintSheet = { highlightTintSheetVisible.value = true },
            onSnackbar = { msg -> lifecycleScope.launch { snackbarHostState.showSnackbar(msg) } },
            onAnnotationNavigated = {
                highlightsSheetVisible.value = false
                toolbarVisible.value = false
            }
        )
        textSelectionManager = com.jdluu.leafline.reader.selection.TextSelectionManager(
            context = this,
            scope = lifecycleScope,
            navigatorProvider = { navigator },
            onHighlight = { annotationManager.saveSelectionAsHighlight() },
            onSnackbar = { msg -> lifecycleScope.launch { snackbarHostState.showSnackbar(msg) } },
            onSnackbarWithAction = { msg, actionLabel, onAction ->
                lifecycleScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = msg,
                        actionLabel = actionLabel,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onAction()
                    }
                }
            },
            bookTitleProvider = { currentBook?.title ?: publication?.metadata?.title }
        )
        bookmarkManager = com.jdluu.leafline.reader.bookmarks.BookmarkManager(
            scope = lifecycleScope,
            repository = bookmarkRepository,
            navigatorLocator = { navigator?.currentLocator?.value?.toJSON()?.toString() },
            navigatorProvider = { navigator },
            onBookmarkToggled = { msg -> lifecycleScope.launch { snackbarHostState.showSnackbar(msg) } },
            onSheetClosed = {
                bookmarkSheetVisible.value = false
                toolbarVisible.value = false
            }
        )
        syncManager = com.jdluu.leafline.reader.sync.ReaderSyncManager(
            context = this,
            scope = lifecycleScope,
            libraryRepository = libraryRepository,
            syncApi = appContainer.koreaderSyncApi,
            currentBookProvider = { currentBook },
            currentLocatorProvider = { currentLocation.value },
            goToLocator = goToLocator,
            onConflictState = { syncConflictState.value = it }
        )
        syncManager.initialize()

        setContentView(R.layout.activity_reader)
        addReaderOverlay()

        val targetFile = epubFile!!
        lifecycleScope.launch {
            try {
                val (loadedBook, pub) = withContext(Dispatchers.IO) {
                    val lookupPath = targetFile.canonicalPath
                    val book = libraryRepository.getBookByFilePath(lookupPath)
                        ?: libraryRepository.getBookByFilePath(targetFile.absolutePath)
                    val asset = assetRetriever.retrieve(targetFile).getOrElse {
                        throw RuntimeException("Failed to retrieve asset: $it")
                    }
                    val openedPub = publicationOpener.open(asset, allowUserInteraction = true).getOrElse {
                        throw RuntimeException("Failed to open publication: $it")
                    }
                    book to openedPub
                }

                currentBook = loadedBook
                val stableId = loadedBook?.stableId ?: intentBookId
                bookStableId = stableId
                val savedLocatorJson = loadedBook?.lastLocatorJson
                loadedBook?.let { book ->
                    if (book.readingStatus == ReadingStatus.UNREAD) {
                        launch { libraryRepository.setReadingStatus(book.stableId, ReadingStatus.READING) }
                    }
                    launch {
                        val agg = appContainer.readingSessionRepository.getAggregateForBook(book.stableId)
                        lifetimeActiveDurationMillis = agg.activeDurationMillis
                    }
                }

                val initialLocator: Locator? = savedLocatorJson?.let { json ->
                    try {
                        Locator.Companion.fromJSON(org.json.JSONObject(json))
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not parse saved locator, starting at beginning", e)
                        null
                    }
                }

                bookTitle.value = pub.metadata.title ?: loadedBook?.title ?: "Reading"
                bookAuthor.value = pub.metadata.authors.firstOrNull()?.name ?: loadedBook?.authors?.firstOrNull()
                tocLinks.value = flattenToc(pub.tableOfContents)
                this@ReaderActivity.publication = pub
                val ttsFactory = runCatching {
                    AndroidTtsNavigatorFactory(application, pub)
                }.getOrNull()
                this@ReaderActivity.ttsFactory = ttsFactory
                ttsController = ReaderTtsController(
                    initialState = if (ttsFactory != null) ReaderTtsState.IDLE else ReaderTtsState.UNAVAILABLE,
                    scope = lifecycleScope,
                    onPlay = { handleTtsPlay() },
                    onPause = { handleTtsPause() },
                    onStop = { handleTtsStop() },
                    onSetRate = { rate -> handleTtsRateChange(rate) },
                    onSetVolume = { volume -> handleTtsVolumeChange(volume) },
                    onSkipPrevious = { ttsNavigator?.skipToPreviousUtterance() },
                    onSkipNext = { ttsNavigator?.skipToNextUtterance() },
                    onError = { message ->
                        if (message != null) {
                            Toast.makeText(this@ReaderActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ttsAudioFocusManager = TtsAudioFocusManager.fromContext(
                    context = this@ReaderActivity,
                    onPausePlayback = { ttsController.pause() },
                    onResumePlayback = { ttsController.play() },
                    onDuckingVolume = { duckRatio -> handleTtsVolumeChange(duckRatio) },
                    onRestoreVolume = { handleTtsVolumeChange(1.0f) }
                )
                if (ttsFactory != null) {
                    val sessionMgr = TtsMediaSessionManager.create(
                        context = this@ReaderActivity,
                        onPlay = { ttsController.play() },
                        onPause = { ttsController.pause() },
                        onSkipNext = { ttsController.skipNext() },
                        onSkipPrevious = { ttsController.skipPrevious() },
                        onStop = { ttsController.stop() }
                    )
                    sessionMgr.updateMetadata(
                        bookTitle = bookTitle.value,
                        author = bookAuthor.value
                    )
                    ttsMediaSessionManager = sessionMgr

                    launch {
                        combine(ttsController.state, ttsController.rate) { state, rate ->
                            state to rate
                        }.collect { (state, rate) ->
                            ttsMediaSessionManager?.updatePlaybackState(state, rate)
                        }
                    }
                }
                searchHistoryStore = SearchHistoryStore.create(this@ReaderActivity)
                bookSearcher = BookSearcher(
                    scope = lifecycleScope,
                    searchFactory = { query, options ->
                        this@ReaderActivity.publication?.search(
                            query = query,
                            options = SearchService.Options(
                                caseSensitive = options.caseSensitive,
                                wholeWord = options.wholeWord,
                                regularExpression = options.regex
                            )
                        )
                    }
                )

                val fontDeclarations = customFontsState.value
                val navigatorFactory = EpubNavigatorFactory(pub)
                val fragmentFactory = navigatorFactory.createFragmentFactory(
                    initialLocator = initialLocator,
                    initialPreferences = settingsController.settings.value.toNavigatorPreferences(),
                    listener = this@ReaderActivity,
                    configuration = EpubNavigatorFragment.Configuration(
                        selectionActionModeCallback = textSelectionManager.selectionActionMode()
                    ).apply {
                        for (font in fontDeclarations) {
                            addFontFamilyDeclaration(FontFamily(font.name)) {}
                        }
                    }
                )

                supportFragmentManager.fragmentFactory = fragmentFactory

                supportFragmentManager.commitNow {
                    replace(R.id.navigator_container, EpubNavigatorFragment::class.java, null, NAVIGATOR_TAG)
                }
                navigator = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as EpubNavigatorFragment
                CustomFontInjector.inject(navigator, fontDeclarations)
                tapZoneHandler = com.jdluu.leafline.reader.navigation.TapZoneHandler(
                    context = this@ReaderActivity,
                    scope = lifecycleScope,
                    navigatorProvider = { navigator },
                    settingsProvider = { settingsController.settings.value },
                    onToggleMenu = { toolbarVisible.value = !toolbarVisible.value },
                    onAnnouncement = { announcement -> pageAnnouncement.value = announcement }
                )
                navigator?.addInputListener(tapZoneHandler.inputListener)
                launch {
                    navigator?.currentLocator?.collect { locator ->
                        CustomFontInjector.inject(navigator, customFontsState.value)
                        tapZoneHandler.onReadingPositionChanged(locator)
                    }
                }
                launch {
                    navigationHistory.jumpNotice.collect { notice ->
                        jumpNoticeDismissJob?.cancel()
                        if (notice != null) {
                            jumpNoticeDismissJob = launch {
                                delay(12_000L)
                                navigationHistory.dismissJumpNotice()
                            }
                        }
                    }
                }

                stableId?.let { id ->
                    if (!sessionRestored) {
                        val initialProg = initialLocator?.locations?.totalProgression
                            ?: initialLocator?.locations?.progression
                        sessionRecorder.startSession(
                            bookId = id,
                            initialLocatorJson = savedLocatorJson,
                            initialProgression = initialProg
                        )
                    }
                    launch {
                        navigator?.currentLocator?.collect { locator ->
                            val locatorJson = locator.toJSON().toString()
                            currentLocation.value = locator
                            val progression = locator.locations.totalProgression
                                ?: locator.locations.progression
                            sessionRecorder.updateLocation(locatorJson, progression)
                        }
                    }
                    launch {
                        @OptIn(FlowPreview::class)
                        navigator?.currentLocator
                            ?.debounce(500L)
                            ?.collect { locator ->
                                try {
                                    libraryRepository.saveLastLocator(
                                        id,
                                        locator.toJSON().toString(),
                                        System.currentTimeMillis()
                                    )
                                } catch (e: Exception) {
                                    Log.w(TAG, "Could not save reading position", e)
                                }
                            }
                    }
                    launch {
                        syncManager.pullRemoteProgress()
                    }
                    launch {
                        try {
                            bookmarkRepository.observeBookmarks(id).collect { stored ->
                                bookmarks.value = stored
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not observe bookmarks", e)
                        }
                    }
                    launch {
                        try {
                            annotationRepository.observeAnnotations(id).collect { stored ->
                                annotations.value = stored
                                annotationManager.applyDecorations()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not observe annotations", e)
                        }
                    }
                }

                navigator?.addDecorationListener(
                    ANNOTATION_DECORATION_GROUP,
                    object : DecorableNavigator.Listener {
                        override fun onDecorationActivated(
                            event: DecorableNavigator.OnActivatedEvent
                        ): Boolean {
                            val annotationId = event.decoration.id
                                .removePrefix(ANNOTATION_DECORATION_PREFIX)
                                .toLongOrNull() ?: return false
                            val target = annotations.value.firstOrNull { it.id == annotationId }
                                ?: return false
                            val message = target.note ?: annotationExcerpt(target)
                            Toast.makeText(this@ReaderActivity, message, Toast.LENGTH_SHORT).show()
                            return true
                        }
                    }
                )

                launch {
                    bookSearcher.state.collect { state ->
                        if (state.status is BookSearchStatus.Completed) {
                            applySearchDecorations()
                        }
                    }
                }

                isLoadingPublication.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open publication", e)
                Toast.makeText(this@ReaderActivity, "Failed to open EPUB: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun flattenToc(links: List<Link>, depth: Int = 0): List<Pair<Link, Int>> {
        val result = mutableListOf<Pair<Link, Int>>()
        for (link in links) {
            result.add(link to depth)
            if (link.children.isNotEmpty()) {
                result.addAll(flattenToc(link.children, depth + 1))
            }
        }
        return result
    }

    private fun navigateToTocLink(link: Link) {
        currentLocation.value?.let { from ->
            navigationHistory.recordJump(
                from = from,
                description = link.title ?: "Chapter"
            )
        }
        navigator?.go(link, false)
        toolbarVisible.value = false
    }

    /**
     * Builds the preferences handed to the Readium navigator, mapping the pure
     * Kotlin [com.jdluu.leafline.reader.theme.ReaderTheme] onto the Readium
     * theme at this Android boundary.
     */
    private fun ReaderSettings.toNavigatorPreferences(): EpubPreferences {
        val baseTheme = theme?.toReadiumTheme()
        return if (theme == com.jdluu.leafline.reader.theme.ReaderTheme.EINK) {
            epub.copy(
                theme = baseTheme,
                backgroundColor = org.readium.r2.navigator.preferences.Color(0xFFFFFFFF.toInt()),
                textColor = org.readium.r2.navigator.preferences.Color(0xFF000000.toInt())
            )
        } else {
            epub.copy(theme = baseTheme)
        }
    }

    /**
     * Applies a brightness override to this activity's window.
     * A null value restores the system default; the system-wide brightness
     * setting is never modified.
     */
    private fun applyBrightnessToWindow(brightness: Float?) {
        val attributes = window.attributes
        attributes.screenBrightness =
            brightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = attributes
    }

    override fun onPause() {
        super.onPause()
        val stableId = bookStableId
        val loc = currentLocation.value
        if (stableId != null && loc != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    libraryRepository.saveLastLocator(
                        stableId,
                        loc.toJSON().toString(),
                        System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not flush reading position on pause", e)
                }
            }
        }
    }

    private fun applyKeepScreenOn(keepOn: Boolean) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private var themeBgAnimator: android.animation.ValueAnimator? = null
    private var currentThemeBgColor: Int? = null

    private fun applyThemeBackground(theme: ReaderTheme?, animate: Boolean = true) {
        val isSystemDark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val targetColor = theme.backgroundColor(isSystemDark)
        val fromColor = currentThemeBgColor ?: targetColor
        currentThemeBgColor = targetColor

        themeBgAnimator?.cancel()
        val container = findViewById<View>(R.id.navigator_container)
        val decorView = window.decorView

        if (!animate || fromColor == targetColor) {
            decorView.setBackgroundColor(targetColor)
            container?.setBackgroundColor(targetColor)
            return
        }

        themeBgAnimator = android.animation.ValueAnimator.ofObject(
            android.animation.ArgbEvaluator(),
            fromColor,
            targetColor
        ).apply {
            duration = 250L
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                decorView.setBackgroundColor(color)
                container?.setBackgroundColor(color)
            }
            start()
        }
    }

    private fun applyOrientation(orientation: ReaderOrientation) {
        requestedOrientation = when (orientation) {
            ReaderOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ReaderOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ReaderOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::sessionRecorder.isInitialized) {
            sessionRecorder.saveState(outState)
        }
    }

    override fun onDestroy() {
        if (this::tapZoneHandler.isInitialized) {
            navigator?.removeInputListener(tapZoneHandler.inputListener)
        }
        if (this::sessionRecorder.isInitialized && isFinishing) {
            sessionRecorder.stopSession()
        }
        if (this::ttsController.isInitialized) {
            ttsController.stop()
        }
        audioBecomingNoisyReceiver.unregister(this)
        if (this::ttsAudioFocusManager.isInitialized) {
            ttsAudioFocusManager.abandonAudioFocus()
        }
        ttsNavigator?.asMedia3Player()?.volume = 1.0f
        ttsNavigator?.close()
        ttsNavigator = null
        ttsMediaSessionManager?.release()
        ttsMediaSessionManager = null
        navigationHistory.clear()
        jumpNoticeDismissJob?.cancel()
        if (isFinishing) {
            try {
                publication?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Could not close publication", e)
            }
        }
        themeBgAnimator?.cancel()
        themeBgAnimator = null
        super.onDestroy()
    }

    private fun submitSearch(rawQuery: String, options: SearchOptions = SearchOptions()) {
        activeSearchResultId = null
        clearSearchDecorations()
        bookSearcher.start(rawQuery, options)
    }

    private fun clearSearch() {
        activeSearchResultId = null
        clearSearchDecorations()
        bookSearcher.reset()
    }

    private fun onSearchResultClicked(result: BookSearchResult) {
        val locator = bookSearcher.locatorFor(result.id) ?: parseLocator(result.locatorJson)
        if (locator == null) {
            Toast.makeText(this, "Could not open this result", Toast.LENGTH_SHORT).show()
            return
        }
        currentLocation.value?.let { from ->
            navigationHistory.recordJump(
                from = from,
                to = locator,
                description = "Search result"
            )
        }
        bookSearcher.state.value.query?.let { searchHistoryStore.recordQuery(it) }
        activeSearchResultId = result.id
        applySearchDecorations()
        navigator?.go(locator, false)
        searchSheetVisible.value = false
        toolbarVisible.value = false
    }

    private fun parseLocator(json: String): Locator? {
        return try {
            Locator.fromJSON(org.json.JSONObject(json))
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse search result locator", e)
            null
        }
    }

    private fun applySearchDecorations() {
        val navigator = this.navigator ?: return
        if (!navigator.supportsDecorationStyle(Decoration.Style.Highlight::class)) return
        lifecycleScope.launch {
            try {
                val state = bookSearcher.state.value
                val decorations = searchDecorationPlanner
                    .planFor(state = state, activeResultId = activeSearchResultId)
                    .mapNotNull { spec ->
                        val locator = bookSearcher.locatorFor(spec.resultId) ?: return@mapNotNull null
                        Decoration(
                            id = "search-${spec.resultId}",
                            locator = locator,
                            style = Decoration.Style.Highlight(
                                tint = spec.tint,
                                isActive = spec.isActive
                            )
                        )
                    }
                navigator.applyDecorations(decorations, SEARCH_DECORATION_GROUP)
            } catch (e: Exception) {
                Log.w(TAG, "Could not apply search decorations", e)
            }
        }
    }

    private fun clearSearchDecorations() {
        val navigator = this.navigator ?: return
        if (!navigator.supportsDecorationStyle(Decoration.Style.Highlight::class)) return
        lifecycleScope.launch {
            try {
                navigator.applyDecorations(emptyList(), SEARCH_DECORATION_GROUP)
            } catch (e: Exception) {
                Log.w(TAG, "Could not clear search decorations", e)
            }
        }
    }

    private fun exportAnnotations(selected: List<Annotation>, title: String) {
        val book = currentBook ?: LibraryBook(
            stableId = bookStableId ?: "",
            title = title,
            authors = publication?.metadata?.authors?.mapNotNull { it.name } ?: emptyList()
        )
        exportSheetData.value = BookExportData(
            book = book,
            annotations = selected,
            collections = emptyList()
        )
        exportSheetVisible.value = true
    }

    private class ReaderOverlayTouchContainer(
        context: Context,
        private val isInteractive: () -> Boolean
    ) : android.widget.FrameLayout(context) {

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (!isInteractive()) {
                return false
            }
            return super.dispatchTouchEvent(ev)
        }
    }

    private fun importFontFromUri(androidUri: android.net.Uri) {
        val fileName = contentResolver.query(androidUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) cursor.getString(nameIndex) else null
            } else null
        }
        fontStore.importFont(androidUri, fileName).onSuccess { customFont ->
            customFontsState.value = fontStore.getCustomFonts()
            CustomFontInjector.inject(navigator, customFontsState.value)
            val current = settingsController.settings.value
            val base = if (styleModeFor(current.epub.publisherStyles) == StyleMode.PUBLISHER) {
                current.epub.withStyleMode(StyleMode.CUSTOM)
            } else {
                current.epub
            }
            settingsController.submit(current.copy(epub = base.copy(fontFamily = FontFamily(customFont.name))))
        }
    }

    private fun addReaderOverlay() {
        val touchContainer = ReaderOverlayTouchContainer(this) {
            isLoadingPublication.value ||
            toolbarVisible.value ||
            settingsSheetVisible.value ||
            bookmarkSheetVisible.value ||
            highlightsSheetVisible.value ||
            exportSheetVisible.value ||
            searchSheetVisible.value ||
            highlightTintSheetVisible.value ||
            syncConflictState.value != null ||
            isDrawerOpen.value
        }
        val composeView = ComposeView(this).apply {
            setContent {
                val isLoading by isLoadingPublication
                Crossfade(
                    targetState = !isLoading && this@ReaderActivity::bookSearcher.isInitialized,
                    animationSpec = tween(durationMillis = 300),
                    label = "reader_loading_crossfade"
                ) { isReady ->
                    if (!isReady) {
                        ReaderLoadingScreen(
                            title = bookTitle.value.takeUnless { it == "Reading" } ?: currentBook?.title,
                            onBack = {
                                finish()
                                overridePendingTransition(0, android.R.anim.fade_out)
                            }
                        )
                    } else {
                        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                LaunchedEffect(drawerState.isOpen) {
                    isDrawerOpen.value = drawerState.isOpen
                }
                val scope = rememberCoroutineScope()
                val searchState by bookSearcher.state.collectAsState()
                val searchHistory by searchHistoryStore.history.collectAsState()
                val currentLoc = currentLocation.value
                val currentChapterHref = currentLoc?.href?.toString()
                val currentChapterTitle = currentLoc?.title
                    ?: publication?.tableOfContents?.firstOrNull { it.href.toString() == currentChapterHref }?.title
                val syncConflict by syncConflictState.collectAsState()
                val currentSettings by settingsController.settings.collectAsState()
                val ttsState by ttsController.state.collectAsState()
                val ttsSleepState by ttsController.sleepState.collectAsStateWithLifecycle()
                val canNavigateHistoryBack by navigationHistory.canGoBack.collectAsState()
                val canNavigateHistoryForward by navigationHistory.canGoForward.collectAsState()
                val jumpNotice by navigationHistory.jumpNotice.collectAsState()
                val tocList = tocLinks.value
                val readingOrder = publication?.readingOrder.orEmpty()
                val (chapterItems, currentIndex) = remember(tocList, readingOrder, currentChapterHref, currentLocation.value) {
                    if (tocList.isNotEmpty()) {
                        val idx = if (currentChapterHref != null) {
                            tocList.indexOfFirst { (link, _) -> matchesChapter(link.href.toString(), currentChapterHref) }
                        } else -1
                        Pair(tocList.map { it.first }, idx)
                    } else {
                        val curHref = currentLocation.value?.href?.toString()
                        val idx = if (curHref != null) {
                            readingOrder.indexOfFirst { matchesChapter(it.href.toString(), curHref) }
                        } else -1
                        Pair(readingOrder, idx)
                    }
                }
                val canSkipPrev = ReaderScrubberHelper.canSkipPrevious(currentIndex)
                val canSkipNext = ReaderScrubberHelper.canSkipNext(currentIndex, chapterItems.size)
                ReaderOverlay(
                    canNavigateHistoryBack = canNavigateHistoryBack,
                    canNavigateHistoryForward = canNavigateHistoryForward,
                    jumpNotice = jumpNotice,
                    onNavigateHistoryBack = { navigateHistoryBack() },
                    onNavigateHistoryForward = { navigateHistoryForward() },
                    onDismissJumpNotice = { navigationHistory.dismissJumpNotice() },
                    title = bookTitle.value,
                    toolbarVisible = toolbarVisible.value,
                    onDismissToolbar = { toolbarVisible.value = false },
                    tocLinks = tocLinks.value,
                    currentSettings = currentSettings,
                    drawerState = drawerState,
                    settingsSheetVisible = settingsSheetVisible.value,
                    bookmarks = bookmarks.value,
                    bookmarkActionsEnabled = bookStableId != null,
                    currentLocatorJson = currentLocation.value?.toJSON()?.toString(),
                    bookmarkSheetVisible = bookmarkSheetVisible.value,
                    annotations = annotations.value,
                    highlightsSheetVisible = highlightsSheetVisible.value,
                    searchSheetVisible = searchSheetVisible.value,
                    searchState = searchState,
                    pageAnnouncement = pageAnnouncement.value,
                    snackbarHostState = snackbarHostState,
                    syncConflict = syncConflict,
                    onJumpToRemote = { syncConflict?.onJump?.invoke(); syncConflictState.value = null },
                    onDismissSyncConflict = { syncConflictState.value = null },
                    onBack = {
                    finish()
                    overridePendingTransition(0, android.R.anim.fade_out)
                },
                    onOpenToc = { scope.launch { drawerState.open() } },
                    onOpenSettings = { settingsSheetVisible.value = true },
                    onDismissSettings = { settingsSheetVisible.value = false },
                    customFonts = customFontsState.value,
                    onImportFont = {
                        fontPickerLauncher.launch(arrayOf("font/*", "application/*"))
                    },
                    onDeleteFont = { font ->
                        fontStore.deleteFont(font)
                        customFontsState.value = fontStore.getCustomFonts()
                        CustomFontInjector.inject(navigator, customFontsState.value)
                        val current = settingsController.settings.value
                        if (current.epub.fontFamily?.name == font.name) {
                            settingsController.submit(current.copy(epub = current.epub.copy(fontFamily = null)))
                        }
                    },
                    onToggleBookmark = { bookmarkManager.toggle(bookStableId) },
                    onOpenBookmarks = { bookmarkSheetVisible.value = true },
                    onDismissBookmarks = { bookmarkSheetVisible.value = false },
                    onBookmarkClick = { bookmarkManager.navigateTo(it) }, // TODO: delegate to bookmarkManager
                    onDeleteBookmark = { bookmarkManager.delete(it) },
                    onOpenHighlights = { highlightsSheetVisible.value = true },
                    onDismissHighlights = { highlightsSheetVisible.value = false },
                    onAnnotationClick = { annotationManager.navigateTo(it) },
                    onDeleteAnnotation = { annotationManager.delete(it) },
                    onRestoreAnnotation = { annotationManager.restore(it) },
                    onEditAnnotation = { annotation, note ->
                        annotationManager.updateNote(annotation, note)
                    },
                    onChangeColor = { annotation, tint ->
                        annotationManager.updateColor(annotation, tint.hex)
                    },
                    onExportAnnotations = { selected ->
                        exportAnnotations(selected, bookTitle.value)
                    },
                    onTocClick = { link ->
                        navigateToTocLink(link)
                        scope.launch { drawerState.close() }
                    },
                    onSettingsChange = { settings -> settingsController.submit(settings) },
                    onBrightnessChange = { value -> settingsController.submitBrightness(value) },
                    onBrightnessReset = { settingsController.submitBrightness(null) },
                    onToggleSepia = { settingsController.toggleSepia() },
                    onOpenSearch = { searchSheetVisible.value = true },
                    onDismissSearch = { searchSheetVisible.value = false },
                    currentChapterTitle = currentChapterTitle,
                    currentChapterHref = currentChapterHref,
                    recentSearches = searchHistory,
                    onSubmitSearch = { query, options -> submitSearch(query, options) },
                    onRemoveRecentSearch = { searchHistoryStore.removeQuery(it) },
                    onClearRecentSearches = { searchHistoryStore.clear() },
                    onClearSearch = { clearSearch() },
                    onSearchResultClick = { result -> onSearchResultClicked(result) },
                    highlightTintSheetVisible = highlightTintSheetVisible.value,
                    onHighlightTintSelected = { tint ->
                        highlightTintSheetVisible.value = false
                        annotationManager.saveHighlightWithTint(tint, pendingHighlightLocator)
                    },
                    ttsState = ttsState,
                    ttsRate = ttsController.rate.collectAsStateWithLifecycle().value,
                    ttsSleepState = ttsSleepState,
                    onTtsPlay = { ttsController.play() },
                    onTtsPause = { ttsController.pause() },
                    onTtsStop = { ttsController.stop() },
                    onTtsSkipPrevious = { ttsController.skipPrevious() },
                    onTtsSkipNext = { ttsController.skipNext() },
                    onTtsRateChange = { rate -> handleTtsRateChange(rate) },
                    onSetTtsSleepTimer = { option ->
                        ttsController.setSleepTimer(
                            option = option,
                            currentChapterHref = navigator?.currentLocator?.value?.href?.toString()
                        )
                    },
                    bookActiveDurationMillis = lifetimeActiveDurationMillis + (if (::sessionRecorder.isInitialized) sessionRecorder.currentActiveDurationMillis() else 0L),
                    activeFootnote = activeFootnote.value,
                    onDismissFootnote = { activeFootnote.value = null },
                    chapterTitles = remember(chapterItems) { chapterItems.map { it.title ?: "" } },
                    canSkipPreviousChapter = canSkipPrev,
                    canSkipNextChapter = canSkipNext,
                    onPreviousChapter = {
                        ReaderScrubberHelper.resolvePreviousChapterIndex(currentIndex)?.let {
                            navigateToTocLink(chapterItems[it])
                        }
                    },
                    onNextChapter = {
                        ReaderScrubberHelper.resolveNextChapterIndex(currentIndex, chapterItems.size)?.let {
                            navigateToTocLink(chapterItems[it])
                        }
                    },
                    onSeekToPercentage = { percent ->
                        val targetIndex = ReaderScrubberHelper.findChapterTarget(percent, chapterItems.size)
                        if (targetIndex != null && targetIndex in chapterItems.indices) {
                            navigateToTocLink(chapterItems[targetIndex])
                        }
                    },
                    onCycleMinimalHudMode = { settingsController.cycleMinimalHudMode() },
                    onJumpToFootnote = { locatorJson ->
                        try {
                            val locator = Locator.fromJSON(org.json.JSONObject(locatorJson))
                            if (locator != null) {
                                onJumpToLocator(locator)
                                navigator?.go(locator, animated = false)
                            }
                        } catch (_: Exception) {}
                    }
                )
                if (exportSheetVisible.value && exportSheetData.value != null) {
                    NotesExportSheet(
                        notesExporter = appContainer.notesExporter,
                        singleBookData = exportSheetData.value,
                        onDismiss = {
                            exportSheetVisible.value = false
                            exportSheetData.value = null
                        }
                    )
                }
                    }
                }
            }
        }
        val layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        touchContainer.addView(composeView, layoutParams)
        addContentView(touchContainer, layoutParams)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (this::settingsController.isInitialized &&
            this::tapZoneHandler.isInitialized &&
            settingsController.settings.value.epub.scroll != true
        ) {
            val settings = settingsController.settings.value
            val isTtsSpeaking = this::ttsController.isInitialized &&
                ttsController.state.value == ReaderTtsState.PLAYING

            val direction = com.jdluu.leafline.reader.navigation.HardwareKeyNavigator.resolveDirection(
                keyCode = keyCode,
                volumeKeyNavigation = settings.volumeKeyNavigation,
                invertVolumeKeys = settings.invertVolumeKeys,
                isTtsSpeaking = isTtsSpeaking
            )

            if (direction != null) {
                val animated = tapZoneHandler.pageTurnAnimated(settings)
                val handled = when (direction) {
                    com.jdluu.leafline.reader.navigation.PageNavigationDirection.FORWARD -> {
                        navigator?.goForward(animated) ?: false
                    }
                    com.jdluu.leafline.reader.navigation.PageNavigationDirection.BACKWARD -> {
                        navigator?.goBackward(animated) ?: false
                    }
                }
                if (handled && settings.hapticFeedback) {
                    tapZoneHandler.performHapticFeedback()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        if (!url.isHttp) return
        val uri = Uri.parse(url.toString())
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app available to open link", Toast.LENGTH_LONG).show()
        }
    }

    override fun onJumpToLocator(locator: Locator) {
        val current = currentLocation.value ?: return
        navigationHistory.recordJump(
            from = current,
            to = locator,
            description = "Footnote / Link"
        )
    }

    override fun shouldFollowInternalLink(
        link: Link,
        context: HyperlinkNavigator.LinkContext?
    ): Boolean {
        if (!settingsController.settings.value.previewFootnotes) {
            return true
        }

        val targetUrl = link.url()
        val fragment = targetUrl.fragment
        if (!FootnoteDetector.isLikelyFootnoteLink(
                urlPath = targetUrl.path,
                fragment = fragment,
                rels = link.rels
            )
        ) {
            return true
        }

        val pub = publication ?: return true
        val targetLocator = pub.locatorFromLink(link)
        val targetLocatorJson = targetLocator?.toJSON()?.toString()

        lifecycleScope.launch {
            val footnote = FootnoteResolver(pub).resolve(link, targetLocatorJson)
            if (footnote != null && footnote.text.isNotBlank()) {
                activeFootnote.value = footnote
            } else {
                if (targetLocator != null) {
                    onJumpToLocator(targetLocator)
                    navigator?.go(targetLocator, animated = false)
                }
            }
        }

        return false
    }

    private fun navigateHistoryBack() {
        val current = currentLocation.value ?: return
        val target = navigationHistory.goBack(current) ?: return
        navigator?.go(target, false)
        pageAnnouncement.value = "Returned to previous location"
    }

    private fun navigateHistoryForward() {
        val current = currentLocation.value ?: return
        val target = navigationHistory.goForward(current) ?: return
        navigator?.go(target, false)
        pageAnnouncement.value = "Navigated forward"
    }

    private fun handleTtsPlay(): Boolean {
        if (this::ttsAudioFocusManager.isInitialized) {
            val granted = ttsAudioFocusManager.requestAudioFocus()
            if (!granted) {
                Log.w(TAG, "Audio focus request denied; cannot start TTS")
                ttsController.onEngineError("Audio focus unavailable")
                return false
            }
        }
        audioBecomingNoisyReceiver.register(this@ReaderActivity)

        val nav = ttsNavigator
        if (nav != null) {
            Log.d(TAG, "Resuming TTS playback")
            nav.play()
            return true
        }

        val factory = ttsFactory ?: return false
        Log.d(TAG, "Initializing TTS navigator from factory")

        ttsJob?.cancel()
        ttsJob = lifecycleScope.launch {
            try {
                val initialLocator = (navigator as? VisualNavigator)?.firstVisibleElementLocator()
                    ?: navigator?.currentLocator?.value
                Log.d(TAG, "Starting TTS from initial locator: $initialLocator")

                val listener = object : TtsNavigator.Listener {
                    override fun onStopRequested() {
                        Log.d(TAG, "TTS stop requested by listener")
                        ttsController.stop()
                    }
                }

                val result = factory.createNavigator(
                    listener = listener,
                    initialLocator = initialLocator
                )

                val newNavigator = result.getOrNull()
                if (newNavigator == null) {
                    val failure = result.failureOrNull()
                    Log.e(TAG, "Failed to create TTS navigator: $failure")
                    ttsController.onEngineError(failure?.message ?: "Failed to initialize TTS")
                    return@launch
                }

                ttsNavigator = newNavigator
                observeTtsNavigator(newNavigator)
                Log.d(TAG, "TTS navigator created; starting playback")
                newNavigator.play()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting TTS playback", e)
                ttsController.onEngineError(e.message ?: "Failed to start text-to-speech")
            }
        }
        return true
    }

    private fun handleTtsPause() {
        Log.d(TAG, "Pausing TTS playback")
        audioBecomingNoisyReceiver.unregister(this@ReaderActivity)
        if (this::ttsAudioFocusManager.isInitialized && !ttsAudioFocusManager.pausedDueToTransientLoss) {
            ttsAudioFocusManager.abandonAudioFocus()
        }
        ttsNavigator?.pause()
    }

    private fun handleTtsStop() {
        Log.d(TAG, "Stopping TTS playback")
        audioBecomingNoisyReceiver.unregister(this@ReaderActivity)
        if (this::ttsAudioFocusManager.isInitialized) {
            ttsAudioFocusManager.abandonAudioFocus()
        }
        ttsJob?.cancel()
        ttsJob = null
        ttsObserveJobs.forEach { it.cancel() }
        ttsObserveJobs.clear()
        lifecycleScope.launch {
            try {
                navigator?.applyDecorations(emptyList(), TTS_DECORATION_GROUP)
            } catch (e: Exception) {
                Log.w(TAG, "Could not clear TTS decorations", e)
            }
        }
        ttsNavigator?.close()
        ttsNavigator = null
    }

    private fun handleTtsVolumeChange(volume: Float) {
        ttsNavigator?.asMedia3Player()?.volume = volume
    }

    private fun handleTtsRateChange(rate: Float) {
        ttsController.setRate(rate)
        ttsNavigator?.submitPreferences(AndroidTtsPreferences(speed = rate.toDouble()))
    }

    private fun observeTtsNavigator(nav: AndroidTtsNavigator) {
        ttsObserveJobs.forEach { it.cancel() }
        ttsObserveJobs.clear()

        ttsObserveJobs += lifecycleScope.launch {
            nav.playback.collect { playback ->
                when (val state = playback.state) {
                    is MediaNavigator.State.Ended -> {
                        Log.d(TAG, "TTS playback ended")
                        ttsController.onPlaybackEnded()
                    }
                    is TtsNavigator.State.Failure -> {
                        val error = state.error
                        val engineError = (error as? TtsNavigator.Error.EngineError<*>)?.cause
                        Log.w(TAG, "TTS playback failure: $error")
                        if (engineError is AndroidTtsEngine.Error.LanguageMissingData) {
                            Log.i(TAG, "TTS missing voice data; requesting install")
                            AndroidTtsEngine.requestInstallVoice(this@ReaderActivity)
                        }
                        ttsController.onEngineError(error.message ?: engineError?.toString() ?: "TTS error")
                    }
                    is MediaNavigator.State.Failure -> {
                        Log.w(TAG, "TTS media failure")
                        ttsController.onEngineError("TTS playback failure")
                    }
                    is MediaNavigator.State.Ready -> {
                        if (playback.playWhenReady) {
                            ttsController.onExternalPlay()
                        } else {
                            ttsController.onExternalPause()
                        }
                    }
                    else -> {}
                }
            }
        }

        ttsObserveJobs += lifecycleScope.launch {
            nav.location
                .mapNotNull { it.utteranceLocator }
                .distinctUntilChanged()
                .collect { utteranceLocator ->
                    val decoration = Decoration(
                        id = TTS_UTTERANCE_DECORATION_ID,
                        locator = utteranceLocator,
                        style = Decoration.Style.Highlight(
                            tint = TTS_UTTERANCE_TINT
                        )
                    )
                    try {
                        this@ReaderActivity.navigator?.applyDecorations(
                            listOf(decoration),
                            TTS_DECORATION_GROUP
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not apply TTS utterance decoration", e)
                    }
                    ttsController.onLocatorChanged(utteranceLocator.href.toString())
                    val chapterTitle = tocLinks.value.find { it.first.href.toString() == utteranceLocator.href.toString() }?.first?.title
                        ?: publication?.metadata?.title
                    ttsMediaSessionManager?.updateMetadata(
                        bookTitle = bookTitle.value,
                        author = bookAuthor.value,
                        chapterTitle = chapterTitle
                    )
                }
        }

        ttsObserveJobs += lifecycleScope.launch {
            nav.location
                .throttleLatest(1.seconds)
                .mapNotNull { it.tokenLocator ?: it.utteranceLocator }
                .distinctUntilChanged()
                .collect { locator ->
                    this@ReaderActivity.navigator?.go(locator, animated = false)
                }
        }
    }

    override fun onStop() {
        super.onStop()
        if (this::sessionRecorder.isInitialized && isFinishing) {
            sessionRecorder.stopSession()
        }
        if (this::ttsController.isInitialized && isFinishing) {
            ttsController.stop()
        }
        if (this::syncManager.isInitialized) {
            syncManager.pushProgressOnExit { book, progress ->
                syncManager.showMarkFinishedDialog(book, progress)
            }
        }
    }
}
