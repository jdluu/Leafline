package com.jdluu.leafline.library

/**
 * Durable identity for an imported EPUB.
 *
 * The content hash is always included so two editions that reuse the same
 * publication identifier cannot share reading state accidentally. The
 * identifier improves continuity when metadata is stable; the hash remains
 * the authoritative distinction between different file contents.
 */
object BookIdentity {
    fun from(identifier: String?, fileHash: String): String {
        val normalizedIdentifier = identifier?.trim()
        return if (normalizedIdentifier.isNullOrBlank()) {
            "hash:$fileHash"
        } else {
            "identifier:$normalizedIdentifier:hash:$fileHash"
        }
    }
}
