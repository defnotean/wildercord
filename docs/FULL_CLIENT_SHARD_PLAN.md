# Explicit full-client shard plan

> **Retired (2026-10-15).** The full-client shard plan and its per-feature native suites were
> deleted. Only the 14-class `release-smoke` suite remains; see `docs/testing/vps.md`.
> The text below is kept as history and no longer describes runnable tooling.


The full-client plan is `tools/client_shard_plan.json`. It schedules the explicitly expanded
349-class descriptor as exactly four groups: **87 / 87 / 88 / 87**.
`tools/client_shard_plan.py` is the authoritative selector for the Python launcher,
manifest/aggregate tooling, and Gradle resource processing and launch preflight.
There is no equal-count fallback or automatic assignment for newly added classes.

The 18 classes added after the reviewed 317-class plan (the new Master packs, moves, codex-era
tests, the performance harness and the HUD layout test) were appended to existing groups only:
every reviewed group keeps its exact members and order, and `tools/tests/full-client-plan-v2-317.json`
holds that reviewed plan so the tests can prove it.

## Identity and invariants

- Plan schema 1, ID `full-client-explicit-v8-emberwood`.
- Ordered roster SHA-256:
  `63e4dae5883e2b9375df0bee213d66ca0ed01096a678c753f1840e079c8f3b40`.
- Complete plan SHA-256:
  `2136c19ee0beaa49862ba0f9d770de5b2a5dabea8a2acfbdc5158dbdcdee5790`.
- Hashes use UTF-8 JSON with sorted object keys, compact separators and no trailing
  newline. The plan digest excludes no fields and is pinned in the selector.
  These are source identities, not claims of runtime acceptance.
- Every selected class occurs exactly once across the four groups. Each group
  follows descriptor-relative order; concatenating the groups need not reproduce
  descriptor order.
- Relay, Wetland Garden through terrain, the complete March trio, Wall Turn, and
  settings through articulated capture remain contiguous and co-located. The
  settings span includes `UpgradeRecoveryTest`. Their exact class lists are both
  declared in the plan and validated against fixed required blocks.
- The focused Masters 47-class roster and its 44/1/2 parts are independent.
  The explicit counter registration adds UnmovedNullAcceptanceTest and
  ArtWardsHardeningTest once. Their full-client placement is immediately after
  MirrorRiposteReleasedOwnerTest in group 4, with the complete earned-counter
  lifetime span pinned as an additional co-location/order block. Every one of
  the previously reviewed 310 classes retains its exact group and relative order;
  the immutable v1 plan is retained in tools/tests/full-client-plan-v1-310.json.
  No class-internal contract is changed by scheduling.
- `ReedlandBiomeTest` follows `WetlandTerrainTest` in group 3. It adds natural
  regional terrain, automatic journal discovery and real save/reopen coverage;
  every previously scheduled class keeps its group and relative order.
- `FerrymanEncounterTest` follows that regional test in group 3. The preceding
  342-class plan is frozen in `tools/tests/full-client-plan-v3-342.json`; a separate
  regression proves every original group member retains its exact relative order.
- `WindscarBiomeTest` is appended to group 4 and to the descriptor. It adds a
  bounded natural highland census, client harvesting, owner discovery and real
  save/reopen coverage. The 343-class plan is frozen in
  `tools/tests/full-client-plan-v4-343.json`; every old class stays in its original
  group and relative order. The first three groups remain byte-identical lists.
- `CartographerEncounterTest` follows `WindscarBiomeTest` in group 4 and in the
  descriptor. The 344-class plan is frozen in `tools/tests/full-client-plan-v5-344.json`;
  every old class stays in its original group and relative order.
- `BelowkeeperBiomeTest` is appended to group 1 and `HollowBellEncounterTest` is
  appended to group 2, following `CartographerEncounterTest` in the descriptor.
  The 345-class plan is frozen in `tools/tests/full-client-plan-v6-345.json`;
  every old class stays in its original group and relative order.
- `EmberwoodBiomeTest` is appended to group 3 and `KilnWidowEncounterTest` is
  appended to group 4, following `HollowBellEncounterTest` in the descriptor.
  The 347-class plan is frozen in `tools/tests/full-client-plan-v7-347.json`;
  every old class stays in its original group and relative order.

