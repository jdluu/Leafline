# Validated OPDS Client Implementation Patterns (ShelfSync/Tauri, 2026-08)

Reference from a completed end-to-end build: authenticated OPDS transport,
acquisition planning, atomic download execution, Tauri command with progress
events, frontend adapter/hook/UI, and app-shell integration. All patterns below
were verified by real test runs (139 Rust tests, 114 frontend tests).

## Layered slice order that worked

Implement and commit one vertical slice at a time, each independently tested:

1. Transport: HTTP client with Basic Auth, redirect disabled, same-origin
   validation, unsafe-scheme rejection, max feed size, streaming body read.
2. Parser: Atom XML fixtures under `test/fixtures/opds/`; pagination params
   replace query args; root request preserves the exact configured URL when no
   pagination is supplied.
3. Acquisition planning (pure functions, no I/O): media-type allowlist
   (EPUB preferred over PDF), URL validation (scheme, embedded credentials,
   cross-origin), sanitized deterministic filenames from title only (never
   server path components), destination planned relative to content root.
4. Download execution: stream to unique sibling `.part` file, atomic rename.
5. Tauri command wiring with progress events.
6. Frontend adapter + hook + presentational UI.
7. Stateful container composing the hooks.
8. App-shell entry point (lazy-loaded, optional callback so existing flows are
   untouched).

## Atomic download execution (Rust)

- `DownloadPlan.destination` must be a path RELATIVE to the content root;
  resolve it exactly once inside the downloader (a plan produced by planning
  code joined to a root must never be re-joined by the downloader - add an
  integration test proving output lands at `root/<filename>`, not
  `root/root/<filename>`).
- Unique temp names: `<name>.part-<uuid v4>` prevents concurrent-download
  clobbering; unambiguous for multi-dot filenames.
- Same-origin check BEFORE sending any request, using one shared
  `origin_matches` helper everywhere (catalog fetch, acquisition resolution,
  download) so default-port handling is consistent.
- Auth header attached only when target origin equals configured catalog origin.
- Enforce max size during streaming with `checked_add` overflow protection;
  reject zero-byte bodies as incomplete.
- Content-type validation: compare after stripping `;` parameters; allow
  parameters, reject mismatches.
- Every failure path removes the `.part` file; a pre-existing final file is
  preserved (rename happens only on success).
- Errors must not embed URLs or credentials; map internal errors through a
  sanitizing layer before returning to the caller/UI.

## Tauri command + progress events

- Command takes catalog URL, transient username/password, Publication, content
  root; validates scheme (http/https only) and rejects credentials-in-URL up
  front; returns `{local_path, media_type}`.
- Progress via one event name (`opds-download-progress`) with payload:
  publication id, title, bytes_received, total_bytes optional, status enum
  (`starting`/`downloading`/`completed`/`failed`). Never credentials or URLs.
- Refactor the downloader to accept `Option<Box<dyn Fn(u64, Option<u64>) +
  Send + Sync>>` progress callback rather than coupling it to Tauri.

## Frontend structure that tests cleanly without Tauri

- Split presentational vs stateful: a fully controlled `Screen` component
  taking all values/callbacks as props can be tested with plain renders; the
  `Container` owns in-memory state and composes data/download hooks.
- Adapter wraps all `invoke` calls behind a `safeInvoke` guard that throws
  outside Tauri; event subscription returns an unlisten function and no-ops in
  browsers so component tests need no Tauri mock beyond the adapter module.
- Event listener lifecycle: subscribe only while a download is active, filter
  payloads by publication id, clean up on completion/failure/unmount.
- Credentials live only in component state, passed down as props while
  connected, cleared on disconnect; never touch localStorage or stores.
- Optional-callback pattern for app integration: render new UI entry points
  only when the parent supplies the handler prop, keeping existing flows
  byte-identical when absent.

## OpenCode piloting pitfalls (recurring, cost real cleanup time twice)

- OpenCode runs frequently leave unrelated rustfmt/formatting edits across many
  legacy files it merely opened. Before every commit run `git status --short`,
  review scope against what was asked, and `git restore` unrelated modified
  files explicitly. Stage only intended files.
- Scope prompts tightly ("work only under src/features/x", "do not modify
  App.tsx / Rust / package.json") - this bounds blast radius but does not
  eliminate formatting spillover.
- Long tasks (10+ minutes) are normal for multi-file slices; use background
  execution with notify-on-complete and poll rather than killing early.
- Rate-limited model upstream (HTTP 429 mid-run): the partial work remains on
  disk; inspect `git status`, verify gates yourself, finish manually if needed.

## Verification gate set used per slice

```bash
cargo test --manifest-path src-tauri/Cargo.toml --lib --no-default-features
cargo check --manifest-path src-tauri/Cargo.toml --lib --no-default-features
pnpm vitest run          # focused dir first, then full
pnpm build               # tsc -b && vite build
node_modules/.bin/biome check .
git diff --check
```

Commit convention: Conventional Commits, no emojis/emdashes, push immediately
after each verified commit.
