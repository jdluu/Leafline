package com.jdluu.leafline.reader

import com.jdluu.leafline.formatMinimalFooterText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MinimalReadingFooterTest {

    @Test
    fun `both chapter and percentage formats combined string`() {
        val result = formatMinimalFooterText("Chapter 1: Down the Rabbit-Hole", 42)
        assertEquals("Chapter 1: Down the Rabbit-Hole • 42%", result)
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
        assertEquals("Epilogue • 99%", result)
    }
}
