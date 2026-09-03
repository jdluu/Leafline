package com.jdluu.leafline.library.rescan

import com.jdluu.leafline.library.LibraryBook

/** One file encountered by a folder scan. */
sealed interface ScannedFile {
    /** A file parsed into a [book] ready for reconciliation. */
    data class Imported(val book: LibraryBook) : ScannedFile

    /** A file that could not be parsed or read. */
    data class Failed(val filePath: String, val reason: String) : ScannedFile
}

/** The outcome of scanning a single folder. */
sealed interface FolderScan {
    /** The folder was reachable and yielded [files]. */
    data class Scanned(val files: List<ScannedFile>) : FolderScan

    /** The folder could not be accessed, typically a missing or revoked permission. */
    data object NoPermission : FolderScan
}

/** Aggregated counters for a reconciled batch of files. */
data class RescanCounts(
    val added: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val failedFiles: Int = 0
) {
    val total: Int get() = added + updated + skipped + failedFiles
}

/** Outcome of reconciling a single folder. */
sealed interface FolderRescanResult {
    data class Success(val counts: RescanCounts) : FolderRescanResult

    /**
     * The folder could not be scanned. When [recoverable] is true the failure is
     * expected to be transient (missing/revoked permission) and safe to retry.
     */
    data class Failed(
        val folderUri: String,
        val reason: String = "",
        val recoverable: Boolean
    ) : FolderRescanResult
}

/** Aggregate result across multiple folders. */
data class RescanSummary(
    val folderResults: List<FolderRescanResult>,
    val totals: RescanCounts
)

/** How an imported file relates to an existing library book. */
internal sealed interface MergeOutcome {
    data class Added(val book: LibraryBook) : MergeOutcome
    data class Updated(val book: LibraryBook) : MergeOutcome

    /** Same stable identity already exists under a different content hash. */
    data class ExistingRecord(val book: LibraryBook) : MergeOutcome
    data object Skipped : MergeOutcome
}
