package com.jdluu.leafline.reader.tts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level playback state for text-to-speech reading mode.
 */
enum class ReaderTtsState {
    IDLE,
    PLAYING,
    PAUSED,
    UNAVAILABLE,
    ERROR;

    val isPlaying: Boolean get() = this == PLAYING
    val isPaused: Boolean get() = this == PAUSED
    val isActive: Boolean get() = this == PLAYING || this == PAUSED
    val isAvailable: Boolean get() = this != UNAVAILABLE
}

/**
 * Seam for underlying TTS engine operations, allowing [ReaderTtsController]
 * to remain decoupled from Android and Readium types.
 */
interface TtsPlayerAdapter {
    /**
     * Start or resume playback.
     * @return true if playback started or was scheduled successfully, false otherwise.
     */
    fun play(): Boolean

    /**
     * Pause ongoing playback.
     */
    fun pause()

    /**
     * Stop playback and release or clear active resources.
     */
    fun stop()
}

/**
 * Owns TTS playback state and lifecycle transitions (idle, playing, paused, unavailable, error).
 *
 * The Readium navigator and Android-specific implementations are kept behind
 * injected callbacks so this controller is fully testable on a plain JVM.
 */
class ReaderTtsController(
    initialState: ReaderTtsState = ReaderTtsState.IDLE,
    private val onPlay: () -> Boolean,
    private val onPause: () -> Unit = {},
    private val onStop: () -> Unit = {},
    private val onError: (String?) -> Unit = {}
) {
    constructor(
        adapter: TtsPlayerAdapter,
        initialState: ReaderTtsState = ReaderTtsState.IDLE,
        onError: (String?) -> Unit = {}
    ) : this(
        initialState = initialState,
        onPlay = { adapter.play() },
        onPause = { adapter.pause() },
        onStop = { adapter.stop() },
        onError = onError
    )

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<ReaderTtsState> = _state.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    /**
     * Requests playback to start or resume.
     */
    fun play() {
        when (_state.value) {
            ReaderTtsState.UNAVAILABLE -> {
                val message = "Text-to-speech is unavailable"
                _lastErrorMessage.value = message
                onError(message)
            }
            ReaderTtsState.PLAYING -> {
                // Already playing; no-op to preserve idempotent behavior.
            }
            ReaderTtsState.PAUSED,
            ReaderTtsState.IDLE,
            ReaderTtsState.ERROR -> {
                val success = try {
                    onPlay()
                } catch (e: Exception) {
                    false
                }
                if (success) {
                    _lastErrorMessage.value = null
                    _state.value = ReaderTtsState.PLAYING
                } else {
                    val message = "Failed to start text-to-speech"
                    _lastErrorMessage.value = message
                    _state.value = ReaderTtsState.ERROR
                    onError(message)
                }
            }
        }
    }

    /**
     * Requests playback to pause. Only transitions if currently playing.
     */
    fun pause() {
        if (_state.value == ReaderTtsState.PLAYING) {
            onPause()
            _state.value = ReaderTtsState.PAUSED
        }
    }

    /**
     * Requests playback to stop and return to idle.
     */
    fun stop() {
        if (_state.value != ReaderTtsState.UNAVAILABLE) {
            onStop()
            _state.value = ReaderTtsState.IDLE
        }
    }

    /**
     * Called when the underlying engine encounters an error.
     */
    fun onEngineError(message: String? = null) {
        if (_state.value != ReaderTtsState.UNAVAILABLE) {
            onStop()
            _lastErrorMessage.value = message
            _state.value = ReaderTtsState.ERROR
            onError(message)
        }
    }

    /**
     * Called when playback finishes naturally (reaches end of publication).
     */
    fun onPlaybackEnded() {
        if (_state.value != ReaderTtsState.UNAVAILABLE) {
            onStop()
            _state.value = ReaderTtsState.IDLE
        }
    }

    /**
     * Called when playback is paused externally (e.g. audio focus loss or headset disconnect).
     */
    fun onExternalPause() {
        if (_state.value == ReaderTtsState.PLAYING) {
            _state.value = ReaderTtsState.PAUSED
        }
    }

    /**
     * Called when playback is resumed externally.
     */
    fun onExternalPlay() {
        if (_state.value == ReaderTtsState.PAUSED || _state.value == ReaderTtsState.IDLE) {
            _state.value = ReaderTtsState.PLAYING
        }
    }

    /**
     * Marks text-to-speech as unavailable for the current publication.
     */
    fun markUnavailable() {
        _state.value = ReaderTtsState.UNAVAILABLE
    }
}
