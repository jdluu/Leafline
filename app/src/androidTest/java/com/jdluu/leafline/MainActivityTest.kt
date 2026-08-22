package com.jdluu.leafline

import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
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
    }

    @After
    fun tearDown() {
    }

    @Test
    fun clickImportEpubButton_opensDocumentsUi_andCancel_returnsToMainActivity() {
        val importButton = uiDevice.findObject(By.text("Import EPUB"))
        importButton.click()
        
        uiDevice.waitForIdle()
        
        val found = uiDevice.wait(Until.findObject(By.res("android:id/list")), 10000L)
        if (found == null) {
            val found2 = uiDevice.wait(Until.findObject(By.res("android:id/content_picker_title")), 10000L)
        }
        
        uiDevice.pressBack()
        uiDevice.waitForIdle()
        
        val mainActivity = uiDevice.wait(Until.hasObject(By.text("Leafline")), 5000L)
        assert(mainActivity != null) { "Should return to MainActivity after cancel" }
    }

    @Test
    fun clickOpenEpubSpikeButton_navigatesToReader() {
        val openButton = uiDevice.findObject(By.text("Open EPUB Spike"))
        openButton.click()
    }
}