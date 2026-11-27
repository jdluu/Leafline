package com.jdluu.leafline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.leafline.library.ImportedBookReconciler
import com.jdluu.leafline.library.BookIdentity
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.jdluu.leafline.library.data.ReadingAggregate
import com.jdluu.leafline.library.data.ReadingSessionRepository
import com.jdluu.leafline.library.ReadingGoalStore
import com.jdluu.leafline.library.ReadingGoals
import java.time.LocalDate

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val coverLoader: CoverLoader? = null,
    private val sortStore: LibrarySortStore? = null,
    private val readingSessionRepository: ReadingSessionRepository? = null,
    private val layoutStore: LibraryLayoutStore? = null,
    private val readingGoalStore: ReadingGoalStore? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onBooksDeleted: suspend (deleted: List<LibraryBook>, remaining: List<LibraryBook>) -> Unit = { _, _ -> }
) : ViewModel() {

    private val sharedAllBooks: SharedFlow<List<LibraryBook>> = repository.getAllBooks()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    val books: StateFlow<List<LibraryBook>> = sharedAllBooks
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
    val isLibraryLoaded: StateFlow<Boolean> = sharedAllBooks
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

    private val _readingGoals = MutableStateFlow(readingGoalStore?.loadGoals() ?: ReadingGoals())
    val readingGoals: StateFlow<ReadingGoals> = _readingGoals.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val searchedBooks: Flow<List<LibraryBook>> = _query
        .debounce { query -> if (query.isBlank()) 0L else 200L }
        .flatMapLatest { query ->
            if (query.isBlank()) sharedAllBooks else repository.searchBooks(query.trim())
        }

    /** Collections available in the library. */
    val collections: StateFlow<List<Collection>> = repository.getAllCollections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Map of collection ID to book count in that collection. */
    val collectionCounts: StateFlow<Map<Long, Int>> = repository.observeCollectionCounts()
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
    val allTags: StateFlow<List<String>> = sharedAllBooks
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
    val tagCounts: StateFlow<Map<String, Int>> = sharedAllBooks
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

    private val coverExtractionSemaphore = Semaphore(MAX_CONCURRENT_COVER_EXTRACTIONS)

    init {
        val loader = coverLoader
        if (loader != null) {
            // Cover loading on IO dispatcher to prevent main thread blocking
            viewModelScope.launch(ioDispatcher) {
                sharedAllBooks.collect { list ->
                    val pending = list.filter { it.coverPath == null && attemptedCoverIds.add(it.stableId) }
                    for (book in pending) {
                        launch {
                            coverExtractionSemaphore.withPermit {
                                val path = try {
                                    loader.loadCoverPath(book)
                                } catch (e: CancellationException) {
                                    throw e
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
            }
        }
    }

    private val attemptedCoverIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val collectionIdsCache = java.util.concurrent.ConcurrentHashMap<String, Flow<Set<Long>>>()

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
                // Hashing only catches the same bytes. The same publication stored
                // as a second file (another edition of the file, a re-download, a
                // Calibre copy) hashes differently and would otherwise become a
                // second row for one book, splitting the reader's progress across
                // two entries. Keep the record the reader already has and point it
                // at the newly imported file instead.
                val samePublication = repository.getAllBooks().first().firstOrNull { candidate ->
                    val id = BookIdentity.publicationOf(candidate.stableId)
                    id != null && id == BookIdentity.publicationOf(book.stableId)
                }
                if (samePublication != null) {
                    repository.updateBook(
                        samePublication.copy(
                            filePath = book.filePath,
                            fileHash = book.fileHash,
                        )
                    )
                } else {
                    repository.addBook(book)
                }
            }
        }
    }

    fun deleteBook(stableId: String) {
        deleteBooks(setOf(stableId))
    }

    fun deleteBooks(stableIds: Set<String>) {
        if (stableIds.isEmpty()) return
        viewModelScope.launch {
            val removedBooks = stableIds.mapNotNull { id -> repository.getBookByStableId(id) }
            stableIds.forEach { id ->
                repository.deleteBook(id)
                attemptedCoverIds.remove(id)
                collectionIdsCache.remove(id)
            }
            if (removedBooks.isNotEmpty()) {
                val remaining = repository.getAllBooks().first()
                onBooksDeleted(removedBooks, remaining)
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

    fun clearAllFilters() {
        _query.value = ""
        _selectedCollection.value = null
        _readingStatusFilter.value = null
        _progressFilter.value = ProgressFilter.ALL
        _selectedTag.value = null
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

    fun updateBookMetadata(
        stableId: String,
        title: String,
        authors: List<String>,
        series: String?,
        seriesIndex: Float?,
        publisher: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            val book = repository.getBookByStableId(stableId) ?: return@launch
            val updated = book.copy(
                title = title.trim(),
                authors = authors.map { it.trim() }.filter { it.isNotEmpty() },
                series = series?.trim()?.takeIf { it.isNotEmpty() },
                seriesIndex = seriesIndex,
                publisher = publisher?.trim()?.takeIf { it.isNotEmpty() } ?: book.publisher,
                description = description?.trim()?.takeIf { it.isNotEmpty() } ?: book.description
            )
            repository.updateBook(updated)
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
        if (days <= 0) return emptyList()
        val startDate = LocalDate.now().minusDays((days - 1).toLong())
        return repo.getDailyAggregates(startDate = startDate, days = days)
    }

    fun updateReadingGoals(dailyMinutes: Int, booksTarget: Int) {
        val newGoals = ReadingGoals(
            dailyMinutes = dailyMinutes.coerceAtLeast(1),
            booksTarget = booksTarget.coerceAtLeast(1)
        )
        _readingGoals.value = newGoals
        readingGoalStore?.saveGoals(newGoals)
    }

    fun reloadPreferences() {
        sortStore?.let { store ->
            val loadedSort = store.load()
            _sort.value = loadedSort
            _sortAscending.value = store.loadAscending(loadedSort.defaultAscending)
        }
        layoutStore?.let { _layout.value = it.load() }
        readingGoalStore?.let { _readingGoals.value = it.loadGoals() }
    }

    private companion object {
        const val MAX_CONCURRENT_COVER_EXTRACTIONS = 3
    }
}
