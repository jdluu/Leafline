package com.jdluu.leafline.reader.tts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager

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
