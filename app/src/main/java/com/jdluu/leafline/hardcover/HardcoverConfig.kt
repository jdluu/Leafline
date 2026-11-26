package com.jdluu.leafline.hardcover

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How the account was connected, which decides how it is refreshed and revoked. */
enum class HardcoverAuthMethod {
    /** Nothing connected. */
    NONE,

    /** A personal API token the reader pasted; no refresh, no expiry. */
    API_TOKEN,

    /** OAuth: an access token with an expiry and usually a refresh token. */
    OAUTH
}

/**
 * Hardcover connection settings.
 *
 * [enabled] is false until the reader turns it on, and nothing is sent while it is
 * false. [userId] is cached from the token check so a push does not spend one
 * request per book just to ask who the user is.
 *
 * [token] is always the current access token, whichever way the account was
 * connected. [refreshToken] and [accessExpiresAtEpochMillis] exist only for OAuth.
 */
data class HardcoverConfig(
    val token: String = "",
    val enabled: Boolean = false,
    val userId: Int? = null,
    val authMethod: HardcoverAuthMethod = HardcoverAuthMethod.NONE,
    val refreshToken: String? = null,
    val accessExpiresAtEpochMillis: Long? = null
) {

    /** True when the connection needs the reader to sign in again. */
    val needsReauthorization: Boolean
        get() = authMethod == HardcoverAuthMethod.OAUTH &&
            token.isBlank()

    companion object {
        /** A pasted token: never expires, nothing to refresh. */
        fun apiToken(token: String, userId: Int? = null, enabled: Boolean = false) = HardcoverConfig(
            token = token,
            enabled = enabled,
            userId = userId,
            authMethod = HardcoverAuthMethod.API_TOKEN
        )

        /** An OAuth connection. */
        fun oauth(tokens: HardcoverTokens, userId: Int? = null, enabled: Boolean = false) =
            HardcoverConfig(
                token = tokens.accessToken,
                enabled = enabled,
                userId = userId,
                authMethod = HardcoverAuthMethod.OAUTH,
                refreshToken = tokens.refreshToken,
                accessExpiresAtEpochMillis = tokens.expiresAtEpochMillis
            )
    }
}

/** In-memory holder, restored from disk when [com.jdluu.leafline.di.AppContainer] builds. */
object HardcoverConfigStore {
    private val _state = MutableStateFlow<HardcoverConfig?>(null)
    val state: StateFlow<HardcoverConfig?> = _state.asStateFlow()

    var config: HardcoverConfig?
        get() = _state.value
        set(value) {
            _state.value = value
        }
}

/**
 * An authorization the reader started but has not finished.
 *
 * The verifier must outlive the process: the reader leaves Leafline for a browser and
 * the system may kill the app while they are away.
 */
data class HardcoverPendingAuth(
    val verifier: String,
    val state: String
)

/** Durable storage seam, kept Android-free for tests. */
interface HardcoverConfigPersistence {
    fun load(): HardcoverConfig?
    fun save(config: HardcoverConfig)
    fun clear()

    /** Stored encrypted, because the verifier completes the PKCE exchange. */
    fun savePendingAuth(pending: HardcoverPendingAuth)
    fun takePendingAuth(): HardcoverPendingAuth?
    fun clearPendingAuth()
}
