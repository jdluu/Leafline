# OpenCode Sandbox Auto-Rejection (verified 2026-08-22, Leafline session)

## The problem

OpenCode's sandbox auto-rejects ANY file access outside the working directory.
This includes:

- Reads from `~/.gradle/caches/` (cached AARs, dependency JARs)
- Writes to `/tmp/opencode/*` (its own scratch directory)
- Reads or writes under `/home/jdluu/` outside the repo

Each rejected permission is a **fatal error that exits the worker immediately**,
discarding all in-flight context (explored files, plan, partial edits). The
error message is:

```
! permission requested: external_directory (/tmp/opencode/*); auto-rejecting
✗ <command> failed
Error: The user rejected permission to use this specific tool call.
```

## What happened (3 failed launches)

1. **Launch 1**: Worker tried `mkdir -p /tmp/opencode/readium` to unzip a
   Readium AAR. Auto-rejected, exited.
2. **Launch 2**: Worker tried `mkdir -p build/tmp/readium-shared && cp
   ~/.gradle/.../readium-shared-3.3.0.aar ...`. Auto-rejected (reads from
   `~/.gradle`), exited.
3. **Launch 3**: Worker tried `mkdir -p /tmp/opencode/readium-shared` to
   unzip the AAR. Auto-rejected, exited.

## The fix (4th launch succeeded)

Pre-stage ALL external resources inside the repo BEFORE launching OpenCode:

```bash
# 1. Copy cached AARs into the repo
mkdir -p build/tmp/readium-ref
cp ~/.gradle/caches/modules-2/files-2.1/org.readium.kotlin-toolkit/readium-shared/3.3.0/*/readium-shared-3.3.0.aar build/tmp/readium-ref/
cp ~/.gradle/caches/modules-2/files-2.1/org.readium.kotlin-toolkit/readium-navigator/3.3.0/*/readium-navigator-3.3.0.aar build/tmp/readium-ref/

# 2. Pre-extract them so the worker only needs read-only tools
mkdir -p build/tmp/readium-ref/extracted
unzip -o -q build/tmp/readium-ref/readium-shared-3.3.0.aar -d build/tmp/readium-ref/extracted
cd build/tmp/readium-ref/extracted && unzip -o -q classes.jar -d classes
```

Then in the prompt, explicitly state:
> "Reference material: the Readium shared 3.3.0 AAR is already extracted under
> build/tmp/readium-ref/extracted/classes/. Do NOT write to /tmp or ~/.gradle;
> work only inside this repo. Do not attempt any command touching paths outside
> this repo, even for reads."

The worker can then use `javap -classpath build/tmp/readium-ref/extracted/classes
org.readium.r2.shared.publication.Publication` to inspect API signatures.

## Recovery after a sandbox-rejection exit

1. Inspect `git status` for partial changes the worker made before exiting.
2. Check for compile errors (the worker often leaves non-compiling code).
3. Fix compile errors directly (don't relaunch the worker for small fixes).
4. Run the full quality gate: `./gradlew test lint assembleDebug`.
5. Clean up any stray directories outside the repo (e.g. `build-tmp/`).
6. Stage and commit the verified changes.

## Key lesson

The sandbox rejection is not a transient error or a configurable permission.
It is a hard boundary. The worker cannot be granted access to external paths
mid-session. Pre-staging is the only reliable approach.
