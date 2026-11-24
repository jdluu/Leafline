#!/usr/bin/env bash
# The canonical gate, defined once. AGENTS.md names these commands and CI calls
# this script, so the two cannot drift into separate definitions of "the gate".
set -euo pipefail

cd "$(dirname "$0")/.."

usage() {
    cat <<'EOF'
usage: scripts/verify.sh [mode]

  --tests      :app:testDebugUnitTest
  --lint       :app:lintDebug
  --assemble   :app:assembleDebug
  --guards     both scripts/check_* guards + git diff --check
  --all        everything, in one Gradle invocation (default)

One Gradle invocation per mode. Three sequential invocations triple daemon
startup on a memory-tight host, which is how a build failure gets mistaken for
a code failure.
EOF
}

run_guards() {
    bash scripts/check_dynamic_type.sh
    bash scripts/check_touch_targets.sh
    git diff --check
}

case "${1:-}" in
    --tests)    ./gradlew :app:testDebugUnitTest ;;
    --lint)     ./gradlew :app:lintDebug ;;
    --assemble) ./gradlew :app:assembleDebug ;;
    --guards)   run_guards ;;
    ""|--all)   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug && run_guards ;;
    -h|--help)  usage ;;
    *)          echo "verify: unknown mode '$1' (try --help)" >&2; exit 2 ;;
esac
