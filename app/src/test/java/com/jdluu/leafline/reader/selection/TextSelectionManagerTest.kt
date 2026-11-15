package com.jdluu.leafline.reader.selection

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextSelectionManagerTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private var highlightCalled = false
    private val snackbars = mutableListOf<String>()

    private lateinit var textSelectionManager: TextSelectionManager

    private class TestActionMode : ActionMode() {
        var finished = false
        override fun setTitle(title: CharSequence?) {}
        override fun setTitle(resId: Int) {}
        override fun setSubtitle(subtitle: CharSequence?) {}
        override fun setSubtitle(resId: Int) {}
        override fun setCustomView(view: View?) {}
        override fun invalidate() {}
        override fun finish() {
            finished = true
        }
        override fun getMenu(): Menu = error("Not needed")
        override fun getTitle(): CharSequence = ""
        override fun getSubtitle(): CharSequence = ""
        override fun getCustomView(): View? = null
        override fun getMenuInflater(): MenuInflater = error("Not needed")
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        highlightCalled = false
        snackbars.clear()

        textSelectionManager = TextSelectionManager(
            context = context,
            scope = testScope,
            navigatorProvider = { null },
            onHighlight = { highlightCalled = true },
            onSnackbar = { snackbars.add(it) },
            bookTitleProvider = { "Test Book Title" }
        )
    }

    @Test
    fun onCreateActionModePopulatesPrimaryMenuItems() {
        val callback = textSelectionManager.selectionActionMode()
        val mode = TestActionMode()
        val popupMenu = PopupMenu(context, View(context))
        val menu = popupMenu.menu

        val result = callback.onCreateActionMode(mode, menu)
        assertTrue(result)

        assertEquals("Highlight", menu.findItem(TextSelectionManager.MENU_ITEM_HIGHLIGHT_ID)?.title)
        assertEquals("Copy", menu.findItem(TextSelectionManager.MENU_ITEM_COPY_ID)?.title)
        assertEquals("Define", menu.findItem(TextSelectionManager.MENU_ITEM_DEFINE_ID)?.title)
        assertEquals("Share", menu.findItem(TextSelectionManager.MENU_ITEM_SHARE_ID)?.title)
    }

    @Test
    fun onActionItemClickedDelegatesHighlightAndFinishesMode() {
        val callback = textSelectionManager.selectionActionMode()
        val mode = TestActionMode()
        val popupMenu = PopupMenu(context, View(context))
        val menu = popupMenu.menu
        callback.onCreateActionMode(mode, menu)

        val highlightItem = menu.findItem(TextSelectionManager.MENU_ITEM_HIGHLIGHT_ID)
        val handled = callback.onActionItemClicked(mode, highlightItem)
        assertTrue(handled)
        assertTrue(highlightCalled)
        assertTrue(mode.finished)
    }

    @Test
    fun onActionItemClickedFinishesOnCopy() = testScope.runTest {
        val callback = textSelectionManager.selectionActionMode()
        val mode = TestActionMode()
        val popupMenu = PopupMenu(context, View(context))
        val menu = popupMenu.menu
        callback.onCreateActionMode(mode, menu)

        val copyItem = menu.findItem(TextSelectionManager.MENU_ITEM_COPY_ID)
        val handled = callback.onActionItemClicked(mode, copyItem)
        assertTrue(handled)
        assertTrue(mode.finished)

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(snackbars.contains("No text selected"))
    }

    @Test
    fun onActionItemClickedFinishesOnDefine() = testScope.runTest {
        val callback = textSelectionManager.selectionActionMode()
        val mode = TestActionMode()
        val popupMenu = PopupMenu(context, View(context))
        val menu = popupMenu.menu
        callback.onCreateActionMode(mode, menu)

        val defineItem = menu.findItem(TextSelectionManager.MENU_ITEM_DEFINE_ID)
        val handled = callback.onActionItemClicked(mode, defineItem)
        assertTrue(handled)
        assertTrue(mode.finished)

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(snackbars.contains("No text selected"))
    }

    @Test
    fun onActionItemClickedFinishesOnShare() = testScope.runTest {
        val callback = textSelectionManager.selectionActionMode()
        val mode = TestActionMode()
        val popupMenu = PopupMenu(context, View(context))
        val menu = popupMenu.menu
        callback.onCreateActionMode(mode, menu)

        val shareItem = menu.findItem(TextSelectionManager.MENU_ITEM_SHARE_ID)
        val handled = callback.onActionItemClicked(mode, shareItem)
        assertTrue(handled)
        assertTrue(mode.finished)

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(snackbars.contains("No text selected"))
    }

    @Test
    fun unhandledMenuItemReturnsFalse() {
        val callback = textSelectionManager.selectionActionMode()
        val mode = TestActionMode()
        val popupMenu = PopupMenu(context, View(context))
        val menu = popupMenu.menu
        val customItem = menu.add(99, 9999, 0, "Custom")

        val handled = callback.onActionItemClicked(mode, customItem)
        assertFalse(handled)
    }
}
