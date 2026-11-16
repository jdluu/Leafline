package com.jdluu.leafline.opds

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jdluu.leafline.opds.model.OpdsFeedSource
import java.util.UUID

/**
 * Manages default catalog feeds and persistent user-added custom OPDS feeds.
 */
class OpdsFeedStore(
    private val prefs: SharedPreferences,
    private val gson: Gson = Gson()
) {

    companion object {
        private const val PREFS_NAME = "leafline_opds_feeds"
        private const val KEY_CUSTOM_FEEDS = "custom_opds_feeds_json"

        val BUILT_IN_FEEDS = listOf(
            OpdsFeedSource(
                id = "standard_ebooks",
                title = "Standard Ebooks",
                url = "https://standardebooks.org/opds",
                isBuiltIn = true,
                description = "Free, high-quality public domain ebooks formatted to modern standards"
            ),
            OpdsFeedSource(
                id = "project_gutenberg",
                title = "Project Gutenberg",
                url = "https://m.gutenberg.org/ebooks.opds/",
                isBuiltIn = true,
                description = "Over 60,000 free public domain ebooks and literary classics"
            ),
            OpdsFeedSource(
                id = "feedbooks",
                title = "Feedbooks",
                url = "https://catalog.feedbooks.com/publicdomain/browse.atom",
                isBuiltIn = true,
                description = "Curated public domain and open-access fiction and non-fiction"
            )
        )

        fun fromContext(context: Context): OpdsFeedStore {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return OpdsFeedStore(prefs)
        }
    }

    /**
     * Loads the combined list of built-in and user-saved custom OPDS feeds.
     */
    fun loadFeeds(): List<OpdsFeedSource> {
        val customJson = prefs.getString(KEY_CUSTOM_FEEDS, null) ?: return BUILT_IN_FEEDS
        return try {
            val type = object : TypeToken<List<OpdsFeedSource>>() {}.type
            val customList: List<OpdsFeedSource> = gson.fromJson(customJson, type) ?: emptyList()
            BUILT_IN_FEEDS + customList
        } catch (_: Exception) {
            BUILT_IN_FEEDS
        }
    }

    /**
     * Adds a custom OPDS feed. Returns the newly created [OpdsFeedSource].
     */
    fun addCustomFeed(
        title: String,
        url: String,
        username: String? = null,
        password: String? = null,
        authToken: String? = null
    ): OpdsFeedSource {
        val trimmedTitle = title.trim().ifEmpty { "Custom Catalog" }
        val trimmedUrl = url.trim()
        val newSource = OpdsFeedSource(
            id = UUID.randomUUID().toString(),
            title = trimmedTitle,
            url = trimmedUrl,
            isBuiltIn = false,
            description = "Custom OPDS feed",
            username = username?.trim()?.ifBlank { null },
            password = password?.ifBlank { null },
            authToken = authToken?.trim()?.ifBlank { null }
        )

        val currentCustom = loadCustomFeedsOnly().toMutableList()
        currentCustom.add(newSource)
        saveCustomFeeds(currentCustom)
        return newSource
    }

    /**
     * Updates an existing custom OPDS feed. Returns the updated [OpdsFeedSource], or null if not found.
     */
    fun updateCustomFeed(
        id: String,
        title: String,
        url: String,
        username: String? = null,
        password: String? = null,
        authToken: String? = null
    ): OpdsFeedSource? {
        val currentCustom = loadCustomFeedsOnly().toMutableList()
        val index = currentCustom.indexOfFirst { it.id == id }
        if (index < 0) return null
        val existing = currentCustom[index]
        val updated = existing.copy(
            title = title.trim().ifEmpty { existing.title },
            url = url.trim().ifEmpty { existing.url },
            username = username?.trim()?.ifBlank { null },
            password = password?.ifBlank { null },
            authToken = authToken?.trim()?.ifBlank { null }
        )
        currentCustom[index] = updated
        saveCustomFeeds(currentCustom)
        return updated
    }

    /**
     * Removes a custom feed by ID. Built-in feeds cannot be removed.
     */
    fun removeCustomFeed(id: String): Boolean {
        val currentCustom = loadCustomFeedsOnly().toMutableList()
        val removed = currentCustom.removeAll { it.id == id }
        if (removed) {
            saveCustomFeeds(currentCustom)
        }
        return removed
    }

    private fun loadCustomFeedsOnly(): List<OpdsFeedSource> {
        val customJson = prefs.getString(KEY_CUSTOM_FEEDS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<OpdsFeedSource>>() {}.type
            gson.fromJson(customJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCustomFeeds(feeds: List<OpdsFeedSource>) {
        val json = gson.toJson(feeds)
        prefs.edit().putString(KEY_CUSTOM_FEEDS, json).apply()
    }
}
