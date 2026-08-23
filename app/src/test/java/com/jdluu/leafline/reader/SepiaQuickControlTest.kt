package com.jdluu.leafline.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
class SepiaQuickControlTest {

    @Test
    fun `engaging sepia applies the sepia theme and remembers the previous one`() {
        val settings = ReaderSettings(epub = EpubPreferences(theme = Theme.LIGHT))

        val engaged = toggleSepia(settings)

        assertEquals(Theme.SEPIA, engaged.epub.theme)
        assertEquals(Theme.LIGHT, engaged.preSepiaTheme)
    }

    @Test
    fun `engaging sepia from an unset theme remembers unset`() {
        val engaged = toggleSepia(ReaderSettings())

        assertEquals(Theme.SEPIA, engaged.epub.theme)
        assertNull(engaged.preSepiaTheme)
    }

    @Test
    fun `disengaging sepia restores the remembered theme and clears the memory`() {
        val engaged = toggleSepia(ReaderSettings(epub = EpubPreferences(theme = Theme.DARK)))

        val disengaged = toggleSepia(engaged)

        assertEquals(Theme.DARK, disengaged.epub.theme)
        assertNull(disengaged.preSepiaTheme)
    }

    @Test
    fun `disengaging sepia without a memory restores the unset theme`() {
        val engaged = ReaderSettings(epub = EpubPreferences(theme = Theme.SEPIA))

        val disengaged = toggleSepia(engaged)

        assertNull(disengaged.epub.theme)
        assertNull(disengaged.preSepiaTheme)
    }

    @Test
    fun `a remembered sepia value is ignored on disengage`() {
        val engaged = ReaderSettings(
            epub = EpubPreferences(theme = Theme.SEPIA),
            preSepiaTheme = Theme.SEPIA
        )

        val disengaged = toggleSepia(engaged)

        assertNull(disengaged.epub.theme)
        assertNull(disengaged.preSepiaTheme)
    }

    @Test
    fun `re-engaging overwrites a stale memory with the current theme`() {
        val stale = ReaderSettings(
            epub = EpubPreferences(theme = Theme.LIGHT),
            preSepiaTheme = Theme.DARK
        )

        val engaged = toggleSepia(stale)

        assertEquals(Theme.SEPIA, engaged.epub.theme)
        assertEquals(Theme.LIGHT, engaged.preSepiaTheme)
    }

    @Test
    fun `toggling preserves unrelated preferences and interaction settings`() {
        val settings = ReaderSettings(
            epub = EpubPreferences(
                theme = Theme.LIGHT,
                fontFamily = FontFamily.SERIF,
                lineHeight = 1.6,
                scroll = true
            ),
            pageTurnAnimation = PageTurnAnimation.NONE,
            brightness = 0.42f
        )

        val toggled = toggleSepia(toggleSepia(settings))

        assertEquals(Theme.LIGHT, toggled.epub.theme)
        assertEquals(FontFamily.SERIF, toggled.epub.fontFamily)
        assertEquals(1.6, toggled.epub.lineHeight!!, 1e-9)
        assertEquals(true, toggled.epub.scroll)
        assertEquals(PageTurnAnimation.NONE, toggled.pageTurnAnimation)
        assertEquals(0.42f, toggled.brightness!!)
        assertNull(toggled.preSepiaTheme)
    }
}
