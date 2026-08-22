package com.jdluu.leafline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository
) : ViewModel() {
    
    val books: StateFlow<List<LibraryBook>> = repository.getAllBooks()
        .map { books -> books }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun addBook(book: LibraryBook) {
        viewModelScope.launch {
            repository.addBook(book)
        }
    }
}