package com.jdluu.leafline.library.rescan

/**
 * Application boundary that re-scans saved library folders and reconciles the
 * discovered EPUBs against the library.
 *
 * The concrete SAF-backed implementation ([SaFFolderRescanRunner]) needs an
 * [android.content.ContentResolver] and [android.content.Context], so it is not
 * JVM-testable; the interface exists so the [FolderRescanViewModel] can be
 * driven by a fake in plain-JVM unit tests.
 */
interface FolderRescanRunner {
    /** Re-scans a single saved [folderUri] and returns that folder's result. */
    suspend fun rescanOne(folderUri: String): FolderRescanResult

    /** Re-scans every saved folder in [folderUris], summarizing totals. */
    suspend fun rescanAll(folderUris: List<String>): RescanSummary
}
