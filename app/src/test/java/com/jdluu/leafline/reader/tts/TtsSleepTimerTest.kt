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
        assertFalse(state.isActive)
        assertEquals("Timer", formatSleepTimerLabel(state))
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
