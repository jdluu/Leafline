# ADB On-Device Verification for Android Reader (verified 2026-08-22)

Techniques for verifying an Android app on a physical device via ADB when
`vision_analyze` cannot reliably describe screenshots.

## When vision_analyze fails on Android screenshots

`vision_analyze` may return "Image attached natively" without producing a
usable description, especially for screenshots with WebView content or complex
Compose UIs. Do not retry it repeatedly. Switch to the UIAutomator dump
approach.

## UIAutomator dump + Python XML parsing (reliable)

```bash
# Dump the UI hierarchy to a file on device, then pull it
adb -s <serial> shell uiautomator dump /sdcard/ui.xml
adb -s <serial> pull /sdcard/ui.xml /tmp/ui.xml

# Parse with Python to extract all text/content-desc labels and their tap coordinates
python3 -c "
import re
xml = open('/tmp/ui.xml').read()
for m in re.finditer(r'<node[^>]*?(?:text|content-desc)=\"([^\"]+)\"[^>]*?bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    label = m.group(1)
    if label:
        cx = (int(m.group(2))+int(m.group(4)))//2
        cy = (int(m.group(3))+int(m.group(5)))//2
        print(f'{label:50s} center=({cx},{cy})')
"
```

This gives you every visible UI element's text/content-description and its
exact center coordinates for `adb shell input tap`.

## Common ADB commands for reader verification

```bash
# Install and launch
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell am start -n com.jdluu.leafline/.MainActivity

# Check which activity is focused
adb -s <serial> shell "dumpsys window | grep mCurrentFocus"

# Check for crashes (filter to app PID)
adb -s <serial> shell "ps -A | grep leafline"
adb -s <serial> logcat -d | grep -E "FATAL|com.jdluu.leafline.*Exception" | tail

# Tap a UI element at coordinates
adb -s <serial> shell input tap <x> <y>

# Press back
adb -s <serial> shell input keyevent KEYCODE_BACK

# Inspect app-private storage (covers, imported EPUBs)
adb -s <serial> shell "run-as com.jdluu.leafline ls -la files/"
adb -s <serial> shell "run-as com.jdluu.leafline ls -la files/covers/"

# Push a test EPUB for import testing
adb -s <serial> push ~/Downloads/pride-and-prejudice.epub /sdcard/Download/
```

## SAF document picker automation

The app uses `ActivityResultContracts.OpenDocument()` for EPUB import. A
`VIEW` intent will NOT trigger import. Instead:

1. Dump UI to find the "Import EPUB" button coordinates.
2. `adb shell input tap <x> <y>` to open the SAF picker.
3. Dump UI again to find the file entry in the picker.
4. Tap the file entry to select it.

## Toggle the reader toolbar

The reader toolbar is toggled by tapping the center of the screen. **Important:**
when the Readium EPUB navigator is active, the reader overlay (toolbar, sepia
chip, bookmark buttons, etc.) is rendered by Compose over a `WebView`.
UIAutomator dumps of the reader screen will show only a single `WebView` node
with no children — the toolbar icons and their `content-desc` attributes are
**NOT visible** in the dump and cannot be used to find tap coordinates. To
verify the toolbar state:

1. Take a screenshot after tapping center: `adb exec-out screencap -p > /tmp/s.png`
2. Inspect the screenshot with vision to confirm toolbar visibility.
3. To interact with toolbar buttons, use known coordinates — the toolbar
   icons sit in a consistent top bar. Tap positions (1080×2400 screen):
   - Back arrow: ~(80, 200)
   - Title / TOC: ~(280, 200)
   - Settings gear: ~(850, 200)
   - More options (⋮): ~(920, 200)

The "More options" overflow menu expands to show bookmark actions ("Add
bookmark", "Bookmarks").

## mobile-mcp alternative interaction

The `mobile_mcp__mobile_*` tools provide a second path for device interaction
that complements raw adb and can be simpler for screenshot-heavy workflows:

- `mobile_mcp__mobile_click_on_screen_at_coordinates` —
  `{"device": "<serial>", "x": N, "y": N}`
- `mobile_mcp__mobile_double_tap_on_screen` — same args; useful when timing
  matters (rapid taps to toggle toolbar when a single tap doesn't register)
- `mobile_mcp__mobile_take_screenshot` —
  `{"device": "<serial>"}` saves to `~/.hermes/cache/images/` with a
  generated filename and returns a `MEDIA:` path for immediate vision
  inspection

Mobile-mcp screenshots are typically smaller (~385 KB) than raw adb screencaps
(~2.5 MB) but are quality-equivalent for vision analysis.

## Verify cover extraction

```bash
# Check that cover files were created in app-private storage
adb -s <serial> shell "run-as com.jdluu.leafline ls -la files/covers/"
# Output should show files like: http___www.gutenberg.org_1342.cover (232KB)
```

## Typical verification flow

1. Install APK, launch app.
2. UIAutomator dump to find "Import EPUB" button.
3. Tap it, dump again to find file in SAF picker.
4. Tap file, dump to confirm book appears in library grid.
5. Tap book tile, confirm ReaderActivity is focused.
6. Tap center to toggle toolbar — verify with screenshot (uiautomator cannot
   see toolbar icons over the Readium WebView; use vision on the screenshot).
7. Tap "More options" to access bookmark menu.
8. Tap "Add bookmark", check for "Bookmark added" toast in UI dump.
9. Open bookmarks sheet, confirm bookmark entry appears.
10. Check app-private storage for cover cache files.
