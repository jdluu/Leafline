package com.jdluu.leafline.sync

import com.jdluu.leafline.FileHashUtil
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.InMemoryBookDataSource
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.library.data.LibraryRepositoryImpl
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReadingProgressMathTest {

    @Test
    fun `totalProgression maps to percentage`() {
        val locator = """{"href":"/a.xhtml","locations":{"totalProgression":0.4235}}"""

        assertEquals(42.35, ReadingProgressMath.percentageFromLocator(locator)!!, 0.0001)
    }

    @Test
    fun `progression is the fallback when totalProgression missing`() {
        val locator = """{"locations":{"progression":0.75}}"""

        assertEquals(75.0, ReadingProgressMath.percentageFromLocator(locator)!!, 0.0001)
    }

    @Test
    fun `values outside zero to one are clamped`() {
        val locator = """{"locations":{"progression":1.8}}"""

        assertEquals(100.0, ReadingProgressMath.percentageFromLocator(locator)!!, 0.0001)
    }

    @Test
    fun `invalid or empty payloads return null`() {
        assertNull(ReadingProgressMath.percentageFromLocator(null))
        assertNull(ReadingProgressMath.percentageFromLocator(""))
        assertNull(ReadingProgressMath.percentageFromLocator("not json"))
        assertNull(ReadingProgressMath.percentageFromLocator("""{"href":"/a.xhtml"}"""))
    }
}

class ProgressSyncerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeApi : KoreaderSyncApi {
        var stored: KoreaderPushProgress? = null
        val remoteForHash: MutableMap<String, KoreaderRemoteProgress?> = mutableMapOf()
        var failWith: Exception? = null

        override suspend fun auth(serverUrl: String, username: String, password: String): Boolean =
            true

        override suspend fun getProgress(
            serverUrl: String,
            username: String,
            password: String,
            bookHash: String
        ): KoreaderRemoteProgress? {
            failWith?.let { throw it }
            return remoteForHash[bookHash]
        }

