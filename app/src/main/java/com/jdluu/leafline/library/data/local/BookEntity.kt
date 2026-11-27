package com.jdluu.leafline.library.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus

@Entity(
    tableName = "books",
    indices = [
        Index("fileHash"),
        Index("lastReadAtEpochMillis"),
        Index("readingStatus")
    ]
)
data class BookEntity(
    @PrimaryKey val stableId: String,
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
    val lastLocatorJson: String? = null,
    val coverPath: String? = null,
    val koreaderHash: String? = null,
    val lastReadAtEpochMillis: Long? = null,
    val readingStatus: String = "unread",
    val rating: Int? = null,
    val tags: List<String> = emptyList(),
    val series: String? = null,
    val seriesIndex: Float? = null
) {
    companion object {
        fun fromLibraryBook(book: LibraryBook): BookEntity {
            return BookEntity(
                stableId = book.stableId,
                title = book.title,
                authors = book.authors,
                language = book.language,
                description = book.description,
                publisher = book.publisher,
                publishedAtEpochMillis = book.publishedAtEpochMillis,
                filePath = book.filePath,
                fileHash = book.fileHash,
                addedAtEpochMillis = book.addedAtEpochMillis,
                pageCount = book.pageCount,
                coverPath = book.coverPath,
                koreaderHash = book.koreaderHash,
                lastReadAtEpochMillis = book.lastReadAtEpochMillis,
                lastLocatorJson = book.lastLocatorJson,
                readingStatus = book.readingStatus.dbValue,
                rating = book.rating,
                tags = book.tags,
                series = book.series,
                seriesIndex = book.seriesIndex
            )
        }

        fun toLibraryBook(entity: BookEntity): LibraryBook {
            return LibraryBook(
                stableId = entity.stableId,
                title = entity.title,
                authors = entity.authors,
                language = entity.language,
                description = entity.description,
                publisher = entity.publisher,
                publishedAtEpochMillis = entity.publishedAtEpochMillis,
                filePath = entity.filePath,
                fileHash = entity.fileHash,
                addedAtEpochMillis = entity.addedAtEpochMillis,
                pageCount = entity.pageCount,
                coverPath = entity.coverPath,
                koreaderHash = entity.koreaderHash,
                lastReadAtEpochMillis = entity.lastReadAtEpochMillis,
                lastLocatorJson = entity.lastLocatorJson,
                readingStatus = ReadingStatus.fromDb(entity.readingStatus),
                rating = entity.rating,
                tags = entity.tags,
                series = entity.series,
                seriesIndex = entity.seriesIndex
            )
        }
    }
}
