# Library Client Audit Pattern

Use this checklist when an existing app may be repurposed as an offline catalog client.

## Evidence table

| Area | Inspect | Record |
|---|---|---|
| Repository | branch, remotes, worktree, recent history | exact revision and cleanliness |
| Product model | README, UI flows, domain models | source of truth and user roles |
| Transport | clients, auth, URL construction, redirects | protocol assumptions and credential scope |
| Persistence | schema, migrations, paths | identity, state, revision handling |
| Downloads | temp files, retries, verification, rename | interruption and replacement safety |
| Android | permissions, storage, workers/services | process death and scoped storage behavior |
| Tests | unit, integration, E2E, fixtures | real gates versus claims |

## OPDS evidence to capture

- documented root endpoint and enablement setting;
- account type and authentication scheme;
- root navigation links and advertised feed types;
- pagination parameters, defaults, maximums, and link semantics;
- entry identifiers, metadata, acquisition links, and media types;
- relative URL and redirect behavior;
- content length/hash and format verification behavior;
- separate progress protocol, credentials, and matching identity.

## Decision rule

Recommend incremental continuation only when the existing source-of-truth,
identity, transport, persistence, and storage semantics already fit. Recommend
substantial refactoring when the native shell and infrastructure are reusable
but product assumptions differ. Recommend replacement only when reuse would
preserve unsafe coupling or the foundations cannot support the smallest tested
vertical slice.

## Report discipline

Separate verified facts, documentation claims, code observations, and unknowns.
If a command cannot run because the environment lacks a toolchain, report it as
unverified rather than converting it into a permanent design constraint. End
with a small next task and explicit non-goals.
