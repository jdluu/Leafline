package com.jdluu.leafline.library

import java.io.File

/** Recursively finds local EPUB files without touching or copying their contents. */
object LocalEpubScanner {
    fun scan(root: File): List<File> {
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .onEnter { it.canRead() }
            .filter { it.isFile && it.extension.equals("epub", ignoreCase = true) }
            .mapNotNull { file -> runCatching { file.canonicalFile }.getOrNull() }
            .sortedBy { it.path }
            .toList()
    }
}

