package com.jdluu.leafline.library.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdluu.leafline.library.LibraryLayout
import com.jdluu.leafline.library.LibraryLayoutStore
import com.jdluu.leafline.library.LibrarySort
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.ReadingGoalStore
import com.jdluu.leafline.library.ReadingGoals
import com.jdluu.leafline.library.data.local.BookEntity
import com.jdluu.leafline.library.data.local.LeaflineDatabase
import com.jdluu.leafline.opds.OpdsFeedStore
import com.jdluu.leafline.opds.TestSecretCipher
import com.jdluu.leafline.reader.MinimalHudMode
import com.jdluu.leafline.reader.PageTurnAnimation
import com.jdluu.leafline.reader.ReaderOrientation
import com.jdluu.leafline.reader.ReaderPreferencesStore
import com.jdluu.leafline.reader.ReaderSettings
import com.jdluu.leafline.reader.TapZoneAction
import com.jdluu.leafline.reader.TapZoneConfig
import com.jdluu.leafline.reader.TapZoneLayout
import com.jdluu.leafline.reader.theme.ReaderTheme
import com.jdluu.leafline.theme.AppearanceStore
import com.jdluu.leafline.theme.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalReadiumApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LibraryBackupManagerTest {

    private lateinit var context: Context
    private lateinit var sourceDb: LeaflineDatabase
    private lateinit var targetDb: LeaflineDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sourceDb = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        targetDb = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        sourceDb.close()
        targetDb.close()
    }

    private fun freshPrefs(name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE).also {
            it.edit().clear().commit()
        }

    @Test
    fun `export omits opds credentials and restore preserves local feeds and settings`() = runTest {
        sourceDb.bookDao().insert(
            BookEntity(
                stableId = "book-series-1",
                title = "Foundation",
                authors = listOf("Isaac Asimov"),
                language = "en",
                description = null,
                publisher = null,
                publishedAtEpochMillis = null,
                filePath = "/books/foundation.epub",
                fileHash = "hash-1",
                addedAtEpochMillis = 1000L,
                pageCount = null,
                koreaderHash = "ko-abc-123",
                series = "Foundation",
                seriesIndex = 1f
            )
        )

        val srcAppearance = AppearanceStore(freshPrefs("src_app"))
        srcAppearance.save(ThemeMode.DARK)
        val srcLayout = LibraryLayoutStore(freshPrefs("src_layout"))
        srcLayout.save(LibraryLayout.LIST)
        val srcSort = LibrarySortStore(freshPrefs("src_sort"))
        srcSort.save(LibrarySort.AUTHOR)
        srcSort.saveAscending(false)
        val srcGoals = ReadingGoalStore(freshPrefs("src_goals"))
        srcGoals.saveGoals(ReadingGoals(dailyMinutes = 40, booksTarget = 20))
        val srcReaderPrefs = ReaderPreferencesStore(freshPrefs("src_reader"))
        srcReaderPrefs.save(
            ReaderSettings(
                epub = EpubPreferences(
                    fontFamily = FontFamily.SERIF,
                    fontSize = 1.4,
                    publisherStyles = false
                ),
                theme = ReaderTheme.SEPIA,
                preSepiaTheme = ReaderTheme.LIGHT,
                tapZoneConfig = TapZoneConfig(
                    leftZone = TapZoneAction.NEXT_PAGE,
                    centerZone = TapZoneAction.TOGGLE_MENU,
                    rightZone = TapZoneAction.PREVIOUS_PAGE,
                    layout = TapZoneLayout.RIGHT_HANDED
                ),
                pageTurnAnimation = PageTurnAnimation.NONE,
                orientation = ReaderOrientation.PORTRAIT,
                minimalHudMode = MinimalHudMode.TIME_REMAINING,
                invertVolumeKeys = true
            )
        )
        val srcOpds = OpdsFeedStore(freshPrefs("src_opds"), cipher = TestSecretCipher())
        srcOpds.addCustomFeed(
            title = "Private Library",
            url = "https://books.example.com/opds",
            username = "secret-user",
            password = "super-secret-password",
            authToken = "super-secret-token"
        )
        srcOpds.addCustomFeed(
            title = "Public Catalog",
            url = "https://public.example.org/opds"
        )

        val sourceManager = LibraryBackupManager(
            bookDao = sourceDb.bookDao(),
            collectionDao = sourceDb.collectionDao(),
            bookmarkDao = sourceDb.bookmarkDao(),
            annotationDao = sourceDb.annotationDao(),
            readingSessionDao = sourceDb.readingSessionDao(),
            appVersion = "0.4.20",
            appearanceStore = srcAppearance,
            libraryLayoutStore = srcLayout,
            librarySortStore = srcSort,
            readingGoalStore = srcGoals,
            readerPreferencesStore = srcReaderPrefs,
            opdsFeedStore = srcOpds
        )

        val out = ByteArrayOutputStream()
        sourceManager.exportBackup(out)
        val json = out.toString(Charsets.UTF_8.name())

        assertFalse("OPDS password must never be exported", json.contains("super-secret-password"))
        assertFalse("OPDS token must never be exported", json.contains("super-secret-token"))
        assertFalse("OPDS username must never be exported", json.contains("secret-user"))

        // Target has the same book missing series and koreaderHash, plus one pre-existing authenticated catalog
        targetDb.bookDao().insert(
            BookEntity(
                stableId = "book-series-1",
                title = "Foundation",
                authors = listOf("Isaac Asimov"),
                language = "en",
                description = null,
                publisher = null,
                publishedAtEpochMillis = null,
                filePath = "/books/foundation.epub",
                fileHash = "hash-1",
                addedAtEpochMillis = 2000L,
                pageCount = null,
                koreaderHash = null,
                series = null,
                seriesIndex = null
            )
        )

        val dstAppearance = AppearanceStore(freshPrefs("dst_app"))
        val dstLayout = LibraryLayoutStore(freshPrefs("dst_layout"))
        val dstSort = LibrarySortStore(freshPrefs("dst_sort"))
        val dstGoals = ReadingGoalStore(freshPrefs("dst_goals"))
        val dstReaderPrefs = ReaderPreferencesStore(freshPrefs("dst_reader"))
        val dstOpds = OpdsFeedStore(freshPrefs("dst_opds"), cipher = TestSecretCipher())
        dstOpds.addCustomFeed(
            title = "My Authed Server",
            url = "https://books.example.com/opds",
            username = "local-user",
            password = "local-password"
        )

        val targetManager = LibraryBackupManager(
            bookDao = targetDb.bookDao(),
            collectionDao = targetDb.collectionDao(),
            bookmarkDao = targetDb.bookmarkDao(),
            annotationDao = targetDb.annotationDao(),
            readingSessionDao = targetDb.readingSessionDao(),
            appVersion = "0.4.20",
            appearanceStore = dstAppearance,
            libraryLayoutStore = dstLayout,
            librarySortStore = dstSort,
            readingGoalStore = dstGoals,
            readerPreferencesStore = dstReaderPrefs,
            opdsFeedStore = dstOpds
        )

        val summary = targetManager.importBackup(json.byteInputStream(Charsets.UTF_8))

        assertTrue(summary.preferencesRestored)
        assertEquals(1, summary.opdsFeedsRestored)

        val restoredBook = targetDb.bookDao().getBookByStableId("book-series-1")!!
        assertEquals("Foundation", restoredBook.series)
        assertEquals(1f, restoredBook.seriesIndex!!, 0.001f)
        assertEquals("ko-abc-123", restoredBook.koreaderHash)

        assertEquals(ThemeMode.DARK, dstAppearance.load())
        assertEquals(LibraryLayout.LIST, dstLayout.load())
        assertEquals(LibrarySort.AUTHOR, dstSort.load())
        assertEquals(false, dstSort.loadAscending(true))
        assertEquals(ReadingGoals(dailyMinutes = 40, booksTarget = 20), dstGoals.loadGoals())

        val restoredReader = dstReaderPrefs.load()
        assertEquals(ReaderTheme.SEPIA, restoredReader.theme)
        assertEquals(FontFamily.SERIF, restoredReader.epub.fontFamily)
        assertEquals(1.4, restoredReader.epub.fontSize!!, 0.001)
        assertEquals(TapZoneLayout.RIGHT_HANDED, restoredReader.tapZoneConfig.layout)
        assertEquals(MinimalHudMode.TIME_REMAINING, restoredReader.minimalHudMode)
        assertEquals(true, restoredReader.invertVolumeKeys)

        val dstFeeds = dstOpds.loadFeeds()
        assertEquals(2, dstFeeds.size)
        val existingAuthed = dstFeeds.first { it.url == "https://books.example.com/opds" }
        assertEquals("local-user", existingAuthed.username)
        assertEquals("local-password", existingAuthed.password)
        assertEquals("https://public.example.org/opds", dstFeeds[1].url)
    }
}
