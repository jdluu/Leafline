package com.jdluu.leafline.reader

import com.jdluu.leafline.reader.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Plain JVM coverage of the sepia quick control domain logic: it only touches
 * [ReaderTheme], never the Readium theme type whose static initializer needs
 * an Android runtime.
 */
@OptIn(ExperimentalReadiumApi::class)
class SepiaQuickControlTest {

    @Test
    fun `engaging sepia applies the sepia theme and remembers the previous one`() {
        val settings = ReaderSettings(theme = ReaderTheme.LIGHT)

        val engaged = toggleSepia(settings)

        assertEquals(ReaderTheme.SEPIA, engaged.theme)
        assertEquals(ReaderTheme.LIGHT, engaged.preSepiaTheme)
    }

    @Test
    fun `engaging sepia from an unset theme remembers unset`() {
        val engaged = toggleSepia(ReaderSettings())

        assertEquals(ReaderTheme.SEPIA, engaged.theme)
        assertNull(engaged.preSepiaTheme)
    }

    @Test
    fun `disengaging sepia restores the remembered theme and clears the memory`() {
        val engaged = toggleSepia(ReaderSettings(theme = ReaderTheme.DARK))

        val disengaged = toggleSepia(engaged)

        assertEquals(ReaderTheme.DARK, disengaged.theme)
        assertNull(disengaged.preSepiaTheme)
    }

    @Test
    fun `disengaging sepia without a memory restores the unset theme`() {
        val engaged = ReaderSettings(theme = ReaderTheme.SEPIA)

        val disengaged = toggleSepia(engaged)

        assertNull(disengaged.theme)
        assertNull(disengaged.preSepiaTheme)
    }

    @Test
    fun `a remembered sepia value is ignored on disengage`() {
        val engaged = ReaderSettings(
            theme = ReaderTheme.SEPIA,
            preSepiaTheme = ReaderTheme.SEPIA
        )

        val disengaged = toggleSepia(engaged)

        assertNull(disengaged.theme)
        assertNull(disengaged.preSepiaTheme)
    }

    @Test
    fun `re-engaging overwrites a stale memory with the current theme`() {
        val stale = ReaderSettings(
            theme = ReaderTheme.LIGHT,
            preSepiaTheme = ReaderTheme.DARK
        )

        val engaged = toggleSepia(stale)

        assertEquals(ReaderTheme.SEPIA, engaged.theme)
        assertEquals(ReaderTheme.LIGHT, engaged.preSepiaTheme)
    }

    @Test
    fun `toggling preserves unrelated preferences and interaction settings`() {
        val settings = ReaderSettings(
            epub = EpubPreferences(
                fontFamily = FontFamily.SERIF,
                lineHeight = 1.6,
                scroll = true
            ),
            theme = ReaderTheme.LIGHT,
            pageTurnAnimation = PageTurnAnimation.NONE,
            brightness = 0.42f
        )

        val toggled = toggleSepia(toggleSepia(settings))

        assertEquals(ReaderTheme.LIGHT, toggled.theme)
        assertEquals(FontFamily.SERIF, toggled.epub.fontFamily)
        assertEquals(1.6, toggled.epub.lineHeight!!, 1e-9)
        assertEquals(true, toggled.epub.scroll)
        assertEquals(PageTurnAnimation.NONE, toggled.pageTurnAnimation)
        assertEquals(0.42f, toggled.brightness!!)
        assertNull(toggled.preSepiaTheme)
    }
}
