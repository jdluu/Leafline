package com.jdluu.leafline.reader.search

/** A single search match shown in the search sheet. */
data class BookSearchResult(
    val id: Int,
    val sectionTitle: String?,
    val excerptBefore: String,
    val excerptMatch: String,
    val excerptAfter: String,
    val locatorJson: String
)

sealed interface BookSearchStatus {
    data object Idle : BookSearchStatus
    data object Searching : BookSearchStatus
    data class Completed(val resultCount: Int) : BookSearchStatus
    data class Failed(val message: String) : BookSearchStatus
}

data class BookSearchState(
    val query: String? = null,
    val status: BookSearchStatus = BookSearchStatus.Idle,
    val results: List<BookSearchResult> = emptyList()
)

object BookSearchQuery {
    private val whitespaceRuns = Regex("\\s+")

    /** Trims and collapses whitespace; returns null when nothing usable remains. */
    fun normalize(raw: String): String? {
        val normalized = raw.trim().replace(whitespaceRuns, " ")
        return normalized.ifEmpty { null }
    }
}
