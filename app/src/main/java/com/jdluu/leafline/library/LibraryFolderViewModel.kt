package com.jdluu.leafline.library

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exposes saved library folders for management: lists them with a human-readable
 * label and accessibility state, and removes a folder while releasing its
 * persisted read grant when the app still holds it.
 *
 * The [FolderMetadata] seam is injected so the folder status and grant-release
 * logic is covered by plain-JVM unit tests (see LibraryFolderViewModelTest);
 * the SAF implementation lives on the device.
 */
class LibraryFolderViewModel(
    private val store: LibraryFolderStore,
    private val metadata: FolderMetadata
) : ViewModel() {

    private val _folders = MutableStateFlow(store.load().map { toFolder(it) })
    val folders: StateFlow<List<LibraryFolder>> = _folders.asStateFlow()

    /** Re-reads the persisted folder list, picking up folders added via the picker. */
    fun refresh() {
        _folders.value = store.load().map { toFolder(it) }
    }

    /** Removes a saved folder and releases its persisted read grant when the app held it. */
    fun remove(uri: String) {
        val folder = _folders.value.firstOrNull { it.uri == uri }
        if (folder == null) return
        // Release defensively and idempotently: the release is a no-op when the grant is not
        // held, so it is safe to attempt even when the earlier accessibility query failed and
        // marked the folder as inaccessible.
        runCatching { metadata.releasePersistedReadGrant(uri) }
        store.remove(uri)
        refresh()
    }

    private fun toFolder(uri: String): LibraryFolder = LibraryFolder(
        uri = uri,
        label = runCatching { metadata.labelFor(uri) }.getOrNull(),
        accessible = runCatching { metadata.hasPersistedReadGrant(uri) }.getOrDefault(false)
    )
}
