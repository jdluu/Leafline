package com.jdluu.leafline.hardcover

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * The three network steps of Hardcover's OAuth flow: redeem a code, refresh, revoke.
 *
 * A public client sends no secret — the only proof it is the same app that started
 * the flow is the PKCE verifier presented at exchange time.
 */
open class HardcoverOAuthClient(
    private val clientId: String,
    private val tokenEndpoint: String = HardcoverOAuth.TOKEN_ENDPOINT,
    private val revocationEndpoint: String = HardcoverOAuth.REVOCATION_ENDPOINT,
    private val userAgent: String = "Leafline/unknown",
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Redeems the authorization code for tokens. */
    open suspend fun exchangeCode(
        code: String,
        verifier: String,
        redirectUri: String = HardcoverOAuth.REDIRECT_URI
    ): HardcoverTokens = withContext(dispatcher) {
        post(
            tokenEndpoint,
            FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .add("client_id", clientId)
                .add("code_verifier", verifier)
                .build()
        )
    }

    /**
     * Trades a refresh token for a new access token.
     *
     * A server may or may not return a new refresh token; when it does not, the
     * caller must keep the old one, which is why [HardcoverTokens.refreshToken] is
     * nullable here and the caller does the merge.
     */
    open suspend fun refresh(refreshToken: String): HardcoverTokens = withContext(dispatcher) {
        post(
            tokenEndpoint,
            FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .build()
        )
    }

    /**
     * Revokes a token at the server. Best effort: a revocation that fails still gets
     * forgotten locally, so disconnecting always disconnects on this device.
     */
    open suspend fun revoke(token: String): Boolean = withContext(dispatcher) {
        runCatching {
            val request = Request.Builder()
                .url(revocationEndpoint)
                .header("user-agent", userAgent)
                .post(
                    FormBody.Builder()
                        .add("token", token)
                        .add("client_id", clientId)
                        .build()
                )
                .build()
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun post(url: String, form: FormBody): HardcoverTokens {
        val request = Request.Builder()
            .url(url)
            .header("user-agent", userAgent)
            .header("accept", "application/json")
            .post(form)
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                throw HardcoverOAuthException(
                    "Hardcover returned an empty token response",
                    statusCode = response.code
                )
            }
            val tokens = try {
                HardcoverOAuth.parseTokens(body, clock())
            } catch (e: HardcoverOAuthException) {
                // Keep the server's status alongside its error code.
                throw HardcoverOAuthException(
                    e.message ?: "Hardcover refused the token request",
                    errorCode = e.errorCode,
                    statusCode = response.code
                )
            }
            tokens
        }
    }
}
