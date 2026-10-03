# Sustained mixed material benchmark

## Verified scene

The native `LivingWorldPerformanceTest` passed on 2026-10-03 (BUILD SUCCESSFUL, 2m23s). Each profile received a fresh practice world with 24 moving dummies and 64 paid-system casts alternating Pelt, Venom, Ember and Windcut Bolts. An AFTER_CAST observer confirmed all 64 were admitted. The measurement collected about 30 seconds of client frame samples and 512 server tick samples per profile at 1280 by 720, with VSync disabled and a 120 FPS cap.

| Profile | Frames | Median frame ms | p95 ms | p99 ms | Server median / p95 ms | Particle deliveries | Scheduled parts |
| --- | ---: | ---: | ---: | ---: | --- | ---: | ---: |
| Performance | 3586 | 8.32 | 10.07 | 11.02 | 0.79 / 1.76 | 2774 | 6 |
| Balanced | 3598 | 8.33 | 9.76 | 10.32 | 0.62 / 1.32 | 3901 | 9 |
| Cinematic | 3596 | 8.33 | 9.65 | 10.15 | 0.51 / 1.13 | 3927 | 11 |

All profiles recorded 64 formations, zero limited formations, zero decoration limit events, and zero idle/menu throttled frame samples. The compiled plan cache held 27 of its 256 allowed entries.

## Evidence and cleanup

Retained evidence lives in `artifacts/review/living-world-performance`: `native.log`, `results.json`, and the three `living_world_benchmark_*.png` screenshots. The test restores and checks all eight visual preference fields, VSync, FPS limit, inactivity policy, and HUD visibility. The existing `PerformanceProfilesTest` now uses that same complete settings snapshot; its previous partial cleanup left some visual and frame preferences changed.

## Interpretation and limits

These measurements establish successful sustained admission and bounded visual work in this particular local scene. They do not establish a speedup or a ranking of the profiles: the medians are close to the imposed frame cap. Packet totals are descriptive observations rather than a controlled attribution of quality cost. The scene uses one local client and moving dummies, not several connected players, a natural ecosystem, complex dungeon AI, an uncapped GPU stress run, or a full survival expedition. Those broader goals remain open in the roadmap. The screenshot captures and timing samples also do not prove that every spell has a completed authored impact and sound lifecycle.
