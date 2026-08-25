# ReaderActivity Refactor Plan

> **Goal:** Split ReaderActivity.kt (2195 lines, 32 functions, 6 responsibilities) into focused modules so each file has a single reason to change, making the codebase easier to navigate, test, and maintain.

**Architecture:** Each domain (bookmarks, highlights, search, sync, navigation, UI) gets its own package under `reader/`. ReaderActivity becomes a thin orchestrator that delegates to these managers. The existing `reader/search/` package is already factored correctly — we mirror that pattern for the other domains.

**Tech Stack:** Kotlin, Jetpack Compose, Readium Kotlin Toolkit, Room, Coroutines

**Principle:** No behavioral changes. Pure extraction — every function moves as-is. The only edits are imports and wiring.

---

## Context

### Current state

```
ReaderActivity.kt (2195 lines)
├── Tap zone handling + page turns (~200 lines)
├── Bookmark CRUD (~150 lines)
├── Highlight/annotation CRUD (~400 lines)
├── Search (5 functions, already uses reader/search/)
├── Sync/progress (~200 lines)
├── Reader overlay composables (~500 lines)
├── Settings/brightness composables (~200 lines)
├── Bottom sheets (bookmarks, highlights, search, sync conflict) (~200 lines)
└── Lifecycle / wiring (~150 lines)
```

Already separate: `reader/ReaderSettings.kt`, `reader/ReaderPreferencesStore.kt`, `reader/WcagContrast.kt`, `reader/search/BookSearcher.kt`, `reader/search/BookSearchModels.kt`, `reader/search/BookSearchResultMapper.kt`

### Files that reference ReaderActivity
- `MainActivity.kt` — opens reader via `ReaderActivity.newIntent()`
- `ReaderFlowTest.kt` — launches reader via intent

### Import paths
- `com.jdluu.leafline.ReaderActivity` → will stay as the entry point
- `com.jdluu.leafline.reader.bookmarks.BookmarkManager`
- `com.jdluu.leafline.reader.annotations.AnnotationManager`
- `com.jdluu.leafline.reader.sync.ReaderSyncManager`
- `com.jdluu.leafline.reader.ui.ReaderBottomSheets`
- `com.jdluu.leafline.reader.ui.ReaderOverlay`

---

## Step-by-step plan

### Task 1: Create `reader/bookmarks/BookmarkManager.kt`

**Objective:** Extract bookmark CRUD (toggle, delete, navigate) into a class taking the navigator and repository as constructor params.

**Extract from ReaderActivity.kt:**
- `toggleBookmark()` — lines ~350-400
- `deleteBookmark()` — lines ~400-430
- `navigateToBookmark()` — lines ~430-460
- `bookmarks` state field — current list

**New file:** `app/src/main/java/com/jdluu/leafline/reader/bookmarks/BookmarkManager.kt`
```kotlin
package com.jdluu.leafline.reader.bookmarks

class BookmarkManager(
    private val navigator: () -> EpubNavigatorFragment?,
    private val repository: BookmarkRepository
) {
    // Extracted functions verbatim, with `bookmarks` exposed as StateFlow
}
```

**Verification:** `./gradlew :app:assembleDebug` compiles

---

### Task 2: Create `reader/annotations/AnnotationManager.kt`

**Objective:** Extract highlight/annotation CRUD + tint selection into a class.

**Extract from ReaderActivity.kt:**
- `annotationSelectionActionMode()` — ActionMode callback
- `saveSelectionAsAnnotation()` — save from selected text
- `saveHighlightWithTint()` — save with chosen tint
- `copySelectedText()` — clipboard helper
- `selectedTextOf()` — extract text from selection
- `applyAnnotationDecorations()` — render decorations
- `deleteAnnotation()` — remove annotation
- `navigateToAnnotation()` — jump to annotation
- `annotationTint()` — resolve tint color
- `pendingHighlightLocator`, `selectedHighlightTint`, `annotations` state

**New file:** `app/src/main/java/com/jdluu/leafline/reader/annotations/AnnotationManager.kt`

**Verification:** `./gradlew :app:assembleDebug` compiles

---

### Task 3: Create `reader/sync/ReaderSyncManager.kt`

**Objective:** Extract sync operations into a class.

