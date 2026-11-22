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

### Attribution

Every entry ends with its citation on the entry's last line, after the prose and
never inside the sentence. The shape follows where the work is in its life:

| Stage | Written as | Links to |
|---|---|---|
| On the branch, unmerged | `(#220)` | the pull request |
| Merged to main | `(7a77307, #220)` | the commit and the pull request |
| Released before this convention | *(exempt)* | — |

`(#220)` alone is what an author can write: a squash or rebase rewrites the
commits, so a SHA written on the branch would point at nothing. On merge, the
`Settle changelog citations` workflow rewrites it on main to `(7a77307, #220)`.
`scripts/changelog_settle.sh` does the same by hand and only runs on main.

Both parts render as links, since GitHub autolinks a bare SHA and a `#220`. A `#`
on the SHA is not used: that is issue syntax, and `#abc1234` renders as plain
text. Entries under [Unreleased] must cite their pull request, so promoting them
in a release never produces an unattributed entry.
`scripts/check_changelog_refs.sh` verifies the shape and that every cited commit
exists on the branch; CI fails without it. Sections 0.0.1 through 0.4.0 are
exempt: their text was composed in batches, so a per-entry SHA there would be a
guess rather than a record.

## [Unreleased]

### Changed
- **The agent documentation leads with the workflow.** AGENTS.md opened with a
  command list, so a new agent met the commands before the loop that decides when
  to run them. The branch, changelog, pull request, and merge sequence now comes
  first, and the pull request template states the citation rule where it applies.
  (PENDING_PR)

## [0.4.10] - 2026-11-23

