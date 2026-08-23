package com.jdluu.leafline.reader

import android.content.Context
import android.content.SharedPreferences
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Theme

/**
 * Persists reader display and interaction settings in app-private
 * SharedPreferences so they survive activity recreation and app restarts. Only
 * the fields managed by the reader settings sheet are stored; every other
 * EpubPreferences field stays unset so Readium applies its own defaults.
 * Values loaded from storage are validated against the reader domain rules:
 * unknown font names fall back to the original font, page margins snap back
 * into range, and a stored publisher-mode selection clears stale custom
 * typography overrides.
 * Tap zones are stored per zone with per-zone fallbacks for unknown names.
 * The superseded single-key tap zone preset ([KEY_TAP_ZONES]) migrates on
 * load until the first save replaces it with per-zone keys.
 * The sepia quick control's restore target ([ReaderSettings.preSepiaTheme])
 * is stored alongside; unknown names and a stored sepia value fall back to
 * unset so the toggle can never restore sepia.
 */
class ReaderPreferencesStore(private val preferences: SharedPreferences) {

    fun load(): ReaderSettings {
        return ReaderSettings(
            epub = loadEpubPreferences(),
            tapZoneConfig = loadTapZoneConfig(),
            pageTurnAnimation = enumFromName(KEY_PAGE_TURN_ANIMATION, PageTurnAnimation.SLIDE),
            brightness = restoreBrightness(),
            preSepiaTheme = restorePreSepiaTheme()
        )
    }

    fun save(settings: ReaderSettings) {
        val epub = settings.epub
        preferences.edit()
            .putString(KEY_THEME, epub.theme?.name)
            .putString(KEY_FONT_FAMILY, epub.fontFamily?.name)
            .putNullableDouble(KEY_LINE_HEIGHT, epub.lineHeight)
            .putNullableDouble(KEY_PAGE_MARGINS, epub.pageMargins?.let(::snapPageMargins))
            .putNullableBoolean(KEY_PUBLISHER_STYLES, epub.publisherStyles)
            .putNullableBoolean(KEY_SCROLL, epub.scroll)
            .putString(KEY_TAP_ZONE_LEFT, settings.tapZoneConfig.leftZone.name)
            .putString(KEY_TAP_ZONE_CENTER, settings.tapZoneConfig.centerZone.name)
            .putString(KEY_TAP_ZONE_RIGHT, settings.tapZoneConfig.rightZone.name)
            .remove(KEY_TAP_ZONES)
            .putString(KEY_PAGE_TURN_ANIMATION, settings.pageTurnAnimation.name)
            .putNullableFloat(KEY_BRIGHTNESS, settings.brightness)
            .putString(KEY_PRE_SEPIA_THEME, settings.preSepiaTheme?.name)
            .apply()
    }

    private fun loadEpubPreferences(): EpubPreferences {
        val restored = EpubPreferences(
            theme = preferences.getString(KEY_THEME, null)?.let { themeFromName(it) },
            fontFamily = preferences.getString(KEY_FONT_FAMILY, null)
                ?.let(::fontFamilyFromStoredName),
            lineHeight = restoreDouble(KEY_LINE_HEIGHT),
            pageMargins = restoreDouble(KEY_PAGE_MARGINS)?.let(::snapPageMargins),
            publisherStyles = restoreBoolean(KEY_PUBLISHER_STYLES),
            scroll = restoreBoolean(KEY_SCROLL)
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
     * Restores the per-zone tap actions. Unknown stored names fall back to
     * that zone's default. Until per-zone keys exist, the superseded
     * [KEY_TAP_ZONES] preset migrates: a stored REVERSED preset becomes the
     * swapped side zones, anything else keeps the conventional defaults.
     */
    private fun loadTapZoneConfig(): TapZoneConfig {
        val hasPerZoneKeys = preferences.contains(KEY_TAP_ZONE_LEFT) ||
            preferences.contains(KEY_TAP_ZONE_CENTER) ||
            preferences.contains(KEY_TAP_ZONE_RIGHT)
        if (!hasPerZoneKeys) {
            return if (preferences.getString(KEY_TAP_ZONES, null) == LEGACY_TAP_ZONES_REVERSED) {
                TapZoneConfig.REVERSED
            } else {
                TapZoneConfig.DEFAULT
            }
        }
        return TapZoneConfig(
            leftZone = enumFromName(KEY_TAP_ZONE_LEFT, TapZoneConfig.DEFAULT.leftZone),
            centerZone = enumFromName(KEY_TAP_ZONE_CENTER, TapZoneConfig.DEFAULT.centerZone),
            rightZone = enumFromName(KEY_TAP_ZONE_RIGHT, TapZoneConfig.DEFAULT.rightZone)
        )
    }

    /** Returns null when unset and clamps stale out-of-range values. */
    private fun restoreBrightness(): Float? {
        if (!preferences.contains(KEY_BRIGHTNESS)) return null
        return clampBrightness(preferences.getFloat(KEY_BRIGHTNESS, BRIGHTNESS_MAX))
    }

    /**
     * Restores the theme remembered by the sepia quick control for
     * [ReaderSettings.preSepiaTheme]. Unknown names fall back to unset via
     * [themeFromName]; a stored sepia value is likewise treated as unset so a
     * corrupt store cannot make disengaging restore sepia.
     */
    private fun restorePreSepiaTheme(): Theme? {
        return preferences.getString(KEY_PRE_SEPIA_THEME, null)
            ?.let(::themeFromName)
            ?.takeUnless { it == Theme.SEPIA }
    }

    private fun themeFromName(name: String): Theme? {
        return try {
            Theme.valueOf(name)
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
        private const val KEY_TAP_ZONE_LEFT = "reader_tap_zone_left"
        private const val KEY_TAP_ZONE_CENTER = "reader_tap_zone_center"
        private const val KEY_TAP_ZONE_RIGHT = "reader_tap_zone_right"

        /** Superseded single-key tap zone preset, migrated on load. */
        private const val KEY_TAP_ZONES = "reader_tap_zones"
        private const val LEGACY_TAP_ZONES_REVERSED = "REVERSED"
        private const val KEY_PAGE_TURN_ANIMATION = "reader_page_turn_animation"
        private const val KEY_BRIGHTNESS = "reader_brightness"
        private const val KEY_PRE_SEPIA_THEME = "reader_pre_sepia_theme"
        internal const val PREFS_NAME = "leafline_reader_prefs"

        fun fromContext(context: Context): ReaderPreferencesStore {
            return ReaderPreferencesStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
        }
    }
}
