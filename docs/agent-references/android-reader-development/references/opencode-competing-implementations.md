# OpenCode Competing Implementations (verified 2026-08-22, Leafline search session)

## The problem

When OpenCode is killed mid-session (e.g., after a phantom-race freeze) and
relaunched with a fresh prompt for the same feature, the relaunched worker
may create a **second, competing implementation** alongside the first worker's
partial work. This is distinct from the phantom-race freeze itself — it's a
consequence of relaunching without first cleaning up the killed worker's
uncommitted changes.

## What happened (Leafline in-book search, commit 3248e30)

### Worker 1 (killed)

OpenCode was launched to implement in-book search. It:
1. Researched the Readium search API via context7 and GitHub.
2. Created a simple `BookSearchSupport.kt` in the root package with:
   - `BookSearchResult(locator, sectionTitle, snippet)` — simple model
   - `BookSearchSupport.normalizeQuery()`, `buildResult()`, `buildSnippet()`
3. Wrote `ReaderActivity` changes using this simple API.
4. Created `BookSearchSupportTest.kt`.
5. Then hit a phantom-race freeze and was killed.

### Worker 2 (also killed)

A second OpenCode launch was attempted. It:
1. Found the existing `BookSearchSupport.kt` from Worker 1.
2. Decided to create a richer implementation in `reader/search/` package:
   - `BookSearchModels.kt` — `BookSearchResult(id, sectionTitle, excerptBefore,
     excerptMatch, excerptAfter, locatorJson)`, `BookSearchState`,
     `BookSearchStatus`, `BookSearchQuery`
   - `BookSearcher.kt` — coroutine-based search runner with StateFlow
   - `BookSearchResultMapper.kt`
   - `BookSearcherTest.kt`, `BookSearchResultMapperTest.kt`
3. Rewrote `ReaderActivity` to use the new package's API.
4. Then exited (sandbox rejection on `/tmp/opencode`).

### Result

The repo had TWO competing implementations:
- `BookSearchSupport.kt` (root package, simple API) — used by nothing
- `reader/search/` package (rich API) — used by ReaderActivity but with
  stale imports from Worker 1's version

Plus stale test files referencing APIs from both implementations.

## Recovery pattern

1. **Inspect all untracked/modified files** with `git status --short`.
2. **Determine which implementation the calling code actually uses.**
   Grep for imports in the files that consume the models:
   ```bash
   grep -rn "BookSearchResult\|BookSearchSupport\|BookSearcher" app/src/main
   ```
3. **Delete the unused implementation entirely.**
   ```bash
   rm -rf app/src/main/java/.../BookSearchSupport.kt
   rm app/src/test/java/.../BookSearchSupportTest.kt
   ```
4. **Delete stale test files that reference deleted APIs.**
   Check each test file compiles against the surviving implementation.
   Tests from the killed worker often reference fields/methods that don't
   exist in the surviving code.
5. **Fix imports in the calling code** to point to the surviving package.
6. **Fix compile errors** identified by `./gradlew assembleDebug`.
7. **Run the full quality gate**: `./gradlew test lint assembleDebug`.

## Prevention

Before relaunching OpenCode after a kill:

1. Run `git status --short` to see all uncommitted changes.
2. If the killed worker left substantial uncommitted work, decide:
   - **Option A**: Commit the partial work first, then relaunch with a
     prompt that says "the data layer for X already exists at <files>; do
     NOT recreate it. Build only the UI wiring."
   - **Option B**: Stash/discard the partial work, then relaunch fresh.
3. Never relaunch with a generic prompt that could lead the worker to
   re-implement what the previous worker already created.

The prompt should explicitly list existing files:
> "The following files already exist and should NOT be recreated:
> BookSearchSupport.kt, BookSearchSupportTest.kt. Build on top of them."

## Key lesson

A kill+relaunch cycle without cleanup produces competing implementations.
Always inspect and triage uncommitted work before launching a replacement
worker. The compile errors from two competing implementations are confusing
but follow a clear pattern: "Unresolved reference" for types from the
deleted implementation, "Argument type mismatch" when two `BookSearchResult`
classes exist in different packages.