        override suspend fun putProgress(
            serverUrl: String,
            username: String,
            password: String,
            progress: KoreaderPushProgress
        ) {
            failWith?.let { throw it }
            stored = progress
        }
    }

    private lateinit var repository: LibraryRepository
    private lateinit var api: FakeApi
    private lateinit var bookFile: File
    private var config: KoreaderSyncConfig? = null
    private var nowMillis: Long = 10_000L

    @Before
    fun setup() {
        bookFile = tempFolder.newFile("book.epub")
        bookFile.writeBytes(ByteArray(2048) { (it % 251).toByte() })
        repository = LibraryRepositoryImpl(InMemoryBookDataSource())
        api = FakeApi()
        config = enabledConfig()
        nowMillis = 10_000L
    }

    private fun enabledConfig() = KoreaderSyncConfig(
        serverUrl = "http://server/api/koreader",
        username = "user",
        password = "pass",
        enabled = true
    )

    private fun syncer(): ProgressSyncer {
        return ProgressSyncer(
            api = api,
            configSource = { config },
            repository = repository,
            clock = { nowMillis },
            deviceName = "Test Device",
            deviceId = "device-uuid"
        )
    }

    private fun bookRef(
        koreaderHash: String? = "computed-hash",
        lastReadAt: Long? = 1_724_390_400_123L
    ): BookRef {
        return BookRef(
            stableId = "book-1",
            filePath = bookFile.absolutePath,
            koreaderHash = koreaderHash,
            lastReadAtEpochMillis = lastReadAt,
            lastLocatorJson = """{"locations":{"totalProgression":0.2}}"""
        )
    }

    private fun libraryBook(koreaderHash: String?): LibraryBook {
        return LibraryBook(
            stableId = "book-1",
            title = "T",
            authors = emptyList(),
            language = null,
            description = null,
            publisher = null,
            publishedAtEpochMillis = null,
            filePath = bookFile.absolutePath,
            fileHash = "sha256",
            addedAtEpochMillis = 1_000L,
            pageCount = null,
            koreaderHash = koreaderHash
        )
    }

    private fun remoteProgress(
        percentage: Double,
        timestamp: Long?,
        progress: String = "{}"
    ) = KoreaderRemoteProgress(
        document = "computed-hash",
        percentage = percentage,
        progress = progress,
        device = "KOReader",
        deviceId = "other-device",
        timestamp = timestamp
    )

    @Test
    fun `push sends locator percentage and document hash`() = runTest {
        repository.addBook(libraryBook(koreaderHash = "computed-hash"))

        val outcome = syncer().push(bookRef(), """{"locations":{"totalProgression":0.42}}""")

        assertTrue(outcome is PushOutcome.Pushed)
        val sent = api.stored!!
        assertEquals("computed-hash", sent.document)
        assertEquals(42.0, sent.percentage, 0.0001)
        assertEquals("Test Device", sent.device)
        assertEquals("device-uuid", sent.deviceId)
        assertEquals(nowMillis, sent.timestamp)
    }

    @Test
    fun `push computes and persists hash lazily`() = runTest {
        val expectedHash = FileHashUtil.koreaderHash(bookFile)
        repository.addBook(libraryBook(koreaderHash = null))

        val outcome = syncer().push(bookRef(koreaderHash = null), """{"locations":{"totalProgression":0.1}}""")

        assertTrue(outcome is PushOutcome.Pushed)
        assertEquals(expectedHash, api.stored?.document)
        assertEquals(expectedHash, repository.getBookByStableId("book-1")?.koreaderHash)
    }

    @Test
    fun `push skips when disabled or percentage unknown`() = runTest {
        repository.addBook(libraryBook(koreaderHash = "computed-hash"))
        config = enabledConfig().copy(enabled = false)

        assertEquals(
            PushOutcome.Disabled,
            syncer().push(bookRef(), """{"locations":{"totalProgression":0.5}}""")
        )

        config = enabledConfig()
        assertEquals(PushOutcome.Skipped, syncer().push(bookRef(), "not a locator"))
        assertEquals(PushOutcome.Skipped, syncer().push(bookRef(), null))
    }

    @Test
    fun `push reports failure from client`() = runTest {
        repository.addBook(libraryBook(koreaderHash = "computed-hash"))
        api.failWith = RuntimeException("offline")

        val outcome = syncer().push(bookRef(), """{"locations":{"totalProgression":0.3}}""")

        assertTrue(outcome is PushOutcome.Failure)
        assertEquals("offline", (outcome as PushOutcome.Failure).message)
    }

    @Test
    fun `pull offers jump when remote timestamp newer`() = runTest {
        repository.addBook(libraryBook(koreaderHash = "computed-hash"))
        api.remoteForHash["computed-hash"] = remoteProgress(
            percentage = 80.0,
            // seconds scale, newer than local last read
            timestamp = 1_724_400_000L
        )

        val outcome = syncer().pull(bookRef())

        assertTrue(outcome is PullOutcome.RemoteAhead)
        assertEquals(80.0, (outcome as PullOutcome.RemoteAhead).remotePercentage!!, 0.0001)
    }

    @Test
    fun `pull stays quiet when remote is older`() = runTest {
        repository.addBook(libraryBook(koreaderHash = "computed-hash"))
        api.remoteForHash["computed-hash"] = remoteProgress(
            percentage = 80.0,
            // milliseconds scale but older than local last read
            timestamp = 1_724_300_000_000L
        )

        val outcome = syncer().pull(bookRef())

        assertEquals(PullOutcome.UpToDate, outcome)
    }

    @Test
    fun `pull falls back to percentage when timestamp missing`() = runTest {
        repository.addBook(libraryBook(koreaderHash = "computed-hash"))

        api.remoteForHash["computed-hash"] = remoteProgress(percentage = 50.0, timestamp = null)
        assertTrue(syncer().pull(bookRef()) is PullOutcome.RemoteAhead)

        api.remoteForHash["computed-hash"] = remoteProgress(percentage = 5.0, timestamp = null)
        assertEquals(PullOutcome.UpToDate, syncer().pull(bookRef()))
    }

    @Test
    fun `pull handles no remote progress and disabled states`() = runTest {
        repository.addBook(libraryBook(koreaderHash = "computed-hash"))

        assertEquals(PullOutcome.NoRemoteProgress, syncer().pull(bookRef()))

        config = null
        assertEquals(PullOutcome.Disabled, syncer().pull(bookRef()))
    }

    @Test
    fun `pull without local history treats any timestamp as newer`() = runTest {
        repository.addBook(libraryBook(koreaderHash = "computed-hash"))
        api.remoteForHash["computed-hash"] = remoteProgress(percentage = 1.0, timestamp = 1L)

        val outcome = syncer().pull(bookRef(lastReadAt = null))

        assertTrue(outcome is PullOutcome.RemoteAhead)
    }

    @Test
    fun `timestamp normalization keeps scales consistent`() {
        assertEquals(1_720_000_000_000L, normalizeTimestampToMillis(1_720_000_000L))
        assertEquals(1_724_390_400_123L, normalizeTimestampToMillis(1_724_390_400_123L))
    }

    @Test
    fun `isRemoteNewer compares normalized timestamps`() {
        val syncer = ProgressSyncer(
            api = api,
            configSource = { config },
            repository = repository,
            clock = { 0L },
            deviceName = "d",
            deviceId = "id"
        )

        val remoteSeconds = KoreaderRemoteProgress(
            document = null,
            percentage = null,
            progress = null,
            device = null,
            deviceId = null,
            timestamp = 2L
        )
        assertTrue(syncer.isRemoteNewer(remoteSeconds, 1_500L, null))
        assertFalse(syncer.isRemoteNewer(remoteSeconds, 2_500L, null))
    }
}
