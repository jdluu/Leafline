# Android EPUB/OpenCode Workflow Notes

## Evidence discipline

OpenCode's exit code and final prose are not verification. Independently inspect `git status`, `git log`, `git show --stat`, and `git diff --check`. Re-run the exact Gradle commands without shell pipelines that can mask failures. Confirm the commit and clean tree before reporting completion.

## Readium API recovery

For Readium Kotlin Toolkit 3.3.0, the verified metadata boundary is `Publication.metadata`. The resolved `Metadata` API exposes `identifier`, `title`, `language.code`, `description`, `published` (`kotlin.time.Instant?`), `authors`, `publishers`, and `numberOfPages`; `Contributor` exposes `name` and `sortAs`. Do not rely on speculative convenience properties such as `Publication.title` or `Publication.authors`.

When an agent needs dependency inspection, keep it inside the repository or use already-resolved artifacts with ordinary local inspection. If a harness rejects external-directory access, do not loop on the same `/tmp` extraction attempt; provide verified API facts directly or use a repository-local alternative.

## Real EPUB device test

A valid public-domain EPUB can be downloaded from its authoritative catalog page, pushed temporarily to the device's Download folder, and media-scanned when DocumentsUI does not immediately list it. In the picker, use the search field if Recent is empty. Select the file, verify `ReaderActivity` is top-resumed and the Readium navigator hierarchy contains `resourcePager`/`webView`, then capture screenshots: cover art alone proves opening, while a subsequent page with readable book text proves rendering. Do not commit the external fixture.

## Local-library sequencing

Keep milestones narrow: (1) bundled Readium rendering, (2) real local EPUB import and rendering, (3) pure metadata model/mapper, (4) persistence boundary, (5) Room-backed storage, (6) visible library UI, (7) OPDS/Grimmory. A planning document may contain speculative Room/API snippets; implementation must use only APIs that compile. If an agent adds unused Room dependencies while delivering only an in-memory repository, audit and make the result coherent before accepting it.

## Common Android checks

For a device smoke test: install the debug APK, launch the package, dump UIAutomator XML, verify visible button text, tap the picker, verify `com.android.documentsui` is top-resumed, cancel and verify the app returns. For an imported EPUB, verify the reader activity and inspect a screenshot of a text page, not only the activity name.
