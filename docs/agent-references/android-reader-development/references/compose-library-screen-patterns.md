# Compose Library Screen UI Patterns

Patterns for the library (book-grid) screen that emerged from implementing
collections (#16), reading status (#17), continue-reading shelf (#18),
book detail sheet (#20), and sync conflict sheet (#24).

Distinct from the reader overlay/settings patterns in `reader-ui-patterns.md`
and `reader-settings-and-config-patterns.md`.

## Filter chip rows (LazyRow + FilterChip)

Use `FilterChip` inside `androidx.compose.foundation.lazy.LazyRow` for
horizontally scrollable filter bars. Pattern:

```kotlin
androidx.compose.foundation.lazy.LazyRow(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    item {
        FilterChip(
            selected = selectedFilter == null,
            onClick = { viewModel.setFilter(null) },
            label = { Text("All") }
        )
    }
    items(filterItems) { item ->
        FilterChip(
            selected = selectedFilter == item.id,
            onClick = { viewModel.setFilter(item.id) },
            label = { Text(item.name) },
            trailingIcon = if (selectedFilter == item.id) {
                { Icon(Icons.Default.Close, ..., modifier = Modifier.size(16.dp)) }
            } else null
        )
    }
}
```

Used for: collection filter chips (#16), reading status filter chips (#17).

## CombinedClickable for long-press on book tiles

Replace `Modifier.clickable` with `Modifier.combinedClickable` to add
long-press for a detail sheet. Requires `@OptIn(ExperimentalFoundationApi::class)`.

```kotlin
@OptIn(ExperimentalFoundationApi::class)
Card(
    modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClickLabel = "Open book",
            onClick = onClick,
            onLongClickLabel = "Book details",
            onLongClick = onLongClick
        )
)
```

Used for: book detail sheet (#20).

## ModalBottomSheet detail sheets (three-section layout)

A detail sheet typically has three visual sections: cover/title at the top,
metadata rows in the middle, actions at the bottom. Pattern:

```kotlin
ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
        modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        // Section 1: Cover + title + primary action
        Row { /* cover thumbnail, title, authors, primary button */ }
        HorizontalDivider()

        // Section 2: Metadata rows (label/value pairs)
        Text("Metadata", style = MaterialTheme.typography.titleSmall)
        MetadataRow("Status", statusString)
        MetadataRow("Publisher", book.publisher)

        HorizontalDivider()

        // Section 3: Actions
        Text("Actions", style = MaterialTheme.typography.titleSmall)
        // Reading status chips, collection chips, delete button
    }
}
```

Helpful helper for label/value rows:

```kotlin
@Composable
fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, ..., modifier = Modifier.width(80.dp))
        Text(value, ..., modifier = Modifier.weight(1f))
    }
}
```

Used for: `BookDetailSheet` (cover + metadata + actions + delete, #20),
`SyncConflictSheet` (local/remote + device info + jump/stay, #24),
`CollectionManagementSheet` (list + new + delete, #16).

## Reading status badge on book tile

When `book.readingStatus != ReadingStatus.UNREAD`, show a small colored
badge below the cover using `MaterialTheme.colorScheme.primary` (Reading)
or `.secondary` (Finished).

## Continue reading shelf

Horizontal scrolling row of compact cards (120dp wide) showing cover +
title, filtered to books with saved locators. Wire as a separate StateFlow
in the ViewModel, distinct from the main grid sortedBooks flow.

Data source: Room query `WHERE lastLocatorJson IS NOT NULL ORDER BY
lastReadAtEpochMillis DESC LIMIT 10`.

## SyncConflictSheet (side-by-side percentage)

ModalBottomSheet with local vs remote percentage displayed side by side
using headMedium typography, plus device name and last-synced timestamp.
Action row: Stay (OutlinedButton) / Jump to remote (Button).

Backed by a MutableStateFlow<SyncConflictState?> on the Activity, where
SyncConflictState carries localPercentage, remotePercentage, remoteDevice,
remoteTimestamp, and onJump: () -> Unit.

## When to use ModalBottomSheet vs AlertDialog

| Content | Component |
|---------|-----------|
| Rich metadata + multiple action groups | ModalBottomSheet |
| Single yes/no confirmation | AlertDialog |
| Collection assignment picker | ModalBottomSheet with FilterChip toggles |
| Delete confirmation | AlertDialog with error-colored confirm |
| Progress sync comparison | ModalBottomSheet with side-by-side display |