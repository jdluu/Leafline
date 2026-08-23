package com.jdluu.leafline.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Leafline spacing constants following a 4dp base unit.
 *
 * Use these instead of inline .dp values throughout the UI to keep
 * spacing consistent across screens and components.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/**
 * Leafline padding constants for common inset patterns.
 */
object Padding {
    val screenHorizontal = 16.dp
    val screenVertical = 16.dp
    val cardContent = 12.dp
    val chipSpacing = 8.dp
    val sectionSpacing = 32.dp
    val itemSpacing = 16.dp
}

/**
 * Leafline icon size constants.
 */
object IconSize {
    val small = 18.dp
    val medium = 24.dp
    val large = 32.dp
}