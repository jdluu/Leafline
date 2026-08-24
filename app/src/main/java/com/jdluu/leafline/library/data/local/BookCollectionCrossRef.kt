package com.jdluu.leafline.library.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * Many-to-many junction between collections and books.
 */
@Entity(
    tableName = "book_collection_cross_ref",
    primaryKeys = ["collectionId", "bookStableId"],
    indices = [Index(value = ["bookStableId"])]
)
data class BookCollectionCrossRef(
    val collectionId: Long,
    val bookStableId: String
)