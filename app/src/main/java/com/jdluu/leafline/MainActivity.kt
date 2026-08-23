package com.jdluu.leafline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdluu.leafline.library.LeaflineDependencyHolder
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.LibraryScreen
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.LibraryViewModelFactory
import com.jdluu.leafline.library.cover.EpubCoverLoader
import com.jdluu.leafline.opds.OpdsBrowseScreen
import com.jdluu.leafline.opds.OpdsConfigStore
import com.jdluu.leafline.opds.OpdsServerConfig
import com.jdluu.leafline.opds.OpdsViewModel
import com.jdluu.leafline.sync.KoreaderSyncClient
import com.jdluu.leafline.sync.KoreaderSyncConfig
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import com.jdluu.leafline.theme.LeaflineTheme
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
    private var currentContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeaflineDependencyHolder.initialize(this)
        currentContext = this
        handleOpdsConfigIntent(intent)
        registerOpdsDebugReceiver()
        setContent { LeaflineApp(this) }
    }

    private fun handleOpdsConfigIntent(intent: Intent?) {
        val url = intent?.getStringExtra("opds_url") ?: return
        val username = intent.getStringExtra("opds_username") ?: ""
        val password = intent.getStringExtra("opds_password") ?: ""
        OpdsConfigStore.config = OpdsServerConfig(
            catalogUrl = url,
            username = username,
            password = password
        )
        showToast("OPDS config set")
    }

    private fun registerOpdsDebugReceiver() {
        val filter = IntentFilter("com.jdluu.leafline.OPDS_CONFIG")
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val url = intent.getStringExtra("url") ?: return
                val username = intent.getStringExtra("username") ?: ""
                val password = intent.getStringExtra("password") ?: ""
                OpdsConfigStore.config = OpdsServerConfig(
                    catalogUrl = url,
                    username = username,
                    password = password
                )
                showToast("OPDS config set via debug broadcast")
            }
        }, filter, Context.RECEIVER_EXPORTED)
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

            val importer = EpubImporter(context)
            val libraryBook = importer.importEpub(targetFile, fileHash)

            return libraryBook
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy EPUB", e)
            onFailure("Failed to import EPUB: ${e.message}")
            return null
        }
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
    Catalog("Catalog"),
    Settings("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaflineApp(activity: ComponentActivity) {
    val repository = LeaflineDependencyHolder.getRepository(activity)
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(
            repository = repository,
            coverLoader = EpubCoverLoader(activity.applicationContext),
            sortStore = LibrarySortStore.fromContext(activity)
        )
    )
    val opdsViewModel: OpdsViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var downloadMessage by remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    var pendingBook by remember { mutableStateOf<LibraryBook?>(null) }

    LaunchedEffect(downloadMessage) {
        downloadMessage?.let { message ->
            (activity as MainActivity).showToast(message)
            downloadMessage = null
        }
    }

    LaunchedEffect(pendingBook) {
        pendingBook?.let { book ->
            libraryViewModel.addBook(book)
            (activity as MainActivity).showToast("Imported: ${book.title}")
            pendingBook = null
        }
    }

    val importEpubLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val result = (activity as MainActivity).importEpub(it, context) { message ->
                activity.showToast(message)
            }
            if (result != null) {
                pendingBook = result
            }
        }
    }

    LeaflineTheme(
            mode = ThemeMode.SYSTEM,
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
                                        LeaflineTab.Catalog -> Icons.Default.CloudDownload
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
                        }
                    )
                    LeaflineTab.Catalog -> {
                        LaunchedEffect(selectedTab) {
                            opdsViewModel.loadIfConfigured()
                        }
                        OpdsBrowseScreen(
                        viewModel = opdsViewModel,
                        onBack = { selectedTab = 0 },
                        onDownload = { entry ->
                            val config = OpdsConfigStore.config
                            if (config == null) {
                                downloadMessage = "Configure the OPDS catalog first"
                            } else {
                                downloadMessage = "Downloading ${entry.title}..."
                                scope.launch {
                                    try {
                                        val book = OpdsDownloadCoordinator(context, repository)
                                            .downloadAndImport(config, entry)
                                        downloadMessage = if (book == null) {
                                            "Could not import ${entry.title}"
                                        } else {
                                            "Imported: ${book.title}"
                                        }
                                    } catch (error: Exception) {
                                        downloadMessage = "Download failed: ${error.message}"
                                    }
                                }
                            }
                        }
                    )
                    }
                    LeaflineTab.Settings -> SettingsTab()
                    null -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab() {
    var url by remember { mutableStateOf(OpdsConfigStore.config?.catalogUrl ?: "") }
    var username by remember { mutableStateOf(OpdsConfigStore.config?.username ?: "") }
    var password by remember { mutableStateOf(OpdsConfigStore.config?.password ?: "") }
    var saved by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") }
        )
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "OPDS Catalog",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; saved = false },
                label = { Text("Catalog URL") },
                placeholder = { Text("https://server/api/v1/opds") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; saved = false },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; saved = false },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            Button(
                onClick = {
                    OpdsConfigStore.config = OpdsServerConfig(
                        catalogUrl = url,
                        username = username,
                        password = password
                    )
                    saved = true
                },
                modifier = Modifier.padding(top = 16.dp)
            ) { Text("Save") }
            if (saved) {
                Text(
                    "Saved. Go to the Catalog tab to browse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                "Credentials are stored only in memory for this session.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )

            KoreaderSyncSection(modifier = Modifier.padding(top = 40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KoreaderSyncSection(modifier: Modifier = Modifier) {
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
            modifier = Modifier.padding(bottom = 16.dp)
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
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        OutlinedTextField(
            value = syncPassword,
            onValueChange = { syncPassword = it; testResult = null },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
                }
            )
        }
        Row(modifier = Modifier.padding(top = 16.dp)) {
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
                            val ok = KoreaderSyncClient()
                                .auth(serverUrl.trim(), syncUsername.trim(), syncPassword)
                            if (ok) "Connection OK" else "Authentication failed"
                        } catch (e: Exception) {
                            "Connection failed: ${e.message}"
                        }
                        testing = false
                    }
                },
                enabled = !testing,
                modifier = Modifier.padding(start = 12.dp)
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
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Text(
            "Syncs reading positions with a KOReader-compatible server using the book content hash. Credentials are stored only in memory for this session.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
