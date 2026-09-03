package com.jdluu.leafline.library

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

/**
 * [FolderMetadata] backed by the Storage Access Framework.
 *
 * It resolves a folder's display name from the tree document and inspects or
 * releases the persisted read grant the app holds on the URI. Requires a live
 * [ContentResolver], so it is exercised on the device rather than in JVM tests.
 */
class SafFolderMetadata(private val contentResolver: ContentResolver) : FolderMetadata {

    override fun labelFor(uri: String): String? = runCatching {
        val treeUri = Uri.parse(uri)
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
        contentResolver.query(
            documentUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
        } ?: null
    }.getOrNull()

    override fun hasPersistedReadGrant(uri: String): Boolean =
        contentResolver.persistedUriPermissions.any {
            it.uri.toString() == uri && it.isReadPermission
        }

    override fun releasePersistedReadGrant(uri: String) {
        val target = contentResolver.persistedUriPermissions.firstOrNull {
            it.uri.toString() == uri && it.isReadPermission
        } ?: return
        contentResolver.releasePersistableUriPermission(
            target.uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}
