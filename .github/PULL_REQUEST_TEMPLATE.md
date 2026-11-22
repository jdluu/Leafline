## What changed

<!-- One paragraph. Name the user-visible effect, not the diff. -->

## What a reader gets

<!--
This is the audience for the whole change. The release notes are generated from
merged pull requests: tagging vX.Y.Z runs gh release create --generate-notes,
which lists every PR title between the previous tag and this one, linked, with its
author. There is no changelog file to update and nothing to cite by hand.

That makes the PR *title* a release-note line, so it has to stand on its own:
"fix: the catalog error offers a recovery it can perform", not "fix: OpdsScreen".
-->

## Verification

<!-- Paste real command output. A claim without a run is not verification. -->

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
bash scripts/check_dynamic_type.sh
bash scripts/check_touch_targets.sh
```

Device checks, if the change is visual: state the device, the font scale, and the
themes you looked at. "Looks fine" is not a check.

## Checklist

- [ ] Title reads as a release-note line, not as a file name
- [ ] Unit tests and lint pass, and both contract guards pass
- [ ] Visual changes checked on a device at font scale 1.3, light and dark
- [ ] `DESIGN.md` updated if the visual contract changed
- [ ] `PRODUCT.md` updated if a product decision changed
- [ ] No dependency, schema, or persisted-key change without agreement
