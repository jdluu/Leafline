package com.jdluu.leafline

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
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

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : FragmentActivity(), EpubNavigatorFragment.Listener {

    companion object {
        private const val EPUB_FILE_NAME = "leafline-spike.epub"
        private const val NAVIGATOR_TAG = "EpubNavigatorFragment"

        fun newIntent(context: Context): Intent {
            return Intent(context, ReaderActivity::class.java)
        }

        private fun copyEpubFromAssets(context: Context, targetFile: File) {
            if (!targetFile.exists()) {
                context.assets.open(EPUB_FILE_NAME).use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private var navigator: EpubNavigatorFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val epubFile = File(filesDir, EPUB_FILE_NAME)
        copyEpubFromAssets(this, epubFile)

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

        val publication = runBlocking {
            val asset = assetRetriever.retrieve(epubFile).getOrElse {
                throw RuntimeException("Failed to retrieve asset: $it")
            }
            publicationOpener.open(asset, allowUserInteraction = true).getOrElse {
                throw RuntimeException("Failed to open publication: $it")
            }
        }

        val navigatorFactory = EpubNavigatorFactory(publication)
        val fragmentFactory = navigatorFactory.createFragmentFactory(
            initialLocator = null,
            listener = this
        )

        supportFragmentManager.fragmentFactory = fragmentFactory

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_reader)

            supportFragmentManager.commitNow {
                add(R.id.navigator_container, EpubNavigatorFragment::class.java, null, NAVIGATOR_TAG)
            }

            navigator = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as EpubNavigatorFragment
        }
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        if (!url.isHttp) return
        val uri = Uri.parse(url.toString())
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }
}
