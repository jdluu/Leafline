package com.jdluu.leafline.library.rescan

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.jdluu.leafline.EpubImporter
import com.jdluu.leafline.FileHashUtil
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.LocalEpubDocumentScanner
import com.jdluu.leafline.library.data.LibraryRepository
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * SAF-backed [FolderRescanRunner].
 *
 * Enumerates a saved SAF tree with [LocalEpubDocumentScanner], stages each
 * discovered document into a unique temp file, parses it via [EpubImporter],
 * then places the final copy at a collision-free app-private path via
 * [RescanTargetFile] and maps every outcome to the rescan result types.
 *
 * Content is staged before any final path is chosen so a document whose bytes
 * changed never overwrites a file an existing library record references: the
 * old file is preserved until the new content is represented consistently at
 * its own unique path.
 *
 * A folder whose persisted read grant is missing is reported as
 * [FolderScan.NoPermission]; a document that cannot be copied or parsed is
 * reported as [ScannedFile.Failed] and never aborts the batch.
 */
class SaFFolderRescanRunner(
    private val contentResolver: ContentResolver,
    private val context: Context,
    private val importer: EpubImporter,
    private val repository: LibraryRepository
) : FolderRescanRunner {

    private val rescanMutex = Mutex()

    private val coordinator = FolderRescanCoordinator(
        engine = RescanEngine(
            loadExisting = repository::getBookByFileHash,
            persistAdded = { book -> repository.addBook(book) },
            persistUpdated = { book -> repository.updateBook(book) },
            lookupByStableId = repository::getBookByStableId
        ),
        scanFolder = ::scanFolder
    )

    override suspend fun rescanOne(folderUri: String): FolderRescanResult = rescanMutex.withLock {
        coordinator.rescanOne(folderUri, seedOccupiedPaths())
    }

    override suspend fun rescanAll(folderUris: List<String>): RescanSummary = rescanMutex.withLock {
        coordinator.rescanAll(folderUris, seedOccupiedPaths())
    }

    private suspend fun seedOccupiedPaths(): MutableMap<String, String> =
        repository.getAllBooks()
            .first()
            .associate { it.filePath to it.fileHash }
            .toMutableMap()

    private suspend fun scanFolder(
        folderUri: String,
        occupied: MutableMap<String, String>
    ): FolderScan {
        val treeUri = Uri.parse(folderUri)
        if (!hasReadGrant(treeUri)) return FolderScan.NoPermission
        val files = LocalEpubDocumentScanner.scanTree(contentResolver, treeUri)
            .map { importDocument(it, occupied) }
        return FolderScan.Scanned(files)
    }

    private fun hasReadGrant(treeUri: Uri): Boolean =
        contentResolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission }

    private fun importDocument(
        documentUri: Uri,
        occupied: MutableMap<String, String>
    ): ScannedFile {
        return try {
            val displayName = displayName(documentUri)
            val book = copyAndImport(documentUri, displayName, occupied)
            if (book != null) ScannedFile.Imported(book)
            else ScannedFile.Failed(displayName, "Unable to import")
        } catch (e: SecurityException) {
            val name = runCatching { displayName(documentUri) }.getOrDefault("unknown")
            ScannedFile.Failed(name, "Permission to read file missing")
        } catch (e: IOException) {
            val name = runCatching { displayName(documentUri) }.getOrDefault("unknown")
            ScannedFile.Failed(name, e.message ?: "Could not read file")
        } catch (e: Exception) {
            val name = runCatching { displayName(documentUri) }.getOrDefault("unknown")
            ScannedFile.Failed(name, e.message ?: "Import failed")
        }
    }

    private fun copyAndImport(
        documentUri: Uri,
        displayName: String,
        occupied: MutableMap<String, String>
    ): LibraryBook? {
        // Stage the source bytes into a unique temp file first: we must not
        // touch a final path (which an existing record may reference) before
        // we know this document's content hash.
        val staged = createStagedFile() ?: return null
        val wrote = contentResolver.openInputStream(documentUri)?.use { input ->
            staged.outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false
        if (!wrote || staged.length() == 0L) {
            staged.delete()
            return null
        }

        val fileHash = FileHashUtil.computeSha256(staged)
        val imported = importer.importEpub(staged, fileHash) ?: run {
            staged.delete()
            return null
        }

        // All documents in a folder are staged before reconciliation runs. If
        // this content was already placed by an earlier document in the same
        // operation, reuse that path and discard this duplicate stage rather
        // than creating an orphaned app-private file.
        occupied.entries.firstOrNull { it.value == fileHash }?.let { (path, _) ->
            staged.delete()
            return imported.copy(filePath = path)
        }

        val target = RescanTargetFile.resolve(
            filesDir = context.filesDir,
            displayName = displayName,
            contentHash = fileHash,
            occupied = occupied
        )
        if (!installAtTarget(staged, target, fileHash, occupied)) return null
        return imported.copy(filePath = target.absolutePath)
    }

    /**
     * Moves a staged file to its final [target] path without ever deleting a
     * path referenced by an existing library book before a replacement is
     * durably installed.
     *
     * [RescanTargetFile.resolve] already hands out a unique path whenever the
     * preferred display-name path holds different content, so the only paths
     * this may touch are free, unreferenced (stale), or already holding
     * identical content. A defensive guard aborts if a differing-content
     * referenced path ever slips through, rather than clobbering it.
     */
    private fun installAtTarget(
        staged: File,
        target: File,
        contentHash: String,
        occupied: MutableMap<String, String>
    ): Boolean {
        if (target.absolutePath == staged.absolutePath) {
            occupied[target.absolutePath] = contentHash
            return true
        }

        val holder = occupied[target.absolutePath]
        if (holder != null && holder != contentHash) {
            // Never overwrite a final path that is referenced by different
            // content. This should be unreachable given resolve() semantics.
            staged.delete()
            return false
        }

        if (target.exists() && holder == contentHash) {
            // Identical content is already durably installed at a referenced
            // path; keep those bytes and drop the stage rather than move them.
            staged.delete()
            occupied[target.absolutePath] = contentHash
            return true
        }

        // Target is free (or a stale orphan unreferenced by the library), so a
        // same-filesystem rename lands it atomically in place.
        if (staged.renameTo(target)) {
            occupied[target.absolutePath] = contentHash
            return true
        }

        // Do not fall back to outputStream() here: truncating an existing path
        // after a failed rename would make replacement non-atomic. The caller
        // reports this document as failed and the staged file is left intact
        // for no referenced path to be damaged.
        staged.delete()
        return false
    }

    private fun createStagedFile(): File? = try {
        File.createTempFile("rescan-import-", ".epub", context.filesDir)
    } catch (e: IOException) {
        null
    }

    private fun displayName(documentUri: Uri): String {
        var name: String? = null
        contentResolver.query(documentUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) name = cursor.getString(nameIndex)
        }
        return name ?: documentUri.lastPathSegment ?: "book"
    }
}
