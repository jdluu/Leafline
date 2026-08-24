package com.jdluu.leafline.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightTintTest {

    @Test
    fun `palette has five colors`() {
        assertEquals(5, HIGHLIGHT_TINTS.size)
    }

    @Test
    fun `palette contains amber as default`() {
        val amber = HIGHLIGHT_TINTS.first()
        assertEquals("Amber", amber.label)
        assertEquals("#55E65100", amber.hex)
    }

    @Test
    fun `default highlight tint matches palette first entry`() {
        assertEquals(HIGHLIGHT_TINTS.first(), DEFAULT_HIGHLIGHT_TINT)
    }

    @Test
    fun `all tints have hex prefixed with hash and 55 alpha`() {
        HIGHLIGHT_TINTS.forEach { tint ->
            assertTrue(
                "Expected hex to start with #55, got ${tint.hex}",
                tint.hex.startsWith("#55")
            )
            assertEquals(9, tint.hex.length) // # + 8 hex chars
        }
    }

    // --- WCAG utilities (reused) ---

    private fun relativeLuminance(hex: String): Double {
        val (r, g, b) = parseRgb(hex)
        return 0.2126 * channelLuminance(r) +
            0.7152 * channelLuminance(g) +
            0.0722 * channelLuminance(b)
    }

    private fun channelLuminance(channel: Int): Double {
        val s = channel / 255.0
        return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }

    private fun parseRgb(hex: String): Triple<Int, Int, Int> {
        val cleaned = hex.removePrefix("#")
        val rgb = if (cleaned.length == 8) cleaned.substring(2) else cleaned
        return Triple(
            rgb.substring(0, 2).toInt(16),
            rgb.substring(2, 4).toInt(16),
            rgb.substring(4, 6).toInt(16)
        )
    }

    private fun contrastRatio(hex1: String, hex2: String): Double {
        val l1 = relativeLuminance(hex1)
        val l2 = relativeLuminance(hex2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}