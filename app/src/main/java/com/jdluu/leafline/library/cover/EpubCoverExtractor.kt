package com.jdluu.leafline.library.cover

import android.content.Context
import android.util.Log
import com.jdluu.leafline.library.LibraryBook
import java.io.File
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/**
 * Extracts cover images for books that were imported before cover caching
 * existed. Opens the EPUB with Readium, stores the cover through a
 * [CoverCache], and returns the stored file path.
 */
class EpubCoverExtractor(
    private val context: Context,
    private val cache: CoverCache
) {

    suspend fun extractCoverPath(book: LibraryBook): String? {
        cache.existingCoverPath(book.stableId)?.let { return it }
        val file = File(book.filePath)
        if (!file.isFile) return null

        val publication = openPublication(file) ?: return null
        return try {
            PublicationCoverWriter.extractAndStore(publication, book.stableId, cache)
        } catch (e: Exception) {
            Log.w(TAG, "Cover extraction failed for ${book.stableId}", e)
            null
        } finally {
            try {
                publication.close()
            } catch (e: Exception) {
                Log.w(TAG, "Could not close publication after cover extraction", e)
            }
        }
    }

    @OptIn(ExperimentalReadiumApi::class)
    private suspend fun openPublication(file: File): Publication? {
        return try {
            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(
                contentResolver = context.contentResolver,
                httpClient = httpClient
            )
            val publicationOpener = PublicationOpener(
                publicationParser = DefaultPublicationParser(
                    context,
                    httpClient = httpClient,
                    assetRetriever = assetRetriever,
                    pdfFactory = null
                )
            )
            val asset = assetRetriever.retrieve(file).getOrElse { return null }
            publicationOpener.open(asset, allowUserInteraction = true).getOrElse { return null }
        } catch (e: Exception) {
            Log.w(TAG, "Could not open publication for cover extraction", e)
            null
        }
    }

    private companion object {
        const val TAG = "EpubCoverExtractor"
    }
}
