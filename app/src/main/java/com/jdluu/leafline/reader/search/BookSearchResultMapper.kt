package com.jdluu.leafline.reader.search

import org.readium.r2.shared.publication.Locator

/** Maps a Readium search locator into a UI row model. */
object BookSearchResultMapper {

    fun map(
        id: Int,
        href: String,
        title: String?,
        before: String?,
        match: String?,
        after: String?,
        locatorJson: String
    ): BookSearchResult {
        return BookSearchResult(
            id = id,
            sectionTitle = sectionTitle(title, href),
            excerptBefore = before.orEmpty(),
            excerptMatch = match.orEmpty(),
            excerptAfter = after.orEmpty(),
            locatorJson = locatorJson
        )
    }

    /** Derives a short section label from the locator title, falling back to href. */
    fun sectionTitle(title: String?, href: String): String? {
        if (!title.isNullOrBlank()) return title
        val fileName = href.substringAfterLast('/')
        return fileName.ifEmpty { null }
    }
}
