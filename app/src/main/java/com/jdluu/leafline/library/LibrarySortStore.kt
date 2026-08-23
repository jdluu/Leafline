package com.jdluu.leafline.library

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the last chosen library sort order in app-private preferences.
 */
class LibrarySortStore(private val preferences: SharedPreferences) {

    fun load(): LibrarySort {
        return LibrarySort.fromNameOrDefault(preferences.getString(KEY, null))
    }

    fun save(sort: LibrarySort) {
        preferences.edit().putString(KEY, sort.name).apply()
    }

    companion object {
        private const val KEY = "library_sort"
        internal const val PREFS_NAME = "leafline_prefs"

        fun fromContext(context: Context): LibrarySortStore {
            return LibrarySortStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
        }
    }
}
