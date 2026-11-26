package com.jdluu.leafline.reader.navigation

import com.jdluu.leafline.reader.ui.matchesChapter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TocChapterMatchingTest {

    @Test
    fun matchesExactHrefs() {
        assertTrue(matchesChapter("chapter1.xhtml", "chapter1.xhtml"))
    }

    @Test
    fun matchesWithLeadingSlashDifferences() {
        assertTrue(matchesChapter("/EPUB/ch01.xhtml", "EPUB/ch01.xhtml"))
        assertTrue(matchesChapter("EPUB/ch01.xhtml", "/EPUB/ch01.xhtml"))
    }

    @Test
    fun matchesWhenLinkContainsHashFragment() {
        assertTrue(matchesChapter("ch02.xhtml#heading-1", "ch02.xhtml"))
        assertTrue(matchesChapter("ch02.xhtml", "ch02.xhtml#part-2"))
    }

    @Test
    fun matchesRelativeSubpaths() {
        assertTrue(matchesChapter("text/ch03.xhtml", "ch03.xhtml"))
        assertTrue(matchesChapter("ch03.xhtml", "text/ch03.xhtml"))
    }

    @Test
    fun returnsFalseWhenDifferentOrNull() {
        assertFalse(matchesChapter("chapter1.xhtml", null))
        assertFalse(matchesChapter("chapter1.xhtml", "chapter2.xhtml"))
        assertFalse(matchesChapter("", "chapter1.xhtml"))
        assertFalse(matchesChapter("chapter1.xhtml", ""))
    }
}
