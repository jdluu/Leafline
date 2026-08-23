package com.jdluu.leafline.library.cover

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CoverCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newCache(): Pair<CoverCache, File> {
        val dir = tempFolder.newFolder("covers")
        return CoverCache(dir) to dir
    }

    @Test
    fun `storeCoverBytes writes file keyed by book id and returns path`() {
        val (cache, dir) = newCache()
        val bytes = byteArrayOf(1, 2, 3, 4)

        val path = cache.storeCoverBytes("book-1", bytes)

        assertNotNull(path)
        val stored = File(path!!)
        assertTrue(stored.isFile)
        assertEquals(dir.canonicalPath, stored.parentFile!!.canonicalPath)
        assertTrue(stored.readBytes().contentEquals(bytes))
    }

    @Test
    fun `existingCoverPath returns path after store and null before`() {
        val cache = newCache().first

        assertNull(cache.existingCoverPath("book-1"))

        cache.storeCoverBytes("book-1", byteArrayOf(9, 9))

        val expectedPath = File(cache.coverFileFor("book-1").parent, "book-1.cover").absolutePath
        assertEquals(expectedPath, cache.existingCoverPath("book-1"))
    }

    @Test
    fun `storeCoverBytes rejects empty payload`() {
        val cache = newCache().first

        val result = cache.storeCoverBytes("book-1", ByteArray(0))

        assertNull(result)
    }

    @Test
    fun `storeCoverBytes overwrites previous content`() {
        val cache = newCache().first
        val first = byteArrayOf(1, 1, 1)
        val second = byteArrayOf(2, 2, 2, 2)

        cache.storeCoverBytes("book-1", first)
        cache.storeCoverBytes("book-1", second)

        val stored = cache.coverFileFor("book-1")
        assertTrue(stored.readBytes().contentEquals(second))
    }

    @Test
    fun `sanitizeBookId removes path separators and unsafe characters`() {
        assertEquals("urn_uuid_123", CoverCache.sanitizeBookId("urn:uuid:123"))
        assertEquals(".._.._evil", CoverCache.sanitizeBookId("../../evil"))
        assertEquals("safe-id_1.file", CoverCache.sanitizeBookId("safe-id_1.file"))
    }

    @Test
    fun `sanitizeBookId handles blank input`() {
        assertEquals("book", CoverCache.sanitizeBookId(""))
        assertEquals("book", CoverCache.sanitizeBookId("///"))
    }

    @Test
    fun `coverFileFor stays inside covers directory for hostile ids`() {
        val (cache, dir) = newCache()

        val file = cache.coverFileFor("../outside")

        assertEquals(dir.canonicalPath, file.parentFile!!.canonicalPath)
    }

    @Test
    fun `deleteCover removes the stored file`() {
        val cache = newCache().first
        cache.storeCoverBytes("book-1", byteArrayOf(7))

        cache.deleteCover("book-1")

        assertNull(cache.existingCoverPath("book-1"))
    }

    @Test
    fun `existingCoverPath returns null when file exists but is empty`() {
        val cache = newCache().first
        cache.coverFileFor("book-1").writeBytes(ByteArray(0))

        assertNull(cache.existingCoverPath("book-1"))
    }
}
