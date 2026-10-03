# Grow: known adjacent bonemeal preflights

## Scope

Grow now preflights three exact vanilla mutation patterns before calling the original bonemeal method. The checks use actual source eligibility, a live caster in the same world, loaded positions, world border/build height, and real edit permission at every direct destination.

| Source | Actual direct destinations | Atomic block reservation |
|---|---|---:|
| Short grass or fern | Original lower cell and upper plant half | 2 |
| Seagrass | Original lower cell and upper water cell converted into tall seagrass | 2 |
| Pale moss carpet | Upper wall-supported topper cell | 1 |

Permission and eligibility checks finish before reservation and before the first native write. Known branches replace the original unconditional one-cell reservation; they do not charge the source again. Failed pair reservations consume no partial budget. Vanilla bonemeal placement and physics remain responsible for admitted growth.

Support is exact vanilla block identity. Arbitrary subclasses, custom features and other bonemealable blocks retain their original source admission path.

## Why preflight is needed

Mapped Minecraft26.3 TallGrassBlock/DoublePlantBlock and SeagrassBlock write the lower cell before the upper cell and ignore the write return values. Refusing only the upper write would leave a partial lower plant. Preflighting both destinations prevents that confirmed protected-neighbor failure without replaying or undoing native physics.

MossyCarpetBlock computes a deterministic topper using the same eligibility logic and writes only source.above. Its reservation counts that one actual direct destination.

## Native acceptance

The registered focused suites are:

- `dev.wildercord.cast.GrowDoublePlantPermissionTest`
- `dev.wildercord.cast.GrowSeagrassPermissionTest`
- `dev.wildercord.cast.GrowMossCarpetPermissionTest`

Each uses an actual Survival player, learned runes, public spell editing/casting and mana payment. Allowed casts must create their real plant/topper before protected-neighbor refusal is checked. The veto denies only the actual upper cell; refused casts must leave the original lower state and upper destination intact.

Additional helper-level checks use real shared Cast budgets without invoking gameplay: a pair cannot spend a one-cell remainder, and a successful pair consumes exactly two; a topper refuses exhausted budgets and consumes exactly one available cell. Reservations themselves do not change blocks.

Allowed/refused screenshots and logs are preserved under `artifacts/review/grow-adjacent/{grass-fern,seagrass,moss}`. Test capture positions are temporary world player positions; the suites do not alter camera type, HUD preference, window size or magic quality settings.

Final focused native results on Windows (2026-10-03): **grass/fern passed34s, seagrass passed34s, moss passed28s**. Production/client/gametest compilation also passed. Eight final screenshots preserve allowed/refused states; recipe/advancement chat overlays were cleared for readability. Earlier successful logs/captures are retained separately.

This is gameplay admission acceptance. Grow's existing unconditional legacy ripple still appears when no block grows; this change does not claim successful-outcome presentation completion. The separately drafted Life owner integration must reconcile that display with actual retained writes.

## Remaining adjacency gap

This change does not establish general tree, mushroom, bamboo, stem-fruit or biome flower feature transactions. GrassBlock's random walk can invoke registry/biome features and nested bonemeal. TreeGrower removes surrounding saplings before feature placement; TreeFeature writes decorators and updates leaf distances and shape edges. Restoring a block-state ledger cannot by itself prove restoration of escaped physics, scheduled work, block entities or dropped resources.

Do not extend this acceptance to a fixed guessed tree cube or a generic setBlock veto/rollback. Such an implementation needs exact feature planning, bounded complete destinations and side-effect evidence before its first mutation. Dynamic custom permission callbacks that mutate the world during admission are also outside these narrow tests.

The isolated source-backed broader review and mapped bytecode evidence remain in `artifacts/drafts/life-outcomes/broader-grow-source-review.md` and `source-analysis/`. No Life outcome observer hooks, voices or presentation suppression are enabled by this change.
