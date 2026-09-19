# Readium OPDS 1.2 Integration (readium-opds 3.3.0)

Verified recipe for parsing a Grimmory-style OPDS 1.2 root catalog feed with the
official Readium Kotlin `readium-opds` module in an Android reader. Covers the
exact API surface, the Kotlin-vs-`javap` gotchas, and how to fixture-test it.
Token of proven setup: AGP 9.3 / Kotlin 2.2.20 / Gradle 9.7 / JDK 21 /
compileSdk 36, Readium 3.3.0.

## Dependency

`app/build.gradle.kts`:

```kotlin
val readiumVersion = "3.3.0"
implementation("org.readium.kotlin-toolkit:readium-opds:$readiumVersion")
// readium-shared is already present and provides DefaultHttpClient + models
```

## Verified entry points (inspect the resolved -api.jar, don't guess)

To learn the real API surface, locate the resolved `readium-opds-3.3.0-api.jar`
under `~/.gradle/caches/9.7.1/transforms/*/transformed/` and run
`javap` on the classes. Do not guess from docs/memory.

```bash
javap -classpath <...readium-opds-3.3.0-api.jar> \
  org.readium.r2.opds.OPDS1Parser \
  same-for-'OPDS1Parser$Companion', 'org.readium.r2.shared.opds.ParseData',
  'org.readium.r2.shared.opds.Feed', 'org.readium.r2.shared.publication.Link'
```

Key facts found by `javap` for 3.3.0:

- `OPDS1Parser.Companion.parseRequest(HttpRequest, HttpClient)` -> `Try<ParseData>`
  (this is the suspend-parsing path used for a real fetch)
- `OPDS1Parser.Companion.parse(byte[], Url)` -> `ParseData` (parse raw XML bytes,
  used for fixture tests)
- `ParseData.feed` -> `Feed` (nullable in Kotlin; use `!!` or safe-call)
- `Feed.navigation` -> `List<Link>` — the root-catalog navigation entries
- `Feed.publications` / `Feed.facets` for acquisition/grouped feeds
- `Link.title: String?`, `Link.href: Href` — `href.toString()` is the URL, and
  readium **resolves relative hrefs against the base URL**, so a feed's
  `/api/v1/opds/catalog` becomes `http://host:port/api/v1/opds/catalog`.
- `HttpRequest(url) { ... }` builder DSL; `HttpRequest(url) { method = Method.GET }`
- `Try` is abstract; use the Companion factories `Try.success(value)` /
  `Try.failure(error)`, and access via `.getOrNull()`, `.isSuccess`, `.isFailure`
  (those are Kotlin properties, not functions).
- `AbsoluteUrl(urlString)` is **nullable** — add `?: throw`.

## Kotlin-vs-javap gotchas (these cost real compile errors)

- The `HttpRequest` builder exposes the HTTP method as a Kotlin **property**
  (`method = Method.GET`), even though `javap` shows a `setMethod` Java setter.
  Calling `setMethod(...)` from Kotlin is an unresolved reference — use property
  assignment inside the builder lambda.
- Many nullable Kotlin types surface as platform types from `javap`; LLM-written
  code reliably under- or over-nulls them. Compile, read the exact
  "Argument type mismatch" / "Only safe (?.) calls allowed" errors, then fix.
- `ParseData.feed` is nullable in Kotlin (`Feed?`), so `parseData.feed.navigation`
  must be `parseData.feed!!.navigation` (or a safe call).

## Service shape (HTTP Basic Auth)

```kotlin
class OpdsCatalogService(private val client: HttpClient = DefaultHttpClient()) {
    private fun basicAuthHeader(c: OpdsServerConfig): String {
        val raw = "${c.username}:${c.password}"
        val enc = Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
        return "Basic $enc"
    }
    suspend fun fetchRootNavigation(c: OpdsServerConfig) = withContext(Dispatchers.IO) {
        try {
            val url = AbsoluteUrl(c.catalogUrl) ?: throw IllegalArgumentException("bad url")
            val req = HttpRequest(url) {
                method = Method.GET
                setHeader("Authorization", basicAuthHeader(c))
                setHeader("Accept", "application/atom+xml")
            }
            val res = OPDS1Parser.parseRequest(req, client)
            val data = res.getOrNull()
            if (data == null) Try.failure(res.failureOrNull() ?: Exception("parse failed"))
            else Try.success(data.feed!!.navigation.map { OpdsNavigationEntry(it.title ?: it.href.toString(), it.href.toString()) })
        } catch (e: Exception) { Try.failure(e) }
    }
}
```

`DefaultHttpClient` from `readium-shared` satisfies the `HttpClient` interface, so
no extra Ktor/OkHttp dependency is needed for OPDS fetching.

## Fixture test (Robolectric required)

`readium-opds` internally calls `android.net.Uri.parse`, which is NOT mocked in a
plain JVM unit test and throws `Method parse in android.net.Uri not mocked`. Fix
with Robolectric:

```kotlin
// app/build.gradle.kts
testImplementation("org.robolectric:robolectric:4.14.1")
testImplementation("androidx.test:core:1.6.1")
```

```kotlin
@RunWith(RobolectricTestRunner::class)
class OpdsCatalogServiceTest {
    // load a fixture OPDS 1.2 root feed from src/test/resources
    // call service.parseXml(xml, "http://localhost:6060/api/v1/opds")
    // assert nav titles + RESOLVED absolute hrefs (http://localhost:6060/api/v1/opds/catalog ...)
}
```

Place the Grimmory-style fixture under
`app/src/test/resources/grimmory-root-feed.xml`. It should carry a `<feed>`
with `<entry>` blocks each containing a `<title>` and a
`<link rel="subsection" href="..." type="application/atom+xml;profile=opds-catalog;kind=..."/>`.

Also use `java.util.logging.Logger` (not `android.util.Log`) in the service so
JVM tests do not hit the "Log not mocked" wall even before Robolectric runs.

## Grimmory OPDS facts (from grimmory.org/docs/integration/opds)

- Root catalog: `/api/v1/opds`; HTTP Basic Auth with OPDS-specific user accounts
  (separate from the web login).
- Feeds: `/catalog`, `/recent`, `/libraries`, `/shelves`, `/authors`, `/series`,
  `/surprise`, and search `/catalog?q={terms}`.
- Pagination: `page` (default 1), `size` (default 50, max 100).
- URL pattern includes an instance port (e.g. `http://host:6060/api/v1/opds`).
