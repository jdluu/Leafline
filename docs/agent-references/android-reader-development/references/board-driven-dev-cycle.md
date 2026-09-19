# Board-Driven Active Development Cycle

Executed 7 times in a single session (issues #16–#20, #23–#24, Leafline project).
This is the per-issue loop for implementing features tracked on a GitHub Project
board (kanban style: Todo → In Progress → Done).

## The cycle (one session, many issues)

```text
1. PICK next Todo from board
2. BRANCH from main:   git checkout -b feat/issue-N-title
3. IMPLEMENT across all layers:
   - Room Entity (new table or column)
   - Room migration (ALTER TABLE / CREATE TABLE)
   - DAO queries
   - DataSource interface + Room implementation + InMemory implementation
   - Repository interface + implementation
   - ViewModel StateFlows + CRUD methods
   - Compose Screen / composables (new sheets, tiles, rows)
   - Call-site integration (DependencyHolder, launcher wires)
4. VERIFY: grep checks across ALL modified files for every expected symbol
   (not Gradle — too slow for iterative dev without toolchain)
5. TEST: unit test for domain models / enums
6. COMMIT: conventional commit with "Closes #N" in body
7. PUSH + PR:  gh pr create → gh pr merge --squash --delete-branch
8. UPDATE BOARD: GraphQL mutation marking item Done
9. LOOP: pick next Todo item
```

## Layer insertion points for a new Android feature

| Layer | What to add | Example from this session |
|-------|-------------|--------------------------|
| `Entity` | `@Entity` class, new columns, `fromLibraryBook`/`toLibraryBook` | `CollectionEntity`, `BookEntity.readingStatus` |
| `Migration` | `val MIGRATION_N_N+1`, bump `version`, add to `.addMigrations()` | `MIGRATION_6_7`, `MIGRATION_7_8` |
| `Dao` | `@Query` methods (CRUD, filter, lookup) | `CollectionDao`, `BookDao.setReadingStatus` |
| `DataSource` | Interface + Room impl + InMemory impl | `BookDataSource.setReadingStatus/getBooksByReadingStatus` |
| `Repository` | Interface method + impl delegation to DataSource | `LibraryRepository.setReadingStatus` |
| `ViewModel` | `MutableStateFlow` + `StateFlow` exposure + CRUD methods | `_readingStatusFilter`, `setBookReadingStatus` |
| `Screen` | Composable for the new UI surface | `BookDetailSheet`, `SyncConflictSheet`, filter chip rows |
| `Call site` | Wire into activity, dependency holder, launcher | `LeaflineDependencyHolder`, `MainActivity.importEpub` |

## Board Done via GraphQL (when gh merge doesn't auto-close)

```python
import json, subprocess
# Find item id for issue N
r = subprocess.run(['gh','api','graphql','-f',...], capture_output=True, text=True)
nodes = json.loads(r.stdout)['data']['viewer']['projectV2']['items']['nodes']
item_id = next(n['id'] for n in nodes if n['content']['number'] == N)
# Mark Done
payload = {
    "query": 'mutation($p:ID!,$i:ID!){ updateProjectV2ItemFieldValue(input:{...}){...} }',
    "variables": {"p": "PROJECT_ID", "i": item_id}
}
# "Done" option ID: "98236657", field ID: "PVTSSF_lAHOBPhPDc4BhPN8zhgLrW4"
```

## Pitfalls

- `git reset --hard origin/main` destroys uncommitted work — only safe when on a
  fresh branch checkout. The PR merge + branch delete + main reset sequence is:
  `gh pr merge N --squash --delete-branch && git checkout main && git fetch origin
  && git reset --hard origin/main && git checkout -b feat/issue-M-title`
- Board GraphQL requires the exact `projectId` and `fieldId` — these are stable
  per project but need to be looked up once. For Leafline project #7:
  `projectId: "PVT_kwHOBPhPDc4BhPN8"`, `fieldId: "PVTSSF_lAHOBPhPDc4BhPN8zhgLrW4"`,
  `doneOptionId: "98236657"`
- When PR title matches conventional commit form and body says "Closes #N", merge
  to main auto-closes the issue but does NOT move the board item — the GraphQL
  mutation is still needed.