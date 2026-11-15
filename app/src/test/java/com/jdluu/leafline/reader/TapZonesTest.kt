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
    fun `narrow edges layout assigns slim 20 percent edges and wide center`() {
        assertEquals(TapZone.LEFT, tapZoneAt(0.15f, TapZoneLayout.NARROW_EDGES))
        assertEquals(TapZone.CENTER, tapZoneAt(0.25f, TapZoneLayout.NARROW_EDGES))
        assertEquals(TapZone.CENTER, tapZoneAt(0.50f, TapZoneLayout.NARROW_EDGES))
        assertEquals(TapZone.CENTER, tapZoneAt(0.75f, TapZoneLayout.NARROW_EDGES))
        assertEquals(TapZone.RIGHT, tapZoneAt(0.85f, TapZoneLayout.NARROW_EDGES))
    }

    @Test
    fun `wide edges layout assigns 40 percent edges and compact 20 percent center`() {
        assertEquals(TapZone.LEFT, tapZoneAt(0.35f, TapZoneLayout.WIDE_EDGES))
        assertEquals(TapZone.CENTER, tapZoneAt(0.50f, TapZoneLayout.WIDE_EDGES))
        assertEquals(TapZone.RIGHT, tapZoneAt(0.65f, TapZoneLayout.WIDE_EDGES))
    }

    @Test
    fun `right-handed layout provides dominant right advance zone`() {
        assertEquals(TapZone.LEFT, tapZoneAt(0.10f, TapZoneLayout.RIGHT_HANDED))
        assertEquals(TapZone.CENTER, tapZoneAt(0.20f, TapZoneLayout.RIGHT_HANDED))
        assertEquals(TapZone.RIGHT, tapZoneAt(0.35f, TapZoneLayout.RIGHT_HANDED))
        assertEquals(TapZone.RIGHT, tapZoneAt(0.85f, TapZoneLayout.RIGHT_HANDED))
    }

    @Test
    fun `left-handed layout provides dominant left advance zone`() {
        assertEquals(TapZone.LEFT, tapZoneAt(0.35f, TapZoneLayout.LEFT_HANDED))
        assertEquals(TapZone.LEFT, tapZoneAt(0.65f, TapZoneLayout.LEFT_HANDED))
        assertEquals(TapZone.CENTER, tapZoneAt(0.75f, TapZoneLayout.LEFT_HANDED))
        assertEquals(TapZone.RIGHT, tapZoneAt(0.90f, TapZoneLayout.LEFT_HANDED))
    }

    @Test
    fun `default config maps left back right forward and center to menu`() {
        assertEquals(TapZoneAction.PREVIOUS_PAGE, tapZoneAction(TapZone.LEFT, TapZoneConfig.DEFAULT))
        assertEquals(TapZoneAction.TOGGLE_MENU, tapZoneAction(TapZone.CENTER, TapZoneConfig.DEFAULT))
        assertEquals(TapZoneAction.NEXT_PAGE, tapZoneAction(TapZone.RIGHT, TapZoneConfig.DEFAULT))
        assertEquals(TapZoneLayout.SYMMETRIC, TapZoneConfig.DEFAULT.layout)
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
        assertTrue(ReaderSettings().volumeKeyNavigation)
        assertTrue(!ReaderSettings().invertVolumeKeys)
    }
}
