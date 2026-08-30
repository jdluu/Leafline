package com.jdluu.leafline.library

import android.content.Context
import android.content.SharedPreferences

/** Persists user-selected Storage Access Framework library folders. */
class LibraryFolderStore(private val preferences: SharedPreferences) {
    fun load(): List<String> = preferences.getStringSet(KEY, emptySet())
        ?.toList()
        ?.sorted()
        ?: emptyList()

    fun save(uris: List<String>) {
        preferences.edit().putStringSet(KEY, uris.toSet()).apply()
    }

    fun add(uri: String) {
        save(load() + uri)
    }

    fun remove(uri: String): Boolean {
        val current = load()
        if (uri !in current) return false
        save(current - uri)
        return true
    }

    companion object {
        private const val KEY = "library_folders"
        internal const val PREFS_NAME = "leafline_prefs"

        fun fromContext(context: Context): LibraryFolderStore = LibraryFolderStore(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        )
    }
}
