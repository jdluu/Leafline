package com.jdluu.leafline.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Leafline spacing constants on an 8dp rhythm.
 *
 * Use these instead of inline .dp values throughout the UI to keep
 * spacing consistent across screens and components. The 8dp rhythm governs
 * padding, gaps between grid items, and vertical breathing room.
 */
object Spacing {
    val xs = 8.dp
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
    val xl = 48.dp
    val xxl = 64.dp
}

/**
 * Leafline padding constants for common inset patterns.
 *
 * [compact] is the only value off the 8dp rhythm: it matches the 12dp
 * container radius so card content sits inside a rounded surface.
 */
object Padding {
    val screen = 16.dp
    val item = 16.dp
    val compact = 12.dp
    val chipSpacing = 8.dp
    val sectionSpacing = 32.dp
}

/**
 * Leafline icon size constants.
 *
 * These are visual sizes for non-interactive glyphs. Interactive controls
 * always carry the 48dp minimum touch target, which is separate from the
 * drawn icon size.
 */
object IconSize {
    val small = 18.dp
    val medium = 24.dp
    val large = 32.dp
}