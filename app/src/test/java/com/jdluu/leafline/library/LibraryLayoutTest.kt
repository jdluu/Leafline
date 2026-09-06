package com.jdluu.leafline.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryLayoutTest {

    @Test
    fun compactWidthUsesTwoColumns() {
        assertEquals(2, libraryGridColumnsForWidth(599))
    }

    @Test
    fun mediumWidthUsesThreeColumns() {
        assertEquals(3, libraryGridColumnsForWidth(600))
        assertEquals(3, libraryGridColumnsForWidth(839))
    }

    @Test
    fun expandedWidthUsesFiveColumns() {
        assertEquals(5, libraryGridColumnsForWidth(840))
    }
}