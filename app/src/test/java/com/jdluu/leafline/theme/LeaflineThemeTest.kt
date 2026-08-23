package com.jdluu.leafline.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaflineThemeTest {

    @Test
    fun `light primary contrast passes AA body threshold on surface`() {
        val ratio = contrastRatio("#315C52", "#FBFDF9")
        assertTrue("Light primary on surface: expected >= 4.5, got $ratio", ratio >= 4.5)
    }

    @Test
    fun `light onSurface contrast passes AA body threshold on surface`() {
        val ratio = contrastRatio("#191C1A", "#FBFDF9")
        assertTrue("Light onSurface on surface: expected >= 4.5, got $ratio", ratio >= 4.5)
    }

    @Test
    fun `light onSurfaceVariant contrast passes AA large-text threshold on surfaceVariant`() {
        val ratio = contrastRatio("#414944", "#DCE5DE")
        assertTrue(
            "Light onSurfaceVariant on surfaceVariant: expected >= 3.0, got $ratio",
            ratio >= 3.0
        )
    }

    @Test
    fun `light error contrast passes AA body threshold on surface`() {
        val ratio = contrastRatio("#BA1A1A", "#FBFDF9")
        assertTrue("Light error on surface: expected >= 4.5, got $ratio", ratio >= 4.5)
    }

    @Test
    fun `dark primary contrast passes AA body threshold on surface`() {
        val ratio = contrastRatio("#9CCCC0", "#191C1A")
        assertTrue("Dark primary on surface: expected >= 4.5, got $ratio", ratio >= 4.5)
    }

    @Test
    fun `dark onSurface contrast passes AA body threshold on surface`() {
        val ratio = contrastRatio("#E1E3DF", "#191C1A")
        assertTrue("Dark onSurface on surface: expected >= 4.5, got $ratio", ratio >= 4.5)
    }

    @Test
    fun `dark error contrast passes AA body threshold on surface`() {
        val ratio = contrastRatio("#FFB4AB", "#191C1A")
        assertTrue("Dark error on surface: expected >= 4.5, got $ratio", ratio >= 4.5)
    }

    @Test
    fun `dark onSurfaceVariant contrast passes AA large-text threshold on surfaceVariant`() {
        val ratio = contrastRatio("#C0C9C3", "#414944")
        assertTrue(
            "Dark onSurfaceVariant on surfaceVariant: expected >= 3.0, got $ratio",
            ratio >= 3.0
        )
    }

    @Test
    fun `oled onSurface contrast on true black passes AA body threshold`() {
        val ratio = contrastRatio("#E1E3DF", "#000000")
        assertTrue("OLED onSurface on black: expected >= 4.5, got $ratio", ratio >= 4.5)
    }

    @Test
    fun `oled primary on true black passes AA body threshold`() {
        val ratio = contrastRatio("#9CCCC0", "#000000")
        assertTrue("OLED primary on black: expected >= 4.5, got $ratio", ratio >= 4.5)
    }

    @Test
    fun `eink onSurface on white is maximum contrast`() {
        val ratio = contrastRatio("#000000", "#FFFFFF")
        assertTrue("Eink onSurface on white: expected >= 10.0, got $ratio", ratio >= 10.0)
    }

    @Test
    fun `eink primary on white passes AA body threshold`() {
        val ratio = contrastRatio("#1A1A1A", "#FFFFFF")
        assertTrue("Eink primary on white: expected >= 10.0, got $ratio", ratio >= 10.0)
    }

    @Test
    fun `eink onSurfaceVariant on surfaceVariant passes AA body threshold`() {
        val ratio = contrastRatio("#333333", "#E6E6E6")
        assertTrue(
            "Eink onSurfaceVariant on surfaceVariant: expected >= 4.5, got $ratio",
            ratio >= 4.5
        )
    }

    @Test
    fun `eink outline on white passes AA body threshold`() {
        val ratio = contrastRatio("#666666", "#FFFFFF")
        assertTrue("Eink outline on white: expected >= 4.5, got $ratio", ratio >= 4.5)
    }

    // --- WCAG contrast utilities ---

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