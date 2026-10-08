# Requested native diagnostics

The `native-diagnostic` job in the existing Build PR workflow is diagnostic only.
Its result never supplies `fullClientGate` or `focusedClientGate` acceptance.
All four full-descriptor shards, all three required Masters parts, articulated
and connected release checks remain enabled and required independently. No production feature, assertion, native sequence, seed,
or release roster is removed by this runner.

The default `.github/native-diagnostic-request.json` is disabled:

```json
{"schemaVersion": 1, "case": null, "sourceSha": null}
```

After source changes are reviewed and committed, activate one case with a separate
commit that changes **only that request file**. Set `sourceSha` to the full lowercase
40-character SHA of the immediately preceding source commit. The request commit
must have exactly that one parent. Both commits are recorded in the evidence; the
checkout is the exact PR head, whose code is identical to the requested source.

The fixed case allowlist is defined in `tools/run_native_diagnostic_ci.py` and includes:

- `gale-vault-ballistic`: exactly the complete registered
  `GaleVaultBallisticTest` through `diagnostic-gale-vault-ballistic`. This test-only
  physics experiment retains every case, original world settings, trajectory,
  assertion and wait. One actual native signed-long world seed, exact ordered
  setup/run/cleanup/returned evidence and its final `GALE_VAULT_BALLISTIC_COMPLETE`
  marker are required. That marker is emitted only after all assertions, owned
  entity cleanup, world close and the idle assertion; Fabric's runner cleanup and
  return must then finish. The class, helper, all four test mixins/config and
  native tracing dependencies are hashed with the current source/request/head/run/
  attempt and configuration. Missing, stale, duplicate, malformed or partial
  evidence cannot pass. `ordinaryAttack=false` always; `physicsNativePending=true`
  until this exact native evidence passes. A tooling/compile pass is not physics
  evidence. No payment, damage, production selector or ordinary attack is enabled.
  This reuses the existing diagnostic job and artifact, unchanged limits and zero
  automatic retries; it adds no workflow or active request. Masters 47 (44/1/2),
  articulated six, paired 46 plus Moon four, and all four full shards remain required.
- `stone-fault-march-presentation`: exactly the one complete original
  `StoneMarchPresentationTest` class.
- `stone-fault-march-opponent`: exactly the one complete original
  `StoneMarchOpponentViewTest` class. These single-class diagnostic selectors keep
  all original trials, captures and waits. Each requires exactly its one seed and
  setup/run/cleanup/returned sequence, strict source/head/run/attempt/configuration
  provenance and unchanged 50-minute native/60-minute job caps. Neither can borrow
  the old two- or three-class scope, replace a required Masters part, or establish
  manual visual acceptance. Their source inventory and nine fixed March artifact
  paths are identical to the existing visual diagnostic. Adding these selectors
  does not activate or alter the committed diagnostic request.
- `stone-fault-march-visuals`: exactly the complete
  `dev.wildercord.aura.world.StoneMarchPresentationTest`, then the complete
  `dev.wildercord.aura.world.StoneMarchOpponentViewTest`. This scoped visual
  diagnostic skips the separate mechanics class only in this diagnostic selection.
  The original `stone-fault-march` diagnostic still requires all three classes;
  all three remain mandatory in the current 43-class Masters and full-descriptor
  release gates. Both visual classes retain every original trial, capture and wait.
  Each original world seed must be observed exactly once in class order, with
  matching source/request/head/run/attempt and source/configuration hashes. Both
  classes must complete setup/run/cleanup/returned exactly once in order.
  Three-class, partial, foreign, repeated, malformed or stale evidence cannot be
  relabelled as this two-class result. The existing registered-class rule remains.
  The shared March fixture, capture helper, visual render sources and capture
  mixins/config are hashed alongside both selected class sources. The existing
  `stone-march-native-diagnostic-<head>-<run>-<attempt>` artifact keeps the same nine
  fixed PNG/JSON pairs plus request/result provenance. Its guard already supports
  this case; no new artifact paths or jobs are added. The unchanged 50-minute
  native step and 60-minute job caps still apply, with no automatic retry or
  runtime guarantee. Missing or timed-out evidence remains unverified. A pass
  covers these two native classes only; mechanics, full/focused release acceptance
  and manual visual review remain separate requirements. Python tooling checks
  do not establish a native run or approve the images.
