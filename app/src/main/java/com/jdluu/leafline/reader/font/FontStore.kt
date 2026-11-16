package com.jdluu.leafline.reader.font

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Manages user-imported custom fonts in internal storage.
 */
class FontStore(private val context: Context) {

    private val fontsDir: File
        get() = File(context.filesDir, "fonts").apply { if (!exists()) mkdirs() }

    private val metadataFile: File
        get() = File(fontsDir, "fonts_metadata.json")

    /**
     * Lists all custom fonts currently imported and available.
     */
    fun getCustomFonts(): List<CustomFont> {
        val metadata = loadMetadata()
        val files = fontsDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".ttf", ignoreCase = true) || file.name.endsWith(".otf", ignoreCase = true))
        } ?: return emptyList()

        return files.mapNotNull { file ->
            val name = metadata[file.name] ?: FontParser.extractFontName(file) ?: file.nameWithoutExtension
            CustomFont(name = name, fileName = file.name, file = file)
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Finds a custom font by its family name.
     */
    fun findByName(name: String): CustomFont? {
        return getCustomFonts().firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Imports a font file from a content URI.
     */
    fun importFont(uri: Uri, originalFileName: String?): Result<CustomFont> = runCatching {
        val extension = when {
            originalFileName?.endsWith(".otf", ignoreCase = true) == true -> ".otf"
            else -> ".ttf"
        }

        val baseName = originalFileName?.substringBeforeLast(".")
            ?.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            ?.takeIf { it.isNotBlank() } ?: "custom_font"

        var targetFile = File(fontsDir, "$baseName$extension")
        var counter = 1
        while (targetFile.exists()) {
            targetFile = File(fontsDir, "${baseName}_$counter$extension")
            counter++
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not open input stream from URI: $uri")

        val parsedName = FontParser.extractFontName(targetFile)
        val finalName = parsedName ?: baseName.replace("_", " ").trim()

        val metadata = loadMetadata().toMutableMap()
        metadata[targetFile.name] = finalName
        saveMetadata(metadata)

        CustomFont(name = finalName, fileName = targetFile.name, file = targetFile)
    }

    /**
     * Deletes a custom font.
     */
    fun deleteFont(font: CustomFont): Boolean {
        val deleted = font.file.delete()
        if (deleted) {
            val metadata = loadMetadata().toMutableMap()
            metadata.remove(font.fileName)
            saveMetadata(metadata)
        }
        return deleted
    }

    private fun loadMetadata(): Map<String, String> {
        if (!metadataFile.exists()) return emptyMap()
        return try {
            val json = JSONObject(metadataFile.readText())
            val map = mutableMapOf<String, String>()
            json.keys().forEach { key ->
                map[key] = json.getString(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveMetadata(map: Map<String, String>) {
        try {
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            metadataFile.writeText(json.toString())
        } catch (_: Exception) {
            // Ignore write failures
        }
    }
}
