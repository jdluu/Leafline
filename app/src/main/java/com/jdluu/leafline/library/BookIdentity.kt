package com.jdluu.leafline.library

/**
 * Durable identity for an imported EPUB.
 *
 * The content hash is always included so two editions that reuse the same
 * publication identifier cannot share reading state accidentally. The
 * identifier improves continuity when metadata is stable; the hash remains
 * the authoritative distinction between different file contents.
 *
 * Because the hash is part of the id, the same publication stored as two files
 * gets two ids. [publicationOf] exposes the identifier half so an import can
 * recognise that case: Calibre libraries and re-downloads routinely hold the
 * same book twice with different bytes.
 */
object BookIdentity {
    private const val IDENTIFIER_PREFIX = "identifier:"
    private const val HASH_MARKER = ":hash:"

    fun from(identifier: String?, fileHash: String): String {
        val normalizedIdentifier = identifier?.trim()
        return if (normalizedIdentifier.isNullOrBlank()) {
            "hash:$fileHash"
        } else {
            "$IDENTIFIER_PREFIX$normalizedIdentifier$HASH_MARKER$fileHash"
        }
    }

    /**
     * The publication identifier this stable id was built from, or null when it
     * carried none. Two ids sharing a non-null value are the same publication
     * stored as different files.
     */
    fun publicationOf(stableId: String): String? {
        if (!stableId.startsWith(IDENTIFIER_PREFIX)) return null
        val end = stableId.lastIndexOf(HASH_MARKER)
        if (end <= IDENTIFIER_PREFIX.length) return null
        return stableId.substring(IDENTIFIER_PREFIX.length, end).takeIf { it.isNotBlank() }
    }
}
