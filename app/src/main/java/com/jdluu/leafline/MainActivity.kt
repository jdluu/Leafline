package com.jdluu.leafline

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdluu.leafline.di.appContainer
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.LibraryScreen
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.LibraryViewModelFactory
import com.jdluu.leafline.library.cover.EpubCoverLoader
import com.jdluu.leafline.sync.KoreaderSyncConfig
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import com.jdluu.leafline.sync.SyncWorker
import com.jdluu.leafline.theme.AppearanceStore
import com.jdluu.leafline.theme.LeaflineTheme
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.theme.ThemeMode
import kotlinx.coroutines.launch
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
    Settings("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaflineApp(activity: MainActivity) {
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(
            repository = activity.appContainer.libraryRepository,
            coverLoader = EpubCoverLoader(activity.applicationContext),
            sortStore = LibrarySortStore.fromContext(activity)
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
                                        LeaflineTab.Library -> Icons.Default.LibraryBooks
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
                        onImportEpub = { importEpubLauncher.launch(arrayOf(MainActivity.EPUB_MIME_TYPE)) },
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
                        }
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
    onAppearanceChange: (ThemeMode) -> Unit
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
            KoreaderSyncSection(modifier = Modifier.fillMaxWidth())
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KoreaderSyncSection(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf(KoreaderSyncConfigStore.config?.serverUrl ?: "") }
    var syncUsername by remember { mutableStateOf(KoreaderSyncConfigStore.config?.username ?: "") }
    var syncPassword by remember { mutableStateOf(KoreaderSyncConfigStore.config?.password ?: "") }
    var enabled by remember { mutableStateOf(KoreaderSyncConfigStore.config?.enabled ?: false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "KOReader Sync",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it; testResult = null },
            label = { Text("Server URL") },
            placeholder = { Text("http://server:6061/api/koreader") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = syncUsername,
            onValueChange = { syncUsername = it; testResult = null },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = Padding.compact)
        )
        OutlinedTextField(
            value = syncPassword,
            onValueChange = { syncPassword = it; testResult = null },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = Padding.compact)
        )
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs)
        ) {
            Text(
                "Enable progress sync",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    enabled = checked
                    val base = KoreaderSyncConfigStore.config ?: KoreaderSyncConfig(
                        serverUrl = serverUrl.trim(),
                        username = syncUsername.trim(),
                        password = syncPassword,
                        enabled = false
                    )
                    KoreaderSyncConfigStore.config = base.copy(enabled = checked)
                    if (checked) {
                        SyncWorker.schedule(context)
                    } else {
                        SyncWorker.cancel(context)
                    }
                }
            )
        }
        Row(modifier = Modifier.padding(top = Spacing.sm)) {
            Button(
                onClick = {
                    val config = KoreaderSyncConfig(
                        serverUrl = serverUrl.trim(),
                        username = syncUsername.trim(),
                        password = syncPassword,
                        enabled = enabled
                    )
                    KoreaderSyncConfigStore.config = config
                    testResult = "Saved."
                }
            ) { Text("Save") }
            Button(
                onClick = {
                    testing = true
                    testResult = null
                    scope.launch {
                        testResult = try {
                            val ok = context.appContainer.koreaderSyncApi
                                .auth(serverUrl.trim(), syncUsername.trim(), syncPassword)
                            if (ok) "Connection OK" else "Authentication failed"
                        } catch (e: Exception) {
                            "Connection failed: ${e.message}"
                        }
                        testing = false
                    }
                },
                enabled = !testing,
                modifier = Modifier.padding(start = Padding.compact)
            ) { Text(if (testing) "Testing..." else "Test connection") }
        }
        testResult?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    message.startsWith("Connection OK") -> MaterialTheme.colorScheme.primary
                    message == "Saved." -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
        Text(
            "Syncs reading positions with a KOReader-compatible server using the book content hash. Credentials are stored only in memory for this session.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = Spacing.sm)
        )
    }
}
