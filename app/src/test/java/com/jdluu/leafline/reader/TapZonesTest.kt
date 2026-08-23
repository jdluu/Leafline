package com.jdluu.leafline.reader

import org.junit.Assert.assertEquals
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
    fun `default mode maps left back right forward and center to menu`() {
        assertEquals(TapZoneAction.PREVIOUS_PAGE, tapZoneAction(TapZone.LEFT, TapZoneMode.DEFAULT))
        assertEquals(TapZoneAction.NEXT_PAGE, tapZoneAction(TapZone.RIGHT, TapZoneMode.DEFAULT))
        assertEquals(TapZoneAction.TOGGLE_MENU, tapZoneAction(TapZone.CENTER, TapZoneMode.DEFAULT))
    }

    @Test
    fun `reversed mode swaps the side zones and keeps the menu zone`() {
        assertEquals(TapZoneAction.NEXT_PAGE, tapZoneAction(TapZone.LEFT, TapZoneMode.REVERSED))
        assertEquals(TapZoneAction.PREVIOUS_PAGE, tapZoneAction(TapZone.RIGHT, TapZoneMode.REVERSED))
        assertEquals(TapZoneAction.TOGGLE_MENU, tapZoneAction(TapZone.CENTER, TapZoneMode.REVERSED))
    }

    @Test
    fun `page turn animation modes carry their animated flag`() {
        assertEquals(false, PageTurnAnimation.NONE.animated)
        assertEquals(true, PageTurnAnimation.SLIDE.animated)
    }
}
