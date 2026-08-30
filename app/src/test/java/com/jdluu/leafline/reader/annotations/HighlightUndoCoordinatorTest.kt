package com.jdluu.leafline.reader.annotations

import androidx.compose.material3.SnackbarResult
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
}

class ConfirmHighlightDismissTest {

    @Test
    fun `end to start confirms the dismissal`() {
        assertTrue(confirmHighlightDismiss(androidx.compose.material3.SwipeToDismissBoxValue.EndToStart))
    }

    @Test
    fun `start to end never confirms`() {
        assertFalse(confirmHighlightDismiss(androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd))
    }

    @Test
    fun `settled swipes never confirm`() {
        assertFalse(confirmHighlightDismiss(androidx.compose.material3.SwipeToDismissBoxValue.Settled))
    }
}