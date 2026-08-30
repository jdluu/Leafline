package com.jdluu.leafline.theme

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the app-level appearance ([ThemeMode]) in app-private preferences.
 * The default is [ThemeMode.SYSTEM]; unknown stored names fall back to it so
 * a stale or corrupt value can never force an invalid color scheme.
 */
class AppearanceStore(private val preferences: SharedPreferences) {

    fun load(): ThemeMode {
        return preferences.getString(KEY, null)?.let(::fromStoredName) ?: ThemeMode.SYSTEM
    }

    fun save(mode: ThemeMode) {
        preferences.edit().putString(KEY, mode.name).apply()
    }

    private fun fromStoredName(name: String): ThemeMode {
        return try {
            ThemeMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    companion object {
        private const val KEY = "appearance"
        internal const val PREFS_NAME = "leafline_prefs"

        fun fromContext(context: Context): AppearanceStore {
            return AppearanceStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
        }
    }
}