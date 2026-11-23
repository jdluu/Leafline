# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Three audiences, in a confirmed precedence order that resolves design
tradeoffs:

1. **The Accessibility-First Reader** wins ties. Relies on large type, high
   contrast, visible focus, dependable touch targets, and predictable
   navigation. Accessibility is a hard requirement, not a preference. Uses
   e-ink and OLED display modes seriously.
2. **The Deep Reader.** Reads in long uninterrupted sessions and wants the
   chrome to disappear. Values comfortable light, sepia, and dark themes,
   adjustable typography and margins, and low distraction.
3. **The Privacy-Minded Library Owner.** Keeps books and reading state
   on-device by choice. Reacts strongly and negatively to telemetry, cloud
   lock-in, and decorated chrome.

Privacy is a hard constraint on every decision rather than a design driver:
nothing may ship that collects data, and no surface may imply a network
dependency it does not have.

## Product Purpose

Leafline is a free, open-source, local-first EPUB reader for Android 8.0
(API 26) and newer. It reads EPUB 2 and EPUB 3, manages an on-device library,
and keeps reading, annotations, statistics, and progress entirely on the
device with no account. Optional connectivity is limited to servers the user
configures: OPDS catalogs, KOReader-compatible progress sync, and, when the
reader turns it on, pushing progress to their own Hardcover account. Every one
of those is off until configured, and each credential is encrypted on device
with a key that never leaves it.

Success means a reader opens a book and stays in it, and a library owner never
has to think about where their data lives.

## Positioning

A reader-grade EPUB app that treats typography and privacy as the product
rather than as features bolted onto a file browser: real reading typography
and accessibility presets (including OpenDyslexic and Accessible DfA),
on-device library and insights, no telemetry of any kind, and self-hosted sync
through any KOReader-compatible server. The claim a neighboring reader could
not truthfully copy is the combination of no-account local-first operation,
reader-grade typographic control, and accessible-by-default surfaces.

## Operating Context

- Single maintainer, open-source repository (`jdluu/Leafline`), MIT licensed.
- Distribution is sideloaded: Obtainium from GitHub releases, or a manual APK.
  There is no Play Store listing, so no store assets or review flow exist.
- Release line is early pre-release (`0.y.z`, current `v0.4.0`). The `0.y.z`
  line signals that the on-device Room schema is not yet stable.
- The app ships to phones; tablets are not a stated target.
- Development is on a Pixel-class device over ADB, plus an emulator for width
  coverage. CI runs build, unit tests, lint, and quality guard scripts.
- Nothing in the product can rely on usage data: no analytics or crash
  reporting is collected, so design decisions are argued from heuristics,
  device inspection, and user reports, never from funnels.

## Capabilities and Constraints

Confirmed capabilities: EPUB 2/3 reading in paginated and scrolled modes;
footnote and note previews; navigation history with a return capsule; on-device
library with folders, collections, reading status, ratings, tags, sorting,
grid and list layouts, and batch actions; OPDS 1.x/2.0 browsing and download;
KOReader-compatible progress sync with encrypted stored credentials;
on-device reading statistics and a 7-day chart; multi-color
highlights, notes, bookmarks, offline dictionary lookup, and full-text in-book
search; notes and highlights export to Markdown, Obsidian vault ZIP, Readwise
CSV, and Anki TSV; JSON backup and restore of library metadata and
annotations; typography and accessibility presets, themes including sepia,
margins, line height, per-book brightness, edge tap zones, volume-key page
turning, and page-turn animation; text-to-speech with a sleep timer.

Technical constraints that bind any design work:

- Jetpack Compose with Material Design 3. Material governs structure,
  navigation, and interaction; brand expresses through theming only.
- Text sizes use `MaterialTheme.typography` in sp. A fixed dp or px text size
  fails `scripts/check_dynamic_type.sh`.
- Every interactive control is at least 48dp; `scripts/check_touch_targets.sh`
  fails the build otherwise.
- One container radius (12dp) and an 8dp spacing rhythm, per DESIGN.md.
- Reader-scoped controls (typography, margins, brightness, page turning) live
  in the reader overlay, not in app settings.
- No new dependencies without explicit approval; AGP, Gradle, Kotlin, KSP,
  Compose, and Readium are a coupled compatibility set.
- Persisted preference keys, enum names, and database columns are stable
  storage keys and must not be renamed.

## Brand Commitments

- Name and identity: Leafline, from the leaf line — the spine of a leaf is the
  line of ink being read. The identity sits at that intersection; the wordmark
  pairs a small leaf glyph with the name in the accent tone.
- Voice: plain, quiet, specific. No marketing pressure, no exclamation, no
  cleverness in error messages or empty states.
- One accent family only: deep green-teal (`#315C52` light, `#9CCCC0` dark) on
  neutral paper surfaces (`#F9F8F4` light, `#191C1A` dark). The paper-and-green
  pairing is locked. No second accent, no gradients, no glows, no colored
  shadows.
- The app should feel like a well-made notebook, not a dashboard. Nothing
  glows, nothing floats, nothing bounces.
- `DESIGN.md` is the authoritative visual contract; it is binding on new work.

## Evidence on Hand

- Two product screenshots committed at `screenshots/library.png` and
  `screenshots/reader.png` (phone, light theme). They are the only visual
  evidence in the repository and go stale when the UI changes.
- `DESIGN.md` holds the token tables, type roles, shape, spacing, motion, and
  component guidance. `AGENTS.md` holds build, test, and quality-gate commands.
- Real EPUB fixtures live in `app/src/androidTest/assets/benchmark-books`.
- Absent and must not be fabricated: user counts, install numbers, ratings,
  testimonials, press coverage, performance benchmarks, or roadmap dates.

## Product Principles

1. The page is the product. Every surface either serves reading or gets out of
   the way; chrome never competes with text.
2. Accessibility is the floor, not a mode. Large type, contrast, touch targets,
   and focus behave correctly before any aesthetic consideration.
3. Nothing leaves the device. A feature that requires trust it has not earned
   is not worth shipping.
4. Quiet over expressive. Hierarchy comes from weight, size, and tone, not from
   decoration or motion.
5. Say the true state. Loading, empty, error, and disabled states are designed
   and never implied by a misleading blank surface.
6. Statistics inform, they do not nag. Reading history is shown as fact, never as
   a run to protect: no streaks, no broken-chain warnings, no nudges to keep a
   number alive. Someone who reads ten pages in a month should not be made to feel
   behind.

## Accessibility & Inclusion

- WCAG AA contrast at minimum: 4.5:1 for body text, 3:1 for large text and UI
  components.
- All interface type in sp, reflowing rather than clipping when the system
  font scale grows.
- 48dp minimum touch targets with at least 8dp between adjacent targets.
- Accessible labels or content descriptions on every interactive element;
  decorative icons hidden from screen readers deliberately.
- Visible focus at all times; never remove focus styling.
- Meaning is never carried by color alone: selection, status, and error pair
  color with an icon, a label, or both.
- Reduced-motion and animation-scale preferences are honored; e-ink and OLED
  display modes are treated as first-class schemes.
