package com.jdluu.leafline

import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.jdluu.leafline.library.SafFolderMetadata
import com.jdluu.leafline.library.cover.EpubCoverLoader
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
import com.jdluu.leafline.theme.AppearanceStore
import com.jdluu.leafline.theme.FormPanel
import com.jdluu.leafline.theme.IconSize
import com.jdluu.leafline.theme.LeaflineTextField
import com.jdluu.leafline.theme.LeaflineTheme
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.theme.ThemeMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.Switch

private const val TAG = "MainActivity"

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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleToast()
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
            val imported = mutableListOf<LibraryBook>()
            var failures = 0
            LocalEpubDocumentScanner.scanTree(contentResolver, uri).forEach { documentUri ->
                val book = importEpub(documentUri, context) { failures++ }
                if (book != null) imported += book
            }
            withContext(Dispatchers.Main) {
                onComplete(imported, failures)
            }
        }
    }

    fun importEpub(contentUri: Uri, context: Context, onFailure: (String) -> Unit): LibraryBook? {
        try {
            val fileName = getFileName(context, contentUri) ?: "imported.epub"
            val sanitizedName = sanitizeFileName(fileName)
            val targetFile = java.io.File(context.filesDir, sanitizedName)

            val inputStream = context.contentResolver.openInputStream(contentUri) ?: run {
                onFailure("No data available")
                return null
            }

            val outputStream = targetFile.outputStream()
            try {
                inputStream.copyTo(outputStream)
            } finally {
                outputStream.close()
            }

            if (targetFile.length() == 0L) {
                targetFile.delete()
                onFailure("Empty file cannot be imported")
                return null
            }

            val fileHash = FileHashUtil.computeSha256(targetFile)

            val libraryBook = context.appContainer.epubImporter.importEpub(targetFile, fileHash)

            return libraryBook
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy EPUB", e)
            onFailure("Failed to import EPUB: ${e.message}")
            return null
        }
    }

    /**
     * Bulk-import multiple EPUBs from SAF content URIs.
     *
     * @return Pair(successCount, failureCount)
     */
    fun importMultipleEpub(
        contentUris: List<Uri>,
        context: Context,
        onResult: (LibraryBook) -> Unit,
        onError: (String) -> Unit
    ): Pair<Int, Int> {
        var successCount = 0
        var failureCount = 0
        for (uri in contentUris) {
            val result = importEpub(uri, context) { msg ->
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
            coverLoader = EpubCoverLoader(activity.applicationContext),
            sortStore = LibrarySortStore.fromContext(activity),
            readingSessionRepository = activity.appContainer.readingSessionRepository,
            layoutStore = LibraryLayoutStore.fromContext(activity),
            readingGoalStore = ReadingGoalStore.fromContext(activity)
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
                    scope = (activity as MainActivity).lifecycleScope
                )
            },
            libraryRepository = activity.appContainer.libraryRepository
        )
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val appearanceStore = remember { AppearanceStore.fromContext(context) }
    var appearance by remember { mutableStateOf(appearanceStore.load()) }

    var pendingBook by remember { mutableStateOf<LibraryBook?>(null) }

    LaunchedEffect(pendingBook) {
        pendingBook?.let { book ->
            libraryViewModel.addBook(book)
            (activity as MainActivity).showToast("Imported: ${book.title}")
            pendingBook = null
        }
    }

    val importEpubLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        uris?.let { uriList ->
            if (uriList.isEmpty()) return@let
            var successCount = 0
            var failureCount = 0
            val imported = mutableListOf<LibraryBook>()
            for (uri in uriList) {
                val result = (activity as MainActivity).importEpub(uri, context) { message ->
                    activity.showToast(message)
                    failureCount++
                }
                if (result != null) {
                    successCount++
                    imported.add(result)
                }
            }
            // Add all successfully imported books to the ViewModel
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
            (activity as MainActivity).saveLibraryFolder(selectedUri, context)
            (activity as MainActivity).importLibraryFolder(selectedUri, context) { imported, failures ->
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
                        onOpenBook = { book ->
                            activity.startActivity(
                                ReaderActivity.newIntent(activity, book.filePath)
                            )
                            @Suppress("DEPRECATION")
                            activity.overridePendingTransition(
                                android.R.anim.fade_in, 0
                            )
                        }
                    )
                    LeaflineTab.Explore -> OpdsScreen(
                        viewModel = opdsViewModel,
                        onOpenBook = { book ->
                            activity.startActivity(
                                ReaderActivity.newIntent(activity, book.filePath)
                            )
                            @Suppress("DEPRECATION")
                            activity.overridePendingTransition(
                                android.R.anim.fade_in, 0
                            )
                        }
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
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState())
        ) {
            AppearanceSection(
                appearance = appearance,
                onAppearanceChange = onAppearanceChange,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
            NotesExportSection(
                notesExporter = notesExporter,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
            KoreaderSyncSection(modifier = Modifier.fillMaxWidth())
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
            AboutFooter()
        }
    }
}

/**
 * Version and privacy footer. Support and bug reports need the installed build, and the
 * on-device promise is worth restating where the sync fields live.
 */
@Composable
private fun AboutFooter() {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Leafline $versionName",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "All reading data stays on this device. No analytics, crash reporting, or telemetry.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

@Composable
private fun NotesExportSection(
    notesExporter: NotesExporter?,
    modifier: Modifier = Modifier
) {
    if (notesExporter == null) return
    val scope = rememberCoroutineScope()
    var batchData by remember { mutableStateOf<List<BookExportData>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Notes & Data Portability",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Spacing.xs)
        )
        Text(
            "Export highlights, notes, and reading metadata to Obsidian Markdown, structured JSON, or an Obsidian Vault archive (.zip).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )
        Button(
            onClick = {
                isLoading = true
                scope.launch {
                    try {
                        batchData = notesExporter.getAllExportData(includeEmptyBooks = false)
                        showExportSheet = true
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp) // touch-target-ok: button decorative icon
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(if (isLoading) "Loading notes…" else "Export all notes & highlights")
        }
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

/**
 * App appearance selector: System follows the OS dark/light setting with the
 * fixed Leafline palette, Light and Dark pick the Leafline schemes directly,
 * OLED uses the pure-black dark scheme, and E-ink uses the high-contrast
 * monochrome scheme. Selections are applied immediately and persisted by
 * [AppearanceStore].
 */
@Composable
private fun AppearanceSection(
    appearance: ThemeMode,
    onAppearanceChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Appearance",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = appearance == mode,
                    onClick = { onAppearanceChange(mode) },
                    label = { Text(mode.label) },
                    modifier = Modifier.padding(end = Spacing.xs, bottom = Spacing.xs)
                )
            }
        }
        Text(
            text = appearance.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

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

/** Outcome of a save or connection test, shown beside an icon so it never relies on colour. */
private data class SyncStatus(val message: String, val success: Boolean)

@Composable
internal fun KoreaderSyncSection(modifier: Modifier = Modifier) {
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
    }

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

    Column(modifier = modifier.fillMaxWidth()) {
        Text("KOReader Sync", style = MaterialTheme.typography.titleMedium)
        Text(
            "Keep your place across devices with a KOReader-compatible server.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm)
        )
        FormPanel {
            LeaflineTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it; status = null },
                label = "Server URL",
                placeholder = "http://server:6061/api/koreader",
                keyboardType = KeyboardType.Uri
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
                        "Checks for newer positions automatically.",
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
                ) { Text(if (testing) "Testing…" else "Test connection") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        applySyncConfig(currentConfig())
                        status = SyncStatus("Saved", success = true)
                    },
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
