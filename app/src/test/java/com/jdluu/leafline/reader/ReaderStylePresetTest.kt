package com.jdluu.leafline.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily

class ReaderStylePresetTest {

    @Test
    fun `preset entries have valid configurations and labels`() {
        val presets = ReaderStylePreset.entries
        assertEquals(5, presets.size)

        for (preset in presets) {
            assertNotNull(preset.label)
            assertNotNull(preset.description)
            assert(preset.fontSize in 0.8..2.0)
            assert(preset.lineHeight in 1.0..2.5)
            assert(preset.pageMargins in PAGE_MARGINS_MIN..PAGE_MARGINS_MAX)
        }
    }

    @Test
    fun `applyTo configures epub preferences and disables publisher styles`() {
        val base = EpubPreferences(publisherStyles = true)
        val dyslexic = ReaderStylePreset.ACCESSIBLE_DYSLEXIC.applyTo(base)

        assertEquals(FontFamily.OPEN_DYSLEXIC, dyslexic.fontFamily)
        assertEquals(1.15, dyslexic.fontSize ?: 1.0, 0.01)
        assertEquals(1.4, dyslexic.lineHeight ?: 1.2, 0.01)
        assertEquals(0.75, dyslexic.pageMargins ?: 0.5, 0.01)
        assertEquals(false, dyslexic.publisherStyles)
    }

    @Test
    fun `matchingPreset detects matching preset from preferences`() {
        for (preset in ReaderStylePreset.entries) {
            val prefs = preset.applyTo(EpubPreferences())
            val matched = ReaderStylePreset.matchingPreset(prefs)
            assertEquals(preset, matched)
        }
    }

    @Test
    fun `matchingPreset returns null when publisher styles are active or custom values diverge`() {
        val balanced = ReaderStylePreset.BALANCED.applyTo(EpubPreferences())
        val withPublisher = balanced.copy(publisherStyles = true)
        assertNull(ReaderStylePreset.matchingPreset(withPublisher))

        val altered = balanced.copy(fontSize = 2.5)
        assertNull(ReaderStylePreset.matchingPreset(altered))
    }
}
