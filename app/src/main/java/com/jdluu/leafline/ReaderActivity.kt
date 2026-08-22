package com.jdluu.leafline

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

class ReaderActivity : FragmentActivity() {
    
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
        
        private fun openEpub(activity: FragmentActivity, epubFile: File) {
            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(
                contentResolver = activity.contentResolver,
                httpClient = httpClient
            )
            val publicationOpener = PublicationOpener(
                publicationParser = DefaultPublicationParser(
                    activity,
                    httpClient = httpClient,
                    assetRetriever = assetRetriever,
                    pdfFactory = null
                )
            )
            
            val asset = runBlocking {
                assetRetriever.retrieve(epubFile).getOrElse { error -> 
                    throw RuntimeException("Failed to retrieve asset: $error") 
                }
            }
            
            val publication = runBlocking {
                publicationOpener.open(asset, allowUserInteraction = true).getOrElse { error ->
                    throw RuntimeException("Failed to open publication: $error")
                }
            }
            
            val navigatorFactory = EpubNavigatorFactory(publication)
            
            val fragmentFactory = navigatorFactory.createFragmentFactory(
                initialLocator = null,
                listener = null
            )
            
            val fm = activity.supportFragmentManager
            fm.fragmentFactory = fragmentFactory
            
            fm.commit {
                add(R.id.navigator_container, EpubNavigatorFragment::class.java, null, NAVIGATOR_TAG)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        
        val epubFile = File(filesDir, EPUB_FILE_NAME)
        copyEpubFromAssets(this, epubFile)
        openEpub(this, epubFile)
    }
}