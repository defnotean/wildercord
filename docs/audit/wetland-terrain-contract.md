# Natural Moonreed fixture contracts

The former assertion that the first swamp-biome search result must contain a reed
within 81 chunks was not a production guarantee. This change retains that exact
failed region as an explicitly named absence regression and adds an independently
named, fixed positive integration site. No production code, rarity, density,
biome registration, ground tag, or habitat predicate changes.

## Original region: native evidence, not inferred absence

`WetlandTerrainAbsenceTest` keeps seed `-7620530482425397421`, NORMAL Overworld,
the original biome search (`RandomSource.create(912)`, radius 6400, interval 32,
Y64), returned position `(-1536,64,-768)`, spiral order, and all 81 requested
chunks X `-100..-92`, Z `-52..-44`. It retains the original height-window detector
and independently checks every already-loaded section for reeds.

Native diagnostic run `37380606913`, job `112001305890`, public request head
`b3ec979da3640afd99755e2f2bcab1c2e5fbd410`, source `b7dc2ad9`, established:

- The actual stage-9 Moonreed feature index is 103 of 114.
- 44 inside and 12 halo source placements were scheduled; 47 rarity rejections,
  nine rarity admissions, four biome rejections, five biome admissions.
- Five real feature calls produced 120 attempts: 108 rejected by moisture and 12
  by occupied space. All 120 have terminal receipts; no writes, omissions, or
  sink errors occurred. The original detector and full-height census found zero.

The test requires the exact counts and compares every source, origin, ordinal,
coordinate, reached gate, and outcome against the 120 sorted native receipts in
`src/gametest/resources/wetland/absence-attempts.txt`. A zero census alone cannot
pass, nor can a disabled observer. It fails if generation or accounting drifts.

## Representative positive site: candidate pending native confirmation

`WetlandTerrainTest` keeps the same seed and requires an actual natural flower in
exactly nine chunks: X `-110..-108`, Z `-51..-49`, centered on source `(-109,-50)`.
All nine are inspected even if a reed is found early. No feature is invoked by
the fixture and no plant or terrain block is inserted. The independent original
detector must find a bud with a successful native write receipt. The fixed source
`(-109,-50)`, origin X/Z `(-1737,-786)`, must itself have an actual successful write.
Origin Y63 is only the model estimate. The full decorated native Y is retained
in every receipt, but does not decide success: production recomputes each
candidate's surface Y independently of the origin Y.
The detected state must be AGE1, supported, moist, and open to the sky. A normal
native screenshot is then taken.

This is a targeted positive integration case, not an unbiased density estimate.
The official-library model surveyed a fixed 33×33 region, 1,089 source chunks,
at four terrain samples per chunk. The terrain-only best 5×5 window centered on
`(-108,-41)` had six modeled wetland origins but zero qualifying bank draws;
that negative result is retained. Of the entire finite survey, four sources had
seven qualifying bank draws. The chosen source has four draws on three unique
base-noise bank sites: `(-1738,63,-789)` twice, `(-1740,63,-786)`, and
`(-1739,63,-786)`. The separate 3×3 positive candidate was selected from these
model results, after the terrain-only survey. It was not selected before those
results and is not a substitute result for the terrain-only window.

Base-noise support is only a solid, non-fluid proxy. Decoration, actual tagged
ground, and native writes remain unproven until this exact fixture runs in
Fabric. **Compile/JUnit success does not verify this site.** Preserve the failed
candidate and investigate any failure; do not expand the search, change the
seed, pick another site, force placement, or silently accept absence.

## Independent rarity and registration

`WetlandPlacementContractTest` reads the real registered feature and requires
the exact modifier sequence: rarity chance6 → in-square → WORLD_SURFACE_WG →
biome. It checks every registered biome and generation stage: there must be one
stage-9 occurrence in each swamp and mangrove swamp, and none elsewhere. It applies only the real rarity modifier to a fixed
81-source native RNG corpus, independently of any world-placement result, and
requires the same 14 admissions. `WetlandGardenTest` continues to cover supplied
wet/dry habitat, pollination, and persistence separately; it does not establish
natural distribution.

## Provenance, bounds, and lifecycle

Baseline: Minecraft26.3, Fabric Loader0.19.5, Fabric API0.161.0+26.3, Java25;
source snapshot `b3ec979da3640afd99755e2f2bcab1c2e5fbd410`. Production hashes and
the complete model-artifact manifest are stored in the accompanying evidence.
Runtime tests require the registered configuration and actual feature index/step
size, and logs record exact seed, region, source, and origin. If these change,
diagnose and explicitly review fixture provenance rather than silently reselect
a passing sample. The diagnostic runner records the exact source/config hashes
and fixed seeds for both natural cases. Diagnostic success still does not mark
the full or focused release gates accepted.

Each natural fixture owns its probe before creating its world and closes the
world before the probe. The obsolete test-method wrapper mixin is removed.
Every scope is at most9×9 chunks with one source halo (at most121 sources), one
feature call per source, 24 terminal attempts per call, and at most4096 log
events. Snapshots contain immutable scalar/coordinate data only. Teardown clears
all retained counters, attempts, writes, indexes, source objects and current
thread-local references. Existing forwarding, RNG neutrality, exception, sink
failure, truncation and teardown tests remain, with explicit independent-scope
and receipt-retention checks added.

Read-only post-generation moisture observations still skip missing neighboring
chunks. They never generate extra chunks and are not used as attempt receipts.
