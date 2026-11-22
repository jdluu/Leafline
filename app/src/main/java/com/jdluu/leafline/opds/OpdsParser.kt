package com.jdluu.leafline.opds

import com.jdluu.leafline.opds.model.OpdsBook
import com.jdluu.leafline.opds.model.OpdsFeedResult
import com.jdluu.leafline.opds.model.OpdsGroup
import com.jdluu.leafline.opds.model.OpdsNavigationLink
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.opds.Feed
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

/**
 * Parser for OPDS 1.x (Atom XML) and OPDS 2.0 (JSON) feeds.
 */
object OpdsParser {

    private const val REL_ACQUISITION = "http://opds-spec.org/acquisition"
    private const val REL_IMAGE = "http://opds-spec.org/image"
    private const val REL_IMAGE_THUMBNAIL = "http://opds-spec.org/image/thumbnail"
    private const val REL_SEARCH = "search"
    private const val REL_NEXT = "next"
    private const val REL_PREVIOUS = "previous"

    fun parse(data: ByteArray, feedUrlString: String, isJson: Boolean = false): OpdsFeedResult {
        val baseUrl = Url(feedUrlString) ?: error("Invalid feed URL: $feedUrlString")

        val parseData: ParseData = if (isJson) {
            try {
                OPDS2Parser.parse(data, baseUrl)
            } catch (_: Exception) {
                OPDS1Parser.parse(data, baseUrl)
            }
        } else {
            try {
                OPDS1Parser.parse(data, baseUrl)
            } catch (_: Exception) {
                OPDS2Parser.parse(data, baseUrl)
            }
        }

        val feed: Feed? = parseData.feed
        val singlePub: Publication? = parseData.publication

        return if (feed != null) {
            convertFeedToResult(feed, baseUrl, feedUrlString, data)
        } else if (singlePub != null) {
            val book = convertPublicationToBook(singlePub, baseUrl)
            OpdsFeedResult(
                title = singlePub.metadata.title.orEmpty().ifBlank { "Publication" },
                feedUrl = feedUrlString,
                books = listOf(book)
            )
        } else {
            OpdsFeedResult(
                title = "Catalog",
                feedUrl = feedUrlString
            )
        }
    }

