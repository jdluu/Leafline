package com.jdluu.leafline.library.rescan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runs saved-folder rescans off the main thread and exposes the latest
 * [RescanSummary] as a [FolderRescanUiState].
 *
 * The [runner] is injected so the ViewModel can be driven by a fake in
 * plain-JVM unit tests; the production wiring passes a [FolderRescanRunner]
 * backed by the device shell.
 */
class FolderRescanViewModel(
    private val runner: FolderRescanRunner,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow<FolderRescanUiState>(FolderRescanUiState.Idle)
    val state: StateFlow<FolderRescanUiState> = _state.asStateFlow()

    /** Re-scans every [folderUri]. */
    fun rescanAll(folderUris: List<String>) {
        viewModelScope.launch(ioDispatcher) {
            _state.value = FolderRescanUiState.Running
            _state.value = runCatching { runner.rescanAll(folderUris) }
                .fold(
                    onSuccess = { FolderRescanUiState.Done(it) },
                    onFailure = { FolderRescanUiState.Error(it.message.orEmpty()) }
                )
        }
    }

    /** Re-scans a single [folderUri] and reports it as a one-folder summary. */
    fun rescanOne(folderUri: String) {
        viewModelScope.launch(ioDispatcher) {
            _state.value = FolderRescanUiState.Running
            _state.value = runCatching { runner.rescanOne(folderUri) }
                .fold(
                    onSuccess = { result ->
                        FolderRescanUiState.Done(
                            RescanSummary(
                                folderResults = listOf(result),
                                totals = when (result) {
                                    is FolderRescanResult.Success -> result.counts
                                    is FolderRescanResult.Failed -> RescanCounts()
                                }
                            )
                        )
                    },
                    onFailure = { FolderRescanUiState.Error(it.message.orEmpty()) }
                )
        }
    }
}
