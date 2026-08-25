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
import com.jdluu.leafline.library.LeaflineDependencyHolder
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
import com.jdluu.leafline.reader.search.BookSearchQuery
import com.jdluu.leafline.theme.DEFAULT_HIGHLIGHT_TINT
import com.jdluu.leafline.theme.HIGHLIGHT_TINTS
import com.jdluu.leafline.theme.HighlightTint
import com.jdluu.leafline.reader.search.BookSearchResult
import com.jdluu.leafline.reader.search.BookSearchState
import com.jdluu.leafline.reader.search.BookSearchStatus
import com.jdluu.leafline.reader.search.BookSearcher
import com.jdluu.leafline.reader.tapZoneAt
import com.jdluu.leafline.reader.withStyleMode
import com.jdluu.leafline.sync.BookRef
import com.jdluu.leafline.sync.KoreaderSyncClient
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
import org.readium.r2.navigator.preferences.Theme
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

/** UI state for the sync conflict bottom sheet. */
internal data class SyncConflictState(
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
        private const val MAX_SEARCH_DECORATIONS = 200
        private const val SEARCH_MATCH_TINT = 0x55FFD54F.toInt()
        private const val SEARCH_ACTIVE_TINT = 0xCCFF8F00.toInt()
        private const val ANNOTATION_DECORATION_GROUP = "leafline-annotations"
        private const val ANNOTATION_DECORATION_PREFIX = "annotation-"
        private const val DEFAULT_ANNOTATION_TINT = 0x55E65100.toInt()
        private const val MENU_ITEM_HIGHLIGHT_ID = 1
        private const val MENU_ITEM_COPY_ID = 2
        private const val KEY_SYNC_DEVICE_ID = "sync_device_id"
        private const val PAGE_TURN_ANNOUNCE_DEBOUNCE_MS = 300L

        fun newIntent(context: Context, filePath: String): Intent {
            return Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
            }
        }
    }

    private var navigator: EpubNavigatorFragment? = null
    private var publication: Publication? = null
    private var bookStableId: String? = null
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var annotationRepository: AnnotationRepository
    private lateinit var readerPreferencesStore: ReaderPreferencesStore
    private lateinit var bookSearcher: BookSearcher
    private lateinit var progressSyncer: ProgressSyncer

    /** Locator JSON captured when the user selects text and taps Highlight, pending tint selection. */
    private var pendingHighlightLocator: String? = null

    /** Currently selected highlight tint shown in the picker sheet. */
    private var selectedHighlightTint: HighlightTint = DEFAULT_HIGHLIGHT_TINT
    private var currentBook: com.jdluu.leafline.library.LibraryBook? = null
    private var toolbarVisible = mutableStateOf(false)
    private var settingsSheetVisible = mutableStateOf(false)
    private var highlightTintSheetVisible = mutableStateOf(false)
    private var bookmarkSheetVisible = mutableStateOf(false)
    private var highlightsSheetVisible = mutableStateOf(false)
    private var searchSheetVisible = mutableStateOf(false)
    private var bookTitle = mutableStateOf("")

    /** Sync conflict state for the bottom sheet. */
    private val syncConflictState = MutableStateFlow<SyncConflictState?>(null)
    private var tocLinks = mutableStateOf<List<Pair<Link, Int>>>(emptyList())
    private var currentSettings = mutableStateOf(ReaderSettings())
    private var bookmarks = mutableStateOf<List<Bookmark>>(emptyList())
    private var annotations = mutableStateOf<List<Annotation>>(emptyList())
    private var currentLocation = mutableStateOf<Locator?>(null)
    @Volatile
    private var activeSearchResultId: Int? = null
    private val snackbarHostState = SnackbarHostState()
    private val pageTurnAnnouncement = mutableStateOf<String?>(null)
    private var pageTurnAnnounceJob: Job? = null
    private var pageTurnAnnouncePrimed = false
    private var lastAnnouncedHref: String? = null
    private var lastAnnouncedPercent = -1

    /**
     * Turns taps reported by the Readium navigator into tap zone actions. The
     * listener consumes every tap except zones configured as none, which are
     * left unconsumed so the publication webview keeps default handling.
     * While scroll mode is on, page-turn actions resolve to none so taps stay
     * unconsumed and vertical webview gestures own the navigation.
     */
    private val readerInputListener = object : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            val width = navigator?.publicationView?.width ?: return false
            if (width <= 0) return false
            val settings = currentSettings.value
            return when (
                effectiveTapZoneAction(
                    tapZoneAt(event.point.x / width),
                    settings.tapZoneConfig,
                    settings.epub.scroll == true
                )
            ) {
                TapZoneAction.NONE -> false
                TapZoneAction.TOGGLE_MENU -> {
                    toolbarVisible.value = !toolbarVisible.value
                    true
                }
                TapZoneAction.NEXT_PAGE -> {
                    navigator?.goForward(pageTurnAnimated(settings))
                    true
                }
                TapZoneAction.PREVIOUS_PAGE -> {
                    navigator?.goBackward(pageTurnAnimated(settings))
                    true
                }
            }
        }
    }

    /**
     * Resolves the effective page turn animation against the system animator
     * duration scale so turns snap instantly while the OS has animations
     * removed, regardless of the stored preference.
     */
    private fun pageTurnAnimated(settings: ReaderSettings): Boolean {
        val animatorScale = android.provider.Settings.Global.getFloat(
            contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return pageTurnIsAnimated(settings.pageTurnAnimation, animatorScale, settings.reduceMotion)
    }

    /**
     * Announces page turns to TalkBack through a state-backed Compose text
     * node carrying a polite live region, rendered by the reader overlay.
     * The first locator emission primes the tracker silently; later
     * emissions announce when the resource changes or the reading percentage
     * moves by at least one point. Rapid successive updates, such as
     * continuous scrolling, collapse into one announcement through a short
     * debounce.
     */
    private fun onReadingPositionChanged(locator: Locator) {
        val percent = try {
            ReadingProgressMath.percentageFromLocator(locator.toJSON().toString())
        } catch (e: Exception) {
            null
        }?.roundToInt() ?: return
        val href = locator.href.toString()
        if (!pageTurnAnnouncePrimed) {
            pageTurnAnnouncePrimed = true
            lastAnnouncedHref = href
            lastAnnouncedPercent = percent
            return
        }
        val changedResource = href != lastAnnouncedHref
        val changedPage = abs(percent - lastAnnouncedPercent) >= 1
        if (!changedResource && !changedPage) return
        lastAnnouncedHref = href
        lastAnnouncedPercent = percent
        pageTurnAnnounceJob?.cancel()
        pageTurnAnnounceJob = lifecycleScope.launch {
            delay(PAGE_TURN_ANNOUNCE_DEBOUNCE_MS)
            pageTurnAnnouncement.value = "Page $percent%"
        }
    }

    @OptIn(ExperimentalReadiumApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarkRepository =
            com.jdluu.leafline.library.LeaflineDependencyHolder.getBookmarkRepository(this)
        annotationRepository =
            com.jdluu.leafline.library.LeaflineDependencyHolder.getAnnotationRepository(this)
        readerPreferencesStore = ReaderPreferencesStore.fromContext(this)

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

        var savedLocatorJson: String? = null
        var savedBook: com.jdluu.leafline.library.LibraryBook? = null
        {
            try {
                val lookupPath = epubFile!!.absolutePath
                savedBook = runBlocking {
                    com.jdluu.leafline.library.LeaflineDependencyHolder
                        .getRepository(this@ReaderActivity)
                        .getBookByFilePath(lookupPath)
                }
                if (savedBook == null) {
                    Log.w(TAG, "No library book found for path: $lookupPath")
                }
                savedLocatorJson = savedBook?.lastLocatorJson
                bookStableId = savedBook?.stableId
                // Update reading status to READING when opening
                savedBook?.let { book ->
                    if (book.readingStatus == ReadingStatus.UNREAD) {
                        val repo = com.jdluu.leafline.library.LeaflineDependencyHolder
                            .getRepository(this@ReaderActivity)
                        lifecycleScope.launch {
                            repo.setReadingStatus(book.stableId, ReadingStatus.READING)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load saved locator", e)
            }
        }
        currentBook = savedBook
        progressSyncer = createProgressSyncer()

        val initialLocator: Locator? = savedLocatorJson?.let { json ->
            try {
                Locator.Companion.fromJSON(org.json.JSONObject(json))
            } catch (e: Exception) {
                Log.w(TAG, "Could not parse saved locator, starting at beginning", e)
                null
            }
        }

        try {
            val publication = runBlocking {
                val asset = assetRetriever.retrieve(epubFile!!).getOrElse {
                    throw RuntimeException("Failed to retrieve asset: $it")
                }
                publicationOpener.open(asset, allowUserInteraction = true).getOrElse {
                    throw RuntimeException("Failed to open publication: $it")
                }
            }

            bookTitle.value = publication.metadata.title ?: "Reading"
            tocLinks.value = flattenToc(publication.tableOfContents)
            this@ReaderActivity.publication = publication
            bookSearcher = BookSearcher(
                scope = lifecycleScope,
                searchFactory = { query -> this@ReaderActivity.publication?.search(query) }
            )

            val animatorScale = android.provider.Settings.Global.getFloat(
                contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            val savedSettings = readerPreferencesStore.load(initialReduceMotion = animatorScale == 0f)
            currentSettings.value = savedSettings
            applyBrightnessToWindow(savedSettings.brightness)

            val navigatorFactory = EpubNavigatorFactory(publication)
            val fragmentFactory = navigatorFactory.createFragmentFactory(
                initialLocator = initialLocator,
                initialPreferences = savedSettings.epub,
                listener = this,
                configuration = EpubNavigatorFragment.Configuration(
                    selectionActionModeCallback = annotationSelectionActionMode()
                )
            )

            supportFragmentManager.fragmentFactory = fragmentFactory

            setContentView(R.layout.activity_reader)

            supportFragmentManager.commitNow {
                add(R.id.navigator_container, EpubNavigatorFragment::class.java, null, NAVIGATOR_TAG)
            }

            navigator = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as EpubNavigatorFragment
            navigator?.addInputListener(readerInputListener)
            lifecycleScope.launch {
                navigator?.currentLocator?.collect { locator ->
                    onReadingPositionChanged(locator)
                }
            }

            bookStableId?.let { stableId ->
                lifecycleScope.launch {
                    navigator?.currentLocator?.collect { locator ->
                        val locatorJson = locator.toJSON().toString()
                        currentLocation.value = locator
                        try {
                            com.jdluu.leafline.library.LeaflineDependencyHolder
                                .getRepository(this@ReaderActivity)
                                .saveLastLocator(stableId, locatorJson, System.currentTimeMillis())
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not save reading position", e)
                        }
                    }
                }
                lifecycleScope.launch {
                    pullRemoteProgress()
                }
                lifecycleScope.launch {
                    try {
                        bookmarkRepository.observeBookmarks(stableId).collect { stored ->
                            bookmarks.value = stored
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not observe bookmarks", e)
                    }
                }
                lifecycleScope.launch {
                    try {
                        annotationRepository.observeAnnotations(stableId).collect { stored ->
                            annotations.value = stored
                            applyAnnotationDecorations()
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

            lifecycleScope.launch {
                bookSearcher.state.collect { state ->
                    if (state.status is BookSearchStatus.Completed) {
                        applySearchDecorations()
                    }
                }
            }

            addReaderOverlay()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open publication", e)
            Toast.makeText(this, "Failed to open EPUB: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
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
        navigator?.go(link, false)
        toolbarVisible.value = false
    }

    private fun toggleBookmark() {
        val stableId = bookStableId ?: return
        val locator = navigator?.currentLocator?.value ?: return
        lifecycleScope.launch {
            try {
                val result = bookmarkRepository.toggleBookmark(
                    bookId = stableId,
                    locatorJson = locator.toJSON().toString()
                )
                snackbarHostState.showSnackbar(
                    when (result) {
                        is BookmarkToggleResult.Added -> "Bookmark added"
                        is BookmarkToggleResult.Removed -> "Bookmark removed"
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not toggle bookmark", e)
            }
        }
    }

    private fun deleteBookmark(bookmark: Bookmark) {
        lifecycleScope.launch {
            try {
                bookmarkRepository.removeBookmark(bookmark.id)
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete bookmark", e)
            }
        }
    }

    private fun navigateToBookmark(bookmark: Bookmark) {
        val locator = try {
            Locator.Companion.fromJSON(org.json.JSONObject(bookmark.locatorJson))
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse bookmark locator", e)
            null
        } ?: return
        navigator?.go(locator, false)
        bookmarkSheetVisible.value = false
        toolbarVisible.value = false
    }

    private fun annotationSelectionActionMode(): ActionMode.Callback {
        return object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.add(Menu.NONE, MENU_ITEM_HIGHLIGHT_ID, Menu.NONE, "Highlight")
                menu.add(Menu.NONE, MENU_ITEM_COPY_ID, Menu.NONE, "Copy")
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return when (item.itemId) {
                    MENU_ITEM_HIGHLIGHT_ID -> {
                        saveSelectionAsAnnotation()
                        mode.finish()
                        true
                    }
                    MENU_ITEM_COPY_ID -> {
                        copySelectedText()
                        mode.finish()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) = Unit
        }
    }

    private fun saveSelectionAsAnnotation() {
        val stableId = bookStableId
        if (stableId == null) {
            Toast.makeText(this, "Highlights need an imported library book", Toast.LENGTH_SHORT)
                .show()
            return
        }
        lifecycleScope.launch {
            try {
                val selection = navigator?.currentSelection()
                if (selection == null) {
                    Toast.makeText(this@ReaderActivity, "No text selected", Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }
                // Capture the locator, clear selection, then show the tint picker
                pendingHighlightLocator = selection.locator.toJSON().toString()
                navigator?.clearSelection()
                selectedHighlightTint = DEFAULT_HIGHLIGHT_TINT
                highlightTintSheetVisible.value = true
            } catch (e: Exception) {
                Log.w(TAG, "Could not prepare highlight", e)
                runCatching { navigator?.clearSelection() }
                Toast.makeText(this@ReaderActivity, "Could not create highlight", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun saveHighlightWithTint(tint: HighlightTint) {
        val stableId = bookStableId ?: return
        val locatorJson = pendingHighlightLocator ?: return
        lifecycleScope.launch {
            try {
                annotationRepository.addAnnotation(
                    bookId = stableId,
                    locatorJson = locatorJson,
                    colorHex = tint.hex
                )
                pendingHighlightLocator = null
                snackbarHostState.showSnackbar("Highlight added")
            } catch (e: Exception) {
                Log.w(TAG, "Could not save highlight", e)
                runCatching { navigator?.clearSelection() }
                Toast.makeText(this@ReaderActivity, "Could not save highlight", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun copySelectedText() {
        lifecycleScope.launch {
            val text = runCatching {
                navigator?.currentSelection()?.locator?.let { selectedTextOf(it) }
            }.getOrNull().takeIf { !it.isNullOrBlank() }
            if (text == null) {
                navigator?.clearSelection()
                return@launch
            }
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Selected text", text))
            navigator?.clearSelection()
            snackbarHostState.showSnackbar("Copied to clipboard")
        }
    }

    private fun selectedTextOf(locator: Locator): String? {
        return try {
            locator.toJSON().optJSONObject("text")?.optString("exact")
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun applyAnnotationDecorations() {
        val navigator = this.navigator ?: return
        if (!navigator.supportsDecorationStyle(Decoration.Style.Highlight::class)) return
        lifecycleScope.launch {
            try {
                val decorations = annotations.value.mapNotNull { annotation ->
                    val locator = parseLocator(annotation.locatorJson) ?: return@mapNotNull null
                    Decoration(
                        id = "$ANNOTATION_DECORATION_PREFIX${annotation.id}",
                        locator = locator,
                        style = Decoration.Style.Highlight(tint = annotationTint(annotation.colorHex))
                    )
                }
                navigator.applyDecorations(decorations, ANNOTATION_DECORATION_GROUP)
            } catch (e: Exception) {
                Log.w(TAG, "Could not apply annotation decorations", e)
            }
        }
    }

    private fun deleteAnnotation(annotation: Annotation) {
        lifecycleScope.launch {
            try {
                annotationRepository.removeAnnotation(annotation.id)
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete annotation", e)
            }
        }
    }

    private fun navigateToAnnotation(annotation: Annotation) {
        val locator = parseLocator(annotation.locatorJson)
        if (locator == null) {
            Toast.makeText(this, "Could not open this highlight", Toast.LENGTH_SHORT).show()
            return
        }
        navigator?.go(locator, false)
        highlightsSheetVisible.value = false
        toolbarVisible.value = false
    }

    private fun annotationTint(colorHex: String): Int {
        return try {
            Color.parseColor(colorHex)
        } catch (e: IllegalArgumentException) {
            DEFAULT_ANNOTATION_TINT
        }
    }

    private fun submitSettings(settings: ReaderSettings) {
        currentSettings.value = settings
        navigator?.submitPreferences(settings.epub)
        readerPreferencesStore.save(settings)
    }

    /**
     * Applies a brightness change to this activity's window and persists it.
     * A null value restores the system default; the system-wide brightness
     * setting is never modified.
     */
    private fun submitBrightness(value: Float?) {
        val updated = currentSettings.value.copy(brightness = value?.let { clampBrightness(it) })
        currentSettings.value = updated
        applyBrightnessToWindow(updated.brightness)
        readerPreferencesStore.save(updated)
    }

    private fun applyBrightnessToWindow(brightness: Float?) {
        val attributes = window.attributes
        attributes.screenBrightness =
            brightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = attributes
    }

    override fun onDestroy() {
        super.onDestroy()
        navigator?.removeInputListener(readerInputListener)
    }

    private fun submitSearch(rawQuery: String) {
        activeSearchResultId = null
        clearSearchDecorations()
        bookSearcher.start(rawQuery)
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
                val decorations = state.results
                    .take(MAX_SEARCH_DECORATIONS)
                    .mapNotNull { result ->
                        val locator = bookSearcher.locatorFor(result.id) ?: return@mapNotNull null
                        val active = result.id == activeSearchResultId
                        Decoration(
                            id = "search-${result.id}",
                            locator = locator,
                            style = Decoration.Style.Highlight(
                                tint = if (active) SEARCH_ACTIVE_TINT else SEARCH_MATCH_TINT,
                                isActive = active
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

    private fun addReaderOverlay() {
        val composeView = ComposeView(this).apply {
            setContent {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val searchState by bookSearcher.state.collectAsState()
                val syncConflict by syncConflictState.collectAsState()
                ReaderOverlay(
                    title = bookTitle.value,
                    toolbarVisible = toolbarVisible.value,
                    tocLinks = tocLinks.value,
                    currentSettings = currentSettings.value,
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
                    pageAnnouncement = pageTurnAnnouncement.value,
                    snackbarHostState = snackbarHostState,
                    syncConflict = syncConflict,
                    onJumpToRemote = { syncConflict?.onJump?.invoke(); syncConflictState.value = null },
                    onDismissSyncConflict = { syncConflictState.value = null },
                    onBack = { finish() },
                    onOpenToc = { scope.launch { drawerState.open() } },
                    onOpenSettings = { settingsSheetVisible.value = true },
                    onDismissSettings = { settingsSheetVisible.value = false },
                    onToggleBookmark = { toggleBookmark() },
                    onOpenBookmarks = { bookmarkSheetVisible.value = true },
                    onDismissBookmarks = { bookmarkSheetVisible.value = false },
                    onBookmarkClick = { navigateToBookmark(it) },
                    onDeleteBookmark = { deleteBookmark(it) },
                    onOpenHighlights = { highlightsSheetVisible.value = true },
                    onDismissHighlights = { highlightsSheetVisible.value = false },
                    onAnnotationClick = { navigateToAnnotation(it) },
                    onDeleteAnnotation = { deleteAnnotation(it) },
                    onTocClick = { link ->
                        navigateToTocLink(link)
                        scope.launch { drawerState.close() }
                    },
                    onSettingsChange = { settings -> submitSettings(settings) },
                    onBrightnessChange = { value -> submitBrightness(value) },
                    onBrightnessReset = { submitBrightness(null) },
                    onToggleSepia = { submitSettings(toggleSepia(currentSettings.value)) },
                    onOpenSearch = { searchSheetVisible.value = true },
                    onDismissSearch = { searchSheetVisible.value = false },
                    onSubmitSearch = { query -> submitSearch(query) },
                    onClearSearch = { clearSearch() },
                    onSearchResultClick = { result -> onSearchResultClicked(result) },
                    highlightTintSheetVisible = highlightTintSheetVisible.value,
                    onHighlightTintSelected = { tint ->
                        highlightTintSheetVisible.value = false
                        saveHighlightWithTint(tint)
                    }
                )
            }
        }
        val layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        addContentView(composeView, layoutParams)
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

    override fun onStop() {
        super.onStop()
        pushProgressOnExit()
    }

    private fun createProgressSyncer(): ProgressSyncer {
        val preferences = getSharedPreferences(LibrarySortStore.PREFS_NAME, Context.MODE_PRIVATE)
        val deviceId = preferences.getString(KEY_SYNC_DEVICE_ID, null) ?: UUID.randomUUID().toString()
            .also { generated ->
                preferences.edit().putString(KEY_SYNC_DEVICE_ID, generated).apply()
            }
        return ProgressSyncer(
            api = KoreaderSyncClient(),
            configSource = { KoreaderSyncConfigStore.config },
            repository = com.jdluu.leafline.library.LeaflineDependencyHolder.getRepository(this),
            deviceName = Build.MODEL ?: "Leafline",
            deviceId = deviceId
        )
    }

    private suspend fun pullRemoteProgress() {
        val book = currentBook ?: return
        if (!this::progressSyncer.isInitialized) return
        when (val outcome = progressSyncer.pull(BookRef(book))) {
            is PullOutcome.RemoteAhead -> presentRemoteProgress(outcome)
            is PullOutcome.Failure -> Toast.makeText(
                this,
                "Progress sync failed: ${outcome.message}",
                Toast.LENGTH_LONG
            ).show()
            else -> Unit
        }
    }

    private fun presentRemoteProgress(outcome: PullOutcome.RemoteAhead) {
        val remote = outcome.remote
        val localProgress = currentLocation.value?.let { locator ->
            ReadingProgressMath.percentageFromLocator(locator.toJSON().toString())
        }
        val target = remote.progress?.let { json -> parseLocatorOrNull(json) }
        if (target == null) {
            syncConflictState.value = SyncConflictState(
                localPercentage = localProgress,
                remotePercentage = outcome.remotePercentage,
                remoteDevice = remote.device,
                remoteTimestamp = remote.timestamp,
                onJump = {}
            )
            return
        }
        syncConflictState.value = SyncConflictState(
            localPercentage = localProgress,
            remotePercentage = outcome.remotePercentage,
            remoteDevice = remote.device,
            remoteTimestamp = remote.timestamp,
            onJump = { navigator?.go(target, false) }
        )
    }

    private fun pushProgressOnExit() {
        val book = currentBook ?: return
        if (!this::progressSyncer.isInitialized) return
        val locator = navigator?.currentLocator?.value ?: return
        val locatorJson = locator.toJSON().toString()
        lifecycleScope.launch {
            val outcome = try {
                withContext(NonCancellable) { progressSyncer.push(BookRef(book), locatorJson) }
            } catch (e: Exception) {
                PushOutcome.Failure(e.message ?: "Pushing progress failed")
            }
            if (outcome is PushOutcome.Failure) {
                Toast.makeText(
                    this@ReaderActivity,
                    "Progress sync failed: ${outcome.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Auto-suggest FINISHED status when progress >= 98%
        val progress = ReadingProgressMath.percentageFromLocator(locatorJson)
        if (progress != null && progress >= 98.0 && book.readingStatus == ReadingStatus.READING) {
            runOnUiThread {
                AlertDialog.Builder(this@ReaderActivity)
                    .setTitle("Mark as finished?")
                    .setMessage("You're at ${progress.toInt()}% — looks like you've finished this book. Would you like to mark it as finished?")
                    .setPositiveButton("Mark finished") { _, _ ->
                        lifecycleScope.launch {
                            com.jdluu.leafline.library.LeaflineDependencyHolder
                                .getRepository(this@ReaderActivity)
                                .setReadingStatus(book.stableId, ReadingStatus.FINISHED)
                        }
                    }
                    .setNegativeButton("Keep reading") { _, _ -> }
                    .show()
            }
        }
    }

    private fun parseLocatorOrNull(json: String): Locator? {
        return try {
            Locator.Companion.fromJSON(org.json.JSONObject(json))
        } catch (e: Exception) {
            null
        }
    }
}