- `progression-feasibility`: exactly the complete `ReweaveFeasibilityTest`, then
  `StoneHingeFeasibilityTest`. Both are appended to the full descriptor without
  changing existing order; the 38-class Masters, six-class articulated and 46-case
  paired rosters stay unchanged. Each original world seed is observed once without
  RNG draws or overrides. Both classes must complete the existing runner's
  setup/run/cleanup/returned sequence exactly once in order, alongside matching
  source/request/head/run/attempt, processed descriptor and successful exit evidence.
  Missing, repeated, failed, foreign or malformed completion/seed receipts cannot
  pass. Every new proof helper and Stone Hinge test-only mixin/config is hashed.
  Reweave retains no ordinary RuneDef, lesson or input registration. Stone Hinge
  retains its movement-disabled boundary; success proves neither its six-step
  movement nor progression teaching, price approval or release acceptance.
- `wetland`: the original entire `WetlandGardenTest`, then the original entire
  `WetlandTerrainTest`, including the fixed failing seed `-7620530482425397421`,
  original habitat, search bounds and natural-presence assertion.
- `aura-fx`: the original entire `WildercordAuraFxTest`, including Glow and all other
  subcases. Its original Fabric world settings and fixture settings are preserved.
- `kiln-ring`: the three complete `EmberKilnTest`, `EmberKilnPresentationTest` and
  `EmberKilnOpponentViewTest` classes, in their original Masters/full-descriptor
  order. Each fixture passively records its actual world seed once, without RNG
  draws or seed overrides. Missing, repeated, conflicting or foreign receipts
  cannot pass; all three fixture sources are hashed in the existing provenance.
  The existing diagnostic job preserves the six fixed Kiln PNG/JSON pairs from
  the Masters artifact, unchanged, alongside request/result provenance under
  `ember-kiln-native-diagnostic-<head>-<run>-<attempt>`. Images remain diagnostic
  evidence requiring visual review; the 33-class Masters roster is unchanged.

A later unrelated source commit makes the old request inactive. An explicit
GitHub job rerun of the same still-valid request is allowed and records its new
`runAttempt`; the scripts never retry automatically. Do not create empty or
unchanged request commits to seek a pass. A disabled or stale request does not
install a virtual display or launch Minecraft.

Before launch, a bounded read of this public repository's current PR head ref
also rejects rerunning an old workflow after the PR has advanced. An unavailable
ref fails closed. Collection keeps evidence from the immutable launch revision
even if the PR advances or the network becomes unavailable afterward; the result
records that later freshness observation separately.

Requests are limited to 512 bytes, reject duplicate/unknown fields and accept no
class name, path, command, seed, environment, timeout or retry override. Each PR has
one running diagnostic job and at most one pending diagnostic job.
New commits queue instead of canceling an admitted diagnostic; stale queued
requests validate and skip without launching Minecraft.
The native step has a 50-minute cap and the job a 60-minute cap. Existing read-only
30/45-minute diagnostic snapshots remain active. The real launcher and Gradle
processed-descriptor validation are reused with an explicit diagnostic scope.

Artifacts contain the request/source/head/workflow/run/attempt identity, exact
selected classes, fixed environment, source/configuration SHA-256 values, actual
native world seeds, launcher selection/exit and processed-descriptor evidence.
Garden and AuraFx world seeds are passively observed; no seed or RNG is overridden.
The log artifact is limited to 16 MiB; oversized logs retain a tail and cannot
establish diagnostic success. Missing/failed/timed-out evidence remains unverified.
External and self-JVM stack snapshots are preserved from their exact known paths,
at most 16 files of 256 KiB each, with hashes and any truncation or omissions.
A green diagnostic result does not resolve any release gate by itself.

- `reweave-player`: exactly the complete `ReweavePlayableTest`, including the dedicated Ebb Ledger lesson helper and a second connected learn/equip/input/lifecycle world. The two observed seed labels are `ReweavePlayableTest#lesson` and `#input`. Its runner must emit the exact setup/run/cleanup/returned sequence once; seed markers and a successful build alone are insufficient. This is ordinary-path diagnostic evidence only, separate from the precursor and the release groups.
