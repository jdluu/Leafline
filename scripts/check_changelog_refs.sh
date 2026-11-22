#!/usr/bin/env bash
# Fails when a CHANGELOG entry is missing or carries a dangling commit reference.
#
# Every bullet under a released version must end with a short commit SHA in the
# form " (abc1234)", and that commit must still exist on the branch being
# checked. GitHub turns a bare SHA into a link to the commit, so the entry is
# clickable wherever the changelog renders: the release notes, the file view, and
# the diff. A leading "#" is deliberately rejected: "#abc1234" is how an issue or
# PR is written, and GitHub leaves a hash-prefixed SHA as plain text instead of
# linking it.
#
# The reference goes at the END of the entry, on its last line, never inside the
# sentence: wrapped bullets are common, and a ref mid-prose stops the text
# reading as prose.
#
# Exempt: the preamble, [Unreleased] (work in progress has no commit yet), and
# the sections in LEGACY below. Those were written in batches before this
# convention existed, and the text of one entry cannot be proven to come from one
# commit, so citing a commit there would be a guess dressed as a fact. The gate is
# strict from the first non-legacy release onward. Removing a version from LEGACY
# requires attributing its entries first.
set -euo pipefail

cd "$(dirname "$0")/.."

CHANGELOG=CHANGELOG.md
[ -f "$CHANGELOG" ] || { echo "FAIL: $CHANGELOG not found"; exit 1; }

# Sections that predate the convention. Adding a version here needs a reason in
# review.
LEGACY=" 0.0.1 0.1.0 0.2.0 0.3.0 0.4.0 "

fail=0
checked=0
attributed=0
legacy=0
declare -A seen

section=""
in_bullet=0
bullet_head=""
bullet_last=""

check_bullet() {
    if [[ $bullet_last =~ \(([0-9a-f]{7,40})\)[[:space:]]*$ ]]; then
        local sha="${BASH_REMATCH[1]}"
        attributed=$((attributed + 1))
        if ! git cat-file -e "${sha}^{commit}" 2>/dev/null; then
            echo "FAIL: $section cites #$sha, which is not a commit in this repository"
            fail=1
        elif ! git merge-base --is-ancestor "$sha" HEAD 2>/dev/null; then
            echo "FAIL: $section cites #$sha, which is not on this branch"
            fail=1
        else
            seen[$sha]=1
        fi
    else
        echo "FAIL: unattributed entry in $section: ${bullet_head:0:72}"
        fail=1
    fi
}

while IFS= read -r line || [[ -n $line ]]; do
    case $line in
        "## ["*)
            [[ $in_bullet -eq 1 ]] && { check_bullet; in_bullet=0; }
            section="${line#\## \[}"
            section="${section%%\]*}"
            ;;
        "### "*)
            [[ $in_bullet -eq 1 ]] && { check_bullet; in_bullet=0; }
            ;;
        "- "*)
            [[ $in_bullet -eq 1 ]] && { check_bullet; in_bullet=0; }
            if [[ $section =~ ^[0-9] ]]; then
                if [[ $LEGACY == *" $section "* ]]; then
                    legacy=$((legacy + 1))
                else
                    checked=$((checked + 1))
                    in_bullet=1
                    bullet_head=$line
                    bullet_last=$line
                fi
            fi
            ;;
        *)
            # Indented text continues the current entry; a blank line ends it,
            # but the ref may also sit on that last continuation line, so only
            # a structural line (handled above) closes it early.
            if [[ $in_bullet -eq 1 && $line =~ ^[[:space:]]+[^[:space:]] ]]; then
                bullet_last=$line
            fi
            ;;
    esac
done < "$CHANGELOG"

[[ $in_bullet -eq 1 ]] && check_bullet

if [ "$fail" -eq 0 ]; then
    echo "OK: $attributed of $checked entries cite a commit (${#seen[@]} distinct), $legacy legacy entries exempt"
fi
exit "$fail"
