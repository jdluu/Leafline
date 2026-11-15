package com.jdluu.leafline.export

import com.google.gson.JsonParser
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.Annotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class NotesExportFormatterTest {

    private val sampleBook = LibraryBook(
        stableId = "book-42",
        title = "Dune",
        authors = listOf("Frank Herbert"),
        rating = 5,
        readingStatus = ReadingStatus.FINISHED,
        pageCount = 688,
        tags = listOf("sci-fi", "classic"),
        lastLocatorJson = """{"locations":{"totalProgression":1.0}}""",
        lastReadAtEpochMillis = 1700000000000L
    )

    private val sampleAnnotation1 = Annotation(
        id = 1,
        bookId = "book-42",
        locatorJson = """
            {
                "href": "chapter1.xhtml",
                "title": "Chapter One",
                "locations": {
                    "totalProgression": 0.05
                },
                "text": {
                    "exact": "I must not fear. Fear is the mind-killer."
                }
            }
        """.trimIndent(),
        colorHex = "#55FDD835", // Yellow tint
        note = "The famous litany against fear.",
        createdAt = 1700001000000L
    )

    private val sampleAnnotation2 = Annotation(
        id = 2,
        bookId = "book-42",
        locatorJson = """
            {
                "href": "chapter3.xhtml",
                "title": "Chapter Three",
                "text": {
                    "exact": "The mystery of life isn't a problem to solve, but a reality to experience."
                }
            }
        """.trimIndent(),
        colorHex = "#55E91E63", // Pink tint
        note = "",
        createdAt = 1700002000000L
    )

    private val bookExportData = BookExportData(
        book = sampleBook,
        annotations = listOf(sampleAnnotation1, sampleAnnotation2),
        collections = listOf("Favorites", "Sci-Fi Masterpieces")
    )

    @Test
    fun parseHighlightExtractsExcerptAndMetadata() {
        val parsed = NotesExportFormatter.parseHighlight(sampleAnnotation1)
        assertEquals("I must not fear. Fear is the mind-killer.", parsed.excerpt)
        assertEquals("Chapter One", parsed.chapterTitle)
        assertEquals(5, (parsed.progressFraction?.times(100))?.toInt())
        assertEquals("Yellow", parsed.colorLabel)
        assertEquals("The famous litany against fear.", parsed.note)
    }

    @Test
    fun parseHighlightHandlesFallbackExcerpt() {
        val emptyTextAnnotation = Annotation(
            id = 3,
            bookId = "book-42",
            locatorJson = """{"href":"ch2.xhtml","title":"Chapter Two"}""",
            colorHex = "#5543A047",
            note = "Note only",
            createdAt = 100L
        )
        val parsed = NotesExportFormatter.parseHighlight(emptyTextAnnotation)
        assertEquals("Green", parsed.colorLabel)
        assertEquals("Chapter Two", parsed.chapterTitle)
        assertEquals("Note only", parsed.note)
    }

    @Test
    fun formatSingleBookMarkdownGeneratesValidFrontmatterAndQuotes() {
        val md = NotesExportFormatter.formatSingleBookMarkdown(bookExportData)

        assertTrue(md.startsWith("---\n"))
        assertTrue(md.contains("title: \"Dune\""))
        assertTrue(md.contains("authors:\n  - \"Frank Herbert\""))
        assertTrue(md.contains("reading_status: finished"))
        assertTrue(md.contains("rating: 5"))
        assertTrue(md.contains("tags:\n  - \"sci-fi\"\n  - \"classic\""))
        assertTrue(md.contains("collections:\n  - \"Favorites\"\n  - \"Sci-Fi Masterpieces\""))
        assertTrue(md.contains("# Dune"))
        assertTrue(md.contains("### Chapter One"))
        assertTrue(md.contains("> I must not fear. Fear is the mind-killer."))
        assertTrue(md.contains("**Note:** The famous litany against fear."))
        assertTrue(md.contains("Color: Yellow"))
        assertTrue(md.contains("5% read"))
        assertTrue(md.contains("> The mystery of life isn't a problem to solve, but a reality to experience."))
    }

    @Test
    fun formatSingleBookJsonGeneratesValidSchema() {
        val jsonString = NotesExportFormatter.formatSingleBookJson(bookExportData)
        val json = JsonParser.parseString(jsonString).asJsonObject

        val bookObj = json.getAsJsonObject("book")
        assertEquals("book-42", bookObj.get("stableId").asString)
        assertEquals("Dune", bookObj.get("title").asString)
        assertEquals("Frank Herbert", bookObj.getAsJsonArray("authors").get(0).asString)
        assertEquals(5, bookObj.get("rating").asInt)
        assertEquals("finished", bookObj.get("readingStatus").asString)

        val highlights = json.getAsJsonArray("highlights")
        assertEquals(2, highlights.size())
        val h1 = highlights.get(0).asJsonObject
        assertEquals("I must not fear. Fear is the mind-killer.", h1.get("excerpt").asString)
        assertEquals("The famous litany against fear.", h1.get("note").asString)
        assertEquals("Yellow", h1.get("colorLabel").asString)
        assertEquals(0.05, h1.get("progressFraction").asDouble, 0.001)
    }

    @Test
    fun formatSingleBookPlainTextIsFormattedCleanly() {
        val text = NotesExportFormatter.formatSingleBookPlainText(bookExportData)
        assertTrue(text.contains("Dune"))
        assertTrue(text.contains("by Frank Herbert"))
        assertTrue(text.contains("\"I must not fear. Fear is the mind-killer.\""))
        assertTrue(text.contains("Note: The famous litany against fear."))
        assertTrue(text.contains("[Chapter One]"))
    }

    @Test
    fun formatBatchCombinedMarkdownIncludesTocAndMultipleBooks() {
        val book2 = LibraryBook(
            stableId = "book-99",
            title = "Foundation",
            authors = listOf("Isaac Asimov")
        )
        val book2Data = BookExportData(
            book = book2,
            annotations = emptyList(),
            collections = emptyList()
        )

        val combinedMd = NotesExportFormatter.formatBatchCombinedMarkdown(listOf(bookExportData, book2Data))
        assertTrue(combinedMd.contains("# Leafline Library Notes"))
        assertTrue(combinedMd.contains("## Table of Contents"))
        assertTrue(combinedMd.contains("- [Dune by Frank Herbert](#dune) (2 highlights)"))
        assertTrue(combinedMd.contains("- [Foundation by Isaac Asimov](#foundation) (0 highlights)"))
        assertTrue(combinedMd.contains("# Dune"))
        assertTrue(combinedMd.contains("# Foundation"))
    }

    @Test
    fun formatBatchJsonCreatesValidJsonArray() {
        val batchJson = NotesExportFormatter.formatBatchJson(listOf(bookExportData))
        val array = JsonParser.parseString(batchJson).asJsonObject.getAsJsonArray("books")
        assertEquals(1, array.size())
        val firstBook = array.get(0).asJsonObject.getAsJsonObject("book")
        assertEquals("Dune", firstBook.get("title").asString)
    }

    @Test
    fun buildVaultZipGeneratesValidArchiveWithObsidianWikiLinks() {
        val zipBytes = NotesExportFormatter.buildVaultZip(listOf(bookExportData))
        assertTrue(zipBytes.isNotEmpty())

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val content = zis.bufferedReader(Charsets.UTF_8).readText()
                entries[entry.name] = content
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("Index.md"))
        assertTrue(entries.containsKey("Dune - Frank Herbert.md"))

        val indexContent = entries["Index.md"]!!
        assertTrue(indexContent.contains("# Leafline Reading Vault"))
        assertTrue(indexContent.contains("[[Dune - Frank Herbert]]"))

        val duneContent = entries["Dune - Frank Herbert.md"]!!
        assertTrue(duneContent.contains("title: \"Dune\""))
        assertTrue(duneContent.contains("I must not fear. Fear is the mind-killer."))
    }

    @Test
    fun sanitizeFileNameRemovesInvalidChars() {
        assertEquals("My Book_ Title_", NotesExportFormatter.sanitizeFileName("My Book: Title?"))
        assertEquals("A_B_C_____", NotesExportFormatter.sanitizeFileName("A/B\\C*<>|\""))
        assertEquals("Untitled", NotesExportFormatter.sanitizeFileName("???:::***"))
    }
}
