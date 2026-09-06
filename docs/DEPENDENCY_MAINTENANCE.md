# Dependency maintenance

This document defines the review process for Leafline's Android dependency graph.
It is intentionally conservative because Readium Kotlin Toolkit is a central
compatibility constraint and the app is local-first.

## Ownership

- `build.gradle.kts` owns plugin versions and repository policy.
- `app/build.gradle.kts` owns direct application, test, and build dependencies.
- `gradle/libs.versions.toml` is not currently used; introducing a catalog is a
  separate refactor and must preserve the existing compatibility set.
- The Gradle wrapper owns the Gradle runtime version.

A resolved version is not automatically an approved upgrade. Gradle may select a
newer transitive version than the declared dependency. Review both values and
record intentional constraints in the owning build file when necessary.

## Inventory procedure

Run from the repository root:

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath \
  > /tmp/leafline-debug-runtime-classpath.txt
./gradlew :app:dependencyInsight \
  --configuration debugRuntimeClasspath \
  --dependency <group-or-module>
```

Review direct declarations in `app/build.gradle.kts`, then use `dependencyInsight`
for any transitive version drift, security advisory, or unexpected upgrade.
Never paste credentials, private URLs, or machine-local paths into a committed
report.

## Upgrade policy

For each proposed upgrade, record before editing:

1. The exact dependency and current declared and resolved versions.
2. The official release notes and compatibility requirements.
3. Readium, Android Gradle Plugin, Kotlin, Compose, Room, and min/target SDK impact.
4. The smallest verification set and rollback commit.
5. Whether the change is direct, transitive, runtime, test-only, or build-only.

Upgrade one compatibility family at a time. Do not combine a Readium upgrade with
an AGP/Kotlin or Compose upgrade unless an upstream compatibility requirement
makes that necessary.

## Supply-chain controls

- Repositories are restricted to Google and Maven Central by
  `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- Dependencies are resolved through the committed Gradle wrapper.
- Credentials and local signing configuration remain outside the repository.
- Dependency verification or locking should be introduced in a separate,
  reviewable change after confirming the generated metadata is reproducible in
  CI and on a clean checkout.

## Review gates

Every dependency change must pass:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

For changes affecting reader rendering, persistence, or lifecycle, also run the
relevant connected tests when a device is available. A connected-test failure
must identify whether it is introduced by the change, a baseline failure, or an
environment limitation; do not hide a baseline failure by weakening assertions.
