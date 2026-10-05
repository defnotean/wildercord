# Existing-world pavilion: reviewed local implementation, 2026-10-05

## What is implemented

- A default-off, owner-operated 123-block open Wayfarer Training Pavilion in one resident
  Overworld chunk; 729-cell immutable guard, no terrain clearing, loot or entities.
- Explicit no-provider/manual-review declaration or all-CLEAR claim adapters, separate
  unknown-history acceptance, full preview hash and offline backup reference.
- Exact snapshot revalidation, two setter attempts per tick, no forced chunk loads, no
  world regeneration, strict inhabited/edit/structure/spawn/activity exclusions.
- Forced write-ahead manifests, compare-and-set resume, later-edit/ABA-safe rollback,
  version-independent site identity and persistent tombstones.
- Bounded future player-edit evidence and before-change guarded-region fences.

The 12-block Cairn is an internal transaction fixture, not a building delivered to players.
No real user world was opened or modified. No commit was pushed by this implementation task.

## Independent review

A separate read-only source review found and verified fixes for:

1. Startup manifest-access failure silently losing later-edit fences. Retained unreadable
   recovery data now refuses server startup; an unsupported fresh journal only disables
   this feature. Indeterminate directory existence is never treated as verified absence.
2. False post-mutation results overwriting newer reentrant edit fences. Both apply and
   rollback refresh the latest entry before persisting any subsequent result, and failed
   post-mutation attempts consume the write budget.
3. Approval adopting a policy revision changed during claim admission. Authorization now
   rechecks and persists its original expected policy.
4. Provider metadata errors becoming a stable approvable error hash. Invalid metadata and
   exceptions now fail policy computation and claim admission closed.
5. Tracking saturation losing exclusion evidence. Saturated recent activity defers all
   candidate regions; saturated future edit history excludes its whole dimension.

The final reviewer reported no remaining source blockers, with native execution outstanding.

## Verification actually run

Official Minecraft 26.3/Fabric classpath verifier, isolated checkout:

`VERIFY_REPO=/workspace/shared/wildercord_safe_upgrades python3 /workspace/shared/wildercord_compile_verification/verify.py --tag safe-upgrades-reviewed-20261005-1324`

- Started 2026-10-05 13:24:34 UTC; completed 13:25:08 UTC.
- Main: 766 Java source files compiled.
- Client: 242 compiled.
- Unit tests: 128 compiled.
- Game tests: 293 compiled, including `UpgradeRecoveryTest`.
- JUnit: **1,158/1,158 passed**, including 18 upgrade-focused tests.
- All 3,353 source fingerprints unchanged during the gate.
- `python3 tools/test_client_ci.py`: 22 tests passed; no new CI variant added.
- `git diff --check`: passed.

The verifier uses official jars and existing declared Fabric metadata. It does not run Loom,
Mixin transformation, or a game/client. No native runtime success is claimed.

## Required native release gate

`dev.wildercord.world.upgrade.UpgradeRecoveryTest` is registered in the existing full client
GameTest descriptor. It creates a disposable fixture save, proves preview/default-off and
container refusal, interrupts after five actual writes, closes and reopens the actual
integrated server, requires fresh policy authorization, completes exactly 123 blocks,
unloads the real chunk without recovery loading it, and verifies actual mixin-recorded ABA
edits survive rollback. This fixture is authored and compiled, **not executed** here.

Its controlled exception and graceful save/reopen test native integration and restart
recovery. They are not an abrupt process kill or physical power-loss test. Deterministic
journal tests separately model all Cairn apply/rollback boundaries and arbitrary lagging
chunk-save mixtures. Entity/boss adapters and large-worldgen retrofits remain unfinished.

## Focused integration gate

The existing focused Masters update job now selects 17 suites, including UpgradeRecoveryTest. It is labeled as update combat and safety coverage, remains separate from all four unchanged aggregate shards, and does not establish the full-repository native gate. Native results must be recorded against the published commit before recovery support is accepted.
