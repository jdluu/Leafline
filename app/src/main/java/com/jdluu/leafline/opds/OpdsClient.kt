package com.jdluu.leafline.opds

import com.jdluu.leafline.opds.model.OpdsFeedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Turns a bare status code into something a reader can act on. OkHttp reports an empty
 * message for HTTP/2 responses, which used to surface as a trailing "HTTP 401:".
 */
internal fun opdsHttpErrorMessage(
    code: Int,
    serverMessage: String,
    action: String? = null
): String {
    val detail = when {
        code == 401 || code == 403 ->
            "This catalog needs a sign-in. Add it again with your username and password."
        code == 404 -> "Catalog not found. The feed address may have moved."
        code in 500..599 -> "The catalog server had a problem. Try again later."
        serverMessage.isNotBlank() -> serverMessage
        else -> "The catalog rejected the request."
    }
    val prefix = if (action != null) "${action.replaceFirstChar { it.uppercase() }}: " else ""
    return "$prefix$detail (HTTP $code)"
}

/**
 * Splits the trailing "(HTTP nnn)" off a message. The code is real information for a
 * bug report, but it is not the sentence a reader needs first, so the UI shows the
 * detail as the message and the code as a caption under the actions.
 */
internal fun splitHttpStatus(message: String): Pair<String, String?> {
    val match = HTTP_SUFFIX.find(message.trim())
        ?: return message.trim() to null
    val detail = message.trim().removeRange(match.range).trim()
    return detail to "HTTP ${match.groupValues[1]}"
}

private val HTTP_SUFFIX = Regex("""\s*\(HTTP (\d{3})\)$""")

/**
 * HTTP client for fetching OPDS catalog feeds, searching feeds, and downloading EPUB publications.
 */
open class OpdsClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val userAgent: String = "Leafline (Android; OPDS Client)"
) {

    /**
     * Fetches and parses an OPDS catalog feed from the given [url].
     */
    open suspend fun fetchFeed(
        url: String,
        authHeader: String? = null
    ): Result<OpdsFeedResult> = withContext(Dispatchers.IO) {
        runCatching {
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/atom+xml, application/opds+json, application/json, text/xml, */*")

            if (!authHeader.isNullOrBlank()) {
                builder.header("Authorization", authHeader)
            }

            val request = builder.build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException(opdsHttpErrorMessage(response.code, response.message))
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
     * Resolves an OpenSearch description XML URL into a concrete search template URL containing {searchTerms}.
     */
    open suspend fun resolveSearchTemplate(
        searchUrl: String,
        authHeader: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (searchUrl.contains("{searchTerms}")) {
            return@withContext searchUrl
        }
        try {
            val builder = Request.Builder().url(searchUrl)
            if (!authHeader.isNullOrBlank()) {
                builder.header("Authorization", authHeader)
            }
            okHttpClient.newCall(builder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val xml = response.body?.string().orEmpty()
                    val templateRegex = Regex("""template=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    val match = templateRegex.find(xml)
                    if (match != null) {
                        val rawTemplate = match.groupValues[1]
                        return@withContext try {
                            val base = searchUrl.toHttpUrl()
                            base.resolve(rawTemplate)?.toString() ?: rawTemplate
                        } catch (_: Exception) {
                            rawTemplate
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to original url
        }
        searchUrl
    }

    /**
     * Searches a catalog feed using its OpenSearch URL template.
     * Replaces `{searchTerms}` with URL-encoded query.
     */
    open suspend fun searchFeed(
        searchTemplateUrl: String,
        query: String,
        authHeader: String? = null
    ): Result<OpdsFeedResult> {
        val resolvedTemplate = if (!searchTemplateUrl.contains("{searchTerms}") &&
            (searchTemplateUrl.contains("opensearch", ignoreCase = true) || searchTemplateUrl.endsWith(".xml", ignoreCase = true))
        ) {
            resolveSearchTemplate(searchTemplateUrl, authHeader)
        } else {
            searchTemplateUrl
        }

        val encodedQuery = withContext(Dispatchers.IO) {
            URLEncoder.encode(query, "UTF-8")
        }
        val targetUrl = if (resolvedTemplate.contains("{searchTerms}")) {
            resolvedTemplate.replace("{searchTerms}", encodedQuery)
        } else if (resolvedTemplate.contains("?")) {
            "$resolvedTemplate&q=$encodedQuery"
        } else {
            "$resolvedTemplate?q=$encodedQuery"
        }
        return fetchFeed(targetUrl, authHeader)
    }

    /**
     * Downloads an EPUB file from [downloadUrl] directly to [destinationFile],
     * invoking [onProgress] with values from 0.0 to 1.0.
     */
    open suspend fun downloadEpubFile(
        downloadUrl: String,
        destinationFile: File,
        authHeader: String? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val builder = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", userAgent)

            if (!authHeader.isNullOrBlank()) {
                builder.header("Authorization", authHeader)
            }

            val request = builder.build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException(opdsHttpErrorMessage(response.code, response.message, "download failed"))
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
