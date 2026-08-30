package com.jdluu.leafline.library

import kotlin.time.ExperimentalTime
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.readium.r2.shared.publication.Contributor
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Metadata
import java.time.Instant

class BookMetadataMapperTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun `stableId uses identifier when nonblank`() {
        val metadata = Metadata().copy(identifier = "test-identifier")
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals("identifier:test-identifier:hash:hash123", result.stableId)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `stableId falls back to fileHash when identifier blank`() {
        val metadata = Metadata().copy(identifier = "   ")
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals("hash:hash123", result.stableId)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `stableId falls back to fileHash when identifier null`() {
        val metadata = Metadata()
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals("hash:hash123", result.stableId)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `title defaults to Untitled when null`() {
        val metadata = Metadata()
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals("Untitled", result.title)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `title uses localizedTitle when set`() {
        val metadata = Metadata().copy(localizedTitle = LocalizedString("  Test Book  "))
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals("Test Book", result.title)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `authors extracts and trims names`() {
        val metadata = Metadata().copy(
            authors = listOf(
                Contributor(name = "  Jane Austen  "),
                Contributor(name = "  ")
            )
        )
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals(listOf("Jane Austen"), result.authors)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `language extracts from Language object`() {
        val metadata = Metadata().copy(languages = listOf("en-US"))
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals("en-US", result.language)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `language is null when no languages`() {
        val metadata = Metadata()
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertNull(result.language)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `description trims value`() {
        val metadata = Metadata().copy(description = "  A great book  ")
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals("A great book", result.description)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `description is null when not set`() {
        val metadata = Metadata()
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertNull(result.description)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `publisher joins names with semicolon when multiple`() {
        val metadata = Metadata().copy(
            publishers = listOf(
                Contributor(name = "Publisher One"),
                Contributor(name = "Publisher Two")
            )
        )
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals("Publisher One; Publisher Two", result.publisher)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `publisher is null when no publishers`() {
        val metadata = Metadata()
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertNull(result.publisher)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `publishedAtEpochMillis converts from published instant`() {
        val published = kotlin.time.Instant.fromEpochSeconds(1686849000L, 0)
        val expectedEpochMillis = 1686849000000L
        val metadata = Metadata().copy(published = published)
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals(expectedEpochMillis, result.publishedAtEpochMillis)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `publishedAtEpochMillis is null when published not set`() {
        val metadata = Metadata()
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertNull(result.publishedAtEpochMillis)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `addedAtEpochMillis converts from addedAt instant`() {
        val instant = Instant.parse("2024-01-15T10:30:00Z")
        val expectedEpochMillis = instant.toEpochMilli()
        val metadata = Metadata()
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = instant
        )
        assertEquals(expectedEpochMillis, result.addedAtEpochMillis)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `pageCount uses numberOfPages from metadata`() {
        val metadata = Metadata().copy(numberOfPages = 250)
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals(250, result.pageCount)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `pageCount is null when numberOfPages not set`() {
        val metadata = Metadata()
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/path/to/file.epub",
            fileHash = "hash123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertNull(result.pageCount)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `filePath and fileHash are preserved`() {
        val metadata = Metadata()
        val result = BookMetadataMapper.map(
            metadata = metadata,
            filePath = "/books/another/path.epub",
            fileHash = "sha256-abc123",
            addedAt = Instant.parse("2024-01-15T10:30:00Z")
        )
        assertEquals("/books/another/path.epub", result.filePath)
        assertEquals("sha256-abc123", result.fileHash)
    }
}