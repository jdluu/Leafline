# Leafline Design Contract

Leafline is a local-first EPUB reader for Android. It keeps books, highlights,
and reading progress entirely on the device, it collects no telemetry, and it
treats comfortable reading as the product. This document is the single source
of truth for how the app looks and moves. It is written for people who build
or review Leafline UI. Always follow this contract before inventing new styles.

## Design read

Leafline takes its name from a leaf line: the spine of a leaf is also the line
of ink being read. The identity sits at that intersection. One quiet
green-teal accent on neutral paper surfaces, strong readable type, and
motion reserved for state changes and feedback.

Reading this as: a calm, content-first reading app for privacy-minded and
accessibility-conscious readers, with a natural and tactile language, built on
the Material 3 design system for Android.

The app should feel like a well-made notebook, not a dashboard. Nothing glows,
nothing floats, nothing bounces. Chrome stays out of the way of the page.

## Audience archetypes

- The Deep Reader. Reads long sessions without interruption and wants the UI
  to disappear. Values comfortable light, sepia, and dark themes, adjustable
  typography and margins, and low distraction.
- The Privacy-Minded Library Owner. Keeps books and reading state on-device by
  choice. Reactions to telemetry, cloud lock-in, and decorated chrome are
  strongly negative. Wants an app that reads as trustworthy and undemanding.
- The Accessibility-First Reader. Relies on large type, high contrast, visible
  focus, dependable touch targets, and predictable navigation. Accessibility
  is a hard requirement, not a preference. E-ink and OLED display modes are
  used seriously.

## Emotion words

- Calm. Quiet surfaces, even rhythm, no visual noise.
- Grounded. Tonal greens and paper neutrals root the app in the natural.
- Clear. Strong hierarchy, concise labels, obvious next actions.

## Design dials

- Variance: low. One accent color, restrained iconography, consistent corners.
  Symmetry and regularity are deliberate. Nothing decorative that carries no
  meaning.
- Motion: low. Animations only mark a state change or confirm an action. No
  looping, no flourish, no decorative entrance. Everything honors the reduced
  motion preference and the user's animation scale setting; when animation is
  disabled the app changes state instantly.
- Density: comfortable. Generous spacing between reading surfaces, readable
  line lengths, and breathing room around actions. The app never crams
  controls or relies on tiny touch targets.

## Color

One accent family: deep green-teal. It is the only brand color. Neutral
surfaces are pulled toward the accent, never a generic purple or gray-blue
wash.

### Light scheme

| Role | Token | Hex |
|---|---|---|
| Primary | accent green-teal | `#315C52` |
| On primary | | `#FFFFFF` |
| Primary container | soft leaf tint | `#B7DFD2` |
| On primary container | | `#002019` |
| Secondary | quieter green-teal | `#4C635B` |
| On secondary | | `#FFFFFF` |
| Secondary container | | `#CFE9DE` |
| On secondary container | | `#09201A` |
| Tertiary | reserved, calm cool | `#3D6473` |
| On tertiary | | `#FFFFFF` |
| Tertiary container | | `#C1E8FA` |
| On tertiary container | | `#001F29` |
| Error | | `#BA1A1A` |
| On error | | `#FFFFFF` |
| Error container | | `#FFDAD6` |
| On error container | | `#410002` |
| Background | paper | `#F9F8F4` |
| On background | ink | `#191C1A` |
| Surface | paper | `#F9F8F4` |
| On surface | ink | `#191C1A` |
| Surface variant | mist | `#DCE5DE` |
| On surface variant | | `#414944` |
| Outline | | `#717974` |
| Outline variant | | `#C0C9C3` |

### Dark scheme

| Role | Token | Hex |
|---|---|---|
| Primary | pale leaf | `#9CCCC0` |
| On primary | | `#00382D` |
| Primary container | deep leaf | `#195146` |
| On primary container | | `#B7DFD2` |
| Secondary | | `#B4CCC2` |
| On secondary | | `#1F352E` |
| Secondary container | | `#354C44` |
| On secondary container | | `#CFE9DE` |
| Tertiary | | `#A1CDDD` |
| On tertiary | | `#023544` |
| Tertiary container | | `#224C5B` |
| On tertiary container | | `#C1E8FA` |
| Error | | `#FFB4AB` |
| On error | | `#690005` |
| Error container | | `#93000A` |
| On error container | | `#FFDAD6` |
| Background | ink | `#191C1A` |
| On background | paper | `#E1E3DF` |
| Surface | ink | `#191C1A` |
| On surface | paper | `#E1E3DF` |
| Surface variant | | `#414944` |
| On surface variant | | `#C0C9C3` |
| Outline | | `#8B938D` |
| Outline variant | | `#414944` |

### Special-purpose variants

- OLED. Identical to dark, but background and surface are true black
  (`#000000`) so panel pixels are off. Keeps text and accent tones from the
  dark scheme for readability and battery life.
- E-ink. Monochrome and high contrast, black text on white surfaces, accent
  rendered as dark gray. Designed for displays that can only render
  grayscale well.

### Color rules

- Use primary for actions, selection, and focus. Use surface and on-surface
  for the bulk of interface text. Use on-surface-variant for secondary text
  and captions.
