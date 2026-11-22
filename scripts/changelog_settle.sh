#!/usr/bin/env bash
# Resolves changelog citations on main after a branch is merged.
#
# Why this exists: a commit SHA written on a feature branch is destroyed by both
# squash and rebase merges, so an entry cannot cite its own commit while the work
# is still on the branch. What is stable at authoring time is the pull request
# number, so entries are written as (#220) and this script adds the commit that
# actually carried them into main, producing (7a77307, #220).
#
# It is idempotent and safe to re-run: entries that already carry a commit are
# left alone. Run it on main immediately after merging, before the release
# commit, so the release notes carry both links.
#
# Exit codes: 0 settled (or nothing to do), 1 a citation could not be resolved.
set -euo pipefail

cd "$(dirname "$0")/.."

CHANGELOG=CHANGELOG.md
DRY_RUN=0
for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=1 ;;
        -h|--help) sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "unknown argument: $arg" >&2; exit 2 ;;
    esac
done

[ -f "$CHANGELOG" ] || { echo "FAIL: $CHANGELOG not found"; exit 1; }

branch=$(git rev-parse --abbrev-ref HEAD)
if [ "$branch" != "main" ]; then
    echo "FAIL: settle runs on main after the merge, not on $branch"
    exit 1
fi

tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

settled=0
pending=0
fail=0
section=""
while IFS= read -r line; do
    if [[ $line =~ ^\#\#\ \[([^]]+)\] ]]; then
        section="${BASH_REMATCH[1]}"
        printf '%s\n' "$line" >> "$tmp"
        continue
    fi
    # Only touch released sections; [Unreleased] has no PR yet by definition.
    if [[ $section =~ ^[0-9] ]] && [[ $line =~ \(#([0-9]+)\)[[:space:]]*$ ]]; then
        pr="${BASH_REMATCH[1]}"
        # The merge that carried this PR: squash keeps "(#123)" in its subject,
        # and a rebase keeps it on the individual commits. Either way the newest
        # commit on main mentioning the PR is the one that landed it.
        sha=$(git log --format='%h' --fixed-strings --grep="(#$pr)" main -1 2>/dev/null || true)
        if [ -z "$sha" ]; then
            echo "FAIL: $section cites #$pr, but no commit on main mentions that pull request"
            fail=1
            printf '%s\n' "$line" >> "$tmp"
            continue
        fi
        if ! git merge-base --is-ancestor "$sha" main 2>/dev/null; then
            echo "FAIL: $section cites #$pr and resolved $sha, which is not on main"
            fail=1
            printf '%s\n' "$line" >> "$tmp"
            continue
        fi
        printf '%s\n' "${line%\(#*}$(printf '(%s, #%s)' "$sha" "$pr")" >> "$tmp"
        settled=$((settled + 1))
        continue
    fi
    # A resolved entry already carrying a commit is left exactly as it is.
    if [[ $section =~ ^[0-9] ]] && [[ $line =~ \([0-9a-f]{7,40},[[:space:]]*#[0-9]+\)[[:space:]]*$ ]]; then
        printf '%s\n' "$line" >> "$tmp"
        continue
    fi
    printf '%s\n' "$line" >> "$tmp"
done < "$CHANGELOG"

if [ "$fail" -ne 0 ]; then
    rm -f "$tmp"
    echo "Nothing written: resolve the citations above first."
    exit 1
fi

if [ "$DRY_RUN" -eq 1 ]; then
    echo "would settle $settled citation(s)"
    diff -u "$CHANGELOG" "$tmp" || true
    exit 0
fi

if [ "$settled" -gt 0 ]; then
    cp "$tmp" "$CHANGELOG"
    echo "settled $settled citation(s) with their merge commit"
else
    echo "nothing to settle: every citation already carries a commit"
fi
