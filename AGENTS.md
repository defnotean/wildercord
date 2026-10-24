# WilderCord agent entry point

Read [docs/AGENT_HANDOFF.md](docs/AGENT_HANDOFF.md) before changing this repository.
It records the current branch, implemented content, exact test evidence, VPS setup,
unfinished work and the next verification steps. Read the current user request first;
it takes precedence over this snapshot.

## Current state: polish loop (2026-10-24)

The owner started a polish loop on branch `0.14-connected-world`: audit, fix, test and refine in
rounds until they say "stop" (then finish the current edit, let VPS tests end, commit, push, clean up
and report). Findings and status live in `docs/audit/2026-10-24-polish.md`. The section below is the
older October 13 pause note.

## Earlier state: paused by the owner (2026-10-13)

On October 13, 2026 the owner explicitly asked to stop development and leave enough
repository context for another agent. The development goal and 15-minute heartbeat
are **paused**. The owned VPS test run was intentionally interrupted; no task test
runner or native test client remained at handoff. Do not restart autonomous work
or the heartbeat merely because an older audit says the goal is active. An explicit
user request to resume development authorizes continuing from this handoff.

## Requirements to preserve when work resumes

- **All builds, asset generators and tests run on `pandora-vps`.** Local editing,
  source inspection and Git operations are fine. GitHub Actions is disabled at
  repository level. Do not reenable Actions, dispatch CI, synthesize green statuses,
  or delete historical failed runs to make the history appear successful.
- Development uses `living-world-expansion` and draft
  [PR #3](https://github.com/defnotean/wildercord/pull/3) (succeeding [PR #2](https://github.com/defnotean/wildercord/pull/2) after branch rename). The owner authorized
  coherent commits and pushes while developing. No release, merge or live-server
  deployment was requested. Preserve unrelated user changes.
- VPS test checkout: `C:\wildercord-ci\repo`. The live server and world at
  `C:\wildercord` are separate. Never use the live world as a fixture or terminate
  all Java processes. Check actual process ownership before stopping a test.
- Never change sources or build outputs underneath a running native client.
  Inspect processes, checkout status and run receipts before switching candidates.
- **Testing override (owner, 2026-10-24, active for the polish loop).** The owner wants broad in-game
  testing and replaced the smoke-only rule below: per-area native client suites may be created and
  maintained in `tools/client_suite_catalog.json` (combat and bosses, mounts and taming, towns and
  contracts, rituals and parties, regions and worldgen, items and crafting, GUIs and HUD, persistence
  across relog/restart, multiplayer). Every gameplay fix gets an in-game test as well as its unit test;
  run the touched areas' suites after each batch and every suite every few rounds and before declaring
  done. Keep suites fast and sharded, record SHA, receipt and duration in the evidence ledger, and use
  real-game screenshots for visual evidence. `release-smoke` stays as the pre-release check.
  Former rule (2026-10-15, superseded while the override stands): native tests were only the 14-class
  `release-smoke` tier, run once before a release:
  `python tools/run_vps_checks.py suite --suite release-smoke --head <sha> --computer WIN-I2GBVRRD872`.
  A smoke pass is a smoke result, not proof of every feature; say so.
- Update generator sources alongside generated resources. Use the real game for
  native evidence and screenshots. Record candidate SHA, receipt ID and practical
  limits in the evidence ledger rather than claiming every feature is verified.

## Reading order

1. [Current handoff](docs/AGENT_HANDOFF.md) — start here, including resume commands.
2. [Progress inventory](docs/audit/2026-10-13-progress.md) — thorough work list.
3. [World-depth audit and roadmap](docs/audit/2026-10-13-world-depth.md) — findings,
   priorities and dated evidence; early sections describe the original baseline.
4. [VPS testing](docs/testing/vps.md) and
   [full native plan](docs/FULL_CLIENT_SHARD_PLAN.md) — execution and acceptance.
5. [Architecture](docs/ARCHITECTURE.md), [lore canon](docs/LORE_CANON.md),
   [design](docs/DESIGN.md), and [contributing](CONTRIBUTING.md) for the area being changed.

The existing mod is large. Prefer completing connected gameplay journeys over
adding disconnected content. A regional addition needs actual world placement,
resources, meaningful encounters or residents, persistence, player documentation
and appropriate VPS verification. Proposed roadmap items are not shipped features.
