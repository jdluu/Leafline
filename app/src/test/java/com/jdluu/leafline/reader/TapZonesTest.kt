package com.jdluu.leafline.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TapZonesTest {

    @Test
    fun `screen thirds map to left center and right zones`() {
        assertEquals(TapZone.LEFT, tapZoneAt(0f))
        assertEquals(TapZone.LEFT, tapZoneAt(0.2f))
        assertEquals(TapZone.CENTER, tapZoneAt(0.5f))
        assertEquals(TapZone.RIGHT, tapZoneAt(0.8f))
        assertEquals(TapZone.RIGHT, tapZoneAt(1f))
    }

    @Test
    fun `default config maps left back right forward and center to menu`() {
        assertEquals(TapZoneAction.PREVIOUS_PAGE, tapZoneAction(TapZone.LEFT, TapZoneConfig.DEFAULT))
        assertEquals(TapZoneAction.TOGGLE_MENU, tapZoneAction(TapZone.CENTER, TapZoneConfig.DEFAULT))
        assertEquals(TapZoneAction.NEXT_PAGE, tapZoneAction(TapZone.RIGHT, TapZoneConfig.DEFAULT))
    }

    @Test
    fun `reversed preset swaps the side zones and keeps the menu zone`() {
        assertEquals(TapZoneAction.NEXT_PAGE, tapZoneAction(TapZone.LEFT, TapZoneConfig.REVERSED))
        assertEquals(TapZoneAction.PREVIOUS_PAGE, tapZoneAction(TapZone.RIGHT, TapZoneConfig.REVERSED))
        assertEquals(TapZoneAction.TOGGLE_MENU, tapZoneAction(TapZone.CENTER, TapZoneConfig.REVERSED))
    }

    @Test
    fun `each zone resolves its own configured action`() {
        val config = TapZoneConfig(
            leftZone = TapZoneAction.NONE,
            centerZone = TapZoneAction.NEXT_PAGE,
            rightZone = TapZoneAction.PREVIOUS_PAGE
        )

        assertEquals(TapZoneAction.NONE, tapZoneAction(TapZone.LEFT, config))
        assertEquals(TapZoneAction.NEXT_PAGE, tapZoneAction(TapZone.CENTER, config))
        assertEquals(TapZoneAction.PREVIOUS_PAGE, tapZoneAction(TapZone.RIGHT, config))
    }

    @Test
    fun `every required action is offered including none`() {
        val actions = TapZoneAction.values().toSet()

        assertTrue(actions.containsAll(setOf(
            TapZoneAction.NEXT_PAGE,
            TapZoneAction.PREVIOUS_PAGE,
            TapZoneAction.TOGGLE_MENU,
            TapZoneAction.NONE
        )))
    }

    @Test
    fun `default reader settings use the conventional tap zone config`() {
        assertEquals(TapZoneConfig.DEFAULT, ReaderSettings().tapZoneConfig)
    }
}
