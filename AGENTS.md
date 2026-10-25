# WilderCord agent entry point

Read the current user request first; it takes precedence over this file. Then read the
[polish audit](docs/audit/2026-10-24-polish.md) for current findings and evidence, and
[docs/AGENT_HANDOFF.md](docs/AGENT_HANDOFF.md) for VPS setup, implemented content and commands
(its branch and release status sections predate 0.13).

## Current state: polish loop (2026-10-24)

The owner started a polish loop on branch `0.14-connected-world`: audit, fix, test and refine in
rounds until they say "stop" (then finish the current edit, let VPS tests end, commit, push, clean up
and report). **No pull requests:** once a fix is confirmed on the VPS and implemented, merge the branch
into `main` directly (owner, 2026-10-24). Findings and status live in `docs/audit/2026-10-24-polish.md`.

History: `living-world-expansion` was merged through PR #3 on 2026-10-15 and released as `v0.13.0-alpha`.
The October 13 pause has ended; development resumed on `0.14-connected-world`. Older docs that call the
work "paused" or "unmerged" describe that earlier snapshot.

## Requirements

- **All builds, asset generators and tests run on `pandora-vps`.** Local editing,
  source inspection and Git operations are fine. GitHub Actions is disabled at
  repository level. Do not reenable Actions, dispatch CI, synthesize green statuses,
  or delete historical failed runs to make the history appear successful.
- The owner authorized coherent commits and pushes on the working branch. Releases,
  CurseForge uploads and live-server changes need the owner's say-so. Preserve
  unrelated user changes.
- VPS test checkout: `C:\wildercord-ci\repo`. The live server and world at
  `C:\wildercord` are separate. Never use the live world as a fixture or terminate
  all Java processes. Check actual process ownership before stopping a test.
- Never change sources or build outputs underneath a running native client.
  Inspect processes, checkout status and run receipts before switching candidates.
- **Testing override (owner, 2026-10-24, active for the polish loop).** The owner wants broad in-game
  testing and replaced the earlier smoke-only rule: per-area native client suites may be created and
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

1. [Polish audit](docs/audit/2026-10-24-polish.md) — current findings and evidence ledger.
   [Handoff](docs/AGENT_HANDOFF.md) holds VPS setup and commands; its status sections predate 0.13.
2. [Progress inventory](docs/audit/2026-10-13-progress.md) — thorough work list.
3. [World-depth audit and roadmap](docs/audit/2026-10-13-world-depth.md) — findings,
   priorities and dated evidence; early sections describe the original baseline.
4. [VPS testing](docs/testing/vps.md) and `tools/client_suite_catalog.json` (the native
   suites). `docs/archive/FULL_CLIENT_SHARD_PLAN.md` describes the retired 349-class gate; do not follow it.
5. [Architecture](docs/ARCHITECTURE.md), [lore canon](docs/LORE_CANON.md),
   [design](docs/DESIGN.md), and [contributing](CONTRIBUTING.md) for the area being changed.

The existing mod is large. Prefer completing connected gameplay journeys over
adding disconnected content. A regional addition needs actual world placement,
resources, meaningful encounters or residents, persistence, player documentation
and appropriate VPS verification. Proposed roadmap items are not shipped features.
