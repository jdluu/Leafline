# Changelog

All notable changes to Leafline are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Versioning policy

- **MAJOR** (1.0.0+): breaking changes to the on-device database schema without
  a migration path, removal of user-facing features, or changes that invalidate
  synced reading progress.
- **MINOR**: new user-facing features (reader capabilities, library features,
  sync features). This is the default bump for roadmap phase completions.
- **PATCH**: bug fixes, performance work, accessibility fixes, dependency
  updates with no new features.
- **versionCode** increments monotonically with every release build; it never
  resets and is independent of versionName.
- Pre-1.0 (0.y.z): the API is unstable; MINOR bumps may contain breaking
  changes, which are called out explicitly in the changelog entries.

## [Unreleased]

## [0.0.1] - 2026-09-29

First public pre-release.

- EPUB 2 and EPUB 3 reading via the Readium Kotlin Toolkit, in paginated or
  continuous-scroll mode, with configurable tap zones, page-turn animation, and
  reduced-motion awareness.
- Publisher and custom typography: font stacks (including OpenDyslexic and
  Accessible DfA), line height, margins, themes, and per-book brightness.
- Text-to-speech reading mode with synchronized progression.
- On-device import of local EPUBs (single and bulk) plus saved-folder scanning
  and rescans, collections, reading-status flags, a continue-reading shelf,
  sorting, and search.
- Bookmarks, multi-color highlights with editable notes, full-text in-book
  search, and annotation export to Markdown.
- KOReader-compatible progress sync with background sync and conflict handling.
- Accessibility: TalkBack labels and announcements, dynamic type to 200%,
  48dp touch targets, WCAG AA contrast, and reduced-motion support.
- No analytics, crash reporting, or telemetry.

Requires Android 8.0 (API 26) or newer.
