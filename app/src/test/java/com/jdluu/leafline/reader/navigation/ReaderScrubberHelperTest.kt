package com.jdluu.leafline.reader.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderScrubberHelperTest {

    @Test
    fun `findChapterTarget correctly maps percentage to index`() {
        assertNull(ReaderScrubberHelper.findChapterTarget(50.0, 0))
        assertNull(ReaderScrubberHelper.findChapterTarget(50.0, -1))

        // 1 chapter: always index 0
        assertEquals(0, ReaderScrubberHelper.findChapterTarget(0.0, 1))
        assertEquals(0, ReaderScrubberHelper.findChapterTarget(50.0, 1))
        assertEquals(0, ReaderScrubberHelper.findChapterTarget(100.0, 1))

        // 5 chapters (indices 0..4)
        assertEquals(0, ReaderScrubberHelper.findChapterTarget(0.0, 5))
        assertEquals(1, ReaderScrubberHelper.findChapterTarget(25.0, 5))
        assertEquals(2, ReaderScrubberHelper.findChapterTarget(50.0, 5))
        assertEquals(3, ReaderScrubberHelper.findChapterTarget(75.0, 5))
        assertEquals(4, ReaderScrubberHelper.findChapterTarget(100.0, 5))

        // Clamping check
        assertEquals(0, ReaderScrubberHelper.findChapterTarget(-10.0, 5))
        assertEquals(4, ReaderScrubberHelper.findChapterTarget(120.0, 5))
    }

    @Test
    fun `chapter skip checks work as expected`() {
        assertFalse(ReaderScrubberHelper.canSkipPrevious(0))
        assertFalse(ReaderScrubberHelper.canSkipPrevious(-1))
        assertTrue(ReaderScrubberHelper.canSkipPrevious(1))
        assertTrue(ReaderScrubberHelper.canSkipPrevious(4))

        assertTrue(ReaderScrubberHelper.canSkipNext(0, 5))
        assertTrue(ReaderScrubberHelper.canSkipNext(3, 5))
        assertFalse(ReaderScrubberHelper.canSkipNext(4, 5))
        assertFalse(ReaderScrubberHelper.canSkipNext(5, 5))
        assertFalse(ReaderScrubberHelper.canSkipNext(-1, 5))

        assertEquals(Integer.valueOf(1), ReaderScrubberHelper.resolvePreviousChapterIndex(2))
        assertNull(ReaderScrubberHelper.resolvePreviousChapterIndex(0))

        assertEquals(Integer.valueOf(3), ReaderScrubberHelper.resolveNextChapterIndex(2, 5))
        assertNull(ReaderScrubberHelper.resolveNextChapterIndex(4, 5))
    }

    @Test
    fun `resolveScrubberPreview formats clean previews`() {
        val titles = listOf("Prologue", "Chapter 1", "Chapter 2", "Epilogue")

        assertEquals("Prologue \u2022 0%", ReaderScrubberHelper.resolveScrubberPreview(0.0, titles))
        assertEquals("Chapter 1 \u2022 33%", ReaderScrubberHelper.resolveScrubberPreview(33.0, titles))
        assertEquals("Chapter 2 \u2022 67%", ReaderScrubberHelper.resolveScrubberPreview(67.0, titles))
        assertEquals("Epilogue \u2022 100%", ReaderScrubberHelper.resolveScrubberPreview(100.0, titles))

        // Without titles
        assertEquals("45%", ReaderScrubberHelper.resolveScrubberPreview(45.0, emptyList()))
    }
}
