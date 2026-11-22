package com.jdluu.leafline.opds

import org.readium.r2.shared.util.Url
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Recovers cover images from an OPDS 1.x Atom feed.
 *
 * Readium's [org.readium.r2.opds.OPDS1Parser] keeps only the acquisition link on a
 * publication; the `http://opds-spec.org/image` and `.../image/thumbnail` links are
 * dropped, which leaves every catalog card without artwork. Rather than replace the
 * parser, this makes one tolerant pass over the same bytes and pairs each entry's
 * cover with its acquisition URL, the one link both parsers agree on.
 */
internal object OpdsCoverLinks {

    private const val REL_ACQUISITION = "http://opds-spec.org/acquisition"
    private const val REL_IMAGE = "http://opds-spec.org/image"
    private const val REL_IMAGE_THUMBNAIL = "http://opds-spec.org/image/thumbnail"

    /**
     * Maps an entry's acquisition URL to its cover URL, both resolved against
     * [baseUrl] so they match what Readium reports for the acquisition link.
     *
     * An entry with no acquisition link, or no image, is simply absent. Failure to
     * parse returns an empty map: missing covers are cosmetic and must never cost
     * the caller the whole feed.
     */
    fun extract(data: ByteArray, baseUrl: Url): Map<String, String> {
        val covers = mutableMapOf<String, String>()
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                // A catalog is untrusted input; no external entities, no DTD fetch.
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                isExpandEntityReferences = false
            }
            val document = factory.newDocumentBuilder()
                .parse(ByteArrayInputStream(data))
            val entries = document.getElementsByTagNameNS("*", "entry")

            for (i in 0 until entries.length) {
                val entry = entries.item(i) as? Element ?: continue
                val links = entry.getElementsByTagNameNS("*", "link")

                var acquisition: String? = null
                var image: String? = null
                var thumbnail: String? = null

                for (j in 0 until links.length) {
                    val link = links.item(j) as? Element ?: continue
                    val rel = link.getAttribute("rel").orEmpty()
                    val href = link.getAttribute("href").orEmpty()
                    val type = link.getAttribute("type").orEmpty()
                    if (href.isBlank()) continue

                    when {
                        rel == REL_IMAGE -> image = resolve(href, baseUrl)
                        rel == REL_IMAGE_THUMBNAIL -> thumbnail = resolve(href, baseUrl)
                        rel.startsWith(REL_ACQUISITION) ||
                            type.contains("epub", ignoreCase = true) ->
                            acquisition = acquisition ?: resolve(href, baseUrl)
                    }
                }

                // The full image beats the thumbnail when a feed offers both.
                val cover = image ?: thumbnail
                val acq = acquisition
                if (cover != null && acq != null) covers[acq] = cover
            }
        } catch (_: Exception) {
            return emptyMap()
        }
        return covers
    }

    /**
     * Resolves [href] against the feed URL.
     *
     * `java.net.URI` rather than Readium's `Url`: Readium's `Url("...")` on a
     * path-only reference does not resolve the way `Link.href.resolve(base)` does,
     * and returned the base unchanged, which silently keyed every cover to the feed
     * URL itself.
     */
    private fun resolve(href: String, baseUrl: Url): String {
        val base = baseUrl.toString()
        return try {
            java.net.URI(base).resolve(href).toString()
        } catch (_: Exception) {
            if (href.startsWith("http")) href else base.substringBeforeLast("/") + "/" + href.trimStart('/')
        }
    }
}
