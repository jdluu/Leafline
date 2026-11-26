package com.jdluu.leafline

import android.content.Context
import com.jdluu.leafline.library.BookMetadataMapper
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.cover.CoverCache
import com.jdluu.leafline.library.cover.PublicationCoverWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.time.Instant

/** [EpubImporter] implementation backed by the Readium Kotlin Toolkit. */
class ReadiumEpubImporter(private val context: Context) : EpubImporter {

    private val coverCache by lazy { CoverCache(CoverCache.coversDirectory(context.filesDir)) }
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

    @OptIn(ExperimentalReadiumApi::class)
    override suspend fun importEpub(file: File, fileHash: String): LibraryBook? = withContext(Dispatchers.IO) {
        try {
            val asset = assetRetriever.retrieve(file).getOrElse {
                return@withContext null
            }
            val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrElse {
                return@withContext null
            }
            try {
                val book = BookMetadataMapper.map(
                    metadata = publication.metadata,
                    filePath = file.absolutePath,
                    fileHash = fileHash,
                    addedAt = Instant.now(),
                    koreaderHash = FileHashUtil.koreaderHash(file)
                )
                val coverPath = try {
                    PublicationCoverWriter.extractAndStore(publication, book.stableId, coverCache)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                book.copy(coverPath = coverPath)
            } finally {
                runCatching { publication.close() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
