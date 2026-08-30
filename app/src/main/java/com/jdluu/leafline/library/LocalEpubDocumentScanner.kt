package com.jdluu.leafline.library

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

/** Discovers EPUB document URIs below a user-granted Storage Access Framework tree. */
object LocalEpubDocumentScanner {
    fun isEpubName(name: String?): Boolean =
        name?.endsWith(".epub", ignoreCase = true) == true

    fun scanTree(contentResolver: ContentResolver, treeUri: Uri): List<Uri> {
        val rootDocumentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return emptyList()
        return scanDocument(contentResolver, treeUri, rootDocumentId)
    }

    private fun scanDocument(
        contentResolver: ContentResolver,
        treeUri: Uri,
        documentId: String
    ): List<Uri> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val results = mutableListOf<Uri>()
        runCatching {
            contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex)
                    val mime = cursor.getString(mimeIndex)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        results += scanDocument(contentResolver, treeUri, childId)
                    } else if (isEpubName(name)) {
                        results += DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    }
                }
            }
        }
        return results.sortedBy(Uri::toString)
    }
}

