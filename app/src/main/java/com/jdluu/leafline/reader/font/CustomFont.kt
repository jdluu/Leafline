package com.jdluu.leafline.reader.font

import java.io.File

/**
 * Represents a custom font imported into the app's internal storage.
 */
data class CustomFont(
    val name: String,
    val fileName: String,
    val file: File,
    val mimeType: String = if (fileName.endsWith(".otf", ignoreCase = true)) "font/otf" else "font/ttf"
)
