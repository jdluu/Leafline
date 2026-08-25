package com.jdluu.leafline.reader.theme

import org.readium.r2.navigator.preferences.Theme

/**
 * Pure Kotlin representation of the reader's theme choice, kept independent of
 * Readium so domain logic and JVM unit tests never touch the Readium type:
 * its enum constants static-initialize through android.graphics.Color and
 * therefore cannot be loaded outside an Android environment.
 *
 * The constant names double as the persisted preference strings and must stay
 * stable.
 */
enum class ReaderTheme {
    LIGHT,
    DARK,
    SEPIA
}

/** Maps this theme onto the matching Readium preference value. */
fun ReaderTheme.toReadiumTheme(): Theme {
    return when (this) {
        ReaderTheme.LIGHT -> Theme.LIGHT
        ReaderTheme.DARK -> Theme.DARK
        ReaderTheme.SEPIA -> Theme.SEPIA
    }
}

/** Maps a Readium preference value back onto the pure Kotlin theme. */
fun Theme.toReaderTheme(): ReaderTheme {
    return when (this) {
        Theme.LIGHT -> ReaderTheme.LIGHT
        Theme.DARK -> ReaderTheme.DARK
        Theme.SEPIA -> ReaderTheme.SEPIA
    }
}
