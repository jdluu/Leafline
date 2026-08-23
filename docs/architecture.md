# Leafline Architecture

## Scope

Leafline owns the Android reading experience and local reader state. It consumes
books from OPDS-compatible services and does not modify server-side libraries.

## Package layout

```text
app/src/main/java/com/jdluu/leafline/
├── library/            # Library screen, ViewModels, settings, DI holder
│   └── data/           # Book models, repositories, Room DAOs/entities
├── opds/               # OPDS client: catalog browsing, download coordination
├── sync/               # KOReader-compatible progress sync (pure Kotlin)
└── reader/             # In-book search support; ReaderActivity hosts Readium
```

`ReaderActivity.kt` lives at the package root and hosts the Readium navigator,
reader UI, bookmarks, highlights, and search wiring. UI code does not call HTTP
clients or manipulate EPUB archives directly.

## Technology stack

- Kotlin, Jetpack Compose, Material 3
- Readium Kotlin Toolkit 3.3.0 for EPUB parsing, rendering, search, and the
  Decorator API used for highlight and match decorations
- Room for durable local state (library, bookmarks, annotations, reading
  positions), with explicit migrations per schema change
- OkHttp for OPDS and progress-sync networking
- Kotlin coroutines and Flow for asynchronous work

## Feature map

- **Library**: grid with cached cover thumbnails, sort by recent/title/author,
  live title/author filtering (`BookDao.searchBooks`)
- **OPDS**: authenticated OPDS 1.2 catalog browsing and EPUB acquisition into
  the local library; credentials are session-scoped in memory only
- **Reader**: TOC drawer, font/theme/line-height preferences, reading-position
  persistence and restore, bookmarks, in-book search via the Readium search
  service, text-selection highlights backed by the `annotations` table
- **Progress sync**: KOReader-compatible sync against Grimmory's
  `/api/koreader` endpoints

## Progress sync decisions

- Transport: OkHttp with HTTP Basic auth on every call
  (`GET /users/auth`, `GET /syncs/progress/{bookHash}`,
  `PUT /syncs/progress`). The client lives in `com.jdluu.leafline.sync` and
  has no Android dependencies, so it is covered by JVM unit tests against
  MockWebServer.
- Book identity: the KOReader partial MD5 convention implemented in
  `FileHashUtil.koreaderHash`. Files larger than 1024 bytes hash the
  concatenation of the first and last 1024 bytes; smaller files (including
  empty ones) use the full-file MD5. The value is stored in the
  `books.koreaderHash` column (Room migration 5 to 6, which also adds
  `books.lastReadAtEpochMillis`). The pre-existing `fileHash` column keeps its
  SHA-256 meaning for stable identity and dedup.
- Percentage: 0-100 floats, derived from the locator's
  `locations.totalProgression` with `locations.progression` as fallback.
- Timestamps: remote timestamps are normalized from seconds or milliseconds
  before comparison against the locally stamped last-read time; when a server
  omits the timestamp the comparison falls back to percentages.
- Credentials: `KoreaderSyncConfig` mirrors `OpdsServerConfig`. Values live
  only in the session-scoped config stores and are never written to disk, git,
  or logs.
- Reader integration: opening a book pulls remote progress and offers a jump
  when the remote timestamp is newer; leaving the reader pushes the current
  locator. Success is silent, failures surface as toasts.
- Note: current KOReader master uses an exponential-sampling partial MD5 rather
  than the first+last-1024 scheme above. If real KOReader devices hash
  differently against a server, `FileHashUtil.koreaderHash` is the single
  place to adjust.

## Open items

- release license and distribution channel;
- minimum Android version after testing on target e-ink devices;
- optional follow-ups on top of highlights: note editing UI, per-color tints,
  swipe-to-delete in the highlights sheet.

## Quality gates

Every change should include:

- unit tests for domain and parser behavior;
- fixture-based EPUB tests for reader behavior where practical;
- `git diff --check`;
- applicable Gradle test, lint, and debug build commands.

A build cannot be reported as verified until the command has actually run on a
machine with the Android toolchain installed.

## Sources

- https://readium.org/kotlin-toolkit
- https://readium.org/kotlin-toolkit/3.3.0/guides/getting-started
- https://grimmory.org/docs/integration/opds
- https://grimmory.org/api
- https://developer.android.com/
