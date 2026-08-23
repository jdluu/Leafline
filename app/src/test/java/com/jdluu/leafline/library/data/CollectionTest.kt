package com.jdluu.leafline.library.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionTest {

    @Test
    fun `collection domain model stores all fields`() {
        val collection = Collection(id = 1L, name = "Favorites", createdAtEpochMillis = 1000L)
        assertEquals(1L, collection.id)
        assertEquals("Favorites", collection.name)
        assertEquals(1000L, collection.createdAtEpochMillis)
    }

    @Test
    fun `collection data class equality works`() {
        val a = Collection(1L, "Favorites", 1000L)
        val b = Collection(1L, "Favorites", 1000L)
        assertEquals(a, b)
    }

    @Test
    fun `collections with different ids are not equal`() {
        val a = Collection(1L, "Favorites", 1000L)
        val b = Collection(2L, "Favorites", 1000L)
        assertTrue(a != b)
    }
}