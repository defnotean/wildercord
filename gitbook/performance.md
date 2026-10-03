# Performance and visual settings

Wildercord separates decorative effects from gameplay. Reduce presentation when many players are casting; keep delivery geometry and combat warnings readable.

## Choose your visual settings

Assign a key to **Magic visual settings** under Minecraft Controls → Wildercord.

| Setting | Suggested starting point | What it changes |
|---|---|---|
| Your own formations | Full for solo screenshots; Balanced for ordinary play | Decorative density for your casts |
| Other casters' formations | Balanced, or Minimal in busy fights | Decorative density from other players |
| Reduced flash | Enable if bright releases are uncomfortable | Dims Wildercord shaped light |
| Camera motion | Reduce if you prefer steady aiming | Wildercord shake and field-of-view punches |

The settings are local in `config/wildercord-visuals.json`. The launcher profiles set defaults; you can change them later. Cinematic includes Iris but requires a separately installed and selected shader pack to enable shaders.

## Server limits

The spell compiler caches at most **256** plans and includes ranks in its keys. Each caller receives its own mutable plan. Server reload clears the cache. Decorative delivery is bounded to **512 packets per player per tick**; essential interactions retain their gameplay rules.

Temporary terrain has finite lifetimes and default budgets of **32 cells per cast**, **64 active cells per owner**, and **512 globally**. Puzzle and home systems operate in loaded areas, and event echoes do not force chunks to load.

## Measure a problem

Operators can use `/wildercord visualstats` and `/wildercord visualstats reset`. In the practice arena, `/wildercord practice benchmark` samples ten seconds of server activity; moving or stress targets help repeat a scenario. Other players contribute to server-wide counters during the sample.

Server counters are not GPU frame times. Prior measurements on a Ryzen 7800X3D / RTX 5080 at 1600×900 and a 120 FPS cap recorded roughly 8.33 ms median frame time and 8.9–9.1 ms p95 during a 24-target Firestorm beam scenario. That cap dominates the result: it does **not** establish an uncapped speedup or a promise for other hardware. The twelve new circle disciplines have focused functional and shader tests, rather than a new full hardware benchmark.

For a reproducible report include hardware, render distance, shaders, visual preset, spell, target count and whether the frame rate is capped.

## Mixed material review

The living-world development build also has a sustained test with twenty-four moving targets and
sixty-four paid Bolts alternating Pelt, Venom, Ember and Windcut. Each preset was measured in a
fresh world for about thirty seconds at 1280 × 720, with VSync disabled and a 120 FPS cap.

| Preset | Median frame | p95 frame | p99 frame | p95 server tick |
|---|---:|---:|---:|---:|
| Performance | 8.32 ms | 10.07 ms | 11.02 ms | 1.76 ms |
| Balanced | 8.33 ms | 9.76 ms | 10.32 ms | 1.32 ms |
| Cinematic | 8.33 ms | 9.65 ms | 10.15 ms | 1.13 ms |

Every scheduled cast was admitted in all three runs. The frame cap dominates the median, so this
comparison does not establish that one preset is faster. It also does not measure remote
multiplayer, a populated natural ecosystem or an uncapped GPU workload. Use it as a repeatable
development workload when reporting a regression, alongside the hardware and settings above.
