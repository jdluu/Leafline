package com.jdluu.leafline.reader.navigation

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HardwareKeyNavigatorTest {

    @Test
    fun `page down key navigates forward`() {
        assertEquals(
            PageNavigationDirection.FORWARD,
            HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_PAGE_DOWN)
        )
    }

    @Test
    fun `page up key navigates backward`() {
        assertEquals(
            PageNavigationDirection.BACKWARD,
            HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_PAGE_UP)
        )
    }

    @Test
    fun `dpad right and down navigate forward`() {
        assertEquals(
            PageNavigationDirection.FORWARD,
            HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_DPAD_RIGHT)
        )
        assertEquals(
            PageNavigationDirection.FORWARD,
            HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_DPAD_DOWN)
        )
    }

    @Test
    fun `dpad left and up navigate backward`() {
        assertEquals(
            PageNavigationDirection.BACKWARD,
            HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_DPAD_LEFT)
        )
        assertEquals(
            PageNavigationDirection.BACKWARD,
            HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_DPAD_UP)
        )
    }

    @Test
    fun `controller shoulder buttons navigate correctly`() {
        assertEquals(
            PageNavigationDirection.FORWARD,
            HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_BUTTON_R1)
        )
        assertEquals(
            PageNavigationDirection.BACKWARD,
            HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_BUTTON_L1)
        )
    }

    @Test
    fun `volume keys navigate forward and backward when enabled`() {
        assertEquals(
            PageNavigationDirection.FORWARD,
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                volumeKeyNavigation = true,
                invertVolumeKeys = false
            )
        )
        assertEquals(
            PageNavigationDirection.BACKWARD,
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                volumeKeyNavigation = true,
                invertVolumeKeys = false
            )
        )
    }

    @Test
    fun `inverted volume keys swap directions`() {
        assertEquals(
            PageNavigationDirection.BACKWARD,
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                volumeKeyNavigation = true,
                invertVolumeKeys = true
            )
        )
        assertEquals(
            PageNavigationDirection.FORWARD,
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                volumeKeyNavigation = true,
                invertVolumeKeys = true
            )
        )
    }

    @Test
    fun `volume keys ignored when volumeKeyNavigation is false`() {
        assertNull(
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                volumeKeyNavigation = false
            )
        )
        assertNull(
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                volumeKeyNavigation = false
            )
        )
    }

    @Test
    fun `volume keys ignored when TTS is speaking to preserve audio volume control`() {
        assertNull(
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
                volumeKeyNavigation = true,
                isTtsSpeaking = true
            )
        )
        assertNull(
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                volumeKeyNavigation = true,
                isTtsSpeaking = true
            )
        )
    }

    @Test
    fun `dedicated hardware page keys still work even when TTS is speaking`() {
        assertEquals(
            PageNavigationDirection.FORWARD,
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_PAGE_DOWN,
                isTtsSpeaking = true
            )
        )
        assertEquals(
            PageNavigationDirection.BACKWARD,
            HardwareKeyNavigator.resolveDirection(
                keyCode = KeyEvent.KEYCODE_PAGE_UP,
                isTtsSpeaking = true
            )
        )
    }

    @Test
    fun `unrelated keys return null`() {
        assertNull(HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_A))
        assertNull(HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_SPACE))
        assertNull(HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_ENTER))
        assertNull(HardwareKeyNavigator.resolveDirection(KeyEvent.KEYCODE_BACK))
    }
}
