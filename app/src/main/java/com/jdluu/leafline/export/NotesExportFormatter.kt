package com.jdluu.leafline.export

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.LocatorIdentity
import com.jdluu.leafline.sync.ReadingProgressMath
import com.jdluu.leafline.theme.HIGHLIGHT_TINTS
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Formats books, annotations, and reading metadata into Obsidian Markdown,
 * structured JSON, plain text, and complete multi-file Obsidian Vault ZIP archives.
 */
object NotesExportFormatter {

    data class ParsedHighlight(
        val id: Long,
        val excerpt: String,
        val note: String?,
        val colorHex: String,
        val colorLabel: String,
        val chapterTitle: String?,
        val progressFraction: Double?,
        val createdAtEpochMillis: Long
    )

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val humanDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val frontmatterDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val readwiseDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Parses an annotation into normalized highlight content with chapter,
     * progress, color name, and excerpt.
     */
    fun parseHighlight(annotation: Annotation): ParsedHighlight {
        var excerpt = ""
        var chapterTitle: String? = null
        var progressFraction: Double? = null

        try {
            val element = com.google.gson.JsonParser.parseString(annotation.locatorJson)
            if (element.isJsonObject) {
                val root = element.asJsonObject
                if (root.has("text") && root.get("text").isJsonObject) {
                    val textObj = root.getAsJsonObject("text")
                    val exact = if (textObj.has("exact") && !textObj.get("exact").isJsonNull) textObj.get("exact").asString.takeIf { it.isNotBlank() } else null
                    val highlight = if (textObj.has("highlight") && !textObj.get("highlight").isJsonNull) textObj.get("highlight").asString.takeIf { it.isNotBlank() } else null
                    excerpt = exact ?: highlight ?: ""
                }

                val title = if (root.has("title") && !root.get("title").isJsonNull) root.get("title").asString.takeIf { it.isNotBlank() } else null
                val href = if (root.has("href") && !root.get("href").isJsonNull) root.get("href").asString.takeIf { it.isNotBlank() } else null
                chapterTitle = title ?: href?.substringAfterLast('/')?.substringBefore('#')?.takeIf { it.isNotBlank() }

                if (root.has("locations") && root.get("locations").isJsonObject) {
                    val locations = root.getAsJsonObject("locations")
                    val totalProg = if (locations.has("totalProgression") && !locations.get("totalProgression").isJsonNull) locations.get("totalProgression").asDouble else null
                    val prog = if (locations.has("progression") && !locations.get("progression").isJsonNull) locations.get("progression").asDouble else null
                    if (totalProg != null && !totalProg.isNaN()) progressFraction = totalProg.coerceIn(0.0, 1.0)
                    else if (prog != null && !prog.isNaN()) progressFraction = prog.coerceIn(0.0, 1.0)
                }
            }
        } catch (_: Exception) {}

        if (excerpt.isBlank()) {
            excerpt = LocatorIdentity.displayTitle(annotation.locatorJson) ?: "Highlight"
        }

        return ParsedHighlight(
            id = annotation.id,
            excerpt = excerpt.trim(),
            note = annotation.note?.trim()?.takeIf { it.isNotEmpty() },
            colorHex = annotation.colorHex,
            colorLabel = colorLabelFor(annotation.colorHex),
            chapterTitle = chapterTitle,
            progressFraction = progressFraction,
            createdAtEpochMillis = annotation.createdAt
        )
    }

