package com.jdluu.leafline.reader

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Scroll mode disables page-turn tap actions: taps mapped to previous/next
 * page fall through unconsumed so the navigator webview keeps default
 * handling while vertical gestures own the navigation.
 */
class ScrollModeTapZonesTest {

    @Test
    fun `scroll off keeps every configured tap zone action`() {
        val config = TapZoneConfig(
            leftZone = TapZoneAction.NEXT_PAGE,
            centerZone = TapZoneAction.NONE,
            rightZone = TapZoneAction.PREVIOUS_PAGE
        )

        assertEquals(TapZoneAction.NEXT_PAGE, effectiveTapZoneAction(TapZone.LEFT, config, false))
        assertEquals(TapZoneAction.NONE, effectiveTapZoneAction(TapZone.CENTER, config, false))
        assertEquals(TapZoneAction.PREVIOUS_PAGE, effectiveTapZoneAction(TapZone.RIGHT, config, false))
    }

    @Test
    fun `scroll on disables page turn taps in both directions`() {
        assertEquals(
            TapZoneAction.NONE,
            effectiveTapZoneAction(TapZone.RIGHT, TapZoneConfig.DEFAULT, true)
        )
        assertEquals(
            TapZoneAction.NONE,
            effectiveTapZoneAction(TapZone.LEFT, TapZoneConfig.DEFAULT, true)
        )
    }

    @Test
    fun `scroll on keeps the menu toggle working`() {
        assertEquals(
            TapZoneAction.TOGGLE_MENU,
            effectiveTapZoneAction(TapZone.CENTER, TapZoneConfig.DEFAULT, true)
        )
    }

    @Test
    fun `scroll on keeps zones already set to none unconsumed`() {
        val config = TapZoneConfig(centerZone = TapZoneAction.NONE)

        assertEquals(
            TapZoneAction.NONE,
            effectiveTapZoneAction(TapZone.CENTER, config, true)
        )
    }

    @Test
    fun `scroll on disables page turns for a reversed layout`() {
        assertEquals(
            TapZoneAction.NONE,
            effectiveTapZoneAction(TapZone.LEFT, TapZoneConfig.REVERSED, true)
        )
        assertEquals(
            TapZoneAction.NONE,
            effectiveTapZoneAction(TapZone.RIGHT, TapZoneConfig.REVERSED, true)
        )
        assertEquals(
            TapZoneAction.TOGGLE_MENU,
            effectiveTapZoneAction(TapZone.CENTER, TapZoneConfig.REVERSED, true)
        )
    }

    @Test
    fun `default reader settings start in paginated mode`() {
        assertEquals(false, ReaderSettings().epub.scroll == true)
    }
}
