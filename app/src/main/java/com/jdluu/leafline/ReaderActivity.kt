package com.jdluu.leafline

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.jdluu.leafline.di.appContainer
import com.jdluu.leafline.export.ui.NotesExportSheet
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.BookmarkRepository
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.reader.ReaderOrientation
import com.jdluu.leafline.reader.ReaderPreferencesStore
import com.jdluu.leafline.reader.ReaderSettings
import com.jdluu.leafline.reader.ReaderSettingsController
import com.jdluu.leafline.reader.ReaderViewModel
import com.jdluu.leafline.reader.ReaderViewModelFactory
import com.jdluu.leafline.reader.StyleMode
import com.jdluu.leafline.reader.annotations.AnnotationManager
import com.jdluu.leafline.reader.bookmarks.BookmarkManager
import com.jdluu.leafline.reader.font.CustomFontInjector
import com.jdluu.leafline.reader.font.FontStore
import com.jdluu.leafline.reader.footnotes.FootnoteDetector
import com.jdluu.leafline.reader.footnotes.FootnoteResolver
import com.jdluu.leafline.reader.navigation.HardwareKeyNavigator
import com.jdluu.leafline.reader.navigation.NavigationHistoryManager
import com.jdluu.leafline.reader.navigation.PageNavigationDirection
import com.jdluu.leafline.reader.navigation.ReaderScrubberHelper
import com.jdluu.leafline.reader.navigation.TapZoneHandler
import com.jdluu.leafline.reader.search.BookSearchResult
import com.jdluu.leafline.reader.search.BookSearchStatus
import com.jdluu.leafline.reader.search.BookSearcher
import com.jdluu.leafline.reader.search.SearchDecorationPlanner
import com.jdluu.leafline.reader.search.SearchHistoryStore
import com.jdluu.leafline.reader.search.SearchOptions
import com.jdluu.leafline.reader.selection.TextSelectionManager
import com.jdluu.leafline.reader.session.ReadingSessionRecorder
import com.jdluu.leafline.reader.styleModeFor
import com.jdluu.leafline.reader.sync.ReaderSyncManager
import com.jdluu.leafline.reader.theme.ReaderTheme
import com.jdluu.leafline.reader.theme.backgroundColor
import com.jdluu.leafline.reader.theme.toReadiumTheme
import com.jdluu.leafline.reader.tts.AudioBecomingNoisyReceiver
import com.jdluu.leafline.reader.tts.ReaderTtsController
import com.jdluu.leafline.reader.tts.ReaderTtsState
import com.jdluu.leafline.reader.tts.TtsAudioService
import com.jdluu.leafline.reader.tts.TtsMediaSessionManager
import com.jdluu.leafline.reader.tts.throttleLatest
import com.jdluu.leafline.reader.ui.READER_FOOTER_INSET_DP
import com.jdluu.leafline.reader.ui.ReaderLoadingScreen
import com.jdluu.leafline.reader.ui.ReaderOverlay
import com.jdluu.leafline.reader.ui.annotationExcerpt
import com.jdluu.leafline.reader.withStyleMode
import com.jdluu.leafline.theme.AppearanceStore
import com.jdluu.leafline.theme.DEFAULT_HIGHLIGHT_TINT
import com.jdluu.leafline.theme.HighlightTint
import com.jdluu.leafline.theme.LeaflineTheme
import com.jdluu.leafline.theme.leaflineTween
import com.jdluu.leafline.theme.readAnimatorDurationScale
import com.jdluu.leafline.theme.rememberReduceMotion
import com.jdluu.leafline.theme.resolveReduceMotion
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.navigator.media.common.MediaNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.HyperlinkNavigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

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

    private val viewModel: ReaderViewModel by lazy {
        ViewModelProvider(this, ReaderViewModelFactory())[ReaderViewModel::class.java]
    }
    private var navigator: EpubNavigatorFragment? = null
    private var publication: Publication? = null
    private val bookStableId: String?
        get() = viewModel.uiState.value.bookStableId
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var annotationRepository: AnnotationRepository
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var readerPreferencesStore: ReaderPreferencesStore
    private lateinit var bookSearcher: BookSearcher
    private lateinit var searchHistoryStore: SearchHistoryStore
    private val searchDecorationPlanner = SearchDecorationPlanner()
    private lateinit var syncManager: ReaderSyncManager
    private lateinit var annotationManager: AnnotationManager
    private lateinit var textSelectionManager: TextSelectionManager
    private val navigationHistory = NavigationHistoryManager()
    private var jumpNoticeDismissJob: Job? = null
    private var ttsFactory: AndroidTtsNavigatorFactory? = null
    private var ttsNavigator: AndroidTtsNavigator? = null
    private var ttsJob: Job? = null
    private val ttsObserveJobs = mutableListOf<Job>()
    private lateinit var ttsController: ReaderTtsController
    private val audioBecomingNoisyReceiver = AudioBecomingNoisyReceiver {
        if (this::ttsController.isInitialized) {
            ttsController.pause()
        }
    }
    private var ttsMediaSessionManager: TtsMediaSessionManager? = null

    /** Description handed to [onJumpToLocator] by the next programmatic navigation. */
    private var pendingJumpDescription: String? = null

    /** Href of a TTS-driven follow; jumps to it are not recorded in history. */
    private var suppressedJumpHref: String? = null

    /** Lazily wired once the navigator exists; see onCreate. */
    private val goToLocator: (Locator) -> Unit = { locator ->
        viewModel.uiState.value.currentLocation?.let { from ->
            navigationHistory.recordJump(
                from = from,
                to = locator,
                description = "Bookmark / Highlight"
            )
        }
        pendingJumpDescription = "Bookmark / Highlight"
        navigator?.go(locator, false)
    }

    /** Locator JSON captured when the user selects text and taps Highlight, pending tint selection. */
    private var pendingHighlightLocator: String? = null

    /** Currently selected highlight tint shown in the picker sheet. */
    private var selectedHighlightTint: HighlightTint = DEFAULT_HIGHLIGHT_TINT
    private var currentBook: LibraryBook? = null
    private lateinit var settingsController: ReaderSettingsController
    @Volatile
    private var activeSearchResultId: Int? = null
    private val snackbarHostState = SnackbarHostState()
    private lateinit var tapZoneHandler: TapZoneHandler
    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var sessionRecorder: ReadingSessionRecorder
    private var sessionRestored: Boolean = false
    private var lifetimeActiveDurationMillis: Long = 0L
    private lateinit var fontStore: FontStore
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
        viewModel.setCustomFonts(fontStore.getCustomFonts())
        readerPreferencesStore = ReaderPreferencesStore.fromContext(this)

        sessionRecorder = ReadingSessionRecorder(
            repository = appContainer.readingSessionRepository,
            scope = lifecycleScope
        )
        lifecycle.addObserver(sessionRecorder)
        sessionRestored = sessionRecorder.restoreState(savedInstanceState)

        val intentBookId = intent?.getStringExtra("extra_book_id")
        if (savedInstanceState == null) {
            viewModel.applyLaunchExtras(
                showToolbar = intent?.getBooleanExtra(EXTRA_SHOW_TOOLBAR, false) == true,
                showHighlights = intent?.getBooleanExtra("extra_show_highlights", false) == true,
                showBookmarks = intent?.getBooleanExtra("extra_show_bookmarks", false) == true,
                bookId = intentBookId
            )
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

        annotationManager = AnnotationManager(
            context = this,
            scope = lifecycleScope,
            repository = annotationRepository,
            navigatorProvider = { navigator },
            bookStableIdProvider = { bookStableId },
            annotationsProvider = { viewModel.uiState.value.annotations },
            onPendingHighlightChanged = { pendingHighlightLocator = it },
            onShowHighlightTintSheet = { viewModel.setHighlightTintSheetVisible(true) },
            onSnackbar = { msg -> lifecycleScope.launch { snackbarHostState.showSnackbar(msg) } },
            onAnnotationNavigated = { viewModel.onAnnotationNavigated() }
        )
        textSelectionManager = TextSelectionManager(
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
        bookmarkManager = BookmarkManager(
            scope = lifecycleScope,
            repository = bookmarkRepository,
            navigatorLocator = { navigator?.currentLocator?.value?.toJSON()?.toString() },
            navigatorProvider = { navigator },
            onBookmarkToggled = { msg -> lifecycleScope.launch { snackbarHostState.showSnackbar(msg) } },
            onSheetClosed = { viewModel.onBookmarkSheetClosed() }
        )
        syncManager = ReaderSyncManager(
            context = this,
            scope = lifecycleScope,
            libraryRepository = libraryRepository,
            syncApi = appContainer.koreaderSyncApi,
            currentBookProvider = { currentBook },
            currentLocatorProvider = { viewModel.uiState.value.currentLocation },
            goToLocator = goToLocator,
            onConflictState = { viewModel.setSyncConflict(it) },
            onSnackbar = { msg -> lifecycleScope.launch { snackbarHostState.showSnackbar(msg) } },
            onOpenNextBook = { nextBook ->
                startActivity(newIntent(this@ReaderActivity, nextBook.filePath))
                finish()
            }
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
                viewModel.setBookStableId(stableId)
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

                val loadedTitle = pub.metadata.title ?: loadedBook?.title ?: "Reading"
                val loadedAuthor = pub.metadata.authors.firstOrNull()?.name ?: loadedBook?.authors?.firstOrNull()
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
                            lifecycleScope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    }
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
                        bookTitle = loadedTitle,
                        author = loadedAuthor
                    )
                    ttsMediaSessionManager = sessionMgr

                    TtsAudioService.serviceListener = object : TtsAudioService.Listener {
                        override fun onPlay() { ttsController.play() }
                        override fun onPause() { ttsController.pause() }
                        override fun onSkipNext() { ttsController.skipNext() }
                        override fun onSkipPrevious() { ttsController.skipPrevious() }
                        override fun onStop() { ttsController.stop() }
                    }

                    launch {
                        combine(ttsController.state, ttsController.rate) { state, rate ->
                            state to rate
                        }.collect { (state, rate) ->
                            ttsMediaSessionManager?.updatePlaybackState(state, rate)
                            val stateSnapshot = viewModel.uiState.value
                            when (state) {
                                ReaderTtsState.PLAYING -> {
                                    TtsAudioService.startOrUpdate(
                                        context = this@ReaderActivity,
                                        bookTitle = stateSnapshot.bookTitle.ifEmpty { loadedTitle },
                                        author = stateSnapshot.bookAuthor ?: loadedAuthor,
                                        chapterTitle = stateSnapshot.currentChapterTitle,
                                        isPlaying = true,
                                        sessionToken = ttsMediaSessionManager?.sessionToken
                                    )
                                }
                                ReaderTtsState.PAUSED -> {
                                    TtsAudioService.startOrUpdate(
                                        context = this@ReaderActivity,
                                        bookTitle = stateSnapshot.bookTitle.ifEmpty { loadedTitle },
                                        author = stateSnapshot.bookAuthor ?: loadedAuthor,
                                        chapterTitle = stateSnapshot.currentChapterTitle,
                                        isPlaying = false,
                                        sessionToken = ttsMediaSessionManager?.sessionToken
                                    )
                                }
                                ReaderTtsState.IDLE,
                                ReaderTtsState.ERROR,
                                ReaderTtsState.UNAVAILABLE -> {
                                    TtsAudioService.stop(this@ReaderActivity)
                                }
                            }
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

                val fontDeclarations = viewModel.uiState.value.customFonts
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
                tapZoneHandler = TapZoneHandler(
                    context = this@ReaderActivity,
                    scope = lifecycleScope,
                    navigatorProvider = { navigator },
                    settingsProvider = { settingsController.settings.value },
                    onToggleMenu = { viewModel.toggleToolbar() },
                    onAnnouncement = { announcement -> viewModel.setPageAnnouncement(announcement) }
                )
                navigator?.addInputListener(tapZoneHandler.inputListener)
                if (stableId != null && !sessionRestored) {
                    val initialProg = initialLocator?.locations?.totalProgression
                        ?: initialLocator?.locations?.progression
                    sessionRecorder.startSession(
                        bookId = stableId,
                        initialLocatorJson = savedLocatorJson,
                        initialProgression = initialProg
                    )
                }
                navigator?.let { activeNavigator ->
                    var lastFontInjectedHref: String? = null
                    var saveLocatorJob: Job? = null
                    launch {
                        activeNavigator.currentLocator.collect { locator ->
                            val href = locator.href.toString()
                            if (href != lastFontInjectedHref) {
                                lastFontInjectedHref = href
                                CustomFontInjector.inject(activeNavigator, viewModel.uiState.value.customFonts)
                            }
                            tapZoneHandler.onReadingPositionChanged(locator)
                            val locatorJson = locator.toJSON().toString()
                            viewModel.updateLocation(locator, locatorJson)
                            if (stableId != null) {
                                val progression = locator.locations.totalProgression
                                    ?: locator.locations.progression
                                sessionRecorder.updateLocation(locatorJson, progression)
                                saveLocatorJob?.cancel()
                                saveLocatorJob = launch {
                                    delay(500L)
                                    try {
                                        libraryRepository.saveLastLocator(
                                            stableId,
                                            locatorJson,
                                            System.currentTimeMillis()
                                        )
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Could not save reading position", e)
                                    }
                                }
                            }
                        }
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
                    launch {
                        syncManager.pullRemoteProgress()
                    }
                    launch {
                        try {
                            bookmarkRepository.observeBookmarks(id).collect { stored ->
                                viewModel.setBookmarks(stored)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not observe bookmarks", e)
                        }
                    }
                    launch {
                        try {
                            annotationRepository.observeAnnotations(id).collect { stored ->
                                viewModel.setAnnotations(stored)
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
                            val target = viewModel.uiState.value.annotations.firstOrNull { it.id == annotationId }
                                ?: return false
                            val message = target.note ?: annotationExcerpt(target)
                            lifecycleScope.launch { snackbarHostState.showSnackbar(message) }
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

                viewModel.onPublicationLoaded(
                    bookStableId = stableId,
                    bookTitle = loadedTitle,
                    bookAuthor = loadedAuthor,
                    tableOfContents = pub.tableOfContents,
                    readingOrder = pub.readingOrder
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open publication", e)
                Toast.makeText(this@ReaderActivity, "Failed to open EPUB: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun navigateToTocLink(link: Link) {
        viewModel.uiState.value.currentLocation?.let { from ->
            navigationHistory.recordJump(
                from = from,
                description = link.title ?: "Chapter"
            )
        }
        pendingJumpDescription = link.title ?: "Chapter"
        navigator?.go(link, false)
        viewModel.setToolbarVisible(false)
    }

    /**
     * Builds the preferences handed to the Readium navigator, mapping the pure
     * Kotlin [com.jdluu.leafline.reader.theme.ReaderTheme] onto the Readium
     * theme at this Android boundary.
     */
    private fun ReaderSettings.toNavigatorPreferences(): EpubPreferences {
        val baseTheme = theme?.toReadiumTheme()
        return if (theme == ReaderTheme.EINK) {
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
        val state = viewModel.uiState.value
        val stableId = state.bookStableId
        val locJson = state.currentLocatorJson ?: state.currentLocation?.toJSON()?.toString()
        if (stableId != null && locJson != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    libraryRepository.saveLastLocator(
                        stableId,
                        locJson,
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
        // The page's own field, not the system side: an unset theme is Readium's
        // light default, and a dark surround behind it left chrome on a seam.
        val targetColor = theme.backgroundColor()
        val fromColor = currentThemeBgColor ?: targetColor
        currentThemeBgColor = targetColor

        themeBgAnimator?.cancel()
        val container = findViewById<View>(R.id.navigator_container)
        val decorView = window.decorView

        val reduceMotion = resolveReduceMotion(readAnimatorDurationScale(contentResolver))
        if (!animate || reduceMotion || fromColor == targetColor) {
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

    /**
     * Reserves space for the minimal reading footer so page text never renders underneath it.
     * The height is [READER_FOOTER_INSET_DP]: the footer's 48dp touch target plus its margin.
     */
    private fun applyReadingFooterInset(showFooter: Boolean) {
        val container = findViewById<View>(R.id.navigator_container) ?: return
        val bottom = if (showFooter) {
            (READER_FOOTER_INSET_DP * resources.displayMetrics.density).roundToInt()
        } else {
            0
        }
        if (container.paddingBottom != bottom) {
            container.setPadding(
                container.paddingLeft,
                container.paddingTop,
                container.paddingRight,
                bottom
            )
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
        ttsNavigator?.asMedia3Player()?.volume = 1.0f
        ttsNavigator?.close()
        ttsNavigator = null
        ttsMediaSessionManager?.release()
        ttsMediaSessionManager = null
        TtsAudioService.serviceListener = null
        TtsAudioService.stop(this)
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
            viewModel.onSearchResultNavigated()
            lifecycleScope.launch { snackbarHostState.showSnackbar("Could not open this result") }
            return
        }
        viewModel.uiState.value.currentLocation?.let { from ->
            navigationHistory.recordJump(
                from = from,
                to = locator,
                description = "Search result"
            )
        }
        bookSearcher.state.value.query?.let { searchHistoryStore.recordQuery(it) }
        activeSearchResultId = result.id
        applySearchDecorations()
        pendingJumpDescription = "Search result"
        navigator?.go(locator, false)
        viewModel.onSearchResultNavigated()
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
        viewModel.showExportSheet(
            selectedAnnotations = selected,
            currentBook = currentBook,
            fallbackAuthorNames = publication?.metadata?.authors?.mapNotNull { it.name } ?: emptyList(),
            title = title
        )
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

    private fun importFontFromUri(androidUri: Uri) {
        val fileName = contentResolver.query(androidUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) cursor.getString(nameIndex) else null
            } else null
        }
        fontStore.importFont(androidUri, fileName).onSuccess { customFont ->
            val updatedFonts = fontStore.getCustomFonts()
            viewModel.setCustomFonts(updatedFonts)
            CustomFontInjector.inject(navigator, updatedFonts)
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
            viewModel.uiState.value.isOverlayInteractive
        }
        val composeView = ComposeView(this).apply {
            // Reader chrome (drawer, sheets, dialogs) has to use the same Leafline scheme as
            // the rest of the app. Without this wrapper the overlays fell back to Material's
            // baseline palette: lilac drawer surfaces and purple tab and row accents.
            setContent {
                val appearanceMode = remember {
                    AppearanceStore.fromContext(this@ReaderActivity).load()
                }
                LeaflineTheme(
                    mode = appearanceMode,
                    darkSystem = isSystemInDarkTheme()
                ) {
                    val reduceMotion = rememberReduceMotion()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    Crossfade(
                        targetState = !uiState.isLoadingPublication && this@ReaderActivity::bookSearcher.isInitialized,
                        animationSpec = leaflineTween(300, reduceMotion),
                        label = "reader_loading_crossfade"
                    ) { isReady ->
                        if (!isReady) {
                            ReaderLoadingScreen(
                                title = uiState.resolveLoadingTitle(currentBook?.title),
                                onBack = {
                                    finish()
                                    overridePendingTransition(
                                        0,
                                        if (reduceMotion) 0 else android.R.anim.fade_out
                                    )
                                }
                            )
                        } else {
                            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                            LaunchedEffect(drawerState.isOpen) {
                                viewModel.setDrawerOpen(drawerState.isOpen)
                            }
                            val scope = rememberCoroutineScope()
                            val searchState by bookSearcher.state.collectAsState()
                            val searchHistory by searchHistoryStore.history.collectAsState()
                            val currentChapterHref = uiState.currentChapterHref
                            val resolvedChapterTitle = uiState.resolveCurrentChapterTitle()
                            val currentSettings by settingsController.settings.collectAsState()
                            // The minimal footer is drawn over the page, so reserve its height in the
                            // navigator; otherwise the last line of every page runs under the label.
                            LaunchedEffect(currentSettings.showMinimalHud) {
                                applyReadingFooterInset(currentSettings.showMinimalHud)
                            }
                            val ttsState by ttsController.state.collectAsState()
                            val ttsSleepState by ttsController.sleepState.collectAsStateWithLifecycle()
                            val canNavigateHistoryBack by navigationHistory.canGoBack.collectAsState()
                            val canNavigateHistoryForward by navigationHistory.canGoForward.collectAsState()
                            val jumpNotice by navigationHistory.jumpNotice.collectAsState()
                            val chapterNav = remember(uiState.tocLinks, uiState.readingOrder, currentChapterHref) {
                                uiState.resolveChapterNavigation()
                            }
                            ReaderOverlay(
                                canNavigateHistoryBack = canNavigateHistoryBack,
                                canNavigateHistoryForward = canNavigateHistoryForward,
                                jumpNotice = jumpNotice,
                                onNavigateHistoryBack = { navigateHistoryBack() },
                                onNavigateHistoryForward = { navigateHistoryForward() },
                                onDismissJumpNotice = { navigationHistory.dismissJumpNotice() },
                                title = uiState.bookTitle,
                                toolbarVisible = uiState.toolbarVisible,
                                onDismissToolbar = { viewModel.setToolbarVisible(false) },
                                tocLinks = uiState.tocLinks,
                                currentSettings = currentSettings,
                                drawerState = drawerState,
                                settingsSheetVisible = uiState.settingsSheetVisible,
                                bookmarks = uiState.bookmarks,
                                bookmarkActionsEnabled = uiState.bookStableId != null,
                                currentLocatorJson = uiState.currentLocatorJson,
                                bookmarkSheetVisible = uiState.bookmarkSheetVisible,
                                annotations = uiState.annotations,
                                highlightsSheetVisible = uiState.highlightsSheetVisible,
                                searchSheetVisible = uiState.searchSheetVisible,
                                searchState = searchState,
                                pageAnnouncement = uiState.pageAnnouncement,
                                snackbarHostState = snackbarHostState,
                                syncConflict = uiState.syncConflict,
                                onJumpToRemote = {
                                    uiState.syncConflict?.onJump?.invoke()
                                    viewModel.setSyncConflict(null)
                                },
                                onDismissSyncConflict = { viewModel.setSyncConflict(null) },
                                onBack = {
                                    finish()
                                    overridePendingTransition(
                                        0,
                                        if (reduceMotion) 0 else android.R.anim.fade_out
                                    )
                                },
                                onOpenToc = { scope.launch { drawerState.open() } },
                                onOpenSettings = { viewModel.setSettingsSheetVisible(true) },
                                onDismissSettings = { viewModel.setSettingsSheetVisible(false) },
                                customFonts = uiState.customFonts,
                                onImportFont = {
                                    fontPickerLauncher.launch(arrayOf("font/*", "application/*"))
                                },
                                onDeleteFont = { font ->
                                    fontStore.deleteFont(font)
                                    val updatedFonts = fontStore.getCustomFonts()
                                    viewModel.setCustomFonts(updatedFonts)
                                    CustomFontInjector.inject(navigator, updatedFonts)
                                    val current = settingsController.settings.value
                                    if (current.epub.fontFamily?.name == font.name) {
                                        settingsController.submit(current.copy(epub = current.epub.copy(fontFamily = null)))
                                    }
                                },
                                onToggleBookmark = { bookmarkManager.toggle(uiState.bookStableId, resolvedChapterTitle) },
                                onOpenBookmarks = { viewModel.setBookmarkSheetVisible(true) },
                                onDismissBookmarks = { viewModel.setBookmarkSheetVisible(false) },
                                onBookmarkClick = { bookmarkManager.navigateTo(it) },
                                onDeleteBookmark = { bookmarkManager.delete(it) },
                                onOpenHighlights = { viewModel.setHighlightsSheetVisible(true) },
                                onDismissHighlights = { viewModel.setHighlightsSheetVisible(false) },
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
                                    exportAnnotations(selected, uiState.bookTitle)
                                },
                                onTocClick = { link ->
                                    navigateToTocLink(link)
                                    scope.launch { drawerState.close() }
                                },
                                onSettingsChange = { settings -> settingsController.submit(settings) },
                                onBrightnessChange = { value -> settingsController.submitBrightness(value) },
                                onBrightnessReset = { settingsController.submitBrightness(null) },
                                onToggleSepia = { settingsController.toggleSepia() },
                                onOpenSearch = { viewModel.setSearchSheetVisible(true) },
                                onDismissSearch = { viewModel.setSearchSheetVisible(false) },
                                currentChapterTitle = resolvedChapterTitle,
                                currentChapterHref = currentChapterHref,
                                recentSearches = searchHistory,
                                onSubmitSearch = { query, options -> submitSearch(query, options) },
                                onRemoveRecentSearch = { searchHistoryStore.removeQuery(it) },
                                onClearRecentSearches = { searchHistoryStore.clear() },
                                onClearSearch = { clearSearch() },
                                onSearchResultClick = { result -> onSearchResultClicked(result) },
                                highlightTintSheetVisible = uiState.highlightTintSheetVisible,
                                onHighlightTintSelected = { tint ->
                                    viewModel.setHighlightTintSheetVisible(false)
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
                                activeFootnote = uiState.activeFootnote,
                                onDismissFootnote = { viewModel.setActiveFootnote(null) },
                                chapterTitles = remember(chapterNav) { chapterNav.chapterTitles },
                                canSkipPreviousChapter = chapterNav.canSkipPrevious,
                                canSkipNextChapter = chapterNav.canSkipNext,
                                onPreviousChapter = {
                                    ReaderScrubberHelper.resolvePreviousChapterIndex(chapterNav.currentChapterIndex)?.let {
                                        navigateToTocLink(chapterNav.chapterItems[it])
                                    }
                                },
                                onNextChapter = {
                                    ReaderScrubberHelper.resolveNextChapterIndex(
                                        chapterNav.currentChapterIndex,
                                        chapterNav.chapterItems.size
                                    )?.let {
                                        navigateToTocLink(chapterNav.chapterItems[it])
                                    }
                                },
                                onSeekToPercentage = { percent ->
                                    val targetIndex = ReaderScrubberHelper.findChapterTarget(
                                        percent,
                                        chapterNav.chapterItems.size
                                    )
                                    if (targetIndex != null && targetIndex in chapterNav.chapterItems.indices) {
                                        navigateToTocLink(chapterNav.chapterItems[targetIndex])
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
                            val exportData = uiState.exportSheetData
                            if (uiState.exportSheetVisible && exportData != null) {
                                NotesExportSheet(
                                    notesExporter = appContainer.notesExporter,
                                    singleBookData = exportData,
                                    onDismiss = { viewModel.dismissExportSheet() }
                                )
                            }
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
            lifecycleScope.launch { snackbarHostState.showSnackbar("No app available to open link") }
        }
    }

    override fun onJumpToLocator(locator: Locator) {
        // TTS sentence following shares the current href: never treat it as a jump.
        if (suppressedJumpHref == locator.href.toString()) {
            suppressedJumpHref = null
            return
        }
        val current = viewModel.uiState.value.currentLocation ?: return
        // Programmatic jumps (contents, search, bookmarks) set this before calling
        // navigator.go, otherwise the notice mislabels every jump as a footnote.
        val description = pendingJumpDescription ?: "Footnote / Link"
        pendingJumpDescription = null
        navigationHistory.recordJump(
            from = current,
            to = locator,
            description = description
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
                viewModel.setActiveFootnote(footnote)
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
        val current = viewModel.uiState.value.currentLocation ?: return
        val target = navigationHistory.goBack(current) ?: return
        navigator?.go(target, false)
        viewModel.setPageAnnouncement("Returned to previous location")
    }

    private fun navigateHistoryForward() {
        val current = viewModel.uiState.value.currentLocation ?: return
        val target = navigationHistory.goForward(current) ?: return
        navigator?.go(target, false)
        viewModel.setPageAnnouncement("Navigated forward")
    }

    private fun handleTtsPlay(): Boolean {
        // Audio focus is owned by Readium's TTS navigator (org.readium...tts.session
        // .AudioFocusManager requests it when playback starts). Claiming it here as well
        // produced two competing GAIN requests: the navigator's request took focus back
        // and the system delivered AUDIOFOCUS_LOSS to us ~100ms after play, so every
        // resume paused itself immediately.
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
        ttsNavigator?.pause()
    }

    private fun handleTtsStop() {
        Log.d(TAG, "Stopping TTS playback")
        audioBecomingNoisyReceiver.unregister(this@ReaderActivity)
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
                    val chapterTitle = viewModel.updateTtsChapterForUtterance(
                        utteranceHref = utteranceLocator.href.toString(),
                        fallbackBookTitle = publication?.metadata?.title
                    )
                    val stateSnapshot = viewModel.uiState.value
                    ttsMediaSessionManager?.updateMetadata(
                        bookTitle = stateSnapshot.bookTitle,
                        author = stateSnapshot.bookAuthor,
                        chapterTitle = chapterTitle
                    )
                    if (ttsController.state.value == ReaderTtsState.PLAYING || ttsController.state.value == ReaderTtsState.PAUSED) {
                        TtsAudioService.startOrUpdate(
                            context = this@ReaderActivity,
                            bookTitle = stateSnapshot.bookTitle,
                            author = stateSnapshot.bookAuthor,
                            chapterTitle = chapterTitle,
                            isPlaying = (ttsController.state.value == ReaderTtsState.PLAYING),
                            sessionToken = ttsMediaSessionManager?.sessionToken
                        )
                    }
                }
        }

        ttsObserveJobs += lifecycleScope.launch {
            nav.location
                .throttleLatest(1.seconds)
                .mapNotNull { it.tokenLocator ?: it.utteranceLocator }
                .distinctUntilChanged()
                .collect { locator ->
                    // TTS following the spoken sentence is not a user jump: mark the
                    // target so onJumpToLocator does not push history or show a banner
                    // on every utterance.
                    suppressedJumpHref = locator.href.toString()
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
