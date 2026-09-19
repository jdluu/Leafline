---
name: native-android-design-systems
description: Use when integrating a brand into native Android Compose UI.
version: 1.0.0
license: MIT
category: software-development
metadata:
  hermes:
    tags: [android, compose, material3, design-system, accessibility, visual-verification]
---

# Native Android Design Systems

Use when a native Android app has a brand contract or needs its visual identity integrated across real Compose surfaces. The goal is an implemented, testable system, not a palette document or a library-only polish pass.

## Workflow

1. Research the actual audience and product context before choosing visual direction. For a local-first reader, prioritize calm hierarchy, low distraction, adjustable typography, comfortable themes, offline trust, annotation clarity, and accessibility.
2. Audit the app before editing: theme entry points, reader/content theme entry points, persisted preferences, primary screens, screenshots, token files, and accessibility checks.
3. Write or update a design contract before implementation. Include audience, emotional qualities, direction, light and dark color roles, typography, shape scale, spacing rhythm, motion, accessibility invariants, component guidance, and anti-patterns.
4. Implement semantic tokens first. Use one Material 3 system throughout the app shell. Apply tokens to library, settings, navigation, dialogs, sheets, and reader overlays. Replace repeated primary-surface literals with shared spacing and shape tokens in focused edits.
5. Keep product-shell appearance separate from EPUB rendering appearance. The shell owns Material 3 colors, typography, shapes, and app-level appearance. Readium owns book theme, font, margins, and publication rendering.
6. Decide explicitly how system appearance interacts with branding. If brand consistency matters, System follows OS light/dark state while retaining the fixed product palette. Dynamic wallpaper colors must be an explicit option, not an accidental replacement.
7. Persist app appearance independently from per-book reader preferences. Support explicit Light, Dark, OLED, and E-ink modes when appropriate. Unknown stored values fall back safely to System.
8. Expose appearance choice in Settings and apply it immediately. Use clear labels and descriptions, preserve existing navigation, and keep interactive controls at least 48dp.

## Research anchors

- Material guidance recommends at least 48dp touch targets for interactive controls.
- Interactive Compose Material components provide minimum target behavior, while decorative glyphs inside them are not controls.
- Android architecture guidance favors testable layers, lifecycle-aware state collection, and clear separation between UI state and data sources.
- Compose accessibility guidance recommends custom accessibility actions for gestures such as swipe-to-dismiss.

## Testing and verification

For each design-system change:

- Add JVM tests for appearance persistence, default and unknown-value fallback, theme-mode resolution, token helpers, and pure state transitions.
- Run the full applicable unit suite, lint, debug build, dynamic-type guard, touch-target guard, and diff check.
- Install the resulting APK on a connected Android device. Verify library and settings surfaces, appearance selection, back navigation, and the relevant reader path.
- For reader changes, exercise forward and backward page turns, tap-zone actions, horizontal swipe navigation, and changed annotation or overlay flows. Check accessibility labels and logs for fatal application crashes.
- For screenshots, capture one current library/home state and one meaningful reader state with a real EPUB open. Validate the assets as PNG files and verify dimensions. Replace existing README asset paths rather than adding stale parallel images.
- Report manual device interaction honestly. Accessibility-tree evidence and automated tests support claims, but are not a substitute for visual inspection when visual inspection was not performed.

## Common failure modes

- A brand contract exists but only the library uses it. Audit all shell surfaces, not just the first screen.
- System mode silently enables dynamic wallpaper colors and makes screenshots vary by device. Decide and test fixed-versus-dynamic behavior explicitly.
- App-shell theme and book-content theme are conflated. Keep preferences and rendering boundaries separate.
- A compact icon is mistaken for a too-small interactive control. Verify the enclosing Material component or touch bounds before changing the glyph.
- Screenshots are copied from stale output or captured from a picker, launcher, or wrong activity. Verify the top activity, accessibility markers, and actual EPUB heading before saving reader screenshots.
- A swipe affordance has no accessible alternative. Add a visible action or custom accessibility action and test both paths.
- A screenshot or device run is reported as complete from subprocess narration alone. Re-check the file, device state, test result, and logs independently.

## Deliverables checklist

- [ ] Design contract exists and is user-safe.
- [ ] Semantic Material 3 tokens are used across primary app-shell surfaces.
- [ ] App appearance persistence is implemented and tested.
- [ ] EPUB rendering preferences remain separate.
- [ ] Accessibility labels and 48dp target rules are preserved.
- [ ] Unit tests, lint, build, and guards pass.
- [ ] Connected-device verification passes with real counts.
- [ ] README screenshots are current, valid PNGs with verified dimensions.
- [ ] Git diff is reviewed, public text is sanitized, and generated/local files are excluded.
