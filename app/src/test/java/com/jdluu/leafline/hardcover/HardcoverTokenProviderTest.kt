package com.jdluu.leafline.hardcover

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory persistence so the provider's refresh logic can be tested without Android. */
private class MemoryStore(var config: HardcoverConfig? = null) : HardcoverConfigPersistence {
    var pending: HardcoverPendingAuth? = null
    val saved = mutableListOf<HardcoverConfig>()

    override fun load(): HardcoverConfig? = config
    override fun save(config: HardcoverConfig) {
        this.config = config
        saved += config
    }
    override fun clear() { config = null }
    override fun savePendingAuth(pending: HardcoverPendingAuth) { this.pending = pending }
    override fun takePendingAuth(): HardcoverPendingAuth? = pending.also { pending = null }
    override fun clearPendingAuth() { pending = null }
}

/** Records refresh calls; returns whatever the test queued. */
private class RecordingOAuthClient(
    private val result: () -> HardcoverTokens
) : HardcoverOAuthClient(clientId = "cid") {
    var refreshCount = 0
    var refreshTokenSeen: String? = null

    override suspend fun refresh(refreshToken: String): HardcoverTokens {
        refreshCount++
        refreshTokenSeen = refreshToken
        return result()
    }
}

/**
 * An expired access token must be refreshed before use, not retried until it fails.
 * A dead refresh token must empty the credentials so nothing keeps retrying it.
 */
class HardcoverTokenProviderTest {

    private val now = 1_000_000L

    private fun storeWith(config: HardcoverConfig) = MemoryStore(config)

    @Test
    fun `an unexpired token is returned without touching the network`() = runBlocking {
        val store = storeWith(
            HardcoverConfig.oauth(
                HardcoverTokens("good", "rt", expiresAtEpochMillis = now + 3_600_000L)
            )
        )
        val client = RecordingOAuthClient { error("must not refresh") }

        val token = HardcoverTokenProvider(store, client) { now }.accessToken()

        assertEquals("good", token)
        assertEquals(0, client.refreshCount)
    }

    @Test
    fun `an expired token is refreshed`() = runBlocking {
        val store = storeWith(
            HardcoverConfig.oauth(
                HardcoverTokens("stale", "rt-old", expiresAtEpochMillis = now - 1_000L)
            )
        )
        val client = RecordingOAuthClient {
            HardcoverTokens("fresh", "rt-new", expiresAtEpochMillis = now + 3_600_000L)
        }

        val token = HardcoverTokenProvider(store, client) { now }.accessToken()

        assertEquals("fresh", token)
        assertEquals(1, client.refreshCount)
        assertEquals("rt-old", client.refreshTokenSeen)
        assertEquals("fresh", store.config!!.token)
        assertEquals("rt-new", store.config!!.refreshToken)
    }

    @Test
    fun `a refresh that omits a new refresh token keeps the old one`() = runBlocking {
        val store = storeWith(
            HardcoverConfig.oauth(
                HardcoverTokens("stale", "keep-me", expiresAtEpochMillis = now - 1L)
            )
        )
        val client = RecordingOAuthClient {
            HardcoverTokens("fresh", refreshToken = null, expiresAtEpochMillis = now + 3_600_000L)
        }

        HardcoverTokenProvider(store, client) { now }.accessToken()

        assertEquals("keep-me", store.config!!.refreshToken)
    }

    @Test
    fun `a pasted api token is used as-is and never refreshed`() = runBlocking {
        val store = storeWith(HardcoverConfig.apiToken("pat", userId = 3))
        val client = RecordingOAuthClient { error("must not refresh") }

        val token = HardcoverTokenProvider(store, client) { now }.accessToken()

        assertEquals("pat", token)
        assertEquals(0, client.refreshCount)
    }

    @Test
    fun `a dead refresh token empties the credentials and stops sending`() = runBlocking {
        val store = storeWith(
            HardcoverConfig.oauth(
                HardcoverTokens("stale", "dead", expiresAtEpochMillis = now - 1L),
                enabled = true
            )
        )
        val client = RecordingOAuthClient {
            throw HardcoverOAuthException("invalid_grant", errorCode = "invalid_grant", statusCode = 400)
        }

        val token = HardcoverTokenProvider(store, client) { now }.accessToken()

        assertNull(token)
        assertEquals("", store.config!!.token)
        assertNull("a dead token must not be kept", store.config!!.refreshToken)
        assertTrue("sending must stop until the reader signs in", !store.config!!.enabled)
        assertTrue("the connection stays so Settings can ask for sign-in", store.config!!.needsReauthorization)
    }

    @Test
    fun `a transient refresh failure keeps the connection intact`() = runBlocking {
        val original = HardcoverConfig.oauth(
            HardcoverTokens("stale", "rt", expiresAtEpochMillis = now - 1L),
            enabled = true
        )
        val store = storeWith(original)
        val client = RecordingOAuthClient { throw java.io.IOException("no network") }

        val token = HardcoverTokenProvider(store, client) { now }.accessToken()

        assertNull(token)
        assertEquals("a network blip must not wipe the account", "stale", store.config!!.token)
        assertEquals("rt", store.config!!.refreshToken)
        assertTrue(store.config!!.enabled)
    }

    @Test
    fun `nothing stored yields no token`() = runBlocking {
        assertNull(HardcoverTokenProvider(MemoryStore(null), null) { now }.accessToken())
    }
}