### Changed
- **One filter row instead of two.** The library showed reading-status chips and a
  separate progress chip row, which asked the reader to tell "Unread" from "Not
  started" and pushed the shelf down. Progress filtering now lives in a sort and
  filter sheet beside the existing sort options, the toolbar icon carries the accent
  when a filter is hiding books, and an empty result names the filter that caused it. (722beb9, #225)


## [0.4.9] - 2026-11-22

### Changed
- **Settings is rebuilt as rows that show their current answer** instead of a
  stack of form blocks separated by rules. The appearance choice is previewed as
  a miniature page in each scheme's own colors, the KOReader sync form stays
  collapsed behind a row that reports whether sync is on and to which server,
  and the notes export is a row rather than the loudest button on the page. (7a77307, #220)
- **Saving a sync server is gated on an address the app can use.** Save and the
  background switch stay off until the field holds an absolute http or https URL
  with a host, the field says why, and turning sync off stays available whatever
  the field holds. (7a77307, #220)

### Fixed
- **The reader's progress footer is readable on every page.** It painted the app
  scheme's secondary gray at 45% over the book page, so a dark app with a white
  page left it nearly invisible; chrome ink now comes from the reading theme and
  clears 4.5:1 on light, sepia, and dark pages. The surround behind the page
  follows the same rule, which removes the dark band under an unset theme. (e4ea8f0, #221)
- **The catalog error state offers a recovery it can perform.** A sign-in error
  told the reader to edit a catalog the app cannot edit and offered only Retry;
  it now names adding the catalog again with credentials and carries that action
  beside Retry, with the raw status code demoted to a caption. (a765266, #223)
- **A book tile shows the whole author name** instead of clipping it to an
  ellipsis at a large system font scale. (8b96ecf, #222)
- **The export sheet no longer offers actions over an empty file.** With no
  annotated books it showed format chips, a preview, and Save/Share buttons over
  a "0 books • 0 highlights" header; it now shows the leaf mark, "Nothing to
  export yet", and one line saying what would put something there. The vault
  preview also stops using an emoji as an icon. (7a77307, #220)

## [0.4.8] - 2026-11-16

### Fixed
- **First-run library showed three competing signals.** Two rows of filter chips
  applied to zero books, an import FAB duplicating the empty state's import
  button, and copy promising "add a local folder" while only offering Import
  EPUB. Filters and the FAB now appear once there is something to act on, and the
  copy matches the single action offered while still pointing at the menu for
  folder scanning. (4e2ba92, #217)

### Changed
- The empty library is treated as the designed moment the contract describes: one
  leaf mark, one heading, one line, one action. (4e2ba92, #217)


## [0.4.7] - 2026-11-16

### Fixed
- **Export sheet actions were off-screen.** The sheet opened partially expanded, so
  its content slot clipped the action row no matter how the layout was arranged.
  It now opens expanded, only the content scrolls, and Share (outlined) and Save
  (filled) stay pinned and reachable. (3e6310e, #216)
- **Catalog search field was invisible.** The search field in the Explore top bar
  drew a paper container on a paper toolbar. It now uses the mist tone so the
  input reads as an inset. (3e6310e, #216)

### Changed
- Text fields accept an optional container tone and a nullable label, so the same
  component serves grouped forms and bare toolbar inputs. (3e6310e, #216)
- `DESIGN.md` now documents the surface container ramp and the rule that sheets
  pin their primary action and open expanded. (3e6310e, #216)


## [0.4.6] - 2026-11-16

### Fixed
- Contents list rows drew an 8dp surface inside a 12dp clipped ripple, so the
  selected chapter's corners did not match the rest of the app. Both now use the
  single container shape. (6fa3904, #215)


## [0.4.5] - 2026-11-16

An app-wide pass checking the UI against the design contract. Most findings were
one root cause: two places where the theme was not applied, so Material's
baseline palette leaked through.

### Fixed
- **Reader chrome used the wrong palette.** `ReaderActivity` never wrapped its
  Compose tree in `LeaflineTheme`, so the drawer, sheets, dialogs and toolbar all
  rendered with Material's baseline scheme: lilac drawer surfaces and purple tab
  indicators and selected rows. Reader chrome now follows the app scheme. (acdf434, #214)
- **Missing surface-container roles.** The light, dark, OLED and e-ink schemes
  never set `surfaceContainer*`, `surfaceDim`, `surfaceBright`, `inverseSurface`
  or `surfaceTint`, so every component reading those roles (navigation bar,
  menus, dialogs, sheets) fell back to baseline lilac neutrals. A green-tinted
  container ramp is now defined for all four schemes. (acdf434, #214)
- **Radii off the 12dp contract.** Tag chips, the export preview panel and
  contents rows used 8dp, and the jump notice used 24dp with a drop shadow. All
  now share the single container radius, and the shadow is gone. (acdf434, #214)
- **Contents tabs were ellipsised** ("Bookma…"). The fixed tab row split the
  width three ways; it is now scrollable and sizes to its labels. (acdf434, #214)
- **Progress filters clipped mid-word.** Five long labels in one row ran off the
  edge and the last chip read "In progres". Labels are now short ("Not started",
  "In progress", "Almost done", "Finished") with the ranges carried by the
  accessibility label. (acdf434, #214)
- **Export sheet** content could run under the gesture bar. (acdf434, #214)

## [0.4.4] - 2026-11-16

### Changed
- **Credential and catalog forms** now follow the design contract. Fields were
  Material's default outlined text fields, which carry a 4dp radius and a border
  against an app whose single container radius is 12dp. They are now paper-toned
  insets at 12dp with no outline, and grouped fields sit on a quiet mist panel so
  the block reads as one object. (9e8e0ce, #213)
- **KOReader Sync** has one emphasized action: Save is filled and Test connection
  is outlined. The password gained a visibility toggle, the background-sync switch
  reads as part of the group with its own supporting line, and save/test results
  now pair an icon with the message instead of relying on colour. (9e8e0ce, #213)
- **Add Catalog Feed** uses a segmented single-choice control for the
  authentication method, replacing chips that wrapped onto a second row, and
  separates feed details from credentials with a hairline divider. (9e8e0ce, #213)

## [0.4.3] - 2026-11-16

### Fixed
- **KOReader sync**: Server, username, and password were held in memory only, so
  progress sync could not survive an app restart and background sync had no
  settings to work with. They are now stored on device. (0667c9c, #212)

### Security
- The sync password is encrypted with an AES/GCM key held in the Android Keystore
  before it is written, so the preference file alone does not expose it. The
  Settings caption now states this instead of claiming the credentials are
  session-only. (0667c9c, #212)

## [0.4.2] - 2026-11-16

### Fixed
- **Reader**: The reading status footer was drawn over the page with no reserved
  space, so the last line of text ran underneath it. The navigator now reserves
  the footer's height, keeping page text clear of the label. (9a78480, #211)
- **TTS**: Playback paused itself about 100 ms after starting or resuming. The app
  and Readium's TTS navigator both requested audio focus, and the navigator's
  request took focus back, delivering `AUDIOFOCUS_LOSS` to the app's listener. The
  app no longer claims audio focus for TTS and defers to the navigator, which
  already handles interruption and resume. Headphone-disconnect pausing is
  unchanged. (9a78480, #211)
- **Reader**: Highlight excerpts truncated mid-word; context is now cut on word
  boundaries. (9a78480, #211)

### Removed
- `TtsAudioFocusManager`, superseded by the navigator's own audio focus handling. (9a78480, #211)

## [0.4.1] - 2026-11-16

Patch release from a full device pass over 0.4.0 on a Pixel 7 (Android 17): every
screen was exercised by hand, and the defects below only surfaced in use, not in
the unit tests or lint.

### Added
- **Reader text size control**: Stepper in Reader Settings from 80% to 160% in 10%
  steps, persisted per reading preferences and clamped on restore. (aa5fb65, #210)
- **Settings version block**: "Leafline <version>" with the on-device privacy
  statement at the foot of Settings, so support reports can name the installed
  build. (aa5fb65, #210)
- **Batch action overflow menu**: Remaining bulk actions (edit tags, export notes,
  delete) moved into an overflow menu when the selection bar runs out of room. (aa5fb65, #210)
- **Restore result summary**: Backup restore now reports matched books,
  collections, bookmarks, and highlights with correct singular/plural wording. (aa5fb65, #210)

### Fixed
- **Reader**: Programmatic jumps (contents, search, bookmarks) were labelled
  "Footnote / Link" in the jump notice; each source now supplies its own
  description. (aa5fb65, #210)
- **Reader**: Text-to-speech sentence following pushed a navigation-history entry
  and showed a jump banner on every utterance. (aa5fb65, #210)
- **Reader**: Bookmarks created in a chapter were stored with no label and listed
  by resource file name; they now capture the chapter title. (aa5fb65, #210)
- **Reader**: Highlights without a Readium `text.exact` locator field were listed
  by their XHTML file name; they now quote the highlighted passage with context. (aa5fb65, #210)
- **Reader**: Contents tab labels were ellipsised ("Content…") because counts were
  appended to each tab title; counts now live inside each pane. (aa5fb65, #210)
- **Library**: The active collection chip's close glyph did not clear the filter. (aa5fb65, #210)
- **Library**: The sort menu clipped "A-Z" / "Z-A" option spacing. (aa5fb65, #210)
- **Library**: The reading goals editor squeezed its value field vertically. (aa5fb65, #210)
- **Library**: Reading Insights metrics and tag rows could overflow their columns. (aa5fb65, #210)
- **Library**: The selection bar title overflowed instead of ellipsising, and its
  action row was truncated on narrow screens. (aa5fb65, #210)
- **Library**: Backup restore buttons sat under the gesture bar on gesture-nav
  devices; the sheet now reserves navigation bar insets. (aa5fb65, #210)
- **Backup**: Exported backups recorded `appVersion` as the literal `1.0`; the
  real installed version is now written. (aa5fb65, #210)
- **Explore**: Catalog failures read "HTTP 401:" with no guidance; 401/403, 404,
  and 5xx now explain what the reader can do. (aa5fb65, #210)
- **Explore**: Catalogs exposing a "Home" link that points at the feed itself
  suppressed the empty state and looped; self-links are filtered as noise. (aa5fb65, #210)
- **Explore**: The selected catalog was forgotten on every restart; it is now
  persisted. (aa5fb65, #210)
- **Explore**: The API token field in the Add Catalog dialog was compressed
  vertically. (aa5fb65, #210)
- **Explore**: Requests now send an accurate `User-Agent` with the app version, so
  catalog servers can tell builds apart. (aa5fb65, #210)

## [0.4.0] - 2026-11-16

### Added
- **TTS Foreground Audio Service & Media Notification**:
  - Implemented `TtsAudioService` running as an Android `mediaPlayback` foreground service to keep text-to-speech continuous across screen lock and app switches.
  - Implemented `TtsNotificationManager` building a media-styled (`Notification.MediaStyle`) playback notification with transport actions (Previous, Play/Pause toggle, Next, Stop).
  - Wires deep link navigation on notification tap to resume `ReaderActivity` immediately.
  - Synchronizes dynamic chapter, book title, author, and play/pause state in real-time.
- **TTS MediaSession & Headset Remote Controls**:
  - Integrated `TtsMediaSessionManager` with Android `MediaSession` exposing full transport controls (Play, Pause, Skip Next, Skip Previous, Stop).
  - Synchronizes book title, author, and active chapter metadata to lockscreen, notification center, and connected remote devices (Bluetooth head units, watches).
  - Wires hardware media buttons (headset single-click play/pause toggle, next/previous skips) directly to reader speech playback.
- **TTS Audio Focus & Noisy Audio Management**:
  - Integrated `TtsAudioFocusManager` to handle system audio focus during text-to-speech reading.
  - Automatically ducks speech volume to 20% on transient notifications or navigation alerts (`AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`) and restores volume when focus returns.
  - Automatically pauses speech on transient audio focus loss (e.g., incoming phone calls or alarms) and resumes seamlessly on focus gain.
  - Added `AudioBecomingNoisyReceiver` to auto-pause speech playback when wired headphones or Bluetooth devices are disconnected, preventing unintended speaker audio.
- **Local-First Reading Goals**:
  - Added configurable daily reading duration targets (e.g., 20 mins/day) and book reading targets, stored privately on-device via `ReadingGoalStore`.
  - Added reading goal progress bars with percentage indicators, goal completion celebration badges, and an in-app goal editor (`EditReadingGoalsDialog`) directly inside the Reading Insights sheet.
  - 100% private and on-device with zero telemetry or network calls.
- **In-App Book Metadata Editor**:
  - Added in-app metadata editor directly in the book detail modal allowing users to edit book title, authors, series name, series index / volume number, publisher, and description.
  - Added `LibraryViewModel.updateBookMetadata` persisting updates to Room database with reactive UI updates across the library grid and reader.
- **Anki Flashcards & Readwise Export Formats**:
  - Added Anki TSV flashcard deck generation with HTML front/back cards, book tagging, and chapter attribution.
  - Added official Readwise-compatible CSV export format (`Highlight,Book Title,Book Author,URL,Note,Location,Date`) with RFC 4180 escaping.
  - Integrated Anki TSV and Readwise CSV choices into both single-book and batch export flows in `NotesExportSheet`.
- **OPDS Catalog Authentication & Search**:
  - Added HTTP Basic Auth and Bearer Token credential support for custom OPDS feeds (enabling integration with protected Calibre-Web, Kavita, and Audiobookshelf servers).
  - OpenSearch XML description document template resolution and search term substitution.
  - Authentication header propagation to feed requests, OpenSearch queries, cover image fetching, and direct EPUB downloads.
  - Visual lock badge in the catalog dropdown indicating authenticated feeds.
  - Expanded `AddOpdsFeedDialog` with authentication type selector and secure credentials input.
- **TTS Sleep Timer Fade-Out & Preset Expansion**:
  - Added 5-minute and 10-minute quick presets to the TTS sleep timer selection sheet.
  - Implemented gradual audio volume fade-out over the final 30 seconds before auto-pausing playback, providing a gentle audio transition into silence.
  - Added real-time visual indicator in the sleep timer bottom sheet and control chip displaying active fade-out state (`Fading MM:SS`).
  - Added automated volume restoration to 100% on manual pause/resume, playback stop, or sleep timer cancellation.

## [0.3.0] - 2026-11-15

### Added
- **Reader Footnote & Note Popup Preview Dialog**:
  - Non-disruptive bottom sheet preview for footnotes, endnotes, and internal note citations without losing reading position.
  - Robust footnote detection distinguishing note citations from backlinks, navigational links, and chapter headers.
  - Multi-pattern EPUB footnote content extraction supporting EPUB 3 semantic markups (`<aside epub:type="footnote">`, `role="doc-footnote"`), EPUB 2 anchor patterns (`<p id="...">`, `<a id="..."></a><p>...</p>`, `<li id="...">`), nested formatting, and entity unescaping.
  - Optional "Jump to note" navigation action for full-context inspection, seamlessly integrated with navigation jump history.
  - Configurable "Preview footnotes & notes" preference in Reader Settings, persisted across app sessions via `ReaderPreferencesStore`.
- **Book Ratings & Custom Tags**:
  - Interactive 1–5 star rating control in the Book Detail sheet with unrated/clear options.
  - Custom tag management in the Book Detail sheet with tag creation, deletion chips, and library suggestions.
  - Tag filter chip carousel in the Library view to isolate books by custom tags.
  - "Rating" sort order (`LibrarySort.RATING`) to sort library books from highest to lowest rated.
  - Visual rating indicators and tag badges on library book grid tiles.
  - Non-destructive Room migration (`MIGRATION_9_10`, version 10) preserving user ratings and tags during library re-scans and EPUB re-imports.
- **TTS Sleep Timer & Audio Navigation Controls**:
  - Sleep timer bottom sheet with configurable countdowns (15 min, 30 min, 45 min, 60 min) or End of Chapter auto-pause.
  - Active countdown indicator in the reader quick controls chip (`MM:SS` format) and within the sleep timer configuration sheet.
  - Audio navigation controls in reader quick controls with sentence/utterance skip backward (`Prev`) and skip forward (`Next`).
  - Chapter boundary detection for End of Chapter sleep timer ensuring playback automatically pauses when crossing chapter href boundaries.
  - Cancellation upon manual playback stop or natural end of publication.

## [0.2.0] - 2026-11-15

### Added
- **Advanced Book Search**: Comprehensive in-book search enhancements with search options, scoped chapter filtering, and persistent search history.
  - Search options chips: Match Case (case-sensitive), Whole Word matching, and Regular Expression (regex) with immediate syntax validation.
  - Scoped chapter filtering toggle to search the entire publication or restrict matches to the currently open chapter.
  - Search history store (`SearchHistoryStore`) persisting recent queries on-device with single-tap query replay, individual chip removal, and clear all.
  - Query clear button in search text input and debounced execution.
- **Reader Navigation History**: Non-sequential location history manager (`NavigationHistoryManager`) tracking jumps across table of contents, footnotes/internal hyperlinks, search results, bookmarks, and annotations.
  - History Back and Forward action controls in the reader top app bar.
  - Floating transient "Back to [source] • Return" return capsule at the bottom of the viewport with 12s auto-dismissal and guaranteed 48dp touch targets.
  - Predictive system back button integration to return to jump origin prior to reader exit.
- Reader text selection actions via custom `ActionMode.Callback` (`TextSelectionManager`):
  - **Highlight**: Creates an annotation with chosen or default tint.
  - **Copy**: Copies selected text to system clipboard.
  - **Define**: Offline-first dictionary lookup supporting ColorDict / GoldenDict (`colordict.intent.action.SEARCH`), Android 10+ system define (`android.intent.action.DEFINE`), and dictionary process text actions, with an explicit privacy-first DuckDuckGo web search fallback.
  - **Share**: Android system share sheet with book title context.
  - **Process Text Actions**: Dynamically registered system `ACTION_PROCESS_TEXT` tools (e.g. Translate, Wikipedia, AnkiDroid) integrated into overflow menu.
  - Package visibility `<queries>` in `AndroidManifest.xml` for `PROCESS_TEXT`, `colordict.intent.action.SEARCH`, and `ACTION_DEFINE` on Android 11+ (API 30+).


## [0.1.0] - 2026-10-18

### Added
- Reading Statistics & Insights dashboard tracking today's reading duration, weekly aggregates, session count, and a 7-day visual activity breakdown in a Material 3 sheet.
- Book detail reading activity metrics: total reading time, session count, and average session length.
- Estimated reading time remaining (`~X min left`) in the reader bottom HUD calculated from session reading speed.
- Highlight color filtering chips and notes & text search in the annotations bottom sheet.
- Text-to-speech (TTS) playback speed rate cycling control (`0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`).
- Library multi-selection mode with batch status updates (Unread, Reading, Finished), collection assignment, and deletion.
- Reader bottom HUD displaying reading progress percentage and visual progress indicator.
- Hardware volume rocker page turning (`Volume Down` for forward, `Volume Up` for backward) in paginated reading mode.
- System back navigation handler (`BackHandler`) in reader overlay to dismiss active HUD and menus before exiting the book.
- In-memory LRU bitmap cache (`BitmapMemoryCache`) for cover thumbnails with power-of-two downsampling and RGB_565 format.
- Optimistic UI updates when toggling reading status chips and collection filters in the library.

### Fixed
- Fixed page turning, horizontal swiping, and tap-zone gesture interception in `ReaderActivity` by wrapping the Compose overlay in a passthrough `ReaderOverlayTouchContainer` when reading HUD is hidden.
- Fixed reader toolbar dismissal by introducing a full-screen tap-to-dismiss background scrim when toolbars are visible.

### Changed
- Streamlined `LibraryScreen` layout to eliminate visual clutter and component overlap: consolidated TopAppBar into Search, Sort, and an overflow menu (MoreVert), removed floating folder button overlapping book cards, embedded headers/shelves directly in `LazyVerticalGrid` for unified scrolling, and added generous bottom padding so the grid clears the FAB.
- Centralized `ReadingStatus` as the single source of truth for display names.
- Optimized EPUB cover loading by reusing Readium parser components across requests.
- Generalized highlight undo handling to an open-closed `UndoCoordinator<T>` interface.
- Debounced library search inputs with 200ms reactive flows and unified collection observing via `flatMapLatest`.

## [0.0.1] - 2026-09-29

First public pre-release.

- EPUB 2 and EPUB 3 reading via the Readium Kotlin Toolkit, in paginated or
  continuous-scroll mode, with configurable tap zones, page-turn animation, and
  reduced-motion awareness.
- Publisher and custom typography: font stacks (including OpenDyslexic and
  Accessible DfA), line height, margins, themes, and per-book brightness.
- Text-to-speech reading mode with synchronized progression.
- On-device import of local EPUBs (single and bulk) plus saved-folder scanning
  and rescans, collections, reading-status flags, a continue-reading shelf,
  sorting, and search.
- Bookmarks, multi-color highlights with editable notes, full-text in-book
  search, and annotation export to Markdown.
- KOReader-compatible progress sync with background sync and conflict handling.
- Accessibility: TalkBack labels and announcements, dynamic type to 200%,
  48dp touch targets, WCAG AA contrast, and reduced-motion support.
- No analytics, crash reporting, or telemetry.

Requires Android 8.0 (API 26) or newer.
