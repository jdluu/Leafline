package com.jdluu.leafline.library.cover

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache

/**
 * In-memory LRU cache and downsampled decoder for book covers.
 *
 * Keeps decoded bitmaps in memory to prevent repeated disk reads, allocation
 * churn, and GC jank during library scrolling. Decodes using [Bitmap.Config.RGB_565]
 * and power-of-two [BitmapFactory.Options.inSampleSize] to bound bitmap heap usage.
 * Responds to system memory trim signals to keep overall app memory usage low.
 */
object BitmapMemoryCache {

    private const val TAG = "BitmapMemoryCache"
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Allocate 1/8th of available memory to cover cache, minimum 4MB
    internal val cacheSizeKb = (maxMemoryKb / 8).coerceAtLeast(4 * 1024)

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

    fun sizeKb(): Int = synchronized(lruCache) {
        lruCache.size()
    }

    fun trimToSize(maxSizeKb: Int) {
        synchronized(lruCache) {
            lruCache.trimToSize(maxSizeKb)
        }
    }

    /**
     * Responds to system memory trim notifications ([ComponentCallbacks2]).
     */
    fun onTrimMemory(level: Int) {
        synchronized(lruCache) {
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE ||
                level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                    lruCache.evictAll()
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                    lruCache.trimToSize(cacheSizeKb / 2)
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                    lruCache.trimToSize((cacheSizeKb * 3) / 4)
                }
            }
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
                inTempStorage = ByteArray(16 * 1024)
            }

            val bitmap = try {
                BitmapFactory.decodeFile(path, decodeOptions)
            } catch (oom: OutOfMemoryError) {
                Log.w(TAG, "OOM decoding cover $path, clearing cache and retrying with larger sample size", oom)
                clear()
                decodeOptions.inSampleSize = (decodeOptions.inSampleSize * 2).coerceAtLeast(2)
                runCatching { BitmapFactory.decodeFile(path, decodeOptions) }.getOrNull()
            }

            if (bitmap != null) {
                put(path, bitmap)
            }
            bitmap
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to decode cover at $path", e)
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
