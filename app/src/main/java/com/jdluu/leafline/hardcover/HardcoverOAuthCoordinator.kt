package com.jdluu.leafline.hardcover

import kotlin.random.Random

/**
 * Drives the browser half of Hardcover's OAuth flow: start, finish, disconnect.
 *
 * [clientId] is blank in a build that has not been given one, and every entry point
 * then reports "not configured" instead of opening a URL that cannot work. The
 * client id is not a secret — it is the PKCE verifier, kept only in the encrypted
 * store, that proves this app started the flow.
 */
class HardcoverOAuthCoordinator(
    private val store: HardcoverConfigPersistence,
    private val oauth: HardcoverOAuthClient?,
    private val api: HardcoverApi,
    private val clientId: String,
    private val random: Random? = null,
    private val clock: () -> Long = System::currentTimeMillis
) {

    /** False when this build carries no client id, so the UI can hide the button. */
    val isConfigured: Boolean
        get() = clientId.isNotBlank() && oauth != null

    /**
     * Stores a fresh verifier and returns the URL to open in a browser.
     *
     * Returns null when the build has no client id. The pending verifier is written
     * before the browser opens, because the process can be killed while the reader is
     * away signing in.
     */
    fun startAuthorization(): String? {
        if (!isConfigured) return null

        val source = random ?: randomSource()
        val verifier = HardcoverOAuth.createCodeVerifier(source)
        val state = HardcoverOAuth.createState(source)
        store.savePendingAuth(HardcoverPendingAuth(verifier = verifier, state = state))
        return HardcoverOAuth.authorizationUrl(clientId = clientId, verifier = verifier, state = state)
    }

    /**
     * Finishes the flow from the redirect the browser handed back.
     *
     * Refuses a redirect whose `state` does not match the one stored at start: that is
     * the check that stops a different app's callback being redeemed here.
     */
    suspend fun completeAuthorization(redirectUri: String): HardcoverConfig {
        val pending = store.takePendingAuth()
            ?: throw HardcoverOAuthException(
                "This sign-in was not started here, or it has already been used. Try connecting again."
            )

        when (val redirect = HardcoverOAuth.parseRedirect(redirectUri)) {
            is HardcoverOAuth.Redirect.Denied -> throw HardcoverOAuthException(
                redirect.description ?: "Hardcover sign-in was refused (${redirect.error})",
                errorCode = redirect.error
            )

            is HardcoverOAuth.Redirect.Authorized -> {
                if (redirect.state != null && redirect.state != pending.state) {
                    throw HardcoverOAuthException(
                        "The sign-in response did not match the request. Try connecting again."
                    )
                }

                val client = oauth ?: throw HardcoverOAuthException("Hardcover sign-in is not configured in this build")
                val tokens = client.exchangeCode(code = redirect.code, verifier = pending.verifier)

                // The account id is fetched now so a later push costs no extra request,
                // and a token that cannot read the profile fails here rather than later.
                val userId = try {
                    api.userId(tokens.accessToken)
                } catch (e: HardcoverException) {
                    throw HardcoverOAuthException(
                        if (e.isUnauthorized) {
                            "Hardcover issued a token that was not accepted. Try connecting again."
                        } else {
                            e.message ?: "Could not read the Hardcover profile"
                        },
                        statusCode = e.statusCode
                    )
                }

                val config = HardcoverConfig.oauth(tokens = tokens, userId = userId, enabled = false)
                store.save(config)
                HardcoverConfigStore.config = config
                return config
            }
        }
    }

    /**
     * Forgets the connection, revoking first when the server can be told.
     *
     * The local state is cleared even if revocation fails, because the reader asked to
     * disconnect and leaving a live token behind would contradict that.
     */
    suspend fun disconnect() {
        val config = store.load()
        if (config != null) {
            val revocable = config.refreshToken ?: config.token
            if (revocable.isNotBlank()) oauth?.revoke(revocable)
        }
        store.clear()
        HardcoverConfigStore.config = null
    }

    private fun randomSource(): Random = kotlin.random.Random(System.nanoTime())
}
