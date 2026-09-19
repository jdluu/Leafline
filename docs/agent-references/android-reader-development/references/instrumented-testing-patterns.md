# Instrumented test patterns (connectedDebugAndroidTest)

Validated 2026-08-23 on Pixel 7 + Pixel 9 Pro XL via Leafline PRs #60 (reader
flow) and #61 (library benchmark). Complements `on-device-verification-workflow.md`.

## Running

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew :app:connectedDebugAndroidTest   # full suite
./gradlew :app:connectedDebugAndroidTest \
  -Dandroid.testInstrumentationRunnerArguments.class=com.jdluu.leafline.ReaderFlowTest  # one class
```

- **With multiple devices attached, Gradle runs the suite on ALL of them in
  parallel.** The UiAutomator-based SAF-picker test flaked ("Could not launch
  intent ... main thread has not gone idle") only when two devices ran at once;
  each device passed in isolation. Run one class at a time or detach extra
  devices before declaring failures real.
- Results XML:
  `app/build/outputs/androidTest-results/connected/**/*.xml` — parse for
  `<failure>`/`<error>` children per `<testcase>`; build output alone is noisy.
- Benchmark numbers land in logcat under a custom tag:
  `adb logcat -d -s <Tag>:I` after the run.

## Test fixture patterns

### Bundling fixtures (androidTest assets)

Files under `app/src/androidTest/assets/` are packaged into the **androidTest
APK**, not the app APK. Read them with the instrumentation's own context:

```kotlin
// WRONG — targetContext reads the APP apk's assets -> FileNotFoundException
InstrumentationRegistry.getInstrumentation().targetContext.assets.open("test-book.epub")
// RIGHT
InstrumentationRegistry.getInstrumentation().context.assets.open("test-book.epub")
```

Then copy to `targetContext.filesDir` when the code-under-test needs a real
file path (e.g. ReaderActivity takes a file path extra).

### JUnit4 + coroutines

`@Before fun setUp() = runBlocking { ... }` **fails class instantiation** with
`InvalidTestClassError: Method setUp() should be void` — expression bodies make
the method return the coroutine result, which JUnit4 rejects. Use:

```kotlin
@Before fun setUp() { runBlocking { setupFixture() } }
private suspend fun setupFixture() { ... }
```

Same rule applies to `@Test` methods used as benchmarks: keep them
`fun x() = runBlocking { }` is fine for @Test (JUnit allows non-void there? no —
use the setUp split if you see the same error on a test method).

### Benchmark methodology

- Always run a **warm-up pass** before timing Room queries: cold first
  `getAllBooks().first()` measured 131ms for 100 books; warm steady-state was
  51ms. Asserting on the cold number produces false regressions.
- Memory-leak checks: `Runtime.getRuntime().gc()` before AND after the loop,
  compare used-memory delta, recycle every Bitmap explicitly.
- Generate deterministic EPUB fixtures with Python (zipfile, STORED mimetype
  first entry, then META-INF/container.xml + OEBPS/*). See
  `app/src/androidTest/assets/test-book.epub` provenance in-repo.

### Reader flow test shape

UiAutomator against the real Readium navigator: launch MainActivity first so
Room initializes, copy EPUB to filesDir, start ReaderActivity with the file-path
extra, wait up to 20s for chapter text (`Until.hasObject(By.textContains(...))`)
since WebView render is slow on first open.
