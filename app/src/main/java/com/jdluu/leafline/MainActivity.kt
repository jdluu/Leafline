package com.jdluu.leafline

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdluu.leafline.di.appContainer
import com.jdluu.leafline.hardcover.HardcoverBookPreference
import com.jdluu.leafline.hardcover.HardcoverConfigStore
import com.jdluu.leafline.hardcover.HardcoverOAuthException
import com.jdluu.leafline.hardcover.HardcoverTrackingDecision
import com.jdluu.leafline.library.BookArtifactCleanup
import com.jdluu.leafline.library.BookIdentity
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.LibraryFolderStore
import com.jdluu.leafline.library.LibraryFolderViewModel
import com.jdluu.leafline.library.LibraryFolderViewModelFactory
import com.jdluu.leafline.library.LibraryLayoutStore
import com.jdluu.leafline.library.LibraryScreen
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.LibraryViewModelFactory
import com.jdluu.leafline.library.LocalEpubDocumentScanner
import com.jdluu.leafline.library.ReadingGoalStore
import com.jdluu.leafline.library.SafEpubStager
import com.jdluu.leafline.library.SafFolderMetadata
import com.jdluu.leafline.library.SafStageResult
import com.jdluu.leafline.library.cover.BitmapMemoryCache
import com.jdluu.leafline.library.rescan.FolderRescanViewModel
import com.jdluu.leafline.library.rescan.FolderRescanViewModelFactory
import com.jdluu.leafline.library.rescan.createFolderRescanRunner
import com.jdluu.leafline.opds.OpdsDownloadManager
import com.jdluu.leafline.opds.OpdsViewModel
import com.jdluu.leafline.opds.OpdsViewModelFactory
import com.jdluu.leafline.opds.ui.OpdsScreen
import com.jdluu.leafline.settings.HardcoverTrackingPrompt
import com.jdluu.leafline.settings.SettingsTab
import com.jdluu.leafline.theme.AppearanceStore
import com.jdluu.leafline.theme.LeaflineTheme
import com.jdluu.leafline.theme.LocalReduceMotion
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainActivity"

private const val HARDCOVER_REDIRECT_SCHEME = "com.jdluu.leafline"
private const val HARDCOVER_REDIRECT_PATH = "/oauth2/callback"

class MainActivity : ComponentActivity() {
    companion object {
        internal const val EPUB_MIME_TYPE = "application/epub+zip"

        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    private val snackbarMessages = Channel<String>(Channel.BUFFERED)
    internal val snackbarFlow = snackbarMessages.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContent { LeaflineApp(this as MainActivity) }
        // The intent this activity was created with, not the `intent` property: on a
        // warm start getIntent() has not been updated yet at this point.
        handleHardcoverRedirect(getIntent())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleHardcoverRedirect(intent)
    }

