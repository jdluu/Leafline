# Leafline

[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect?url=https%3A%2F%2Fgithub.com%2Fjdluu%2FLeafline)
[![CI](https://github.com/jdluu/Leafline/actions/workflows/ci.yml/badge.svg)](https://github.com/jdluu/Leafline/actions/workflows/ci.yml)

Leafline is a free, open-source EPUB reader for Android. It keeps your books,
highlights, and reading progress entirely on your device, and connects to any
OPDS-compatible library server to browse and download books.

## Highlights

- Read EPUB 2 and EPUB 3 books in a paginated or scrolled view
- Import your own EPUB files into an on-device library with cover thumbnails
- Browse authenticated OPDS catalogs and download books over the network
- Sync reading progress between your devices using any KOReader-compatible
  sync server you host yourself
- Bookmarks, multi-color highlights, and full-text in-book search
- Adjustable fonts, themes (including sepia), margins, line height, tap zones,
  page-turn animation, and per-book brightness

Leafline collects no analytics, crash reports, or usage data. Everything stays
on your device except the progress sync you configure yourself against your
own server.

## Requirements

- Android 8.0 (API 26) or newer

## Installing

### Obtainium

Tap the "Get it on Obtainium" badge above to add Leafline to
[Obtainium](https://github.com/ImranR98/Obtainium), which installs it straight
from this repository's releases and keeps it up to date automatically.

### Manual APK

Download an APK from the [Releases](https://github.com/jdluu/Leafline/releases)
page and install it. You may need to allow installation from unknown sources.

## Using OPDS catalogs

Open the app, add your catalog URL, and sign in with HTTP Basic credentials.
Credentials are held in memory for the session only and never leave the device.
Any OPDS 1.2 catalog works; self-hosted library software such as
[Grimmory](https://grimmory.org/) pairs naturally with Leafline, but it also
works fully offline once books are on the device.

## Building from source

Requirements: JDK 17+ and the Android SDK.

```bash
./gradlew assembleDebug        # build debug APK
./gradlew testDebugUnitTest    # run unit tests
./gradlew lintDebug            # run lint checks
```

See [docs/release-builds.md](docs/release-builds.md) for signed release builds
and F-Droid/Acres packaging notes. Continuous integration runs on every pull
request; tagged releases (`v*`) produce signed APKs attached to GitHub
Releases when signing secrets are configured.

## Contributing

Leafline is maintained by its author. Issues and bug reports are welcome via
the issue tracker; code contributions are accepted case by case.

## License

[MIT](LICENSE)

## Name

Leafline refers to both the leaves of a book and a continuous reading line
across devices.
