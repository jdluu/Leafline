package com.jdluu.leafline.opds

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

/** Downloads one OPDS acquisition into an app-private directory. */
class OpdsEpubDownloader(
    private val destinationDirectory: File
) {
    fun download(config: OpdsServerConfig, acquisitionUrl: String): File {
        val uri = URI(acquisitionUrl)
        require(uri.scheme == "http" || uri.scheme == "https") {
            "Acquisition URL must use HTTP or HTTPS"
        }
        destinationDirectory.mkdirs()
        require(destinationDirectory.isDirectory) { "Download directory is unavailable" }

        val fileName = safeFileName(uri.path.substringAfterLast('/'))
        val temporaryFile = File.createTempFile("download-", ".part", destinationDirectory)
        val destination = File(destinationDirectory, fileName)
        val connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Authorization", basicAuth(config))
            setRequestProperty("Accept", "application/epub+zip,application/octet-stream")
        }

        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw DownloadException("OPDS download failed with HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                temporaryFile.outputStream().use { output -> input.copyTo(output) }
            }
            check(temporaryFile.length() > 0) { "OPDS returned an empty EPUB" }
            if (destination.exists()) destination.delete()
            check(temporaryFile.renameTo(destination)) { "Could not finalize EPUB download" }
            return destination
        } catch (error: Exception) {
            temporaryFile.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun basicAuth(config: OpdsServerConfig): String {
        val credentials = "${config.username}:${config.password}"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
        return "Basic $encoded"
    }

    companion object {
        fun safeFileName(candidate: String): String {
            val cleaned = candidate
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .trim('.', '_', '-')
            val withExtension = if (cleaned.endsWith(".epub", ignoreCase = true)) cleaned else "$cleaned.epub"
            return if (withExtension == ".epub" || withExtension.isBlank()) "downloaded.epub" else withExtension
        }
    }
}

class DownloadException(message: String) : Exception(message)
