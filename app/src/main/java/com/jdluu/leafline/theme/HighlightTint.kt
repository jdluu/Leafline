package com.jdluu.leafline.theme

import androidx.compose.ui.graphics.Color

/**
 * Curated highlight tint palette for annotation colors.
 *
 * Each tint is stored as an ARGB hex string for persistent storage and also
 * exposed as a Compose [Color] for the swatch row UI. All tints have 33%
 * alpha (0x55 prefix) so highlights are semi-transparent overlays on text.
 */
data class HighlightTint(
    /** Display name shown in the swatch picker. */
    val label: String,
    /** ARGB hex string (e.g. "#55E65100") used for persistence. */
    val hex: String,
    /** Opaque Compose color for the swatch chip display. */
    val swatchColor: Color,
    /** Compose color at annotation alpha for decoration rendering. */
    val tintColor: Color
)

/**
 * The complete set of highlight tint swatches offered to the user.
 *
 * The first entry is the default and is used when no selection is made.
 */
val HIGHLIGHT_TINTS: List<HighlightTint> = listOf(
    HighlightTint(
        label = "Amber",
        hex = "#55E65100",
        swatchColor = Color(0xFFE65100),
        tintColor = Color(0x55E65100)
    ),
    HighlightTint(
        label = "Yellow",
        hex = "#55FDD835",
        swatchColor = Color(0xFFFDD835),
        tintColor = Color(0x55FDD835)
    ),
    HighlightTint(
        label = "Green",
        hex = "#5543A047",
        swatchColor = Color(0xFF43A047),
        tintColor = Color(0x5543A047)
    ),
    HighlightTint(
        label = "Blue",
        hex = "#551E88E5",
        swatchColor = Color(0xFF1E88E5),
        tintColor = Color(0x551E88E5)
    ),
    HighlightTint(
        label = "Pink",
        hex = "#55E91E63",
        swatchColor = Color(0xFFE91E63),
        tintColor = Color(0x55E91E63)
    )
)

/** The default highlight tint used when no swatch is picked. */
val DEFAULT_HIGHLIGHT_TINT: HighlightTint = HIGHLIGHT_TINTS.first()