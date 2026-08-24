package com.jdluu.leafline

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

object FileHashUtil {
    private const val SHA256_ALGORITHM = "SHA-256"
    private const val MD5_ALGORITHM = "MD5"

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
     * KOReader-compatible partial MD5 hash, matching KOReader master's
     * `util.partialMD5` exactly.
     *
     * Reads 1024-byte samples at 12 exponentially-spaced offsets
     * (256, 1024, 4096, 16384, 65536, … up to ~1GB) and returns the MD5
     * of the concatenated samples. This is the algorithm KOReader uses
     * as the document key for progress sync and .sdr folder naming.
     *
     * Reference:
     *   https://github.com/koreader/koreader/blob/master/frontend/util.lua
     *   function util.partialMD5(filepath)
     */
    fun koreaderHash(file: File): String {
        val digest = MessageDigest.getInstance(MD5_ALGORITHM)
        val step = 1024
        val size = 1024
        RandomAccessFile(file, "r").use { raf ->
            for (i in -1..10) {
                val offset = if (i == -1) {
                    (step ushr 2).toLong() // 1024 >> 2 = 256
                } else {
                    step.toLong() shl (2 * i)
                }
                if (offset >= file.length()) break
                raf.seek(offset)
                val buffer = ByteArray(size)
                val bytesRead = raf.read(buffer)
                if (bytesRead <= 0) break
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}