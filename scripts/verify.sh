#!/usr/bin/env bash
# The canonical gate, defined once. AGENTS.md names these commands and CI calls
# this script, so the two cannot drift into separate definitions of "the gate".
#
#   ./scripts/verify.sh --tests      # :app:testDebugUnitTest
#   ./scripts/verify.sh --lint       # :app:lintDebug
#   ./scripts/verify.sh --assemble   # :app:assembleDebug
#   ./scripts/verify.sh --guards     # both scripts/check_* guards + git diff --check
#   ./scripts/verify.sh --all        # everything, in one Gradle invocation
#
# One Gradle invocation per mode. Three sequential invocations triple daemon
# startup on a memory-tight host, which is how a build failure gets mistaken for
# a code failure.
set -uo pipefail

cd "$(dirname "$0")/.."

run_guards() {
    bash scripts/check_dynamic_type.sh || return 1
    bash scripts/check_touch_targets.sh || return 1
    git diff --check
}

mode="${1:-}"
case "$mode" in
    --tests)    ./gradlew :app:testDebugUnitTest ;;
    --lint)     ./gradlew :app:lintDebug ;;
    --assemble) ./gradlew :app:assembleDebug ;;
    --guards)   run_guards ;;
    ""|--all)   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug && run_guards ;;
    -h|--help)  sed -n '2,13p' "$0"; exit 0 ;;
    *)          echo "verify: unknown mode '$mode' (try --help)" >&2; exit 2 ;;
esac
