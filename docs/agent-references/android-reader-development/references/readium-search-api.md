# Readium 3.3.0 In-Book Search API (verified 2026-08-22, Leafline commit 3248e30)

Verified against extracted `readium-shared-3.3.0.aar` classes (javap) and
on-device testing with Pride and Prejudice EPUB on Pixel 7.

## API surface

### Publication.search() extension function

```kotlin
import org.readium.r2.shared.publication.services.search.search

// Returns SearchIterator? (null if publication is not searchable)
val iterator: SearchIterator? = publication.search(query)
```

The `search` extension is defined in `SearchServiceKt` (file
`SearchService.kt`). It delegates to the publication's `SearchService`
if one is registered. Not all publications are searchable; always null-check.

### SearchIterator

```kotlin
interface SearchIterator : Closeable {
    val resultCount: Int?  // null if unknown until iteration completes
    suspend fun next(): Try<Result>
    fun close()
}
```

`next()` returns `Try<SearchIterator.Result>` where:
- `failureOrNull()` returns `SearchError?` (Engine or Reading)
- `getOrNull()` returns `SearchIterator.Result.Collection?` (null when iteration is done)

### SearchIterator.Result.Collection

```kotlin
class Result {
    data class Collection(val locators: List<Locator>)
}
```

Each `Locator` in `locators` contains:
- `locator.href` — chapter/resource href
- `locator.title` — section title (nullable)
- `locator.text` — `Locator.Text?` with `.before`, `.highlight` (the match), `.after`

### SearchError

```kotlin
sealed class SearchError {
    data class Engine : SearchError   // search engine failed
    data class Reading : SearchError  // part of the book could not be read
}
```

## Import paths

```kotlin
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.search.SearchError
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search  // extension
```

All search APIs require `@OptIn(ExperimentalReadiumApi::class)`.

## Integration pattern: BookSearcher

A coroutine-based wrapper that runs search on Dispatchers.IO, streams results
into a `StateFlow`, and supports cancellation:

```kotlin
class BookSearcher(
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val searchFactory: suspend (query: String) -> SearchIterator?,
)
```

Key design decisions:
1. `start(rawQuery)` normalizes the query, cancels any prior search, launches
   a new coroutine on the background dispatcher.
2. `drainResults()` calls `iterator.next()` in a loop, maps each `Locator` to
   a UI row model, publishes incremental state updates.
3. `locatorFor(resultId)` maps back from UI row ID to the original `Locator`
   for `navigator.go(locator, false)` navigation.
4. `reset()` cancels the current search and clears state.
5. `publish()` checks `job.isActive` before writing state to avoid stale
   coroutine emissions overwriting newer search results.

## UI integration

The search sheet is a `ModalBottomSheet` with:
- `OutlinedTextField` with `ImeAction.Search` keyboard action
- Debounced auto-search: `LaunchedEffect(query)` with 300ms delay before
  calling `onSubmitSearch(normalizedQuery)`
- `BookSearchQuery.normalize(raw)` trims and collapses whitespace, returns
  null for blank queries (used for both debounce dedup and start guard)
- Results in a `LazyColumn`, each row shows section title + annotated snippet
  with the match in bold/primary color
- Status indicator: `LinearProgressIndicator` while searching, match count
  text when complete, error text when failed

## Locator text mapping

```kotlin
fun mapLocator(id: Int, locator: Locator): BookSearchResult {
    val text = locator.text
    return BookSearchResult(
        sectionTitle = locator.title ?: locator.href.toString().substringAfterLast('/'),
        excerptBefore = text?.before.orEmpty(),
        excerptMatch = text?.highlight.orEmpty(),
        excerptAfter = text?.after.orEmpty(),
        locatorJson = locator.toJSON().toString()
    )
}
```

The `locatorJson` is stored so navigation can fall back to
`Locator.Companion.fromJSON(JSONObject(json))` if the in-memory locator map
was cleared.

## On-device verification

1. Install APK, open a book, toggle toolbar.
2. Tap "Search in book" icon (content-desc).
3. UIAutomator dump confirms "Search in book" header + "Find in this book"
   placeholder.
4. Type query via `adb shell input text "Bennet"`.
5. Results stream in within 1-2 seconds showing section titles and snippets.
6. Tapping a result closes the sheet and navigates the reader.
