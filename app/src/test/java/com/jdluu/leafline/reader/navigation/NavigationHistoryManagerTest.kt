package com.jdluu.leafline.reader.navigation

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationHistoryManagerTest {

    private lateinit var history: NavigationHistoryManager

    private fun createLocator(href: String, progression: Double, title: String? = null): Locator {
        val json = JSONObject().apply {
            put("href", href)
            put("type", "application/xhtml+xml")
            if (title != null) put("title", title)
            put("locations", JSONObject().apply {
                put("progression", progression)
            })
        }
        return Locator.fromJSON(json)!!
    }

    @Before
    fun setUp() {
        history = NavigationHistoryManager(maxHistorySize = 5)
    }

    @Test
    fun initialStacksAreEmpty() {
        assertFalse(history.canGoBack.value)
        assertFalse(history.canGoForward.value)
        assertNull(history.jumpNotice.value)
        assertEquals(0, history.backStackCount)
        assertEquals(0, history.forwardStackCount)
    }

    @Test
    fun recordJumpPushesToBackStackAndSetsNotice() {
        val loc1 = createLocator("chapter1.xhtml", 0.1, "Chapter 1")
        val loc2 = createLocator("chapter2.xhtml", 0.0, "Chapter 2")

        history.recordJump(from = loc1, to = loc2, description = "Chapter 1")

        assertTrue(history.canGoBack.value)
        assertFalse(history.canGoForward.value)
        assertEquals(1, history.backStackCount)

        val notice = history.jumpNotice.value
        assertNotNull(notice)
        assertEquals("Chapter 1", notice?.sourceDescription)
        assertEquals(loc1, notice?.returnLocator)
    }

    @Test
    fun recordJumpIgnoresSimilarLocators() {
        val loc1 = createLocator("chapter1.xhtml", 0.100)
        val loc2 = createLocator("chapter1.xhtml", 0.102) // within 0.005 threshold

        history.recordJump(from = loc1, to = loc2)

        assertFalse(history.canGoBack.value)
        assertEquals(0, history.backStackCount)
    }

    @Test
    fun goBackAndGoForwardTraverseHistory() {
        val loc1 = createLocator("ch1.xhtml", 0.1, "Chapter 1")
        val loc2 = createLocator("ch2.xhtml", 0.2, "Chapter 2")
        val loc3 = createLocator("ch3.xhtml", 0.3, "Chapter 3")

        history.recordJump(from = loc1, to = loc2)
        history.recordJump(from = loc2, to = loc3)

        assertEquals(2, history.backStackCount)
        assertTrue(history.canGoBack.value)
        assertFalse(history.canGoForward.value)

        // Go back from loc3 -> should return loc2
        val backResult1 = history.goBack(current = loc3)
        assertEquals(loc2, backResult1)
        assertEquals(1, history.backStackCount)
        assertEquals(1, history.forwardStackCount)
        assertTrue(history.canGoBack.value)
        assertTrue(history.canGoForward.value)

        // Go back from loc2 -> should return loc1
        val backResult2 = history.goBack(current = loc2)
        assertEquals(loc1, backResult2)
        assertEquals(0, history.backStackCount)
        assertEquals(2, history.forwardStackCount)
        assertFalse(history.canGoBack.value)
        assertTrue(history.canGoForward.value)

        // No more back entries
        assertNull(history.goBack(current = loc1))

        // Go forward from loc1 -> should return loc2
        val forwardResult1 = history.goForward(current = loc1)
        assertEquals(loc2, forwardResult1)
        assertEquals(1, history.backStackCount)
        assertEquals(1, history.forwardStackCount)

        // Go forward from loc2 -> should return loc3
        val forwardResult2 = history.goForward(current = loc2)
        assertEquals(loc3, forwardResult2)
        assertEquals(2, history.backStackCount)
        assertEquals(0, history.forwardStackCount)
        assertTrue(history.canGoBack.value)
        assertFalse(history.canGoForward.value)
    }

    @Test
    fun newJumpClearsForwardStack() {
        val loc1 = createLocator("ch1.xhtml", 0.1)
        val loc2 = createLocator("ch2.xhtml", 0.2)
        val loc3 = createLocator("ch3.xhtml", 0.3)
        val loc4 = createLocator("ch4.xhtml", 0.4)

        history.recordJump(from = loc1, to = loc2)
        history.goBack(current = loc2)
        assertTrue(history.canGoForward.value)

        // New jump occurs from loc1 to loc4
        history.recordJump(from = loc1, to = loc4)
        assertFalse(history.canGoForward.value)
        assertEquals(0, history.forwardStackCount)
    }

    @Test
    fun historyHonorsMaxSizeLimit() {
        val locators = (1..10).map { createLocator("ch$it.xhtml", it * 0.1) }

        for (i in 0 until locators.size - 1) {
            history.recordJump(from = locators[i], to = locators[i + 1])
        }

        // maxHistorySize = 5
        assertEquals(5, history.backStackCount)
    }

    @Test
    fun dismissJumpNoticeClearsNotice() {
        val loc1 = createLocator("ch1.xhtml", 0.1)
        val loc2 = createLocator("ch2.xhtml", 0.2)
        history.recordJump(from = loc1, to = loc2)

        assertNotNull(history.jumpNotice.value)
        history.dismissJumpNotice()
        assertNull(history.jumpNotice.value)
        // Stack should still be available
        assertTrue(history.canGoBack.value)
    }

    @Test
    fun clearWipesAllStacksAndNotices() {
        val loc1 = createLocator("ch1.xhtml", 0.1)
        val loc2 = createLocator("ch2.xhtml", 0.2)
        history.recordJump(from = loc1, to = loc2)

        history.clear()
        assertFalse(history.canGoBack.value)
        assertFalse(history.canGoForward.value)
        assertNull(history.jumpNotice.value)
        assertEquals(0, history.backStackCount)
        assertEquals(0, history.forwardStackCount)
    }
}
