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
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.OutlinedTextField
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
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.library.data.BookmarkRepository
import com.jdluu.leafline.library.data.BookmarkToggleResult
import com.jdluu.leafline.library.data.LocatorIdentity
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.reader.search.BookSearchQuery
import com.jdluu.leafline.reader.search.BookSearchResult
import com.jdluu.leafline.reader.search.BookSearchState
import com.jdluu.leafline.reader.search.BookSearchStatus
import com.jdluu.leafline.reader.search.BookSearcher
import com.jdluu.leafline.sync.BookRef
import com.jdluu.leafline.sync.KoreaderSyncClient
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import com.jdluu.leafline.sync.ProgressSyncer
import com.jdluu.leafline.sync.PullOutcome
import com.jdluu.leafline.sync.PushOutcome
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
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
import org.readium.r2.navigator.preferences.FontFamily
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

private const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : FragmentActivity(), EpubNavigatorFragment.Listener {

    companion object {
        private const val TAG = "ReaderActivity"
        private const val EPUB_FILE_NAME = "leafline-spike.epub"
        private const val NAVIGATOR_TAG = "EpubNavigatorFragment"
        private const val EXTRA_FILE_PATH = "extra_file_path"
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val SEARCH_DECORATION_GROUP = "leafline-search"
        private const val MAX_SEARCH_DECORATIONS = 200
        private const val SEARCH_MATCH_TINT = 0x55FFD54F.toInt()
        private const val SEARCH_ACTIVE_TINT = 0xCCFF8F00.toInt()
        private const val ANNOTATION_DECORATION_GROUP = "leafline-annotations"
        private const val ANNOTATION_DECORATION_PREFIX = "annotation-"
        private const val DEFAULT_ANNOTATION_TINT = 0x55FFF59F.toInt()
        private const val MENU_ITEM_HIGHLIGHT_ID = 1
        private const val MENU_ITEM_COPY_ID = 2
        private const val KEY_SYNC_DEVICE_ID = "sync_device_id"

        fun newIntent(context: Context): Intent {
            return Intent(context, ReaderActivity::class.java)
        }

        fun newIntent(context: Context, filePath: String): Intent {
            return Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
            }
        }

        private fun copyEpubFromAssets(context: Context, targetFile: File) {
            if (!targetFile.exists()) {
                try {
                    context.assets.open(EPUB_FILE_NAME).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to copy bundled EPUB", e)
                }
            }
        }
    }

    private var navigator: EpubNavigatorFragment? = null
    private var publication: Publication? = null
    private var bookStableId: String? = null
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var annotationRepository: AnnotationRepository
    private lateinit var bookSearcher: BookSearcher
    private lateinit var progressSyncer: ProgressSyncer
    private var currentBook: com.jdluu.leafline.library.LibraryBook? = null
    private var toolbarVisible = mutableStateOf(false)
    private var settingsSheetVisible = mutableStateOf(false)
    private var bookmarkSheetVisible = mutableStateOf(false)
    private var highlightsSheetVisible = mutableStateOf(false)
    private var searchSheetVisible = mutableStateOf(false)
    private var bookTitle = mutableStateOf("")
    private var tocLinks = mutableStateOf<List<Pair<Link, Int>>>(emptyList())
    private var currentPreferences = mutableStateOf(EpubPreferences())
    private var bookmarks = mutableStateOf<List<Bookmark>>(emptyList())
    private var annotations = mutableStateOf<List<Annotation>>(emptyList())
    private var currentLocation = mutableStateOf<Locator?>(null)
    @Volatile
    private var activeSearchResultId: Int? = null
    private val snackbarHostState = SnackbarHostState()

    @OptIn(ExperimentalReadiumApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarkRepository =
            com.jdluu.leafline.library.LeaflineDependencyHolder.getBookmarkRepository(this)
        annotationRepository =
            com.jdluu.leafline.library.LeaflineDependencyHolder.getAnnotationRepository(this)

        val importedPath = intent?.getStringExtra(EXTRA_FILE_PATH)
        var epubFile: File? = null
        var usedImportedFile = false

        if (!importedPath.isNullOrEmpty()) {
            val file = File(importedPath)
            try {
                val canonicalPath = file.canonicalPath
                val allowedPath = filesDir.canonicalPath
                if (canonicalPath.startsWith(allowedPath + File.separator)) {
                    epubFile = file
                    usedImportedFile = true
                }
            } catch (e: IOException) {
                Log.w(TAG, "Cannot resolve imported file canonical path", e)
            }
        }

        if (epubFile == null) {
            val bundledFile = File(filesDir, EPUB_FILE_NAME)
            copyEpubFromAssets(this, bundledFile)
            epubFile = bundledFile
        }

        if (!epubFile!!.exists()) {
            Log.e(TAG, "EPUB file does not exist: ${epubFile!!.absolutePath}")
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
        if (usedImportedFile) {
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

            val navigatorFactory = EpubNavigatorFactory(publication)
            val fragmentFactory = navigatorFactory.createFragmentFactory(
                initialLocator = initialLocator,
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

            if (!usedImportedFile) {
                Toast.makeText(this, "Opened bundled EPUB", Toast.LENGTH_SHORT).show()
            }
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
                annotationRepository.addAnnotation(
                    bookId = stableId,
                    locatorJson = selection.locator.toJSON().toString()
                )
                navigator?.clearSelection()
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

    private fun submitPreferences(prefs: EpubPreferences) {
        currentPreferences.value = prefs
        navigator?.submitPreferences(prefs)
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
                ReaderOverlay(
                    title = bookTitle.value,
                    toolbarVisible = toolbarVisible.value,
                    tocLinks = tocLinks.value,
                    currentPreferences = currentPreferences.value,
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
                    snackbarHostState = snackbarHostState,
                    onToggleToolbar = { toolbarVisible.value = !toolbarVisible.value },
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
                    onPreferencesChange = { prefs -> submitPreferences(prefs) },
                    onOpenSearch = { searchSheetVisible.value = true },
                    onDismissSearch = { searchSheetVisible.value = false },
                    onSubmitSearch = { query -> submitSearch(query) },
                    onClearSearch = { clearSearch() },
                    onSearchResultClick = { result -> onSearchResultClicked(result) }
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
        val target = remote.progress?.let { json -> parseLocatorOrNull(json) }
        if (target == null) {
            val percentage = outcome.remotePercentage
            val detail = if (percentage != null) String.format(Locale.US, "%.0f%%", percentage) else ""
            Toast.makeText(this, "Newer reading position$detail on ${remote.device ?: "another device"}", Toast.LENGTH_LONG).show()
            return
        }
        val percentage = outcome.remotePercentage
        val message = buildString {
            append("Remote reading position")
            if (percentage != null) {
                append(String.format(Locale.US, " (%.0f%%)", percentage))
            }
            append(" from ").append(remote.device ?: "another device")
            append(" is newer. Jump there?")
        }
        AlertDialog.Builder(this)
            .setTitle("Progress sync")
            .setMessage(message)
            .setPositiveButton("Jump") { _, _ -> navigator?.go(target, false) }
            .setNegativeButton("Stay", null)
            .show()
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
    }

    private fun parseLocatorOrNull(json: String): Locator? {
        return try {
            Locator.Companion.fromJSON(org.json.JSONObject(json))
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderOverlay(
    title: String,
    toolbarVisible: Boolean,
    tocLinks: List<Pair<Link, Int>>,
    currentPreferences: EpubPreferences,
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
    snackbarHostState: SnackbarHostState,
    onToggleToolbar: () -> Unit,
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
    onTocClick: (Link) -> Unit,
    onPreferencesChange: (EpubPreferences) -> Unit,
    onOpenSearch: () -> Unit,
    onDismissSearch: () -> Unit,
    onSubmitSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResultClick: (BookSearchResult) -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Contents",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
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
            InvisibleTapZone(onToggleToolbar)
            if (toolbarVisible) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
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
            }

            if (settingsSheetVisible) {
                ReaderSettingsSheet(
                    preferences = currentPreferences,
                    onPreferencesChange = onPreferencesChange,
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

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
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
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
private fun BookmarkListSheet(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Bookmarks",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        if (bookmarks.isEmpty()) {
            Text(
                "No bookmarks yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
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
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(end = 24.dp)
                                )
                            }
                        }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { onBookmarkClick(bookmark) })
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatCreatedAt(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

private fun annotationExcerpt(annotation: Annotation): String {
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
private fun AnnotationListSheet(
    annotations: List<Annotation>,
    onAnnotationClick: (Annotation) -> Unit,
    onDeleteAnnotation: (Annotation) -> Unit,
    onDismiss: () -> Unit,
    excerptFor: (Annotation) -> String = { annotationExcerpt(it) }
) {    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Highlights",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        if (annotations.isEmpty()) {
            Text(
                "No highlights yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(annotations, key = { it.id }) { annotation ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onAnnotationClick(annotation) })
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 12.dp,
                                bottom = 12.dp,
                                end = 4.dp
                            )
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = excerptFor(annotation),
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
                            IconButton(onClick = { onDeleteAnnotation(annotation) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete highlight")
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookSearchSheet(
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
                .padding(horizontal = 16.dp)
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
                    .padding(top = 8.dp)
                    .focusRequester(focusRequester),
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
                .heightIn(max = 420.dp)
        ) {
            items(state.results, key = { it.id }) { result ->
                SearchResultRow(result = result, onClick = { onResultClick(result) })
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SearchStatusText(state: BookSearchState, modifier: Modifier = Modifier) {
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
            )
        }
        is BookSearchStatus.Completed -> Text(
            text = if (status.resultCount == 0) "No matches found" else matchCountLabel(status.resultCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        is BookSearchStatus.Failed -> Text(
            text = status.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier
        )
    }
}

private fun matchCountLabel(count: Int): String {
    val total = count.toString()
    return if (count == 1) "$total match" else "$total matches"
}

@Composable
private fun SearchResultRow(
    result: BookSearchResult,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
private fun searchExcerpt(result: BookSearchResult): AnnotatedString {
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
private fun ReaderSettingsSheet(
    preferences: EpubPreferences,
    onPreferencesChange: (EpubPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                "Reader Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text("Theme", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                listOf(
                    Theme.LIGHT to "Light",
                    Theme.SEPIA to "Sepia",
                    Theme.DARK to "Dark"
                ).forEach { (theme, label) ->
                    FilterChip(
                        selected = preferences.theme == theme,
                        onClick = { onPreferencesChange(preferences.copy(theme = theme)) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Text("Font", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                listOf(
                    null to "Original",
                    FontFamily.SERIF to "Serif",
                    FontFamily.SANS_SERIF to "Sans"
                ).forEach { (family, label) ->
                    FilterChip(
                        selected = preferences.fontFamily == family,
                        onClick = { onPreferencesChange(preferences.copy(fontFamily = family)) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Text("Line height", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            ) {
                IconButton(onClick = {
                    val current = preferences.lineHeight ?: 1.2
                    if (current > 1.0) {
                        onPreferencesChange(preferences.copy(lineHeight = current - 0.2))
                    }
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease line height")
                }
                Text(
                    text = "%.1f".format(preferences.lineHeight ?: 1.2),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(onClick = {
                    val current = preferences.lineHeight ?: 1.2
                    if (current < 2.5) {
                        onPreferencesChange(preferences.copy(lineHeight = current + 0.2))
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase line height")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Use publisher styles",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = preferences.publisherStyles ?: true,
                    onCheckedChange = { checked ->
                        onPreferencesChange(preferences.copy(publisherStyles = checked))
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TocItem(link: Link, depth: Int, onClick: () -> Unit) {
    val indent = (depth * 16).dp
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

@Composable
private fun InvisibleTapZone(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onTap)
    )
}