    /**
     * Formats a single book and its annotations into an Obsidian-compatible Markdown
     * document with rich YAML frontmatter, quote blocks, notes, and chapter headings.
     */
    fun formatSingleBookMarkdown(data: BookExportData): String = buildString {
        val now = Date()
        val parsedHighlights = data.annotations.map(::parseHighlight)

        // 1. YAML Frontmatter
        appendLine("---")
        appendLine("title: ${escapeYamlString(data.book.title)}")
        if (data.book.authors.isNotEmpty()) {
            appendLine("authors:")
            data.book.authors.forEach { author ->
                appendLine("  - ${escapeYamlString(author)}")
            }
        }
        if (data.book.tags.isNotEmpty()) {
            appendLine("tags:")
            data.book.tags.forEach { tag ->
                appendLine("  - ${escapeYamlString(tag)}")
            }
        }
        if (data.collections.isNotEmpty()) {
            appendLine("collections:")
            data.collections.forEach { col ->
                appendLine("  - ${escapeYamlString(col)}")
            }
        }
        appendLine("reading_status: ${data.book.readingStatus.name.lowercase(Locale.US)}")

        val progressPercent = ReadingProgressMath.percentageFromLocator(data.book.lastLocatorJson)
        if (progressPercent != null) {
            appendLine("progress: \"${Math.round(progressPercent)}%\"")
        }
        if (data.book.rating != null) {
            appendLine("rating: ${data.book.rating}")
        }
        if (data.book.pageCount != null && data.book.pageCount > 0) {
            appendLine("page_count: ${data.book.pageCount}")
        }
        if (data.book.lastReadAtEpochMillis != null) {
            appendLine("last_read: ${frontmatterDateFormat.format(Date(data.book.lastReadAtEpochMillis))}")
        }
        appendLine("highlights_count: ${parsedHighlights.size}")
        appendLine("exported_at: ${isoDateFormat.format(now)}")
        appendLine("---")
        appendLine()

        // 2. Document Header
        appendLine("# ${data.book.title}")
        if (data.book.authors.isNotEmpty()) {
            appendLine("*By ${data.book.authors.joinToString(", ")}*")
        }
        appendLine()

        if (!data.book.description.isNullOrBlank()) {
            appendLine("> ${data.book.description.trim().replace("\n", "\n> ")}")
            appendLine()
        }

        // 3. Highlights & Notes
        appendLine("## Highlights & Notes")
        appendLine()

        if (parsedHighlights.isEmpty()) {
            appendLine("*No highlights or notes recorded for this book.*")
        } else {
            parsedHighlights.forEachIndexed { index, highlight ->
                if (!highlight.chapterTitle.isNullOrBlank()) {
                    appendLine("### ${highlight.chapterTitle}")
                    appendLine()
                }

                // Blockquote highlight
                val formattedExcerpt = highlight.excerpt.replace("\n", "\n> ")
                appendLine("> $formattedExcerpt")
                appendLine()

                // Note if present
                if (!highlight.note.isNullOrBlank()) {
                    appendLine("**Note:** ${highlight.note}")
                    appendLine()
                }

                // Meta footnote
                val dateStr = humanDateFormat.format(Date(highlight.createdAtEpochMillis))
                val progressStr = highlight.progressFraction?.let { " • ${Math.round(it * 100)}% read" } ?: ""
                val colorStr = " • Color: ${highlight.colorLabel}"
                appendLine("*Highlighted on $dateStr$progressStr$colorStr*")

                if (index < parsedHighlights.lastIndex) {
                    appendLine()
                    appendLine("---")
                    appendLine()
                }
            }
        }
    }

    /**
     * Formats a single book and its annotations into structured JSON.
     */
    fun formatSingleBookJson(data: BookExportData): String {
        val root = JsonObject()
        root.addProperty("version", 1)
        root.addProperty("exportedAt", isoDateFormat.format(Date()))

        val bookObj = JsonObject()
        bookObj.addProperty("stableId", data.book.stableId)
        bookObj.addProperty("title", data.book.title)

        val authorsArray = JsonArray()
        data.book.authors.forEach { authorsArray.add(it) }
        bookObj.add("authors", authorsArray)

        val tagsArray = JsonArray()
        data.book.tags.forEach { tagsArray.add(it) }
        bookObj.add("tags", tagsArray)

        val colArray = JsonArray()
        data.collections.forEach { colArray.add(it) }
        bookObj.add("collections", colArray)

        bookObj.addProperty("readingStatus", data.book.readingStatus.name.lowercase(Locale.US))
        data.book.rating?.let { bookObj.addProperty("rating", it) }
        ReadingProgressMath.percentageFromLocator(data.book.lastLocatorJson)?.let {
            bookObj.addProperty("progressPercentage", it)
        }
        data.book.pageCount?.let { bookObj.addProperty("pageCount", it) }
        data.book.lastReadAtEpochMillis?.let { bookObj.addProperty("lastReadAt", it) }
        root.add("book", bookObj)

        val parsed = data.annotations.map(::parseHighlight)
        root.addProperty("highlightsCount", parsed.size)

        val highlightsArray = JsonArray()
        parsed.forEach { highlight ->
            val hObj = JsonObject()
            hObj.addProperty("id", highlight.id)
            hObj.addProperty("excerpt", highlight.excerpt)
            highlight.note?.let { hObj.addProperty("note", it) }
            hObj.addProperty("colorHex", highlight.colorHex)
            hObj.addProperty("colorLabel", highlight.colorLabel)
            highlight.chapterTitle?.let { hObj.addProperty("chapter", it) }
            highlight.progressFraction?.let { hObj.addProperty("progressFraction", it) }
            hObj.addProperty("createdAt", highlight.createdAtEpochMillis)
            hObj.addProperty("createdAtFormatted", humanDateFormat.format(Date(highlight.createdAtEpochMillis)))
            highlightsArray.add(hObj)
        }
        root.add("highlights", highlightsArray)

        return GsonBuilder().setPrettyPrinting().create().toJson(root)
    }

