package com.jdluu.leafline.library

import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.ReadingStatus.UNREAD

data class LibraryBook(
    val stableId: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val language: String? = null,
    val description: String? = null,
    val publisher: String? = null,
    val publishedAtEpochMillis: Long? = null,
    val filePath: String = "",
    val fileHash: String = "",
    val addedAtEpochMillis: Long? = null,
    val pageCount: Int? = null,
    val coverPath: String? = null,
    val koreaderHash: String? = null,
    val lastReadAtEpochMillis: Long? = null,
    val lastLocatorJson: String? = null,
    val readingStatus: ReadingStatus = UNREAD,
    val rating: Int? = null,
    val tags: List<String> = emptyList(),
    val series: String? = null,
    val seriesIndex: Float? = null
) {
    fun formattedSeries(): String? {
        val s = series?.trim() ?: return null
        if (s.isEmpty()) return null
        val idx = seriesIndex ?: return s
        val formattedIndex = if (idx % 1f == 0f) idx.toInt().toString() else idx.toString()
        return "$s #$formattedIndex"
    }
}
