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

    /**
     * Returns all books in [allBooks] belonging to the same series (trimmed, case-insensitive),
     * sorted by numeric series index (nulls last), then title, then stableId.
     */
    fun seriesPeers(allBooks: List<LibraryBook>): List<LibraryBook> {
        val currentSeries = series?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        return allBooks
            .filter { candidate ->
                candidate.series?.trim()?.equals(currentSeries, ignoreCase = true) == true
            }
            .sortedWith(
                compareBy<LibraryBook>(
                    { it.seriesIndex == null },
                    { it.seriesIndex ?: Float.MAX_VALUE },
                    { it.title.lowercase() },
                    { it.stableId }
                )
            )
    }

    /**
     * Returns the next book in the same series within [allBooks], or null when this is the last
     * volume or no series peers exist.
     */
    fun nextInSeries(allBooks: List<LibraryBook>): LibraryBook? {
        val sortedPeers = seriesPeers(allBooks)
        if (sortedPeers.size <= 1) return null

        val idx = seriesIndex
        if (idx != null) {
            return sortedPeers
                .filter { peer ->
                    peer.stableId != stableId &&
                        peer.seriesIndex != null &&
                        peer.seriesIndex > idx
                }
                .minWithOrNull(
                    compareBy<LibraryBook>(
                        { it.seriesIndex!! },
                        { it.readingStatus == ReadingStatus.FINISHED },
                        { it.title.lowercase() },
                        { it.stableId }
                    )
                )
        }

        val currentPos = sortedPeers.indexOfFirst { it.stableId == stableId }
        return if (currentPos >= 0 && currentPos + 1 < sortedPeers.size) {
            sortedPeers[currentPos + 1]
        } else {
            null
        }
    }
}
