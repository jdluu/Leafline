package com.jdluu.leafline.reader.tts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    /**
     * Adjust playback rate (e.g. 0.75x to 2.0x).
     */
    fun setRate(rate: Float) {}

    /**
     * Adjust playback volume attenuation multiplier (e.g. 1.0f full down to 0.0f silent).
     */
    fun setVolume(volume: Float) {}

    /**
     * Skip to the previous sentence or utterance.
     */
    fun skipPrevious() {}

    /**
     * Skip to the next sentence or utterance.
     */
    fun skipNext() {}
}

/**
 * Owns TTS playback state, audio navigation, sleep timers, and lifecycle transitions
 * (idle, playing, paused, unavailable, error).
 *
 * The Readium navigator and Android-specific implementations are kept behind
 * injected callbacks so this controller is fully testable on a plain JVM.
 */
class ReaderTtsController(
    initialState: ReaderTtsState = ReaderTtsState.IDLE,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val onPlay: () -> Boolean,
    private val onPause: () -> Unit = {},
    private val onStop: () -> Unit = {},
    private val onSetRate: (Float) -> Unit = {},
    private val onSetVolume: (Float) -> Unit = {},
    private val onSkipPrevious: () -> Unit = {},
    private val onSkipNext: () -> Unit = {},
    private val onError: (String?) -> Unit = {}
) {
    constructor(
        adapter: TtsPlayerAdapter,
        initialState: ReaderTtsState = ReaderTtsState.IDLE,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        onError: (String?) -> Unit = {}
    ) : this(
        initialState = initialState,
        scope = scope,
        onPlay = { adapter.play() },
        onPause = { adapter.pause() },
        onStop = { adapter.stop() },
        onSetRate = { adapter.setRate(it) },
        onSetVolume = { adapter.setVolume(it) },
        onSkipPrevious = { adapter.skipPrevious() },
        onSkipNext = { adapter.skipNext() },
        onError = onError
    )

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<ReaderTtsState> = _state.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private val _rate = MutableStateFlow(1.0f)
    val rate: StateFlow<Float> = _rate.asStateFlow()

    private val _sleepState = MutableStateFlow(TtsSleepState())
    val sleepState: StateFlow<TtsSleepState> = _sleepState.asStateFlow()

    private var timerJob: Job? = null

    fun setRate(newRate: Float) {
        val clamped = newRate.coerceIn(0.5f, 3.0f)
        _rate.value = clamped
        onSetRate(clamped)
    }

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
     * Requests playback to stop, resets any active sleep timer, and returns to idle.
     */
    fun stop() {
        if (_state.value != ReaderTtsState.UNAVAILABLE) {
            timerJob?.cancel()
            timerJob = null
            onSetVolume(1.0f)
            _sleepState.value = TtsSleepState(option = TtsSleepOption.OFF)
            onStop()
            _state.value = ReaderTtsState.IDLE
        }
    }

    /**
     * Skips backward to the previous sentence or utterance.
     */
    fun skipPrevious() {
        if (_state.value.isActive) {
            onSkipPrevious()
        }
    }

    /**
     * Skips forward to the next sentence or utterance.
     */
    fun skipNext() {
        if (_state.value.isActive) {
            onSkipNext()
        }
    }

    /**
     * Configures the sleep timer with an option, optional current chapter href,
     * and optional fade duration in seconds (defaults to 30s).
     */
    fun setSleepTimer(
        option: TtsSleepOption,
        currentChapterHref: String? = null,
        fadeDurationSeconds: Int = 30
    ) {
        timerJob?.cancel()
        timerJob = null
        onSetVolume(1.0f)

        if (option == TtsSleepOption.OFF) {
            _sleepState.value = TtsSleepState(option = TtsSleepOption.OFF)
            return
        }

        if (option == TtsSleepOption.END_OF_CHAPTER) {
            _sleepState.value = TtsSleepState(
                option = TtsSleepOption.END_OF_CHAPTER,
                targetChapterHref = normalizeChapterHref(currentChapterHref),
                fadeDurationSeconds = fadeDurationSeconds
            )
            return
        }

        val minutes = option.minutes ?: return
        val totalSeconds = minutes * 60L
        _sleepState.value = TtsSleepState(
            option = option,
            remainingSeconds = totalSeconds,
            fadeDurationSeconds = fadeDurationSeconds,
            isFadingOut = false,
            volumeMultiplier = 1.0f
        )

        timerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                val shouldFade = remaining <= fadeDurationSeconds && fadeDurationSeconds > 0
                val multiplier = if (shouldFade) {
                    (remaining.toFloat() / fadeDurationSeconds.toFloat()).coerceIn(0.0f, 1.0f)
                } else {
                    1.0f
                }
                _sleepState.value = _sleepState.value.copy(
                    remainingSeconds = remaining,
                    isFadingOut = shouldFade,
                    volumeMultiplier = multiplier
                )
                if (shouldFade) {
                    onSetVolume(multiplier)
                }
            }
            _sleepState.value = TtsSleepState(option = TtsSleepOption.OFF)
            onSetVolume(1.0f)
            pause()
        }
    }

    /**
     * Cancels any active sleep timer.
     */
    fun cancelSleepTimer() {
        setSleepTimer(TtsSleepOption.OFF)
    }

    /**
     * Notifies the controller of the current locator/chapter href so the
     * [TtsSleepOption.END_OF_CHAPTER] timer can detect chapter boundaries.
     */
    fun onLocatorChanged(chapterHref: String?) {
        val current = _sleepState.value
        if (current.option != TtsSleepOption.END_OF_CHAPTER) return

        val normalized = normalizeChapterHref(chapterHref) ?: return
        if (current.targetChapterHref == null) {
            _sleepState.value = current.copy(targetChapterHref = normalized)
        } else if (normalized != current.targetChapterHref) {
            _sleepState.value = TtsSleepState(option = TtsSleepOption.OFF)
            pause()
        }
    }

    /**
     * Called when the underlying engine encounters an error.
     */
    fun onEngineError(message: String? = null) {
        if (_state.value != ReaderTtsState.UNAVAILABLE) {
            timerJob?.cancel()
            timerJob = null
            onSetVolume(1.0f)
            _sleepState.value = TtsSleepState(option = TtsSleepOption.OFF)
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
            timerJob?.cancel()
            timerJob = null
            onSetVolume(1.0f)
            _sleepState.value = TtsSleepState(option = TtsSleepOption.OFF)
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
