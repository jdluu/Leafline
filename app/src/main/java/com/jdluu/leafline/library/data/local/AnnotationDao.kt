package com.jdluu.leafline.library.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY createdAt DESC, id DESC")
    fun observeForBook(bookId: String): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY createdAt DESC, id DESC")
    suspend fun getForBook(bookId: String): List<AnnotationEntity>

    @Insert
    suspend fun insert(annotation: AnnotationEntity): Long

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("UPDATE annotations SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?): Int
}
