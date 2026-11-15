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
        assertEquals("ephemeral", DictionaryLookupHelper.sanitizeQuery("“ephemeral”"))
        assertEquals("solitude", DictionaryLookupHelper.sanitizeQuery("\"solitude\"..."))
        assertEquals("wonder", DictionaryLookupHelper.sanitizeQuery("«wonder»!"))
        assertEquals("test", DictionaryLookupHelper.sanitizeQuery("(test);"))
        assertEquals("clean", DictionaryLookupHelper.sanitizeQuery("  clean.  "))
    }

    @Test
    fun sanitizeQueryPreservesHyphenatedWordsAndApostrophes() {
        assertEquals("well-known", DictionaryLookupHelper.sanitizeQuery("\"well-known\""))
        assertEquals("don't", DictionaryLookupHelper.sanitizeQuery("don't."))
    }

    @Test
    fun sanitizeQueryFallsBackWhenOnlyPunctuation() {
        assertEquals("...", DictionaryLookupHelper.sanitizeQuery("..."))
        assertEquals("---", DictionaryLookupHelper.sanitizeQuery("---"))
    }

    @Test
    fun buildColorDictIntentSetsExpectedActionAndExtras() {
        val intent = DictionaryLookupHelper.buildColorDictIntent("serendipity")
        assertEquals("colordict.intent.action.SEARCH", intent.action)
        assertEquals("serendipity", intent.getStringExtra("EXTRA_QUERY"))
        assertEquals(false, intent.getBooleanExtra("EXTRA_FULLSCREEN", true))
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun buildDefineIntentSetsExpectedActionAndText() {
        val intent = DictionaryLookupHelper.buildDefineIntent("quintessential")
        assertEquals(Intent.ACTION_DEFINE, intent.action)
        assertEquals("quintessential", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun buildProcessTextIntentSetsMimeAndExtras() {
        val intent = DictionaryLookupHelper.buildProcessTextIntent("sample passage", readonly = true)
        assertEquals(Intent.ACTION_PROCESS_TEXT, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("sample passage", intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT))
        assertEquals(true, intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false))
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
        val dataStr = intent.data.toString()
        assertTrue(dataStr.startsWith("https://duckduckgo.com/?q=define+"))
        assertTrue(dataStr.contains("%26"))
        assertTrue(dataStr.contains("Alice") && dataStr.contains("Wonderland"))
    }

    @Test
    fun resolveDictionaryIntentReturnsNullWhenNoDictionaryAppsInstalled() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val resolved = DictionaryLookupHelper.resolveDictionaryIntent(context.packageManager, "testword")
        assertNull(resolved)
    }
}
