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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdluu.leafline.library.LeaflineDependencyHolder
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.LibraryViewModel
import com.jdluu.leafline.library.LibraryViewModelFactory
import com.jdluu.leafline.opds.OpdsBrowseScreen
import com.jdluu.leafline.opds.OpdsConfigStore
import com.jdluu.leafline.opds.OpdsServerConfig
import com.jdluu.leafline.opds.OpdsViewModel
import kotlinx.coroutines.launch

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
        factory = LibraryViewModelFactory(repository)
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

    MaterialTheme {
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
                    LeaflineTab.Library -> LibraryTab(
                        viewModel = libraryViewModel,
                        onImportEpub = { importEpubLauncher.launch(arrayOf(MainActivity.EPUB_MIME_TYPE)) },
                        onOpenBook = { book ->
                            activity.startActivity(
                                ReaderActivity.newIntent(activity, book.filePath)
                            )
                        }
                    )
                    LeaflineTab.Catalog -> OpdsBrowseScreen(
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
                    LeaflineTab.Settings -> SettingsTab()
                    null -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTab(
    viewModel: LibraryViewModel,
    onImportEpub: () -> Unit,
    onOpenBook: (LibraryBook) -> Unit
) {
    val books by viewModel.books.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Library") }
        )
        Button(
            onClick = onImportEpub,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Import EPUB")
        }
        if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No books imported yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(books) { book ->
                    BookItem(book = book, onClick = { onOpenBook(book) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") }
        )
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "OPDS Catalog",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Configure your OPDS server in the Catalog tab.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Reader defaults will be available in a future update.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun BookItem(book: LibraryBook, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(book.title, style = MaterialTheme.typography.titleMedium)
            if (book.authors.isNotEmpty()) {
                Text(
                    book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (book.description != null) {
                Text(
                    book.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
