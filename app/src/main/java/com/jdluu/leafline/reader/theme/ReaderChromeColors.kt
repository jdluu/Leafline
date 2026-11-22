package com.jdluu.leafline.reader.theme

// Kept in its own file: these are the only ReaderTheme helpers that a plain JVM
// test can load. ReaderTheme.kt maps onto Readium's Theme, whose static
// initializer touches android.graphics.Color, so anything sharing that file's
// when-mappings cannot run outside an Android environment.

/**
 * Returns the standard background color Int for this theme.
 * When theme is null, falls back to light or dark depending on [isDark].
 */
fun ReaderTheme?.backgroundColor(): Int {
    return when (this) {
        ReaderTheme.LIGHT, ReaderTheme.EINK -> 0xFFFFFFFF.toInt()
        ReaderTheme.DARK -> 0xFF121212.toInt()
        ReaderTheme.SEPIA -> 0xFFFAF4E8.toInt()
        // Unset means the page Readium actually paints, which is its light default
        // whatever the system side is. Following the system here instead left a dark
        // band under a white page, and chrome sitting on that seam was unreadable.
        null -> 0xFFFFFFFF.toInt()
    }
}

/**
 * Ink for reader chrome drawn on top of the page (the minimal footer today).
 *
 * It is derived from the page, never from the app color scheme: the app can be
 * dark while the book page is white, and a light gray at 45% over white is
 * unreadable. Every value below clears 4.5:1 against the matching
 * [backgroundColor], so the footer stays quiet without failing AA. Kept as a
 * plain Int so it tests on the JVM without an Android environment.
 *
 * An unset theme is the light page, not the system side: `toNavigatorPreferences`
 * passes Readium a null theme, and Readium's default renders white paper with
 * dark type whatever the app's own scheme is. That disagreement was the original
 * bug, so the null case is fixed here rather than delegated to the system.
 */
fun ReaderTheme?.chromeTextColor(): Int {
    return when (this) {
        ReaderTheme.LIGHT, ReaderTheme.EINK -> 0xFF4A4F4B.toInt()
        ReaderTheme.SEPIA -> 0xFF4A4034.toInt()
        ReaderTheme.DARK -> 0xFFB8BCB8.toInt()
        null -> 0xFF4A4F4B.toInt()
    }
}
