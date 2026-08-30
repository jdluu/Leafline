package com.jdluu.leafline.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Round-trip and fallback behavior of [AppearanceStore], plus the [themeScheme]
 * mapping from [ThemeMode] to the fixed Leafline color schemes.
 *
 * [AppearanceStore] is exercised against the real Robolectric SharedPreferences
 * (cleared per test), matching the other preference-store tests in the repo.
 * [themeScheme] is a pure function of its arguments, so it is asserted directly.
 */
@RunWith(RobolectricTestRunner::class)
class AppearanceStoreTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(AppearanceStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun store(): AppearanceStore = AppearanceStore.fromContext(context)

    private fun writeStored(name: String) {
        context.getSharedPreferences(AppearanceStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("appearance", name)
            .commit()
    }

    // --- AppearanceStore persistence ---

    @Test
    fun `load defaults to SYSTEM when nothing saved`() {
        assertEquals(ThemeMode.SYSTEM, store().load())
    }

    @Test
    fun `all ThemeMode values round trip through save then load`() {
        val store = store()
        for (mode in ThemeMode.entries) {
            store.save(mode)
            assertEquals(mode, store.load())
        }
    }

    @Test
    fun `unknown stored name falls back to SYSTEM`() {
        writeStored("GARBAGE")
        assertEquals(ThemeMode.SYSTEM, store().load())
    }

    @Test
    fun `second save overwrites the first choice`() {
        val store = store()
        store.save(ThemeMode.DARK)
        store.save(ThemeMode.OLED)
        assertEquals(ThemeMode.OLED, store.load())
    }

    // --- themeScheme mapping ---

    @Test
    fun `SYSTEM with dark system resolves to the fixed dark scheme`() {
        assertEquals(LeaflineColors.DarkSurface, themeScheme(ThemeMode.SYSTEM, true).surface)
    }

    @Test
    fun `SYSTEM with light system resolves to the fixed light scheme`() {
        assertEquals(LeaflineColors.LightSurface, themeScheme(ThemeMode.SYSTEM, false).surface)
    }

    @Test
    fun `SYSTEM never uses dynamic colors`() {
        val light = themeScheme(ThemeMode.SYSTEM, false)
        val dark = themeScheme(ThemeMode.SYSTEM, true)

        assertEquals(LeaflineColors.LightPrimary, light.primary)
        assertEquals(LeaflineColors.DarkPrimary, dark.primary)
    }

    @Test
    fun `explicit LIGHT matches the fixed light scheme regardless of system`() {
        assertEquals(LeaflineColors.LightSurface, themeScheme(ThemeMode.LIGHT, true).surface)
        assertEquals(LeaflineColors.LightSurface, themeScheme(ThemeMode.LIGHT, false).surface)
    }

    @Test
    fun `explicit DARK matches the fixed dark scheme regardless of system`() {
        assertEquals(LeaflineColors.DarkSurface, themeScheme(ThemeMode.DARK, false).surface)
    }

    @Test
    fun `OLED surface is true black and distinct from the dark surface`() {
        assertEquals(
            colorValue(LeaflineColors.OledSurface),
            colorValue(themeScheme(ThemeMode.OLED, false).surface)
        )
        assertNotEquals(
            colorValue(LeaflineColors.DarkSurface),
            colorValue(themeScheme(ThemeMode.OLED, false).surface)
        )
    }

    @Test
    fun `E_INK surface is white and distinct from the light surface`() {
        assertEquals(
            colorValue(LeaflineColors.EinkSurface),
            colorValue(themeScheme(ThemeMode.E_INK, false).surface)
        )
        assertNotEquals(
            colorValue(LeaflineColors.LightSurface),
            colorValue(themeScheme(ThemeMode.E_INK, false).surface)
        )
    }

    @Test
    fun `every mode maps to one of the four fixed schemes`() {
        val fixed = setOf(
            colorValue(LeaflineColors.LightSurface),
            colorValue(LeaflineColors.DarkSurface),
            colorValue(LeaflineColors.OledSurface),
            colorValue(LeaflineColors.EinkSurface)
        )
        for (mode in ThemeMode.entries) {
            val dark = themeScheme(mode, true).surface
            val light = themeScheme(mode, false).surface
            assertEquals("mode $mode must produce a fixed scheme", true, colorValue(dark) in fixed)
            assertEquals("mode $mode must produce a fixed scheme", true, colorValue(light) in fixed)
        }
    }

    /** Reduces a Compose color to a comparable value for assertions. */
    private fun colorValue(color: Color): ULong = color.value
}
