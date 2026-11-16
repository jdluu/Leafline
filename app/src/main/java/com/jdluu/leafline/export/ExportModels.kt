package com.jdluu.leafline.export

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.Annotation

/**
 * Supported data export formats for books, highlights, and annotations.
 */
enum class ExportFormat(
    val id: String,
    val label: String,
    val extension: String,
    val mimeType: String,
    val isBatchOnly: Boolean = false
) {
    OBSIDIAN_MARKDOWN("obsidian_md", "Obsidian Markdown", "md", "text/markdown"),
    READWISE_CSV("readwise_csv", "Readwise CSV", "csv", "text/csv"),
    ANKI_TSV("anki_tsv", "Anki Flashcards (TSV)", "tsv", "text/tab-separated-values"),
    STRUCTURED_JSON("json", "Structured JSON", "json", "application/json"),
    PLAIN_TEXT("plain_text", "Plain Text", "txt", "text/plain"),
    VAULT_ZIP("vault_zip", "Obsidian Vault (ZIP)", "zip", "application/zip", isBatchOnly = true)
}

/**
 * Data bundle representing a book, its collections, and its annotations.
 */
data class BookExportData(
    val book: LibraryBook,
    val annotations: List<Annotation>,
    val collections: List<String> = emptyList()
)

/**
 * Output of a single-book export.
 */
data class SingleExportResult(
    val format: ExportFormat,
    val filename: String,
    val content: String,
    val mimeType: String,
    val highlightCount: Int
)

/**
 * Output of a batch multi-book or library-wide export.
 */
data class BatchExportResult(
    val format: ExportFormat,
    val filename: String,
    val bookCount: Int,
    val highlightCount: Int,
    val textPayload: String? = null,
    val binaryPayload: ByteArray? = null,
    val mimeType: String
)
