package com.jdluu.leafline

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
    fun libraryTab_showsImportButton() {
        val importButton = uiDevice.findObject(By.text("Import EPUB"))
        assert(importButton != null) { "Import EPUB button should be visible on Library tab" }
    }

    @Test
    fun bottomNav_settingsTab_showsSettingsTitle() {
        val settingsTab = uiDevice.findObject(By.text("Settings"))
        settingsTab.click()
        uiDevice.waitForIdle()

        val title = uiDevice.wait(Until.findObject(By.text("Settings")), 5000L)
        assert(title != null) { "Settings title should be visible after tapping Settings tab" }
    }

    @Test
    fun bottomNav_catalogTab_showsCatalogTitle() {
        val catalogTab = uiDevice.findObject(By.text("Catalog"))
        catalogTab.click()
        uiDevice.waitForIdle()

        val title = uiDevice.wait(Until.findObject(By.text("OPDS Catalog")), 5000L)
        assert(title != null) { "OPDS Catalog title should be visible after tapping Catalog tab" }
    }

    @Test
    fun clickImportEpubButton_opensDocumentsUi_andCancel_returnsToLibrary() {
        val importButton = uiDevice.findObject(By.text("Import EPUB"))
        importButton.click()

        uiDevice.waitForIdle()

        uiDevice.wait(Until.findObject(By.res("android:id/list")), 10000L)
        uiDevice.wait(Until.findObject(By.res("android:id/content_picker_title")), 10000L)

        uiDevice.pressBack()
        uiDevice.waitForIdle()

        val libraryVisible = uiDevice.wait(Until.hasObject(By.text("Library")), 5000L)
        assert(libraryVisible) { "Should return to Library tab after cancel" }
    }
}
