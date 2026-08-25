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

The project is pre-release: no versions have been published. Release builds
and tagging are on hold until the feature set stabilizes.
