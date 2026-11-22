package com.jdluu.leafline.reader.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * The reader's minimal footer draws over the book page, not over the app surface,
 * so its ink has to clear WCAG AA against the page the reader chose. These tests
 * are the contract: change a token and a failing ratio says so.
 */
class ReaderThemeChromeColorTest {

    private fun channel(value: Int): Double {
        val s = value / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(argb: Int): Double {
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrast(foreground: Int, background: Int): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    @Test
    fun everyReadingThemeClearsAaForTheFooterInk() {
        for (theme in ReaderTheme.entries) {
            val background = theme.backgroundColor()
            val ink = theme.chromeTextColor()
            val ratio = contrast(ink, background)
            assertTrue(
                "$theme chrome ink $ratio:1 is below the 4.5:1 floor",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun anUnsetThemeIsTheLightPageReadiumRenders() {
        // toNavigatorPreferences hands Readium a null theme, and its default paints
        // white paper whatever the app scheme is; the footer must agree with the page.
        assertEquals(ReaderTheme.LIGHT.chromeTextColor(), null.chromeTextColor())
        val ratio = contrast(null.chromeTextColor(), null.backgroundColor())
        assertTrue("unset theme ink $ratio:1 is below the 4.5:1 floor", ratio >= 4.5)
    }

    @Test
    fun theInkFlipsDirectionWithThePage() {
        // A dark page needs a lighter ink than its own field and a light page a darker
        // one; the old bug was a single light gray for both. Direction is the property,
        // not an absolute luminance threshold.
        assertTrue(
            luminance(ReaderTheme.DARK.chromeTextColor()) >
                luminance(ReaderTheme.DARK.backgroundColor())
        )
        assertTrue(
            luminance(ReaderTheme.LIGHT.chromeTextColor()) <
                luminance(ReaderTheme.LIGHT.backgroundColor())
        )
        assertTrue(
            luminance(ReaderTheme.SEPIA.chromeTextColor()) <
                luminance(ReaderTheme.SEPIA.backgroundColor())
        )
    }

    @Test
    fun theInkStaysQuieterThanThePageText() {
        // AA is the floor, not the target: the footer is chrome, so it must not read
        // as loud as pure black or pure white on its own page.
        assertTrue(contrast(ReaderTheme.LIGHT.chromeTextColor(), ReaderTheme.LIGHT.backgroundColor()) < 12.0)
        assertTrue(contrast(ReaderTheme.DARK.chromeTextColor(), ReaderTheme.DARK.backgroundColor()) < 12.0)
    }
}
