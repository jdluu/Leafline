package com.jdluu.leafline

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.navigator.media.common.MediaNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

/**
 * On-device verification of the text-to-speech integration (#153).
 *
 * Opens a real bundled EPUB through the same Readium stack the reader uses, then
 * checks that the Readium 3.3.0 TTS navigator accepts it and that playback
 * actually starts. This is the riskiest part of the feature: the reader only
 * offers TTS controls when the factory accepts the publication
 * ([com.jdluu.leafline.reader.tts.ReaderTtsController] would otherwise report
 * UNAVAILABLE), so a passing run proves the integration works on device rather
 * than merely compiling.
 *
 * Audible output is not asserted; the controller's lifecycle and
 * unavailable-engine behaviour are covered by the JVM unit tests.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ReaderTtsEngineTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun openTestPublication(epub: File): Publication {
        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
        val publicationOpener = PublicationOpener(
            DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory = null)
        )
        return runBlocking {
            val asset = assetRetriever.retrieve(epub)
                .getOrElse { throw AssertionError("Failed to retrieve EPUB asset: $it") }
            publicationOpener.open(asset, allowUserInteraction = true)
                .getOrElse { throw AssertionError("Failed to open publication: $it") }
        }
    }

    private fun createNavigator(publication: Publication): AndroidTtsNavigator {
        val factory = AndroidTtsNavigatorFactory(context.applicationContext as Application, publication)
        assertNotNull("Readium TTS should accept a standard EPUB on this device", factory)
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
        return navigator!!
    }

    @Test
    fun readiumTtsAcceptsRealEpubOnDevice() {
        val epub = ReaderTestHelper.copyAssetToPrivateStorage("test-book.epub", "reader-tts-engine-test.epub")
        try {
            createNavigator(openTestPublication(epub)).close()
        } finally {
            ReaderTestHelper.cleanupFile(epub)
        }
    }

    @Test
    fun readiumTtsPlaybackStartsOnDevice() {
        val epub = ReaderTestHelper.copyAssetToPrivateStorage("test-book.epub", "reader-tts-playback-test.epub")
        try {
            val navigator = createNavigator(openTestPublication(epub))

            navigator.play()

            // Playback must settle into Ready with playback requested, or fail
            // with a typed engine error. Reaching Ready proves the system TTS
            // engine accepted the utterance and the navigator is driving it.
            val settled = runBlocking {
                withTimeoutOrNull(30_000) {
                    navigator.playback.first { playback ->
                        (playback.playWhenReady && playback.state is MediaNavigator.State.Ready) ||
                            playback.state is MediaNavigator.State.Failure
                    }
                }
            }

            assertNotNull("TTS playback should settle within 30s", settled)
            assertTrue(
                "TTS playback should be Ready and playing, but was ${settled!!.state} " +
                    "(playWhenReady=${settled.playWhenReady})",
                settled.playWhenReady && settled.state is MediaNavigator.State.Ready
            )

            navigator.close()
        } finally {
            ReaderTestHelper.cleanupFile(epub)
        }
    }
}
