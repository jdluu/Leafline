package com.jdluu.leafline.library.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-defined collection (tag/grouping) for organizing books.
 */
@Entity(
    tableName = "collections",
    indices = [Index(value = ["name"])]
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAtEpochMillis: Long
)