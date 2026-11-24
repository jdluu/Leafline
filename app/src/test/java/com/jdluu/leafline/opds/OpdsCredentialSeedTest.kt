package com.jdluu.leafline.opds

import com.jdluu.leafline.opds.model.BreadcrumbItem
import com.jdluu.leafline.opds.model.OpdsFeedSource
import com.jdluu.leafline.opds.ui.credentialSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The 401 recovery form has to name a catalog the reader can actually browse. A sign-in
 * prompt can arrive from any depth, so the seed must come from the root, never the
 * sub-feed that happened to answer the request.
 */
class OpdsCredentialSeedTest {

    private val standardEbooks = OpdsFeedSource(
        id = "standard_ebooks",
        title = "Standard Ebooks",
        url = "https://standardebooks.org/feeds/opds",
        isBuiltIn = true
    )

    @Test
    fun `a 401 below the root seeds the catalog root, not the sub-feed`() {
        val breadcrumbs = listOf(
            BreadcrumbItem("Standard Ebooks", "https://standardebooks.org/feeds/opds"),
            BreadcrumbItem("Patrons Circle", "https://standardebooks.org/feeds/opds/patrons")
        )

        val seed = credentialSeed(breadcrumbs, standardEbooks)

        assertEquals("Standard Ebooks", seed.title)
        assertEquals("https://standardebooks.org/feeds/opds", seed.url)
        assertFalse("the seed is a form input, not a saved catalog", seed.isBuiltIn)
        assertEquals("", seed.id)
        assertEquals(null, seed.password)
    }

    @Test
    fun `with no breadcrumbs the seed falls back to the selected catalog`() {
        val seed = credentialSeed(emptyList(), standardEbooks)

        assertEquals("Standard Ebooks", seed.title)
        assertEquals("https://standardebooks.org/feeds/opds", seed.url)
    }
}
