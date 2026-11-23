package com.jdluu.leafline.hardcover

import android.content.Context
import android.content.SharedPreferences
import com.jdluu.leafline.storage.KeystoreSecretCipher
import com.jdluu.leafline.storage.SecretCipher

/**
 * Stores the Hardcover credentials on device, encrypted with an Android Keystore
 * key, so a backup of the preference file does not expose an account credential.
 *
 * Both the access token and the OAuth refresh token go through the cipher: the
 * refresh token is the more dangerous of the two, since it mints new access tokens
 * without the reader present.
 *
 * A token written before encryption existed is recognised by the missing prefix,
 * still loads, and is re-encrypted on the next save.
 */
class HardcoverConfigStorage internal constructor(
    private val prefs: SharedPreferences,
    private val cipher: SecretCipher = KeystoreSecretCipher()
) : HardcoverConfigPersistence {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override fun load(): HardcoverConfig? {
        val storedToken = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
        val method = prefs.getString(KEY_AUTH_METHOD, null)
            ?.let { runCatching { HardcoverAuthMethod.valueOf(it) }.getOrNull() }

        val token = storedToken?.let { decrypt(it) }.orEmpty()
        // An OAuth link with no access token is kept: it is a connection the reader
        // has to re-authorize, and forgetting it would silently drop the switch.
        val survivesWithoutToken = method == HardcoverAuthMethod.OAUTH
        if (token.isBlank() && !survivesWithoutToken) return null

        return HardcoverConfig(
            token = token,
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            userId = prefs.getInt(KEY_USER_ID, 0).takeIf { it > 0 },
            authMethod = method ?: HardcoverAuthMethod.API_TOKEN,
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)?.let { decrypt(it) },
            accessExpiresAtEpochMillis = prefs.getLong(KEY_EXPIRES_AT, 0L).takeIf { it > 0 }
        )
    }

    override fun save(config: HardcoverConfig) {
        prefs.edit()
            .putString(KEY_TOKEN, config.token.takeIf { it.isNotBlank() }?.let { encrypt(it) })
            .putBoolean(KEY_ENABLED, config.enabled)
            .putInt(KEY_USER_ID, config.userId ?: 0)
            .putString(KEY_AUTH_METHOD, config.authMethod.name)
            .putString(
                KEY_REFRESH_TOKEN,
                config.refreshToken?.takeIf { it.isNotBlank() }?.let { encrypt(it) }
            )
            .putLong(KEY_EXPIRES_AT, config.accessExpiresAtEpochMillis ?: 0L)
            .apply()
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_ENABLED)
            .remove(KEY_USER_ID)
            .remove(KEY_AUTH_METHOD)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_PENDING_VERIFIER)
            .remove(KEY_PENDING_STATE)
            .apply()
    }

    override fun savePendingAuth(pending: HardcoverPendingAuth) {
        prefs.edit()
            .putString(KEY_PENDING_VERIFIER, encrypt(pending.verifier))
            .putString(KEY_PENDING_STATE, pending.state)
            .apply()
    }

    /**
     * Reads the pending authorization and forgets it in the same step, so a redirect
     * replayed by the browser cannot be exchanged twice.
     */
    override fun takePendingAuth(): HardcoverPendingAuth? {
        val verifier = prefs.getString(KEY_PENDING_VERIFIER, null)?.let { decrypt(it) }
        val state = prefs.getString(KEY_PENDING_STATE, null)
        clearPendingAuth()
        if (verifier.isNullOrBlank() || state.isNullOrBlank()) return null
        return HardcoverPendingAuth(verifier = verifier, state = state)
    }

    override fun clearPendingAuth() {
        prefs.edit()
            .remove(KEY_PENDING_VERIFIER)
            .remove(KEY_PENDING_STATE)
            .apply()
    }

    private fun encrypt(value: String): String = ENC_PREFIX + cipher.encrypt(value)

    private fun decrypt(stored: String): String? =
        if (stored.startsWith(ENC_PREFIX)) {
            cipher.decrypt(stored.removePrefix(ENC_PREFIX))
        } else {
            // A value written before encryption existed.
            stored
        }

    private companion object {
        const val PREFS_NAME = "leafline_hardcover"
        const val ENC_PREFIX = "enc:v1:"
        const val KEY_TOKEN = "token"
        const val KEY_ENABLED = "enabled"
        const val KEY_USER_ID = "user_id"
        const val KEY_AUTH_METHOD = "auth_method"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_PENDING_VERIFIER = "pending_verifier"
        const val KEY_PENDING_STATE = "pending_state"
    }
}
