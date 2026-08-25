package com.jdluu.leafline.reader

import com.jdluu.leafline.reader.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
class ReaderSettingsTest {

    @Test
    fun `font catalog starts with original and offers every readium selectable stack`() {
        val families = READER_FONT_FAMILIES.map { it.first }

        assertNull(families.first())
        assertTrue(families.contains(FontFamily.SERIF))
        assertTrue(families.contains(FontFamily.SANS_SERIF))
        assertTrue(families.contains(FontFamily.MONOSPACE))
        assertTrue(families.contains(FontFamily.CURSIVE))
        assertTrue(families.contains(FontFamily.FANTASY))
        assertTrue(families.contains(FontFamily.OPEN_DYSLEXIC))
        assertTrue(families.contains(FontFamily.ACCESSIBLE_DFA))
        assertTrue(families.contains(FontFamily.IA_WRITER_DUOSPACE))
    }

    @Test
    fun `font catalog labels are unique and non blank`() {
        val labels = READER_FONT_FAMILIES.map { it.second }

        assertEquals(labels.size, labels.distinct().size)
        assertTrue(labels.all { it.isNotBlank() })
    }

    @Test
    fun `stored font family names resolve back to catalog entries`() {
        READER_FONT_FAMILIES.filterNotNullKeys().forEach { (family, _) ->
            assertEquals(family, fontFamilyFromStoredName(family.name))
        }
    }

    @Test
    fun `unknown stored font family names resolve to null`() {
        assertNull(fontFamilyFromStoredName("Comic Sans"))
    }

    @Test
    fun `unset publisher styles behave as custom`() {
        assertEquals(StyleMode.CUSTOM, styleModeFor(null))
        assertEquals(StyleMode.CUSTOM, styleModeFor(false))
        assertEquals(StyleMode.PUBLISHER, styleModeFor(true))
    }

    @Test
    fun `publisher mode clears custom typography and keeps appearance`() {
        val preferences = EpubPreferences(
            scroll = true,
            fontFamily = FontFamily.SERIF,
            lineHeight = 1.8,
            pageMargins = 1.0,
            publisherStyles = false
        )

        val switched = preferences.withStyleMode(StyleMode.PUBLISHER)

        assertEquals(true, switched.publisherStyles)
        assertNull(switched.fontFamily)
        assertNull(switched.lineHeight)
        assertNull(switched.pageMargins)
        assertEquals(true, switched.scroll)
    }

    @Test
    fun `reader theme names match the persisted preference strings`() {
        assertEquals("LIGHT", ReaderTheme.LIGHT.name)
        assertEquals("DARK", ReaderTheme.DARK.name)
        assertEquals("SEPIA", ReaderTheme.SEPIA.name)
    }

    @Test
    fun `custom mode keeps user typography and disables publisher styles`() {
        val preferences = EpubPreferences(
            fontFamily = FontFamily.MONOSPACE,
            lineHeight = 1.6,
            pageMargins = 0.75,
            publisherStyles = true
        )

        val switched = preferences.withStyleMode(StyleMode.CUSTOM)

        assertEquals(false, switched.publisherStyles)
        assertEquals(FontFamily.MONOSPACE, switched.fontFamily)
        assertEquals(1.6, switched.lineHeight!!, 1e-9)
        assertEquals(0.75, switched.pageMargins!!, 1e-9)
    }

    @Test
    fun `page margins are clamped into the supported range`() {
        assertEquals(PAGE_MARGINS_MIN, clampPageMargins(0.1), 1e-9)
        assertEquals(PAGE_MARGINS_MAX, clampPageMargins(2.0), 1e-9)
        assertEquals(0.75, clampPageMargins(0.75), 1e-9)
    }

    @Test
    fun `page margins snap to stepper increments inside the range`() {
        assertEquals(0.5, snapPageMargins(0.6), 1e-9)
        assertEquals(PAGE_MARGINS_MAX, snapPageMargins(1.4), 1e-9)
        assertEquals(PAGE_MARGINS_MAX, snapPageMargins(9.0), 1e-9)
        assertEquals(PAGE_MARGINS_MIN, snapPageMargins(0.0), 1e-9)
        assertEquals(0.75, snapPageMargins(0.75), 1e-9)
    }
}

private fun List<Pair<FontFamily?, String>>.filterNotNullKeys():
    List<Pair<FontFamily, String>> {
    return mapNotNull { (family, label) ->
        family?.let { it to label }
    }
}
