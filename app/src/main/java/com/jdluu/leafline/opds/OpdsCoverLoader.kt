package com.jdluu.leafline.opds

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.jdluu.leafline.library.cover.BitmapMemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Downloads and caches remote book covers from OPDS feeds.
 */
object OpdsCoverLoader {

    private val httpClient by lazy {
        OkHttpClient.Builder().build()
    }

    suspend fun loadCover(coverUrl: String, cacheDir: File): Bitmap? = withContext(Dispatchers.IO) {
        val memoryCached = BitmapMemoryCache.get(coverUrl)
        if (memoryCached != null) return@withContext memoryCached

        val hash = sha256(coverUrl)
        val cachedDiskFile = File(cacheDir, "opds_cover_$hash.jpg")

        if (cachedDiskFile.exists() && cachedDiskFile.length() > 0) {
            val bitmap = decodeSampled(cachedDiskFile.absolutePath)
            if (bitmap != null) {
                BitmapMemoryCache.put(coverUrl, bitmap)
                return@withContext bitmap
            }
        }

        try {
            val request = Request.Builder().url(coverUrl).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null

                cacheDir.mkdirs()
                FileOutputStream(cachedDiskFile).use { it.write(bytes) }

                val bitmap = decodeSampled(cachedDiskFile.absolutePath)
                if (bitmap != null) {
                    BitmapMemoryCache.put(coverUrl, bitmap)
                }
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampled(path: String, maxDim: Int = 360): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        val origW = options.outWidth
        val origH = options.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        while (origW / (sampleSize * 2) >= maxDim && origH / (sampleSize * 2) >= maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(path, decodeOptions)
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Composable helper to asynchronously load a remote OPDS cover image.
 */
@Composable
fun rememberOpdsCoverBitmap(url: String?): Bitmap? {
    if (url == null) return null
    val context = androidx.compose.ui.platform.LocalContext.current
    val cached = remember(url) { BitmapMemoryCache.get(url) }
    if (cached != null) return cached

    return produceState<Bitmap?>(initialValue = null, url) {
        value = OpdsCoverLoader.loadCover(url, context.cacheDir)
    }.value
}
