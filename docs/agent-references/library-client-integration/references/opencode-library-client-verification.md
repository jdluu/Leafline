# OpenCode-Piloted Library Client Verification

Use this reference when a library-client slice is delegated to OpenCode.

## Evidence pattern

1. Inspect the repository and baseline before delegation.
2. Give OpenCode a bounded slice with exact non-goals and explicit commands.
3. After OpenCode exits, independently inspect `git status`, changed-file scope, and the complete diff.
4. Re-run focused tests with direct toolchain binaries. Do not accept a worker summary as evidence.
5. Restore unrelated formatter-only changes before staging, but inspect the worktree first to avoid overwriting genuine user work.
6. For dependency work, separate audit results, registry signature verification, frozen-lockfile reproducibility, and build/test results.

## OPDS-specific checks

For an authenticated OPDS client, tests should verify the actual Basic Authorization header at a local mock server, reject credential-bearing and cross-origin URLs, preserve the configured catalog path, replace pagination keys without duplicates, and enforce response-size limits while streaming when Content-Length is absent. Errors must not include full URLs or credentials.

## Safe completion gate

Only commit after focused parser/transport tests, full applicable tests, compile/type/build checks, formatting, `git diff --check`, and secret/machine-path scans have real passing output. Record remaining warnings separately from vulnerabilities and do not silently treat unmaintained transitive crates as fixed.
