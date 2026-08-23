package com.jdluu.leafline.reader.search

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.search.SearchError
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search

/**
 * Runs searches over the currently open publication with the Readium search
 * service.
 *
 * Searches execute on [backgroundDispatcher] and stream result pages into a
 * single [BookSearchState]. Starting a new search cancels and closes the
 * previous one, so the UI can debounce input freely. Emissions from a stale
 * run never overwrite the state of a newer run.
 */
@OptIn(ExperimentalReadiumApi::class)
class BookSearcher(
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val searchFactory: suspend (query: String) -> SearchIterator?,
    private val mapLocator: (id: Int, locator: Locator) -> BookSearchResult? =
        ::defaultMapLocator,
) {

    @Volatile
    private var job: Job? = null

    @Volatile
    private var locatorsById: Map<Int, Locator> = emptyMap()

    private val mutex = Mutex()

    private val _state = MutableStateFlow(BookSearchState())
    val state: StateFlow<BookSearchState> = _state.asStateFlow()

    /** Locator backing a result id, for navigation and highlighting. */
    fun locatorFor(resultId: Int): Locator? = locatorsById[resultId]

    /**
     * Starts a new search for [rawQuery], replacing any running search.
     * Blank queries reset the search instead.
     */
    fun start(rawQuery: String) {
        scope.launch {
            mutex.withLock {
                stopCurrentSearch()

                val query = BookSearchQuery.normalize(rawQuery)
                if (query == null) {
                    clearState()
                    return@withLock
                }

                job = scope.launch {
                    runSearch(query)
                }
            }
        }
    }

    /** Cancels any running search and clears results. */
    fun reset() {
        scope.launch {
            mutex.withLock {
                stopCurrentSearch()
                clearState()
            }
        }
    }

    private suspend fun runSearch(query: String) = withContext(backgroundDispatcher) {
        if (!scope.isActive) return@withContext
        val runLocators = mutableMapOf<Int, Locator>()
        val results = mutableListOf<BookSearchResult>()

        publish(
            BookSearchState(
                query = query,
                status = BookSearchStatus.Searching,
                results = emptyList()
            )
        )

        val iterator = try {
            searchFactory(query)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            publishFailed(query, START_FAILED_MESSAGE)
            return@withContext
        }

        if (iterator == null) {
            publishFailed(query, UNSEARCHABLE_MESSAGE)
            return@withContext
        }

        try {
            drainResults(query, iterator, runLocators, results)
        } finally {
            iterator.close()
        }
    }

    private suspend fun drainResults(
        query: String,
        iterator: SearchIterator,
        runLocators: MutableMap<Int, Locator>,
        results: MutableList<BookSearchResult>,
    ) {
        var nextId = 0

        while (scope.isActive) {
            val page = try {
                iterator.next()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publishFailed(query, ITERATION_FAILED_MESSAGE)
                return
            }

            val failure = page.failureOrNull()
            if (failure != null) {
                publishFailed(query, messageFor(failure))
                return
            }

            val collection = page.getOrNull()
            if (collection == null) break

            synchronized(runLocators) {
                for (locator in collection.locators) {
                    val mapped = mapLocator(nextId, locator)
                    if (mapped != null) {
                        runLocators[nextId] = locator
                        results.add(mapped)
                        nextId++
                    }
                }
            }

            locatorsById = runLocators.toMap()

            publish(
                BookSearchState(
                    query = query,
                    status = BookSearchStatus.Searching,
                    results = results.toList()
                )
            )
        }

        publish(
            BookSearchState(
                query = query,
                status = BookSearchStatus.Completed(results.size),
                results = results.toList()
            )
        )
    }

    /**
     * Applies [update] to the published state, but only while this coroutine
     * is still the active search. Stale runs are dropped silently.
     */
    private fun publish(update: BookSearchState) {
        val currentJob = job ?: return
        if (!currentJob.isActive) return
        _state.value = update
    }

    private fun publishFailed(query: String, message: String) {
        publish(BookSearchState(query = query, status = BookSearchStatus.Failed(message)))
    }

    private fun stopCurrentSearch() {
        job?.cancel()
        job = null
    }

    private fun clearState() {
        locatorsById = emptyMap()
        _state.value = BookSearchState(status = BookSearchStatus.Idle)
    }

    companion object {
        const val UNSEARCHABLE_MESSAGE = "This book cannot be searched."
        const val START_FAILED_MESSAGE = "Could not start the search."
        const val ITERATION_FAILED_MESSAGE = "The search failed unexpectedly."

        fun messageFor(error: SearchError): String = when (error) {
            is SearchError.Engine -> "The search engine failed."
            is SearchError.Reading -> "Part of this book could not be read while searching."
        }

        private fun defaultMapLocator(id: Int, locator: Locator): BookSearchResult? {
            val json = try {
                locator.toJSON().toString()
            } catch (e: Exception) {
                null
            }
            val text = locator.text
            return BookSearchResultMapper.map(
                id = id,
                href = locator.href.toString(),
                title = locator.title,
                before = text?.before,
                match = text?.highlight,
                after = text?.after,
                locatorJson = json ?: return null
            )
        }
    }
}
