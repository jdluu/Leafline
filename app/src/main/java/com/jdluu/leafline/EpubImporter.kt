package com.jdluu.leafline

import com.jdluu.leafline.library.LibraryBook
import java.io.File

/**
 * Import operation seam: turns an EPUB file already stored on disk into a
 * [LibraryBook], or null when parsing fails.
 *
 * Callers depend on this contract so tests and future import paths can
 * substitute implementations; the Readium-backed implementation owns
 * metadata parsing and cover extraction.
 */
interface EpubImporter {
    fun importEpub(file: File, fileHash: String): LibraryBook?
}
