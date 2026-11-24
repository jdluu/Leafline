package com.jdluu.leafline.opds

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A catalog password or token must never reach disk in a readable form: a Calibre
 * or Kavita credential in plaintext JSON is recoverable from the app's prefs file.
 */
@RunWith(RobolectricTestRunner::class)
class OpdsFeedStoreCredentialTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefsName = "leafline_opds_feeds"
    private val jsonKey = "custom_opds_feeds_json"

    @Before
    fun clearStore() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun rawPrefs(): String =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(jsonKey, "")
            .orEmpty()

    @Test
    fun `a saved password is never written in plaintext`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        store.addCustomFeed(
            title = "Calibre",
            url = "https://calibre.test/opds",
            password = "hunter2"
        )

        val raw = rawPrefs()
        assertFalse("password reached disk unreadable-free: $raw", raw.contains("hunter2"))
        assertTrue("password did not go through the cipher", raw.contains("enc:v1:"))
    }

    @Test
    fun `saving a built-in catalog with credentials never writes the password in plaintext`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        val feed = store.addCustomFeed(
            title = "Standard Ebooks",
            url = "https://standardebooks.org/feeds/opds",
            username = "patron",
            password = "hunter2"
        )

        val raw = rawPrefs()
        assertFalse("credential reached disk readable: $raw", raw.contains("hunter2"))
        assertTrue("credential did not go through the cipher", raw.contains("enc:v1:"))

        val saved = store.loadFeeds().filter { !it.isBuiltIn }
        assertEquals(1, saved.size)
        assertEquals(feed.id, saved.single().id)
        assertEquals("hunter2", saved.single().password)
        assertEquals("patron", saved.single().username)
    }

    @Test
    fun `adding the same URL twice keeps one custom feed`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val url = "https://standardebooks.org/feeds/opds"

        val first = store.addCustomFeed("Standard Ebooks", url, username = "patron", password = "first")
        val second = store.addCustomFeed("Standard Ebooks", url, username = "patron", password = "second")

        assertEquals("a re-add must keep the existing row's id", first.id, second.id)
        val custom = OpdsFeedStore.fromContext(context, TestSecretCipher())
            .loadFeeds()
            .filter { !it.isBuiltIn }
        assertEquals(1, custom.size)
        assertEquals("second", custom.single().password)
    }

    @Test
    fun `re-adding a saved URL with blank credentials keeps the stored account`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val url = "https://standardebooks.org/feeds/opds"
        val first = store.addCustomFeed(
            title = "Standard Ebooks",
            url = url,
            username = "patron",
            password = "hunter2",
            authToken = "tok_123"
        )

        // The dialog sends nulls when the auth section is left untouched.
        val second = store.addCustomFeed("Standard Ebooks (Patrons)", url)

        assertEquals("a re-add must keep the existing row's id", first.id, second.id)
        val custom = OpdsFeedStore.fromContext(context, TestSecretCipher())
            .loadFeeds()
            .filter { !it.isBuiltIn }
        assertEquals(1, custom.size)
        assertEquals("Standard Ebooks (Patrons)", custom.single().title)
        assertEquals("patron", custom.single().username)
        assertEquals("hunter2", custom.single().password)
        assertEquals("tok_123", custom.single().authToken)
        assertEquals(first.id, custom.single().id)
    }

    @Test
    fun `clearing credentials keeps the row and removes all three fields`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val url = "https://calibre.test/opds"
        val feed = store.addCustomFeed(
            title = "Calibre",
            url = url,
            username = "patron",
            password = "hunter2",
            authToken = "tok_123"
        )
        store.saveSelectedFeedId(feed.id)

        assertTrue(store.clearCredentials(feed.id))

        val reopened = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val custom = reopened.loadFeeds().filter { !it.isBuiltIn }
        assertEquals(1, custom.size)
        assertEquals(feed.id, custom.single().id)
        assertEquals("Calibre", custom.single().title)
        assertEquals(url, custom.single().url)
        assertEquals(null, custom.single().username)
        assertEquals(null, custom.single().password)
        assertEquals(null, custom.single().authToken)
        assertFalse("cleared row must not report auth", custom.single().hasAuth)
        assertEquals("clearing credentials must not disturb selection", feed.id, reopened.loadSelectedFeedId())
        assertFalse("cleared password survived on disk", rawPrefs().contains("hunter2"))
        assertFalse("cleared token survived on disk", rawPrefs().contains("tok_123"))
    }

    @Test
    fun `a second clear is a no-op rather than an error`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val feed = store.addCustomFeed("Calibre", "https://calibre.test/opds", password = "hunter2")

        assertTrue(store.clearCredentials(feed.id))
        val afterFirst = rawPrefs()
        assertTrue("a second clear of the same row must still succeed", store.clearCredentials(feed.id))

        assertEquals(afterFirst, rawPrefs())
        assertEquals(1, store.loadFeeds().count { !it.isBuiltIn })
    }

    @Test
    fun `clearing credentials for an unknown feed reports nothing to clear`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        assertFalse(store.clearCredentials("no-such-feed"))
        assertEquals("", rawPrefs())
    }

    @Test
    fun `a saved auth token is never written in plaintext`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        store.addCustomFeed(
            title = "Kavita",
            url = "https://kavita.test/opds",
            authToken = "tok_secret_123"
        )

        assertFalse(rawPrefs().contains("tok_secret_123"))
    }

    @Test
    fun `a stored password round-trips through the cipher`() {
        OpdsFeedStore.fromContext(context, TestSecretCipher())
            .addCustomFeed("Calibre", "https://calibre.test/opds", password = "hunter2")

        val reloaded = OpdsFeedStore.fromContext(context, TestSecretCipher()).loadFeeds()

        assertEquals("hunter2", reloaded.first { it.title == "Calibre" }.password)
    }

    @Test
    fun `a legacy plaintext password still loads and is re-encrypted on save`() {
        // Simulate a pre-fix install holding plaintext JSON.
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
            .putString(
                jsonKey,
                """[{"id":"old","title":"Old","url":"https://old.test/opds","isBuiltIn":false,"password":"legacy"}]"""
            )
            .commit()

        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())
        assertEquals("legacy", store.loadFeeds().first { it.title == "Old" }.password)

        store.addCustomFeed("Second", "https://second.test/opds")
        assertFalse("legacy plaintext survived a save", rawPrefs().contains("legacy"))
    }
}
