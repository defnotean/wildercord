# Spell feel audit: fire and blood (44 runes)

Phase 1 of the spell-feel pass: the audit and a proposal for every rune of the **fire** and **blood**
elements (26 fire, 18 blood: base, fused, signature, innate and world runes). Nothing in the game has
been changed. Everything below was read from the code at `main` (commit `f48ab39`, 0.5.0-alpha); no
number was measured in-game (see section 8 for what could not be verified).

Effort tags on proposals: **[S]** about an hour, **[M]** half a day, **[L]** a day or more.

## 0. Headline findings

1. **Fire (Tier II, 8 mana) is the best fire rune per mana, and nearly every Tier III fire rune loses to
   it for one target.** Fire delivers 11 for 8 mana (5 now, 6 from the burn: 1.38 per mana); Explode
   delivers 12 for 18, Meteor 14 for 24, Firestorm 10 for 18, Steam 4 for 14. With Extend on Fire the ratio
   is 1.52 per mana. Tier III fire has to win on *verbs*, not on numbers, and today most of them do not.
2. **Three blasts multiply themselves.** Explode fires one blast per harmed target (up to 4), Meteor one
   meteor per target (up to 4), Primer one bomb per target (up to 8), and nothing stops an enemy standing
   in several of them. Burst + Explode (33 mana) on four bunched enemies deals about 41 to *each* (3.4 times
   the single-target ratio); Burst + Primer on eight deals about 66 to each. Skyburst and Magma/Tempest already
   solve this (one blast per enemy per cast); Explode, Meteor and Primer do not. It is also the biggest
   "screen-filling effect": 4 to 8 full explosions (about 45 particle packets and a loud vanilla sound each).
3. **Cinderheart stacks with itself** (bug). Every cast starts a fresh 12-pulse aura with no de-duplication.
   Keeping one aura up costs 2.5 mana a second, well under the 5 to 8 you regenerate, and spending all of your
   regeneration on recasts (the cooldown is only 1.5 s) piles up 2 to 3 overlapping auras: 6 to 10 damage a second
   to every enemy in 4 blocks. Even a single aura is 2.4 times its tier's ratio.
4. **Blood is a family of good ideas with weak numbers in the middle.** Transfusion is strictly worse than the
   Tier I Heal (same 12 mana, same 8 health, costs you 4 health). Hemomancy is worse than 8-mana Harm until you
   are nearly dead. Crimson Mist and Heartstopper deliver about half their tier's value. Cleave is 0.36 damage
   per mana against ordinary mobs and 1.9 against a Wither. Gash and Rend do almost nothing to monsters.
5. **Blood has no upside creature and no home terrain.** No creature is weak to blood, eight resist it
   (skeletons, golems, wither, blaze, breeze, vex), and there is no climate shift for it. Fire's home, the
   Nether, is where it is worst: 13 entries of the `resists_fire` tag are Nether mobs and water creatures that take
   half, and the vanilla fire-proof ones among them (blaze, ghast, magma cube, strider, wither skeleton, zombified
   piglin; not verified for 26.3) take nothing from fire damage sources at all.
6. **Blood drain has four near-clones** (Leech, Lifesteal, Blood Moss, Parasite) and **fire's single-target
   ignites have eight** (Ember, Fire, Sunscorch, Firestorm, Blazecall, Kindling, Cinderbrand, Everburn).
   Section 3 gives each a distinct verb.
7. **Sound is the weakest layer.** Seventeen fire and blood effect cues use the same vanilla
   `FIRECHARGE_USE`; nine blood cues (six runes) use `WARDEN_HEARTBEAT`; the custom `impact_fire` is played
   explicitly by only three effects (Sunscorch, Conflagration, Seethe) and `cast_fire` by two (Ashen Veil,
   Cinderheart). The caster's cue (circle, sound, pose) depends only on the *element* and the *shape*, never
   on the effect, so Ember, Fire, Explode and Inferno feel the same to cast.
