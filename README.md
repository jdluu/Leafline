# Leafline

Leafline is an Android EPUB reader built for focused reading on phones,
tablets, and e-ink devices. It keeps your books and reading progress on your
device, and connects to any OPDS-compatible library server to browse and
download books.

## Features

- Render EPUB 2 and EPUB 3 books with a paginated reader from the Readium
  Kotlin Toolkit
- Import local EPUB files into an on-device library with cover thumbnails
- Sort by recent, title, or author; filter as you type
- Browse authenticated OPDS 1.2 catalogs and download books into the library
- Table of contents, adjustable fonts, themes, margins, and line height
- Reading position is remembered and restored when you reopen a book
- Bookmarks, highlights, and in-book search
- Configurable tap zones and page-turn animation
- Sync reading progress across devices with KOReader-compatible servers

Leafline pairs naturally with self-hosted library software such as
[Grimmory](https://grimmory.org/), but works fully offline once books are on
the device.

## Requirements

- Android 8.0 (API 26) or newer

## Building from source

Requirements: JDK 17+, Android SDK.

```bash
./gradlew assembleDebug   # build debug APK
./gradlew test            # run unit tests
./gradlew lint            # run lint checks
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## OPDS setup

Open the app, add your catalog URL, and sign in with HTTP Basic credentials.
Credentials are held in memory for the session only and never leave the
device. The catalog server must expose OPDS access for your account.

## Status

Leafline is under active development and not yet published. There is no
release license yet; until one is added, all rights are reserved.

## Name

Leafline refers to both the leaves of a book and a continuous reading line
across devices.
