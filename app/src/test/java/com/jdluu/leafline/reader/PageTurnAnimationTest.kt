package com.jdluu.leafline.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageTurnAnimationTest {

    @Test
    fun `page turn modes carry their animated flag`() {
        assertEquals(false, PageTurnAnimation.NONE.animated)
        assertEquals(true, PageTurnAnimation.SLIDE.animated)
    }

    @Test
    fun `slide animates while the system keeps animations running`() {
        assertTrue(pageTurnIsAnimated(PageTurnAnimation.SLIDE, 1f))
        assertTrue(pageTurnIsAnimated(PageTurnAnimation.SLIDE, 0.5f))
    }

    @Test
    fun `system animations removed snap page turns instantly regardless of preference`() {
        assertFalse(pageTurnIsAnimated(PageTurnAnimation.SLIDE, 0f))
    }

    @Test
    fun `none never animates even with system animations running`() {
        assertFalse(pageTurnIsAnimated(PageTurnAnimation.NONE, 1f))
        assertFalse(pageTurnIsAnimated(PageTurnAnimation.NONE, 0.5f))
    }

    @Test
    fun `default settings animate page turns on devices with animations`() {
        assertEquals(PageTurnAnimation.SLIDE, ReaderSettings().pageTurnAnimation)
        assertTrue(pageTurnIsAnimated(ReaderSettings().pageTurnAnimation, 1f))
    }

    @Test
    fun `reduce motion forces instant turns even with system animations`() {
        assertFalse(pageTurnIsAnimated(PageTurnAnimation.SLIDE, 1f, reduceMotion = true))
        assertFalse(pageTurnIsAnimated(PageTurnAnimation.SLIDE, 0.5f, reduceMotion = true))
    }

    @Test
    fun `reduce motion keeps none page turns instant`() {
        assertFalse(pageTurnIsAnimated(PageTurnAnimation.NONE, 1f, reduceMotion = true))
    }
}
