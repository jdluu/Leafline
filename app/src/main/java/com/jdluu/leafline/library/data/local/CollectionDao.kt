package com.jdluu.leafline.library.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CollectionBookCount(
    val collectionId: Long,
    val count: Int
)

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY name ASC")
    suspend fun getAllCollectionsList(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionById(id: Long): CollectionEntity?

    @Query("SELECT * FROM collections WHERE name = :name LIMIT 1")
    suspend fun getCollectionByName(name: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(collection: CollectionEntity): Long

    @Delete
    suspend fun delete(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    // -- Book-collection association --

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToCollection(crossRef: BookCollectionCrossRef)

    @Query("DELETE FROM book_collection_cross_ref WHERE collectionId = :collectionId AND bookStableId = :bookStableId")
    suspend fun removeBookFromCollection(collectionId: Long, bookStableId: String)

    @Query("DELETE FROM book_collection_cross_ref WHERE bookStableId = :bookStableId")
    suspend fun removeBookFromAllCollections(bookStableId: String)

    @Query("SELECT collectionId FROM book_collection_cross_ref WHERE bookStableId = :bookStableId")
    fun getCollectionIdsForBook(bookStableId: String): Flow<List<Long>>

    @Query("SELECT * FROM collections WHERE id IN (SELECT collectionId FROM book_collection_cross_ref WHERE bookStableId = :bookStableId)")
    suspend fun getCollectionsForBook(bookStableId: String): List<CollectionEntity>

    @Query(
        "SELECT c.* FROM collections c " +
            "INNER JOIN book_collection_cross_ref bcc ON c.id = bcc.collectionId " +
            "WHERE bcc.bookStableId = :bookStableId " +
            "ORDER BY c.name ASC"
    )
    fun observeCollectionsForBook(bookStableId: String): Flow<List<CollectionEntity>>

    @Query("SELECT bookStableId FROM book_collection_cross_ref WHERE collectionId = :collectionId")
    fun getBookIdsForCollection(collectionId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM book_collection_cross_ref WHERE collectionId = :collectionId")
    fun getBookCountForCollection(collectionId: Long): Flow<Int>

    @Query(
        "SELECT c.id AS collectionId, COUNT(bcc.bookStableId) AS count FROM collections c " +
            "LEFT JOIN book_collection_cross_ref bcc ON c.id = bcc.collectionId " +
            "GROUP BY c.id"
    )
    fun observeCollectionBookCounts(): Flow<List<CollectionBookCount>>

    @Query("SELECT DISTINCT c.id, c.name, c.createdAtEpochMillis FROM collections c " +
        "INNER JOIN book_collection_cross_ref bcc ON c.id = bcc.collectionId " +
        "ORDER BY c.name ASC")
    fun getNonEmptyCollections(): Flow<List<CollectionEntity>>
}