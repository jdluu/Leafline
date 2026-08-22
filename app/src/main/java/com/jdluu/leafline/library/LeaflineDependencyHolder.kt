package com.jdluu.leafline.library

import android.content.Context
import androidx.room.Room
import com.jdluu.leafline.library.data.BookDataSource
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.library.data.LibraryRepositoryImpl
import com.jdluu.leafline.library.data.local.LeaflineDatabase
import com.jdluu.leafline.library.data.local.RoomBookDataSource

object LeaflineDependencyHolder {
    @Volatile
    private var database: LeaflineDatabase? = null
    
    @Volatile
    private var repository: LibraryRepository? = null
    
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
        return Room.databaseBuilder(
            context.applicationContext,
            LeaflineDatabase::class.java,
            "leafline-database"
        )
            .fallbackToDestructiveMigration()
            .build()
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
}