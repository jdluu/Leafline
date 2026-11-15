package com.jdluu.leafline.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressFilterTest {

    private fun book(
        id: String,
        progression: Double? = null,
        readingStatus: ReadingStatus = ReadingStatus.UNREAD
    ): LibraryBook {
        val locatorJson = progression?.let {
            """{"locations":{"progression":$it},"href":"chapter1.html","type":"application/xhtml+xml"}"""
        }
        return LibraryBook(
            stableId = id,
            title = "Book $id",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = "/books/$id.epub",
            fileHash = "hash-$id",
            addedAtEpochMillis = 100L,
            pageCount = null,
            lastLocatorJson = locatorJson,
            readingStatus = readingStatus
        )
    }

    @Test
    fun `ALL matches every book`() {
        val unread = book("1", null, ReadingStatus.UNREAD)
        val reading = book("2", 0.5, ReadingStatus.READING)
        val finished = book("3", 1.0, ReadingStatus.FINISHED)

        assertTrue(ProgressFilter.ALL.matches(unread))
        assertTrue(ProgressFilter.ALL.matches(reading))
        assertTrue(ProgressFilter.ALL.matches(finished))
    }

    @Test
    fun `UNREAD matches books without progress or status unread`() {
        val unread = book("1", null, ReadingStatus.UNREAD)
        val zeroProgress = book("2", 0.0, ReadingStatus.UNREAD)
        val inProgress = book("3", 0.25, ReadingStatus.READING)
        val finished = book("4", 1.0, ReadingStatus.FINISHED)

        assertTrue(ProgressFilter.UNREAD.matches(unread))
        assertTrue(ProgressFilter.UNREAD.matches(zeroProgress))
        assertFalse(ProgressFilter.UNREAD.matches(inProgress))
        assertFalse(ProgressFilter.UNREAD.matches(finished))
    }

    @Test
    fun `IN_PROGRESS matches books between 1 and 99 percent or reading status`() {
        val unread = book("1", null, ReadingStatus.UNREAD)
        val early = book("2", 0.15, ReadingStatus.READING)
        val mid = book("3", 0.50, ReadingStatus.READING)
        val finished = book("4", 1.0, ReadingStatus.FINISHED)

        assertFalse(ProgressFilter.IN_PROGRESS.matches(unread))
        assertTrue(ProgressFilter.IN_PROGRESS.matches(early))
        assertTrue(ProgressFilter.IN_PROGRESS.matches(mid))
        assertFalse(ProgressFilter.IN_PROGRESS.matches(finished))
    }

    @Test
    fun `NEAR_FINISH matches books with at least 75 percent progress`() {
        val early = book("1", 0.50, ReadingStatus.READING)
        val almostDone = book("2", 0.85, ReadingStatus.READING)
        val finished = book("3", 1.0, ReadingStatus.FINISHED)

        assertFalse(ProgressFilter.NEAR_FINISH.matches(early))
        assertTrue(ProgressFilter.NEAR_FINISH.matches(almostDone))
        assertFalse(ProgressFilter.NEAR_FINISH.matches(finished))
    }

    @Test
    fun `FINISHED matches books marked finished or with 100 percent progress`() {
        val mid = book("1", 0.50, ReadingStatus.READING)
        val doneByStatus = book("2", 0.50, ReadingStatus.FINISHED)
        val doneByProgress = book("3", 1.0, ReadingStatus.READING)

        assertFalse(ProgressFilter.FINISHED.matches(mid))
        assertTrue(ProgressFilter.FINISHED.matches(doneByStatus))
        assertTrue(ProgressFilter.FINISHED.matches(doneByProgress))
    }
}
