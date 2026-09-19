# Repo Hygiene and Professional Git Workflow (user directive, 2026-08-23)

The user explicitly corrected how Leafline (and by extension reader projects)
should be maintained. These are standing preferences, not one-off tasks.

## Documentation placement

- `README.md` is USER-FACING only: what the app does, features, requirements,
  build instructions, OPDS/account setup, status/license note. No internal
  planning content, no architecture decision records.
- Engineering notes for agents and contributors live in `AGENTS.md`:
  engineering rules, git/workflow conventions, app-boundary contracts,
  architecture decisions, quality gates, command references.
- Do NOT commit internal development documents (roadmaps, plans, spike notes,
  session logs) into the repository. Roadmap items become GitHub Issues;
  plans live in the issue tracker / project board, not in `docs/*.md`.
- The old pattern of `docs/<feature>-plan.md` files is retired for this
  project class. If a plan document exists in-repo, propose migrating it to
  issues before adding more.

## GitHub workflow

- Work happens on feature branches (`feat/...`, `fix/...`, `chore/...`,
  `docs/...`) merged via PR (squash-merge small slices). Never commit
  directly to `main`.
- Roadmap lives as GitHub Issues labeled by phase (e.g.
  `phase-1-reading-polish` ... `phase-7-distribution`), collected on a
  GitHub Project board ("Leafline Development"). Reference issue numbers in
  commits (`feat: ... (#12)`); use "Closes #N" in PR bodies.
- Conventional commits, no emojis or emdashes (existing rule).

## Board/issue bootstrap recipe (executed 2026-08-23)

```bash
gh label create 'phase-N-name' --color <hex> --description '<text>'   # per phase
gh issue create --title '...' --body '...' --label 'phase-N-name'     # one per roadmap item
gh project create --title '...' --owner <user>                        # requires project scope
```

## Pitfall: gh token missing the `project` scope

`gh project create` fails with
`GraphQL: Resource not accessible by personal access token (createProjectV2)`
when the stored credential lacks Projects permission. Labels, issues, and PRs
still work without it. Fix is an interactive device-flow refresh:

1. Run `gh auth refresh -s project -h github.com` (needs a PTY).
2. It prints a one-time code (e.g. `XXXX-XXXX`) and
   https://github.com/login/device .
3. Relay the code/URL to the user; they authorize in their browser.
4. Retry `gh project create`. Branch protection changes need admin API
   access on the same refreshed credential.

Do not retry `project create` in a loop hoping the scope appears — surface
the device code to the user and continue with non-blocked work (issues,
labels) meanwhile.

## State after the 2026-08-23 cleanup

- `docs/` deleted from Leafline; architecture + app-boundary content folded
  into AGENTS.md; README rewritten user-facing. Merged as PR #1
  (branch `chore/docs-cleanup`).
- Issues #2-#33 filed covering the seven roadmap phases with phase labels.
- Project board creation was pending the user's device-flow authorization;
  verify `gh project list --owner jdluu` before assuming it exists.
