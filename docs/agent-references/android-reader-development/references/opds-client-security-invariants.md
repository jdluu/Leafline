# OPDS Client Security Invariants (verified in ShelfSync, 2026-08-22)

Class-level security contract for any OPDS client (Tauri, Android, CLI).
Every item below was implemented and covered by passing tests in ShelfSync
(commits 8ed52c5..9c5b131); reuse the invariants even when the implementation
language differs.

## Credentials

- Transient only: held in memory for the duration of a request, never written
  to disk, stores, localStorage, logs, error messages, or returned payloads.
- Never embedded in URLs. Validate and reject any URL carrying userinfo
  (`user:pass@host`) before use.
- Send as HTTP Basic Auth only to origins that match the configured catalog
  origin.
- Sanitize all user-facing error messages: map internal error variants to
  fixed strings; never interpolate URLs, paths, or credentials into errors.

## Transport

- HTTP/HTTPS schemes only; reject everything else (including `data:`,
  `javascript:`, `mailto:`).
- Redirects disabled; cross-origin responses rejected.
- One shared origin-matching helper for the entire codebase that correctly
  handles default ports (https:443, http:80), explicit ports, and IPv6
  hosts. Do not hand-roll per-call origin comparisons.
- Feed and download size caps enforced both from Content-Length and by
  counting streamed bytes (with checked arithmetic against overflow).
- Timeouts on every client and per-request.

## Acquisition downloads

- Select advertised acquisitions from an explicit media-type allowlist
  (e.g. EPUB, PDF); ignore or reject everything else.
- Resolve relative hrefs against the configured catalog URL, then re-validate
  origin and credentials on the resolved URL.
- Derive filenames from publication titles, never from server path
  components. Sanitize (strip path separators, control characters, leading
  dots) and fall back to a constant when nothing survives.
- Plan the destination under a caller-provided content root with
  canonical-path containment checks; reject traversal.
- Stream into a unique `.part-<uuid>` sibling file (unique suffix prevents
  concurrent-download clobbering), create parent directories only inside the
  validated root.
- On success: flush, close, atomic rename to the final destination.
- On any failure (transport, status, size, content-type, IO): remove the
  `.part` file and leave any pre-existing destination file untouched.

## Planner/executor contract

- If a planner produces a download plan and an executor consumes it, define
  exactly one destination contract — plan.destination relative to the content
  root, resolved exactly once by the executor. A plan carrying a pre-joined
  absolute path that the executor joins again yields `root/root/...`.
- Guard this with an integration test that feeds planner output directly into
  the executor and asserts the output lands directly under the requested
  root.
