package com.jdluu.leafline.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryBookSeriesTest {

    private fun book(
        id: String,
        title: String = "Book $id",
        series: String? = null,
        seriesIndex: Float? = null,
        status: ReadingStatus = ReadingStatus.UNREAD
    ) = LibraryBook(
        stableId = id,
        title = title,
        series = series,
        seriesIndex = seriesIndex,
        readingStatus = status
    )

    @Test
    fun `seriesPeers matches trimmed case-insensitively and sorts nulls last`() {
        val b3 = book("3", title = "Cinder", series = "Lunar", seriesIndex = 3f)
        val b1 = book("1", title = "Alpha", series = " lunar ", seriesIndex = 1f)
        val bUnnumbered = book("u", title = "Bonus Story", series = "LUNAR", seriesIndex = null)
        val b2 = book("2", title = "Beta", series = "Lunar", seriesIndex = 2f)
        val other = book("x", title = "Unrelated", series = "Other", seriesIndex = 1f)

        val peers = b1.seriesPeers(listOf(b3, other, bUnnumbered, b2, b1))

        assertEquals(listOf("1", "2", "3", "u"), peers.map { it.stableId })
    }

    @Test
    fun `seriesPeers returns empty for blank or null series`() {
        val noSeries = book("1", series = null)
        val blankSeries = book("2", series = "   ")
        val peers = listOf(noSeries, blankSeries, book("3", series = "Dune", seriesIndex = 1f))

        assertEquals(emptyList<LibraryBook>(), noSeries.seriesPeers(peers))
        assertEquals(emptyList<LibraryBook>(), blankSeries.seriesPeers(peers))
    }

    @Test
    fun `nextInSeries returns immediate next numbered volume including decimals`() {
        val v1 = book("1", series = "Mistborn", seriesIndex = 1f)
        val v1_5 = book("1.5", series = "Mistborn", seriesIndex = 1.5f)
        val v2 = book("2", series = "Mistborn", seriesIndex = 2f)

        val all = listOf(v2, v1, v1_5)

        assertEquals("1.5", v1.nextInSeries(all)?.stableId)
        assertEquals("2", v1_5.nextInSeries(all)?.stableId)
        assertNull(v2.nextInSeries(all))
    }

    @Test
    fun `nextInSeries prefers non-finished duplicate volume before finished tie`() {
        val v1 = book("1", series = "Earthsea", seriesIndex = 1f)
        val v2Finished = book("2-fin", title = "A", series = "Earthsea", seriesIndex = 2f, status = ReadingStatus.FINISHED)
        val v2Unread = book("2-unr", title = "B", series = "Earthsea", seriesIndex = 2f, status = ReadingStatus.UNREAD)

        val next = v1.nextInSeries(listOf(v1, v2Finished, v2Unread))

        assertEquals("2-unr", next?.stableId)
    }

    @Test
    fun `nextInSeries falls back to title order when seriesIndex is null`() {
        val a = book("a", title = "A Quiet Dawn", series = "Chronicles", seriesIndex = null)
        val b = book("b", title = "Before Noon", series = "Chronicles", seriesIndex = null)

        val all = listOf(b, a)

        assertEquals("b", a.nextInSeries(all)?.stableId)
        assertNull(b.nextInSeries(all))
    }

    @Test
    fun `nextInSeries returns null for single book or blank series`() {
        val solo = book("1", series = "Standalone Series", seriesIndex = 1f)
        val blank = book("2", series = "  ", seriesIndex = 1f)

        assertNull(solo.nextInSeries(listOf(solo)))
        assertNull(blank.nextInSeries(listOf(blank, book("3", series = "  ", seriesIndex = 2f))))
    }
}
