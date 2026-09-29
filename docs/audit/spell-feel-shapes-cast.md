# Spell feel audit: shapes, modifiers, links and the casting experience

Branch `spell-audit-shapes-cast` · phase 1 (audit and proposal; no game code, assets, lang or tests were changed) ·
scope: the 39 shapes, 24 modifiers, 17 links, and the casting pipeline every spell shares.

## 0. Summary

The request: *every spell should have a unique feel, unique property, unique power and display, so casting doesn't
feel the same every time.* For the layer I audited (everything that is not an effect rune) the honest answer is:

1. **Shapes are already the best-differentiated layer, but only as "shape skeleton x element palette".** Each of the 39
   shapes has its own runner, its own light pattern and mostly its own launch sound (`ShapeRunners`, `ExpansionVfx`,
   `TechniqueVfx`, `CraftedVfx`, `ExplorerVfx`). They still collapse into seven skeletons: a line of light (8 shapes:
   touch, beam, ray, lance, prism, stream, latch, sweep); a glowing orb in flight (8: bolt, arc, spark, wisp, comet,
   ricochet, cluster, orb); a slash arc (6: cone, crescent, glaive, barrage, wave, blitz); a ground seal or field that
   lingers (9: zone, totem, wall, vortex, domain, trail, mine, snare, imprint); a radial shell of rings (4: burst, nova,
   ring, pillar); a caster-anchored aura (2: self, orbit); and a call from above (2: rain, constellation). An element only
   swaps two colours, a mote, a spark and two sounds (`Vfx.Theme`, Vfx.java:37).
2. **Nothing else about the spell reaches the display.** No visual or audio class (`Vfx`, `ElementFx`,
   `ExpansionVfx`, `TechniqueVfx`, `CraftedVfx`, `ExplorerVfx`, `ReactionVfx`) has a method that takes a `Cast`, a power, a
   charge fraction, a mana cost or a tier (checked by grep: none). The effect's role (strike, bind, mend...) does not choose the
   impact either: a Heal bolt lands with the same flare, ring and `impact_<element>` sound as an attack (`RuneBolt.hitEntity`
   calls `Vfx.impact` before any effect runs). Scale of display follows radius only.
3. **Charge and release are identical for every spell.** One hum loop (0.8 to 1.3 pitch), one hold pose, one circle
   animation at one size (0.42 blocks), one `circle_open`, one `charge_full`, one `release`. A tap cast (the usual way to
   cast) plays none of the release cues at all. 20 of 39 shapes share the default "thrust" pose, and the pose is picked
   from the spell's *first rune*, not its first shape.
4. **The sound layer is thin and unmixed.** `Fx.sound` is called 855 times; 681 (80%) use a vanilla event at a pitch and volume
   written into the call site; the code references 202 distinct vanilla events, `AMETHYST_BLOCK_CHIME`/`RESONATE` alone 94 times.
   The mod's own casting sounds are 2 casts + 3 impacts per element and 15 casting utilities. The element cast sound is played
   *twice* on 12 shapes (once by `Vfx.castCircle`, again by the shape), and there is no per-tick voice limit: a Burst that
   hits 30 mobs plays 30 identical effect sounds at the same tick.
5. **Power sits in two separate economies.** Instant single-landing shapes deliver 0.4 to 0.6 damage per mana
   (Harm reference, Bolt = 0.59); every repeating shape delivers 0.9 to 2.6 (1.6 to 4.4x Bolt), because a shape's cost
   multiplier ignores how many times it lands. Three modifiers then multiply landings again for almost nothing: **Quicken**
   (x2 to x4 landings on Zone/Wall/Totem/Domain/Latch/Stream for x1.2 to x1.44 cost), **Linger** (3 landings for x1.8, on the
   same creatures wherever they run) and **Extend**. `Zone . Harm . Linger . Quicken . Quicken` deals 504 damage to one
   creature for 53 mana (9.5 dmg/mana, 16x Bolt).
6. **Dominant modifiers/links:** Vow, Blood Price (with Heal: 3.3x mana laundering), Belated, Linger, Quicken (on repeaters),
   Split+Volley (9 bolts for x5.76), On Hit/On Kill on multi-landing shapes (payload paid once, fired up to 8x per landing).
   **Weak:** Amplify, Overcharge, Kindled, Combo (pays its finisher on all three casts to fire it on one), Rain, Orb, Cluster.
   **Near clones:** Ring/Burst, Sweep/Cone, Arc/Bolt, Ray/Beam, Snare/Mine, Wall/Zone.
7. **Modifiers and links barely announce themselves.** 8 of 24 modifiers have no visible or audible presence at all
   (Amplify, Frugal, Overcharge, Rapid, Vow, Trial Key, Pierce, Homing); Linger looks like a second cast; Focus, Extend, Quicken and Execute
   show only as side effects of their number. 5 of 17 links are silent when they fire (Delay, On Hit, Echo, Pulse, If Sneaking), and
   nothing shows that a reactive link (On Hurt, On Low Health, On Land) is armed.
8. **Proposal in one paragraph (section 3.3 and 4):** compute one small immutable `Feel` per group
   (`motion` from the shape x `element` from the mana-dominant effect x `role` from the effect's category x `band` from cast
   cost/charge/tier x modifier flags), carry it *inside `Vfx.Theme`* so the shape methods that already receive a `Theme` (28 direct `theme.cast`/`theme.impact` plays, 9 `Vfx.impact` calls) gain it with no
   signature changes, and give it five layers (cue, travel, impact, aftermath, feedback) that each dispatch on motion, role,
   element skin or band. Add a sound kit of about 126 new events (10 new verbs per element on top of today's cast and impact,
   13 notes for a "spell melody" that plays the runes as a phrase while charging, and 13 neutral events for the
   gold/violet family tells, the HUD and failures), authored with the existing procedural pipeline (6.5 s to synthesise
   the whole current set), split into per-element files and a generated manifest so several people can add sounds in
   parallel. Estimated effort: 8 to 10 developer-days of core code plus 3 to 5 days of parallel sound authoring and the per-effect work.

## 1. Method and baselines

**Read, not guessed.** I read the rules (`Runes`, `SpellNumbers`, `SpellCompiler`, `SpellPlan`, `SpellSigil`), the runtime
(`CastEngine`, `ShapeRunners`, `CraftedShapes`, `ExplorerShapes`, `RuneBolt`, `Cast`, `Charging`, `SpellCaster`, `Effects.hurt`
and `Effects.apply`, `Rhythm`, `Gear.flourish`), the presentation (`Vfx`, `Fx`, `ElementFx`, `Sigils`, `Light`, `ScreenFx`, and the
shape methods of `ExpansionVfx`, `TechniqueVfx`, `CraftedVfx`, `ExplorerVfx`), the client (`ChargeCircles`, `ChargeHum`, `BoltComets`,
`AimPreview`, `SpellHud`, `ScreenEffects`, `PlayerModelMixin`, `AvatarRendererMixin`, `CameraMixin`), the sound pipeline
(`tools/sound_art.py`, `sounds.json`, `WildercordSounds`) and `docs/DESIGN.md`, `ARCHITECTURE.md`, `ART.md`, `ADDING_RUNES.md`.

**Numbers.** I re-implemented the cost formula (`SpellCompiler.groupCost/partCost`) and checked it against the exact
assertions in `SpellCompilerTest` (e.g. `(3 + 8*1.1) * 2.4`, `bolt + 2 + bolt`, `2 + 3*(...)`). The script is committed beside this report
(`docs/audit/shapes-cast-numbers.py`, run it with the path of a `runes.tsv` dump) so the numbers can be redone after any rebalance; nothing was run in the game.

- **Reference effect: Harm** (arcane, 8 mana, 7 damage at power 1, has the Power and Linger traits). `Shape . Harm` costs
  `shape.cost + 8 x shape.multiplier`. **dpm** below means *Harm damage per mana per stationary target over one whole cast*
  (Bolt . Harm = 7 / 11.8 = **0.59**, written 1.00x). Landings per target are counted from the runners
  (Zone 6, Wall 6, Totem 6, Domain 7, Vortex 5, Orbit up to 8, Trail up to 8 realistic (16 theoretical), Barrage 8 x 0.35,
  Stream 6 x 0.35, Latch 4 x 0.7, Glaive 2, Orb about 3, Rain about 1.1 on a centred target).
- Cooldown is one tick per mana (0.5 to 20 s) and mana regenerates 5 to 8 a second, so **regeneration, not cooldown, limits
  sustained fire** (a 24-mana spell has a 1.2 s cooldown but 3 to 5 s of regeneration behind it), and any lingering shape
  outlasts its own cooldown (Zone lasts 6 s, cooldown 1.2 s), so lingering shapes stack.
- Roster (from `runes.tsv`): shapes 39 (T1 8, T2 22, T3 8, T4 1), modifiers 24 (T1 3, T2 13, T3 8), links 17 (T1 1, T2 10, T3 6),
  effects 248 (harmful 149, helpful 66, world 21, movement 12; 173 carry the Power trait, 149 Duration, 71 Radius, 64 Linger, 44 Share).
  Median harmful effect costs 16 mana.

**Looked at.** `docs/images/*.jpg` (beam, burst, charge, crescent, nova, pillar, rain, prism...), the scratchpad contact
sheets (`new_shapes.png`: spark, ray, nova, wisp, comet, ricochet, cluster, lance, sweep in action; `casting.png`: spectrograms of
the casting sounds) and a few `fx_shots`. Most of those `fx_shots` frames caught the wrong instant (a caster's back and a crowd
of zombies), so I did not rely on them.

**Could not verify.** I heard nothing and saw nothing move: every claim about *sound* comes from the code and the synthesis
script, every claim about *motion* from the runners and particle calls. In particular: how loud a stack of identical sounds
really is in Minecraft's mixer; whether Sweep's ray gaps (section 6) show up against real mob hitboxes; Trail's realistic hit
count; and any profiling of packet counts (I count particle calls, not packets). Items that rest on reasoning alone are marked
*(unverified)*.

**Feel vocabulary used below.**

| Term | Meaning |
|---|---|
| **motion** | the shape's gesture family: FLICK, HURL, BEAM, SLASH, BLAST, SEAL, CALL, AURA (section 3.3) |
| **role** | what the effect does to the target, taken from `RuneCategories`: damage (strike), control (bind), support (mend), movement, time, world, summon |
| **skin** | the element's look and timbre (palette, motif particles, materials of its sounds) |
| **band** | scale class S / M / L / XL from mana cost, charge and tier |
| **tell** | a cue that a modifier or link is present: gold for modifiers, violet for links, using the family colours the UI already uses (`RuneColors`) |

## 2. Verdict tables (all 80 runes)

