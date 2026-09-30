package com.jdluu.leafline.reader.tts

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain JVM unit tests for [ReaderTtsController], covering playback state transitions,
 * error handling, and the unavailable-engine fallback path.
 */
class ReaderTtsControllerTest {

    private class Harness(
        initialState: ReaderTtsState = ReaderTtsState.IDLE,
        playResult: Boolean = true
    ) {
        val calls = mutableListOf<String>()
        val errorNotifications = mutableListOf<String?>()
        var playReturns: Boolean = playResult
        var playShouldThrow: Boolean = false

        val controller = ReaderTtsController(
            initialState = initialState,
            onPlay = {
                calls += "play"
                if (playShouldThrow) throw IllegalStateException("Speech engine crashed")
                playReturns
            },
            onPause = { calls += "pause" },
            onStop = { calls += "stop" },
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
        val harness = Harness()
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
        val harness = Harness()
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
        val harness = Harness()
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
        val harness = Harness()
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
        val harness = Harness()
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
        val harness = Harness(playResult = false)

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
        val harness = Harness()
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
        val harness = Harness()
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
        val harness = Harness()
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
    fun `stop from ERROR state transitions to IDLE and calls onStop`() {
        val harness = Harness(playResult = false)
        harness.controller.play()
        assertEquals(ReaderTtsState.ERROR, harness.controller.state.value)

        harness.controller.stop()
        assertEquals(ReaderTtsState.IDLE, harness.controller.state.value)
        assertEquals(listOf("play", "stop"), harness.calls)
    }

    @Test
    fun `markUnavailable transitions state to UNAVAILABLE`() {
        val harness = Harness()
        assertEquals(ReaderTtsState.IDLE, harness.controller.state.value)

        harness.controller.markUnavailable()
        assertEquals(ReaderTtsState.UNAVAILABLE, harness.controller.state.value)
    }

    @Test
    fun `works with TtsPlayerAdapter constructor`() {
        val calls = mutableListOf<String>()
        val adapter = object : TtsPlayerAdapter {
            override fun play(): Boolean {
                calls += "play"
                return true
            }

            override fun pause() {
                calls += "pause"
            }

            override fun stop() {
                calls += "stop"
            }
        }
        val controller = ReaderTtsController(adapter = adapter)

        controller.play()
        assertEquals(ReaderTtsState.PLAYING, controller.state.value)

        controller.pause()
        assertEquals(ReaderTtsState.PAUSED, controller.state.value)

        controller.stop()
        assertEquals(ReaderTtsState.IDLE, controller.state.value)

        assertEquals(listOf("play", "pause", "stop"), calls)
    }
}
