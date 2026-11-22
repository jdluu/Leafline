package com.jdluu.leafline.opds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The built-in catalogs are the whole of "download from common platforms", so their
 * addresses are load-bearing. Standard Ebooks moved its OPDS feed and now answers
 * 401 without a Patrons Circle account, and Feedbooks' public-domain catalog
 * returns 404, so both had to change.
 */
class BuiltInFeedsTest {

    @Test
    fun `every built-in feed is https and uniquely identified`() {
        val feeds = OpdsFeedStore.BUILT_IN_FEEDS

        assertEquals(feeds.size, feeds.map { it.id }.distinct().size)
        feeds.forEach {
            assertTrue("${it.id} is not https: ${it.url}", it.url.startsWith("https://"))
        }
    }

    @Test
    fun `standard ebooks points at the published opds path`() {
        val standardEbooks = OpdsFeedStore.BUILT_IN_FEEDS.first { it.id == "standard_ebooks" }

        assertEquals("https://standardebooks.org/feeds/opds", standardEbooks.url)
    }

    @Test
    fun `a catalog that needs an account says so before it is opened`() {
        val standardEbooks = OpdsFeedStore.BUILT_IN_FEEDS.first { it.id == "standard_ebooks" }

        assertTrue(
            "Standard Ebooks OPDS is Patrons-only; the description must say so",
            standardEbooks.description.orEmpty().contains("Patrons Circle")
        )
    }

    @Test
    fun `a catalog that needs no account does not claim one`() {
        val gutenberg = OpdsFeedStore.BUILT_IN_FEEDS.first { it.id == "project_gutenberg" }

        assertTrue(
            "Gutenberg needs no account; implying one would be a false instruction",
            !gutenberg.description.orEmpty().contains("membership", ignoreCase = true)
        )
    }

    @Test
    fun `the retired feedbooks catalog is not offered`() {
        val ids = OpdsFeedStore.BUILT_IN_FEEDS.map { it.id }

        assertTrue("feedbooks returns 404 and must not be offered", "feedbooks" !in ids)
    }
}
