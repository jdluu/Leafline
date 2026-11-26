package com.jdluu.leafline.reader

import com.jdluu.leafline.reader.ui.formatMinimalFooterText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MinimalReadingFooterTest {

    @Test
    fun `both chapter and percentage formats combined string`() {
        val result = formatMinimalFooterText("Chapter 1: Down the Rabbit-Hole", 42)
        assertEquals("Chapter 1: Down the Rabbit-Hole \u2022 42%", result)
    }

    @Test
    fun `chapter only formats without separator when percentage is null`() {
        val result = formatMinimalFooterText("Prologue", null)
        assertEquals("Prologue", result)
    }

    @Test
    fun `percentage only formats percentage without separator when chapter is null`() {
        val result = formatMinimalFooterText(null, 75)
        assertEquals("75%", result)
    }

    @Test
    fun `percentage only formats when chapter is blank or empty`() {
        val result = formatMinimalFooterText("   ", 100)
        assertEquals("100%", result)
    }

    @Test
    fun `returns null when both chapter and percentage are null or blank`() {
        assertNull(formatMinimalFooterText(null, null))
        assertNull(formatMinimalFooterText("", null))
        assertNull(formatMinimalFooterText("   ", null))
    }

    @Test
    fun `trims surrounding whitespace from chapter title`() {
        val result = formatMinimalFooterText("  Epilogue  ", 99)
        assertEquals("Epilogue \u2022 99%", result)
    }

    @Test
    fun `respects minimal hud mode selection`() {
        val chapter = "Chapter 4"
        val percent = 50
        val remaining = "~20 min left"

        // Percent only mode
        assertEquals(
            "50%",
            formatMinimalFooterText(chapter, percent, remaining, MinimalHudMode.PERCENT_ONLY)
        )

        // Time remaining mode
        assertEquals(
            "~20 min left",
            formatMinimalFooterText(chapter, percent, remaining, MinimalHudMode.TIME_REMAINING)
        )
        // Time remaining fallback when remaining is null
        assertEquals(
            "50%",
            formatMinimalFooterText(chapter, percent, null, MinimalHudMode.TIME_REMAINING)
        )

        // Chapter only mode
        assertEquals(
            "Chapter 4",
            formatMinimalFooterText(chapter, percent, remaining, MinimalHudMode.CHAPTER_ONLY)
        )
        // Chapter only fallback when chapter is null
        assertEquals(
            "50%",
            formatMinimalFooterText(null, percent, remaining, MinimalHudMode.CHAPTER_ONLY)
        )
    }
}
