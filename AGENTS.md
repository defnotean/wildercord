# WilderCord agent entry point

Read [docs/AGENT_HANDOFF.md](docs/AGENT_HANDOFF.md) before changing this repository.
It records the current branch, implemented content, exact test evidence, VPS setup,
unfinished work and the next verification steps. Read the current user request first;
it takes precedence over this snapshot.

## Current state: paused by the owner

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
  [PR #2](https://github.com/defnotean/wildercord/pull/2). The owner authorized
  coherent commits and pushes while developing. No release, merge or live-server
  deployment was requested. Preserve unrelated user changes.
- VPS test checkout: `C:\wildercord-ci\repo`. The live server and world at
  `C:\wildercord` are separate. Never use the live world as a fixture or terminate
  all Java processes. Check actual process ownership before stopping a test.
- Never change sources or build outputs underneath a running native client.
  Inspect processes, checkout status and run receipts before switching candidates.
- **Latest content candidate `57fff8a9` includes Windscar but has not passed a
  candidate build or its new native test.** Earlier passes do not establish that
  the current branch passes. The full 344-class native aggregate is unverified.
- Preserve original tests, cases, seeds, exact timing, geometry and receipt
  boundaries. Diagnose a failure before changing expectations. A focused pass,
  partial class count, archived screenshot check or interrupted run is not a full
  native pass. The explicit scheduling plan has no automatic fallback or balancing.
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
