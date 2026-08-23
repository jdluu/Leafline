package com.jdluu.leafline

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
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

        val initialLocator: org.readium.r2.shared.publication.Locator? = savedLocatorJson?.let { json ->
            try {
                org.readium.r2.shared.publication.Locator.Companion.fromJSON(
                    org.json.JSONObject(json)
                )
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

            if (!usedImportedFile) {
                Toast.makeText(this, "Opened bundled EPUB", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open publication", e)
            Toast.makeText(this, "Failed to open EPUB: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
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
