package com.jdluu.leafline.reader.tts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.VisibleForTesting

/**
 * Manages audio focus requests and responses for text-to-speech reading.
 *
 * Responsibilities:
 * - Requests AUDIOFOCUS_GAIN with CONTENT_TYPE_SPEECH before starting playback.
 * - Handles transient loss (e.g., incoming phone call) with auto-resume when focus returns.
 * - Handles transient ducking (e.g., navigation directions or notifications) by reducing speech volume.
 * - Handles permanent loss by pausing playback and relinquishing focus.
 * - Monitors ACTION_AUDIO_BECOMING_NOISY to auto-pause when headphones or Bluetooth disconnect.
 */
class TtsAudioFocusManager(
    private val audioManager: AudioManager,
    private val onPausePlayback: () -> Unit,
    private val onResumePlayback: () -> Unit,
    private val onDuckingVolume: (Float) -> Unit = {},
    private val onRestoreVolume: () -> Unit = {}
) {
    var isDucked: Boolean = false
        private set

    var pausedDueToTransientLoss: Boolean = false
        private set

    private var hasFocus: Boolean = false

    @VisibleForTesting
    internal val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        handleAudioFocusChange(focusChange)
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    }

    /**
     * Requests audio focus.
     * @return true if focus was granted, false otherwise.
     */
    fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(focusRequest)
        hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        if (hasFocus) {
            pausedDueToTransientLoss = false
            if (isDucked) {
                isDucked = false
                onRestoreVolume()
            }
        }
        return hasFocus
    }

    /**
     * Abandons audio focus and resets transient tracking states.
     */
    fun abandonAudioFocus() {
        if (hasFocus) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            hasFocus = false
        }
        if (isDucked) {
            isDucked = false
            onRestoreVolume()
        }
        pausedDueToTransientLoss = false
    }

    @VisibleForTesting
    internal fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                if (isDucked) {
                    isDucked = false
                    onRestoreVolume()
                }
                if (pausedDueToTransientLoss) {
                    pausedDueToTransientLoss = false
                    onResumePlayback()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                isDucked = true
                onDuckingVolume(DUCK_VOLUME_RATIO)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pausedDueToTransientLoss = true
                if (isDucked) {
                    isDucked = false
                    onRestoreVolume()
                }
                onPausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausedDueToTransientLoss = false
                if (isDucked) {
                    isDucked = false
                    onRestoreVolume()
                }
                hasFocus = false
                onPausePlayback()
            }
        }
    }

    companion object {
        const val DUCK_VOLUME_RATIO = 0.2f

        fun fromContext(
            context: Context,
            onPausePlayback: () -> Unit,
            onResumePlayback: () -> Unit,
            onDuckingVolume: (Float) -> Unit = {},
            onRestoreVolume: () -> Unit = {}
        ): TtsAudioFocusManager {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return TtsAudioFocusManager(
                audioManager = audioManager,
                onPausePlayback = onPausePlayback,
                onResumePlayback = onResumePlayback,
                onDuckingVolume = onDuckingVolume,
                onRestoreVolume = onRestoreVolume
            )
        }
    }
}

/**
 * Listens for headphone/Bluetooth disconnection ([AudioManager.ACTION_AUDIO_BECOMING_NOISY])
 * and triggers an action (such as pausing speech playback).
 */
class AudioBecomingNoisyReceiver(
    private val onAudioBecomingNoisy: () -> Unit
) : BroadcastReceiver() {

    private var isRegistered = false

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            onAudioBecomingNoisy()
        }
    }

    fun register(context: Context) {
        if (!isRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(this, filter)
            isRegistered = true
        }
    }

    fun unregister(context: Context) {
        if (isRegistered) {
            try {
                context.unregisterReceiver(this)
            } catch (ignored: IllegalArgumentException) {
                // Receiver was not registered or already unregistered
            }
            isRegistered = false
        }
    }
}
