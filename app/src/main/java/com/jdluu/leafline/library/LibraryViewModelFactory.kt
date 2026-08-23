package com.jdluu.leafline.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jdluu.leafline.LibraryViewModel
import com.jdluu.leafline.library.cover.CoverLoader
import com.jdluu.leafline.library.data.LibraryRepository

class LibraryViewModelFactory(
    private val repository: LibraryRepository,
    private val coverLoader: CoverLoader? = null,
    private val sortStore: LibrarySortStore? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(repository, coverLoader, sortStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
