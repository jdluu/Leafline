#!/usr/bin/env bash
# Runs debug instrumented tests on an attached device and archives the XML results.
set -euo pipefail

RESULTS_DIR="app/build/outputs/androidTest-results/connected"
ARCHIVE_DIR=".build-tmp/androidTest-results"

attached=$(adb devices 2>/dev/null | awk 'NR > 1 && $2 == "device"') || attached=""
if [ -z "$attached" ]; then
    echo "No adb device attached" >&2
    exit 1
fi

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
