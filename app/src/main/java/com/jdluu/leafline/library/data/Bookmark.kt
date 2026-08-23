package com.jdluu.leafline.library.data

data class Bookmark(
    val id: Long,
    val bookId: String,
    val locatorJson: String,
    val createdAt: Long,
    val label: String? = null
)

sealed interface BookmarkToggleResult {
    data object Added : BookmarkToggleResult
    data object Removed : BookmarkToggleResult
}
