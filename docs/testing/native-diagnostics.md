# Requested native diagnostics

The `native-diagnostic` job in the existing Build PR workflow is diagnostic only.
Its result never supplies `fullClientGate` or `focusedClientGate` acceptance.
All four full-descriptor shards and both focused release jobs remain enabled and
required independently. No production feature, assertion, native sequence, seed,
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
