package com.jdluu.leafline

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

    private var pendingToast: String? = null

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
        handleToast()
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
                showToast(
                    if (config.userId != null) {
                        "Connected to Hardcover as user ${config.userId}. Turn on progress sync to start sending."
                    } else {
                        "Connected to Hardcover. Turn on progress sync to start sending."
                    }
                )
            } catch (e: HardcoverOAuthException) {
                showToast("Hardcover sign-in failed: ${e.message}")
            } catch (e: Exception) {
                showToast("Hardcover sign-in failed: ${e.message ?: "unknown error"}")
            } finally {
                // Re-publish disk state into the reactive store so Settings updates without restart.
                HardcoverConfigStore.config = applicationContext.appContainer.hardcoverConfigStore.load()
            }
        }
    }

    private fun handleToast() {
        pendingToast?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            pendingToast = null
        }
    }

    fun showToast(message: String) {
        pendingToast = message
        handleToast()
    }

    fun saveLibraryFolder(uri: Uri, context: Context) {
        try {
            contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            Log.w(TAG, "Folder permission could not be persisted", e)
        }
        LibraryFolderStore.fromContext(context).add(uri.toString())
        showToast("Library folder added")
    }

    fun importLibraryFolder(uri: Uri, context: Context, onComplete: (List<LibraryBook>, Int) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val occupied = seedOccupiedPaths(context)
            val imported = mutableListOf<LibraryBook>()
            var failures = 0
            LocalEpubDocumentScanner.scanTree(contentResolver, uri).forEach { documentUri ->
                val book = importEpub(documentUri, context, occupied) { failures++ }
                if (book != null) imported += book
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
        onResult: (LibraryBook) -> Unit,
        onError: (String) -> Unit
    ): Pair<Int, Int> {
        val occupied = withContext(Dispatchers.IO) { seedOccupiedPaths(context) }
        var successCount = 0
        var failureCount = 0
        for (uri in contentUris) {
            val result = importEpub(uri, context, occupied) { msg ->
                Log.w(TAG, "Import failed for $uri: $msg")
                failureCount++
                onError(msg)
            }
            if (result != null) {
                successCount++
                onResult(result)
            }
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
    val context = LocalContext.current
    val appearanceStore = remember { AppearanceStore.fromContext(context) }
    var appearance by remember { mutableStateOf(appearanceStore.load()) }

    val importEpubLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        val uriList = uris?.takeIf { it.isNotEmpty() } ?: return@rememberLauncherForActivityResult
        activity.lifecycleScope.launch {
            val imported = mutableListOf<LibraryBook>()
            val (successCount, failureCount) = activity.importMultipleEpub(
                contentUris = uriList,
                context = context,
                onResult = { imported.add(it) },
                onError = { message -> activity.showToast(message) }
            )
            imported.forEach { libraryViewModel.addBook(it) }
            if (successCount > 0) {
                val msg = if (failureCount > 0) {
                    "Imported $successCount book(s), $failureCount failed"
                } else {
                    "Imported $successCount book(s)"
                }
                activity.showToast(msg)
            }
        }
    }

    var pendingReplaceFolderUri by rememberSaveable { mutableStateOf<String?>(null) }

    // A book the reader has not yet decided about, held while the prompt is open.
    var pendingTrackingBook by remember { mutableStateOf<LibraryBook?>(null) }

    val launchReader: (LibraryBook) -> Unit = { book ->
        activity.startActivity(ReaderActivity.newIntent(activity, book.filePath))
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(android.R.anim.fade_in, 0)
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
            activity.importLibraryFolder(selectedUri, context) { imported, failures ->
                imported.forEach(libraryViewModel::addBook)
                val message = if (failures == 0) {
                    "Imported ${imported.size} EPUB(s)"
                } else {
                    "Imported ${imported.size} EPUB(s), $failures failed"
                }
                activity.showToast(message)
            }
        }
        // Reflect add, reselect, or a cancelled picker in the open folder sheet.
        libraryFolderViewModel.refresh()
    }

    LeaflineTheme(
            mode = appearance,
            darkSystem = isSystemInDarkTheme()
        ) {
        Scaffold(
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
            Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (LeaflineTab.entries.getOrNull(selectedTab)) {
                    LeaflineTab.Library -> LibraryScreen(
                        viewModel = libraryViewModel,
                        foldersViewModel = libraryFolderViewModel,
                        folderRescanViewModel = folderRescanViewModel,
                        backupManager = activity.appContainer.backupManager,
                        notesExporter = activity.appContainer.notesExporter,
                        onImportEpub = { importEpubLauncher.launch(arrayOf(MainActivity.EPUB_MIME_TYPE)) },
                        onAddLibraryFolder = { addLibraryFolderLauncher.launch(null) },
                        onReselectFolder = { uri ->
                            pendingReplaceFolderUri = uri
                            addLibraryFolderLauncher.launch(null)
                        },
                        onOpenBook = openReaderOrAsk
                    )
                    LeaflineTab.Explore -> OpdsScreen(
                        viewModel = opdsViewModel,
                        onOpenBook = openReaderOrAsk
                    )
                    LeaflineTab.Settings -> SettingsTab(
                        appearance = appearance,
                        onAppearanceChange = { selected ->
                            appearance = selected
                            appearanceStore.save(selected)
                        },
                        notesExporter = activity.appContainer.notesExporter
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