    /**
     * Completes a Hardcover sign-in the browser handed back.
     *
     * The scheme is matched in the manifest, but the path is checked here: another app
     * can send an intent with our scheme, and only this one leads anywhere.
     */
    private fun handleHardcoverRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != HARDCOVER_REDIRECT_SCHEME) return
        if (!data.toString().contains(HARDCOVER_REDIRECT_PATH)) return

        lifecycleScope.launch {
            val coordinator = applicationContext.appContainer.hardcoverOAuth
            try {
                val config = coordinator.completeAuthorization(data.toString())
                showMessage(
                    if (config.userId != null) {
                        "Connected to Hardcover as user ${config.userId}. Turn on progress sync to start sending."
                    } else {
                        "Connected to Hardcover. Turn on progress sync to start sending."
                    }
                )
            } catch (e: HardcoverOAuthException) {
                showMessage("Hardcover sign-in failed: ${e.message}")
            } catch (e: Exception) {
                showMessage("Hardcover sign-in failed: ${e.message ?: "unknown error"}")
            } finally {
                // Re-publish disk state into the reactive store so Settings updates without restart.
                HardcoverConfigStore.config = applicationContext.appContainer.hardcoverConfigStore.load()
            }
        }
    }

    fun showMessage(message: String) {
        snackbarMessages.trySend(message)
    }

    fun saveLibraryFolder(uri: Uri, context: Context) {
        try {
            contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            Log.w(TAG, "Folder permission could not be persisted", e)
        }
        LibraryFolderStore.fromContext(context).add(uri.toString())
        showMessage("Library folder added")
    }

    fun importLibraryFolder(
        uri: Uri,
        context: Context,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> },
        onComplete: (List<LibraryBook>, Int) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val occupied = seedOccupiedPaths(context)
            val imported = mutableListOf<LibraryBook>()
            var failures = 0
            val documents = LocalEpubDocumentScanner.scanTree(contentResolver, uri)
            withContext(Dispatchers.Main) {
                onProgress(0, documents.size)
            }
            documents.forEachIndexed { index, documentUri ->
                val book = importEpub(documentUri, context, occupied) { failures++ }
                if (book != null) imported += book
                withContext(Dispatchers.Main) {
                    onProgress(index + 1, documents.size)
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(imported, failures)
            }
        }
    }

    suspend fun importEpub(
        contentUri: Uri,
        context: Context,
        occupied: MutableMap<String, String>? = null,
        onFailure: (String) -> Unit
    ): LibraryBook? = withContext(Dispatchers.IO) {
        val occupiedMap = occupied ?: seedOccupiedPaths(context)
        val displayName = getFileName(context, contentUri) ?: "imported.epub"
        when (
            val result = SafEpubStager.stageAndImport(
                filesDir = context.filesDir,
                displayName = displayName,
                openStream = { context.contentResolver.openInputStream(contentUri) },
                importer = context.appContainer.epubImporter,
                occupied = occupiedMap
            )
        ) {
            is SafStageResult.Imported -> result.book
            is SafStageResult.Failed -> {
                Log.w(TAG, "Import failed for $contentUri: ${result.message}")
                withContext(Dispatchers.Main) { onFailure(result.message) }
                null
            }
        }
    }

    /**
     * Bulk-import multiple EPUBs from SAF content URIs.
     *
     * @return Pair(successCount, failureCount)
     */
    suspend fun importMultipleEpub(
        contentUris: List<Uri>,
        context: Context,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> },
        onResult: (LibraryBook) -> Unit,
        onError: (String) -> Unit
    ): Pair<Int, Int> {
        val occupied = withContext(Dispatchers.IO) { seedOccupiedPaths(context) }
        var successCount = 0
        var failureCount = 0
        onProgress(0, contentUris.size)
        contentUris.forEachIndexed { index, uri ->
            val result = importEpub(uri, context, occupied) { msg ->
                Log.w(TAG, "Import failed for $uri: $msg")
                failureCount++
                onError(msg)
            }
            if (result != null) {
                successCount++
                onResult(result)
            }
            onProgress(index + 1, contentUris.size)
        }
        return successCount to failureCount
    }

    private suspend fun seedOccupiedPaths(context: Context): MutableMap<String, String> =
        context.appContainer.libraryRepository
            .getAllBooks()
            .first()
            .associate { it.filePath to it.fileHash }
            .toMutableMap()
}

private data class ImportProgressState(
    val label: String,
    val processed: Int = 0,
    val total: Int? = null
)

private fun getFileName(context: Context, uri: Uri): String? {
    var fileName: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            fileName = cursor.getString(nameIndex)
        }
    }
    return fileName
}

private enum class LeaflineTab(val label: String) {
    Library("Library"),
    Explore("Explore"),
    Settings("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaflineApp(activity: MainActivity) {
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(
            repository = activity.appContainer.libraryRepository,
            coverLoader = activity.appContainer.coverLoader,
            sortStore = LibrarySortStore.fromContext(activity),
            readingSessionRepository = activity.appContainer.readingSessionRepository,
            layoutStore = LibraryLayoutStore.fromContext(activity),
            readingGoalStore = ReadingGoalStore.fromContext(activity),
            onBooksDeleted = { deleted, remaining ->
                BookArtifactCleanup.cleanUp(
                    filesDir = activity.applicationContext.filesDir,
                    deletedBooks = deleted,
                    remainingBooks = remaining,
                    evictMemoryCache = BitmapMemoryCache::evict
                )
            }
        )
    )

    val libraryFolderViewModel: LibraryFolderViewModel = viewModel(
        factory = LibraryFolderViewModelFactory(
            store = LibraryFolderStore.fromContext(activity),
            metadata = SafFolderMetadata(activity.contentResolver)
        )
    )

