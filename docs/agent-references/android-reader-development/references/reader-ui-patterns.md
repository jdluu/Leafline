# Reader UI Patterns: Compose Overlay, TOC, Preferences, Navigation

Verified patterns for building a reader UI on top of Readium's fragment-based
EPUB navigator (AGP 9.3 / Kotlin 2.2.20 / Readium 3.3.0 / Pixel 7 API 37).

## Compose overlay on a fragment-based navigator

Readium's `EpubNavigatorFragment` is a Fragment, not a Composable. To add
Compose UI controls (toolbar, tap zones, drawers) on top of it without
rewriting the navigator, use `addContentView` with a `ComposeView`:

```kotlin
private fun addReaderOverlay() {
    val composeView = ComposeView(this).apply {
        setContent {
            ReaderOverlay(
                title = bookTitle.value,
                toolbarVisible = toolbarVisible.value,
                onToggleToolbar = { toolbarVisible.value = !toolbarVisible.value },
                onBack = { finish() }
            )
        }
    }
    val layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    addContentView(composeView, layoutParams)
}
```

The overlay sits on top of the fragment container. State is held in
`mutableStateOf` fields on the Activity so Compose recomposes when they change.

## Tap zones for page navigation and toolbar toggle

A full-screen invisible `Box` with `clickable` handles the center tap to
toggle the toolbar. Readium's own WebView handles left/right swipe for page
navigation, so the overlay should NOT intercept swipes — only taps.

```kotlin
@Composable
private fun InvisibleTapZone(onTap: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().clickable(onClick = onTap)
    )
}
```

When the toolbar is visible, it appears as a `Surface` with `TopAppBar` at the
top with `statusBarsPadding()`. The surface uses `copy(alpha = 0.95f)` for a
semi-transparent look that doesn't fully obscure content.

## Table of Contents drawer

Readium exposes the TOC as `publication.tableOfContents: List<Link>`. Each
`Link` has:
- `getTitle(): String?` — chapter title (nullable)
- `getHref(): Href` — chapter URL
- `getChildren(): List<Link>` — nested sub-chapters

Flatten the tree into a list of `(Link, depth)` pairs for display:

```kotlin
private fun flattenToc(links: List<Link>, depth: Int = 0): List<Pair<Link, Int>> {
    val result = mutableListOf<Pair<Link, Int>>()
    for (link in links) {
        result.add(link to depth)
        if (link.children.isNotEmpty()) {
            result.addAll(flattenToc(link.children, depth + 1))
        }
    }
    return result
}
```

Use `ModalNavigationDrawer` with `rememberDrawerState` for the drawer UI.
Indent items by depth: `(depth * 16).dp` padding on the start.

Navigation to a chapter uses `navigator.go(link, false)`:

```kotlin
navigator?.go(link, animated = false)
```

Close the drawer after navigation and hide the toolbar for a clean reading view.

## Reader preferences (font size, theme, font family)

Readium 3.3.0 preferences API (experimental, `@OptIn(ExperimentalReadiumApi)`):

```kotlin
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme

// Submit preferences to the navigator
val prefs = EpubPreferences(
    fontFamily = FontFamily.SERIF,
    fontSize = 2.0,           // 1.0 = default, 2.0 = double
    theme = Theme.DARK,
    publisherStyles = false   // required for advanced prefs
)
navigator.submitPreferences(prefs)
```

Available reflowable EPUB preferences (verified from Readium docs):
- `fontSize: Double?` — scale factor (1.0 = default)
- `fontFamily: FontFamily?` — SERIF, SANS_SERIF, OPEN_DYSLEXIC, etc.
- `theme: Theme?` — AUTO, LIGHT, DARK, SEPIA
- `lineHeight: Double?`
- `textAlign: TextAlign?`
- `scroll: Boolean?` — scroll vs paginated
- `publisherStyles: Boolean?` — must be false for advanced prefs
- `pageMargins: Double?`
- `hyphens`, `letterSpacing`, `wordSpacing`, `paragraphIndent`, `paragraphSpacing`
  — require `publisherStyles = false`

The `EpubNavigatorFactory` constructor accepts initial preferences via
`createFragmentFactory(initialPreferences = ...)`.

For a simple font-size stepper: increment by 0.25 per tap.
`navigator.submitPreferences(currentPrefs.copy(fontSize = newSize))`.

## Bottom navigation bar (Material 3)

Replace single-screen navigation with a `Scaffold` + `NavigationBar`:

```kotlin
Scaffold(
    bottomBar = {
        NavigationBar {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    icon = { Icon(Icons.Default.LibraryBooks, ...) },
                    label = { Text(tab.label) }
                )
            }
        }
    }
) { padding ->
    Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
        when (selectedTab) {
            0 -> LibraryTab(...)
            1 -> CatalogTab(...)
            2 -> SettingsTab(...)
        }
    }
}
```

Use `mutableIntStateOf` for the selected tab index.

## material-icons-extended dependency

The core `material3` artifact includes only a small set of icons
(`Icons.Default.Add`, `Icons.Default.Settings`, `Icons.Default.Book`).
Extended icons like `LibraryBooks`, `CloudDownload`, `Menu`, `ArrowBack`,
`TextFields` require:

```kotlin
implementation("androidx.compose.material:material-icons-core")
implementation("androidx.compose.material:material-icons-extended")
```

These resolve through the Compose BOM — no version needed if the BOM is
already applied.

## File path mismatch: /data/user/0 vs /data/data

When a book is downloaded via OPDS, `context.filesDir` resolves to
`/data/user/0/com.example/files/` on some Android versions. The stored
`filePath` in Room uses this path. However, `File.canonicalPath` may resolve
the `/data/user/0/...` symlink to `/data/data/com.example/...`, causing
string-comparison lookups to fail silently.

**Fix:** Use `File.absolutePath` (not `canonicalPath`) for DB lookups when
the stored path came from `filesDir`. Use `canonicalPath` only for the
security check (verifying the file is within the app's data directory).

## Verified API signatures (javap on readium-navigator-3.3.0-api.jar)

- `EpubNavigatorFragment.go(Link, Boolean): Boolean` — navigate to a TOC link
- `EpubNavigatorFragment.goForward(Boolean): Boolean`
- `EpubNavigatorFragment.goBackward(Boolean): Boolean`
- `EpubNavigatorFragment.currentLocator: StateFlow<Locator>` — Kotlin property
- `EpubNavigatorFragment.submitPreferences(EpubPreferences)` — applies live
- `Publication.getTableOfContents(): List<Link>`
- `Publication.getReadingOrder(): List<Link>`
- `Link.getTitle(): String?`
- `Link.getHref(): Href`
- `Link.getChildren(): List<Link>`
- `Publication.getMetadata().getTitle(): String?` — nullable, handle with `?:`
