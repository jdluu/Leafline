package com.jdluu.leafline.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.jdluu.leafline.sync.KoreaderSyncConfig
import com.jdluu.leafline.sync.KoreaderSyncConfigPersistence
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts a single value at rest. Injected so the storage logic is testable off-device. */
internal interface SecretCipher {
    fun encrypt(value: String): String

    /** Returns null when the stored value cannot be decrypted (for example after a key reset). */
    fun decrypt(stored: String): String?
}

/**
 * Stores KOReader sync settings on device, keeping the password encrypted with an
 * Android Keystore key so a device backup of the preference file does not expose it.
 */
class KoreaderSyncConfigStorage internal constructor(
    private val prefs: SharedPreferences,
    private val cipher: SecretCipher = KeystoreSecretCipher()
) : KoreaderSyncConfigPersistence {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override fun load(): KoreaderSyncConfig? {
        val serverUrl = prefs.getString(KEY_SERVER_URL, null)?.takeIf { it.isNotBlank() }
            ?: return null
        val encryptedPassword = prefs.getString(KEY_PASSWORD, null)
        return KoreaderSyncConfig(
            serverUrl = serverUrl,
            username = prefs.getString(KEY_USERNAME, null).orEmpty(),
            password = encryptedPassword?.let { cipher.decrypt(it) }.orEmpty(),
            enabled = prefs.getBoolean(KEY_ENABLED, false)
        )
    }

    override fun save(config: KoreaderSyncConfig) {
        prefs.edit()
            .putString(KEY_SERVER_URL, config.serverUrl)
            .putString(KEY_USERNAME, config.username)
            .putString(
                KEY_PASSWORD,
                if (config.password.isEmpty()) null else cipher.encrypt(config.password)
            )
            .putBoolean(KEY_ENABLED, config.enabled)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "koreader_sync"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_ENABLED = "enabled"
    }
}

/** AES/GCM backed by a non-exportable Android Keystore key created on first use. */
internal class KeystoreSecretCipher(
    private val keyAlias: String = "leafline_koreader_sync"
) : SecretCipher {

    override fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return encode(cipher.iv) + SEPARATOR + encode(payload)
    }

    override fun decrypt(stored: String): String? = runCatching {
        val separator = stored.indexOf(SEPARATOR)
        require(separator > 0) { "malformed payload" }
        val iv = decode(stored.substring(0, separator))
        val payload = decode(stored.substring(separator + 1))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(payload), Charsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val SEPARATOR = ":"
    }
}
