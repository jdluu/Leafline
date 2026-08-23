package com.jdluu.leafline

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.IOException

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : FragmentActivity(), EpubNavigatorFragment.Listener {

    companion object {
        private const val TAG = "ReaderActivity"
        private const val EPUB_FILE_NAME = "leafline-spike.epub"
        private const val NAVIGATOR_TAG = "EpubNavigatorFragment"
        private const val EXTRA_FILE_PATH = "extra_file_path"

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
    private var bookStableId: String? = null
    private var toolbarVisible = mutableStateOf(false)
    private var settingsSheetVisible = mutableStateOf(false)
    private var bookTitle = mutableStateOf("")
    private var tocLinks = mutableStateOf<List<Pair<Link, Int>>>(emptyList())
    private var currentPreferences = mutableStateOf(EpubPreferences())

    @OptIn(ExperimentalReadiumApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        if (usedImportedFile) {
            try {
                val lookupPath = epubFile!!.absolutePath
                val bookRecord = runBlocking {
                    com.jdluu.leafline.library.LeaflineDependencyHolder
                        .getRepository(this@ReaderActivity)
                        .getBookLocatorByFilePath(lookupPath)
                }
                if (bookRecord == null) {
                    Log.w(TAG, "No library book found for path: $lookupPath")
                }
                savedLocatorJson = bookRecord?.second
                bookStableId = bookRecord?.first
            } catch (e: Exception) {
                Log.w(TAG, "Could not load saved locator", e)
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

            val navigatorFactory = EpubNavigatorFactory(publication)
            val fragmentFactory = navigatorFactory.createFragmentFactory(
                initialLocator = initialLocator,
                listener = this
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
                        try {
                            com.jdluu.leafline.library.LeaflineDependencyHolder
                                .getRepository(this@ReaderActivity)
                                .saveLastLocator(stableId, locator.toJSON().toString())
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not save reading position", e)
                        }
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

    private fun submitPreferences(prefs: EpubPreferences) {
        currentPreferences.value = prefs
        navigator?.submitPreferences(prefs)
    }

    private fun addReaderOverlay() {
        val composeView = ComposeView(this).apply {
            setContent {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                ReaderOverlay(
                    title = bookTitle.value,
                    toolbarVisible = toolbarVisible.value,
                    tocLinks = tocLinks.value,
                    currentPreferences = currentPreferences.value,
                    drawerState = drawerState,
                    settingsSheetVisible = settingsSheetVisible.value,
                    onToggleToolbar = { toolbarVisible.value = !toolbarVisible.value },
                    onBack = { finish() },
                    onOpenToc = { scope.launch { drawerState.open() } },
                    onOpenSettings = { settingsSheetVisible.value = true },
                    onDismissSettings = { settingsSheetVisible.value = false },
                    onTocClick = { link ->
                        navigateToTocLink(link)
                        scope.launch { drawerState.close() }
                    },
                    onPreferencesChange = { prefs -> submitPreferences(prefs) }
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
    onToggleToolbar: () -> Unit,
    onBack: () -> Unit,
    onOpenToc: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onTocClick: (Link) -> Unit,
    onPreferencesChange: (EpubPreferences) -> Unit
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
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Reader settings")
                            }
                        }
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
        }
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
