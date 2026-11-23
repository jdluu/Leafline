# Leafline

[![CI](https://img.shields.io/github/actions/workflow/status/jdluu/Leafline/ci.yml?style=for-the-badge&label=CI&logo=githubactions)](https://github.com/jdluu/Leafline/actions/workflows/ci.yml)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

A local-first EPUB reader for Android. Reading, search, annotations, statistics,
and the library all work offline, with no account and no telemetry. Bring your own
books, or browse OPDS 1.x/2.0 catalogs (Calibre-web, Kavita) over your own network.
Progress sync is optional and can go to a KOReader-compatible server you host, to Hardcover, or to both at once.

<img src="screenshots/library.png" alt="Leafline library in list layout: four books with covers, authors, reading-status tags, and progress" width="300" /> <img src="screenshots/reader.png" alt="Leafline reader showing the Chapter 2 heading above justified serif prose, with progress in the footer" width="300" />

## What it does

**Reading.** EPUB 2 and EPUB 3, paginated or scrolled. Curated typography and
accessibility presets including OpenDyslexic and Accessible DfA; adjustable size,
line height, margins, and five color schemes. Edge taps and volume keys turn pages,
with per-book brightness and a footer reporting progress and time remaining.

**Finding your way back.** Footnotes and endnotes preview in a sheet without losing
your place. A history stack moves back and forward across chapters, links, search
hits, and bookmarks, with a floating return capsule after a jump.

**Notes and marks.** Multi-color highlights and notes with color filtering, an
offline dictionary lookup, and full-text search with match case, whole word, regex,
and per-chapter filtering.

**Your library.** Grid and list layouts, multi-select batch actions, collections,
reading-status filters, 1–5 star ratings, custom tags, and sorting by recency,
title, author, or rating.

**Reading time.** On-device tracking with daily and weekly insights, a 7-day
activity chart, and an optional daily goal. The numbers inform; nothing nags.

**Everything leaves cleanly.** Export annotations to Markdown, an Obsidian vault
ZIP, Readwise CSV, or Anki TSV. Back up and restore the whole library as JSON.

**Reading aloud.** On-device text-to-speech with adjustable speed, sentence skip,
and a sleep timer (5–60 minutes or end of chapter) that fades the audio out.

**Finding books.** Browse OPDS catalogs (your own Calibre-web or Kavita, or the
built-in public-domain ones) and download straight into the library.

**Optional Hardcover sync.** If you track reading on Hardcover, Leafline can send
a book's status and page progress there — on its own, or alongside a KOReader
server. You sign in once and the connection refreshes itself, so nothing expires
mid-read; a pasted API token works too. It stays off until you switch it on, the
credentials are encrypted on the device, and nothing is sent for a book with no
ISBN. You choose which books go: all of them, ask once per book, or none, and any
single book can be changed later from its details.

## Loading books

Two ways in, and they compose:

- **Import a file.** The import action opens the system picker: choose one EPUB to
  read right away, or several at once.
- **Point at folders, as many as you organize by.** In the app menu, *Manage folders*
  → *Add* grants a folder and imports every EPUB beneath it, **recursively at any
  depth**. Each saved folder lists its own *Rescan*, so a scan repeats only where
  something changed. A `Calibre/<Author>/<Title (id)>/book.epub` export imports
  correctly from its single top-level folder.

  Android will not grant a whole storage root, so pick a folder such as `Calibre`,
  `Books`, or `Download/Leafline` rather than the root of internal storage. The
  picker says so if the location is refused.

Removing a folder releases the read access granted with it, and re-adding a folder
replaces the stale entry rather than stacking a second grant on the same location.

The same book stored twice — a re-download, another edition of the file, a
duplicated Calibre copy — imports once. The library keeps one row and adopts the
newer file, so reading position is never split across two entries.

**Requires Android 8.0 (API 26) or newer.**

## Install

[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fadd%2Fhttps%3A%2F%2Fgithub.com%2Fjdluu%2FLeafline)

Tap the badge to add Leafline to [Obtainium](https://github.com/ImranR98/Obtainium),
which installs from this repository's releases and keeps it updated. Or download an
APK from [Releases](https://github.com/jdluu/Leafline/releases) directly; you may
need to allow installs from unknown sources.

Releases are pre-1.0 (`0.y.z`): the on-device database schema is not yet stable, so
a major bump can still require a reinstall.

## Build

JDK 17 and the Android SDK.

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # lint
```

`AGENTS.md` records the conventions this repository is built to, including the loop
every change follows. Pull requests run the same checks in CI.

## License

[MIT](LICENSE)
