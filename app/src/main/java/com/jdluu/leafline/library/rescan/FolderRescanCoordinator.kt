package com.jdluu.leafline.library.rescan

/**
 * Drives rescans across one or many library folders.
 *
 * [scanFolder] returns a [FolderScan]; a missing or revoked folder permission
 * is signalled as [FolderScan.NoPermission], and unexpected scanner exceptions
 * are caught and surfaced as a recoverable [FolderRescanResult.Failed] rather
 * than thrown.
 *
 * An [occupied] map of path-to-hash is passed to every [scanFolder] call so
 * that path allocation is consistent within a single top-level rescan
 * operation without relying on mutable shared state.
 */
class FolderRescanCoordinator(
    private val engine: RescanEngine,
    private val scanFolder: suspend (folderUri: String, occupied: MutableMap<String, String>) -> FolderScan
) {

    /** Rescans a single [folderUri] with a freshly seeded occupied map. */
    suspend fun rescanOne(
        folderUri: String,
        occupied: MutableMap<String, String> = mutableMapOf()
    ): FolderRescanResult = rescanSingle(folderUri, occupied)

    /** Rescans every folder in [folderUris], sharing [occupied] across folders. */
    suspend fun rescanAll(
        folderUris: List<String>,
        occupied: MutableMap<String, String> = mutableMapOf()
    ): RescanSummary {
        val results = folderUris.map { rescanSingle(it, occupied) }
        val totals = RescanCounts(
            added = results.sumOf { (it as? FolderRescanResult.Success)?.counts?.added ?: 0 },
            updated = results.sumOf { (it as? FolderRescanResult.Success)?.counts?.updated ?: 0 },
            skipped = results.sumOf { (it as? FolderRescanResult.Success)?.counts?.skipped ?: 0 },
            failedFiles = results.sumOf { (it as? FolderRescanResult.Success)?.counts?.failedFiles ?: 0 }
        )
        return RescanSummary(folderResults = results, totals = totals)
    }

    private suspend fun rescanSingle(
        folderUri: String,
        occupied: MutableMap<String, String>
    ): FolderRescanResult {
        val scan = runCatching { scanFolder(folderUri, occupied) }.getOrElse {
            return FolderRescanResult.Failed(
                folderUri = folderUri,
                reason = it.message.orEmpty(),
                recoverable = true
            )
        }
        return when (scan) {
            FolderScan.NoPermission -> FolderRescanResult.Failed(
                folderUri = folderUri,
                reason = "Folder permission missing or revoked",
                recoverable = true
            )
            is FolderScan.Scanned -> FolderRescanResult.Success(engine.reconcile(scan.files))
        }
    }
}
