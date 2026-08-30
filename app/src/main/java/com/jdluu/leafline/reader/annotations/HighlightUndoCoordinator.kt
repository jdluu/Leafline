package com.jdluu.leafline.reader.annotations

import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBoxValue
import com.jdluu.leafline.library.data.Annotation

/**
 * Confirms a highlight-row swipe only when it settles into an end-to-start
 * dismissal. Partial swipes settle back to `Settled` and never confirm, so
 * nothing is deleted until the dismissal threshold is reached.
 */
internal fun confirmHighlightDismiss(value: SwipeToDismissBoxValue): Boolean {
    return value == SwipeToDismissBoxValue.EndToStart
}

/**
 * Deletion-with-undo orchestration for a highlight row. A confirmed dismissal
 * deletes through the repository, offers an Undo snackbar action, and restores
 * the exact annotation when that action is taken. Lambdas are injected so the
 * flow stays JVM-testable; cancelling the snackbar await (for example when the
 * sheet closes) leaves the delete committed.
 */
internal class HighlightUndoCoordinator(
    private val delete: suspend (Annotation) -> Unit,
    private val restore: suspend (Annotation) -> Unit,
    private val showUndoSnackbar: suspend () -> SnackbarResult
) {
    suspend fun deleteWithUndo(annotation: Annotation) {
        delete(annotation)
        if (showUndoSnackbar() == SnackbarResult.ActionPerformed) {
            restore(annotation)
        }
    }
}