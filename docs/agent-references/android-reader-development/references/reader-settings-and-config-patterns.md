# Reader Settings and Navigation Patterns

Verified patterns for reader settings UI, OPDS config in Settings tab, and
debug config injection. Complements `references/reader-ui-patterns.md`.
All items verified live on Pixel 7 (API 37) with Readium 3.3.0.

## Reader settings bottom sheet (ModalBottomSheet)

A `ModalBottomSheet` with `FilterChip` rows for theme/font and steppers for
line height provides a complete reader settings UI. Verified live on Pixel 7
with dark theme applied to reflowable text pages.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    preferences: EpubPreferences,
    onPreferencesChange: (EpubPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            // Theme: Light / Sepia / Dark as FilterChips
            listOf(Theme.LIGHT to "Light", Theme.SEPIA to "Sepia", Theme.DARK to "Dark")
                .forEach { (theme, label) ->
                    FilterChip(
                        selected = preferences.theme == theme,
                        onClick = { onPreferencesChange(preferences.copy(theme = theme)) },
                        label = { Text(label) }
                    )
                }
            // Font family: null (Original) / SERIF / SANS_SERIF
            // Line height: stepper with -/+ IconButtons, range 1.0 to 2.5, step 0.2
            // Publisher styles: Switch (checked = true by default)
        }
    }
}
```

State is held as `mutableStateOf(false)` for sheet visibility on the Activity.
The settings icon in the TopAppBar sets it to `true`; the sheet's
`onDismissRequest` sets it to `false`.

**Important:** Cover pages in EPUBs are fixed-layout (images). Theme changes
(dark/sepia) do NOT affect cover pages -- they only affect reflowable text
pages. When testing theme changes on device, navigate past the cover to a
text chapter via the TOC drawer before screenshotting.

## Settings tab with OPDS config form

Move OPDS server configuration into a dedicated Settings tab with
`OutlinedTextField` fields for URL, username, and password. Save writes to
`OpdsConfigStore.config` (in-memory singleton).

```kotlin
@Composable
fun SettingsTab() {
    var url by remember { mutableStateOf(OpdsConfigStore.config?.catalogUrl ?: "") }
    var username by remember { mutableStateOf(OpdsConfigStore.config?.username ?: "") }
    var password by remember { mutableStateOf(OpdsConfigStore.config?.password ?: "") }
    var saved by remember { mutableStateOf(false) }
    // OutlinedTextFields + Save button that sets OpdsConfigStore.config
}
```

### Auto-load catalog when config exists

The Catalog tab's `OpdsViewModel` starts in `Idle` state. If config is already
set (from Settings tab or intent extras), the Catalog should auto-load the
root feed instead of showing the config form again.

Add an `init` block and a `loadIfConfigured()` method to the ViewModel:

```kotlin
class OpdsViewModel : ViewModel() {
    init {
        config?.let { loadFeed(it) }  // auto-load if config exists at creation
    }

    fun loadIfConfigured() {
        if (_uiState.value is OpdsUiState.Idle) {
            config?.let { loadFeed(it) }
        }
    }
}
```

Call `loadIfConfigured()` from a `LaunchedEffect` when the Catalog tab becomes
visible, so config saved in Settings after ViewModel creation still triggers
a load:

```kotlin
LeaflineTab.Catalog -> {
    LaunchedEffect(selectedTab) {
        opdsViewModel.loadIfConfigured()
    }
    OpdsBrowseScreen(...)
}
```

## Debug config injection via intent extras

For automated device testing, Compose `TextField` focus is unreliable with
`adb shell input text`. Beyond the BroadcastReceiver approach (see
`references/live-opds-locators-and-git-rewrite.md`), a simpler method is
passing config via intent extras on launch:

```bash
adb shell am start -n com.example/.MainActivity \
  --es opds_url "https://server/api/v1/opds" \
  --es opds_username "user" \
  --es opds_password "pass"
```

Handle in `onCreate`:

```kotlin
private fun handleOpdsConfigIntent(intent: Intent?) {
    val url = intent?.getStringExtra("opds_url") ?: return
    val username = intent.getStringExtra("opds_username") ?: ""
    val password = intent.getStringExtra("opds_password") ?: ""
    OpdsConfigStore.config = OpdsServerConfig(url, username, password)
}
```

This is more reliable than `adb shell input text` into Compose fields and
simpler than registering a BroadcastReceiver. It does not survive process
death (the intent is consumed on first `onCreate`), but for test automation
that re-launches the app each time, this is sufficient.

## Theme enum values (Readium 3.3.0)

From `javap` on `readium-navigator-3.3.0-api.jar`:

- `Theme.LIGHT` -- light background, dark text
- `Theme.DARK` -- dark background, light text
- `Theme.SEPIA` -- sepia-tinted background

Note: there is no `Theme.AUTO` in the enum despite some docs mentioning it.
The navigator follows the system theme by default when `theme` is null.

## FontFamily companion constants (Readium 3.3.0)

From `javap` on `FontFamily`:

- `FontFamily.SERIF`
- `FontFamily.SANS_SERIF`
- `FontFamily.CURSIVE`
- `FontFamily.FANTASY`
- `FontFamily.MONOSPACE`
- `FontFamily.ACCESSIBLE_DFA`
- `FontFamily.IA_WRITER_DUOSPACE`
- `FontFamily.OPEN_DYSLEXIC`

Setting `fontFamily = null` means "use the publisher's original font".
