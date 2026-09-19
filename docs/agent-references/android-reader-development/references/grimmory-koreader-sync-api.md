# Grimmory KOReader Progress Sync API (verified 2026-08-23, Leafline commit d6883ab)

Verified against the live Grimmory instance (v3.3.3) hosted on the local mini PC
at `127.0.0.1:6061`, and the public API documentation at https://grimmory.org/api.

## API endpoints

Base path: the KOReader API path configured in Grimmory Settings > Devices,
typically `http://<host>:<port>/api/koreader`.

### GET /users/auth — Authenticate

```
GET /api/koreader/users/auth
Authorization: Basic <base64(user:pass)>
```

- **200** — credentials valid (returns a JSON object with a token/key)
- **401** — invalid credentials or KOReader sync not enabled

Use this as a "Test connection" check in settings UI.

### GET /syncs/progress/{bookHash} — Get progress

```
GET /api/koreader/syncs/progress/{bookHash}
Authorization: Basic <base64(user:pass)>
```

- **200** — returns progress JSON:
  ```json
  {
    "timestamp": 1724390400,
    "document": "<bookHash>",
    "percentage": 42.5,
    "progress": "<opaque position string>",
    "device": "<device name>",
    "device_id": "<device UUID>"
  }
  ```
- **404** — no progress stored for this book

`timestamp` is in seconds (epoch). `percentage` is 0-100 float. `progress` is
an opaque string — KOReader sends a locator-like string; Leafline sends a
Readium Locator JSON string. `document` is the book hash.

### PUT /syncs/progress — Update progress

```
PUT /api/koreader/syncs/progress
Authorization: Basic <base64(user:pass)>
Content-Type: application/json

{
  "timestamp": 1724390400,
  "document": "<bookHash>",
  "percentage": 42.5,
  "progress": "<opaque position string>",
  "device": "Leafline",
  "device_id": "<UUID>"
}
```

- **200** — progress updated successfully

## Authentication

HTTP Basic Auth with **KOReader-specific credentials** configured in Grimmory
Settings > Devices. These are separate from the main Grimmory login and from
OPDS credentials. Both username and password are case-sensitive.

## Book matching — file content hash

Grimmory matches books by **file content hash**. The same physical file must
exist on both the server and the client (typically downloaded via OPDS).

### Hash convention

The documented KOReader convention is: **md5 of first 1024 bytes + md5 of
last 1024 bytes** for files larger than 1024 bytes; **full-file md5** for
files of 1024 bytes or fewer.

```kotlin
fun koreaderHash(file: File): String {
    val bytes = file.readBytes()
    return if (bytes.size <= 1024) {
        md5Hex(bytes)
    } else {
        md5Hex(bytes.copyOfRange(0, 1024)) + md5Hex(bytes.copyOfRange(bytes.size - 1024, bytes.size))
    }
}
```

**Important caveat:** Current KOReader master (as of 2026-08) uses an
exponential-sampling partial MD5 rather than the first+last-1024 scheme.
Grimmory's documented contract and the live server accept the first+last-1024
hash. If real KOReader devices produce different hashes against your server,
`FileHashUtil.koreaderHash` is the single place to adjust. Verify hash
compatibility with a real KOReader device if interoperability is required.

## Client integration pattern (Leafline)

### Data layer

- `BookEntity.koreaderHash` column (nullable, computed at import time)
- `BookEntity.lastReadAtEpochMillis` column (for newer-than-local comparison)
- Room migration adding both columns
- `FileHashUtil.koreaderHash(file)` — pure function, unit-tested with vectors

### Sync client

```kotlin
class KoreaderSyncClient(private val client: OkHttpClient) {
    suspend fun auth(baseUrl: String, user: String, pass: String): Boolean
    suspend fun getProgress(baseUrl: String, user: String, pass: String, bookHash: String): KoreaderRemoteProgress?
    suspend fun putProgress(baseUrl: String, user: String, pass: String, progress: KoreaderProgressPayload): Boolean
}
```

- `getProgress` returns null on 404 (no progress stored)
- `putProgress` returns false on non-200
- MockWebServer tests cover auth, parsing, body shape, and error codes

### Progress comparison

Remote timestamp is in **seconds**; local `lastReadAtEpochMillis` is in
**milliseconds**. Normalize before comparing:

```kotlin
fun isRemoteNewer(remote: KoreaderRemoteProgress, localMs: Long?, localPercentage: Double?): Boolean {
    val remoteMs = remote.timestamp?.let { 
        if (it < 1_000_000_000L) it * 1000 else it  // seconds -> ms heuristic
    } ?: return false
    if (remoteMs > (localMs ?: 0)) return true
    // Fallback: if timestamps are equal, compare percentage
    if (remoteMs == localMs && (remote.percentage ?: 0.0) > (localPercentage ?: 0.0)) return true
    return false
}
```

### Reader integration

- **On book open:** pull remote progress. If remote is newer, show a dialog
  offering to jump (parse `progress` string as a Readium Locator if possible;
  fall back to a toast with percentage if not).
- **On activity stop/pause:** push current locator + percentage. Use
  `NonCancellable` coroutine context so push completes even if the activity
  is being destroyed. Silent on success, toast on failure.

### Settings UI

Settings screen section "KOReader Sync":
- Server URL text field (e.g. `http://192.168.1.100:6061/api/koreader`)
- Username and password fields
- Enable toggle
- "Test connection" button (calls `auth()`, shows result)
- Credentials stored in-memory only (same pattern as OPDS credentials)

## Live server verification

```bash
# Check auth endpoint (should return 401 without creds, 200 with valid creds)
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:6061/api/koreader/users/auth
curl -s -o /dev/null -w "%{http_code}" -u <user>:<pass> http://127.0.0.1:6061/api/koreader/users/auth

# Push progress (requires valid creds)
curl -s -X PUT http://127.0.0.1:6061/api/koreader/syncs/progress \
  -H 'Content-Type: application/json' \
  -u <user>:<pass> \
  -d '{"timestamp":1724390400,"document":"<hash>","percentage":42.5,"progress":"x","device":"test","device_id":"id"}'

# Get progress
curl -s -u <user>:<pass> http://127.0.0.1:6061/api/koreader/syncs/progress/<hash>
```

## On-device verification

1. Install APK, go to Settings tab.
2. Verify "KOReader Sync" section appears with server URL, username, password
   fields and enable toggle.
3. Enter server URL. (Full round-trip requires valid KOReader sync credentials
   configured in Grimmory Settings > Devices.)
4. Open a book — pull attempt is silent if sync is disabled or unconfigured.
5. Close a book — push attempt is silent on success.
