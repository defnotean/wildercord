# Passive native wetland receipts

Historical implementation note for the diagnostic through `b3ec979`. Native run
`37380606913` subsequently established the exact legitimate absence: 120 terminal
rejections, no writes, no lost receipts. The reviewed replacement contract and
current probe ownership/scopes are documented in
[Natural Moonreed fixture contracts](../audit/wetland-terrain-contract.md).
The original observation-phase constraints below are retained as history; the
current tests explicitly separate the original absence case from the fixed
representative positive candidate.

This GameTest-only probe observes the existing `WetlandTerrainTest`. Its seed
`-7620530482425397421`, habitat `(-1536,64,-768)`, radius-four search, original
height-window detector, screenshot path, and natural-presence assertion are
unchanged. A missing flower still fails the test.

The probe is enabled only while that exact test is running. Observed calls must
use `WorldGenRegion`, Overworld, the reproduction seed, the registered Moonreed
placed feature, and source chunks X `-101..-91`, Z `-53..-43`. This is the existing
81-chunk square plus the one-chunk source halo that can place a candidate inside
it. It does not load any of those chunks; it only sees calls generation already
makes. No world, generator, placement context, or RNG is retained.

`wetland-generation-gametest.mixins.json` belongs only in the GameTest mod's
`mixins` list. Register `dev.wildercord.wildlife.WetlandGenerationScopeTest` as a
client GameTest alongside the existing terrain test. The scope test creates no
world and cannot supply evidence of native generation or Mixin applicability.

## Receipts

All lines start with `WETLAND_GENERATION`.

- `SCHEDULE`: actual source chunk, inside/halo classification, seed, decoration
  stage, runtime feature index, step size, and placed/configured feature keys.
  The private `featuresPerStep` supplier is already memoized and evaluated by
  vanilla decoration before the observed placement call.
- `MODIFIER`: the existing modifier's input, emitted immutable position snapshots,
  output count, and normal/exceptional return. The original modifier is called
  once with its original context/RNG/input. Its consumer receives the original
  output object immediately; only the receipt gets a copy.
- `FEATURE_HEAD`, `ATTEMPT`, `FEATURE_RETURN`: the actual origin, each of the
  unchanged 24 candidate heights, reached predicates, first rejecting predicate
  or write result, and feature result. Skipped short-circuit predicates are not
  evaluated. A false `setBlock` result is preserved separately: production still
  increments its local placed count regardless of that result.
- `CHECKPOINT`: a snapshot after one of the fixture's already-requested chunks
  has returned. Inside and halo receipts remain distinguishable. Other pending
  native work may still contribute later receipts.
- `END`: final diagnostic counters after the original fixture teardown, including
  when its existing natural-presence assertion fails.

Retention is capped at 121 source records, one observed feature invocation per
source, 24 candidate terminal receipts per invocation, 24 output snapshots per
modifier, and 81 checkpoints. Unexpected extra source/invocation/attempt events
are counted. Event output is capped at 4,096 lines plus one truncation receipt
and one final summary. Truncation and sink failures invalidate completeness;
they cannot silently establish a zero-attempt result. Runtime logging failures
are counted without replacing native returns or exceptions. Session maps and
thread-local invocation state clear through success/failure `finally` paths.

## Native compatibility and limits

The two placement methods, all seven wrapped invocation sites, and the fixture
method use official Minecraft 26.3 descriptors. All injectors require and expect
exactly one site and allow at most one. The configured `defaultRequire` is one.
The feature-index accessor targets the native private supplier directly.

Focused Java 25 compilation and the no-world scope test verify bounds, rejected
scope, original consumer object identity and timing, one original call, unchanged
RNG continuation, failed-write receipts, exception propagation, cleanup, restart,
output truncation, and throwing-sink cleanup. They do not prove Mixin application.
Native CI must apply the mixins and run the unchanged terrain test before these
receipts can classify the observed absence. Read-only base-noise calculations
remain a model, not decorated-runtime evidence. No fixture-contract or production
rarity change is included here.
