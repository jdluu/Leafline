package com.jdluu.leafline.storage

import com.jdluu.leafline.hardcover.HardcoverConfigStorage
import com.jdluu.leafline.opds.OpdsFeedStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackSecretCipherTest {

    private class TaggedCipher(private val tag: String) : SecretCipher {
        override fun encrypt(value: String): String = "$tag:$value"
        override fun decrypt(stored: String): String? =
            if (stored.startsWith("$tag:")) stored.removePrefix("$tag:") else null
    }

    @Test
    fun `encrypt always uses primary cipher`() {
        val cipher = FallbackSecretCipher(
            primary = TaggedCipher("new"),
            fallback = TaggedCipher("old")
        )

        val encrypted = cipher.encrypt("secret")

        assertEquals("new:secret", encrypted)
    }

    @Test
    fun `decrypt returns primary result before trying fallback`() {
        val cipher = FallbackSecretCipher(
            primary = TaggedCipher("new"),
            fallback = TaggedCipher("old")
        )

        assertEquals("secret", cipher.decrypt("new:secret"))
    }

    @Test
    fun `decrypt falls back to legacy cipher when primary returns null`() {
        val cipher = FallbackSecretCipher(
            primary = TaggedCipher("new"),
            fallback = TaggedCipher("old")
        )

        assertEquals("legacy-secret", cipher.decrypt("old:legacy-secret"))
    }

    @Test
    fun `decrypt returns null when neither primary nor fallback can decrypt`() {
        val cipher = FallbackSecretCipher(
            primary = TaggedCipher("new"),
            fallback = TaggedCipher("old")
        )

        assertNull(cipher.decrypt("unknown:secret"))
    }

    @Test
    fun `feature stores use distinct Keystore aliases`() {
        val aliases = listOf(
            KeystoreSecretCipher.DEFAULT_KEY_ALIAS,
            HardcoverConfigStorage.KEY_ALIAS,
            OpdsFeedStore.KEY_ALIAS
        )

        assertEquals(3, aliases.toSet().size)
        assertTrue(aliases.all { it.isNotBlank() })
    }
}
