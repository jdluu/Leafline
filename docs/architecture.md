# Leafline Architecture Notes

## Scope

Leafline owns the Android reading experience and local reader state. It consumes
books from OPDS-compatible services and does not modify server-side libraries.

## First vertical slice

The first feature slice is intentionally narrow:

1. package a known-good EPUB as a test fixture;
2. open it through Readium Kotlin Toolkit;
3. render reflowable content;
4. expose the table of contents;
5. save a locator locally;
6. restore that locator after recreation.

No Grimmory authentication, download queue, sync, annotations, or full library
UI belongs in this slice. Keeping the reader engine isolated makes failures easy
to diagnose and prevents network concerns from hiding rendering problems.

## Proposed package layout

```text
app/src/main/java/com/jdluu/leafline/
├── app/            # Application class, navigation, dependency wiring
├── domain/         # Book, Locator, ReadingProgress, repository contracts
├── data/local/     # Room database, DAOs, file storage
├── data/opds/      # OPDS models, parser, HTTP data source
├── data/sync/      # Progress-sync adapters and retry queue
├── reader/         # Readium publication/session integration
└── feature/        # Compose screens and ViewModels
```

The exact split may change after the Readium spike, but UI code should not call
HTTP clients or manipulate EPUB archives directly.

## Progress sync

Leafline syncs reading positions with KOReader-compatible servers (Grimmory's
`/api/koreader` endpoints) so progress moves between Leafline, KOReader
devices, and the Grimmory web reader.

Decisions recorded for this slice:

- Transport: OkHttp with HTTP Basic auth on every call
  (`GET /users/auth`, `GET /syncs/progress/{bookHash}`,
  `PUT /syncs/progress`). The client lives in `com.jdluu.leafline.sync` and
  has no Android dependencies, so it is covered by JVM unit tests against
  MockWebServer.
- Book identity: the KOReader partial MD5 convention. Files larger than
  1024 bytes hash the concatenation of the first and last 1024 bytes;
  smaller files (including empty ones) use the full-file MD5. The value is
  stored in the `books.koreaderHash` column (Room migration 5 to 6, which
  also adds `books.lastReadAtEpochMillis`). The pre-existing `fileHash`
  column keeps its SHA-256 meaning for stable identity and dedup.
- Percentage: 0-100 floats, derived from the locator's
  `locations.totalProgression` with `locations.progression` as fallback.
- Timestamps: remote timestamps are normalized from seconds or milliseconds
  before comparison against the locally stamped last-read time; when a
  server omits the timestamp the comparison falls back to percentages.
- Credentials: `KoreaderSyncConfig` mirrors `OpdsServerConfig`. Values live
  only in the session-scoped `KoreaderSyncConfigStore` object and are never
  written to disk, git, or logs.
- Reader integration: opening a book pulls remote progress and offers a jump
  when the remote timestamp is newer; leaving the reader pushes the current
  locator. Success is silent, failures surface as toasts.

## Decisions pending

- exact Readium Kotlin version and module set;
- Ktor versus OkHttp for OPDS networking (progress sync uses OkHttp);
- Room schema and stable book identity strategy;
- release license and distribution channel;
- minimum Android version after testing target e-ink devices.

Record each decision in this document or an ADR before implementation relies on
it.

## Quality gates

Every vertical slice should include:

- unit tests for domain and parser behavior;
- Compose/UI tests for user-visible state;
- fixture-based EPUB tests for reader behavior;
- `git diff --check`;
- applicable Gradle test, lint, and debug build commands.

A build cannot be reported as verified until the command has actually run on a
machine with the Android toolchain installed.

## Sources

- https://readium.org/kotlin-toolkit
- https://readium.org/kotlin-toolkit/3.3.0/guides/getting-started
- https://grimmory.org/docs/integration/opds
- https://developer.android.com/
- https://kotlinlang.org/docs/home.html
- https://docs.gradle.org/current/userguide/userguide.html
