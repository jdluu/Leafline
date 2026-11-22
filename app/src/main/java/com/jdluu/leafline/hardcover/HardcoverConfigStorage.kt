package com.jdluu.leafline.hardcover

import android.content.Context
import android.content.SharedPreferences
import com.jdluu.leafline.storage.KeystoreSecretCipher
import com.jdluu.leafline.storage.SecretCipher

/**
 * Stores the Hardcover token on device, encrypted with an Android Keystore key, so
 * a backup of the preference file does not expose an account credential.
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
            ?: return null
        val token = if (storedToken.startsWith(ENC_PREFIX)) {
            cipher.decrypt(storedToken.removePrefix(ENC_PREFIX)) ?: return null
        } else {
            storedToken
        }
        return HardcoverConfig(
            token = token,
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            userId = prefs.getInt(KEY_USER_ID, 0).takeIf { it > 0 }
        )
    }

    override fun save(config: HardcoverConfig) {
        prefs.edit()
            .putString(
                KEY_TOKEN,
                if (config.token.isBlank()) null else ENC_PREFIX + cipher.encrypt(config.token)
            )
            .putBoolean(KEY_ENABLED, config.enabled)
            .putInt(KEY_USER_ID, config.userId ?: 0)
            .apply()
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_ENABLED)
            .remove(KEY_USER_ID)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "leafline_hardcover"
        const val ENC_PREFIX = "enc:v1:"
        const val KEY_TOKEN = "token"
        const val KEY_ENABLED = "enabled"
        const val KEY_USER_ID = "user_id"
    }
}
