package com.jdluu.leafline.library.rescan

/**
 * UI state for a folder rescan.
 *
 * [Done] carries the latest [RescanSummary] so the management sheet can show a
 * concise, accessible report of added, updated, skipped, and failed files.
 * [Error] is a terminal state reached when the runner itself throws (as opposed
 * to a recoverable per-folder result, which is reported inside [Done]). The
 * sheet treats it as unrecoverable and not a spinner.
 */
sealed interface FolderRescanUiState {
    data object Idle : FolderRescanUiState
    data object Running : FolderRescanUiState
    data class Done(val summary: RescanSummary) : FolderRescanUiState

    /**
     * The runner raised an unhandled exception. [message] carries the failure
     * detail when available. This is terminal: it never falls back to Running.
     */
    data class Error(val message: String = "Rescan failed unexpectedly") : FolderRescanUiState
}
