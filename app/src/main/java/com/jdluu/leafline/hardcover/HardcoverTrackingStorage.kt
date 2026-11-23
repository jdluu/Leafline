package com.jdluu.leafline.hardcover

import android.content.Context
import android.content.SharedPreferences

/**
 * Remembers the tracking mode and the reader's answers per book.
 *
 * Per-book answers live here rather than in the library database on purpose: a Room
 * column would mean a schema migration for what is a small, disposable map of
 * decisions, and losing one only costs a single prompt.
 *
 * Keys are book stable ids, which is the same identity the rest of the app uses.
 */
class HardcoverTrackingStorage internal constructor(
    private val prefs: SharedPreferences
) : HardcoverTrackingStore {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override fun mode(): HardcoverTrackingMode {
        val stored = prefs.getString(KEY_MODE, null)
            ?: return DEFAULT_MODE
        return runCatching { HardcoverTrackingMode.valueOf(stored) }.getOrDefault(DEFAULT_MODE)
    }

    override fun setMode(mode: HardcoverTrackingMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    override fun preferenceFor(stableId: String): HardcoverBookPreference? {
        val stored = prefs.getString(KEY_PREFIX + stableId, null) ?: return null
        return runCatching { HardcoverBookPreference.valueOf(stored) }.getOrNull()
    }

    override fun setPreference(stableId: String, preference: HardcoverBookPreference) {
        prefs.edit().putString(KEY_PREFIX + stableId, preference.name).apply()
    }

    override fun clearPreference(stableId: String) {
        prefs.edit().remove(KEY_PREFIX + stableId).apply()
    }

    companion object {
        /**
         * Tracking every matchable book is the sane default: turning sync on is
         * already the consent, and asking about each book would be a second question
         * the reader has effectively answered.
         */
        val DEFAULT_MODE = HardcoverTrackingMode.ALL_BOOKS

        private const val PREFS_NAME = "leafline_hardcover_tracking"
        private const val KEY_MODE = "mode"
        private const val KEY_PREFIX = "book:"
    }
}

/** Seam, kept Android-free at the call sites that only need the rules. */
interface HardcoverTrackingStore {
    fun mode(): HardcoverTrackingMode
    fun setMode(mode: HardcoverTrackingMode)
    fun preferenceFor(stableId: String): HardcoverBookPreference?
    fun setPreference(stableId: String, preference: HardcoverBookPreference)
    fun clearPreference(stableId: String)
}
