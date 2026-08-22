package com.jdluu.leafline

import java.io.File
import java.security.MessageDigest

object FileHashUtil {
    private const val SHA256_ALGORITHM = "SHA-256"
    
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
}