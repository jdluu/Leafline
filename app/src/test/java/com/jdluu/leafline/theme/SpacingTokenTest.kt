package com.jdluu.leafline.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spacing token invariants from the design contract: every gap sits on the
 * 8dp rhythm so sections line up across screens and components.
 */
class SpacingTokenTest {

    @Test
    fun `spacing steps follow the 8dp rhythm`() {
        assertEquals(8.dp, Spacing.xs)
        assertEquals(16.dp, Spacing.sm)
        assertEquals(24.dp, Spacing.md)
        assertEquals(32.dp, Spacing.lg)
        assertEquals(48.dp, Spacing.xl)
        assertEquals(64.dp, Spacing.xxl)
    }

    @Test
    fun `every spacing value is a whole multiple of 8dp`() {
        val values = listOf(
            Spacing.xs,
            Spacing.sm,
            Spacing.md,
            Spacing.lg,
            Spacing.xl,
            Spacing.xxl
        )
        values.forEach { value ->
            assertEquals(
                "Expected $value to be a multiple of 8dp",
                0f,
                value.value % 8f
            )
        }
    }
}