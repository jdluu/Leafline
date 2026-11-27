#!/usr/bin/env sh
# Design contract guard: shapes and text fields must come from theme tokens
# (`LeaflineShapes` / `LeaflinePillShape` and `LeaflineTextField`) rather than
# ad-hoc `RoundedCornerShape` or raw `TextField` / `OutlinedTextField` calls
# outside `app/src/main/java/com/jdluu/leafline/theme/`.
set -eu

cd "$(dirname "$0")/.."

status=0
theme_dir="app/src/main/java/com/jdluu/leafline/theme"

shape_matches=$(grep -rnE 'RoundedCornerShape' app/src/main/java \
    | grep -v "^${theme_dir}/" || true)
if [ -n "$shape_matches" ]; then
    printf 'Ad-hoc RoundedCornerShape usage outside theme package (use MaterialTheme.shapes or LeaflinePillShape):\n%s\n' "$shape_matches"
    status=1
fi

field_matches=$(grep -rnE '(^|[^A-Za-z0-9_])(Outlined)?TextField\(' app/src/main/java \
    | grep -v "^${theme_dir}/" || true)
if [ -n "$field_matches" ]; then
    printf 'Raw TextField/OutlinedTextField usage outside theme package (use LeaflineTextField):\n%s\n' "$field_matches"
    status=1
fi

if [ "$status" -ne 0 ]; then
    exit 1
fi

echo 'OK: all shapes and text fields use Leafline theme tokens.'
