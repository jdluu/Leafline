package com.jdluu.leafline.library

import kotlin.time.ExperimentalTime
import org.readium.r2.shared.publication.Metadata
import java.time.Instant

object BookMetadataMapper {

    @OptIn(ExperimentalTime::class)
    fun map(
        metadata: Metadata,
        filePath: String,
        fileHash: String,
        addedAt: Instant,
        koreaderHash: String? = null
    ): LibraryBook {
        val stableId = run {
            val identifier = metadata.identifier?.trim()
            if (!identifier.isNullOrBlank()) identifier else fileHash
        }

        val title = run {
            val t = metadata.title?.trim()
            if (t.isNullOrBlank()) "Untitled" else t
        }

        val authors = metadata.authors
            .map { it.name.trim() }
            .filter { it.isNotBlank() }

        val language = metadata.language?.code

        val description = metadata.description?.trim()

        val publisher = run {
            val names = metadata.publishers
                .map { it.name.trim() }
                .filter { it.isNotBlank() }
            if (names.isEmpty()) null else names.joinToString("; ")
        }

        val publishedAtEpochMillis = metadata.published?.let { inst ->
            java.time.Instant.ofEpochSecond(
                inst.epochSeconds,
                inst.nanosecondsOfSecond.toLong()
            ).toEpochMilli()
        }

        val addedAtEpochMillis = addedAt.toEpochMilli()

        val pageCount = metadata.numberOfPages

        return LibraryBook(
            stableId = stableId,
            title = title,
            authors = authors,
            language = language,
            description = description,
            publisher = publisher,
            publishedAtEpochMillis = publishedAtEpochMillis,
            filePath = filePath,
            fileHash = fileHash,
            addedAtEpochMillis = addedAt.toEpochMilli(),
            pageCount = pageCount,
            koreaderHash = koreaderHash
        )
    }
}