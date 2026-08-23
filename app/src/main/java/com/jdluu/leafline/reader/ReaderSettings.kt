package com.jdluu.leafline.reader

import org.readium.r2.navigator.epub.EpubPreferences

/**
 * Mapping of the three horizontal tap zones over the reading surface. DEFAULT
 * turns pages with the conventional left-back, right-forward mapping; REVERSED
 * swaps the sides for left-handed use or right-to-left publications.
 */
enum class TapZoneMode {
    DEFAULT,
    REVERSED
}

/**
 * Page turn animation used when navigating through tap zones. SLIDE animates
 * the turn; NONE jumps straight to the target page.
 */
enum class PageTurnAnimation(val animated: Boolean) {
    NONE(false),
    SLIDE(true)
}

/**
 * Lowest window brightness override allowed by the reader slider. Keeping the
 * floor slightly above zero prevents a fully black screen on devices where an
 * absolute zero override is unusable.
 */
const val BRIGHTNESS_MIN = 0.05f

/** Highest window brightness override allowed by the reader slider. */
const val BRIGHTNESS_MAX = 1.0f

/** Clamps a raw brightness value into the range allowed by the reader slider. */
fun clampBrightness(value: Float): Float {
    return value.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
}

/**
 * Everything persisted by [ReaderPreferencesStore]: the Readium EPUB
 * preferences plus the Leafline-specific interaction settings.
 */
data class ReaderSettings(
    val epub: EpubPreferences = EpubPreferences(),
    val tapZones: TapZoneMode = TapZoneMode.DEFAULT,
    val pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.SLIDE,
    /**
     * Per-app window brightness override in [BRIGHTNESS_MIN]..[BRIGHTNESS_MAX],
     * or null to follow the system brightness setting.
     */
    val brightness: Float? = null
)

/** Horizontal tap zones spanning the reader screen from left to right. */
enum class TapZone {
    LEFT,
    CENTER,
    RIGHT
}

/** Action triggered by a tap zone. */
enum class TapZoneAction {
    PREVIOUS_PAGE,
    NEXT_PAGE,
    TOGGLE_MENU
}

/** Maps a horizontal position (0..1 across the screen) to its tap zone. */
fun tapZoneAt(fraction: Float): TapZone {
    return when {
        fraction < 1f / 3f -> TapZone.LEFT
        fraction > 2f / 3f -> TapZone.RIGHT
        else -> TapZone.CENTER
    }
}

/** Resolves the action of a tap zone under the given tap zone mode. */
fun tapZoneAction(zone: TapZone, mode: TapZoneMode): TapZoneAction {
    return when (zone) {
        TapZone.CENTER -> TapZoneAction.TOGGLE_MENU
        TapZone.LEFT ->
            if (mode == TapZoneMode.REVERSED) TapZoneAction.NEXT_PAGE
            else TapZoneAction.PREVIOUS_PAGE
        TapZone.RIGHT ->
            if (mode == TapZoneMode.REVERSED) TapZoneAction.PREVIOUS_PAGE
            else TapZoneAction.NEXT_PAGE
    }
}
