package com.jdluu.leafline.opds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpdsErrorMessageTest {

    @Test
    fun `401 explains that the catalog needs credentials`() {
        val message = opdsHttpErrorMessage(401, "")
        assertTrue(message.contains("needs a sign-in", ignoreCase = true))
        assertTrue(message.endsWith("(HTTP 401)"))
    }

    @Test
    fun `403 is treated as an authentication problem`() {
        val message = opdsHttpErrorMessage(403, "Forbidden")
        assertTrue(message.contains("needs a sign-in", ignoreCase = true))
    }

    @Test
    fun `404 mentions a moved feed`() {
        val message = opdsHttpErrorMessage(404, "")
        assertTrue(message.contains("may have moved", ignoreCase = true))
    }

    @Test
    fun `server errors ask the reader to retry later`() {
        val message = opdsHttpErrorMessage(503, "")
        assertTrue(message.contains("try again later", ignoreCase = true))
        assertTrue(message.endsWith("(HTTP 503)"))
    }

    @Test
    fun `a blank server message never yields a dangling colon`() {
        val message = opdsHttpErrorMessage(418, "")
        assertFalse(message.contains(": ("))
        assertTrue(message.contains("HTTP 418"))
    }

    @Test
    fun `a useful server message is preserved`() {
        assertEquals("Gone fishing (HTTP 410)", opdsHttpErrorMessage(410, "Gone fishing"))
    }

    @Test
    fun `the download action is prefixed and capitalised`() {
        val message = opdsHttpErrorMessage(404, "", "download failed")
        assertTrue(message.startsWith("Download failed: "))
    }
}
