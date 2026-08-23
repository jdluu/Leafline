package com.jdluu.leafline.library.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LeaflineDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN lastLocatorJson TEXT")
            }
        }

        fun build(context: Context): LeaflineDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                LeaflineDatabase::class.java,
                "leafline-database"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
