package com.jdluu.leafline.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class LibraryFolderViewModelFactory(
    private val store: LibraryFolderStore,
    private val metadata: FolderMetadata
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryFolderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryFolderViewModel(store, metadata) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
