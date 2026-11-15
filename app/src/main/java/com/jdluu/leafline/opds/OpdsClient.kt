package com.jdluu.leafline.opds

import com.jdluu.leafline.opds.model.OpdsFeedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * HTTP client for fetching OPDS catalog feeds, searching feeds, and downloading EPUB publications.
 */
open class OpdsClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    /**
     * Fetches and parses an OPDS catalog feed from the given [url].
     */
    open suspend fun fetchFeed(url: String): Result<OpdsFeedResult> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Leafline-Reader/0.3.0 (Android; OPDS Client)")
                .header("Accept", "application/atom+xml, application/opds+json, application/json, text/xml, */*")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}: ${response.message}")
                }
                val body = response.body ?: throw IllegalStateException("Empty response body from $url")
                val bytes = body.bytes()
                val contentType = response.header("Content-Type")?.lowercase().orEmpty()
                val isJson = contentType.contains("json")

                OpdsParser.parse(
                    data = bytes,
                    feedUrlString = response.request.url.toString(),
                    isJson = isJson
                )
            }
        }
    }

    /**
     * Searches a catalog feed using its OpenSearch URL template.
     * Replaces `{searchTerms}` with URL-encoded query.
     */
    open suspend fun searchFeed(searchTemplateUrl: String, query: String): Result<OpdsFeedResult> {
        val encodedQuery = withContext(Dispatchers.IO) {
            URLEncoder.encode(query, "UTF-8")
        }
        val targetUrl = if (searchTemplateUrl.contains("{searchTerms}")) {
            searchTemplateUrl.replace("{searchTerms}", encodedQuery)
        } else if (searchTemplateUrl.contains("?")) {
            "$searchTemplateUrl&q=$encodedQuery"
        } else {
            "$searchTemplateUrl?q=$encodedQuery"
        }
        return fetchFeed(targetUrl)
    }

    /**
     * Downloads an EPUB file from [downloadUrl] directly to [destinationFile],
     * invoking [onProgress] with values from 0.0 to 1.0.
     */
    open suspend fun downloadEpubFile(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Leafline-Reader/0.3.0 (Android; OPDS Client)")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code} downloading book: ${response.message}")
                }

                val body = response.body ?: throw IllegalStateException("Download body was empty")
                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()

                val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.downloading")
                tempFile.parentFile?.mkdirs()

                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesCopied: Long = 0
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            val fraction = (bytesCopied.toFloat() / totalBytes).coerceIn(0f, 1f)
                            onProgress(fraction)
                        }
                    }
                    outputStream.flush()
                }

                if (tempFile.length() == 0L) {
                    tempFile.delete()
                    throw IllegalStateException("Downloaded file was empty")
                }

                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                if (!tempFile.renameTo(destinationFile)) {
                    tempFile.copyTo(destinationFile, overwrite = true)
                    tempFile.delete()
                }

                onProgress(1f)
                destinationFile
            }
        }
    }
}
