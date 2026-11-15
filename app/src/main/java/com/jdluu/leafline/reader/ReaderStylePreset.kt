package com.jdluu.leafline.reader

import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import kotlin.math.abs

/**
 * Curated typography and accessibility presets for quick one-tap reader configuration.
 */
enum class ReaderStylePreset(
    val label: String,
    val description: String,
    val fontFamily: FontFamily?,
    val fontSize: Double,
    val lineHeight: Double,
    val pageMargins: Double
) {
    BALANCED(
        label = "Balanced",
        description = "Clean serif styling for comfortable everyday reading",
        fontFamily = FontFamily.SERIF,
        fontSize = 1.0,
        lineHeight = 1.2,
        pageMargins = PAGE_MARGINS_DEFAULT
    ),
    BOOK_CLASSIC(
        label = "Book Classic",
        description = "Traditional paper-book proportions with generous line spacing",
        fontFamily = FontFamily.SERIF,
        fontSize = 1.05,
        lineHeight = 1.4,
        pageMargins = 0.75
    ),
    COMPACT(
        label = "Compact",
        description = "Dense modern sans-serif layout maximizing text per screen",
        fontFamily = FontFamily.SANS_SERIF,
        fontSize = 0.95,
        lineHeight = 1.0,
        pageMargins = PAGE_MARGINS_MIN
    ),
    ACCESSIBLE_DYSLEXIC(
        label = "Dyslexic Friendly",
        description = "Weighted OpenDyslexic letterforms and open spacing for legibility",
        fontFamily = FontFamily.OPEN_DYSLEXIC,
        fontSize = 1.15,
        lineHeight = 1.4,
        pageMargins = 0.75
    ),
    HIGH_LEGIBILITY(
        label = "High Legibility",
        description = "Enlarged sans-serif text with high spacing for relaxed reading",
        fontFamily = FontFamily.SANS_SERIF,
        fontSize = 1.25,
        lineHeight = 1.4,
        pageMargins = PAGE_MARGINS_MIN
    );

    /**
     * Applies this preset to the given [EpubPreferences], ensuring publisher styles are disabled.
     */
    fun applyTo(preferences: EpubPreferences): EpubPreferences {
        return preferences.copy(
            fontFamily = fontFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            pageMargins = pageMargins,
            publisherStyles = false
        )
    }

    companion object {
        /**
         * Identifies which preset, if any, matches the given [EpubPreferences].
         * Tolerates minor floating-point variations in fontSize/lineHeight.
         */
        fun matchingPreset(preferences: EpubPreferences): ReaderStylePreset? {
            if (preferences.publisherStyles == true) return null
            return entries.firstOrNull { preset ->
                val fontMatches = preset.fontFamily == preferences.fontFamily
                val sizeMatches = preferences.fontSize?.let { abs(it - preset.fontSize) < 0.04 } ?: (preset.fontSize == 1.0)
                val lineMatches = preferences.lineHeight?.let { abs(it - preset.lineHeight) < 0.05 } ?: (preset.lineHeight == 1.2)
                val marginMatches = preferences.pageMargins?.let { abs(it - preset.pageMargins) < 0.05 } ?: (preset.pageMargins == PAGE_MARGINS_DEFAULT)
                fontMatches && sizeMatches && lineMatches && marginMatches
            }
        }
    }
}
