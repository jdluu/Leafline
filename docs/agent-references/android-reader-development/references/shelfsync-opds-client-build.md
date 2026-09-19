# ShelfSync OPDS Client Build (Tauri + React, verified 2026-08-22)

Session record for building a desktop/Tauri OPDS client (ShelfSync) to
Calibre-Sync-like functionality against Grimmory. Distinct from the Android
reader flow in `live-grimmory-opds-integration.md`; shares the same security
invariants (see `opds-client-security-invariants.md`).

## Architecture that worked (16 commits, all gates green)

Vertical slices, bottom-up, each independently tested and committed:

1. Authenticated OPDS transport (reqwest, rustls, redirects disabled, Basic
   Auth gated on shared origin match, feed size caps, credential-safe errors)
2. Catalog parser with pagination + XML fixtures under `test/fixtures/opds/`
3. Tauri command `fetch_opds_catalog` with URL scheme/credential validation
4. Frontend adapter (`safeInvoke`) + React Query hook
5. Presentational catalog view (loading skeletons, error+retry, empty,
   pagination, non-actionable format badges)
6. Pure acquisition planning (media-type allowlist EPUB>PDF, title-derived
   sanitized filenames, canonical containment)
7. Atomic downloader (unique `.part-<uuid>`, size cap, content-type check,
   rename-on-success, cleanup-on-failure) — axum TestServer-mocked HTTP tests
8. Tauri download command emitting `opds-download-progress` events
9. Frontend adapter methods + `useOpdsDownload` hook (listener scoped to
   active downloads, filtered by publication id, cleaned up on completion)
10. Publication card format selector + connection screen + stateful container
11. App-shell integration: optional role-selection card mounting the container

## Key implementation decisions

- **Presentational/container split**: screens take all state as props so vitest
  can drive them without Tauri; one container owns real hooks. Kept the whole
  feature isolated from legacy dashboards until a single integration commit.
- **Progress events**: Rust emits `{publication_id, title, bytes_received,
  total_bytes, status}` — identity plus numbers only; frontend filters by id.
- **Mocked HTTP tests**: `axum-test::TestServer` gives real loopback HTTP for
  downloader tests (success, auth header assertion, 4xx/5xx, oversized body,
  content-type mismatch, atomic replacement, no `.part` leftovers).
- **UX polish pass after function**: wire progress bars end-to-end (a prop
  existed but was unconnected), close dropdowns on Escape/click-outside, fold
  advanced options into a `<details>` disclosure.

## Phase-review findings (next-step backlog)

Trim candidates: unused mock data paths; legacy P2P host/client sync stack
partially superseded by OPDS acquisition.
Missing features, priority order:
1. Register downloaded books into the local library DB (completes the loop)
2. Persisted catalog URL bookmarks (non-secret) for quick reconnect
3. OPDS search (transport already supports arbitrary feed URLs)
4. UI download cancellation
UX: publication detail modal, session downloads drawer, consistent empty states.

## Verification gate set (every slice)

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib <filter> --no-default-features
cargo check --manifest-path src-tauri/Cargo.toml --lib --no-default-features
pnpm vitest run && pnpm build && node_modules/.bin/biome check .
git diff --check
# scope check: restore any out-of-scope files the coding agent touched
git add <intended-only> && git diff --cached --check && git commit && git push
```
