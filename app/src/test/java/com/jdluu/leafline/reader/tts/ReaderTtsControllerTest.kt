package com.jdluu.leafline.reader.tts

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain JVM unit tests for [ReaderTtsController], covering playback state transitions,
 * audio navigation, sleep timers, error handling, and the unavailable-engine fallback path.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReaderTtsControllerTest {

    private class Harness(
        initialState: ReaderTtsState = ReaderTtsState.IDLE,
        playResult: Boolean = true,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
    ) {
        val calls = mutableListOf<String>()
        val errorNotifications = mutableListOf<String?>()
        val rateChanges = mutableListOf<Float>()
        val volumeChanges = mutableListOf<Float>()
        var playReturns: Boolean = playResult
        var playShouldThrow: Boolean = false

        val controller = ReaderTtsController(
            initialState = initialState,
            scope = scope,
            onPlay = {
                calls += "play"
                if (playShouldThrow) throw IllegalStateException("Speech engine crashed")
                playReturns
            },
            onPause = { calls += "pause" },
            onStop = { calls += "stop" },
            onSetRate = { rateChanges += it },
            onSetVolume = { volumeChanges += it },
            onSkipPrevious = { calls += "skipPrevious" },
            onSkipNext = { calls += "skipNext" },
            onError = { errorNotifications += it }
        )
    }

    @Test
    fun `initial state is exposed by state flow as IDLE by default`() {
        val harness = Harness()
        assertEquals(ReaderTtsState.IDLE, harness.controller.state.value)
        assertNull(harness.controller.lastErrorMessage.value)
    }

    @Test
    fun `initial state can be UNAVAILABLE`() {
        val harness = Harness(initialState = ReaderTtsState.UNAVAILABLE)
        assertEquals(ReaderTtsState.UNAVAILABLE, harness.controller.state.value)
    }

    @Test
    fun `play transitions from IDLE to PLAYING when onPlay succeeds`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.state.test {
            assertEquals(ReaderTtsState.IDLE, awaitItem())

            harness.controller.play()

            assertEquals(ReaderTtsState.PLAYING, awaitItem())
        }
        assertEquals(listOf("play"), harness.calls)
        assertNull(harness.controller.lastErrorMessage.value)
    }

    @Test
    fun `play is idempotent when already PLAYING`() {
        val harness = Harness()
        harness.controller.play()
        assertEquals(listOf("play"), harness.calls)
        assertEquals(ReaderTtsState.PLAYING, harness.controller.state.value)

        harness.controller.play()
        assertEquals(listOf("play"), harness.calls)
        assertEquals(ReaderTtsState.PLAYING, harness.controller.state.value)
    }

    @Test
    fun `pause transitions from PLAYING to PAUSED`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()

        harness.controller.state.test {
            assertEquals(ReaderTtsState.PLAYING, awaitItem())

            harness.controller.pause()

            assertEquals(ReaderTtsState.PAUSED, awaitItem())
        }
        assertEquals(listOf("play", "pause"), harness.calls)
    }

    @Test
    fun `pause is no-op when IDLE or PAUSED`() {
        val harness = Harness()
        harness.controller.pause()
        assertEquals(emptyList<String>(), harness.calls)
        assertEquals(ReaderTtsState.IDLE, harness.controller.state.value)

        harness.controller.play()
        harness.controller.pause()
        assertEquals(listOf("play", "pause"), harness.calls)

        harness.controller.pause()
        assertEquals(listOf("play", "pause"), harness.calls)
        assertEquals(ReaderTtsState.PAUSED, harness.controller.state.value)
    }

    @Test
    fun `play transitions from PAUSED to PLAYING`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()
        harness.controller.pause()

        harness.controller.state.test {
            assertEquals(ReaderTtsState.PAUSED, awaitItem())

            harness.controller.play()

            assertEquals(ReaderTtsState.PLAYING, awaitItem())
        }
        assertEquals(listOf("play", "pause", "play"), harness.calls)
    }

    @Test
    fun `stop transitions from PLAYING to IDLE and calls onStop`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()

        harness.controller.state.test {
            assertEquals(ReaderTtsState.PLAYING, awaitItem())

            harness.controller.stop()

            assertEquals(ReaderTtsState.IDLE, awaitItem())
        }
        assertEquals(listOf("play", "stop"), harness.calls)
    }

    @Test
    fun `stop transitions from PAUSED to IDLE and calls onStop`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()
        harness.controller.pause()

        harness.controller.state.test {
            assertEquals(ReaderTtsState.PAUSED, awaitItem())

            harness.controller.stop()

            assertEquals(ReaderTtsState.IDLE, awaitItem())
        }
        assertEquals(listOf("play", "pause", "stop"), harness.calls)
    }

    @Test
    fun `stop is idempotent when IDLE and calls onStop for cleanup`() {
        val harness = Harness()
        harness.controller.stop()

        assertEquals(listOf("stop"), harness.calls)
        assertEquals(ReaderTtsState.IDLE, harness.controller.state.value)
    }

    @Test
    fun `play when UNAVAILABLE does not invoke onPlay and notifies error`() {
        val harness = Harness(initialState = ReaderTtsState.UNAVAILABLE)

        harness.controller.play()

        assertEquals(emptyList<String>(), harness.calls)
        assertEquals(ReaderTtsState.UNAVAILABLE, harness.controller.state.value)
        assertEquals(listOf("Text-to-speech is unavailable"), harness.errorNotifications)
        assertEquals("Text-to-speech is unavailable", harness.controller.lastErrorMessage.value)
    }

    @Test
    fun `pause and stop when UNAVAILABLE do not invoke callbacks and state stays UNAVAILABLE`() {
        val harness = Harness(initialState = ReaderTtsState.UNAVAILABLE)

        harness.controller.pause()
        harness.controller.stop()

        assertEquals(emptyList<String>(), harness.calls)
        assertEquals(ReaderTtsState.UNAVAILABLE, harness.controller.state.value)
    }

    @Test
    fun `play transitions to ERROR when onPlay returns false`() = runTest {
        val harness = Harness(playResult = false, scope = this)

        harness.controller.state.test {
            assertEquals(ReaderTtsState.IDLE, awaitItem())

            harness.controller.play()

            assertEquals(ReaderTtsState.ERROR, awaitItem())
        }
        assertEquals(listOf("play"), harness.calls)
        assertEquals(listOf("Failed to start text-to-speech"), harness.errorNotifications)
        assertEquals("Failed to start text-to-speech", harness.controller.lastErrorMessage.value)
    }

    @Test
    fun `play transitions to ERROR when onPlay throws`() = runTest {
        val harness = Harness(scope = this)
        harness.playShouldThrow = true

        harness.controller.state.test {
            assertEquals(ReaderTtsState.IDLE, awaitItem())

            harness.controller.play()

            assertEquals(ReaderTtsState.ERROR, awaitItem())
        }
        assertEquals(listOf("play"), harness.calls)
        assertEquals(listOf("Failed to start text-to-speech"), harness.errorNotifications)
    }

    @Test
    fun `onEngineError transitions state to ERROR, invokes onStop, and notifies error`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()

        harness.controller.state.test {
            assertEquals(ReaderTtsState.PLAYING, awaitItem())

            harness.controller.onEngineError("Language data missing")

            assertEquals(ReaderTtsState.ERROR, awaitItem())
        }
        assertEquals(listOf("play", "stop"), harness.calls)
        assertEquals(listOf("Language data missing"), harness.errorNotifications)
        assertEquals("Language data missing", harness.controller.lastErrorMessage.value)
    }

    @Test
    fun `onPlaybackEnded transitions state to IDLE and invokes onStop`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()

        harness.controller.state.test {
            assertEquals(ReaderTtsState.PLAYING, awaitItem())

            harness.controller.onPlaybackEnded()

            assertEquals(ReaderTtsState.IDLE, awaitItem())
        }
        assertEquals(listOf("play", "stop"), harness.calls)
    }

    @Test
    fun `onExternalPause transitions state from PLAYING to PAUSED`() {
        val harness = Harness()
        harness.controller.play()
        assertEquals(ReaderTtsState.PLAYING, harness.controller.state.value)

        harness.controller.onExternalPause()
        assertEquals(ReaderTtsState.PAUSED, harness.controller.state.value)
    }

    @Test
    fun `onExternalPlay transitions state from PAUSED to PLAYING`() {
        val harness = Harness()
        harness.controller.play()
        harness.controller.pause()
        assertEquals(ReaderTtsState.PAUSED, harness.controller.state.value)

        harness.controller.onExternalPlay()
        assertEquals(ReaderTtsState.PLAYING, harness.controller.state.value)
    }

    @Test
    fun `can retry play from ERROR state`() {
        val harness = Harness(playResult = false)
        harness.controller.play()
        assertEquals(ReaderTtsState.ERROR, harness.controller.state.value)

        harness.playReturns = true
        harness.controller.play()
        assertEquals(ReaderTtsState.PLAYING, harness.controller.state.value)
        assertNull(harness.controller.lastErrorMessage.value)
    }

    @Test
    fun `skipPrevious and skipNext invoke callbacks when active`() {
        val harness = Harness()
        harness.controller.skipPrevious()
        harness.controller.skipNext()
        assertEquals(emptyList<String>(), harness.calls)

        harness.controller.play()
        harness.controller.skipPrevious()
        harness.controller.skipNext()
        assertEquals(listOf("play", "skipPrevious", "skipNext"), harness.calls)

        harness.controller.pause()
        harness.controller.skipPrevious()
        harness.controller.skipNext()
        assertEquals(listOf("play", "skipPrevious", "skipNext", "pause", "skipPrevious", "skipNext"), harness.calls)
    }

    @Test
    fun `timed sleep timer counts down and pauses playback upon expiration`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()
        assertEquals(ReaderTtsState.PLAYING, harness.controller.state.value)

        harness.controller.setSleepTimer(TtsSleepOption.MINUTES_15)
        assertEquals(TtsSleepOption.MINUTES_15, harness.controller.sleepState.value.option)
        assertEquals(900L, harness.controller.sleepState.value.remainingSeconds)
        assertTrue(harness.controller.sleepState.value.isActive)

        // Advance 10 seconds
        advanceTimeBy(10_001)
        assertEquals(890L, harness.controller.sleepState.value.remainingSeconds)
        assertEquals(ReaderTtsState.PLAYING, harness.controller.state.value)

        // Advance remaining 890 seconds to trigger expiration
        advanceTimeBy(890_000)
        assertEquals(ReaderTtsState.PAUSED, harness.controller.state.value)
        assertEquals(TtsSleepOption.OFF, harness.controller.sleepState.value.option)
        assertFalse(harness.controller.sleepState.value.isActive)
        assertTrue(harness.calls.contains("pause"))
    }

    @Test
    fun `cancel sleep timer resets sleep state to OFF`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()
        harness.controller.setSleepTimer(TtsSleepOption.MINUTES_30)
        assertTrue(harness.controller.sleepState.value.isActive)

        harness.controller.cancelSleepTimer()
        assertEquals(TtsSleepOption.OFF, harness.controller.sleepState.value.option)
        assertNull(harness.controller.sleepState.value.remainingSeconds)
        assertFalse(harness.controller.sleepState.value.isActive)
    }

    @Test
    fun `end of chapter timer pauses when chapter href changes`() {
        val harness = Harness()
        harness.controller.play()

        harness.controller.setSleepTimer(TtsSleepOption.END_OF_CHAPTER, "chapter1.xhtml")
        assertEquals(TtsSleepOption.END_OF_CHAPTER, harness.controller.sleepState.value.option)
        assertEquals("chapter1.xhtml", harness.controller.sleepState.value.targetChapterHref)
        assertTrue(harness.controller.sleepState.value.isActive)

        // Utterance within the same chapter
        harness.controller.onLocatorChanged("chapter1.xhtml#p2")
        assertEquals(ReaderTtsState.PLAYING, harness.controller.state.value)
        assertTrue(harness.controller.sleepState.value.isActive)

        // Utterance moves into next chapter
        harness.controller.onLocatorChanged("chapter2.xhtml#p1")
        assertEquals(ReaderTtsState.PAUSED, harness.controller.state.value)
        assertEquals(TtsSleepOption.OFF, harness.controller.sleepState.value.option)
        assertFalse(harness.controller.sleepState.value.isActive)
    }

    @Test
    fun `stopping playback resets active sleep timer`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()
        harness.controller.setSleepTimer(TtsSleepOption.MINUTES_45)
        assertTrue(harness.controller.sleepState.value.isActive)

        harness.controller.stop()
        assertEquals(ReaderTtsState.IDLE, harness.controller.state.value)
        assertEquals(TtsSleepOption.OFF, harness.controller.sleepState.value.option)
        assertFalse(harness.controller.sleepState.value.isActive)
    }

    @Test
    fun `MINUTES_5 and MINUTES_10 timed options initialize and count down`() = runTest {
        val harness5 = Harness(scope = this)
        harness5.controller.play()
        harness5.controller.setSleepTimer(TtsSleepOption.MINUTES_5)
        assertEquals(TtsSleepOption.MINUTES_5, harness5.controller.sleepState.value.option)
        assertEquals(300L, harness5.controller.sleepState.value.remainingSeconds)
        assertFalse(harness5.controller.sleepState.value.isFadingOut)

        advanceTimeBy(5_001)
        assertEquals(295L, harness5.controller.sleepState.value.remainingSeconds)

        val harness10 = Harness(scope = this)
        harness10.controller.play()
        harness10.controller.setSleepTimer(TtsSleepOption.MINUTES_10)
        assertEquals(TtsSleepOption.MINUTES_10, harness10.controller.sleepState.value.option)
        assertEquals(600L, harness10.controller.sleepState.value.remainingSeconds)
        assertFalse(harness10.controller.sleepState.value.isFadingOut)
    }

    @Test
    fun `timed sleep timer fades out volume during the last 30 seconds`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()
        harness.controller.setSleepTimer(TtsSleepOption.MINUTES_5) // 300 seconds

        // Advance 270 seconds (30s remaining: start of fade)
        advanceTimeBy(270_001)
        assertEquals(30L, harness.controller.sleepState.value.remainingSeconds)
        assertTrue(harness.controller.sleepState.value.isFadingOut)
        assertEquals(1.0f, harness.controller.sleepState.value.volumeMultiplier, 0.01f)

        // Advance 15 seconds (15s remaining: 50% volume)
        advanceTimeBy(15_000)
        assertEquals(15L, harness.controller.sleepState.value.remainingSeconds)
        assertTrue(harness.controller.sleepState.value.isFadingOut)
        assertEquals(0.5f, harness.controller.sleepState.value.volumeMultiplier, 0.01f)
        assertTrue(harness.volumeChanges.any { Math.abs(it - 0.5f) < 0.01f })

        // Advance remaining 15 seconds to trigger expiration
        advanceTimeBy(15_000)
        assertEquals(ReaderTtsState.PAUSED, harness.controller.state.value)
        assertEquals(TtsSleepOption.OFF, harness.controller.sleepState.value.option)
        assertFalse(harness.controller.sleepState.value.isActive)
        assertFalse(harness.controller.sleepState.value.isFadingOut)
        assertEquals(1.0f, harness.controller.sleepState.value.volumeMultiplier, 0.01f)
        assertEquals(1.0f, harness.volumeChanges.last(), 0.01f)
    }

    @Test
    fun `canceling sleep timer during fade-out restores volume to 1_0`() = runTest {
        val harness = Harness(scope = this)
        harness.controller.play()
        harness.controller.setSleepTimer(TtsSleepOption.MINUTES_5)

        // Advance into fade-out
        advanceTimeBy(285_001) // 15s remaining
        assertTrue(harness.controller.sleepState.value.isFadingOut)

        harness.controller.cancelSleepTimer()
        assertEquals(TtsSleepOption.OFF, harness.controller.sleepState.value.option)
        assertFalse(harness.controller.sleepState.value.isActive)
        assertEquals(1.0f, harness.volumeChanges.last(), 0.01f)
    }

    @Test
    fun `adapter secondary constructor delegates setVolume`() {
        val adapterCalls = mutableListOf<String>()
        val volumeCalls = mutableListOf<Float>()
        val adapter = object : TtsPlayerAdapter {
            override fun play(): Boolean = true
            override fun pause() { adapterCalls += "pause" }
            override fun stop() { adapterCalls += "stop" }
            override fun setVolume(volume: Float) { volumeCalls += volume }
        }

        val controller = ReaderTtsController(adapter = adapter)
        controller.setSleepTimer(TtsSleepOption.OFF)
        assertEquals(listOf(1.0f), volumeCalls)
    }
}
