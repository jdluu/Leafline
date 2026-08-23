package com.jdluu.leafline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.LibrarySort
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.cover.CoverLoader
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.library.data.LibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    /** Collections available in the library. */
    val collections: StateFlow<List<Collection>> = repository.getAllCollections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Currently selected collection filter (null = show all). */
    private val _selectedCollection = MutableStateFlow<Long?>(null)
    val selectedCollection: StateFlow<Long?> = _selectedCollection.asStateFlow()

    /** Book stableIds that belong to the currently selected collection. */
    private val _collectionBookIds = MutableStateFlow<Set<String>?>(null)

    /** Currently selected reading status filter (null = show all). */
    private val _readingStatusFilter = MutableStateFlow<ReadingStatus?>(null)
    val readingStatusFilter: StateFlow<ReadingStatus?> = _readingStatusFilter.asStateFlow()

    val sortedBooks: StateFlow<List<LibraryBook>> =
        combine(
            searchedBooks,
            _sort,
            _selectedCollection,
            _collectionBookIds,
            _readingStatusFilter
        ) { books, sort, collectionId, bookIds, statusFilter ->
            val byCollection = if (collectionId == null || bookIds == null) {
                books
            } else {
                books.filter { it.stableId in bookIds }
            }
            val byStatus = if (statusFilter == null) {
                byCollection
            } else {
                byCollection.filter { it.readingStatus == statusFilter }
            }
            sort.sorted(byStatus)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // When selected collection changes, load its book IDs
        viewModelScope.launch {
            _selectedCollection.collect { collectionId ->
                if (collectionId == null) {
                    _collectionBookIds.value = null
                } else {
                    val ids = repository.getBookIdsForCollection(collectionId)
                    _collectionBookIds.value = ids.toSet()
                }
            }
        }
        // Refresh book IDs when collections data changes
        viewModelScope.launch {
            collections.collect {
                val sel = _selectedCollection.value ?: return@collect
                val ids = repository.getBookIdsForCollection(sel)
                _collectionBookIds.value = ids.toSet()
            }
        }
        // Cover loading (existing logic)
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

    private val attemptedCoverIds = mutableSetOf<String>()

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

    // -- Reading status filter --

    fun setReadingStatusFilter(status: ReadingStatus?) {
        _readingStatusFilter.value = status
    }

    fun setBookReadingStatus(stableId: String, status: ReadingStatus) {
        viewModelScope.launch {
            repository.setReadingStatus(stableId, status)
        }
    }

    // -- Collection operations --

    fun selectCollection(id: Long?) {
        _selectedCollection.value = id
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            repository.createCollection(name)
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            repository.deleteCollection(id)
            if (_selectedCollection.value == id) _selectedCollection.value = null
        }
    }

    fun renameCollection(id: Long, name: String) {
        viewModelScope.launch {
            repository.renameCollection(id, name)
        }
    }

    fun addBookToCollection(bookStableId: String, collectionId: Long) {
        viewModelScope.launch {
            repository.addBookToCollection(collectionId, bookStableId)
        }
    }

    fun removeBookFromCollection(bookStableId: String, collectionId: Long) {
        viewModelScope.launch {
            repository.removeBookFromCollection(collectionId, bookStableId)
        }
    }
}