# Room Persistence Setup for an Android Reader

Verified recipe for adding a Room-backed local book-metadata layer to a
Readium-based Android reader (AGP 9.3 / Kotlin 2.2.20 / Gradle 9.7.1 / JDK 21 /
compileSdk 36). Applies to any Android module that persists book metadata and
has instrumented database tests.

## Why the annotation-processing setup is the risk

Room generates `*Database_Impl` and DAO implementations at compile time. If the
generator does not run, the project still compiles and JVM unit tests pass, but
every database build call fails at runtime with:

```
Cannot find implementation for com.example.MyDatabase.
MyDatabase_Impl does not exist. Is Room annotation processor correctly configured?
```

Symptoms on device: the `@Before` that builds the in-memory database throws that
exception; the `@After` then throws `lateinit property database has not been
initialized` on every test, hiding the real failure.

## Correct Gradle wiring

Root `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.2" apply false
}
```

App module `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

dependencies {
    val roomVersion = "2.8.4"          // documented current Room 2.x line
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")   // NOT annotationProcessor
    implementation("com.google.code.gson:gson:2.10.1") // for author TypeConverter
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
```

`gradle.properties`:

```
android.disallowKotlinSourceSets=false
```

## KSP versioning rule

KSP publishes as `<kotlinVersion>-<kspVersion>`. For Kotlin 2.2.20 the current
KSP is `2.2.20-2.0.2`. A bare `2.0.21` or `2.0.0` will NOT resolve:

```
Plugin [id: 'com.google.devtools.ksp', version: '2.0.0'] was not found ...
```

Confirm the exact matching release from the official google/ksp releases page
before building. The KSP version must match the Kotlin compiler line, not an
arbitrary KSP revision.

## Built-in Kotlin source-set guard (AGP 9.3+)

KSP registers generated source dirs; AGP's built-in Kotlin rejects that with:

```
Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.
Kotlin source set 'debug' contains: [.../build/generated/ksp/debug/kotlin ...]
Solution: Use android.sourceSets DSL instead.
To suppress this error, set android.disallowKotlinSourceSets=false in gradle.properties.
```

Set the property (an experimental option; expect a warning). The build then runs
`kspDebugKotlin`, and Room exports/compiles normally.

## DEX-safe instrumented test names

JVM unit tests accept backtick method names with spaces, but DEX does not:

```
Space characters in SimpleName 'addBook stores book and returns id' are not allowed
prior to DEX version 040 (method name ... on class ...RoomBookDataSourceTest)
```

Rename every `androidTest` `@Test fun \`...\`()` to a camelCase identifier
(`addBook_stores_book_and_returns_id`).

## Verification — run the device tests

`./gradlew test` only runs `src/test` (JVM). Room database coverage lives in
`src/androidTest`, so run:

```
./gradlew test lint assembleDebug
./gradlew connectedDebugAndroidTest    # needs a connected device/emulator
```

The instrumented suite should cover:
- upsert by stableId (`@Insert(onConflict = REPLACE)`),
- lookup by stableId and by fileHash,
- ordering by addedAtEpochMillis DESC,
- full field round-trip (all columns, incl. special chars / non-ASCII authors),
- Flow re-emission on insert / update / delete.

## TypeConverter for list-valued fields

```kotlin
class Converters {
    private val gson = Gson()
    @TypeConverter fun fromAuthors(authors: List<String>): String = gson.toJson(authors)
    @TypeConverter fun toAuthors(json: String): List<String> =
        if (json.isEmpty()) emptyList()
        else gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
}
```

Register with `@TypeConverters(Converters::class)` on the `@Database` class.
Guard the `""` empty-string case in the reader direction. Add a round-trip test
with `O'Brien`, `Müller, Jürgen`, and CJK names to prove escaping survives.

## Domain-layer note

Keep the pure `LibraryBook` domain model and `BookMetadataMapper` (Readium
`Metadata` -> domain) free of Room annotations. Put `@Entity`/`@Dao`/`@Database`
and converters in a `data/local` package, with `fromLibraryBook`/`toLibraryBook`
as the bridge. Stable identity: non-blank `metadata.identifier` trimmed, else
SHA-256 file hash. This keeps persistence swappable (in-memory data source for
tests, Room for production) and keeps Readium out of the DB layer.