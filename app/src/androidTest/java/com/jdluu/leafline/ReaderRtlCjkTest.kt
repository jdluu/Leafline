package com.jdluu.leafline

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented tests verifying that ReaderActivity can open and render
 * RTL (Arabic, Hebrew) and CJK (horizontal, vertical Japanese) EPUB 3 fixtures (#154).
 *
 * These tests launch ReaderActivity directly without depending on MainActivity
 * or its initialization flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ReaderRtlCjkTest {

    private lateinit var uiDevice: UiDevice
    private var currentFile: File? = null

    @Before
    fun setUp() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        ReaderTestHelper.prepareDevice(uiDevice)
    }

    @After
    fun tearDown() {
        ReaderTestHelper.closeReader(uiDevice)
        ReaderTestHelper.cleanupFile(currentFile)
        currentFile = null
    }

    @Test
    fun openArabicRtl_rendersArabicText() {
        val file = ReaderTestHelper.copyAssetToPrivateStorage("rtl-arabic.epub", "test-arabic-rtl.epub")
        currentFile = file

        ReaderTestHelper.launchReader(uiDevice, file, expectedText = "الفصل الأول")
        assertTrue(
            "Distinctive Arabic body text should render",
            uiDevice.wait(Until.hasObject(By.textContains("مرحبا بالعالم في اختبار القراءة باللغة العربية")), 10000L)
        )
    }

    @Test
    fun openHebrewRtl_rendersHebrewText() {
        val file = ReaderTestHelper.copyAssetToPrivateStorage("rtl-hebrew.epub", "test-hebrew-rtl.epub")
        currentFile = file

        ReaderTestHelper.launchReader(uiDevice, file, expectedText = "פרק ראשון")
        assertTrue(
            "Distinctive Hebrew body text should render",
            uiDevice.wait(Until.hasObject(By.textContains("שלום עולם בבדיקת קריאה בעברית")), 10000L)
        )
    }

    @Test
    fun openJapaneseCjk_rendersJapaneseText() {
        val file = ReaderTestHelper.copyAssetToPrivateStorage("cjk-japanese.epub", "test-japanese-cjk.epub")
        currentFile = file

        ReaderTestHelper.launchReader(uiDevice, file, expectedText = "第一章")
        assertTrue(
            "Distinctive Japanese body text should render",
            uiDevice.wait(Until.hasObject(By.textContains("吾輩は猫である")), 10000L)
        )
    }

    @Test
    fun openVerticalJapanese_rendersVerticalJapaneseText() {
        val file = ReaderTestHelper.copyAssetToPrivateStorage("vertical-japanese.epub", "test-vertical-japanese.epub")
        currentFile = file

        ReaderTestHelper.launchReader(uiDevice, file, expectedText = "縦書き第一章")
        assertTrue(
            "Distinctive vertical Japanese body text should render",
            uiDevice.wait(Until.hasObject(By.textContains("親譲りの無鉄砲で小供の時から損ばかりしている")), 10000L)
        )
    }
}
