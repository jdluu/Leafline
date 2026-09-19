# Readium OPDS 3.3.0 API Notes

Verified against the resolved `readium-opds:3.3.0` and `readium-shared:3.3.0` API jars during the Leafline Milestone 4 spike.

## Parser entry points

- `OPDS1Parser.Companion.parseRequest(HttpRequest, HttpClient)` returns `Try<ParseData>` and performs the HTTP request through the supplied shared `HttpClient`.
- `OPDS1Parser.Companion.parseUrlString(String, HttpClient)` is available when no custom request headers are required.
- `OPDS1Parser.Companion.parse(ByteArray, Url)` parses already-fetched OPDS XML bytes.
- `ParseData.feed` is nullable and must be checked before use.
- `Feed.navigation` is `List<Link>` for root navigation feeds.
- `Link.title` is nullable; `Link.href.toString()` provides the resolved URL string.

## HTTP request construction

- `DefaultHttpClient` implements `org.readium.r2.shared.util.http.HttpClient`.
- `HttpRequest` is constructed with an `AbsoluteUrl` and a builder lambda.
- `AbsoluteUrl(String)` returns a nullable value; reject invalid catalog URLs explicitly.
- Kotlin exposes the builder's Java `setMethod` as property assignment: `method = HttpRequest.Method.GET`.
- Use `setHeader("Authorization", "Basic <base64>")` for Grimmory OPDS Basic Auth and set an appropriate `Accept` header.

## Test environment

`OPDS1Parser.parse` touches `android.net.Uri`. Plain JVM tests without Android framework support fail with `Method parse in android.net.Uri not mocked`. Use Robolectric for fixture-based JVM parser tests, or move them to `androidTest`. Robolectric 4.14.1 worked with the Leafline toolchain.

The parser resolves relative fixture links against the supplied base URL. Assertions should expect absolute URLs, e.g. `http://localhost:6060/api/v1/opds/catalog`, not only the relative path.

## Grimmory protocol facts

From the official Grimmory OPDS documentation:

- Root: `/api/v1/opds`
- Full catalog: `/api/v1/opds/catalog`
- Recent: `/api/v1/opds/recent`
- Libraries, shelves, authors, series feeds are available.
- Search uses `/api/v1/opds/catalog?q={terms}`.
- Acquisition feeds accept `page` and `size` (`size` max 100 in the documented behavior).
- OPDS credentials are separate OPDS user accounts and use HTTP Basic Auth.

Do not place real credentials or private server URLs in fixtures or logs.
