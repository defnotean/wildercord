# Explicit full-client shard plan

The full-client plan is `tools/client_shard_plan.json`. It schedules the explicitly expanded
312-class descriptor as exactly four groups: **80 / 84 / 77 / 71**.
`tools/client_shard_plan.py` is the authoritative selector for the Python launcher,
manifest/aggregate tooling, and Gradle resource processing and launch preflight.
There is no equal-count fallback or automatic assignment for newly added classes.

## Identity and invariants

- Plan schema 1, ID `full-client-explicit-v2-counter-lifetimes`.
- Ordered roster SHA-256:
  `7b390799007708004706af9340e92b6c1ec35fa6031b388820b4341ed6dc7543`.
- Complete plan SHA-256:
  `daa7d1a6d8537791d47e84d1aa33ad896310cc4aac6de6c37ac0d91991158943`.
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
Windows; the CI job explicitly installs Python 3.12. The helper and plan are
resource-task inputs. Checked-in configuration/resource readers use bounded reads
with path/opened-file identity checks before and after reading, with additional
no-follow/nonblocking open flags where available. The existing Linux runtime
evidence readers are unchanged. The owned CI launcher remains Linux-only; Gradle
plan selection does not add that restriction. Focused selection and its existing
portable descriptor reader do not depend on this full-client plan.

Resource processing writes the exact plan identity to
`custom["wildercord:clientShardPlan"]` in the processed test-mod descriptor.
Immediately before the native client starts, the helper validates the raw processed
descriptor's entries and persisted identity, even after UP-TO-DATE resource tasks.
A missing/failed selector, stale resource or invalid plan cannot fall back to a
different selection. A direct Gradle invocation cannot manufacture the launcher's
owned source/run/attempt proof.

Full-client evidence schema 3 binds the plan identity into launcher selection,
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
The two new counter-lifetime classes extend only that fourth group, 69 to 71;
their runtime costs are unknown and are not assigned zero-duration estimates.
Historical known/proxy sums were approximately 131.92, 110.12, 46.03 and 97.17
minutes. They contain missing/censored work, use an older source for measurements,
and exclude unknown runtime costs. They are **unverified estimates**, not bounds.

The job caps remain **180 minutes**. No assertion, seed, internal case, deadline or
workflow permission is weakened. New full-client acceptance requires a fresh
exact-source four-group native run, successful uploads and a passing aggregate.
Relocated March/settings process prefixes and the runtime budget have not been
accepted by local Python/Groovy fixture tests. Native/Loom execution is not part of
this local validation.

## Offline regression checks

Run `python -m unittest discover -s tools -p 'test_*ci*.py' -v`.
`test_client_shard_plan_ci.py` covers plan schema/identity/membership/order/blocks,
nonregular and malformed files, stale processed resources, identity mutation at
every proof boundary, legacy evidence and focused-gate independence.
`test_full_client_ci.py` executes the same Groovy bridge with a local Groovy runtime
from a Gradle distribution (no Gradle task, Loom or native invocation). Set
`JAVA_HOME` and `GRADLE_HOME` to existing local toolchains to enable this parity
check. A skip means executable parity was not established.
