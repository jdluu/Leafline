package com.jdluu.leafline

import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.jdluu.leafline.theme.themeScheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.jdluu.leafline.hardcover.HardcoverBookPreference
import com.jdluu.leafline.hardcover.HardcoverTrackingDecision
import com.jdluu.leafline.hardcover.HardcoverConfig
import com.jdluu.leafline.hardcover.HardcoverOAuthException
import com.jdluu.leafline.hardcover.HardcoverTrackingMode
import com.jdluu.leafline.hardcover.HardcoverConfigStore
import com.jdluu.leafline.di.appContainer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Download
import com.jdluu.leafline.export.BookExportData
import com.jdluu.leafline.export.NotesExporter
import com.jdluu.leafline.export.ui.NotesExportSheet
import com.jdluu.leafline.library.BookArtifactCleanup
import com.jdluu.leafline.library.BookIdentity
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.LibraryFolderStore
import com.jdluu.leafline.library.LibraryFolderViewModel
import com.jdluu.leafline.library.LibraryFolderViewModelFactory
import com.jdluu.leafline.library.LocalEpubDocumentScanner
import com.jdluu.leafline.library.LibraryScreen
import com.jdluu.leafline.library.LibraryLayoutStore
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.LibraryViewModelFactory
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
import com.jdluu.leafline.sync.KoreaderSyncConfig
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import com.jdluu.leafline.sync.SyncWorker
import com.jdluu.leafline.sync.isKoreaderServerUrlValid
import com.jdluu.leafline.sync.koreaderSyncSummary
import com.jdluu.leafline.theme.AppearanceStore
import com.jdluu.leafline.theme.FormPanel
import com.jdluu.leafline.theme.IconSize
import com.jdluu.leafline.theme.LeaflineTextField
import com.jdluu.leafline.theme.LeaflineTheme
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.Switch

private const val TAG = "MainActivity"

private const val HARDCOVER_REDIRECT_SCHEME = "com.jdluu.leafline"
private const val HARDCOVER_REDIRECT_PATH = "/oauth2/callback"

/** Bumped when a redirect changes the Hardcover connection, to refresh the row. */
private val hardcoverConfigVersion = mutableIntStateOf(0)

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
                // Re-read the settings row so it shows the new state without a restart.
                HardcoverConfigStore.config = applicationContext.appContainer.hardcoverConfigStore.load()
                hardcoverConfigVersion.value = hardcoverConfigVersion.value + 1
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    appearance: ThemeMode,
    onAppearanceChange: (ThemeMode) -> Unit,
    notesExporter: NotesExporter? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
        ) {
            Spacer(Modifier.height(Spacing.xs))
            AppearanceSection(
                appearance = appearance,
                onAppearanceChange = onAppearanceChange
            )
            Spacer(Modifier.height(Spacing.lg))
            SyncSection()
            Spacer(Modifier.height(Spacing.lg))
            HardcoverSection()
            Spacer(Modifier.height(Spacing.lg))
            if (notesExporter != null) {
                ExportSection(notesExporter = notesExporter)
                Spacer(Modifier.height(Spacing.lg))
            }
            AboutSection()
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

/** Section heading on the 8dp rhythm: more air above the heading than below it. */
@Composable
private fun SettingsSectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = Spacing.xs)
    )
}

/**
 * One decision on the settings screen: a label, its current value, and the
 * control that changes it. The whole row is the target, so the reader aims at
 * a name instead of a chip buried in a wall of controls.
 */
@Composable
private fun SettingsRow(
    title: String,
    summary: String?,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = Spacing.xs)
            .semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(Spacing.xs))
        trailing()
    }
}

/** The four fixed schemes, in the order the app offers them. */
private val FIXED_THEME_MODES = listOf(
    ThemeMode.LIGHT,
    ThemeMode.DARK,
    ThemeMode.OLED,
    ThemeMode.E_INK
)

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.OLED -> "OLED"
        ThemeMode.E_INK -> "E-ink"
    }

private val ThemeMode.description: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Follows the system light or dark setting using the Leafline palette."
        ThemeMode.LIGHT -> "Always uses the Leafline light scheme."
        ThemeMode.DARK -> "Always uses the Leafline dark scheme."
        ThemeMode.OLED -> "Uses the pure-black dark scheme for OLED displays."
        ThemeMode.E_INK -> "Uses the high-contrast monochrome scheme for e-ink displays."
    }

