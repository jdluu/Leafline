package com.jdluu.leafline.reader.tts

import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TtsMediaSessionManagerTest {

    private lateinit var context: Context
    private lateinit var session: MediaSession

    private var playCalls = 0
    private var pauseCalls = 0
    private var skipNextCalls = 0
    private var skipPreviousCalls = 0
    private var stopCalls = 0

    private lateinit var manager: TtsMediaSessionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        session = MediaSession(context, "TestSession")

        playCalls = 0
        pauseCalls = 0
        skipNextCalls = 0
        skipPreviousCalls = 0
        stopCalls = 0

        manager = TtsMediaSessionManager(
            session = session,
            onPlay = { playCalls++ },
            onPause = { pauseCalls++ },
            onSkipNext = { skipNextCalls++ },
            onSkipPrevious = { skipPreviousCalls++ },
            onStop = { stopCalls++ }
        )
    }

    @Test
    fun `callback methods trigger corresponding listener invocations`() {
        manager.callback.onPlay()
        assertEquals(1, playCalls)

        manager.callback.onPause()
        assertEquals(1, pauseCalls)

        manager.callback.onSkipToNext()
        assertEquals(1, skipNextCalls)

        manager.callback.onSkipToPrevious()
        assertEquals(1, skipPreviousCalls)

        manager.callback.onStop()
        assertEquals(1, stopCalls)
    }

    @Test
    fun `onMediaButtonEvent routes key codes to transport actions`() {
        fun mediaButtonIntent(keyCode: Int): Intent {
            return Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
        }

        manager.callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PLAY))
        assertEquals(1, playCalls)

        manager.callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PAUSE))
        assertEquals(1, pauseCalls)

        manager.callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_NEXT))
        assertEquals(1, skipNextCalls)

        manager.callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        assertEquals(1, skipPreviousCalls)

        manager.callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_STOP))
        assertEquals(1, stopCalls)

        // Toggling when not playing -> onPlay called
        manager.callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertEquals(2, playCalls)

        // When playing -> onPause called
        manager.updatePlaybackState(ReaderTtsState.PLAYING)
        manager.callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_HEADSETHOOK))
        assertEquals(2, pauseCalls)
    }

    @Test
    fun `updatePlaybackState sets active flag and playback state correctly`() {
        manager.updatePlaybackState(ReaderTtsState.PLAYING, 1.25f)
        assertTrue(session.isActive)
        val playingState = manager.currentPlaybackState
        assertEquals(PlaybackState.STATE_PLAYING, playingState?.state)
        assertEquals(1.25f, playingState?.playbackSpeed ?: 0f, 0.001f)

        manager.updatePlaybackState(ReaderTtsState.PAUSED)
        assertTrue(session.isActive)
        val pausedState = manager.currentPlaybackState
        assertEquals(PlaybackState.STATE_PAUSED, pausedState?.state)

        manager.updatePlaybackState(ReaderTtsState.IDLE)
        assertFalse(session.isActive)
        val stoppedState = manager.currentPlaybackState
        assertEquals(PlaybackState.STATE_STOPPED, stoppedState?.state)
    }

    @Test
    fun `updateMetadata populates title, author, and album fields`() {
        manager.updateMetadata(
            bookTitle = "Dune",
            author = "Frank Herbert",
            chapterTitle = "Chapter 1"
        )

        val metadata = manager.currentMetadata
        assertEquals("Chapter 1", metadata?.getString(MediaMetadata.METADATA_KEY_TITLE))
        assertEquals("Dune", metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM))
        assertEquals("Frank Herbert", metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST))
    }

    @Test
    fun `release deactivates session`() {
        manager.updatePlaybackState(ReaderTtsState.PLAYING)
        assertTrue(session.isActive)

        manager.release()
        assertFalse(session.isActive)
    }
}
