# Kotlin Activity Extraction Technique

A repeatable pattern for splitting monolithic Android Activity files
into focused manager classes — zero behavioral change, verified after
each slice.

## When to use

- An Activity file exceeds ~800 lines and has multiple responsibilities
  (bookmarks, highlights, search, sync, navigation, settings, UI)
- Each responsibility can be extracted as its own class without changing
  the Activity's public API
- You've already read the source and know exactly what to extract

## Technique steps (one slice at a time)

### 1. Identify a clean boundary

Each extracted responsibility maps to a contiguous block of functions
in the Activity. Example:

```
ReaderActivity (2195 lines)
├── Bookmarks (3 functions, ~80 lines)
├── Annotations (7 functions, ~150 lines)
├── Sync (5 functions, ~120 lines)
├── Navigation/tap zones (3 components, ~80 lines)
├── Search (6 functions, ~90 lines)
├── Settings/brightness (3 functions, ~40 lines)
└── Compose UI (>1000 lines)
```

### 2. Create the manager class in the same package

**Same package = no import changes.** The new file stays in
`com.jdluu.leafline` or a subpackage of it — not a different module.

If the new class needs a subpackage (e.g. `reader/navigation/`),
keep the manager in that subpackage under the same root package.
The Activity will import the subpackage type (one import per manager).

### 3. Change private → internal visibility

Functions extracted from the Activity were `private`. In the new file,
they become `internal fun` (or `public fun`). Top-level constants that
were `private const val` become `internal const val` so both files see
them.

```kotlin
// Before in ReaderActivity.kt:
private fun annotationTint(colorHex: String): Int { ... }

// After in AnnotationManager.kt:
fun annotationTint(colorHex: String): Int { ... }
```

### 4. Use constructor-injected lambdas for Activity coupling

The manager needs access to the navigator, repositories, scope, and
UI effects (toast, snackbar, sheet visibility). Pass these as lambdas
and providers in the constructor:

```kotlin
class AnnotationManager(
    private val context: Context,
    private val scope: LifecycleCoroutineScope,
    private val repository: AnnotationRepository,
    private val navigatorProvider: () -> EpubNavigatorFragment?,
    private val bookStableIdProvider: () -> String?,
    private val annotationsProvider: () -> List<Annotation>,
    private val onSnackbar: (String) -> Unit,
    private val onAnnotationNavigated: () -> Unit,
)
```

This makes the manager testable (mock the lambdas) and avoids holding
an Activity reference.

### 5. Rewire call sites in the Activity

Replace direct function calls with manager delegation:

```kotlin
// Before:
selectionActionModeCallback = annotationSelectionActionMode()
// After:
selectionActionModeCallback = annotationManager.selectionActionMode()
```

Init the manager in `onCreate()` after repository setup.

### 6. Remove the extracted functions from the Activity

Use `patch` to delete the now-extracted function block from the
Activity file. The Activity keeps only lifecycle, field declarations,
and orchestration.

### 7. Build-verify after each slice

```bash
./gradlew :app:compileDebugKotlin
```

Fix any import or visibility issues before moving on. Each slice
should be independently compilable.

### 8. Test-verify after each slice

```bash
./gradlew :app:testDebugUnitTest
```

Compare total failures against pre-refactor baseline. If the same
number fail with the same class names, zero regression. If a new
failure appears, fix it immediately — do not stack slices on broken
ground.

### 9. Commit per slice

Each extraction gets its own feature branch, PR, and squash merge.
Reference the tracking issue.

## Results from this session

| Slice | PR | Lines removed |
|---|---|---|
| Overlay UI → `ReaderOverlayUi.kt` | #63 | ~1,125 |
| Sync → `reader/sync/ReaderSyncManager` | #65 | ~150 |
| Annotations → `reader/annotations/AnnotationManager` | #66 | ~190 |
| Tap zones → `reader/navigation/TapZoneHandler` | #67 | ~80 |
| Bookmarks → `reader/bookmarks/BookmarkManager` | #68 | ~30 (wiring only) |
| **Total** | 5 PRs | **2195 → 738 (−66%)** |

## Why not use a subagent

Mechanical extraction refactors look like good subagent tasks (simple,
repetitive) but hit a wall: the LLM behind the agent re-reads the
entire source file on every reasoning turn, burning context. A 2,000+
line file means the model never has room to write the output. Direct
execution by the pilot is 10x faster and costs zero API credits.