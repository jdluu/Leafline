# Leafline

[![CI](https://img.shields.io/github/actions/workflow/status/jdluu/Leafline/ci.yml?style=for-the-badge&label=CI&logo=githubactions)](https://github.com/jdluu/Leafline/actions/workflows/ci.yml)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![EPUB](https://img.shields.io/badge/EPUB-8B4513?style=for-the-badge&logo=bookreader&logoColor=white)

Leafline is a free, open-source EPUB reader for Android. It keeps your books,
highlights, and reading progress entirely on your device. The name refers to
both the leaves of a book and a continuous reading line across devices.
For fetching books from OPDS library servers, see
[ShelfSync](https://github.com/jdluu/ShelfSync), a companion sync client that
downloads and hands off EPUBs to any local reader, Leafline included.

Leafline collects no analytics, crash reports, or usage data. Everything stays
on your device except the progress sync you configure yourself against your
own server.

## Screenshots

**Your library** — imported EPUBs with cover thumbnails, reading-status filters,
and collections:

<img src="docs/assets/screenshot-library.png" alt="Leafline library showing imported books with cover thumbnails and reading-status filters" width="320" />

**The reader** — paginated text with real typography: chapter headings, italics,
and paragraph indents, with reading chrome out of the way:

<img src="docs/assets/screenshot-reader.png" alt="Leafline reader showing chapter one of Pride and Prejudice" width="320" />

## Features

- **EPUB reading** — Read EPUB 2 and EPUB 3 books in a paginated or scrolled view
- **On-device library** — Import your own EPUB files with cover thumbnails
- **Progress sync** — Sync reading progress between your devices using any
  KOReader-compatible sync server you host yourself
- **Bookmarks & annotations** — Multi-color highlights and full-text in-book
  search
- **Customizable reading** — Adjustable fonts, themes (including sepia),
  margins, line height, tap zones, page-turn animation, and per-book brightness
- **Text-to-speech** — Optional on-device reading aloud with synchronized page
  turning and spoken utterance highlighting
- **Offline-first** — Works fully offline once books are on the device
- **No telemetry** — No analytics, crash reports, or usage data collected

**Requirements:** Android 8.0 (API 26) or newer.

## Installation

### Obtainium

[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fadd%2Fhttps%3A%2F%2Fgithub.com%2Fjdluu%2FLeafline)

Tap the badge to add Leafline to
[Obtainium](https://github.com/ImranR98/Obtainium), which installs it straight
from this repository's releases and keeps it up to date automatically.

### Manual APK

Download an APK from the [Releases](https://github.com/jdluu/Leafline/releases)
page and install it. You may need to allow installation from unknown sources.

**Note:** Leafline is in early pre-release (`0.0.x`). The current release is
`v0.0.1`; the `0.0.x` line signals that the on-device database schema is not yet
stable. Building from source is described below.

## Building from source

Requirements: JDK 17+ and the Android SDK.

```bash
./gradlew assembleDebug        # build debug APK
./gradlew testDebugUnitTest    # run unit tests
./gradlew lintDebug            # run lint checks
```

Continuous integration runs on every pull request.

## License

[MIT](LICENSE)
