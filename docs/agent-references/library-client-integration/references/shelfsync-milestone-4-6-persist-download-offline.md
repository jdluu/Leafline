# ShelfSync Milestone 4-6 Implementation Notes (2026-08-23)

Reference from a completed end-to-end build of Milestones 4 (persist module),
5 (verified download pipeline), and 6 (offline library + reconciliation UX)
on the ShelfSync Tauri/Rust repository.

## Milestone 4: Local download-centric persistence

New `src-tauri/src/persist/` module with schema, repo, store, and tests.

### Schema design

- `catalog_account(provider, base_url, username)` — one row per configured server.
- `publication(account_id, provider, canonical_id)` with `UNIQUE(account_id, provider, canonical_id)` plus metadata snapshot JSON and availability flag.
- `acquisition(publication_id, media_type)` with `UNIQUE(publication_id, media_type)` — dedupes per format.
- `file_revision(acquisition_id, expected_length, hash_algorithm, hash_value, local_relative_path)` — content-root-relative path.
- `download_job(revision_id, state, timestamps, error)` with CHECK-constrained states.
- Versioned via `PRAGMA user_version`.

### Migration safety

- Transactional, idempotent, runs on every open.
- Legacy `books` table is never read into the new model or reinterpreted.
- Legacy row count is recorded in `persist_meta` as `unavailable_in_new_model`.
- New sequences start fresh so new publication ids never collide with legacy book ids.

### Identity rules (enforced)

- All lookups are provider-scoped `(account_id, provider, canonical_id)`.
- Acquisitions dedupe per media type.
- Local paths must be relative, backslash-free, and cannot escape the content root (`validate_relative_path`).
- On Unix, `Path::components()` silently drops interior `.` segments, so `books/./book.epub` normalizes to `books/book.epub` rather than being rejected. Test the normalized result, not rejection.

### Job lifecycle

- CAS-style state transitions guarded to active states only.
- `recover_interrupted_jobs()` marks queued/running as `interrupted` on restart.
- `purge_stale_jobs()` removes aged terminal jobs.

### Test coverage (7 new, 146 total)

Provider-scoped dedupe across accounts/mirrors, format-specific acquisitions/revisions, restart recovery of a mid-flight job then successful retry, stale terminal purge keeping active jobs, legacy-row preservation through two migration passes, path-escape rejection, job-requires-revision FK sanity.

## Milestone 5: Verified single EPUB download

New `src-tauri/src/opds/install.rs` wiring the persist module to the existing download helpers.

### Pipeline

1. Select acquisition link via existing `acquisition.rs` planning helpers.
2. Create `download_job` row in persist module.
3. Stream to unique `.part-<uuid>` file under the content root.
4. Verify HTTP status, `Content-Length` vs received bytes, advertised expected length, and SHA-256 hash when provided.
5. Validate EPUB ZIP structure (readable archive with correct `mimetype` entry).
6. Atomically rename `.part` to final path.
7. Call transactional `complete_download` (attaches revision path + marks job completed in one SQLite tx).

### Failure semantics

- Typed `DownloadError` variants with `is_retryable()` — retry only Network/Server/RateLimited (3 attempts with backoff).
- Cancellation via `CancellationToken`.
- Every failure path removes part files, marks job failed or cancelled, never sets a revision local path.
- Replacement failure leaves the old verified file intact since rename happens last.
- Auth failure, malformed response, length/hash mismatch, and invalid ZIP must leave no falsely complete record.

### Helpers added

`safe_join`, `safe_remove_within_root`, `sha256_file`, `validate_epub_zip`.

### Test coverage (12 new, 159 total)

Success end-to-end with DB assertions and progress callbacks, hash mismatch, invalid ZIP, auth failure, length mismatch, replacement failure keeping old file intact, server-error retry, cancellation, non-EPUB rejection, and safe-delete containment.

## Milestone 6: Offline library + reconciliation UX

New `src-tauri/src/offline/` module (maintenance + refresh) plus frontend types/services/UI.

### Backend

- **Maintenance**: `check_disk_space` (uses `statvfs` via `libc`), `cleanup_stale_parts` on startup, `recover_interrupted_jobs` reuse from persist module.
- **Refresh**: re-fetch catalog metadata via paginated walk, detect new/changed/removed publications, mark server-removed ones `unavailable` (set `publication_available = false`). NO automatic destructive actions — never auto-delete local files.
- **Safe delete**: explicit user-controlled `delete_offline_content` using `safe_remove_within_root` (content-root containment enforced).
- **Tauri commands**: `list_offline_library`, `refresh_offline_library`, `delete_offline_content`, `check_download_space`.
- Added `libc = "0.2"` to `Cargo.toml` for `statvfs`.

### Frontend

- `src/types/offline.ts`: `OfflineLibrarySection` union (complete/downloading/failed/unavailable/superseded), `OfflineLibraryRecord`, `OfflineLibrarySnapshot`, `CategorizedLibraryRecord`, `PublicationLibraryInfo`, `buildPublicationLibraryInfo()` grouping function with priority ordering (downloading > unavailable > failed > complete).
- `src/services/offlineLibrary.ts`: Tauri command bindings with snake_case parameter mapping.
- `src/features/opds/LibraryStateBadge.tsx`: status badge component.
- `OpdsPublicationCard` extended with `libraryInfo` prop: badges for each state, retry action for failed records, delete action for complete/unavailable/superseded, per-revision deletion for superseded copies.

### Test coverage (20 new Rust, 11 new TS; 179 Rust total, 147 vitest total)

Rust: refresh detection (new/changed/removed), delete containment, stale part cleanup, disk space check.
TS: service binding invocation, grouping logic, badge rendering, card state integration.

### JSX trailing-comma parse error pitfall

OpenCode generated test files with trailing commas inside JSX expression containers:

```tsx
// BROKEN — SWC parse error at the closing }
libraryInfo={
  libraryInfo({
    ...baseRecord,
    section: "unavailable",
  }),
}
```

The comma after `)` makes it a sequence expression inside JSX, which SWC
rejects. Fix: remove the trailing comma or inline the expression:

```tsx
// CORRECT
libraryInfo={libraryInfo({
  ...baseRecord,
  section: "unavailable",
})}
```

This only manifests as a vitest transform error, not a `tsc` error, because
SWC's JSX parser is stricter than TypeScript's type checker.

## Verification gates used

```bash
cargo test --manifest-path src-tauri/Cargo.toml   # 179 passed
pnpm vitest run --reporter=json                    # 57 suites, 147 tests
git diff --check
```

## Commit sequence

```
8901b4b feat: group persisted download records by library state
bf8390f feat: add offline maintenance and catalog refresh reconciliation
874cd96 feat: expose offline library management commands
e0ae189 fix: restore missing test imports for refresh module
ff72219 fix: correct trailing comma in offline library states test
```
