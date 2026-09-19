---
name: android-reader-development
description: "Use for native Android ebook reader development."
version: 0.1.0
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [android, kotlin, epub, ebook-reader, readium, opds, gradle, compose]
    related_skills: [opencode, grimmory-library]
category: software-development
---

# Android Reader Development

Use this class-level skill when creating or extending a native Android EPUB reader
that consumes an OPDS library such as Grimmory. The goal is a small, reproducible,
local-first reader rather than a server administrator, Calibre replacement, or
library-hosting application.

## Core architecture

Keep these concerns separate:

```text
Compose UI
  -> ViewModels / immutable UI state
  -> domain models and use cases
  -> repository interfaces
       -> Room/local files
       -> OPDS/HTTP data source
       -> EPUB publication/navigation engine
       -> progress-sync adapter
```

The reader owns local books, reading state, preferences, and the reading
experience. A server integration owns catalog browsing, acquisition, and progress
transport. Do not make rendering depend on a server being reachable.

## Technology baseline

Prefer Kotlin, Jetpack Compose/Material 3, ViewModel plus StateFlow, Readium
Kotlin Toolkit, Room, WorkManager, kotlinx.serialization, Coil, JUnit, Compose
UI tests, and protocol fixtures. Use Readium for EPUB parsing, navigation,
pagination, preferences, search, and annotations. Do not implement a new EPUB
renderer casually. Choose Ktor or OkHttp only after an OPDS/API spike.

## Bootstrap procedure

1. Research current official Android, AGP, Gradle, Kotlin, Compose, and Readium
   compatibility guidance before selecting versions.
2. Install a supported JDK, Gradle wrapper, Android SDK command-line tools,
   target platform, build tools, and platform tools. Accept licenses using the
   documented SDK manager flow.
3. Commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/*`; build through the
   wrapper rather than machine-specific system Gradle.
4. Create the smallest shell: one app module, launcher activity, theme,
   placeholder screen, `.gitignore`, README, and agent instructions.
5. Run `./gradlew help`, `./gradlew test`, `./gradlew lint`, and
   `./gradlew assembleDebug` before adding reader dependencies.
6. Add Readium through a bounded spike: one bundled EPUB, rendering, TOC
   navigation, locator save, and locator restoration. Do not combine it with
   Grimmory authentication or sync.
7. Use conventional commits without emojis or emdashes and keep generated output
   and local configuration ignored.

## EPUB and progress identity

Never identify a book only by filename. Prefer a stable server identifier or
acquisition link, then the EPUB package identifier, then a content hash fallback.
Persist a Readium-compatible locator plus a percentage fallback. Keep progress
sync behind an adapter so Grimmory/KOReader compatibility can evolve independently
of the reader UI.

## Grimmory/OPDS boundary

Start with documented OPDS and HTTP Basic Auth. Cover catalog discovery,
pagination, search, acquisition, and local storage first. Use an OPDS user with
only needed library access. Never store credentials in source, Gradle files, Git,
or a committed `.env`. Validate the live Grimmory version before relying on
undocumented endpoint or permission behavior.

## Development workflow

For every repository task, keep public artifacts strictly professional: do not mention internal orchestration, model routing, agent identities, or private session details in issue text, pull requests, commit messages, branch names, code comments, or project documentation. Treat this as a release-safety check before publishing GitHub or repository text.

For any non-trivial change, inspect the live issue, current branch and status, recent history, relevant source, and existing tests before editing. Use a feature branch, make the smallest vertical slice, review the diff, run local gates, push, inspect CI, and merge only after required checks succeed. Delete the merged remote branch and verify the final local branch and working tree.

Use pure reusable helpers for width-class and other layout decisions so breakpoint behavior can be tested on the JVM without requiring a device. Do not infer responsive behavior from orientation alone: use measured window width classes or equivalent width thresholds, and test every boundary.

Two modes are established for this project:

### OpenCode workflow (for larger vertical slices)

OpenCode is appropriate when the change spans multiple files and benefits from
Plan/Build modes, bounded runs, and reviewable Git changes. Use Plan mode for
architecture and dependency decisions and Build mode for one vertical slice at a
time. Keep `AGENTS.md` current. Every prompt should name scope, acceptance
criteria, and verification command. Review the diff and run Gradle checks before
committing.

### Direct workflow (for focused feature issues)

When working directly (Hermes writing files without OpenCode mediation), use the
per-issue board-driven cycle from `references/board-driven-dev-cycle.md`:

1. Pick next Todo → branch → implement across all layers (Entity → Dao →
   DataSource → Repository → ViewModel → Screen) → verify with grep checks →
   write unit test → commit with "Closes #N" → PR → squash-merge → board update.
2. Verify with grep counts across ALL modified files after each feature — check
   that every expected symbol name appears in its target layer. This is faster
   than Gradle and catches wiring gaps (missing imports, stubbed interfaces).
3. Gradle verification is done when a toolchain is available; until then, grep
   checks and `git diff --check` are the primary quality gates.

Prefer the direct workflow for small, well-defined features where the scope is
clear from a single issue. Prefer OpenCode when the change is exploratory or
benefits from a Plan phase first.

## Secret handling for OpenCode

OpenCode expects `OPENROUTER_API_KEY`; a custom variable such as
`OPENCODE_OPENROUTER_API_KEY` is not automatically recognized. When using BWS,
resolve the exact secret at process start, export it only to the child process as
the standard variable, never write it to disk, and refuse to run if
`BWS_ACCESS_TOKEN` or the secret is unavailable. A `.env` is unnecessary when
BWS is available; an untracked `.env` or `/connect` is a fallback.

## Verification checklist

- [ ] Official compatibility guidance checked.
- [ ] `./gradlew help`, `test`, `lint`, and `assembleDebug` succeed.
- [ ] APK artifact exists and its path is known.
- [ ] `git diff --check` succeeds and Git status is clean after commit.
- [ ] No credentials, signing files, APKs, `local.properties`, or build directories
      are tracked.
- [ ] OpenCode smoke testing proves the provider path, not only the CLI version.

## Pitfalls

- AGP, Gradle, Kotlin, JDK, Compose, and Readium versions must be treated as a
  compatibility set.
- Major AGP releases can change Kotlin plugin requirements; follow current
  guidance and actual build errors rather than obsolete declarations.
- Gradle configuration success is not an Android build; run assemble and inspect
  the artifact.
- `NO-SOURCE` means no tests were discovered, not that behavior has coverage.
- A custom OpenRouter variable does nothing until mapped to
  `OPENROUTER_API_KEY`.
- Never put BWS tokens or resolved keys in history, project files, or logs.
- Keep the first EPUB spike independent of Grimmory availability.

## References

- `references/verified-toolchain-and-opencode-bws.md` — validated setup pattern,
  compatibility correction, and secret-resolution smoke-test notes.
- `references/room-persistence-setup.md` — Room KSP/Gradle wiring, TypeConverters,
  domain-model separation, DEX-safe test names.
- `references/room-migration-patterns.md` — adding columns/tables via incremental
  ALTER TABLE migrations, version numbering, no destructive fallback.
- `references/readium-opds-integration.md` — OPDS bootstrap, pagination, search,
  download, and Grimmory-specific endpoint notes.
- `references/readium-opds-3.3.0-api.md` — Readium OPDS 3.3.0 API exploration,
  Page API, entries, metadata, and discovery patterns.
- `references/live-grimmory-opds-integration.md` — verified OPDS catalog access
  and download on a live Grimmory instance.
- `references/live-opds-locators-and-git-rewrite.md` — locator persistence
  and Git history rewrite record.
- `references/reader-ui-patterns.md` — Compose overlay on Readium fragment,
  tap zones, toolbar, brightness controls.
- `references/reader-settings-and-config-patterns.md` — reader settings
  ModalBottomSheet, OPDS config in Settings tab, debug config injection.
- `references/compose-library-screen-patterns.md` — FilterChip rows,
  combinedClickable for long-press, ModalBottomSheet detail sheets,
  reading status badges, continue-reading shelf, sync conflict sheet.
- `references/board-driven-dev-cycle.md` — per-issue development loop:
  pick → branch → implement across all layers → verify → commit → PR →
  merge → board update via GraphQL.
- `references/repo-hygiene-and-github-workflow.md` — user-corrected conventions
  for README vs AGENTS.md, GitHub workflow, board/issue bootstrap, gh token
  pitfalls.
