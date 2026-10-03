# Grow admission and earned nursery pickup — development review

## Implemented and accepted

Grow checks actual direct adjacent writes for vanilla short grass, fern,
seagrass and pale moss carpet before native bonemeal placement. It retains
vanilla eligibility and physics and uses exact atomic shared block reservations.
The three focused Survival/paid permission suites passed: grass/fern 34 s,
seagrass 34 s and moss 28 s, including allowed growth, protected-upper refusal,
original-state preservation and real budget edges. Eight original captures are
retained under `artifacts/review/grow-adjacent/`.

The nursery now tracks each genuinely emitted dew UUID and uses actual client
movement toward its randomized landing. Earned Survival inventory acquisition
remains required. Inventory/entity checks are atomic within each server callback,
so normal pickup between callbacks cannot fail as a missing drop. There is no
item insertion, item/player teleport to the reward, ownership/delay change or
shortened resource rest. The final complete acquisition, crafting, once-only
reward and full restart run passed in 2 min 20 s:
`artifacts/review/glowcap-nursery/native-ci-pickup-final.log`.

## Checked artifact

`artifacts/review/grow-pickup-milestone/wildercord-grow-pickup-review+mc26.3.jar`

- SHA256: `e08c1c056c046a4d3f5a71703fdc2614420c0ebb51e6efb33f67129ac327951a`
- 5,061 processed resources and 1,684 compiled main/client classes match exactly.
- ZIP CRC, duplicate paths and unexpected/missing mod classes checked.
- Build passed in 14 s; 968 unit tests, zero failures/errors/skips.
- Version metadata remains 0.9.1-alpha.1+mc26.3; the development filename identifies
  these new bytes. No public upload/server activation is part of this increment.

## Limits and next work

The last Linux descriptor at f4d77883 passed ordinary build/guide but failed the
stationary earned-drop assumption. This final corrected native Windows pass
requires a renewed full Linux run. It is not real multiplayer evidence.

General tree, mushroom, bamboo, stem-fruit and biome flower feature adjacency
remains open. Grow's unconditional legacy ripple can still appear when no plant
changes; the separately prepared Life success-owner integration must reconcile
that presentation. These limited acceptance statements preserve the full goal.
See `grow-adjacent-preflights.md` and `glowcap-nursery-native.md` for details.
