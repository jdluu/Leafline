package com.jdluu.leafline.opds

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OpdsClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OpdsClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OpdsClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fetchFeed_transmitsAuthorizationHeaderWhenProvided() = runBlocking {
        val sampleFeedXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <id>urn:catalog</id>
              <title>Authenticated Feed</title>
              <updated>2026-11-15T12:00:00Z</updated>
            </feed>
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/atom+xml")
                .setBody(sampleFeedXml)
        )

        val feedUrl = server.url("/opds").toString()
        val result = client.fetchFeed(feedUrl, authHeader = "Basic dXNlcjpwYXNz")

        assertTrue(result.isSuccess)
        val recordedRequest = server.takeRequest()
        assertEquals("Basic dXNlcjpwYXNz", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun searchFeed_substitutesQueryAndAttachesAuth() = runBlocking {
        val sampleSearchXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <id>urn:search</id>
              <title>Search Results</title>
              <updated>2026-11-15T12:00:00Z</updated>
            </feed>
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/atom+xml")
                .setBody(sampleSearchXml)
        )

        val searchTemplate = "${server.url("/search")}?q={searchTerms}"
        val result = client.searchFeed(searchTemplate, "Frankenstein Mary", authHeader = "Bearer secret-token")

        assertTrue(result.isSuccess)
        val recordedRequest = server.takeRequest()
        assertEquals("/search?q=Frankenstein+Mary", recordedRequest.path)
        assertEquals("Bearer secret-token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun searchFeed_resolvesOpenSearchDescriptionXml() = runBlocking {
        val openSearchXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
              <ShortName>Catalog Search</ShortName>
              <Url type="application/atom+xml" template="${server.url("/opds/find")}?query={searchTerms}"/>
            </OpenSearchDescription>
        """.trimIndent()

        val searchResultXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <id>urn:results</id>
              <title>Query Results</title>
              <updated>2026-11-15T12:00:00Z</updated>
            </feed>
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/opensearchdescription+xml")
                .setBody(openSearchXml)
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/atom+xml")
                .setBody(searchResultXml)
        )

        val openSearchUrl = server.url("/opensearch.xml").toString()
        val result = client.searchFeed(openSearchUrl, "Dracula", authHeader = "Basic dXNlcjpwYXNz")

        assertTrue(result.isSuccess)

        val firstReq = server.takeRequest()
        assertEquals("/opensearch.xml", firstReq.path)
        assertEquals("Basic dXNlcjpwYXNz", firstReq.getHeader("Authorization"))

        val secondReq = server.takeRequest()
        assertEquals("/opds/find?query=Dracula", secondReq.path)
        assertEquals("Basic dXNlcjpwYXNz", secondReq.getHeader("Authorization"))
    }

    @Test
    fun downloadEpubFile_attachesAuthAndSavesFile() = runBlocking {
        val fakeEpubData = "PK\u0003\u0004fake-epub-binary-data"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/epub+zip")
                .setBody(fakeEpubData)
        )

        val tempDir = File.createTempFile("test_dest", "").also { it.delete(); it.mkdirs() }
        val targetFile = File(tempDir, "downloaded.epub")

        try {
            val downloadUrl = server.url("/books/book.epub").toString()
            val result = client.downloadEpubFile(downloadUrl, targetFile, authHeader = "Basic dXNlcjpwYXNz")

            assertTrue(result.isSuccess)
            val recordedReq = server.takeRequest()
            assertEquals("Basic dXNlcjpwYXNz", recordedReq.getHeader("Authorization"))
            assertTrue(targetFile.exists())
            assertEquals(fakeEpubData, targetFile.readText())
        } finally {
            targetFile.delete()
            tempDir.delete()
        }
    }

    @Test
    fun fetchFeed_coroutineCancellationCancelsInFlightRequestImmediately() = runBlocking {
        val releaseServer = java.util.concurrent.CountDownLatch(1)
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                releaseServer.await(10, java.util.concurrent.TimeUnit.SECONDS)
                return MockResponse().setResponseCode(200).setBody("<feed/>")
            }
        }

        try {
            var failureCaught: Throwable? = null
            val elapsedMs = kotlin.system.measureTimeMillis {
                val job = launch(Dispatchers.Default) {
                    try {
                        client.fetchFeed(server.url("/slow").toString())
                    } catch (e: Throwable) {
                        failureCaught = e
                        throw e
                    }
                }
                delay(50L)
                job.cancel()
                job.join()
            }

            assertTrue(
                "Expected cancellation to abort quickly without waiting 10s, took ${elapsedMs}ms",
                elapsedMs < 3000L
            )
            assertTrue(failureCaught is CancellationException)
        } finally {
            releaseServer.countDown()
        }
    }

    @Test
    fun downloadEpubFile_coroutineCancellationCleansUpTempFile() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/epub+zip")
                .setBody("PK\u0003\u0004" + "A".repeat(1024))
                .throttleBody(64, 500, java.util.concurrent.TimeUnit.MILLISECONDS)
        )

        val tempDir = File.createTempFile("test_cancel_dl", "").also { it.delete(); it.mkdirs() }
        val targetFile = File(tempDir, "cancelled.epub")
        val tempFile = File(tempDir, "cancelled.epub.downloading")

        try {
            var caught: Throwable? = null
            val job = launch(Dispatchers.Default) {
                try {
                    client.downloadEpubFile(server.url("/slow.epub").toString(), targetFile)
                } catch (e: Throwable) {
                    caught = e
                    throw e
                }
            }
            delay(100L)
            job.cancel()
            job.join()

            assertTrue(caught is CancellationException)
            assertTrue(!targetFile.exists())
            assertTrue(!tempFile.exists())
        } finally {
            targetFile.delete()
            tempFile.delete()
            tempDir.delete()
        }
    }
}
