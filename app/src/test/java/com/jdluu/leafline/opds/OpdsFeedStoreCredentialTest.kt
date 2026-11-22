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
