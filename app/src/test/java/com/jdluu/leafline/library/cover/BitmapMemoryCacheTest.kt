package com.jdluu.leafline.library.cover

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapMemoryCacheTest {

    @Before
    fun setUp() {
        BitmapMemoryCache.clear()
    }

    @Test
    fun `calculateInSampleSize calculates powers of two correctly`() {
        val options = BitmapFactory.Options().apply {
            outWidth = 1200
            outHeight = 1800
        }
        val sampleSize = BitmapMemoryCache.calculateInSampleSize(options, 300, 450)
        assertEquals(4, sampleSize)
    }

    @Test
    fun `calculateInSampleSize returns 1 when requested size exceeds bounds`() {
        val options = BitmapFactory.Options().apply {
            outWidth = 200
            outHeight = 300
        }
        val sampleSize = BitmapMemoryCache.calculateInSampleSize(options, 300, 450)
        assertEquals(1, sampleSize)
    }

    @Test
    fun `put and get caches bitmap in memory`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565)
        BitmapMemoryCache.put("/path/to/cover.jpg", bitmap)

        val cached = BitmapMemoryCache.get("/path/to/cover.jpg")
        assertNotNull(cached)
        assertEquals(bitmap, cached)
    }

    @Test
    fun `evict removes item from cache`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565)
        BitmapMemoryCache.put("/path/to/cover.jpg", bitmap)
        BitmapMemoryCache.evict("/path/to/cover.jpg")

        assertNull(BitmapMemoryCache.get("/path/to/cover.jpg"))
    }

    @Test
    fun `clear empties the cache`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565)
        BitmapMemoryCache.put("/path/to/cover1.jpg", bitmap)
        BitmapMemoryCache.put("/path/to/cover2.jpg", bitmap)
        BitmapMemoryCache.clear()

        assertNull(BitmapMemoryCache.get("/path/to/cover1.jpg"))
        assertNull(BitmapMemoryCache.get("/path/to/cover2.jpg"))
    }
}
