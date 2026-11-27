package com.jdluu.leafline.library.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryBackupSerializerTest {

    @Test
    fun `serialize and deserialize round trip preserves all library data`() {
        val backup = LibraryBackup(
            version = 1,
            exportedAt = 1700000000000L,
            appVersion = "1.0",
            books = listOf(
                BookBackup(
                    stableId = "book-1",
                    title = "The Time Machine",
                    authors = listOf("H.G. Wells"),
                    tags = listOf("Sci-Fi", "Classic"),
                    rating = 5,
                    readingStatus = "READING",
                    lastLocatorJson = """{"href":"chapter1.html","locations":{"progression":0.42}}"""
                ),
                BookBackup(
                    stableId = "book-2",
                    title = "War of the Worlds",
                    authors = listOf("H.G. Wells"),
                    tags = listOf("Sci-Fi"),
                    rating = null,
                    readingStatus = null,
                    lastLocatorJson = null
                )
            ),
            collections = listOf(
                CollectionBackup(
                    name = "Favorites",
                    bookStableIds = listOf("book-1")
                )
            ),
            bookmarks = listOf(
                BookmarkBackup(
                    bookStableId = "book-1",
                    locatorJson = """{"href":"ch1.html"}""",
                    label = "Chapter 1",
                    createdAt = 1700000050000L
                )
            ),
            annotations = listOf(
                AnnotationBackup(
                    bookStableId = "book-1",
                    locatorJson = """{"href":"ch1.html","text":{"exact":"Great quote"}}""",
                    colorHex = "#FFE082",
                    note = "Thought-provoking",
                    createdAt = 1700000060000L
                )
            )
        )

        val json = LibraryBackupSerializer.serialize(backup)
        assertNotNull(json)
        assert(json.contains("The Time Machine"))
        assert(json.contains("Favorites"))

        val restored = LibraryBackupSerializer.deserialize(json)
        assertEquals(1, restored.version)
        assertEquals(1700000000000L, restored.exportedAt)
        assertEquals("1.0", restored.appVersion)

        assertEquals(2, restored.books.size)
        val book1 = restored.books[0]
        assertEquals("book-1", book1.stableId)
        assertEquals("The Time Machine", book1.title)
        assertEquals(listOf("H.G. Wells"), book1.authors)
        assertEquals(listOf("Sci-Fi", "Classic"), book1.tags)
        assertEquals(5, book1.rating)
        assertEquals("READING", book1.readingStatus)
        assertEquals("""{"href":"chapter1.html","locations":{"progression":0.42}}""", book1.lastLocatorJson)

        assertEquals(1, restored.collections.size)
        assertEquals("Favorites", restored.collections[0].name)
        assertEquals(listOf("book-1"), restored.collections[0].bookStableIds)

        assertEquals(1, restored.bookmarks.size)
        assertEquals("book-1", restored.bookmarks[0].bookStableId)
        assertEquals("Chapter 1", restored.bookmarks[0].label)

        assertEquals(1, restored.annotations.size)
        assertEquals("book-1", restored.annotations[0].bookStableId)
        assertEquals("#FFE082", restored.annotations[0].colorHex)
        assertEquals("Thought-provoking", restored.annotations[0].note)
    }

    @Test
    fun `deserialize handles empty or missing sections gracefully`() {
        val emptyJson = """{"version":1,"books":[],"collections":[]}"""
        val backup = LibraryBackupSerializer.deserialize(emptyJson)
        assertEquals(1, backup.version)
        assertEquals(0, backup.books.size)
        assertEquals(0, backup.collections.size)
        assertEquals(0, backup.bookmarks.size)
        assertEquals(0, backup.annotations.size)
    }

    @Test
    fun `per-book hardcover choices survive a round trip`() {
        val backup = LibraryBackup(
            version = 2,
            books = listOf(BookBackup(stableId = "book-1", title = "T", authors = emptyList())),
            hardcoverTracking = mapOf(
                "book-1" to "IGNORE",
                "urn:isbn:9780141439471" to "TRACK"
            )
        )

        val restored = LibraryBackupSerializer.deserialize(LibraryBackupSerializer.serialize(backup))

        assertEquals(
            mapOf("book-1" to "IGNORE", "urn:isbn:9780141439471" to "TRACK"),
            restored.hardcoverTracking
        )
    }

    @Test
    fun `a version 1 file with no choices still loads`() {
        val v1 = """
            {"version":1,"exportedAt":1700000000000,"appVersion":"1.0",
             "books":[{"stableId":"book-1","title":"T","authors":[]}],
             "collections":[],"bookmarks":[],"annotations":[]}
        """.trimIndent()

        val restored = LibraryBackupSerializer.deserialize(v1)

        assertEquals(1, restored.version)
        assertEquals(1, restored.books.size)
        assertTrue(
            "a v1 file must not invent choices",
            restored.hardcoverTracking.isEmpty()
        )
    }

    @Test
    fun `reading sessions survive a round trip`() {
        val backup = LibraryBackup(
            version = 3,
            books = listOf(BookBackup(stableId = "book-1", title = "T", authors = emptyList())),
            readingSessions = listOf(
                ReadingSessionBackup(
                    bookStableId = "book-1",
                    startTimeEpochMillis = 1_700_000_000_000L,
                    endTimeEpochMillis = 1_700_000_600_000L,
                    activeDurationMillis = 540_000L,
                    startProgression = 0.1,
                    endProgression = 0.2,
                    startLocatorJson = "{\"a\":1}",
                    endLocatorJson = "{\"a\":2}"
                )
            )
        )

        val restored = LibraryBackupSerializer.deserialize(LibraryBackupSerializer.serialize(backup))

        val session = restored.readingSessions.single()
        assertEquals("book-1", session.bookStableId)
        assertEquals(1_700_000_000_000L, session.startTimeEpochMillis)
        assertEquals(1_700_000_600_000L, session.endTimeEpochMillis)
        assertEquals(540_000L, session.activeDurationMillis)
        assertEquals(0.2, session.endProgression!!, 0.0001)
        assertEquals("{\"a\":2}", session.endLocatorJson)
    }

    @Test
    fun `a version 2 file with no sessions still loads`() {
        val v2 = """
            {"version":2,"exportedAt":1700000000000,"appVersion":"1.0",
             "books":[{"stableId":"b","title":"T","authors":[]}],
             "collections":[],"bookmarks":[],"annotations":[],
             "hardcoverTracking":{"b":"IGNORE"}}
        """.trimIndent()

        val restored = LibraryBackupSerializer.deserialize(v2)

        assertTrue("a v2 file must not invent sessions", restored.readingSessions.isEmpty())
        assertEquals(mapOf("b" to "IGNORE"), restored.hardcoverTracking)
    }

    /**
     * The merge rule: identity is (book, start, end), so importing the same file twice
     * collapses to one session and the reader's hours are not double counted.
     */
    @Test
    fun `session identity deduplicates a repeated import`() {
        val a = ReadingSessionBackup("book-1", 100L, 200L, 90_000L)
        val sameAgain = ReadingSessionBackup("book-1", 100L, 200L, 90_000L)
        val differentDuration = ReadingSessionBackup("book-1", 100L, 200L, 1L)
        val laterStart = ReadingSessionBackup("book-1", 101L, 200L, 90_000L)
        val otherBook = ReadingSessionBackup("book-2", 100L, 200L, 90_000L)

        val identities = listOf(a, sameAgain, differentDuration, laterStart, otherBook)
            .map { it.identity }
            .toSet()

        assertEquals(
            "only book, start and end distinguish sessions; duration must not resurrect one",
            3,
            identities.size
        )
    }

    @Test
    fun `version 4 preserves series koreaderHash and non-secret preferences`() {
        val backup = LibraryBackup(
            version = 4,
            books = listOf(
                BookBackup(
                    stableId = "book-1",
                    title = "Dune",
                    authors = listOf("Frank Herbert"),
                    series = "Dune Chronicles",
                    seriesIndex = 1.5f,
                    koreaderHash = "deadbeef1234"
                )
            ),
            preferences = BackupPreferences(
                appearanceMode = "DARK",
                libraryLayout = "LIST",
                librarySort = "TITLE",
                librarySortAscending = false,
                dailyGoalMinutes = 35,
                booksGoalTarget = 24,
                opdsFeeds = listOf(
                    OpdsFeedBackup(
                        id = "feed-1",
                        title = "Local Calibre",
                        url = "https://catalog.example.org/opds",
                        description = "My catalog"
                    )
                ),
                readerPreferences = ReaderPreferencesBackup(
                    theme = "SEPIA",
                    fontFamily = "Serif",
                    fontSize = 1.3,
                    lineHeight = 1.5,
                    pageMargins = 1.0,
                    publisherStyles = false,
                    scroll = true,
                    hyphens = false,
                    columnCount = "TWO",
                    tapZoneLeft = "NEXT_PAGE",
                    tapZoneCenter = "TOGGLE_MENU",
                    tapZoneRight = "PREVIOUS_PAGE",
                    tapZoneLayout = "RIGHT_HANDED",
                    pageTurnAnimation = "NONE",
                    brightness = 0.6f,
                    preSepiaTheme = "LIGHT",
                    reduceMotion = true,
                    previewFootnotes = false,
                    keepScreenOn = false,
                    orientation = "PORTRAIT",
                    showMinimalHud = true,
                    minimalHudMode = "TIME_REMAINING",
                    hapticFeedback = true,
                    volumeKeyNavigation = false,
                    invertVolumeKeys = true
                )
            )
        )

        val json = LibraryBackupSerializer.serialize(backup)
        val restored = LibraryBackupSerializer.deserialize(json)

        assertEquals(4, restored.version)
        val book = restored.books.single()
        assertEquals("Dune Chronicles", book.series)
        assertEquals(1.5f, book.seriesIndex!!, 0.001f)
        assertEquals("deadbeef1234", book.koreaderHash)

        val prefs = restored.preferences!!
        assertEquals("DARK", prefs.appearanceMode)
        assertEquals("LIST", prefs.libraryLayout)
        assertEquals("TITLE", prefs.librarySort)
        assertEquals(false, prefs.librarySortAscending)
        assertEquals(35, prefs.dailyGoalMinutes)
        assertEquals(24, prefs.booksGoalTarget)
        assertEquals(1, prefs.opdsFeeds.size)
        assertEquals("https://catalog.example.org/opds", prefs.opdsFeeds.single().url)

        val reader = prefs.readerPreferences!!
        assertEquals("SEPIA", reader.theme)
        assertEquals("Serif", reader.fontFamily)
        assertEquals(1.3, reader.fontSize!!, 0.001)
        assertEquals("RIGHT_HANDED", reader.tapZoneLayout)
        assertEquals("TIME_REMAINING", reader.minimalHudMode)
        assertEquals(true, reader.invertVolumeKeys)
    }

    @Test
    fun `version 3 file without preferences deserializes cleanly with null preferences`() {
        val v3 = """
            {"version":3,"exportedAt":1700000000000,"appVersion":"0.4.20",
             "books":[{"stableId":"b","title":"T","authors":[]}],
             "collections":[],"bookmarks":[],"annotations":[]}
        """.trimIndent()

        val restored = LibraryBackupSerializer.deserialize(v3)
        assertEquals(3, restored.version)
        assertEquals(null, restored.preferences)
        assertEquals(null, restored.books.single().series)
        assertEquals(null, restored.books.single().koreaderHash)
    }
}
