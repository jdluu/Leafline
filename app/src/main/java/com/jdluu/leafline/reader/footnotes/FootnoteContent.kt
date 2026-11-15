package com.jdluu.leafline.reader.footnotes

/**
 * Cleaned footnote or endnote content extracted from an EPUB publication.
 *
 * Designed as a pure domain model free of Android, Room, or Readium dependencies.
 */
data class FootnoteContent(
    val id: String,
    val title: String,
    val text: String,
    val targetLocatorJson: String? = null
)
