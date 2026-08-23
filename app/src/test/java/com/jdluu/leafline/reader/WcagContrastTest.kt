package com.jdluu.leafline.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WcagContrastTest {

    @Test
    fun `black on white is maximum contrast`() {
        val ratio = WcagContrast.contrastRatio("#000000", "#FFFFFF")
        assertEquals(21.0, ratio, 0.01)
    }

    @Test
    fun `white on black is same as black on white`() {
        val r1 = WcagContrast.contrastRatio("#000000", "#FFFFFF")
        val r2 = WcagContrast.contrastRatio("#FFFFFF", "#000000")
        assertEquals(r1, r2, 0.001)
    }

    @Test
    fun `identical colors have ratio 1`() {
        val ratio = WcagContrast.contrastRatio("#888888", "#888888")
        assertEquals(1.0, ratio, 0.01)
    }

    @Test
    fun `Material 3 light theme onSurface vs surface meets body threshold`() {
        // Material 3 baseline light: onSurface ~ #1C1B1F, surface ~ #FFFBFE
        val ratio = WcagContrast.contrastRatio("#1C1B1F", "#FFFBFE")
        assertTrue("Expected >= 4.5, got $ratio", ratio >= WcagContrast.MIN_BODY)
    }

    @Test
    fun `Material 3 dark theme onSurface vs surface meets body threshold`() {
        // Material 3 baseline dark: onSurface ~ #E6E1E5, surface ~ #1C1B1F
        val ratio = WcagContrast.contrastRatio("#E6E1E5", "#1C1B1F")
        assertTrue("Expected >= 4.5, got $ratio", ratio >= WcagContrast.MIN_BODY)
    }

    @Test
    fun `default highlight tint on white meets UI threshold`() {
        // The highlight tint color #E65100 (deep amber) on white background
        // must meet 3:1 for UI visibility (it is a decoration, not body text)
        val ratio = WcagContrast.contrastRatio("#E65100", "#FFFFFF")
        assertTrue(
            "Highlight tint on white: expected >= 3.0, got $ratio",
            ratio >= WcagContrast.MIN_LARGE_OR_UI,
        )
    }

    @Test
    fun `default highlight tint on sepia meets UI threshold`() {
        // Sepia background ~ #F4ECD8
        val ratio = WcagContrast.contrastRatio("#E65100", "#F4ECD8")
        assertTrue(
            "Highlight tint on sepia: expected >= 3.0, got $ratio",
            ratio >= WcagContrast.MIN_LARGE_OR_UI,
        )
    }

    @Test
    fun `default highlight tint on dark theme meets UI threshold`() {
        // Material 3 dark surface ~ #1C1B1F
        val ratio = WcagContrast.contrastRatio("#E65100", "#1C1B1F")
        assertTrue(
            "Highlight tint on dark: expected >= 3.0, got $ratio",
            ratio >= WcagContrast.MIN_LARGE_OR_UI,
        )
    }

    @Test
    fun `alpha prefixed hex is parsed correctly`() {
        // #55E65100 should parse to the same RGB as #E65100
        val (r1, g1, b1) = WcagContrast.parseRgb("#55E65100")
        val (r2, g2, b2) = WcagContrast.parseRgb("#E65100")
        assertEquals(r1, r2)
        assertEquals(g1, g2)
        assertEquals(b1, b2)
    }

    @Test
    fun `sepia theme text on background meets body threshold`() {
        // Readium SEPIA: text ~ #5B4636, background ~ #F4ECD8
        val ratio = WcagContrast.contrastRatio("#5B4636", "#F4ECD8")
        assertTrue("Sepia text/bg: expected >= 4.5, got $ratio", ratio >= WcagContrast.MIN_BODY)
    }
}
