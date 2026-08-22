package com.jdluu.leafline

fun sanitizeFileName(fileName: String): String {
    if (fileName.isBlank()) return "imported.epub"

    var result = fileName

    result = result.filter { it.code !in 0..31 && it.code != 127 }

    result = result.replace("..", "")
    result = result.replace("/", "_").replace("\\", "_")

    result = result.trim().trim('.', '_')

    if (!result.endsWith(".epub", ignoreCase = true)) {
        result = "$result.epub"
    }

    return if (result.isBlank() || result == ".epub") "imported.epub" else result
}
