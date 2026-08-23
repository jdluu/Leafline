package com.jdluu.leafline.reader

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
@RunWith(RobolectricTestRunner::class)
class ReaderPreferencesStoreTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ReaderPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `load returns empty preferences when nothing saved`() {
        val store = ReaderPreferencesStore.fromContext(context)

        val loaded = store.load()

        assertNull(loaded.theme)
        assertNull(loaded.fontFamily)
        assertNull(loaded.lineHeight)
        assertNull(loaded.pageMargins)
        assertNull(loaded.publisherStyles)
        assertNull(loaded.scroll)
    }

    @Test
    fun `save then load round trips every managed field`() {
        val store = ReaderPreferencesStore.fromContext(context)

        store.save(
            EpubPreferences(
                theme = Theme.SEPIA,
                fontFamily = FontFamily.SERIF,
                lineHeight = 1.6,
                pageMargins = 0.75,
                publisherStyles = false,
                scroll = true
            )
        )

        val loaded = store.load()

        assertEquals(Theme.SEPIA, loaded.theme)
        assertEquals(FontFamily.SERIF, loaded.fontFamily)
        assertEquals(1.6, loaded.lineHeight!!, 1e-9)
        assertEquals(0.75, loaded.pageMargins!!, 1e-9)
        assertEquals(false, loaded.publisherStyles)
        assertEquals(true, loaded.scroll)
    }

    @Test
    fun `saving null clears previously stored fields`() {
        val store = ReaderPreferencesStore.fromContext(context)

        store.save(
            EpubPreferences(
                theme = Theme.DARK,
                fontFamily = FontFamily.SANS_SERIF,
                lineHeight = 1.4,
                pageMargins = 1.0,
                publisherStyles = false,
                scroll = true
            )
        )
        store.save(EpubPreferences())

        val loaded = store.load()

        assertNull(loaded.theme)
        assertNull(loaded.fontFamily)
        assertNull(loaded.lineHeight)
        assertNull(loaded.pageMargins)
        assertNull(loaded.publisherStyles)
        assertNull(loaded.scroll)
    }

    @Test
    fun `second save overwrites the first choice`() {
        val store = ReaderPreferencesStore.fromContext(context)

        store.save(EpubPreferences(theme = Theme.LIGHT, lineHeight = 1.0))
        store.save(EpubPreferences(theme = Theme.DARK, lineHeight = 2.0))

        val loaded = store.load()

        assertEquals(Theme.DARK, loaded.theme)
        assertEquals(2.0, loaded.lineHeight!!, 1e-9)
    }
}
