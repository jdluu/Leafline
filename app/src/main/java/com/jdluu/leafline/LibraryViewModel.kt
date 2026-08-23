package com.jdluu.leafline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.LibrarySort
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.cover.CoverLoader
import com.jdluu.leafline.library.data.LibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val coverLoader: CoverLoader? = null,
    private val sortStore: LibrarySortStore? = null
) : ViewModel() {

    val books: StateFlow<List<LibraryBook>> = repository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sort = MutableStateFlow(sortStore?.load() ?: LibrarySort.RECENT)
    val sort: StateFlow<LibrarySort> = _sort.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchedBooks: Flow<List<LibraryBook>> = _query.flatMapLatest { query ->
        if (query.isBlank()) repository.getAllBooks() else repository.searchBooks(query.trim())
    }

    val sortedBooks: StateFlow<List<LibraryBook>> =
        combine(searchedBooks, _sort) { books, sort -> sort.sorted(books) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val attemptedCoverIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            books.collect { list ->
                for (book in list) {
                    if (book.coverPath != null) continue
                    if (!attemptedCoverIds.add(book.stableId)) continue
                    val path = try {
                        coverLoader?.loadCoverPath(book)
                    } catch (e: Exception) {
                        null
                    }
                    if (path != null && path != book.coverPath) {
                        repository.setCoverPath(book.stableId, path)
                    }
                }
            }
        }
    }

    fun setSort(sort: LibrarySort) {
        if (_sort.value == sort) return
        _sort.value = sort
        sortStore?.save(sort)
    }

    fun setQuery(query: String) {
        _query.value = query
    }

    fun addBook(book: LibraryBook) {
        viewModelScope.launch {
            repository.addBook(book)
        }
    }
}
