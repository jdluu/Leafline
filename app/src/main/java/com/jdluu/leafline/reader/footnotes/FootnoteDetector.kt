package com.jdluu.leafline.reader.footnotes

import java.util.regex.Pattern

/**
 * Determines whether an internal link or target fragment refers to a footnote
 * or endnote rather than normal chapter / body progression.
 */
object FootnoteDetector {

    private val FOOTNOTE_FRAGMENT_PATTERNS = listOf(
        Pattern.compile("""^fn[-_0-9].*""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""^footnote[-_0-9]?.*""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""^note[-_0-9]?.*""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""^endnote[-_0-9]?.*""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""^n[0-9]+$""", Pattern.CASE_INSENSITIVE),
        Pattern.compile(""".*[-_](?:fn|footnote|note|endnote)[-_0-9]?.*""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""^noteref[-_0-9]?.*""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""^ref[-_0-9].*""", Pattern.CASE_INSENSITIVE)
    )

    /**
     * Checks if [fragment] resembles a footnote / endnote ID.
     */
    fun isLikelyFootnoteTarget(fragment: String?): Boolean {
        if (fragment.isNullOrBlank()) return false
        val clean = fragment.removePrefix("#").trim()
        if (isBacklinkTarget(clean)) return false
        return FOOTNOTE_FRAGMENT_PATTERNS.any { it.matcher(clean).matches() }
    }

    /**
     * Checks if [fragment] is a backlink returning to the body text.
     */
    fun isBacklinkTarget(fragment: String?): Boolean {
        if (fragment.isNullOrBlank()) return false
        val clean = fragment.removePrefix("#").trim().lowercase()
        return clean.startsWith("fnref") ||
            clean.startsWith("back") ||
            clean.contains("refback") ||
            clean.endsWith("-back")
    }

    /**
     * Checks if a link reference is likely pointing to a footnote or endnote.
     *
     * @param urlPath The resource path (e.g. "chapter1.xhtml" or "notes.xhtml").
     * @param fragment The anchor within the resource (e.g. "fn1" or null).
     * @param rels Relation attributes on the link.
     */
    fun isLikelyFootnoteLink(
        urlPath: String?,
        fragment: String?,
        rels: Set<String> = emptySet()
    ): Boolean {
        if (rels.any { it.equals("footnote", ignoreCase = true) }) return true
        if (rels.any { it.equals("backlink", ignoreCase = true) }) return false
        if (fragment.isNullOrBlank()) return false
        if (isBacklinkTarget(fragment)) return false
        if (isLikelyFootnoteTarget(fragment)) return true

        // If target resource file itself is named notes/footnotes/endnotes and has an anchor:
        val lowerPath = urlPath?.lowercase().orEmpty()
        if (lowerPath.contains("footnote") || lowerPath.contains("endnote") || lowerPath.contains("notes.")) {
            return true
        }

        return false
    }
}
