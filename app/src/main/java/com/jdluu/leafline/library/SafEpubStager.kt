package com.jdluu.leafline.library

import com.jdluu.leafline.EpubImporter
import com.jdluu.leafline.FileHashUtil
import com.jdluu.leafline.library.rescan.RescanTargetFile
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.CancellationException

sealed interface SafStageResult {
    data class Imported(val book: LibraryBook) : SafStageResult
    data class Failed(val message: String) : SafStageResult
}

/**
 * Stages a SAF-selected EPUB stream into a temporary file, hashes and parses it,
 * deduplicates against already-installed files holding the same bytes, and moves
 * new content to a collision-free path under [filesDir].
 */
object SafEpubStager {

    suspend fun stageAndImport(
        filesDir: File,
        displayName: String,
        openStream: () -> InputStream?,
        importer: EpubImporter,
        occupied: MutableMap<String, String>
    ): SafStageResult {
        val staged = try {
            File.createTempFile("saf-import-", ".epub", filesDir)
        } catch (e: Exception) {
            return SafStageResult.Failed("Failed to import EPUB: ${e.message ?: "could not stage file"}")
        }

        return try {
            val stream = openStream() ?: run {
                staged.delete()
                return SafStageResult.Failed("No data available")
            }

            stream.use { input ->
                staged.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (staged.length() == 0L) {
                staged.delete()
                return SafStageResult.Failed("Empty file cannot be imported")
            }

            val fileHash = FileHashUtil.computeSha256(staged)
            val imported = importer.importEpub(staged, fileHash) ?: run {
                staged.delete()
                return SafStageResult.Failed("Failed to import EPUB")
            }

            // Same bytes already on disk in the library: reuse that path and drop the stage.
            val existingDuplicatePath = occupied.entries
                .firstOrNull { (path, hash) -> hash == fileHash && File(path).isFile }
                ?.key
            if (existingDuplicatePath != null) {
                staged.delete()
                return SafStageResult.Imported(imported.copy(filePath = existingDuplicatePath))
            }

            val target = RescanTargetFile.resolve(
                filesDir = filesDir,
                displayName = displayName,
                contentHash = fileHash,
                occupied = occupied
            )

            val installed = installAtTarget(staged, target, fileHash, occupied)
            if (!installed) {
                SafStageResult.Failed("Failed to store imported EPUB")
            } else {
                SafStageResult.Imported(imported.copy(filePath = target.absolutePath))
            }
        } catch (e: CancellationException) {
            staged.delete()
            throw e
        } catch (e: Exception) {
            staged.delete()
            SafStageResult.Failed("Failed to import EPUB: ${e.message ?: "unknown error"}")
        }
    }

    private fun installAtTarget(
        staged: File,
        target: File,
        contentHash: String,
        occupied: MutableMap<String, String>
    ): Boolean {
        val holder = occupied[target.absolutePath]
        if (holder != null && holder != contentHash) {
            staged.delete()
            return false
        }

        if (target.exists() && holder == contentHash) {
            staged.delete()
            occupied[target.absolutePath] = contentHash
            return true
        }

        if (staged.renameTo(target)) {
            occupied[target.absolutePath] = contentHash
            return true
        }

        staged.delete()
        return false
    }
}
