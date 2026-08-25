package com.jdluu.leafline.sync

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KoreaderSyncClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: KoreaderSyncClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = KoreaderSyncClient()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun baseUrl(): String {
        return server.url("/api/koreader").toString().trimEnd('/')
    }

    private fun recordedPath(): String {
        return server.takeRequest().path.orEmpty()
    }

    @Test
    fun `auth returns true on 200`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        val result = client.auth(baseUrl(), "user", "pass")

        assertTrue(result)
        val request = server.takeRequest()
        assertEquals("/api/koreader/users/auth", request.path)
        // KOReader sync protocol: X-Auth-User + X-Auth-Key (md5 of the key phrase)
        val expectedMd5 = java.security.MessageDigest.getInstance("MD5")
            .digest("pass".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertEquals("user", request.getHeader("X-Auth-User"))
        assertEquals(expectedMd5, request.getHeader("X-Auth-Key"))
    }

    @Test
    fun `auth returns false on 401`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = client.auth(baseUrl(), "user", "wrong")

        assertFalse(result)
    }

    @Test
    fun `auth throws on unexpected status`() {
        server.enqueue(MockResponse().setResponseCode(500))

        var thrown: KoreaderSyncException? = null
        try {
            runBlocking { client.auth(baseUrl(), "user", "pass") }
        } catch (e: KoreaderSyncException) {
            thrown = e
        }

        assertEquals(500, thrown?.statusCode)
    }

    @Test
    fun `getProgress parses response fields`() = runBlocking {
        val body = """
            {"document": "abc123", "percentage": 42.5, "progress": "/body/ch2.xhtml",
             "device": "Pixel 7", "device_id": "uuid-1", "timestamp": 1724390400000}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val progress = client.getProgress(baseUrl(), "user", "pass", "abc123")

        assertEquals("abc123", progress?.document)
        assertEquals(42.5, progress?.percentage!!, 0.0001)
        assertEquals("/body/ch2.xhtml", progress?.progress)
        assertEquals("Pixel 7", progress?.device)
        assertEquals("uuid-1", progress?.deviceId)
        assertEquals(1724390400000L, progress?.timestamp)
        assertEquals("/api/koreader/syncs/progress/abc123", recordedPath())
    }

    @Test
    fun `getProgress returns null on 404`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        val progress = client.getProgress(baseUrl(), "user", "pass", "missing")

        assertNull(progress)
    }

    @Test
    fun `putProgress sends json body with expected fields`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        val push = KoreaderPushProgress(
            document = "hash-1",
            percentage = 33.25,
            progress = "{\"href\":\"/text/part1.xhtml\"}",
            device = "Leafline",
            deviceId = "device-uuid",
            timestamp = 1724390400123L
        )

        client.putProgress(baseUrl(), "user", "pass", push)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/koreader/syncs/progress", request.path)
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"))
        val sentBody = request.body.readUtf8()
        val parsed = com.google.gson.JsonParser.parseString(sentBody).asJsonObject
        assertEquals(1724390400123L, parsed.get("timestamp").asLong)
        assertEquals("hash-1", parsed.get("document").asString)
        assertEquals(33.25, parsed.get("percentage").asDouble, 0.0001)
        assertEquals("{\"href\":\"/text/part1.xhtml\"}", parsed.get("progress").asString)
        assertEquals("Leafline", parsed.get("device").asString)
        assertEquals("device-uuid", parsed.get("device_id").asString)
    }

    @Test
    fun `putProgress throws on error status`() {
        server.enqueue(MockResponse().setResponseCode(403))

        var thrown: KoreaderSyncException? = null
        try {
            runBlocking {
                client.putProgress(
                    baseUrl(),
                    "user",
                    "pass",
                    KoreaderPushProgress(
                        document = "hash-1",
                        percentage = 10.0,
                        progress = "p1",
                        device = "Leafline",
                        deviceId = "uuid",
                        timestamp = 1L
                    )
                )
            }
        } catch (e: KoreaderSyncException) {
            thrown = e
        }

        assertEquals(403, thrown?.statusCode)
    }

    @Test
    fun `parseRemoteProgress tolerates missing fields`() {
        val parser = KoreaderSyncClient()

        val progress = parser.parseRemoteProgress("""{"percentage": 7}""")

        assertNull(progress.document)
        assertEquals(7.0, progress.percentage!!, 0.0001)
        assertNull(progress.progress)
        assertNull(progress.timestamp)
    }
}
