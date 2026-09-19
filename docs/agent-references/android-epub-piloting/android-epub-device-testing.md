# Android EPUB device-testing recipe

Use this reference when an autonomous Android reader task reaches device validation.

## Evidence sequence

1. Run `adb devices -l` and confirm the target is `device`; `unauthorized` is not usable test evidence.
2. Install the exact current APK with `adb install -r` and launch the package.
3. Inspect visible UI with `uiautomator dump` and focused `logcat`; do not infer rendering from process exit alone.
4. For file-picker features, verify both picker launch and cancellation return separately from book rendering.
5. For imported-publication compatibility, use a temporary EPUB from an authoritative source, transfer it to the device without committing it, select it through the actual picker when possible, and inspect a screenshot or UI hierarchy for readable content.
6. Report evidence precisely: picker-only, bundled EPUB rendering, or imported EPUB rendering. Do not collapse these into a generic “tested” claim.

## Repository hygiene

Keep downloaded external EPUBs outside the repository or remove them after testing. Run the project quality gate against the current working tree, then inspect `git diff --check`, changed files, and Git status before committing.

## Common interpretation

A successful `adb install`, activity launch, and no fatal logcat exception prove startup only. A `WebView` or navigator container in the UI hierarchy proves the navigator was created, but readable text or a screenshot is needed to claim publication rendering.
