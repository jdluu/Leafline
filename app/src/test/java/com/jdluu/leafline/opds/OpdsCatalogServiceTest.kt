package com.jdluu.leafline.opds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpdsCatalogServiceTest {

    private val service = OpdsCatalogService()

    private fun loadFixture(name: String): ByteArray {
        val stream = javaClass.classLoader!!.getResourceAsStream(name)
            ?: error("Missing test resource: $name")
        return stream.readBytes()
    }

    @Test
    fun parseXml_extractsRootNavigationEntries() {
        val xml = loadFixture("grimmory-root-feed.xml")
        val result = service.parseXml(xml, "http://localhost:6060/api/v1/opds")

        assertTrue("Expected successful parse", result.isSuccess)

        val entries = result.getOrNull()
        assertTrue("Expected navigation entries", entries != null && entries.isNotEmpty())

        val byTitle = entries!!.associateBy { it.title }
        val base = "http://localhost:6060"
        assertEquals("$base/api/v1/opds/catalog", byTitle["All Books"]?.href)
        assertEquals("$base/api/v1/opds/recent", byTitle["Recently Added"]?.href)
        assertEquals("$base/api/v1/opds/libraries", byTitle["Libraries"]?.href)
        assertEquals("$base/api/v1/opds/authors", byTitle["Authors"]?.href)
    }

    @Test
    fun parseXmlPage_extractsAcquisitionEntry() {
        val result = service.parseXmlPage(
            loadFixture("grimmory-catalog-feed.xml"),
            "http://localhost:6060/api/v1/opds/catalog"
        )

        assertTrue("Expected successful parse", result.isSuccess)
        val page = result.getOrNull()!!
        val acquisitions = page.acquisitionEntries()

        assertEquals(1, acquisitions.size)
        assertEquals("Pride and Prejudice", acquisitions.single().title)
        assertEquals("Jane Austen", acquisitions.single().authors.single())
        assertEquals("http://localhost:6060/books/pride.epub", acquisitions.single().href)
    }

    @Test
    fun parseXml_returnsFailure_onBadXml() {
        val result = service.parseXml(
            "<feed><entry></feed>".toByteArray(),
            "http://localhost:6060/api/v1/opds"
        )
        assertTrue("Expected failure on malformed feed", result.isFailure)
    }
}

class OpdsFeedEntryTest {
    @Test
    fun acquisitionRel_acceptsOpenAccessAndAcquisitionVariants() {
        assertTrue(isAcquisitionRel(setOf("http://opds-spec.org/acquisition")))
        assertTrue(isAcquisitionRel(setOf("http://opds-spec.org/acquisition/open-access")))
        assertTrue(isAcquisitionRel(setOf("http://opds-spec.org/acquisition/borrow")))
    }
}