    private fun convertFeedToResult(feed: Feed, baseUrl: Url, feedUrlString: String, data: ByteArray): OpdsFeedResult {
        // Readium drops OPDS image links, so recover covers from the raw feed.
        val covers = OpdsCoverLinks.extract(data, baseUrl)
        val books = feed.publications.map { convertPublicationToBook(it, baseUrl, covers) }

        val navigationCandidateLinks = feed.navigation + feed.links.filter { link ->
            link.rels.contains("subsection") ||
                link.rels.any { it.contains("subsection") } ||
                link.rels.contains("http://opds-spec.org/sort/new") ||
                link.rels.contains("http://opds-spec.org/sort/popular") ||
                (link.title != null && link.mediaType?.matches("application/atom+xml") == true &&
                    !link.rels.contains("self") && !link.rels.contains(REL_SEARCH) &&
                    !link.rels.contains(REL_NEXT) && !link.rels.contains(REL_PREVIOUS))
        }

        val navigationLinks = navigationCandidateLinks
            .distinctBy { it.href.toString() }
            .map { link ->
                val resolvedUrl = link.href.resolve(baseUrl)?.toString() ?: link.href.toString()
                OpdsNavigationLink(
                    title = link.title ?: "Section",
                    href = resolvedUrl
                )
            }

        val groups = feed.groups.map { group ->
            OpdsGroup(
                title = group.title,
                books = group.publications.map { convertPublicationToBook(it, baseUrl, covers) },
                navigationLinks = group.links.map { link ->
                    val resolvedUrl = link.href.resolve(baseUrl)?.toString() ?: link.href.toString()
                    OpdsNavigationLink(
                        title = link.title ?: "Section",
                        href = resolvedUrl
                    )
                }
            )
        }

        val searchLink = feed.links.firstOrNull { it.rels.contains(REL_SEARCH) }
            ?: feed.links.firstOrNull { it.rels.any { rel -> rel.contains("search") } }

        var searchTemplateUrl = searchLink?.href?.let { href ->
            href.resolve(baseUrl)?.toString() ?: href.toString()
        }

        if (searchTemplateUrl == null) {
            // Readium's strict Url parser drops templated hrefs containing '{' and '}'.
            // Extract search template from raw XML/JSON directly if present.
            val rawText = data.toString(Charsets.UTF_8)
            val searchRegex = Regex("""<link[^>]+rel=["']search["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val match = searchRegex.find(rawText)
                ?: Regex("""<link[^>]+href=["']([^"']+)["'][^>]+rel=["']search["']""", RegexOption.IGNORE_CASE).find(rawText)
            searchTemplateUrl = match?.groupValues?.get(1)
        }

        val nextLink = feed.links.firstOrNull { it.rels.contains(REL_NEXT) }
        val nextPageUrl = nextLink?.href?.resolve(baseUrl)?.toString() ?: nextLink?.href?.toString()

        val prevLink = feed.links.firstOrNull { it.rels.contains(REL_PREVIOUS) }
        val prevPageUrl = prevLink?.href?.resolve(baseUrl)?.toString() ?: prevLink?.href?.toString()

        return OpdsFeedResult(
            title = feed.title.ifBlank { "Catalog" },
            feedUrl = feedUrlString,
            books = books,
            navigationLinks = navigationLinks,
            groups = groups,
            searchTemplateUrl = searchTemplateUrl,
            nextPageUrl = nextPageUrl,
            previousPageUrl = prevPageUrl
        )
    }

    private fun convertPublicationToBook(
        pub: Publication,
        baseUrl: Url,
        covers: Map<String, String> = emptyMap()
    ): OpdsBook {
        val title = pub.metadata.title.orEmpty().ifBlank { "Untitled" }
        val id = pub.metadata.identifier?.ifBlank { null }
            ?: title.hashCode().toString()

        val author = pub.metadata.authors
            .mapNotNull { it.name.ifBlank { null } }
            .joinToString(", ")
            .ifBlank { null }

        val summary = pub.metadata.description?.ifBlank { null }

        val imageSubcollectionLinks = pub.subcollections["images"]
            ?.flatMap { it.links }
            .orEmpty()

        val coverLink = imageSubcollectionLinks.firstOrNull()
            ?: pub.linkWithRel(REL_IMAGE)
            ?: pub.linkWithRel(REL_IMAGE_THUMBNAIL)
            ?: pub.linkWithRel("cover")
            ?: pub.links.firstOrNull {
                it.rels.contains(REL_IMAGE) ||
                    it.rels.contains(REL_IMAGE_THUMBNAIL) ||
                    it.rels.contains("cover")
            } ?: pub.links.firstOrNull { it.mediaType?.isBitmap == true }

        var coverUrl = coverLink?.href?.let { href ->
            href.resolve(baseUrl)?.toString() ?: href.toString()
        }

        val acquisitionLink = pub.links.firstOrNull { link ->
            link.mediaType?.matches("application/epub+zip") == true ||
                link.rels.contains(REL_ACQUISITION) ||
                link.rels.any { it.contains("acquisition") }
        } ?: pub.links.firstOrNull { link ->
            link.href.toString().endsWith(".epub", ignoreCase = true)
        }

        val downloadUrl = acquisitionLink?.href?.let { href ->
            href.resolve(baseUrl)?.toString() ?: href.toString()
        }

        if (coverUrl == null && downloadUrl != null) {
            coverUrl = covers[downloadUrl]
        }

        val categories = pub.metadata.subjects.mapNotNull { it.name.ifBlank { null } }

        return OpdsBook(
            id = id,
            title = title,
            author = author,
            summary = summary,
            coverUrl = coverUrl,
            downloadUrl = downloadUrl,
            categories = categories
        )
    }
}
