# Spell feel audit: frost and wind (51 runes)

Phase 1 (audit only, nothing in the game changed). Base: `main` at `e073939`, audited on the branch `spell-audit-frost-wind`.
Scope: every effect rune whose element is frost (28) or wind (23), fused, signature, innate and world runes included. The frost
element also carries the whole *water* family (Bubble, Tidecall, Undertow, Tidehook, Tidewrit, Drowning Word, Current, Tidebreath),
so "frost" here means ice and water.

Everything below was read from the code (`Effects`, `Techniques`, `CraftedRunes`, `Innates`, `ExplorerEffects`, `FusedFrost`,
`FusedStorm`, `SignatureFusions`, `Reactions`, `WorldRules`, the `*Vfx` classes, `sound_art.py`) and from `runes.tsv`. I did not run the
game: see "What I could not verify" at the end.

## 0. Read this first

**The ten findings that matter most**

1. **Two runes are clearly overdone.** *Bubble* (T2, 8 mana) is a full 3 s hold plus 4 damage plus a soak: 0.375 hold-seconds per mana,
   **3.0x the median hold** of its peers and **2.4x Freeze** (T3, 16 mana). *Flash Freeze* is a 3 s hold for 9 mana on anything wet, and
   rain makes everything wet: 2.7x the median, in a T2 slot. Smaller: *Repel* (1.3x the area median, armour-piercing, no cost) and
   *Mirrorfrost* (returns a stranger's whole spell for 12 mana, PvP).
2. **Eleven runes are dominated or thin.** *Hail* is worse than the T2 *Frost* it costs twice as much as (0.6x its damage per mana, 0.33x
   its slow per mana, half its reaction window). *Glacier* (0 damage, 2 s, 16 mana), *Absolute Zero* (20 mana for a conditional 7 damage),
   *Coldsnap*, *Levitate* (Bubble does the job better at the same price), *Feather Fall* (Cushion gives 3x the fall protection per mana),
   *Zephyr* (Burst + Swift + Leap is cheaper and stronger), dry-land *Undertow*, *Cryostasis* (Brace costs 4 mana), *Frostbloom*, *Frostward*.
3. **The biggest clone clusters.** (a) **Slow + FROZEN primer**: Chill, Frost, Coldsnap, Hail, Avalanche, Blizzard all deal "frost damage,
   a slowness, and a hidden frozen mark" at different sizes. (b) **Holds**: nine runes hold a target for 1 to 3 seconds (Bubble, Flash Freeze,
   Freeze, Glacier, Black Ice, Rime Seal, Hoarfrost, Absolute Zero, Frostbite). (c) **Vertical hurl**: Launch, Updraft, Summit Wind (plus Skyburst,
   Monolith, Tremor, Basalt Surge, Tusk Charge in other elements). (d) **Pull-to-caster**: Tidehook, Vinelash, Pull.
   (e) **Hoarfrost is Fossilize** (earth + time) in all but two details: the same Slowness ramp (I, II, III over 3 s), the same 2 s hold, the same 6 damage, the same 16 mana; the damage lands at the freeze instead of at the crack, and Hoarfrost holds a player 2 s where Fossilize holds one 1 s.
4. **The fused and signature runes are already the best-presented runes in the mod** (Frostbite's heartbeat, Recoil's stopped clock, the
   Dust Devil funnel, Cryostasis' ice cocoon, Skyglyph). The **sameness problem is the T1 and T2 basics**: one 4-damage poke after another,
   all wind runes ending in the same vanilla wind-charge pop, all frost runes ending in the same glass/powder-snow crack.
5. **Sound is element-wide, not rune-wide.** 20 cast and 30 impact sounds exist for ten elements; everything else is vanilla, reused
   heavily (`BREEZE_WIND_CHARGE_BURST`/`WIND_CHARGE_BURST` 28 times, `GLASS_BREAK`/`GLASS_PLACE` 29, `AMETHYST_BLOCK_CHIME`/`RESONATE`
   94 across the whole mod). Casting `Self . Swift` and `Self . Leap` and `Self . Feather Fall` is the same pose, the same circle colour,
   the same `cast_wind` sound: only a ~0.5 volume vanilla sting differs.
6. **Reaction marks are invisible.** FROZEN, WINDSWEPT, SOAKED, BLEEDING have no on-target indicator, so the setup half of every reaction
   (frost's whole job) can't be read. The payoff economics also make most primers break-even: Shatter adds 60% of the payoff hit, so
   Chill (4 mana) before Fire (5 damage) buys 3 damage, i.e. exactly what 4 mana buys anywhere else. Only big payoffs (Explode, Meteor) reward it.
7. **Frost has no reaction of its own to trigger, and wind has no setup that other elements can cash in except Wildfire.** Two cheap, small
   systemic fixes are proposed: end holds when Shatter/Fracture fire (today "the ice bursts" is text only, the hold runs on), and an
   **Airborne** mark (+20% damage from any source while off the ground) set by Launch, Levitate, Updraft and Cyclone.
8. **There is no "cannot cast" status in the mod except Cryostasis.** Silence, Hush and Decree don't stop a caster. A tiny `Silenced`
   state would give *Drowning Word* (a spoken word that drowns speech) and *Windcut* (cuts a channel) a PvP and PvE niche.
9. **Simple breakage** (fixed regardless): Frost says "freezes solid" but never holds; Repel is drawn in off-palette red; Glacier has no ice shell
   while the T3 it fuses from does; Tidewrit plays one sound up to 15 times in 15 ticks; a Disarmed mob killed within 5 s never drops its
   weapon; Frostward "can't freeze" but holds ignore it. Full list in section 7.
10. **Proposals are small on purpose.** Every rune has one; most are a single trigger, mark, number or hook that reuses an existing
    system (Reactions, Scheduler, Spirits.hold, TemporaryBlocks, Wards). Rough total: about 16 person-days for everything (a useful cut is about 5), most of it sound and polish.

## 1. Method and baselines

**Four axes** as briefed: power (arithmetic against peers), identity (a one-line verb, near clones), presentation (signature today and
proposed, incl. sound), caster feel. Everything is at power 1, duration 1, no reaction bonus, no PvP scale (x0.6 on players), no
Amplify/Extend, cost = the rune's own base cost (a shape adds the same flat cost and multiplier to every rune, so it doesn't change
comparisons; Touch adds +1, Bolt +3 and x1.1).

**Roles and what counts**

| role | measure | notes |
|---|---|---|
| single-target damage | damage per mana | every hit and DoT tick counted; burning = 1 per second, Poison II = 1 per 12 ticks; a heal or rider is not counted |
| area damage | damage per target caught per mana | a rune's own radius; "per target" so crowd size doesn't flatter it |
| hold (control) | seconds the target can't act per mana | a full hold (AI off, or Slowness VII plus Weakness V) = 1 per second |
| slow | slow-seconds per mana | speed cut x seconds (Slowness III = 45%): a slowed enemy still fights back, so it is not weighted like a hold |
| buff / ward / movement / world | compared with the siblings that do the same job | no honest single number: verdict by domination (is another rune strictly better?) |

**Verdict thresholds** (numeric roles): UNDER below 0.7x the peer median, FAIR 0.7x to 1.4x, OVER above 1.4x. Where a rune has a
condition (Flash Freeze on a wet target, Absolute Zero on a cold one) both cases are given. Presentation verdict codes: **OK**
(distinct and right-sized), **GENERIC** (shares silhouette or sound with siblings), **UNDER** / **OVER** (too small / too big for its
power), **MISMATCH** (wrong palette or wrong message), **SPAM** (repeated sound or heavy per-tick emitters).

**Reference numbers** (the full peer lists and the arithmetic are in Appendix A)

| role | tier | median of peers | range | peers (n) |
|---|---|---|---|---|
| single-target dmg / mana | T1 | **0.80** | 0.38 to 1.00 | harm, ember, shock, pelt, umbra, leech, countdown (7) |
| | T2 | **0.68** | 0.33 to 1.50 | fire, aftershock, stalactite, bleed, dismantle, venom, infest, ... (16) |
| | T3 | **0.60** | 0.31 to 1.00 | smite, lightning, sunscorch, soulfire, blackflame, resonant shriek, ... (13) |
| area dmg / target / mana | T2 | **0.42** | 0.27 to 0.64 | flashfire, thunderclap, moonpetal, rootsnare, sporebloom (5) |
| | T3 | **0.50** | 0.27 to 0.67 | explode, tremor, inferno, primer, sandstorm, riftcall, eclipse, ... (14) |
| | T4 | **0.64** | 0.46 to 0.83 | cometfall, dragon breath, hollow, sonic boom, wither (5) |
| hold s / mana | T2 to T4 | **0.125** (0.147 with Drowse/Shackle) | 0.08 to 0.56 | decree, jolt, rootsnare, drowse, stasis, sinkhole, heartstopper, fossilize, shackle (9) |
| slow-s / mana | T2 | 0.33 to 0.375 | | root, mire, weigh (earth) |

How the frost/wind runes read against them (details in the tables; arithmetic in Appendix A):

- single target: Icicle 0.67 (1.0 vs a slowed target), Windcut 0.67 (0.83x T1), Frost 0.62 (0.93x T2), Hail 0.38 (0.62x T3), Hoarfrost 0.38,
  Frostbite 0.50 (0.83x), Drowning Word 0.62 (1.04x), Absolute Zero 0.35 on a cold target (0.58x), Freeze 0.19 (0.31x, it is a hold);
- area: Repel **0.56 (1.33x T2)**, Coldsnap 0.27 (0.65x), Cyclone 0.30 (0.72x), Blizzard 0.22 (0.44x), Avalanche 0.33 to 0.50 (0.67x to 1.0x),
  Tidecall 0.38 (0.75x), Razorgale 0.50 (1.0x), Summit Wind 0.36 (0.71x), Updraft 0.25 (0.50x), Downdraft 0.29 to 0.71, Dust Devil 0.58 (0.90x),
  Tidewrit 0.33 (0.52x, but a 7-wide corridor of 10 to 20 blocks);
- holds: **Bubble 0.375 (3.0x median, 2.4x Freeze)**, **Flash Freeze wet 0.33 (2.7x)**, Cyclone 0.20 per target (1.6x), Freeze 0.156 (1.25x), Glacier
  0.125 (1.0x), Hoarfrost 0.125, Absolute Zero 0.10, Black Ice 0.094, Rime Seal 0.094 per enemy, Frostbite 0.062;
- slows: Chill **0.45** (the cheapest slow in the game: 1.2x to 1.4x the earth T2 slows, 2x Frost), Frost 0.225, Undertow 0.15, everything else 0.075 to 0.11.

**Mechanics that shape the numbers** (verified in code)

- Every harmful frost rune except Bubble and Tidehook also freezes water where it lands, cools lava, snuffs fire (`WorldRules`); every harmful wind
  rune knocks projectiles out of the air within 3.5 blocks and blows out small fires. So the four water runes that hurt (Tidecall, Undertow,
  Drowning Word, Tidewrit) do *frost* world magic (they can freeze the pond they are washing).
- Frost's reaction role: FROZEN mark (Chill, Coldsnap, Hail, Avalanche 2 s; Frost 4 s; Blizzard refreshes 1.5 s every half second) sets up
  Shatter (fire +60%) and Fracture (earth +40% and Cracked: everything +20% for 5 s). Frost damage triggers **no** reaction of its own.
- Wind's reaction role: WINDSWEPT (2.5 s) sets up Wildfire; any wind *damage* on a BLEEDING target is Rupture (+50%, +4 true damage, caster
  heals 2). `Rend . Windcut` = 11 mana for 10 damage and 2 healing (test: `WildercordReactionsTest.rupture`).
- Freeze-family holds: `Spirits.hold` = AI off for mobs, Slowness VII plus Weakness V for players and bosses ("bosses are only slowed" also
  strips their melee); `Spirits.freeze` adds the ice look and the FROZEN mark. Players so held can still cast (only Cryostasis blocks casting).
- Cooldown = 1 tick per mana (0.5 s to 20 s); passives (Self or Orbit) pay 0.12 mana/s per point of cost: a passive Swift is Speed III
  for 0.72 mana/s, a passive Cushion 0.6 mana/s.

## 2. Verdict table, one row per rune (51)

