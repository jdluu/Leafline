package com.jdluu.leafline.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrightnessTest {

    @Test
    fun `clamp keeps in range values unchanged`() {
        assertEquals(BRIGHTNESS_MIN, clampBrightness(BRIGHTNESS_MIN))
        assertEquals(0.5f, clampBrightness(0.5f))
        assertEquals(BRIGHTNESS_MAX, clampBrightness(BRIGHTNESS_MAX))
    }

    @Test
    fun `clamp pulls out of range values back into bounds`() {
        assertEquals(BRIGHTNESS_MIN, clampBrightness(-1f))
        assertEquals(BRIGHTNESS_MIN, clampBrightness(0f))
        assertEquals(BRIGHTNESS_MIN, clampBrightness(0.04f))
        assertEquals(BRIGHTNESS_MAX, clampBrightness(1.5f))
    }

    @Test
    fun `brightness defaults to following the system`() {
        assertNull(ReaderSettings().brightness)
    }
}
