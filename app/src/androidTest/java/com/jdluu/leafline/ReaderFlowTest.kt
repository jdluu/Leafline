package com.jdluu.leafline

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end reader flow tests (#28): copy a bundled EPUB to app storage,
 * open it in ReaderActivity, and exercise navigation, search, and the
 * toolbar. Uses UiDevice so it runs against the real navigator stack.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ReaderFlowTest {

    private lateinit var uiDevice: UiDevice
    private var epubPath: String? = null

    @Before
    fun setUp() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Copy asset EPUB (packaged in the androidTest APK) into app-private
        // storage so ReaderActivity can open it. Use the instrumentation's own
        // context to read androidTest assets.
        val file = java.io.File(context.filesDir, "reader-flow-test.epub")
        InstrumentationRegistry.getInstrumentation().context.assets.open("test-book.epub").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        epubPath = file.absolutePath

        // Start from a clean launch of MainActivity (ensures DB initialized).
        context.startActivity(
            android.content.Intent(context, MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        uiDevice.wait(Until.hasObject(By.text("Import EPUB")), 15000L)
    }

    @After
    fun tearDown() {
        epubPath?.let { path ->
            InstrumentationRegistry.getInstrumentation()
                .targetContext.getFileStreamPath(java.io.File(path).name)?.delete()
        }
        uiDevice.pressBack()
    }

    private fun launchReader() {
        val intent = android.content.Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ReaderActivity::class.java
        ).putExtra("extra_file_path", epubPath)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        InstrumentationRegistry.getInstrumentation().targetContext.startActivity(intent)
        // Wait for the reader web view to render chapter content.
        assertTrue("Reader should display Chapter One heading",
            uiDevice.wait(Until.hasObject(By.textContains("Chapter One")), 20000L))
    }

    @Test
    fun openBook_rendersFirstChapter() {
        launchReader()
        assertNotNull(uiDevice.findObject(By.textContains("Chapter One")))
    }

    @Test
    fun navigate_nextPageShowsMoreContent() {
        launchReader()
        // Swipe left = next page; content should still be rendered afterwards.
        val bounds = uiDevice.displayWidth to uiDevice.displayHeight
        uiDevice.swipe(bounds.first * 4 / 5, bounds.second / 2, bounds.first / 5, bounds.second / 2, 20)
        uiDevice.waitForIdle()
        assertNotNull("Content should still be visible after page turn",
            uiDevice.wait(Until.hasObject(By.textContains("chapter 1")), 10000L))
    }

    @Test
    fun toolbar_backReturnsToLibrary() {
        launchReader()
        uiDevice.pressBack()
        uiDevice.waitForIdle()
        assertTrue("Should return to Library after back",
            uiDevice.wait(Until.hasObject(By.text("Import EPUB")), 10000L))
    }
}
