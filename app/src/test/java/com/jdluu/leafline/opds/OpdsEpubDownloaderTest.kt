package com.jdluu.leafline.opds

import java.io.File
import java.net.ServerSocket
import java.nio.file.Files
import java.util.Base64
import kotlin.concurrent.thread
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpdsEpubDownloaderTest {
    @Test
    fun safeFileName_sanitizesAndAddsEpubExtension() {
        assertEquals("book.epub", OpdsEpubDownloader.safeFileName("book.epub"))
        assertEquals("book.epub", OpdsEpubDownloader.safeFileName("book"))
        assertEquals("cover.epub", OpdsEpubDownloader.safeFileName("../cover"))
        assertEquals("downloaded.epub", OpdsEpubDownloader.safeFileName("..."))
    }

    @Test
    fun download_sendsBasicAuthAndWritesResponseAtomically() {
        val payload = "epub fixture".toByteArray()
        val expectedAuth = "Basic " + Base64.getEncoder()
            .encodeToString("reader:secret".toByteArray())
        val server = ServerSocket(0)
        val worker = thread {
            server.use { socketServer ->
                socketServer.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    val request = buildList {
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            add(line)
                        }
                    }
                    val auth = request.firstOrNull { it.startsWith("Authorization:") }
                    val response = if (auth == "Authorization: $expectedAuth") {
                        "HTTP/1.1 200 OK\r\nContent-Length: ${payload.size}\r\n\r\n"
                    } else {
                        "HTTP/1.1 401 Unauthorized\r\nContent-Length: 0\r\n\r\n"
                    }
                    socket.getOutputStream().use { output ->
                        output.write(response.toByteArray())
                        if (auth == "Authorization: $expectedAuth") output.write(payload)
                    }
                }
            }
        }

        val directory = Files.createTempDirectory("leafline-download-test").toFile()
        try {
            val file = OpdsEpubDownloader(directory).download(
                OpdsServerConfig(catalogUrl = "http://localhost:${server.localPort}/api/v1/opds", username = "reader", password = "secret"),
                "http://localhost:${server.localPort}/books/sample.epub"
            )
            assertTrue(file.exists())
            assertArrayEquals(payload, file.readBytes())
            assertTrue(directory.listFiles()!!.none { it.extension == "part" })
        } finally {
            worker.join()
            directory.deleteRecursively()
        }
    }
}
