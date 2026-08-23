package com.jdluu.leafline.library.cover

import java.io.File

/**
 * Stores cover images as files under an app-private directory, keyed by book id.
 *
 * The stored bytes are the encoded image bytes produced by the extractor
 * (JPEG). Files use a fixed ".cover" suffix; decoding happens at display time.
 */
class CoverCache(private val coversDir: File) {

    init {
        if (!coversDir.exists()) coversDir.mkdirs()
    }

    fun coverFileFor(bookId: String): File {
        return File(coversDir, "${sanitizeBookId(bookId)}.cover")
    }

    fun existingCoverPath(bookId: String): String? {
        val file = coverFileFor(bookId)
        return if (file.isFile && file.length() > 0) file.absolutePath else null
    }

    fun storeCoverBytes(bookId: String, imageBytes: ByteArray): String? {
        if (imageBytes.isEmpty()) return null
        return try {
            val target = coverFileFor(bookId)
            val temp = File(coversDir, "${target.name}.tmp")
            temp.writeBytes(imageBytes)
            if (!temp.renameTo(target)) {
                target.writeBytes(imageBytes)
                temp.delete()
            }
            target.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun deleteCover(bookId: String) {
        try {
            coverFileFor(bookId).delete()
        } catch (e: Exception) {
            // Best effort cleanup only.
        }
    }

    companion object {
        fun coversDirectory(filesDir: File): File = File(filesDir, "covers")

        fun sanitizeBookId(bookId: String): String {
            val sanitized = bookId
                .map { c -> if (c.isLetterOrDigit() || c == '.' || c == '-' || c == '_') c else '_' }
                .joinToString("")
                .take(MAX_FILE_NAME_LENGTH)
            if (sanitized.none { it.isLetterOrDigit() }) return FALLBACK_NAME
            return sanitized
        }

        private const val MAX_FILE_NAME_LENGTH = 120
        private const val FALLBACK_NAME = "book"
    }
}
