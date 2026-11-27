package com.jdluu.leafline.library.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LeaflineDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testDbName = "migration-test.db"

    @Before
    fun setUp() {
        deleteTestDb()
    }

    @After
    fun tearDown() {
        deleteTestDb()
    }

    private fun deleteTestDb() {
        val dbFile = context.getDatabasePath(testDbName)
        if (dbFile.exists()) {
            context.deleteDatabase(testDbName)
        }
    }

    private fun createRawDatabase(version: Int, onCreate: (SupportSQLiteDatabase) -> Unit): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(testDbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    onCreate(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        return helper.writableDatabase
    }

    @Test
    fun migration1To2_addsLastLocatorJsonColumn() {
        val db = createRawDatabase(1) { db ->
            db.execSQL(
                "CREATE TABLE books (" +
                    "stableId TEXT NOT NULL PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "authors TEXT NOT NULL, " +
                    "language TEXT, " +
                    "description TEXT, " +
                    "publisher TEXT, " +
                    "publishedAtEpochMillis INTEGER, " +
                    "filePath TEXT NOT NULL, " +
                    "fileHash TEXT NOT NULL, " +
                    "addedAtEpochMillis INTEGER, " +
                    "pageCount INTEGER)"
            )
            db.execSQL(
                "INSERT INTO books (stableId, title, authors, filePath, fileHash) " +
                    "VALUES ('book-1', 'Title 1', '[\"Author\"]', '/path/1.epub', 'hash-1')"
            )
        }

        LeaflineDatabase.MIGRATION_1_2.migrate(db)

        val cursor = db.query("SELECT stableId, lastLocatorJson FROM books WHERE stableId = 'book-1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("book-1", cursor.getString(0))
        assertNull(cursor.getString(1))
        cursor.close()

        db.execSQL("UPDATE books SET lastLocatorJson = '{\"href\":\"/ch1\"}' WHERE stableId = 'book-1'")
        val updatedCursor = db.query("SELECT lastLocatorJson FROM books WHERE stableId = 'book-1'")
        assertTrue(updatedCursor.moveToFirst())
        assertEquals("{\"href\":\"/ch1\"}", updatedCursor.getString(0))
        updatedCursor.close()
        db.close()
    }

    @Test
    fun migration2To3_addsCoverPathColumn() {
        val db = createRawDatabase(2) { db ->
            db.execSQL(
                "CREATE TABLE books (" +
                    "stableId TEXT NOT NULL PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "authors TEXT NOT NULL, " +
                    "language TEXT, " +
                    "description TEXT, " +
                    "publisher TEXT, " +
                    "publishedAtEpochMillis INTEGER, " +
                    "filePath TEXT NOT NULL, " +
                    "fileHash TEXT NOT NULL, " +
                    "addedAtEpochMillis INTEGER, " +
                    "pageCount INTEGER, " +
                    "lastLocatorJson TEXT)"
            )
            db.execSQL(
                "INSERT INTO books (stableId, title, authors, filePath, fileHash) " +
                    "VALUES ('book-1', 'Title 1', '[\"Author\"]', '/path/1.epub', 'hash-1')"
            )
        }

        LeaflineDatabase.MIGRATION_2_3.migrate(db)

        val cursor = db.query("SELECT stableId, coverPath FROM books WHERE stableId = 'book-1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("book-1", cursor.getString(0))
        assertNull(cursor.getString(1))
        cursor.close()
        db.close()
    }

    @Test
    fun migration3To4_createsBookmarksTableAndIndex() {
        val db = createRawDatabase(3) { db ->
            db.execSQL("CREATE TABLE books (stableId TEXT NOT NULL PRIMARY KEY)")
        }

        LeaflineDatabase.MIGRATION_3_4.migrate(db)

        db.execSQL(
            "INSERT INTO bookmarks (bookId, locatorJson, createdAt, label) " +
                "VALUES ('book-1', '{\"href\":\"/ch1\"}', 12345, 'Chapter 1')"
        )
        val cursor = db.query("SELECT id, bookId, label FROM bookmarks")
        assertTrue(cursor.moveToFirst())
        assertEquals("book-1", cursor.getString(1))
        assertEquals("Chapter 1", cursor.getString(2))
        cursor.close()

        val indexCursor = db.query("PRAGMA index_list('bookmarks')")
        var hasIndex = false
        while (indexCursor.moveToNext()) {
            val name = indexCursor.getString(indexCursor.getColumnIndexOrThrow("name"))
            if (name == "index_bookmarks_bookId") hasIndex = true
        }
        indexCursor.close()
        assertTrue("Expected index_bookmarks_bookId to exist", hasIndex)
        db.close()
    }

    @Test
    fun migration4To5_createsAnnotationsTableAndIndex() {
        val db = createRawDatabase(4) { db ->
            db.execSQL("CREATE TABLE books (stableId TEXT NOT NULL PRIMARY KEY)")
        }

        LeaflineDatabase.MIGRATION_4_5.migrate(db)

        db.execSQL(
            "INSERT INTO annotations (bookId, locatorJson, colorHex, note, createdAt) " +
                "VALUES ('book-1', '{\"href\":\"/ch1\"}', '#FFFF00', 'Important thought', 54321)"
        )
        val cursor = db.query("SELECT id, bookId, colorHex, note FROM annotations")
        assertTrue(cursor.moveToFirst())
        assertEquals("book-1", cursor.getString(1))
        assertEquals("#FFFF00", cursor.getString(2))
        assertEquals("Important thought", cursor.getString(3))
        cursor.close()

        val indexCursor = db.query("PRAGMA index_list('annotations')")
        var hasIndex = false
        while (indexCursor.moveToNext()) {
            val name = indexCursor.getString(indexCursor.getColumnIndexOrThrow("name"))
            if (name == "index_annotations_bookId") hasIndex = true
        }
        indexCursor.close()
        assertTrue("Expected index_annotations_bookId to exist", hasIndex)
        db.close()
    }

    @Test
    fun migration5To6_addsKoreaderHashAndLastReadAtEpochMillis() {
        val db = createRawDatabase(5) { db ->
            db.execSQL(
                "CREATE TABLE books (" +
                    "stableId TEXT NOT NULL PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "filePath TEXT NOT NULL)"
            )
            db.execSQL(
                "INSERT INTO books (stableId, title, filePath) VALUES ('book-1', 'Title', '/p.epub')"
            )
        }

        LeaflineDatabase.MIGRATION_5_6.migrate(db)

        val cursor = db.query("SELECT koreaderHash, lastReadAtEpochMillis FROM books WHERE stableId = 'book-1'")
        assertTrue(cursor.moveToFirst())
        assertNull(cursor.getString(0))
        assertTrue(cursor.isNull(1))
        cursor.close()

        db.execSQL("UPDATE books SET koreaderHash = 'hash123', lastReadAtEpochMillis = 99999 WHERE stableId = 'book-1'")
        val updated = db.query("SELECT koreaderHash, lastReadAtEpochMillis FROM books WHERE stableId = 'book-1'")
        assertTrue(updated.moveToFirst())
        assertEquals("hash123", updated.getString(0))
        assertEquals(99999L, updated.getLong(1))
        updated.close()
        db.close()
    }

    @Test
    fun migration6To7_createsCollectionsAndCrossRef() {
        val db = createRawDatabase(6) { db ->
            db.execSQL("CREATE TABLE books (stableId TEXT NOT NULL PRIMARY KEY)")
        }

        LeaflineDatabase.MIGRATION_6_7.migrate(db)

        db.execSQL("INSERT INTO collections (name, createdAtEpochMillis) VALUES ('Favorites', 1000)")
        val colCursor = db.query("SELECT id, name FROM collections WHERE name = 'Favorites'")
        assertTrue(colCursor.moveToFirst())
        val collectionId = colCursor.getLong(0)
        assertEquals("Favorites", colCursor.getString(1))
        colCursor.close()

        db.execSQL(
            "INSERT INTO book_collection_cross_ref (collectionId, bookStableId) " +
                "VALUES ($collectionId, 'book-1')"
        )
        val refCursor = db.query("SELECT collectionId, bookStableId FROM book_collection_cross_ref")
        assertTrue(refCursor.moveToFirst())
        assertEquals(collectionId, refCursor.getLong(0))
        assertEquals("book-1", refCursor.getString(1))
        refCursor.close()
        db.close()
    }

    @Test
    fun migration7To8_addsReadingStatusColumnWithDefaultUnread() {
        val db = createRawDatabase(7) { db ->
            db.execSQL(
                "CREATE TABLE books (" +
                    "stableId TEXT NOT NULL PRIMARY KEY, " +
                    "title TEXT NOT NULL)"
            )
            db.execSQL("INSERT INTO books (stableId, title) VALUES ('book-1', 'Title')")
        }

        LeaflineDatabase.MIGRATION_7_8.migrate(db)

        val cursor = db.query("SELECT stableId, readingStatus FROM books WHERE stableId = 'book-1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("book-1", cursor.getString(0))
        assertEquals("unread", cursor.getString(1))
        cursor.close()
        db.close()
    }

    @Test
    fun migration8To9_createsReadingSessionsTableAndIndexes() {
        val db = createRawDatabase(8) { db ->
            db.execSQL("CREATE TABLE books (stableId TEXT NOT NULL PRIMARY KEY)")
        }

        LeaflineDatabase.MIGRATION_8_9.migrate(db)

        db.execSQL(
            "INSERT INTO reading_sessions (" +
                "bookId, startTimeEpochMillis, endTimeEpochMillis, activeDurationMillis, " +
                "startProgression, endProgression, startLocatorJson, endLocatorJson" +
                ") VALUES ('book-1', 1000, 2000, 1000, 0.1, 0.2, '{\"a\":1}', '{\"a\":2}')"
        )
        val cursor = db.query("SELECT bookId, activeDurationMillis, startProgression FROM reading_sessions")
        assertTrue(cursor.moveToFirst())
        assertEquals("book-1", cursor.getString(0))
        assertEquals(1000L, cursor.getLong(1))
        assertEquals(0.1, cursor.getDouble(2), 0.0001)
        cursor.close()

        val indexCursor = db.query("PRAGMA index_list('reading_sessions')")
        val indexNames = mutableSetOf<String>()
        while (indexCursor.moveToNext()) {
            indexNames.add(indexCursor.getString(indexCursor.getColumnIndexOrThrow("name")))
        }
        indexCursor.close()
        assertTrue(indexNames.contains("index_reading_sessions_bookId"))
        assertTrue(indexNames.contains("index_reading_sessions_startTimeEpochMillis"))
        db.close()
    }

    @Test
    fun migration9To10_addsRatingAndTagsColumns() {
        val db = createRawDatabase(9) { db ->
            db.execSQL(
                "CREATE TABLE books (" +
                    "stableId TEXT NOT NULL PRIMARY KEY, " +
                    "title TEXT NOT NULL)"
            )
            db.execSQL("INSERT INTO books (stableId, title) VALUES ('book-1', 'Title')")
        }

        LeaflineDatabase.MIGRATION_9_10.migrate(db)

        val cursor = db.query("SELECT stableId, rating, tags FROM books WHERE stableId = 'book-1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("book-1", cursor.getString(0))
        assertTrue(cursor.isNull(1))
        assertEquals("[]", cursor.getString(2))
        cursor.close()
        db.close()
    }

    @Test
    fun migration10To11_addsSeriesAndSeriesIndexColumns() {
        val db = createRawDatabase(10) { db ->
            db.execSQL(
                "CREATE TABLE books (" +
                    "stableId TEXT NOT NULL PRIMARY KEY, " +
                    "title TEXT NOT NULL)"
            )
            db.execSQL("INSERT INTO books (stableId, title) VALUES ('book-1', 'Title')")
        }

        LeaflineDatabase.MIGRATION_10_11.migrate(db)

        val cursor = db.query("SELECT stableId, series, seriesIndex FROM books WHERE stableId = 'book-1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("book-1", cursor.getString(0))
        assertNull(cursor.getString(1))
        assertTrue(cursor.isNull(2))
        cursor.close()

        db.execSQL("UPDATE books SET series = 'Dune Chronicles', seriesIndex = 1.0 WHERE stableId = 'book-1'")
        val updated = db.query("SELECT series, seriesIndex FROM books WHERE stableId = 'book-1'")
        assertTrue(updated.moveToFirst())
        assertEquals("Dune Chronicles", updated.getString(0))
        assertEquals(1.0f, updated.getFloat(1), 0.001f)
        updated.close()
        db.close()
    }

    @Test
    fun migration11To12_createsBookIndexes() {
        val db = createRawDatabase(11) { db ->
            db.execSQL(
                "CREATE TABLE books (" +
                    "stableId TEXT NOT NULL PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "fileHash TEXT NOT NULL, " +
                    "lastReadAtEpochMillis INTEGER, " +
                    "readingStatus TEXT NOT NULL DEFAULT 'unread')"
            )
            db.execSQL(
                "INSERT INTO books (stableId, title, fileHash, lastReadAtEpochMillis, readingStatus) " +
                    "VALUES ('book-1', 'Title', 'hash-1', 123456, 'reading')"
            )
        }

        LeaflineDatabase.MIGRATION_11_12.migrate(db)

        val indexCursor = db.query("PRAGMA index_list('books')")
        val indexNames = mutableSetOf<String>()
        while (indexCursor.moveToNext()) {
            indexNames.add(indexCursor.getString(indexCursor.getColumnIndexOrThrow("name")))
        }
        indexCursor.close()

        assertTrue(indexNames.contains("index_books_fileHash"))
        assertTrue(indexNames.contains("index_books_lastReadAtEpochMillis"))
        assertTrue(indexNames.contains("index_books_readingStatus"))

        val cursor = db.query("SELECT stableId FROM books WHERE fileHash = 'hash-1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("book-1", cursor.getString(0))
        cursor.close()
        db.close()
    }

    @Test
    fun fullMigrationChain1To12_preservesDataAndPassesRoomSchemaValidation() = runTest {
        val db = createRawDatabase(1) { db ->
            db.execSQL(
                "CREATE TABLE books (" +
                    "stableId TEXT NOT NULL PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "authors TEXT NOT NULL, " +
                    "language TEXT, " +
                    "description TEXT, " +
                    "publisher TEXT, " +
                    "publishedAtEpochMillis INTEGER, " +
                    "filePath TEXT NOT NULL, " +
                    "fileHash TEXT NOT NULL, " +
                    "addedAtEpochMillis INTEGER, " +
                    "pageCount INTEGER)"
            )
            db.execSQL(
                "INSERT INTO books (" +
                    "stableId, title, authors, language, description, publisher, " +
                    "publishedAtEpochMillis, filePath, fileHash, addedAtEpochMillis, pageCount" +
                    ") VALUES (" +
                    "'book-legacy', 'Legacy Novel', '[\"Jane Doe\",\"John Smith\"]', 'en', " +
                    "'A great book', 'Acme Publishing', 1609459200000, '/sdcard/legacy.epub', " +
                    "'hash-12345', 1609459300000, 320)"
            )
        }

        // Apply every migration in sequence from 1 up to 12
        LeaflineDatabase.ALL_MIGRATIONS.forEach { migration ->
            migration.migrate(db)
        }
        db.version = 12
        db.close()

        // Open with Room to verify that Room's internal schema validator accepts the migrated database!
        val roomDb = Room.databaseBuilder(context, LeaflineDatabase::class.java, testDbName)
            .addMigrations(*LeaflineDatabase.ALL_MIGRATIONS)
            .build()

        val book = roomDb.bookDao().getBookByStableId("book-legacy")
        assertNotNull(book)
        assertEquals("book-legacy", book!!.stableId)
        assertEquals("Legacy Novel", book.title)
        assertEquals(listOf("Jane Doe", "John Smith"), book.authors)
        assertEquals("en", book.language)
        assertEquals("Acme Publishing", book.publisher)
        assertEquals(320, book.pageCount)
        assertEquals("unread", book.readingStatus)
        assertEquals(emptyList<String>(), book.tags)
        assertNull(book.rating)
        assertNull(book.coverPath)
        assertNull(book.lastLocatorJson)
        assertNull(book.series)
        assertNull(book.seriesIndex)

        val byHash = roomDb.bookDao().getBookByFileHash("hash-12345")
        assertEquals("book-legacy", byHash?.stableId)

        // Verify setSeries updates the book
        roomDb.bookDao().setSeries("book-legacy", "The Foundation", 2.0f)
        val updatedBook = roomDb.bookDao().getBookByStableId("book-legacy")
        assertNotNull(updatedBook)
        assertEquals("The Foundation", updatedBook!!.series)
        assertEquals(2.0f, updatedBook.seriesIndex)

        // Verify bookmarkDao works on migrated DB
        val bookmarkId = roomDb.bookmarkDao().insert(
            BookmarkEntity(
                bookId = "book-legacy",
                locatorJson = "{\"href\":\"/text/ch01.xhtml\"}",
                createdAt = 1700000000000,
                label = "Bookmark 1"
            )
        )
        val bookmarks = roomDb.bookmarkDao().getForBook("book-legacy")
        assertEquals(1, bookmarks.size)
        assertEquals(bookmarkId, bookmarks[0].id)
        assertEquals("Bookmark 1", bookmarks[0].label)

        // Verify annotationDao works on migrated DB
        val annotationId = roomDb.annotationDao().insert(
            AnnotationEntity(
                bookId = "book-legacy",
                locatorJson = "{\"href\":\"/text/ch01.xhtml\"}",
                colorHex = "#FFEB3B",
                note = "Important quote",
                createdAt = 1700000001000
            )
        )
        val annotations = roomDb.annotationDao().getForBook("book-legacy")
        assertEquals(1, annotations.size)
        assertEquals(annotationId, annotations[0].id)
        assertEquals("#FFEB3B", annotations[0].colorHex)

        // Verify collectionDao works on migrated DB
        val collectionId = roomDb.collectionDao().insert(
            CollectionEntity(name = "Sci-Fi", createdAtEpochMillis = 1700000002000)
        )
        roomDb.collectionDao().addBookToCollection(
            BookCollectionCrossRef(collectionId = collectionId, bookStableId = "book-legacy")
        )
        val collections = roomDb.collectionDao().getCollectionsForBook("book-legacy")
        assertEquals(1, collections.size)
        assertEquals("Sci-Fi", collections[0].name)

        // Verify readingSessionDao works on migrated DB
        val sessionId = roomDb.readingSessionDao().insert(
            ReadingSessionEntity(
                bookId = "book-legacy",
                startTimeEpochMillis = 1700000003000,
                endTimeEpochMillis = 1700000004000,
                activeDurationMillis = 60000,
                startProgression = 0.05,
                endProgression = 0.15
            )
        )
        val sessions = roomDb.readingSessionDao().getForBook("book-legacy")
        assertEquals(1, sessions.size)
        assertEquals(sessionId, sessions[0].id)
        assertEquals(60000L, sessions[0].activeDurationMillis)

        roomDb.close()
    }
}
