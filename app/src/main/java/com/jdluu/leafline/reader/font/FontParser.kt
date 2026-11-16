package com.jdluu.leafline.reader.font

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

/**
 * Extracts typography metadata (family name) from OpenType (.otf) and TrueType (.ttf) files.
 */
object FontParser {

    private const val TAG_NAME = 0x6E616D65 // 'name' in ASCII hex

    private const val NAME_ID_FAMILY = 1
    private const val NAME_ID_FULL = 4
    private const val NAME_ID_TYPOGRAPHIC_FAMILY = 16

    private const val PLATFORM_UNICODE = 0
    private const val PLATFORM_MAC = 1
    private const val PLATFORM_WINDOWS = 3

    /**
     * Extracts the primary font family name from a font file.
     * Returns null if the file cannot be parsed as a valid TTF/OTF font.
     */
    fun extractFontName(file: File): String? {
        if (!file.exists() || file.length() < 12) return null

        try {
            RandomAccessFile(file, "r").use { raf ->
                // Check header
                val sfntVersion = raf.readInt()
                val isValidSfnt = sfntVersion == 0x00010000 ||
                    sfntVersion == 0x4F54544F || // 'OTTO'
                    sfntVersion == 0x74727565 // 'true'
                if (!isValidSfnt) return null

                val numTables = raf.readUnsignedShort()
                raf.skipBytes(6) // searchRange, entrySelector, rangeShift

                var nameTableOffset: Long? = null
                var nameTableLength: Long? = null

                for (i in 0 until numTables) {
                    val tag = raf.readInt()
                    val checkSum = raf.readInt()
                    val offset = raf.readInt().toLong() and 0xFFFFFFFFL
                    val length = raf.readInt().toLong() and 0xFFFFFFFFL

                    if (tag == TAG_NAME) {
                        nameTableOffset = offset
                        nameTableLength = length
                        break
                    }
                }

                if (nameTableOffset == null || nameTableLength == null || nameTableLength < 6) {
                    return null
                }

                raf.seek(nameTableOffset)
                val format = raf.readUnsignedShort()
                val count = raf.readUnsignedShort()
                val stringOffset = raf.readUnsignedShort()

                val stringStorageBase = nameTableOffset + stringOffset

                val candidates = mutableMapOf<Int, String>()

                for (i in 0 until count) {
                    val platformId = raf.readUnsignedShort()
                    val encodingId = raf.readUnsignedShort()
                    val languageId = raf.readUnsignedShort()
                    val nameId = raf.readUnsignedShort()
                    val length = raf.readUnsignedShort()
                    val offset = raf.readUnsignedShort()

                    if (nameId == NAME_ID_TYPOGRAPHIC_FAMILY || nameId == NAME_ID_FAMILY || nameId == NAME_ID_FULL) {
                        val currentPos = raf.filePointer
                        raf.seek(stringStorageBase + offset)

                        val bytes = ByteArray(length)
                        val bytesRead = raf.read(bytes)
                        raf.seek(currentPos)

                        if (bytesRead == length) {
                            val str = when (platformId) {
                                PLATFORM_WINDOWS, PLATFORM_UNICODE -> {
                                    String(bytes, StandardCharsets.UTF_16BE).trim()
                                }
                                PLATFORM_MAC -> {
                                    String(bytes, StandardCharsets.ISO_8859_1).trim()
                                }
                                else -> null
                            }

                            if (!str.isNullOrBlank()) {
                                // Prefer Windows/Unicode over Mac, and Typographic > Family > Full
                                val priority = when (nameId) {
                                    NAME_ID_TYPOGRAPHIC_FAMILY -> 300
                                    NAME_ID_FAMILY -> 200
                                    NAME_ID_FULL -> 100
                                    else -> 0
                                } + if (platformId == PLATFORM_WINDOWS || platformId == PLATFORM_UNICODE) 50 else 0

                                val currentPriority = candidates.keys.firstOrNull { candidates[it] == str }
                                if (currentPriority == null || priority > currentPriority) {
                                    candidates[priority] = str
                                }
                            }
                        }
                    }
                }

                return candidates.maxByOrNull { it.key }?.value
            }
        } catch (_: Exception) {
            return null
        }
    }
}
