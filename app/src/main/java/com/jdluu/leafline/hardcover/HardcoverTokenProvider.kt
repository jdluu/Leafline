package com.jdluu.leafline.hardcover

/**
 * Hands out a usable access token, refreshing it first when it is spent.
 *
 * Refreshing here rather than at each call site keeps the whole app from racing the
 * same expiry: every push and every background run asks this one place.
 *
 * When the server says the refresh token is dead, the connection is kept but emptied
 * of credentials and switched off. Keeping it (rather than deleting it) lets the
 * settings row say "sign in again" instead of pretending Hardcover was never set up.
 */
class HardcoverTokenProvider(
    private val store: HardcoverConfigPersistence,
    private val oauth: HardcoverOAuthClient?,
    private val clock: () -> Long = System::currentTimeMillis
) {

    /** Returns a token, refreshing if needed. Null when nothing usable is stored. */
    suspend fun accessToken(): String? {
        val config = store.load() ?: return null
        if (config.token.isBlank()) return null

        val expired = config.accessExpiresAtEpochMillis
            ?.let { clock() + HardcoverTokens.DEFAULT_SKEW_MILLIS >= it }
            ?: false
        if (!expired) return config.token

        val refreshToken = config.refreshToken ?: return null
        val client = oauth ?: return null

        return try {
            val fresh = client.refresh(refreshToken)
            val merged = config.copy(
                token = fresh.accessToken,
                // A server that rotates refresh tokens sends a new one; one that does
                // not sends none, and keeping the old one is what keeps us connected.
                refreshToken = fresh.refreshToken ?: refreshToken,
                accessExpiresAtEpochMillis = fresh.expiresAtEpochMillis
            )
            store.save(merged)
            HardcoverConfigStore.config = merged
            merged.token
        } catch (e: HardcoverOAuthException) {
            if (e.requiresReauthorization) markNeedsReauthorization(config) else Unit
            null
        } catch (e: Exception) {
            // A transient network failure must not destroy a working connection.
            null
        }
    }

    private fun markNeedsReauthorization(config: HardcoverConfig) {
        val cleared = config.copy(
            token = "",
            refreshToken = null,
            accessExpiresAtEpochMillis = null,
            enabled = false,
            authMethod = HardcoverAuthMethod.OAUTH
        )
        store.save(cleared)
        HardcoverConfigStore.config = cleared
    }
}
