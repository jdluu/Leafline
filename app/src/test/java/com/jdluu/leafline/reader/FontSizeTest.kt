package com.jdluu.leafline.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class FontSizeTest {

    @Test
    fun `clamp keeps in range values unchanged`() {
        assertEquals(FONT_SIZE_MIN, clampFontSize(FONT_SIZE_MIN), 0.0)
        assertEquals(FONT_SIZE_DEFAULT, clampFontSize(FONT_SIZE_DEFAULT), 0.0)
        assertEquals(FONT_SIZE_MAX, clampFontSize(FONT_SIZE_MAX), 0.0)
    }

    @Test
    fun `clamp pulls out of range values back into bounds`() {
        assertEquals(FONT_SIZE_MIN, clampFontSize(0.1), 0.0)
        assertEquals(FONT_SIZE_MAX, clampFontSize(4.0), 0.0)
    }

    @Test
    fun `snap aligns to the stepper increment`() {
        assertEquals(1.1, snapFontSize(1.13), 0.0001)
        assertEquals(1.2, snapFontSize(1.16), 0.0001)
        assertEquals(0.9, snapFontSize(0.86), 0.0001)
    }

    @Test
    fun `stepping up then down returns to the original scale`() {
        val up = snapFontSize(FONT_SIZE_DEFAULT + FONT_SIZE_STEP)
        assertEquals(1.1, up, 0.0001)
        val down = snapFontSize(up - FONT_SIZE_STEP)
        assertEquals(FONT_SIZE_DEFAULT, down, 0.0001)
    }

    @Test
    fun `snap never escapes the supported range`() {
        assertEquals(FONT_SIZE_MIN, snapFontSize(0.01), 0.0)
        assertEquals(FONT_SIZE_MAX, snapFontSize(9.0), 0.0)
    }
}
