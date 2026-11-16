package com.jdluu.leafline.reader.navigation

import android.view.KeyEvent

/**
 * Direction of page navigation triggered by hardware keys or buttons.
 */
enum class PageNavigationDirection {
    FORWARD,
    BACKWARD
}

/**
 * Resolves hardware key events into page navigation actions.
 *
 * Supports dedicated e-reader hardware page turn keys (Onyx Boox, Meebook, PocketBook),
 * DPAD controllers, Bluetooth remotes/clickers, and volume rocker buttons.
 */
object HardwareKeyNavigator {

    /**
     * Resolves whether [keyCode] corresponds to a hardware page navigation action.
     * Returns the direction ([PageNavigationDirection.FORWARD] or [PageNavigationDirection.BACKWARD]),
     * or null if the key is not mapped to page navigation.
     *
     * @param keyCode The Android [KeyEvent] key code.
     * @param volumeKeyNavigation Whether volume rocker buttons should be used for page turning.
     * @param invertVolumeKeys When true, volume up advances forward and volume down goes backward.
     * @param isTtsSpeaking When true, volume keys are reserved for audio level adjustments.
     */
    fun resolveDirection(
        keyCode: Int,
        volumeKeyNavigation: Boolean = true,
        invertVolumeKeys: Boolean = false,
        isTtsSpeaking: Boolean = false
    ): PageNavigationDirection? {
        // Dedicated page turn keys and remotes (always active in paginated mode)
        when (keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_BUTTON_R1 -> return PageNavigationDirection.FORWARD

            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_BUTTON_L1 -> return PageNavigationDirection.BACKWARD
        }

        // Volume buttons (only when enabled and TTS is not actively playing audio)
        if (volumeKeyNavigation && !isTtsSpeaking) {
            return when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (!invertVolumeKeys) PageNavigationDirection.FORWARD else PageNavigationDirection.BACKWARD
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (!invertVolumeKeys) PageNavigationDirection.BACKWARD else PageNavigationDirection.FORWARD
                }
                else -> null
            }
        }

        return null
    }
}
