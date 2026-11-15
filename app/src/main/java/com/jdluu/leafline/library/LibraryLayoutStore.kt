package com.jdluu.leafline.library

import android.content.Context
import android.content.SharedPreferences

enum class LibraryLayout {
    GRID,
    LIST;

    companion object {
        fun fromNameOrDefault(name: String?): LibraryLayout =
            entries.firstOrNull { it.name == name } ?: GRID
    }
}

/**
 * Persists the user's preferred library presentation (Grid or List).
 */
class LibraryLayoutStore(private val preferences: SharedPreferences) {

    fun load(): LibraryLayout {
        return LibraryLayout.fromNameOrDefault(preferences.getString(KEY, null))
    }

    fun save(layout: LibraryLayout) {
        preferences.edit().putString(KEY, layout.name).apply()
    }

    companion object {
        private const val KEY = "library_layout"
        internal const val PREFS_NAME = "leafline_prefs"

        fun fromContext(context: Context): LibraryLayoutStore {
            return LibraryLayoutStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
        }
    }
}
