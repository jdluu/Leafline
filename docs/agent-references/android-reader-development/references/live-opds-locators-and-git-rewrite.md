# Session notes: OPDS live interop, locator persistence, history rewrite

Condensed, verified details from a full Leafline build-out session (Aug 2026).
All items below were executed and observed working, not theorized.

## Live Grimmory OPDS end-to-end verification

- Endpoint reachable via Tailscale Funnel hostname; unauthenticated request
  returns `401` with `WWW-Authenticate: Basic realm="Grimmory OPDS"`.
- Root feed (`/api/v1/opds`) is titled "Booklore Catalog" and returns six
  navigation entries: All Books, Recently Added, Libraries, Shelves,
  Magic Shelves, Authors. Acquisition feeds carry `opensearch:totalResults`
  (481 books in this instance) and per-book links shaped like
  `/api/v1/opds/{id}/download?fileId={id}` with `type="application/epub+zip"`.
- The regular web-login credential secret returned 401 on OPDS; only the admin
  credential (stored in BWS as `username:password`) authenticated. When OPDS
  auth fails with valid-looking credentials, suspect account-type separation
  before suspecting code.
- Full verified flow on a Pixel 7: root browse -> drill into All Books ->
  select acquisition -> download 1.4MB EPUB with Basic Auth -> EpubImporter
  (SHA-256 + Readium metadata) -> Room insert -> book visible in Library.

## Compose TextField vs adb input (major time sink)

`adb shell input text` into Jetpack Compose TextFields is unreliable:

- Tapping field B then typing often keeps typing into field A (focus does not
  move as expected); text concatenates into one field.
- `KEYCODE_TAB` inserts a literal tab character instead of moving focus
  between Compose fields.

Working workaround for automated device setup: register a debug-only
BroadcastReceiver in the activity that writes an in-memory config object, and
set it via `adb shell am broadcast -a <action> --es key value`. Requirements
that cost debugging time:

- Use `Context.RECEIVER_EXPORTED`, not `RECEIVER_NOT_EXPORTED`; ADB-originated
  broadcasts are treated as external and NOT_EXPORTED receivers never fire
  (silently - broadcast completes with result=0 but the receiver body does not
  run).
- An explicit component (`-n pkg/.MainActivity`) is not required once exported;
  plain action-based broadcast works.
- Verify receipt by watching for the receiver's Toast in logcat / WindowManager
  surface creation before proceeding with UI taps.

## Readium 3.3.0 locator persistence (verified signatures)

From javap on the Gradle-cache `-api.jar` files:

- `EpubNavigatorFactory.createFragmentFactory(initialLocator: Locator?,
  links: List<Link>, preferences: EpubPreferences, listener, paginationListener,
  configuration)` with `$default` overloads - named args work in Kotlin.
- `Navigator.getCurrentLocator()` returns `StateFlow<Locator>` - Kotlin
  property access is `navigator.currentLocator`. There is no
  `currentLocation()` function.
- Serialization is org.json based, NOT Gson: `locator.toJSON(): JSONObject`
  and `Locator.Companion.fromJSON(JSONObject, WarningLogger?)` (nullable
  second param has a `$default`). Store `toJSON().toString()` as TEXT.
- Locator lives at `org.readium.r2.shared.publication.Locator`.

## Room schema evolution pattern

Replace `fallbackToDestructiveMigration()` with explicit migrations as soon as
real devices hold data. Verified shape:

```kotlin
@Database(entities = [BookEntity::class], version = 2, exportSchema = true)
abstract class LeaflineDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN lastLocatorJson TEXT")
            }
        }
        fun build(context: Context) = Room.databaseBuilder(...)
            .addMigrations(MIGRATION_1_2).build()
    }
}
```

When adding an interface method to `BookDataSource`, every implementation needs
it - including the in-memory interim source used by unit tests.

## Git history rewrite of one file across all commits

To replace a file's content everywhere in history (e.g. sanitizing a README
before making a repo public):

1. Backup first: `git bundle create backup.bundle --all` and verify it.
2. Precompute the replacement blob: `NEW=$(git hash-object -w newfile)`.
3. `git filter-branch -f --index-filter "git update-index --add --cacheinfo
   100644,$NEW,README.md" -- --all`.
   - PITFALL: `--cacheinfo` requires the comma form `<mode>,<sha>,<path>`; the
     space-separated three-argument legacy form errors out inside index-filter.
   - Piping through `xargs` inside the filter also breaks; expand the variable
     into the filter string instead.
4. Cleanup: delete `refs/original/*`, expire reflogs, `git gc --prune=now`.
5. Force-push carefully: if you deleted `refs/remotes/origin/main` during
   cleanup, plain `--force-with-lease` rejects with "(stale info)" because
   there is no lease expectation. `git fetch origin main` first, then push
   with the explicit lease:
   `git push --force-with-lease=refs/heads/main:<old-sha> origin main`.
6. Post-verify: remote SHA equals local HEAD; loop over `git rev-list` showing
   the file from each commit to confirm exactly one blob remains; grep all
   historical versions for removed markers.

Note: force pushes may require interactive approval in some agent harnesses;
DNS blips can also fail a push non-destructively - retry after re-fetching.

## Device test hygiene

- A locked phone (PIN/keyguard) makes UiAutomator tests fail with NPEs
  (`UiObject2.click()` on null) because app views are invisible through the
  shade/bouncer. Check lock state before blaming code:
  `adb shell dumpsys window | grep mCurrentFocus` shows NotificationShade /
  AlternateBouncerView when locked. Only the user can supply the PIN.
- `adb shell cmd statusbar collapse` fixes a stuck-open notification shade.
- After back-pressing out of the app during scripted walks, re-launch with
  `am start -n pkg/.MainActivity` rather than assuming monkey restores state.
