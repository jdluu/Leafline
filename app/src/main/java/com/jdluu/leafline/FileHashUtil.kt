package com.jdluu.leafline

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

object FileHashUtil {
    private const val SHA256_ALGORITHM = "SHA-256"
    private const val MD5_ALGORITHM = "MD5"
    private const val KOREADER_SAMPLE_BYTES = 1024

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance(SHA256_ALGORITHM)
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * KOReader-compatible partial MD5 used as the document hash by sync
     * servers. Files larger than one sample block hash the concatenation of
     * the first and last 1024 bytes; smaller files (including empty ones)
     * fall back to the full-file MD5.
     */
    fun koreaderHash(file: File): String {
        val digest = MessageDigest.getInstance(MD5_ALGORITHM)
        file.inputStream().use { input ->
            val head = ByteArray(KOREADER_SAMPLE_BYTES)
            var headBytes = 0
            while (headBytes < head.size) {
                val read = input.read(head, headBytes, head.size - headBytes)
                if (read == -1) break
                headBytes += read
            }
            if (headBytes < KOREADER_SAMPLE_BYTES || file.length() <= KOREADER_SAMPLE_BYTES.toLong()) {
                digest.update(head, 0, headBytes)
            } else {
                digest.update(head, 0, headBytes)
                val tail = readTailSample(file)
                digest.update(tail)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun readTailSample(file: File): ByteArray {
        RandomAccessFile(file, "r").use { randomAccess ->
            val start = maxOf(0L, file.length() - KOREADER_SAMPLE_BYTES)
            randomAccess.seek(start)
            val tail = ByteArray(KOREADER_SAMPLE_BYTES)
            var tailBytes = 0
            while (tailBytes < tail.size) {
                val read = randomAccess.read(tail, tailBytes, tail.size - tailBytes)
                if (read == -1) break
                tailBytes += read
            }
            return if (tailBytes == tail.size) tail else tail.copyOf(tailBytes)
        }
    }
}