package com.jdluu.leafline.hardcover

import com.jdluu.leafline.library.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HardcoverProgressTest {

    @Test
    fun `reading statuses map to hardcover status ids`() {
        assertEquals(1, HardcoverStatus.idFor(ReadingStatus.UNREAD))
        assertEquals(2, HardcoverStatus.idFor(ReadingStatus.READING))
        assertEquals(3, HardcoverStatus.idFor(ReadingStatus.FINISHED))
    }

    @Test
    fun `progression converts to pages of the edition`() {
        assertEquals(142, HardcoverProgress.pages(progression = 0.5, editionPages = 284))
    }

    @Test
    fun `progress never reports more pages than the edition has`() {
        assertEquals(284, HardcoverProgress.pages(progression = 1.0, editionPages = 284))
        assertEquals(284, HardcoverProgress.pages(progression = 1.7, editionPages = 284))
    }

    @Test
    fun `progress is at least one page once reading has started`() {
        assertEquals(1, HardcoverProgress.pages(progression = 0.001, editionPages = 284))
    }

    @Test
    fun `no edition page count means no page progress`() {
        assertNull(HardcoverProgress.pages(progression = 0.5, editionPages = null))
        assertNull(HardcoverProgress.pages(progression = null, editionPages = 284))
        assertNull(HardcoverProgress.pages(progression = 0.5, editionPages = 0))
    }
}
