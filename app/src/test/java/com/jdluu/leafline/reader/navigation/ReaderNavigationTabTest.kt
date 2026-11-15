package com.jdluu.leafline.reader.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderNavigationTabTest {

    @Test
    fun `navigation tabs have correct order and readable titles`() {
        val tabs = ReaderNavigationTab.entries
        assertEquals(3, tabs.size)
        assertEquals(ReaderNavigationTab.CONTENTS, tabs[0])
        assertEquals("Contents", tabs[0].title)

        assertEquals(ReaderNavigationTab.BOOKMARKS, tabs[1])
        assertEquals("Bookmarks", tabs[1].title)

        assertEquals(ReaderNavigationTab.HIGHLIGHTS, tabs[2])
        assertEquals("Highlights", tabs[2].title)
    }

    @Test
    fun `navigation tab lookup by name matches expected enum constants`() {
        assertEquals(ReaderNavigationTab.CONTENTS, ReaderNavigationTab.valueOf("CONTENTS"))
        assertEquals(ReaderNavigationTab.BOOKMARKS, ReaderNavigationTab.valueOf("BOOKMARKS"))
        assertEquals(ReaderNavigationTab.HIGHLIGHTS, ReaderNavigationTab.valueOf("HIGHLIGHTS"))
    }
}
