package com.jdluu.leafline.reader.tts

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAudioManager
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TtsAudioFocusManagerTest {

    private lateinit var context: Context
    private lateinit var audioManager: AudioManager
    private lateinit var shadowAudioManager: ShadowAudioManager

    private var pauseCallCount = 0
    private var resumeCallCount = 0
    private var duckingVolume: Float? = null
    private var restoreVolumeCallCount = 0

    private lateinit var focusManager: TtsAudioFocusManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        shadowAudioManager = Shadows.shadowOf(audioManager)

        pauseCallCount = 0
        resumeCallCount = 0
        duckingVolume = null
        restoreVolumeCallCount = 0

        focusManager = TtsAudioFocusManager(
            audioManager = audioManager,
            onPausePlayback = { pauseCallCount++ },
            onResumePlayback = { resumeCallCount++ },
            onDuckingVolume = { duckingVolume = it },
            onRestoreVolume = { restoreVolumeCallCount++ }
        )
    }

    @Test
    fun `requestAudioFocus requests focus and returns true when granted`() {
        val granted = focusManager.requestAudioFocus()
        assertTrue(granted)
        assertFalse(focusManager.isDucked)
        assertFalse(focusManager.pausedDueToTransientLoss)
    }

    @Test
    fun `AUDIOFOCUS_LOSS pauses playback and clears transient state`() {
        focusManager.requestAudioFocus()

        focusManager.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        assertEquals(1, pauseCallCount)
        assertEquals(0, resumeCallCount)
        assertFalse(focusManager.pausedDueToTransientLoss)
    }

    @Test
    fun `AUDIOFOCUS_LOSS_TRANSIENT pauses playback and records transient loss`() {
        focusManager.requestAudioFocus()

        focusManager.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        assertEquals(1, pauseCallCount)
        assertEquals(0, resumeCallCount)
        assertTrue(focusManager.pausedDueToTransientLoss)
    }

    @Test
    fun `AUDIOFOCUS_GAIN after transient loss resumes playback`() {
        focusManager.requestAudioFocus()

        focusManager.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertTrue(focusManager.pausedDueToTransientLoss)

        focusManager.handleAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(1, pauseCallCount)
        assertEquals(1, resumeCallCount)
        assertFalse(focusManager.pausedDueToTransientLoss)
    }

    @Test
    fun `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ducks volume and GAIN restores it`() {
        focusManager.requestAudioFocus()

        focusManager.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        assertTrue(focusManager.isDucked)
        assertEquals(0.2f, duckingVolume ?: 0f, 0.001f)
        assertEquals(0, restoreVolumeCallCount)

        focusManager.handleAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertFalse(focusManager.isDucked)
        assertEquals(1, restoreVolumeCallCount)
    }

    @Test
    fun `abandonAudioFocus resets state and restores volume if ducked`() {
        focusManager.requestAudioFocus()
        focusManager.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        assertTrue(focusManager.isDucked)

        focusManager.abandonAudioFocus()

        assertFalse(focusManager.isDucked)
        assertEquals(1, restoreVolumeCallCount)
        assertFalse(focusManager.pausedDueToTransientLoss)
    }

    @Test
    fun `AudioBecomingNoisyReceiver triggers callback when ACTION_AUDIO_BECOMING_NOISY is broadcast`() {
        var noisyCalled = false
        val receiver = AudioBecomingNoisyReceiver {
            noisyCalled = true
        }

        receiver.register(context)

        val noisyIntent = Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        context.sendBroadcast(noisyIntent)
        ShadowLooper.idleMainLooper()

        assertTrue(noisyCalled)

        // Unregister and ensure subsequent broadcasts are not handled
        receiver.unregister(context)
        noisyCalled = false
        context.sendBroadcast(noisyIntent)
        ShadowLooper.idleMainLooper()
        assertFalse(noisyCalled)
    }
}
