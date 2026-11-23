package com.jdluu.leafline.hardcover

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/**
 * OAuth tokens for Hardcover, and the arithmetic around their lifetime.
 *
 * [expiresAtEpochMillis] is stored as an absolute time so a phone that was asleep
 * for a day refreshes instead of discovering the problem mid-push.
 */
data class HardcoverTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtEpochMillis: Long? = null,
    val scope: String? = null,
    val tokenType: String = "Bearer"
) {
    /** True when the token is spent or about to be; the skew avoids racing expiry. */
    fun isExpired(nowEpochMillis: Long, skewMillis: Long = DEFAULT_SKEW_MILLIS): Boolean {
        val expiry = expiresAtEpochMillis ?: return false
        return nowEpochMillis + skewMillis >= expiry
    }

    fun authorizationHeader(): String = "$tokenType $accessToken"

    companion object {
        const val DEFAULT_SKEW_MILLIS = 60_000L
    }
}

/** Why an OAuth step failed, so the UI can say something true. */
class HardcoverOAuthException(
    message: String,
    /** OAuth error code, e.g. `invalid_grant`, when the server supplied one. */
    val errorCode: String? = null,
    val statusCode: Int? = null
) : Exception(message) {

    /** The refresh token is dead: the reader has to connect again. */
    val requiresReauthorization: Boolean
        get() = errorCode == "invalid_grant" || errorCode == "invalid_client"
}

/**
 * Hardcover's OAuth 2.0 authorization-code flow with PKCE.
 *
 * Verified against https://hardcover.app/.well-known/openid-configuration: only
 * `S256` challenges are offered, and `token_endpoint_auth_methods_supported`
 * includes `none`, which is what lets a mobile app run as a public client with no
 * secret. The secret-free design is why PKCE is mandatory here, not optional.
 *
 * No Android or OkHttp types: the network half lives in [HardcoverOAuthClient].
 */
object HardcoverOAuth {

    const val AUTHORIZATION_ENDPOINT = "https://hardcover.app/oauth2/authorize"
    const val TOKEN_ENDPOINT = "https://api.hardcover.app/oauth2/token"
    const val REVOCATION_ENDPOINT = "https://api.hardcover.app/oauth2/revoke"
    const val USERINFO_ENDPOINT = "https://api.hardcover.app/oauth2/userinfo"

    /**
     * Reverse-DNS custom scheme registered on the developer app. PKCE is what makes
     * this safe: another app on the device can claim the scheme and read the code,
     * but without the verifier the code cannot be redeemed.
     */
    const val REDIRECT_URI = "com.jdluu.leafline:/oauth2/callback"

    /** `offline_access` is what earns a refresh token; without it, tokens die hourly. */
    val SCOPES = listOf(
        "read:me",
        "read:catalog:data",
        "read:library",
        "write:library",
        "offline_access"
    )

    /** RFC 7636 allows 43..128 characters; 32 random bytes encode to 43. */
    private const val VERIFIER_BYTES = 32

    /**
     * A fresh code verifier: base64url of 32 random bytes, unpadded.
     *
     * [random] is injectable so tests are deterministic; production callers use the
     * platform's [SecureRandom].
     */
    fun createCodeVerifier(random: Random = SecureRandom().asKotlinRandom()): String {
        val bytes = ByteArray(VERIFIER_BYTES)
        random.nextBytes(bytes)
        return bytes.toBase64Url()
    }

    /** `BASE64URL(SHA256(ASCII(verifier)))`, the only method Hardcover accepts. */
    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(verifier.toByteArray(Charsets.US_ASCII)).toBase64Url()
    }

    /** Random, unguessable state: the caller must reject a redirect that echoes another. */
    fun createState(random: Random = SecureRandom().asKotlinRandom()): String {
        val bytes = ByteArray(VERIFIER_BYTES)
        random.nextBytes(bytes)
        return bytes.toBase64Url()
    }

    fun authorizationUrl(
        clientId: String,
        verifier: String,
        state: String,
        redirectUri: String = REDIRECT_URI
    ): String {
        require(clientId.isNotBlank()) { "A client id is required to authorize" }
        val params = listOf(
            "response_type" to "code",
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "scope" to SCOPES.joinToString(" "),
            "state" to state,
            "code_challenge" to codeChallenge(verifier),
            "code_challenge_method" to "S256"
        )
        return AUTHORIZATION_ENDPOINT + "?" + params.joinToString("&") { (k, v) ->
            "$k=${v.percentEncode()}"
        }
    }

    /** What the browser sent back to [redirectUri]. */
    sealed interface Redirect {
        data class Authorized(val code: String, val state: String?) : Redirect
        data class Denied(val error: String, val description: String?) : Redirect
    }

    /**
     * Reads the redirect without a URL parser: the registered URI is not a
     * hierarchical `scheme://host` URL, and `java.net.URI` is unreliable on it.
     *
     * A redirect carrying neither `code` nor `error` is [Redirect.Denied] rather than
     * an exception, because the browser can hand us anything a user typed.
     */
    fun parseRedirect(uri: String): Redirect {
        val query = uri.substringAfter('?', missingDelimiterValue = "")
        val params = query.split('&')
            .filter { it.isNotBlank() }
            .associate { pair ->
                val name = pair.substringBefore('=').percentDecode()
                val value = pair.substringAfter('=', missingDelimiterValue = "").percentDecode()
                name to value
            }

        val error = params["error"]
        if (error != null) {
            return Redirect.Denied(error, params["error_description"])
        }
        val code = params["code"]
        if (code.isNullOrBlank()) {
            return Redirect.Denied("missing_code", "The authorization response carried no code")
        }
        return Redirect.Authorized(code, params["state"]?.takeIf { it.isNotBlank() })
    }

    /**
     * Parses a token or refresh response.
     *
     * [nowEpochMillis] is passed in rather than read from the clock, so the expiry is
     * a pure function of the response that tests can pin down.
     */
    fun parseTokens(body: String, nowEpochMillis: Long): HardcoverTokens {
        val json = com.google.gson.JsonParser.parseString(body).asJsonObject
        json.get("error")?.takeIf { !it.isJsonNull }?.let { error ->
            val code = error.asString
            val description = json.get("error_description")?.takeIf { !it.isJsonNull }?.asString
            throw HardcoverOAuthException(
                description ?: "Hardcover refused the token request ($code)",
                errorCode = code
            )
        }
        val accessToken = json.get("access_token")?.takeIf { !it.isJsonNull }?.asString
            ?: throw HardcoverOAuthException("Hardcover returned no access token")
        val expiresIn = json.get("expires_in")?.takeIf { !it.isJsonNull }
            ?.let { runCatching { it.asLong }.getOrNull() }

        return HardcoverTokens(
            accessToken = accessToken,
            refreshToken = json.get("refresh_token")?.takeIf { !it.isJsonNull }?.asString,
            expiresAtEpochMillis = expiresIn?.let { nowEpochMillis + it * 1000L },
            scope = json.get("scope")?.takeIf { !it.isJsonNull }?.asString,
            tokenType = json.get("token_type")?.takeIf { !it.isJsonNull }?.asString ?: "Bearer"
        )
    }

    private fun ByteArray.toBase64Url(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(this)

    private fun String.percentEncode(): String =
        java.net.URLEncoder.encode(this, "UTF-8").replace("+", "%20")

    private fun String.percentDecode(): String =
        runCatching { java.net.URLDecoder.decode(this, "UTF-8") }.getOrDefault(this)

}
