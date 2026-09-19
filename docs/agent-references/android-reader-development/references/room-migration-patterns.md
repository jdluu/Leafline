# Room Migration Patterns (Leafline)

Pattern for adding columns or tables incrementally without destructive
migration (`fallbackToDestructiveMigration`). Used for MIGRATION_5_6
(koreaderHash + lastReadAtEpochMillis), MIGRATION_6_7 (collections table +
junction), MIGRATION_7_8 (readingStatus column).

## Adding a new column

```kotlin
// 1. Add field to Entity
@Entity(tableName = "books")
data class BookEntity(
    // ... existing fields ...
    val readingStatus: String = "unread"  // NEW: default value
)

// 2. Bump version in @Database annotation
@Database(entities = [...], version = 8, ...)

// 3. Create migration
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE books ADD COLUMN readingStatus TEXT NOT NULL DEFAULT 'unread'"
        )
    }
}

// 4. Add to .addMigrations() in build()
.addMigrations(MIGRATION_1_2, MIGRATION_2_3, ..., MIGRATION_7_8)
```

**Key rules:**
- New columns must have a DEFAULT value (existing rows need a value).
- Use `TEXT`, `INTEGER`, or `REAL` — Room's SQLite types. Do NOT use
  `BOOLEAN` (use `INTEGER` with 0/1) or Java/Kotlin object types.
- `NOT NULL DEFAULT ...` is required for columns added to existing tables
  because old rows won't have the column.
- ALTER TABLE ADD COLUMN in SQLite can only add columns at the end, not
  in arbitrary positions. Room doesn't care about column order, but the
  limitation matters if you read raw DB dumps.

## Adding a new table

```kotlin
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Standalone table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `collections` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        
        // Junction table for many-to-many
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `book_collection_cross_ref` (
                `collectionId` INTEGER NOT NULL,
                `bookStableId` TEXT NOT NULL,
                PRIMARY KEY(`collectionId`, `bookStableId`)
            )
        """.trimIndent())
        
        // Index for junction reverse-lookup
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_bcc_bookStableId`
            ON `book_collection_cross_ref` (`bookStableId`)
        """.trimIndent())
    }
}
```

## Version numbering

- Each migration goes up by exactly 1 (N → N+1).
- The `@Database(version = N)` always matches the latest migration target.
- Do NOT skip versions — each migration is applied sequentially.
- Never remove old migrations — they're needed for users upgrading from
  old versions.

## If a migration is missing and you hit a crash

Room throws `IllegalStateException: A migration from N to N+1 was required
but not found` on any database build. Fix: create the migration, add it to
`.addMigrations()`, and reinstall.

## No destructive fallback

Leafline does NOT use `.fallbackToDestructiveMigration()`. Every schema
change gets a real migration. This keeps user data intact across upgrades.