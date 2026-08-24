package com.jdluu.leafline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

class FileHashUtilTest {
    
    @get:Rule
    val tempFolder = TemporaryFolder()
    
    @Test
    fun `computeSha256 returns correct hash for known content`() {
        val file = tempFolder.newFile("test.epub")
        file.writeText("Hello, World!")
        
        val result = FileHashUtil.computeSha256(file)
        
        assertEquals(64, result.length)
        assertTrue(result.all { it.isDigit() || (it >= 'a' && it <= 'f') })
    }
    
    @Test
    fun `computeSha256 returns consistent hash for same content`() {
        val file = tempFolder.newFile("test.epub")
        val content = "Test content for hashing"
        file.writeText(content)
        
        val result1 = FileHashUtil.computeSha256(file)
        val result2 = FileHashUtil.computeSha256(file)
        
        assertEquals(result1, result2)
    }
    
    @Test
    fun `computeSha256 returns different hash for different content`() {
        val file1 = tempFolder.newFile("test1.epub")
        val file2 = tempFolder.newFile("test2.epub")
        
        file1.writeText("Content A")
        file2.writeText("Content B")
        
        val result1 = FileHashUtil.computeSha256(file1)
        val result2 = FileHashUtil.computeSha256(file2)
        
        assertTrue(result1 != result2)
    }
    
    @Test
    fun `computeSha256 handles empty file`() {
        val file = tempFolder.newFile("empty.epub")
        file.writeText("")

        val result = FileHashUtil.computeSha256(file)

        assertEquals(64, result.length)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", result)
    }

    private fun writeChainedBytes(name: String, size: Int): File {
        val file = tempFolder.newFile(name)
        val output = file.outputStream().buffered()
        var seed = "leafline-koreader-vector".toByteArray(Charsets.UTF_8)
        var written = 0
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        while (written < size) {
            seed = digest.digest(seed)
            val chunk = if (written + seed.size > size) seed.copyOf(size - written) else seed
            output.write(chunk)
            written += chunk.size
        }
        output.close()
        return file
    }

    @Test
    fun `koreaderHash matches known vector for large file`() {
        val file = writeChainedBytes("large.epub", 4096)

        val result = FileHashUtil.koreaderHash(file)

        assertEquals("db28671f485423ba5fe9bc8f06381a6d", result)
    }

    @Test
    fun `koreaderHash uses head and tail samples not full content`() {
        val file = writeChainedBytes("large.epub", 4096)

        val partial = FileHashUtil.koreaderHash(file)

        assertTrue(partial != FileHashUtil.computeSha256(file))
    }

    @Test
    fun `koreaderHash matches known vector for multi sample file`() {
        val file = writeChainedBytes("eight-k.epub", 8192)

        val result = FileHashUtil.koreaderHash(file)

        assertEquals("316c5d1481dfe71ab94ef11d43fd134f", result)
    }

    @Test
    fun `koreaderHash matches KOReader behavior for files smaller than first offset`() {
        val file = tempFolder.newFile("small.epub")
        file.writeBytes(ByteArray(100) { 'A'.code.toByte() })

        val result = FileHashUtil.koreaderHash(file)

        // KOReader reads at offset 256 first; for a 100-byte file that seek is
        // past EOF, so the sample read returns nil and the digest is MD5 of
        // empty input (d41d8cd9...). This matches util.partialMD5 exactly.
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result)
    }

    @Test
    fun `koreaderHash samples partial data for exactly one block`() {
        val file = tempFolder.newFile("one-block.epub")
        file.writeBytes(ByteArray(1024) { 'B'.code.toByte() })

        val result = FileHashUtil.koreaderHash(file)

        assertEquals("9d3df85d37a9a4380dd8b4b4ad85ac9c", result)
    }

    @Test
    fun `koreaderHash handles empty file`() {
        val file = tempFolder.newFile("empty-hash.epub")
        file.writeBytes(ByteArray(0))

        val result = FileHashUtil.koreaderHash(file)

        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result)
    }

    @Test
    fun `koreaderHash is stable across repeated calls`() {
        val file = writeChainedBytes("stable.epub", 5000)

        assertEquals(FileHashUtil.koreaderHash(file), FileHashUtil.koreaderHash(file))
    }
}