# OpenCode Kotlin API error recovery

Use this reference when OpenCode produces Kotlin compilation errors it cannot self-diagnose during autonomous Android development.

## Pattern

OpenCode (laguna-xs-2.1) repeatedly produces Kotlin type errors that pass its internal reasoning but fail the actual compiler. Observed examples:

1. **String vs Char argument mismatch**: `trim(".", "_")` — Kotlin's `String.trim()` accepts `Char` varargs, not `String`. Fix: `trim('.', '_')`.
2. **Missing interface method override**: `class ReaderActivity : FragmentActivity(), EpubNavigatorFragment.Listener` fails because `onExternalLinkActivated(url: AbsoluteUrl)` is abstract and not implemented. OpenCode added the interface declaration but did not implement the required method.
3. **Missing `@OptIn` annotation**: Readium experimental APIs require `@OptIn(ExperimentalReadiumApi::class)` on the class or method. OpenCode may use the API without the annotation.

## Recovery procedure

1. Independently run `./gradlew compileDebugKotlin` (or `./gradlew assembleDebug`) and capture the exact compiler error.
2. Read the error message: it names the file, line, and the type mismatch or missing member.
3. Relaunch OpenCode with a prompt that explicitly names the exact fix:
   ```
   Fix the exact Kotlin error in <file>:<line>: <error message>.
   Use <correct API>. Then run ./gradlew test lint assembleDebug.
   ```
4. Do not tell OpenCode to "inspect and fix" — it may inspect broadly and miss the specific fix. Name the exact change.

## Prevention

Instruct OpenCode to compile after each edit, not just at the end. A prompt that says "compile after each change and correct all errors before proceeding" reduces the chance of accumulating type errors.

## Environment assessment failures

OpenCode may report "the Android SDK is not installed" even when `java -version`, `adb`, and `./gradlew assembleDebug` all work. This happens because OpenCode's sandboxed environment view may differ from the actual shell. Always verify toolchain availability independently rather than trusting the agent's environment assessment.
