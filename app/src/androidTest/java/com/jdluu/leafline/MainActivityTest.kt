package com.jdluu.leafline

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    private lateinit var uiDevice: UiDevice
    private val timeout = 15000L

    @get:Rule
    val activityScenarioRule = androidx.test.rule.ActivityTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // The instrumentation's accessibility view of the app is empty while the
        // screen is off or a keyguard is up, which makes every findObject return
        // null (#96). Bring the device to a ready state before querying.
        if (!uiDevice.isScreenOn) {
            uiDevice.wakeUp()
        }
        uiDevice.executeShellCommand("wm dismiss-keyguard")
    }

    /** Polls for the first selector that matches, instead of a single-shot findObject. */
    private fun waitForAny(timeoutMillis: Long, vararg selectors: BySelector): UiObject2? {
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
     * The library always shows an Import EPUB affordance: a labeled button in the
     * empty state and a FAB with a matching content description once books exist.
     */
    private fun importControlSelector(): Array<BySelector> = arrayOf(
        By.text("Import EPUB"),
        By.desc("Import EPUB")
    )

    @Test
    fun libraryTab_showsImportButton() {
        val importControl = waitForAny(timeout, *importControlSelector())
        assertNotNull("Import EPUB control should be visible on Library tab", importControl)
    }

    @Test
    fun bottomNav_settingsTab_showsSettingsTitle() {
        val settingsTab = checkNotNull(waitForAny(timeout, By.text("Settings"), By.desc("Settings"))) {
            "Settings tab should be visible in bottom navigation"
        }
        settingsTab.click()
        uiDevice.waitForIdle()

        assertTrue("Settings title should be visible after tapping Settings tab",
            uiDevice.wait(Until.hasObject(By.text("Settings")), 5000L))
        // Real Settings-screen content proves the tab switch rendered.
        assertTrue("KOReader Sync section should be visible on Settings tab",
            uiDevice.wait(Until.hasObject(By.text("KOReader Sync")), 5000L))
    }

    @Test
    fun clickImportEpubButton_opensDocumentsUi_andCancel_returnsToLibrary() {
        val importButton = checkNotNull(waitForAny(timeout, *importControlSelector())) {
            "Import EPUB control should be visible before clicking"
        }
        importButton.click()

        uiDevice.waitForIdle()

        val documentsUiShown = waitForAny(
            10000L,
            By.res("android:id/list"),
            By.res("android:id/content_picker_title"),
            By.pkg("com.android.documentsui")
        ) != null
        assertTrue("Documents UI should open after tapping Import EPUB", documentsUiShown)

        uiDevice.pressBack()
        uiDevice.waitForIdle()

        val backOnLibrary = waitForAny(
            5000L,
            By.text("Leafline"),
            *importControlSelector()
        ) != null
        assertTrue("Should return to Library tab after cancel", backOnLibrary)
    }
}
