#!/usr/bin/env sh
# Touch target guard (issue #9). Material 3 interactive components enforce a
# 48dp minimum touch target by default, so regressions come from custom
# clickable elements or explicit size modifiers that shrink them. This check
# fails when:
#   1. an IconButton call sets Modifier.size below 48dp;
#   2. an IconButton argument block sets Modifier.size below 48dp on a later
#      line;
#   3. any Compose code uses Modifier.size below 48dp, which is reserved for
#      non-interactive decoration only.
# Append the marker comment `touch-target-ok` to a line to exempt it from
# rule 3 after verifying the element is not interactive.
set -eu

cd "$(dirname "$0")/.."

status=0

inline=$(grep -rnE 'IconButton\(.*\.size\((4[0-7]|[0-3]?[0-9])(\.[0-9]+)?\.dp' app/src/main/java || true)
if [ -n "$inline" ]; then
    printf 'Icon buttons sized below the 48dp minimum touch target:\n%s\n' "$inline"
    status=1
fi

nearby=$(find app/src/main/java -name '*.kt' -type f -print0 \
    | xargs -0 awk '
        /IconButton\(/ { iconButtonLine = FNR }
        /\.size\((4[0-7]|[0-3]?[0-9])(\.[0-9]+)?\.dp/ \
            && iconButtonLine && FNR > iconButtonLine && FNR - iconButtonLine <= 5 {
            printf "%s:%d: size below 48dp inside IconButton arguments (line %d)\n",
                FILENAME, FNR, iconButtonLine
        }' || true)
if [ -n "$nearby" ]; then
    printf 'Icon buttons sized below the 48dp minimum touch target:\n%s\n' "$nearby"
    status=1
fi

other=$(grep -rnE '\.size\((4[0-7]|[0-3]?[0-9])(\.[0-9]+)?\.dp' app/src/main/java \
    | grep -v 'touch-target-ok' || true)
if [ -n "$other" ]; then
    printf 'Sizes below the 48dp minimum touch target; interactive elements must reach 48dp:\n%s\n' "$other"
    status=1
fi

if [ "$status" -ne 0 ]; then
    exit 1
fi

echo 'OK: no interactive elements sized below the 48dp minimum touch target.'