Missing/foreign/duplicate/reordered classes, changed descriptors, invalid counts,
noncanonical shard selectors, altered blocks, malformed JSON or types, duplicate
JSON keys, stale identities and unreviewed reassignment fail closed. Configuration
readers accept only bounded regular non-symlink files. To change the descriptor or
plan, deliberately review and update the expanded plan, roster/plan pins and
regression anchors together. Timing data never enters runtime selection.

## Gradle parity and launch proof

Gradle's thin Groovy bridge runs Python 3 directly, without a shell. Both resource
processing and preflight validate the strict raw source descriptor and invoke the
same selector. Python 3 must be on PATH as `python3` on macOS/Linux or `python` on
Windows; the VPS environment supplies Python 3.12. The helper and plan are
resource-task inputs. Checked-in configuration/resource readers use bounded reads
with path/opened-file identity checks before and after reading, with additional
no-follow/nonblocking open flags where available. Runtime evidence readers also
support Windows reparse-point and concurrent-write refusal. The original Linux
launcher is retained as historical tooling; the owned Windows launcher is
`tools/run_vps_checks.py`. Focused selection and its existing
portable descriptor reader do not depend on this full-client plan.

Resource processing writes the exact plan identity to
`custom["wildercord:clientShardPlan"]` in the processed test-mod descriptor.
Immediately before the native client starts, the helper validates the raw processed
descriptor's entries and persisted identity, even after UP-TO-DATE resource tasks.
A missing/failed selector, stale resource or invalid plan cannot fall back to a
different selection. A direct Gradle invocation cannot manufacture the launcher's
owned source/run/attempt proof.

Full-client evidence schema 4 on the VPS (schema 3 for historical GitHub evidence)
binds the plan identity into launcher selection,
processed-descriptor evidence, provenance, shard manifests and the all-four
aggregate. Existing source head versus merge checkout/workflow identity, run,
attempt, job, exact execution profile, per-launch UUID, strict native lifecycle
envelopes, setup/run/cleanup/returned order, successful owned exit and same-run
artifact checks remain mandatory. Old arithmetic or legacy-plan evidence cannot
substitute, even for an unchanged class set. Only the aggregate may set
`fullClientGate=passed`; a single shard or unsharded run remains insufficient.
The required gallery and explicit accounting for the three optional skipped
profiles remain unchanged. Focused, articulated and connected gates stay separate.

## Timing and acceptance limits

The plan follows the reviewed historical grouping proposal: move the March trio
to group 1 and the settings/articulated span to group 2, retain historical group 3,
and leave the remaining final span in group 4 with Mirror in descriptor order.
The two new counter-lifetime classes extend only that fourth group, 69 to 71.
The five Masters form and lesson-pack classes (Form Dash lesson, safety and
commitment, Stone Hinge lesson, lesson pack playable) extend it again, 71 to 76.
Stone Hinge sits after the Form Dash trio so the wall-turn block stays contiguous.
Their runtime costs are unknown and are not assigned zero-duration estimates.
Historical known/proxy sums were approximately 131.92, 110.12, 46.03 and 97.17
minutes. They contain missing/censored work, use an older source for measurements,
and exclude unknown runtime costs. They are **unverified estimates**, not bounds.

The job caps remain **180 minutes**. No assertion, seed, internal case, deadline or
workflow permission is weakened. New full-client acceptance requires a fresh
exact-source four-group native run, all retained manifests and a passing aggregate.
Relocated March/settings process prefixes and the runtime budget have not been
accepted by Python/Groovy protocol tests. Native execution is a separate required
check. GitHub Actions remains disabled; run both kinds of checks on the VPS.

## VPS protocol regression checks

Run `python -m unittest discover -s tools -p 'test_*ci*.py' -v`.
`test_client_shard_plan_ci.py` covers plan schema/identity/membership/order/blocks,
nonregular and malformed files, stale processed resources, identity mutation at
every proof boundary, legacy evidence and focused-gate independence.
`test_full_client_ci.py` executes the same Groovy bridge with a local Groovy runtime
from a Gradle distribution (no Gradle task, Loom or native invocation). Set
`JAVA_HOME` and `GRADLE_HOME` to existing local toolchains to enable this parity
check. A skip means executable parity was not established.
