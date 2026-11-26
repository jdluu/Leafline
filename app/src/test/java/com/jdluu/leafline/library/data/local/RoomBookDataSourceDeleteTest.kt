package com.jdluu.leafline.library.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdluu.leafline.library.LibraryBook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RoomBookDataSourceDeleteTest {

    private lateinit var database: LeaflineDatabase
    private lateinit var dataSource: RoomBookDataSource

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataSource = RoomBookDataSource(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun testBook(id: String) = LibraryBook(
        stableId = id,
        title = "Title $id",
        authors = listOf("Author"),
        language = "en",
        description = null,
        publisher = null,
        publishedAtEpochMillis = null,
        filePath = "/books/$id.epub",
        fileHash = "hash-$id",
        addedAtEpochMillis = 1_000L,
        pageCount = 100
    )

    @Test
    fun `delete removes book and all dependent rows while leaving other books intact`() = runTest {
        dataSource.insert(testBook("victim"))
        dataSource.insert(testBook("survivor"))

        database.bookmarkDao().insert(
            BookmarkEntity(bookId = "victim", locatorJson = """{"href":"/a.xhtml"}""", createdAt = 1L)
        )
        database.bookmarkDao().insert(
            BookmarkEntity(bookId = "survivor", locatorJson = """{"href":"/b.xhtml"}""", createdAt = 2L)
        )

        database.annotationDao().insert(
            AnnotationEntity(
                bookId = "victim",
                locatorJson = """{"href":"/a.xhtml"}""",
                colorHex = "#55E65100",
                note = "Note",
                createdAt = 1L
            )
        )
        database.annotationDao().insert(
            AnnotationEntity(
                bookId = "survivor",
                locatorJson = """{"href":"/b.xhtml"}""",
                colorHex = "#55E65100",
                note = "Keep",
                createdAt = 2L
            )
        )

        val collectionId = database.collectionDao().insert(
            CollectionEntity(name = "Favorites", createdAtEpochMillis = 10L)
        )
        database.collectionDao().addBookToCollection(BookCollectionCrossRef(collectionId, "victim"))
        database.collectionDao().addBookToCollection(BookCollectionCrossRef(collectionId, "survivor"))

        database.readingSessionDao().insert(
            ReadingSessionEntity(
                bookId = "victim",
                startTimeEpochMillis = 100L,
                endTimeEpochMillis = 200L,
                activeDurationMillis = 100L
            )
        )
        database.readingSessionDao().insert(
            ReadingSessionEntity(
                bookId = "survivor",
                startTimeEpochMillis = 300L,
                endTimeEpochMillis = 400L,
                activeDurationMillis = 100L
            )
        )

        dataSource.delete("victim")

        assertNull(dataSource.getBookByStableId("victim"))
        assertTrue(database.bookmarkDao().getForBook("victim").isEmpty())
        assertTrue(database.annotationDao().getForBook("victim").isEmpty())
        assertTrue(database.collectionDao().getCollectionsForBook("victim").isEmpty())
        assertTrue(database.readingSessionDao().getForBook("victim").isEmpty())

        assertEquals("survivor", dataSource.getBookByStableId("survivor")?.stableId)
        assertEquals(1, database.bookmarkDao().getForBook("survivor").size)
        assertEquals(1, database.annotationDao().getForBook("survivor").size)
        assertEquals(listOf("survivor"), database.collectionDao().getBookIdsForCollection(collectionId).first())
        assertEquals(1, database.readingSessionDao().getForBook("survivor").size)
    }
}
