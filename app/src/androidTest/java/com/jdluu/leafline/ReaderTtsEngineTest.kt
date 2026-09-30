package com.jdluu.leafline

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/**
 * On-device verification of the text-to-speech integration (#153).
 *
 * Opens a real bundled EPUB through the same Readium stack the reader uses and
 * asserts that the Readium 3.3.0 TTS navigator factory accepts it on the real
 * device, then creates a navigator. This is the riskiest part of the feature:
 * the reader only offers TTS controls when the factory accepts the publication
 * ([com.jdluu.leafline.reader.tts.ReaderTtsController] would otherwise report
 * UNAVAILABLE), so a passing run proves the integration works end to end on
 * device rather than only compiling.
 *
 * Audio output itself is not asserted; the controller's lifecycle and
 * unavailable-engine behaviour are covered by the JVM unit tests.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ReaderTtsEngineTest {

    @Test
    fun readiumTtsAcceptsRealEpubOnDevice() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val context = instr.targetContext
        val epub = ReaderTestHelper.copyAssetToPrivateStorage("test-book.epub", "reader-tts-engine-test.epub")

        try {
            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
            val publicationOpener = PublicationOpener(
                DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory = null)
            )

            val publication = runBlocking {
                val asset = assetRetriever.retrieve(epub)
                    .getOrElse { throw AssertionError("Failed to retrieve EPUB asset: $it") }
                publicationOpener.open(asset, allowUserInteraction = true)
                    .getOrElse { throw AssertionError("Failed to open publication: $it") }
            }

            val factory = AndroidTtsNavigatorFactory(context.applicationContext as Application, publication)
            assertNotNull(
                "Readium TTS should accept a standard EPUB on this device",
                factory
            )

            val navigator = runBlocking {
                factory!!.createNavigator(
                    listener = object : TtsNavigator.Listener {
                        override fun onStopRequested() = Unit
                    }
                ).getOrNull()
            }
            assertNotNull(
                "Readium TTS navigator should be creatable for a standard EPUB on this device",
                navigator
            )
            navigator!!.close()
        } finally {
            ReaderTestHelper.cleanupFile(epub)
        }
    }
}
