package com.jdluu.leafline.reader.font

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

class FontParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `extractFontName returns null for non-existent or tiny files`() {
        val nonExistent = File(tempFolder.root, "nope.ttf")
        assertNull(FontParser.extractFontName(nonExistent))

        val tiny = tempFolder.newFile("tiny.ttf")
        tiny.writeBytes(byteArrayOf(1, 2, 3))
        assertNull(FontParser.extractFontName(tiny))
    }

    @Test
    fun `extractFontName returns null for invalid sfnt header`() {
        val invalid = tempFolder.newFile("invalid.ttf")
        invalid.writeBytes(ByteArray(64) { 0 })
        assertNull(FontParser.extractFontName(invalid))
    }

    @Test
    fun `extractFontName parses Windows UTF-16BE font family name`() {
        val fontFile = tempFolder.newFile("custom.ttf")
        val fontName = "Literata Custom"
        val fontBytes = buildMinimalTtf(fontName, platformId = 3, nameId = 1)
        fontFile.writeBytes(fontBytes)

        val extracted = FontParser.extractFontName(fontFile)
        assertEquals(fontName, extracted)
    }

    @Test
    fun `extractFontName parses Macintosh ISO-8859-1 font family name`() {
        val fontFile = tempFolder.newFile("custom_mac.otf")
        val fontName = "Atkinson Custom"
        val fontBytes = buildMinimalTtf(fontName, platformId = 1, nameId = 1)
        fontFile.writeBytes(fontBytes)

        val extracted = FontParser.extractFontName(fontFile)
        assertEquals(fontName, extracted)
    }

    @Test
    fun `extractFontName prioritizes typographic family over family name`() {
        val fontFile = tempFolder.newFile("multi_name.ttf")
        val regularName = "MyFont Regular"
        val typoName = "MyFont Typo"

        val stringBytesRegular = regularName.toByteArray(StandardCharsets.UTF_16BE)
        val stringBytesTypo = typoName.toByteArray(StandardCharsets.UTF_16BE)

        val stringStorage = stringBytesRegular + stringBytesTypo

        val baos = ByteArrayOutputStream()
        val out = DataOutputStream(baos)

        // sfnt header
        out.writeInt(0x00010000)
        out.writeShort(1) // 1 table
        out.writeShort(16)
        out.writeShort(0)
        out.writeShort(0)

        // table record
        val nameTableOffset = 12 + 16
        val nameTableLength = 6 + (2 * 12) + stringStorage.size
        out.writeInt(0x6E616D65) // 'name'
        out.writeInt(0) // checksum
        out.writeInt(nameTableOffset)
        out.writeInt(nameTableLength)

        // name table header
        out.writeShort(0) // format
        out.writeShort(2) // count
        out.writeShort(6 + 24) // stringOffset

        // Record 1: Family Name (nameId = 1)
        out.writeShort(3) // platformId = Windows
        out.writeShort(1)
        out.writeShort(0x0409)
        out.writeShort(1) // nameId
        out.writeShort(stringBytesRegular.size)
        out.writeShort(0) // string offset

        // Record 2: Typographic Family (nameId = 16)
        out.writeShort(3) // platformId = Windows
        out.writeShort(1)
        out.writeShort(0x0409)
        out.writeShort(16) // nameId
        out.writeShort(stringBytesTypo.size)
        out.writeShort(stringBytesRegular.size) // string offset

        // string storage
        out.write(stringStorage)
        out.flush()

        fontFile.writeBytes(baos.toByteArray())

        val extracted = FontParser.extractFontName(fontFile)
        assertEquals(typoName, extracted)
    }

    private fun buildMinimalTtf(fontName: String, platformId: Int, nameId: Int): ByteArray {
        val stringBytes = if (platformId == 1) {
            fontName.toByteArray(StandardCharsets.ISO_8859_1)
        } else {
            fontName.toByteArray(StandardCharsets.UTF_16BE)
        }

        val baos = ByteArrayOutputStream()
        val out = DataOutputStream(baos)

        // sfnt header
        out.writeInt(0x00010000)
        out.writeShort(1) // 1 table
        out.writeShort(16)
        out.writeShort(0)
        out.writeShort(0)

        // table record
        val nameTableOffset = 12 + 16
        val nameTableLength = 6 + 12 + stringBytes.size
        out.writeInt(0x6E616D65) // 'name'
        out.writeInt(0) // checksum
        out.writeInt(nameTableOffset)
        out.writeInt(nameTableLength)

        // name table header
        out.writeShort(0) // format
        out.writeShort(1) // count
        out.writeShort(6 + 12) // stringOffset

        // name record
        out.writeShort(platformId)
        out.writeShort(1)
        out.writeShort(0x0409)
        out.writeShort(nameId)
        out.writeShort(stringBytes.size)
        out.writeShort(0) // string offset

        // string storage
        out.write(stringBytes)
        out.flush()

        return baos.toByteArray()
    }
}
