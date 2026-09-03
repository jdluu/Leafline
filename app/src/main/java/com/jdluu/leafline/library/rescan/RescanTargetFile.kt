package com.jdluu.leafline.library.rescan

import com.jdluu.leafline.sanitizeFileName
import java.io.File

/**
 * Deterministically chooses an app-private file path for a scanned document.
 *
 * Guarantees:
 * - Reuses the display-name path when it is free, or when it is already
 *   referenced by a book holding the SAME content hash (unchanged content is
 *   mapped back to its existing path so it is reconciled as skipped).
 * - Otherwise appends a numeric suffix (-2, -3, ...) so the new content is
 *   written to a distinct path and never overwrites bytes referenced by an
 *   existing library record (changed content is preserved consistently rather
 *   than clobbered).
 * - Never returns a path outside [filesDir]: both inputs collapse to a single
 *   filename via [sanitizeFileName] before joining, so no traversal escapes.
 *
 * [occupied] maps canonical absolute paths already claimed by the library
 * (or earlier in the current scan) to the content hash currently held there.
 */
object RescanTargetFile {
    fun resolve(
        filesDir: File,
        displayName: String,
        contentHash: String,
        occupied: Map<String, String>
    ): File {
        val base = sanitizeFileName(displayName)
        val stem = stripEpubExtension(base)

        val preferred = File(filesDir, base)
        val preferredHolder = occupied[preferred.absolutePath]
        if ((preferredHolder == null && !preferred.exists()) || preferredHolder == contentHash) {
            return preferred
        }

        // The display-name path already holds different content (an existing
        // record references it), so preserve it and pick the first free suffix.
        // Base is implicitly version 1, so alternates start at -2.
        for (i in 2..Int.MAX_VALUE) {
            val alt = File(filesDir, "$stem-$i.epub")
            val holderHash = occupied[alt.absolutePath]
            if ((holderHash == null && !alt.exists()) || holderHash == contentHash) return alt
        }
        error("unable to allocate a target path for $base")
    }

    private fun stripEpubExtension(name: String): String {
        val lower = name.lowercase()
        return if (lower.endsWith(".epub")) name.dropLast(5) else name
    }
}
