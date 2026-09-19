# OpenCode Worker Failure Modes (verified 2026-08-22, Leafline bookmarks session)

## Phantom-race freeze

### Symptoms

OpenCode emits messages like:
- "Two opencode agents are racing on this repo"
- "The file was rewritten externally during this session"
- "another session (started 23:16) implemented a variant of this feature"

But `process list` shows only ONE worker running (the current one). The worker
then says it will "wait for it to go quiet, monitoring for file changes" and
freezes indefinitely.

### Root cause

OpenCode detects that files on disk have changed since it last read them (the
changes were its own earlier writes in the same session, possibly after a
context window compaction). It misattributes these to a competing agent.

### Recovery

1. Check `process list` to confirm no other worker is actually running.
2. Kill the frozen process immediately. It will not self-resolve.
3. Inspect `git status` and `git diff` for partial changes the worker made
   before freezing.
4. Either:
   a. Relaunch with a fresh prompt that explicitly states "no other session
      is modifying these files; file timestamps may differ from your cache
      due to your own earlier edits", OR
   b. Implement the remaining changes directly if the worker already produced
   most of the code (preferred for small completions).

### When it happened

During the bookmarks UI wiring slice (commit 3b2194d), OpenCode had already:
- Created the entire bookmark data layer (Entity, DAO, Repository, RepositoryImpl)
- Written 306 lines of ReaderActivity changes (bookmark toggle, bottom sheet,
  swipe-to-delete, snackbar)
- Written unit tests

Then it hit the phantom-race detection and froze. Killing it and fixing the
two remaining compile errors directly was faster than relaunching.

## Incomplete rename pattern

### Symptoms

OpenCode renames a field declaration but misses usage sites, leaving
non-compiling code. Example:

- Declaration: `private var currentLocation = mutableStateOf<Locator?>(null)`
- Missed site 1: `currentLocatorJson.value = locatorJson` (should be
  `currentLocation.value = locator`)
- Missed site 2: `currentLocatorJson = currentLocatorJson.value` (should be
  `currentLocation.value?.toJSON()?.toString()`)

### Recovery

1. Run `./gradlew assembleDebug` and read the compile errors.
2. `grep` for the old name to find all remaining references.
3. Patch each directly with `patch` tool.
4. Re-run the build to confirm.

Do not relaunch the worker for a 2-line fix. The compile error message gives
you exact file:line locations.

## Key lesson

When OpenCode exits prematurely (for any reason), always:
1. Check `git status` for partial work.
2. Run the build to find compile errors.
3. Fix small errors directly.
4. Run the full quality gate (`test lint assembleDebug`).
5. Commit the verified result.

The worker's partial work is often 80-90% complete and just needs a few
manual fixes to compile and pass.
