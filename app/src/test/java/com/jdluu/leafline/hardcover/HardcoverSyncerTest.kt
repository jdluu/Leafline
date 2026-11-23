package com.jdluu.leafline.hardcover

import com.jdluu.leafline.library.ReadingStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Records every call so the test can assert what was *not* sent. */
private class FakeHardcoverApi(
    private val edition: HardcoverEdition? = HardcoverEdition(editionId = 7, bookId = 19, pages = 284),
    private val existing: HardcoverRead? = null,
    private val failure: HardcoverException? = null
) : HardcoverApi {
    val calls = mutableListOf<String>()
    var lastProgressPages: Int? = null
    var lastStatusId: Int? = null

    private fun record(name: String) {
        calls += name
        failure?.let { throw it }
    }

    override suspend fun userId(token: String): Int { record("userId"); return 42 }

    override suspend fun editionByIsbn13(token: String, isbn13: String): HardcoverEdition? {
        record("editionByIsbn13")
        return edition
    }

    override suspend fun userBookRead(token: String, userId: Int, bookId: Int): HardcoverRead? {
        record("userBookRead")
        return existing
    }

    override suspend fun insertUserBook(token: String, bookId: Int, statusId: Int): HardcoverRead? {
        record("insertUserBook")
        lastStatusId = statusId
        return HardcoverRead(userBookId = 456, openReadId = 123, edition = edition)
    }

    override suspend fun updateStatus(token: String, userBookId: Int, statusId: Int) {
        record("updateStatus")
        lastStatusId = statusId
    }

    override suspend fun updateProgress(token: String, userBookReadId: Int, pages: Int) {
        record("updateProgress")
        lastProgressPages = pages
    }
}

class HardcoverSyncerTest {

    private val enabled = HardcoverConfig(token = "tok", enabled = true, userId = 42)

    private fun syncer(api: HardcoverApi, config: HardcoverConfig? = enabled) =
        HardcoverSyncer(api = api, config = { config }, accessToken = { config?.token })

    @Test
    fun `nothing is sent when the integration is off`() = runBlocking {
        val api = FakeHardcoverApi()

        val outcome = syncer(api, enabled.copy(enabled = false))
            .push("urn:isbn:9780141439471", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.Disabled, outcome)
        assertTrue("no request may be made while disabled: ${api.calls}", api.calls.isEmpty())
    }

