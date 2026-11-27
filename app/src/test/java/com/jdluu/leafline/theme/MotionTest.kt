package com.jdluu.leafline.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {

    @Test
    fun resolveReduceMotion_scaleZero_returnsTrue() {
        assertTrue(resolveReduceMotion(animatorDurationScale = 0f))
    }

    @Test
    fun resolveReduceMotion_negativeOrNonFiniteScale_returnsTrue() {
        assertTrue(resolveReduceMotion(animatorDurationScale = -1f))
        assertTrue(resolveReduceMotion(animatorDurationScale = Float.NaN))
    }

    @Test
    fun resolveReduceMotion_positiveScale_returnsFalseUnlessExplicit() {
        assertFalse(resolveReduceMotion(animatorDurationScale = 1f))
        assertFalse(resolveReduceMotion(animatorDurationScale = 0.5f))
        assertTrue(resolveReduceMotion(animatorDurationScale = 1f, explicitReduceMotion = true))
    }

    @Test
    fun motionDurationMs_snapsToZeroWhenReduced() {
        assertEquals(0, motionDurationMs(durationMillis = 250, reduceMotion = true))
        assertEquals(0, motionDurationMs(durationMillis = -10, reduceMotion = false))
        assertEquals(250, motionDurationMs(durationMillis = 250, reduceMotion = false))
    }
}
