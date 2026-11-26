package com.jdluu.leafline.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jdluu.leafline.LibraryViewModel
import com.jdluu.leafline.library.cover.CoverLoader
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.library.data.ReadingSessionRepository

class LibraryViewModelFactory(
    private val repository: LibraryRepository,
    private val coverLoader: CoverLoader? = null,
    private val sortStore: LibrarySortStore? = null,
    private val readingSessionRepository: ReadingSessionRepository? = null,
    private val layoutStore: LibraryLayoutStore? = null,
    private val readingGoalStore: ReadingGoalStore? = null,
    private val onBooksDeleted: suspend (deleted: List<LibraryBook>, remaining: List<LibraryBook>) -> Unit = { _, _ -> }
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(
                repository = repository,
                coverLoader = coverLoader,
                sortStore = sortStore,
                readingSessionRepository = readingSessionRepository,
                layoutStore = layoutStore,
                readingGoalStore = readingGoalStore,
                onBooksDeleted = onBooksDeleted
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
