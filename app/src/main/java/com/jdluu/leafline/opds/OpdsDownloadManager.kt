package com.jdluu.leafline.opds

import android.content.Context
import com.jdluu.leafline.EpubImporter
import com.jdluu.leafline.FileHashUtil
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.opds.model.DownloadStatus
import com.jdluu.leafline.opds.model.OpdsBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages downloading EPUBs from OPDS catalogs, parsing metadata, and inserting into the library.
 */
class OpdsDownloadManager(
    private val context: Context,
    private val opdsClient: OpdsClient,
    private val importer: EpubImporter,
    private val repository: LibraryRepository,
    private val scope: CoroutineScope
) {

    private val _downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatuses: StateFlow<Map<String, DownloadStatus>> = _downloadStatuses.asStateFlow()

    fun downloadAndImport(
        book: OpdsBook,
        onSuccess: (LibraryBook) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val downloadUrl = book.downloadUrl
        if (downloadUrl.isNullOrBlank()) {
            _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.Error("No download URL available"))
            onError("No download URL available")
            return
        }

        scope.launch {
            _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.Downloading(0f))

            val sanitizedTitle = book.title
                .replace(Regex("[^a-zA-Z0-9.-]"), "_")
                .take(30)
                .ifBlank { "book" }
            val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.epub"
            val targetFile = File(context.filesDir, fileName)

            val downloadResult = opdsClient.downloadEpubFile(downloadUrl, targetFile) { fraction ->
                _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.Downloading(fraction))
            }

            downloadResult.fold(
                onSuccess = { file ->
                    _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.Importing)
                    val importResult = withContext(Dispatchers.IO) {
                        try {
                            val hash = FileHashUtil.computeSha256(file)
                            val libraryBook = importer.importEpub(file, hash)
                            if (libraryBook != null) {
                                repository.addBook(libraryBook)
                                libraryBook
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            file.delete()
                            null
                        }
                    }

                    if (importResult != null) {
                        _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.Success(importResult))
                        onSuccess(importResult)
                    } else {
                        val errorMsg = "Could not parse downloaded EPUB file"
                        _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.Error(errorMsg))
                        onError(errorMsg)
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Download failed"
                    _downloadStatuses.value = _downloadStatuses.value + (book.id to DownloadStatus.Error(errorMsg))
                    onError(errorMsg)
                }
            )
        }
    }
}
