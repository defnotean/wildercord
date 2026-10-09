# Required Masters 47 partition

The existing Masters focused workflow job now has three mandatory matrix parts,
with `max-parallel: 2`, `fail-fast: false`, the existing 90-minute job limit and
85-minute native-step cap. All three must pass. No part substitutes for another.

| Required GitHub check | Whole classes | Order |
| --- | ---: | --- |
| `Masters update / masters-core (required part)` | 44 | The prior core roster in its existing relative order, Masters presentation last |
| `Masters update / masters-march-mechanics (required part)` | 1 | `StoneMarchTest` |
| `Masters update / masters-march-visuals (required part)` | 2 | `StoneMarchPresentationTest`, then `StoneMarchOpponentViewTest` |

The exact union is the expanded 47-class `masters` catalog, with no omissions or
duplicates. The core part contains 44 classes, with `EarnedCounterAcceptanceTest`,
`MirrorRiposteReleasedOwnerTest`, `UnmovedNullAcceptanceTest`, and
`ArtWardsHardeningTest` following `MastersStyleTimelineTest`; every prior class
retains its relative order. The four full-descriptor shards also include these
registered classes. The articulated six-class and connected
54-case plus four-view Moon gates remain unchanged.
The redundant unpartitioned workflow launch is removed; `--suite masters` remains a
whole-roster local selector. Diagnostic selectors never replace required checks.
Repository branch/ruleset protection must require each new matrix check name when
this workflow change is rolled out; workflow YAML alone cannot change server-side
branch protection, and this patch makes no such remote change.

Each part keeps normal workflow checkout behavior: on PRs this is the workflow's
merge candidate, recorded separately from its PR head. The checked-out SHA must
match `GITHUB_SHA`; the explicit head SHA, workflow/checkout SHA, run ID, attempt,
part selection hash and full Masters selection hash are bound into the launcher
receipt and verified again when the manifest is written. Tracked source changes
are refused. Every selected class must emit exactly the original ordered
setup/run/cleanup/returned sequence. The March parts additionally require exactly
one valid signed-long seed for each selected class in order, with no overrides.
Missing, duplicate, malformed, stale or foreign evidence fails closed.

A passed part records `requiredPartOutcome: passed`, while `focusedClientGate`
and `fullClientGate` stay `unverified`. To validate downloaded manifests from the
same run and attempt, use this bounded offline command; it creates no CI job:

```sh
python tools/masters_required_ci.py \
  --manifest core/masters-native-manifest.json \
  --manifest mechanics/masters-native-manifest.json \
  --manifest visuals/masters-native-manifest.json \
  --head-sha EXACT_40_CHARACTER_PR_HEAD \
  --workflow-sha EXACT_40_CHARACTER_WORKFLOW_CHECKOUT \
  --run-id RUN_ID --run-attempt ATTEMPT \
  --output masters-required-aggregate.json
```

The validator requires exactly three regular non-symlink JSON manifests, each at
most 1 MiB. Each must prove its complete assigned classes, status and provenance;
all must share the exact candidate/run/attempt and workflow checkout, and the union
must be exactly 47. Rerunning only failed jobs cannot mix new-attempt evidence with
previous-attempt passes; rerun all required parts for a new accepted attempt. Only
the aggregate may report the focused gate passed. It never reports the full-client
gate passed or establishes manual visual acceptance.
Part native logs are bounded to 16 MiB for acceptance; oversized logs stay archived
but cannot establish a pass. Every matrix artifact name includes its part to avoid
upload collisions; existing failure archives still run under their original guards.

Measured March mechanics duration was 2277.26 seconds. Ten observed body frames
arrived 60–67 seconds apart, averaging 63.1 seconds. At that observed cadence,
38 presentation trials estimate about 40 minutes and 28 opponent trials estimate
about 29 minutes. The combined visual part therefore estimates about 69 minutes;
these are estimates, not a runtime guarantee. Actual native completion beneath the
unchanged 85-minute cap remains a required acceptance check. Python tooling tests
and fabricated unit-test receipts do not establish that runtime acceptance.