/** The one-line subtitle a fixed tile carries under its name. */
private val ThemeMode.shortSummary: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Follows your device setting"
        ThemeMode.LIGHT -> "Leafline light"
        ThemeMode.DARK -> "Leafline dark"
        ThemeMode.OLED -> "True black surfaces"
        ThemeMode.E_INK -> "Monochrome, high contrast"
    }

/**
 * App-wide scheme choice. Every option is previewed as a miniature page drawn
 * in that scheme's own colors, so the choice is made by looking at it rather
 * than by reading five names and hoping. Following the system is a different
 * kind of choice from picking one of the four fixed schemes, so it reads as
 * its own full-width row above the 2x2 grid.
 */
@Composable
private fun AppearanceSection(
    appearance: ThemeMode,
    onAppearanceChange: (ThemeMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("Appearance")
        // One radio group across the full-width row and the 2x2 grid, so TalkBack
        // announces a position in the set instead of five unrelated buttons.
        Column(modifier = Modifier.selectableGroup()) {
            ThemePreviewTile(
                mode = ThemeMode.SYSTEM,
                selected = appearance == ThemeMode.SYSTEM,
                onClick = { onAppearanceChange(ThemeMode.SYSTEM) },
                horizontal = true,
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.padding(top = Spacing.xs)
            ) {
                FIXED_THEME_MODES.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        pair.forEach { mode ->
                            ThemePreviewTile(
                                mode = mode,
                                selected = appearance == mode,
                                onClick = { onAppearanceChange(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "Active: ${appearance.label}. ${appearance.description}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm)
        )
    }
}

@Composable
private fun ThemePreviewTile(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false
) {
    // System previews the scheme applied right now; the others preview their
    // own fixed scheme.
    val preview = if (mode == ThemeMode.SYSTEM) {
        MaterialTheme.colorScheme
    } else {
        themeScheme(mode, darkSystem = false)
    }
    val tileModifier = modifier
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .semantics(mergeDescendants = true) {}
        .padding(vertical = Spacing.xs)
    if (horizontal) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = tileModifier
        ) {
            ThemePreviewCanvas(
                preview = preview,
                selected = selected,
                modifier = Modifier.width(72.dp)
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                ThemeTileName(mode = mode, selected = selected)
                Text(
                    text = mode.shortSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(modifier = tileModifier) {
            ThemePreviewCanvas(
                preview = preview,
                selected = selected,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.xs))
            ThemeTileName(mode = mode, selected = selected)
        }
    }
}

/** Name plus the selection mark, so selection never rests on color alone. */
@Composable
private fun ThemeTileName(mode: ThemeMode, selected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = mode.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = Spacing.xs)
                    .size(16.dp) // touch-target-ok: selection glyph inside the selectable tile
            )
        }
    }
}

/**
 * A miniature page: three text lines and one accent mark drawn in the scheme's
 * own tokens. Geometry only, no type, so a large system font scale cannot
 * distort or clip the preview.
 */
@Composable
private fun ThemePreviewCanvas(
    preview: ColorScheme,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(preview.surface)
            .border(
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                ),
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Padding.compact),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(preview.onSurface.copy(alpha = 0.85f))
            )
            Box(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(preview.onSurface.copy(alpha = 0.45f))
            )
            Box(
                Modifier
                    .fillMaxWidth(0.62f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(preview.onSurface.copy(alpha = 0.45f))
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .width(24.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(preview.primary)
            )
        }
    }
}

