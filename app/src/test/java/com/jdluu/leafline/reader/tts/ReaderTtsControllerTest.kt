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
}
