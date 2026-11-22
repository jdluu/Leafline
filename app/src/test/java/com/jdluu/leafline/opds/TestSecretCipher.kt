package com.jdluu.leafline.opds

import com.jdluu.leafline.storage.SecretCipher

/**
 * Reversible stand-in for the Keystore-backed cipher.
 *
 * Robolectric has no AndroidKeyStore, and a cipher that fell back to plaintext
 * would let a "credentials are encrypted" regression pass unnoticed, so this keeps
 * the same on-disk shape (`enc:v1:` prefix) while being trivially reversible.
 */
internal class TestSecretCipher : SecretCipher {

    override fun encrypt(value: String): String = PREFIX + value.reversed()

    override fun decrypt(stored: String): String? =
        if (stored.startsWith(PREFIX)) stored.removePrefix(PREFIX).reversed() else null

    private companion object {
        const val PREFIX = "enc:v1:"
    }
}
