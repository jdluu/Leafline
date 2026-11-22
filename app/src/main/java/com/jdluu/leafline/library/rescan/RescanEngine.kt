package com.jdluu.leafline.library.rescan

import com.jdluu.leafline.library.BookIdentity
import com.jdluu.leafline.library.ImportedBookReconciler
import com.jdluu.leafline.library.LibraryBook

/**
 * Pure reconciliation of scanned files against existing library books.
 *
 * Classifies each successfully imported file as added, updated, or skipped
 * using [ImportedBookReconciler.merge] semantics so stable identity and
 * reading state survive an unchanged file at a new path. Persistence is
 * injected as suspend lambdas, keeping this core plain-JVM testable.
 *
 * When [lookupByStableId] is provided and a content hash has no persisted
 * match but the imported book's [LibraryBook.stableId] already exists in the
 * library, the existing record is updated in place rather than a duplicate
 * being created.  This prevents identical content found in multiple folders
 * from retargeting one existing record and orphaning its prior file.
 */
class RescanEngine(
    private val loadExisting: suspend (fileHash: String) -> LibraryBook?,
    private val persistAdded: suspend (book: LibraryBook) -> Unit,
    private val persistUpdated: suspend (book: LibraryBook) -> Unit,
    private val lookupByStableId: (suspend (stableId: String) -> LibraryBook?)? = null,
    private val lookupByPublication: (suspend (publication: String) -> LibraryBook?)? = null
) {

    /**
     * Reconciles [imported] files and returns aggregate counts. A failed file
     * or a persistence/lookup error is counted as failed and the batch
     * continues; a single failure never aborts the scan.
     */
    suspend fun reconcile(imported: List<ScannedFile>): RescanCounts {
        var added = 0
        var updated = 0
        var skipped = 0
        var failed = 0

        for (file in imported) {
            val book = when (file) {
                is ScannedFile.Failed -> {
                    failed++
                    continue
                }
                is ScannedFile.Imported -> file.book
            }

            val outcome = runCatching {
                classify(loadExisting(book.fileHash), book)
            }.getOrElse {
                failed++
                continue
            }

            val persisted = when (outcome) {
                is MergeOutcome.Added -> runCatching { persistAdded(outcome.book) }.isSuccess
                is MergeOutcome.Updated -> runCatching { persistUpdated(outcome.book) }.isSuccess
                is MergeOutcome.ExistingRecord -> runCatching { persistUpdated(outcome.book) }.isSuccess
                MergeOutcome.Skipped -> true
            }

            when (outcome) {
                is MergeOutcome.Added -> if (persisted) added++ else failed++
                is MergeOutcome.Updated -> if (persisted) updated++ else failed++
                is MergeOutcome.ExistingRecord -> if (persisted) updated++ else failed++
                MergeOutcome.Skipped -> skipped++
            }
        }

        return RescanCounts(
            added = added,
            updated = updated,
            skipped = skipped,
            failedFiles = failed
        )
    }

    private suspend fun classify(existing: LibraryBook?, imported: LibraryBook): MergeOutcome {
        if (existing != null) {
            val merged = ImportedBookReconciler.merge(existing, imported)
            return when {
                merged == null -> MergeOutcome.Skipped
                else -> {
                    // Same content already exists in the library. Preserve the
                    // existing record's path so an unchanged file found under a
                    // different display name/path never retargets (and orphans)
                    // the file it currently points at. Only metadata deltas
                    // surface as an update; a pure path difference is skipped.
                    val reconciled = merged.copy(filePath = existing.filePath)
                    if (reconciled == existing) MergeOutcome.Skipped
                    else MergeOutcome.Updated(reconciled)
                }
            }
        }

        // No hash match.  When a stableId lookup is available, check whether
        // the imported book's identity already exists in the library under a
        // different content hash (content changed).  Update in place rather
        // than creating a duplicate that would orphan the prior file.
        val lookup = lookupByStableId
        if (lookup != null) {
            val prior = lookup(imported.stableId)
            if (prior != null) {
                val updated = imported.copy(
                    stableId = prior.stableId,
                    addedAtEpochMillis = prior.addedAtEpochMillis,
                    lastLocatorJson = prior.lastLocatorJson,
                    coverPath = prior.coverPath ?: imported.coverPath,
                    koreaderHash = prior.koreaderHash ?: imported.koreaderHash,
                    lastReadAtEpochMillis = prior.lastReadAtEpochMillis,
                    readingStatus = prior.readingStatus
                )
                return MergeOutcome.ExistingRecord(updated)
            }
        }

        // Nothing matched by hash or by exact identity. The same publication
        // still may sit in the library as a second file: a re-download, another
        // edition of the EPUB, or the same book exported twice from Calibre.
        // Those bytes hash differently, so only the publication identifier links
        // them. Adopting the existing record keeps one row (and one reading
        // position) per book instead of splitting it in two.
        val byPublication = lookupByPublication
        if (byPublication != null) {
            val publication = BookIdentity.publicationOf(imported.stableId)
            if (publication != null) {
                val prior = byPublication(publication)
                if (prior != null) {
                    val updated = imported.copy(
                        stableId = prior.stableId,
                        addedAtEpochMillis = prior.addedAtEpochMillis,
                        lastLocatorJson = prior.lastLocatorJson,
                        coverPath = prior.coverPath ?: imported.coverPath,
                        koreaderHash = prior.koreaderHash ?: imported.koreaderHash,
                        lastReadAtEpochMillis = prior.lastReadAtEpochMillis,
                        readingStatus = prior.readingStatus
                    )
                    return MergeOutcome.ExistingRecord(updated)
                }
            }
        }

        return MergeOutcome.Added(imported)
    }
}