    val folderRescanViewModel: FolderRescanViewModel = viewModel(
        factory = FolderRescanViewModelFactory(
            runner = createFolderRescanRunner(
                context = activity,
                importer = activity.appContainer.epubImporter,
                repository = activity.appContainer.libraryRepository
            )
        )
    )

    val opdsViewModel: OpdsViewModel = viewModel(
        factory = OpdsViewModelFactory(
            feedStore = activity.appContainer.opdsFeedStore,
            opdsClient = activity.appContainer.opdsClient,
            downloadManager = remember {
                OpdsDownloadManager(
                    context = activity.applicationContext,
                    opdsClient = activity.appContainer.opdsClient,
                    importer = activity.appContainer.epubImporter,
                    repository = activity.appContainer.libraryRepository,
                    scope = activity.lifecycleScope
                )
            },
            libraryRepository = activity.appContainer.libraryRepository
        )
    )

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val libraryListState = rememberLazyListState()
    val libraryGridState = rememberLazyGridState()
    var librarySearchActive by rememberSaveable { mutableStateOf(false) }
    val opdsGridState = rememberLazyGridState()
    var opdsSearchActive by rememberSaveable { mutableStateOf(false) }
    val settingsScrollState = rememberScrollState()
    val context = LocalContext.current
    val appearanceStore = remember { AppearanceStore.fromContext(context) }
    var appearance by remember { mutableStateOf(appearanceStore.load()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var importProgress by remember { mutableStateOf<ImportProgressState?>(null) }

    LaunchedEffect(activity) {
        activity.snackbarFlow.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val importEpubLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        val uriList = uris?.takeIf { it.isNotEmpty() } ?: return@rememberLauncherForActivityResult
        activity.lifecycleScope.launch {
            importProgress = ImportProgressState(
                label = "Importing books…",
                processed = 0,
                total = uriList.size
            )
            try {
                val imported = mutableListOf<LibraryBook>()
                var lastError: String? = null
                val (successCount, failureCount) = activity.importMultipleEpub(
                    contentUris = uriList,
                    context = context,
                    onProgress = { processed, total ->
                        importProgress = ImportProgressState(
                            label = "Importing books…",
                            processed = processed,
                            total = total
                        )
                    },
                    onResult = { imported.add(it) },
                    onError = { message -> lastError = message }
                )
                imported.forEach { libraryViewModel.addBook(it) }
                val msg = when {
                    successCount > 0 && failureCount > 0 ->
                        "Imported $successCount book(s), $failureCount failed"
                    successCount > 0 -> "Imported $successCount book(s)"
                    failureCount == 1 -> lastError ?: "Failed to import EPUB"
                    failureCount > 1 -> "Failed to import $failureCount EPUB(s)"
                    else -> null
                }
                if (msg != null) activity.showMessage(msg)
            } finally {
                importProgress = null
            }
        }
    }

    var pendingReplaceFolderUri by rememberSaveable { mutableStateOf<String?>(null) }

    // A book the reader has not yet decided about, held while the prompt is open.
    var pendingTrackingBook by remember { mutableStateOf<LibraryBook?>(null) }

    val addLibraryFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val replaceUri = pendingReplaceFolderUri
        pendingReplaceFolderUri = null
        uri?.let { selectedUri ->
            if (replaceUri != null) {
                // Reselect: drop the stale stored entry (releasing any held grant) before adding
                // the replacement so the new grant is the only one held for this location.
                libraryFolderViewModel.remove(replaceUri)
            }
            activity.saveLibraryFolder(selectedUri, context)
            importProgress = ImportProgressState(label = "Scanning folder…")
            activity.importLibraryFolder(
                uri = selectedUri,
                context = context,
                onProgress = { processed, total ->
                    importProgress = ImportProgressState(
                        label = "Importing folder…",
                        processed = processed,
                        total = total.takeIf { it > 0 }
                    )
                }
            ) { imported, failures ->
                importProgress = null
                imported.forEach(libraryViewModel::addBook)
                val message = if (failures == 0) {
                    "Imported ${imported.size} EPUB(s)"
                } else {
                    "Imported ${imported.size} EPUB(s), $failures failed"
                }
                activity.showMessage(message)
            }
        }
        // Reflect add, reselect, or a cancelled picker in the open folder sheet.
        libraryFolderViewModel.refresh()
    }

    LeaflineTheme(
            mode = appearance,
            darkSystem = isSystemInDarkTheme()
        ) {
        val reduceMotion = LocalReduceMotion.current
        val launchReader: (LibraryBook) -> Unit = { book ->
            activity.startActivity(ReaderActivity.newIntent(activity, book.filePath))
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(if (reduceMotion) 0 else android.R.anim.fade_in, 0)
        }

        /**
         * Opens a book, asking first when Hardcover is on and the reader has chosen to be
         * asked about this one. Any failure to read the setting opens the book rather than
         * blocking reading behind a question about a background feature.
         */
        val openReaderOrAsk: (LibraryBook) -> Unit = { book ->
            val shouldAsk = runCatching {
                activity.appContainer.hardcoverConfigStore.load()?.enabled == true &&
                    activity.appContainer.hardcoverTracking.decide(
                        book.stableId,
                        BookIdentity.publicationOf(book.stableId)
                    ) == HardcoverTrackingDecision.Ask
            }.getOrDefault(false)

            if (shouldAsk) {
                pendingTrackingBook = book
            } else {
                launchReader(book)
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    LeaflineTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    when (tab) {
                                        LeaflineTab.Library -> Icons.AutoMirrored.Filled.LibraryBooks
                                        LeaflineTab.Explore -> Icons.Default.Explore
                                        LeaflineTab.Settings -> Icons.Default.Settings
                                    },
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                importProgress?.let { progress ->
                    ImportProgressBanner(progress = progress)
                }
                Surface(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (LeaflineTab.entries.getOrNull(selectedTab)) {
                        LeaflineTab.Library -> LibraryScreen(
                            viewModel = libraryViewModel,
                            foldersViewModel = libraryFolderViewModel,
                            folderRescanViewModel = folderRescanViewModel,
                            backupManager = activity.appContainer.backupManager,
                            notesExporter = activity.appContainer.notesExporter,
                            listState = libraryListState,
                            gridState = libraryGridState,
                            searchActive = librarySearchActive,
                            onSearchActiveChange = { librarySearchActive = it },
                            onImportEpub = { importEpubLauncher.launch(arrayOf(MainActivity.EPUB_MIME_TYPE)) },
                            onAddLibraryFolder = { addLibraryFolderLauncher.launch(null) },
                            onReselectFolder = { uri ->
                                pendingReplaceFolderUri = uri
                                addLibraryFolderLauncher.launch(null)
                            },
                            onBackupRestored = {
                                appearance = appearanceStore.load()
                                libraryViewModel.reloadPreferences()
                                opdsViewModel.reloadFeeds()
                            },
                            onOpenBook = openReaderOrAsk
                        )
                        LeaflineTab.Explore -> OpdsScreen(
                            viewModel = opdsViewModel,
                            gridState = opdsGridState,
                            searchActive = opdsSearchActive,
                            onSearchActiveChange = { opdsSearchActive = it },
                            onOpenBook = openReaderOrAsk
                        )
                        LeaflineTab.Settings -> SettingsTab(
                            appearance = appearance,
                            onAppearanceChange = { selected ->
                                appearance = selected
                                appearanceStore.save(selected)
                            },
                            notesExporter = activity.appContainer.notesExporter,
                            scrollState = settingsScrollState
                        )
                        null -> {}
                    }

                    pendingTrackingBook?.let { book ->
                        HardcoverTrackingPrompt(
                            onAnswer = { tracked ->
                                activity.appContainer.hardcoverTracking.remember(
                                    book.stableId,
                                    if (tracked) {
                                        HardcoverBookPreference.TRACK
                                    } else {
                                        HardcoverBookPreference.IGNORE
                                    }
                                )
                                pendingTrackingBook = null
                                launchReader(book)
                            },
                            onDismiss = {
                                // Dismissing is not an answer: nothing is remembered and
                                // the book opens, so the question can be asked again.
                                pendingTrackingBook = null
                                launchReader(book)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportProgressBanner(progress: ImportProgressState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.screen, vertical = Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (progress.total != null && progress.total > 0) {
                        "${progress.label} (${progress.processed} of ${progress.total})"
                    } else {
                        progress.label
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            val total = progress.total
            if (total != null && total > 0) {
                LinearProgressIndicator(
                    progress = { (progress.processed.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