Columns: **id · tier · cost (base x multiplier) · role (the rune's Codex category) · key numbers · power verdict · verb ·
overlap · presentation today · presentation verdict · proposal.** *dpm* = Harm damage per mana per stationary target over one
whole cast; *xB* = relative to Bolt (0.59). Verdict words: FAIR, WEAK (underwhelming), OVER (overdone), DOMINANT,
DOMINATED. Presentation verdicts: INVISIBLE, SILENT, PARTLY, GOOD, OVERDONE. "Hit.power" means a new per-hit multiplier field on
`Cast.Hit` (section 3.3); "the skin" is the element skin of section 3.3.

### 2.1 Shapes (39)

| id | T | cost | role | key numbers | power verdict | verb | overlap | presentation today | pres. verdict | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| self | 1 | 0 x1.0 | personal | you only; Harm ref 8 mana | FAIR (free; implicit shape of every effect-first spell) | Turn it on yourself | none | 3 rings climb + helix + 8 motes (`Vfx.self`); no sound of its own; open-hands pose | OK but only recoloured per element | Element aura skin from the `Gear.flourish` primitives (fire embers rise, frost rime creeps, life leaf spiral); small `swell` sound scaled by band |
| touch | 1 | 1 x1.0 | direct | reach = entity reach (about 3 to 4.5); chain; 9 mana, 0.78 dpm (1.31xB) | FAIR; out-ranged by Ray (2 mana, 10 blocks); survives on price, block targeting and melee | Lay a hand on it | Ray, Beam | the full Beam show: 0.16 ray, hand circle, travelling rings, `BEAM_FIRE` 0.8, generic impact (`CastEngine.touch`) | OVERDONE for a poke | Own cue: palm glow, small contact flash, soft `snap` (no `BEAM_FIRE`). Mechanic: +30% power via Hit.power ("laying on hands") so it beats Ray only at melee range |
| bolt | 1 | 3 x1.1 | projectile | 48 blocks at 1.6 b/tick; pierce, bounce, homing, chain, split, volley; 11.8 mana, 0.59 dpm (1.00xB) | BASELINE | Throw one bright thing far | Arc, Spark, Wisp, Comet, Ricochet | `RuneBolt` entity drawn client-side as a comet (white core in a glow, 7-tick tapering trail, colours synced); launch = element cast sound 0.5 (doubled with `castCircle`'s); impact = `Vfx.impact` | GOOD silhouette, smooth; every bolt identical whatever its modifiers | Sync a style int with the entity: Quicken = long streak, Pierce = needle core + through-rings, Homing = corkscrew tail, Amplify/Overcharge = fatter core + second halo, Frugal = thin; volley shots climb the scale; remove the duplicate launch sound |
| beam | 2 | 4 x1.2 | direct | 24 blocks instant; pierce, chain, split, volley; 13.6 mana, 0.51 dpm (0.87xB) | WEAK: DOMINATED by Ray inside 10 blocks (Ray 0.70) | Draw a line of light | Ray, Lance, Touch | 0.16 ray + halo circle at the hand + rings racing down + end flare + custom `BEAM_FIRE` 0.8 | STRONG; best-sounding shape and the template the other line shapes copy | Keep the show, lengthen the tail with distance. Mechanic: +2% power per block beyond 8 up to +30% ("sniper"); say in the tooltip it cannot be parried (only Bolt, Arc, Spark, Wisp, Comet and Cluster can be). Cost 4 to 3 |
| burst | 2 | 6 x1.5 | area | r4 sphere from you (or the trigger), once; 50 m2; 18 mana, 0.39 dpm (0.66xB) | WEAK vs Ring: same price, Ring covers 3.1x the area | Explode outward | Ring, Nova, Comet | flare + two crossed rings + ground ring + about 32 motes/sparks + `BREEZE_WIND_CHARGE_BURST` 1.3 + impact sound + camera shake (0.12 x r) | STRONG for its price; the blast sound is a vanilla one at fixed pitch for every element | Be the heavy one: scale volume, pitch and shake by band, dust ring on L+, an element `burst` sound. Mechanic: a small upward pop (0.3) so it lifts and hits fliers, which also separates it from flat Ring |
| zone | 3 | 8 x2.0 | lingering | r3 at aim, 6 pulses in 6 s; 28 m2; 24 mana, 42 dmg per target, 1.75 dpm (2.95xB) | OVER: landings cost nothing; Quicken, Linger, Extend stack it (2.4) | Claim ground | Totem, Wall, Vortex, Imprint | ornate spell circle for its whole life (radius = zone radius), flare, per pulse a ring + 6 enchant motes + r x 4 mote packets; `circle_open` at start (doubled); no per-pulse sound | GOOD: scale readable from the circle; pulses silent | A soft `pulse` per landing (3 variants, ducked), `swell` at open, `settle` at the end; price by landings (2.5) |
| rain | 3 | 10 x2.5 | area | 5 strikes r1.6 inside r4 over 2 s at random ground points; 30 mana; about 1.1 expected landings on a centred target (7.9 dmg), 0.26 dpm (0.44xB) | WEAK: the dearest shape delivers the least, and by chance | Call down the sky | Zone, Constellation | sky circle 12 blocks up + ground reticle + per strike a 9-block streak and splash + impact sound | Spectacle exceeds payoff: biggest-looking T3, smallest output | Strikes seek: 60% pick a random enemy in the area (fallback a random point), each with a 6-tick target ring; strike radius 1.6 to 2.0. `drop` whistle per strike, `boom` on L+ |
| arc | 1 | 3 x1.1 | projectile | lob 1.1 b/tick with gravity, r2 splash (target included); bounce, split, volley; 11.8 mana | OVER vs Bolt: same price, free 2-block splash | Lob it | Bolt, Comet | the same comet launched 0.28 upward; landing = impact x1.4 | Indistinguishable from Bolt except the curve | Price x1.1 to x1.25. A dotted ground-shadow tracer in flight, a `whistle-down`, a heavier `thud` landing with a splash-radius ring so the splash is readable |
| cone | 2 | 5 x1.4 | area | 60 deg x 6 blocks, instant, 19 m2; 16.2 mana, 0.43 dpm (0.73xB) | FAIR, slightly weak: Barrage hits 2.8x per target in a similar cone; Sweep covers 4.6x the area for 1.1x the price | Spray forward | Barrage, Sweep, Crescent, Burst | 7 light rays fan out + 3 opening arcs + 10 motes; element cast sound 0.7 again (duplicate) | OK, clear fan | Shotgun falloff via Hit.power: near half x1.35, far half x0.85 (identity: rewards closeness); short `sweep` gesture sound; add an AimPreview fan (none today) |
| trail | 2 | 7 x1.8 | lingering | 5 s laying + 3 s life; patches r0.9 hit every 0.5 s; up to 16 landings, about 8 realistic (56 dmg); 21.4 mana, 2.62 dpm (4.4xB) | OVER against chasers (melee mobs walk your footprints) | Leave danger behind | Zone, Orbit | small seal every 30 ticks, dust every 4 ticks per patch, one amethyst resonate at start | Low contrast; barely reads | Element footprints via the skin (fire embers, frost rime, storm static, life flowers); a soft `tick` when someone is caught; multiplier 1.8 to 2.6 |
| wall | 3 | 10 x2.2 | lingering | 7 wide, 3 high, 6 strikes in 5 s within 1 block of the line; 21 m2; 27.6 mana, 1.52 dpm (2.57xB) | OVER on dpm, and a thin Zone: no barrier property despite the name | Draw a line they must cross | Zone, Totem | posts + top/bottom bars redrawn every 20 ticks (14-tick life, so it blinks: 6.4 item 12) + motes; `MACE_SMASH_GROUND` at start | GOOD fence silhouette | Give it the barrier: reuse Haven's projectile-reflect loop (`Effects.haven`) along the segment and slow creatures touching it 30%. `thrum` bed, `crack` when it stops a projectile. AimPreview line (none today) |
| orbit | 3 | 9 x2.0 | personal | 3 orbs at r2.2 for 8 s, each creature at most once a second; up to 8 landings (56 dmg); 25 mana, 2.24 dpm (3.8xB) | OVER vs melee attackers; Split is a trap (3 to 9 orbs, same one hit per creature per second) | Guard with satellites | Trail, Zone | glow core + dust per orb every tick; `circle_open` at start (doubled); nothing when an orb hits | Fine, but three identical dots in every element | Orb-hit chime per orb (D, F#, A); orb bodies from the skin (ember comet, ice shard, arcing spark); make the once-a-second limit per orb so Split pays; multiplier 2.0 to 2.7 |
| ring | 2 | 6 x1.5 | area | expands to r7 in 10 ticks, flat (+-2.5 vertical), each once; 154 m2; 18 mana, 0.39 dpm (0.66xB) | OVER vs Burst: same price, 3.1x the area (0.12 mana/m2 vs 0.36) | Send a shockwave | Burst, Nova | band of light racing out along the ground + mote rings + `BREEZE_WIND_CHARGE_BURST` 0.8 | GOOD expanding-front read | Make it hollow: skip the inner 25%, so it is the ranged, safe-centre blast and stops mirroring Burst; x1.5 to x1.7; `rush` sound |
| pillar | 2 | 5 x1.4 | area | r1.5, 6 high at aim, once; 7 m2; 16.2 mana, 0.43 dpm (0.73xB); 2.3 mana/m2 | WEAK for the footprint | Erupt from below | Burst, Mine | ground circle + 7-block light column + 6 rings + shake 0.35 + `BREEZE_WIND_CHARGE_BURST` 0.7 | OVER-SCALED show for a 1.5-block hit | Tie column height and shake to band. Mechanic: raise targets 1.5 blocks (Launch-lite) as its identity and so it hits fliers; keep the price |
| wave | 2 | 6 x1.5 | projectile | 3 wide x 14 long along the ground, each once; 42 m2; 18 mana, 0.39 dpm (0.66xB) | FAIR / weak: Crescent is wider and longer for 16.2 | Roll along the ground | Crescent | glowing crest slashes + spray + `SPLASH` particles + splash sound (water, for every element) | Distinct but water-themed for fire and storm too | Element crest from the skin (lava sheet, ice crawl, rolling stone, wind ripple); `rush` sound; keep terrain-following as its unique property |
| mine | 2 | 5 x1.3 | lingering | hidden trap r3, trigger within 1.8, 30 s; 28 m2; 15.4 mana, 0.45 dpm (0.77xB) | FAIR (ambush value) | Set a trap | Snare, Imprint | reticle + ring at arm, heartbeat ring every 2 s, `TRIPWIRE_ATTACH`; trigger = generic `Vfx.burst` | GOOD tension, generic trigger | Idle ring shown to caster and allies only (fair and informative); trigger = an inward implosion then the burst, with a `snap` |
| totem | 3 | 10 x2.4 | lingering | r5, 6 pulses in 10 s; 79 m2; 29.2 mana, 1.44 dpm (2.42xB) | OVER vs Zone (2.8x the area for +5 mana) | Plant a beacon that beats | Zone | floating crystal bobbing + orb, ground seal every 2 s, pulse = ring + ray down + flash + amethyst chime | GOOD | A `toll` bell per pulse climbing a scale degree each time (audible count), element bell timbre; price by landings |
| domain | 4 | 20 x3.0 | lingering | r9 (max 24), 7 strikes in 6 s + Slowness II; 254 m2; 44 mana, 1.11 dpm (1.88xB) | STRONG and right for T4 (9x Zone's area for 1.8x the price) | Make a world of your own | Zone | the spell's own circle across the floor, dome of meridians, screen tint inside, `DOMAIN_OPEN`/`CLOSE`, clash | BEST presentation in the mod; scale reads | Keep. Element dome skins (heat shimmer walls, frost dome...) and a `toll` per strike |
| crescent | 2 | 5 x1.4 | projectile | 5 wide x 16 blocks at 1.5 b/tick, each once; 80 m2; 16.2 mana, 0.43 dpm (0.73xB) | FAIR | Slash at range | Wave, Cone, Glaive | two layered slash arcs + motes; `PLAYER_ATTACK_SWEEP` 0.7 + element cast (duplicate); sweep pose | STRONG read | Element blade skin (flame edge, ice edge with shards); element `sweep` sound instead of the vanilla sweep |
| barrage | 2 | 5 x1.6 | direct | 8 blows x0.35 in 1 s on all within 3.6 blocks and 80 deg = 19.6 dmg per target; 17.8 mana, 1.10 dpm (1.86xB); Quicken adds 4 blows | OVER: best cheap dps; each blow fires On Hit (8x) | Flurry of blows | Cone | 8 tilted slash arcs + crit sparks + alternating attack sounds stepping in pitch; finale ring + bang | STRONG; the most tactile shape | Keep the feel. x1.6 to x2.2 (or 6 blows); blows tinted by the skin |
| orb | 3 | 9 x2.2 | projectile | 0.5 b/tick for 20 blocks (2 s), hits each creature once a second within r2, bursts at the end: about 3 landings (21 dmg); 26.6 mana, 0.79 dpm (1.33xB) | WEAK for T3: a Zone deals 42 for 24 | Drift heavy | Comet, Zone | glow core with 3 rings, custom `ORB_HUM` every 10 ticks, sonic-charge, burst at the end | GOOD; one of the few custom sound identities | Make it piloted: the caster's aim steers it while it lives (lerp 0.08 a tick) and range 20 to 30, so Bolt is straight, Wisp seeks, Orb is piloted; x2.2 to x2.0 |
| blitz | 2 | 6 x1.5 | direct | dash up to 8 blocks striking all within 1.2 of the line; 18 mana, 0.39 dpm (0.66xB) plus a dash | FAIR (the dash is the value) | Dash through them | Dash and Blink effects | flare, thick ray, 3 silhouettes, sparks, crossed slashes at the end, `TRIDENT_RIPTIDE` 1.4 | VERY GOOD, cinematic | Short FOV kick and a doppler-style `rush`; otherwise keep. AimPreview line (none today) |
| spark | 1 | 1 x1.0 | projectile | 75% power, 2.4 b/tick, 16 blocks; 9 mana, 0.58 dpm (0.98xB); split, volley | FAIR: the 1-mana entry | Flick | Bolt, Ray | small glow + thin streak + spark particle; `AMETHYST_BLOCK_HIT` 1.8 + cast 0.3 | Reads as a dot; a glass tick | Short high `snap`; each Volley shot one pentatonic step up |
| ray | 1 | 2 x1.0 | direct | 10 blocks instant; pierce, chain; 10 mana, 0.70 dpm (1.18xB) | OVER at T1 vs Beam at T2 (0.51) | Zap | Beam, Touch, Spark | 0.1 ray + white core + small hand circle; `BEACON_POWER_SELECT` 2.0 + amethyst chime | A thin Beam clone | x1.0 to x1.15 or range 10 to 8; drop the hand circle (Beam keeps it); `zap` sound |
| nova | 1 | 3 x1.3 | area | r2.5 from you, instant; 20 m2; 13.4 mana, 0.52 dpm (0.88xB) | FAIR | Shove them off me | Burst, Cone | flare + 3 tilted ring shells + ground ring + STAR seal + motes; `AMETHYST_CLUSTER_BREAK` + impact; default thrust pose | Big for its size; pose contradicts Burst's | Peel identity: hostile targets nudged 0.6 away; arms-out pose; small `burst` |
| wisp | 2 | 4 x1.3 | projectile | 0.7 b/tick, seeks the nearest visible enemy within 16 for 4 s; 14.4 mana, 0.49 dpm (0.82xB) | FAIR (fire-and-forget premium) | Send a hunter | Bolt + Homing (15.3) | small orb + circling mote + faint trail + seek thread and chime; allay sound | Charming and distinct | Element wisp bodies (ember, snow, spark) + a soft moving `hum`; lock-on ring on the prey |
| comet | 2 | 5 x1.6 | projectile | 1.1 b/tick, 24 blocks, bursts r3 on first contact; 28 m2; 17.8 mana, 0.39 dpm (0.66xB) | FAIR (a ranged Burst) | Meteor bolt | Burst, Cluster | heavy orb in rings + thick streak + `BEACON_AMBIENT` every 8 ticks; burst = flare, shell, ground ring, `GENERIC_EXPLODE` 1.5 | GOOD | Element comet bodies (flaming meteor with smoke, ice boulder, ball lightning); `burst` by band |
| ricochet | 2 | 5 x1.5 | projectile | 4 bounces (+3 per Bounce), passes creatures (each once), up to 7 s; 17 mana, 0.41 dpm (0.69xB) | FAIR (chaos value) | Bounce around | Bolt + Bounce | small orb + trail, flat ring per bounce, slime/amethyst sounds | OK | Each bounce steps up the scale (audible count) and leaves a fading scuff mark |
| cluster | 2 | 6 x1.7 | projectile | ball then 5 shards, each r1.5 where it lands, nothing struck twice; 19.6 mana, 0.36 dpm (0.60xB) | WEAK: a near clone of Comet with a ragged footprint | Scatter shot | Comet | orb with 5 turning shards; break flash + falling shards; `AMETHYST_CLUSTER_BREAK` + `GLASS_BREAK` | FINE | Shards home on the nearest enemy near the impact (sub-seekers): its unique multi-target job; x1.7 to x1.5 |
| lance | 2 | 5 x1.5 | direct | 16 blocks, width 0.7, through every creature; 17 mana, 0.41 dpm per target (0.69xB) | FAIR (Beam + Pierce costs 16.5 for 4 targets; the Lance is unlimited) | Skewer a line | Beam + Pierce | thick beam + white core + triple circle + rings running down; `TRIDENT_THROW` 0.7 + beacon | BOLD and good | Keep; a `crack` sting; each creature passed flashes a brief cross-mark |
| sweep | 2 | 5 x1.6 | direct | 100 deg x 10 blocks fan in 10 ticks (11 rays 10 deg apart), each once; 87 m2; 17.8 mana, 0.39 dpm (0.66xB) | OVER on area (4.6x Cone) and BUGGY: Quicken or Widen thin the fan, so it misses (section 6) | Wipe the front | Cone, Crescent | hand circle + faint arc + rotating searchlight beam; beacon + sweep sounds | GOOD | Fix: one ray per 0.8 blocks at the tip, constant angular step. x1.6 to x1.8. `sweep` sound with a pitch glide that follows the beam |
| prism | 2 | 5 x1.5 | direct | beam to the first creature then 3 rays +-25 deg (10 blocks) each hitting the next; up to 4 landings; 17 mana, 0.41 dpm per target | FAIR (crowd value) | Split light | Lance, Beam + Split | beam + turning star + 3 rays in element/white/secondary; beacon, `AMETHYST_CLUSTER_BREAK` | UNIQUE and beautiful | A three-note chord at the split (in key) |
| stream | 2 | 5 x1.8 | direct | 6 x0.35 strikes in 1 s on the first thing within 20 blocks, follows aim; 14.7 dmg; 19.4 mana, 0.76 dpm (1.28xB) | FAIR (a channel) | Hold the line | Beam | beam redrawn every tick, thicker on a strike + spray; impact sound 6x at one pitch; beacon ambient every 6 ticks | GOOD look, machine-gun sound | Pitch ladder across the strikes at volume 0.2 with 3 variants; a `hum` bed for its length |
| vortex | 3 | 9 x2.2 | lingering | drag r5 to an eye of 1.8, 5 strikes in 3 s; 26.6 mana, 1.32 dpm (2.2xB) plus control | OVER vs Zone in damage; the control is fair | Suck them in | Zone, Gravity Well | 3 spiral crescents at 3 heights + eye ring + motes; wind burst, breeze idle every 10 ticks | GOOD, distinct | Element vortex skins (fire whirl, storm cell, void hole) + a rising `swell` |
| snare | 2 | 4 x1.4 | lingering | tripwire up to 12 blocks, 30 s, springs r2.5 on the first enemy; 20 m2; 15.2 mana, 0.46 dpm (0.78xB) | FAIR; slightly dominates Mine (15.4 for a smaller trigger) | Trip them | Mine | thread flares once, then nothing; glints near posts; tripwire sounds | RIGHT tone | Its own hook: sprung creatures are tripped (Slowness II for 1 s) and the thread whips round them |
| constellation | 3 | 8 x2.4 | area | 5 nearest visible enemies within 12; 27.2 mana; 35 dmg total (1.29 dpm total, 0.26 per target) | FAIR (unerring, for crowds) | Connect the stars | Prism, Wisp | stars over each target joined by lines; amethyst chime 1.5 + impact | ELEGANT | Stagger the strikes 2 ticks apart, each star one scale degree higher (audible count) |
| glaive | 2 | 5 x1.8 | projectile | 12 blocks out and back, hits on both passes; 19.4 mana, 0.72 dpm (1.22xB) | FAIR | Boomerang | Crescent, Bolt | two spinning blades round a hub; riptide tick every 3 ticks | DISTINCT; tick spam | Riptide tick every 6; a `catch` on return; element blade |
| imprint | 1 | 3 x1.3 | lingering | seal under you, erupts r3 after 2 s; 28 m2; 13.4 mana, 0.52 dpm (0.88xB) | FAIR | Leave a surprise | Mine, Burst | seal, closing rings ticking up in pitch, eruption = column + shake | GOOD tension; the eruption is Pillar's column | Erupt as an inward implosion then the burst, so it differs from Pillar |
| latch | 2 | 6 x1.9 | direct | thread to the first creature within 16; 4 x0.7 strikes over 4 s while within 24 blocks and in sight = 19.6 dmg; 21.2 mana, 0.92 dpm (1.56xB); Quicken x2 = 16 strikes | OVER: guaranteed single-target damage; stacks with Quicken and On Hit | Tether | Stream | thread + running bead, target ring, strike flash; lead sounds | CLEAN, unique | A `pluck` per strike on a pitch ladder; x1.9 to x2.4 |

### 2.2 Modifiers (24)

| id | T | cost | role | key numbers | power verdict | verb | overlap | presentation today | pres. verdict | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| amplify | 1 | x1.6 on the effect | power | +50% power; 0.94 value per cost, two stacks 0.88 | WEAK: worse per mana than Linger (1.67), Belated (1.12 to 1.40), Focus (1.25), Frugal (1.2); it sells sockets, not mana | Hit harder | Overcharge, Focus | none | INVISIBLE | A gold ring per stack thickening the cast circle; a low thump under the cast sound; impact +15% per stack |
| extend | 1 | x1.4 on an effect, lingering shape or Delay | timing | x2 duration; on lingering shapes x2 landings for x1.4 (1.43) | STRONG on lingering shapes; plain on buffs | Last longer | Linger | only through longer lifetimes; nothing on instant effects | PARTLY | Sand motes falling through the shape's rim and a longer `thrum`/`settle` tail, a step lower |
| widen | 2 | x1.5 | area | radius x1.5 (area x2.25, 1.5 per cost), up to x8 | FAIR | Bigger | Focus | radius visibly larger everywhere (circles, rings, reticle) | GOOD | An outward ring pulse at cast and a low swell |
| quicken | 2 | x1.2 | projectile | speed x2 (cap x8); intervals halved on Zone, Wall, Totem, Domain, Pulse, Delay, Imprint; Latch and Stream strikes x2; Barrage +4 blows; Sweep gets thinner | DOMINANT on repeaters (x2 to x4 landings for x1.2 to x1.44: 1.7 to 2.8) and it breaks Sweep; the description mentions none of it | Faster | Rapid | flying shapes faster, repeaters pulse faster | PARTLY | Change the semantics to tempo: the same landings in half the time (Zone: 6 pulses at 0.5 s, 3 s life), so it snaps instead of adding free damage. Streak afterimages, a short `snap`, +1 pitch step |
| pierce | 2 | x1.3 | projectile | passes 3 more (Bolt, Beam, Ray); up to 3.1 when lined up | GOOD when lined up, idle otherwise | Through | Lance | none: the bolt just continues | INVISIBLE | A ring flare on each creature passed and a `thread` tick; elongate the comet to a needle |
| bounce | 2 | x1.3 | projectile | 3 bounces (Bolt, Arc, Ricochet) | FAIR | Ricochet | Ricochet | small impact 0.4 at each bounce | GOOD | Pitch ladder per bounce; the flat ring Ricochet already uses |
| split | 3 | x2.4 whole group | area | x3 copies (cap 9), fan 12 deg or spread; 1.25 value | DOMINANT with Volley (9 for x5.76); a trap on Orbit | Multiply | Volley | fan or spread is visible; launch sounds just stack | VISIBLE, audio muddy | The hand circle splits into 3 small circles; a 3-note chord; per-copy pitch offset |
| homing | 3 | x1.4 | projectile | steers within 12 (lerp .25); Bolt only | STRONG (accuracy is worth more than x1.4) | Seek | Wisp | none but the curve | INVISIBLE | Lock-on ring on the target, corkscrew tail, a chirp on lock |
| chain | 3 | x1.8 | projectile | 3 jumps within 6 blocks at full power (Touch, Bolt, Beam, Ray); 2.2 in a crowd | DOMINANT in crowds; multiplies On Hit triggers | Arc to neighbours | Constellation, Prism | a beam and impact per jump (visible), but each jump plays `BEAM_FIRE` at full volume | VISIBLE, loud | A small `zap` per jump climbing a scale degree; power 100/85/70% |
| frugal | 1 | x0.5 | power | half mana, x0.6 power and duration: 1.2 per mana, stacks to 1.73; the shape's base cost is not halved | STRONG for effect-heavy spells, neutral on cheap ones | Thin | (Amplify inverse) | none | INVISIBLE | A small thin circle, a quieter cast at higher pitch, impact x0.85 |
| linger | 2 | x1.8 on the effect | timing | 2 more landings 1 s apart on the same creatures wherever they ran; 1.67; multiplies every pulse of repeating shapes | DOMINANT (Zone . Harm . Linger = 126 dmg per target for 36.8 mana, 3.4 dpm, 5.8xB) | Again | Extend, Echo | repeats the identical full-size impact | INDISTINGUISHABLE from a second cast | Re-landings at 60% power drawn as pale ghost echoes (smaller ring, sound one step down, a ring that rewinds) |
| volley | 2 | x2.4 | projectile | 3 shots 5 ticks apart (Bolt, Beam, Arc, Crescent, Spark, Comet); 1.25 | STRONG; best with Split | Repeat | Split | 3 launches with the same sound and pitch | VISIBLE, audio flat | An arpeggio (each shot one degree up); a hand recoil per shot |
| focus | 2 | x1.2 | area | radius x0.5, power x1.5: 1.25 power, area x0.25 | STRONG on single targets (free x1.5 for x1.2; the cost is footprint) | Concentrate | Widen inverse | a smaller circle only | WEAK | An inward-converging ring and a tighter, higher cast sound |
| overcharge | 3 | x3.0 | power | x2.5 power: 0.83; Amplify x2 gives x2.25 at x2.56 (0.88); cooldown x3 with the cost | WEAK for T3; only saves a socket | All in | Amplify x2 | none | INVISIBLE (the biggest power modifier) | Must feel big: a crackling halo on the circle, a low `boom` layer, impact x1.6, FOV kick. x3.0 to x2.6 |
| rapid | 2 | x1.4 whole spell | timing | cooldown x0.5 (min 0.25 s); each cast pays x1.4: damage per mana x0.71 | FAIR niche: a tempo tax | Again sooner | none | none | INVISIBLE | A crisp clock `tick` in the cast and the HUD ready ping (3.2) |
| vow | 3 | x1.0 | power | the shape's effects x2, cooldown x4 (mana, not cooldown, limits sustained fire) | DOMINANT: the strongest modifier, about 2x mana efficiency for the price of tempo | Swear an oath | Amplify | none | INVISIBLE (the best modifier) | An oath ring at the feet that fills over the x4 cooldown (a visible cooldown) and a solemn bell on cast. Price: x1.3, or cooldown x5 |
| blood_price | 3 | x1.0 | power | 1 health per 5 mana instead of mana; Heal (12 mana = 8 hp) funds 40 mana of spells: 3.3x laundering | DOMINANT in sustained fights with Life runes | Pay in blood | none | `PLAYER_HURT` + damage indicators on the caster | GOOD | A heartbeat thump and a red rim on the circle. 1 health per 4 mana |
| execute | 2 | x1.3 | power | x2 vs targets under half health: 1.54 conditional | GOOD | Finish | Trial Key | damage indicators only when it fires | OK | A distinct `gong` when it triggers (once per cast) and a thin red slash |
| trial_key | 2 | x1.3 | power | x1.6 vs full health: 1.23 conditional | FAIR | Open | Execute | none | INVISIBLE | A key-turn click and a gold spark when it triggers |
| kindled | 3 | x1.4 | power | +20% power and a 4 s burn (about +4 damage); 0.86 plus burn | WEAK for T3 | Set alight | Fire runes | flames on the target when it burns | GOOD | +20% to +30%; otherwise keep |
| unstable | 3 | x1.2 | power | 0.5 to 2.0 log-uniform (mean about 1.0): 0.83 unless variance is the point | RISKY, fair | Gamble | none | a flicker at landing sized to the swing | GOOD | A dice `chime` pitched by the swing (low weak, high strong) |
| kindred | 2 | x1.4 | area | helpful effects also land on you and the nearest ally missed, at 50% | GOOD utility | Share | none | a thread of light to the sharer | GOOD | Keep |
| thirst | 2 | x1.4 | power | heals 25% of damage dealt (75% max) | FAIR | Drink | leech effects | a blood thread to the caster | GOOD | A soft `sip` (drip) when it heals |
| belated | 2 | x1.25 | timing | +40% power, 1.5 s late (max 3: 2.74x for x1.95, 1.40 value); locks onto the struck creatures and follows them any distance | DOMINANT (best power per mana; the drawback is a delay) | Later, harder | Delay | a small clock over each target + tick | GOOD | Re-check the target is within 24 blocks at landing, or +25% per stack instead of +40% |

### 2.3 Links (17)

| id | T | cost | role | key numbers | power verdict | verb | overlap | presentation today | pres. verdict | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| delay | 1 | 2 | timing | +1 s (x2 Extend, /2 Quicken), min 2 ticks; fires from you, re-aimed to where you look then | FAIR utility | Wait | Belated, Pulse | none | SILENT | A fuse: three rising `tick`s and a violet spark at the hand; say in the tooltip that it re-aims |
| on_hit | 2 | 2 | trigger | the rest fires at each creature hit, up to 8 per landing, paid once; cap 128 segments per cast | DOMINANT on multi-landing shapes (Barrage . On Hit . Explode = 8 blasts for about 25 mana) | Hand off | On Kill, On Reaction | none | SILENT seam | A violet ring and `ting` at each handoff point, dimmer for the 5th and later. Price: the k-th trigger in one cast at 85%^(k-1) power |
| on_land | 2 | 2 | trigger | fires when you next touch ground within 10 s, at the landing point | FAIR niche | On touchdown | If Airborne | shockwave + `MACE_SMASH_GROUND` | GOOD | A violet spark at the feet while armed |
| on_kill | 3 | 2 | trigger | fires at each creature killed | GOOD | On death | On Hit | soul particles | OK | A low `toll` and a soul chime |
| echo | 3 | 2 + repeat | timing | everything before fires again 0.5 s later, max 3 (8x), priced linearly | FAIR | Again | Linger, Pulse | silent replay | SILENT | A 20% dimmer replay with a reverb-tail version of the cast sound; a faint violet ripple at the caster |
| pulse | 2 | 2 + 3x rest | timing | the rest fires 3 times, 1 s apart, from you | FAIR | Repeat on a beat | Echo | only the shapes it fires | SILENT link | A metronome `tick` ladder and a violet bead orbiting that empties per run |
| on_hurt | 2 | 2 | reactive | armed 15 s, fires at whatever hurts you (once); paid whether or not | FAIR | Retaliate | On Low Health | shockwave at the caster when it fires | OK | A violet halo on the caster while armed (visible to others: a PvP tell) |
| if_sneaking | 2 | 1 | condition | branch paid whether it fires or not | FAIR utility | If sneaking | If Airborne | none | SILENT | A gate click and violet ring when it passes, a dull tock when it doesn't |
| on_low_health | 3 | 2 | reactive | armed 30 s, fires under 30% health | FAIR | Last stand | If Wounded | shockwave in life green | OK | A heartbeat cue when armed and near the line |
| if_airborne | 2 | 1 | condition | branch paid whether it fires or not | FAIR | Only in the air | If Sneaking | wind ring under the feet | OK | The shared gate click |
| combo | 3 | 2 | condition | every 3rd cast; the branch is paid on all three casts, so the finisher costs 3x per firing | WEAK | Finisher | If conditions | gold ring + callout + level-up sound on the 3rd; nothing on the 1st and 2nd | HALF | Count in the sound: casts 1 and 2 add a note (D, F#), the 3rd resolves on A; refund the unspent finisher's mana |
| imbue | 2 | 3 (stored part x3) | trigger | stores the rest with 3 charges; releases cost nothing; up to 6 imbued | FAIR / STRONG (prepaid) | Store | Delay | imbue sound + glyph and sword VFX | GOOD | Keep |
| if_wounded | 2 | 1 | condition | below half health | FAIR | If hurt | On Low Health | blood-red seal ring | OK | The shared gate click |
| if_outnumbered | 3 | 1 | condition | 3 or more foes within 8 | FAIR | If surrounded | none | seal ring | OK | The shared gate click |
| if_wet | 2 | 1 | condition | in water or rain | FAIR, situational | If soaked | none | water ring | OK | The shared gate click |
| on_reaction | 3 | 2 | trigger | at each creature this group set a reaction off on | GOOD | On chain reaction | On Hit | `linkSprung` flourish | OK | The link `ting` in the reaction's colour |
| on_weakness | 2 | 2 | trigger | at each creature struck with an element it is weak to | GOOD | On exploit | On Hit | `linkSprung` flourish | OK | The link `ting` |

### 2.3b Identity map: where runes overlap and what makes each one distinct

**Shapes.** *Around you:* Nova (peel: a small shove), Burst (lift: the big instant blast, pops targets up), Ring (hollow donut: safe centre, far edge), Cone
(shotgun: near hits harder). *Hitscan lines:* Touch (melee, +30%), Ray (short, cheap zap), Beam (long, instant, sniper scaling), Lance (unlimited skewer), Prism
(splits at the first hit), Stream (channelled), Latch (tether), Sweep (fan). *Projectiles:* Bolt (straight, every projectile modifier), Arc (lob with splash, now priced for it),
Spark (cheap and fast), Wisp (seeks), Comet (heavy burst), Ricochet (bounces through), Cluster (homing fragments), Orb (piloted), Glaive (out and back), Crescent (wide slash),
Wave (ground-hugging). *Traps:* Mine (hidden, proximity), Snare (tripwire that trips), Imprint (delayed, under you). *Fields:* Zone (ground), Totem (beacon that tolls), Wall
(barrier that reflects projectiles), Vortex (drags), Trail (footprints), Orbit (satellites, each with its own once-a-second), Domain (a world), Rain (seeking sky).
Six shapes still lean on their look alone for identity (Self, Ricochet, Glaive, Crescent, Wave, Prism), which is acceptable: each already has a mechanic no other shape has.

**Modifiers.** *Power:* Amplify (raw), Overcharge (spike, one socket), Focus (concentrate), Frugal (thin), Vow (oath), Blood Price (blood), Execute (finish), Trial Key (open), Kindled (ignite),
Unstable (gamble), Thirst (drink). *Area:* Widen (bigger), Split (multiply), Kindred (share). *Timing:* Extend (longer), Linger (again), Rapid (sooner), Belated (later, harder).
*Projectile:* Quicken (faster), Pierce (through), Bounce, Homing (seek), Chain (arc), Volley (repeat). The overlapping pairs to keep apart by their tells: Amplify/Overcharge, Widen/Focus,
Split/Volley (spread against focus), Extend/Linger (the same shape again against the same effect again), Execute/Trial Key (the end of a fight against the start).

**Links.** *Timing:* Delay (wait), Pulse (a beat), Echo (again). *Trigger:* On Hit (hand off), On Kill, On Reaction, On Weakness, On Land, Imbue (store). *Reactive:* On Hurt (retaliate),
On Low Health (last stand). *Condition:* If Sneaking, If Airborne, If Wounded, If Outnumbered, If Wet, Combo. The overlapping pairs: On Hurt/On Low Health, If Wounded/On Low Health (armed against
checked now), Pulse/Echo (a beat against a replay).

### 2.4 The arithmetic behind the verdicts

**Shape league** (`Shape . Harm`, one stationary target, dpm and xB):
repeating shapes Trail 2.62 (4.4x), Orbit 2.24 (3.8x), Zone 1.75 (2.9x), Wall 1.52 (2.6x), Totem 1.44 (2.4x), Vortex 1.32
(2.2x), Domain 1.11 (1.9x), Barrage 1.10 (1.9x), Latch 0.92 (1.6x); then Orb 0.79, Stream 0.76, Glaive 0.72 (1.2 to 1.3x),
Ray 0.70; then the instant single-landing shapes clustered at 0.4 to 0.6: Bolt/Arc 0.59, Spark 0.58, Nova/Imprint 0.52,
Beam 0.51, Wisp 0.49, Snare 0.46, Mine 0.45, Cone/Pillar/Crescent 0.43, Ricochet/Lance/Prism 0.41, Comet/Sweep/Burst/Ring/
Wave/Blitz 0.39, Cluster 0.36, and last Rain 0.26 and Constellation 0.26 per target. The gap is a *price-model* gap: the
multiplier (x1.0 to x3.0) is set by feel and tier, not by landings. Instant area shapes are fair once three or more
creatures stand in them (Burst at three targets is 1.17 total, on par with a Zone on one).

**Area per mana** (Harm, m2 per mana): Ring 8.6, Sweep 4.9, Crescent 4.9, Burst 2.8, Wave 2.3, Imprint 2.1, Mine 1.8, Cluster 1.8,
Comet 1.6, Nova 1.5, Snare 1.3, Cone 1.2, Blitz 1.1, Arc 1.1, Pillar 0.44. Ring, Sweep and Crescent are cheap coverage;
Pillar is 20x dearer per m2 than Ring.

**Modifier value per cost multiplier** (power or landings gained per unit of cost):

| modifier | gain | cost | value |
|---|---|---|---|
| Amplify | x1.5 power | x1.6 | 0.94 (two: 0.88) |
| Overcharge | x2.5 power | x3.0 | 0.83 |
| Focus | x1.5 power, area x0.25 | x1.2 | 1.25 |
| Frugal | x0.6 power | x0.5 | 1.20 (two 1.44, three 1.73) |
| Belated | x1.4 power, 1.5 s late | x1.25 | 1.12 (three: 1.40) |
| Kindled | x1.2 power, 4 s burn | x1.4 | 0.86 plus burn |
| Execute / Trial Key | x2.0 under half / x1.6 at full health | x1.3 | 1.54 / 1.23, conditional |
| Vow | x2.0 power, cooldown x4 | x1.0 | 2.0 |
| Linger | 3 landings on the effect | x1.8 on the effect | 1.67 (two: 1.54; three: 1.20) |
| Extend on a lingering shape | x2 landings | x1.4 | 1.43 |
| Quicken on Zone / Latch / Wall / Domain | x2 to x4 landings | x1.2 to x1.44 | 1.7 to 2.8 |
| Split, Volley | 3 projectiles | x2.4 | 1.25 (both: 9 for x5.76 = 1.56) |
| Pierce / Chain | up to 4 hits | x1.3 / x1.8 | up to 3.1 / 2.2, situational |

**Stacked, `Zone . Harm`** (cost, damage to one creature standing in it, dpm): plain 24 mana, 42, 1.75; +Quicken 28.8, 84, 2.92; +Quicken x2 34.6,
168, 4.86; +Linger 36.8, 126, 3.42; +Amplify 33.6, 63, 1.88; +Extend 33.6, 84, 2.50; +Linger +Quicken x2 53.0, 504, 9.51;
+Linger +Quicken x2 +Extend 74.2, 1008, 13.6. (Bolt . Harm . Linger: 18.8 mana, 21 dmg, 1.11 dpm, already 1.9x plain Bolt.)

**Cooldown against regeneration.** Cooldown ticks equal mana cost (0.5 to 20 s); regeneration is 5 to 8 mana/s (0.25 to
0.4 per tick). A 24-mana spell has a 1.2 s cooldown but takes 3 to 4.8 s of regeneration to pay for, so cooldown is
binding only in the opening burst; and Vow's x4 cooldown (4.8 s here) costs almost nothing in a long fight.

### 2.5 Balance proposals (starting numbers, to be playtested)

1. **Price repeating shapes by landings**: multiplier + 0.15 per landing beyond the first. Zone 2.0 to 2.75 (dpm 1.75 to 1.40),
   Wall 2.2 to 2.95 (1.25), Totem 2.4 to 3.15 (1.19), Orbit 2.0 to 3.05 (1.68), Trail 1.8 to 2.85 (1.88), Vortex 2.2 to 2.8 (1.11),
   Domain 3.0 to 3.9 (0.96), Barrage 1.6 to 1.87 (0.98), Latch 1.9 to 2.17 (0.84), Stream 1.8 to 1.97 (0.71). They stay 1.2x to
   3x Bolt, which is right for a shape that needs setup or positioning, but stop at 4x.
2. **Quicken becomes tempo on repeaters**: landings fixed (Zone 6, Wall 6, Totem 6, Domain 7, Latch 4, Stream 6, Barrage 8), interval
   and lifetime shrink. Sweep gets its ray count from its length (bug fix below).
3. **Linger's re-landings deal 60%** (3 landings = 2.2x for x1.8 = 1.22 value) and do not re-linger on repeating shapes' pulses.
4. **Close clones**: Ring x1.5 to x1.7 and hollow; Arc x1.1 to x1.25; Ray x1.0 to x1.15; Sweep x1.6 to x1.8; Beam 4 to 3 mana;
   Snare and Mine keep their prices but get different hooks (trip vs proximity).
5. **Lift the weak**: Rain (seeking strikes, radius 2.0), Orb (piloted, range 30, x2.0), Cluster (homing shards, x1.5), Overcharge
   x2.6, Kindled +30%, Touch (+30% power), Combo (refund the unspent finisher).
6. **Price the dominant**: Vow (x1.3 or cooldown x5), Blood Price (1 health per 4 mana), Belated (range check or +25%), On Hit
   (k-th trigger at 85%^(k-1)), Split+Volley (leave: it is a socket-hungry, tier III build).

## 3. The casting pipeline as a system

### 3.1 Stage by stage: what is generic today

Paths are under `src/main/java/dev/wildercord/cast/` unless marked *client* (`src/client/java/dev/wildercord/client/`).

| Stage | Today (evidence) | What is generic | What should vary, and by what |
|---|---|---|---|
| **0. Press and aim** | A tap casts on *key-up*; nothing happens on press; a hold of 5 ticks starts a charge (*client* `WildercordKeys.java:24,84-108`). `AimPreview` (only while charging) draws a reticle or dotted line for 28 shapes and nothing for the other 11 (Touch, Self, Cone, Wall, Blitz, Barrage, Trail, Orbit, Vortex, Snare, Constellation); its colour is the rune at index 1, not the first effect (*client* `fx/AimPreview.java:52`) | one dotted line or one ring | the motion (fan, segment, line, ring), the band, the element; the colour bug |
| **1. Charge** | `circle_open` at the start (`Charging.java:127`); one hum loop pitched 0.8 to 1.3 (*client* `fx/ChargeHum.java:26-28`); one both-hands-forward pose although `AvatarRendererMixin` passes the shape (*client* `mixin/PlayerModelMixin.java:31-37`); the spell's own circle at the hands, always 0.42 blocks, its roundels appearing one by one (*client* `fx/ChargeCircles.java:35`), coloured by the first effect; beads glow x2.5; `charge_full` chime at 30 ticks; -40% speed; +40% power | sound, pose, circle size, opening animation; only the roundel *content* and colour are the spell's | pose by motion; hum base pitch and timbre by element and band; each roundel opening plays a note (the spell's melody, 3.3.8); circle size and flare by band; the full-charge chime tinted by the element |
| **2. Release and cast cue** | arm swing; `Vfx.castCircle` (`Vfx.java:179-193`): the spell circle at the feet at radius 1.0 for 28 + n ticks, 6 enchant motes, a 0.3 hand glow, `circle_open` 0.35 and the element cast sound 0.55, **on every cast**; `CAST_POSE` from the *first rune* for 12 ticks (`SpellCaster.java:196`); `release` and the FOV kick only when charged at least 20% (`Charging.java:84-86`); the staff's `Gear.flourish` only with a staff (`Gear.java:83-130`) | the circle's size and lifetime, both sounds, the pose duration; no recoil at all; no cue on a tap | motion picks the gesture sound and pose; band picks circle radius (0.8 / 1.0 / 1.3 / 1.8) and adds kick or recoil at L and XL; the element hand cue (the `Gear.flourish` switch, promoted to everyone); modifier and link tells |
| **3. Travel** | Bolt and Arc: a `RuneBolt` entity drawn client-side as a comet, only two colours synced (`RuneBolt.java:41-42`, *client* `fx/BoltComets.java`); everything else is server-stepped light every tick (`ExpansionVfx.sparkTick` about 3.5 packets a tick, `cometTick` about 4); the flying thing is silent apart from occasional vanilla ticks (`BEACON_AMBIENT` every 6 to 8 ticks, `TRIDENT_RIPTIDE` every 3) | one comet look, no flight sound, no reaction to modifiers | comet style bits (mods), element trail decoration, a flight hum (a client loop following the entity, like `ChargeHum`), later one lightweight visual entity for every travelling shape |
| **4. Impact** | `Vfx.impact` (`Vfx.java:236`): a flare, 2 rings, sparks, motes, sparkle and `theme.impact` 0.7 for Bolt, Beam, Touch, every Chain jump, every Bounce; then each effect's own VFX per target, plus `Vfx.touched` on every target of every effect, even a Heal (`Effects.java:603-607`) | the flare/ring/sparks kit and its size (constants 0.4 to 1.4) | role picks the impact class (strike, blast, pulse, bind, mend, drop); band the size and sound variant; the skin the motif; simultaneous hits aggregate |
| **5. Target feedback** | `Effects.hurt` (`Effects.java:687-730`): vanilla hurt, whose only spell-dependent part is the damage type's sound and knockback; Execute adds damage indicators; damage of 8 or more gives the *caster* a 5-tick FOV punch; no hit flash, no per-hit loudness | every hit feels like vanilla's | hit flash tinted by element, a caster punch scaled by the share of the target's health, a `crack` layer on big hits |
| **6. Aftermath** | none generic. Zone keeps its circle, some effects leave seals (`frostCreep`); explosions leave smoke | the shapes leave nothing behind | from band M up a short-lived ground mark from the skin (scorch, frost sheen, static, cracks, petals, void wisps) for 40 to 80 ticks, and a `linger` sound tail |
| **7. HUD and failures** | badge shade + seconds (*client* `SpellHud.java:262-271`), charge % and a blinking FULL (`SpellHud.java:352-358`), Rhythm's chimes; **no cue when the cooldown ends**; every failure (`SpellCaster.fail`, `SpellCaster.java:435`) is red overlay text and **silent** | one bar, one number | a ready ping and badge pulse; a fizzle tone per reason (cooldown, mana, health, empty) |
| **8. Modifier and link tells** | see 2.2 and 2.3 | 8 modifiers and 5 links have no cue at all | gold tells for modifiers, violet for links (3.3.7) |

### 3.2 The first 200 ms, honestly

A tap cast of `Bolt . Fire` (the usual way to cast): the key comes up (0 to 250 ms after the press, with no feedback during it);
then, in the same tick: arm swing, the spell circle at the feet (radius 1.0), six enchant motes and a hand glow; `circle_open`
0.35 and `cast_fire_n` 0.55; the thrust pose; the `RuneBolt` spawns and plays `cast_fire_n` **again** at 0.5 (same sound, same instant); the comet
leaves. No `release`, no FOV kick, no recoil. The next 200 ms is a comet and a 7-tick trail.

**Does a fire Bolt with Harm feel different from a fire Bolt with Meteor, or from a frost Bolt?**

- The projectile's element is the *first effect's*: `Vfx.theme(group)` is `theme(group.effects.getFirst().effect)` (`Vfx.java:100`).
  So `Bolt . Fire . Harm` and `Bolt . Fire . Meteor` are the same spell until impact: same circle colour, same charge circle,
  same hum, pose, sounds, comet and speed. They diverge only when the effect runs: Harm draws a star seal and orbiting comets
  in arcane pink, Meteor calls a delayed fireball. And `Bolt . Harm . Fire` flies as an *arcane* comet with arcane sounds although the
  damage is fire: there is no blend, the first effect decides the whole shape's look.
- A frost Bolt differs from a fire Bolt in palette (2 colours), mote and spark particles, and 2 cast + 3 impact sounds. It shares
  the skeleton, size, speed, trail length, pose, circle, hum, release and camera.
- A `Bolt . Fire` and a `Bolt . Fire . Amplify . Pierce . Homing . Quicken` look identical (the circle at the feet lists more roundels).

**Where every spell converges:** one charge hum, one hold pose, one circle animation and radius, one `circle_open` and one `charge_full`;
the same cast circle under the feet whatever the cost; one element cast sound (played twice on 12 shapes: Bolt, Arc, Cone,
Crescent, Spark, Comet, Ricochet, Glaive, Imprint, Latch, Stream, Blitz); one impact kit; the same `touched` glow on every target;
vanilla hurt for every hit; one HUD readout; no aftermath. Only the *first effect's element* colours the cast.

### 3.3 A compositional feel system

**Goal:** the shape sets the motion and silhouette, the element the palette and timbre, the effect's role the impact character,
tier and charge the scale, and modifiers and links are *present* in the display, without 248 hand-written show-pieces. The per-rune
work (each effect's own VFX, which the element auditors are reviewing) plugs into a scaffold that supplies everything around it.

#### 3.3.1 Data model

One immutable value per delivery group, derived deterministically from the spell so server and client agree (the client derives
it from the synced `Charge.runes()` ids, the server from the compiled plan):

```java
package dev.wildercord.cast.feel;

enum Motion { FLICK, HURL, BEAM, SLASH, BLAST, SEAL, CALL, AURA }          // from the shape (table below)
enum Role   { STRIKE, BIND, MEND, MOVE, TIME, WORLD, CALL_UP }             // from the effect's RuneCategories category + EffectKind
enum Band   { S, M, L, XL }                                                 // from cost, charge, tier

record Feel(Motion motion, String element, String accent, Role role, Band band, double scale, Mods mods) {
    static Feel of(SpellPlan.Group g, Cast cast);        // in-world (cast gives cost/charge/power)
    static Feel ofRunes(List<RuneDef> runes, double charge);   // client-side, for the charge and the melody
    Vfx.Theme theme();                                          // Vfx.theme(element).with(this): the Theme now carries its Feel (3.3.3)
}
record Mods(int amplify, int widen, int extend, int quicken, int split, int volley, int pierce, int bounce, int homing, int chain,
            int focus, int frugal, int overcharge, int linger, boolean rapid, boolean vow, boolean blood, boolean execute) {}
```

- **Motion** from the shape: FLICK spark, ray, touch; HURL bolt, arc, wisp, ricochet, cluster, comet, orb; BEAM beam, lance, prism, stream,
  latch, sweep; SLASH cone, crescent, glaive, barrage, wave, blitz; BLAST burst, nova, ring, pillar; SEAL zone, totem, wall, vortex, domain,
  imprint, mine, snare, trail; CALL rain, constellation; AURA self, orbit. Unknown (add-on) shapes default to HURL.
- **Element** = the group's *mana-dominant* element (group-level version of `SpellCompiler.elementShares`, which already exists for whole
  spells), **accent** = the second if it has at least 30% of the mana. Today's "first effect" becomes a special case.
- **Role** = the first effect's category (`RuneCategories.categoryFor`): damage to STRIKE, control to BIND, support to MEND, movement to
  MOVE, time to TIME, world to WORLD, summon to CALL_UP; innate follows its nature. `Effects` already knows HARMFUL/HELPFUL, so a Heal
  can never get a strike impact.
- **Impact class** = f(role, motion): BIND and MEND roles always play their own class; CALL motions and from-above effects play DROP; SEAL landings after the first play PULSE; BLAST motions and any strike at band L or above play BLAST; everything else plays STRIKE.
- **Band and scale**: `scale = clamp(0.6 + 0.30 * log2(1 + cost/12) + 0.25 * charge + 0.08 * (maxTier - 1), 0.6, 2.2)`, with `cost` =
  `cast.weight()` (already computed for Shields), so the same spell reads bigger when charged. Bands: S below 0.85, M below 1.2, L
  below 1.6, XL above. Examples: Spark . Harm (9) 0.84, Bolt . Fire (11.8) 0.89, Zone . Harm (24) 1.08, Domain . Harm (44) 1.27, charged 1.52,
  a 150-mana spell 1.73. **Band M is defined as today's constants**, so nothing changes at the middle.

#### 3.3.2 The five layers

Each layer is a static method that takes a `Feel` and dispatches on `motion`, `role`, `band` and the element skin, with a body that
calls today's code when no override exists.

| Layer | Method | Dispatches on | Notes |
|---|---|---|---|
| **Cue** (charge and cast) | `Feels.cue(cast, feel)` replaces `Vfx.castCircle`'s sound and glow; `Feels.chargeStart/Stage(player, feel)` | motion (gesture sound, pose), band (circle radius, kick), element (hand cue = the `Gear.flourish` switch promoted to everyone), mods (gold tells), armed links (violet beads) | fixes the doubled cast sound: the *shape* stops playing `theme.cast` |
| **Travel** | `Feels.trail(level, feel, from, to, tick)`; client `ProjectileHum` | motion, element skin, mods (style bits synced with the entity) | the skin adds a decoration to the shape's own light (embers, snowflakes, static, curls, pebbles, petals, dark implode, glyph flecks, tick marks, drops) |
| **Impact** | `Feels.impact(level, at, normal, feel, count)` replaces the 9 `Vfx.impact` calls and the direct `theme.impact` plays (28 sites with `theme.cast`) | impact class (from role x motion), band, element | STRIKE flash + sparks; BLAST shell + shockwave (area motions, or any strike at L+); PULSE ring + chime (the field landings after the first); BIND closing rings; MEND bloom and no flash; DROP a falling streak then a thud; the sound is `kit(element, verb)` chosen by the class, aggregated per tick |
| **Aftermath** | `Feels.aftermath(level, at, feel, ticks)` | element skin, band | skipped below band M, in passives (`cast.passive`) and past the budget |
| **Feedback** | `Feels.hit(cast, target, amount)` from `Effects.hurt`; HUD ready ping | element (flash tint), damage share, role | replaces `touched`'s constant glow; the caster's punch scales with damage / max health |

**Element skins** (`feel/skin/FireSkin.java` ... ten small classes behind one interface) implement `castHand`, `trail`, `impactAccent`,
`aftermath` and `sound(verb)`; every default body already exists as an `ElementFx` primitive (`flameBurst`, `heatFlare`, `shatterRing`,
`shards`, `bolt`, `sparks`, `gustRing`, `swirl`, `crack`, `stoneShards`, `leafSpiral`, `petals`, `implode`, `blackCore`, `starSeal`,
`shimmer`, `clock`, `goldenTicks`, `pulse`, `drip`), several already assembled in the `Gear.flourish` switch.

#### 3.3.3 Where it plugs into the code that exists

1. **Carry the `Feel` inside `Vfx.Theme`** (`Vfx.java:37`): add a nullable 7th component and keep a 6-argument constructor. Every shape
   VFX method already receives a `Theme` (28 direct plays of `theme.cast` and `theme.impact`), so `Vfx.impact`, `Vfx.beam`, `Vfx.burst` and the
   shape methods gain the feel with no signature changes. `ElementFx.elementOf(theme)` (which guesses the element by comparing colours)
   becomes `theme.feel().element()`.
2. `CastEngine.deliver` (`CastEngine.java:159`): `Vfx.Theme theme = Vfx.theme(g)` becomes `Vfx.theme(g).with(cast.feel(g))`, cached per
   group on `Cast.Shared`. `RuneBolt.launch` does the same.
3. `SpellCaster.cast` (`SpellCaster.java:189-196`): create the `Cast` before `castCircle`, then call `Feels.cue(cast, feel)`; pass the
   `charge` fraction into the `Cast` (today it is folded into `bonuses.power` and lost).
4. `Cast.Hit` gets a `double power` component with a 7-argument convenience constructor defaulting to 1.0, so the 56 existing
   `new Cast.Hit(...)` calls compile; `CastEngine.onHit` multiplies it into `groupPower`. This is the hook for Cone falloff, Beam
   sniper scaling, Touch's +30% and per-trigger On Hit falloff.
5. `Fx.sound` (`Fx.java:160`): the single choke point for sound gets a per-tick voice gate and pitch jitter (3.3.5).
6. `Charging.begin/tick` and client `ChargeCircles`, `ChargeHum`, `PlayerModelMixin`: the charge already carries the rune ids
   (`WildercordAttachments.Charge`), so the client derives `Feel.ofRunes` with no new packet.
7. `Effects.hurt` and the `touched` block (`Effects.java:603-607, 687`): `Feels.hit`.
8. `CastEngine.runSegment` link branches (`CastEngine.java:52-150`): one `Feels.linkTell(cast, link, at, armed/fired)` per link.
9. `RuneBolt`: a third `DATA_STYLE` int (mod counts packed), read by `BoltComets`.
10. `SpellHud`: watch `remaining` go from positive to zero and play the ready cue (client-only).
11. `WildercordSounds`: replaced by a manifest-driven registry (section 4).

#### 3.3.4 What per-rune implementers provide

For each effect rune (248), only an *opt-in* entry in its element's table; a rune with no entry keeps today's behaviour:

```java
// feel/effects/FireFeels.java
static void register(EffectFeels r) {
    r.of("meteor").role(Role.STRIKE).verb(Verb.DROP).aftermath(Aftermath.CRATER);
    r.of("inferno").verb(Verb.SWELL).pulse(Verb.LINGER).aftermath(Aftermath.SCORCH);
    r.of("fire").hit(Verb.STRIKE).aftermath(Aftermath.EMBERS);
}
```

- the **role** if the category default is wrong; the **verb** its impact plays (from the sound kit); optional **aftermath**;
- its own `Vfx.<effect>` (unchanged: this is where each rune's unique look lives) which now sits *on top of* a role-correct base
  impact instead of a generic one;
- for a shape or modifier: nothing (the tables in 2.1 and 2.2 already specify their cues).

Element authors also own their skin (one class) and their sound file (section 4). A checklist per effect rune, five questions: (1) is the category's role right? (2) which impact verb? (3) does it leave something behind, and what? (4) does it have a signature look that is *not* the element's (its own `Vfx` method)? (5) is it meant to be loud (a band floor, for the few runes that should never read as small)?

#### 3.3.5 Throttling and sound rules (extends `Fx`)

Today the only limits are the eye-clearance rule (`Fx.EYE_CLEARANCE`), the 32-block range test, the `muted` flag for passives, and the
game-side caps (64 creatures, 32 blocks, 128 segments, 24 live bolts). There is no sound or packet budget. Add:

- **Voice gate in `Fx.sound`**: at most 3 of the same event per level per tick (each further one dropped), so a Burst on 30 mobs plays 3
  `GENERIC_BURN`, not 30. Multi-hit shapes (Barrage, Stream, Glaive, Orb) should route through `Feels.sound`, which applies the pitch ladders.
- **Pitch and volume rules:** tonal sounds (everything from `sound_art.py`) are played at 1.0, 0.5 or 2.0 (in key) plus a random spread
  of +-3%; arpeggios and ladders use the pentatonic ratios (1.0, 1.122, 1.26, 1.498, 1.682, 2.0); noise-like sounds may sit anywhere from 0.7 to
  1.4. Volume follows band (S 0.7, M 1.0, L 1.15, XL 1.3 of the base) and never stacks two same-event sounds inside one tick within
  4 blocks. Anything that can repeat has at least 3 variants, round-robin with no immediate repeat.
- **Particle budget:** `Cast.Shared` counts packets per tick (soft cap, say 150); over budget, aftermath and skin decoration are dropped first, then
  mote fills; the shape's own skeleton and the impact flash are never dropped.
- **Accessibility:** the existing "Screen Effect Scale" already scales shake and kicks; add a config flag `feel.flash` for flashes
  and give the mod's sounds their own `SoundSource` (today all 855 calls use `PLAYERS`, so spell sounds cannot be mixed apart from
  footsteps and hurt).

#### 3.3.6 Pose per motion (client `PlayerModelMixin`)

Today the charge pose ignores the shape although `AvatarRendererMixin` hands it over, and the release pose keys off the *first rune*
(`SpellCaster.java:196`), so a spell that starts with an effect, or a modifier, always thrusts. Only 19 of 39 shapes have their own
release pose; Bolt, Beam, Arc, Touch, Cone, Spark, Ray, Nova, Wisp, Comet, Ricochet, Cluster, Lance, Sweep, Prism, Stream, Latch, Vortex,
Snare and Constellation share the thrust. Proposed: pick the pose from the **first shape** (or the implicit Self) by *motion*, and give
charging the same eight:

| Motion | Charge pose | Release pose |
|---|---|---|
| FLICK | one hand forward, the other tucked; a finger's flick | a quick snap of the wrist |
| HURL | drawing a bow: casting arm back, other forward | the throw (today's thrust) |
| BEAM | both arms locked forward, still | hold steady for the length of the beam |
| SLASH | sword arm back and across | the sweep (today's Crescent) |
| BLAST | fists together at the chest | arms thrown wide (today's Burst) |
| SEAL | both palms pressed down toward the ground | arms lowered, then lifted (Zone, Domain) |
| CALL | both arms raised | hands opening skyward |
| AURA | arms open, palms out | rings close in (today's Self) |

#### 3.3.7 Family tells: gold for modifiers, violet for links

The UI already colours families (shape teal, modifier gold, link violet, effect by element: `RuneColors`, and the rune ring patterns say the
family too). The world should speak the same language:

- **Modifier tells** at cast time, around the hand circle and the shape's rim, in gold `0xF0C440`: a small gold ring per Amplify, a
  converging ring for Focus, sand for Extend, a snapping streak for Quicken, a split circle for Split, sound layers as in 2.2. They are
  short (under 10 ticks) and never tint the element.
- **Link tells** in violet `0xA064F0`: an *armed* link shows a small violet bead orbiting the caster while it waits (Delay, Pulse,
  On Land, On Hurt, On Low Health; visible to others, which makes reactive links a fair PvP tell); a link that *fires* shows a violet
  ring and a `ting` at the handoff point; a condition shows one gate click (pass) or a dull tock (fail). Nothing tells the *number* of
  On Hit triggers past the fifth (dimmer each).

#### 3.3.8 The spell's melody

Each rune already has an emblem and a ring pattern; give it a note. `note(rune) = pentatonic degree (hash of id) x octave (family)`, with
a timbre per family (shape: wooden knock, effect: the element's timbre, modifier: bell, link: chain clink). While charging, every roundel that
opens on the circle plays its note (the client already times the opening: `ChargeCircles.opening(progress)`); a tap cast plays the first
five or six notes as a 35 ms arpeggio, chosen to include the shape, first effect, first modifier and first link. No two spells sound alike, the
melody is a *signature* opponents can learn to hear, and it costs 13 short samples (a pluck per element, a wooden knock, a bell, a chain clink, each tuned to D and transposed by the pentatonic ratios, octaves via pitch 0.5/2.0) and a
client hook. It needs no new packet: the charge attachment and `CAST_POSE` already reach every nearby client (extend `CastPose` to carry
the rune ids so a tap plays it too).

#### 3.3.9 Defaults, so nothing regresses

- Every `Feels.*` method has a body that calls today's function when no override exists; `Feel.of` never throws and falls back to `Motion.HURL`, `Role.STRIKE`, the shape colour.
- **Band M reproduces today's sizes and volumes exactly** (the constants in `Vfx` become band M's); S and L/XL scale around them.
- Opt-in tables: a rune with no `EffectFeels` entry keeps its category role and its current `Vfx` method untouched.
- A config flag `feel.enabled` (default on once the tour passes) and the existing `Fx.quietly` mute for passives still short-circuit everything.
- Golden checks: `WildercordFeatureTour` frames before and after at band M; `FeelTest` asserts the band-M numbers.

### 3.4 Implementation plan and effort

| Phase | Work | Effort |
|---|---|---|
| 0 | `Feel`, `Motion`, `Role`, `Band`, `Mods` + derivation + pure unit tests (every shape maps to a motion, every category to a role, band boundaries, M = today's sizes) | 0.5 day |
| 1 | `Fx` voice gate and pitch jitter; `Feels.sound`; remove the doubled cast sound; Touch stops using the Beam show; `AimPreview` colour fix; charge fraction into `Cast`; `Cast.Hit.power` | 1 day |
| 2 | Cue layer: element hand cue (extract the `Gear.flourish` switch), band-scaled circle, gesture sounds, pose per motion (charge and release), FOV kick/recoil at L+, `Feel` in `Theme` | 1.5 days |
| 3 | Impact layer: role-based impact classes, `touched` replaced, aggregation; ten element skins with default bodies; aftermath | 2 days |
| 4 | Tells: modifier tells, link tells, HUD ready ping and fizzle tones | 1.5 days |
| 5 | Travel: style bits on `RuneBolt`, flight hum loop, skin trails; (later) one visual entity for all travelling shapes | 1 day (+2 days later) |
| 6 | Sound kit (section 4): package split, manifest registry (1 day), then 10 verbs x 10 elements, 13 notes, 13 neutral events | 3 to 5 days, parallel |
| 7 | Balance changes of 2.5 with test updates | 0.5 day |
| 8 | Per-element work: `EffectFeels` tables, skins, sound files | parallel, per element |

About 8 days of core code (phases 0 to 5 and 7), 10 with the later projectile-entity work, plus 3 to 5 days of sound authoring (parallelisable) and the per-element work.

## 4. Sound kit design

### 4.1 What exists

- `tools/sound_art.py` (1,527 lines) synthesises everything from numpy/scipy primitives (envelopes, FM bells, struck glass, moving-band
  noise, grains, chorus, synthetic reverb), one builder per sound `builder(variant, rng)`, written as mono Ogg Vorbis (q5) through ffmpeg,
  each checked after encoding (peak, loudness by role, DC, click at the ends, energy above 8 kHz and in the 250 Hz to 5 kHz band, loop
  seams). Every tonal sound is in D major pentatonic, so simultaneous sounds harmonise. Seeds are `crc32(name/variant)`, so a rerun is
  byte-identical. I timed the whole current palette (77 events) without encoding: **6.5 s**; the biggest single sound (`domain_open`) 0.28 s.
- 134 files, 2.4 MB: per element `cast_<el>` (2 variants) and `impact_<el>` (3), 15 casting utilities (`charge_loop`, `charge_full`, `release`,
  `circle_open`, `beam_fire`, `orb_hum`, shield sounds, `imbue`, domain, `blink`, `magic_break`), the interface, events, familiars, bosses.
- In the code: `WildercordSounds` registers each by hand; `cast(element)` and `impact(element)` are the only per-element lookups.
  855 `Fx.sound` calls: 681 with a vanilla event (202 distinct vanilla events are referenced in the code), 146 with a custom one (mostly `theme.cast`/`theme.impact`), 28 neither; pitch fixed at the call site
  (134 calls at exactly 1.0; 8 calls randomise), all on `SoundSource.PLAYERS`. The 12 most-referenced vanilla sounds are amethyst chime 48,
  amethyst resonate 46, `FIRECHARGE_USE` 25, `BREEZE_WIND_CHARGE_BURST` 20, `BELL_BLOCK` 19, `AMETHYST_CLUSTER_BREAK` 19, `GLASS_BREAK` 18,
  `LIGHTNING_BOLT_IMPACT` 17, `GENERIC_EXPLODE` 15, `FIRE_EXTINGUISH` 14, `AMETHYST_BLOCK_HIT` 14, `WARDEN_HEARTBEAT` 13.
- The one thing that does not scale: the script is a **single file**, `palette()` a single list, `CASTS`/`IMPACTS` single dicts, `main()` rewrites the
  whole `sounds.json` and *deletes every .ogg it did not just write*, and each event's subtitle text must be added to `NEW_LANG` in
  `generate_assets.py`. Two people adding sounds at once would conflict on all four.

### 4.2 The palette of sound verbs

A **verb** is a kind of gesture, not a specific sound. Element-timbred verbs exist once per element (same job, different material);
neutral verbs are shared.

| Verb | The sound | Used for (stage, role, shape) | Timbre by element (material) | Length | Variants |
|---|---|---|---|---|---|
| **draw** (exists: `cast_<el>`) | the spell leaves the hand | default release | fire crackling whoosh, frost glass chime, storm zap, wind rush, earth grind, life warm bloom, void reversed drone, arcane FM bell, time tick, blood drop | 0.9 to 1.2 s | 2 |
| **snap** | a short crack or click | FLICK release, hitscan, Touch, Ray, Spark | ember pop, ice tick, arc crackle, air pop, stone knock, twig snap, glitch, spark ting, clock click, wet pluck | 60 to 150 ms | 3 |
| **sweep** | a pitch-gliding whoosh | SLASH release (Crescent, Glaive, Cone, Sweep, Barrage, Wave) | flame lick, ice blade, thunder rip, gale, boulder roll, vine lash, void tear, comet arc, hands of a clock, blood arc | 250 to 450 ms | 2 |
| **swell** | a rising bloom to a soft peak | SEAL and CALL opening, Self aura, Rain, Domain, Totem | rising furnace roar, deepening freeze, gathering static, building wind, tectonic groan, bloom, gravity well, star chord, winding spring, heartbeat build | 0.8 to 1.2 s | 1 |
| **hum** | a seamless 1 s loop (client instance following the thing) | projectile flight, Orbit, Stream, Wall and Trail beds, charge variants | crackle-hiss, shimmering glass, buzz, whistle, rumble, soft choir, low drone, sparkle, ticking, pulse | 1.0 s loop | 1 |
| **strike** (exists: `impact_<el>`) | the single hit | STRIKE role, small and medium scale | as today | 0.85 to 1.0 s | 3 |
| **burst** | a wide-band blast with a tail | BLAST impact class, and any strike at L and XL (Burst, Nova, Ring, Comet, Pillar) | fireball whump, shatter, thunderclap, gust, quake, bloom, implosion, starburst, chime crash, gout | 0.6 to 1.0 s | 2 |
| **pulse** | a soft 80 to 150 ms beat | one landing of a field (Zone, Totem, Vortex, Wall, Domain) | dull ember, tink, hiss, breath, thud, leaf rustle, blip, ping, tock, drip | 80 to 150 ms | 3 (round robin) |
| **bind** | a tightening: a falling glide and a click | BIND role (Root, Freeze, Snare, Latch on, Stasis) | scorch clamp, ice clamp, static grip, air pin, stone clamp, vine tie, gravity grip, sigil lock, tick-lock, blood clot | 0.4 to 0.6 s | 2 |
| **mend** | a warm rising bloom | MEND role (Heal, Regrowth, Shield up, Cleanse) | warm hearth, thaw, hum, breeze, spring, bloom (main), rebirth, choir, rewind, transfusion | 0.7 to 1.0 s | 2 |
| **drop** | a falling whistle into a thud | Meteor, Rain, Imprint set, anything from above | falling ember, falling shard, thunder drop, downdraft, boulder, seed fall, black drop, star, minute-hand fall, blood rain | 0.5 to 0.8 s | 2 |
| **linger** | a 0.8 to 1.2 s decaying tail | aftermath and lingering fields fading | embers crackle out, melt drip, static fade, dust settling, pebble settle, petals, thin wisps, glitter, ticking down, drip | 0.8 to 1.2 s | 2 |
| **note** (melody, tells) | one pitched pluck at D | the spell melody, Amplify, On Hit, link tells; played at ratios 1.0, 1.122, 1.26, 1.498, 1.682 and 2.0 | one per element for effects; wooden knock (shape), bell (modifier), chain clink (link) | 200 to 350 ms | 1 each (13) |
| **rumble** | a low sub roll | XL bursts, Domain, big hits, Amplify's thump layer | neutral | 0.8 to 1.5 s | 2 |
| **zap** | a short FM/electric sting | Chain jumps, Ray, storm-like hitscan | neutral | 100 to 250 ms | 3 |
| **tick** | a clock or fuse tick | Delay, Pulse, Combo count, Rapid, Imprint | neutral | 40 to 60 ms | 3 |
| **toll** | a large tuned bell | Totem pulses, Domain strikes, On Kill, Vow | neutral (tuned D and A) | 1.5 to 2.5 s | 2 |
| **rush** | moving air | Ring, Wave, Blitz, Quicken | neutral | 0.5 to 0.9 s | 2 |
| **drip** | a liquid drop | Thirst, Blood Price, melts | neutral | 100 to 200 ms | 3 |
| **crack** | a solid break | Wall stops a projectile, Execute, a big hit's top layer | neutral | 150 to 300 ms | 3 |
| **ui** | ready ping; fizzle for cooldown, mana, health; gate pass and fail | the HUD, failed casts, conditions | neutral, quiet | 100 to 400 ms | 1 each (6) |

**Counts.** New: 10 element-timbred verbs (snap, sweep, swell, hum, burst, pulse, bind, mend, drop, linger) x 10 elements = **100 events**;
13 notes (10 element plucks, a shape knock, a modifier bell, a link clink); 13 neutral events (rumble, zap, tick, toll, rush, drip, crack, and
the six `ui` sounds): in all **126 events and about 237 files** (element verbs 20 files per element = 200, notes 13, neutral 24), about
**3 MB** at the present average of 12 to 18 KB (many of these are short), so the whole mod's sound goes from 2.4 to about 5.5 MB. Synthesis time stays under 30 s; encoding is the slow part (about a minute).

**Which impact class plays which verb** (`Feels.sound` picks by class and band; S plays the small variant at 0.7, XL adds `rumble`):
STRIKE: strike (S, M), burst (L, XL) · BLAST: burst · BIND: bind · MEND: mend · MOVE: rush or snap · TIME: tick, toll · WORLD: crack · CALL_UP: swell + note ·
fields: swell at opening, pulse per landing, linger at the end · hitscan cue: snap · DROP: drop + strike/burst.

**Modifier and link tells reuse the kit** (nothing new to author beyond the notes and neutral verbs): Amplify = `rumble` at 0.3 per stack;
Extend = `linger`; Quicken = `snap`; Split = three `note`s (D, F#, A); Volley = a rising `note` ladder; Pierce = `zap` low; Homing = an upward
`note`; Focus = `snap` pitched up; Frugal = the cast at 0.6; Vow = `toll`; Blood Price = `drip` + low `rumble`; Execute = `crack`; Delay = `tick` x3;
On Hit = link `note`; Echo = the cast sound at 0.6, pitch 0.75; Pulse = `tick` ladder; On Kill = `toll`; conditions = `ui` gate.

**Replacing the overused vanilla sounds**: amethyst chime (48) to `note`/`pulse`; amethyst resonate (46) to `swell`/`hum`; `FIRECHARGE_USE` to fire
`burst`/`sweep`; `BREEZE_WIND_CHARGE_BURST` to wind `burst` and `rush`; `BELL_BLOCK` to `toll`; `AMETHYST_CLUSTER_BREAK` to `crack` and frost
`burst`; `LIGHTNING_BOLT_IMPACT` to storm `strike`; `GENERIC_EXPLODE` to `burst`; `FIRE_EXTINGUISH` to `linger`. Vanilla stays where it is the *material*
(glass break for a shatter, splash for water), routed through `Feels.sound` so it gets jitter and the voice gate.

### 4.3 How to author with the procedural pipeline

1. **Voice profiles first.** Every existing `cast_<el>`/`impact_<el>` builder already encodes its element's material (fire: band-passed noise
   whoosh + saturated sine body + grains; frost: `glass` partials + `shatter` + `sparkle`; storm: `soft_saw` x `chopper` + FM zap; wind: `moving_band`;
   earth: `brown` noise + `thump`; life: chord of `bell`s; void: reversed sweeps + sub; arcane: FM bells; time: `tick` + `clock_bell`; blood: wet
   `blip`s + low `thump`). Extract that into a `Voice` per element (root note, material, noise band, brightness, decay, grit, reverb time) in
   `tools/sounds/voices.py`. A verb builder is then `snap(voice, v, rng)`: shape of the gesture (envelope, glide, length) from the verb, material
   from the voice.
2. **One builder file per element**, each exporting its verbs: `def snap(v, rng)`, `sweep`, ..., using `dsp` primitives. The element author owns that file
   and tunes by ear; `audition.py <element>` renders an audition wav (all verbs in a row) and a spectrogram sheet, the way the existing
   `casting.png` review sheet was made.
3. **Roles and levels**: add `pulse` (-12 dBFS peak, -26 loudness), `tell` (-9, -24) and `hum` (as `loop`) to `ROLES`; everything else reuses
   `cast`/`impact`/`effect`/`grand`. Loops follow `charge_loop`: integer-cycle partials and `loop_noise`, so the seam passes `check()`.
4. **Notes** are single pitched plucks tuned to D4 (293.66 Hz, `ROOT_HZ`) so transposing by the pentatonic ratios plays the scale.
5. **Naming**: event `kit_<element>_<verb>` (`kit_fire_burst`), files `sounds/kit/<element>/<verb>_<n>.ogg`, neutral `kit_neutral_<verb>`; the
   existing `cast_*`/`impact_*` stay (aliases of `draw`/`strike`).
6. **Subtitles**: one per *verb*, not per element (`subtitles.wildercord.kit.burst` "Spell bursts"), about 21 lines added once by the core author; authors
   never touch the language file.
7. **Attenuation**: default 16 blocks; `pulse` 12; `burst` L/XL 24; `swell`, `toll` 32 (as `domain_open`).

### 4.4 Registration

Replace the hand-written constants with `SoundKit`, built from a generated manifest `assets/wildercord/sound_kit.json`
(`{"events": {"kit_fire_burst": {"element": "fire", "verb": "burst", "variants": 2, "attenuation": 24}, ...}}`), read once from the classpath during
`WildercordSounds.init()` (before the registry freezes) and registering every event generically. `SoundKit.of(element, verb)` returns the
event, falling back to the arcane one and then to neutral, so a missing sound degrades instead of crashing. `Feels.sound` (3.3.5) is the only caller.
The old `WildercordSounds.cast/impact/CIRCLE_OPEN...` stay as aliases so nothing else changes.

### 4.5 Working in parallel without conflicts

Split the tool into a package and make every output belong to exactly one part:

```
tools/sounds/dsp.py                 primitives, moved verbatim from sound_art.py lines 1 to ~380 (core owner)
tools/sounds/voices.py              the Voice profiles: one dict entry per element (each author edits their own)
tools/sounds/elements/<element>.py  ten files: cast, impact (moved verbatim) + the 10 new verbs (one author each)
tools/sounds/neutral.py             rumble, zap, tick, toll, rush, drip, crack, notes, ui (core owner)
tools/sounds/legacy.py              everything that is not the kit (interface, events, familiars, bosses), moved verbatim
tools/sounds/build.py               --only <part>, --check, --prune, --merge
tools/sounds/manifest/<part>.json   generated per part: events, files, roles, attenuation
assets/wildercord/sounds/kit/<element>/*.ogg          generated; owned by that part
assets/wildercord/sound_kit.json and sounds.json     generated by --merge from the manifests
```

Rules: (1) an author edits only `elements/<theirs>.py`, adds only `sounds/kit/<theirs>/` and `manifest/<theirs>.json`; (2) `build.py --only fire`
never deletes files outside its own folder (`--prune` is explicit and per part); (3) **`sounds.json` and `sound_kit.json` are generated and are
not committed on element branches**: the integrator runs `python tools/sounds/build.py --merge` (seconds) and commits them once; mark them in
`.gitattributes` so a conflict is resolved by regenerating; (4) a JUnit `SoundKitTest` (no ffmpeg needed) checks that every manifest event
has all its files, every `sounds.json` entry points at an existing `.ogg`, every subtitle key exists in `en_us.json`, and every (element, verb)
pair the `Verb` enum requires exists: an author sees what is missing without starting the game; (5) seeds stay `crc32(name/variant)`, so re-runs are
byte-identical and diffs only ever show real changes.

### 4.6 Pitch and volume variation rules (summary)

Tonal sounds at 1.0, 0.5 or 2.0 with +-3% random spread; ladders and arpeggios at the pentatonic ratios; noise-like sounds anywhere in 0.7 to
1.4; volume by band (S 0.7, M 1.0, L 1.15, XL 1.3); at most 3 of one event per level per tick; at least 3 variants for anything that can repeat, with
no immediate repeat; the mod's sounds on their own `SoundSource`.

## 5. Cross-cutting observations

1. **The element is the only variable that reaches the display.** Two colours, a mote, a spark, two sounds (`Vfx.Theme`, Vfx.java:37). The *first effect*
   picks it for the whole group (`Vfx.java:100`); a `Bolt . Frost . Fire` is a frost comet, and `ElementFx.elementOf(theme)` recovers the element
   by comparing colours, so the Theme cannot even say what it is. Mixed spells never blend.
2. **The same sound pair per element** (2 casts, 3 impacts) plays for every shape and effect of that element: a fire Ring, a fire Beam and a fire
   Domain strike all end on one of three `impact_fire` files. Ten fire casts and you have heard all of them.
3. **The same circle animation** everywhere: 0.42 blocks while charging, 1.0 blocks at the feet on release whatever the cost (`Vfx.castCircle`,
   `ChargeCircles`), 28 + n ticks; only the ground-circle shapes (Zone, Domain, Vortex, Imprint, Pillar) scale theirs, by radius.
4. **The same impact kit** for Bolt, Beam, Touch, Chain jumps and Bounces (9 `Vfx.impact` calls) and the same `touched` glow for every
   effect on every target, Heal included.
5. **The same hold pose** and 20 of 39 shapes on the same release thrust; poses key off the first rune.
6. **Seven skeletons.** Line of light, orb in flight, slash arc, ground seal, radial shell, aura, call from above. Within a skeleton the shapes differ in
   thickness, duration and one gimmick (Prism splits, Stream follows, Latch tethers), which reads, but is a small vocabulary.
7. **Display scale does not follow power.** No display method reads power, charge, cost or tier: a 3-mana Spark and a 150-mana ten-rune spell open the same
   circle; a full charge (+40% power) changes the release volume and a FOV kick but not one particle in the world. The exceptions scale with *radius*
   (Burst, Zone, Domain), which is a modifier's doing.
8. **Modifiers are numbers.** 8 of 24 have no cue at all; 5 more (Linger, Focus, Extend, Quicken, Execute) only show as a side effect of the number (Extend's longer field, Quicken's faster pulses). Modifiers *are*
   written in the spell circle (its script band and star roundels list every rune, with family ring patterns), which only someone who has learned the runes can read.
9. **Links are seams.** 5 of 17 are silent when they fire; nothing shows a link is *armed*, which is the interesting state for On Hurt, On Low Health
   and On Land (and would be a fair PvP tell).
10. **The mod leans on vanilla sound at fixed pitch.** 80% of 855 calls; the amethyst family alone 94 references; the mod's D-pentatonic key (ART.md: "play
    the mod's tonal sounds at pitch 1.0") means nothing to a vanilla sound at 1.4 or 1.8 playing beside it, which most shape cues do.
11. **Sound stacking.** No per-tick voice gate: a Burst on N mobs plays N of each per-target sound; Stream plays its impact sound 6 times at one pitch in
    a second; Glaive plays a riptide tick every 3 ticks; four shapes (Zone, Orbit, Totem, Vortex) play `circle_open` on top of `castCircle`'s own `circle_open`.
12. **Silent failure and no ready cue.** Not enough mana, cooldown, health, empty spell: a red line of text only. When a cooldown ends nothing happens.
13. **No recoil.** The caster never moves or kicks; the camera only ever changes field of view (a kick on a charged release, a squeeze on a heavy hit) and
    shakes for big area shapes (Burst, Pillar, Domain, explosions), so shake follows shape, not power.
14. **Two projectile technologies.** Bolt and Arc are entities drawn client-side (smooth, cheap, colour-only style); every other travelling shape is server-stepped
    particles at 20 Hz (3 to 5 packets per tick per player in range each). Only the first can carry modifier style.
15. **Cooldown is shorter than duration**, so lingering shapes stack (Zone: 1.2 s cooldown, 6 s life); no cap other than mana. A design choice to confirm.

## 6. Proposed implementation order, risks, tests, docs

### 6.1 Order

1. **Quick fixes and the mixer (about 1 day)**, visible at once and independent of everything else: remove the doubled cast sound (12 shapes), Touch
   stops using the Beam show, `AimPreview` colour, Sweep's ray count, the `Fx.sound` voice gate and pitch jitter, `SpellCaster.fail` fizzle sounds
   once the kit exists. These are the "casting feels the same and slightly muddy" bugs.
2. **Balance pass with tests (about 0.5 day)** *before* tuning any timing: Quicken as tempo, Linger's re-landings, the landings-based prices, the clone fixes
   and the weak-rune lifts of 2.5. Tuning sound length to a Zone's duration first would mean redoing it.
3. **Feel core** (3.4 phases 0 to 2): `Feel`, `Theme` carrying it, `Cast.Hit.power`, the charge fraction in `Cast`, the cue layer and poses. Land the API
   *stubs* first (a day), so the element implementers can code against `Feels.impact/hit/aftermath`, `EffectFeels` and the skin interface while the layers are being built.
4. **Sound kit infrastructure** (package split, `SoundKit` registry, manifest, `SoundKitTest`, audition tool): 1 day, then the element authors record their verbs in parallel
   (10 elements, each one file). Neutral verbs and notes are one owner.
5. **Impact, aftermath and feedback layers**, element skins with default bodies, tells (3.4 phases 3 and 4). At this point every spell already reads better even with no
   per-rune work.
6. **Per-element work** (parallel): `EffectFeels` tables (role, verb, aftermath), skin, sound file, and the effect runes' own unique VFX from the element audits, against the same `Feel`.
7. **Travel**: style bits on `RuneBolt`, the flight hum, and later one visual entity for all travelling shapes (this one touches every projectile runner, so last).

### 6.2 Risks

- **Sound fatigue and clipping** from layering (gesture + element + tells): the voice gate, the band gains and a per-cast cap of 3 cue voices and 2 impact voices; audition on a full
  fight (Barrage . On Hit . Explode, a Zone with Linger) before shipping.
- **Packet load**: aftermath, skin decoration and tells add particles. The budget (soft cap about 150 packets a tick per cast) drops aftermath and decoration first; measure with
  the multi-hit worst cases (Domain with Quicken, Zone Split), which today already send the most.
- **Rebalancing ripples**: costs are asserted in `SpellCompilerTest`, `ExpansionRunesTest`, `HeartAndPassivesTest` and `CraftedRunesTest`, several gametests touch Quicken/Linger,
  and the wiki and the readout text are generated from `Runes`, `SpellNumbers` and `SpellCompiler` (`tools/wiki.py`); Quicken's new meaning changes the descriptions of
  Quicken, Zone, Wall, Totem, Domain, Latch, Stream and Barrage. Numbers in 2.5 are starting values.
- **Add-ons**: `Vfx.Theme` is not part of the public API (`dev.wildercord.api` never references it), so widening the record is safe; shapes and effects from add-ons default to HURL and
  the category role, and a client that has never heard of a rune uses the same defaults, so server and client always agree.
- **PvP information**: armed-link halos and melodies tell opponents something about your spell. That is intended (a readable fight), but make it a documented rule.
- **Accessibility**: more flashes at XL; add `feel.flash` next to the existing Screen Effect Scale, keep Domain's tint optional.
- **Size**: about 3 MB of new audio; keep the kit at 12 to 18 KB a file.
- **Dev workflow**: the project notes that rebuilding or running game tests while `runClient` is open closes the developer's game; batch the phases and rebuild between sessions.

### 6.3 Tests and docs affected

- **New unit tests** (headless, `src/test`): `FeelTest` (every shape maps to a motion; every effect category to a role; `Feel.ofRunes` never throws for any of the 80 x element
  combinations; band boundaries; band M reproduces today's sizes; determinism between server and client derivations), `SoundKitTest` (manifest, files, subtitles, verbs).
- **Changed unit tests**: `SpellCompilerTest`, `ExpansionRunesTest`, `HeartAndPassivesTest`, `CraftedRunesTest` (prices and Quicken/Linger numbers).
- **Gametests**: `WildercordFeatureTour` and `WildercordScreenshots` (the smoke list: every shape and secret is filmed; add one frame per band and one per role), a new
  `WildercordFeelTest` (cast every motion x role, assert no exception and that the budget holds).
- **Docs**: `ART.md` (Motion and Sound sections: replace "pitch 1.0" with the verb and pitch rules), `ARCHITECTURE.md` (Visuals and Sounds), `ADDING_RUNES.md` ("Give it a look": now
  role, verb and skin), `DESIGN.md` (shape multipliers and the Quicken/Linger/Extend text), the wiki pages for shapes, modifiers and links (generated), `CHANGELOG.md`, and
  `generate_assets.py` `NEW_LANG` for the 21 verb subtitles.

### 6.4 Broken or suspicious (each with where)

1. **The element cast sound is played twice on 12 shapes** (Bolt, Arc, Cone, Crescent, Spark, Comet, Ricochet, Stream, Blitz, Glaive, Imprint, Latch): `Vfx.castCircle`
   (Vfx.java:190) plays `theme.cast` at 0.55 on every cast, and `RuneBolt.java:116`, `Vfx.java:633`, `ShapeRunners.java:398`, `ExpansionVfx.java:77,180,213,358`,
   `TechniqueVfx.java:237`, `CraftedVfx.java:61,99,127` play it again. Zone, Orbit, Totem and Vortex likewise stack a second `circle_open`. Certain (code).
2. **Sweep misses at range when Quicken or Widen is used.** `SpellNumbers.sweepTicks` = 10 / 2^Quicken (min 3) steps, so 11, 6 or 4 rays over 100 degrees, each `along(..., 0.5, ...)` wide, while `sweepLength`
   grows with Widen but the ray count does not: at the tip of a plain 10-block Sweep the rays are 1.7 blocks apart against a catch width of about 1.6 for a zombie (worst case just missed); with one Quicken they are 3.5 apart, with two
   5.8 apart; with one Widen (15 blocks) 2.6 apart. *(Unverified in game; arithmetic from `ShapeRunners.sweep` and `SpellNumbers`.)*
3. **`AimPreview` colours by the wrong rune**: `charge.runes().get(min(1, size-1))` (client `fx/AimPreview.java:52`) is the *second* rune: a modifier's gold, a second shape's teal, or the
   shape's teal for an effect-first spell, not the first effect's element the charge circle uses.
4. **Touch plays the whole Beam show** (a 24-block beam sound and rings for a 3-block poke): `CastEngine.touch` calls `Vfx.beam`.
5. **Release pose and charge pose ignore the shape**: the pose is keyed on `runes.getFirst()` (`SpellCaster.java:196`), so effect-first and modifier-first spells always thrust, and
   `PlayerModelMixin` ignores the shape it is handed while charging.
6. **Heal and other helpful runes get an attack impact** on Bolt/Beam/Touch hits: `Vfx.impact` runs before the effect and knows nothing of its kind (`RuneBolt.java` hitEntity, `CastEngine.beam`).
7. **Combo charges the finisher on all three casts** and fires it on one; **conditions and reactive links charge their branch whether or not it fires** (no refund anywhere):
   documented as "build two spells in one" but not as "pay for both".
8. **Quicken's description is wrong about what it does.** "Bolts fly twice as fast; delays are halved" (`Runes.java:204`): it also doubles the pulses of Zone, Wall, Totem and Domain,
   the strikes of Latch and Stream, adds blows to Barrage, and thins Sweep.
9. **Readout text against runtime counts**: Domain strikes 7 times in 6 s (the text says every second for 6), Wall 6 times in 5 s, Vortex 5 times in 3 s ("twice a second"); the first
   landing is at tick 0. Cosmetic.
10. **Rain's strikes are pure chance** (about 1.1 landings on a centred creature of 5 promised strikes; the preview reticle shows radius 4 as if it were all hit).
11. **Spell sounds share `SoundSource.PLAYERS`** with footsteps and hurt sounds (all 855 calls): a player who turns "Players" down loses their own spell feedback.
12. **Wall's fence blinks.** `ShapeRunners.wall` calls `Vfx.wall` on ticks that are multiples of 4, and `Vfx.wall` draws the posts only when `tick % 10 == 0`, so only every 20th tick
    draws them, each with a 14-tick life: the fence is dark for 6 of every 20 ticks, and its motes (`tick % 3`) appear every 12 ticks. *(From the code; not seen in game.)*
13. **Lingering shapes stack without limit** because cooldown (1 tick per mana) is shorter than every duration; only mana stops five overlapping Zones. Design question, not a bug.

### 6.5 What I could not verify

I did not run the game, hear any sound, or profile. Unverified: the audible effect of stacked identical sounds; Sweep's ray gaps against real hitboxes (item 2); Trail's realistic hit
count (I used 8 of a theoretical 16); Rain's 1.1 expected landings (a geometric estimate, strikes uniform in the disc, hit radius 1.6 + 0.3 for a zombie); particle *packet* counts (I
counted particle calls, and packets are sent once per player in range); the `fx_shots` frames (most caught the wrong instant, so I used `docs/images` and the contact sheets instead);
and how the proposed band thresholds land on real spells beyond the handful computed in 3.3.1.
