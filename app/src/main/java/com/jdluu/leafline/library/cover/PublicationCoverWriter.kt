package com.jdluu.leafline.library.cover

import android.graphics.Bitmap
import android.util.Size
import java.io.ByteArrayOutputStream
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.coverFitting

/**
 * Extracts a cover bitmap from an already opened Readium publication and
 * stores it through a [CoverCache], keyed by book id.
 */
object PublicationCoverWriter {

    val maxCoverSize = Size(600, 900)

    private const val JPEG_QUALITY = 85

    suspend fun extractAndStore(
        publication: Publication,
        bookId: String,
        cache: CoverCache
    ): String? {
        return try {
            val bitmap = publication.coverFitting(maxCoverSize) ?: return null
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.toByteArray()
            }
            cache.storeCoverBytes(bookId, bytes)
        } catch (e: Exception) {
            null
        }
    }
}
