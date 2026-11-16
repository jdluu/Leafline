package com.jdluu.leafline.reader.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsSleepTimerTest {

    @Test
    fun `default sleep state is inactive`() {
        val state = TtsSleepState()
        assertEquals(TtsSleepOption.OFF, state.option)
        assertNull(state.remainingSeconds)
        assertNull(state.targetChapterHref)
        assertEquals(30, state.fadeDurationSeconds)
        assertFalse(state.isFadingOut)
        assertEquals(1.0f, state.volumeMultiplier)
        assertFalse(state.isActive)
        assertEquals("Timer", formatSleepTimerLabel(state))
    }

    @Test
    fun `options include 5 and 10 minutes presets`() {
        assertEquals(5, TtsSleepOption.MINUTES_5.minutes)
        assertEquals("5 min", TtsSleepOption.MINUTES_5.label)
        assertTrue(TtsSleepOption.MINUTES_5.isTimed)

        assertEquals(10, TtsSleepOption.MINUTES_10.minutes)
        assertEquals("10 min", TtsSleepOption.MINUTES_10.label)
        assertTrue(TtsSleepOption.MINUTES_10.isTimed)
    }

    @Test
    fun `end of chapter option formatting`() {
        val state = TtsSleepState(option = TtsSleepOption.END_OF_CHAPTER)
        assertTrue(state.isActive)
        assertFalse(state.option.isTimed)
        assertEquals("End ch.", formatSleepTimerLabel(state))
    }

    @Test
    fun `timed option formatting with seconds`() {
        val state1 = TtsSleepState(option = TtsSleepOption.MINUTES_15, remainingSeconds = 900)
        assertEquals("15:00", formatSleepTimerLabel(state1))

        val state2 = TtsSleepState(option = TtsSleepOption.MINUTES_15, remainingSeconds = 899)
        assertEquals("14:59", formatSleepTimerLabel(state2))

        val state3 = TtsSleepState(option = TtsSleepOption.MINUTES_15, remainingSeconds = 45)
        assertEquals("00:45", formatSleepTimerLabel(state3))

        val state4 = TtsSleepState(option = TtsSleepOption.MINUTES_60, remainingSeconds = 3600)
        assertEquals("1:00:00", formatSleepTimerLabel(state4))

        val state5 = TtsSleepState(option = TtsSleepOption.MINUTES_60, remainingSeconds = 3665)
        assertEquals("1:01:05", formatSleepTimerLabel(state5))
    }

    @Test
    fun `fading out formatting prefixes Fading`() {
        val state1 = TtsSleepState(
            option = TtsSleepOption.MINUTES_5,
            remainingSeconds = 25,
            isFadingOut = true
        )
        assertEquals("Fading 00:25", formatSleepTimerLabel(state1))

        val state2 = TtsSleepState(
            option = TtsSleepOption.MINUTES_60,
            remainingSeconds = 3605,
            isFadingOut = true
        )
        assertEquals("Fading 1:00:05", formatSleepTimerLabel(state2))
    }

    @Test
    fun `timed option fallback when remainingSeconds is null`() {
        val state = TtsSleepState(option = TtsSleepOption.MINUTES_30, remainingSeconds = null)
        assertEquals("30m", formatSleepTimerLabel(state))
    }

    @Test
    fun `normalize chapter href strips anchors and queries`() {
        assertNull(normalizeChapterHref(null))
        assertEquals("chapter1.xhtml", normalizeChapterHref("chapter1.xhtml"))
        assertEquals("chapter1.xhtml", normalizeChapterHref("/chapter1.xhtml"))
        assertEquals("text/chapter1.xhtml", normalizeChapterHref("text/chapter1.xhtml#section-2"))
        assertEquals("text/chapter1.xhtml", normalizeChapterHref("/text/chapter1.xhtml?query=param#hash"))
    }
}
