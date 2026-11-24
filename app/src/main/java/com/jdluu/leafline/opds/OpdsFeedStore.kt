package com.jdluu.leafline.opds

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jdluu.leafline.opds.model.OpdsFeedSource
import com.jdluu.leafline.storage.KeystoreSecretCipher
import com.jdluu.leafline.storage.SecretCipher
import java.util.UUID

/**
 * Manages default catalog feeds and persistent user-added custom OPDS feeds.
 */
class OpdsFeedStore internal constructor(
    private val prefs: SharedPreferences,
    private val gson: Gson = Gson(),
    private val cipher: SecretCipher = KeystoreSecretCipher()
) {

    companion object {
        private const val PREFS_NAME = "leafline_opds_feeds"
        private const val KEY_CUSTOM_FEEDS = "custom_opds_feeds_json"
        private const val KEY_SELECTED_FEED = "opds_selected_feed_id"

        /** Marks a stored credential as encrypted; anything else is legacy plaintext. */
        private const val ENC_PREFIX = "enc:v1:"

        val BUILT_IN_FEEDS = listOf(
            OpdsFeedSource(
                id = "standard_ebooks",
                title = "Standard Ebooks",
                // Published OPDS path per standardebooks.org/feeds. The previous
                // /opds path answers 401 for every request, with or without an
                // OPDS/Atom Accept header.
                url = "https://standardebooks.org/feeds/opds",
                isBuiltIn = true,
                description = "Free, high-quality public domain ebooks. Browsing the full catalog needs a Patrons Circle membership."
            ),
            OpdsFeedSource(
                id = "project_gutenberg",
                title = "Project Gutenberg",
                url = "https://m.gutenberg.org/ebooks.opds/",
                isBuiltIn = true,
                description = "Over 60,000 free public domain ebooks and literary classics"
            )
        )

        fun fromContext(context: Context): OpdsFeedStore =
            OpdsFeedStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

        /** Injects the cipher so storage is testable without an Android Keystore. */
        internal fun fromContext(context: Context, cipher: SecretCipher): OpdsFeedStore =
            OpdsFeedStore(
                prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
                cipher = cipher
            )
    }

    /**
     * Remembers which catalog the reader was last browsing so the choice survives a restart.
     */
    fun loadSelectedFeedId(): String? = prefs.getString(KEY_SELECTED_FEED, null)

    fun saveSelectedFeedId(id: String) {
        prefs.edit().putString(KEY_SELECTED_FEED, id).apply()
    }

    /**
     * Loads the combined list of built-in and user-saved custom OPDS feeds.
     */
    fun loadFeeds(): List<OpdsFeedSource> {
        val customJson = prefs.getString(KEY_CUSTOM_FEEDS, null) ?: return BUILT_IN_FEEDS
        return try {
            val type = object : TypeToken<List<OpdsFeedSource>>() {}.type
            val customList: List<OpdsFeedSource> = gson.fromJson(customJson, type) ?: emptyList()
            BUILT_IN_FEEDS + customList.map(::withDecryptedCredentials)
        } catch (_: Exception) {
            BUILT_IN_FEEDS
        }
    }

    /**
     * Adds a custom OPDS feed. Returns the saved [OpdsFeedSource].
     *
     * A feed whose URL is already saved is updated in place, keeping its id. Re-adding a
     * built-in catalog from the 401 screen must not leave two rows for the same address.
     * An omitted credential leaves the stored one alone, because the add form is also the
     * only place to fix a title, and clearing the account would make the row useless.
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

        val currentCustom = loadCustomFeedsOnly().toMutableList()
        val index = currentCustom.indexOfFirst { it.url == trimmedUrl }
        val existing = currentCustom.getOrNull(index)
        val saved = OpdsFeedSource(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = trimmedTitle,
            url = trimmedUrl,
            isBuiltIn = false,
            description = "Custom OPDS feed",
            username = username?.trim()?.ifBlank { null } ?: existing?.username,
            password = password?.ifBlank { null } ?: existing?.password,
            authToken = authToken?.trim()?.ifBlank { null } ?: existing?.authToken
        )
        if (index >= 0) currentCustom[index] = saved else currentCustom.add(saved)

        saveCustomFeeds(currentCustom)
        return saved
    }

    /**
     * Updates an existing custom OPDS feed. Returns the updated [OpdsFeedSource], or null if not found.
     *
     * A blank credential field means "unchanged", exactly as in [addCustomFeed]; removing a
     * stored account is [clearCredentials], the single explicit gesture for it.
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
            username = username?.trim()?.ifBlank { null } ?: existing.username,
            password = password?.ifBlank { null } ?: existing.password,
            authToken = authToken?.trim()?.ifBlank { null } ?: existing.authToken
        )
        currentCustom[index] = updated
        saveCustomFeeds(currentCustom)
        return updated
    }

    /**
     * Removes the stored credentials from a custom feed, keeping the row, its id, title,
     * URL and selection. Returns false when no custom feed has that id. A second call on
     * an already-cleared row succeeds without writing.
     */
    fun clearCredentials(id: String): Boolean {
        val currentCustom = loadCustomFeedsOnly().toMutableList()
        val index = currentCustom.indexOfFirst { it.id == id }
        if (index < 0) return false
        val existing = currentCustom[index]
        if (!existing.hasAuth) return true
        currentCustom[index] = existing.copy(username = null, password = null, authToken = null)
        saveCustomFeeds(currentCustom)
        return true
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
            val parsed: List<OpdsFeedSource> = gson.fromJson(customJson, type) ?: emptyList()
            parsed.map(::withDecryptedCredentials)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCustomFeeds(feeds: List<OpdsFeedSource>) {
        val sealed = feeds.map { feed ->
            feed.copy(
                password = seal(feed.password),
                authToken = seal(feed.authToken)
            )
        }
        prefs.edit().putString(KEY_CUSTOM_FEEDS, gson.toJson(sealed)).apply()
    }

    /** Encrypts a credential for disk. Blank stays null so Gson omits the field. */
    private fun seal(value: String?): String? {
        val plain = value?.trim()?.ifBlank { null } ?: return null
        return ENC_PREFIX + cipher.encrypt(plain)
    }

    /**
     * Reads a credential, accepting values written before encryption existed.
     * A value without the prefix is legacy plaintext from an older install and is
     * re-encrypted the next time anything is saved.
     */
    private fun unseal(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        return if (stored.startsWith(ENC_PREFIX)) {
            cipher.decrypt(stored.removePrefix(ENC_PREFIX))
        } else {
            stored
        }
    }

    private fun withDecryptedCredentials(feed: OpdsFeedSource): OpdsFeedSource = feed.copy(
        password = unseal(feed.password),
        authToken = unseal(feed.authToken)
    )
}
