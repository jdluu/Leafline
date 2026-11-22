# Leafline

[![CI](https://img.shields.io/github/actions/workflow/status/jdluu/Leafline/ci.yml?style=for-the-badge&label=CI&logo=githubactions)](https://github.com/jdluu/Leafline/actions/workflows/ci.yml)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![EPUB](https://img.shields.io/badge/EPUB-8B4513?style=for-the-badge&logo=bookreader&logoColor=white)

Leafline is a free, open-source EPUB reader for Android built on a local-first
philosophy: every core feature—reading, in-book search, annotations, reading
statistics, and library management—works fully offline without requiring an
account or internet connection. Users can bring their own EPUBs or connect to
OPDS catalogs and compatible servers (such as Calibre-web or Kavita) to browse
and download books directly to their devices. For connected setups, reading
progress can optionally sync across your devices using any KOReader-compatible
sync server you configure.

Leafline collects no analytics, crash reports, or telemetry. Everything stays
on your device except the progress sync and OPDS feeds you choose to configure
against your own servers.

## Screenshots

**Your library** — imported EPUBs with cover thumbnails, reading-status filters,
and collections:

<img src="screenshots/library.png" alt="Leafline library showing imported books with cover thumbnails and reading-status filters" width="320" />

**The reader** — paginated text with real typography: chapter headings, italics,
and paragraph indents, with reading chrome out of the way:

<img src="screenshots/reader.png" alt="Leafline reader showing the chapter opening of Frankenstein with justified serif text" width="320" />

## Features

- **Local-first & fully offline** — Every core feature works completely offline; bring your own books with no accounts, logins, or cloud lock-in required
- **EPUB reading** — Read EPUB 2 and EPUB 3 books in a paginated or scrolled view
- **Footnote & note previews** — Non-disruptive popup bottom sheet previews for footnotes, endnotes, and internal note citations without losing reading position, with optional jump-to-note action
- **Navigation history & quick return** — History back/forward navigation stack and floating return capsule when jumping between chapters, footnotes, links, search results, and bookmarks
- **On-device library & OPDS catalogs** — Import local EPUB files, scan folders, or browse and download directly from OPDS 1.x/2.0 catalogs and compatible servers (such as Calibre-web or Kavita)
- **Progress sync** — Sync reading progress between your devices using any
  KOReader-compatible sync server you host yourself; credentials stay on device
  and the password is encrypted before it is stored
- **Reading statistics & insights** — On-device reading time tracking, daily and
  weekly insights, reading streak counter, interactive 7-day activity chart, and estimated time remaining
- **Bookmarks, annotations & dictionary** — Multi-color highlights, color filtering, notes
  search, advanced full-text in-book search (match case, whole word, regex, chapter filtering, and recent search history), offline dictionary lookup (ColorDict/GoldenDict/system Define), text sharing, and system text actions
- **Notes & highlights export** — Export annotations, notes, and highlights to Markdown, Obsidian-compatible vault ZIP archives, Readwise CSV, and Anki flashcard TSV decks
- **100% on-device backup & restore** — Export and restore complete library metadata, collections, bookmarks, and annotations via JSON
- **Customizable reading & style presets** — Curated typography and accessibility presets (including OpenDyslexic and Accessible DfA), adjustable fonts, text size,
  themes (including sepia), margins, line height, interactive scrubber, edge tap zones, volume key page turning, reading progress HUD
  with estimated time left, page-turn animation, and per-book brightness
- **Text-to-speech & sleep timer** — Optional on-device reading aloud with configurable playback
  speed, sentence-by-sentence skip navigation, countdown sleep timer (5 to 60 min or End of Chapter) with audio volume fade-out and auto-pause, synchronized page turning, and spoken utterance highlighting
- **Library management** — Grid and list layout toggle, multi-selection batch actions (mark status, assign
  collections, delete, tag), collections, reading status filters, 1–5 star ratings, custom tags, tag filtering, and sorting (recent, title, author, rating)
- **Settings that show their answers** — Appearance, progress sync, and notes export each state their current value, so the scheme in use and the sync server are visible without opening a form; the five schemes are previewed as miniature pages drawn in their own colors
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

**Note:** Leafline is in early pre-release (`0.y.z`). The current release is
`v0.4.0`; the `0.y.z` line signals that the on-device database schema is not yet
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
