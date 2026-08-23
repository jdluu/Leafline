package com.jdluu.leafline.reader

import kotlin.math.abs

/**
 * WCAG 2.1 contrast ratio utilities.
 *
 * Computes the contrast ratio between two colors for accessibility auditing.
 * Ratios range from 1.0 (identical) to 21.0 (black vs white).
 *
 * WCAG AA thresholds:
 * - 4.5:1 for normal text (body text below 18pt / 14pt bold)
 * - 3.0:1 for large text (18pt+ or 14pt+ bold) and UI components
 */
object WcagContrast {

    /** Minimum contrast ratio for normal body text (WCAG AA). */
    const val MIN_BODY = 4.5

    /** Minimum contrast ratio for large text or UI components (WCAG AA). */
    const val MIN_LARGE_OR_UI = 3.0

    /**
     * Parse a hex color string (#RRGGBB or #AARRGGBB) into (R, G, B) ints 0-255.
     * Alpha prefix is ignored for contrast purposes.
     */
    fun parseRgb(hex: String): Triple<Int, Int, Int> {
        val cleaned = hex.removePrefix("#")
        val rgb = if (cleaned.length == 8) cleaned.substring(2) else cleaned
        val r = rgb.substring(0, 2).toInt(16)
        val g = rgb.substring(2, 4).toInt(16)
        val b = rgb.substring(4, 6).toInt(16)
        return Triple(r, g, b)
    }

    /** Relative luminance of a color per WCAG 2.1. */
    fun relativeLuminance(hex: String): Double {
        val (r, g, b) = parseRgb(hex)
        val rs = channelLuminance(r)
        val gs = channelLuminance(g)
        val bs = channelLuminance(b)
        return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs
    }

    private fun channelLuminance(channel: Int): Double {
        val s = channel / 255.0
        return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }

    /** Contrast ratio between two hex colors. Returns a value from 1.0 to 21.0. */
    fun contrastRatio(hex1: String, hex2: String): Double {
        val l1 = relativeLuminance(hex1)
        val l2 = relativeLuminance(hex2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Whether two colors meet the body-text threshold (4.5:1). */
    fun meetsBodyText(fg: String, bg: String): Boolean =
        contrastRatio(fg, bg) >= MIN_BODY

    /** Whether two colors meet the large-text/UI threshold (3.0:1). */
    fun meetsLargeOrUi(fg: String, bg: String): Boolean =
        contrastRatio(fg, bg) >= MIN_LARGE_OR_UI
}
