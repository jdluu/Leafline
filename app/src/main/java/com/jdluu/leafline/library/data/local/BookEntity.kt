package com.jdluu.leafline.library.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jdluu.leafline.library.LibraryBook

@Entity(tableName = "books")
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
    val coverPath: String? = null
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
                coverPath = book.coverPath
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
                coverPath = entity.coverPath
            )
        }
    }
}