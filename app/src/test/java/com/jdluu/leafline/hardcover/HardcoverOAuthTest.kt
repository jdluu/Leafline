package com.jdluu.leafline.hardcover

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HardcoverOAuthTest {

    // RFC 7636 Appendix B, verbatim: an independent check that the challenge is a
    // real SHA-256/base64url and not merely self-consistent.
    private val rfcVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    private val rfcChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

    @Test
    fun `the code challenge matches the RFC 7636 example`() {
        assertEquals(rfcChallenge, HardcoverOAuth.codeChallenge(rfcVerifier))
    }

    @Test
    fun `a generated verifier is url-safe and within the allowed length`() {
        val verifier = HardcoverOAuth.createCodeVerifier(Random(42))

        assertTrue("length was ${verifier.length}", verifier.length in 43..128)
        assertTrue("not url-safe: $verifier", verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertFalse("padding is not allowed", verifier.contains('='))
    }

    @Test
    fun `two verifiers differ`() {
        val random = Random(7)
        assertTrue(HardcoverOAuth.createCodeVerifier(random) != HardcoverOAuth.createCodeVerifier(random))
    }

    @Test
    fun `the authorization url carries every parameter the server requires`() {
        val url = HardcoverOAuth.authorizationUrl(
            clientId = "cid123",
            verifier = rfcVerifier,
            state = "state-abc"
        )

        assertTrue(url.startsWith(HardcoverOAuth.AUTHORIZATION_ENDPOINT + "?"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("client_id=cid123"))
        assertTrue(url.contains("code_challenge=$rfcChallenge"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("state=state-abc"))
        assertTrue(
            "the redirect uri must be encoded",
            url.contains("redirect_uri=com.jdluu.leafline%3A%2Foauth2%2Fcallback")
        )
        assertTrue(
            "scopes are space separated and encoded, not plus-joined",
            url.contains("scope=read%3Ame+read%3Acatalog%3Adata".replace("+", "%20"))
        )
    }

    @Test
    fun `a blank client id is refused rather than producing a broken url`() {
        val failure = runCatching {
            HardcoverOAuth.authorizationUrl(clientId = " ", verifier = rfcVerifier, state = "s")
        }.exceptionOrNull()

        assertTrue("expected an IllegalArgument, got $failure", failure is IllegalArgumentException)
    }

    @Test
    fun `a redirect carrying a code is parsed with its state`() {
        val redirect = HardcoverOAuth.parseRedirect(
            "com.jdluu.leafline:/oauth2/callback?code=abc123&state=st-1"
        )

        assertTrue(redirect is HardcoverOAuth.Redirect.Authorized)
        redirect as HardcoverOAuth.Redirect.Authorized
        assertEquals("abc123", redirect.code)
        assertEquals("st-1", redirect.state)
    }

    @Test
    fun `an encoded redirect value is decoded`() {
        val redirect = HardcoverOAuth.parseRedirect(
            "com.jdluu.leafline:/oauth2/callback?code=a%2Fb%3D&state=x"
        ) as HardcoverOAuth.Redirect.Authorized

        assertEquals("a/b=", redirect.code)
    }

    @Test
    fun `a denial is reported with the server's description`() {
        val redirect = HardcoverOAuth.parseRedirect(
            "com.jdluu.leafline:/oauth2/callback?error=access_denied&error_description=User+said+no"
        )

        assertTrue(redirect is HardcoverOAuth.Redirect.Denied)
        redirect as HardcoverOAuth.Redirect.Denied
        assertEquals("access_denied", redirect.error)
        assertEquals("User said no", redirect.description)
    }

    @Test
    fun `a redirect with no query is a denial, not a crash`() {
        val redirect = HardcoverOAuth.parseRedirect("com.jdluu.leafline:/oauth2/callback")

        assertTrue(redirect is HardcoverOAuth.Redirect.Denied)
        assertEquals("missing_code", (redirect as HardcoverOAuth.Redirect.Denied).error)
    }

    @Test
    fun `a token response becomes an absolute expiry`() {
        val tokens = HardcoverOAuth.parseTokens(
            """{"access_token":"at","token_type":"Bearer","expires_in":3600,"refresh_token":"rt","scope":"read:me"}""",
            nowEpochMillis = 1_000_000L
        )

        assertEquals("at", tokens.accessToken)
        assertEquals("rt", tokens.refreshToken)
        assertEquals(1_000_000L + 3_600_000L, tokens.expiresAtEpochMillis)
        assertEquals("read:me", tokens.scope)
        assertFalse("a fresh token must not read as expired", tokens.isExpired(1_000_000L))
    }

    @Test
    fun `a token without expires_in never reads as expired`() {
        val tokens = HardcoverOAuth.parseTokens("""{"access_token":"at"}""", nowEpochMillis = 0L)

        assertNull(tokens.expiresAtEpochMillis)
        assertFalse(tokens.isExpired(Long.MAX_VALUE / 2))
    }

    @Test
    fun `a token inside the skew window is treated as expired`() {
        val tokens = HardcoverOAuth.parseTokens(
            """{"access_token":"at","expires_in":3600}""",
            nowEpochMillis = 0L
        )

        // 30 seconds before expiry: refreshing early beats failing a push.
        assertTrue(tokens.isExpired(3_600_000L - 30_000L))
    }

    @Test
    fun `an oauth error body becomes an exception carrying its code`() {
        val failure = runCatching {
            HardcoverOAuth.parseTokens(
                """{"error":"invalid_grant","error_description":"Refresh token is invalid"}""",
                nowEpochMillis = 0L
            )
        }.exceptionOrNull()

        assertTrue("expected HardcoverOAuthException, got $failure", failure is HardcoverOAuthException)
        failure as HardcoverOAuthException
        assertEquals("invalid_grant", failure.errorCode)
        assertEquals("Refresh token is invalid", failure.message)
        assertTrue("a dead refresh token needs re-authorization", failure.requiresReauthorization)
    }

    @Test
    fun `a response with no access token is a failure`() {
        val failure = runCatching {
            HardcoverOAuth.parseTokens("""{"token_type":"Bearer"}""", nowEpochMillis = 0L)
        }.exceptionOrNull()

        assertTrue(failure is HardcoverOAuthException)
    }
}
