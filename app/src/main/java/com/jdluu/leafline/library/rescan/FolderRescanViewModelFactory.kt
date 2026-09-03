package com.jdluu.leafline.library.rescan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jdluu.leafline.EpubImporter
import com.jdluu.leafline.library.data.LibraryRepository

class FolderRescanViewModelFactory(
    private val runner: FolderRescanRunner
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FolderRescanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FolderRescanViewModel(runner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/** Builds a SAF-backed [FolderRescanRunner] from the app shell's dependencies. */
fun createFolderRescanRunner(
    context: Context,
    importer: EpubImporter,
    repository: LibraryRepository
): FolderRescanRunner = SaFFolderRescanRunner(
    contentResolver = context.contentResolver,
    context = context.applicationContext,
    importer = importer,
    repository = repository
)