    @Test
    fun `nothing is sent when unconfigured`() = runBlocking {
        val api = FakeHardcoverApi()

        val outcome = syncer(api, null).push("urn:isbn:9780141439471", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.Disabled, outcome)
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun `an enabled connection with no token sends nothing`() = runBlocking {
        val api = FakeHardcoverApi()
        val noToken = HardcoverConfig(enabled = true, authMethod = HardcoverAuthMethod.OAUTH)

        val outcome = HardcoverSyncer(
            api = api,
            config = { noToken },
            accessToken = { null }
        ).push("urn:isbn:9780141439471", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.Disabled, outcome)
        assertTrue("a token-less connection caused traffic: ${api.calls}", api.calls.isEmpty())
    }

    @Test
    fun `a refreshed token is used instead of the stored one`() = runBlocking {
        val api = FakeHardcoverApi()
        val stale = HardcoverConfig(
            token = "stale",
            enabled = true,
            authMethod = HardcoverAuthMethod.OAUTH,
            refreshToken = "rt",
            accessExpiresAtEpochMillis = 1L
        )

        val outcome = HardcoverSyncer(
            api = api,
            config = { stale },
            accessToken = { "fresh" }
        ).push("urn:isbn:9780141439471", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.Pushed, outcome)
    }

    @Test
    fun `a book without an isbn is skipped without a request`() = runBlocking {
        val api = FakeHardcoverApi()

        val outcome = syncer(api).push("http://www.gutenberg.org/1952", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.SkippedNoIsbn, outcome)
        assertTrue("unidentifiable book caused network traffic: ${api.calls}", api.calls.isEmpty())
    }

    @Test
    fun `a new book is added to the library then given progress`() = runBlocking {
        val api = FakeHardcoverApi(existing = null)

        val outcome = syncer(api).push("urn:isbn:9780141439471", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.Pushed, outcome)
        assertTrue("insertUserBook" in api.calls)
        assertEquals(2, api.lastStatusId) // Currently Reading
        assertEquals(142, api.lastProgressPages) // 0.5 * 284
    }

    @Test
    fun `an existing library entry is updated without being re-added`() = runBlocking {
        val api = FakeHardcoverApi(
            existing = HardcoverRead(
                userBookId = 456,
                openReadId = 789,
                edition = HardcoverEdition(editionId = 7, bookId = 19, pages = 200)
            )
        )

        val outcome = syncer(api).push("urn:isbn:9780141439471", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.Pushed, outcome)
        assertTrue("book was re-added: ${api.calls}", "insertUserBook" !in api.calls)
        assertTrue("status was not synced: ${api.calls}", "updateStatus" in api.calls)
        assertEquals(2, api.lastStatusId) // Currently Reading
        assertEquals(100, api.lastProgressPages)
    }

    /**
     * Setting status to Read makes Hardcover finish the read itself. Sending progress
     * afterwards would overwrite its full-page completion with a partial page, so the
     * finished case must send no progress at all.
     */
    @Test
    fun `a finished book is marked Read and its progress is left alone`() = runBlocking {
        val api = FakeHardcoverApi(
            existing = HardcoverRead(
                userBookId = 456,
                openReadId = 789,
                edition = HardcoverEdition(editionId = 7, bookId = 19, pages = 200)
            )
        )

        val outcome = syncer(api).push("urn:isbn:9780141439471", ReadingStatus.FINISHED, 0.5)

        assertEquals(HardcoverPushOutcome.FinishedRemote, outcome)
        assertEquals(3, api.lastStatusId) // Read
        assertTrue(
            "progress must not be written for a finished book: ${api.calls}",
            "updateProgress" !in api.calls
        )
    }

    @Test
    fun `finishing a book not yet in the library inserts it as Read without progress`() = runBlocking {
        val api = FakeHardcoverApi(existing = null)

        val outcome = syncer(api).push("urn:isbn:9780141439471", ReadingStatus.FINISHED, 1.0)

        assertEquals(HardcoverPushOutcome.FinishedRemote, outcome)
        assertEquals(3, api.lastStatusId)
        assertTrue("insert carries the Read status", "insertUserBook" in api.calls)
        assertTrue("no progress after finishing", api.lastProgressPages == null)
    }

    @Test
    fun `an edition without page count sends status but no progress`() = runBlocking {
        val api = FakeHardcoverApi(
            existing = HardcoverRead(
                userBookId = 456,
                openReadId = 789,
                edition = HardcoverEdition(editionId = 7, bookId = 19, pages = null)
            ),
            edition = HardcoverEdition(editionId = 7, bookId = 19, pages = null)
        )

        val outcome = syncer(api).push("urn:isbn:9780141439471", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.StatusOnly, outcome)
        assertTrue("a guessed page count was sent", "updateProgress" !in api.calls)
    }

    @Test
    fun `a rejected token is reported so the caller can switch it off`() = runBlocking {
        val api = FakeHardcoverApi(failure = HardcoverException("invalid_token", statusCode = 401))

        val outcome = syncer(api).push("urn:isbn:9780141439471", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.Unauthorized, outcome)
    }

    @Test
    fun `rate limiting is reported separately from failure`() = runBlocking {
        val api = FakeHardcoverApi(failure = HardcoverException("too many", statusCode = 429))

        val outcome = syncer(api).push("urn:isbn:9780141439471", ReadingStatus.READING, 0.5)

        assertEquals(HardcoverPushOutcome.RateLimited, outcome)
    }
}
