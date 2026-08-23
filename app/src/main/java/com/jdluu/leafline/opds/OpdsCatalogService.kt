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

object OpdsConfigStore {
    @Volatile
    var config: OpdsServerConfig? = null
}

class OpdsCatalogService(
    private val client: HttpClient = DefaultHttpClient()
) {
    private fun basicAuthHeader(config: OpdsServerConfig): String {
        val credentials = "${config.username}:${config.password}"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
        return "Basic $encoded"
    }

    private fun org.readium.r2.shared.publication.Link.resolvedHref(): String = href.toString()

    private fun org.readium.r2.shared.publication.Link.toFeedEntry(
        title: String,
        authors: List<String> = emptyList()
    ): OpdsFeedEntry = OpdsFeedEntry(
        title = title,
        href = resolvedHref(),
        isAcquisition = isAcquisitionRel(rels),
        authors = authors
    )

    private fun org.readium.r2.shared.opds.Feed.toPage(): OpdsFeedPage {
        val navigation = navigation.map { link ->
            link.toFeedEntry(link.title ?: link.href.toString())
        }
        val publications = publications.flatMap { publication ->
            val authors = publication.metadata.authors.mapNotNull { it.name }
            publication.links
                .filter { isAcquisitionRel(it.rels) }
                .map { link ->
                    link.toFeedEntry(publication.metadata.title ?: link.href.toString(), authors)
                }
        }
        return OpdsFeedPage(
            title = metadata?.title ?: title,
            entries = navigation + publications
        )
    }

    suspend fun fetchRootNavigation(config: OpdsServerConfig): Try<List<OpdsNavigationEntry>, Exception> =
        fetchFeed(config).map { it.navigationEntries() }

    /** Fetches and parses any OPDS 1.2 feed, preserving resolved links. */
    suspend fun fetchFeed(config: OpdsServerConfig): Try<OpdsFeedPage, Exception> =
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
                    Try.success(parseData.feed!!.toPage())
                }
            } catch (e: Exception) {
                logger.severe("OPDS root fetch failed: ${e.message}")
                Try.failure(e)
            }
        }

    fun parseXml(xml: ByteArray, catalogUrl: String): Try<List<OpdsNavigationEntry>, Exception> =
        parseXmlPage(xml, catalogUrl).map { it.navigationEntries() }

    fun parseXmlPage(xml: ByteArray, catalogUrl: String): Try<OpdsFeedPage, Exception> =
        try {
            val url = AbsoluteUrl(catalogUrl)
                ?: throw IllegalArgumentException("Invalid catalog URL")
            val parseData = OPDS1Parser.parse(xml, url)
            Try.success(parseData.feed!!.toPage())
        } catch (e: Exception) {
            logger.severe("OPDS XML parse failed: ${e.message}")
            Try.failure(e)
        }
}

private fun List<org.readium.r2.shared.publication.Link>.toNavigation(): List<OpdsNavigationEntry> =
    mapNotNull { link ->
        val hrefText = link.href.toString()
        if (hrefText.isBlank()) null else OpdsNavigationEntry(
            title = link.title ?: hrefText,
            href = hrefText
        )
    }

internal fun isAcquisitionRel(relations: Set<String>): Boolean =
    relations.any {
        it == "http://opds-spec.org/acquisition" ||
            it.startsWith("http://opds-spec.org/acquisition/") ||
            it == "http://opds-spec.org/acquisition/open-access"
    }
