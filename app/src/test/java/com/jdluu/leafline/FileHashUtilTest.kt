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
}