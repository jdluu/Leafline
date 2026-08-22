package com.jdluu.leafline.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.LibraryRepository

class LibraryViewModelFactory(
    private val repository: LibraryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.jdluu.leafline.LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.jdluu.leafline.LibraryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}