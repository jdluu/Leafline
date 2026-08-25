# Release builds

This document describes how to produce signed release builds of Leafline.
It is written for maintainers; end users should get builds from their
package repository.

## Signing keys

Signing keys are never committed to this repository (`.gitignore` already
excludes `*.jks` and `*.keystore`).

Generate a release keystore once:

```bash
keytool -genkeypair -v \
  -keystore leafline-release.jks \
  -alias leafline \
  -keyalg RSA -keysize 4096 -validity 10000
```

Store it somewhere safe and back it up. Losing it means losing the ability
to ship updates to existing installs under the same application id.

## Local signed builds

Add the credentials to `~/.gradle/gradle.properties` (never to the repo or
any file under version control):

```properties
LEAFLINE_STORE_FILE=/absolute/path/to/leafline-release.jks
LEAFLINE_STORE_PASSWORD=...
LEAFLINE_KEY_ALIAS=leafline
LEAFLINE_KEY_PASSWORD=...
```

Then build:

```bash
./gradlew :app:assembleRelease
```

With properties present this produces a signed APK at
`app/build/outputs/apk/release/`. Without them the build still succeeds and
produces `app-release-unsigned.apk`.

## F-Droid / Acres recipe builds

F-Droid and similar repositories build from source with **their own**
signing keys, so the recipe needs no secrets at all:

```yaml
- versionName: 0.2.0
  versionCode: 2
  commit: v0.2.0
  gradle:
    - yes
```

Requirements this repo satisfies for such recipes:

- All dependencies come from public Maven repositories (`mavenCentral`,
  Google's Maven, and Readium's Maven host); no proprietary SDK blobs.
- No prebuilt binaries are checked in; everything is built from source.
- `./gradlew :app:assembleRelease` works without any signing credentials,
  producing an unsigned APK the packager signs itself.
- The baseline profile in `app/src/main/baseline-prof.txt` is a plain text
  artifact compiled by the Android Gradle Plugin during the normal build;
  no extra tooling is needed.

## Versioning

Versions follow the scheme documented in [CHANGELOG.md](CHANGELOG.md):
`versionCode` increments monotonically per release, `versionName` follows
semver.
