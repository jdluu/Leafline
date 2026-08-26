#!/usr/bin/env bash
# Runs debug instrumented tests on an attached device and archives the XML results.
set -euo pipefail

RESULTS_DIR="app/build/outputs/androidTest-results/connected"
ARCHIVE_DIR=".build-tmp/androidTest-results"

attached=$(adb devices 2>/dev/null | awk 'NR > 1 && $2 == "device" {print $1}') || attached=""
if [ -z "$attached" ]; then
    echo "No adb device attached" >&2
    exit 1
fi

# UiAutomator sees an empty accessibility tree while a device sleeps or shows a
# keyguard (#96), which fails every UI-dependent test. Wake and unlock each
# device, and keep the screen on via USB for the duration of the run.
declare -a SERIALS=()
declare -a SAVED_STAY_ON=()
for serial in $attached; do
    saved=$(adb -s "$serial" shell settings get global stay_on_while_plugged_in 2>/dev/null | tr -d '\r')
    SERIALS+=("$serial")
    SAVED_STAY_ON+=("$saved")
    adb -s "$serial" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
    adb -s "$serial" shell wm dismiss-keyguard >/dev/null 2>&1 || true
    adb -s "$serial" shell settings put global stay_on_while_plugged_in 7 >/dev/null 2>&1 || true
done

restore_device_settings() {
    for i in "${!SERIALS[@]}"; do
        adb -s "${SERIALS[$i]}" shell settings put global \
            stay_on_while_plugged_in "${SAVED_STAY_ON[$i]}" >/dev/null 2>&1 || true
    done
}
trap restore_device_settings EXIT

status=0
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest || status=$?

rm -rf "$ARCHIVE_DIR"
mkdir -p "$ARCHIVE_DIR"

results_list=""
if [ -d "$RESULTS_DIR" ]; then
    results_list=$(find "$RESULTS_DIR" -name '*.xml' -type f)
fi

if [ -n "$results_list" ]; then
    # Device names can contain spaces (e.g. "Pixel 7 - 17"), so hand the file
    # list to awk/cp through find -exec rather than unquoted expansion.
    stats=$(find "$RESULTS_DIR" -name '*.xml' -type f -exec awk '
        # Match <testsuite> but not the wrapping <testsuites> root.
        /<testsuite[ >]/ {
            line = $0
            while (match(line, /[a-z]+="[0-9]+"/)) {
                attr = substr(line, RSTART, RLENGTH)
                split(attr, pair, /=/)
                value = pair[2]
                gsub(/"/, "", value)
                totals[pair[1]] += value
                line = substr(line, RSTART + RLENGTH)
            }
        }
        END {
            printf "%d %d %d\n", totals["tests"],
                totals["failures"] + totals["errors"], totals["skipped"]
        }
    ' {} +)
    # shellcheck disable=SC2086
    set -- $stats
    failed=$2
    skipped=$3
    passed=$(($1 - failed - skipped))
    find "$RESULTS_DIR" -name '*.xml' -type f -exec cp {} "$ARCHIVE_DIR"/ \;
    echo "Instrumented tests: $passed passed, $failed failed, $skipped skipped"
else
    echo "Instrumented tests: no result XML found in $RESULTS_DIR"
fi

exit "$status"
