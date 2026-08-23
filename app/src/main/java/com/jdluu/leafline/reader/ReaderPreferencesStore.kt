package com.jdluu.leafline.reader

import android.content.Context
import android.content.SharedPreferences
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme

/**
 * Persists reader display and interaction settings in app-private
 * SharedPreferences so they survive activity recreation and app restarts. Only
 * the fields managed by the reader settings sheet are stored; every other
 * EpubPreferences field stays unset so Readium applies its own defaults.
 */
class ReaderPreferencesStore(private val preferences: SharedPreferences) {

    fun load(): ReaderSettings {
        return ReaderSettings(
            epub = loadEpubPreferences(),
            tapZones = enumFromName(KEY_TAP_ZONES, TapZoneMode.DEFAULT),
            pageTurnAnimation = enumFromName(KEY_PAGE_TURN_ANIMATION, PageTurnAnimation.SLIDE),
            brightness = restoreBrightness()
        )
    }

    fun save(settings: ReaderSettings) {
        val epub = settings.epub
        preferences.edit()
            .putString(KEY_THEME, epub.theme?.name)
            .putString(KEY_FONT_FAMILY, epub.fontFamily?.name)
            .putNullableDouble(KEY_LINE_HEIGHT, epub.lineHeight)
            .putNullableDouble(KEY_PAGE_MARGINS, epub.pageMargins)
            .putNullableBoolean(KEY_PUBLISHER_STYLES, epub.publisherStyles)
            .putNullableBoolean(KEY_SCROLL, epub.scroll)
            .putString(KEY_TAP_ZONES, settings.tapZones.name)
            .putString(KEY_PAGE_TURN_ANIMATION, settings.pageTurnAnimation.name)
            .putNullableFloat(KEY_BRIGHTNESS, settings.brightness)
            .apply()
    }

    private fun loadEpubPreferences(): EpubPreferences {
        return EpubPreferences(
            theme = preferences.getString(KEY_THEME, null)?.let { themeFromName(it) },
            fontFamily = preferences.getString(KEY_FONT_FAMILY, null)?.let { FontFamily(it) },
            lineHeight = restoreDouble(KEY_LINE_HEIGHT),
            pageMargins = restoreDouble(KEY_PAGE_MARGINS),
            publisherStyles = restoreBoolean(KEY_PUBLISHER_STYLES),
            scroll = restoreBoolean(KEY_SCROLL)
        )
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

    /** Returns null when unset and clamps stale out-of-range values. */
    private fun restoreBrightness(): Float? {
        if (!preferences.contains(KEY_BRIGHTNESS)) return null
        return clampBrightness(preferences.getFloat(KEY_BRIGHTNESS, BRIGHTNESS_MAX))
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
        private const val KEY_TAP_ZONES = "reader_tap_zones"
        private const val KEY_PAGE_TURN_ANIMATION = "reader_page_turn_animation"
        private const val KEY_BRIGHTNESS = "reader_brightness"
        internal const val PREFS_NAME = "leafline_reader_prefs"

        fun fromContext(context: Context): ReaderPreferencesStore {
            return ReaderPreferencesStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
        }
    }
}