@Composable
private fun SyncSection() {
    val context = LocalContext.current
    // Touch the container once so saved settings land in the store before the
    // row summary reads it; otherwise a fresh process reports "Not set up".
    remember { context.appContainer.koreaderSyncConfigStore }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var configVersion by remember { mutableIntStateOf(0) }
    val stored = remember(configVersion) { KoreaderSyncConfigStore.config }
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("Progress sync")
        SettingsRow(
            title = "KOReader-compatible server",
            summary = koreaderSyncSummary(stored),
            onClick = { expanded = !expanded },
            trailing = {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        AnimatedVisibility(visible = expanded) {
            KoreaderSyncForm(onConfigChanged = { configVersion++ })
        }
    }
}

@Composable
private fun ExportSection(notesExporter: NotesExporter) {
    val scope = rememberCoroutineScope()
    var batchData by remember { mutableStateOf<List<BookExportData>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("Your data")
        SettingsRow(
            title = "Export notes & highlights",
            summary = when {
                isLoading -> "Preparing\u2026"
                errorMessage != null ->
                    "Could not read your notes: $errorMessage. Tap to try again."
                else -> "Markdown, JSON, Obsidian vault, Readwise CSV"
            },
            onClick = {
                if (!isLoading) {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            batchData = notesExporter.getAllExportData(includeEmptyBooks = false)
                            showExportSheet = true
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "unknown error"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            trailing = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), // touch-target-ok: status indicator inside the row's 56dp target
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    if (showExportSheet && batchData != null) {
        NotesExportSheet(
            notesExporter = notesExporter,
            batchBooksData = batchData,
            onDismiss = {
                showExportSheet = false
                batchData = null
            }
        )
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("About")
        Text("Leafline $versionName", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "All reading data stays on this device. No analytics, crash reporting, or telemetry.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

/** Outcome of a save or connection test, shown beside an icon so it never relies on colour. */
private data class SyncStatus(val message: String, val success: Boolean)

/**
 * The KOReader sync form, collapsed behind its summary row until opened. The
 * first paragraph of the old standalone section now lives in the row summary.
 */
/**
 * Opt-in Hardcover sync.
 *
 * Two ways in: "Connect" runs the OAuth flow, which is the normal path and keeps a
 * refresh token so the connection does not expire mid-read; pasting a personal API
 * token stays available underneath for anyone who already has one, or for builds
 * with no OAuth app registered.
 *
 * Neither path sends anything on its own: the switch is the opt-in.
 */
@Composable
private fun HardcoverSection() {
    val context = LocalContext.current
    remember { context.appContainer.hardcoverConfigStore }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var configVersion by remember { mutableIntStateOf(0) }
    // Reading the process-wide counter here is what refreshes the row when the browser
    // hands the redirect back while this screen is already composed.
    val bumped = hardcoverConfigVersion.intValue
    val stored = remember(configVersion, bumped) {
        HardcoverConfigStore.config ?: context.appContainer.hardcoverConfigStore.load()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("Hardcover")
        SettingsRow(
            title = "Reading progress",
            summary = when {
                stored == null -> "Not set up"
                stored.needsReauthorization -> "Sign in again"
                !stored.enabled -> "Connected, paused"
                else -> "Sending progress"
            },
            onClick = { expanded = !expanded },
            trailing = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        AnimatedVisibility(visible = expanded) {
            HardcoverForm(onConfigChanged = { configVersion++ })
        }
    }
}

/**
 * Which books are allowed to reach Hardcover.
 *
 * "All books" is the default because switching sync on is already the consent;
 * asking about each book would be a second question the reader has effectively
 * answered. "Ask each book" exists for people who want to keep some out.
 */
/**
 * Asks once whether one book may be sent to Hardcover.
 *
 * Dismissing is deliberately not an answer: it opens the book and records nothing, so
 * an accidental tap does not silently opt a book out for good.
 */
@Composable
private fun HardcoverTrackingPrompt(
    onAnswer: (tracked: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send this book to Hardcover?") },
        text = {
            Text(
                "Leafline can send this book's reading status and page progress to your " +
                    "Hardcover account as you read it. The answer is remembered, and you " +
                    "can change it later in the book's details."
            )
        },
        confirmButton = {
            TextButton(onClick = { onAnswer(true) }) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = { onAnswer(false) }) { Text("Don't send") }
        }
    )
}

@Composable
private fun HardcoverTrackingModePicker(
    mode: HardcoverTrackingMode,
    onModeChange: (HardcoverTrackingMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            "Track books",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            val options = listOf(
                HardcoverTrackingMode.ALL_BOOKS to "All books",
                HardcoverTrackingMode.ASK_EACH_BOOK to "Ask each book",
                HardcoverTrackingMode.OFF to "Off"
            )
            options.forEach { (option, label) ->
                FilterChip(
                    selected = mode == option,
                    onClick = { onModeChange(option) },
                    label = { Text(label) }
                )
            }
        }
        Text(
            text = when (mode) {
                HardcoverTrackingMode.ALL_BOOKS ->
                    "Every book with a matching ISBN is sent as you read it."
                HardcoverTrackingMode.ASK_EACH_BOOK ->
                    "You are asked once for each book, and the answer is remembered. " +
                        "Change a book's answer in its details."
                HardcoverTrackingMode.OFF ->
                    "Nothing is sent. Your Hardcover connection is kept."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HardcoverForm(onConfigChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    remember { context.appContainer.hardcoverConfigStore }
    val coordinator = remember { context.appContainer.hardcoverOAuth }
    val bumped = hardcoverConfigVersion.intValue
    val stored = remember(bumped) {
        HardcoverConfigStore.config ?: context.appContainer.hardcoverConfigStore.load()
    }

    var token by remember { mutableStateOf(stored?.token.orEmpty()) }
    var tokenVisible by remember { mutableStateOf(false) }
    var showTokenEntry by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    fun persist(next: HardcoverConfig) {
        HardcoverConfigStore.config = next
        context.appContainer.hardcoverConfigStore.save(next)
        // The worker exits immediately when nothing is enabled, so scheduling is
        // harmless; cancelling here would stop KOReader sync too.
        SyncWorker.schedule(context)
        onConfigChanged()
    }

    val connected = stored != null && (stored.token.isNotBlank() || stored.needsReauthorization)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (coordinator.isConfigured) {
            if (!connected) {
                Text(
                    "Sign in to send your reading progress to Hardcover. Leafline asks only " +
                        "for your profile, your library, catalog lookups, and writing progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        val url = coordinator.startAuthorization()
                        if (url == null) {
                            status = "Hardcover sign-in is not configured in this build."
                        } else {
                            uriHandler.openUri(url)
                        }
                    },
                    enabled = !working
                ) {
                    Text("Connect to Hardcover")
                }
            } else {
                Text(
                    text = when {
                        stored!!.needsReauthorization -> "Hardcover needs you to sign in again."
                        stored.userId != null -> "Connected as Hardcover user ${stored.userId}."
                        else -> "Connected to Hardcover."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (stored.needsReauthorization) {
                        Button(
                            onClick = {
                                val url = coordinator.startAuthorization()
                                if (url == null) {
                                    status = "Hardcover sign-in is not configured in this build."
                                } else {
                                    uriHandler.openUri(url)
                                }
                            }
                        ) { Text("Sign in again") }
                    }
                    TextButton(
                        onClick = {
                            working = true
                            scope.launch {
                                try {
                                    coordinator.disconnect()
                                    token = ""
                                    status = "Disconnected from Hardcover"
                                    onConfigChanged()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    status = e.message ?: "Could not disconnect"
                                } finally {
                                    working = false
                                }
                            }
                        },
                        enabled = !working
                    ) { Text("Disconnect") }
                }
            }
        } else {
            Text(
                "This build has no Hardcover OAuth app, so sign-in is unavailable. " +
                    "You can still connect with a personal API token below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (connected && stored != null && !stored.needsReauthorization) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Send reading progress", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = stored.enabled,
                    onCheckedChange = { next ->
                        persist(stored.copy(enabled = next))
                        status = if (next) "Sending progress" else "Paused"
                    }
                )
            }
            if (stored.enabled) {
                HardcoverTrackingModePicker(
                    mode = context.appContainer.hardcoverTracking.mode(),
                    onModeChange = { mode ->
                        context.appContainer.hardcoverTracking.setMode(mode)
                        onConfigChanged()
                    }
                )
            }
        }

        TextButton(onClick = { showTokenEntry = !showTokenEntry }) {
            Text(if (showTokenEntry) "Hide API token option" else "Use an API token instead")
        }

        if (showTokenEntry) {
            Text(
                "Create a token at hardcover.app/account/api with the read:me, read:library " +
                    "and write:library scopes. It is stored encrypted on this device and is " +
                    "only used when you turn sync on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LeaflineTextField(
                value = token,
                onValueChange = {
                    token = it
                    status = null
                },
                label = "API token",
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (tokenVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                        Icon(
                            imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (tokenVisible) "Hide token" else "Show token"
                        )
                    }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(
                    onClick = {
                        val clean = token.trim()
                        if (clean.isEmpty() || working) return@Button
                        working = true
                        status = null
                        scope.launch {
                            try {
                                val userId = context.appContainer.hardcoverApi.userId(clean)
                                // Saving a token leaves the switch as it was, so the
                                // integration still only runs when the reader turns it on.
                                persist(
                                    HardcoverConfig.apiToken(
                                        token = clean,
                                        userId = userId,
                                        enabled = stored?.enabled ?: false
                                    )
                                )
                                status = "Connected as Hardcover user $userId"
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                status = e.message ?: "Could not reach Hardcover"
                            } finally {
                                working = false
                            }
                        }
                    },
                    enabled = token.isNotBlank() && !working
                ) {
                    Text(if (working) "Checking\u2026" else "Test and save")
                }
            }
        }

        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KoreaderSyncForm(onConfigChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Touch the container once so saved settings land in the store before the fields
    // initialise; otherwise a fresh process shows empty fields.
    remember { context.appContainer.koreaderSyncConfigStore }
    val stored = KoreaderSyncConfigStore.config
    var serverUrl by remember { mutableStateOf(stored?.serverUrl.orEmpty()) }
    var syncUsername by remember { mutableStateOf(stored?.username.orEmpty()) }
    var syncPassword by remember { mutableStateOf(stored?.password.orEmpty()) }
    var enabled by remember { mutableStateOf(stored?.enabled ?: false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<SyncStatus?>(null) }

    fun currentConfig() = KoreaderSyncConfig(
        serverUrl = serverUrl.trim(),
        username = syncUsername.trim(),
        password = syncPassword,
        enabled = enabled
    )

    fun applySyncConfig(config: KoreaderSyncConfig) {
        KoreaderSyncConfigStore.config = config
        context.appContainer.koreaderSyncConfigStore.save(config)
        if (config.enabled) SyncWorker.schedule(context) else SyncWorker.cancel(context)
        onConfigChanged()
    }

    // Save writes what the reader typed, so the button stays off until the
    // address is one this app can actually reach.
    val urlValid = isKoreaderServerUrlValid(serverUrl)
    val showUrlError = serverUrl.isNotBlank() && !urlValid

    fun testConnection() {
        testing = true
        status = null
        scope.launch {
            status = try {
                val authorized = context.appContainer.koreaderSyncApi
                    .auth(serverUrl.trim(), syncUsername.trim(), syncPassword)
                if (authorized) {
                    SyncStatus("Connection OK", success = true)
                } else {
                    SyncStatus("Authentication failed", success = false)
                }
            } catch (e: Exception) {
                SyncStatus("Could not reach the server: ${e.message}", success = false)
            }
            testing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs)
    ) {
        FormPanel {
            LeaflineTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it; status = null },
                label = "Server URL",
                placeholder = "http://server:6061/api/koreader",
                keyboardType = KeyboardType.Uri,
                isError = showUrlError,
                supportingText = if (showUrlError) {
                    "Enter the full address, starting with http:// or https://"
                } else {
                    null
                }
            )
            LeaflineTextField(
                value = syncUsername,
                onValueChange = { syncUsername = it; status = null },
                label = "Username"
            )
            LeaflineTextField(
                value = syncPassword,
                onValueChange = { syncPassword = it; status = null },
                label = "Password",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        )
                    }
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = enabled,
                        // An invalid field must not strand a syncing config: turning
                        // sync off stays available even while the address is broken.
                        enabled = urlValid || enabled,
                        role = Role.Switch,
                        onValueChange = { checked ->
                            enabled = checked
                            applySyncConfig(currentConfig().copy(enabled = checked))
                            status = null
                        }
                    )
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sync in the background", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (urlValid || enabled) {
                            "Checks for newer positions automatically."
                        } else {
                            "Set a server URL first."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = null)
            }
            status?.let { current ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Icon(
                        imageVector = if (current.success) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.ErrorOutline
                        },
                        contentDescription = null,
                        tint = if (current.success) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(IconSize.small) // touch-target-ok: status glyph
                    )
                    Text(
                        current.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (current.success) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs)
            ) {
                OutlinedButton(
                    onClick = { testConnection() },
                    enabled = !testing,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (testing) "Testing\u2026" else "Test connection") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        applySyncConfig(currentConfig())
                        status = SyncStatus("Saved", success = true)
                    },
                    enabled = urlValid,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Save") }
            }
        }
        Text(
            "Credentials stay on this device; the password is encrypted before it is stored.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}
