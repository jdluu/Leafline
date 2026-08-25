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

<p align="center">
  <img src="docs/assets/screenshot-library.png" alt="Library view" width="280" />
  <img src="docs/assets/screenshot-reader.png" alt="Reader view" width="280" />
</p>

## Features

- **EPUB reading** — Read EPUB 2 and EPUB 3 books in a paginated or scrolled view
- **On-device library** — Import your own EPUB files with cover thumbnails
- **Progress sync** — Sync reading progress between your devices using any
  KOReader-compatible sync server you host yourself
- **Bookmarks & annotations** — Multi-color highlights and full-text in-book
  search
- **Customizable reading** — Adjustable fonts, themes (including sepia),
  margins, line height, tap zones, page-turn animation, and per-book brightness
- **Offline-first** — Works fully offline once books are on the device
- **No telemetry** — No analytics, crash reports, or usage data collected

**Requirements:** Android 8.0 (API 26) or newer.

**Note:** Leafline is pre-release. No APKs are published yet; install by
building from source below.

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
