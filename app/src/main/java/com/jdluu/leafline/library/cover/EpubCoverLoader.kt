package com.jdluu.leafline.library.cover

import android.content.Context
import com.jdluu.leafline.library.LibraryBook
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/**
 * Loads the cover path for a book, extracting and caching it on first use.
 */
fun interface CoverLoader {
    suspend fun loadCoverPath(book: LibraryBook): String?
}

/**
 * Opens an imported EPUB with Readium and extracts its cover into the
 * [CoverCache].
 *
 * Caches Readium parser dependencies to avoid heavy re-instantiations
 * across repeated cover extractions.
 */
class EpubCoverLoader(
    private val context: Context,
    private val cache: CoverCache = CoverCache(CoverCache.coversDirectory(context.filesDir))
) : CoverLoader {

    private val httpClient by lazy { DefaultHttpClient() }
    private val assetRetriever by lazy {
        AssetRetriever(
            contentResolver = context.contentResolver,
            httpClient = httpClient
        )
    }
    private val publicationOpener by lazy {
        PublicationOpener(
            publicationParser = DefaultPublicationParser(
                context,
                httpClient = httpClient,
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )
    }

    override suspend fun loadCoverPath(book: LibraryBook): String? = withContext(Dispatchers.IO) {
        cache.existingCoverPath(book.stableId)?.let { return@withContext it }

        val file = File(book.filePath)
        if (!file.isFile) return@withContext null

        val publication = try {
            openPublication(file)
        } catch (e: Exception) {
            null
        } ?: return@withContext null

        try {
            PublicationCoverWriter.extractAndStore(publication, book.stableId, cache)
        } finally {
            runCatching { publication.close() }
        }
    }

    private suspend fun openPublication(file: File): Publication? {
        val asset = assetRetriever.retrieve(file).getOrElse { return null }
        return publicationOpener.open(asset, allowUserInteraction = false).getOrElse { null }
    }
}
