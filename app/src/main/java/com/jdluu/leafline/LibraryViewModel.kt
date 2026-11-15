package com.jdluu.leafline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.leafline.library.ImportedBookReconciler
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.LibrarySort
import com.jdluu.leafline.library.LibraryLayout
import com.jdluu.leafline.library.LibraryLayoutStore
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.ProgressFilter
import com.jdluu.leafline.library.cover.CoverLoader
import com.jdluu.leafline.library.data.Collection
import com.jdluu.leafline.library.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.jdluu.leafline.library.data.ReadingAggregate
import com.jdluu.leafline.library.data.ReadingSessionRepository
import java.time.LocalDate

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val coverLoader: CoverLoader? = null,
    private val sortStore: LibrarySortStore? = null,
    private val readingSessionRepository: ReadingSessionRepository? = null,
    private val layoutStore: LibraryLayoutStore? = null
) : ViewModel() {

    val books: StateFlow<List<LibraryBook>> = repository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * True once the library has emitted at least one book list.
     *
     * The `books` and `sortedBooks` flows start at `emptyList()`, which is
     * indistinguishable from a genuinely empty library. The Library screen
     * shows a loading surface while this is false and only presents the
     * empty state after the store has actually been read.
     */
    val isLibraryLoaded: StateFlow<Boolean> = repository.getAllBooks()
        .map { true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /** Books with saved progress, sorted by last-read time (newest first). */
    val continueReadingBooks: StateFlow<List<LibraryBook>> = repository.getRecentlyReadBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sort = MutableStateFlow(sortStore?.load() ?: LibrarySort.RECENT)
    val sort: StateFlow<LibrarySort> = _sort.asStateFlow()

    private val _sortAscending = MutableStateFlow(
        sortStore?.loadAscending(_sort.value.defaultAscending) ?: _sort.value.defaultAscending
    )
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _progressFilter = MutableStateFlow(ProgressFilter.ALL)
    val progressFilter: StateFlow<ProgressFilter> = _progressFilter.asStateFlow()

    private val _layout = MutableStateFlow(layoutStore?.load() ?: LibraryLayout.GRID)
    val layout: StateFlow<LibraryLayout> = _layout.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val searchedBooks: Flow<List<LibraryBook>> = _query
        .debounce { query -> if (query.isBlank()) 0L else 200L }
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllBooks() else repository.searchBooks(query.trim())
        }

    /** Collections available in the library. */
    val collections: StateFlow<List<Collection>> = repository.getAllCollections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Map of collection ID to book count in that collection. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val collectionCounts: StateFlow<Map<Long, Int>> = repository.getAllCollections()
        .flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(list.map { col ->
                    repository.observeBookIdsForCollection(col.id).map { col.id to it.size }
                }) { pairs ->
                    pairs.toMap()
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    /** Currently selected collection filter (null = show all). */
    private val _selectedCollection = MutableStateFlow<Long?>(null)
    val selectedCollection: StateFlow<Long?> = _selectedCollection.asStateFlow()

    /** Book stableIds that belong to the currently selected collection. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val collectionBookIds: StateFlow<Set<String>?> = _selectedCollection
        .flatMapLatest { collectionId ->
            if (collectionId == null) {
                flowOf(null)
            } else {
                repository.observeBookIdsForCollection(collectionId).map { it.toSet() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Currently selected reading status filter (null = show all). */
    private val _readingStatusFilter = MutableStateFlow<ReadingStatus?>(null)
    val readingStatusFilter: StateFlow<ReadingStatus?> = _readingStatusFilter.asStateFlow()

    /** All distinct tags currently in the library, sorted alphabetically. */
    val allTags: StateFlow<List<String>> = repository.getAllBooks()
        .map { list ->
            list.flatMap { it.tags }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Map of lowercase tag name to count of books bearing that tag. */
    val tagCounts: StateFlow<Map<String, Int>> = repository.getAllBooks()
        .map { list ->
            val counts = mutableMapOf<String, Int>()
            list.forEach { book ->
                book.tags.forEach { rawTag ->
                    val tag = rawTag.trim()
                    if (tag.isNotEmpty()) {
                        val key = tag.lowercase()
                        counts[key] = (counts[key] ?: 0) + 1
                    }
                }
            }
            counts
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    /** Currently selected tag filter (null = show all). */
    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private data class LibraryFilterCriteria(
        val sort: LibrarySort,
        val sortAscending: Boolean,
        val collectionId: Long?,
        val bookIds: Set<String>?,
        val statusFilter: ReadingStatus?,
        val progressFilter: ProgressFilter,
        val tagFilter: String?
    )

    private val filterCriteria: Flow<LibraryFilterCriteria> =
        combine(
            _sort,
            _sortAscending,
            _selectedCollection,
            collectionBookIds,
            _readingStatusFilter,
            _progressFilter,
            _selectedTag
        ) { array ->
            @Suppress("UNCHECKED_CAST")
            LibraryFilterCriteria(
                sort = array[0] as LibrarySort,
                sortAscending = array[1] as Boolean,
                collectionId = array[2] as Long?,
                bookIds = array[3] as Set<String>?,
                statusFilter = array[4] as ReadingStatus?,
                progressFilter = array[5] as ProgressFilter,
                tagFilter = array[6] as String?
            )
        }

    val sortedBooks: StateFlow<List<LibraryBook>> =
        combine(searchedBooks, filterCriteria) { books, criteria ->
            val byCollection = if (criteria.collectionId == null || criteria.bookIds == null) {
                books
            } else {
                books.filter { it.stableId in criteria.bookIds }
            }
            val byStatus = if (criteria.statusFilter == null) {
                byCollection
            } else {
                byCollection.filter { it.readingStatus == criteria.statusFilter }
            }
            val byProgress = if (criteria.progressFilter == ProgressFilter.ALL) {
                byStatus
            } else {
                byStatus.filter { criteria.progressFilter.matches(it) }
            }
            val byTag = if (criteria.tagFilter == null) {
                byProgress
            } else {
                byProgress.filter { book -> book.tags.any { it.equals(criteria.tagFilter, ignoreCase = true) } }
            }
            criteria.sort.sorted(byTag, criteria.sortAscending)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Cover loading on IO dispatcher to prevent main thread blocking
        viewModelScope.launch(Dispatchers.IO) {
            books.collect { list ->
                for (book in list) {
                    if (book.coverPath != null) continue
                    if (!attemptedCoverIds.add(book.stableId)) continue
                    val path = try {
                        coverLoader?.loadCoverPath(book)
                    } catch (e: Exception) {
                        null
                    }
                    if (path != null) {
                        repository.setCoverPath(book.stableId, path)
                    }
                }
            }
        }
    }

    private val attemptedCoverIds = mutableSetOf<String>()

    private val collectionIdsCache = mutableMapOf<String, Flow<Set<Long>>>()

    /**
     * Reactive set of collection IDs that [bookStableId] belongs to.
     *
     * Emits the known membership whenever the repository's
     * [LibraryRepository.getCollectionsForBook] flow emits. Repeated calls for
     * the same stable ID return the same cached flow instance.
     */
    fun collectionIdsForBook(bookStableId: String): Flow<Set<Long>> =
        collectionIdsCache.getOrPut(bookStableId) {
            repository.getCollectionsForBook(bookStableId).map { collections ->
                collections.mapTo(mutableSetOf()) { it.id }
            }
        }

    fun setSort(sort: LibrarySort) {
        if (_sort.value == sort) {
            toggleSortDirection()
        } else {
            _sort.value = sort
            _sortAscending.value = sort.defaultAscending
            sortStore?.save(sort)
            sortStore?.saveAscending(sort.defaultAscending)
        }
    }

    fun toggleSortDirection() {
        val next = !_sortAscending.value
        _sortAscending.value = next
        sortStore?.saveAscending(next)
    }

    fun setSortDirection(ascending: Boolean) {
        if (_sortAscending.value == ascending) return
        _sortAscending.value = ascending
        sortStore?.saveAscending(ascending)
    }

    fun setProgressFilter(filter: ProgressFilter) {
        _progressFilter.value = filter
    }

    fun setLayout(layout: LibraryLayout) {
        if (_layout.value == layout) return
        _layout.value = layout
        layoutStore?.save(layout)
    }

    fun toggleLayout() {
        val next = if (_layout.value == LibraryLayout.GRID) LibraryLayout.LIST else LibraryLayout.GRID
        setLayout(next)
    }

    fun setQuery(query: String) {
        _query.value = query
    }

    fun addBook(book: LibraryBook) {
        viewModelScope.launch {
            val existing = repository.getBookByFileHash(book.fileHash)
            val reconciled = existing?.let { ImportedBookReconciler.merge(it, book) }
            if (reconciled != null) {
                repository.updateBook(reconciled)
            } else if (existing == null) {
                repository.addBook(book)
            }
        }
    }

    fun deleteBook(stableId: String) {
        viewModelScope.launch {
            repository.deleteBook(stableId)
        }
    }

    fun deleteBooks(stableIds: Set<String>) {
        viewModelScope.launch {
            stableIds.forEach { id ->
                repository.deleteBook(id)
            }
        }
    }

    fun setBooksReadingStatus(stableIds: Set<String>, status: ReadingStatus) {
        viewModelScope.launch {
            stableIds.forEach { id ->
                repository.setReadingStatus(id, status)
            }
        }
    }

    fun addBooksToCollection(stableIds: Set<String>, collectionId: Long) {
        viewModelScope.launch {
            stableIds.forEach { id ->
                repository.addBookToCollection(collectionId, id)
            }
        }
    }

    // -- Reading status filter --

    fun setReadingStatusFilter(status: ReadingStatus?) {
        _readingStatusFilter.value = status
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun setRating(stableId: String, rating: Int?) {
        viewModelScope.launch {
            repository.setRating(stableId, rating)
        }
    }

    fun setTags(stableId: String, tags: List<String>) {
        viewModelScope.launch {
            repository.setTags(stableId, tags)
        }
    }

    fun addTag(stableId: String, tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val book = repository.getBookByStableId(stableId) ?: return@launch
            if (!book.tags.any { it.equals(trimmed, ignoreCase = true) }) {
                repository.setTags(stableId, book.tags + trimmed)
            }
        }
    }

    fun removeTag(stableId: String, tag: String) {
        viewModelScope.launch {
            val book = repository.getBookByStableId(stableId) ?: return@launch
            repository.setTags(stableId, book.tags.filterNot { it.equals(tag, ignoreCase = true) })
        }
    }

    fun addTagToBooks(stableIds: Set<String>, tag: String) {
        val trimmed = tag.trim().removePrefix("#")
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            stableIds.forEach { stableId ->
                val book = repository.getBookByStableId(stableId) ?: return@forEach
                if (!book.tags.any { it.equals(trimmed, ignoreCase = true) }) {
                    repository.setTags(stableId, book.tags + trimmed)
                }
            }
        }
    }

    fun removeTagFromBooks(stableIds: Set<String>, tag: String) {
        val trimmed = tag.trim().removePrefix("#")
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            stableIds.forEach { stableId ->
                val book = repository.getBookByStableId(stableId) ?: return@forEach
                val remaining = book.tags.filterNot { it.equals(trimmed, ignoreCase = true) }
                if (remaining.size != book.tags.size) {
                    repository.setTags(stableId, remaining)
                }
            }
        }
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

    suspend fun getReadingAggregateForBook(bookId: String): ReadingAggregate {
        return readingSessionRepository?.getAggregateForBook(bookId) ?: ReadingAggregate.EMPTY
    }

    suspend fun getTodayAggregate(): ReadingAggregate {
        return readingSessionRepository?.getDailyAggregate(LocalDate.now()) ?: ReadingAggregate.EMPTY
    }

    suspend fun getWeeklyAggregate(): ReadingAggregate {
        return readingSessionRepository?.getWeeklyAggregate(LocalDate.now()) ?: ReadingAggregate.EMPTY
    }

    suspend fun getRecentDailyAggregates(days: Int = 7): List<Pair<LocalDate, ReadingAggregate>> {
        val repo = readingSessionRepository ?: return emptyList()
        val today = LocalDate.now()
        val result = mutableListOf<Pair<LocalDate, ReadingAggregate>>()
        for (i in (days - 1) downTo 0) {
            val date = today.minusDays(i.toLong())
            val aggregate = repo.getDailyAggregate(date)
            result.add(date to aggregate)
        }
        return result
    }
}
