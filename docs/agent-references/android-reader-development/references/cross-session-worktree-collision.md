# Cross-Session Worktree Collision (verified 2026-08-23, Leafline highlights session)

## The problem

Two **concurrent Hermes sessions** each launched OpenCode workers against the
same repository simultaneously. Unlike the kill+relaunch race (see
`opencode-competing-implementations.md`), both workers were live at the same
time, each creating a divergent implementation of the same feature.

## What happened (Leafline highlights, commits 59dec12)

### Session A (this session)
- Wrote `docs/highlights-plan.md` specifying an `Annotation*` schema
  (AnnotationEntity, AnnotationDao, AnnotationRepository)
- Launched OpenCode to implement per that plan
- OpenCode committed `59dec12` with the Annotation design

### Session B (a separate Hermes session, intended for ShelfSync)
- Drifted into Leafline and launched its own OpenCode worker
- Implemented a divergent `Highlight*` schema (HighlightEntity with
  excerpt/color/note, HighlightTints, NoteEditorDialog, SwipeToDismissBox)
- Never committed its supporting files, but kept rewriting `ReaderActivity.kt`
  to reference them

### Symptoms

- `ReaderActivity.kt` kept changing between Hermes commands — `git checkout`
  restored it, but within seconds it was rewritten again
- Build failures on references to types that don't exist in the tree
  (`onAnnotationClick`, `highlightsSheetVisible`, `Highlight`, `HighlightTints`)
- OpenCode itself reported: "files I did not create showed up in the working
  tree, and my annotation files were repeatedly deleted or reverted by a
  concurrent writer"
- `ps aux` showed no orphaned OpenCode processes — the writes came from the
  other session's still-active agent

## Detection

1. **Files change between your commands** — you `git checkout -- .` and the
   next command shows different uncommitted changes you didn't make.
2. **`git status` shows deletes of files you just committed** — the other
   session's agent deleted your committed files and created alternative ones.
3. **Build errors reference types you didn't create** — the other session's
   divergent schema is being wired into shared files.
4. **`ps aux` shows no rogue processes** — because the writer is another
   Hermes session's agent, not a standalone process you can see.

### Confirming via session_search

```python
# Search for other sessions working on the same repo
session_search(query="Leafline highlight annotation reader", limit=5)
```

If another session's history shows OpenCode commands with the same working
directory, that's the culprit.

## Recovery

1. **Stop touching the repo.** Any changes you make will be overwritten.
2. **Identify the other session** via `session_search` — look for a session
   whose OpenCode prompts target the same repository.
3. **Wait for the other session's agent to finish** (check `ps aux` for
   opencode processes, or poll `process list`).
4. **Once the other session is quiet**, do a full reconciliation:
   ```bash
   git reset -q
   git checkout -- .
   git clean -fd app/src
   ```
5. **Decide which design wins** based on what was committed vs. uncommitted.
   Committed work (your session's) should generally win; uncommitted divergent
   work should be discarded unless it has features worth porting.
6. **Verify and push** the clean state.

## Prevention

- **Only one Hermes session should drive a repository's working tree at a
  time.** If two sessions are active, they must work on different repos.
- When a user says "continue with the next phase," verify no other session
  is working in the same repo before launching an OpenCode worker.
- If you detect file churn you didn't cause, STOP immediately and
  investigate via `session_search` before making more changes.
- Save the collision to memory so future sessions know the rule.

## Key lesson

The `coding-agent-piloting` pitfall "Do not launch a second OpenCode worker
because the first appears slow" covers intra-session races. Cross-session
races are harder to detect because `ps aux` shows no rogue processes — the
writer is another Hermes session's agent. The detection signal is: **files
change between your own commands when no process you started is running.**
