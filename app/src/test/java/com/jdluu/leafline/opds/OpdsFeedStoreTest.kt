package com.jdluu.leafline.opds

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpdsFeedStoreTest {

    @Test
    fun aFreshStoreHasNoCatalogs() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        assertTrue("The app ships no catalog of its own", store.loadFeeds().isEmpty())
    }

    @Test
    fun addAndRemoveCustomFeed_persistsCorrectly() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        val custom = store.addCustomFeed("My Home Calibre", "http://192.168.1.50:8080/opds")
        assertNotNull(custom.id)
        assertFalse(custom.isBuiltIn)

        // Verify loaded in new store instance
        val store2 = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val loaded = store2.loadFeeds()
        val found = loaded.firstOrNull { it.id == custom.id }
        assertNotNull("Custom feed should be persisted", found)
        assertEquals("My Home Calibre", found!!.title)
        assertEquals("http://192.168.1.50:8080/opds", found.url)

        // Remove custom feed
        store2.removeCustomFeed(custom.id)
        val afterRemove = store2.loadFeeds()
        assertFalse("Custom feed should be removed", afterRemove.any { it.id == custom.id })
    }

    @Test
    fun addCustomFeed_withBasicAuth_computesHeaderAndPersists() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        val feed = store.addCustomFeed(
            title = "Calibre-Web Private",
            url = "https://calibre.example.com/opds",
            username = "admin",
            password = "secretpassword"
        )

        assertTrue(feed.hasAuth)
        assertEquals("Basic YWRtaW46c2VjcmV0cGFzc3dvcmQ=", feed.authHeader())

        val store2 = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val loaded = store2.loadFeeds().first { it.id == feed.id }
        assertEquals("admin", loaded.username)
        assertEquals("secretpassword", loaded.password)
        assertEquals("Basic YWRtaW46c2VjcmV0cGFzc3dvcmQ=", loaded.authHeader())
    }

    @Test
    fun addCustomFeed_withBearerToken_computesHeaderAndPersists() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        val feed = store.addCustomFeed(
            title = "Kavita Library",
            url = "https://kavita.example.com/api/opds/v1",
            authToken = "my-secret-api-token"
        )

        assertTrue(feed.hasAuth)
        assertEquals("Bearer my-secret-api-token", feed.authHeader())

        val store2 = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val loaded = store2.loadFeeds().first { it.id == feed.id }
        assertEquals("my-secret-api-token", loaded.authToken)
        assertEquals("Bearer my-secret-api-token", loaded.authHeader())
    }

    @Test
    fun updateCustomFeed_updatesCredentialsSuccessfully() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())

        val feed = store.addCustomFeed(
            title = "Home Library",
            url = "http://192.168.1.100:8080/opds"
        )
        assertFalse(feed.hasAuth)

        val updated = store.updateCustomFeed(
            id = feed.id,
            title = "Home Library Secured",
            url = "http://192.168.1.100:8080/opds",
            username = "user",
            password = "pass"
        )

        assertNotNull(updated)
        assertTrue(updated!!.hasAuth)
        assertEquals("Basic dXNlcjpwYXNz", updated.authHeader())
    }

    @Test
    fun removingAnUnknownCatalogChangesNothing() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = OpdsFeedStore.fromContext(context, TestSecretCipher())
        val saved = store.addCustomFeed("Calibre", "https://calibre.test/opds")

        // The ids older versions shipped are ordinary unknown ids now: nothing to remove,
        // and nothing resurrected in their place.
        assertFalse(store.removeCustomFeed("standard_ebooks"))
        assertEquals(listOf(saved.id), store.loadFeeds().map { it.id })
    }
}
