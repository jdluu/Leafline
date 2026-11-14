package com.jdluu.leafline.reader.selection

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DictionaryLookupHelperTest {

    @Test
    fun sanitizeQueryStripsPunctuationAndQuotes() {
        assertEquals("curiosity", DictionaryLookupHelper.sanitizeQuery("  \"curiosity,\"  "))
        assertEquals("wonder", DictionaryLookupHelper.sanitizeQuery("“wonder!”"))
        assertEquals("reading", DictionaryLookupHelper.sanitizeQuery("‘reading’..."))
        assertEquals("definition", DictionaryLookupHelper.sanitizeQuery("«definition»"))
        assertEquals("parentheses", DictionaryLookupHelper.sanitizeQuery("(parentheses);"))
        assertEquals("em-dash", DictionaryLookupHelper.sanitizeQuery("—em-dash—"))
    }

    @Test
    fun sanitizeQueryPreservesInternalApostrophesAndHyphens() {
        assertEquals("it's", DictionaryLookupHelper.sanitizeQuery("“it's”"))
        assertEquals("well-known", DictionaryLookupHelper.sanitizeQuery("  well-known.  "))
    }

    @Test
    fun sanitizeQueryFallsBackToTrimmedIfAllPunctuation() {
        assertEquals("...", DictionaryLookupHelper.sanitizeQuery(" ... "))
        assertEquals("???", DictionaryLookupHelper.sanitizeQuery("???"))
    }

    @Test
    fun buildColorDictIntentSetsExpectedExtrasAndFlags() {
        val intent = DictionaryLookupHelper.buildColorDictIntent("testword")
        assertEquals(DictionaryLookupHelper.COLORDICT_ACTION, intent.action)
        assertEquals("testword", intent.getStringExtra(DictionaryLookupHelper.EXTRA_QUERY))
        assertEquals("testword", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(false, intent.getBooleanExtra(DictionaryLookupHelper.EXTRA_FULLSCREEN, true))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun buildDefineIntentSetsExpectedExtrasAndFlags() {
        val intent = DictionaryLookupHelper.buildDefineIntent("testword")
        assertEquals(Intent.ACTION_DEFINE, intent.action)
        assertEquals("testword", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun buildProcessTextIntentSetsExpectedExtrasAndType() {
        val intent = DictionaryLookupHelper.buildProcessTextIntent("selected text", readonly = true)
        assertEquals(Intent.ACTION_PROCESS_TEXT, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("selected text", intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT))
        assertEquals(true, intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun buildShareIntentSetsExpectedTypeAndTitle() {
        val intent = DictionaryLookupHelper.buildShareIntent("excerpt from book", "Pride and Prejudice")
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("excerpt from book", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("Pride and Prejudice", intent.getStringExtra(Intent.EXTRA_TITLE))
    }

    @Test
    fun buildWebSearchIntentEncodesQuery() {
        val intent = DictionaryLookupHelper.buildWebSearchIntent("Alice & Wonderland")
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertNotNull(intent.data)
        assertTrue(intent.data.toString().contains("Alice+%26+Wonderland"))
        assertTrue(intent.data.toString().startsWith("https://duckduckgo.com/?q=define+"))
    }

    @Test
    fun resolveDictionaryIntentReturnsNullWhenNoDictionaryAppsInstalled() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val resolved = DictionaryLookupHelper.resolveDictionaryIntent(context.packageManager, "testword")
        assertNull(resolved)
    }
}
