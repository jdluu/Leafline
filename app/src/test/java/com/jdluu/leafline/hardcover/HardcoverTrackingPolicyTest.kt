package com.jdluu.leafline.hardcover

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** In-memory store so the resolver's rules can be tested without Android. */
private class FakeTrackingStore(
    var currentMode: HardcoverTrackingMode = HardcoverTrackingStorage.DEFAULT_MODE,
    private val preferences: MutableMap<String, HardcoverBookPreference> = mutableMapOf()
) : HardcoverTrackingStore {
    override fun mode(): HardcoverTrackingMode = currentMode
    override fun setMode(mode: HardcoverTrackingMode) { currentMode = mode }
    override fun preferenceFor(stableId: String) = preferences[stableId]
    override fun setPreference(stableId: String, preference: HardcoverBookPreference) {
        preferences[stableId] = preference
    }
    override fun clearPreference(stableId: String) { preferences.remove(stableId) }
}

/**
 * The defaults and the precedence here are what decide whether a reader's data leaves
 * the device without them being asked, so each rule is pinned separately.
 */
class HardcoverTrackingPolicyTest {

    private val isbn = "urn:isbn:9780141439471"
    private val noIsbn = "http://www.gutenberg.org/1952"

    @Test
    fun `the default mode tracks every matchable book`() {
        assertEquals(HardcoverTrackingMode.ALL_BOOKS, HardcoverTrackingStorage.DEFAULT_MODE)

        assertEquals(
            HardcoverTrackingDecision.Track,
            HardcoverTrackingPolicy.decide(
                HardcoverTrackingStorage.DEFAULT_MODE, preference = null, hasIsbn = true
            )
        )
    }

    @Test
    fun `a book with no isbn is never tracked and never asked about`() {
        HardcoverTrackingMode.entries.forEach { mode ->
            assertEquals(
                "mode $mode should skip a book Hardcover cannot match",
                HardcoverTrackingDecision.Skip,
                HardcoverTrackingPolicy.decide(mode, preference = null, hasIsbn = false)
            )
        }
    }

    @Test
    fun `off wins over a remembered yes`() {
        assertEquals(
            HardcoverTrackingDecision.Skip,
            HardcoverTrackingPolicy.decide(
                HardcoverTrackingMode.OFF,
                preference = HardcoverBookPreference.TRACK,
                hasIsbn = true
            )
        )
    }

    @Test
    fun `ask mode asks when there is no answer yet`() {
        assertEquals(
            HardcoverTrackingDecision.Ask,
            HardcoverTrackingPolicy.decide(
                HardcoverTrackingMode.ASK_EACH_BOOK, preference = null, hasIsbn = true
            )
        )
    }

    @Test
    fun `a remembered answer wins over the mode in both directions`() {
        assertEquals(
            "a declined book stays declined under all books",
            HardcoverTrackingDecision.Skip,
            HardcoverTrackingPolicy.decide(
                HardcoverTrackingMode.ALL_BOOKS,
                preference = HardcoverBookPreference.IGNORE,
                hasIsbn = true
            )
        )
        assertEquals(
            "an accepted book stays accepted when the mode would ask",
            HardcoverTrackingDecision.Track,
            HardcoverTrackingPolicy.decide(
                HardcoverTrackingMode.ASK_EACH_BOOK,
                preference = HardcoverBookPreference.TRACK,
                hasIsbn = true
            )
        )
    }

    @Test
    fun `the resolver reads the mode and the answer for that book`() {
        val store = FakeTrackingStore(currentMode = HardcoverTrackingMode.ASK_EACH_BOOK)
        val resolver = HardcoverTrackingResolver(store)

        assertEquals(HardcoverTrackingDecision.Ask, resolver.decide("book-1", isbn))

        resolver.remember("book-1", HardcoverBookPreference.IGNORE)
        assertEquals(HardcoverTrackingDecision.Skip, resolver.decide("book-1", isbn))
        assertEquals(
            "another book must not inherit the answer",
            HardcoverTrackingDecision.Ask,
            resolver.decide("book-2", isbn)
        )
    }

    @Test
    fun `the resolver recognises a book with no isbn`() {
        val resolver = HardcoverTrackingResolver(FakeTrackingStore())

        assertEquals(HardcoverTrackingDecision.Skip, resolver.decide("b", noIsbn))
        assertEquals(HardcoverTrackingDecision.Skip, resolver.decide("b", null))
    }

    @Test
    fun `switching the mode off stops everything already answered yes`() {
        val store = FakeTrackingStore()
        val resolver = HardcoverTrackingResolver(store)
        resolver.remember("b", HardcoverBookPreference.TRACK)

        resolver.setMode(HardcoverTrackingMode.OFF)

        assertEquals(HardcoverTrackingDecision.Skip, resolver.decide("b", isbn))
        assertEquals(
            "the answer is kept, so turning the mode back on restores it",
            HardcoverBookPreference.TRACK,
            store.preferenceFor("b")
        )
    }

    @Test
    fun `forgetting an answer returns the book to being asked`() {
        val store = FakeTrackingStore(currentMode = HardcoverTrackingMode.ASK_EACH_BOOK)
        val resolver = HardcoverTrackingResolver(store)
        resolver.remember("b", HardcoverBookPreference.IGNORE)

        resolver.forget("b")

        assertNull(store.preferenceFor("b"))
        assertEquals(HardcoverTrackingDecision.Ask, resolver.decide("b", isbn))
    }
}
