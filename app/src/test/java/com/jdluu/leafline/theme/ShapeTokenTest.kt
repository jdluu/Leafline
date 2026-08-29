package com.jdluu.leafline.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Shape token invariants from the design contract: one 12dp container radius
 * everywhere, with the pill reserved for compact single-action controls.
 */
class ShapeTokenTest {

    @Test
    fun `small shape is the single 12dp radius`() {
        assertEquals(RoundedCornerShape(12.dp), LeaflineShapes.small)
    }

    @Test
    fun `medium shape is the single 12dp radius`() {
        assertEquals(RoundedCornerShape(12.dp), LeaflineShapes.medium)
    }

    @Test
    fun `large shape is the single 12dp radius`() {
        assertEquals(RoundedCornerShape(12.dp), LeaflineShapes.large)
    }

    @Test
    fun `pill shape stays a full pill`() {
        assertEquals(RoundedCornerShape(percent = 50), LeaflinePillShape)
    }
}