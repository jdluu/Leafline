package com.jdluu.leafline.library.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY addedAtEpochMillis DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query(
        "SELECT * FROM books " +
            "WHERE title LIKE '%' || :query || '%' OR authors LIKE '%' || :query || '%' " +
            "ORDER BY addedAtEpochMillis DESC"
    )
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE stableId = :stableId LIMIT 1")
    suspend fun getBookByStableId(stableId: String): BookEntity?

    @Query("SELECT * FROM books WHERE fileHash = :fileHash LIMIT 1")
    suspend fun getBookByFileHash(fileHash: String): BookEntity?

    @Query("SELECT * FROM books WHERE filePath = :filePath LIMIT 1")
    suspend fun getBookByFilePath(filePath: String): BookEntity?

    @Query("UPDATE books SET lastLocatorJson = :locatorJson WHERE stableId = :stableId")
    suspend fun updateLastLocator(stableId: String, locatorJson: String?)

    @Query("UPDATE books SET coverPath = :coverPath WHERE stableId = :stableId")
    suspend fun setCoverPath(stableId: String, coverPath: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("DELETE FROM books WHERE stableId = :stableId")
    suspend fun delete(stableId: String): Int
}