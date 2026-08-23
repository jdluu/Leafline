# Leafline Development Roadmap

Status-based plan for the next phases of development. Each phase is a set of
independent slices; slices are ordered by user impact within each phase.

## Phase 1: Reading experience polish (highest impact)

The reader works; now make it feel premium.

1. **Reading settings live preview**: font family choices beyond default
   (Readium supports selectable font stacks), adjustable margins, publisher
   vs custom styles toggle.
2. **Tap zone customization**: let users configure left/right/center tap
   actions (page turn direction, menu toggle).
3. **Page-turn animation options**: none / slide / fade; respect the
   system "remove animations" accessibility setting.
4. **Scroll mode**: continuous scroll as an alternative to pagination,
   persisted per book or globally.
5. **Brightness/sepia quick controls**: reader overlay slider plus a warm
   background theme variant for night reading.

## Phase 2: Accessibility (a11y)

1. **TalkBack audit**: every interactive element in library, reader overlay,
   sheets, and dialogs needs a content description and correct semantics;
   fix focus order and announce page changes in the reader.
2. **Dynamic type**: honor system font scale up to 200% in all Compose text;
   verify sheets and dialogs reflow instead of clipping.
3. **Touch targets**: audit all icon buttons for the 48dp minimum; the
   reader top bar is the likely offender.
4. **Color contrast**: verify all theme color pairs meet WCAG AA (4.5:1
   body, 3:1 large/UI); especially highlight tints over light themes.
5. **Reduce motion**: gate nonessential animations behind a setting that
   defaults to the OS preference.

## Phase 3: Design system and theming

1. **Extract a Leafline design system**: centralize colors, typography,
   spacing, and shapes into a theme package instead of inline values.
2. **Color palette refresh**: define a full Material 3 scheme with light,
   dark, and pure-black (OLED) variants; add an e-ink friendly high-contrast
   theme; consider dynamic color (Material You) on Android 12+.
3. **Highlight tint palette**: curated swatch row when creating a highlight
   (yellow/green/blue/pink) stored per annotation.
4. **App icon and splash screen**: proper adaptive icon and a
   SplashScreen API integration.

## Phase 4: Library power features

1. **Collections/tags**: user-defined groupings with a filter chip row on
   the library grid.
2. **Reading status**: unread/reading/finished flag per book with filter;
   auto-suggest finished when progress passes ~98%.
3. **Continue reading shelf**: smart row at the top of the library sorted by
   last-read time (data already exists).
4. **Bulk import**: multi-select EPUB import from SAF with per-file error
   reporting.
5. **Book detail sheet**: long-press a cover for metadata, file info, sync
   state, and per-book actions.

## Phase 5: Sync hardening

1. **End-to-end verification against live Grimmory** with real KOReader
   credentials, including cross-device round-trip (KOReader device pushes,
   Leafline pulls).
2. **KOReader hash compatibility check**: validate `koreaderHash` against
   real KOReader's exponential-sampling partial MD5; switch if they differ
   (single-function change).
3. **Background sync**: periodic WorkManager job pushing positions for
   recently-read books even if the reader was killed.
4. **Sync conflict UX**: replace the jump dialog with a small bottom sheet
   showing local vs remote percentage and timestamps.

## Phase 6: Performance and quality

1. **Library grid performance**: benchmark 500+ book imports; ensure cover
   decoding happens off the main thread and thumbnails are bounded in memory.
2. **Startup time**: measure cold start; lazy-initialize OPDS/sync
   dependencies only when first used.
3. **Baseline profiles**: add a baseline profile for reader open and page
   turn paths.
4. **Screenshot/integration tests**: instrumented tests for the reader flow
   (open, navigate, search, highlight) so refactors stay safe.
5. **Crash/error reporting decision**: pick a self-hosted or no-telemetry
   approach before any public release.

## Phase 7: Distribution

1. **License selection** (blocking any public release).
2. **Min/target SDK pinning** after testing target e-ink devices.
3. **Release signing and F-Droid/Acres-friendly build recipe**; keep
   dependencies free of proprietary blockers.
4. **Changelog and versioning scheme**.

## Suggested order

Phase 1 and Phase 2 together form the next natural milestone ("make the
reader feel great and usable by everyone"), followed by Phase 3's palette
work which builds on the a11y contrast audit. Phases 4-5 can interleave
based on interest; Phase 6-7 matter most before sharing the APK.
