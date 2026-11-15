package com.jdluu.leafline.reader.footnotes

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

/**
 * Loads resource content from a [Publication] and extracts footnote details
 * asynchronously on [ioDispatcher].
 */
@OptIn(ExperimentalReadiumApi::class)
class FootnoteResolver(
    private val publication: Publication,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Resolves and extracts footnote content for [link].
     *
     * @param link The target internal link tapped by the user.
     * @param targetLocatorJson Serialized locator to jump to if user taps "Jump to note".
     * @return [FootnoteContent] if located and readable, or null if resolution fails.
     */
    suspend fun resolve(
        link: Link,
        targetLocatorJson: String? = null
    ): FootnoteContent? = withContext(ioDispatcher) {
        val targetUrl = link.url()
        val fragment = targetUrl.fragment ?: return@withContext null

        val resourceUrl = targetUrl.removeFragment()
        val resource = publication.get(resourceUrl) ?: return@withContext null
        val rawBytes = try {
            resource.read().getOrNull()
        } finally {
            resource.close()
        } ?: return@withContext null

        val htmlString = String(rawBytes, Charsets.UTF_8)
        FootnoteExtractor.extractFootnote(
            html = htmlString,
            fragment = fragment,
            targetLocatorJson = targetLocatorJson
        )
    }
}