Columns: id, tier, mana, role, key numbers (letters = what scales: **A**mplify, **E**xtend, **W**iden, **L**inger, **S**hare/Kindred),
power verdict with ratio, **verb** (its one distinct property), overlap, presentation signature today (look, then sound), presentation
verdict, proposal (mechanic, numbers, new signature, sound id from section 5). "Look" cells name the silhouette and the sounds are the
vanilla or custom events the code plays.

**Five small systemic changes the proposals lean on** (each is one hook; details and risks in section 6)

- **S1 Ice ends when it is cracked.** Shatter and Fracture also end a Freeze/Glacier/Black Ice hold (`Spirits.thawNow`). Today the text says
  the ice bursts or thaws but the hold runs to its timer.
- **S2 Wind has mass.** Wind knockback scales with the creature's size (light things fly up to 1.5x as far, heavy ones down to 0.5x).
- **S3 Marks are visible.** A small server-side halo for each live mark: FROZEN a slow ring of frost glass over the head, WINDSWEPT a tiny
  crescent orbit, SOAKED and BLEEDING a drip, one particle per half second per marked creature.
- **S4 A `Silenced` state** (a map like Drowse's `ASLEEP`): `SpellCaster.cast` refuses like it does for Cryostasis, a Runebound's telegraph is cancelled.
- **S5 An Airborne mark** (`Reactions.Mark.AIRBORNE`, cleared on landing): +20% damage from any source while the creature is off the ground.

### 2.1 Frost: primers, strikes and holds

| id | T | mana | role | key numbers | power | verb | overlap | look and sound today | look | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| **chill** | 1 | 4 | slow, primer | 1 dmg; Slow II 6 s (1.8 slow-s); FROZEN 2 s. E L | FAIR, high as control: 0.45 slow-s/mana (1.2x to 1.4x the earth T2 slows, 2x Frost); 0.25 dmg/mana (0.31x T1) | primes and slows, cheaply | Frost, Coldsnap, Undertow | ground frost seal, thin ring, 5 flakes; powder-snow step | GENERIC: the same `frostCreep` seal as six frost runes; right size | **Stacks**: each Chill within 6 s of the last deepens the slow one level (II, III, IV) and refreshes it: the cold is built, and it is the only slow in the mod that stacks (three casts = 12 mana for Slow IV 6 s). Look: three thin rings climbing feet to hips over 6 ticks, one more ring per stack, breath fog after. Sound `frost_crust` soft (0.25 s), a step higher per stack. |
| **frost** | 2 | 8 | strike, primer | 5 freeze dmg; Slow III 4 s (1.8 slow-s); FROZEN 4 s (the longest window); frozen skin; **no hold**. A E L | FAIR: 0.62/mana = 0.93x T2; slow 0.225 | the reference frost hit: strike, slow, prime for 4 s | Chill, Hail, Coldsnap, Icicle | flash, 7 shard rays, shatter ring, frost seal; glass break + powder-snow break | OK, but shares its skeleton with Icicle, Hail, Glacier, Zero | Fix the text (it says "freezes solid"). Make the 4 s window visible (S3): a ring of frost glass over the head that thins as the window closes. Sound `frost_crust` (0.5 s) with an `ice_needle` layer. |
| **icicle** | 1 | 6 | strike (payoff) | 4 dmg, 6 vs a slowed or FROZEN target; +40 ticks of skin frost. A L | FAIR: 0.67 to 1.0/mana = 0.83x to 1.25x T1 | finisher for the slowed | Windcut, Pelt, Ember | flash, 10 ice chips, ring (slowed: extra white ring, glass break); snow-golem shoot | GENERIC: a radial burst like every T1 strike | **Melt**: the icicle stays lodged for 2 s (a needle of ice light, dripping), then melts and leaves the target SOAKED for 5 s, so frost hands over to storm (Conduct) and to Flash Freeze; it is also the first frost strike with an aftermath. Look: a needle of white light falling from 3 blocks up (a vertical silhouette, unlike every radial burst); slowed: thick needle plus an 8-point star. Sound `ice_needle`, a soft drip when it melts. |
| **coldsnap** | 2 | 11 | area primer | r3: 3 dmg; Slow II 4 s (1.2 slow-s); FROZEN 2 s. A E W | **UNDER**: 0.27/target/mana = 0.65x T2 area; slow 0.11 | group frost with no reason to pick it: `Burst . Chill` (12 mana) is r4 with Slow II 6 s | Chill; Blizzard, Avalanche, Hail; Flashfire, Thunderclap | ground seal, 2 rings, flash, 20 flakes, 10 chips; powder-snow break + glass break | GENERIC, fine size | **Crowd primer**: FROZEN 2 s -> 4 s on everyone hit, damage 3 -> 4, and the ring *travels*: enemies are hit by distance over 6 ticks, each with a glass tick, so the ring visibly snaps across the crowd. Sound `frost_crust` + light `snow_whump`. |
| **hail** | 3 | 16 | strike (volley) | 3 stones x 2 = 6 dmg over 0.5 s; Slow II 4 s; FROZEN 2 s. A E L | **UNDER**: 0.38/mana = 0.62x T3; vs Frost: 0.6x damage, 0.33x slow, half the window, twice the cost | nothing Frost doesn't do better | Frost; Jolt (a 1 s stun), Starfire (fire + arcane volley) | 3 rays from above + shards + 6 chips each; glass hits rising | UNDER: a T3 finisher that looks like three small icicles | **Pummel**: 5 stones x 2 = 10 (0.62/mana), one every 4 ticks, each staggering the target for 5 ticks: about a second of flinching in which it can't finish a swing, a draw or a cast (players are only slowed). Look: a flat grey cloud disc (2 wide, faint yellow flicker) over the target, white pellets, a tiny crack ring per hit. Sound `hail_patter`. |
| **glacier** | 3 | 16 | hold | hold 2 s (1 s players), 0 dmg. E L | **UNDER**: 0.125 hold/mana (1.0x median) with no damage; 0.33x Bubble | a smaller Freeze | Freeze, Bubble, Black Ice | frost creep, cracks, 8 shards, ring, packed-ice chips, **no shell** (Freeze and Flash Freeze have one); glass place + impact frost | UNDER: the fused rune looks smaller than the T3 it fuses from | **Calving glacier**: up to 3 other enemies within 2.5 blocks are held 1 s; when the ice cracks everything held takes 2. Real ice shells (`BlockFx.encase`) on all of them, fissures running along the ground between them. Sound `ice_lock` then `ice_break`. |
| **freeze** | 3 | 16 | hold | mob: AI off 2.5 s; players: Slow VII + Weakness V; 3 dmg; FROZEN. A E L | FAIR on its own numbers (0.156 hold/mana = 1.25x median; damage 0.19), but 0.42x Bubble per mana | the unconditional, unbreakable single lock | Bubble, Glacier, Black Ice, Flash Freeze | ice-block shell on mobs, 2 rings, 5 shards, seal, flakes; glass place + powder-snow break (low) | OK: the best frost look | Cost 16 -> 14 (0.18 hold/mana, closing part of the gap to Bubble). Identity: the only hold that ignores size (Bubble's limit) and wetness (Flash Freeze), and only fire or earth can crack it (S1: the reaction ends the hold). A 3-tick snap ring closes on the target before the shell rises. Sound `ice_lock`. |
| **black_ice** | 3 | 16 | hold, curse | hold 1.5 s (1 s players); Weakness II 5 s; dies within 5 s: 4 dmg r3. A E L | FAIR-low: 0.094 hold/mana (0.75x) plus a melee-neutering Weakness and the shatter | frozen, weakened, brittle: kill it and its neighbours pay | Freeze, Glacier; Malison (void) | violet-black ice closing, spikes, violet cracks, black-glass burst; low glass place + impact void + impact frost | OK: distinct palette | **Cascade**: the shatter re-applies brittle (3 s) to the survivors it hits, chain depth 3. Sound `ice_lock` dark (-4 semitones) and `ice_break` dark. |
| **absolute_zero** | 3 | 20 | finisher | always Slow IV 3 s (1.8 slow-s); if already cold: hold 2 s (1 s players) + 7 dmg, then a 3 s lockout. A E L | **UNDER**: 0.35/mana on a cold target = 0.58x T3, 0.10 hold/mana, needs a second cast | the finisher for cold | Freeze; Prismatic Burst (arcane also spends marks) | hard white rings falling in, snow stopped dead, crown of ice spikes, deep blue; low powder-snow, impact frost, glass place, amethyst break | OK | **Counts the cold**: conditions = slowed, FROZEN mark, frozen skin, held (n = 0..4). n >= 1: damage 2 + 2.5n (4.5 / 7 / 9.5 / 12), hold 1 + 0.5n s (players half), lockout kept; cost 20 -> 18. n ice spikes rise so the damage reads. Sound `zero_hush` (air pulled out, one glass note). |
| **rime_seal** | 3 | 16 | trap | r1.5 seal, 6 s; an enemy standing 1 s: hold 1.5 s (1 s players) + 3 dmg, once each; cap 16. A E W | FAIR: 0.09 hold/mana per enemy (0.75x) + 3 dmg; needs enemies to stand still | the ground trap that freezes whoever lingers | Mine, Snare shapes | layered arcane-frost sigil (circle, ring, star, ring), comets, tightening rings, pillar of light; illusioner spell + powder-snow + impact arcane | OK: strong, distinct | An enemy that steps off *before* its second is up keeps Slow I 2 s (skirting costs something). The glyph pulses once a second so the trigger is readable. Sound `ice_lock` bright (+3 semitones) on trigger. |
| **flash_freeze** | 2 | 9 | conditional hold | 4 dmg; wet or soaked: hold 3 s (1.5 players), its soak turns to ice; dry: Slow II 3 s. A E L | **OVER when wet**: 0.33 hold/mana = 2.7x median, 2.1x Freeze; FAIR dry (0.44 dmg/mana = 0.66x T2, slow 0.10) | freezes what is wet | Freeze, Bubble | flash, ring, flakes (+ ice chips and a shell if wet); powder-snow step (+ glass place) | GENERIC, small for its best case | Rain-wet: hold 2 s; soaked (any water rune, Tidebreath, a popped Bubble): 3 s; dry: Slow II 3 s and FROZEN 2 s. Look: a frozen splash crown (a ring of upward blue-to-white spikes at the feet). Sound `flash_splat` (wet splat cut by a glass ping). |
| **frostbite** | 3 | 16 | DoT, delayed hold | 3 + 5 x 1 = 8 dmg over 5 s; Slow I, II, III; 1 s hold at the end; skin chill. A E L | FAIR-low: 0.50/mana = 0.83x T3; 0.06 hold/mana | cold that sets in beat by beat | Hoarfrost, Fossilize; DoTs | frost-over-crimson jaws, heartbeat rings, blood-red crystals; **warden heartbeat** dropping in pitch | OK: one of the most distinctive | Final hold 1 s -> 1.5 s. The heartbeat is its signature: keep it, no other change. |
| **hoarfrost** | 3 | 16 | delayed hold | Slow I, II, III over 3 s (0.9 slow-s); at 3 s: hold 2 s + 6 dmg; FROZEN. A E | FAIR: 0.38 dmg (0.62x) and 0.125 hold (1.0x) per mana; **the same numbers as Fossilize** (damage timing and a player's hold aside) | the delayed, escalating lock | Fossilize (earth + time), Frostbite | rime rings climbing by stage, seal, flakes; frost impact at the end; powder-snow step rising, glass break | OK | **Bloom**: when the ice closes, enemies within 2 blocks take 3 and Slowness III 2 s. Fossilize's owner diverges the other way (section 3, H). Sound `rime_creep` (three rising notes), `ice_break` at the end. |
| **blizzard** | 3 | 18 | field, primer | r3, 4 s: Slow II held on, chill, FROZEN refreshed twice a second; 1 dmg per second (4); cap 16. A E W | **UNDER** on damage: 0.22 = 0.44x T3 area; value is that everything in it is Shatter-ready (fire +60% on the whole crowd) | a storm that keeps a crowd primed | Sandstorm, Inferno, Crimson Mist (fields), Hush | wind ring + frost star sigil, funnel of wind rings, crescent whips, snow billows; breeze loop + powder-snow step | OK: strong | **Advancing whiteout**: the storm walks 6 blocks along the caster's facing over its 4 s and bites 1 -> 1.5. The funnel leans into its travel. Sound `blizzard_bed` loop + `snow_whump` on open. |
| **avalanche** | 3 | 18 | area strike | r3: 6 dmg (9 bare head); Slow III 3 s; FROZEN 2 s; up to 8 snow drifts 10 s. A E W | FAIR: 0.33 to 0.50/target/mana = 0.67x to 1.0x | crush from above and leave the ground white | Coldsnap, Stalactite (parents), Tremor | star seal, 8 falling rays with dripstone and ice chips, frost impact, shatter ring, snow billows, screen shake; dripstone land + powder-snow break + impact frost | OK | Keep the mechanic (impact plus terrain is already its own). Replace the three vanilla layers with one custom `snow_whump` (big). |

### 2.2 Frost: water, world

| id | T | mana | role | key numbers | power | verb | overlap | look and sound today | look | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| **bubble** | 2 | 8 | hold | mob: AI off 3 s (players: Levitation I + Slow IV, they can still cast); pop 4 dmg + soaked; lifts ~1.8 blocks. A E | **OVER**: 0.375 hold/mana = 3.0x median, 2.4x Freeze; plus 0.5 dmg/mana, one tier below | a floating stun that pays when left alone | Freeze, Flash Freeze, Levitate | shimmering water sphere (two tilted rings + glint), pop with rings and splash; bubble pop + splash | OK | **Strained bubble**: a bubble holds small things longest: 2.5 s on a creature up to 0.8 wide (zombies, skeletons, creepers, endermen), 2 s up to 1.4, 1.5 s above; cost 8 -> 9. It floats 2.5 blocks (was 1.8) and wobbles visibly against a big body. Pop unchanged (4, soaked). Sounds `bubble_inflate`, `bubble_pop`. |
| **undertow** | 2 | 9 | slow, water damage | Slow III 3 s (1.35 slow-s) + soaked; **0 dmg on land**; in water: pulled under + 5 drown dmg. A E | **UNDER on land**: 0.15 slow-s/mana (0.33x Chill), 0 dmg; in water 0.55 dmg/mana | drag it under | Chill, Mire (earth), Drowning Word | water ring, ground ring, down-current bubbles, splash; whirlpool | OK | **Toward the water**: pulls the target toward the nearest water within 6 blocks and under it (5 dmg); with none near: 3 dmg + Slow III 3 s + soaked (the ground turns to slurry). Look: a downward corkscrew of bubbles. Sound `water_drag`. |
| **tidecall** | 3 | 16 | area strike | r3.5: 6 magic dmg; dragged ~0.25 of the way in; soaked; cap 32. A W L | FAIR-low: 0.38/target/mana = 0.75x T3 area | the wave that gathers a crowd | Gravity Well, Sinkhole, Singularity (pull-in), Avalanche | water ring converging, 16 splash jets, crash + shards 6 ticks later; conduit + splash + impact frost | OK | Marks **PULLED** (Repel's Collapse x2, Explode's Implode +50% radius) and drags harder (~0.5 of the way, capped 1.2): a crowd bunched by the tide takes +1 per neighbour within 1.5 blocks (max +4). Sound `water_surge` (crash). |
| **tidewrit** | 4 | 30 | line strike | 7 wide, 10 to 20 blocks: 10 magic dmg, swept ~11 blocks, soaked. A W | FAIR: 0.33/target/mana = 0.52x T4, but it hits everything in a corridor | the tide that clears a hall | Cometfall, Starfall | crest ray + 7 splash jets + bubble columns per step; **riptide sound on each of ~15 steps** | SPAM (sound); look OK | One sound at the start and one at the end, none per step. Vertical curtain rays (2.2 tall, one block apart) so it reads as a *wall*. Sound `water_surge` (1.4 s sweep, once). |
| **tidehook** | 2 | 9 | pull | 4 magic dmg; three tugs 7 ticks apart reel it to your feet; soaked; bosses struck only. A L | FAIR-low: 0.44/mana = 0.66x T2 ST; reels 8+ blocks | the fishing line | Vinelash (life), Pull (void) | line of water from the hand, hook crescent, splashes; bobber throw, splash, retrieve rising | OK: distinct | **Stagger on arrival**: a 0.5 s hold when it reaches you (it gasps); flying mobs are reeled to the ground. Sound `hook_whip` (zip, splash, a twang rising per tug). |
| **drowning_word** | 3 | 16 | DoT | 5 pulses x 2 = 10 drown dmg over 5 s; air 0; soaked. A E | FAIR: 0.62/mana = 1.04x T3 | silent, armour-piercing drowning | Bleed, Blood Moss, Entropy; Silence (arcane, doesn't stop casters) | bubbles from the mouth, water glyph band over the head, ring; drowned ambient | OK | **Silence**: while its lungs are full a player can't cast (S4) and a Runebound's telegraph is cancelled. Sound `water_drag` (breath pulses, one a second). |
| **current** | 2 | 6 | movement | ~15 blocks the way you look, only in water or rain (fizzles on dry land), no fall damage until you land. A | UNDER as a conditional dash (Dash: ~18 blocks for the same 6, anywhere) | ride the water | Dash, Launch, Tusk Charge | water ring, spray, bubbles streaming behind; riptide + cast frost | OK | **Carries allies** within 2 blocks along with you (a river takes the group). Sound `water_surge` (short). |
| **tidebreath** | 1 | 4 | buff | Water Breathing + Dolphin's Grace 30 s; leaves you WET 5 s (fire hits -25%). E S | FAIR (utility) | breathe and swim | Current, Icepath | water crescents swirling up, ground ring, bubbles, splash; conduit activate | OK | Say what it does: it also douses you and allies (extinguishes, fire -25% for 5 s, already true; add the extinguish). Sound `breath_bubbles`. |
| **icepath** | 1 | 3 | world | freezes water within 3 blocks into frosted ice (thaws by 30 s or in light); block budget 32. W | FAIR (niche) | walk on water | Span (arcane: 16 blocks of glass over anything), Current | frost seal spreading, 4 shards, flakes; glass place | OK | **A path, literally**: on Self or Touch it lays a 3-wide strip up to 10 blocks along your look over water, an ice front growing outward tick by tick; a disc when aimed at a point. Sound `frost_crust` long (0.9 s creak). |

### 2.3 Frost: wards and the innate

| id | T | mana | role | key numbers | power | verb | overlap | look and sound today | look | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| **frostward** | 1 | 3 | ward | 60 s: clears frozen skin and the FROZEN mark every 5 ticks (blocks Shatter/Fracture on the ward). Slowness and holds are untouched. E S | **UNDER**: the text promises "can't freeze" but holds still land; it stops powder snow and two reactions | cold-proofing | Fireward (fire) | pale rings climbing, snow melting away; chime + extinguish | OK | **Holds on you last at most 1 s** (a hook in `Spirits.freeze`/`hold`) on top of today's effect; a warded freeze puffs steam. Sound `warm_ward` (warm glass chord + breath). |
| **frostbloom** | 3 | 14 | ally ward | Regen II 5 s (4 hp); 8 s: each striker gets Slow III 2 s. A E S | **UNDER**: 0.29 hp/mana = 0.45x Heal (0.67) and Regrowth (0.64); the ward is the value | hit me and freeze | Geode, Bramble, Reflect | ice lotus petals, star seal, motes; chime + azalea leaves + impact life | OK | Each frozen striker **heals the ally 1** (max 4 per cast): being hit is rewarded. Sound `lotus_chime` plus a ping per proc. |
| **cryostasis** | 3 | 18 | ally save | 2 s (4 max) untouchable, can't cast, heal 6; 10 s lockout. A E | **UNDER**: Brace (4 mana, 80% less for 2 s) costs 0.22x; full immunity plus 6 heal is the only edge | the panic button | Brace (earth), Shulkershell, Second Wind | clear ice cocoon, clock face, gold ticks; glass place + bell + impact time, hat tick, glass break | OK | **Bursts out**: on release, enemies within 3 blocks are thrown back 1.2, take 3, get Slow II 3 s and FROZEN 2 s. Sound `ice_lock` then `ice_break`. |
| **mirrorfrost** | 1 | 12 | innate | recasts the last spell that hit you in 30 s as yours: no extra mana, any rune (no ownership check). | FAIR to OVER in PvP: returns a whole 30 to 60 mana spell for 12; reactive only | answer | Reflect, Riposte, Parry | telegraph sigil, glass panes, shatter ring, shards; glass break + mirror move | OK | Returned at 70% power; a mirrored spell can't be mirrored. Look: shards spiral back *into* the hand (6-tick anticipation) before the spell leaves. Sound `mirror_reform` (reversed shatter into a glass ring). |

### 2.4 Wind: buffs, mobility, wards, utility

| id | T | mana | role | key numbers | power | verb | overlap | look and sound today | look | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| **feather_fall** | 1 | 6 | buff | Slow Falling 12 s + no fall damage. E S | **UNDER**: Cushion gives 30 s for 5 = 3x the protection per mana | slow descent | Cushion, Summit Wind self | 5 feathers falling, ground ring, crescent, clouds; amethyst chime | OK, quiet | **Featherglide**: while airborne under it you drift the way you look (0.03/tick, max 0.35 blocks/tick); sneaking stops the drift. Feathers trail from the shoulders. Sound `feather_chime`. (Keep it out of passives.) |
| **swift** | 1 | 6 | buff | Speed III 10 s (+60%). E A S | FAIR, high: the cheapest strong buff; as a passive 0.72 mana/s for permanent Speed III | speed | Leap, Dash, Gale Mantle | swirl round the legs, ground ring, small gusts; breeze jump | GENERIC (same gust grammar as eight wind runes) | **Shakes off the cold**: the cast clears Slowness and frozen skin (not holds): wind is frost's counter. Streaks at the legs only and a sonic ring at the feet. Sound `dash_streak` (soft). |
| **leap** | 1 | 4 | buff | Jump Boost III 15 s. E A S | FAIR | spring | Swift, Summit Wind self | 2 ground rings, swirl, clouds; rabbit jump | GENERIC | Keep. Look: a crouch ring then an upward burst. Sound `feather_chime` with a quick upward glide. |
| **cushion** | 1 | 5 | ward | 30 s no fall damage; a fall over 4 blocks throws a gust (knock 1.1, r3, no damage). E S | FAIR (it dominates Feather Fall) | soft landing | Feather Fall, Cyclone | soft ground ring, gust; wool + breeze; landing ring + gust | OK | **Landing impact**: the gust deals 0.75 per block fallen beyond 4 (max 6 at 12 blocks), r3, wind: your fall becomes their damage. Look: the landing burst scales with the fall (a ring per 3 blocks). Sound `feather_chime` low; landing `gust_thump`. |
| **deflect** | 2 | 9 | ward | 8 s: projectiles within 3 blocks of the target are turned aside (velocity away, x0.6). E S | FAIR | turn arrows away | Haven, Infinity, Shield | ring of wind rising, orbiting gusts every 5 ticks, slash per deflection; breeze deflect | OK | **Returns to sender**: a deflected projectile flies back at its shooter at 0.8x speed, owned by the ward's target (once per projectile). Look: a bright ricochet line along the return path. Sound `deflect_ping`. |
| **prune** | 1 | 2 | world | clears leaves, grass, flowers, vines, cobwebs within 3 blocks (nearest first, budget 32). W | FAIR (utility) | clear the brush | Chisel, Glimmer (other world runes) | two gust rings + 12 small gusts; wind-charge burst + grass break | OK | **As shears**: leaves, cobwebs and vines drop as they would to shears (string from webs). Look: a flat scythe crescent sweeping a full circle over 4 ticks. Sound `gust_slash` short + grass break. |
| **dash** | 2 | 6 | movement, shove | Self: ~18 blocks along the look; others shoved the same way (impulse 2.6). A | FAIR | the straight dash | Launch, Gale Mantle, Current, Tusk Charge | two rings behind, speed-line rays, gust; breeze shoot | OK | **Precise**: horizontal only, velocity held 5 ticks then braked (about 12 blocks, never up a cliff or off one). Two long speed lines, a compression ring at the start and a brake ring at the end. Sound `dash_streak` ending in a soft stop pop. |
| **gale_mantle** | 1 | 10 | innate, movement | 12 s: jump again in midair to dash (3 dashes, ~10 blocks each, resets the fall). E | FAIR (~30 blocks of flight for 10) | air-dashes | Dash, Skyglyph | gust ring + swirl + wind-charge burst; each dash: gust, ring, breeze jump | OK | Dashes go where you **steer** (WASD), not where you look; each ends in a small gust that shoves enemies within 1.5 blocks aside. A ghost trail per dash. Sound `dash_streak` (short, rising through the chain). |
| **zephyr** | 3 | 14 | party buff | r4, 6 s: Speed I, Jump I, Regen I to allies (cap 8). A E W | **UNDER**: `Burst . Swift . Leap` = 21 mana buys Speed III + Jump III for 10 to 15 s for the same crowd | a warm breeze for the party | Warcry (blood), Regrowth | warm seal + petals + three breeze waves; wind burst + cherry leaves + impact life | OK | **Clear air**: also cleanses Blindness, Darkness, Nausea and Slowness, and lasts 10 s. Sound `feather_chime` warm (chorus). |

### 2.5 Wind: displacement and damage

| id | T | mana | role | key numbers | power | verb | overlap | look and sound today | look | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| **push** | 1 | 4 | displace | knock 2.2 + 0.45 up (~17 blocks), WINDSWEPT, no damage. A | FAIR: its whole job; ~17 blocks for 4 (Thunderclap ~17 for 12) | shove | Repel, Thunderclap, Pull, Pelt | two crescents, ring, gust, clouds; wind-charge burst | GENERIC | **Wall slam**: a pushed creature that hits a block hard within 1 s takes 2 and 0.5 s stun (once). Wind has mass (S2). Look: one broad crescent slab thrust along the push and a low dust ring, instead of two crescents, a ring and clouds. Sound `gust_thump`, the slam adds its dry knock. |
| **launch** | 2 | 8 | displace, mobility | up 1.5 (12-block apex, 1.8 s); Self: rocket + 0.7 forward; WINDSWEPT. A | FAIR; Self ends in ~9 fall damage unless you slow-fall | up | Updraft, Summit Wind, Skyburst, Monolith | gust ring, spiral swirl, gust emitter, cloud ring; breeze jump | GENERIC | Sets **AIRBORNE** (S5, up to 2 s). Look: a pillar of spiralling crescents (Self: under your feet). Sound `rise_whoosh`. |
| **levitate** | 2 | 8 | control | Levitation II 3 s (rise ~5.6 blocks, then ~2.6 fall dmg); bosses slowed; WINDSWEPT. E L | **UNDER**: Bubble holds at the same price (3 s AI off + 4 dmg); a levitating mob still attacks what it reaches | float | Bubble, Launch | 3 rising rings, 8 END_ROD motes drifting up, clouds; shulker shoot | OK, modest | **Suspend**: Levitation I (rise ~2.8) with horizontal drift stopped, and AIRBORNE for the 3 s. A hanging target is what Downdraft and Skyburst want. Look: a pale halo ring at hover height. Sound `rise_whoosh` soft. |
| **windcut** | 1 | 6 | strike | 4 wind dmg + shove 0.7 (~4 blocks), WINDSWEPT; Rupture on the bleeding. A L | FAIR: 0.67/mana = 0.83x T1; a near-clone of Pelt (5 mana) | the cutting wind | Pelt, Icicle, Ember | two crossed blades, small gusts; sweep attack + breeze shoot | GENERIC | **Interrupts**: cancels an enemy player's charge, a bow draw, a creeper's fuse, a Runebound's telegraph. Look: one long white-hot slash line with a mint halo (a cut, not a burst). Sound `gust_slash`; on an interrupt a tiny reversed puff. |
| **repel** | 2 | 9 | area strike | r3: 5 magic dmg + knock 2.4 x (1 - 0.4 d/r) + 0.5 up (12 to 20 blocks), WINDSWEPT; x2 on the just-pulled (Collapse). A W L | FAIR, high: 0.56/target/mana = 1.33x T2 area, armour-piercing, no drawback | the blast that gets things off me | Thunderclap (12 mana: same 5 dmg, r3, plus Slow IV), Push | **red** shell rings and blades (0xFF5050), gusts; wind-charge burst + explosion | MISMATCH: red on a mint element | Damage 5 -> 4 (0.44/mana, at the median; Collapse still doubles it). Palette to wind; a **dome** (hemisphere of ring stacks) instead of flat rings. Wind has mass (S2). Sound `gust_thump` plus a 90 Hz shock. |
| **disarm** | 2 | 7 | control | a mob's main-hand item is gone 5 s (bosses, players, empty hands keep hold); 0.35 shove; WINDSWEPT. E | FAIR (niche; decisive against skeletons, pillagers, vindicators, piglins) | snatch the weapon | Silence, Blind | swirl + item flying up; item break + wind-charge burst; rearm cloth sound | OK | Players: their main-hand item goes on a 3 s cooldown (bow, crossbow, shield, trident, pearl), nothing taken. A mob killed while disarmed drops its weapon (today it is lost). Sound `snatch_flutter`. |
| **cyclone** | 2 | 10 | area control | r3: carried around 2 s (up 0.14/tick), flung 1.4 + 0.5 up (~12 blocks), 3 dmg; bosses struck not moved. A E W | FAIR: 0.20 hold-s/target/mana (1.6x), 0.30 dmg (0.72x) | the crowd whirl | Dust Devil, Sandstorm, Vortex | crescents at 3 heights, gust spray, rise ring, fling ring; breeze air loop | OK | **Bowling**: the fling goes the way the caster faced, not radially, so the crowd is thrown as one; the whirl sets AIRBORNE. The funnel leans toward the fling for its last 5 ticks (telegraph). Sound `whirl_loop` + `gust_thump`. |

### 2.6 Wind: fused and signature

| id | T | mana | role | key numbers | power | verb | overlap | look and sound today | look | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| **downdraft** | 3 | 14 | anti-air | r6: every airborne enemy (cap 8) slammed down: 4 + 1 per block fallen (max 10); flyers lose lift, gliders 2 s. A W | FAIR, conditional: 0.29 to 0.71/target/mana; **does nothing if nothing is airborne** | swat things out of the sky | Weigh (earth, drags fliers); Launch, Levitate (setups) | spiral crescents falling, sand-turning colours, streaks, cracks, rings; wind burst + impact earth + mace smash | OK, but a miss shows a big downburst for nothing | **Whiff fallback**: with no airborne target, grounded enemies in r6 are pinned (Slowness III 1 s + 2 dmg). Sound `fall_crash`. |
| **updraft** | 3 | 16 | vertical hurl | r2.5 (cap 8): thrown ~8 blocks; 13 ticks later smashed down for 4; no fall damage. A W | FAIR: 0.25/mana single (0.50x) but 1.4 s of aerial denial x 8 | the launch that always comes back down | Launch, Summit Wind, Downdraft | ring column rising, two swirls, four side rays, cloud spray; wind burst + wind throw; smash: mace smash | OK | Keep; it sets AIRBORNE while it flies (S5). Sound `rise_whoosh`, then `fall_crash` on the smash tick. |
| **summit_wind** | 3 | 14 | vertical hurl, self | r3: 5 dmg, hurled up ~7 and away ~9; Self: 12-block rocket + 10 s slow fall. A W | FAIR-low: 0.36/mana = 0.71x T3; overlaps Updraft and Launch | mountain wind | Updraft, Launch, Tempest, Skyburst | mint-white swirl column + gust ring + flakes; wind burst + impact wind | GENERIC (its own swirl, shared sound) | **Exile**: hurled enemies get Slow Falling 4 s, so they glide out of the fight instead of dropping onto it (damage unchanged); +20% above y=120 (thin air). Updraft engages, this disengages. Look: a streak trailing each hurled enemy. Sound `rise_whoosh` dark with a horn-like partial. |
| **skyglyph** | 3 | 12 | pad | glyph 10 s (2 per caster, 1 s rest): an ally is launched ~8 blocks + forward, no fall; an enemy thrown 4 blocks, WINDSWEPT. A E | FAIR (cheapest T3) | a launch pad | Launch, Gale Mantle; no clone | arcane star turning in a wind ring, pinwheel of crescents, glyphs; amethyst resonate + cast wind; launch chime | OK: most distinct | Keep. Sound `glyph_hum` (low hum + shimmer, 0.8 s). |
| **recoil** | 3 | 14 | displace, rewind | thrown 5 blocks; 2 s later snapped back to where it stood, 3 dmg (adds up); cap 8. A | UNDER on damage 0.21/mana; the value is positional | throw, then rewind | Warp Step, Banish, Rewind | gold ring + wind crescents; a stopped clock where it stood, gold tether, hands running back; clicks + mirror move | OK: distinct | Damage 3 -> 5. Sound `rewind_whip` (reversed whoosh plus a tick). |
| **razorgale** | 3 | 18 | area strike, reaction | r3: cut 2 + bleeding; 0.5 s later 2 again = Rupture (+50%, +4 true): ~9 per enemy; heals 2 (once a second). A W | FAIR: 0.50/target/mana = 1.0x T3 | cut, then tear | Windcut + Bleed (parents), Crimson Mist | wind and crimson crescents in two passes (the second reversed), gust ring, cuts and drips; sweep + breeze shoot | OK | Keep. Sound `gust_slash` twice (the second higher). |
| **dust_devil** | 4 | 26 | roaming field | 5 s, r2 (cap 16), chases the nearest enemy within 12 blocks; scour 3 x 5 = 15 per enemy; blinds; whirls; flings at the end. A E W | FAIR: 0.58/target/mana = 0.90x T4 | the whirl that hunts | Cyclone, Sandstorm (earth) | funnel of wind and sand rings, sand block particles, clouds; wind burst, breeze idle, sand break | OK | Keep. Sound `whirl_loop` with sand grains. |

### 2.7 How they play through the common shapes

- **Self.** A harmful single-target rune does nothing on Self (`harmed` never contains the caster): Frost, Chill, Icicle, Windcut, Push, Disarm.
  But every rune that takes its own point works as a *nova around you*: Coldsnap, Blizzard, Rime Seal, Avalanche, Razorgale, Dust Devil, Tidecall,
  Cyclone, Repel, and the movement runes move you (Launch, Dash, Levitate, Current, Summit Wind). Self is free, so `Self . Coldsnap` (11 mana) is
  the cheapest way to cast any point rune.
- **Burst and Nova make point runes redundant.** They add 6 (or 3) mana and a x1.5 (x1.3) multiplier and hand the rune only its centre, so
  `Burst . Coldsnap` (22.5 mana) is the same disc as `Self . Coldsnap` (11) or `Touch . Coldsnap` (12). Point runes are for Self, Touch, Beam, Bolt
  (choose where) and Zone, Rain, Pulse (repeat), never the area shapes. The same is true of Flashfire, Thunderclap, Explode and Inferno.
- **Area shapes turn single-target runes into area runes at x1.5.** `Burst . Frost` (18 mana: r4, 5 damage, Slow III, a 4 s mark) beats Coldsnap
  (11 mana: r3, 3 damage, Slow II, a 2 s mark) for 7 more; `Burst . Chill` (12 mana) is a wider, longer slow than Coldsnap for one more.
- **Zone, Rain, Linger and Echo repeat the whole effect.** A Zone is 8 mana plus x2.0 for six pulses, so anything under it is about 2.5x as
  efficient per mana as under Touch (Hail: 36 damage for 40 mana against 6 for 17). The effects meant to be stackable say "starts over" and are
  guarded (Frostbite, Drowning Word, Fossilize; Blizzard, Rime Seal and Dust Devil renew instead of stacking); Hoarfrost and Bubble's pop are not
  (section 7, item 9).
- **Amplify multiplies displacement too**: Push at x1.5 is about 26 blocks, Repel about 30 at the centre. Fine for a T1 rune, worth a cap on the wind mass rule (S2).
- **Cooldown is one tick per mana of the whole spell (0.5 s to 20 s)**, so under 10 mana a spell is limited by mana only: `Touch . Chill` is 5 mana per
  half second (10 a second), `Touch . Windcut` 7 (14 a second) against 5 to 8 a second of regeneration, which empties a Twine Cord's 100 mana in about
  11 seconds. A permanent Swift is 6 mana per 10 s.

## 3. Clusters and how each is broken up

The per-rune proposals are in section 2. This section shows the *axis* on which each cluster member becomes different, so that two members
never pull the same lever. Axes: **delivery** (instant, delayed, travelling, lingering, trap, pursuing), **targeting** (one, spread, arc,
ring, line, self-centred), **interaction** (marks, conditions, terrain, modifiers), **price or counterplay**.

### A. Slow + FROZEN primer (Chill, Frost, Icicle, Coldsnap, Hail, Avalanche, Blizzard, Absolute Zero, Flash Freeze)

Today all of them are "frost damage, a Slowness, a hidden FROZEN mark" in different sizes. The mark is what matters, and it is invisible.

| member | delivery | targeting | interaction | its own trick |
|---|---|---|---|---|
| Chill | instant | one | slow 6 s, window 2 s | **stacks** (II, III, IV) |
| Frost | instant | one | the longest window (4 s) | **visible window** (S3) |
| Icicle | instant, needle from above | one | payoff: +50% vs slowed | **melts into a soak** |
| Coldsnap | instant, travelling ring | crowd r3 | window 4 s on everyone | **crowd primer** |
| Hail | volley over 1 s | one | Slow II, a 1 s flinch | **pummel** |
| Avalanche | impact from above | crowd r3 | Slow III + terrain | keep (impact plus drifts) |
| Blizzard | field, walking | crowd r3 | window held for 4 s | **advancing whiteout** |
| Absolute Zero | instant, conditional | one | counts the cold, scaled finisher | **n = 0..4** |
| Flash Freeze | instant, conditional | one | wet becomes ice | **wet-only hold** |

### B. Holds (Bubble, Flash Freeze, Freeze, Glacier, Black Ice, Rime Seal, Hoarfrost, Frostbite, Cryostasis for an ally)

| member | what it is *for* | how it is delivered | what ends it or what it costs |
|---|---|---|---|
| Freeze | the honest lock: unconditional, single, unbreakable | instant | only fire or earth (S1) |
| Bubble | the strained stun | instant, floats | holds small things longest (2.5, 2, 1.5 s by size) |
| Flash Freeze | the water payoff | instant, needs wet | rain 2 s, soaked 3 s; dry only slows |
| Glacier | the spreading lock | instant, 1 + up to 3 | shells crack for 2 each |
| Black Ice | the curse | instant, weakens | shatter cascades to survivors |
| Rime Seal | the trap | placed, 1 s to trigger | leaving early leaves Slow I |
| Hoarfrost | the delayed lock | 3 s creep | blooms outward when it closes |
| Frostbite | the creeping bite | 5 s ramp | final 1.5 s hold |
| Absolute Zero | the finisher | needs cold | scales with what is already there |
| Cryostasis | the ally's panic button | instant | bursts on release |

### C. Water (Bubble, Undertow, Tidecall, Tidewrit, Tidehook, Drowning Word, Current, Tidebreath, Icepath)

Water is frost's second identity and it has the cleanest reaction story (a SOAKED target: storm +50%, Flash Freeze holds it, fire -25%).
Give each rune one distinct role and one of two marks: **gatherers** leave PULLED as well as SOAKED (so Collapse and Implode work with wind and
void), the rest leave SOAKED only.

| rune | role | mark | distinct trick |
|---|---|---|---|
| Tidecall | gather | PULLED + SOAKED | bunching damage |
| Tidewrit | sweep a corridor | SOAKED | one sound, a wall silhouette |
| Tidehook | reel one in | SOAKED | arrival stagger, grounds flyers |
| Undertow | sink | SOAKED | pulls toward water |
| Drowning Word | silence and drown | SOAKED | cannot cast (S4) |
| Bubble | float and stun | SOAKED (on pop) | size-limited |
| Current | ride | none | carries allies |
| Tidebreath | douse and breathe | WET (self) | extinguishes |
| Icepath | bridge | none | a literal path |

Note the four hurting water runes (Tidecall, Undertow, Drowning Word, Tidewrit) run the *frost* world interaction (freeze the surface, snuff fires)
because `WorldRules.QUIET` only lists Bubble and Tidehook. Either add them to `QUIET` (a wave of water does not freeze the pond it is
washing) or keep it as a small bonus; I lean to adding Tidecall and Tidewrit.

### D. Guards: the wards each answer a different trigger

Frostward (a hold, 1 s cap), Frostbloom (a blow, heals), Cryostasis (a lethal blow, bursts), Mirrorfrost (a spell, returns 70%), Cushion (a fall,
becomes damage), Deflect (a projectile, returns it), Feather Fall (gravity, glides), Zephyr (fumes, clears them), Swift (a slow, shakes it off),
Tidebreath (fire, douses). Ten guards, ten different triggers, none of them a duplicate; that is the goal of the proposals.

### E. Wind strikes (Push, Windcut, Repel, Disarm; cross-element Pelt, Thunderclap)

| rune | job | distinct trick |
|---|---|---|
| Push | reposition, no damage | wall slam, mass (S2) |
| Windcut | the T1 poke | **interrupts** a charge, a draw, a fuse, a telegraph |
| Repel | the panic blast around you | dome, mass; armour-piercing stays |
| Disarm | remove the weapon | cooldown on a player's held item |

Cross-element: **Pelt** (earth, 4 damage and a knock for 5 mana) is Windcut's twin today, and **Thunderclap** (storm, 12 mana) is Repel's. I would
give Pelt a *stagger* (0.3 s hold, "stones ring the skull") and Thunderclap a *deafen* (screen-edge tinnitus, Silenced 1 s once S4 exists) so that each
element's strike is a different kind of hit.

### F. Vertical (Launch, Levitate, Updraft, Summit Wind, Downdraft, Skyglyph, Cushion, Feather Fall; cross-element Skyburst, Monolith, Tremor, Basalt Surge, Tusk Charge)

Tell the verticals apart by **what happens at the top**: wind carries and holds aloft (Levitate hangs, Summit Wind glides them out, Launch hands
them to the juggle mark, Updraft returns them), earth throws and crashes, fire carries and bursts. Two roles inside wind: *engage* (Updraft:
hurl and smash; Downdraft: swat) and *disengage* (Summit Wind: exile, Skyglyph: a pad away). Cushion and Feather Fall are the two ways to come
down (a shock or a glide).

### G. Whirls and fields (Cyclone, Dust Devil, Blizzard, Razorgale; cross-element Sandstorm, Inferno, Crimson Mist, Eclipse, Hush)

Split by **motion**: stationary (Sandstorm, Inferno, Crimson Mist, Eclipse, Hush), walking (Blizzard), hunting (Dust Devil), tossing as one (Cyclone),
sweeping twice (Razorgale). Every member then has a silhouette that also says how it moves.

### H. The "creep, then lock" triad (Hoarfrost, Frostbite, Fossilize)

Hoarfrost and Fossilize are numerically the same rune (Slowness I, II, III over 3 s, then 2 s held and 6 damage, 16 mana; only the moment the 6 lands and a player's hold, 2 s against 1 s, differ). Hoarfrost blooms;
Frostbite is a DoT with a short lock; **Fossilize** (earth, not in this audit) should diverge: a stone shell that lasts until 6 damage has been
dealt to it or 2 s pass, whichever comes first (a damage-gated lock is a different feel from a timed one).

### I. Mobility buffs (Swift, Leap, Dash, Gale Mantle, Current, Feather Fall, Summit Wind and Launch on Self)

Horizontal burst (Dash, precise), steer in the air (Gale Mantle), continuous speed (Swift, shakes off cold), spring (Leap), glide (Feather Fall),
water carry (Current, brings allies), climb (Summit Wind), rocket (Launch, the only one that hurts you).

### J. Pull to the caster (Tidehook; cross-element Vinelash, Pull, Grapple)

Tidehook reels to *your feet* and staggers; Vinelash (life) yanks 4 blocks and should bleed on the thorns; Pull (void) draws to the *spell's point*;
Grapple pulls *you*. Four different pulls once each has its own arrival.

## 4. Cross-element and caster-feel observations (for the lead to treat globally)

### 4.1 The same rune, once per element

| pattern | members across the mod | what to do |
|---|---|---|
| tier-1 four-damage poke | Windcut, Icicle, Pelt, Ember, Shock, Umbra, Harm | each one needs one verb (interrupt, finisher, stagger, ...) |
| tier-2 "everything within 3 blocks" | Repel, Coldsnap, Thunderclap, Flashfire, Moonpetal | strike kinds: displace, prime, deafen, burn, heal |
| hurl up | Launch, Updraft, Summit Wind, Skyburst, Monolith, Tremor, Basalt Surge, Tusk Charge | decide by the top of the arc (section 3, F) |
| pull into the middle | Tidecall, Gravity Well, Sinkhole, Singularity, Riftcall, Hellmouth, Magnetize | gather (mark PULLED), pin, crush, chew, drag |
| creep then lock | Hoarfrost = Fossilize, Frostbite | section 3, H |
| ally that answers a hit | Frostbloom, Geode, Bramble, Reflect, Riposte, Stoneform, Stormheart | one trigger each (section 3, D) |
| delayed single hit | Countdown, Stalactite, Primer, Hoarfrost | fine: different delays and payoffs |
| DoT of about 8 to 15 | Bleed, Blood Moss, Infest, Drowning Word, Frostbite, Soulfire, Entropy | fine: each has a rider |
| field of radius 3 to 4 for 4 to 6 s | Blizzard, Sandstorm, Inferno, Crimson Mist, Eclipse, Hush, Magma | section 3, G |
| hold 1 to 3 s | ten runes listed in section 3, B, plus Root, Decree, Jolt, Drowse | one hold per delivery style |

### 4.2 The caster's first 200 ms

What a caster sees and hears from a tap of R, in order: the spell's circle (`Vfx.castCircle`: a sigil of the spell's runes on the ground in the **first
effect's** element colour, six enchant motes, a glow in the hand, `circle_open` at 0.35 and the element's `cast_*` sound at 0.55), the **shape's**
pose (a thrust, open hands for Self, arms flung up for Zone and Burst...) and the shape's own delivery, then the rune's own effect. The
element and the shape carry the identity; the *effect* only arrives at the target, with a ~0.5 volume vanilla sting.

Consequences the lead should treat globally:

- `Self . Swift`, `Self . Leap`, `Self . Feather Fall` and `Self . Cushion` open identically (open hands, mint circle, `cast_wind`); only the
  feet-level effect differs. The same holds for every frost Self spell.
- `Bolt . Chill`, `Bolt . Frost` and `Bolt . Icicle` are the same bolt, the same landing (`Vfx.impact` and `impact_frost`), and differ only in the size of the burst that follows;
  `Bolt . Push`, `Bolt . Windcut` and `Bolt . Launch` likewise. For wind the effect layer is three crescents and a gust in each case.
- All charged casts (hold R) look the same until release, coloured by the first element; a release from 20% charge or more adds a release sound and a field-of-view kick, nothing else differs.
- There is no recoil: a Repel that throws twenty creatures twenty blocks gives the caster nothing, while a shape's pose is the only physical cue.
  A tiny `ScreenFx.kick` (0.15) for the strong displacement runes (Repel, Push, Launch, Dash) and a different hand flourish per verb would fix most of it.
- **Proposal (mod-wide):** a 0.15 s *cast tag* per rune family from the sound kit, played at 40% under the element's `cast_*` sound (a chime for
  buffs, a tick for strikes, a low click for locks), so that the sound of casting says what is being cast.

### 4.3 The hit and its feedback

Every landing plays the shape's impact (`Vfx.impact`: a flare, two rings, sparks and the element's `impact_*`), then the effect's own burst, then
a `touched` pop (a glow and three motes). For a tier-1 nudge that is three overlapping bursts; the shape impact should shrink when the effect
carries its own big signature (Freeze, Repel, Tidecall). Feedback for damage is a hurt sound, the pop and (for a heavy hit of 8 or more) a
`ScreenFx.punch`; there are no damage numbers; "Weak!" and "Resisted" callouts exist and reaction names appear once a second. What is
missing is a **per-verb sting** (strike, lock, shove, gather), which the kit in section 5 supplies, and **visible marks** (S3).

### 4.4 Sound reuse, counted

- Wind: `BREEZE_WIND_CHARGE_BURST` and `WIND_CHARGE_BURST` (28 uses across the mod) are the impact of Push, Repel, Prune, Cyclone (twice), Gale
  Mantle, Disarm, Summit Wind, Downdraft, Updraft, Skyglyph, Recoil (twice), Zephyr, Dust Devil (twice) and Blizzard: about **fourteen wind runes end in
  the same vanilla pop at a different pitch** (0.6 to 1.5).
- Frost: `GLASS_BREAK`/`GLASS_PLACE`/`POWDER_SNOW_*` (42 uses across the mod) end Frost, Chill, Icicle, Icepath, Coldsnap, Freeze, Glacier, Black Ice, Absolute
  Zero, Rime Seal, Flash Freeze, Hoarfrost, Frostbite, Avalanche and Mirrorfrost, **fifteen frost runes with one glass-and-powder family**.
- `AMETHYST_BLOCK_CHIME`/`RESONATE`: 94 uses across the mod as the generic "magic" note.
- The custom kit is per element only: `cast_<element>` (2 variants) and `impact_<element>` (3), all in D major pentatonic. There is no per-rune or
  per-verb sound. Section 5 lists 30 short cues that fit the same scale and the same synthesis code.

### 4.5 Palette

Frost (`0x8CDCFF`, white, `0x3A8CFF`) and wind (`0xC8F0DC`, white, `0x7FE0C0`) are both pale and both drawn additively, so overlapping light burns to
white; on snow, sky or a bright day I would expect both to wash out (I could not confirm in game). Blizzard, the fusion of the two, sits between them
(`0xDDF6FF`). Proposal: family accents, not new elements. Frost: **rime** (near white, frosted), **lock** (deep cyan `0x5AB4FF` to `0x2A5CFF`), **water**
(`0x4AA8FF`, already used). Wind: **gust** (mint, as now), **sky** (`0xBFE3FF`, for verticals), **heavy** (grey-green `0x9FB8B0`, for slams and
whirls), **dust** (sand `0xD8C08A`, exists). Repel's red goes to gust.

The signature language by family, so that a rune reads from its silhouette and its sound before its number:

| family | members | silhouette | motion | accent | sound family |
|---|---|---|---|---|---|
| frost, rime (creeping) | Chill, Frost, Hoarfrost, Frostbite | thin rings up the body, crystal spirals | rising, slow | near-white frost | `frost_crust`, `rime_creep` |
| frost, shatter (striking) | Icicle, Hail, Coldsnap, Avalanche, Absolute Zero | needles, hard cracks, crowns of spikes | falling, snapping | hard white and steel blue | `ice_needle`, `hail_patter`, `snow_whump`, `zero_hush` |
| frost, lock (holding) | Freeze, Glacier, Black Ice, Rime Seal, Cryostasis, Flash Freeze | shells, sigils, cocoons | closing in | deep cyan; violet (Black Ice), gold (Cryostasis) | `ice_lock`, `ice_break` |
| water | Bubble, Undertow, Tidecall, Tidewrit, Tidehook, Drowning Word, Current, Tidebreath | spheres, jets, lines, walls | rolling, dragging, sinking | saturated aqua | `water_surge`, `water_drag`, `bubble_*`, `hook_whip` |
| wind, displace | Push, Windcut, Repel, Disarm, Recoil | slabs, slash lines, domes | outward, straight | mint-white | `gust_thump`, `gust_slash`, `snatch_flutter`, `rewind_whip` |
| wind, vertical | Launch, Levitate, Updraft, Downdraft, Summit Wind, Skyglyph, Cushion, Feather Fall | columns, rising rings, falling crescents | up or down | sky white; grey-green for slams | `rise_whoosh`, `fall_crash`, `glyph_hum`, `feather_chime` |
| wind, whirl | Cyclone, Dust Devil, Blizzard, Razorgale | funnels, spiralling crescents | rotating | grey-green, sand, snow white | `whirl_loop`, `blizzard_bed`, `gust_slash` |
| wind, buff and ward | Swift, Leap, Dash, Gale Mantle, Deflect, Zephyr | streaks, rings at the feet, orbiting gusts | forward, orbiting | mint; warm gold for Zephyr | `dash_streak`, `feather_chime`, `deflect_ping` |

### 4.6 Reaction economics (why frost primers feel optional)

Shatter adds 60% of the payoff hit. Fire (5 dmg) gains 3.0, Flashfire 2.4 per target, Sunscorch 4.8, Meteor 6.0, Explode about 5.8. A plain strike
of the same price buys about 0.68 to 0.8 damage per mana, so a 4-mana Chill (2 s window) must be followed by a payoff of 5 or more to pay for
itself: **only the big payoffs reward a primer, and the primer worth using is Frost itself** (its strike costs what a strike costs, so the 4 s mark
is free). That leaves Chill (priming, cheap), Coldsnap, Hail and the rest to compete on their own numbers, which is why they read as filler.
Fixes in this document: a visible window (S3), Coldsnap as the *crowd* primer (one cast primes everyone for 4 s), and holds that the reaction
ends (S1) so the payoff has a cost as well.

Wind has the mirror problem: its only cash-in is Rupture (needs a bleeding target from blood), and its setup (WINDSWEPT) only serves fire. The
**Airborne** mark (S5) gives every vertical rune a payoff that any element can use, and makes Levitate and Launch the first half of a combo.

### 4.7 Missing verbs in the mod that these two elements can supply

- A **Silenced** state (no rune stops a caster today except Cryostasis): Drowning Word, Windcut's interrupt, and Thunderclap's deafen.
- A **juggle** state (Airborne): Launch, Levitate, Updraft, Cyclone.
- **Terrain that lasts**: Avalanche's drifts and Icepath's ice already do; Glacier's shells and Bubble's lift could too.

### 4.8 Timing: anticipation, impact, aftermath

| kind | today | proposed |
|---|---|---|
| instant strikes (Chill, Frost, Icicle, Windcut, Push, Repel, Launch, Dash) | no anticipation, a one-tick burst, almost no aftermath (Frost leaves a frost seal for a second) | fine at T1; from T2 add a 2-tick draw-in (rings converging) so the impact has a beat |
| T3 falling strikes (Avalanche, Hail) | land on the tick; Tidecall converges and crashes 6 ticks later (good) | a dark ring on the ground 6 to 8 ticks before landing: it reads as a threat and is counterplay in PvP |
| holds (Freeze, Glacier, Bubble, Flash Freeze) | the shell rises at once; Bubble inflates | a 3-tick snap ring before the shell; hairline cracks in the last half second (the tell that it is ending) |
| creeps (Hoarfrost, Frostbite, Drowning Word) | staged, good | a white flash 5 ticks before the closing freeze |
| fields (Blizzard, Cyclone, Dust Devil) | good; Cyclone's fling has no tell | the funnel leans toward the fling (proposed) |
| wards (Frostbloom, Deflect, Cushion, Frostward) | rings at cast, little afterwards (Deflect and Frostbloom have an aura) | Cushion and Frostward: a soft ring at the feet while they last, gone with the effect |
| aftermath | most instant runes leave nothing | wind: leaves and dust settling for a second; frost: a 2 s frost patch under the hit; water: a wet sheen (drips) on the target |

## 5. Sound needs (a shared kit)

Briefs use the vocabulary of `tools/sound_art.py` (`glass`, `sparkle`, `shatter`, `moving_band`, `thump`, `grains`, `tick`, `bead`, `bubbles`, `reverb`,
D major pentatonic). 30 cues, several serving many runes by a pitch or length offset (`ice_lock` six, `feather_chime` four, `gust_thump` five), most a
twenty-line recipe built from those helpers: about two and a half days to author, a day to wire them (replace the vanilla layers in each `*Vfx`,
keep the element's `cast_*`/`impact_*` underneath at a lower level). Scale volume by tier (T1 about 0.5, T3 about 0.9, T4 1.0). Several of these are useful to
every element (`ice_needle` as a "thin pierce", `snow_whump` as a "soft heavy landing", `gust_thump` as a "body blow of air", `zero_hush`).

| id | brief (character, length) | used by |
|---|---|---|
| `ice_needle` | a thin dry pierce: a 4 to 6 kHz noise tick (2 ms) into a falling glass partial (A6 gliding to E6, tau 0.08 s), no tail; 0.35 s; the melt is a soft drip (bead at 1.8 kHz) 2 s later | Icicle, a layer of Frost |
| `frost_crust` | a sheet freezing over: fine grains rising in rate (40 to 200 per second, 3 to 8 kHz) closed by one low glass tock (D4, tau 0.15 s); 0.25 s soft, 0.5 s full, 0.9 s long with a slow moving band under it | Chill (soft), Frost, Coldsnap, Flash Freeze (dry), Icepath (long) |
| `rime_creep` | crystals growing: three sparkle partials rising one scale step each (D5, E5, F#5), slow 0.15 s attack, a brown-noise bed; 0.6 s, each stage one step higher | Hoarfrost |
| `ice_lock` | a deep resonant lock: thump 150 to 60 Hz (0.25 s) under a long glass partial (D3, tau 0.7 s), a 20 ms shatter burst in front; 0.9 s. Offsets: -4 semitones Black Ice, +3 Rime Seal trigger, +5 Absolute Zero solid, a clock tick added for Cryostasis | Freeze, Glacier, Black Ice, Rime Seal, Absolute Zero, Cryostasis |
| `ice_break` | a fast shatter cluster (26 grains, 2.5 to 7 kHz, tau 30 to 140 ms) over a soft thump; 0.6 s. Dark (-5 semitones, void undertone) for Black Ice | Glacier and Cryostasis release, Black Ice shatter, Freeze ending |
| `snow_whump` | a soft heavy landing in powder: thump 110 to 50 Hz, low-passed noise 400 to 1500 Hz decaying 0.6 s, then a patter of 30 fine grains; 0.9 s (0.5 s light) | Avalanche, Blizzard opening, Coldsnap (light) |
| `blizzard_bed` | loop: band-limited noise 500 to 2500 Hz with a slow 0.7 Hz drift plus a faint 4 to 9 kHz grain layer (60 per second); 2 s seamless | Blizzard |
| `hail_patter` | four to six fast staccato ticks (2.6 to 3.4 kHz, 5 ms glass partials) 40 to 60 ms apart, rising slightly; 0.4 s | Hail |
| `flash_splat` | a wet splat (noise 600 to 2500 Hz, tau 30 ms) cut by a glass ping (E6) and 15 shatter grains; 0.3 s. Dry: a "psst" of frost (high-passed noise and a soft tick) | Flash Freeze |
| `zero_hush` | air leaves the room: high-passed noise fading to nothing over 0.2 s, then one glass note (D6, tau 0.4 s) in the silence; 0.6 s | Absolute Zero |
| `lotus_chime` | warm ice: two glass partials (D5, F#5) with a chorus and a soft breath swell; 0.8 s; a single high ping (A6) per ward proc | Frostbloom |
| `warm_ward` | a warm glass chord (D4, A4) with a brief brown-noise breath; 0.6 s | Frostward |
| `mirror_reform` | a shatter cluster played *in reverse*, converging into a glass ring (E5, A5); 0.5 s | Mirrorfrost |
| `water_surge` | a rising wave: moving band 300 to 1800 to 600 Hz, bubbling grains, a low swell thump; 0.9 s crash, 1.4 s single sweep, 0.5 s short | Tidecall (crash), Tidewrit (once), Current (short) |
| `water_drag` | a muffled gurgle: bubbles (rate 25, 300 to 900 Hz) under a descending glide 700 to 300 Hz, low-passed at 700 Hz; 0.8 s; a 0.3 s breath rasp for the pulse | Undertow, Drowning Word (a rasp each second) |
| `bubble_inflate`, `bubble_pop` | inflate: a sine glide 300 to 900 Hz with FM shimmer, 0.35 s; pop: a bead at 2.6 kHz plus a short splash, 0.15 s | Bubble |
| `hook_whip` | a line whip: a 3 to 6 kHz noise sweep (0.08 s) and a plip; each tug a taut twang (A4 partial) one scale step higher | Tidehook |
| `breath_bubbles` | a soft stream of rising bubbles (rate 25, 500 to 1400 Hz) under a two-note chime (A4, D5); 0.6 s | Tidebreath |
| `gust_slash` | an airy razor: noise above 2 kHz (0.09 s) with a fast pitch-down whistle (E6 to A5); double pass 0.25 s for Razorgale, short 0.12 s for Prune; a reversed 0.1 s puff for Windcut's interrupt | Windcut, Razorgale, Prune |
| `gust_thump` | a body blow of air: a low-passed thump 150 to 60 Hz and a 300 to 900 Hz puff; 0.35 s. Repel adds a 90 Hz sine shock, a wall slam adds a dry knock | Push, Repel, Cushion landing, Cyclone fling, wall slam |
| `rise_whoosh` | a moving band 250 to 2200 Hz over 0.5 s with a whistle partial, pitch scaled to the lift; Summit Wind darker with a horn-like partial (A3); Levitate soft | Launch, Levitate, Updraft, Summit Wind, Skyglyph launch |
| `fall_crash` | a falling whoosh (2200 to 300 Hz, 0.25 s) into a heavy ground thud and a soft air thwomp; 0.6 s | Downdraft, Updraft's smash |
| `dash_streak` | a Doppler streak: a band 800 to 3500 Hz in 0.2 s with a tiny onset pop; soft for Swift, short and higher for Gale Mantle's chain, a stop pop at the end for Dash | Dash, Gale Mantle, Swift |
| `feather_chime` | a soft wind chime: glass partials (D5, A5, E6, tau 0.4 s) over a 2 to 5 kHz breath swell; 0.7 s. Feather Fall high, Leap with an upward glide, Cushion low, Zephyr warm with a chorus | Feather Fall, Leap, Cushion, Zephyr |
| `whirl_loop` | a swirling loop: a moving band with 6 Hz eddy modulation (as `impact_wind`'s swirl) and a low rumble; 1.5 s; Dust Devil adds sand grains (60 per second, 2 to 6 kHz) | Cyclone, Dust Devil |
| `deflect_ping` | a bright ricochet: a glass partial at A6 with a fast pitch-down zip; 0.25 s | Deflect |
| `snatch_flutter` | a sharp snatch (a tick and a 1.5 kHz noise burst), then paper flutter (chopped noise, 0.25 s) | Disarm |
| `glyph_hum` | a low hum: sine D3 with a slow chorus shimmer, soft attack; 0.8 s | Skyglyph |
| `rewind_whip` | a reversed whoosh (0.4 s) into a single clock tick | Recoil |

## 6. Proposed implementation order, effort and risks

**Order** (lowest risk and most feel per hour first). Effort is one developer, rough.

| step | work | effort |
|---|---|---|
| 1 | **The fix list** (section 7): Frost text, Repel palette, Glacier shell, Tidewrit sound, Disarm death drop, Frostward text or hook | 0.5 d |
| 2 | **Number tunes**: Hail 5 stones, Repel 4 damage, Bubble cost 9, Freeze cost 14, Coldsnap (4 dmg, 4 s mark), Absolute Zero (18 mana, scaling), Recoil 5, Zephyr 10 s and clear air, Frostbite +0.5 s, Flash Freeze rain/soaked split | 1 d |
| 3 | **Sound kit**: author the 30 cues in `sound_art.py`, wire them into the `*Vfx` methods listed above | 3.5 d |
| 4 | **Frost mechanics** (each 1 to 3 h): Bubble size limit, Chill stacks, Icicle melt, Hail pummel, Glacier calving, Black Ice cascade, Hoarfrost bloom, Blizzard walks, Cryostasis burst, Frostbloom heal, Tidecall PULLED + bunching, Undertow toward water, Tidehook stagger, Current allies, Icepath path, Frostward hook, Mirrorfrost 70% | 3 d |
| 5 | **Wind mechanics** (each 1 to 3 h): Push wall slam, Windcut interrupt, Feather Featherglide, Cushion impact, Deflect return, Cyclone bowling, Downdraft whiff, Summit exile, Disarm cooldown, Gale Mantle steering, Dash braking, Levitate suspend | 3 d |
| 6 | **Systemic**: S5 Airborne (0.5 d), S4 Silenced + Drowning Word (0.5 d), S3 mark halos (0.5 d), S2 wind mass (0.25 d), S1 ice cracks end holds (0.25 d) | 2 d |
| 7 | **Presentation signatures**: family palette constants, needle, hail cloud, repel dome, splash crown, wall curtain, hand flourish and cast tag | 2 d |
| 8 | Tests, docs, wiki, lang, changelog | 1 d |

**Primitives.** Nothing new is needed on the client: every proposed look is built server-side from `ElementFx.ring`, `ray`, `slash`, `swirl`, `flatSigil`, `Motes.clouds` and `Light`
(the needle is a vertical `ray`, the hail cloud a flat ring of `Motes.clouds`, the Repel dome a stack of rings as `ExpansionVfx.havenOpen` builds one, the splash crown and the wall curtain
are rays). The one new piece is the mark-halo emitter (S3).

Total about 16 developer-days for everything; a useful cut of about 5 days is steps 1, 2, the Bubble change, and the sound kit for the ten most-used runes
(Frost, Chill, Icicle, Push, Windcut, Swift, Launch, Freeze, Bubble, Cyclone).

**Risks**

- **Balance.** Bubble loses a sixth of its hold on small mobs and half on big ones and costs 1 more (an early-game PvE control tool, players will notice); Flash Freeze's rain hold drops from 3 to 2 s;
  Absolute Zero can now reach 12 damage plus a 3 s hold on a well-prepared target (Freeze then Absolute Zero is 3 + 12 damage and two holds for 34 mana: check PvP
  with the 0.6 scale); Hail goes from 6 to 10 damage (0.38 to 0.62 per mana, now at the T3 median). The **Airborne** mark stacks multiplicatively with Hex (+25%),
  Cracked (+20%), Eclipse (+20%) and Rupture (+50%): put it inside `Reactions.hit` next to Cracked and consider capping total mark multipliers.
- **Passives.** `Passives.BUFFS` contains Swift, Leap, Feather Fall, Cushion, Tidebreath, Frostward, and `AURA` contains Push, Windcut, Icicle, Chill, Frost. Featherglide as a permanent
  passive would be a free glide, so take it out of `BUFFS` (or leave passive Feather Fall at plain Slow Falling); Swift as a permanent Speed III at 0.72 mana/s is the number to
  watch; Frostward's hold cap as a permanent 0.36 mana/s ward is strong against frost PvP (probably fine, it is a niche counter).
- **PvP and griefing.** Push's wall slam and Cushion's landing damage need `Targets.canHarm` and the PvP scale (they go through `Effects.hurt`, which does both); Windcut's
  interrupt and Drowning Word's silence are new PvP tools (cap at once per 8 s per target); Disarm's item cooldown must never touch a player in a duel that forbids it.
- **API and add-ons.** Rune costs and descriptions are read by add-ons and by the Cord screen: Absolute Zero's cost and every description change should go through `Runes.java`
  (the descriptions live there) and the wiki/lang regenerate.
- **Performance.** S3 adds one particle per half second per marked creature (bounded by the mark map; the map is swept); Blizzard's walking centre is one vector per tick.

**Tests to update** (from the grep in Appendix C): `WildercordFusedFrostTest` (Blizzard, Frostbloom, Black Ice, Rime Seal, Cryostasis, Frostbite, Absolute Zero),
`WildercordFusedStormTest` (Downdraft, Updraft, Skyglyph, Recoil), `WildercordSignatureFusionTest` (Hail, Razorgale, Dust Devil, Avalanche, Chill), `WildercordNewRunesTest`
(Tidecall, Hoarfrost), `WildercordNewRunes2Test` (Flash Freeze, Disarm, Swift), `WildercordFishingTest` (Tidebreath, Bubble, Tidehook, Current), `WildercordReactionsTest`
(Windcut Rupture, Chill and Push Unweave), `WildercordWorldMagicTest` (Icepath, Push), `WildercordFusedLifeTest` (Zephyr), unit tests `FusionTest`, `FusedFrostRulesTest`,
`FusedStormNumbersTest`, `SignatureRulesTest`, `HeartAndPassivesTest`. Runes with **no dedicated assertion** today (only the screenshot and feature-tour runs touch them):
Feather Fall, Launch, Dash, Levitate, Leap, Repel, Frostward, Cushion, Deflect, Prune, Icicle, Coldsnap, Cyclone, Gale Mantle, Glacier, Freeze, Undertow, Drowning Word,
Tidewrit, Summit Wind (unit only): each proposal above should ship with one.

**Docs and wiki that describe these runes**: `docs/DESIGN.md` (Frost row at line 182, Flash Freeze at 426, the reactions table and "Element visual languages"),
`docs/features/new-runes.md` (Hoarfrost, Flash Freeze, Windcut, Icicle, Coldsnap, Cyclone...), `docs/features/fusion-altar.md` and `docs/RECIPES.md` (Rime Seal, Absolute Zero,
Blizzard...), `docs/features/world-magic.md` (frost and wind interactions), `CHANGELOG.md`; the player wiki pages `wiki/runes/effects/frost.md`, `wind.md`, `fused.md`,
`innate.md`, `world.md` are generated by `python tools/wiki.py` from the game's data (republish from `main` to `gh-pages` as usual).

## 7. Things that are simply broken (fix regardless)

Confirmed in code; none needs a design decision.

| # | what | evidence | fix |
|---|---|---|---|
| 1 | **Frost says "freezes solid" and doesn't hold.** It sets frozen skin, Slowness III and the FROZEN mark, never a hold. Same wording in `docs/DESIGN.md:182` and the wiki. | `Effects.java` `case "frost"` (no `Spirits.freeze`/`hold`) | reword: "5 freeze damage, Slowness III for 4 seconds and frost that leaves it brittle for 4 seconds" |
| 2 | **Shatter and Fracture do not end a hold.** They say "the ice bursts" / "it thaws", but a Freeze, Glacier or Black Ice hold runs on to its timer (the mark and frozen skin are cleared, the AI stays off). | `Reactions.fire`, `Reactions.fracture`: no `Spirits.thawNow` | S1, or reword the reaction text |
| 3 | **Repel is red.** The only wind rune off its element's palette (`0xFF5050`, `0xFFB0A0`). | `TechniqueVfx.REPEL` | use `ElementFx.WIND` |
| 4 | **Glacier has no ice shell**, while Freeze and Flash Freeze (T2) do. The fused T3 looks smaller than what it upgrades. | `FusionVfx.glacier` never calls `BlockFx.encase` | encase, as `Vfx.freeze` does |
| 5 | **Tidewrit plays its riptide sound once per step**, up to 15 times in 15 ticks. | `ExplorerVfx.tidewrit` (called per step) | sound once at the start and once at the end |
| 6 | **A Disarmed mob that dies within 5 s loses its weapon and its drop chance** (the held item is removed and never dropped or returned). | `CraftedRunes.forget` on `AFTER_DEATH` removes the entry; `rearm` only runs for the living | put the item back in `ALLOW_DEATH` (before the loot is rolled), or drop it there |
| 7 | **Frostward "can't freeze" but holds (Freeze, Glacier, Black Ice, Rime Seal, Absolute Zero) and slows still land**; it only stops frozen skin and the FROZEN mark. | `Effects.frostward` | the hold cap (proposal) or reword |
| 8 | **Water runes do frost world magic** (Tidecall, Undertow, Drowning Word, Tidewrit freeze the surface they wash; only Bubble and Tidehook are in `WorldRules.QUIET`). Minor. | `WorldRules.QUIET` | add Tidecall and Tidewrit |
| 9 | **Hoarfrost is not deduplicated**, unlike Frostbite, Fossilize and Drowning Word (each "starts over" on the same caster and target). A Zone, Linger, Volley or Echo stacks a full creep per pulse: `Zone . Hoarfrost` lands 6 freezes of 6 damage for 40 mana (0.9/mana against 0.38 for one). Bubble's pop stacks the same way. | `ExplorerEffects.hoarfrost` (no `claim`/`current`), `Techniques.bubble` | one token per (caster, target) as `FusedFrost.frostbite` does |
| 10 | **Tidewrit sweeps ~11 blocks, the text says 8; Updraft is described as ~7 blocks high, ~8 by the drag constants.** Text drift, not a bug. | physics estimate, Appendix B | reword or retune |

Not broken but worth a warning line in the rune text: **Self . Launch** ends in about 9 fall damage unless you slow-fall (the design doc's own example, `Self . Launch . On Land . Burst . Lightning`,
lands you for it); **Self . Levitate** ends in about 2.6; Bubble and Freeze on a boss also apply Weakness V (`Spirits.hold`), which should neuter its melee as well as slow it: worth a line in the text.

## Appendix A. The arithmetic

Everything at power 1, duration 1; damage counts hits and DoT ticks; hold = seconds the target cannot act; slow-seconds = speed cut x seconds (Slowness n = 15% x n). The peer tables were computed with a throwaway script (not committed) from the numbers as the code reads them.

**A.1 Single-target damage per mana (damage incl. DoT / rune cost)**

- T1 (median **0.80**): harm 7/8 = 0.88, ember 6/6 = 1.00 (3 + 3 s of burning), shock 4/7 = 0.57, pelt 4/5 = 0.80, umbra 4/6 = 0.67, leech 3/8 = 0.38, countdown 6/7 = 0.86.
- T2 (median **0.68**, n = 16): fire 11/8 = 1.38, aftershock 10/9 = 1.11, stalactite 7/9 = 0.78, fangs 6/10 = 0.60, bleed 10/8 = 1.25, dismantle 9/10 = 0.90, venom 12/8 = 1.50,
  vinelash 5/9 = 0.56, ripple 6/10 = 0.60, jolt 4/9 = 0.44, spellbrand 6/8 = 0.75, infest 8/9 = 0.89, blood moss 6/10 = 0.60, cinderbrand 3/9 = 0.33, gash 3/9 = 0.33, manaburn 5/10 = 0.50.
- T3 (median **0.60**, n = 13): smite 0.62, lightning 0.60, lifesteal 0.31, plasma 0.45, sunscorch 0.81, starshard 0.56, cleave 0.36, bloodboil 0.81, soulfire 0.94, blackflame 1.00, entropy 0.47,
  resonant shriek 0.67, hemomancy 0.44.

**A.2 Area damage per target per mana**

- T2 (median **0.42**): flashfire 7/11 = 0.64, thunderclap 5/12 = 0.42, moonpetal 4/12 = 0.33, rootsnare 3/11 = 0.27, sporebloom 4.8/10 = 0.48.
- T3 (median **0.50**, n = 14): explode 9.6/18 = 0.53, tremor 0.44, meteor 0.42, inferno 0.60, primer 0.56, firestorm 0.56, sandstorm 0.44, bonespur 0.50, magma 0.50, riftcall 0.67, eclipse 0.56,
  crimson mist 0.28, sinkhole 0.28, singularity 0.27.
- T4 (median **0.64**): cometfall 16/32 = 0.50, dragon breath 25/30 = 0.83, hollow 28/36 = 0.78, sonic boom 16/35 = 0.46, wither 16/25 = 0.64.

**A.3 Holds per mana** (peers): decree 2/10 = 0.20, jolt 1/9 = 0.11, rootsnare 2/11 = 0.18, drowse 6/14 = 0.43, stasis 5/34 = 0.15, sinkhole 2/18 = 0.11, heartstopper 1.5/18 = 0.08,
fossilize 2/16 = 0.125, shackle 5/9 = 0.56. Median 0.147; without Drowse and Shackle **0.125**. Against the tier-II peers alone (decree, jolt, rootsnare, shackle; median 0.19) Bubble is
2.0x and a wet Flash Freeze 1.75x: still OVER, so the call does not depend on the peer set.

**A.4 The frost and wind runes against them**

| rune | number | per mana | vs median |
|---|---|---|---|
| Chill dmg | 1/4 | 0.25 | 0.31x T1 |
| Icicle | 4/6 (6/6 slowed) | 0.67 (1.00) | 0.83x (1.25x) |
| Windcut | 4/6 | 0.67 | 0.83x T1 |
| Frost | 5/8 | 0.62 | 0.93x T2 |
| Flash Freeze dmg | 4/9 | 0.44 | 0.66x T2 |
| Tidehook | 4/9 | 0.44 | 0.66x T2 |
| Bubble dmg | 4/8 | 0.50 | 0.74x T2 |
| Hail | 6/16 | 0.38 | 0.62x T3 |
| Hoarfrost | 6/16 | 0.38 | 0.62x T3 |
| Frostbite | 8/16 | 0.50 | 0.83x T3 |
| Drowning Word | 10/16 | 0.62 | 1.04x T3 |
| Absolute Zero (cold target) | 7/20 | 0.35 | 0.58x T3 |
| Coldsnap | 3/11 per target | 0.27 | 0.65x T2 area |
| Repel | 5/9 | 0.56 | **1.33x** T2 area |
| Cyclone | 3/10 | 0.30 | 0.72x |
| Blizzard | 4/18 | 0.22 | 0.44x T3 area |
| Avalanche | 6 (9 bare)/18 | 0.33 (0.50) | 0.67x (1.0x) |
| Tidecall | 6/16 | 0.38 | 0.75x |
| Razorgale | (2 + 2 x 1.5 + 4)/18 = 9/18 | 0.50 | 1.0x |
| Summit Wind | 5/14 | 0.36 | 0.71x |
| Updraft | 4/16 | 0.25 | 0.50x |
| Downdraft | 4 to 10 / 14 | 0.29 to 0.71 | 0.57x to 1.43x |
| Dust Devil | 15/26 | 0.58 | 0.90x T4 |
| Tidewrit | 10/30 | 0.33 | 0.52x T4 |
| **Bubble hold** | 3 s / 8 | **0.375** | **3.0x** median, 2.4x Freeze |
| **Flash Freeze hold (wet)** | 3 s / 9 | **0.333** | **2.7x**, 2.1x Freeze |
| Cyclone hold per target | 2 s / 10 | 0.20 | 1.6x |
| Freeze hold | 2.5 s / 16 | 0.156 | 1.25x |
| Glacier / Hoarfrost hold | 2 s / 16 | 0.125 | 1.0x |
| Absolute Zero hold | 2 s / 20 | 0.10 | 0.80x |
| Black Ice / Rime Seal hold | 1.5 s / 16 | 0.094 | 0.75x |
| Frostbite hold | 1 s / 16 | 0.062 | 0.50x |

Slow-seconds per mana: Chill 1.8/4 = **0.45**, Frost 1.8/8 = 0.225, Undertow 1.35/9 = 0.15, Coldsnap 1.2/11 = 0.11, Flash Freeze dry 0.9/9 = 0.10, Absolute Zero 1.8/20 = 0.09, Hail 1.2/16 = 0.075,
Avalanche 1.35/18 = 0.075, Blizzard (per enemy) 1.35/18 = 0.075, Hoarfrost 0.9/16 = 0.056; earth peers root 3/9 = 0.33, mire 3/8 = 0.375, weigh 3/8 = 0.375.

Dominance: fall protection Cushion 30 s / 5 = 6.0 against Feather Fall 12 s / 6 = 2.0 (3x). Frostbloom heals Regeneration II 1 hp per 25 ticks = 0.8 hp/s, 5 s = 4 hp / 14 = 0.29 hp/mana against Heal 8/12 = 0.67 and
Regrowth 6.4/10 = 0.64 (0.45x). Zephyr 14 mana for Speed I + Jump I + Regen I 6 s against `Burst . Swift . Leap` = 6 + (6 + 4) x 1.5 = 21 mana for Speed III + Jump III 10 to 15 s to the same crowd.
Hail against Frost: 6 dmg, Slow II 4 s, 2 s window at 16 against 5 dmg, Slow III 4 s, 4 s window at 8: damage per mana 0.38 against 0.62 (0.6x), slow-s per mana 0.075 against 0.225 (0.33x).
Cryostasis against Brace: Brace is 4 mana for 80% less damage for 2 s (0.22x the price); Cryostasis' edge is full immunity, a heal of 6 and use on allies.

**A.5 Reaction combos that shape the picture**: Rend (5) then Windcut (6) = 11 mana for 10 damage and a 2 hp heal (Rupture: 4 x 1.5 + 4). Chill (4) then Fire: 1 + 5 x 1.6 + burn 6 = 15 for 12 mana
against Fire alone 11 for 8: a primer that is worth it only for a big payoff (section 4.6).

## Appendix B. Movement and knockback (estimated)

Vanilla drag constants (air 0.91, ground 0.546, gravity 0.08 with 0.98 vertical drag), player-like, no armour or knockback resistance, from the impulses in the code. **Estimates, not measured in game;
expect 30% either way.** Push 2.2 (+0.45 up): ~17 blocks. Repel 2.4 (+0.5 up): ~20 at the centre, ~12 at the edge. Launch 1.5 up: apex ~12 blocks, 1.8 s (Self also 0.7 forward: ~7.5 blocks).
Dash Self 2.6: ~18 blocks on a flat look, up to ~24 looking up. Gale Mantle dash (1.25 + 0.42 up): ~10 blocks each. Skyglyph ally (0.9 fwd, 1.2 up): apex ~8, ~9 forward. Updraft 1.2 up: apex ~8 (the code
says ~7). Summit Wind on an enemy (0.9 + 1.1 up): ~7 up, ~9 away; Self (1.55 up): apex ~13. Cyclone fling (1.4 + 0.5 up): ~12. Cushion gust (1.1 + 0.35 up): ~8. Thunderclap 2.0: ~17. Tidewrit sweep 1.6: ~11.7
(text says 8). Levitation II for 3 s: rises ~5.6 blocks, then falls ~5.6 for ~2.6 damage; Levitation I rises ~2.8.

## Appendix C. Where each rune is tested

`WildercordScreenshots`, `WildercordFeatureTour` and `WildercordShowcase` excluded (they show, not assert).

| rune | tests |
|---|---|
| swift | LoadoutRulesTest, HeartAndPassivesTest, WildercordCordTest, WildercordLoadoutsTest, WildercordNewRunes2Test |
| push | LoadoutRulesTest, WildercordCordTest, WildercordFusionTest, WildercordLoadoutsTest, WildercordReactionsTest, WildercordWorldMagicTest |
| frost | many (a base rune: reactions, affinities, fusion, world magic, gear) |
| chill | WildercordReactionsTest, WildercordSignatureFusionTest |
| tidebreath, bubble, tidehook, current | WildercordFishingTest |
| icepath | WildercordWorldMagicTest |
| windcut | WildercordReactionsTest |
| mirrorfrost | AdvancementTreeTest |
| hail, razorgale, avalanche | WildercordSignatureFusionTest |
| blizzard, frostbloom, black_ice, rime_seal, cryostasis | WildercordFusedFrostTest |
| frostbite, absolute_zero | WildercordFusedFrostTest (+ FusionTest, ApiRegistrationTest) |
| downdraft | FusionTest, WildercordFusedStormTest |
| updraft, skyglyph, recoil | WildercordFusedStormTest |
| zephyr | WildercordFusedLifeTest |
| dust_devil | FusionTest, WildercordSignatureFusionTest |
| tidecall, hoarfrost | WildercordNewRunesTest |
| flash_freeze, disarm | WildercordNewRunes2Test |
| summit_wind | FusionTest |
| none dedicated | feather_fall, launch, dash, levitate, freeze, leap, repel, frostward, cushion, deflect, prune, icicle, coldsnap, cyclone, gale_mantle, glacier, undertow, drowning_word, tidewrit |

## Appendix D. What I could not verify

- **I did not run the game.** Every presentation judgment is from reading the particle and sound calls; the supplied `fx_shots` frames I looked at (Cyclone, Bubble, Tidecall, Tidewrit) show the caster's circle and, for Tidewrit, a pale crest ray with blue spray;
  the other three caught no visible effect, and `docs/images` has nothing for wind, so palette washout, loudness and the felt scale of each burst are inferred, not seen or heard.
- **Physics** (Appendix B) is from vanilla constants, not measured; creatures with their own friction or knockback quirks will differ.
- **Armour interaction**: I treated freeze and magic damage as armour-piercing as in vanilla's `bypasses_armor` data tag; I did not re-read the 26.3 tag files.
- **Drowning damage on water-breathers** (Drowned, Guardians, Axolotls): not checked whether vanilla's `drown` type does anything to them; Drowning Word's real value against them is unknown.
- **Mirrorfrost against mob casters** (Runebound): `spellHit` records only spells that hit a player and that carry a root plan; I did not trace whether a Runebound's cast does.
- **Peer sets** for the medians are my choice (listed in Appendix A); a different set moves a ratio by roughly 10% to 20%, not enough to change an UNDER or OVER call except the borderline ones
  (Repel at 1.33x is the nearest to a threshold).
- The mod's other groups may already be planning changes that overlap (Pelt, Thunderclap, Fossilize, Vinelash, Skyburst): they are named here only as cross-element notes.

