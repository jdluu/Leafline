package com.jdluu.leafline.opds.model

import com.jdluu.leafline.library.LibraryBook

/**
 * Representation of an OPDS catalog feed source (built-in or user-added custom feed).
 */
data class OpdsFeedSource(
    val id: String,
    val title: String,
    val url: String,
    val isBuiltIn: Boolean = false,
    val description: String? = null,
    val username: String? = null,
    val password: String? = null,
    val authToken: String? = null
) {
    /**
     * Computes the Authorization header string (HTTP Basic or Bearer) if credentials are present.
     */
    fun authHeader(): String? {
        if (!authToken.isNullOrBlank()) {
            return "Bearer ${authToken.trim()}"
        }
        if (!username.isNullOrBlank() || !password.isNullOrBlank()) {
            val user = username.orEmpty()
            val pass = password.orEmpty()
            val credentials = "$user:$pass"
            val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
            return "Basic $encoded"
        }
        return null
    }

    val hasAuth: Boolean
        get() = !authToken.isNullOrBlank() || !username.isNullOrBlank() || !password.isNullOrBlank()
}

/**
 * An individual publication item available for download in an OPDS catalog.
 */
data class OpdsBook(
    val id: String,
    val title: String,
    val author: String? = null,
    val summary: String? = null,
    val coverUrl: String? = null,
    val downloadUrl: String? = null,
    val categories: List<String> = emptyList(),
    val publishedDate: String? = null
)

/**
 * A navigation link within an OPDS catalog (e.g. subcategory, author shelf, genre).
 */
data class OpdsNavigationLink(
    val title: String,
    val href: String,
    val subtitle: String? = null,
    val count: Int? = null
)

/**
 * A named group of publications or navigation links within an OPDS feed.
 */
data class OpdsGroup(
    val title: String,
    val books: List<OpdsBook> = emptyList(),
    val navigationLinks: List<OpdsNavigationLink> = emptyList()
)

/**
 * The parsed result of an OPDS feed (compatible with OPDS 1.x and OPDS 2.0).
 */
data class OpdsFeedResult(
    val title: String,
    val feedUrl: String,
    val books: List<OpdsBook> = emptyList(),
    val navigationLinks: List<OpdsNavigationLink> = emptyList(),
    val groups: List<OpdsGroup> = emptyList(),
    val searchTemplateUrl: String? = null,
    val nextPageUrl: String? = null,
    val previousPageUrl: String? = null
)

/**
 * An item in the breadcrumb navigation stack for hierarchical catalog browsing.
 */
data class BreadcrumbItem(
    val title: String,
    val url: String
)

/**
 * Lifecycle state of a catalog book download and import.
 */
sealed interface DownloadStatus {
    object Idle : DownloadStatus
    data class Downloading(val progressFraction: Float) : DownloadStatus
    object Importing : DownloadStatus
    data class Success(val book: LibraryBook) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}
