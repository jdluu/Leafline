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
    fun `a catalog id this build no longer ships has nothing to clear`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        // Older versions offered this catalog by id without ever writing it to disk, so a
        // saved selection can still name it. Nothing is stored under that id, nothing is
        // cleared, and nothing is resurrected.
        assertFalse(store.clearCredentials("standard_ebooks"))
        assertEquals("", rawPrefs())
        assertTrue(store.loadFeeds().isEmpty())
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

    @Test
    fun `blank fields on an edit leave all three credentials in place`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val feed = store.addCustomFeed(
            title = "Calibre",
            url = "https://calibre.test/opds",
            username = "patron",
            password = "hunter2",
            authToken = "tok_123"
        )

        // An edit form that submits the same URL with empty credential fields.
        val updated = store.updateCustomFeed(feed.id, "Calibre Library", "https://calibre.test/opds")

        assertEquals("Calibre Library", updated?.title)
        val stored = OpdsFeedStore.fromContext(context, TestSecretCipher())
            .loadFeeds().single { it.id == feed.id }
        assertTrue("a blank edit must not strip the stored account", stored.hasAuth)
        assertEquals("patron", stored.username)
        assertEquals("hunter2", stored.password)
        assertEquals("tok_123", stored.authToken)
    }

    @Test
    fun `non-blank fields on an edit replace the stored credentials`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val feed = store.addCustomFeed(
            title = "Calibre",
            url = "https://calibre.test/opds",
            username = "patron",
            password = "hunter2",
            authToken = "tok_123"
        )

        store.updateCustomFeed(
            id = feed.id,
            title = "Calibre",
            url = "https://calibre.test/opds",
            username = "librarian",
            password = "s3cret",
            authToken = "tok_456"
        )

        val stored = OpdsFeedStore.fromContext(context, TestSecretCipher())
            .loadFeeds().single { it.id == feed.id }
        assertEquals("librarian", stored.username)
        assertEquals("s3cret", stored.password)
        assertEquals("tok_456", stored.authToken)
    }

    @Test
    fun `editing an unknown id returns null and writes nothing`() {
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val before = rawPrefs()

        assertEquals(null, store.updateCustomFeed("no-such-feed", "Title", "https://calibre.test/opds"))

        assertEquals(before, rawPrefs())
        assertEquals(0, store.loadFeeds().count { !it.isBuiltIn })
    }

    @Test
    fun `credentials encrypted under the legacy key alias still load and re-encrypt under the new alias on save`() {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val legacyCipher = object : com.jdluu.leafline.storage.SecretCipher {
            override fun encrypt(value: String): String = "old:$value"
            override fun decrypt(stored: String): String? =
                if (stored.startsWith("old:")) stored.removePrefix("old:") else null
        }
        val newCipher = object : com.jdluu.leafline.storage.SecretCipher {
            override fun encrypt(value: String): String = "opds:$value"
            override fun decrypt(stored: String): String? =
                if (stored.startsWith("opds:")) stored.removePrefix("opds:") else null
        }

        val legacyStore = OpdsFeedStore(prefs, cipher = legacyCipher)
        val feed = legacyStore.addCustomFeed(
            title = "Calibre",
            url = "https://calibre.test/opds",
            username = "patron",
            password = "legacy-password",
            authToken = "legacy-token"
        )

        val migratedStore = OpdsFeedStore(
            prefs = prefs,
            cipher = com.jdluu.leafline.storage.FallbackSecretCipher(primary = newCipher, fallback = legacyCipher)
        )
        val loaded = migratedStore.loadFeeds().single { it.id == feed.id }
        assertEquals("legacy-password", loaded.password)
        assertEquals("legacy-token", loaded.authToken)

        migratedStore.updateCustomFeed(feed.id, "Calibre Updated", "https://calibre.test/opds")
        val primaryOnly = OpdsFeedStore(prefs, cipher = newCipher).loadFeeds().single { it.id == feed.id }
        assertEquals("legacy-password", primaryOnly.password)
        assertEquals("legacy-token", primaryOnly.authToken)
    }
}
