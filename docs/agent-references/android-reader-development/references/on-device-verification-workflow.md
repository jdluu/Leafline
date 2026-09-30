# On-device verification workflow (adb + Pixel 7)

Verified end-to-end pattern for building, installing, and driving Leafline on a
physical device over adb. Validated 2026-08-23 on Pixel 7 (panther), Android 16.

## Build environment

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # required; default may point at java-17 and fail
cd ~/projects/Leafline
./gradlew :app:compileDebugKotlin   # fast error check (~30s)
./gradlew :app:testDebugUnitTest    # JVM tests
./gradlew :app:assembleDebug        # APK -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Known pre-existing test failures: SepiaQuickControlTest (7) and ReaderSettingsTest (1)
fail with `ExceptionInInitializerError` from Readium's `Theme` class in JVM tests —
Android classes not mockable in plain JUnit. Compare failure counts before/after;
do not treat these as regressions.

## Multi-device adb

When two devices are attached (`adb devices` shows both), every command needs
`-s <serial>` or it fails with "more than one device/emulator". Pin the serial in
a shell variable: `D=28261FDH200F50`.

## Driving the UI

1. Find elements via uiautomator dump (gives text + center coordinates):

```bash
adb -s $D shell uiautomator dump /sdcard/ui.xml >/dev/null && adb -s $D pull /sdcard/ui.xml /tmp/ui.xml >/dev/null
python3 -c "
import re
xml = open('/tmp/ui.xml').read()
for m in re.finditer(r'<node[^>]*?(?:text|content-desc)=\"([^\"]+)\"[^>]*?bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    label = m.group(1)
    if label and label.strip():
        cx=(int(m.group(2))+int(m.group(4)))//2; cy=(int(m.group(3))+int(m.group(5)))//2
        print(f'{label[:50]:50s} ({cx},{cy})')
"
```

2. Act: `input tap X Y`, `input swipe X1 Y1 X2 Y2 ms`, `input text ...`,
   `input keyevent KEYCODE_DEL / KEYCODE_MOVE_HOME / 4 (back)`.

3. Verify visually: `adb exec-out screencap -p > /tmp/s.png` then inspect with
   vision. Screenshots are black when the screen is off/locked.

### Pitfalls

- **Soft keyboard covers the bottom half** — any toast/snackbar result under the
  focused field is invisible until you dismiss the keyboard (`input keyevent 4`,
  but exactly once; twice backs out of the app).
- **`adb input text` mangles URLs**: typing `http://...` loses/garbles the `://`.
  Workaround: tap field, type only `host:port/path`, then `input keyevent
  KEYCODE_MOVE_HOME`, then `input text "http://"`. Clearing a long field via
  repeated DEL is unreliable — `pm clear com.jdluu.leafline` + fresh start is
  faster than fighting selection state.
- **Back keyevent cascades**: dismiss-keyboard taps of "back" can exit the app
  into other apps (camera was hit this way). After each back, screenshot before
  the next step.
- **Session-only config**: sync credentials live in memory only; force-stop or
  reinstall wipes them. Re-enter after every fresh install when testing sync.
- **Crash triage**: `adb logcat -c`, reproduce, then
  `adb logcat -d | grep -A25 FATAL`. Filter by timestamp — old crashes linger
  in the buffer and mislead.
- **SAF file picker starts in "Recent" with no items**: after `adb push` to
  `/sdcard/Download/`, the system file picker opens to a "Recent files" view
  that may show "No items". Tap the hamburger ("Show roots", typically top-left
  at ~94,220 on a 1080×2400 screen), then select "Downloads" from the drawer.
  Files pushed to `/sdcard/Download/` will appear there.
- **Readium WebView blinds uiautomator**: once a book is open, the EPUB
  navigator is a `WebView`. UIAutomator sees only that single element — no
  toolbar icons, sepia chip, or reader buttons are visible in the dump. Use
  screenshots + vision to verify reader UI state.

## Network to local servers (Grimmory)

- Cleartext HTTP is blocked by default: "CLEARTEXT communication ... not permitted
  by network security policy". Fix is committed:
  `res/xml/network_security_config.xml` with `cleartextTrafficPermitted="true"`
  registered via `android:networkSecurityConfig` in the manifest.
- Ping success does NOT imply TCP reachability. The dev VM could ping
  the server host but the KOReader port timed out from both VM and phone (different subnets,
  firewall). Test the actual port from the same network segment as the client.

## Cold-start measurement (#26 pattern)

```bash
for i in 1 2 3 4 5; do
  adb -s $D shell am force-stop com.jdluu.leafline; sleep 2
  adb -s $D shell am start -W -n com.jdluu.leafline/.MainActivity | grep TotalTime
done
```

Debug-build numbers are JIT-dominated (~1.6 s vs ~0.74 s for system Settings);
only release/R8 numbers are meaningful for optimization decisions.
