package com.jdluu.leafline.hardcover

import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class CoordStore(var config: HardcoverConfig? = null) : HardcoverConfigPersistence {
    var pending: HardcoverPendingAuth? = null
    override fun load(): HardcoverConfig? = config
    override fun save(config: HardcoverConfig) { this.config = config }
    override fun clear() { config = null }
    override fun savePendingAuth(pending: HardcoverPendingAuth) { this.pending = pending }
    override fun takePendingAuth(): HardcoverPendingAuth? = pending.also { pending = null }
    override fun clearPendingAuth() { pending = null }
}

private class StubOAuth(
    private val tokens: HardcoverTokens = HardcoverTokens("at", "rt", expiresAtEpochMillis = 9_000L)
) : HardcoverOAuthClient(clientId = "cid") {
    var revoked: String? = null
    var exchangedWithVerifier: String? = null

    override suspend fun exchangeCode(code: String, verifier: String, redirectUri: String): HardcoverTokens {
        exchangedWithVerifier = verifier
        return tokens
    }

    override suspend fun revoke(token: String): Boolean {
        revoked = token
        return true
    }
}

private class StubApi(private val userId: Int = 4242, private val fail: Boolean = false) : HardcoverApi {
    override suspend fun userId(token: String): Int {
        if (fail) throw HardcoverException("nope", statusCode = 500)
        return userId
    }
    override suspend fun editionByIsbn13(token: String, isbn13: String) = null as HardcoverEdition?
    override suspend fun userBookRead(token: String, userId: Int, bookId: Int): HardcoverRead? = null
    override suspend fun insertUserBook(token: String, bookId: Int, statusId: Int): HardcoverRead? = null
    override suspend fun updateStatus(token: String, userBookId: Int, statusId: Int) = Unit
    override suspend fun updateProgress(token: String, userBookReadId: Int, pages: Int) = Unit
}

/**
 * The coordinator is the security-sensitive half: it decides whether a redirect
 * belongs to a sign-in this app started, and it never enables sending on its own.
 */
class HardcoverOAuthCoordinatorTest {

    private fun coordinator(
        store: CoordStore,
        oauth: StubOAuth? = StubOAuth(),
        api: HardcoverApi = StubApi(),
        clientId: String = "cid"
    ) = HardcoverOAuthCoordinator(
        store = store,
        oauth = oauth,
        api = api,
        clientId = clientId,
        random = Random(11)
    )

    @Test
    fun `a build without a client id cannot start authorization`() {
        val store = CoordStore()
        val coordinator = coordinator(store, clientId = "")

        assertFalse(coordinator.isConfigured)
        assertNull("no URL may be produced without a client id", coordinator.startAuthorization())
        assertNull("nothing may be stored", store.pending)
    }

    @Test
    fun `starting stores a verifier and returns a PKCE authorization url`() {
        val store = CoordStore()
        val url = coordinator(store).startAuthorization()!!

        assertTrue(url.startsWith(HardcoverOAuth.AUTHORIZATION_ENDPOINT))
        assertTrue(url.contains("client_id=cid"))
        assertTrue(url.contains("code_challenge="))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertNotNull("the verifier must be stored before the browser opens", store.pending)
        assertTrue(url.contains("state=${store.pending!!.state}"))
    }

    @Test
    fun `a redirect completes the connection and caches the account id`() = runBlocking {
        val store = CoordStore()
        val coordinator = coordinator(store)
        coordinator.startAuthorization()
        val state = store.pending!!.state

        val config = coordinator.completeAuthorization(
            "com.jdluu.leafline:/oauth2/callback?code=the-code&state=$state"
        )

        assertEquals("at", config.token)
        assertEquals("rt", config.refreshToken)
        assertEquals(4242, config.userId)
        assertEquals(HardcoverAuthMethod.OAUTH, config.authMethod)
        assertFalse("connecting must not switch sending on by itself", config.enabled)
        assertEquals(config, store.config)
    }

    @Test
    fun `a redirect whose state does not match is refused`() = runBlocking {
        val store = CoordStore()
        val coordinator = coordinator(store)
        coordinator.startAuthorization()

        val failure = runCatching {
            coordinator.completeAuthorization(
                "com.jdluu.leafline:/oauth2/callback?code=the-code&state=someone-elses"
            )
        }.exceptionOrNull()

        assertTrue("expected a refusal, got $failure", failure is HardcoverOAuthException)
        assertNull("nothing may be saved from a mismatched redirect", store.config)
    }

    @Test
    fun `a redirect with no pending authorization is refused`() = runBlocking {
        val store = CoordStore()

        val failure = runCatching {
            coordinator(store).completeAuthorization(
                "com.jdluu.leafline:/oauth2/callback?code=the-code&state=whatever"
            )
        }.exceptionOrNull()

        assertTrue(failure is HardcoverOAuthException)
    }

    @Test
    fun `the stored verifier is what gets exchanged`() = runBlocking {
        val store = CoordStore()
        val oauth = StubOAuth()
        val coordinator = coordinator(store, oauth)
        coordinator.startAuthorization()
        val verifier = store.pending!!.verifier
        val state = store.pending!!.state

        coordinator.completeAuthorization("com.jdluu.leafline:/oauth2/callback?code=c&state=$state")

        assertEquals(verifier, oauth.exchangedWithVerifier)
    }

    @Test
    fun `a denial from the server is surfaced with its description`() = runBlocking {
        val store = CoordStore()
        val coordinator = coordinator(store)
        coordinator.startAuthorization()

        val failure = runCatching {
            coordinator.completeAuthorization(
                "com.jdluu.leafline:/oauth2/callback?error=access_denied&error_description=Nope"
            )
        }.exceptionOrNull() as HardcoverOAuthException

        assertEquals("access_denied", failure.errorCode)
        assertEquals("Nope", failure.message)
    }

    @Test
    fun `a token that cannot read the profile fails here rather than later`() = runBlocking {
        val store = CoordStore()
        val coordinator = coordinator(store, api = StubApi(fail = true))
        coordinator.startAuthorization()
        val state = store.pending!!.state

        val failure = runCatching {
            coordinator.completeAuthorization("com.jdluu.leafline:/oauth2/callback?code=c&state=$state")
        }.exceptionOrNull()

        assertTrue(failure is HardcoverOAuthException)
        assertNull("a token that cannot read the account must not be saved", store.config)
    }

    @Test
    fun `disconnecting revokes and forgets`() = runBlocking {
        val store = CoordStore(
            HardcoverConfig.oauth(HardcoverTokens("at", "rt", expiresAtEpochMillis = 1L), userId = 5)
        )
        val oauth = StubOAuth()

        coordinator(store, oauth).disconnect()

        assertEquals("the refresh token is the one worth revoking", "rt", oauth.revoked)
        assertNull(store.config)
    }

    @Test
    fun `disconnecting forgets even when revocation fails`() = runBlocking {
        val store = CoordStore(HardcoverConfig.apiToken("pat", userId = 1))

        coordinator(store, oauth = null).disconnect()

        assertNull("a local disconnect must not depend on the server", store.config)
    }
}
