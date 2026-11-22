#!/usr/bin/env bash
# Fails when a CHANGELOG entry is missing or carries a dangling commit reference.
#
# Two forms are accepted at the end of an entry, and both render as links:
#
#   (7a77307)          work that landed before pull requests were cited
#   (7a77307, #220)    the commit that carried it and the pull request it came from
#
# A bare SHA must be first, because GitHub autolinks it to the commit; the PR
# reference after it covers the review and the diff of the branch as a whole.
# "#abc1234" is rejected: a hash is issue syntax, and GitHub leaves a
# hash-prefixed SHA as plain text instead of linking it.
#
# "(#220)" alone is also accepted, because that is the only reference available
# when the entry is written: the commit does not exist yet. Pull requests must
# pass this gate, so a release PR that promotes [Unreleased] cannot be blocked on
# a SHA that the merge will create. scripts/changelog_settle.sh upgrades it to
# "(sha, #220)" on main after the merge, and this file accepts every stage.
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
unsettled=0
pending=0
pending_checked=0
unreleased_phase=0
declare -A seen

section=""
in_bullet=0
bullet_head=""
bullet_last=""

check_bullet() {
    if [ "${unreleased_phase:-0}" -eq 1 ]; then
        unreleased_phase=0
        if [[ $bullet_last =~ \(#[0-9]+\)[[:space:]]*$ ]]; then
            pending=$((pending + 1))
        else
            echo "FAIL: unreleased entry names no pull request: ${bullet_head:0:66}"
            fail=1
        fi
        return
    fi
    if [[ $bullet_last =~ \(([0-9a-f]{7,40})(,[[:space:]]*#[0-9]+)?\)[[:space:]]*$ ]]; then
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
    elif [[ $bullet_last =~ \(#[0-9]+\)[[:space:]]*$ ]]; then
        # Valid but unsettled: the pull request exists, the merge commit does not.
        # settle upgrades it on main; until then it still links to the review.
        attributed=$((attributed + 1))
        unsettled=$((unsettled + 1))
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
            elif [ "$section" = "Unreleased" ]; then
                # Unreleased work still needs its pull request: that is the only
                # reference that exists before the merge, and it is what makes the
                # entry traceable in the PR that adds it.
                pending_checked=$((pending_checked + 1))
                in_bullet=1
                bullet_head=$line
                bullet_last=$line
                unreleased_phase=1
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
    echo "OK: $attributed of $checked entries are attributed (${#seen[@]} distinct commits), $legacy legacy entries exempt"
    if [ "$unsettled" -gt 0 ]; then
        echo "NOTE: $unsettled cite a pull request only; scripts/changelog_settle.sh adds the merge commit on main"
    fi
    if [ "$pending" -gt 0 ]; then
        echo "OK: $pending unreleased entr(y/ies) name their pull request"
    fi
fi
exit "$fail"