    /**
     * Formats a single book and its annotations into simple plain text.
     */
    fun formatSingleBookPlainText(data: BookExportData): String = buildString {
        appendLine(data.book.title)
        if (data.book.authors.isNotEmpty()) {
            appendLine("by ${data.book.authors.joinToString(", ")}")
        }
        appendLine("=".repeat(data.book.title.length.coerceAtLeast(20)))
        appendLine()

        val parsed = data.annotations.map(::parseHighlight)
        if (parsed.isEmpty()) {
            appendLine("No highlights or notes recorded.")
        } else {
            parsed.forEachIndexed { index, highlight ->
                if (!highlight.chapterTitle.isNullOrBlank()) {
                    appendLine("[${highlight.chapterTitle}]")
                }
                appendLine("\"${highlight.excerpt}\"")
                if (!highlight.note.isNullOrBlank()) {
                    appendLine("Note: ${highlight.note}")
                }
                val dateStr = humanDateFormat.format(Date(highlight.createdAtEpochMillis))
                val prog = highlight.progressFraction?.let { " (${Math.round(it * 100)}%)" } ?: ""
                appendLine("- $dateStr$prog")
                if (index < parsed.lastIndex) {
                    appendLine()
                }
            }
        }
    }

    /**
     * Formats multiple books and their annotations into a combined master Markdown document.
     */
    fun formatBatchCombinedMarkdown(booksData: List<BookExportData>): String = buildString {
        val now = Date()
        val totalHighlights = booksData.sumOf { it.annotations.size }

        appendLine("---")
        appendLine("title: \"Leafline Library Notes Export\"")
        appendLine("books_count: ${booksData.size}")
        appendLine("highlights_count: $totalHighlights")
        appendLine("exported_at: ${isoDateFormat.format(now)}")
        appendLine("---")
        appendLine()
        appendLine("# Leafline Library Notes")
        appendLine("*Exported on ${humanDateFormat.format(now)} • ${booksData.size} books, $totalHighlights highlights*")
        appendLine()

        // Table of Contents
        appendLine("## Table of Contents")
        booksData.forEach { data ->
            val authorPart = if (data.book.authors.isNotEmpty()) " by ${data.book.authors.joinToString(", ")}" else ""
            val count = data.annotations.size
            appendLine("- [${data.book.title}$authorPart](#${slugify(data.book.title)}) ($count highlights)")
        }
        appendLine()
        appendLine("---")
        appendLine()

        // Individual Book Sections
        booksData.forEachIndexed { index, data ->
            appendLine(formatSingleBookMarkdown(data))
            if (index < booksData.lastIndex) {
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }

    /**
     * Formats multiple books into a combined structured JSON array.
     */
    fun formatBatchJson(booksData: List<BookExportData>): String {
        val root = JsonObject()
        root.addProperty("version", 1)
        root.addProperty("exportedAt", isoDateFormat.format(Date()))
        root.addProperty("booksCount", booksData.size)
        root.addProperty("highlightsCount", booksData.sumOf { it.annotations.size })

        val booksArray = JsonArray()
        booksData.forEach { data ->
            val bookJson = formatSingleBookJson(data)
            val parsedObj = com.google.gson.JsonParser.parseString(bookJson).asJsonObject
            booksArray.add(parsedObj)
        }
        root.add("books", booksArray)

        return GsonBuilder().setPrettyPrinting().create().toJson(root)
    }

    /**
     * Formats a single book and its annotations into a Readwise-compatible CSV.
     * Columns: Highlight,Book Title,Book Author,URL,Note,Location,Date
     */
    fun formatSingleBookReadwiseCsv(data: BookExportData): String = buildString {
        appendLine("Highlight,Book Title,Book Author,URL,Note,Location,Date")
        val parsed = data.annotations.map(::parseHighlight)
        parsed.forEach { highlight ->
            append(escapeCsvField(highlight.excerpt)).append(',')
            append(escapeCsvField(data.book.title)).append(',')
            append(escapeCsvField(data.book.authors.joinToString(", "))).append(',')
            append(',').append(escapeCsvField(highlight.note.orEmpty())).append(',')
            val location = highlight.chapterTitle ?: highlight.progressFraction?.let { "${Math.round(it * 100)}%" }.orEmpty()
            append(escapeCsvField(location)).append(',')
            append(escapeCsvField(readwiseDateFormat.format(Date(highlight.createdAtEpochMillis))))
            appendLine()
        }
    }

    /**
     * Formats multiple books and their annotations into a combined Readwise-compatible CSV.
     */
    fun formatBatchReadwiseCsv(booksData: List<BookExportData>): String = buildString {
        appendLine("Highlight,Book Title,Book Author,URL,Note,Location,Date")
        booksData.forEach { data ->
            val parsed = data.annotations.map(::parseHighlight)
            parsed.forEach { highlight ->
                append(escapeCsvField(highlight.excerpt)).append(',')
                append(escapeCsvField(data.book.title)).append(',')
                append(escapeCsvField(data.book.authors.joinToString(", "))).append(',')
                append(',').append(escapeCsvField(highlight.note.orEmpty())).append(',')
                val location = highlight.chapterTitle ?: highlight.progressFraction?.let { "${Math.round(it * 100)}%" }.orEmpty()
                append(escapeCsvField(location)).append(',')
                append(escapeCsvField(readwiseDateFormat.format(Date(highlight.createdAtEpochMillis))))
                appendLine()
            }
        }
    }

    /**
     * Formats a single book's annotations into Anki flashcard TSV format with headers:
     * #separator:tab
     * #html:true
     * #tags column:4
     */
    fun formatSingleBookAnkiTsv(data: BookExportData): String = buildString {
        appendLine("#separator:tab")
        appendLine("#html:true")
        appendLine("#tags column:4")
        val parsed = data.annotations.map(::parseHighlight)
        parsed.forEach { highlight ->
            appendAnkiCardRow(this, data, highlight)
        }
    }

    /**
     * Formats multiple books and their annotations into a combined Anki flashcard TSV deck.
     */
    fun formatBatchAnkiTsv(booksData: List<BookExportData>): String = buildString {
        appendLine("#separator:tab")
        appendLine("#html:true")
        appendLine("#tags column:4")
        booksData.forEach { data ->
            val parsed = data.annotations.map(::parseHighlight)
            parsed.forEach { highlight ->
                appendAnkiCardRow(this, data, highlight)
            }
        }
    }

    private fun appendAnkiCardRow(builder: StringBuilder, data: BookExportData, highlight: ParsedHighlight) {
        val titleHtml = escapeHtml(data.book.title)
        val authorText = if (data.book.authors.isNotEmpty()) " by ${data.book.authors.joinToString(", ")}" else ""
        val chapterText = highlight.chapterTitle?.let { " ($it)" }.orEmpty()

        val frontHtml = if (!highlight.note.isNullOrBlank()) {
            "<b>${escapeHtml(highlight.note)}</b><br><br><small style=\"color:gray;\">From <i>$titleHtml</i>$chapterText</small>"
        } else {
            "Quote from <i>$titleHtml</i>$chapterText"
        }

        val backHtml = "<blockquote>${escapeHtml(highlight.excerpt)}</blockquote>"
        val source = cleanSingleLine("${data.book.title}$authorText$chapterText")

        val tagList = mutableListOf("leafline")
        val bookTag = slugify(data.book.title).takeIf { it.isNotBlank() }
        if (bookTag != null) tagList.add("book_$bookTag")
        if (highlight.colorLabel.isNotBlank()) {
            tagList.add("color_${slugify(highlight.colorLabel)}")
        }
        data.collections.forEach { col ->
            val s = slugify(col)
            if (s.isNotBlank()) tagList.add("collection_$s")
        }
        data.book.tags.forEach { tag ->
            val s = slugify(tag)
            if (s.isNotBlank()) tagList.add(s)
        }
        val tags = tagList.distinct().joinToString(" ")

        builder.append(cleanField(frontHtml)).append('\t')
            .append(cleanField(backHtml)).append('\t')
            .append(source).append('\t')
            .append(tags)
            .append('\n')
    }

    /**
     * Builds an Obsidian Vault ZIP archive containing individual .md files per book
     * and an Index.md linking all books with Obsidian [[WikiLinks]].
     */
    fun buildVaultZip(booksData: List<BookExportData>): ByteArray {
        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zip ->
            val usedFilenames = mutableSetOf<String>()

            // 1. Write individual book notes
            val bookEntries = mutableListOf<Pair<String, BookExportData>>()
            booksData.forEach { data ->
                val baseName = sanitizeFileName(
                    if (data.book.authors.isNotEmpty()) {
                        "${data.book.title} - ${data.book.authors.first()}"
                    } else {
                        data.book.title
                    }
                )

                var finalName = "$baseName.md"
                var counter = 2
                while (usedFilenames.contains(finalName.lowercase(Locale.US))) {
                    finalName = "$baseName ($counter).md"
                    counter++
                }
                usedFilenames.add(finalName.lowercase(Locale.US))
                bookEntries.add(finalName to data)

                val markdownContent = formatSingleBookMarkdown(data)
                zip.putNextEntry(ZipEntry(finalName))
                zip.write(markdownContent.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            // 2. Write Index.md
            val indexContent = buildString {
                val now = Date()
                val totalHighlights = booksData.sumOf { it.annotations.size }

                appendLine("---")
                appendLine("title: \"Leafline Reading Vault\"")
                appendLine("books_count: ${booksData.size}")
                appendLine("highlights_count: $totalHighlights")
                appendLine("exported_at: ${isoDateFormat.format(now)}")
                appendLine("---")
                appendLine()
                appendLine("# Leafline Reading Vault")
                appendLine("*Exported on ${humanDateFormat.format(now)} • ${booksData.size} books, $totalHighlights highlights*")
                appendLine()
                appendLine("## Books")
                appendLine()
                bookEntries.forEach { (fileName, data) ->
                    val noteName = fileName.removeSuffix(".md")
                    val authors = if (data.book.authors.isNotEmpty()) " by ${data.book.authors.joinToString(", ")}" else ""
                    val count = data.annotations.size
                    appendLine("- [[$noteName]]$authors — $count highlights")
                }
            }

            zip.putNextEntry(ZipEntry("Index.md"))
            zip.write(indexContent.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        return byteOut.toByteArray()
    }

    /**
     * Sanitizes file names for safe use across file systems (removes forbidden characters).
     */
    fun sanitizeFileName(name: String): String {
        val sanitized = name.replace(Regex("""[\\\\/:*?\"<>|]"""), "_").trim()
        if (sanitized.all { it == '_' || it.isWhitespace() }) {
            return "Untitled"
        }
        return sanitized.take(100)
    }

    /**
     * Suggests a default filename for single book export.
     */
        fun defaultSingleFilename(bookTitle: String, format: ExportFormat): String {
        val clean = sanitizeFileName(bookTitle)
        return "$clean-Notes.${format.extension}"
    }

    /**
     * Suggests a default filename for batch export.
     */
    fun defaultBatchFilename(format: ExportFormat): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return "Leafline-Notes-$date.${format.extension}"
    }

    private fun colorLabelFor(hex: String): String {
        val direct = HIGHLIGHT_TINTS.firstOrNull { it.hex.equals(hex, ignoreCase = true) }
        if (direct != null) return direct.label
        val stripped = hex.removePrefix("#")
        val match = HIGHLIGHT_TINTS.firstOrNull { tint ->
            val tintStripped = tint.hex.removePrefix("#")
            tintStripped.endsWith(stripped, ignoreCase = true) || stripped.endsWith(tintStripped, ignoreCase = true)
        }
        return match?.label ?: "Highlight"
    }

    private fun escapeYamlString(value: String): String {
        val escaped = value.replace("\"", "\\\"")
        return "\"$escaped\""
    }

    private fun slugify(text: String): String {
        return text.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
    }

    fun escapeCsvField(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("\t", "    ")
            .replace("\r\n", "<br>")
            .replace("\n", "<br>")
            .replace("\r", "<br>")
    }

    private fun cleanField(html: String): String {
        return html.replace("\t", "    ").replace("\r\n", "<br>").replace("\n", "<br>").replace("\r", "<br>")
    }

    private fun cleanSingleLine(text: String): String {
        return text.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').trim()
    }
}
