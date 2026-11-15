package com.jdluu.leafline.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

/**
 * Service coordinating data gathering and exporting of books, annotations, and notes.
 */
class NotesExporter(
    private val libraryRepository: LibraryRepository,
    private val annotationRepository: AnnotationRepository
) {

    suspend fun getExportDataForBook(book: LibraryBook): BookExportData = withContext(Dispatchers.IO) {
        val annotations = annotationRepository.getAnnotations(book.stableId)
        val collections = libraryRepository.getCollectionsForBook(book.stableId).first().map { it.name }
        BookExportData(book, annotations, collections)
    }

    suspend fun getAllExportData(includeEmptyBooks: Boolean = false): List<BookExportData> = withContext(Dispatchers.IO) {
        val allBooks = libraryRepository.getAllBooks().first()
        val allAnnotations = annotationRepository.getAllAnnotations()
        val annotationsByBook = allAnnotations.groupBy { it.bookId }

        allBooks.mapNotNull { book ->
            val bookAnnotations = annotationsByBook[book.stableId] ?: emptyList()
            if (!includeEmptyBooks && bookAnnotations.isEmpty()) {
                null
            } else {
                val collections = libraryRepository.getCollectionsForBook(book.stableId).first().map { it.name }
                BookExportData(book, bookAnnotations, collections)
            }
        }
    }

    fun exportSingle(data: BookExportData, format: ExportFormat): SingleExportResult {
        val filename = NotesExportFormatter.defaultSingleFilename(data.book.title, format)
        val content = when (format) {
            ExportFormat.OBSIDIAN_MARKDOWN -> NotesExportFormatter.formatSingleBookMarkdown(data)
            ExportFormat.STRUCTURED_JSON -> NotesExportFormatter.formatSingleBookJson(data)
            ExportFormat.PLAIN_TEXT -> NotesExportFormatter.formatSingleBookPlainText(data)
            ExportFormat.VAULT_ZIP -> NotesExportFormatter.formatSingleBookMarkdown(data)
        }
        return SingleExportResult(
            format = format,
            filename = filename,
            content = content,
            mimeType = format.mimeType,
            highlightCount = data.annotations.size
        )
    }

    fun exportBatch(booksData: List<BookExportData>, format: ExportFormat): BatchExportResult {
        val filename = NotesExportFormatter.defaultBatchFilename(format)
        val totalHighlights = booksData.sumOf { it.annotations.size }

        return when (format) {
            ExportFormat.VAULT_ZIP -> {
                val zipBytes = NotesExportFormatter.buildVaultZip(booksData)
                BatchExportResult(
                    format = format,
                    filename = filename,
                    bookCount = booksData.size,
                    highlightCount = totalHighlights,
                    binaryPayload = zipBytes,
                    mimeType = format.mimeType
                )
            }
            ExportFormat.STRUCTURED_JSON -> {
                val json = NotesExportFormatter.formatBatchJson(booksData)
                BatchExportResult(
                    format = format,
                    filename = filename,
                    bookCount = booksData.size,
                    highlightCount = totalHighlights,
                    textPayload = json,
                    mimeType = format.mimeType
                )
            }
            ExportFormat.OBSIDIAN_MARKDOWN -> {
                val md = NotesExportFormatter.formatBatchCombinedMarkdown(booksData)
                BatchExportResult(
                    format = format,
                    filename = filename,
                    bookCount = booksData.size,
                    highlightCount = totalHighlights,
                    textPayload = md,
                    mimeType = format.mimeType
                )
            }
            ExportFormat.PLAIN_TEXT -> {
                val text = booksData.joinToString("\n\n========================================\n\n") {
                    NotesExportFormatter.formatSingleBookPlainText(it)
                }
                BatchExportResult(
                    format = format,
                    filename = filename,
                    bookCount = booksData.size,
                    highlightCount = totalHighlights,
                    textPayload = text,
                    mimeType = format.mimeType
                )
            }
        }
    }

    suspend fun writeSingleToStream(result: SingleExportResult, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(result.content)
            writer.flush()
        }
    }

    suspend fun writeBatchToStream(result: BatchExportResult, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        if (result.binaryPayload != null) {
            outputStream.use { out ->
                out.write(result.binaryPayload)
                out.flush()
            }
        } else if (result.textPayload != null) {
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(result.textPayload)
                writer.flush()
            }
        }
    }

    fun shareSingle(context: Context, result: SingleExportResult) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = result.mimeType
            putExtra(Intent.EXTRA_SUBJECT, result.filename)
            putExtra(Intent.EXTRA_TEXT, result.content)
        }
        val chooser = Intent.createChooser(intent, "Export notes")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareBatch(context: Context, result: BatchExportResult) {
        if (result.textPayload != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = result.mimeType
                putExtra(Intent.EXTRA_SUBJECT, result.filename)
                putExtra(Intent.EXTRA_TEXT, result.textPayload)
            }
            val chooser = Intent.createChooser(intent, "Export library notes")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } else if (result.binaryPayload != null) {
            try {
                val cacheDir = File(context.cacheDir, "exports")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val tempFile = File(cacheDir, result.filename)
                tempFile.writeBytes(result.binaryPayload)

                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, tempFile)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = result.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Export library notes")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (_: Exception) {
                // Ignore fallback
            }
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Notes") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }
}
