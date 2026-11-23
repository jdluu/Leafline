package com.jdluu.leafline.hardcover

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jdluu.leafline.storage.SecretCipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class FakeCipher : SecretCipher {
    override fun encrypt(value: String): String = "enc:v1:" + value.reversed()
    override fun decrypt(stored: String): String? =
        if (stored.startsWith("enc:v1:")) stored.removePrefix("enc:v1:").reversed() else null
}

/**
 * A Hardcover token can delete an account's data, so it must never sit in the
 * preference file in a readable form.
 */
@RunWith(RobolectricTestRunner::class)
class HardcoverConfigStorageTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: android.content.SharedPreferences

    @Before
    fun setUp() {
        prefs = context.getSharedPreferences("leafline_hardcover", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    private fun raw(): String = prefs.all.toString()

    private fun storage() = HardcoverConfigStorage(prefs, FakeCipher())

    @Test
    fun `a token is never written in plaintext`() {
        storage().save(HardcoverConfig.apiToken("hc_live_token_123", userId = 42, enabled = true))

        assertFalse("token reached disk: ${raw()}", raw().contains("hc_live_token_123"))
        assertTrue("token did not go through the cipher", raw().contains("enc:v1:"))
    }

    @Test
    fun `a saved token round-trips with its settings`() {
        storage().save(HardcoverConfig.apiToken("hc_live_token_123", userId = 42, enabled = true))

        val loaded = storage().load()!!
        assertEquals("hc_live_token_123", loaded.token)
        assertTrue(loaded.enabled)
        assertEquals(42, loaded.userId)
    }

    @Test
    fun `nothing saved loads as null`() {
        assertNull(storage().load())
    }

    @Test
    fun `clearing removes the token entirely`() {
        val storage = storage()
        storage.save(HardcoverConfig(token = "hc_live_token_123", enabled = true))
        storage.clear()

        assertNull(storage.load())
        assertFalse(raw().contains("hc_live_token_123"))
    }

    @Test
    fun `an oauth refresh token is never written in plaintext either`() {
        storage().save(
            HardcoverConfig.oauth(
                HardcoverTokens(
                    accessToken = "at_access",
                    refreshToken = "rt_refresh_secret",
                    expiresAtEpochMillis = 5_000L
                ),
                userId = 7
            )
        )

        val raw = raw()
        assertFalse("refresh token reached disk: $raw", raw.contains("rt_refresh_secret"))
        assertFalse("access token reached disk: $raw", raw.contains("at_access"))
    }

    @Test
    fun `an oauth connection round-trips its expiry and refresh token`() {
        storage().save(
            HardcoverConfig.oauth(
                HardcoverTokens(
                    accessToken = "at_access",
                    refreshToken = "rt_refresh",
                    expiresAtEpochMillis = 1_700_000_000_000L
                ),
                userId = 7,
                enabled = true
            )
        )

        val loaded = storage().load()!!
        assertEquals("at_access", loaded.token)
        assertEquals("rt_refresh", loaded.refreshToken)
        assertEquals(1_700_000_000_000L, loaded.accessExpiresAtEpochMillis)
        assertEquals(HardcoverAuthMethod.OAUTH, loaded.authMethod)
        assertTrue(loaded.enabled)
        assertFalse("a token with a future expiry is not stuck needing sign-in", loaded.needsReauthorization)
    }

    @Test
    fun `an oauth link whose token was dropped still loads so the row can ask for sign-in`() {
        storage().save(
            HardcoverConfig(
                token = "",
                authMethod = HardcoverAuthMethod.OAUTH,
                enabled = false
            )
        )

        val loaded = storage().load()
        assertTrue("the connection must survive so Settings can offer sign-in again", loaded != null)
        assertTrue(loaded!!.needsReauthorization)
    }

    @Test
    fun `a pending authorization round-trips and is consumed once`() {
        val storage = storage()
        storage.savePendingAuth(HardcoverPendingAuth(verifier = "the-verifier", state = "the-state"))

        assertFalse("the verifier is a credential", raw().contains("the-verifier"))

        val taken = storage.takePendingAuth()!!
        assertEquals("the-verifier", taken.verifier)
        assertEquals("the-state", taken.state)

        assertNull("a second read must find nothing, so a redirect cannot be replayed", storage.takePendingAuth())
    }

    @Test
    fun `clearing removes a pending authorization too`() {
        val storage = storage()
        storage.savePendingAuth(HardcoverPendingAuth(verifier = "v", state = "s"))
        storage.clear()

        assertNull(storage.takePendingAuth())
    }

    @Test
    fun `a legacy plaintext token still loads and is re-encrypted on save`() {
        prefs.edit().putString("token", "legacy_token").putBoolean("enabled", true).commit()

        val storage = storage()
        assertEquals("legacy_token", storage.load()!!.token)

        storage.save(storage.load()!!)
        assertFalse("legacy plaintext survived a save", raw().contains("legacy_token"))
    }
}
