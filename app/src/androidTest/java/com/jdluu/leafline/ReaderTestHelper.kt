package com.jdluu.leafline

import android.content.Intent
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.io.File
import java.util.regex.Pattern

/**
 * Reusable instrumented test helpers for preparing the device, copying EPUB
 * test assets to app-private storage, launching [ReaderActivity] directly
 * without depending on [MainActivity], and asserting rendered content.
 */
object ReaderTestHelper {

    const val DEFAULT_TIMEOUT_MILLIS = 20000L

    /**
     * Wakes the screen and dismisses the keyguard so that accessibility
     * inspection and UI Automator find queries operate reliably.
     */
    fun prepareDevice(uiDevice: UiDevice) {
        if (!uiDevice.isScreenOn) {
            uiDevice.wakeUp()
        }
        uiDevice.executeShellCommand("wm dismiss-keyguard")
    }

    /**
     * Copies an EPUB asset bundled with the androidTest APK into app-private
     * storage (`targetContext.filesDir`) so [ReaderActivity] can open it.
     */
    fun copyAssetToPrivateStorage(
        assetName: String,
        targetFileName: String = assetName
    ): File {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val destination = File(targetContext.filesDir, targetFileName)
        testContext.assets.open(assetName).use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return destination
    }

    /**
     * Launches [ReaderActivity] directly for the specified EPUB file and waits
     * for the Readium navigator WebView to mount. If [expectedText] is provided,
     * also waits for and asserts that the distinctive text is rendered.
     */
    fun launchReader(
        uiDevice: UiDevice,
        epubFile: File,
        expectedText: String? = null,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
    ): UiObject2 {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = ReaderActivity.newIntent(targetContext, epubFile.absolutePath)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        targetContext.startActivity(intent)

        val webView = uiDevice.wait(
            Until.findObject(By.clazz(Pattern.compile(".*WebView.*"))),
            timeoutMillis
        )
        assertNotNull("Reader navigator WebView should mount", webView)

        if (expectedText != null) {
            val textFound = uiDevice.wait(Until.hasObject(By.textContains(expectedText)), timeoutMillis)
            assertTrue("Reader should display expected text: '$expectedText'", textFound)
        }

        return webView
    }

    /**
     * Polls for the first selector that matches, instead of a single-shot findObject.
     */
    fun waitForAny(
        uiDevice: UiDevice,
        timeoutMillis: Long,
        vararg selectors: BySelector
    ): UiObject2? {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            for (selector in selectors) {
                uiDevice.findObject(selector)?.let { return it }
            }
            SystemClock.sleep(200)
        }
        return null
    }

    /**
     * Deletes the given file from app-private storage if it exists.
     */
    fun cleanupFile(file: File?) {
        file?.let {
            if (it.exists()) {
                it.delete()
            }
        }
    }

    /**
     * Closes the reader activity by pressing back and waiting for idle.
     */
    fun closeReader(uiDevice: UiDevice) {
        uiDevice.pressBack()
        uiDevice.waitForIdle()
    }
}