8. **Good news:** the fused, signature and world runes already have distinct silhouettes (Phoenix Pyre's wings,
   Hellmouth's black pit, Everburn's clock, Sanguine Rite's lance, Hemomancy's blood runes, Seethe's bubbles,
   Skyburst's rain of fire). The presentation problem is concentrated in the older Tier I and II runes and in
   sound. Most proposals below are one-hop mechanic twists plus a sound.

## 1. Method and baselines

### 1.1 Reading the numbers

- Cost of a whole spell: `shape cost + effect cost x shape multiplier` (x each modifier's multiplier);
  cooldown is one tick per mana (0.5 s to 20 s). Mana regenerates at 5 to 8 a second (Twine to Echo), plus
  0.5 a second per heart circle, so **mana efficiency, not cooldown, is the limit**; the pool (100 to 300)
  sets how much burst you can carry.
- All numbers are base power 1.0, PvE, target dry and not resisting, DoT fully delivered. Each rune's
  numbers come from the effect code (`Effects`, `Techniques`, `ExplorerEffects`, `CraftedRunes`, `Innates`,
  `FusedFlame`, `FusedLife`, `FusedStorm`, `FusedVoid`, `SignatureFusions`), not from the descriptions.
- Vanilla burning is 1 damage a second (`ReactionRules.lingering`). Fire's burn ignores Amplify; Extend
  doubles it. Burn is worth less than a direct hit (it lands late, water or a Cleanse ends it, it cannot
  burst), so every table gives two currencies: **I** = front-loaded damage per mana (within about a second),
  **T** = total damage per mana once the DoT has run.
- Hurt frames are cleared on every spell hit (`Effects.readyToHurt`), so multi-hit runes really land every hit.
- Health is priced at the mod's own exchange rate: Heal is 8 health for 12 mana, so **1 health = 1.5 mana**.

### 1.2 Role baselines (per mana, median [range]; arithmetic in Appendix A)

| Role | Reference | Peers used |
|---|---|---|
| T1 single-target poke | 0.86 [0.57 to 1.0] | Harm .88, Icicle .83, Pelt .80, Windcut .67, Shock 1.0 (with a second enemy), Umbra 1.0, Countdown .86 |
| T2 damage over time | 1.375 [1.25 to 1.5] | Venom 1.5, Fire 1.375, Bleed 1.25 |
| T2 instant hit with a rider | 0.62 [0.5 to 1.1] | Frost .62, Fangs .6, Ripple .6, Manaburn .5, Stalactite .97, Aftershock 1.1 |
| T3 single-target damage | 0.53 [0.31 to 0.69] | Smite .62, Lightning .60, Blackspark .69, Plasma .45, Entropy .47, Shriek .67, Tempest .36, Devour .31 |
| T2/T3 area, per enemy in the area | 0.5 [0.27 to 0.67] | Thunderclap .42, Coldsnap .27, Repel .56, Tremor .44, Explode .67 |
| T4 damage | 0.71 [0.46 to 0.83] | Sonic Boom .46, Hollow .78, Dragon Breath .83, Wither .64 |
| Healing | 1.5 mana per health | Heal 8 for 12, Regrowth 6.4 for 10, Lifebloom about 12 for 16 |
| Control, seconds per mana | 0.08 to 0.43 | Jolt .11, Glacier .125, Freeze .16, Decree .20, Root .33, Bubble .38 (per target), Drowse .43 |

Verdict thresholds: **under** below 0.75 of the band or dominated by a cheaper sibling; **fair** 0.75 to 1.4;
**over** above 1.4 or free of any price. Every verdict says which band it was judged in.

### 1.3 Things that change the arithmetic for the whole group

- **Shapes multiply.** Zone, Wall, Totem, Domain, Orbit, Pulse, Latch and Linger re-apply an effect, so any
  instant rune becomes a field; Inferno (20) competes with Zone + Fire (24 mana: 30 direct plus burn, twice
  Inferno's total). Judge an effect as a *verb*, since the shape supplies the repetition.
- **Linger** re-lands an effect twice more, a second apart, for x1.8 of that effect's cost. Fire, Ember,
  Kindling, Cleave, Dismantle, Leech, Meteor, Sunscorch and Blazecall have the Linger trait; Explode, Inferno,
  Primer, Flashfire, Conflagration and most fused runes do not.
- **Burst and Nova are self-centred**; Bolt, Beam, Comet and Zone give range. So "Fire + Burst" is a
  point-blank ignite around you, and Firestorm/Flashfire on a Bolt is the ranged version.
- Almost all spell damage ignores armour (magic, fire, freeze), so armour-breaking only matters for
  weapons, arrows, mob melee and physical spells (Cleave, Aftershock).
- Innate runes (Kindling, Blood Thread) are one per caster at random (1 in 10) and gain 6% per heart circle
  (up to +48% at the 8th).

## 2. Verdict tables (one row per rune, two halves keyed by id)

Roles: **P** poke/instant, **D** damage over time, **A** area, **H** hybrid, **B** buff/ward, **C** control,
**U** utility. Clusters are in section 3.

### 2A. Power and identity

| id | T | mana | role | key numbers (engine, base power) | I / T per mana | power verdict | verb (what it does that nothing else does) | overlap |
|---|---|---|---|---|---|---|---|---|
| ember | 1 | 6 | P/D | 3 fire + burn 3 s (3); 0.5 s cooldown | .50 / 1.00 | fair (1.16x T1 band; instant 0.58x) | the cheapest way to put Burning on something | Fire (Ember x1.8 for x1.33 mana), Kindling; Icicle, Pelt, Windcut, Umbra, Harm |
| fire | 2 | 8 | D | 5 + burn 6 s (6); Extend doubles the burn (1.52/mana); Amplify raises only the 5 | .63 / 1.38 | fair (1.0x of the T2 DoT band; the best fire rune per mana) | the standard ignite: hit plus long burn | Ember, Venom, Sunscorch, Everburn |
| flashfire | 2 | 11 | A | 4 + burn 3 s (3) to every enemy within 3 blocks (sphere), at the aim point or around you on Self | .36 / .64 per enemy | fair (1.2x T2 area band); redundant | a pre-packaged small-area ignite | Nova + Fire (13.4 mana: 11 each in 2.5 blocks), Fire + Burst; Coldsnap, Thunderclap, Repel |
| explode | 3 | 18 | A | 12 at the centre falling to 7.2 at 3.5 blocks, knockback 1.3 + 0.45 up; **one blast per harmed target, up to 4, no per-enemy cap** | .67 / .67 | fair alone (1.3x area band); **over in crowds (about 41 each on 4 bunched enemies, 3.4x)** | the instant punch that throws things | Meteor, Primer (same explosion code and visuals), Skyburst (needs it); Tremor, Thunderclap, Repel |
| meteor | 3 | 24 | A | 10 at the target's feet + burn 4 s; lands 0.6 s after the cast on the position captured at cast; r3; up to 4 | .42 / .58 | **under: 0.63x Explode's instant, 0.87x its total, for 33% more mana and 0.6 s late** | a delayed Explode | Explode, Primer; Cometfall (needs it), Starfall |
| inferno | 3 | 20 | A/D | 4 pulses of 3 (one a second) + ignite 3 s each = 12 + about 6 burn; hit area is a **box** (radius x radius, 2 up/down), so corners reach 5.7 | .15 / .90 per enemy who stays | fair (0.9 if they stay, about 0.45 if they walk out; no Linger) | a static burning field | Zone + Fire (24 mana, 2x its total), Hellmouth, Cinderheart, Crimson Mist |
| primer | 3 | 18 | A | target becomes a bomb: after 2 s explodes where it *then* stands, 10 at the centre, r3; up to 8 bombs, no per-enemy cap | 0 / .56 | fair alone (delayed); **over in crowds (about 66 each on 8 bunched enemies)** | a bomb that follows its target | Explode, Meteor; Doomclock (its child) |
| kindling | 1 | 7 | P/D | innate: 3 + a stack; 5th stack (within 6 s) +10 in a **cube** of +/-3; 5 casts = 25 for 35 mana | .43 / .71 (1.29 with 3 in the burst) | fair to under alone (0.83x T1 band), fair in packs | stack up, then detonate | Ember, Combo link, Blackspark (a 1-in-4 spike) |
| firestorm | 3 | 18 | A/D | 4 + burn 6 s on the target (10); every enemy within 2 blocks of a target is set alight 6 s (no direct damage); up to 8 targets | .22 / .56 | **under: 0.4x Fire per mana; its spread loses to Flashfire on a Bolt (15 mana, 7 each in 3 blocks)** | ignition that spreads to neighbours | Fire, Flashfire, Conflagration, Wildfire reaction |
| steam | 3 | 14 | C/P | 4 (element fire, no Shatter) + Blindness 3 s; mobs drop their target | .29 / .29 | **under (0.55x T3 band): Blind (5) + Ember (6) = 11 mana for 6 damage and 5 s of blindness** | scald + blind | Blind, Seethe, Sandstorm |
| sunscorch | 3 | 16 | P/D | world (badlands): 8 + burn 5 s = 13; under open sky by day, 8 x1.5 = 12 (17 total) | .50 / .81 (day .75 / 1.06) | fair (1.5x T3 band, 2x by day; 0.6x Fire at night) | a sunbeam that scales with daylight | Smite (holy), Lightning (from the sky), Fire |
| soulfire | 3 | 16 | D | world (soul sand valley): 5 pulses of 3 = 15, no ignite; gives back 0.35 x damage as mana, 5 per cast at most (net cost 11) | .19 / .94 (1.36 net) | fair; text says "burns on through water" but wet targets take 0.75x and fire-proof mobs take 0 | a burn that pays you mana | Blackflame (void, water-proof DoT), Fire, Ember |
| blazecall | 2 | 11 | P/D | world (fortress): 3 fireballs 0.35 s apart, 2 each + ignite 3 s (about 6 + 3.5) | .18 / .86 | **under: 0.6x Fire for a single target (Fire 8 mana: 11)** | three staggered hits | Fire (+Volley), Starfire, Barrage |
| cinderbrand | 2 | 9 | U/P | world (Ember Sanctum): 3 + a brand for 6 s: your *spell* damage of fire hits it x1.5 (burn ticks excluded) | .33 / .33 | fair as a setup (pays back after 6 extra fire damage), text says "burn hotter" but the burn is untouched | a mark that amplifies fire | Hex, Spellbrand, Eclipse (marks) |
| ashen_veil | 3 | 14 | B | world (Ember Sanctum): Fire Resistance 10 s; melee attackers within 4.5 blocks are set alight 4 s (about 4 each, not stacking) | n/a | **under: Bramble (T1, 6 mana) does 3 per hit for 10 s; Fireward gives 30 s of immunity for 8** | a ward that burns those who touch you | Fireward, Bramble, Stormheart |
| cinderheart | 4 | 30 | H/B | world (Cinder Warden): 12 s of Strength II + fire immunity + 3 a second to every enemy within 4 blocks in line of sight (12 pulses = 36, about 50 with burn); **no de-duplication, recast every 1.5 s** | n/a / 1.67 per enemy | **over: 2.4x the T4 band; 6 to 10 damage a second per enemy when recast (bug)** | a burning heart: mobile aura | Phoenix Pyre, Inferno, Bramble |
| searing_edge | 2 | 8 | B | 15 s: each melee hit the target lands +2 fire and ignite 4 s (about 35 over 10 hits, 4.4/mana); passive-capable (0.96 mana/s) | n/a | fair (Empower: 12 mana for +6 a hit) | a weapon enchant that sears | Empower, Imbue, Overdrive |
| fireward | 2 | 8 | B/U | Fire Resistance 30 s + snuff; blocks every IS_FIRE source (burn, fire spells, lava) | n/a | fair (a hard counter, cheap per second) | fire immunity | Ashen Veil, Cinderheart, Phoenix Pyre (each bundles it), Frostward |
| smelt | 2 | 6 | U | mines the block and drops it smelted (+ furnace XP); iron, diamond with Amplify | n/a | fair | a portable furnace | Break, Excavate, Vein, Chisel |
| hellmouth | 3 | 20 | A/C | fused fire+void: 3 s pit, r3 drag (not bosses), core (1.2 blocks) burns 2 x3 + cave-in 4 = about 12 to 14 for those who stay | .2 / .60 (core) | fair (niche) | a black-fire pit that drags then collapses | Gravity Well, Sinkhole, Riftcall, Singularity, Vortex |
| starfire | 3 | 16 | A/D | fused fire+arcane: 5 seeking motes (within 6 blocks, line of sight), 2 + ignite 3 s each: 14 on one target, 5 each on five | 0 / .88 (1.56 on 5) | fair to high (1.66x T3 band) | motes that hunt | Blazecall, Wisp, Homing, Constellation |
| everburn | 3 | 16 | D | fused fire+time: 5 s at double rate (+1 a second) then one rekindle 3 s = 16, no direct hit | 0 / 1.00 | **under: Fire + Extend is 17 for 11.2 mana (1.52/mana); Everburn is 0.66x that** | a fast double burn that relights once | Fire, Blackflame, Bloodboil, Elapse |
| conflagration | 3 | 20 | A/D | fused fire+fire: 8 s burn, then every burning enemy within 6 blocks of a target flares 3 and +2 s (up to 12): 13 alone, 40 on eight burning | .15 / .65 (2.0 on a pre-lit pack) | fair (1.0x alone, strong with setup) | a chain flare on burning enemies | Firestorm, Everburn, Kindled |
| phoenix_pyre | 3 | 18 | B/A | fused fire+life: Regeneration I 6 s (2.4 health) + fire resistance + aura r2: a second, ignite 2 s + 1 (about 13 per enemy) | n/a / .67 (with heal) | fair | healing flame that follows an ally | Cinderheart, Bramble, Geode, Zephyr |
| bloodboil | 3 | 16 | D | (blood) 3 + up to five 2-damage fire answers to being hurt within 5 s (burn ticks and DoTs count): 13 with five hurts | .19 / .81 | fair (needs hurts; conditional) | every hurt is answered with fire | Reckoning, Resonance, Doomclock |
| rend | 1 | 5 | C/U | -4 armour 10 s (weapons, arrows, Cleave, Aftershock only) + a Bleeding mark 4 s | 0 | **under alone (no damage); fair as a cheap Rupture setup** | tears armour, marks a wound | Hex, Nullify |
| leech | 1 | 8 | P/H | 3 + heal 3 (value 3/.86 + 4.5 = 8.0 for 8 mana) | .38 / .38 + heal | fair (1.0x) | an instant siphon | Lifesteal (Leech x1.67), Blood Moss, Parasite, Thirst, Ripple |
| bleed | 2 | 8 | D | 2 + 8 wounds of 1 every 0.5 s = 10; arms Rupture; stacks per cast | .25 / 1.25 | fair (1.0x of the T2 DoT band) | a wound that ticks and arms Rupture | Gash, Venom, Razorgale (child), Blood Moss |
| gash | 2 | 9 | C | 3 + no healing 8 s + a Bleeding mark 8 s | .33 / .33 | **under in PvE (mobs rarely heal), a strong counter in PvP** | a heal-lock | Bleed, Rend; Silence, Manaburn, Nullify |
| dismantle | 2 | 10 | P | three slashes of 3, 0.1 s apart = 9 in 0.2 s (magic) | .90 / .90 | fair (1.45x T2 instant band; top of it with Aftershock and Stalactite) | a flurry | Barrage (shape), Blazecall, Windcut |
| cleave | 3 | 18 | P | 4 + 12% of max health (cap 30 more), physical (armour applies): 6.4 on a zombie, 16 on a golem, 34 on a Wither | .36 to 1.89 | **under on ordinary targets (0.68x); over on bosses (3.6x)** | a proportional cut | Smite, Execute, Sonic Boom |
| overdrive | 2 | 10 | B | 10 s of Strength II + Speed II + Haste II, -1 health per 2 s (5, floors at 1 health); passive 1.2 mana/s | n/a | over against Empower (12 mana for Strength II alone); the 5 health (7.5 mana) makes a fight fair | pay health for a surge | Empower, Swift, Haste, Warcry, Accelerate |
| warcry | 2 | 10 | B | world (outposts): Strength I + Speed I 12 s for you and allies within 8 blocks | n/a | fair (solo about 0.8x the value of the 18 mana of Empower + Swift it half-replaces; scales with party size) | a party rally | Empower, Zephyr, Surge, Chronoshift |
| blood_moss | 2 | 10 | D/H | world (crimson forest): 1 a second for 6 s and you heal all of it; refreshes, does not stack | 0 / .60 + heal 6 (value 13.4 for 10 mana, 1.34x) | fair to high; beats Leech and Lifesteal per mana | a slow siphon | Leech, Lifesteal, Parasite |
| parasite | 3 | 16 | D/H | signature venom+leech: Poison I 6 s (4.8) + 6 drain (healed): 10.8 and 6 healed; leaps once to the next enemy on host death | 0 / .68 + heal 6 | fair | a burrowing parasite that hops on death | Venom + Leech (parents), Blackflame and Malison (spread on death) |
| lifesteal | 3 | 16 | P/H | fused life+void: 5 + heal 5 (value 16.9 for 16 mana) | .31 / .31 + heal 5 | fair (1.05x) but redundant: Leech x1.67 at 2x the mana | a bigger siphon | Leech, Blood Moss, Thirst |
| crimson_mist | 3 | 18 | A/H | fused wind+blood: 5 s field r3: enemies 1 a second (5), allies +1 health a second (5) | 0 / .28 (+5 heal per ally) | **under (0.53x T3 area band)** | a red haze that bleeds foes and mends friends | Moonpetal, Sporebloom, Haven, Inferno |
| heartstopper | 3 | 18 | C | fused storm+blood: 5 lightning + heart skips at 2, 4, 6 s, each a 0.5 s hold (1.5 s in 6 s) | .28 / .28 + 0.08 s/mana | **under (0.53x; Jolt: 4 + 1 s stun for 9 mana)** | a stun that arrives in beats | Jolt, Freeze, Glacier, Decree |
| sanguine_rite | 3 | 14 | P | fused blood+blood: pay 3 health each time it lands (a Burst pays once for the whole crowd; Volley, Split and Linger pay again), 12 to **every** victim, ignoring armour; the price does not scale with Amplify or Overcharge | .86 / .86 raw, .65 with health priced | fair to high (1.2x T3 band once health is priced); best single-target instant per mana at T3 | blood for power | Hemomancy, Transfusion, Overdrive, Blood Price (modifier) |
| hemomancy | 3 | 16 | P | fused arcane+blood: 4 + 1 per 2 health you are missing (up to +6): 4 at full health, 10 at 12 missing | .25 to .62 | **under (0.47x at full health; needs 12 health missing to reach the band); Harm (8 mana, 7) beats it until you are nearly dead** | power from your own wounds | Sanguine Rite, Overdrive, Execute, On Low Health |
| transfusion | 3 | 12 | B/H | fused life+blood: give up to 4 health (never below 2), the ally heals 8 | 0.44 health/mana with health priced | **under: 0.67x Heal (Tier I, 12 mana, 8 health, free)** | a sacrifice heal | Heal, Lifebloom, Regrowth, Soulbond |
| blood_thread | 1 | 10 | B/C | innate: 8 s, every damage instance on a member (yours, allies', DoTs) deals 50% to each other member; one target threads neighbours within 6 blocks (5 members at most), but **a shape that hits several (Burst, Cone, Nova) threads every one of them, uncapped** | multiplier x1.5 / x2.0 / x3.0 (2 / 3 / 5 members), x5.5 at 10 | **over in packs (3x all damage at 5, unbounded above; for 10 mana); fair by innate design** | link damage between enemies | Soulbond, Reckoning, Resonance, Stormweave |
| seethe | 3 | 16 | C/A | signature bubble+fire: up to 4 targets held 2 s in boiling bubbles (4 scalds of 1), then a burst of 4 to enemies within 2.5 blocks, Blindness 2 s, soaked; each enemy scalded once | .25 / .50 (+ 2 s hold, 8 s across four) | fair | a boiling trap that bursts into steam | Bubble (cheaper control), Steam, Frostwire |
| skyburst | 3 | 20 | A | signature launch+explode: flings up to 3 targets, at the top a blast of 7 (falling off) to everyone within 3 blocks or beneath it, ignite 4 s, Wildfire on the flung one; each enemy caught once | .28 / .58 single (about 60 on six) | fair (high in crowds; 0.3 to 1 s delay) | launch, then rain fire from the air | Explode, Updraft, Wildfire |

### 2B. Presentation and proposals

Legend for verdicts: **good** reads and sounds like its own rune; **generic** shares its silhouette or sound with
siblings; **plain** a bare or borrowed effect; **mismatch** scale or cue disagrees with the power. Sound names
refer to the kit in section 5. `Motes`, `ElementFx`, `Light`, `Sigils`, `ScreenFx` are the existing primitives.
Appendix C restates the proposed signature of every rune in one line (silhouette, motion, accent, sound, timing).

| id | presentation today | verdict | proposal (N = numbers, M = mechanic, S = signature) |
|---|---|---|---|
| ember | eight flame flings in a ring at the feet, gold glow, ground ring; `FIRECHARGE_USE` 0.4 at 1.7 | generic: a smaller Fire (same tongues, ring, embers); the sound is the pop shared by 17 fire and blood cues | M: **Feed**: on an already-burning target it adds 3 s (cap 10) instead of resetting: the cheap way to keep Everburn, Conflagration, Overload and Elapse alive [S]. S: one flicked cinder (a `Motes.seek` arc from the hand, 0.2 s) that lodges with three sparks and a small glow, no ring; `fire_flick`, pitch rising with the burn's age. Scale: tiny |
| fire | heat flare, five then three flame tongues, ground ring, eight embers, smoke; `GENERIC_BURN` 0.5 | good look, quiet voice: the most-cast fire rune has no signature sound | N: keep (it is the yardstick; tune the Tier III against it). S: make ignition audible: `fire_whump` plus the tongues *climbing* for the burn's length (one quiet lick a second). Note: burn should scale a little with power (Amplify currently adds only 23% for +60% mana) [S] |
| flashfire | flare, shell rings, ground ring, 24 radial flames, six smoke clouds, `FIRECHARGE_USE` + `BLAZE_SHOOT` (about 34 packets) | mismatch: a 4-damage Tier II flash draws about three quarters of Explode's display | M: **Thaw**: it also clears Frozen, freezing, Slowness and Wet/Soaked from allies in the radius (heat as a counter to frost mobs and powder snow) [S]. N: 4 to 5 damage, burn 3 to 4 s. S: a white-gold flash disc (0.25 s, flat), a hard white ring, dazzle glare, **no smoke or flames**; `flare_flash` |
| explode | emitter, heat flare, up to eight flame slashes, two fire rings, two ground rings, 18 flames, 7 smoke, 4 lava, camera shake, `GENERIC_EXPLODE` 1.2 (about 45 packets) | mismatch in crowds (4 to 8 at once); the sound is the unmodified vanilla creeper boom | **Fix**: merge blast centres closer than 0.8 x radius into one, and let each enemy take only its strongest blast within 10 ticks (the `blasted` set Skyburst already uses) [S]. M: the blast throws and marks targets Windswept, so an Ember right after sets off Wildfire [S]. S: "the punch": one hard sphere shell and a dust ring, 0.4 s, orange to white; `blast_crack` |
| meteor | reticle (16 ticks), 12-tick orange streak from 18 up with orb, flame trail and smoke, `BLAZE_SHOOT` 1.2 at 0.5, then the same explosion, shake 0.8 | good anticipation, identical impact to Explode | N: delay 0.6 to 1.2 s, damage 10 to 14, radius 3 to 3.5, at most 2 meteors, knock-up 0.9. M: **crater**: r2 of molten ground for 3 s (two mini-Inferno pulses of 1 + ignite) [M]. S: comet from 24 blocks, a reticle that *closes*, dust ring and a cracked fire seal (`flatSigil CRACKED`, 60 ticks) at impact; `sky_roar` (falling whistle, then thud) |
| inferno | per pulse: two ground rings, up to eight flame tongues, up to 16 flame flings, smoke; `GENERIC_BURN` 0.7 once a second | generic and quiet; the ring is a circle but the hit area is a box | M: **fire curtain**: arrows and small fireballs that enter the field burn up (checked each tick); hit area becomes a true circle [M]. S: a standing ring of low flames for the whole duration and a rising heat shimmer; `field_roar` (a looped low bed) with a soft `fwoosh` per pulse |
| primer | pink fuse ring closing, glow and smoke; every 0.3 s a ring, spark and `TRIPWIRE_CLICK` at a constant 2.0; then the full explosion (x8) | good idea (fuse), colour off-element (pink), sound never changes | **Fix**: per-enemy cap across one cast's bombs [S]. M: a primed creature that dies detonates at once at full power (a reason to time your allies' kills) [M]. S: fuse glow pink to orange to white, click pitch climbing, then **0.15 s of silence** before the boom; `fuse_hiss` + rising `fuse_tick` |
| kindling | one flame tongue and one ember in a ring over the head per stack; `FIRECHARGE_USE` at 1.2 + 0.15 x stacks; the 5th: fire impact, six tongues, shockwave, explosion sound | **good**: the best cue in the group (the stacks are visible and audible) | N: burst 10 to 14; burst is a sphere, not a cube. M: **chain**: everyone in the burst is left at 2 stacks, so a pack detonates in turn [S]. S: keep the pitch ladder but make it a pentatonic scale that resolves into a bell chord on the 5th; `fire_flick` per stack, `blast_crack` (high) on the fifth |
| firestorm | swirl, ten flame tongues, gust ring, ten embers, flash, per target (up to eight targets) and a lick per neighbour; `FIRECHARGE_USE` 0.8, `impact_wind` 0.5 | mismatch: heavy for 10 damage; the same tongues as Fire | N: 4 to 5 damage. M: **contagion**: neighbours within 2 blocks ignite 5 s (+2 damage) at once, and their neighbours 1.5 s later (two waves, depth 2) [M]. S: arcs of flame leaping creature to creature (reuse Conflagration's `flareUp` arch); light visuals for non-primary targets; `fire_whump` + `flick` per hop |
| steam | shatter ring, heat flare, nine steam clouds, a white ring; `FIRE_EXTINGUISH` 1.0 at 0.9 | good look; the sound is the game's "fizzle" cue (Charging, Warp, Hellmouth fizzles) | M: reuse the world steam cloud (`WorldMagic.steam`): a 4 s, r2.5 cloud at the target, enemies inside are blinded and marked **Wet** (dulls their fire, primes Conduct and Flash Freeze), mobs lose target; damage 4 to 5 [M]. S: a rolling white dome; `steam_hiss` |
| sunscorch | 12-block white-gold beam (0.35 wide by day), star seal, fire impact; `BEACON_ACTIVATE` 1.8 + `impact_fire` | **good**, distinct; the beam is big for 8 damage but only by day | M: **dazzle**: the target is Glowing 6 s and, by day, Blinded 1.5 s (mobs lose the caster); bright block light (12+) counts as dusk (x1.25) at night and underground [S]. S: keep the beam; add `sun_beam` (a glass tone climbing the pentatonic scale into a crackle) |
| soulfire | blue tongues (five, then three), soul flame, a soul wisp; `SOUL_ESCAPE`, then `FIRE_AMBIENT` | good colour and silhouette, generic sound | **Fix + M**: make it ignore wetness and fire-immunity (magic source, fire element): blue flame the fireproof cannot shrug off, and Fireward does not stop [S]. S: a hollow whistle glide; `soul_whisper` |
| blazecall | three vertical rays from 6 blocks up at 120 degrees, fire impact each; `BLAZE_SHOOT` at 1.0, 1.1, 1.2 | generic: the same "ray from above" as Rain, Meteor, Skyburst | N: 2 to 3 damage. M: **stagger**: each fireball is a 5-tick hit-stun (`Spirits.hold`), three of them pin a target for about 0.75 s [S]. S: spherical fireballs (`ElementFx.orb` + smoke trail) arcing in from the caster's side in a fan, small burst on impact; `fireball_whoosh` (three rising steps) |
| cinderbrand | star seal (20 ticks), heat flare, embers; `FIRECHARGE_USE` 1.3 | mismatch: the cue vanishes after one second, the brand lasts six; unreadable to allies | M: the brand also adds 0.5 a second while the target burns (so the text is true) [S]. S: a persistent brand glyph over the head (a sigil layer refreshed each second, fading in the last two); `brand_hiss` (hot iron on water) |
| ashen_veil | ash swirl and `ASH` particles; `FIRE_AMBIENT` 0.6 + `cast_fire`; a ray and flame on retaliation | generic; retaliation is barely staged | M: retaliation also throws an ash puff: the attacker is Blinded 1 s (once per 3 s each) [S]. S: a grey ash cloak drifting round the body, an ash burst and cinder on each hit; `ash_puff` |
| cinderheart | per second: a 4-block ground ring and up to eight tongues, sun seal and heat flare on the first; `BLAZE_AMBIENT` + `cast_fire` | good and big enough for Tier IV | **Fix**: key it to the target (a recast refreshes, never stacks) and add a 24 s lockout after it ends ("the coals must cool") [S]. S: keep; add a slow orbit of embers and a furnace thrum on each pulse; `coals_thrum` |
| searing_edge | embers and a ring at the hand; a smoulder every 6 ticks; per hit: embers + flash; `FIRECHARGE_USE` 1.4 / 1.2 | generic hit (same as Ember's) | S: a **blade sear**: hot-iron quench per hit (`blade_sear`) and a short ember ribbon along the swing (`Light.slash`, small) when a seared blow lands |
| fireward | flame tongues curling in, two folding rings; `FIRE_EXTINGUISH` 0.6 at 1.4 | fair look; sound collides with "fizzle" | M (optional): **heat sink**: each blocked fire hit gives +1 absorption (cap 4) [M]. S: a warm amber shell (rings folding in, a thin ember halo that fades); `ward_hush` (a soft chord) |
| smelt | heat flash, ring, 12 flames, lava, smoke; `FIRECHARGE_USE` + `FURNACE_FIRE_CRACKLE` | **good**: the furnace crackle is unique | M: with Widen it also smelts loose items within 3 blocks (raw ore, sand, food) [S]. S: keep; lower the shared `FIRECHARGE_USE` |
| hellmouth | dark disc, red-hot cracks, violet ring, black-and-violet rim flame, implode, per quarter second flames and embers, cave-in with slashes; `RESPAWN_ANCHOR_DEPLETE`, `cast_void`, `impact_void` | **good**: black fire is a strong signature | keep; S: `pit_drone` (a low fire-and-void drone through its 3 s, instead of three vanilla sounds) |
| starfire | star seal, gold and pink motes, chime; `AMETHYST_BLOCK_CHIME` | **good**: distinct motion | keep; S: `starfire_chime` (a three-note pentatonic bell run, one note per mote landing) |
| everburn | a clock face inside flames, hands sweeping on each extra burn, golden ticks; `NOTE_BLOCK_HAT`, `BELL_BLOCK` | **good** | N: the extra burn 1 to 1.5 a second (20 total, 1.25/mana). S: `tick_flame` (clock tick over a crackle) |
| conflagration | up to three sun seals and 3.2-block pillars, rings, heat flares; arches to each flared enemy; `BLAZE_SHOOT` + `GHAST_SHOOT` + `impact_fire` | **good** (big, and the arches are unique) | N: base burn 8 to 6 s; each flare +1 per other burning enemy near it (max +4) [S]. S: `field_roar` under the pillars |
| phoenix_pyre | **wings** (six feathers a side, flame with green quills), seal, rings, embers, petals; `ENDER_DRAGON_FLAP`, `FIRECHARGE_USE`, `impact_life` | **good**: the best silhouette in the group | M: **rebirth flare**: the first time the wreathed ally falls under 35% health the pyre flares once: +6 health and a 4-damage burst r3 [M]. S: `phoenix_cry` (a short rising fluty glide) on the rise and the flare |
| bloodboil | crimson cut + heat flash, bubbles rising, lava pops; per answer a cut, tongues, drips; `LAVA_POP`, `BUBBLE_POP`, `impact_blood` | **good** | keep; S: `lava_pop` variants that rise with the answer count (already 0.9 + 0.1 x n) |
| rend | two crimson slashes crossing the chest, iron nuggets; `ITEM_BREAK` | generic: the same slash as Bleed | M: **tears natural resistances** too: for 10 s the target takes what it resists at full strength (immunities hold): the fix for the Nether and for skeletons vs blood [S]. S: crimson fissure lines on the chest that stay for 2 s; `armor_rend` (metal tear, rivet pings) |
| leech | eight crimson motes stream from target to caster, dust, glow; `GENERIC_DRINK` 0.5 at 0.7 | generic (Lifesteal uses the same call) | M: overhealing becomes Absorption (cap 4): usable at full health [S]. S: one thin bright thread from target to the caster's hand and a gulp; `blood_sip` (short) |
| bleed | one crimson slash, then drops falling per wound; `PLAYER_ATTACK_SWEEP` 1.5 | generic: the same cut and drip as Gash | M: the wound tears when the target moves: ticks x1.5 while it moves [S]. S: the cut visibly widens with each tick; a wet drip sound per tick; `blood_slice_light` |
| gash | ragged crimson slash and drops; `PLAYER_ATTACK_CRIT` 0.7 | generic | M: the wound also bleeds 0.6 + 1% of max health a second for 8 s (an anti-tank wound; Cleave is the burst) [S]. S: a black-red *stitched seam* glyph that keeps weeping, not a crescent; `gash_tear` (wet rip) |
| dismantle | three hairline slashes (white over crimson), random tilts, drips; `PLAYER_ATTACK_SWEEP` 1.6 + 0.15 per slash | **good** ("unseen" feel) | M: **backstab**: if the target is not facing you the third slash is x2 and the slashes appear behind it (pairs with Shadowstep) [S]. S: keep; `blood_slice_triple` (three rising 'shk') |
| cleave | one huge diagonal crimson cut, heartbeat ring, flash, sweep particle, drips; `SWEEP` 0.6 + `CRIT` 0.7 | **good**, right scale | N: 6 + 10% of max health (cap 20 more): 8 / 16 / 26. M: **sweep**: enemies within 2.5 blocks of the target take 50% (max 3) [S]. S: `blood_slice_heavy` (a low thud under the cut) |
| overdrive | heartbeat ring, ground ring, four crimson tongues, flash; `WARDEN_HEARTBEAT` 1.4; each drain a ring, drips, damage indicator | good idea (heartbeat), shared with five blood runes | M: **pain fuels**: Strength III under half health, IV under a quarter, re-checked at each drain [S]. S: the heartbeat quickens with each drain and the screen edge pulses red (`ScreenFx.tint`); `heart_race` |
| warcry | two pulsing crimson ground rings, `RAID_HORN` 1.1 + `cast_blood`; allies get a ring and an angry-villager puff | **good**: the horn is distinct | M: **bloodlust**: while buffed, each kill a buffed creature makes heals it 1 (12 s) [M]. S: keep; add a low drum under the horn (`war_horn`) |
| blood_moss | crimson spores, drips and a thin thread to the caster each second; `MOSS_PLACE` | good, distinct | M: **team siphon**: the heal goes to the most wounded ally within 10 blocks (or you) [S]. S: a mossy patch on the ground under the target (a flat sigil, 6 s) and threads to the recipient; `moss_drink` |
| parasite | a seeking green-and-crimson mote, blood pulse, slime; `SILVERFISH_AMBIENT` + `HONEY_BLOCK_SLIDE`, drain pulse, leap streak | **good** | keep; S: `parasite_skitter` (chitter over a wet slide) |
| lifesteal | pulse, drips, blood stream, implode; `GENERIC_DRINK` 0.7 at 0.6 | generic (Leech's cue, lower) | M: **siphon mark**: for 6 s, all damage the target takes from anyone heals you 25% [M]. S: a red tether ring on the target's chest that beats; `siphon_hum` (long) |
| crimson_mist | swirling haze billows, heartbeat pulses through the ground, drips; `WIND_CHARGE_BURST` + `WARDEN_HEARTBEAT` | good atmosphere, low impact | N: 1 to 2 damage a second, heal 1 to 1.5 a second. M: **veil**: allies inside are hidden from monsters targeting them from more than 4 blocks (targets cleared each pulse, as Veil does) [S]. S: thicker fog so it blocks sight; `mist_breath` (low wind) with a *slow* heartbeat |
| heartstopper | crimson lightning into the chest, heartbeat rings; a skip: a lightning ring clenching, a dark flash, shake 0.35; `LIGHTNING_BOLT_IMPACT` + `WARDEN_HEARTBEAT` | **good** | M: **escalating skips**: 0.25 / 0.5 / 1.5 s (2.25 s, ending in a hard stop) [S]. S: the heartbeat *slows* toward silence, then one hard thump on the stop; `heart_stop` |
| sanguine_rite | a three-layer blood sigil under the caster, heartbeat, `PLAYER_HURT`; a crimson lance over a dark stroke, sigil and crossed cuts at the target; `TRIDENT_THROW/HIT` + `impact_blood` | **good**: the best blood presentation | N: price +1 health per Amplify/Overcharge and per 4 victims, so power per health stays level [S]. S: keep; `rite_blade` (a low chant note, a knife scrape, one drip) |
| hemomancy | blood runes circling the caster (a ring, a pale band, comets), threads to the target (more with less health), star seal; `WARDEN_HEARTBEAT` rising with the bonus | **good**, and it scales with state | N: 4 + 1 per 1.5 health missing (up to +8 = 12) and heal 25% of the damage under half health [S]. S: heartbeat tempo follows missing health; `heart_tempo` |
| transfusion | a cut and heartbeat on the giver, a blood-to-green stream, bloom; `WARDEN_HEARTBEAT`, `PLAYER_HURT`, chime | **good** | N: give 4, heal 12 (3x); cleanse one debuff; overflow becomes Absorption [S]. S: `blood_gift` (a drip resolving into a chime) |
| blood_thread | a crimson sagging thread between members redrawn every 0.5 s; `CHAIN_PLACE` at cast; **each shared hit draws Shackle's grey steel chain** (`TechniqueVfx.chain`) | mismatch: the shared-hit cue is the wrong colour and unbounded (a hit x members) | N: share 40%, at most 4 members, at most 3 shares a second [S]. S: a crimson pulse running along the thread on each share; `thread_twang` (a plucked taut string, pitch by member count) |
| seethe | water rings, bubbles, steam and a burst cloud with flames; bubble-column sounds, `GENERIC_EXTINGUISH_FIRE`, `impact_fire` | **good** | keep |
| skyburst | gust ring and swirl, ember trail, a burst with ten flame slashes and seven fire streaks raining down; `FIREWORK_ROCKET_LARGE_BLAST` + `GENERIC_EXPLODE` | **good**: an aerial silhouette | keep; use as the model for the per-enemy cap |

## 3. Clusters and how each is broken up

### 3.1 Fire

**Ignite one target** (Ember, Fire, Sunscorch, Firestorm, Blazecall, Kindling, Cinderbrand, Everburn). All are
"N damage and set alight". Distinct verbs, in order of tier:

| Rune | New verb | What makes it different in play |
|---|---|---|
| Ember | **Feed** | the only rune that tops up an existing burn; the cheap keeper of your burning marks |
| Fire | **Ignite** | the yardstick: hit plus a long burn, nothing else |
| Blazecall | **Stagger** | three hits that pin the target; the fire rune for keeping things off you |
| Kindling (innate) | **Stack** | count to five, then everything around it goes up; chains through packs |
| Cinderbrand | **Brand** | a visible mark; fire hits it x1.5 and the burn 0.5 a second hotter |
| Sunscorch | **Dazzle** | the daylight beam: strongest and blinding by day; bright light counts as dusk at night |
| Firestorm | **Contagion** | the fire spreads through a crowd in two waves |
| Everburn | **Time burn** | the fast double burn that relights once; 20 damage, no hit |

**Explosions** (Explode, Meteor, Primer, Flashfire, Skyburst):

| Rune | New verb |
|---|---|
| Explode | the instant punch: throws and primes Wildfire; one blast per cluster |
| Meteor | the sky falls: 1.2 s telegraph, big hit, a molten crater |
| Primer | a walking bomb: follows its target, and it goes off early if the target dies |
| Flashfire | the flash: light, and a thaw for allies |
| Skyburst | launch, then air-burst rain (unchanged: the model) |

**Fields and auras** (Inferno, Hellmouth, Cinderheart, Phoenix Pyre, Crimson Mist, Seethe): Inferno becomes
the *fire curtain* (eats arrows); Hellmouth stays the pit (drag then collapse); Cinderheart the burning
heart with a lockout; Phoenix Pyre gains *rebirth*; Crimson Mist becomes the *veil*; Seethe stays the trap.

**Defence** (Fireward, Ashen Veil, Searing Edge): Fireward the heat sink, Ashen Veil the blinding ash, Searing
Edge the weapon.

**Cross-element:** fire's Tier I poke (Ember) is one of nine "one-target 3 to 7 damage" Tier I runes
(Harm, Icicle, Pelt, Windcut, Umbra, Shock, Countdown, Leech, Ember); its "pit that drags then slams" is one of
six (Gravity Well, Sinkhole, Riftcall, Singularity, Vortex, Hellmouth); its delayed sky strike shares a slot
with Cometfall (which needs Meteor), Starfall and Thunderhead.

### 3.2 Blood

**Drain** (Leech, Lifesteal, Blood Moss, Parasite; Thirst is the modifier form):

| Rune | New verb | Difference |
|---|---|---|
| Leech | **Sip** | instant, exact, to you; overhealing becomes Absorption |
| Blood Moss | **Team siphon** | slow; the heal goes to the most wounded ally |
| Lifesteal | **Siphon mark** | a 6 s mark that turns everyone's damage on the target into your healing |
| Parasite | **Host-hopper** | burrows, poisons, leaps on death (unchanged) |

**Wounds** (Bleed, Gash, Dismantle, Cleave, Rend, Crimson Mist):

| Rune | New verb |
|---|---|
| Bleed | a wound that punishes movement; arms Rupture |
| Gash | the anti-tank wound: percentage of max health, and no healing |
| Dismantle | the flurry and the backstab |
| Cleave | the axe sweep: proportional cut that splashes |
| Rend | tears armour *and* natural resistances (the team debuff) |
| Crimson Mist | the veil: bleeds foes, mends and hides friends |

**Health as a price** (Sanguine Rite, Hemomancy, Transfusion, Overdrive): Sanguine Rite pays for power (price
scales with your modifiers); Hemomancy takes power from what you have lost; Transfusion moves health to a friend at
3 to 1; Overdrive pays for a surge and gets stronger as you weaken. Blood Price (the modifier) stays the spell-wide
version. The four together are a real build: pay (Rite) then spend (Hemomancy), heal a friend (Transfusion).

**Reactive** (Bloodboil, Blood Thread, Conflagration, Cinderbrand): each waits for damage from something else;
the price of the group is that they do nothing alone.

**Control** (Heartstopper, Gash, Rend): Heartstopper the escalating stop, Gash the heal-lock, Rend the debuff.

## 4. Cross-element and caster-feel observations for the whole mod

1. **The caster's cue is per element and per shape, never per effect.** `Vfx.castCircle` plays the same circle
   (with each rune's emblem: unique per spell, but tiny), `CIRCLE_OPEN` at 0.35 and the element's `cast_<x>` at 0.55
   for every effect; the pose (`PlayerModelMixin`) depends only on the shape; charging and its hum are spell-wide.
   Casting Ember, Fire, Explode and Inferno therefore feels the same for the first 200 ms; Leech, Bleed and Cleave
   likewise (`cast_blood`'s lub-dub). Suggested global fix: an **effect accent layer** on `castCircle` (a glyph
   rising from the circle in the effect's own emblem shape, the hand spark in the effect's secondary colour, and a
   pitch or octave layer by tier: 0.5 under Tier III and IV, since the art guide keeps 0.5 and 2.0 in key).
2. **Two shared impact helpers carry every hit.** `Vfx.touched` (a glow and three motes on every creature any
   effect touches) and `Vfx.impact` (a flare, two rings, sparks and `impact_<element>`) are the same for all
   runes of an element; `ElementFx.<element>Impact` and the element themes do the rest. That is the
   "element visual language" of `docs/DESIGN.md`; it is what the request wants to move past. A workable rule:
   keep the palette and motif per element, and give every rune its own *silhouette and motion* on top.
3. **The custom sound set is 20 element sounds, used at the shape layer.** Of the effect Vfx that play sound,
   the vanilla ones dominate: `AMETHYST_BLOCK_CHIME`/`RESONATE` 76 calls, `FIRECHARGE_USE` 25,
   `BREEZE_WIND_CHARGE_BURST` 20, `GLASS_BREAK` 18, `LIGHTNING_BOLT_IMPACT` 17, `BELL_BLOCK` 17,
   `GENERIC_EXPLODE` 15, `WARDEN_HEARTBEAT` 13. Outside the element theme table, `impact_fire` is called by three
   effects and `cast_fire` by two; the custom sounds reach most effects only through `castCircle` and `Vfx.impact`.
4. **`FIRE_EXTINGUISH` means "it fizzled"** (a charge held too long, Warp Step, a full Hellmouth limit) yet is the
   success sound of Fireward, Steam and Phoenix Pyre's fade. Pick one meaning.
5. **Multiplicity is unguarded outside the newer runes.** Explode 4 blasts, Meteor 4, Primer 8, Lightning 8
   strikes, and so on hit the same enemy repeatedly, while Skyburst (`blasted`), Magma and Tempest
   (`FusedEffects.unstacked`) cap it. Apply the cap to every "one per target" area rune.
6. **Areas are boxes, drawn as circles.** Inferno, Kindling's burst (and the thunder runes, not audited) use
   `AABB.inflate` with no distance check, so a "4-block" area reaches 5.7 blocks on the diagonal while its ring
   shows a circle.
7. **Affinities are lopsided.** Weak to fire: stray, snow golem, polar bear, creaking (4). Resist fire: 13 entries.
   Weak to blood: none; resist blood: skeletons, wither, iron/copper/snow golems, blaze, breeze, vex (8). No climate
   shift for blood; fire has four. `Rend` (resistance-tearing) and one new blood weakness (illagers, ravager) would
   give blood a "when to reach for it".
8. **DoT-heavy runes ignore Amplify.** Every ignite rune's burn ignores power; Extend is the best fire
   modifier. If damage over time is meant to scale, do it once at the burn (a per-second bonus tick).
9. **Hidden strengths.** Linger (x3 hits for x1.8 cost) only fits runes with the trait; passive Overdrive is
   Strength II + Speed II + Haste II for 1.2 mana a second; passive Orbit + Dismantle is 9 damage a second per
   creature for 3.5 mana a second (with Aftershock's 10 for 3.2, the top of the aura options).
10. **Tier I sameness across the mod**: "every element's Tier I damage rune is a one-target 3 to 5 damage poke".
    Give each a different *delivery* (feed, chill-and-crack, knock, cut, curse...), not a different colour.
11. **Feedback on a hit** is a screen punch only at 8 damage or more (Explode, Meteor, day Sunscorch, Sanguine Rite,
    heavy Cleave) and floating "Weak!/Resisted" words; there is no per-hit sound sting or damage number. A short
    pitched sting for ordinary hits (the `blade_sear`/`blood_slice` family) would carry the difference between a
    burn tick, a bleed tick and a fireball.
12. **Fire and blood runes elsewhere.** Runes outside this group that lean on fire or blood and should be audited
    with them: Magma (earth, "the ground burns"), Plasma (storm), Blackflame (void), Cometfall (arcane, sets
    alight), Lightning (ignites), Bonespur and Razorgale (bleeding), Devour, Reckoning, Frostbite.

### 4.1 The caster's first 200 ms, by family (what feels identical today)

| Family | What the caster gets in the first 200 ms | Feels identical to | Proposed difference |
|---|---|---|---|
| Any fire effect (Ember, Fire, Flashfire, Explode, Meteor, Inferno, Primer, Firestorm, Steam...) | the spell's circle (rune emblems), `CIRCLE_OPEN` 0.35, `cast_fire` 0.55, the shape's own sound and pose | each other, except through the shape | effect accent layer: an emblem glyph rising from the circle, hand spark in the effect's secondary colour, an octave layer by tier |
| Any blood effect (Leech, Bleed, Gash, Rend, Cleave, Dismantle, Lifesteal, Crimson Mist...) | the same, with `cast_blood`'s lub-dub | each other | the same layer; the heartbeat kept only for the runes that pay or spend health |
| Runes that cost health (Sanguine Rite, Transfusion, Overdrive's drain, Blood Price) | a `PLAYER_HURT` grunt and a heartbeat (Transfusion, Sanguine Rite); Overdrive's drain a small ring and drips; no screen feedback | one another | a red screen-edge flash (`ScreenFx.tint`, 6 ticks) and one shared "price paid" sound, so the cost is felt |
| Ordinary hits (a burn tick, a bleed tick, a fireball) | `Vfx.touched` (glow + three motes) and the shape's `impact_<element>`; no hit sound, no number | each other | the per-family sting (`blade_sear`, `blood_slice`, `fire_flick`); tick sounds only for the caster |
| Heavy hits (8 damage or more: Explode, Meteor, day Sunscorch, Sanguine Rite, big Cleave) | a screen punch | each other | keep; vary punch length by rune (a sharp kick for Rite, a long shove for Meteor) |
| Charged, Rhythm, Twin Star | spell-wide: rising hum, FOV kick, gold ring | all spells | none needed |

### 4.2 Timing and scale, at a glance

- **Anticipation exists** for Primer (a 2 s fuse), Meteor (0.6 s reticle and fall), Blazecall (three balls over 1 s),
  Seethe (2 s bubble), Skyburst (fling and rise), Starfire (mote flight), Hellmouth (the pit opening), Sanguine Rite
  (the sigil, then the lance). **None** for Ember, Fire, Flashfire, Explode, Sunscorch, Cleave, Leech, Rend, Bleed, Gash,
  Dismantle (0.2 s), which is right for pokes but leaves Explode and Cleave, the two heavy instants, without a wind-up.
- **Aftermath exists** only for fields and auras (Inferno, Hellmouth, Cinderheart, Phoenix Pyre, Crimson Mist), Skyburst
  (its rain of fire) and Everburn (the clock stopping). Explode and Meteor leave nothing but smoke; the Meteor crater would be the
  first "scar" in the group.
- **Scale versus power:** Flashfire is over-scaled (about 34 packets for 4 damage); Explode is correctly scaled once but
  multiplies by 4 to 8 in crowds; Ember and Fire are nearly the same size for 6 versus 11 damage; Cinderbrand's cue lasts
  1 s for a 6 s effect (under-scaled in time); Sunscorch's 12-block beam is big for 8 damage but only by day; Cinderheart,
  Phoenix Pyre and Sanguine Rite are scaled well.
- **Laggy or spammy:** Primer with eight targets (eight explosions and eight fuse loops), Explode on a crowd (four),
  Firestorm (ten tongues, a swirl and ten embers per target, up to eight targets), Blood Thread (a chain per shared hit),
  Inferno's per-pulse flames (about 25 packets a second for four seconds, fine alone, heavy in a Zone).

## 5. Sound needs (a shared kit; each is built in `tools/sound_art.py`)

All tonal parts on the D pentatonic scale; the existing helpers cover everything (`thump`, `grains`,
`moving_band`, `decay`, `bell`, `glass`, `sparkle`, `reverb`, `saturate`). Durations are targets.

| Sound | Brief (verb, element, character) | Used by |
|---|---|---|
| `fire_flick` | fire, a single ignition: a dry 'tik' and a short 'ssst', 0.25 s, pitch steps up the scale | Ember, Kindling stacks, Everburn tick, Searing Edge hit |
| `fire_whump` | fire, catching: a soft low 'fwoomp' (lowpassed noise near 400 Hz, a 65 to 90 Hz thump, ember grains), 0.6 s | Fire, Firestorm, Everburn |
| `blast_crack` | fire, a detonation: a 2 ms crack (1 to 4 kHz), a low boom (90 to 40 Hz) and a debris tail, 0.8 s; a low variant and a high variant | Explode, Primer, Meteor impact, Kindling's fifth |
| `fuse_hiss` / `fuse_tick` | fire, a lit fuse: a bright crackle with a rising band, loopable 0.5 s; a 40 ms tick | Primer |
| `sky_roar` | fire, falling: a whistle that drops in pitch then a thud, 1.2 s, ending on silence | Meteor |
| `flare_flash` | fire, a flashbulb: a highpassed noise burst and a high ping, 0.3 s | Flashfire, Sunscorch, Cinderheart start |
| `field_roar` | fire, a burning field: a low roar bed, brown noise 100 to 400 Hz with slow flutter and crackle, loopable 1 s | Inferno, Conflagration, Cinderheart, Hellmouth |
| `soul_whisper` | fire (soul), hollow: a sine glide D5 to A4 with breath noise 1.5 to 3 kHz, 0.9 s | Soulfire |
| `blade_sear` | fire, hot metal: a ting (3 to 6 kHz partials) and a quench hiss, 0.4 s | Searing Edge; a lower variant for Smelt and Cinderbrand |
| `fireball_whoosh` | fire, a launch: a whoosh sweeping 300 to 1500 Hz and a low 'thup', 0.5 s, played three times up the scale | Blazecall |
| `ward_hush` | fire, calm: a soft D-A-E pad with a cool noise breath, 0.6 s | Fireward, Ashen Veil, Phoenix Pyre fade |
| `steam_hiss` | fire/water, pressurised: a highpassed 'tsss' with a slow decay and bubbling pops, 1.2 s | Steam, Seethe burst |
| `phoenix_cry` | fire/life, a short rising fluty glide D5 to A5 with vibrato over embers, 0.8 s | Phoenix Pyre |
| `coals_thrum` | fire, a furnace pulse: 55 to 80 Hz saturated sine with crackle, 0.5 s | Cinderheart |
| `sun_beam` | fire, a beam: a glass tone climbing the scale into a crackle, 0.8 s | Sunscorch |
| `blood_slice` | blood, a wet cut 'shk': band noise sweeping 1.2 k to 400 Hz and a small splash. Variants: **light** (0.15 s), **heavy** (0.35 s with a low thud), **triple** (three rising in 0.25 s), **ragged** (a double rip) | Bleed, Cleave, Dismantle, Gash |
| `armor_rend` | blood, metal torn: a scrape and rivet pings, 0.35 s | Rend |
| `blood_sip` / `blood_drain` / `moss_drink` | blood, taking: a short gulp with a rising chime (0.3 s); a long swallowing glide into the body (0.9 s); a soft wet moss sip | Leech; Lifesteal, Blood Moss respectively |
| `parasite_skitter` | blood/life, burrowing: a chitter over a wet slide, 0.5 s | Parasite |
| `heart_race` | blood, a heart quickening: lub-dub at increasing tempo, 1 s | Overdrive |
| `heart_stop` | blood, a heart failing: beats slowing to silence, 0.15 s of nothing, one hard thump, 1.2 s | Heartstopper |
| `heart_tempo` | blood, a heart whose tempo follows a value (short loop, three speeds) | Hemomancy, Crimson Mist (slow) |
| `rite_blade` | blood, a pact: a low chant note (D2), a knife scrape, one drip, 0.7 s | Sanguine Rite |
| `blood_gift` | blood to life: a drip resolving into a warm chime, 0.7 s | Transfusion |
| `thread_twang` | blood, a taut string plucked (a decaying partial series), 0.5 s, pitch by member count | Blood Thread |
| `war_horn` | blood, a low drum hit layered under the vanilla horn, 0.8 s | Warcry |
| `mist_breath` | blood/wind, a slow low wind, 1.5 s | Crimson Mist |
| `ash_puff` | fire, a soft dry 'pff' and cinder ticks, 0.3 s | Ashen Veil |
| `brand_hiss` | fire, hot iron on water: a short sizzle, 0.4 s | Cinderbrand |
| `pit_drone` / `starfire_chime` / `tick_flame` | fire+void low drone; a three-note bell run; a clock tick over a crackle | Hellmouth; Starfire; Everburn |

Thirty-five event names in all, counting variants (the `blood_slice` variants are one recipe; `fuse_hiss` and
`fuse_tick` one recipe). Ten of them serve two or more runes (`fire_flick`, `fire_whump`, `blast_crack`, `flare_flash`,
`field_roar`, `blade_sear`, `ward_hush`, `steam_hiss`, `blood_slice`, `heart_tempo`), and several would serve other
elements' runes too (`blast_crack`, `steam_hiss`, `heart_*`, `blood_slice`, `field_roar`).

**Priority.** P1 (twenty-one names) replaces a vanilla sound that many runes share, so that runes stop sounding alike:
`fire_flick`, `fire_whump`, `blast_crack`, `fuse_hiss`, `fuse_tick`, `sky_roar`, `flare_flash`, `field_roar`,
`blade_sear`, `fireball_whoosh`, `ward_hush`, `steam_hiss`, `blood_slice`, `armor_rend`, `blood_sip`, `blood_drain`,
`heart_race`, `heart_stop`, `heart_tempo`, `thread_twang`, `mist_breath`. P2 (fourteen) is polish for runes that are
already distinct: `soul_whisper`, `phoenix_cry`, `coals_thrum`, `sun_beam`, `moss_drink`, `parasite_skitter`,
`rite_blade`, `blood_gift`, `war_horn`, `ash_puff`, `brand_hiss`, `pit_drone`, `starfire_chime`, `tick_flame`.
Keep the vanilla `RAID_HORN` (Warcry), `FURNACE_FIRE_CRACKLE` (Smelt) and the silverfish/slime pair (Parasite): they
already read.

**Sound semantics to settle with the kit:** one cue for "fizzled" (`FIRE_EXTINGUISH` is used for it in three places and
as a success sound in three others), one for "a health price was paid" (`WARDEN_HEARTBEAT` plus `PLAYER_HURT` is used
by Transfusion and Sanguine Rite; give it its own event and let Overdrive, Blood Price and Hemomancy share it).

## 6. Proposed implementation order, effort and risks

### 6.1 Order

1. **Fixes and plain number changes first, about a day.**
   - Cinderheart refresh + lockout [S].
   - Explode/Meteor/Primer: cluster the blasts and cap per enemy [S].
   - Soulfire ignores wetness and fire-immunity [S]; Cinderbrand burn tick so the text is true [S].
   - Blood Thread visuals and cap [S]; Sanguine Rite price scaling [S].
   - Numbers only: Transfusion 3x, Hemomancy 4 + 1 per 1.5 missing, Cleave 6 + 10%, Everburn 1.5 extra, Crimson Mist 2 / 1.5, Kindling burst 14, Meteor 14 and delay, Blazecall 3 dmg [S each].
2. **Mechanic twists (about half a day each), in this order of payoff:** Rend resistance-tearing [S]; Steam cloud [M];
   Firestorm contagion [M]; Ember feed [S]; Kindling chain [S]; Flashfire thaw [S]; Meteor crater [M]; Primer death-detonation
   [M]; Overdrive pain fuels [S]; Dismantle backstab [S]; Bleed movement [S]; Gash %HP [S]; Cleave sweep [S]; Leech
   absorption [S]; Lifesteal siphon mark [M]; Blood Moss team siphon [S]; Heartstopper escalation [S]; Crimson Mist veil
   [S]; Phoenix rebirth [M]; Ashen Veil blind [S]; Warcry bloodlust [M]; Sunscorch dazzle [S]; Blazecall stagger [S]; Inferno
   curtain [M]; Fireward heat sink [M] (optional).
3. **Presentation and sound:** build the kit (section 5) first, then replace the cues in this order: Fire family
   (Ember, Fire, Explode, Meteor, Primer, Flashfire, Inferno), the wound family (Bleed, Gash, Rend, Cleave, Dismantle), the
   drain family, then the rest. Add the effect accent layer to `castCircle` [M].

### 6.2 Risks and what to update

- **Balance.** Every number change moves a ratio: re-run the arithmetic in Appendix A. The most sensitive:
  Explode/Primer crowd damage falls by 3 to 6 times (intended), Cinderheart uptime falls to about a third, Everburn and
  Transfusion rise.
- **Tests that pin numbers or text:** `FusedFlameRulesTest`, `FusedLifeRulesTest`, `FusedVoidRulesTest`
  (hemomancy bonus), `FusedStormNumbersTest` (heartstopper), `SignatureRulesTest`, `ExplorerNumbersTest` (Soulfire
  refund), `ReactionRulesTest`, `ExpansionRunesTest`, `CraftedRunesTest`; gametests `WildercordFusedFlameTest`,
  `WildercordFusedLifeTest`, `WildercordFusedStormTest`, `WildercordFusedVoidTest`, `WildercordSignatureFusionTest`,
  `WildercordNewRunesTest`, `WildercordNewRunes2Test`, `WildercordAffinitiesTest` (Rend, fire affinities),
  `WildercordFeatureTour` (Cleave, Kindling, Leech, Flashfire).
- **Text that describes a rune** (all must change together): `Runes.java` descriptions, `lang/en_us.json`
  (`rune.wildercord.<id>.desc`), `docs/DESIGN.md` rune tables, `docs/RECIPES.md` fused tables, the generated wiki pages
  (`wiki/runes/effects/fire.md`, `wiki/runes/effects/blood.md`, `wiki/runes/fused.md`, `wiki/runes/innate.md`,
  `wiki/runes/world.md`; regenerate with `tools/wiki.py`), and
  `CHANGELOG.md`. `tools/generate_assets.py` writes the creature affinity tags if a blood weakness is added.
- **Working-tree overlap.** At the start of this audit `main`'s working tree had uncommitted edits to `Passives.java`,
  `HeartAndPassivesTest`, `ExpansionRunesTest`, `CHANGELOG.md`, `docs/DESIGN.md` and the passives/cord wiki pages;
  Overdrive and Searing Edge are passive-eligible, so coordinate before touching them.
- **Passives.** Overdrive's pain-fuel and Searing Edge's ribbon must stay quiet when a passive renews
  (`Fx.muted`); none of the proposals adds a per-tick emitter.
- **Particle budget.** The proposals reduce packets (blast clustering, lighter non-primary Firestorm targets, no smoke on
  Flashfire); the only additions are one-per-second (Inferno ring, Cinderbrand glyph, Blood Thread pulse).

## 7. Broken, or not doing what it says (fix regardless)

1. **Cinderheart stacks with itself** (`ExplorerEffects.cinderheart` uses `repeat` with no `Linger` key): each cast adds a
   full 12-pulse aura. The rest of the runes of the world (Soulfire, Blood Moss, Mire, Shulkershell...) and the fused and
   signature runes key what lasts to its creature and refresh it, as their own class notes say.
2. **Explode, Meteor and Primer multiply damage on clustered enemies** (one blast per harmed target, up to 4/4/8, no
   per-enemy cap), unlike Skyburst, Magma and Tempest. Also up to 8 simultaneous full explosions of visuals and sound.
3. **Soulfire** "burns on through water" but takes the 0.75x wet penalty and does nothing to fire-immune mobs (it uses
   a fire damage source).
4. **Cinderbrand** says "your fire spells burn it 50% hotter" but only spell damage is boosted, not the burn ticks that
   are most of a fire spell's damage.
5. **Blood Thread**: the shared-hit cue is Shackle's grey steel chain (`TechniqueVfx.chain`), and the member cap of 5
   applies only when one target is threaded to its neighbours; a Burst or Cone threads every enemy it hits, so damage
   multiplies by 1 + 0.5 x (members - 1) with no ceiling and each hit draws (members - 1) chains and hurts.
6. **Kindling's burst and Inferno's field are boxes** (cube of +/-3; a box of radius x radius, 2 up and down) though
   described and drawn as 3 and 4-block spheres/circles.
7. **Steam** is a fire-element hit that never triggers Shatter or Wildfire (it does not call `Reactions.fire`), and is
   dulled by wetness though it produces steam.
8. **Leech, Lifesteal, Blood Moss and Parasite heal only for lost *health***: damage taken by a target's Absorption heals
   nothing, while Soulfire's refund counts absorption. Pick one rule.
9. **Bleed adds eight more wounds with every cast**, while Soulfire, Blood Moss, Parasite, Everburn, Bloodboil and
   Heartstopper restart or refresh: pick one rule for wounds.
10. **`FIRE_EXTINGUISH` is both "fizzle" and the success sound of three runes** (section 4).

## 8. What could not be verified

- No number was measured in game: the arithmetic is from the effect code. The gametests and screenshots were not run.
  `fx_shots` frames of sunscorch, soulfire and comet were checked but show only whatever moment they caught.
- Whether vanilla `#bypasses_armor` covers `IN_FIRE`, `ON_FIRE`, `HOT_FLOOR` and `FREEZE` in 26.3 was not checked against the
  jar (only against how the mod talks about them: Plasma says lightning "half through the armour as usual"). The armour
  arguments for Rend and Cleave assume physical and lightning damage is reduced and magic is not.
- Passive Overdrive's real cost depends on hunger and saturation regeneration (not modelled): the 5 health per 10 s may be
  outpaced by natural regeneration for a well-fed player.
- Sounds were judged from the synthesis code and the vanilla events named, never listened to. Particle counts are
  estimates by counting calls.
- Which vanilla mobs are `fireImmune()` in 26.3 is taken from the mod's `resists_fire` tag and vanilla convention.

## Appendix A. Arithmetic

Conventions: base power 1.0; PvE; DoT fully delivered; one target unless stated; health at 1.5 mana.

### A.1 Peers

| id | T | mana | value | value/mana | note |
|---|---|---|---|---|---|
| harm | T1 | 8 | 7 | 0.88 | 7 magic |
| icicle | T1 | 6 | 5 | 0.83 | 4, or 6 on a slowed target |
| pelt | T1 | 5 | 4 | 0.80 | |
| windcut | T1 | 6 | 4 | 0.67 | + shove |
| shock | T1 | 7 | 7 | 1.00 | 4 + 3 on a second enemy (0.57 alone) |
| umbra | T1 | 6 | 6 | 1.00 | 4, or 8 in dim light |
| countdown | T1 | 7 | 6 | 0.86 | 1.5 s late |
| venom | T2 | 8 | 12 | 1.50 | Poison II 6 s about 10, + 2 |
| frost | T2 | 8 | 5 | 0.62 | + Slowness III and freeze (control not counted) |
| aftershock | T2 | 9 | 10 | 1.11 | half a second late |
| stalactite | T2 | 9 | 8.75 | 0.97 | 7, 10.5 on a bare head |
| fangs | T2 | 10 | 6 | 0.60 | |
| ripple | T2 | 10 | 6 | 0.60 | + heals a third |
| manaburn | T2 | 10 | 5 | 0.50 | + 4 against casters |
| smite | T3 | 16 | 10 | 0.62 | x2 undead |
| lightning | T3 | 20 | 12 | 0.60 | + stun + burn |
| blackspark | T3 | 16 | 11 | 0.69 | 8 x (0.75 + 0.25 x 2.5) |
| plasma | T3 | 20 | 9 | 0.45 | |
| entropy | T3 | 16 | 7.5 | 0.47 | 0.5 + 1 + 1.5 + 2 + 2.5 |
| resonant_shriek | T3 | 18 | 12 | 0.67 | 8 + echo 4 |
| tempest | T3 | 22 | 8 | 0.36 | + gale |
| devour | T3 | 16 | 5 | 0.31 | + 10 mana on a kill |
| sonic_boom | T4 | 35 | 16 | 0.46 | |
| hollow | T4 | 36 | 28 | 0.78 | 20 + 8 pulled in |
| dragon_breath | T4 | 30 | 25 | 0.83 | 5 x 5 s, one target |
| wither | T4 | 25 | 16 | 0.64 | Wither III 8 s |

Medians: T1 0.86 (0.67 to 1.0); T2 0.62 (0.5 to 1.5; DoT subgroup 1.25 to 1.5); T3 0.53 (0.31 to 0.69); T4 0.71 (0.46 to 0.83).

### A.2 The group

Shown as direct + over time, per mana of the effect alone (no shape). "eff. mana" adds health paid at 1.5 mana.

| id | T | mana | direct | over time | heal | health paid | eff. mana | total/eff. mana | vs band |
|---|---|---|---|---|---|---|---|---|---|
| ember | 1 | 6 | 3 | 3 | 0 | 0 | 6 | 1.00 | 1.18x T1 |
| fire | 2 | 8 | 5 | 6 | 0 | 0 | 8 | 1.38 | 1.0x T2 DoT |
| flashfire | 2 | 11 | 4 | 3 | 0 | 0 | 11 | 0.64 (per enemy) | 1.2x T2 area |
| explode | 3 | 18 | 12 | 0 | 0 | 0 | 18 | 0.67 | 1.3x T3 area |
| meteor | 3 | 24 | 10 | 4 | 0 | 0 | 24 | 0.58 | 0.87x Explode |
| inferno | 3 | 20 | 12 | 6 | 0 | 0 | 20 | 0.90 (per enemy who stays) | 1.8x |
| primer | 3 | 18 | 10 | 0 | 0 | 0 | 18 | 0.56 | 1.05x |
| firestorm | 3 | 18 | 4 | 6 | 0 | 0 | 18 | 0.56 | 0.4x Fire |
| steam | 3 | 14 | 4 | 0 | 0 | 0 | 14 | 0.29 | 0.55x |
| sunscorch | 3 | 16 | 8 | 5 | 0 | 0 | 16 | 0.81 (day 1.06) | 1.5x (2x) |
| soulfire | 3 | 16 | 0 | 15 | 0 | 0 | 16 | 0.94 (net of refund 11: 1.36) | 1.8x |
| blazecall | 2 | 11 | 6 | about 3.5 | 0 | 0 | 11 | 0.86 | 0.6x Fire |
| cinderbrand | 2 | 9 | 3 | 0 | 0 | 0 | 9 | 0.33 | setup |
| everburn | 3 | 16 | 0 | 16 | 0 | 0 | 16 | 1.00 | 0.66x Fire + Extend |
| conflagration | 3 | 20 | 3 | 10 | 0 | 0 | 20 | 0.65 | 1.0x |
| starfire | 3 | 16 | 10 | 4 | 0 | 0 | 16 | 0.88 | 1.66x |
| hellmouth | 3 | 20 | 4 | 8 | 0 | 0 | 20 | 0.60 (core only) | 1.1x |
| phoenix_pyre | 3 | 18 | 0 | 12 | 2.4 | 0 | 18 | 0.67 | 1.0x |
| cinderheart | 4 | 30 | 0 | 50 | 0 | 0 | 30 | 1.67 (per enemy) | 2.4x T4 |
| leech | 1 | 8 | 3 | 0 | 3 | 0 | 8 | 0.38 + heal | value 8.0 for 8 |
| bleed | 2 | 8 | 2 | 8 | 0 | 0 | 8 | 1.25 | 0.9x T2 DoT |
| gash | 2 | 9 | 3 | 0 | 0 | 0 | 9 | 0.33 | 0.5x |
| dismantle | 2 | 10 | 9 | 0 | 0 | 0 | 10 | 0.90 | 1.45x T2 instant |
| blood_moss | 2 | 10 | 0 | 6 | 6 | 0 | 10 | 0.60 + heal | value 13.4 for 10 |
| cleave (zombie / golem / wither) | 3 | 18 | 6.4 / 16 / 34 | 0 | 0 | 0 | 18 | 0.36 / 0.89 / 1.89 | 0.68x / 1.7x / 3.6x |
| lifesteal | 3 | 16 | 5 | 0 | 5 | 0 | 16 | 0.31 + heal | value 16.9 for 16 |
| parasite | 3 | 16 | 0 | 10.8 | 6 | 0 | 16 | 0.68 + heal | 1.3x |
| crimson_mist | 3 | 18 | 0 | 5 | 0 | 0 | 18 | 0.28 (+5 heal per ally) | 0.53x |
| heartstopper | 3 | 18 | 5 | 0 | 0 | 0 | 18 | 0.28 (+1.5 s stun) | 0.53x |
| sanguine_rite | 3 | 14 | 12 | 0 | 0 | 3 | 18.5 | 0.65 (raw 0.86) | 1.2x T3 |
| hemomancy (full health / 12 missing) | 3 | 16 | 4 / 10 | 0 | 0 | 0 | 16 | 0.25 / 0.62 | 0.47x / 1.2x |
| transfusion | 3 | 12 | 0 | 0 | 8 | 4 | 18 | 0.44 health/mana | 0.67x Heal (0.67) |
| bloodboil | 3 | 16 | 3 | 10 | 0 | 0 | 16 | 0.81 | 1.5x if five hurts |
| seethe | 3 | 16 | 4 | 4 | 0 | 0 | 16 | 0.50 (+ 2 s hold, 8 s across four) | 0.9x |
| skyburst | 3 | 20 | 5.6 | 6 | 0 | 0 | 20 | 0.58 single | 1.1x |

Derivations worth showing:

- **Fire**: 5 direct + 6 s x 1 = 11; 11/8 = 1.375. Extend: 5 + 12 = 17; 8 x 1.4 = 11.2 mana; 1.52. Amplify: 7.5 + 6 = 13.5;
  8 x 1.6 = 12.8; 1.05. Linger (x1.8, three landings, burn refreshed): 15 + 8 = 23 for 14.4; 1.60.
- **Explode crowds**: each blast hits every enemy within 3.5 blocks of its own centre (falloff 1.0 to 0.6, about 0.85 for a
  bunch). Four bunched enemies: 12 + 3 x (12 x 0.85) = 42.6, about 41; Burst 6 + 18 x 1.5 = 33 mana. Six enemies: the cap is 4
  blasts, so the same 41 each. Primer (10 within 3): eight bunched: 10 + 7 x 8.5 = 69.5, about 66 (the bombs also go off at
  slightly different moments).
- **Cinderheart stacking**: 30 mana per cast, cooldown 1.5 s, pulses every second for 12 s. At 5 mana/s regeneration one cast per
  6 s, overlap 12/6 = 2.0 auras (6 a second); at 8 mana/s one per 3.75 s, overlap 3.2 (9.6 a second). Single aura: 12 x 3 = 36,
  plus ignite 2 s refreshed each pulse (about 14 burn) = 50; 50/30 = 1.67; T4 band 0.71: 2.4x.
- **Blood Thread**: N members, each damage instance on one deals 0.5 x to each of the others: 1 + 0.5(N - 1) = 1.5, 2.0, 3.0 for
  2, 3, 5 (and 5.5 for 10).
- **Kindling**: 5 casts x 7 = 35 mana: 4 x 3 + (3 + 10) = 25 on the target; 10 to each other enemy in the burst: 35, 45, 55 for 2, 3, 4
  enemies (1.0, 1.29, 1.57 per mana).
- **Sanguine Rite**: 3 health x 1.5 = 4.5 mana; 14 + 4.5 = 18.5; 12/18.5 = 0.65 (raw 12/14 = 0.86).
- **Transfusion**: 12 mana + 4 health (6 mana) = 18 for 8 health = 0.44 health/mana; Heal 8/12 = 0.67.
- **Hemomancy**: 4 + floor(missing/2) capped at 6. Missing 0: 4; missing 12 (8 of 20): 4 + 6 = 10.
- **Leech**: 3/0.86 (T1 band) = 3.5 mana of damage + 3 health x 1.5 = 4.5: value 8.0 for 8 mana. **Lifesteal**: 5/0.53 + 7.5 = 16.9 for
  16. **Blood Moss**: 6/1.375 + 9 = 13.4 for 10.
- **Cleave**: 4 + min(30, 0.12 x max health): 20 -> 6.4; 100 -> 16; 300 -> 34.
- **Meteor vs Explode**: instant 10/24 = 0.42 vs 12/18 = 0.67 (0.63x); total 14/24 = 0.58 vs 0.67 (0.87x).
- **Mana budget**: Twine 100 mana at 5 a second, Echo 300 at 8; each heart circle adds 15 mana, 0.5 a second and 3% power.

## Appendix B. Where each rune lives

| Rune | Effect | Presentation |
|---|---|---|
| ember, fire, flashfire, explode, meteor, inferno, fireward, smelt, leech, rend, bleed | `cast/Effects.java` | `Vfx`, `ExpansionVfx` |
| cleave, dismantle, primer, overdrive | `cast/Techniques.java` (dispatched from `Effects`) | `TechniqueVfx` |
| gash, searing_edge | `cast/CraftedRunes.java` | `CraftedVfx` |
| blood_thread, kindling | `cast/Innates.java` | `Innates` (inline), `TechniqueVfx.chain` |
| firestorm, steam, lifesteal | `cast/FusedEffects.java` | `FusionVfx` |
| phoenix_pyre, hellmouth, starfire, everburn, bloodboil, conflagration | `cast/FusedFlame.java` (+ `FusedFlameRules`) | `FusedFlameVfx` |
| heartstopper | `cast/FusedStorm.java` (+ `FusedStormNumbers`) | `FusedStormVfx` |
| crimson_mist, transfusion, sanguine_rite | `cast/FusedLife.java` (+ `FusedLifeRules`) | `FusedLifeVfx` |
| hemomancy | `cast/FusedVoid.java` (+ `FusedVoidRules`) | `FusedVoidVfx` |
| seethe, skyburst, parasite | `cast/SignatureFusions.java` (+ `SignatureRules`) | `SignatureVfx` |
| warcry, blazecall, sunscorch, soulfire, blood_moss, cinderbrand, ashen_veil, cinderheart | `cast/ExplorerEffects.java` (+ `ExplorerNumbers`) | `ExplorerVfx` |
| Reactions (Shatter, Wildfire, Overload, Rupture, Elapse) | `cast/Reactions.java`, `spell/ReactionRules.java` | `ReactionVfx` |
| Creature affinities and climate | `cast/Affinities.java`, `spell/Affinity.java`, `spell/ClimateRules.java`, `data/wildercord/tags/entity_type/affinity/` | |

## Appendix C. The proposed presentation signature sheet

One line per rune, for whoever builds phase 2. Silhouette is what you would draw from across the room; timing is
anticipation / impact / aftermath. "(keep)" means today's presentation stays. Palettes stay the element's (fire
`#F06E32` / `#FFD060` / `#FF3A1A`, blood `#D2283C` / `#FF6474` / `#5A0A14`) with the accent named here.

| id | silhouette | motion | accent | sound | timing |
|---|---|---|---|---|---|
| ember | one cinder | flicked in an arc, lodges | gold core | `fire_flick` | none / a spark / none |
| fire | flames climbing the body | licking up over the 6 s burn | orange and gold | `fire_whump` | none / whump / burn licks |
| flashfire | flat white-gold disc, hard ring | expands in 0.25 s | white core, gold rim | `flare_flash` | none / flash / none (no smoke) |
| explode | a hard sphere shell and dust ring | expands in 0.4 s, orange to white | white heart | `blast_crack` | none / crack and boom / a smoke wisp |
| meteor | comet streak, closing reticle, crater | falls for 1.2 s | red-orange, dark smoke | `sky_roar`, `blast_crack` (low) | 1.2 s reticle / thud / molten patch 3 s |
| inferno | a standing ring of low flames, heat shimmer | pulses once a second | orange and gold | `field_roar` | none / pulse / a 4 s field |
| primer | a closing fuse ring on the body | tightens, pink to orange to white | white at the end | `fuse_hiss`, `fuse_tick` | 2 s fuse, 0.15 s silence / blast / none |
| kindling | a ring of flames over the head, one per stack | grows to five, then bursts | gold ember per stack | `fire_flick` ladder, `blast_crack` (high) | none / stack / burst |
| firestorm | arcs of flame leaping creature to creature | two waves of leaps | orange over wind-white swirl | `fire_whump`, `fire_flick` per hop | 1.5 s to wave two / hops / smouldering |
| steam | a rolling white dome | billows for 4 s | white, pale blue | `steam_hiss` | none / cloud / fog 4 s |
| sunscorch | a vertical sun beam over a star seal | drops at once, a lens ring first | gold-white | `sun_beam` | 0.3 s lens / beam / Glowing 6 s |
| soulfire | blue tongues and soul wisps | licking, rising | cyan-blue | `soul_whisper` | none / blue flare / flicker 5 s |
| blazecall | three spherical fireballs in a fan | arc in 0.35 s apart | orange, black smoke | `fireball_whoosh` x3 | 0.35 s gaps / three small bursts / none |
| cinderbrand | a brand glyph, then over the head | glows, fades over 6 s | ember red-gold | `brand_hiss` | none / sear / glyph 6 s |
| ashen_veil | a grey ash cloak | drifts; a puff when struck | ash grey, ember flecks | `ash_puff` | none / puff / cloak 10 s |
| cinderheart | a 4-block ground ring, an ember orbit | pulses each second | deep orange | `coals_thrum` | none / pulse / aura 12 s |
| searing_edge | an ember ribbon along the swing | smoulders at the hand | orange-gold | `blade_sear` | none / flash / smoulder 15 s |
| fireward | an amber shell folding in | closing rings, a thin halo | amber | `ward_hush` | none / shell / faint halo |
| smelt | a furnace flash | 0.3 s | gold-white | (keep) | none / crackle / none |
| hellmouth | a black disc with red cracks | drags inward | black and violet | `pit_drone` | pit opens / cave-in / dust |
| starfire | gold and pink motes | seek and curve | gold and pink | `starfire_chime` | mote flight / star seal / embers |
| everburn | a clock face inside flame | hands sweep per tick | gold and orange | `tick_flame` | none / relight / stopped clock |
| conflagration | pillars of flame under burning enemies, arches | rise 3 blocks | orange, white | `field_roar` and blaze | arches by distance / pillars / smoulder |
| phoenix_pyre | wings of flame and leaf | unfold, then flicker | orange and green | `phoenix_cry` | wings unfold / rebirth flare / fade |
| bloodboil | crimson bubbles, red steam | simmers, bursts on each hurt | crimson and orange | `lava_pop` (keep) | none / burst / simmer |
| rend | fissure lines across the chest | cracks spread over 2 s | crimson, steel flecks | `armor_rend` | none / tear / cracks 2 s |
| leech | a thin thread to the caster's hand | streams | crimson | `blood_sip` | none / gulp / none |
| bleed | a widening cut, falling drops | opens each tick | crimson | `blood_slice` (light) and drips | none / cut / drips 4 s |
| gash | a black-red stitched seam | weeps | dark crimson | `blood_slice` (ragged) | none / rip / weeping 8 s |
| dismantle | three hairlines | rising tilt | white over crimson | `blood_slice` (triple) | none / three cuts / none |
| cleave | one huge diagonal cut, a heartbeat ring | sweeps in 0.2 s | crimson, white edge | `blood_slice` (heavy) | none / cut / drips |
| overdrive | racing heartbeat rings, red screen edge | quickens with each drain | crimson | `heart_race` | none / surge / pulses 10 s |
| warcry | ground pulse rings | expand | crimson | `war_horn` | none / horn / rings |
| blood_moss | a mossy patch and threads | creeps | crimson and moss green | `moss_drink` | none / patch / 6 s |
| parasite | a mote burrowing in | spirals in, leaps | green and crimson | `parasite_skitter` (keep sounds) | 0.5 s burrow / drain beats / leap |
| lifesteal | a beating tether ring | tethered 6 s | deep crimson | `blood_drain` | none / mark / ring 6 s |
| crimson_mist | thick red fog, slow pulses | billows for 5 s | crimson haze | `mist_breath`, slow heartbeat | none / fog / thins |
| heartstopper | crimson lightning into the chest, slowing rings | beats slow to a stop | crimson and storm yellow | `heart_stop` | 6 s of slowing beats / hard stop / none |
| sanguine_rite | a blood sigil, then a lance | sigil 0.5 s, then the lance | crimson over black | `rite_blade` | sigil / lance / seam |
| hemomancy | a rune ring, comets, threads by missing health | orbits | crimson and violet | `heart_tempo` | none / threads / none |
| transfusion | a red-to-green stream | arcs over 0.4 s | crimson to green | `blood_gift` | none / bloom / none |
| blood_thread | taut crimson threads between members | a pulse runs along on each share | crimson | `thread_twang` | none / pulses / thread 8 s |
| seethe | bubbles and a steam burst | rising, then bursting | water blue, white steam | `steam_hiss` (burst) | 2 s trap / burst / soaked |
| skyburst | a rising ember trail, a burst, a rain | up, then rain | orange and gold | (keep) | fling 0.3 to 1 s / burst / rain |
