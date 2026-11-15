package com.jdluu.leafline.reader.search

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.util.Try
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookSearcherTest {

    private class FakeSearchIterator(
        private val pages: List<List<Locator>>
    ) : SearchIterator {
        private var index = 0
        var closed = false

        override val resultCount: Int? = pages.sumOf { it.size }

        override suspend fun next(): Try<LocatorCollection?, org.readium.r2.shared.publication.services.search.SearchError> {
            if (index >= pages.size) return Try.success(null)
            val page = pages[index++]
            return Try.success(LocatorCollection(locators = page))
        }

        override fun close() {
            closed = true
        }
    }

    private fun makeLocator(href: String, text: String, title: String? = null): Locator {
        return Locator(
            href = org.readium.r2.shared.util.Url(href)!!,
            mediaType = org.readium.r2.shared.util.mediatype.MediaType.XHTML,
            title = title,
            text = Locator.Text(highlight = text, before = "", after = "")
        )
    }

    @Test
    fun `search completes with results`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val loc1 = makeLocator("chapter1.xhtml", "Match one")
        val loc2 = makeLocator("chapter2.xhtml", "Match two")
        val iterator = FakeSearchIterator(listOf(listOf(loc1, loc2)))

        var capturedOptions: SearchOptions? = null
        val searcher = BookSearcher(
            scope = this,
            backgroundDispatcher = testDispatcher,
            searchFactory = { _, opts ->
                capturedOptions = opts
                iterator
            }
        )

        searcher.start("Match")
        advanceUntilIdle()

        val state = searcher.state.value
        assertEquals("Match", state.query)
        assertTrue(state.status is BookSearchStatus.Completed)
        assertEquals(2, (state.status as BookSearchStatus.Completed).resultCount)
        assertEquals(2, state.results.size)
        assertTrue(iterator.closed)
    }

    @Test
    fun `search passes custom search options to factory`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var capturedOptions: SearchOptions? = null

        val searcher = BookSearcher(
            scope = this,
            backgroundDispatcher = testDispatcher,
            searchFactory = { _, opts ->
                capturedOptions = opts
                FakeSearchIterator(emptyList())
            }
        )

        val options = SearchOptions(caseSensitive = true, wholeWord = true, regex = false)
        searcher.start("Test", options)
        advanceUntilIdle()

        assertEquals(options, capturedOptions)
    }

    @Test
    fun `search with chapterHref filters results to target chapter`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val loc1 = makeLocator("OEBPS/chapter1.xhtml", "Match in ch1")
        val loc2 = makeLocator("OEBPS/chapter2.xhtml", "Match in ch2")
        val iterator = FakeSearchIterator(listOf(listOf(loc1, loc2)))

        val searcher = BookSearcher(
            scope = this,
            backgroundDispatcher = testDispatcher,
            searchFactory = { _, _ -> iterator }
        )

        val options = SearchOptions(chapterHref = "OEBPS/chapter1.xhtml")
        searcher.start("Match", options)
        advanceUntilIdle()

        val state = searcher.state.value
        assertEquals(1, state.results.size)
        assertEquals("Match in ch1", state.results[0].excerptMatch)
    }

    @Test
    fun `invalid regex emits failed status without calling searchFactory`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var factoryCalled = false

        val searcher = BookSearcher(
            scope = this,
            backgroundDispatcher = testDispatcher,
            searchFactory = { _, _ ->
                factoryCalled = true
                FakeSearchIterator(emptyList())
            }
        )

        searcher.start("[unclosed-regex", SearchOptions(regex = true))
        advanceUntilIdle()

        val state = searcher.state.value
        assertTrue(state.status is BookSearchStatus.Failed)
        assertTrue((state.status as BookSearchStatus.Failed).message.contains("Invalid regular expression"))
        assertEquals(false, factoryCalled)
    }

    @Test
    fun `reset clears search state and locators`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val loc = makeLocator("ch1.xhtml", "Found")
        val searcher = BookSearcher(
            scope = this,
            backgroundDispatcher = testDispatcher,
            searchFactory = { _, _ -> FakeSearchIterator(listOf(listOf(loc))) }
        )

        searcher.start("Found")
        advanceUntilIdle()
        assertEquals(1, searcher.state.value.results.size)

        searcher.reset()
        advanceUntilIdle()

        assertEquals(BookSearchStatus.Idle, searcher.state.value.status)
        assertTrue(searcher.state.value.results.isEmpty())
    }
}
