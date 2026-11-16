package com.jdluu.leafline.reader.tts

import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.annotation.VisibleForTesting
import androidx.core.content.IntentCompat

/**
 * Manages an Android [MediaSession] for text-to-speech reading mode.
 *
 * Exposes playback state (Playing, Paused, Stopped), transport actions (Play, Pause,
 * Skip Next, Skip Previous, Stop), and book metadata to system surfaces such as
 * the lockscreen, quick settings media player, and Bluetooth/headset media buttons.
 */
class TtsMediaSessionManager(
    private val session: MediaSession,
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit,
    private val onSkipNext: () -> Unit,
    private val onSkipPrevious: () -> Unit,
    private val onStop: () -> Unit
) {
    val sessionToken: MediaSession.Token
        get() = session.sessionToken

    var currentPlaybackState: PlaybackState? = null
        private set

    var currentMetadata: MediaMetadata? = null
        private set

    private var isPlaying: Boolean = false

    @VisibleForTesting
    internal val callback = object : MediaSession.Callback() {
        override fun onPlay() {
            this@TtsMediaSessionManager.onPlay()
        }

        override fun onPause() {
            this@TtsMediaSessionManager.onPause()
        }

        override fun onSkipToNext() {
            this@TtsMediaSessionManager.onSkipNext()
        }

        override fun onSkipToPrevious() {
            this@TtsMediaSessionManager.onSkipPrevious()
        }

        override fun onStop() {
            this@TtsMediaSessionManager.onStop()
        }

        override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
            val keyEvent = IntentCompat.getParcelableExtra(
                mediaButtonIntent,
                Intent.EXTRA_KEY_EVENT,
                KeyEvent::class.java
            )
            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_UP) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        this@TtsMediaSessionManager.onPlay()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        this@TtsMediaSessionManager.onPause()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_HEADSETHOOK -> {
                        // Toggle play/pause based on current playback state
                        if (isPlaying) {
                            this@TtsMediaSessionManager.onPause()
                        } else {
                            this@TtsMediaSessionManager.onPlay()
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        this@TtsMediaSessionManager.onSkipNext()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        this@TtsMediaSessionManager.onSkipPrevious()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                        this@TtsMediaSessionManager.onStop()
                        return true
                    }
                }
            }
            return super.onMediaButtonEvent(mediaButtonIntent)
        }
    }

    init {
        @Suppress("DEPRECATION")
        session.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        session.setCallback(callback)
    }

    /**
     * Synchronizes current TTS playback state and rate with the [MediaSession].
     */
    fun updatePlaybackState(state: ReaderTtsState, speed: Float = 1.0f) {
        val playbackStateInt = when (state) {
            ReaderTtsState.PLAYING -> PlaybackState.STATE_PLAYING
            ReaderTtsState.PAUSED -> PlaybackState.STATE_PAUSED
            ReaderTtsState.IDLE,
            ReaderTtsState.ERROR,
            ReaderTtsState.UNAVAILABLE -> PlaybackState.STATE_STOPPED
        }

        val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_STOP

        val pbState = PlaybackState.Builder()
            .setActions(actions)
            .setState(playbackStateInt, PlaybackState.PLAYBACK_POSITION_UNKNOWN, speed)
            .build()

        currentPlaybackState = pbState
        session.setPlaybackState(pbState)
        isPlaying = (state == ReaderTtsState.PLAYING)
        session.isActive = (state == ReaderTtsState.PLAYING || state == ReaderTtsState.PAUSED)
    }

    /**
     * Updates book metadata displayed on the lockscreen and remote devices.
     */
    fun updateMetadata(
        bookTitle: String,
        author: String? = null,
        chapterTitle: String? = null
    ) {
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, chapterTitle ?: bookTitle)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, bookTitle)
            .apply {
                if (!author.isNullOrBlank()) {
                    putString(MediaMetadata.METADATA_KEY_ARTIST, author)
                    putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, author)
                }
            }
            .build()

        currentMetadata = metadata
        session.setMetadata(metadata)
    }

    /**
     * Deactivates and releases the underlying [MediaSession].
     */
    fun release() {
        session.isActive = false
        session.release()
        currentPlaybackState = null
        currentMetadata = null
    }

    companion object {
        private const val SESSION_TAG = "LeaflineTtsSession"

        fun create(
            context: Context,
            onPlay: () -> Unit,
            onPause: () -> Unit,
            onSkipNext: () -> Unit,
            onSkipPrevious: () -> Unit,
            onStop: () -> Unit
        ): TtsMediaSessionManager {
            val session = MediaSession(context, SESSION_TAG)
            return TtsMediaSessionManager(
                session = session,
                onPlay = onPlay,
                onPause = onPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onStop = onStop
            )
        }
    }
}
