package com.jdluu.leafline.reader

import android.content.Context
import android.content.SharedPreferences
import com.jdluu.leafline.reader.theme.ReaderTheme
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * SharedPreferences-backed persistence for [ReaderSettings].
 *
 * Font family choices are stored as strings using the stable name of Readium's
 * [org.readium.r2.navigator.preferences.FontFamily] instances; unrecognized
 * values fall back to null (original publisher font) via
 * [fontFamilyFromStoredName].
 * Line height and page margins are stored as double bits; page margins are
 * clamped and snapped to [PAGE_MARGINS_STEP] on load and save.
 * Storing publisher styles as true drops the custom font, line height, and
 * margins on load via [withStyleMode] so an active publisher-mode choice
 * cannot reintroduce stale overrides after a restart.
 * Per-zone tap actions are stored under separate keys for left, center, right.
 * The superseded single-key tap zone preset ([KEY_TAP_ZONES]) migrates on
 * load until the first save replaces it with per-zone keys.
 * The sepia quick control's restore target ([ReaderSettings.preSepiaTheme])
 * is stored alongside; unknown names and a stored sepia value fall back to
 * unset so the toggle can never restore sepia.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReaderPreferencesStore(private val preferences: SharedPreferences) {

    fun load(initialReduceMotion: Boolean = false): ReaderSettings {
        return ReaderSettings(
            epub = loadEpubPreferences(),
            theme = restoreTheme(),
            tapZoneConfig = loadTapZoneConfig(),
            pageTurnAnimation = enumFromName(KEY_PAGE_TURN_ANIMATION, PageTurnAnimation.SLIDE),
            brightness = restoreBrightness(),
            preSepiaTheme = restorePreSepiaTheme(),
            reduceMotion = if (preferences.contains(KEY_REDUCE_MOTION)) {
                preferences.getBoolean(KEY_REDUCE_MOTION, false)
            } else {
                initialReduceMotion
            },
            previewFootnotes = preferences.getBoolean(KEY_PREVIEW_FOOTNOTES, true),
            keepScreenOn = preferences.getBoolean(KEY_KEEP_SCREEN_ON, true),
            orientation = enumFromName(KEY_ORIENTATION, ReaderOrientation.AUTO),
            showMinimalHud = preferences.getBoolean(KEY_SHOW_MINIMAL_HUD, true),
            minimalHudMode = enumFromName(KEY_MINIMAL_HUD_MODE, MinimalHudMode.CHAPTER_AND_PERCENT),
            hapticFeedback = preferences.getBoolean(KEY_HAPTIC_FEEDBACK, false),
            volumeKeyNavigation = preferences.getBoolean(KEY_VOLUME_KEY_NAVIGATION, true),
            invertVolumeKeys = preferences.getBoolean(KEY_INVERT_VOLUME_KEYS, false)
        )
    }

    fun save(settings: ReaderSettings) {
        val epub = settings.epub
        preferences.edit()
            .putString(KEY_THEME, settings.theme?.name)
            .putString(KEY_FONT_FAMILY, epub.fontFamily?.name)
            .putNullableDouble(KEY_LINE_HEIGHT, epub.lineHeight)
            .putNullableDouble(KEY_PAGE_MARGINS, epub.pageMargins?.let(::snapPageMargins))
            .putNullableBoolean(KEY_PUBLISHER_STYLES, epub.publisherStyles)
            .putNullableBoolean(KEY_SCROLL, epub.scroll)
            .putNullableBoolean(KEY_HYPHENS, epub.hyphens)
            .putString(KEY_COLUMN_COUNT, epub.columnCount?.name)
            .putString(KEY_TAP_ZONE_LEFT, settings.tapZoneConfig.leftZone.name)
            .putString(KEY_TAP_ZONE_CENTER, settings.tapZoneConfig.centerZone.name)
            .putString(KEY_TAP_ZONE_RIGHT, settings.tapZoneConfig.rightZone.name)
            .putString(KEY_TAP_ZONE_LAYOUT, settings.tapZoneConfig.layout.name)
            .remove(KEY_TAP_ZONES)
            .putString(KEY_PAGE_TURN_ANIMATION, settings.pageTurnAnimation.name)
            .putNullableFloat(KEY_BRIGHTNESS, settings.brightness)
            .putString(KEY_PRE_SEPIA_THEME, settings.preSepiaTheme?.name)
            .putBoolean(KEY_REDUCE_MOTION, settings.reduceMotion)
            .putBoolean(KEY_PREVIEW_FOOTNOTES, settings.previewFootnotes)
            .putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)
            .putString(KEY_ORIENTATION, settings.orientation.name)
            .putBoolean(KEY_SHOW_MINIMAL_HUD, settings.showMinimalHud)
            .putString(KEY_MINIMAL_HUD_MODE, settings.minimalHudMode.name)
            .putBoolean(KEY_HAPTIC_FEEDBACK, settings.hapticFeedback)
            .putBoolean(KEY_VOLUME_KEY_NAVIGATION, settings.volumeKeyNavigation)
            .putBoolean(KEY_INVERT_VOLUME_KEYS, settings.invertVolumeKeys)
            .apply()
    }

    private fun loadEpubPreferences(): EpubPreferences {
        val restored = EpubPreferences(
            fontFamily = preferences.getString(KEY_FONT_FAMILY, null)
                ?.let(::fontFamilyFromStoredName),
            lineHeight = restoreDouble(KEY_LINE_HEIGHT),
            pageMargins = restoreDouble(KEY_PAGE_MARGINS)?.let(::snapPageMargins),
            publisherStyles = restoreBoolean(KEY_PUBLISHER_STYLES),
            scroll = restoreBoolean(KEY_SCROLL),
            hyphens = restoreBoolean(KEY_HYPHENS),
            columnCount = preferences.getString(KEY_COLUMN_COUNT, null)?.let { name ->
                try {
                    ColumnCount.valueOf(name)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        )
        // A stored publisher-mode selection drops stale custom typography so a
        // restart cannot reintroduce overrides the user switched away from.
        return if (restored.publisherStyles == true) {
            restored.withStyleMode(StyleMode.PUBLISHER)
        } else {
            restored
        }
    }

    private fun restoreDouble(key: String): Double? {
        if (!preferences.contains(key)) return null
        val raw = java.lang.Double.longBitsToDouble(preferences.getLong(key, 0L))
        return if (raw.isNaN()) null else raw
    }

    private fun restoreBoolean(key: String): Boolean? {
        if (!preferences.contains(key)) return null
        return preferences.getBoolean(key, false)
    }

    /**
     * Restores the per-zone tap actions and layout. Unknown stored names fall back to
     * default. Until per-zone keys exist, the superseded
     * [KEY_TAP_ZONES] preset migrates: a stored REVERSED preset becomes the
     * swapped side zones, anything else keeps the conventional defaults.
     */
    private fun loadTapZoneConfig(): TapZoneConfig {
        val layout = enumFromName(KEY_TAP_ZONE_LAYOUT, TapZoneLayout.SYMMETRIC)
        val hasPerZoneKeys = preferences.contains(KEY_TAP_ZONE_LEFT) ||
            preferences.contains(KEY_TAP_ZONE_CENTER) ||
            preferences.contains(KEY_TAP_ZONE_RIGHT)
        if (!hasPerZoneKeys) {
            val base = if (preferences.getString(KEY_TAP_ZONES, null) == LEGACY_TAP_ZONES_REVERSED) {
                TapZoneConfig.REVERSED
            } else {
                TapZoneConfig.DEFAULT
            }
            return base.copy(layout = layout)
        }
        return TapZoneConfig(
            leftZone = enumFromName(KEY_TAP_ZONE_LEFT, TapZoneConfig.DEFAULT.leftZone),
            centerZone = enumFromName(KEY_TAP_ZONE_CENTER, TapZoneConfig.DEFAULT.centerZone),
            rightZone = enumFromName(KEY_TAP_ZONE_RIGHT, TapZoneConfig.DEFAULT.rightZone),
            layout = layout
        )
    }

    /** Returns null when unset and clamps stale out-of-range values. */
    private fun restoreBrightness(): Float? {
        if (!preferences.contains(KEY_BRIGHTNESS)) return null
        return clampBrightness(preferences.getFloat(KEY_BRIGHTNESS, BRIGHTNESS_MAX))
    }

    /**
     * Restores the selected reading theme. Unknown stored names fall back to
     * unset so a stale or corrupt value cannot force an invalid theme.
     */
    private fun restoreTheme(): ReaderTheme? {
        return preferences.getString(KEY_THEME, null)?.let(::themeFromName)
    }

    /**
     * Restores the theme remembered by the sepia quick control for
     * [ReaderSettings.preSepiaTheme]. Unknown names fall back to unset via
     * [themeFromName]; a stored sepia value is likewise treated as unset so a
     * corrupt store cannot make disengaging restore sepia.
     */
    private fun restorePreSepiaTheme(): ReaderTheme? {
        return preferences.getString(KEY_PRE_SEPIA_THEME, null)
            ?.let(::themeFromName)
            ?.takeUnless { it == ReaderTheme.SEPIA }
    }

    private fun themeFromName(name: String): ReaderTheme? {
        return try {
            ReaderTheme.valueOf(name)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private inline fun <reified T : Enum<T>> enumFromName(key: String, fallback: T): T {
        val name = preferences.getString(key, null) ?: return fallback
        return try {
            enumValueOf<T>(name)
        } catch (_: IllegalArgumentException) {
            fallback
        }
    }

    private fun SharedPreferences.Editor.putNullableDouble(
        key: String,
        value: Double?
    ): SharedPreferences.Editor {
        return if (value == null) {
            remove(key)
        } else {
            putLong(key, java.lang.Double.doubleToRawLongBits(value))
        }
    }

    private fun SharedPreferences.Editor.putNullableBoolean(
        key: String,
        value: Boolean?
    ): SharedPreferences.Editor {
        return if (value == null) {
            remove(key)
        } else {
            putBoolean(key, value)
        }
    }

    private fun SharedPreferences.Editor.putNullableFloat(
        key: String,
        value: Float?
    ): SharedPreferences.Editor {
        return if (value == null) {
            remove(key)
        } else {
            putFloat(key, clampBrightness(value))
        }
    }

    companion object {
        private const val KEY_THEME = "reader_theme"
        private const val KEY_FONT_FAMILY = "reader_font_family"
        private const val KEY_LINE_HEIGHT = "reader_line_height"
        private const val KEY_PAGE_MARGINS = "reader_page_margins"
        private const val KEY_PUBLISHER_STYLES = "reader_publisher_styles"
        private const val KEY_SCROLL = "reader_scroll"
        private const val KEY_HYPHENS = "reader_hyphens"
        private const val KEY_COLUMN_COUNT = "reader_column_count"
        private const val KEY_TAP_ZONE_LEFT = "reader_tap_zone_left"
        private const val KEY_TAP_ZONE_CENTER = "reader_tap_zone_center"
        private const val KEY_TAP_ZONE_RIGHT = "reader_tap_zone_right"
        private const val KEY_TAP_ZONE_LAYOUT = "reader_tap_zone_layout"

        /** Superseded single-key tap zone preset, migrated on load. */
        private const val KEY_TAP_ZONES = "reader_tap_zones"
        private const val LEGACY_TAP_ZONES_REVERSED = "REVERSED"
        private const val KEY_PAGE_TURN_ANIMATION = "reader_page_turn_animation"
        private const val KEY_BRIGHTNESS = "reader_brightness"
        private const val KEY_PRE_SEPIA_THEME = "reader_pre_sepia_theme"
        private const val KEY_REDUCE_MOTION = "reader_reduce_motion"
        const val KEY_PREVIEW_FOOTNOTES = "reader_preview_footnotes"
        private const val KEY_KEEP_SCREEN_ON = "reader_keep_screen_on"
        private const val KEY_ORIENTATION = "reader_orientation"
        private const val KEY_SHOW_MINIMAL_HUD = "reader_show_minimal_hud"
        private const val KEY_MINIMAL_HUD_MODE = "reader_minimal_hud_mode"
        private const val KEY_HAPTIC_FEEDBACK = "reader_haptic_feedback"
        private const val KEY_VOLUME_KEY_NAVIGATION = "reader_volume_key_navigation"
        private const val KEY_INVERT_VOLUME_KEYS = "reader_invert_volume_keys"
        internal const val PREFS_NAME = "leafline_reader_prefs"

        fun fromContext(context: Context): ReaderPreferencesStore {
            return ReaderPreferencesStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
        }
    }
}
