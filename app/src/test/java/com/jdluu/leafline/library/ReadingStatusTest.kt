package com.jdluu.leafline.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingStatusTest {

    @Test
    fun `entries are UNREAD, READING, FINISHED`() {
        assertEquals(3, ReadingStatus.entries.size)
        assertEquals(ReadingStatus.UNREAD, ReadingStatus.entries[0])
        assertEquals(ReadingStatus.READING, ReadingStatus.entries[1])
        assertEquals(ReadingStatus.FINISHED, ReadingStatus.entries[2])
    }

    @Test
    fun `db values round-trip correctly`() {
        assertEquals("unread", ReadingStatus.UNREAD.dbValue)
        assertEquals("reading", ReadingStatus.READING.dbValue)
        assertEquals("finished", ReadingStatus.FINISHED.dbValue)
    }

    @Test
    fun `fromDb parses valid values`() {
        assertEquals(ReadingStatus.UNREAD, ReadingStatus.fromDb("unread"))
        assertEquals(ReadingStatus.READING, ReadingStatus.fromDb("reading"))
        assertEquals(ReadingStatus.FINISHED, ReadingStatus.fromDb("finished"))
    }

    @Test
    fun `fromDb defaults to UNREAD for unknown values`() {
        assertEquals(ReadingStatus.UNREAD, ReadingStatus.fromDb(null))
        assertEquals(ReadingStatus.UNREAD, ReadingStatus.fromDb("unknown"))
        assertEquals(ReadingStatus.UNREAD, ReadingStatus.fromDb(""))
    }
}