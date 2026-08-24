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
- Releases are cut from `main` and tagged `v<versionName>` (e.g. `v0.2.0`).

## [Unreleased]

### Added

- User-defined collections/tags with filter chip row (#16)
- Reading status flags (unread/reading/finished) with auto-suggested transitions (#17)
- Continue-reading shelf with recently-read books (#18)
- Bulk EPUB import from SAF with per-file error reporting (#19)
- Book detail sheet with metadata, file info, and actions (#20)
- Sync conflict bottom sheet comparing local vs remote progress (#24)
- Background sync via WorkManager for periodic progress push (#23)
- Adaptive app icon and SplashScreen API integration (#15)

### Fixed

- KOReader partial-MD5 hash now matches KOReader master's exponential-sampling
  algorithm exactly; Room index declarations added to fix migration crash (#22)
- Settings page is now scrollable so all KOReader Sync fields are reachable
- Cleartext HTTP permitted for local server connections (Grimmory on LAN)

## [0.1.0] - 2026-08-22

### Added

- EPUB reader built on Readium Kotlin Toolkit: paginated and scrolled modes,
  configurable tap zones, page-turn animations, continuous scroll
- Reading settings with live preview: font catalog, style modes, margins,
  themes (light/sepia/dark/OLED/e-ink high-contrast), brightness quick control
- Sepia quick control in reader overlay with restore memory
- Bookmarks, highlights with curated tint palette and swatch picker, in-book search
- Table of contents navigation
- Local library: grid with cover thumbnails, sorting, search
- OPDS catalog browsing and authenticated download from Grimmory-compatible servers
- KOReader-compatible reading position sync (push/pull with conflict detection)
- Accessibility: TalkBack audit with content descriptions and page announcements,
  dynamic type up to 200%, 48dp touch targets, reduced-motion support
- Dynamic color theme support and full palette refresh

[Unreleased]: https://github.com/jdluu/Leafline/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/jdluu/Leafline/releases/tag/v0.1.0
