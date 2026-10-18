package com.jdluu.leafline.library.cover

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

/**
 * In-memory LRU cache and downsampled decoder for book covers.
 *
 * Keeps decoded bitmaps in memory to prevent repeated disk reads, allocation
 * churn, and GC jank during library scrolling. Decodes using [Bitmap.Config.RGB_565]
 * and power-of-two [BitmapFactory.Options.inSampleSize] to bound bitmap heap usage.
 */
object BitmapMemoryCache {

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Allocate 1/8th of available memory to cover cache, minimum 4MB
    private val cacheSizeKb = (maxMemoryKb / 8).coerceAtLeast(4 * 1024)

    private val lruCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(path: String): Bitmap? = synchronized(lruCache) {
        lruCache.get(path)?.takeUnless { it.isRecycled }
    }

    fun put(path: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            synchronized(lruCache) {
                lruCache.put(path, bitmap)
            }
        }
    }

    fun evict(path: String) {
        synchronized(lruCache) {
            lruCache.remove(path)
        }
    }

    fun clear() {
        synchronized(lruCache) {
            lruCache.evictAll()
        }
    }

    /**
     * Decodes a bitmap from disk downsampled to approximately [reqWidth] x [reqHeight].
     * If already cached in memory, returns the cached instance immediately.
     */
    fun decodeSampledBitmap(
        path: String,
        reqWidth: Int = 300,
        reqHeight: Int = 450
    ): Bitmap? {
        get(path)?.let { return it }

        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, boundsOptions)

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return null
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeFile(path, decodeOptions)
            if (bitmap != null) {
                put(path, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    internal fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
