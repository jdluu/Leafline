package com.jdluu.leafline.hardcover

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The mode and per-book answers must survive a restart, since they are the reader's decisions. */
@RunWith(RobolectricTestRunner::class)
class HardcoverTrackingStorageTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: android.content.SharedPreferences

    @Before
    fun setUp() {
        prefs = context.getSharedPreferences("leafline_hardcover_tracking", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `an untouched install reports the default mode`() {
        assertEquals(HardcoverTrackingMode.ALL_BOOKS, HardcoverTrackingStorage(prefs).mode())
    }

    @Test
    fun `the mode round-trips`() {
        val storage = HardcoverTrackingStorage(prefs)
        storage.setMode(HardcoverTrackingMode.ASK_EACH_BOOK)

        assertEquals(HardcoverTrackingMode.ASK_EACH_BOOK, HardcoverTrackingStorage(prefs).mode())
    }

    @Test
    fun `a book answer round-trips and is scoped to its own book`() {
        val storage = HardcoverTrackingStorage(prefs)
        storage.setPreference("urn:isbn:9780141439471", HardcoverBookPreference.IGNORE)

        assertEquals(
            HardcoverBookPreference.IGNORE,
            HardcoverTrackingStorage(prefs).preferenceFor("urn:isbn:9780141439471")
        )
        assertNull(storage.preferenceFor("urn:isbn:9780439420891"))
    }

    @Test
    fun `clearing an answer removes it`() {
        val storage = HardcoverTrackingStorage(prefs)
        storage.setPreference("b", HardcoverBookPreference.TRACK)
        storage.clearPreference("b")

        assertNull(storage.preferenceFor("b"))
    }

    @Test
    fun `a corrupt stored mode falls back to the default rather than crashing`() {
        prefs.edit().putString("mode", "NOT_A_MODE").commit()

        assertEquals(HardcoverTrackingMode.ALL_BOOKS, HardcoverTrackingStorage(prefs).mode())
    }
}
