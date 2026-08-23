package com.jdluu.leafline.library

import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.ReadingStatus.UNREAD

data class LibraryBook(
    val stableId: String,
    val title: String,
    val authors: List<String>,
    val language: String?,
    val description: String?,
    val publisher: String?,
    val publishedAtEpochMillis: Long?,
    val filePath: String,
    val fileHash: String,
    val addedAtEpochMillis: Long?,
    val pageCount: Int?,
    val coverPath: String? = null,
    val koreaderHash: String? = null,
    val lastReadAtEpochMillis: Long? = null,
    val lastLocatorJson: String? = null,
    val readingStatus: ReadingStatus = UNREAD
)