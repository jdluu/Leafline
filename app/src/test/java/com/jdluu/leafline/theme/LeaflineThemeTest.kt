package com.jdluu.leafline.theme

import androidx.compose.ui.graphics.Color
import com.jdluu.leafline.reader.WcagContrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrast invariants for the Leafline semantic color tokens.
 *
 * Every assertion reads the real [LeaflineColors] values through the shared
 * [WcagContrast] helper, so a token drift either fails here or is covered by
 * the threshold it was given. Body-text pairs must meet 4.5:1, and role pairs
 * such as outline must meet the 3:1 UI threshold.
 */
class LeaflineThemeTest {

    // --- Sanity: the ARGB extraction must round-trip a known token ---

    @Test
    fun `token hex extraction round-trips a known color`() {
        assertEquals("#315C52", Color(0xFF315C52).toHex())
    }

    // --- Light scheme ---

    @Test
    fun `light primary meets body threshold on surface`() {
        assertBody(LeaflineColors.LightPrimary, LeaflineColors.LightSurface)
    }

    @Test
    fun `light onSurface meets body threshold on surface`() {
        assertBody(LeaflineColors.LightOnSurface, LeaflineColors.LightSurface)
    }

    @Test
    fun `light onSurfaceVariant meets body threshold on surfaceVariant`() {
        assertBody(LeaflineColors.LightOnSurfaceVariant, LeaflineColors.LightSurfaceVariant)
    }

    @Test
    fun `light onPrimaryContainer meets body threshold on primaryContainer`() {
        assertBody(LeaflineColors.LightOnPrimaryContainer, LeaflineColors.LightPrimaryContainer)
    }

    @Test
    fun `light error meets body threshold on surface`() {
        assertBody(LeaflineColors.LightError, LeaflineColors.LightSurface)
    }

    @Test
    fun `light outline meets UI threshold on surface`() {
        assertUi(LeaflineColors.LightOutline, LeaflineColors.LightSurface)
    }

    // --- Dark scheme ---

    @Test
    fun `dark primary meets body threshold on surface`() {
        assertBody(LeaflineColors.DarkPrimary, LeaflineColors.DarkSurface)
    }

    @Test
    fun `dark onSurface meets body threshold on surface`() {
        assertBody(LeaflineColors.DarkOnSurface, LeaflineColors.DarkSurface)
    }

    @Test
    fun `dark onSurfaceVariant meets body threshold on surfaceVariant`() {
        assertBody(LeaflineColors.DarkOnSurfaceVariant, LeaflineColors.DarkSurfaceVariant)
    }

    @Test
    fun `dark onPrimaryContainer meets body threshold on primaryContainer`() {
        assertBody(LeaflineColors.DarkOnPrimaryContainer, LeaflineColors.DarkPrimaryContainer)
    }

    @Test
    fun `dark error meets body threshold on surface`() {
        assertBody(LeaflineColors.DarkError, LeaflineColors.DarkSurface)
    }

    // --- OLED scheme ---

    @Test
    fun `oled onSurface on true black meets body threshold`() {
        assertBody(LeaflineColors.OledOnSurface, LeaflineColors.OledSurface)
    }

    @Test
    fun `oled primary on true black meets body threshold`() {
        assertBody(LeaflineColors.DarkPrimary, LeaflineColors.OledSurface)
    }

    // --- E-ink scheme ---

    @Test
    fun `eink onSurface on white is maximum contrast`() {
        val ratio = WcagContrast.contrastRatio(
            LeaflineColors.EinkOnSurface.toHex(),
            LeaflineColors.EinkSurface.toHex()
        )
        assertTrue("Eink onSurface on white: expected >= 10.0, got $ratio", ratio >= 10.0)
    }

    @Test
    fun `eink primary on white meets body threshold`() {
        assertBody(LeaflineColors.EinkPrimary, LeaflineColors.EinkSurface)
    }

    @Test
    fun `eink onSurfaceVariant meets body threshold on surfaceVariant`() {
        assertBody(LeaflineColors.EinkOnSurfaceVariant, LeaflineColors.EinkSurfaceVariant)
    }

    @Test
    fun `eink outline on white meets body threshold`() {
        assertBody(LeaflineColors.EinkOutline, LeaflineColors.EinkSurface)
    }

    // --- Helpers ---

    /** Converts a Compose color to the #RRGGBB hex used across the project. */
    private fun Color.toHex(): String {
        val argb = (value shr 32).toLong()
        val red = (argb shr 16) and 0xFF
        val green = (argb shr 8) and 0xFF
        val blue = argb and 0xFF
        return "#%02X%02X%02X".format(red, green, blue)
    }

    private fun assertBody(fg: Color, bg: Color) {
        val ratio = WcagContrast.contrastRatio(fg.toHex(), bg.toHex())
        assertTrue(
            "Expected >= ${WcagContrast.MIN_BODY}, got $ratio for ${fg.toHex()} on ${bg.toHex()}",
            ratio >= WcagContrast.MIN_BODY
        )
    }

    private fun assertUi(fg: Color, bg: Color) {
        val ratio = WcagContrast.contrastRatio(fg.toHex(), bg.toHex())
        assertTrue(
            "Expected >= ${WcagContrast.MIN_LARGE_OR_UI}, got $ratio for ${fg.toHex()} on ${bg.toHex()}",
            ratio >= WcagContrast.MIN_LARGE_OR_UI
        )
    }
}