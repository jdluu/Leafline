package com.jdluu.leafline.reader.annotations

import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBoxValue
import com.jdluu.leafline.library.data.Annotation

/**
 * Confirms a row swipe only when it settles into an end-to-start
 * dismissal. Partial swipes settle back to `Settled` and never confirm, so
 * nothing is deleted until the dismissal threshold is reached.
 */
fun confirmEndToStartDismiss(value: SwipeToDismissBoxValue): Boolean {
    return value == SwipeToDismissBoxValue.EndToStart
}

/**
 * Backward-compatible alias for highlight row dismissals.
 */
internal fun confirmHighlightDismiss(value: SwipeToDismissBoxValue): Boolean =
    confirmEndToStartDismiss(value)

/**
 * Generic execution-with-undo orchestration. An action executes, an Undo
 * snackbar action is presented, and if confirmed, the undo action is invoked.
 * Lambdas are injected so the flow stays purely JVM-testable without Android UI coupling.
 */
open class UndoCoordinator<T>(
    private val execute: suspend (T) -> Unit,
    private val undo: suspend (T) -> Unit,
    private val showUndoSnackbar: suspend () -> SnackbarResult
) {
    suspend fun executeWithUndo(item: T) {
        execute(item)
        if (showUndoSnackbar() == SnackbarResult.ActionPerformed) {
            undo(item)
        }
    }
}

/**
 * Deletion-with-undo orchestration for a highlight row. A confirmed dismissal
 * deletes through the repository, offers an Undo snackbar action, and restores
 * the exact annotation when that action is taken.
 */
internal class HighlightUndoCoordinator(
    delete: suspend (Annotation) -> Unit,
    restore: suspend (Annotation) -> Unit,
    showUndoSnackbar: suspend () -> SnackbarResult
) : UndoCoordinator<Annotation>(delete, restore, showUndoSnackbar) {
    suspend fun deleteWithUndo(annotation: Annotation) = executeWithUndo(annotation)
}
