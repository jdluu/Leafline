package com.jdluu.leafline.reader

import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily

/**
 * Selectable EPUB font families offered by the reader settings sheet, backed by
 * the generic font stacks Readium maps inside publications. A null family keeps
 * the publisher font untouched ("Original"); the accessible stacks (OpenDyslexic,
 * Accessible DfA, iA Writer Duospace) are bundled by Readium itself.
 */
val READER_FONT_FAMILIES: List<Pair<FontFamily?, String>> = listOf(
    null to "Original",
    FontFamily.SERIF to "Serif",
    FontFamily.SANS_SERIF to "Sans",
    FontFamily.MONOSPACE to "Monospace",
    FontFamily.CURSIVE to "Cursive",
    FontFamily.FANTASY to "Fantasy",
    FontFamily.OPEN_DYSLEXIC to "OpenDyslexic",
    FontFamily.ACCESSIBLE_DFA to "Accessible DfA",
    FontFamily.IA_WRITER_DUOSPACE to "iA Writer Duospace"
)

/**
 * Resolves a persisted font family name against [READER_FONT_FAMILIES]. Returns
 * null for unknown names so stale stored values fall back to the original font.
 */
fun fontFamilyFromStoredName(name: String): FontFamily? {
    return READER_FONT_FAMILIES.firstOrNull { it.first?.name == name }?.first
}

/** Default page margin fraction applied around the reading surface. */
const val PAGE_MARGINS_DEFAULT = 0.5

/** Lowest page margin fraction allowed by the reader stepper. */
const val PAGE_MARGINS_MIN = 0.5

/** Highest page margin fraction allowed by the reader stepper. */
const val PAGE_MARGINS_MAX = 1.5

/** Increment used by the reader margin stepper. */
const val PAGE_MARGINS_STEP = 0.25

/** Clamps a page margin into the supported range. */
fun clampPageMargins(value: Double): Double {
    return value.coerceIn(PAGE_MARGINS_MIN, PAGE_MARGINS_MAX)
}

/** Snaps a page margin to the nearest stepper increment inside the supported range. */
fun snapPageMargins(value: Double): Double {
    val snapped = kotlin.math.round(value / PAGE_MARGINS_STEP) * PAGE_MARGINS_STEP
    return clampPageMargins(snapped)
}

/** Whether a publication renders with its publisher styles or reader overrides. */
enum class StyleMode {
    PUBLISHER,
    CUSTOM
}

/** Maps the raw publisher-styles flag to its style mode; unset behaves as custom. */
fun styleModeFor(publisherStyles: Boolean?): StyleMode {
    return if (publisherStyles == true) StyleMode.PUBLISHER else StyleMode.CUSTOM
}

/**
 * Rebuilds these EPUB preferences for [mode]. Publisher mode clears the custom
 * typography overrides so Readium renders the publication's own styles;
 * custom mode keeps the reader-controlled values and disables publisher styles.
 */
fun EpubPreferences.withStyleMode(mode: StyleMode): EpubPreferences {
    return when (mode) {
        StyleMode.PUBLISHER -> copy(
            fontFamily = null,
            lineHeight = null,
            pageMargins = null,
            publisherStyles = true
        )
        StyleMode.CUSTOM -> copy(publisherStyles = false)
    }
}

/**
 * Per-zone configuration of what a tap does over the reading surface: each of
 * the three horizontal zones maps to one [TapZoneAction]. DEFAULT mirrors the
 * conventional left-back, right-forward layout; REVERSED swaps the sides for
 * left-handed use or right-to-left publications.
 */
data class TapZoneConfig(
    val leftZone: TapZoneAction = TapZoneAction.PREVIOUS_PAGE,
    val centerZone: TapZoneAction = TapZoneAction.TOGGLE_MENU,
    val rightZone: TapZoneAction = TapZoneAction.NEXT_PAGE
) {
    companion object {
        /** Conventional mapping: left turns back, right turns forward. */
        val DEFAULT = TapZoneConfig()

        /** Swapped side zones for left-handed use or right-to-left publications. */
        val REVERSED = TapZoneConfig(
            leftZone = TapZoneAction.NEXT_PAGE,
            rightZone = TapZoneAction.PREVIOUS_PAGE
        )
    }
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
 * Resolves whether a page turn may animate. The stored choice only applies
 * while the OS runs animations: when the system removes them (animator
 * duration scale zeroed out, e.g. via the Remove Animations accessibility
 * toggle or developer options), page turns snap instantly regardless of the
 * preference. Any nonzero scale keeps animations available.
 */
fun pageTurnIsAnimated(
    animation: PageTurnAnimation,
    systemAnimatorDurationScale: Float
): Boolean {
    return animation.animated && systemAnimatorDurationScale != 0f
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
    val tapZoneConfig: TapZoneConfig = TapZoneConfig.DEFAULT,
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
    TOGGLE_MENU,
    NONE
}

/** Maps a horizontal position (0..1 across the screen) to its tap zone. */
fun tapZoneAt(fraction: Float): TapZone {
    return when {
        fraction < 1f / 3f -> TapZone.LEFT
        fraction > 2f / 3f -> TapZone.RIGHT
        else -> TapZone.CENTER
    }
}

/** Resolves the action configured for a tap zone under the given configuration. */
fun tapZoneAction(zone: TapZone, config: TapZoneConfig): TapZoneAction {
    return when (zone) {
        TapZone.LEFT -> config.leftZone
        TapZone.CENTER -> config.centerZone
        TapZone.RIGHT -> config.rightZone
    }
}

/**
 * Resolves the effective action of a tap on [zone], where [scrollModeOn]
 * indicates whether the publication renders in continuous scroll mode.
 *
 * Page-turn actions are disabled while scrolling: Readium 3.3.0 exposes no
 * public screenful-scroll hook for scroll mode, so reusing its page-turn
 * navigation cannot be relied on to step a viewport. Resolving page turns to
 * [TapZoneAction.NONE] leaves such taps unconsumed so the navigator webview
 * keeps default handling; vertical pan gestures remain the way to move through
 * the content. Menu toggles and none keep working unchanged.
 */
fun effectiveTapZoneAction(
    zone: TapZone,
    config: TapZoneConfig,
    scrollModeOn: Boolean
): TapZoneAction {
    val action = tapZoneAction(zone, config)
    if (!scrollModeOn) return action
    return when (action) {
        TapZoneAction.PREVIOUS_PAGE, TapZoneAction.NEXT_PAGE -> TapZoneAction.NONE
        else -> action
    }
}
