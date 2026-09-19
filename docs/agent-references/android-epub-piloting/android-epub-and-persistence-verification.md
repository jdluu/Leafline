# Android EPUB and Persistence Verification Notes

Reusable evidence patterns for autonomous Leafline-style work.

## Readium API verification

Before delegating a mapper, verify the resolved Readium artifact or official 3.x
reference from the orchestrator. For Readium Kotlin 3.3.0, the reliable access
path is `publication.metadata`; the metadata object exposes the fields used by
the reader slice, including identifier, title, language, description, published,
authors, publishers, and numberOfPages. Keep prompts tied to verified getters,
not speculative architecture documents.

## Real EPUB device test

Use an authoritative public-domain EPUB only as a temporary device fixture. Validate
it as a ZIP and check the EPUB `mimetype`, push it to the device Download folder,
and trigger `MEDIA_SCANNER_SCAN_FILE` if DocumentsUI does not list it immediately.
Open the app picker, select the fixture, confirm the reader activity and Readium
navigator are resumed, then capture a screenshot after advancing beyond the cover.
A cover alone proves opening, not readable body content; verify prose, metadata, or
TOC text as well. Do not commit the fixture.

## Autonomous evidence discipline

A delegated agent's summary is not verification. Independently inspect status,
commit, changed-file inventory, and diff checks. Run Gradle commands without
`head`, `tail`, pipelines, or `||` so failures cannot be masked. For UI claims,
query the device UI hierarchy and activity state; inspect logcat for fatal crashes.

## Persistence milestone audit

Keep the backend claim and dependency graph consistent. An in-memory repository is
an interim boundary, not Room. Room dependencies require a real compiler setup and
tests that exercise the Room backend. If that setup is not verified, prefer the
correct in-memory implementation with a documented next experiment rather than a
broken hybrid or guessed KSP version. Check ID generation, synchronized Flow
emissions, duplicate/upsert semantics, complete field round-trips, and ordering.
