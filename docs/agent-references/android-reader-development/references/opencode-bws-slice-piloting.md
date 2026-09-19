# OpenCode Slice Piloting on ShelfSync (verified 2026-08-22)

Session-proven pattern for piloting OpenCode against a local Tauri + React repo
(ShelfSync) through BWS-resolved secrets. All steps below were executed and
verified during the OPDS client build (commits 8ed52c5..9c5b131).

## Invocation

```bash
export PATH="/home/jdluu/.local/bin:/home/jdluu/.cargo/bin:$PATH"
cd <project-root>
/home/jdluu/.local/bin/opencode-bws run --format json --auto \
  --model openrouter/<model-id> \
  --dir <project-root> '<single-quoted task prompt>'
```

- Run as a background terminal process with `notify_on_complete=true`; typical
  runtime is 3-15 minutes per slice. Poll with wait/poll rather than killing.
- The binary resolves `OPENROUTER_API_KEY` from Bitwarden Secrets Manager at
  process start. Never write the key to disk or echo it.
- Model ids live in `~/.config/opencode/opencode.jsonc` under
  `provider.openrouter.models`. Verify a model is free before relying on it:
  fetch `https://openrouter.ai/api/v1/models` to a temp file and read
  `pricing.prompt` / `pricing.completion` (0 = free).

## Prompt structure that worked

Every prompt named, in order:

1. Scope: exact directories allowed, plus an explicit do-not-touch list
   (App shell, legacy sync/local-db code, package.json, persistence).
2. Inspect-first instruction naming the existing APIs to read before coding.
3. The one deliverable, described behaviorally (not by file layout guesses).
4. Test requirements enumerated per behavior (success, auth, failure,
   cleanup, no-credential-leak).
5. Verification commands to run, verbatim:
   - Rust: `cargo test --manifest-path src-tauri/Cargo.toml --lib <filter> --no-default-features`,
     then `cargo check ... --lib --no-default-features`
   - Frontend: `pnpm vitest run`, `pnpm build`, `node_modules/.bin/biome check .`
   - Always finish with `git diff --check`
6. "Do not commit."

Keep each slice vertical but tiny: one layer of one feature per run. Larger
prompts caused multi-hour runs and rate-limit exits mid-task.

## Known agent failure modes (check every run)

1. **Unrelated formatting churn**: the agent repeatedly rustfmt/biome-formats
   files outside scope (core/, http/, commands/*). Before committing, run
   `git status`, diff every modified path against the intended scope, and
   `git restore --` everything out of scope. Only stage intended files.
2. **Rate-limit exit**: `stealth/ox-alpha` can 429 upstream mid-run
   (`APIError ... temporarily rate-limited`). The run exits nonzero; partial
   edits remain on disk. Inspect state, fix gaps directly if small, or rerun
   the remaining subtask.
3. **Double root-join bugs**: when a plan object carries a pre-joined
   destination and the executor joins it again, output lands at root/root.
   Define one destination contract (relative-to-root) and enforce it with a
   dedicated integration test passing planner output into the executor.
4. **Stale captured variables in tests**: tests asserting props captured
   before a rerender can pass vacuously; reset captures before triggering the
   state change being proven.

## Verification gates before any commit

```bash
git restore -- <out-of-scope files>
git add <intended files only>
git diff --cached --check
git commit -m "<conventional commit>"
git push
```

Full gate set: focused tests → full suite → build → biome/rustfmt clean →
`git diff --check` → scoped staging → commit → immediate push.
