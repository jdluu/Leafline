## What changed

<!-- One paragraph. Name the user-visible effect, not the diff. -->

## Changelog

<!--
Every user-visible change needs an entry in CHANGELOG.md, and every entry ends
with a citation on its own last line:

  on this branch   (#<this PR number>)
  after merge      (<sha>, #<this PR number>)   written by the settle workflow

Cite the pull request here, not a commit: a squash or rebase rewrites your
commits, so a SHA written now would point at nothing after the merge. The
"Settle changelog citations" workflow attaches the merge commit on main.

Copy each entry below so the same reference links here and in the file.
-->

- 

## Verification

<!-- Paste real command output. A claim without a run is not verification. -->

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
bash scripts/check_dynamic_type.sh
bash scripts/check_touch_targets.sh
bash scripts/check_changelog_refs.sh
```

Device checks, if the change is visual: state the device, the font scale, and the
themes you looked at. "Looks fine" is not a check.

## Checklist

- [ ] `scripts/check_changelog_refs.sh` passes (entry present, cites this PR)
- [ ] Unit tests and lint pass, and both contract guards pass
- [ ] Visual changes checked on a device at font scale 1.3, light and dark
- [ ] `DESIGN.md` updated if the visual contract changed
- [ ] `PRODUCT.md` updated if a product decision changed
- [ ] No dependency, schema, or persisted-key change without agreement
