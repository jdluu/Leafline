package com.jdluu.leafline.hardcover

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HardcoverOAuthClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(now: Long = 1_000L) = HardcoverOAuthClient(
        clientId = "cid123",
        tokenEndpoint = server.url("/oauth2/token").toString(),
        revocationEndpoint = server.url("/oauth2/revoke").toString(),
        userAgent = "Leafline/test",
        clock = { now }
    )

    @Test
    fun `exchanging a code sends the pkce verifier and no secret`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"access_token":"at","token_type":"Bearer","expires_in":3600,"refresh_token":"rt"}"""
            )
        )

        val tokens = client().exchangeCode(code = "the-code", verifier = "the-verifier")

        assertEquals("at", tokens.accessToken)
        assertEquals("rt", tokens.refreshToken)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("grant_type=authorization_code"))
        assertTrue(body.contains("code=the-code"))
        assertTrue(body.contains("code_verifier=the-verifier"))
        assertTrue(body.contains("client_id=cid123"))
        assertTrue("a public client must not send a secret", !body.contains("client_secret"))
    }

    @Test
    fun `refreshing sends the refresh grant without a verifier`() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{"access_token":"new","expires_in":3600}""")
        )

        val tokens = client().refresh("old-refresh")

        assertEquals("new", tokens.accessToken)
        assertNull("nothing to carry over when the server omits it", tokens.refreshToken)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("grant_type=refresh_token"))
        assertTrue(body.contains("refresh_token=old-refresh"))
        assertTrue(!body.contains("code_verifier"))
    }

    @Test
    fun `an invalid grant is reported as needing re-authorization`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"error":"invalid_grant","error_description":"Refresh token expired"}""")
        )

        val failure = runCatching { client().refresh("stale") }.exceptionOrNull()

        assertTrue("expected HardcoverOAuthException, got $failure", failure is HardcoverOAuthException)
        failure as HardcoverOAuthException
        assertEquals("invalid_grant", failure.errorCode)
        assertEquals(400, failure.statusCode)
        assertTrue("the reader must be asked to reconnect", failure.requiresReauthorization)
    }

    @Test
    fun `a rejected client id keeps its status for diagnosis`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody("""{"error":"invalid_client"}""")
        )

        val failure = runCatching { client().exchangeCode("c", "v") }.exceptionOrNull() as HardcoverOAuthException

        assertEquals("invalid_client", failure.errorCode)
        assertEquals(401, failure.statusCode)
    }

    @Test
    fun `an empty token body fails instead of yielding a blank token`() = runBlocking {
        server.enqueue(MockResponse().setBody(""))

        val failure = runCatching { client().exchangeCode("c", "v") }.exceptionOrNull()

        assertTrue(failure is HardcoverOAuthException)
    }

    @Test
    fun `revoking posts the token and reports success`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        assertTrue(client().revoke("at"))

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("token=at"))
        assertTrue(body.contains("client_id=cid123"))
    }

    @Test
    fun `a failed revocation is reported without throwing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(false, client().revoke("at"))
    }
}
