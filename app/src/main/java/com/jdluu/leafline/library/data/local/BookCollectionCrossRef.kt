package com.jdluu.leafline.library.data.local

import androidx.room.Entity

/**
 * Many-to-many junction between collections and books.
 */
@Entity(
    tableName = "book_collection_cross_ref",
    primaryKeys = ["collectionId", "bookStableId"]
)
data class BookCollectionCrossRef(
    val collectionId: Long,
    val bookStableId: String
)