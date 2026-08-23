package com.jdluu.leafline.library.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT collectionId FROM book_collection_cross_ref WHERE bookStableId = :bookStableId")
    fun getCollectionIdsForBook(bookStableId: String): Flow<List<Long>>

    @Query("SELECT * FROM collections WHERE id IN (SELECT collectionId FROM book_collection_cross_ref WHERE bookStableId = :bookStableId)")
    suspend fun getCollectionsForBook(bookStableId: String): List<CollectionEntity>

    @Query("SELECT bookStableId FROM book_collection_cross_ref WHERE collectionId = :collectionId")
    fun getBookIdsForCollection(collectionId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM book_collection_cross_ref WHERE collectionId = :collectionId")
    fun getBookCountForCollection(collectionId: Long): Flow<Int>

    @Query("SELECT DISTINCT c.id, c.name, c.createdAtEpochMillis FROM collections c " +
        "INNER JOIN book_collection_cross_ref bcc ON c.id = bcc.collectionId " +
        "ORDER BY c.name ASC")
    fun getNonEmptyCollections(): Flow<List<CollectionEntity>>
}