**Extract from ReaderActivity.kt:**
- `createProgressSyncer()` — factory
- `pullRemoteProgress()` — fetch remote locator
- `presentRemoteProgress()` — show conflict sheet
- `pushProgressOnExit()` — push on activity stop
- `syncConflictState` field

**New file:** `app/src/main/java/com/jdluu/leafline/reader/sync/ReaderSyncManager.kt`

**Verification:** `./gradlew :app:assembleDebug` compiles

---

### Task 4: Create `reader/navigation/TapZoneHandler.kt`

**Objective:** Extract tap zone logic and page turn animation.

**Extract from ReaderActivity.kt:**
- `readerInputListener` — InputListener instance
- `pageTurnAnimated()` — animation resolver
- `pageTurnAnnouncement` + related debounce logic

**New file:** `app/src/main/java/com/jdluu/leafline/reader/navigation/TapZoneHandler.kt`

**Verification:** `./gradlew :app:assembleDebug` compiles

---

### Task 5: Create `reader/ui/ReaderOverlay.kt`

**Objective:** Extract all Compose UI composables from ReaderActivity.

**Extract from ReaderActivity.kt:**
- `addReaderOverlay()` — the main composable
- Toolbar, bottom sheets, drawer, settings panel, search panel
- All the `@Composable` functions

**New file:** `app/src/main/java/com/jdluu/leafline/reader/ui/ReaderOverlay.kt`
**Secondary file:** `app/src/main/java/com/jdluu/leafline/reader/ui/ReaderBottomSheets.kt` (if > 400 lines)

**Verification:** `./gradlew :app:assembleDebug` compiles

---

### Task 6: Thin ReaderActivity.kt to orchestration

**Objective:** ReaderActivity keeps only lifecycle, field declarations, and wiring to the managers.

**Remaining in ReaderActivity.kt:**
- `onCreate()` — lifecycle, initialize managers
- `newIntent()` — companion factory
- `onTap()` — delegate to TapZoneHandler
- `onReadingPositionChanged()` — save position
- `onDecorationActivated()` — nav to annotation
- `onStop()` — push sync
- `onDestroy()` — cleanup
- Field declarations for all managers
- `EpubNavigatorFragment.Listener` overrides that delegate

**Target:** ~250 lines

**Verification:**
- `./gradlew :app:assembleDebug` compiles
- `./gradlew :app:testDebugUnitTest` same 8 pre-existing failures
- Connected test: `ReaderFlowTest` passes

---

### Task 7: Update imports in MainActivity.kt and ReaderFlowTest.kt

**MainActivity.kt:** `ReaderActivity.newIntent()` — no change needed (same package path)
**ReaderFlowTest.kt:** `ReaderActivity::class.java` — no change needed (same class name)

**Verification:** `./gradlew :app:connectedDebugAndroidTest -Dandroid.testInstrumentationRunnerArguments.class=com.jdluu.leafline.ReaderFlowTest` passes

---

### Task 8: Verify all tests pass

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Expected: 253 unit tests (8 pre-existing Readium failures), 22+ instrumented tests (0 failures)

---

## Risks and tradeoffs

- **Risk:** Extract-and-import errors — the compiler catches all of these, and we verify with `assembleDebug` after each task. No behavioral regression possible.
- **Risk:** Manager classes need access to `Activity` context or `FragmentActivity` APIs — mitigated by passing `() -> navigator` lambdas and keeping the managers as pure Kotlin classes where possible.
- **Tradeoff:** The managers hold `MutableState` fields, which means they're tied to the activity lifecycle. This is the same as the current code. A future `ViewModel` migration would clean this up further.
- **Tradeoff:** The UI composables in `ReaderOverlay.kt` will still be large (~500 lines). Further splitting into `ReaderToolbar.kt`, `ReaderBottomSheets.kt`, `ReaderSettingsPanel.kt` is deferred to a follow-up refactor.

## Verification

After all tasks:
1. `./gradlew :app:assembleDebug` — ✅
2. `./gradlew :app:testDebugUnitTest` — same 8 failures, no new ones
3. Connected test: `ReaderFlowTest` — 3/3 green
4. Manual smoke test: open any EPUB, verify page turns, bookmarks, highlights, search, settings, sync all work