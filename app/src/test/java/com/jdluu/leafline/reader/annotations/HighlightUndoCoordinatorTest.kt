package com.jdluu.leafline.reader.annotations

import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBoxValue
import com.jdluu.leafline.library.data.Annotation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightUndoCoordinatorTest {

    private companion object {
        val ANNOTATION = Annotation(
            id = 7L,
            bookId = "book-1",
            locatorJson = """{"href": "/OEBPS/chapter01.xhtml"}""",
            colorHex = "#80B39DDB",
            note = "Key passage",
            createdAt = 5000L
        )
    }

    private class Recorder {
        val calls = mutableListOf<String>()
        var snackbarResult: SnackbarResult = SnackbarResult.Dismissed

        fun delete(annotation: Annotation) {
            calls.add("delete:${annotation.id}")
        }

        fun restore(annotation: Annotation) {
            calls.add("restore:${annotation.id}")
        }
    }

    @Test
    fun `deleteWithUndo deletes then restores when undo action performed`() = runTest {
        val recorder = Recorder()
        recorder.snackbarResult = SnackbarResult.ActionPerformed
        val coordinator = HighlightUndoCoordinator(
            delete = { recorder.delete(it) },
            restore = { recorder.restore(it) },
            showUndoSnackbar = { recorder.snackbarResult }
        )

        coordinator.deleteWithUndo(ANNOTATION)

        assertEquals(listOf("delete:7", "restore:7"), recorder.calls)
    }

    @Test
    fun `deleteWithUndo deletes without restoring when snackbar dismissed`() = runTest {
        val recorder = Recorder()
        recorder.snackbarResult = SnackbarResult.Dismissed
        val coordinator = HighlightUndoCoordinator(
            delete = { recorder.delete(it) },
            restore = { recorder.restore(it) },
            showUndoSnackbar = { recorder.snackbarResult }
        )

        coordinator.deleteWithUndo(ANNOTATION)

        assertEquals(listOf("delete:7"), recorder.calls)
    }

    @Test
    fun `deleteWithUndo deletes before showing the undo snackbar`() = runTest {
        val order = mutableListOf<String>()
        val coordinator = HighlightUndoCoordinator(
            delete = { order.add("delete") },
            restore = { order.add("restore") },
            showUndoSnackbar = {
                order.add("snackbar")
                SnackbarResult.ActionPerformed
            }
        )

        coordinator.deleteWithUndo(ANNOTATION)

        assertEquals(listOf("delete", "snackbar", "restore"), order)
    }

    @Test
    fun `generic UndoCoordinator works for arbitrary string items`() = runTest {
        val log = mutableListOf<String>()
        val coordinator = UndoCoordinator<String>(
            execute = { log.add("exec:$it") },
            undo = { log.add("undo:$it") },
            showUndoSnackbar = { SnackbarResult.ActionPerformed }
        )

        coordinator.executeWithUndo("item-123")

        assertEquals(listOf("exec:item-123", "undo:item-123"), log)
    }
}

class ConfirmHighlightDismissTest {

    @Test
    fun `end to start confirms the dismissal`() {
        assertTrue(confirmHighlightDismiss(SwipeToDismissBoxValue.EndToStart))
        assertTrue(confirmEndToStartDismiss(SwipeToDismissBoxValue.EndToStart))
    }

    @Test
    fun `start to end never confirms`() {
        assertFalse(confirmHighlightDismiss(SwipeToDismissBoxValue.StartToEnd))
        assertFalse(confirmEndToStartDismiss(SwipeToDismissBoxValue.StartToEnd))
    }

    @Test
    fun `settled swipes never confirm`() {
        assertFalse(confirmHighlightDismiss(SwipeToDismissBoxValue.Settled))
        assertFalse(confirmEndToStartDismiss(SwipeToDismissBoxValue.Settled))
    }
}
