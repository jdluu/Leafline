package com.jdluu.leafline.reader.search

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Manages persistent on-device search history for book searches.
 * Keeps up to [maxHistory] unique recent queries in SharedPreferences.
 */
class SearchHistoryStore(
    private val preferences: SharedPreferences,
    private val maxHistory: Int = 10
) {
    companion object {
        const val PREF_KEY_RECENT_SEARCHES = "recent_search_queries"
        const val PREFS_NAME = "leafline_search_history"
        const val OPDS_PREFS_NAME = "leafline_opds_search_history"

        fun create(context: Context): SearchHistoryStore {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return SearchHistoryStore(prefs)
        }

        fun createOpds(context: Context): SearchHistoryStore {
            val prefs = context.getSharedPreferences(OPDS_PREFS_NAME, Context.MODE_PRIVATE)
            return SearchHistoryStore(prefs)
        }
    }

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private fun loadHistory(): List<String> {
        val json = preferences.getString(PREF_KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) {
                    list.add(item)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        preferences.edit().putString(PREF_KEY_RECENT_SEARCHES, array.toString()).apply()
        _history.value = list
    }

    /**
     * Records [rawQuery] in recent searches.
     * Moves existing occurrences to the top, trims to [maxHistory].
     */
    fun recordQuery(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isBlank()) return
        val current = loadHistory().toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        current.add(0, query)
        val trimmed = if (current.size > maxHistory) current.take(maxHistory) else current
        persist(trimmed)
    }

    /**
     * Removes a single query from search history.
     */
    fun removeQuery(query: String) {
        val current = loadHistory().toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        persist(current)
    }

    /**
     * Clears all search history.
     */
    fun clear() {
        persist(emptyList())
    }
}
