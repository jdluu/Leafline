package com.jdluu.leafline.reader.footnotes

import java.util.regex.Pattern

/**
 * Extracts and formats footnote/endnote content from EPUB HTML documents.
 *
 * Handles both modern EPUB 3 semantic markups (`<aside epub:type="footnote">`,
 * `role="doc-footnote"`) and traditional EPUB 2 anchor patterns (`<p id="fn1">`,
 * `<a id="fn1"></a><p>...</p>`, `<li id="note-1">`).
 */
object FootnoteExtractor {

    private val BLOCK_TAG_PATTERN = Pattern.compile(
        """<(p|div|li|aside|dd|section)\b[^>]*>(.*?)</\1>""",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )

    private val HEADING_PATTERN = Pattern.compile(
        """<h[1-6]\b[^>]*>(.*?)</h[1-6]>""",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )

    private val LEADING_SUP_PATTERN = Pattern.compile(
        """^\s*<sup\b[^>]*>(.*?)</sup>""",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )

    private val NUMBER_PATTERN = Pattern.compile("""(\d+)""")

    private val BACKLINK_PATTERNS = listOf(
        // Symbols: ↩, ↵, ^, [back], return to text
        Pattern.compile(
            """<a\b[^>]*>(?:\s*(?:↩|↵|&#8617;|&#x21a9;|\^|\[back\]|return to text|back)\s*)</a>""",
            Pattern.CASE_INSENSITIVE
        ),
        // Anchors targeting backlink references e.g. href="#fnref1" or href="#ref1"
        Pattern.compile(
            """<a\b[^>]*href=["\x27]#[^"\x27]*(?:ref|back)[^"\x27]*["\x27][^>]*>.*?</a>""",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
    )

    /**
     * Extracts footnote content matching [fragment] from [html].
     *
     * @param html The raw XHTML / HTML content of the target chapter/notes resource.
     * @param fragment The anchor identifier without '#' (e.g. "fn1" or "note-12").
     * @param targetLocatorJson Optional serialized locator for jumping to the target.
     * @return [FootnoteContent] if located and non-empty, or null if not found.
     */
    fun extractFootnote(
        html: String,
        fragment: String,
        targetLocatorJson: String? = null
    ): FootnoteContent? {
        val cleanId = fragment.removePrefix("#").trim()
        if (cleanId.isBlank() || html.isBlank()) return null

        val rawInnerHtml = findElementInnerHtml(html, cleanId) ?: return null
        val title = extractTitle(rawInnerHtml, cleanId)
        val text = cleanFootnoteText(rawInnerHtml)

        if (text.isBlank()) return null
        return FootnoteContent(
            id = cleanId,
            title = title,
            text = text,
            targetLocatorJson = targetLocatorJson
        )
    }

    private fun findElementInnerHtml(html: String, targetId: String): String? {
        // Try exact match first, then case-insensitive
        val exactPattern = Pattern.compile(
            """<([a-zA-Z0-9]+)\b([^>]*\b(?:id|name)=["\x27]${Pattern.quote(targetId)}["\x27][^>]*)>""",
            Pattern.DOTALL
        )
        var matcher = exactPattern.matcher(html)
        if (!matcher.find()) {
            val ciPattern = Pattern.compile(
                """<([a-zA-Z0-9]+)\b([^>]*\b(?:id|name)=["\x27]${Pattern.quote(targetId)}["\x27][^>]*)>""",
                Pattern.DOTALL or Pattern.CASE_INSENSITIVE
            )
            matcher = ciPattern.matcher(html)
            if (!matcher.find()) return null
        }

        val tagName = matcher.group(1)?.lowercase() ?: return null
        val attrs = matcher.group(2) ?: ""
        val startTagEnd = matcher.end()

        // Check if self-closing
        val isSelfClosing = attrs.trim().endsWith("/")
        var innerHtml: String? = null

        if (!isSelfClosing) {
            innerHtml = extractBalancedInnerHtml(html, tagName, startTagEnd)
        }

        // If inner is empty or this is an empty <a> anchor tag, look for following block
        if ((innerHtml == null || innerHtml.isBlank()) && (tagName == "a" || isSelfClosing)) {
            val following = html.substring(startTagEnd)
            val blockMatcher = BLOCK_TAG_PATTERN.matcher(following)
            if (blockMatcher.find() && blockMatcher.start() < 200) {
                // Adjacent block within 200 characters
                innerHtml = blockMatcher.group(2)
            }
        }

        return innerHtml
    }

    private fun extractBalancedInnerHtml(html: String, tagName: String, innerStart: Int): String {
        var depth = 1
        var pos = innerStart
        val tagPattern = Pattern.compile("""<(/?)(${Pattern.quote(tagName)})\b([^>]*)>""", Pattern.CASE_INSENSITIVE)

        while (pos < html.length) {
            val matcher = tagPattern.matcher(html)
            if (!matcher.find(pos)) {
                return html.substring(innerStart)
            }
            val isClosing = matcher.group(1)?.isNotEmpty() == true
            val attrs = matcher.group(3) ?: ""
            val isSelfClosing = attrs.trim().endsWith("/")

            if (isClosing) {
                depth--
                if (depth == 0) {
                    return html.substring(innerStart, matcher.start())
                }
            } else if (!isSelfClosing) {
                depth++
            }
            pos = matcher.end()
        }
        return html.substring(innerStart)
    }

    private fun extractTitle(innerHtml: String, fragment: String): String {
        val headingMatcher = HEADING_PATTERN.matcher(innerHtml)
        if (headingMatcher.find()) {
            val headingText = stripTags(headingMatcher.group(1).orEmpty()).trim()
            if (headingText.isNotBlank()) return headingText
        }

        val supMatcher = LEADING_SUP_PATTERN.matcher(innerHtml)
        if (supMatcher.find()) {
            val supText = stripTags(supMatcher.group(1).orEmpty()).trim()
            if (supText.isNotBlank()) return "Note $supText"
        }

        // Check for number in fragment (e.g. fn1 -> Note 1, note-12 -> Note 12)
        val numMatcher = NUMBER_PATTERN.matcher(fragment)
        if (numMatcher.find()) {
            return "Note ${numMatcher.group(1)}"
        }

        return "Footnote"
    }

    private fun cleanFootnoteText(innerHtml: String): String {
        var cleaned = innerHtml

        // Remove backlink anchors
        for (pattern in BACKLINK_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceAll("")
        }

        // Replace line breaks and block openings/closings with newlines
        cleaned = cleaned.replace(Regex("""<(?:br|p|div|li|aside|section)[^>]*>""", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("""</(?:p|div|li|aside|section)>""", RegexOption.IGNORE_CASE), "\n")

        // Strip remaining HTML tags
        cleaned = stripTags(cleaned)

        // Decode HTML entities
        cleaned = decodeHtmlEntities(cleaned)

        // Normalize lines and whitespace
        val lines = cleaned.split("\n")
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotEmpty() }

        return lines.joinToString("\n\n")
    }

    private fun stripTags(input: String): String {
        return input.replace(Regex("""<[^>]+>"""), "")
    }

    private fun decodeHtmlEntities(input: String): String {
        var s = input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")

        // Numeric decimal entities &#123;
        s = Regex("""&#(\d+);""").replace(s) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null && code in 1..0xFFFF) {
                code.toChar().toString()
            } else {
                match.value
            }
        }

        // Numeric hex entities &#x1a;
        s = Regex("""&#x([0-9a-fA-F]+);""").replace(s) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null && code in 1..0xFFFF) {
                code.toChar().toString()
            } else {
                match.value
            }
        }

        return s
    }
}
