package com.jdluu.leafline.reader

import android.content.Context
import android.content.SharedPreferences
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme

/**
 * Persists reader display preferences in app-private SharedPreferences so they
 * survive activity recreation and app restarts. Only the fields managed by the
 * reader settings sheet are stored; every other EpubPreferences field stays
 * unset so Readium applies its own defaults.
 */
class ReaderPreferencesStore(private val preferences: SharedPreferences) {

    fun load(): EpubPreferences {
        return EpubPreferences(
            theme = preferences.getString(KEY_THEME, null)?.let { themeFromName(it) },
            fontFamily = preferences.getString(KEY_FONT_FAMILY, null)?.let { FontFamily(it) },
            lineHeight = restoreDouble(KEY_LINE_HEIGHT),
            pageMargins = restoreDouble(KEY_PAGE_MARGINS),
            publisherStyles = restoreBoolean(KEY_PUBLISHER_STYLES),
            scroll = restoreBoolean(KEY_SCROLL)
        )
    }

    fun save(prefs: EpubPreferences) {
        preferences.edit()
            .putString(KEY_THEME, prefs.theme?.name)
            .putString(KEY_FONT_FAMILY, prefs.fontFamily?.name)
            .putNullableDouble(KEY_LINE_HEIGHT, prefs.lineHeight)
            .putNullableDouble(KEY_PAGE_MARGINS, prefs.pageMargins)
            .putNullableBoolean(KEY_PUBLISHER_STYLES, prefs.publisherStyles)
            .putNullableBoolean(KEY_SCROLL, prefs.scroll)
            .apply()
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

    private fun themeFromName(name: String): Theme? {
        return try {
            Theme.valueOf(name)
        } catch (_: IllegalArgumentException) {
            null
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

    companion object {
        private const val KEY_THEME = "reader_theme"
        private const val KEY_FONT_FAMILY = "reader_font_family"
        private const val KEY_LINE_HEIGHT = "reader_line_height"
        private const val KEY_PAGE_MARGINS = "reader_page_margins"
        private const val KEY_PUBLISHER_STYLES = "reader_publisher_styles"
        private const val KEY_SCROLL = "reader_scroll"
        internal const val PREFS_NAME = "leafline_reader_prefs"

        fun fromContext(context: Context): ReaderPreferencesStore {
            return ReaderPreferencesStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
        }
    }
}
