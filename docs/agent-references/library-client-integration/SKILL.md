---
name: library-client-integration
description: "Use when building offline clients for book catalogs."
version: 1.0.0
license: MIT
platforms: [linux, macos, windows, android]
metadata:
  hermes:
    tags: [opds, ebooks, offline-library, downloads, android]
    related_skills: [software-research, dev-toolkit, systematic-debugging]
category: software-development
---

# Offline Library Client Integration

Use when repurposing or building a client that browses and downloads books from
a documented library protocol. The goal is an offline library manager, not an
ebook reader or server replacement.

## Workflow

1. Audit before implementation: inspect repository state, manifests, tests,
   architecture, history, and actual behavior. Treat README/wiki/changelog
   claims as hypotheses until compared with code and executable validation.
2. Research current official documentation and upstream source. Prefer documented
   stable interfaces over generated, private, or reverse-engineered endpoints.
3. Keep provider-neutral domain models separate from transport adapters. Use a
   small provider interface, not a speculative multi-provider framework.
4. Prove the smallest slice first: configure catalog, authenticate, fetch and
   parse root, browse one paginated feed, display metadata, download one
   advertised format, verify it, and persist state.
5. Do not add reader/rendering behavior. Defer progress synchronization until
   its protocol and identity rules are independently verified.

## Audit requirements

Record branch, remotes, commits, worktree, file inventory, frontend/native/
transport/persistence/download/progress/Android boundaries, real validation
results, reusable foundations, obsolete assumptions, target architecture,
risks, unknowns, non-goals, and phased milestones. Distinguish unavailable
toolchains from code failures. Inspect history for why the current architecture
exists; do not preserve host/client, peer-to-peer, Calibre-editor, or discovery
behavior solely because it is implemented.

## Protocol research

For OPDS or similar catalogs, verify from live documentation: root URL and
server enablement; authentication type and account scope; root navigation;
books/authors/series/shelves/search feeds; pagination parameters, limits, and
next/previous links; identifiers and metadata; acquisition links and media types;
relative URL resolution; redirect/origin behavior; content length/hash; format
verification; and any separate progress protocol. Use mocked HTTP/XML fixtures.
Never put real credentials or private server URLs in tests, reports, or
diagnostics.

## Identity and domain model

Model catalogs, navigation, publications, acquisitions, download revisions, and
local records independently of provider XML. Prefer provider-scoped stable
publication or canonical identifiers. If absent, use an explicitly provisional
key from normalized title, ordered authors, and format, eligible for later
merge. Never use filename, local path, title alone, or an unscoped integer as
the sole identity. Content hashes identify downloaded revisions and may be the
future progress key, but do not replace catalog identity.

## Safe download state machine

1. Validate scheme and resolve relative links against the configured catalog
   origin. Send Basic Auth only to that origin.
2. Select an advertised allowlisted media type; never infer format from a path.
3. Generate a safe relative path beneath the configured content root.
4. Download to a unique `.part` file with cancellation and durable progress.
5. Retry only retryable failures, retaining an existing verified revision.
6. Verify status, expected length/hash, and format structure such as ZIP/EPUB.
7. Atomically rename only after verification, then mark the database complete.
8. Clean stale partials on startup.
9. For replacement, retain the old revision until the new one completes. For
   server removal, mark unavailable and keep local content by default. Deletion
   must be explicit and root-contained.

Track provider/catalog identity, metadata snapshot, selected format/media type,
integrity data, relative path, state, bytes, errors, timestamps, and supersession
relationships. Keep content storage separate from metadata storage.

## Android

Prefer app-private storage and Storage Access Framework handles over arbitrary
paths. Use WorkManager or a user-visible foreground service only for long-running
user-requested downloads. Request only required permissions, and test process
death, backgrounding, rotation, metered networks, low storage, cancellation,
and restart recovery. Use native secure storage/Android Keystore where possible;
never expose OPDS passwords to WebView logs or diagnostics.

## Progress boundary

Progress is a separate adapter. Verify protocol, credentials, conflicts, and
matching method first. Never map progress by title, filename, or server integer
id. If matching uses file-content hash, preserve byte-identical downloads. An
external handoff to a compatible reader is safer than an unverified adapter.

## Verification

Run focused parser, pagination, auth, duplicate, interruption, integrity, and
path-safety tests, followed by full applicable test/lint/type-check/build gates.
Inspect `git diff --check` and changed files for secrets and machine-specific
paths. Report unavailable toolchains and unverified gates honestly.

## References

- `references/library-client-audit-pattern.md` - reusable audit checklist, evidence table, and OPDS research notes.
