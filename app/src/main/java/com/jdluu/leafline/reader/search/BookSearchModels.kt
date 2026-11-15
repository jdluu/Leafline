package com.jdluu.leafline.reader.search

/** Configuration options for in-book search runs. */
data class SearchOptions(
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val regex: Boolean = false,
    val chapterHref: String? = null
) {
    val isDefault: Boolean
        get() = !caseSensitive && !wholeWord && !regex && chapterHref == null
}

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
    val options: SearchOptions = SearchOptions(),
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

    /** Validates a regular expression pattern; returns error message or null if valid. */
    fun validateRegex(pattern: String): String? {
        return try {
            Regex(pattern)
            null
        } catch (e: Exception) {
            e.localizedMessage ?: "Invalid regular expression"
        }
    }
}
