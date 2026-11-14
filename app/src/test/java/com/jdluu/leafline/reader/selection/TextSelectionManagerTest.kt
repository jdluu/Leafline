package com.jdluu.leafline.reader.selection

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TextSelectionManagerTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private var highlightCalled = false
    private val snackbars = mutableListOf<String>()

    private lateinit var textSelectionManager: TextSelectionManager

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
        val mockMode = mock(ActionMode::class.java)
        val mockMenu = mock(Menu::class.java)
        val mockItem = mock(MenuItem::class.java)

        `when`(mockMenu.add(TextSelectionManager.GROUP_PRIMARY, TextSelectionManager.MENU_ITEM_HIGHLIGHT_ID, 1, "Highlight"))
            .thenReturn(mockItem)
        `when`(mockMenu.add(TextSelectionManager.GROUP_PRIMARY, TextSelectionManager.MENU_ITEM_COPY_ID, 2, "Copy"))
            .thenReturn(mockItem)
        `when`(mockMenu.add(TextSelectionManager.GROUP_PRIMARY, TextSelectionManager.MENU_ITEM_DEFINE_ID, 3, "Define"))
            .thenReturn(mockItem)
        `when`(mockMenu.add(TextSelectionManager.GROUP_PRIMARY, TextSelectionManager.MENU_ITEM_SHARE_ID, 4, "Share"))
            .thenReturn(mockItem)

        val result = callback.onCreateActionMode(mockMode, mockMenu)
        assertTrue(result)

        verify(mockMenu).add(TextSelectionManager.GROUP_PRIMARY, TextSelectionManager.MENU_ITEM_HIGHLIGHT_ID, 1, "Highlight")
        verify(mockMenu).add(TextSelectionManager.GROUP_PRIMARY, TextSelectionManager.MENU_ITEM_COPY_ID, 2, "Copy")
        verify(mockMenu).add(TextSelectionManager.GROUP_PRIMARY, TextSelectionManager.MENU_ITEM_DEFINE_ID, 3, "Define")
        verify(mockMenu).add(TextSelectionManager.GROUP_PRIMARY, TextSelectionManager.MENU_ITEM_SHARE_ID, 4, "Share")
    }

    @Test
    fun onActionItemClickedDelegatesHighlightAndFinishesMode() {
        val callback = textSelectionManager.selectionActionMode()
        val mockMode = mock(ActionMode::class.java)
        val mockItem = mock(MenuItem::class.java)
        `when`(mockItem.itemId).thenReturn(TextSelectionManager.MENU_ITEM_HIGHLIGHT_ID)

        val handled = callback.onActionItemClicked(mockMode, mockItem)
        assertTrue(handled)
        assertTrue(highlightCalled)
        verify(mockMode).finish()
    }

    @Test
    fun onActionItemClickedFinishesOnCopy() = testScope.runTest {
        val callback = textSelectionManager.selectionActionMode()
        val mockMode = mock(ActionMode::class.java)
        val mockItem = mock(MenuItem::class.java)
        `when`(mockItem.itemId).thenReturn(TextSelectionManager.MENU_ITEM_COPY_ID)

        val handled = callback.onActionItemClicked(mockMode, mockItem)
        assertTrue(handled)
        verify(mockMode).finish()

        testDispatcher.scheduler.advanceUntilIdle()
        // Since navigator returns null, it reports no text selected
        assertTrue(snackbars.contains("No text selected"))
    }

    @Test
    fun onActionItemClickedFinishesOnDefine() = testScope.runTest {
        val callback = textSelectionManager.selectionActionMode()
        val mockMode = mock(ActionMode::class.java)
        val mockItem = mock(MenuItem::class.java)
        `when`(mockItem.itemId).thenReturn(TextSelectionManager.MENU_ITEM_DEFINE_ID)

        val handled = callback.onActionItemClicked(mockMode, mockItem)
        assertTrue(handled)
        verify(mockMode).finish()

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(snackbars.contains("No text selected"))
    }

    @Test
    fun onActionItemClickedFinishesOnShare() = testScope.runTest {
        val callback = textSelectionManager.selectionActionMode()
        val mockMode = mock(ActionMode::class.java)
        val mockItem = mock(MenuItem::class.java)
        `when`(mockItem.itemId).thenReturn(TextSelectionManager.MENU_ITEM_SHARE_ID)

        val handled = callback.onActionItemClicked(mockMode, mockItem)
        assertTrue(handled)
        verify(mockMode).finish()

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(snackbars.contains("No text selected"))
    }

    @Test
    fun unhandledMenuItemReturnsFalse() {
        val callback = textSelectionManager.selectionActionMode()
        val mockMode = mock(ActionMode::class.java)
        val mockItem = mock(MenuItem::class.java)
        `when`(mockItem.itemId).thenReturn(9999)
        `when`(mockItem.groupId).thenReturn(9999)

        val handled = callback.onActionItemClicked(mockMode, mockItem)
        assertFalse(handled)
    }
}
