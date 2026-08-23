package com.jdluu.leafline.opds

import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.HttpRequest.Method

/** Lightweight logger that works in both JVM unit tests and Android. */
private val logger = java.util.logging.Logger.getLogger("OpdsCatalogService")

/**
 * Simplest in-memory holder of OPDS server configuration for the first slice.
 * Credentials live only here and are never committed.
 */
object OpdsConfigStore {
    @Volatile
    var config: OpdsServerConfig? = null
}

/**
 * Fetches and parses a Grimmory OPDS 1.2 root catalog feed using the official
 * Readium `readium-opds` parser and the shared `DefaultHttpClient`.
 *
 * HTTP Basic Auth is applied from [OpdsServerConfig]. Returns navigation
 * entries (title + href) found in the feed root.
 */
class OpdsCatalogService(
    private val client: HttpClient = DefaultHttpClient()
) {
    private fun basicAuthHeader(config: OpdsServerConfig): String {
        val credentials = "${config.username}:${config.password}"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
        return "Basic $encoded"
    }

    private fun List<org.readium.r2.shared.publication.Link>.toNavigation(): List<OpdsNavigationEntry> =
        mapNotNull { link ->
            val hrefText = link.href.toString()
            if (hrefText.isBlank()) null else OpdsNavigationEntry(
                title = link.title ?: hrefText,
                href = hrefText
            )
        }

    /** Fetches and parses the root OPDS feed, returning its navigation links. */
    suspend fun fetchRootNavigation(config: OpdsServerConfig): Try<List<OpdsNavigationEntry>, Exception> =
        withContext(Dispatchers.IO) {
            try {
                val url = AbsoluteUrl(config.catalogUrl)
                    ?: throw IllegalArgumentException("Invalid catalog URL")
                val request = HttpRequest(url) {
                    method = Method.GET
                    setHeader("Authorization", basicAuthHeader(config))
                    setHeader("Accept", "application/atom+xml")
                }
                val parseResult = OPDS1Parser.parseRequest(request, client)
                val parseData = parseResult.getOrNull()
                if (parseData == null) {
                    val failure = parseResult.failureOrNull()
                    logger.severe("OPDS parse failed: $failure")
                    Try.failure(failure ?: Exception("OPDS parse failed"))
                } else {
                    Try.success(parseData.feed!!.navigation.toNavigation())
                }
            } catch (e: Exception) {
                logger.severe("OPDS root fetch failed: ${e.message}")
                Try.failure(e)
            }
        }

    /** Parses raw OPDS 1.2 XML bytes, used in tests with fixture feeds. */
    fun parseXml(xml: ByteArray, catalogUrl: String): Try<List<OpdsNavigationEntry>, Exception> =
        try {
            val url = AbsoluteUrl(catalogUrl)
                ?: throw IllegalArgumentException("Invalid catalog URL")
            val parseData = OPDS1Parser.parse(xml, url)
            Try.success(parseData.feed!!.navigation.toNavigation())
        } catch (e: Exception) {
            logger.severe("OPDS XML parse failed: ${e.message}")
            Try.failure(e)
        }
}
