package com.jdluu.leafline.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jdluu.leafline.sync.KoreaderSyncConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KoreaderSyncConfigStorageTest {

    /** Reversible stand-in so the storage logic is testable without the Android Keystore. */
    private class FakeCipher : SecretCipher {
        override fun encrypt(value: String): String = value.reversed()

        override fun decrypt(stored: String): String? = stored.reversed()
    }

    private lateinit var storage: KoreaderSyncConfigStorage

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        storage = KoreaderSyncConfigStorage(
            prefs = context.getSharedPreferences("test_koreader_sync", Context.MODE_PRIVATE),
            cipher = FakeCipher()
        )
        storage.clear()
    }

    @Test
    fun `nothing is returned before anything is saved`() {
        assertNull(storage.load())
    }

    @Test
    fun `saved settings round trip`() {
        val config = KoreaderSyncConfig(
            serverUrl = "https://sync.example.com/api/koreader",
            username = "reader",
            password = "hunter2",
            enabled = true
        )

        storage.save(config)

        assertEquals(config, storage.load())
    }

    @Test
    fun `the password is not stored as plain text`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        storage.save(
            KoreaderSyncConfig(
                serverUrl = "https://sync.example.com",
                username = "reader",
                password = "hunter2",
                enabled = false
            )
        )

        val raw = context.getSharedPreferences("test_koreader_sync", Context.MODE_PRIVATE)
            .getString("password", null)

        assertTrue("password should be present", raw != null)
        assertFalse("password stored in the clear", raw == "hunter2")
        assertEquals("hunter2", FakeCipher().decrypt(raw!!))
    }

    @Test
    fun `an empty password is stored as absent`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        storage.save(
            KoreaderSyncConfig(
                serverUrl = "https://sync.example.com",
                username = "reader",
                password = "",
                enabled = false
            )
        )

        assertNull(
            context.getSharedPreferences("test_koreader_sync", Context.MODE_PRIVATE)
                .getString("password", null)
        )
        assertEquals("", storage.load()?.password)
    }

    @Test
    fun `an undecryptable password keeps the other settings and clears only the password`() {
        val decryptFailure = object : SecretCipher {
            override fun encrypt(value: String): String = value
            override fun decrypt(stored: String): String? = null
        }
        val context: Context = ApplicationProvider.getApplicationContext()
        val failing = KoreaderSyncConfigStorage(
            prefs = context.getSharedPreferences("test_koreader_sync", Context.MODE_PRIVATE),
            cipher = decryptFailure
        )
        failing.save(
            KoreaderSyncConfig(
                serverUrl = "https://sync.example.com",
                username = "reader",
                password = "hunter2",
                enabled = true
            )
        )

        val loaded = failing.load()
        assertEquals("https://sync.example.com", loaded?.serverUrl)
        assertEquals("reader", loaded?.username)
        assertEquals("", loaded?.password)
        assertEquals(true, loaded?.enabled)
    }
}
