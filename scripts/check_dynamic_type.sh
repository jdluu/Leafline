#!/usr/bin/env sh
# Dynamic type guard (issue #8). Compose text must scale with the system
# font size setting, so text sizes should come from MaterialTheme.typography
# or default styles. Hardcoded fontSize values in dp or px do not follow the
# user's font scale and fail this check; sp values still scale and pass.
set -eu

cd "$(dirname "$0")/.."

pattern='fontSize[[:space:]]*=[[:space:]]*[0-9]+(\.[0-9]+)?\.(dp|px)'
matches=$(grep -rnE "$pattern" app/src/main/java || true)

if [ -n "$matches" ]; then
    printf 'Hardcoded dp or px Compose text sizes found:\n%s\n' "$matches"
    exit 1
fi

echo 'OK: no hardcoded dp or px Compose text sizes.'
