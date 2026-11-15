package com.jdluu.leafline.export

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.LibraryRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class NotesExporterTest {

    private class FakeAnnotationRepository : AnnotationRepository {
        val list = mutableListOf<Annotation>()

        override fun observeAnnotations(bookId: String): Flow<List<Annotation>> =
            flowOf(list.filter { it.bookId == bookId })

        override suspend fun getAnnotations(bookId: String): List<Annotation> =
            list.filter { it.bookId == bookId }

        override suspend fun getAllAnnotations(): List<Annotation> = list

        override suspend fun addAnnotation(
            bookId: String,
            locatorJson: String,
            colorHex: String,
            note: String?
        ): Long {
            val id = (list.size + 1).toLong()
            list.add(Annotation(id, bookId, locatorJson, colorHex, note, System.currentTimeMillis()))
            return id
        }

        override suspend fun removeAnnotation(id: Long) {
            list.removeIf { it.id == id }
        }
        override suspend fun restoreAnnotation(annotation: Annotation) {
            list.add(annotation)
        }
        override suspend fun updateAnnotationNote(id: Long, note: String?): Int {
            return 1
        }
        override suspend fun updateAnnotationColor(id: Long, colorHex: String): Int {
            return 1
        }
    }

    @Test
    fun getExportDataForBookCollectsBookAndAnnotations() = runTest {
        val libraryRepo = LibraryRepositoryImpl()
        val annotationRepo = FakeAnnotationRepository()
        val exporter = NotesExporter(libraryRepo, annotationRepo)

        val book = LibraryBook(
            stableId = "book-1",
            title = "Test Book",
            authors = listOf("Author A")
        )
        libraryRepo.addBook(book)
        annotationRepo.addAnnotation(
            bookId = "book-1",
            locatorJson = """{"text":{"exact":"Some quote"}}""",
            colorHex = "#FFEB3B",
            note = "Insight"
        )

        val data = exporter.getExportDataForBook(book)
        assertEquals("Test Book", data.book.title)
        assertEquals(1, data.annotations.size)
        assertEquals("Insight", data.annotations[0].note)
    }

    @Test
    fun getAllExportDataFiltersEmptyBooksWhenRequested() = runTest {
        val libraryRepo = LibraryRepositoryImpl()
        val annotationRepo = FakeAnnotationRepository()
        val exporter = NotesExporter(libraryRepo, annotationRepo)

        val book1 = LibraryBook(stableId = "b1", title = "Book 1")
        val book2 = LibraryBook(stableId = "b2", title = "Book 2")
        libraryRepo.addBook(book1)
        libraryRepo.addBook(book2)

        annotationRepo.addAnnotation("b1", """{"text":{"exact":"q1"}}""", "#FFEB3B", "note1")

        val nonEmptyOnly = exporter.getAllExportData(includeEmptyBooks = false)
        assertEquals(1, nonEmptyOnly.size)
        assertEquals("Book 1", nonEmptyOnly[0].book.title)

        val allBooks = exporter.getAllExportData(includeEmptyBooks = true)
        assertEquals(2, allBooks.size)
    }

    @Test
    fun exportSingleProducesExpectedFormats() {
        val libraryRepo = LibraryRepositoryImpl()
        val annotationRepo = FakeAnnotationRepository()
        val exporter = NotesExporter(libraryRepo, annotationRepo)

        val data = BookExportData(
            book = LibraryBook(stableId = "b1", title = "Great Book", authors = listOf("Jane")),
            annotations = listOf(
                Annotation(1, "b1", """{"text":{"exact":"hello world"}}""", "#FFEB3B", "nice", 0L)
            ),
            collections = listOf("Classics")
        )

        val mdResult = exporter.exportSingle(data, ExportFormat.OBSIDIAN_MARKDOWN)
        assertEquals("Great Book-Notes.md", mdResult.filename)
        assertTrue(mdResult.content.contains("title: \"Great Book\""))
        assertTrue(mdResult.content.contains("hello world"))

        val jsonResult = exporter.exportSingle(data, ExportFormat.STRUCTURED_JSON)
        assertEquals("Great Book-Notes.json", jsonResult.filename)
        assertTrue(jsonResult.content.contains("\"title\": \"Great Book\""))

        val txtResult = exporter.exportSingle(data, ExportFormat.PLAIN_TEXT)
        assertEquals("Great Book-Notes.txt", txtResult.filename)
        assertTrue(txtResult.content.contains("Great Book"))
    }

    @Test
    fun exportBatchProducesVaultZip() {
        val libraryRepo = LibraryRepositoryImpl()
        val annotationRepo = FakeAnnotationRepository()
        val exporter = NotesExporter(libraryRepo, annotationRepo)

        val data = BookExportData(
            book = LibraryBook(stableId = "b1", title = "Great Book"),
            annotations = listOf(
                Annotation(1, "b1", """{"text":{"exact":"quote"}}""", "#FFEB3B", "note", 0L)
            ),
            collections = emptyList()
        )

        val batchResult = exporter.exportBatch(listOf(data), ExportFormat.VAULT_ZIP)
        assertTrue(batchResult.filename.startsWith("Leafline-Notes-") && batchResult.filename.endsWith(".zip"))
        assertNotNull(batchResult.binaryPayload)
        assertTrue(batchResult.binaryPayload!!.isNotEmpty())
    }

    @Test
    fun writeSingleToStreamWritesExpectedBytes() = runTest {
        val libraryRepo = LibraryRepositoryImpl()
        val annotationRepo = FakeAnnotationRepository()
        val exporter = NotesExporter(libraryRepo, annotationRepo)

        val singleResult = SingleExportResult(
            format = ExportFormat.PLAIN_TEXT,
            filename = "test.txt",
            content = "Sample text content",
            mimeType = "text/plain",
            highlightCount = 1
        )

        val stream = ByteArrayOutputStream()
        exporter.writeSingleToStream(singleResult, stream)
        val written = stream.toString(Charsets.UTF_8.name())
        assertEquals("Sample text content", written)
    }

    @Test
    fun writeBatchToStreamWritesBinaryPayload() = runTest {
        val libraryRepo = LibraryRepositoryImpl()
        val annotationRepo = FakeAnnotationRepository()
        val exporter = NotesExporter(libraryRepo, annotationRepo)

        val payload = "dummy zip bytes".toByteArray(Charsets.UTF_8)
        val batchResult = BatchExportResult(
            format = ExportFormat.VAULT_ZIP,
            filename = "vault.zip",
            bookCount = 1,
            highlightCount = 1,
            binaryPayload = payload,
            mimeType = "application/zip"
        )

        val stream = ByteArrayOutputStream()
        exporter.writeBatchToStream(batchResult, stream)
        val written = stream.toByteArray()
        assertEquals("dummy zip bytes", String(written, Charsets.UTF_8))
    }
}
