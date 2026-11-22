package com.jdluu.leafline.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The settings row compresses sync state into one line. These cases cover the
 * states the row can actually be in, including the values a reader is likely to
 * type by hand.
 */
class SyncSummaryTest {

    private fun config(
        serverUrl: String = "http://server:6061/api/koreader",
        enabled: Boolean = true
    ) = KoreaderSyncConfig(
        serverUrl = serverUrl,
        username = "reader",
        password = "secret",
        enabled = enabled
    )

    @Test
    fun noConfigReadsAsNotSetUp() {
        assertEquals("Not set up", koreaderSyncSummary(null))
    }

    @Test
    fun blankUrlReadsAsNotSetUpEvenWhenEnabled() {
        assertEquals("Not set up", koreaderSyncSummary(config(serverUrl = "   ", enabled = true)))
    }

    @Test
    fun enabledConfigShowsTheHost() {
        assertEquals("On \u00B7 server", koreaderSyncSummary(config()))
    }

    @Test
    fun disabledConfigSaysTheSettingIsSaved() {
        assertEquals(
            "Off \u00B7 server saved",
            koreaderSyncSummary(config(enabled = false))
        )
    }

    @Test
    fun wwwPrefixIsDropped() {
        assertEquals(
            "On \u00B7 calibre.example.org",
            koreaderSyncSummary(config(serverUrl = "https://www.calibre.example.org/api/koreader"))
        )
    }

    @Test
    fun urlWithoutSchemeFallsBackToTheTypedValue() {
        assertEquals(
            "On \u00B7 server.local:6061",
            koreaderSyncSummary(config(serverUrl = "server.local:6061"))
        )
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        assertEquals(
            "On \u00B7 server",
            koreaderSyncSummary(config(serverUrl = "  http://server:6061/api/koreader "))
        )
    }

    @Test
    fun absoluteHttpAndHttpsUrlsAreValid() {
        assertTrue(isKoreaderServerUrlValid("http://server:6061/api/koreader"))
        assertTrue(isKoreaderServerUrlValid("https://www.calibre.example.org/api/koreader"))
        assertTrue(isKoreaderServerUrlValid("  https://server  "))
    }

    @Test
    fun blankAndBareValuesAreNotValid() {
        assertFalse(isKoreaderServerUrlValid(""))
        assertFalse(isKoreaderServerUrlValid("   "))
        assertFalse(isKoreaderServerUrlValid("server.local:6061"))
        assertFalse(isKoreaderServerUrlValid("koreader"))
    }

    @Test
    fun nonHttpSchemesAreNotValid() {
        assertFalse(isKoreaderServerUrlValid("ftp://server/books"))
        assertFalse(isKoreaderServerUrlValid("file:///sdcard/books"))
    }

    @Test
    fun aSchemeWithNoHostIsNotValid() {
        assertFalse(isKoreaderServerUrlValid("http://"))
        assertFalse(isKoreaderServerUrlValid("https:///api/koreader"))
    }
}
