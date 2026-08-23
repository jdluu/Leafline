package com.jdluu.leafline.library

import android.content.Context
import androidx.room.Room
import com.jdluu.leafline.library.data.BookDataSource
import com.jdluu.leafline.library.data.BookmarkRepository
import com.jdluu.leafline.library.data.BookmarkRepositoryImpl
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.library.data.LibraryRepositoryImpl
import com.jdluu.leafline.library.data.local.LeaflineDatabase
import com.jdluu.leafline.library.data.local.RoomBookDataSource

object LeaflineDependencyHolder {
    @Volatile
    private var database: LeaflineDatabase? = null
    
    @Volatile
    private var repository: LibraryRepository? = null

    @Volatile
    private var bookmarkRepository: BookmarkRepository? = null
    
    private var context: Context? = null
    
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }
    
    fun getDatabase(context: Context): LeaflineDatabase {
        return database ?: synchronized(this) {
            database ?: buildDatabase(context).also { database = it }
        }
    }
    
    private fun buildDatabase(context: Context): LeaflineDatabase {
        return LeaflineDatabase.build(context)
    }
    
    fun getRepository(context: Context): LibraryRepository {
        return repository ?: synchronized(this) {
            repository ?: buildRepository(context).also { repository = it }
        }
    }
    
    private fun buildRepository(context: Context): LibraryRepository {
        val dataSource: BookDataSource = RoomBookDataSource(getDatabase(context))
        return LibraryRepositoryImpl(dataSource)
    }

    fun getBookmarkRepository(context: Context): BookmarkRepository {
        return bookmarkRepository ?: synchronized(this) {
            bookmarkRepository ?: BookmarkRepositoryImpl(getDatabase(context).bookmarkDao())
                .also { bookmarkRepository = it }
        }
    }
}