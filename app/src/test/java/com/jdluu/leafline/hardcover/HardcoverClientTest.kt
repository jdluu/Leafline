package com.jdluu.leafline.hardcover

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HardcoverClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client() = HardcoverClient(
        baseUrl = server.url("/v1/graphql").toString(),
        userAgent = "Leafline/test"
    )

    @Test
    fun `me returns the user id and identifies itself`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"me":[{"id":42}]}}"""))

        assertEquals(42, client().userId("tok"))

        val sent = server.takeRequest()
        assertEquals("Bearer tok", sent.getHeader("authorization"))
        assertTrue(
            "Hardcover asks callers to identify themselves",
            sent.getHeader("user-agent")!!.startsWith("Leafline/")
        )
    }

    @Test
    fun `a 401 raises an unauthorized failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"invalid_token"}"""))

        val failure = runCatching { client().userId("bad") }.exceptionOrNull()

        assertTrue("expected HardcoverException, got $failure", failure is HardcoverException)
        assertTrue((failure as HardcoverException).isUnauthorized)
    }

    @Test
    fun `a 429 is reported as rate limited`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429))

        val failure = runCatching { client().userId("tok") }.exceptionOrNull()

        assertTrue(failure is HardcoverException)
        assertTrue((failure as HardcoverException).isRateLimited)
    }

    @Test
    fun `an edition lookup with no rows returns null`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"editions":[]}}"""))

        assertNull(client().editionByIsbn13("tok", "9780141439471"))
    }

    @Test
    fun `an edition lookup returns the book id and page count`() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{"data":{"editions":[{"id":7,"book_id":19,"pages":284}]}}""")
        )

        val edition = client().editionByIsbn13("tok", "9780141439471")!!

        assertEquals(7, edition.editionId)
        assertEquals(19, edition.bookId)
        assertEquals(284, edition.pages)
    }

    @Test
    fun `an edition without pages reports null rather than zero`() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{"data":{"editions":[{"id":7,"book_id":19,"pages":null}]}}""")
        )

        assertNull(client().editionByIsbn13("tok", "9780141439471")!!.pages)
    }

    @Test
    fun `a library entry with an open read is parsed`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"data":{"user_books":[{"id":456,"status_id":2,"user_book_reads":[{"id":123,"edition":{"pages":284}}]}]}}"""
            )
        )

        val read = client().userBookRead("tok", userId = 42, bookId = 19)!!

        assertEquals(456, read.userBookId)
        assertEquals(123, read.openReadId)
        assertEquals(284, read.edition!!.pages)
    }

    @Test
    fun `a book that is not in the library returns null`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"user_books":[]}}"""))

        assertNull(client().userBookRead("tok", userId = 42, bookId = 19))
    }

    @Test
    fun `an error payload without a status code is a failure, not a silent success`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"errors":[{"message":"field not found"}]}"""))

        val failure = runCatching { client().userId("tok") }.exceptionOrNull()

        assertTrue("a GraphQL error must not be swallowed: $failure", failure is HardcoverException)
    }
}
