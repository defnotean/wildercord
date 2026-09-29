# Spell feel audit: storm and earth (49 runes)

Phase 1 (audit only). Branch `spell-audit-storm-earth`, based on local `main` at `e073939`. No game code, assets, lang, tests or other docs were changed; this file is the whole deliverable. Everything below was read from the code (paths under `src/main/java/dev/wildercord/`); nothing was profiled or listened to in game, and section 8 lists what I could not verify.

The request: every spell should have a unique feel, property, understanding and power, and display, so casting does not feel the same every time.

## 0. Summary

**Scope.** 49 runes: 18 storm (T1 3, T2 3, T3 12) and 31 earth (T1 5, T2 16, T3 10), counting fused, signature, innate and world runes. The element column of `Runes.all()` decides the group, so Frostwire, Thunderstep, Ripple, Surge and Thunderquake are in it and Heartstopper, Downdraft and Bloom are not.

**Power verdicts.** 4 UNDER, 4 under-ish, 5 OVER, 2 over-ish, 34 fair (mostly utility, control and situational runes). Each is judged two ways (section 1.2): value per mana against the median of the same role and tier taken from the other eight elements, with riders credited at what the cheapest rune selling them charges; and a parts test (could two cheaper runes in one spell do the same for less?).

| Verdict | Runes |
|---|---|
| OVER | Thunderbird (2.5 dmg/mana, 4.4x the T3 median, no input, no counterplay), Lightning (over-ish on one target at 1.5x with its burn, but each strike hits everyone near every strike: 5.4/mana at three clustered enemies, 15/mana at five), Ripple against undead (1.8/mana, 3.5x with the heal), Stoneskin (2.0 prevented/mana against 0.75 for the other T2 and T3 wards; 40% off for 1.44 mana/s as a passive), Stoneform in swarms (an area retaliation every 0.5 s from any damage) |
| over-ish | Aftershock (1.6x), Stalactite on a bare head (1.7x, and the hit ignores its own telegraph) |
| UNDER | Thunderclap (a Repel that costs a third more and throws no farther), Tempest (Harm + Push does 7 and a 12-block throw for 12 mana; Tempest is 22), Plasma (worse than Lightning at the same price), Surge (0.42x; Swift + Empower gives 2.8x the buff for the same mana) |
| under-ish | Riftbolt (damage 0.59x; about 1.0x credited, but Harm + Banish is 16 against its 18), Stormweave (0.30x alone, 0.63x at three), Monolith (0.63x, the purest earth fusion has the weakest T3 hit), Geode (0.6x to 0.9x: Stoneskin + Bramble is 9 to 12 against its 16) |
| fair, but underwhelming to cast | Jolt (fair value; a stun nothing shows and nothing uses), Frostwire (0.5x alone, 1.1x with two cold enemies, 1.6x with three), Fossilize (fair; identical numbers to frost's Hoarfrost), Galvanize, Stormheart, Pelt, Ripple on the living |

**Biggest clusters** (section 3): the storm "hit plus a rider" family (8 runes that are all an instant lightning hit with the same look), four T2 holds (Root, Weigh, Shackle, Mire) that are one verb, the ground-AoE family (9 runes, one crack-and-chips skeleton), five mitigation runes of which one (Geode) is beaten by two cheaper ones, and the world and mining set that shares one break sound.

**Cross-cutting patterns the lead should treat globally** (section 4):
1. Only two things in a cast depend on the effect: the first effect's element picks the `cast_<element>` sound and the spell circle's colour. Nothing else about the first 200 ms depends on it: the circle, the arm swing, the pose (keyed to the first rune only: an effect-first spell with an implicit Self thrusts) and the charge are identical for every storm rune and every earth rune.
2. Element-wide overlays run on top of every rune of the element: earth's HEAVE (crack, five slabs, a hop) on every harmful earth rune including the pin runes, and storm's CONDUCT. They make different runes look the same and sometimes contradict them (Mire, Sinkhole and Fossilize "sink" while HEAVE lifts).
3. Only 21 of the 49 runes (6 storm, 15 earth) play any custom event in their own code, and those are the element's generic `cast_<element>` or `impact_<element>` (or the shared `MAGIC_BREAK`, six mining runes); the other 28 use vanilla `SoundEvents` (trident thunder, mace smash, anvil, chains, block breaks). No storm rune has a sound that is its own, and among earth only Shield does.
4. Tier does not buy damage efficiency: single-target damage sits at about 0.67, 0.69 and 0.56 damage per mana at T1, T2 and T3. Tier buys area and utility, so a T3 rune that does not deliver either (Tempest, Monolith, Riftbolt, Plasma) is underwhelming by construction.
5. Show does not follow power in both directions: Thunderclap, Excavate, Riftbolt, Stormclock, Basalt Surge and Thunderbird show far more than they do; Pelt, Ripple, Jolt's stun, Stormheart's armed state and Surge's whole buff show less than they should.
6. Armour is an unlisted axis: Infest, Sandstorm, Tusk Charge, Basalt Surge, Monolith, Sinkhole, Fossilize and Stoneform deal magic damage that ignores armour; Pelt, Tremor, Thunderquake, Aftershock, Bonespur and Stalactite are reduced by it. No description says which.

**Broken or inconsistent (fix regardless)**, details in section 7:
- Lightning stacks quadratically on clustered enemies (Magma and Tempest were deduped in 0.4.3-alpha, Lightning was not).
- Stoneform's aftershock fires on any damage above zero (fire, poison, fall), not only blows from an attacker.
- Ripple heals for the damage it computed, not the damage the target took.
- Earth's HEAVE overlay lifts and slabs on pin runes (Mire, Sinkhole, Fossilize, Infest, Sandstorm).
- Excavate plays the Tremor screen shake even when it mines nothing.
- Lightning's description says it "stuns"; it applies Slowness IV for one second.
- Shock's arc victim skips the Conduct and Overload reactions the first target gets.
- A spell that starts with an effect (implicit Self) thrusts instead of using the Self pose.
- Thunderclap shakes the screen within 19 blocks even when it hits nothing.

## 1. Method and baselines

### 1.1 Rules that decide value

- **Mana and cooldown.** A spell costs shape + effects x shape multiplier (`SpellCompiler.cost`). Cooldown is one tick per mana of the whole spell (`SpellNumbers.cooldownTicks`, 0.5 to 20 s), so a 20-mana spell recovers in a second and mana (5 to 8 per second regen) is the only real limiter. Every rune below is judged by its own mana against the same shape multiplier, which cancels in every ratio.
- **Numbers are at power 1**: no Amplify, no heart circle bonus, duration and radius 1, against an unarmoured 20-health mob; burn counts 1 damage per second, and a hit is counted whole (burn, later pulses, second hits) when it is part of the rune.
- **Reactions** (`Reactions`, `ReactionRules`): storm damage on a wet target is Conduct (+50%, two 4-damage arcs within 5); on a burning target Overload (+30%, 4 damage and a throw to neighbours within 3). Earth damage on a frozen target is Fracture (+40%, and the target is Cracked: every spell hits it 20% harder for 5 s). Every harmful storm rune and every harmful earth rune takes part; no rune is chosen for it.
- **Climate**: storm x1.25 in a thunderstorm with open sky; earth x1.15 below y=0. **Creature affinities** (datapack tags): storm is strong against iron and copper golems and resisted by creepers; earth is strong against breezes and resisted by iron and copper golems, slimes and magma cubes. So `On Weakness` is nearly meaningless for both elements, and neither has the wide bestiary coverage of fire, frost or life.
- **Passives** (`Passives`): a passive costs 0.12 mana per second per point of rune cost; Stoneskin is allowed, and Shock, Ripple, Aftershock and Pelt through an Orbit.
- **Damage types**: which runes ignore armour (`indirectMagic`) and which do not is in section 4. The vanilla tag contents are from memory (the Loom jars in this environment do not carry the data tags).

### 1.2 Role definitions and reference numbers

References use the runes of the other eight elements only, unconditional numbers (the full lists and arithmetic are in appendix A).

| Role | Metric | Reference (median, range) |
|---|---|---|
| Single-target instant or short damage | damage per effect mana, T1 | 0.67 (0.38 to 1.00), n=7 |
| | same, T2 | 0.69 (0.50 to 1.38), n=10 |
| | same, T3 | 0.56 (0.31 to 1.00), n=16 |
| Area damage | damage per enemy per mana | 0.44 (0.22 to 0.83), n=13; a rune is good when it beats this at two or three enemies |
| Hard control (stun, freeze, sleep) | seconds of "cannot act" per mana | Decree 0.20, Freeze 0.16, Glacier 0.12, Drowse 0.43 |
| Soft control (slow, leash, root) | seconds per mana | Chill 1.5 (Slowness II), Weigh 0.62, Mire 0.62, Shackle 0.56, Root 0.33 |
| Buff | effect-level-seconds per mana (levels x seconds) | Swift 5.0, Empower 1.7, Regrowth 1.6, Overdrive 6.0, Accelerate 6.4, Warcry 2.4, Zephyr 1.3 |
| Mitigation | damage prevented per mana against 6 incoming damage per second | Brace 2.4 (25% uptime cap), Stoneskin 2.0, Shulkershell 1.4, Stoneform 0.8, Geode 0.75, Haven 0.7 |
| Summon or sentinel | damage per mana over the whole life | none in the group; Thunderbird 2.5 |

**Two tests decide a power verdict.**
1. *Role test*: credited value per mana against the median of the rune's own role and tier. Damage is credited at the tier median (1.5, 1.45 and 1.8 mana per damage point at T1, T2 and T3); a rider is credited at the price of the cheapest rune that sells it, and only when it is worth at least a mana or so: a hard stun second 4.8 mana (Decree 5.0, Freeze 4.6, Drowse 2.3, Glacier 8), a second of Slowness IV or a leash or a pin 1.7 (Root 3.0, Weigh, Mire, Shackle about 1.7), a second of Slowness II 0.67 (Chill), a throw 0.33 per block (Push: 4 mana for 12 blocks), a relocation 1.0 per block (Banish), a second of blindness 1.0 (Blind), a health healed 1.5 (Heal), an effect-level-second of a buff 0.42.
2. *Parts test*: could two cheaper runes in one spell do the same for less? The shape multiplier is the same on both sides, so the comparison is between effect costs.

Bands: fair 0.8x to 1.3x; under-ish 0.6x to 0.8x; UNDER below 0.6x, or dominated (a sibling of the same tier that costs no more does the same and more); over-ish 1.3x to 1.7x; OVER above 1.7x, or unbounded (no cost, no input, no counterplay). Area runes are judged per enemy per mana at two or three enemies. Utility (mining, sensing, redstone) is judged on cost against the closest alternative, not on damage. Where the two tests disagree the verdict is under-ish or over-ish and the row says why. The credited values are in appendix A9.

**Presentation budget** (used in the show verdicts). A particle packet ("pk") is one server packet: a light or sigil primitive is 1, `radial(n)` is n, `emit(count n)` is 1. Counts are read from the code per target hit at power 1 (agents read every Vfx function and summed calls; they are estimates, not measurements). As a proxy for "scale follows power": packets per damage point. A poke sits at 2 to 5, a T3 finisher at 4 to 8; above about 12 the show is out of proportion (Thunderclap 14, Monolith 12, Riftbolt 23, Thunderbird 23, Stormclock 32). Sustained emitters above about 30 pk/s per effect, and more than 3 sound events per hit, are flagged as noisy.

### 1.3 What each cast already carries (not repeated per rune)

- `Vfx.castCircle` (from `SpellCaster`): the spell's own circle under the caster (28 + n ticks), 6 enchant motes, a hand glow, `CIRCLE_OPEN` 0.35 and `cast_<element of the first group's first effect>` 0.55. About 8 pk and 2 sounds.
- `Vfx.touched` on every non-self target: a 0.9 glow and 3 element motes (storm: electric sparks, earth: crit).
- `Effects.hurt` sends the caster a screen punch (`min(1, damage/20)`) for any single hit of 8 or more.
- `WorldMagic.onSpell` after every effect: storm runs CONDUCT (through water: 4 damage to every creature in the same water, up to 8; copper and rods; 25% creeper charge), earth runs HEAVE (5 slabs, crack, a 0.45 hop for grounded enemies within 2). Up to 3 heaves (15 slabs) per cast.
- A shape's own visuals and vanilla shape sounds (bolt streak, beam circle, ring, ...).

## 2. Verdict table, one row per rune

Legend: `dmg/mana` is damage per mana of the effect rune; `x` after a number is the ratio to the tier reference (damage only, unless it says credited; the credited values are appendix A9); `pk` = particle packets; `HEAVE` and `CONDUCT` are the element-wide overlays of section 1.3; "vanilla" sounds are `SoundEvents.*`, "custom" are the mod's `cast_/impact_<element>` events. Overlap names runes of other elements too, on purpose. Tables 2C (proposals) and 2D (the new signature of each rune at a glance) follow the two element tables; sound ids `Sxx` are in section 5.

### 2A. Storm (18)

| id | T | cost | role | key numbers (power 1) | power verdict | verb | overlap | presentation today | show verdict |
|---|---|---|---|---|---|---|---|---|---|
| shock | 1 | 7 | single-target poke, two targets | 4, plus 3 to the nearest other enemy within 4. Conduct and Overload only on the first hit. Orbit-passive legal | fair: 0.57 dmg/mana (0.86x), 1.0 with a neighbour (1.5x) | arc to a neighbour | Chain modifier (this is Bolt+Chain x1 for a T1 price); the T1 4-damage pokes (Pelt, Windcut, Icicle) | small jagged yellow arc, 7 pk (20 with a neighbour), `TRIDENT_THUNDER` 0.25 pitch 1.9 | right scale; the arc unit (`Vfx.shockArc`) is also Jolt's, Stormheart's and Thunderbird's; vanilla sound |
| jolt | 2 | 9 | control (hard stun) plus poke | 4, and a 1 s stun (mob AI off; players Slowness VII + Weakness V; bosses slowed) | fair: 0.44 dmg/mana on its own (0.65x), 1.18x once the 1 s stun is credited at the market 4.8 mana a second (it sells it for about 3); it is only weak to cast | stun | Decree (arcane), Freeze and Glacier (frost), Thunderstep and Heartstopper stuns, Fossilize's hold | two crossing arcs, flash, ring, 10 sparks (about 26 pk), 3 vanilla sounds; no stun cue, no end cue | over-sounded, and the stun (its whole point) is unmarked |
| thunderclap | 2 | 12 | area knock | 5, a throw of about 11 blocks (push 2.0 and 0.5 up; Push, 4 mana, throws 12), Slowness IV 1 s, windswept; radius 3 at the impact point, through walls | UNDER: dominated. 0.42 dmg/enemy/mana (0.93x area ref), but 0.75x Repel, which is the same 5 damage in radius 3 with a throw of 8 to 13 blocks, ignoring armour, for 9 | knock-away ring | Repel (wind: the same, cheaper, throws farther), Push, Summit Wind, Tempest | flash, 2 rings, 4 forks of 5 segments (69 pk, 44 of them rays), screen shake 0.55 within 19 blocks even on a miss, vanilla thunder | loud for 5 damage (14 pk/dmg); Lightning's skeleton with more forks |
| lightning | 3 | 20 | single-target burst plus burn (plus splash) | 12 at each target, Slowness IV 1 s, burn 4 s (+4); a strike hits everything within 2 blocks (3 up and down) of every strike, up to 8 strikes | over-ish on one target: 1.07x for the strike, 1.5x with the burn and the slow. OVER in crowds: 3 clustered enemies take 108 in total (5.4/mana), 5 take 300 (15/mana); not deduped | sky strike and burn | Plasma, Tempest, Thunderstep, Smite (arcane), Explode (fire) | vanilla bolt entity, white flash, 3 ground forks, 2 rings, 14 sparks, soot: about 41 pk per strike (up to 320 pk and 8 bolts in a cast), one impact sound, no shake | right scale for one target; defines "plain lightning" that ten runes copy |
| ripple | 2 | 10 | drain poke | 6 (x3 on undead), heals min(10, dmg/3) of the computed damage, whatever the target actually took | fair on the living: 0.60 dmg/mana (0.87x), 1.17x with the heal credited. OVER on undead: 1.8/mana (2.6x), 3.5x with the 6-health heal | drain (sun) | Leech and Lifesteal (blood), Smite (x2) and Halo (x3) in arcane | gold glow, 3 tilted rings, wax flecks, 10 sparks (15 pk), `BEACON_POWER_SELECT` | plain; the heal and the undead triple are both invisible; gold, not storm, palette |
| thunderbird | 3 | 20 | summon (sentinel) | 15 s; every 1.5 s strikes the nearest enemy within 12 that the caster can see: 5 and Slowness III 1 s; 10 strikes = 50; up to 3 birds; the bird cannot be hit | OVER: 2.5/mana (4.4x). Extend gives 3.6. Free after the cast, no counterplay | sentinel | Summon (arcane), Shades (void), Halo, Thunderhead, Rain shape | orb, 2 wings, tail every 2 ticks (43 pk/s, about 655 pk per bird) plus a strike each 1.5 s (arc and impact, about 50 pk, 2 sounds): about 1,150 pk and up to 37 sounds per bird; 3 birds = 130 pk/s | heaviest sustained emitter in the group, 23 pk per damage; lag risk |
| stormheart | 1 | 12 | innate: retaliation | 10 s: a living attacker (any range, arrows too) is struck for 6 x heart-circle scale, at most once a second | fair: 12 to 36 typical (1 to 3/mana); an innate, so luck decides who has it | retaliate | Bramble (life), Stoneform, Geode, Reflect | cast: short bolt, shrinking ring, sparks (13 pk), `TRIDENT_THUNDER`; retaliation: vanilla bolt and an arc; no cue for the armed 10 s, none when it ends | needs a visible stance |
| galvanize | 1 | 3 | world: circuit | a redstone block against the struck face for 5 s (air only, no drops, pistons cannot move it) | fair (utility, unique) | power a circuit | none | flash and 12 sparks, hum ticks, silent removal (13 pk), vanilla impact 0.3 | bare; the expiry is silent |
| tempest | 3 | 22 | fused storm and wind: hit plus fling | 8 (Conduct, Overload) and a throw of 2.8 x sqrt(power) plus 0.9 up (about 18 blocks by the vanilla drag model), windswept; once per creature per tick | UNDER by the parts test: Harm + Push (12 mana) gives 7 damage and a 12-block throw; Tempest is 22 for 8 and 18 blocks (0.55x). Role test alone: 0.36 dmg/mana (0.65x), 0.92x with the fling credited | fling | Push, Repel, Summit Wind, Thunderclap | vanilla bolt, stormImpact(1.4), gust rings, 5-slash swirl (about 39 pk per strike), shake 0.5 per strike (up to 8 shakes), 2 vanilla sounds | fine scale for T3; shakes stack |
| plasma | 3 | 20 | fused storm and fire: single target | 4.5 lightning plus 4.5 armour-ignoring magic; Conduct and Overload apply | UNDER: dominated. 0.45/mana (0.80x); Lightning costs the same and does 12 + 4 burn against Plasma's 9. Ahead only against armour 20 (5.6 to Lightning's 3.8, but 7.8 with the burn) | armour cutter (intended, not delivered) | Lightning, Sonic Boom, Sanguine Rite, Dismantle | orange-sheathed gold bolt, white orb, heat flare, 12 sparks (41 to 61 pk per target, uncapped), 2 vanilla sounds | reads as orange lightning; its bolt call is Riftbolt's |
| surge | 3 | 16 | fused storm and life: buff | Speed I and Strength I for 8 s | UNDER: 1.0 level-s/mana against 1.3 to 6.4 for siblings (median 2.4: 0.42x); Swift + Empower (18 mana) is 50 level-s to its 16 | none (a generic buff) | Swift, Empower, Overdrive, Accelerate, Warcry, Zephyr | short yellow bolt with a green halo up the body, 2 rings, 10 sparks (30 pk), 1 vanilla sound | an 8 s buff shown for 0.3 s |
| magnetize | 3 | 16 | fused storm and earth: control and damage over time | 4 s: enemies within 5 are dragged to the target (max 0.28 blocks/tick, never bosses or through walls), each one touching takes 3 a second; up to 4 magnets; marks Pulled | fair: 0.56/mana alone, 1.7 with three touching (3 x the 0.44 area ref = 1.3); feeds Implode and Collapse | clump | Gravity Well, Singularity, Vortex, Pull (void), Sinkhole | lodestone seal, 4 turning field loops, iron nuggets, gold threads, shock arcs: 134 pk per 4 s idle (33 pk/s per magnet, 32 pk per shock); vanilla lodestone sounds and one custom storm impact | distinct silhouette; load grows with the crowd |
| riftbolt | 3 | 18 | fused storm and void: hit plus relocation | 6, Darkness 3 s, target torn up to 5 blocks along the bolt (first 8; never bosses; only to a safe spot) | under-ish: 0.33/mana damage (0.59x); credited 1.04x, but Harm + Banish (16 mana) does 7 damage and 8 blocks against its 6 and 5 for 18 | tear | Banish, Portalfall, Warp, Shadowstep | violet-white bolt on a black under-stroke, an entry implode, an exit ring and portal spray: 110 to 136 pk per target, 4 sounds | strongest silhouette in the group, and the biggest show for 6 damage (23 pk per damage) |
| stormweave | 3 | 18 | fused storm and arcane: mark then web | marks up to 4 enemies within 6; 0.75 s later 3 + (n-1) each | under-ish: alone 0.17/mana (0.30x); two enemies 0.22/enemy/mana (0.5x); three 0.28 (0.63x); four 0.33 (0.75x) | web | Constellation (arcane shape), Resonance, Starfire, Chain | pink star over each mark, noose and threads; then a web of bolts on every pair and a star flare at each knot in one tick (110 to 170 pk); the two sounds are arcane | good concept; a one-tick spike; sounds off-element |
| stormclock | 3 | 16 | fused storm and time: delayed zone | 4 now, then 3 at 2 s and 4 s within 1.5 of the spot: 10 if it stays put | fair: 0.62 stationary (1.1x), about 0.44 against movers (0.78x) | punishes standing still | Countdown, Imprint, Primer, Doomclock, Thunderhead | sky clock face, hand tick every 5 ticks, ground reticle, bell, falling bolt: about 322 pk per spot, 25 sounds per spot (14 comparator clicks, 3 bolt triples); up to 4 spots = about 1,290 pk and 100 sounds | best silhouette of the group; over-noisy |
| thunderhead | 3 | 20 | fused storm and storm: roaming zone | a cloud follows its target for 4 s; each second a random enemy within 4 of it takes 3 (4 strikes = 12); does not scale with the crowd | fair: 0.60/mana (1.07x) on one enemy, flat 0.6 at three (0.45x of the area ref of 1.3) | roaming cloud | Thunderbird, Rain shape, Sandstorm and Blizzard zones | slate billows, falling water, flashes every 4 ticks (21 pk/s), a forked bolt each second (41 pk): about 260 pk per cloud, 62 pk/s, up to 4 clouds; 10 vanilla sounds | right silhouette; highest per-tick cost of any zone |
| frostwire | 3 | 18 | signature chill and shock: multi-target | chill all (Slowness II 4 s, brittle); 0.3 s later a current runs through every chilled or frozen enemy within 6 of each origin (up to 4 origins, chain up to 6): 4, or 6 if frozen solid | fair, crowd-dependent: credited 0.54x alone, 1.09x with two cold enemies, 1.63x with three (the slow on every target is worth about 2.7 mana each) | current through the cold | Stormweave, Chain, Shatter set-ups | frost seal, snowflakes; pale blue arcs with ice shards and yellow sparks: 88 pk for a full chain | distinct palette; the touch glow is yellow (its element is storm) over blue |
| thunderstep | 3 | 18 | signature shadowstep and lightning: movement | teleport up to 24 blocks to behind the first enemy hit (or the point); 8 storm within 2.5, 0.5 s stun; on Self it strikes where you stand | fair: 0.44/mana as area (0.79x) with the blink included (Shadowstep alone is 12) | blink-strike | Shadowstep, Blitz, Tusk Charge, Blink | dark implode at the start, vanilla bolt, 9 m column, stormImpact(2.5), 5 flat arcs (108 pk), shake 0.5 always, custom blink and vanilla impact | good; the same bolt as Lightning |

### 2B. Earth (31)

| id | T | cost | role | key numbers (power 1) | power verdict | verb | overlap | presentation today | show verdict |
|---|---|---|---|---|---|---|---|---|---|
| shield | 2 | 12 | ward (spell block) | one harmful spell costing no more than the raising spell is stopped, 30 s; a costlier one breaks it; circles = strength/8 (2 to 7); a raise within 7 ticks parries | fair, niche: no value against mob melee or arrows, strong in PvP and against Runebound and bosses | block one spell | Reflect, Foresight, Infinity, Haven | best in the mod: amber circle stack, glass-shatter physics (about 66 shards a circle), gold parry, 5 custom sounds; invisible while idle by design | excellent; amber rather than earth |
| break | 2 | 4 | world: mine one | one block up to iron hardness (Amplify: diamond) | fair; Chisel + Amplify (3.2 mana) does iron for less | mine | Chisel, Excavate, Tunnel, Vein | vanilla break burst and `MAGIC_BREAK`, no mod particles | plain |
| stoneskin | 2 | 12 | ward (buff) | Resistance II 10 s (40% off); passive-legal at 1.44 mana/s | OVER: 2.0 prevented/mana at 6 dps (Geode 0.75, Stoneform 0.8, Haven 0.7 = 2.5 to 3x); a permanent 40% for a quarter of a Twine cord's regen | armour | Brace, Stoneform, Geode, Barrier, Anchor | cracked seal, 2 closing rings, flash, chips (14 pk), `ARMOR_EQUIP_NETHERITE`; a self-cast shows the caster only the floor crack; Stoneform's cast is the same call | fine, but shared with Stoneform |
| root | 2 | 9 | control (hold) | Slowness VII 3 s (no movement; it can still hit what is next to it), vertical motion clamped | fair: 0.33 s/mana of full immobilisation | hold | Weigh, Shackle, Mire, Rootsnare (life), Freeze | crack, 4 vine spirals of 3 crescents in green, moss (24 pk), `AZALEA_LEAVES_PLACE`; the cue ends at 1.5 s of 3 | readable; ends early; green on an earth rune |
| tremor | 3 | 18 | area damage | 8 in radius 4 to grounded enemies, thrown up 0.75 | fair: 0.44/enemy/mana (0.98x) | ground eruption | Thunderquake (its upgrade), Thunderclap, Explode, Basalt Surge, Monolith | cracked seal, 2 wide rings, up to 4 stone spires (block displays), shake 0.7 within 20, screen punch, heavy mace sound (about 21 pk and 8 to 13 displays) plus a second crack and 5 slabs from HEAVE | biggest earth display for 8 damage; duplicated by HEAVE |
| excavate | 2 | 10 | world: mine wide | 3x3 up to iron hardness | fair | wide mine | Break, Tunnel | plays `Vfx.tremor` at radius 1.5 even when nothing was mined: shake 0.45 within 12, crack, 2 rings, heavy mace sound, up to 9 break bursts, up to 11 sounds | mismatched: a screen shake for digging |
| aftershock | 2 | 9 | single-target damage | 5, then 5 again 0.5 s later (+0.35 up); armour applies; Orbit-passive legal | over-ish: 1.11/mana (1.6x), the best instant non-burn single-target at T2 | double hit | Echo link, Dismantle, Barrage, Blazecall | first hit: generic `earthImpact` (9 pk); second: crack, 2 shockwaves, shards (26 pk); 2 vanilla sounds | the second hit is the moment; the first is generic |
| weigh | 2 | 8 | control (crush) | gravity x3, speed -60%, jump -70% for 5 s; fliers pushed down every 5 ticks | fair: 0.62 s/mana of soft control | press down | Root, Shackle, Mire, Downdraft (wind) | anvil dust and a contracting ring every 5 ticks for 5 s (8 pk/s), cracked seal at the start, `ANVIL_LAND` | readable weight; quiet |
| shackle | 2 | 9 | control (leash) | chained to the spot for 5 s: pulled back past 2 blocks, teleported back past 5; bosses only slowed | fair: 0.56 s/mana of soft control | leash | Root, Weigh, Starlight Tether (arcane) | steel ray and beads to the anchor every 4 ticks (10 to 30 pk/s), `CHAIN_PLACE`, `CHAIN_HIT` while stretched; grey, not earth | good silhouette (a chain); grey palette |
| rampart | 2 | 8 | world: cover | a 5-wide, 3-high wall of packed mud for 10 s (skips columns with a creature in them; 15 blocks of the 32 budget) | fair | cover | Wall shape, Haven, Shield | no mod particles; 3 rows pop with vanilla break bursts; up to 15 vanilla sounds at once on raise and again on crumble | plain; sound spam |
| brace | 1 | 4 | ward (timed) | Resistance IV 2 s (80% off), then a 6 s lockout | fair: 2.4 prevented/mana, best at T1, but 25% uptime and a 2 s window | brace for the blow | Stoneskin, Stoneform, Foresight, Anchor | chest ring, floor circle for the 2 s, ground ring, 10 chips (13 pk), anvil and mace sounds; a "spent" cue exists | right scale |
| chisel | 1 | 2 | world: mine one | one block, stone hardness (Amplify: iron) | fair | mine (careful) | Break | ring on the face, 8 chips, 4 crit, 3 sounds (13 pk) | fine |
| tunnel | 2 | 8 | world: bore | 2 high, 4 deep (8 blocks), iron hardness | fair | bore | Excavate, Break | a circle on the wall, a ring and 6 chips per cell (about 59 particles), up to 14 sounds | busy but readable |
| vein | 2 | 10 | world: ore vein | the block, and for an ore every matching ore joined to it (16) | fair | follow the vein | Break, Fell | gold thread, flash, chime per block; 3 sounds a tick at 16 ores; pale gold | fine; sound-heavy |
| fell | 2 | 8 | world: tree | the log and every log joined above (32) | fair | fell | Vein | a horizontal crescent that ignores the aim, then a green ring and villager sparks per log; up to 48 sounds in 16 ticks | wrong palette (green), sound-heavy |
| pelt | 1 | 5 | single-target poke | 4 and a knock of 0.6 x sqrt(power) (about 2.7 blocks; Windcut's shove is 2.8) | fair: 0.80 (1.2x) | shove | Windcut (6 mana, 4 damage, 0.7 shove: a near clone), Icicle, Harm | 10 cobble chips, 1 ring, 5 crit (12 pk), `STONE_BREAK`; HEAVE adds 3x as much (crack, 5 slabs, 20 puffs, a sound) | plain and swamped by the overlay |
| stoneform | 1 | 12 | innate: stance | 8 s: Resistance I, no knockback; every damage taken above zero (any source) sends 3 x scale to every enemy within 3, knocked back, at most every 0.5 s | OVER in swarms (up to 16 procs of 3 on each enemy in reach = 48 each; fires on fire, poison and fall damage too); fair in duels | stand your ground | Brace, Stoneskin, Bramble, Anchor | cast = Stoneskin's call unchanged plus 12 deepslate chips; each proc = a double ring, crack, heavy mace (15 pk); expiry silent | no stance cue; the same cast as Stoneskin |
| magma | 3 | 16 | fused fire and earth: pool | up to 4 pools of radius 1.6 under targets, 4 s: 2 a second and ignite (once per enemy per second across pools) = 8 + about 5 burn | fair: 0.81/mana standing still (1.45x credited), about 0.8x against enemies that walk out of the small pool | floor hazard | Inferno, Hellmouth, Sandstorm, Blizzard, Sinkhole | crack, a fire-red cracked sigil, magma chips (22 pk), then per second a lava pop, flame and ring (3 pk/s); 7 sounds per pool (28 for 4) | the plainest pulse in the list |
| sinkhole | 3 | 18 | fused earth and void: pit | drags enemies within 3 to the middle over 0.6 s, pins them 2 s (Slowness IV, no jumping), then 5 | fair: 0.28/mana damage but the pin is worth 2 s per enemy: 0.68x per enemy credited, 2.05x with three caught (up to 8) | pit and pin | Gravity Well, Singularity, Magnetize, Vortex | dark disc widening in 3 steps, 6 ground slabs tipping in, earth and void crush (about 117 pk plus 13 per enemy, 6 displays), 10 sounds, 2 shakes | strong silhouette; costly |
| geode | 3 | 16 | fused earth and arcane: ward | Resistance II 5 s and 2 damage to whatever strikes the ally | under-ish: credited 0.86x against one attacker (0.5 hits/s), 1.8x against three; as a ward 0.75 prevented/mana against Stoneskin's 2.0 (0.38x); Stoneskin + Bramble for 5 s is 9 mana against 16 (0.56x by parts) | crystal armour | Stoneskin, Reflect, Bramble, Frostbloom | 6 amethyst displays, violet cage of 18 rays, seals (31 pk and 6 entities); each hit 3 rays and 8 shards; the wearer cannot see the cage (eye clearance) | good look; purple is fine for an arcane blend |
| fossilize | 3 | 16 | fused earth and time: slow petrify | Slowness I, II, III over 3 s, stone hold 2 s (1 on players), then 6 | fair: 0.38/mana damage (0.67x) but a 2 s hard hold (1.39x credited, 0.98x after discounting the 3 s delay); the same numbers as frost's Hoarfrost | petrify | Hoarfrost (frost, identical numbers), Freeze, Glacier | grit rings, falling dust, a gold clock slowing, stone shell display, chips and shake (about 115 pk over 4 to 5 s), 10 sounds | best fit of fantasy; good anticipation; heavy |
| bonespur | 3 | 16 | fused earth and blood: multi-strike | up to 4 enemies within 4: 5 (armour applies) and bleed 3 s = 8 each; leaves the Bleeding mark (Rupture) | fair, top of earth area: 0.5/enemy/mana (1.1x), 2.0 in total at four | spurs from below | Thunderquake, Tremor, Razorgale, Crimson Mist | bone ring, cracked seal under each, 5 converging spurs with blood, drops and a heartbeat ring (about 45 pk per target, 13 sounds for 4) | strong; about 190 pk in a burst |
| monolith | 3 | 20 | fused earth and earth: uplift | 6 and a 3-block throw (lift 0.69); a 4 s pillar of block displays; bosses not moved | under-ish: 0.30/mana (0.53x); 0.63x with the toss credited; Harm + Launch (16 mana) is 0.8x of its price | uplift | Launch, Updraft, Rampart, Stalactite | biggest earth display: seal, gold seams, 5 block displays (plinth, drums, capital) standing 4 s then crumbling: about 72 pk and 5 displays per target, 10 sounds, shake per target | matches the fantasy; 12 pk per damage |
| thunderquake | 3 | 20 | signature thunderclap and tremor: three pulses | waves at 2, 4, 6 blocks, 4 each, thrown up 0.35: 12 at the heart, 8 at 4, 4 at 6 | fair: 0.6, 0.4, 0.2 per mana by distance; 1.0 to 1.2x Tremor for 2 more mana | three pulses | Tremor, Thunderclap, Explode | cracked seal, bolt from above, three expanding earth and storm rings with chips and flat arcs (about 119 pk plus 7 per enemy per wave), 3 shakes (0.45 to 0.65), all vanilla sounds | distinct rhythm; the only earth rune here with no custom sound |
| infest | 2 | 9 | single-target damage over time | 1 every 0.5 s for 4 s (8, magic: ignores armour) and Slowness I | fair: 0.89/mana (1.3x) | gnaw | Bleed (blood: 10 in 4 s for 8), Venom | cracked seal, 10 chips, 8 silverfish particles, then 6 particles a pulse (12/s); 9 vanilla silverfish sounds | readable, small |
| sandstorm | 3 | 18 | area zone | 4 s, radius 3.5: 2 a second (4 pulses: 8 total, magic), blind, Slowness II | fair: 0.44/enemy/mana (1.0x) | choking zone | Blizzard (frost), Eclipse (void), Steam (fire), Dust Devil (signature) | cracked seal and dust burst, then 19 pk every 5 ticks (76 pk/s, about 336 in all) plus billows; 6 sounds | heaviest per-second emitter of any earth rune for 8 damage |
| tusk_charge | 2 | 9 | movement plus damage | a charge of up to 8 blocks; each enemy in the path takes 5 (magic) once and is tossed | fair: 0.56/enemy/mana plus the movement (Blitz does the same for 6 with any effect) | charge | Blitz, Dash, Thunderstep, Blink | chip wake, crescents in front, cracked mark at the start, an `earthImpact` per toss; hoglin sounds and `cast_earth` | fine |
| mire | 2 | 8 | control (bog) | Slowness IV, jump -90%, 5 s, and Soaked (wet for Conduct) | fair: 0.62 s/mana and it feeds storm | bog | Root, Weigh, Shackle, Tidecall (frost) | cracked seal 2 s, 12 mud chips, bubbles, then 14 pk/s; one sound in 5 s; HEAVE hops the target up | quiet; overlay contradicts it |
| stalactite | 2 | 9 | single-target burst (delayed) | 0.4 s later 7, x1.5 on a bare head (the norm): 10.5; always lands on the target though the reticle marks its old spot | over-ish: 1.17/mana bare (1.7x), 0.78 with a helmet | drop from above | Meteor, Countdown, Primer | drips, reticle, 8-tick telegraph, a 4-tick streak and burst (25 pk), 2 vanilla sounds | good telegraph that the hit does not honour |
| basalt_surge | 3 | 18 | area line | a line of up to 12 columns over up to 16 blocks; 7 (magic) and a toss to each enemy once | fair: 0.39/enemy/mana (0.87x) | eruption line | Pillar, Wave and Lance shapes, Tremor | a rolling line of basalt columns with a fire core, chips, ash, cracks: 27 pk each, about 324 pk in 12 ticks (540 pk/s), one sound per column (12 in 12 ticks) | great silhouette; the noisiest burst in the group |
| prospect | 1 | 3 | world: sense | every ore within 12 blocks (up to 48, 16 widened) glows through rock for 20 s | fair (utility) | sonar | Treasure Sense (arcane) | 2 rings, a target sigil, up to 48 block displays at once, bell and resonance; the ring takes 16 ticks but the glows are instant | ok; the glow should follow the ring |

### 2C. Proposals, one row per rune

Effort: S is under half a day of code, M about a day, L more. "Bug" rows are in section 7 as well. Every mechanic reuses a system that exists (marks, `unstacked`, `Spirits.hold`, `Scheduler.later`, `ElementFx.arc`, the reactions). Numbers keep each rune inside its role band; where a price is added it is named.

**Storm**

| id | mechanics: numbers and the twist | new presentation signature (silhouette, motion, palette accent, timing, sound) | effort |
|---|---|---|---|
| shock | Keep 4 + 3. The arc looks for a conductor first (a wet or soaked enemy, or one in two or more pieces of metal armour) within 5, else the nearest within 4; an arc that finds one deals 4. The arc victim also gets Conduct and Overload (bug). Verb: it finds the conductor | The arc stays 6 ticks as a crackling tether with a small node flash at each end (one `ElementFx.arc` packet), then a second, higher zap; white-yellow, thin; S1 | S |
| jolt | 4 and a 1 s stun (x1.5 when the target is wet). It interrupts: cancels a Runebound or boss telegraph (`DungeonBoss.interrupt()`, the Runebound windup) and a charging player's charge (`Charging.stop`), and locks that caster out for 1.5 s. Verb: the counter-spell. Cost stays 9 | While stunned, a ring of four tiny arcs turns over the head (1 pk per 4 ticks) and clamps shut when the stun ends; a reversed-cut sting only when it cancels a cast. The three sounds become one: S2 | M |
| thunderclap | Flash, then crack: a 5-tick telegraph (a white contracting ground ring), then 5 damage. Enemies within 3 are staggered 0.5 s (`Spirits.hold` 10) and mobs forget their target for 2 s; the throw drops from 11 blocks to 4 (knock 0.8), so Repel keeps "throw" and Thunderclap owns "the stun ring". Cost 12 | Flash disc, then one wide ground ring and 2 forks (69 pk to about 30); shake 0.55 to 0.3 and only when something is hit; S13 (short) into S4 | S to M |
| lightning | Dedupe like Magma and Tempest (`FusedEffects.unstacked`): each enemy takes its strongest strike once (12); an enemy near a strike aimed at someone else takes 50% once. "Stuns" becomes "slows" in the text. No other price | Flash first, thunder lag (S3: crack now, roll 8 ticks later); the full bolt and forks for the first 3 strikes, a light version after (320 to about 140 pk); scorch fades over 2 s | S |
| ripple | Undead x3 to x2 (Smite's parity); heal = a quarter of the damage actually dealt (cap 6); a second ring 8 ticks later deals 3 to other enemies within 2.5 of the target and heals you 1 each (max 3). Verb: the drain ripples outward. (Lead's call: the sun theme belongs to arcane or life; moving its element costs nothing in code) | Sun-gold and white rings expanding from the target, heal motes streaming back to the caster (the heal finally visible); drop the storm sparks; S16 | M |
| thunderbird | Max 2 birds, 12 s. Every 2 s it dives on the enemy you last hit (else the nearest it can see), with a 0.5 s telegraph (a ground ring under the victim; the strike lands on the marked spot, so it can be dodged): 6 damage and Slowness III 1 s. It vanishes if you take one blow of 6 or more (concentration). At most 6 dives = 36 (1.8/mana at full hits, about 1.0 realistic). Cost 20 | Emitter every 4 ticks (43 to 12 pk/s), raptor cry at arrival (S6) and a falling whistle per dive (S6b), strike = one arc and a small impact (50 to about 20 pk), a faint wing-static hum (S7) instead of 16 flaps | M |
| stormheart | Retaliation 5 plus an arc of 3 to one enemy near the attacker; blows under 2 damage are ignored; still at most once a second | The armed stance is visible: a small charged ring orbits the chest (1 spark per 10 ticks, 3 pk/s, seen by others), a crackle on each retaliation, a pop when it ends; S15, S1 | S |
| galvanize | No numbers. Extend works (already); a lightning rod within 2 blocks of the spark is pulsed (storm already does this on hit) | A crackle halo around the block for the 5 s, a discharge ring with sparks and S15 when it expires | S |
| tempest | 8 and the fling (about 18 blocks), then a second bolt where the target lands (or after 2 s, whichever comes first): 4 to it and to anyone within 2 of the landing, Conduct and Overload apply, so a pile-up is the point. Verb: strike, fling, strike. Cost stays 22 (credited 1.24x with the second strike) | The fling reads as a spiral gust carrying a bolt; the landing is a delayed thunder (S4) and the fling S5; one shake per cast instead of one per strike | M |
| plasma | 10 (5 lightning + 5 armour-ignoring); cost stays 20 (credited 0.99x). Ionised: the target counts as wet for Conduct for 5 s (a new `IONISED` mark, or `Mark.WET` without its fire penalty), so Plasma primes the next storm hit. Verb: the storm primer | A straight, steady beam (no forks: plasma does not jag), white-hot core in a magenta-violet sheath (not orange, not Riftbolt's black), a 5 s violet glow on the target, a heat-haze ring; S12 | S to M |
| surge | Speed I and Strength I for 8 s, plus static discharge: your melee hits arc 2 to one enemy within 4, once every 1.5 s, at most 4 arcs (8 damage): Searing Edge's pattern for storm. Cost 16 (credited 1.31x) | A crackling aura for the whole duration (1 spark per 8 ticks, 4 pk/s) and glowing hands; a rising whine (S13) at the cast, S1 on each arc | M |
| magnetize | Iron, chain or gold-armoured enemies are pulled 1.5x harder and shocked for 1 more. Pulse every 8 ticks not 4 (same damage per second) | A magnet whine loop (S14, pitch rises with the drag), field loops every 12 ticks; keep the iron nuggets and gold threads; about 60% fewer packets | S |
| riftbolt | 6 to 7 (credited 1.14x). The rift leaves a dark strand between the entry and exit for 3 s (one dark `ElementFx.arc`, lifetime 60): an enemy crossing within 0.8 blocks takes 2 storm per 0.5 s. Verb: a black wire | Keep the black under-stroke; only the first 2 targets get the full tear (110 to about 60 pk); S11 reversed at the entry, forward at the exit | M |
| stormweave | Base 3 to 4 (+1 per other), delay 15 to 12 ticks; a lone target is anchored to the ground for +2 (so solo is 6, n=4 is 7 each = 28). Verb: the web | Each mark plucks a wire note (S9: D E F# A over 4 ticks); the web's bolts stagger over 3 ticks (a 170-pk spike becomes 3 x about 55); zip S1; the sounds become storm's | S |
| stormclock | Numbers as they are. Striking twelve: an enemy hit by both later strikes is stunned 0.5 s on the last. Verb: punishes standing still | Hand ticks every 10 ticks (7 not 14), one bolt sound per strike instead of three, the reticle stays; S10; about half the packets | S |
| thunderhead | Strike 3 to 2, but the cloud soaks whoever it strikes (`Mark.WET` 40 ticks) so its bolts Conduct (x1.5, arcs to 2 within 5): 3 alone, 12 in all as today, far more in a crowd. Cost 20 | A rain curtain under the cloud and puddle rings; emitter every 8 ticks (62 to about 30 pk/s); S8 rumble, S3 (short) per strike | S |
| frostwire | 4 to 5 (frozen 6 to 7); the current runs, each hop 3 ticks apart so it is seen travelling. The touch glow becomes frost-blue (bug) | A blue-white wire over a snowflake lattice; S1 muffled with an ice tick | S |
| thunderstep | No numbers. Shake only when an enemy is hit | A rising whine (S13) as you leave, thunder lag (S3) as you land; keep the 9 m column | S |

**Earth**

| id | mechanics: numbers and the twist | new presentation signature | effort |
|---|---|---|---|
| shield | No change. (Fix the stale Javadoc in `Shields`: it says a small circle circles the wearer; `raised()` leaves nothing after the raise.) | Keep: the reference for how much a rune's show can say | none |
| break | No numbers (its niche is diamond hardness). Verb: brute | A distinct crunch (S31, mid) and a dust puff on the face instead of a bare vanilla break | S |
| stoneskin | Resistance II 10 s and Slowness I (stone is heavy): the price that stops it being a free permanent passive (or remove it from `Passives.BUFFS`) | Four stone plates (block displays) settle on the shoulders and back and shed dust on expiry; sandstone palette; S19 mid | S |
| root | Hold 3 s; breaks when the target takes 6 or more in one hit (a setup, not a damage rune) and fire burns it away (an ignited target is freed). Verb: hold | Thick brown roots from cracks coiling up the legs, thin green tips (not all-green), visible the whole 3 s with a slow pulse, a snap flourish when broken; S23 | S |
| tremor | A travelling ring (about 20 blocks a second): enemies airborne when it reaches them are missed, so a jump dodges. Cost 18 | Drop HEAVE's duplicate; shake 0.7 to 0.5; keep the spires; S17 large and S18 | M |
| excavate | No numbers | Replace `Vfx.tremor` with a dig plume (a dust column per mined block and chips), no shake, nothing on a miss; S31 low, double | S |
| aftershock | The second impact hits the spot, not the target (+10 ticks, radius 1.5, 5): whoever stands there, neighbours too; step aside and it misses. Cost 9 | The first hit a small stomp; the second a deeper ring that starts 2 ticks before its damage (readable, dodgeable); S17 twice, the second lower | S |
| weigh | Fall damage x2 while weighed and fliers grounded (pairs with Launch, Levitate and Updraft) | Keep dust and rings; the ring under the target stays for the whole 5 s so the sag reads; S20 | S |
| shackle | A yank bites: 2 damage per yank (at most 1/s); bosses still only slowed | A rusty iron chain with brown dust, visibly taut when stretched; silent at rest, S21 on the yank only | S |
| rampart | No numbers | Three grind sounds (one per row, S18) and one crumble instead of 15 simultaneous vanilla sounds; dust columns rising; the seams glow for the last 2 s as a warning | S |
| brace | The damage prevented while braced (cap 10) is released when the 2 s end as a shockwave of radius 3 (half as damage, plus a knock): a timed counter. Cost 4 | A short high clack (S19), the chest ring; the release is an expanding ring with S17 small | M |
| chisel | Carves the block out whole (Silk-Touch behaviour) at stone hardness (Amplify: iron), so it is a different verb from Break's loot (lead's call: balance) | A precise tick (S31), thin chalk-white dust, no ring | S to M |
| tunnel | No numbers | One rotating dust spiral at the mouth instead of a ring per cell; a rolling crunch rising per cell (S31), at most 4 sounds (from about 14) | S |
| vein | No numbers | Each ore's chime climbs the pentatonic; one break sound per block; the thread stays gold | S |
| fell | No numbers | The crescent aims at the tree; brown wood chips instead of green rings; one creak per 4 logs and a thock (S31 axe) | S |
| pelt | A heavy shove (knock 0.6 to 1.1, about 5 blocks) and a wall slam: if it is thrown into a wall within 6 ticks, +3 damage and 0.5 s stagger. Cost 5. Verb: shove. Exempt from HEAVE | Three pebbles fly from the hand in a small spread; the impact is a rattle (S32); optional shared `wallSlam` helper with Tempest | M |
| stoneform | The aftershock fires only for blows from a living attacker (Stormheart's rule), at most once a second (was twice, including fire and poison ticks), 3.5 damage in radius 3 | Dark slate plates on the body for the 8 s (deepslate, unlike Stoneskin's sandstone), a single ring and S17 small per proc, a crumble when it ends | S |
| magma | The pool grows by 0.4 blocks a second (1.6 to 3.2); 2 a second and the ignite unchanged. Verb: a swelling hazard | A bubbling lava disc (the flat cracked sigil tinted lava, one slow ring), slag pops (S28, pitch varies), a glow for the whole duration; open burst halved | S to M |
| sinkhole | Crush 5 to 6; exempt from HEAVE (it lifts what the pit pulls down) | Keep the dark disc and slabs; S26; trim to about 80 pk | S |
| geode | Attackers are left Cracked (+20% spell damage for 5 s): the crystals open them up (the Fracture mark), which is what a fused ward should do that Stoneskin + Bramble cannot. Resistance II 5 s and 2 damage stay | Crystals hold the full 5 s; each hit rings one pentatonic step higher (S30); amethyst palette stays | S |
| fossilize | Damage unchanged (credited 1.0 to 1.4x already). The stone stage marks the target Cracked (brittle) for its 2 s, so a follow-up spell lands 20% harder. Verb: petrify, then shatter. Hoarfrost has the same numbers; the two must stop being one rune (frost's auditor decides its side) | Keep the clock-slowing ramp; S27 (three rising ticks, then the lock and the crack); trim to about 90 pk | S |
| bonespur | No numbers | About 30% fewer packets; bone snaps staggered over the spurs (S25, four pitches); blood on the last only | S |
| monolith | 6 to 8, the throw 3 to 4 blocks, and a landing slam: +3 in radius 1.5 where it lands (11 in all on one target; credited 1.10x). Cost 20 | Keep the block-display column; rise S18, landing S17 large; the crumble's 10 sounds become 3; drop HEAVE's duplicate | M |
| thunderquake | No numbers | Three pulses rising in pitch (S17 mid, +2 semitones each) with S4 under the first: the earth rune that had no custom sound gets one | S |
| infest | Contagion: every 2 s an infested enemy passes the swarm to one uninfested enemy within 2 blocks for its remaining time (max 3 hops). Verb: the swarm spreads. Cost 9 | A brown line of silverfish hopping between hosts; keep vanilla skitters, add pebble chips (S32) | M |
| sandstorm | Enemy projectiles entering the zone lose 70% of their speed and drop (cover against archers); 2 a second, blind and Slowness II stay | A pulse every 10 ticks (76 to about 38 pk/s), billows kept; a loop S24; exempt from HEAVE | S to M |
| tusk_charge | Momentum: 3 + 0.8 per block charged before contact (up to 8 blocks: 9.4) | A dust wake that grows with speed, three hoof thumps (S17 small) with the vanilla snort; tossed enemies fly forward along the charge | S |
| mire | No numbers; exempt from HEAVE | A bubbling mud patch that stays for the whole 5 s (ring 1/s, sigil 100 ticks); S22 at the start and a burp every 2 s | S |
| stalactite | The drop lands on the marked spot (dodgeable) 0.4 s later; 7, bare head x1.3 (was x1.5, and bare is the norm) = 9.1 | Keep the drips and reticle; the streak lasts the whole 8 ticks so the fall is seen; S29 | S |
| basalt_surge | No numbers | 27 to about 14 pk per column; one rising grind and one crack at the end instead of 12 sounds (S18); the orange core only on the last column; a lighter basalt tone | S |
| prospect | No numbers | The glows appear as the ring reaches each ore (staggered over the 16 ticks); the ping's pitch tells the count | S |



### 2D. Signature cards: the new look and sound of each rune at a glance

One line per rune so uniqueness can be checked by eye: no two runes of the same element share a silhouette, and the sound is the rune's own (S ids, section 5). "Timing" is anticipation, impact and aftermath in ticks. Scale follows power: T1 cards are one small primitive, T3 cards may be a set piece.

**Storm**

| rune | silhouette | motion | palette accent | timing | sound |
|---|---|---|---|---|---|
| shock | a thin tether between two bright nodes | snaps across, holds | white-yellow | impact 0; the tether holds 6 | S1, second zap higher |
| jolt | four tiny arcs ringing the head | spins, then clamps shut | white with a blue core | impact 0; the ring lasts the stun (20), clamps at the end | S2 (+ a cut sting on an interrupt) |
| thunderclap | a white disc, then one wide ground ring | contracts, then a single shock outward | white and pale blue | anticipation 5; impact 5; no aftermath | S13 short, S4 |
| lightning | a vertical sky column with three ground forks | straight down | white-yellow with soot | impact 0; crack now, roll at +8; scorch 40 | S3, S3b |
| ripple | rings spreading from the target, motes flowing back | outward, then home | sun gold and white | impact 0; second ring at +8 | S16 |
| thunderbird | a raptor: orb, two wings, a tail | circles, folds, dives | yellow-white, blue rim | dive telegraph 10; strike; nothing after | S6, S6b, S7 |
| stormheart | a small charged ring orbiting the chest | slow orbit, a spark every 10 ticks | yellow, pale blue | cast 0; armed 10 s; crackle on a hit; pop at the end | S15, S1 |
| galvanize | a crackle halo round a floating red block | hums | red core, yellow sparks | impact 0; halo 100; discharge at the end | S15 |
| tempest | a spiral gust carrying a bolt | spins out, then lands | yellow with mint | strike 0, fling, second bolt at the landing | S3, S5, S4 |
| plasma | a straight, steady beam, white core in a violet sheath | linear, no jag | white, magenta-violet | impact 0; violet glow 100 | S12 |
| surge | a crackling aura on the whole body | shimmers; rises at the cast | yellow with green | cast 0; aura for 160; an arc per melee hit | S13, S1 |
| magnetize | a lodestone seal and turning field loops | lines curve inward | gold, yellow, iron grey | seal 0; field for 80 | S14 |
| riftbolt | a violet bolt in a black sheath, a dark strand | tears in; the strand hums | black, violet, storm blue | impact 0; strand for 60 | S11 |
| stormweave | star nodes joined by threads | notes pluck, then the web flashes | pink and white | marks 0 to 12; web at 12 | S9, S1 |
| stormclock | a clock face in the sky over the spot | the hand ticks; the hour strikes | gold and white | marks 0; strikes at 40 and 80; the face holds 80 | S10, S3 |
| thunderhead | a slate cloud with a rain curtain | drifts after its target | grey, blue flashes | gather 0; a strike each 20; puddles after | S8, S3 short |
| frostwire | a blue-white wire over a snowflake lattice | the current runs hop by hop | ice blue and white | chill 0; the current at 6, a hop every 3 | S1 muffled, an ice tick |
| thunderstep | a dark implode, then a 9 m column | vanish, streak, land | black, then white-yellow | whine 4; land 0; nothing after | S13, S3 |

**Earth**

| rune | silhouette | motion | palette accent | timing | sound |
|---|---|---|---|---|---|
| shield | an amber stack of circles | opens back to front | amber; gold on a parry | raise 0; nothing until a spell comes | the existing shield sounds |
| break | a dust puff and chips on the face | crumbles | grey-tan | impact 0 | S31 mid |
| stoneskin | four stone plates on the shoulders and back | drop and settle | sandstone | cast 0; dust on expiry | S19 mid |
| root | brown roots climbing the legs from cracks | coil upward | brown with green tips | impact 0; pulses for 60; a snap if broken | S23 |
| tremor | a travelling ground ring with spires | expands about 20 blocks a second | tan and pale gold | impact 0 to 15; spires 1 to 3 | S17 large, S18 |
| excavate | a dust column per mined block | rises, settles | grey-brown | cell by cell | S31 low double |
| aftershock | a stomp ring, then a deeper echo ring | ring, then a ring at the spot | tan, brown | hit 0; echo cue at 8, hit at 10 | S17 twice |
| weigh | dust rain and a sagging ring at the feet | falls; the ring stays | grey-brown | impact 0; ring for 100 | S20 |
| shackle | a rusty chain to a ground anchor | taut lines | iron grey, brown | bind 0; taut for 100 | S21 on the yank |
| rampart | rising columns of packed mud | three rows rise | brown | rows at 1, 3, 5; seams glow the last 40 | S18 x3 |
| brace | a chest ring and a short ground ring | clamps | tan, a stone chip | cast 0; window 40; a release ring at the end | S19 short, S17 small |
| chisel | a chalk tick on the face | ticks | white | impact 0 | S31 tick |
| tunnel | a rotating dust spiral at the mouth | bores in | grey-brown | cell by cell over 8 | S31 roll |
| vein | a gold thread from ore to ore | hops | gold | one a tick | S31 chime run |
| fell | a brown crescent, then chips | the crescent turns to the tree | brown | 0, then log by log | S31 axe |
| pelt | three pebbles from the hand | arc and rattle | grey cobble | impact 0 | S32 |
| stoneform | dark slate plates on the body | settle | deepslate, a sandstone flash | cast 0; stance 160; a ring per proc | S19 deep, S17 small |
| magma | a bubbling lava disc that grows | swells | fire-red and black | open 0; a pulse each 20 | S28 |
| sinkhole | a dark disc widening in steps, tipped slabs | falls inward | black, violet rim | open 0; drag to 12; crush at 41 | S26, S17 large |
| geode | a ring of amethyst crystals | grow; ring on a hit | violet | cast 0; crystals 100; a ping per hit | S30 |
| fossilize | grit rings, a slowing gold clock, a tuff shell | tightens | grey-brown, gold | three stages of 20; hold; crack | S27 |
| bonespur | bone spurs from below | converge | bone white, blood red | warning 4; strikes staggered by 2 | S25 |
| monolith | a stone column of block displays | rises, stands, crumbles | stone brick with gold seams | rise 3; stand 80; crumble 21 | S18, S17 large |
| thunderquake | three expanding rings | outward at 2, 4 and 6 blocks | tan and storm yellow | pulses at 1, 10, 20 | S17 mid x3, S4 |
| infest | a line of silverfish hopping between hosts | hops | brown and grey | a pulse each 10 | vanilla skitter, S32 |
| sandstorm | a whirl of sand | swirls; stops arrows | tan | open 0; pulses each 10 | S24 |
| tusk_charge | a dust wake and forward crescents | streaks | tan | 8 ticks | S17 small x3 |
| mire | a bubbling mud patch | bubbles rise | brown | cast 0; patch for 100 | S22 |
| stalactite | drips and a reticle, then a streak from above | falls | grey-tan | telegraph 8, impact 8 | S29 |
| basalt_surge | a line of basalt columns ending in an ember | a sequence outward | dark basalt, an ember tip | about 12 ticks | S18 rising, one crack |
| prospect | a ring that lights each ore as it passes | expands | ore colours | 16 ticks | the vanilla ping, pitched by the count |

## 3. Clusters and how each is broken up

Each cluster is a set of runes that answer the same question ("what do I press for this?") with the same shape of outcome. Proposals change the mechanic (delivery, targeting, interaction), not only the numbers; the per-rune detail is table 2C.

### C1. Storm: an instant lightning hit with a rider (8 runes)

Shock, Jolt, Thunderclap, Lightning, Plasma, Tempest, Riftbolt and Ripple each deliver damage to the target at tick 0 through the same bolt-and-sparks look, the same two reactions and no distinct sound. The riders differ on paper (an arc, a stun, a knock, a burn, armour, a fling, a relocation, a heal) but not in how you cast them or what you see.

| Rune | Delivery today | Delivery after | Its own thing after |
|---|---|---|---|
| Shock | instant, arc | instant tether that seeks a conductor | finds wet and metal-armoured enemies |
| Jolt | instant, stun | instant | interrupts a cast or charge (the counter-spell) |
| Thunderclap | instant ring | flash, then crack (5 ticks) | stun ring: stagger and lost target, a short throw |
| Lightning | instant, sky | instant, thunder lag | the burn and slow single strike, deduped in crowds |
| Plasma | instant, orange bolt | steady straight beam | ionises: primes the next storm hit |
| Tempest | instant, throw | strike, fling, second strike where they land | pile-ups |
| Riftbolt | instant, teleport | strike then a lingering strand | a black wire between entry and exit |
| Ripple | instant, heal | strike then a ring outward | the drain ripples (crowd heal) |

Cross-element: Thunderclap and Repel (wind) are the same "damage and throw everything within 3" (Thunderclap throws 11 blocks flat, Repel 13 at the centre falling to 8 at the edge and ignores armour, Push 12 for 4 mana); Pelt and Windcut deliver the same shove (2.7 and 2.8 blocks); Ripple and Smite and Halo and Leech and Lifesteal are five "hit that heals or hurts undead more" across four elements.

### C2. Storm over time, zones and marks (6 runes)

| Rune | Targeting today | After |
|---|---|---|
| Thunderbird | nearest in 12, automatic | the enemy you last hit, telegraphed dive, dodgeable, ends if you are hit hard |
| Thunderhead | random enemy under a following cloud | soaks what it strikes, so it conducts (a storm loop of its own) |
| Stormclock | a spot, three strikes | punishes standing still (a stun on the last) |
| Stormweave | up to four marked enemies, one web | a plucked mark per enemy, the web's size is the story, solo works |
| Magnetize | one enemy becomes the magnet | clumps, and metal is pulled harder |
| Frostwire | the cold enemies around each origin | a current you can watch run through the cold |

These already differ in targeting (that is why they read better than C1). What is missing is a different *time shape* for each: one dive per 2 s, one strike per second, three strikes on a clock, one web, a field for 4 s, one flash. The proposals sharpen each of those and cut the packets (Thunderbird 43 to 12 pk/s, Thunderhead 62 to 30, Stormclock about half, Magnetize about 60% less).

### C3. Storm support: Stormheart, Surge

Both are "storm on me": Stormheart retaliates, Surge buffs. Stormheart is fine but invisible; Surge is dominated by Swift and Empower. Surge becomes the melee-arc buff (Searing Edge's pattern for storm: fire has one, storm gets one), so the two storm supports are "punish those who hit me" and "hit things with lightning in my hands".

### C4. The holds: Root, Weigh, Shackle, Mire (+ Sinkhole, Fossilize, and Jolt as the stun)

Four T2 runes cost 8 to 9 mana for 3 to 5 s of "cannot get away", all a slow effect plus a cracked seal.

| Rune | Verb after | What is different (mechanic and interaction) |
|---|---|---|
| Root | hold | full immobilisation that breaks on a hit of 6 or more, so it is a setup for someone else's spell, not a damage rune; fire frees it |
| Weigh | press | fall damage x2 and fliers grounded: pairs with wind's Launch, Levitate and Updraft (the wind lifts, the weight drops) |
| Shackle | leash | the yank bites (2 damage), so running costs health and standing and fighting inside the leash costs nothing |
| Mire | bog | soaks the target, which is a Conduct setup for storm; the patch stays visible its whole 5 s |
| Sinkhole | pit | the only one that moves them: drags and pins many, then crushes |
| Fossilize | petrify | a slow ramp then a hold; the stone stage is Cracked, so the follow-up spell hits 20% harder |

Cross-element: Fossilize is numerically Hoarfrost (frost): 3 s ramp, 2 s hold, 6 damage, 16 mana each. Both cannot stay; the proposal makes Fossilize's stone stage brittle (Cracked) and leaves its numbers alone; Hoarfrost is the frost auditor's to differentiate.

### C5. Ground area damage (9 runes)

Tremor, Thunderquake, Monolith, Aftershock, Bonespur, Basalt Surge, Magma, Sandstorm and Infest are all "earth hurts what is on the ground" and share one skeleton: a cracked sigil, a ground ring, block chips, a dust pillar, and earth's generic impact sound. On top of that HEAVE draws a second crack and five slabs, so a cast of any of them starts with two cracks.

| Rune | Delivery after | Distinguishing property |
|---|---|---|
| Tremor | one travelling ring | a jump dodges it |
| Thunderquake | three pulses at 2, 4, 6 blocks | rhythm; nearer is harder |
| Monolith | one uplift and a landing slam | a pillar you can see; the throw hurts twice |
| Aftershock | target, then the place | the second hit is dodgeable |
| Bonespur | from below, staggered over 8 ticks | up to four, and a bleed mark for Rupture |
| Basalt Surge | a sequence outward at 12 blocks/s | a line, one rising grind and a crack |
| Magma | a pool that swells | denial that grows |
| Sandstorm | a zone that chokes projectiles | cover against archers |
| Infest | a swarm that spreads | contagion on a crowd |

### C6. Mitigation (5 runes)

| Rune | Verb after | Cost of using it |
|---|---|---|
| Stoneskin | heavy armour | Slowness I (also stops it being a free permanent passive) |
| Brace | absorb and release | 2 s window, 6 s lockout; the released damage is a counter |
| Stoneform | stance that punishes attackers | only for living attackers, at most once a second |
| Geode | crystals that open the attacker | Cracked attackers make your team's spells hit 20% harder |
| Shield | spell block | unchanged (PvP and boss ward) |

Stoneskin out-defends Geode (12 mana for 10 s against 16 for 5 s, and Stoneskin + Bramble is cheaper than Geode). Geode's new mark makes it the team ward rather than the personal one.

### C7. World and mining (8 runes)

Break, Chisel, Excavate, Tunnel, Vein, Fell, Prospect and Galvanize each have a distinct region (one block, 3x3, a 2x4 bore, an ore vein, a tree, a sonar, a spark), which is more identity than the damage runes have. What they lack is sound and look: six share one break sound and the vanilla burst; Excavate borrows Tremor's screen shake; Fell uses green life rings; Vein is pale gold. Proposals: a per-rune crunch (S31, six pitched variants), an earth-brown chip palette, Excavate's own dig plume, and Chisel as the careful Silk-Touch block against Break's loot.

### C8. The T1 pokes across the mod (cross-element)

Shock, Pelt, Windcut, Icicle, Ember, Harm, Countdown, Umbra and Leech are the mod's opening damage: a 3 to 4 damage hit at 5 to 8 mana. They differ by a rider (arc, shove, setup for Wildfire, bonus on slowed, burn, plain, delay, doubled in the dark, heal). Two are the same (Pelt and Windcut). The rule the lead can apply mod-wide: every T1 poke gets a rider that changes what you do next (Shock finds conductors, Pelt slams into walls, Icicle punishes the slowed, Ember burns, Countdown delays) and a one-line sound of its own.

### C9. Sentinels (cross-element)

Thunderbird (storm), Summon (arcane wolves), Shades (void hounds) and Halo (arcane) are all "something fights beside you for 15 to 20 s without input". Thunderbird is the only one that cannot be attacked, has no cap on hits, and asks nothing. If the lead wants sentinels to differ, Thunderbird is the aerial, telegraphed one (dives that can be dodged, ends when you are struck hard); wolves and hounds are the ground ones that can be killed.

## 4. Cross-element and caster-feel observations

### 4.1 The first 200 ms of a cast: what the caster feels

What happens when a spell goes off (`SpellCaster.cast`, `Vfx.castCircle`, `Charging`, client `PlayerModelMixin`):

| Time | Cue | Depends on the rune? |
|---|---|---|
| 0 | vanilla arm swing (`player.swing`) | no |
| 0 | the spell's own circle opens under the feet (28 + n ticks), 6 enchant motes rise, a hand glow | only the circle's colour (first effect's element) and its emblems |
| 0 | `CIRCLE_OPEN` 0.35, then `cast_<element of the first effect>` 0.55 (2 random variants) | only the element |
| 0 to about 6 | arm pose for the first rune's id: thrust (bolt, beam and everything unknown), sweep (crescent, glaive), flung up (zone, rain, ring, burst, totem, mine), push (wall, wave, orb), swept back (blitz), open hands (self, orbit, trail, imprint) | the shape, never the effect; an effect-first spell (implicit Self) carries the effect's id and falls into the thrust |
| 0 | a held charge: the circle builds, hum rises, chime at full, `RELEASE` and a field-of-view kick | no |
| impact | the element's `impact` sound only for a few runes, `Vfx.touched` glow and 3 motes, then the rune's own show | yes, the only part that is |
| impact | caster screen punch for a single hit of 8 or more | damage only |

So casting Shock, Plasma and Thunderclap from the same shape looks and sounds the same for the first quarter second (a circle, a swing, `cast_storm`), and casting Stoneskin, Brace and Root from Self is the same too. The difference the caster feels between two storm runes arrives only at the target. The proposal that fixes it globally is small: a per-rune cue registry (rune id to a short stinger sound, a hand-glow size and shade, an optional pose override) read once by `Vfx.castCircle` for the first effect (or the most expensive effect of the spell) and by `Effects.apply` for the impact. Section 5's kit is designed to fill it: about 32 sounds give every rune its own stinger, and the ten core ones cover 31 of the 49.

Pose pairs worth adding once the registry exists: a guard for wards (Stoneskin, Brace, Stoneform: forearms crossed), a slam for ground runes (Tremor, Monolith, Aftershock: hands raised and brought down), arms raised for Thunderbird. The `Pillar` shape already has "raised and brought down".

### 4.2 The sameness ledger: units shared by many runes

| Shared unit | Where it is used (this audit's runes) | Why it reads as the same |
|---|---|---|
| `ElementFx.bolt` with `stormImpact` | Lightning (3), Thunderclap (4), Shock and Jolt (via `shockArc`), Stormheart, Thunderbird, Tempest, Magnetize, Stormclock, Thunderstep, Thunderhead, Plasma, Riftbolt, Surge, Stormweave, Thunderquake | width 0.05, 2 flickers, jagged yellow, the only difference is colour or count; nine of the ten fused and signature storm runes reuse it (Frostwire draws arcs) |
| the vanilla visual-only `LightningBolt` entity | Lightning, Tempest, Thunderstep, Stormheart | one vanilla thunder and one sky flash for four different runes |
| `Vfx.shockArc` with `TRIDENT_THUNDER` 0.25 pitch 1.9 | Shock, Jolt (twice), Stormheart retaliation, Thunderbird strikes | the same 4 sparks and the same sound |
| `ElementFx.crack` (cracked sigil, ring, chips, dust) | Stoneskin, Stoneform (cast and each proc), Root, Weigh, Aftershock, Tremor, Excavate, Infest, Tusk Charge, Mire, Basalt Surge, Magma, Monolith, Sinkhole, Fossilize, Bonespur, Thunderquake, and HEAVE on all harmful earth runes | every earth "hit the ground" moment is the same seal |
| `earthImpact`, `stoneShards`, `impact("earth")` | Pelt, Aftershock, Sandstorm, Tusk Charge, Stalactite, Monolith, Sinkhole, Magma, Fossilize, Bonespur | a radial burst of the ground's own block |
| `Vfx.stoneskin` unchanged | Stoneskin and the Stoneform cast | two different runes, one identical cast |
| `Vfx.touched` | every non-self rune | a 0.9 glow and 3 motes on everything |
| `MAGIC_BREAK` with the vanilla break burst | Break, Excavate, Chisel, Tunnel, Vein, Fell | six mining runes, one sound |
| bolt call shape with an under-stroke colour | Plasma (orange), Surge (green), Riftbolt (black), Stormweave (pink), Stormclock (gold) | one primitive, five colours |

### 4.3 Sounds

- The mod has 20 custom element events (`cast_` and `impact_` for ten elements, two and three variants) and 15 casting events (charge, release, circle, beam, orb, four shield sounds, imbue, domain open and close, blink, magic break). Everything else in a rune's show is a vanilla `SoundEvents.*` constant: 28 of the 49 runes here use no custom event in their own code.
- Storm's own vocabulary today is `TRIDENT_THUNDER`, `LIGHTNING_BOLT_IMPACT` and `LIGHTNING_BOLT_THUNDER` plus the vanilla bolt entity's own sounds; earth's is `MACE_SMASH_GROUND(_HEAVY)`, `STONE_BREAK`, `ANVIL_*`, `BASALT_BREAK`, `SAND_FALL`. Neither element has a sound that says which rune it was, and Thunderclap, Lightning, Tempest and Thunderquake are the same vanilla thunder at different volumes.
- Sound spam: Stormclock 25 events per spot (100 for four spots), Thunderbird up to 37 per bird, Rampart 15 simultaneous vanilla breaks twice, Basalt Surge 12 in 12 ticks, Vein 3 a tick at 16 ores, Fell up to 48 in 16 ticks.
- Distance: real thunder arrives after the flash. Every strike here plays its crack and its roll together. A single helper (crack now, roll 6 to 10 ticks later at half volume) gives Lightning, Tempest, Thunderstep, Thunderhead and Stormclock the "storm feel" for free.

### 4.4 Element-wide overlays that make different runes the same

`WorldMagic.onSpell` runs after every effect, keyed by element (`WorldRules.of`):
- CONDUCT on every harmful storm rune (except those in `WorldRules.QUIET`, none of which is storm).
- HEAVE on every harmful earth rune except Root, Weigh and Shackle (`QUIET`). That includes Mire, Sinkhole, Fossilize, Infest, Sandstorm, Stalactite, Magma, Monolith, Thunderquake, Bonespur, Tremor, Pelt and Aftershock: a second crack, five slabs and a 0.45 hop for grounded enemies within 2, at most 3 heaves a cast. Pelt's overlay is three times bigger than Pelt; the pin runes hop the target up at the moment they should pull it down.
- Fix: add the runes with their own ground show (Tremor, Monolith, Thunderquake, Sinkhole, Fossilize, Mire, Magma, Sandstorm, Basalt Surge) to `QUIET`, or let a rune opt out.

### 4.5 Stacking and per-target rules are inconsistent

Effects that strike "on each target" and hit an area pile onto clusters unless deduped:
- Lightning: up to 8 strikes, each hits everything within 2 (3 vertical) of every strike. N clustered enemies take up to N x 12 each.
- Meteor (4 blasts) and Explode (4 blasts), not in this group, work the same way.
- Tempest and Magma were deduped in 0.4.3-alpha (`FusedEffects.unstacked`: an enemy takes the strongest overlapping hit once). One shared helper in `Effects` and a rule ("an area effect landing on each target hits an enemy once, its strongest, per cast tick") would settle it for all.
- Frostwire, Stormweave, Thunderquake and Stormclock already handle it by hand.

### 4.6 Armour: an unlisted axis

Damage source decides whether armour applies. Earth's runes are split, and no description says which:

| Ignores armour (`indirectMagic`) | Reduced by armour |
|---|---|
| Infest, Sandstorm, Tusk Charge, Basalt Surge, Monolith, Sinkhole, Fossilize (its crack), Stoneform aftershock, Ripple (storm, but `indirectMagic`), half of Plasma | Pelt and Tremor (`FALLING_BLOCK`), Thunderquake (`FALLING_BLOCK`), Aftershock (`PLAYER_ATTACK`), Bonespur (`playerAttack`), Stalactite (`FALLING_STALACTITE`), the storm runes that strike as lightning (`LIGHTNING_BOLT`: Shock, Jolt, Thunderclap, Lightning, Thunderbird, Tempest, Riftbolt, Stormweave, Stormclock, Thunderhead, Frostwire, Thunderstep) |

Against a fully armoured player (armour 20, toughness 8) a 12-damage armour-reduced hit lands as 3.8, an armour-ignoring one as 12. Vanilla's exact tag contents are from memory (see section 8). A standard would help: say it in the description ("through armour") for the magic ones, and keep PvP scale in mind.

### 4.7 Anticipation, impact, aftermath

- Almost everything lands at tick 0 with no telegraph: Lightning, Thunderclap, Shock, Jolt, Tempest, Plasma, Riftbolt, Ripple, Tremor, Pelt, Excavate, Aftershock's first hit, Stoneskin and every buff.
- The ones with a real anticipation are the good ones: Stalactite (8 ticks), Stormweave (15), Stormclock (each tick of the hand), Bonespur (4 to 10), Fossilize (three stages), Thunderquake (three waves), Sinkhole (the drag). A tier rule would help: every T3 damage rune gets at least 4 ticks between the cue and the hit, and every buff shows for as long as it lasts (Surge 0.3 s of 8; Stoneform, Stormheart and Stoneskin nothing while active).
- Aftermath: most runes leave nothing after the hit (Shock, Jolt, Pelt, Aftershock, Tremor; Lightning a soot puff). The lasting ones are Monolith's 4 s pillar, Sinkhole's slabs, Magma, Sandstorm and Mire (pulses), and Stormclock's and Thunderhead's sky pieces.

### 4.8 Screen shake does not follow power

`ScreenFx.shake` strengths: Tremor 0.7, Thunderquake 0.45 to 0.65 (three times), Thunderclap 0.55 (even on a miss), Tempest 0.5 per strike (up to 8), Thunderstep 0.5, Excavate 0.45 (even when nothing is mined), Sinkhole 0.3 and 0.5, Monolith 0.35 per target, Fossilize 0.25. Thunderclap (5 damage) and Excavate (mining) are the outliers; Tempest's shake should be once per cast.

### 4.9 Palette mismatches

Root (life green vines), Fell (green life rings), Vein (pale gold), Shackle (grey steel), Ripple (sun gold), Frostwire's touch glow (yellow over blue), Stormweave's sounds (arcane), Geode (amethyst purple: fine for an arcane blend). The `Vfx.touched` glow follows the rune's own element, so Frostwire's is yellow although its show is blue. A rule for "blend" runes: the accent of the second element on the first's base.

### 4.10 Innates

Stormheart and Stoneform are both innate runes (the 1st Circle wakes one at random, +6% per circle), so their balance is luck. Stoneform is stronger than Stormheart in any fight with two or more enemies and works on fire and fall damage too. Section 2C brings them level (living attackers only, one proc a second).

### 4.11 What to treat globally (the short list)

1. A rune cue registry: per-rune stinger, hand glow, pose (section 4.1). Smallest change with the widest effect.
2. One dedupe helper for area effects that land per target (4.5).
3. An opt-out for the element overlays (4.4), starting with the pin runes.
4. A show budget by tier (packets per hit and sound events per hit, section 1.2) and a rule that buffs show for as long as they last.
5. A shared "thunder lag" helper and a sound kit whose first ten entries cover most runes (section 5).
6. Say "through armour" in the descriptions of magic-damage runes.

## 5. Sound needs

The mod authors its sounds in `tools/sound_art.py` (numpy and scipy: `grains`, `bandpass`, `thump`, `tick`, `bell`, `glass`, `moving_band`, `chopper`, `soft_saw`, `fm`, `sweep`, `reverb`, `reverse`; everything tonal in D major pentatonic, played at pitch about 1). The kit below is written in that vocabulary. Each sound is a brief, not a spec. Ids are the ones used in tables 2C and 2D.

**Core (build first): S1, S2, S3 with S3b, S4, S17, S18, S19, S31, S6, S13. Ten sounds give a stinger to 31 of the 49 runes.**

### Storm

| id | name | verb, character | synthesis brief | length | used by |
|---|---|---|---|---|---|
| S1 | arc_zap | a dry tick of static, no rumble | `grains` (rate 180, 0.4 to 1.5 ms) band-passed 2 to 7.5 kHz, plus an fm sweep 3.5 to 1.2 kHz, decay 0.05; three pitch variants (D, F#, A) | 0.2 s | Shock (tether, second zap higher), Stormheart retaliation, Surge melee arc, Magnetize touch, Stormweave release, Frostwire (muffled, plus an ice tick) |
| S2 | stun_clamp | a buzz that cuts to a hard thunk, then a beat of quiet | 0.12 s `chopper` buzz (soft_saw D3, lowpass 3 kHz) into `thump(150, 60, 0.2, 0.05)` and a `tick`; 0.1 s of ducked tail. A reversed-cut sting variant for an interrupt | 0.35 s | Jolt, Thunderclap stagger (deeper), Thunderstep stun, Stormclock's last strike |
| S3 | bolt_crack | the sky strike: one hard broadband crack with almost no tail | noise band-passed 500 to 8000 Hz, decay 0.012 (the crack of `impact_storm` on its own); a distance-lag pair with S3b, the roll (`moving_band` 1500 to 500 Hz plus brown noise under 220 Hz, decay 0.28), played 6 to 10 ticks later at half volume | crack 0.3 s, roll 0.9 s | Lightning, Tempest strike, Thunderstep arrival, Stormclock's hour, Thunderhead strike (crack only) |
| S4 | thunder_boom | a round whump, not a crack | `thump(95, 40, 0.6, 0.14)` saturated, a soft crack on top (decay 0.02), no sizzle | 0.8 s | Thunderclap, Thunderquake (three, +2 semitones each), Tempest landing |
| S5 | gale_crack | a strike that turns into wind | S3's crack, then a band-passed whoosh rising 500 to 3000 Hz over 0.5 s | 0.7 s | Tempest fling |
| S6 | bird_cry | a raptor's scream from above | fm carrier gliding 2.2 to 1.4 kHz, ratio 1.5, short reverb; S6b, the dive: a falling whistle 3 to 0.8 kHz | 0.5 s, 0.4 s | Thunderbird (arrival, each dive) |
| S7 | wing_static | a faint hum with a flutter | a quiet 100 Hz saw-and-noise hum with a 12 Hz chopped flutter, once every 1.5 s at low volume | 1.0 s | Thunderbird's orbit (replaces 16 flaps) |
| S8 | cloud_roll | a low swell with rain | brown noise low-passed 250 Hz swelling over 1 s, rain grains 2 to 6 kHz under it; fades | 2.0 s | Thunderhead (gather and end) |
| S9 | web_pluck | four taut wires struck one after the other | a Karplus-Strong or `partials` pluck (D, E, F#, A), 0.4 s each, a soft zip between | 0.4 s each | Stormweave marks |
| S10 | clock_tick | a ratchet that ends in a bell | a dry pitched click (0.05 s, pitch rises with the hand); the hour is a `bell` (D5) with S3's crack | 0.05 s, 0.8 s | Stormclock |
| S11 | rift_tear | black lightning: a crack that unzips | S3's crack reversed into a swell, a sub drop (sweep 200 to 40 Hz), a short violet `bell` ring; forward at the exit, reversed at the entry | 0.7 s | Riftbolt |
| S12 | plasma_sizzle | a superheated hiss, steady not jagged | filtered noise rising in pitch (a soft fwoop), then 0.3 s of steady crackle; no impact crack | 0.6 s | Plasma |
| S13 | charge_whine | a rising whine and a snap | a glide D3 to D5 over 0.4 s (`glide`, soft_saw lowpassed), then a snap; a short version for a flash | 0.5 s | Surge (cast), Thunderstep (departure), Thunderclap (the flash) |
| S14 | magnet_whine | a metallic hum that leans in | two detuned sines beating at 3 Hz, swelling; pitch rises with the drag; a lodestone-like click on each shock | 1.0 s loop | Magnetize |
| S15 | spark_pop | a tiny pip | a 0.12 s `tick` and a sparkle grain | 0.12 s | Galvanize (hum and expiry), Stormheart (armed pulse and end), Surge aura |
| S16 | sun_pulse | a warm bell swell, no storm in it | an airy bell (D and A) swelling over 0.3 s and fading, soft chorus | 0.6 s | Ripple |

### Earth

| id | name | verb, character | synthesis brief | length | used by |
|---|---|---|---|---|---|
| S17 | quake_thud | ground weight in three sizes | `thump(120 to 45 Hz)` saturated, a `knock` (noise 200 to 1500 Hz, decay 0.018), debris grains 180 to 3200 Hz; small 0.4 s, mid 0.7 s, large 1.1 s | 0.4 to 1.1 s | Tremor (large), Thunderquake (mid, three), Aftershock (twice, the second lower and longer), Stoneform proc (small), Monolith landing (large), Brace release (small), Tusk Charge (three small hoof thumps) |
| S18 | slab_grind | stone on stone, rising | `moving_band` 300 to 900 Hz plus grains low-passed at 2.8 kHz over a brown rumble, swelling over 0.5 s | 0.7 s | Monolith rise, Rampart (one per row), Basalt Surge (one, rising), Tremor's spires |
| S19 | stone_clamp | plates locking | a `tick` at 900 Hz, a short `thump(180, 70)`, a resonant band 400 to 1200 Hz; mid, short-high (Brace) and deep (Stoneform) variants | 0.35 s | Stoneskin, Brace, Stoneform, Fossilize's lock |
| S20 | gravity_press | a sinking groan | a sine gliding 120 to 50 Hz with an anvil-like thump at the end | 0.6 s | Weigh |
| S21 | chain_clank | metal on metal, taut | a clank with a short ring; the bind is three links rattling, the yank one hard clank and a zip | 0.3 s | Shackle |
| S22 | mud_gloop | a slow bubble and a squelch | a low bloop (sweep 300 to 120 Hz) with a band 150 to 600 Hz wobbling; a burp every 2 s | 0.5 s | Mire |
| S23 | root_creak | wood stretching | a creak (fm with slow ratio), leaf rustle grains, a snap variant for when it breaks | 0.5 s | Root |
| S24 | sand_hiss | wind-blown grit | noise 2 to 6 kHz swelling with gritty grains, loopable; a "thwip-thud" for a projectile it stops | 1.5 s loop | Sandstorm |
| S25 | bone_snap | dry and hollow | a hollow knock and a high crack, four pitches, staggered | 0.15 s | Bonespur |
| S26 | sink_rumble | the floor going out from under you | a brown-noise rumble 300 to 80 Hz, a reversed suction whoomp, then S17 large | 0.9 s | Sinkhole (open, crush) |
| S27 | petrify_ticks | three ticks and a lock | stone ticks rising D, E, F#, then S19 (deep) and a crack | 1.2 s | Fossilize |
| S28 | lava_bloop | slow, thick | a slow bubble pop with a hiss, pitch varied per pulse | 0.3 s | Magma |
| S29 | rock_drop | a whistle and a crunch | a falling whistle 2.5 to 0.6 kHz into a hard crunch and thock | 0.5 s | Stalactite |
| S30 | crystal_ping | a clean cut crystal | inharmonic bell partials in the A pentatonic (`glass` builder), one step higher per hit | 0.6 s | Geode |
| S31 | dig_crunch | a pick biting rock, pitched by the work | a noise burst 800 to 4000 Hz (decay 0.03) and a wood tap; Chisel a tick, Break mid, Excavate a low double, Tunnel a roll rising per cell, Vein a chime run, Fell an axe thock and a creak | 0.2 s | Break, Chisel, Excavate, Tunnel, Vein, Fell |
| S32 | pebble_rattle | small stones on stone | 5 to 8 grains 300 to 3000 Hz, hard attack, dry | 0.25 s | Pelt, Infest, Stalactite's drips |

Notes for the kit: S3 and S17 each need two or three variants (the mod plays two or three per element); S3 and S3b are two events so a helper can schedule the roll; Riftbolt (S11), Stormweave (S9, S1) and Stormclock (S10) currently borrow void, arcane and time sounds and should stop. Custom sound files stay small (about 20 to 40 KB each as mono Ogg, like the existing ones).

## 6. Proposed implementation order, effort and risks

### 6.1 Order

| Phase | What | Effort | Notes |
|---|---|---|---|
| 0 | The bugs of section 7 (Lightning dedupe, Stoneform trigger, Ripple heal, HEAVE quiet list, Excavate shake, Thunderclap miss-shake, Shock arc reactions, texts, effect-first pose) | about 1.5 days | fixes first; each is small and testable |
| 1 | The rune cue registry (`RuneCues`: stinger sound, hand glow, optional pose) read by `Vfx.castCircle` and `Effects.apply`, plus the thunder-lag helper, plus the core ten sounds | about 3 days (about 2 for the code, the rest for sound design) | the widest change; everything after can hook into it |
| 2 | Numbers and prices (S rows of table 2C): Thunderbird, Stoneskin, Plasma, Tempest, Surge, Riftbolt, Monolith, Geode, Stormweave, Frostwire, Thunderhead, Stoneform, Pelt shove, Stalactite | about 1.5 days | balance pass; one batch so playtests compare like with like |
| 3 | Mechanics, by cluster: C1 (Jolt interrupt, Thunderclap, Tempest, Plasma, Ripple, Riftbolt), C2 (Thunderbird dive, Thunderhead soak, Stormclock), C4 (Root, Weigh, Shackle), C5 (Tremor ring, Aftershock spot, Sandstorm cover, Infest contagion, Monolith slam, Magma growth), C6 (Brace release, Geode), Surge | about 6 days | each row is 2 to 6 hours; the M rows first |
| 4 | Presentation per rune: signatures, packet trims (Thunderbird, Stormclock, Thunderhead, Riftbolt, Stormweave, Basalt Surge, Sandstorm, Magnetize), the remaining 22 sounds, poses | about 6 days | after 1 and 2; screenshots and the wiki images to regenerate |

Total about 18 working days for the group; phases 0 to 2 (about 6 days) already fix the OVER verdicts and most of the UNDER ones; the ones that need a mechanic to stop being one rune are Thunderclap, Tempest, Ripple and Riftbolt.

### 6.2 Risks

- **Balance.** Thunderbird, Stoneskin and Lightning's dedupe nerf runes players use; Plasma, Tempest, Surge, Riftbolt, Monolith, Geode, Frostwire and Stormweave are nudged up (by 10% to 30% of their credited value), Fossilize gets a payoff for follow-ups rather than numbers. The changes are ratios against a fixed reference, so a single playtest pass over the T3 runes (and a re-read of appendix A after the other auditors report) settles them. Stoneform and Stormheart are innates: their changes affect players who already rolled them.
- **Tests to update** (unit): `WorldRulesTest` (QUIET set, `Interaction.of` for the pin runes and Thunderbird), `HeartAndPassivesTest` (Stoneskin as a passive, if it is removed or slowed), `FusedStormNumbersTest` (weave damage, cloud strike), `SignatureRulesTest` (Frostwire 4 and 6, quake numbers), `FusedFlameRulesTest` (Monolith lift), `ExplorerNumbersTest`, `ExpansionRunesTest` (Pelt, Brace), `ReactionRulesTest` (Thunderbird's trigger, Tusk Charge, Bonespur mark), `CraftedRunesTest`. Gametests: `WildercordReactionsTest` (Jolt, Pelt, Overload on a crowd of Lightning), `WildercordFusedStormTest`, `WildercordFusedFlameTest`, `WildercordFusedFrostTest`, `WildercordFusedLifeTest`, `WildercordSignatureFusionTest`, `WildercordNewRunesTest`, `WildercordFeatureTour`, plus the Screenshots and Showcase samples. Run gametests only with no dev client open (a running `runClient` closes).
- **Docs and generated pages.** Descriptions live in `Runes.java` (effect definitions) and `lang/en_us.json` (tooltips); `tools/wiki.py` regenerates `wiki/runes/effects/storm.md`, `earth.md`, `wiki/runes/fused.md`, `innate.md`, `world.md`; hand-written pages that mention them: `wiki/spellcraft/reactions.md`, `passives.md`, `shields.md`, `reading-spells.md`; `docs/DESIGN.md` (the batch 3, 4, 6 and 7 tables) and the CHANGELOG. `docs/RECIPES.md` does not change (no recipe is touched).
- **New marks and helpers.** `IONISED` (or reusing `Mark.WET`) touches `Reactions.Mark`, the Unweave mark count (`WOVEN`) and the Grimoire text; keep it out of `WOVEN` to avoid changing Unweave's numbers.
- **Interrupt** (Jolt) needs a public hook on `DungeonBoss.interrupt()` (protected today) and on Runebound's windup; a boss must be allowed to ignore it (a short lockout, never a stun).
- **Effort figures** assume the existing scheduler and Vfx primitives; nothing here needs a new render path. The two new visual needs are small: a persistent thin arc (exists: `ElementFx.arc` with a long lifetime) and exposing `WorldMagic.slab` for Rampart, Monolith and Basalt Surge.

## 7. Broken or inconsistent (fix regardless)

1. **Lightning stacks quadratically on clusters** (`Effects.lightning`, lines 741 to 756 and the `lightning` case at 301 to 313). Each of up to 8 strikes hurts every enemy within 2 blocks (3 vertical) of it, and every enemy of a shape's hit gets its own strike, so N enemies within 2 blocks of each other take up to N x 12 each: 3 enemies 108 in total, 5 enemies 300, 8 enemies 768. 0.4.3-alpha fixed Magma and Tempest (`FusedEffects.unstacked`) and Overload re-triggering; the strike damage itself was left. Meteor and Explode (4 blasts each) share the pattern.
2. **Stoneform fires on any damage** (`Innates.init`, `stoneAftershock` from `AFTER_DAMAGE`, no source check; `Stormheart` requires a living attacker). Standing in fire, poison or after a fall sends 3 damage and a throw to every enemy within 3, every 0.5 s.
3. **Ripple heals for damage it did not deal** (`Techniques.ripple`: `heal(min(10, amount / 3))` with the computed amount, not the health taken: an invulnerable or protected target, an untouchable boss phase or a hit that Stasis holds still all heal the caster; Leech and Lifesteal use the health actually taken).
4. **HEAVE on the pin runes** (`WorldRules.QUIET` lacks Mire, Sinkhole, Fossilize, and the runes with their own ground show): a second crack, five slabs and a 0.45 hop at the moment a rune is meant to sink or hold.
5. **Excavate plays Tremor** (`Effects.excavate` ends with `Vfx.tremor(..., 1.5)`) whether or not a block was mined: a 0.45 screen shake within 12 blocks, a heavy smash and rings, and no shake on Chisel, Break or Tunnel.
6. **Thunderclap shakes even on a miss** (`Vfx.thunderclap` calls `ScreenFx.shake(0.55, radius*3+10)` unconditionally).
7. **Lightning's text says "stuns"**; the code applies Slowness IV for 1 s (60% slower). Jolt is the rune that stuns.
8. **Shock's second victim skips storm's reactions** (`Effects.shock`: `Reactions.storm` on the first only; the arc's 3 damage misses Conduct and Overload).
9. **A spell that starts with an effect (implicit Self) thrusts**: `CastPose` carries the first rune's id, and the client only knows the shape ids, so `Stoneskin · Empower` uses the default thrust instead of open hands.
10. **Stalactite's reticle is decoration**: the hit lands on the target wherever it went (`ExplorerEffects.stalactite` uses `t` at tick 8), so the telegraph cannot be dodged.
11. **Borrowed looks and sounds**: Frostwire's touch glow is yellow (its element is storm) although the rune is drawn in frost blue; Thunderstep and Riftbolt borrow the blink sound (Riftbolt also void's impact), Stormweave arcane's, Stormclock time's.
12. **Stale text**: the `Shields` class comment says a small circle circles the wearer; `raised()` opens the circles and leaves nothing.
13. **Rampart and Vein sound spam** (15 simultaneous vanilla breaks at raise and crumble; 3 sounds a tick at 16 ores) and **Stormclock's 25 sounds per spot** are bugs of scale rather than logic, listed here so the presentation phase catches them.

## 8. What I could not verify

- **Nothing was run.** No game, no gametest, no build, no unit test (running a gametest closes the user's dev client). Every number comes from reading the code; the counts of packets and sounds were read by three sub-audits of the Vfx functions and are estimates of what one cast sends, not measurements of what the client renders.
- **Sound.** I judged the sounds from the code that plays them (event, volume, pitch), not by listening. The vanilla lightning bolt entity very likely plays its own thunder and a sky flash on the client; that is vanilla behaviour I did not confirm.
- **Vanilla constants from memory.** The armour formula (used in appendix A), the `bypasses_armor` damage tag contents (which of `indirectMagic`, `freeze`, in-fire and lightning ignore armour), Slowness VII being total immobility, and that burning deals 1 damage a second. The repo's own code agrees with the burning figure (`ReactionRules.lingering`) and with lightning being armour-reduced (Plasma's comment); the rest I did not confirm against the jars (the data tags are not in the Loom cache).
- **Tier references** are computed from the descriptions and the code of the other eight elements' runes; some of those have hidden conditions I counted at their unconditional value. The other auditors' reports may move a median by a few hundredths; the verdicts that sit near a band edge are Stalactite (1.70x, over-ish or OVER), Monolith (0.63x), Plasma (0.80x, but dominated by Lightning, which is robust) and Riftbolt (1.04x credited, 0.89x by parts): these are the ones another auditor's numbers could tip.
- **The HEAVE and CONDUCT overlays** were read in `WorldMagic`; I did not see them fire on Mire, Sinkhole or Fossilize in game. **Effect of `Effects.push` on a boss or a knockback-resistant creature** is scaled by knockback resistance in code; distances in section 3 are for an ordinary creature under vanilla's drag model (the mod's own `FusedStormNumbers.glide`).
- **Player-facing balance** (a fight against a real player in armour) is arithmetic here, not play.

## Appendix A. The arithmetic

**A1. Reference medians** (damage per effect mana, single target, unconditional numbers, other eight elements).

- T1, n=7: Harm 7/8 = 0.88, Ember (3 + 3 burn)/6 = 1.00, Icicle 4/6 = 0.67, Windcut 4/6 = 0.67, Leech 3/8 = 0.38, Countdown 6/7 = 0.86, Umbra 4/6 = 0.67. Median 0.667.
- T2, n=10: Fire (5 + 6)/8 = 1.38, Frost 5/8 = 0.63, Bleed (2 + 8)/8 = 1.25, Dismantle 9/10 = 0.90, Blazecall (6 + 3)/11 = 0.82, Fangs 6/10 = 0.60, Vinelash 5/9 = 0.56, Spellbrand 6/8 = 0.75, Manaburn 5/10 = 0.50, Blood Moss 6/10 = 0.60. Median 0.688.
- T3, n=16: Smite 10/16 = 0.63, Starshard 12/16 = 0.75, Sanguine Rite 12/14 = 0.86, Blackspark 11/16 = 0.69 (8 x (0.75 + 0.25 x 2.5)), Cleave 6.4/18 = 0.36 (4 + 12% of 20), Lifesteal 5/16 = 0.31, Entropy 7.5/16 = 0.47, Devour 5/16 = 0.31, Hemomancy 7/16 = 0.44, Frostbite 8/16 = 0.50, Hoarfrost 6/16 = 0.38, Resonant Shriek 12/18 = 0.67, Sunscorch 13/16 = 0.81, Blackflame 18/18 = 1.00, Soulfire 15/16 = 0.94, Hail 6/16 = 0.38. Median 0.562.
- Area, n=13, per enemy per mana: Repel 0.56, Flashfire 0.64, Coldsnap 0.27, Explode 0.53, Meteor 0.50, Inferno 0.80, Moonpetal 0.33, Cyclone 0.30, Avalanche 0.42, Tidecall 0.38, Eclipse 0.44, Blizzard 0.22, Dragon Breath 0.83. Median 0.444.

**A2. The runes of this audit, damage only** (damage, mana, ratio to the tier median; riders are credited in A9).

| rune | mana | damage counted | dmg/mana | ratio |
|---|---|---|---|---|
| shock | 7 | 4 (7 with a neighbour) | 0.57 (1.00) | 0.86x (1.5x) |
| pelt | 5 | 4 | 0.80 | 1.20x |
| jolt | 9 | 4 | 0.44 | 0.65x |
| aftershock | 9 | 5 + 5 | 1.11 | 1.62x |
| stalactite | 9 | 7 (10.5 bare head) | 0.78 (1.17) | 1.13x (1.70x) |
| infest | 9 | 1 x 8 pulses | 0.89 | 1.29x |
| ripple | 10 | 6 (18 undead) | 0.60 (1.80) | 0.87x (2.62x) |
| lightning | 20 | 12 (16 with 4 s burn) | 0.60 (0.80) | 1.07x (1.42x) |
| plasma | 20 | 4.5 + 4.5 | 0.45 | 0.80x |
| tempest | 22 | 8 | 0.36 | 0.65x |
| monolith | 20 | 6 | 0.30 | 0.53x |
| riftbolt | 18 | 6 | 0.33 | 0.59x |
| fossilize | 16 | 6 | 0.38 | 0.67x |
| sinkhole | 18 | 5 | 0.28 | 0.49x (plus a 2 s pin on up to 8) |
| thunderbird | 20 | 10 strikes x 5 | 2.50 | 4.44x (Extend: 100 for 28 = 3.57) |
| magma | 16 | 4 x 2 + about 5 burn, standing still | 0.81 | 1.44x |
| thunderhead | 20 | 4 strikes x 3 | 0.60 | 1.07x |
| stormclock | 16 | 4 + 3 + 3 if it stays (7 if it walks) | 0.62 (0.44) | 1.11x (0.78x) |
| stormweave, alone | 18 | 3 | 0.17 | 0.30x |
| thunderstep | 18 | 8 (area) | 0.44 | 0.79x |

Area (per enemy per mana, against 0.444): Thunderclap 5/12 = 0.42 (0.94x); Tremor 8/18 = 0.44 (1.0x); Sandstorm (2 x 4)/18 = 0.44 (1.0x); Basalt Surge 7/18 = 0.39 (0.88x); Bonespur (5 + 3)/16 = 0.50 (1.1x); Thunderquake 12, 8, 4 by distance over 20 = 0.60, 0.40, 0.20; Stormweave at 4 enemies: 6/18 = 0.33 (0.75x); Frostwire 4 or 6 over 18 = 0.22 or 0.33 (0.5 to 0.75x); Magnetize (3 x 3 s)/16 = 0.56 per toucher (1.3x); Thunderclap against Repel: 0.42 / 0.56 = 0.75x.

**A3. Lightning in a cluster.** N enemies each within the 4 x 6 x 4 box of the others; `strikes = min(8, N)`; each strike hurts all N: total = 12 x min(8, N) x N: N=1 12 (0.60/mana); N=2 48; N=3 108 (5.4/mana); N=5 300 (15/mana); N=8 768. The area reference at N=3 is 3 x 0.444 = 1.3/mana.

**A4. Plasma against Lightning.** Damage formula min(20, max(armour/5, armour - damage/(2 + toughness/4)))/25. Armour 0: Lightning 12 + 4 burn = 16, Plasma 9. Armour 8 (toughness 0): Lightning 11.0 + 4 = 15.0, Plasma 3.5 + 4.5 = 8.0. Armour 20 (toughness 8): Lightning 3.84 + 4 = 7.84, Plasma 1.1 + 4.5 = 5.6. Lightning is ahead or level in every case that is not fire-immune.

**A5. Buffs (effect-level-seconds per mana).** Surge (1 + 1) x 8 / 16 = 1.0. Swift 3 x 10 / 6 = 5.0; Empower 2 x 10 / 12 = 1.67; Swift + Empower 50 / 18 = 2.8; Overdrive (2 + 2 + 2) x 10 / 10 = 6.0; Accelerate (3 + 3 + 2 + 1) x 10 / 14 = 6.4; Warcry (1 + 1) x 12 / 10 = 2.4; Zephyr (1 + 1 + 1) x 6 / 14 = 1.3; Regrowth 2 x 8 / 10 = 1.6.

**A6. Mitigation** (prevented = incoming dps 6 x reduction x seconds, per mana; the median of the other T2 and T3 wards, Shulkershell 1.4, Stoneform 0.8, Geode 0.75, Haven 0.69, Barrier 0.67, is 0.75, so Stoneskin is 2.7x and Brace 3.2x, but Brace is capped at 9.6 prevented per 8 s and its 2 s window is a skill test: sustained it prevents 1.2 damage a second to Stoneskin's 2.4. Either Stoneskin is over or the T3 wards are under; table 2C does both: a price on Stoneskin, a payoff on Geode). Brace 6 x 0.8 x 2 / 4 = 2.4 (uptime 2 of every 8 s); Stoneskin 6 x 0.4 x 10 / 12 = 2.0; Shulkershell 6 x 0.8 x 4 / 14 = 1.4; Stoneform 6 x 0.2 x 8 / 12 = 0.8; Geode 6 x 0.4 x 5 / 16 = 0.75; Haven 6 x 0.2 x 8 / 14 = 0.69. Passive upkeep is `0.12 x cost` per second: Stoneskin 1.44 mana/s; a Twine Cord regenerates 5 per second.

**A7. Throw distances** (vanilla drag model, `FusedStormNumbers.glide`): Tempest push 2.8 with 0.9 up, 18.1 blocks; Push 2.2 and 0.45, 11.8; Thunderclap 2.0 and 0.5, 11.3; Repel 2.4 and 0.5 at the centre, 13.5 (1.44 at the edge, 8.1); Pelt 0.6 and 0.25, 2.7; Windcut 0.7 and 0.2, 2.8; proposed Thunderclap 0.8 and 0.3, 3.8; proposed Pelt 1.1 and 0.25, 5.0.

**A8. Control seconds per mana.** Jolt 1/9 = 0.11; Root 3/9 = 0.33; Weigh 5/8 = 0.62; Shackle 5/9 = 0.56; Mire 5/8 = 0.62; Sinkhole 2/18 = 0.11 (per enemy; times up to 8 enemies); Fossilize 2/16 = 0.13 (plus the 3 s ramp); Freeze 2.5/16 = 0.16; Glacier 2/16 = 0.13; Decree 2/10 = 0.20; Drowse 6/14 = 0.43; Chill (Slowness II) 6/4 = 1.5.

**A9. Credited values** (value = damage x tier price + riders at the market price; ratio = value / mana). Prices: damage 1.50, 1.45, 1.78 mana per point at T1, T2, T3 (the reciprocals of the medians in A1); hard stun 4.8 per second; Slowness IV, leash or pin 1.7 per second; Slowness II 0.67 per second; throw 0.33 per block; relocation 1.0 per block; blindness 1.0 per second; heal 1.5 per health; buff 0.42 per effect-level-second.

| rune | mana | value | ratio | note |
|---|---|---|---|---|
| shock, one target / two | 7 | 6.0 / 10.5 | 0.86 / 1.50 | |
| jolt | 9 | 10.6 | 1.18 | 4 damage 5.8 + 1 s stun 4.8 |
| aftershock | 9 | 14.5 | 1.61 | |
| stalactite, bare head | 9 | 15.3 | 1.70 | 1.13 with a helmet |
| ripple, living / undead | 10 | 11.7 / 35.2 | 1.17 / 3.52 | heal 2 / 6 health |
| lightning, the strike / with burn and slow | 20 | 21.4 / 30.2 | 1.07 / 1.51 | burn 4 damage, Slowness IV 1 s |
| thunderbird | 20 | 89.0 | 4.45 | 50 damage |
| tempest | 22 | 20.2 | 0.92 | throw 18 blocks credited 6; parts test: Harm 8 + Push 4 = 12, so 0.55 |
| plasma | 20 | 16.0 | 0.80 | Lightning is 21.4 to 30.2 for the same price |
| surge | 16 | 6.7 | 0.42 | Swift + Empower: 50 level-s for 18 mana = 21.0, 1.17 |
| riftbolt | 18 | 18.7 | 1.04 | Harm 8 + Banish 8 = 16 does 7 damage and 8 blocks: 0.89 |
| stormclock, still / walking | 16 | 17.8 / 12.5 | 1.11 / 0.78 | |
| thunderhead | 20 | 21.4 | 1.07 | flat in a crowd |
| frostwire, N = 1 / 2 / 3 | 18 | 9.8 / 19.6 / 29.4 | 0.54 / 1.09 / 1.63 | each target also slowed II for 4 s (2.7) |
| thunderstep | 18 | 18.2 | 1.01 | blink credited 4 |
| monolith | 20 | 12.7 | 0.63 | toss credited 2; Harm 8 + Launch 8 = 16: 0.8 |
| fossilize, before / after the 3 s delay | 16 | 22.3 / 15.6 | 1.39 / 0.98 | 2 s hold credited 9.6 |
| sinkhole, per enemy / three caught | 18 | 12.3 / 36.9 | 0.68 / 2.05 | 2 s pin credited 3.4 an enemy |
| magma, standing still | 16 | 23.1 | 1.45 | pool radius 1.6 |
| geode, one attacker / three | 16 | 13.8 / 29.2 | 0.86 / 1.83 | 12 health prevented at 0.5 mana, thorns 2 per hit at 0.5 hits a second; parts: Stoneskin + Bramble 9 to 12 mana: 0.56 to 0.75 |

After the proposals of table 2C: Tempest with a 4-damage second strike, cost 22: 1.24. Plasma at 10 with Ionised, cost 20: 0.99. Surge at 8 s and up to 4 arcs of 2: 1.31. Riftbolt at 7: 1.14. Monolith at 8 with the slam and a 4-block toss: 1.10. Thunderhead unchanged in damage: 1.07 (more in a crowd). Stormweave alone at 6: 0.59, at four enemies 7 each: 0.39 per enemy per mana (0.88x the area median).

## Appendix B. Presentation counts (packets per one cast at one target, power 1)

Read from the Vfx functions; treat as estimates. "Sounds" counts explicit `Fx.sound` events.

| rune | packets | sustained | sounds | shake | notes |
|---|---|---|---|---|---|
| shock | 7 to 20 | none | 1 to 2 | none | arc unit shared |
| jolt | about 26 | none | 3 | none | no stun cue |
| thunderclap | about 69 (44 rays) | none | 1 | 0.55 | on a miss too |
| lightning | about 41 per strike (up to 320) | none | 1 (+ vanilla bolts) | none | 8 bolt entities at most |
| ripple | about 15 | none | 1 | none | heal invisible |
| thunderbird | about 1,150 per bird | 43 pk/s | up to 37 | none | 3 birds = 130 pk/s |
| stormheart | 13 (cast) | none | 1 | none | retaliation: vanilla bolt |
| galvanize | 13 | 9 hums | 1 | none | expiry silent |
| tempest | about 39 per strike | none | 2 | 0.5 per strike | |
| plasma | 41 to 61 per target | none | 2 | none | uncapped |
| surge | about 30 | none | 1 | none | buff shown 0.3 s |
| magnetize | about 134 per 4 s | 33 pk/s per magnet | 3 + 2 per shock | none | 32 pk per shock |
| riftbolt | 110 to 136 per target | none | 4 | none | |
| stormweave | 110 to 170 in one tick | none | 4 | none | arcane sounds |
| stormclock | about 322 per spot | 12 pk/s | 25 per spot | none | up to 4 spots |
| thunderhead | about 260 per cloud | 62 pk/s | 10 | none | up to 4 clouds |
| frostwire | about 88 (full chain) | none | 8 | none | |
| thunderstep | about 108 | none | 2 | 0.5 | |
| shield | 1 flash + circles; break about 66 shards per circle | none | 3 to 5 | punch 0.6 on break | custom sounds |
| break, chisel, tunnel, vein, fell, excavate | 0 to 59 | none | 2 to 48 | excavate 0.45 | see C7 |
| stoneskin, brace | 13 to 14 | none | 1 to 2 | none | |
| stoneform | 14 (cast); 15 per proc | none | 2; 1 per proc | none | |
| root, weigh, shackle | 24, 11 + 8/s, 4 + 10 to 30/s | 8 to 30 pk/s | 1 to 17 | none | |
| pelt, aftershock | 12; 35 | none | 1; 2 | none | HEAVE adds about 38 + 5 displays |
| tremor | about 21 + 8 to 13 displays | none | 1 (+1) | 0.7 | |
| magma | about 35 per pool (4 pools) | 3 pk/s per pool | 7 per pool | none | |
| sinkhole | about 117 + 13 per enemy | 13 pk/s | 10 | 0.3, 0.5 | 6 displays |
| geode | 31 + 6 displays; hit 12 | 3 pk/s | 3; 2 per hit | none | |
| fossilize | about 115 over 4 to 5 s | none | 10 | 0.25 | |
| bonespur | about 45 per target | 2 pk/s | 3 per target | none | |
| monolith | about 72 + 5 displays per target | none | 10 | 0.35 per target | |
| thunderquake | about 119 + 7 per enemy per wave | none | 7 | 0.45, 0.55, 0.65 | no custom sound |
| infest | 26 first, 6 per pulse | 12 pk/s | 9 | none | |
| sandstorm | about 336 | 76 pk/s | 6 | none | |
| tusk_charge | about 22 + 9 per toss | none | 2 + 1 per toss | none | |
| mire | about 20 first, 14 per s | 14 pk/s | 1 | none | |
| stalactite | about 25 | none | 2 | punch if bare head (10.5) | |
| basalt_surge | 27 per column (up to about 324) | 540 pk/s during the burst | 1 per column | none | |
| prospect | 3 + up to 48 displays | none | 2 | none | |
