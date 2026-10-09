# Performance and HUD layout checks

Two client gametests cover this. They are native runs, so they need a desktop session, or CI's client job.

```
./gradlew runClientGameTest -PfocusedSuite=dev.wildercord.gametest.perf.PerformanceHarnessTest
./gradlew runClientGameTest -PfocusedSuite=dev.wildercord.client.HudHandednessLayoutTest
```

## Performance harness

`src/gametest/java/dev/wildercord/gametest/perf/PerformanceHarnessTest.java`

Eight fake players stand on a bench beside the observer and work every pack at once, for 100 warm-up ticks and then 600 measured ticks:
- farming, delve, ward and hearth spells, cast in turn;
- two passive slots: slowburn and warm cloak;
- Aura forms and arts, used against Sword Masters.

The run records:
- server tick time (mean, p50, p95 and max);
- server and client entity counts;
- client particle counts;
- packets per tick, for the observer and for the fake players;
- every static map, collection or array in the mod, before and after.

The players then leave through the real disconnect path. After a grace period and a GC, the harness walks every loaded `dev.wildercord` class's static state for anything that still names one of them. A field that does is a leak and fails the test, unless it is in `KEPT` with a reason.

The results go to `build/perf/perf-harness.json` and to the log, on the line tagged `WILDERCORD_PERF`. The budgets are loose ceilings that only catch a blow-up: a mean of 40 ms, a p95 of 120 ms and a max of 2500 ms.

### Last measured run (Windows desktop, dev client, 8 players + observer)

| | |
|---|---|
| Tick time | p50 1.43 ms, p95 3.12 ms, mean 1.63 ms, max 9.35 ms |
| Server entities | mean 12.9, max 15 |
| Client entities | mean 4.9, max 7 |
| Client particles | mean 30, max 117 (the observer's view) |
| Packets/tick | observer 45.2, all fake players 340.7 (42.6 each) |
| Particle packets/tick | 328, carrying 459 particles |
| Arts landed | 88/140, with 700 swings |
| Retained after leave | nothing unexpected (`leaked {}`) |

Earlier runs fell between 1.6 and 2.2 ms mean and 2.8 and 4.7 ms p95. One max spike of 34 ms happened during warm-up class loading.

### Leaks the harness found, now fixed

These are state keyed by a player, or by any caster, that nothing released.

| Where | What was held | Fix |
|---|---|---|
| `cast/Effects` | `GLIDES` entries | The removal is now scheduled |
| `cast/ExplorerEffects` | `BLOODLUST`, `HEARTS`, `DRANK` | Pruned once expired (`spell/StatePrune`) |
| `cast/WayfarerEffects` | the `forget` path | Rewritten, so every map is released |
| `cast/packs/WardState` | zones and rests | Released on disconnect, plus a sweep every 200 ticks |
| `aura/MasterForms` | `MasterFormMovement` owners | `retire()` now forgets them |
| `cast/RuneReadings` | `RECENT` | Removed on disconnect |
| `wildlife/Wildlife` | `LAST_CAST` | Removed on disconnect |
| `cast/Innates` | `HURT_HISTORY`, `LAST_SPELL_ON` | Removed on disconnect |
| `cast/VoidTime` | `SPENT` | Removed on disconnect |
| `cast/Residues` | `RESTED`, keyed by any caster including mobs, so it was unbounded | Pruned once rested, and removed on disconnect |
| `cast/HeartCircles` | `LAST_SPELL_HIT` caster marks | A leaver's marks are dropped on disconnect |
| `runesmith/Contracts` | `LAST_HIT` caster marks | A leaver's marks are dropped on disconnect |
| `duel/Duels` | `LAST_HURT`, `LAST_PVP`, `LAST_DUEL` | Rests that have run are pruned at disconnect, past 256 |

The shared helper is `spell/StatePrune`. Under a soft cap of 256 it does nothing; above the cap it prunes the entries whose expiry or rest has run. Its unit test is `StatePruneTest`.

### Kept by design (`KEPT`)

These still name a player after they leave. Each one is bounded, and is there so that a relog cannot reset something:

- `Duels.LAST_HURT` and `Duels.LAST_PVP`: a relog never skips the duel readiness rest.
- `Statuses.CLAIMS`: a per-creature stacking guard, swept 600 ticks after its last grant.
- `WorldEvents.STORMS`: a live storm's per-player cast tally and given-rune set. They go when the storm ends.
- `Mastery.MEMORY`: so a relog doesn't reset diminishing returns. It holds one small entry per player per server run.
- `WardState.GRACE_SPENT`, `FAITHFUL_SPENT` and `AEGIS_SPENT`: save rests, swept once they have run.
- `WayfarerEffects.LOCATED`: the structure-search rest. It is dropped at leave once it has run.

## HUD layout and handedness

`src/gametest/java/dev/wildercord/client/HudHandednessLayoutTest.java`

The test works in a 1280x960 window. It runs every combination of:
- main arm: left or right;
- GUI scale: 1, 2, 3, 4 or auto;
- the attack indicator on the hotbar, with a torch in the offhand.

That gives three HUD phases:
- **Cord:** aura form, cord, spell, crossroads and string all shown together.
- **Aura alone.**
- **Stance.**

Each frame's GUI rectangles are captured along with the HUD that drew them. Every rectangle must stay inside the window and clear of vanilla's hotbar, offhand slot and attack indicator. With the cord up, the spell wheel, cord, aura and rune notebook screens are opened, and each must also fit inside the window.

The test also checks that the left-handed windup is the exact mirror of the right-handed one, for both the articulated combat pose and the first-person art animation.

### Layout bugs it found, now fixed

- **`StanceHud`** sat on the offhand slot or attack indicator at every scale. It now moves past them, using `AuraHud.aside`.
- **`AuraHud`**, alone and at a narrow width, fell back onto the hotbar. It now sits beside the hotbar, raised clear of it.
- **`SpellHud`** tucked into the hotbar row at a 320 px GUI width. It now rises above the hotbar, or above the offhand side.
- **`WayHud`** (crossroads): the backdrop and text ran off a 320 px wide GUI at scale 4 and auto. The backdrop is now clamped, and the text shrinks to fit.
