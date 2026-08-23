package com.jdluu.leafline

import android.content.Context
import com.jdluu.leafline.library.BookMetadataMapper
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.cover.CoverCache
import com.jdluu.leafline.library.cover.PublicationCoverWriter
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.publication.Metadata
import java.io.File
import java.time.Instant

class EpubImporter(private val context: Context) {

    private val coverCache = CoverCache(CoverCache.coversDirectory(context.filesDir))

    @OptIn(ExperimentalReadiumApi::class)
    fun importEpub(file: File, fileHash: String): LibraryBook? {
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
        
        val result = runBlocking {
            try {
                val asset = assetRetriever.retrieve(file).getOrElse {
                    return@runBlocking null
                }
                val publication = publicationOpener.open(asset, allowUserInteraction = true).getOrElse {
                    return@runBlocking null
                }
                val metadata = publication.metadata
                val book = BookMetadataMapper.map(
                    metadata = metadata,
                    filePath = file.absolutePath,
                    fileHash = fileHash,
                    addedAt = Instant.now()
                )
                val coverPath = try {
                    PublicationCoverWriter.extractAndStore(publication, book.stableId, coverCache)
                } catch (e: Exception) {
                    null
                }
                book.copy(coverPath = coverPath)
            } catch (e: Exception) {
                null
            }
        }
        
        return result
    }
}