- Do not introduce a second accent color. Tertiary exists only where the
  component system requires a third role and must stay desaturated.
- Do not use gradients, glows, or shadows with color. Elevation is implied by
  surface tone, not by decoration.
- Never express meaning through color alone. Selection state, errors, and
  reading status always pair color with an icon, a label, or both.

## Typography

The app UI uses the standard system sans-serif family at every size, all in sp
units so text scales with the system font setting. Defaults in code must never
be overridden to dp or px sizes.

- Display and headlines. SemiBold weight, used only for first-impression
  moments such as the empty library heading. Never for running interface
  chrome.
- Titles. Medium weight, used for screen titles, section headings, and book
  titles.
- Body. Regular weight, generous line height. Primary interface text.
- Labels. Medium weight and concise. Buttons and chips never read as
  sentences; keep to two or three words.
- Book content is the reader's choice. The app offers adjustable typography
  (families, size, line height, margins) and must not impose a house style on
  the pages being read. Typography rules in this section govern the app
  chrome only.
- Hierarchy comes from weight and size, not color alone. When text must be
  secondary, prefer on-surface-variant on paper over reduced size.

## Shape

One container radius for the whole app: 12dp. Cards, sheets, dialogs,
surfaces, and inputs share it.

- Pill shape is reserved for compact controls and single actions such as
  chips. Do not give large containers a pill or a wildly different radius.
- Do not mix radius families. A 4dp card next to a 16dp card is a defect, as
  is a square tile on a rounded page.
- Corners never carry decoration such as borders or outlines as a rule;
  a hairline divider or a tinted tone is preferred for separation.

## Spacing

The spacing rhythm is 8dp. Small gaps are 8dp, standard gaps are 16dp, larger
breaks are 24dp and 32dp, and section-level air is 48dp and 64dp. The rhythm
governs padding, gaps between grid items, and vertical breathing room.
Half-steps (4dp and 12dp) may appear only where a compact inset is required by
an exact control measurement, never as the default.

## Motion

- Animate only state transitions and feedback: a sheet opening, a chip being
  selected, a progress indicator, a page turn. Ask what the motion
  communicates before adding it. "It looks alive" is not a valid answer.
- Animated page transitions may be offered as a reader setting, but the
  default reading experience must not depend on motion.
- Honor the system reduced motion preference and the animation scale setting.
  When the user disables animation, every transition snaps instantly.
- No infinite loops, parallax, floating elements, or decorative entrances.

## Accessibility invariants

These are non-negotiable, same as the quality bar for the app:

- Every interactive control has a touch target of at least 48dp. Icon buttons
  that look small still keep the 48dp minimum around their visual icon.
- Interface text meets WCAG AA contrast: at minimum 4.5:1 for body text and
  3:1 for large text and UI components. Contexts that deviate from the token
  pairs are not allowed to ship without a measured ratio.
- All App text sizes are in sp so the system font scale applies. Layouts reflow
  instead of clipping when type grows.
- Interactive elements carry accessible labels or content descriptions.
  Decorative icons are hidden from screen readers on purpose.
- Focus is always visible. Never remove focus styling.

## Component guidance

- Top app bars. One title, one product identity mark, and only actions that
  are needed on that screen. The wordmark pairs a small leaf glyph with the
  Leafline name in the accent tone.
- Buttons. The primary action on a screen uses the filled accent style and is
  the only emphasized action on screen. Text and tonal buttons sit below it
  in the hierarchy. Labels are concise.
- Chips. Used for filters and single-choice selection. A selected chip uses
  the accent tone with readable text inside. Chips carry at least 48dp of
  touch target even when compact.
- Cards. Cover and metadata tiles use the 12dp surface radius with the paper
  tone. Never render a card purely for decoration; elevation must mean
  grouping or an action.
- Sheets and dialogs. Modal surfaces rest on the paper container tone. Keep
  the title heading consistent, space the content on the 8dp rhythm, and
  scroll content instead of clipping it when type sizes grow.
- Empty states. A first-run library, an empty collection, and a search with
  no results are designed moments, not afterthoughts. Each uses the leaf
  mark on the container tone with a short heading, a one-line explanation,
  and a single clear action where one exists.
- Loading states. While content is being read a progress indicator is shown
  in the accent tone with an accessible label. Loading is never represented
  by a misleading empty result.
- Floating action buttons. Reserved for the primary content action of a
  screen, always reachable without scroll, never stacked or duplicated in
  view. The label, where shown, is the full action name.

## Anti-patterns

- A second accent color, a brand gradient, or a purple-heavy glow.
- Warm beige-and-brass or espresso "premium craft" palettes; the paper tone
  stays quiet and neutral and the accent is green-teal, and that pairing is
  locked.
- Decorative clutter: floating shapes, background illustrations, emoji as
  icons, drop shadows with color, borders on every element.
- Motion that loops or exists only to look alive.
- Text expressed in fixed pixel or dp sizes that ignore the font scale, or
  layouts that clip instead of reflowing.
- Interactive elements smaller than 48dp or icons whose meaning relies on
  color alone.
- Long labels on primary actions, or two controls with the same action in one
  view.
- Newsletter-signup-style visual pressure inside an empty state. The library
  invites, it does not sell.