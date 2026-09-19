# Verified Toolchain and BWS-Backed OpenCode Pattern

This reference records a reusable setup pattern validated during an Android reader
bootstrap on Debian 13.

## Toolchain

The working compatibility set was:

- OpenJDK 21
- Android Gradle Plugin 9.3.0
- Gradle 9.7.1 wrapper
- Android SDK Platform 36
- Android Build Tools 36.0.0
- Android Platform Tools 37.0.1

Android's AGP 9.3 guidance indicated Gradle 9.5+ and JDK 17+; JDK 21 was used.
The project initially used AGP 8.13 with Gradle 9.7.1 and failed during plugin
configuration because an internal Gradle API had been removed. After upgrading
AGP, the old `org.jetbrains.kotlin.android` plugin failed because AGP 9 no longer
requires it, so it was removed. Re-check this behavior on future upgrades.

## Build verification

Use the checked-in wrapper:

```bash
./gradlew help --no-daemon
./gradlew test --no-daemon
./gradlew lint --no-daemon
./gradlew assembleDebug --no-daemon
```

A baseline shell produced successful `help`, `test`, `lint`, and `assembleDebug`
results. The test task reported `NO-SOURCE`, which means no tests existed yet and
must not be described as behavioral coverage. The debug APK was then verified to
exist under `app/build/outputs/apk/debug/`.

## BWS-backed OpenCode

OpenCode's OpenRouter provider recognizes `OPENROUTER_API_KEY`. It does not
infer that `OPENCODE_OPENROUTER_API_KEY` should be used. A safe launcher can:

1. require `BWS_ACCESS_TOKEN`;
2. run `bws secret list -o json`;
3. select `OPENCODE_OPENROUTER_API_KEY` by exact key name;
4. export it as `OPENROUTER_API_KEY` only for the child process;
5. `exec opencode` without writing the key to disk.

Smoke-test with a bounded, non-mutating prompt and machine-readable output. Verify
exit code zero, expected response text, and no provider-auth error. Do not print
the key or store it in repository files. OpenCode `/connect` or an untracked local
`.env` are fallbacks, but BWS is preferable when it is already authoritative.

## Sources

- https://developer.android.com/build/releases/gradle-plugin
- https://developer.android.com/tools/sdkmanager
- https://gradle.org/releases
- https://opencode.ai/docs/providers
