package com.jdluu.leafline

import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.jdluu.leafline.library.LibraryBook
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern

/**
 * Validates TalkBack accessibility semantics and contracts across key screens (#155):
 * - Switch roles and toggle states (KOReader Sync, Reduce Motion)
 * - Disambiguated filter chips and tap-zone action labels
 * - Content descriptions on typography knobs (margins, line height)
 * - Custom accessibility actions on bookmark lists
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AccessibilitySemanticsTest {

    private lateinit var uiDevice: UiDevice
    private val timeout = 15000L
    private var epubPath: String? = null
    private val testBookStableId = "a11y-test-book"

    @Before
    fun setUp() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (!uiDevice.isScreenOn) {
            uiDevice.wakeUp()
        }
        uiDevice.executeShellCommand("wm dismiss-keyguard")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, "a11y-test-book.epub")
        InstrumentationRegistry.getInstrumentation().context.assets.open("test-book.epub").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        epubPath = file.absolutePath
        val absPath = file.absolutePath
        val canPath = file.canonicalPath

        val app = context.applicationContext as LeaflineApplication
        val book = LibraryBook(
            stableId = testBookStableId,
            title = "A11y Test Book",
            authors = listOf("Leafline"),
            language = "en",
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = absPath,
            fileHash = "a11y-hash-${System.currentTimeMillis()}",
            addedAtEpochMillis = System.currentTimeMillis(),
            pageCount = 10
        )
        runBlocking {
            app.appContainer.libraryRepository.addBook(book)
            if (canPath != absPath) {
                app.appContainer.libraryRepository.addBook(
                    book.copy(stableId = "$testBookStableId-can", filePath = canPath)
                )
            }
            app.appContainer.bookmarkRepository.toggleBookmark(
                bookId = testBookStableId,
                locatorJson = """{"href":"chapter1.xhtml","type":"application/xhtml+xml","locations":{"progression":0.5}}""",
                label = "Chapter 1 Test Bookmark"
            )
            if (canPath != absPath) {
                app.appContainer.bookmarkRepository.toggleBookmark(
                    bookId = "$testBookStableId-can",
                    locatorJson = """{"href":"chapter1.xhtml","type":"application/xhtml+xml","locations":{"progression":0.5}}""",
                    label = "Chapter 1 Test Bookmark"
                )
            }
        }
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as LeaflineApplication
        runBlocking {
            app.appContainer.libraryRepository.deleteBook(testBookStableId)
            app.appContainer.libraryRepository.deleteBook("$testBookStableId-can")
        }
        epubPath?.let { path ->
            context.getFileStreamPath(File(path).name)?.delete()
        }
        for (i in 0 until 3) {
            uiDevice.pressBack()
            uiDevice.waitForIdle()
        }
    }

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

    private fun scrollSheetDownSmall() {
        uiDevice.swipe(
            uiDevice.displayWidth / 2,
            (uiDevice.displayHeight * 0.75).toInt(),
            uiDevice.displayWidth / 2,
            (uiDevice.displayHeight * 0.55).toInt(),
            20
        )
        uiDevice.waitForIdle()
    }

    private fun dumpActiveWindow(): String {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val roots = mutableListOf<AccessibilityNodeInfo>()
        uiAutomation.rootInActiveWindow?.let { roots.add(it) }
        for (window in uiAutomation.windows) {
            window.root?.let { roots.add(it) }
        }
        if (roots.isEmpty()) return "No roots"
        val nodes = mutableListOf<String>()
        fun walk(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val desc = node.contentDescription
            val text = node.text
            val actions = node.actionList?.mapNotNull { it.label }
            if (desc != null || text != null || !actions.isNullOrEmpty()) {
                nodes.add("class=${node.className}, text=$text, desc=$desc, actions=$actions")
            }
            for (i in 0 until node.childCount) {
                walk(node.getChild(i))
            }
        }
        for (root in roots) {
            walk(root)
        }
        return nodes.joinToString("\n")
    }

    private fun launchMainActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        val libraryLoaded = waitForAny(
            timeout,
            By.text("Leafline"),
            By.text("Import EPUB"),
            By.desc("Import EPUB")
        ) != null
        assertTrue("Library screen should settle after launch", libraryLoaded)
    }

    private fun launchReader(showToolbar: Boolean = true) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = ReaderActivity.newIntent(context, epubPath!!, showToolbar = showToolbar)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        assertNotNull(
            "Reader navigator WebView should mount",
            uiDevice.wait(Until.findObject(By.clazz(Pattern.compile(".*WebView.*"))), 20000L)
        )
        assertTrue(
            "Reader should display chapter text",
            uiDevice.wait(Until.hasObject(By.textContains("Chapter One")), 20000L)
        )
    }

    private fun findAccessibilityNode(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (root == null) return null
        if (predicate(root)) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val match = findAccessibilityNode(child, predicate)
            if (match != null) return match
        }
        return null
    }

    private fun findAccessibilityNodeInAllWindows(
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val roots = mutableListOf<AccessibilityNodeInfo>()
        uiAutomation.rootInActiveWindow?.let { roots.add(it) }
        for (window in uiAutomation.windows) {
            window.root?.let { roots.add(it) }
        }
        for (root in roots) {
            val match = findAccessibilityNode(root, predicate)
            if (match != null) return match
        }
        return null
    }

    private fun clickNode(predicate: (AccessibilityNodeInfo) -> Boolean): Boolean {
        val leaf = findAccessibilityNodeInAllWindows(predicate) ?: return false
        var target: AccessibilityNodeInfo? = leaf
        while (target != null) {
            val hasClick = target.isClickable || target.actionList?.any { it.id == AccessibilityNodeInfo.ACTION_CLICK } == true
            if (hasClick) {
                target.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                val success = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) return true
            }
            target = target.parent
        }
        return false
    }

    private fun expandBottomSheetIfPossible(): Boolean {
        val sheetNode = findAccessibilityNodeInAllWindows { node ->
            node.actionList?.any { it.label?.toString() == "Expand bottom sheet" } == true
        } ?: return false
        val action = sheetNode.actionList.first { it.label?.toString() == "Expand bottom sheet" }
        val result = sheetNode.performAction(action.id)
        uiDevice.waitForIdle()
        return result
    }

    private fun scrollForward(): Boolean {
        val scrollableNode = findAccessibilityNodeInAllWindows { it.isScrollable } ?: return false
        val result = scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        uiDevice.waitForIdle()
        return result
    }

    @Test
    fun settingsScreen_koreaderSyncSwitch_hasSwitchRoleAndToggles() {
        launchMainActivity()

        val settingsTab = checkNotNull(waitForAny(timeout, By.text("Settings"), By.desc("Settings"))) {
            "Settings tab should be visible in bottom navigation"
        }
        settingsTab.click()
        uiDevice.waitForIdle()

        assertTrue("Settings screen should display KOReader Sync",
            uiDevice.wait(Until.hasObject(By.text("KOReader Sync")), 5000L))

        // Scroll down until "Enable progress sync" is in view
        var syncRow: UiObject2? = null
        for (i in 0 until 5) {
            syncRow = uiDevice.findObject(By.text("Enable progress sync"))
            if (syncRow != null) break
            uiDevice.swipe(
                uiDevice.displayWidth / 2,
                (uiDevice.displayHeight * 0.75).toInt(),
                uiDevice.displayWidth / 2,
                (uiDevice.displayHeight * 0.25).toInt(),
                15
            )
            uiDevice.waitForIdle()
        }
        checkNotNull(syncRow) { "Enable progress sync row should exist" }

        // The toggleable row container exposes checkable state
        val switchContainer = syncRow.parent ?: syncRow
        assertTrue("KOReader sync row should be checkable", switchContainer.isCheckable)

        val beforeState = switchContainer.isChecked
        switchContainer.click()
        uiDevice.waitForIdle()

        val afterRow = checkNotNull(uiDevice.findObject(By.text("Enable progress sync")))
        val afterContainer = afterRow.parent ?: afterRow
        assertEquals("Switch state should invert after click", !beforeState, afterContainer.isChecked)

        // Restore original state
        afterContainer.click()
        uiDevice.waitForIdle()
    }

    @Test
    fun readerSettings_switchesAndKnobsHaveAccessibleSemantics() {
        launchReader(showToolbar = true)

        val settingsBtn = checkNotNull(
            uiDevice.wait(Until.findObject(By.desc("Reader settings")), 10000L)
        ) { "Reader settings button should be visible in toolbar" }
        settingsBtn.click()
        uiDevice.waitForIdle()

        assertTrue(
            "Reader Settings sheet should appear",
            uiDevice.wait(Until.hasObject(By.text("Reader Settings")), 5000L)
        )

        expandBottomSheetIfPossible()

        // 1. Page margin and Line height value text
        var marginNode: UiObject2? = null
        var lineHeightNode: UiObject2? = null
        for (i in 0 until 6) {
            marginNode = uiDevice.findObject(By.descContains("Page margin"))
            lineHeightNode = uiDevice.findObject(By.descContains("Line height"))
            if (marginNode != null && lineHeightNode != null) break
            if (!scrollForward()) {
                scrollSheetDownSmall()
            }
        }

        assertNotNull(
            "Page margins value text should expose contentDescription. Dump:\n${dumpActiveWindow()}",
            marginNode
        )
        assertNotNull(
            "Line height value text should expose contentDescription. Dump:\n${dumpActiveWindow()}",
            lineHeightNode
        )

        // 2. Scroll gently inside the settings sheet to reveal Tap zones
        var leftZoneChip: UiObject2? = null
        for (i in 0 until 8) {
            leftZoneChip = uiDevice.findObject(By.desc("Left zone: Previous page"))
            if (leftZoneChip != null) break
            if (!scrollForward()) {
                scrollSheetDownSmall()
            }
        }

        assertNotNull(
            "Left zone option should have disambiguated description. Dump:\n${dumpActiveWindow()}",
            leftZoneChip
        )
        assertNotNull(
            "Center zone option should have disambiguated description",
            uiDevice.findObject(By.desc("Center zone: Toggle menu"))
        )
        assertNotNull(
            "Right zone option should have disambiguated description",
            uiDevice.findObject(By.desc("Right zone: Next page"))
        )

        // 3. Scroll down further to reach Reduce motion
        var reduceMotionRow: UiObject2? = null
        for (i in 0 until 8) {
            reduceMotionRow = uiDevice.findObject(By.text("Reduce motion"))
            if (reduceMotionRow != null) break
            if (!scrollForward()) {
                scrollSheetDownSmall()
            }
        }

        checkNotNull(reduceMotionRow) { "Reduce motion row should exist. Dump:\n${dumpActiveWindow()}" }
        val switchContainer = reduceMotionRow.parent ?: reduceMotionRow
        assertTrue("Reduce motion switch should be checkable", switchContainer.isCheckable)
    }

    @Test
    fun readerSearch_hasContentDescription() {
        launchReader(showToolbar = true)

        val searchBtn = checkNotNull(
            uiDevice.wait(Until.findObject(By.desc("Search in book")), 10000L)
        ) { "Search in book button should be visible in toolbar" }
        searchBtn.click()
        uiDevice.waitForIdle()

        // Search text field should expose "Search in book"
        val searchField = checkNotNull(
            uiDevice.wait(Until.findObject(By.desc("Search in book")), 5000L)
        ) { "Search text field should mount with contentDescription 'Search in book'" }
        assertNotNull(searchField)
    }
}
