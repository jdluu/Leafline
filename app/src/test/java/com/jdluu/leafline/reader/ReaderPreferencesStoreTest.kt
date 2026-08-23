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

        assertNull(loaded.epub.theme)
        assertNull(loaded.epub.fontFamily)
        assertNull(loaded.epub.lineHeight)
        assertNull(loaded.epub.pageMargins)
        assertNull(loaded.epub.publisherStyles)
        assertNull(loaded.epub.scroll)
        assertEquals(TapZoneConfig.DEFAULT, loaded.tapZoneConfig)
        assertEquals(PageTurnAnimation.SLIDE, loaded.pageTurnAnimation)
        assertNull(loaded.brightness)
    }

    @Test
    fun `save then load round trips every managed field`() {
        val store = ReaderPreferencesStore.fromContext(context)

        store.save(
            ReaderSettings(
                epub = EpubPreferences(
                    theme = Theme.SEPIA,
                    fontFamily = FontFamily.SERIF,
                    lineHeight = 1.6,
                    pageMargins = 0.75,
                    publisherStyles = false,
                    scroll = true
                ),
                tapZoneConfig = TapZoneConfig(
                    leftZone = TapZoneAction.NEXT_PAGE,
                    centerZone = TapZoneAction.NONE,
                    rightZone = TapZoneAction.TOGGLE_MENU
                ),
                pageTurnAnimation = PageTurnAnimation.NONE,
                brightness = 0.42f
            )
        )

        val loaded = store.load()

        assertEquals(Theme.SEPIA, loaded.epub.theme)
        assertEquals(FontFamily.SERIF, loaded.epub.fontFamily)
        assertEquals(1.6, loaded.epub.lineHeight!!, 1e-9)
        assertEquals(0.75, loaded.epub.pageMargins!!, 1e-9)
        assertEquals(false, loaded.epub.publisherStyles)
        assertEquals(true, loaded.epub.scroll)
        assertEquals(TapZoneAction.NEXT_PAGE, loaded.tapZoneConfig.leftZone)
        assertEquals(TapZoneAction.NONE, loaded.tapZoneConfig.centerZone)
        assertEquals(TapZoneAction.TOGGLE_MENU, loaded.tapZoneConfig.rightZone)
        assertEquals(PageTurnAnimation.NONE, loaded.pageTurnAnimation)
        assertEquals(0.42f, loaded.brightness!!)
    }

    @Test
    fun `saving null clears previously stored fields`() {
        val store = ReaderPreferencesStore.fromContext(context)

        store.save(
            ReaderSettings(
                epub = EpubPreferences(
                    theme = Theme.DARK,
                    fontFamily = FontFamily.SANS_SERIF,
                    lineHeight = 1.4,
                    pageMargins = 1.0,
                    publisherStyles = false,
                    scroll = true
                ),
                tapZoneConfig = TapZoneConfig.REVERSED,
                pageTurnAnimation = PageTurnAnimation.NONE,
                brightness = 0.3f
            )
        )
        store.save(ReaderSettings())

        val loaded = store.load()

        assertNull(loaded.epub.theme)
        assertNull(loaded.epub.fontFamily)
        assertNull(loaded.epub.lineHeight)
        assertNull(loaded.epub.pageMargins)
        assertNull(loaded.epub.publisherStyles)
        assertNull(loaded.epub.scroll)
        assertEquals(TapZoneConfig.DEFAULT, loaded.tapZoneConfig)
        assertEquals(PageTurnAnimation.SLIDE, loaded.pageTurnAnimation)
        assertNull(loaded.brightness)
    }

    @Test
    fun `second save overwrites the first choice`() {
        val store = ReaderPreferencesStore.fromContext(context)

        store.save(
            ReaderSettings(
                epub = EpubPreferences(theme = Theme.LIGHT, lineHeight = 1.0),
                tapZoneConfig = TapZoneConfig.REVERSED,
                pageTurnAnimation = PageTurnAnimation.NONE
            )
        )
        store.save(
            ReaderSettings(
                epub = EpubPreferences(theme = Theme.DARK, lineHeight = 2.0),
                tapZoneConfig = TapZoneConfig(
                    leftZone = TapZoneAction.TOGGLE_MENU,
                    centerZone = TapZoneAction.PREVIOUS_PAGE,
                    rightZone = TapZoneAction.NONE
                ),
                pageTurnAnimation = PageTurnAnimation.SLIDE
            )
        )

        val loaded = store.load()

        assertEquals(Theme.DARK, loaded.epub.theme)
        assertEquals(2.0, loaded.epub.lineHeight!!, 1e-9)
        assertEquals(TapZoneAction.TOGGLE_MENU, loaded.tapZoneConfig.leftZone)
        assertEquals(TapZoneAction.PREVIOUS_PAGE, loaded.tapZoneConfig.centerZone)
        assertEquals(TapZoneAction.NONE, loaded.tapZoneConfig.rightZone)
        assertEquals(PageTurnAnimation.SLIDE, loaded.pageTurnAnimation)
    }

    @Test
    fun `unknown stored per-zone actions fall back to that zone default`() {
        val preferences =
            context.getSharedPreferences(ReaderPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putString("reader_tap_zone_left", "SPIN")
            .putString("reader_tap_zone_center", "WIGGLE")
            .putString("reader_tap_zone_right", "FLIP")
            .commit()
        val store = ReaderPreferencesStore.fromContext(context)

        val loaded = store.load()

        assertEquals(TapZoneAction.PREVIOUS_PAGE, loaded.tapZoneConfig.leftZone)
        assertEquals(TapZoneAction.TOGGLE_MENU, loaded.tapZoneConfig.centerZone)
        assertEquals(TapZoneAction.NEXT_PAGE, loaded.tapZoneConfig.rightZone)
    }

    @Test
    fun `legacy reversed preset migrates to swapped per-zone config`() {
        val preferences =
            context.getSharedPreferences(ReaderPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putString("reader_tap_zones", "REVERSED")
            .commit()
        val store = ReaderPreferencesStore.fromContext(context)

        assertEquals(TapZoneConfig.REVERSED, store.load().tapZoneConfig)
    }

    @Test
    fun `legacy unknown preset migrates to conventional per-zone config`() {
        val preferences =
            context.getSharedPreferences(ReaderPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putString("reader_tap_zones", "LEFT_HANDED")
            .putString("reader_page_turn_animation", "FADE")
            .commit()
        val store = ReaderPreferencesStore.fromContext(context)

        val loaded = store.load()

        assertEquals(TapZoneConfig.DEFAULT, loaded.tapZoneConfig)
        assertEquals(PageTurnAnimation.SLIDE, loaded.pageTurnAnimation)
    }

    @Test
    fun `per-zone keys take precedence over the stored legacy preset`() {
        val preferences =
            context.getSharedPreferences(ReaderPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putString("reader_tap_zones", "REVERSED")
            .putString("reader_tap_zone_left", "TOGGLE_MENU")
            .commit()
        val store = ReaderPreferencesStore.fromContext(context)

        val loaded = store.load()

        assertEquals(TapZoneAction.TOGGLE_MENU, loaded.tapZoneConfig.leftZone)
        assertEquals(TapZoneAction.TOGGLE_MENU, loaded.tapZoneConfig.centerZone)
        assertEquals(TapZoneAction.NEXT_PAGE, loaded.tapZoneConfig.rightZone)
    }

    @Test
    fun `saving removes the superseded legacy preset key`() {
        val preferences =
            context.getSharedPreferences(ReaderPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putString("reader_tap_zones", "REVERSED")
            .commit()
        val store = ReaderPreferencesStore.fromContext(context)

        store.save(ReaderSettings())

        assertEquals(false, preferences.contains("reader_tap_zones"))
    }

    @Test
    fun `out of range stored brightness values are clamped on load`() {
        val preferences =
            context.getSharedPreferences(ReaderPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putFloat("reader_brightness", 7f)
            .commit()
        val store = ReaderPreferencesStore.fromContext(context)
        assertEquals(BRIGHTNESS_MAX, store.load().brightness!!)

        preferences.edit()
            .putFloat("reader_brightness", -2f)
            .commit()
        assertEquals(BRIGHTNESS_MIN, store.load().brightness!!)
    }

    @Test
    fun `unknown stored font family falls back to original`() {
        val preferences =
            context.getSharedPreferences(ReaderPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putString("reader_font_family", "Comic Sans")
            .commit()
        val store = ReaderPreferencesStore.fromContext(context)

        assertNull(store.load().epub.fontFamily)
    }

    @Test
    fun `stale stored page margins snap back into range on load`() {
        val preferences =
            context.getSharedPreferences(ReaderPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putLong(
                "reader_page_margins",
                java.lang.Double.doubleToRawLongBits(9.0)
            )
            .commit()
        val store = ReaderPreferencesStore.fromContext(context)

        assertEquals(1.5, store.load().epub.pageMargins!!, 1e-9)

        preferences.edit()
            .putLong(
                "reader_page_margins",
                java.lang.Double.doubleToRawLongBits(0.6)
            )
            .commit()

        assertEquals(0.5, store.load().epub.pageMargins!!, 1e-9)
    }

    @Test
    fun `saved page margins snap to stepper increments`() {
        val store = ReaderPreferencesStore.fromContext(context)

        store.save(
            ReaderSettings(epub = EpubPreferences(pageMargins = 0.6))
        )

        assertEquals(0.5, store.load().epub.pageMargins!!, 1e-9)
    }

    @Test
    fun `stored publisher mode drops stale custom typography on load`() {
        val store = ReaderPreferencesStore.fromContext(context)

        store.save(
            ReaderSettings(
                epub = EpubPreferences(
                    theme = Theme.SEPIA,
                    fontFamily = FontFamily.SERIF,
                    lineHeight = 1.8,
                    pageMargins = 1.25,
                    scroll = true,
                    publisherStyles = true
                )
            )
        )

        val loaded = store.load().epub

        assertEquals(true, loaded.publisherStyles)
        assertNull(loaded.fontFamily)
        assertNull(loaded.lineHeight)
        assertNull(loaded.pageMargins)
        assertEquals(Theme.SEPIA, loaded.theme)
        assertEquals(true, loaded.scroll)
    }
}
