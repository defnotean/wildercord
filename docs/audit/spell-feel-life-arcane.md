# Spell feel audit: life and arcane (phase 1)

Branch `spell-audit-life-arcane`. Audit only: no game code, assets, lang, tests or other docs were changed.
Scope: the 55 runes whose element is life (27) or arcane (28): 30 craftable effects, 13 runes of the world (found or attuned), 2 innate, 6 element fusions and 4 signature fusions.

| Kind | Life | Arcane |
|---|---|---|
| Craftable effects | Heal, Grow, Regrowth, Cleanse, Venom, Nourish, Harvest, Reversal, Restore, Bramble, Haven, Glimmer, Drowse | Night Eye, Harm, Light, Haste, Reveal, Empower, Smite, Silence, Resonance, Decree, Reflect, Swap, Barrier, Span, Summon, Starfall, Spellbrand |
| Runes of the world | Vinelash, Remedy, Ancient Seed, Moonpetal, Sporebloom, Glowvine, Rootsnare | Fangs, Treasure Sense, Starlight Tether, Starshard, Manaburn, Manatide |
| Innate | Fortune | Twin Star |
| Element fusions | Bloom (life+earth), Soulbond (life+arcane), Second Wind (life+time), Lifebloom (life+life) | Nullify (arcane+void), Prismatic Burst (arcane+arcane) |
| Signature fusions | Bloomstep (Grow+Blink), Stitchtime (Heal+Countdown) | Halo (Smite+Regrowth), Cometfall (Starfall+Meteor) |

## 0. The short version

**Verdicts (power, per mana against role peers):** 14 under, 30 fair, 11 over. Full list in section 2.

**Biggest under-performers**
- **Prismatic Burst** is strictly beaten by Harm + Unweave at every mark count (Harm 0.88 to 1.93 damage/mana, Prismatic 0.25 to 1.38), yet it has the best presentation in the group.
- **Venom** is a poison rune, and vanilla undead and spiders are immune to Poison (checked in the game's classes): its 10-damage tail does nothing to zombies, skeletons or spiders, and Poison can't kill anything anyway.
- **Smite** (T3, 16 mana, 10 damage) does less per mana than the T1 Harm (0.625 vs 0.875) and loses to the T2 Ripple even against undead (Ripple 18 damage for 10 mana, Smite 20 for 16).
- **Halo** delivers 12 damage for 18 mana; Thunderbird, its closest peer, delivers 50 for 20.
- **Bloom** is Regrowth made worse (4 health for 16 mana against 6 for 10), **Decree**, **Spellbrand**, **Starshard**, **Manaburn**, **Manatide** (solo), **Sporebloom**, **Moonpetal** (1v1), **Ancient Seed** and **Second Wind** (against Reversal) all sit well under their peers.

**Biggest over-performers**
- **Resonance** is quadratic in crowd size: a Burst on 5 enemies deals 40 for 27 mana, a Zone on 5 enemies about 340 for 36 mana (9.4 damage/mana against a T3 median of 0.6), and on one enemy it is the worst rune in its tier (0.29).
- **Summon**: 3 tamed wolves (40 health each, 4 attack plus Strength I, Speed II) for 20 seconds. On paper 21 damage/s; nothing else at T4 comes near.
- **Fortune** (+50% expected damage for 10 seconds at 10 mana, about 4x Harm's mana efficiency), **Bramble** (T1, 3 damage per hit taken by every attacker for 10 seconds, no cap), **Empower** (Strength II, permanent for 1.4 mana/s as a passive; melee only), **Silence** (6 seconds of "no melee" for 9 mana, area capable, and it doesn't silence), **Bloomstep** (Blink plus more for less), **Restore** (unlimited free gear repair through Zone/Linger/Pulse) and **Twin Star** on big spells.

**Biggest clusters (near-clones)**
- Life "put health back": Heal, Regrowth, Restore, Remedy, Bloom (plus Cleanse against Remedy).
- Arcane "hit one enemy for arcane damage": Harm, Smite, Starshard, Fangs, Manaburn (plus the mark payoff family Spellbrand, Prismatic Burst, Resonance).
- "Make light": Light, Glimmer, Glowvine. "Grow crops": Grow, Ancient Seed, Harvest.
- "Stop this monster": Decree, Rootsnare, Drowse, Silence (and Root, Jolt, Freeze, Shackle from other elements).
- Cross-element: Bramble/Reflect/Halo against Geode, Stoneform, Stormheart, Ashen Veil, Frostbloom, Riposte ("hit back"), and Reversal/Second Wind against Cryostasis, Foresight, Borrowed Time ("cheat death").

**Cross-cutting patterns**
- **21 of the 28 arcane runes open a star seal or star sigil** as their central visual; about 19 of the 27 life runes are built from bloom, leaf spiral and petals. Arcane and life read as two templates, not 55 spells.
- **The cast cue is per element, not per rune**: the circle and `cast_<element>` sound are chosen from the first effect's element, so every life cast opens with the same 1.6 s soft bell bloom and every arcane cast with the same 1.4 s strum. Only 20 custom cast/impact sounds exist (2 and 3 per element); everything rune-specific is a vanilla sound, and amethyst sounds dominate both elements.
- **Cooldown is irrelevant** (1 tick per mana, so 0.6 s for a 12-mana spell); mana (pool 100 to 300, regeneration 5 to 8 per second) is the only real gate. Shapes multiply per-hit effects (Zone: 6 pulses for x2.0 cost), which is why every "instant" rune (Heal, Restore, Resonance, Harm) scales far beyond its Self numbers.
- **Broken or off-spec things** (section 7): Silence doesn't silence, Treasure Sense lights up every player chest, hopper and dispenser and wastes its 24 slots on them, Restore is an unlimited repair loop, Nourish is a silent no-op on pets, Light fizzles silently.

**What I could not verify:** I did not run the game, so everything is from code, from javap of the game's classes (poison immunity, tamed wolf stats), and from the arithmetic in the appendix. The existing screenshots for my runes (`fx_haven`, `fx_moonpetal`, `fx_starshard`) show only the cast circle under the caster, not the effect, so no presentation judgement below rests on a picture; they rest on the drawing code. Summon's damage is on paper (vanilla wolf AI, target choice and death rate not measured).

---

## 1. Method and baselines

### 1.1 What I read

Rules and numbers: `spell/Runes`, `SpellNumbers`, `SpellCompiler` (cost), `ReactionRules`, `Affinity`, `ClimateRules`, `Passives`, `Fusions`, `CordTier`, `Circles`, `Mana`. Implementations: `cast/Effects` (dispatcher and most base runes), `Techniques`, `Wards`, `DeathsDoor`, `CraftedRunes`, `Innates`, `ExplorerEffects`, `FusedEffects`, `FusedLife`, `FusedVoid`, `SignatureFusions`, `SignatureWards`, `Reactions`, `Spirits`, `SpellCaster`, `Charging`, `Runebound`, `Targets`. Presentation: `Vfx`, `ElementFx`, `ExpansionVfx`, `ExplorerVfx`, `TechniqueVfx`, `CraftedVfx`, `FusionVfx`, `FusedLifeVfx`, `FusedVoidVfx`, `SignatureVfx`, `ScreenFx`, `CastingPose`, `sounds.json`, `tools/sound_art.py`. Docs: `docs/DESIGN.md` and the header of the generated wiki pages (they are built by `tools/wiki.py`; I did not read the generator itself). Tests: which gametests and unit tests touch each rune (section 6).

### 1.2 Economy facts that shape every verdict

| Fact | Value | Where |
|---|---|---|
| Spell cost | (shape base x factor + sum of effect cost x shape multiplier) x modifier multipliers | `SpellCompiler.cost` |
| Shape multipliers on effects | Self/Touch x1.0, Bolt x1.1, Beam x1.2, Burst x1.5, Zone x2.0 (6 pulses, 1 s apart), Rain x2.5 | `Runes` |
| Cooldown | one tick per mana, clamped to 0.5 s to 20 s | `SpellNumbers.cooldownTicks` |
| Mana | pool 100/150/225/300 and 5/6/7/8 per second for Twine/Copper/Amethyst/Echo; each Heart Circle adds 15 pool and 0.5 per second | `CordTier`, `Circles` |
| Charge | hold 1.5 s for +40% power | `Charging` |
| Linger | x1.8 cost for 3 hits: +67% efficiency on any per-hit effect | `SpellNumbers.lingerHits` |
| Amplify | x1.6 cost for x1.5 power (0.94x efficiency) | `SpellNumbers.power` |
| Life vs creatures | x1.5 vs `#undead`, the Tide Scribe, the Star Eater; x0.5 vs arthropods and witches | affinity tags |
| Arcane vs creatures | x1.5 vs vex only; x0.5 vs evoker, illusioner, Archivist | affinity tags |
| Climate | life +10% in sun, arcane +15% on a ley line (the smallest climate hooks of any element) | `ClimateRules` |
| Reactions | life: **Blight** (life damage on a shadowed foe: 3 damage and poison to up to 6 foes, heals caster). Arcane: **Unweave** (arcane damage on a foe with 2+ marks: +30% per mark, at most +120%, every mark used up) | `Reactions` |
| Helpful runes | touch only you, your team and your tamed pets (`Targets.canHelp`): villagers, iron golems and wild animals are never healed or fed | `Targets` |
| Passives | Self/Orbit; eligible from my group: Night Eye, Haste, Regrowth, Empower, Reflect (buffs), Harm and Venom (with Orbit); upkeep 0.12 mana/s per point of cost | `Passives` |

Consequences: cooldown does nothing to balance; a rune's price is its mana against a 5 to 8 mana/s trickle; anything that repeats per hit (heals, repairs, damage) is multiplied by shapes and Linger; anything limited "once per cast" or by a lockout (Second Wind, Reversal, Manatide, Halo renewals) is protected.

### 1.3 Roles and reference numbers

All figures are per point of base rune cost (shape x1.0, no modifiers), reactions and affinities excluded, unless stated. Arithmetic in the appendix.

| Role | Tier | Range | Median | Peers used |
|---|---|---|---|---|
| Single-target instant damage | T1 | 0.38 to 1.33 | **0.75** | Harm 0.88, Shock 0.57 (1.14 with its arc), Ember 0.5 (+burn 1.0), Icicle 0.67 to 1.0, Pelt 0.8, Windcut 0.67, Leech 0.38 (+heal), Countdown 0.86 (delayed), Umbra 0.67 to 1.33 |
| | T2 | 0.5 to 1.17 | **0.75** | Fire 0.63 (+burn 1.4), Aftershock 1.11, Dismantle 0.9, Stalactite 0.78 to 1.17, Ripple 0.6 (1.8 vs undead), Vinelash 0.56, Fangs 0.6, Manaburn 0.5, Spellbrand 0.75 (conditional) |
| | T3 | 0.25 to 1.4 | **0.6** | Smite 0.63, Lightning 0.6 (+stun), Explode 0.67 per target, Meteor 0.42 per target, Sanguine Rite 0.86, Plasma 0.45, Starshard 0.56, Resonance 0.29, Prismatic Burst 0.25 to 1.38 |
| | T4 | 0.43 to 0.78 | **0.5** | Sonic Boom 0.46, Starmaw 0.44+, Hollow 0.78 (AoE), Starfall 0.43, Cometfall 0.63 |
| Damage over time (total per mana) | T2 to T3 | 0.44 to 1.5 | ~1.0 | Venom 1.5 (nominal), Fire 1.4, Bleed 1.25, Blackflame 1.0, Ember 1.0 |
| Healing (HP per mana) | T1 to T3 | 0.14 to 1.0 | **0.65** | Heal 0.67, Regrowth 0.6, Remedy 0.6, Lifebloom 0.75, Stitchtime up to 1.0, Restore 0.43, Bloom 0.25, Leech 0.38, Lifesteal 0.31 |
| Control (seconds of full disable per mana) | T2 to T3 | 0.11 to 0.55 | **0.25** | Root 0.33 (slow VII only), Shackle 0.55 (leash), Decree 0.2, Freeze 0.16, Jolt 0.11 (+4 damage), Rootsnare 0.18 per target, Drowse 0.43 (breaks on damage), Silence 0.67 (melee only) |
| Shield/absorb | T1 | ~0.67 | 0.67 | Barrier 0.67, Heal 0.67 |
| Buffs | | by proxy (see rune rows) | | melee damage, damage taken, healing over time |

Real damage against moving targets is lower than these on-paper figures; ratios below are compared like with like.

### 1.4 How to read the verdict letters

**U** under: clearly weaker per mana than its tier and role, or no reason to choose it over a sibling. **F** fair. **O** over: clearly best in class with no cost, trivially repeatable, or strictly dominating a sibling. The ratio is against the tier-and-role median from 1.3 (or against the named sibling). **(shape)** marks a verdict that comes from how shapes and Linger multiply the rune rather than its own numbers. In the counts (14 U, 30 F, 11 O) Rootsnare (F/O) and Restore (shape O, U as a heal) are counted as over; Heal's shape-level O is noted but its own verdict is F.

---

## 2. Verdict tables, one row per rune

Each rune has two rows in the same order: **2a rules and power** (id, tier, cost, role, real numbers, power verdict with ratio, verb, overlap) and **2b presentation and proposal** (what it looks and sounds like today, the verdict, and the proposed mechanic, signature and sound). "M" is mana. Ratios are explained in the appendix. Sound names (`like_this`) are briefs in section 5.

### 2a. Rules and power

#### Life (27)

| id | T | cost | role | key numbers (read from code) | power verdict | verb | overlap |
|---|---|---|---|---|---|---|---|
| heal | I | 12 | heal, instant | 8 health x power; Linger 3 hits | **F** 0.67 HP/M on Self. **(shape) O**: Burst x4 allies 1.33; Zone x4 allies (6 pulses) 192 HP for 32 M = 6.0 HP/M, 9x Self | put health back now | Restore, Remedy, Regrowth, Lifebloom, Stitchtime, Bloom; Transfusion (blood) |
| grow | I | 4 | world | bone meal x round(2 x power) on every growable block in 3x3x3, drawn from the cast's block budget | **F** cheap utility (about 7 stages a crop) | make plants grow now | Ancient Seed, Harvest |
| harvest | I | 3 | world | breaks ripe `CropBlock`s in a 5x3x5 (x radius) and replants one seed each | **F** | reap and replant | Grow, Ancient Seed |
| glimmer | I | 2 | world | up to 5 glow-lichen patches (x radius) around the hit face, permanent | **F** | leave a light on a wall | Light, Glowvine |
| glowvine | I | 3 | world | up to 5 cave-vine columns 1 to 3 long from a ceiling within 8 blocks, berries on, permanent | **F** | leave a light and food on a ceiling | Light, Glimmer |
| ancient_seed | I | 4 | world | a torchflower (65%) or pitcher plant (35%) plants; crops within 4 blocks grow exactly 1 stage (cap 32) | **U** about 0.5x Grow per crop (Grow adds about 7 stages to 9 crops, Seed 1 stage to up to 32); the flower is cosmetic | plant a flower and nudge a field | Grow, Harvest |
| nourish | I | 6 | support | 6 hunger + 7.2 saturation, **players only** | **F** (about a steak for 6 M) | feed | Sporebloom (feeds allies) |
| regrowth | II | 10 | heal over time | Regeneration II 8 s = 6 HP (Regen II heals 1 per 25 ticks); passive-eligible | **F** 0.6 HP/M | steady sustain | Heal, Remedy, Bloom, Zephyr, Phoenix Pyre |
| cleanse | II | 8 | support | removes all harmful effects, fire, freeze mark | **F**; but Remedy (10 M) does this plus 4 HP plus Regen I | wipe ailments | Remedy, Nullify, Restore (fire), Frostward |
| venom | II | 8 | damage over time | 2 damage + Poison II 6 s (10 more: 1 per 12 ticks); life x1.5 vs undead; Poison can't kill | **U in practice**: 1.5 dmg/M nominal, but undead and spiders are immune to Poison (javap: `IGNORES_POISON_AND_REGEN`, `Spider.canBeAffected`), so vs zombies/skeletons/spiders 2 x1.5 = 3 damage for 8 M = 0.38/M (0.5x median) | poison | Bleed (blood), Blood Moss, Parasite, Sporebloom, Entropy |
| moonpetal | II | 12 | damage + heal, area | 4 damage to enemies within 3 blocks, 3 HP to you and allies there | **U** in a duel (0.33/M, 0.4x median); **F** in a mixed fight (0.33/M per enemy plus 0.25 HP/M per ally) | a hybrid: hurt them, mend us | Crimson Mist (blood), Thunderclap, Coldsnap, Flashfire |
| sporebloom | II | 10 | control/area | Poison I 6 s (4 damage) + Nausea 6 s within 3 blocks; 4 hunger to allies. Nausea does nothing to mobs | **U** 0.4 dmg/M per target and no control on mobs (0.5x median) | a spore cloud | Sandstorm, Infest, Venom, Hush |
| rootsnare | II | 11 | control/area | every enemy within 3 blocks: **true hold** 2 s (`Spirits.hold`: NoAI, no attacks) + 3 damage | **F/O** vs Root (9 M: Slowness VII only, target can still swing): +2 M buys a real stun, area and damage; attunement-only (mangrove swamp) | entangle everything near a point | Root, Shackle, Mire, Freeze |
| vinelash | II | 9 | damage + pull | 5 magic damage (armour ignored), yanks up to ~4 blocks toward you; does **not** leave the `PULLED` mark | **F** 0.56/M plus pull (0.75x median for damage) | whip and reel | Pull, Tidehook (frost), Gravity Well |
| remedy | II | 10 | support | removes harmful effects, 4 HP, Regen I 6 s (2 HP); weakens zombie villagers | **F** 0.6 HP/M plus cure; dominates Cleanse for +2 M | cure and mend | Cleanse, Heal, Nullify |
| bramble | I | 6 | ward | 10 s: everything that hurts you from within 4.5 blocks takes 3 damage (thorns damage) and is shoved 0.9; **no cap on procs** | **O** vs melee packs: 20 procs (2 attackers, 1 hit/s) = 60 damage for 6 M = 10/M, 13x T1 median; about 5/M once the shove halves the hits. Costs nothing but being hit. Useless vs ranged | punish melee | Reflect, Geode, Stoneform, Stormheart, Ashen Veil, Frostbloom |
| haven | II | 14 | ward, area | 4-block dome, 8 s, fixed where cast: Resistance I (-20%) for allies inside (refreshed every 10 ticks); enemy projectiles (incl. enemy rune bolts) whose owner is outside bounce off | **F** about 6 HP-equivalent per ally under 30 damage/8 s (0.4/M per ally, more per party) plus projectile immunity | shelter for a fight | Stoneskin, Infinity (void), Deflect (wind), Shield (earth) |
| drowse | III | 14 | control | sleep 6 s (2 s on players): held (NoAI) until any damage; bosses only slowed; wake sneeze | **F** 0.43 s/M vs mobs (1.7x median), 0.14 vs players; broken by any damage so a stealth or escape tool, not a fight tool | sleep | Freeze, Glacier, Stasis, Decree |
| fortune | I | 10 (innate) | buff | 10 s: each hit you deal 25% to strike x3 (melee x3 too); expected +50%; +6% per Heart Circle; can be cast on allies | **O** about 4x Harm's efficiency in a sustained fight: a caster spending 80 mana in 10 s dealing about 64 damage gains about +32 for 10 M (3.2/M against Harm 0.88) | luck | Hex (+25% on one target), Kindled, Cinderbrand, Eclipse |
| bloom | III | 16 | heal over time | Regen II 6 s (4 HP) to allies hit; bone-meal blossoms round the first 3 | **U** 0.25 HP/M: Regrowth (10 M) gives 6 HP; strictly dominated | heal and garden | Regrowth, Zephyr, Frostbloom |
| soulbond | III | 16 | ward | you and one ally, 10 s, within 16 blocks: half of every wound after armour goes to the other; bosses can't bond | **F** conserves total damage, lets a tank offload spikes | share wounds | no other damage-share |
| second_wind | III | 20 | death ward | 20 s: first killing blow leaves 4 HP (x power) + Regen II 4 s (3 HP); 60 s lockout shared through `DeathsDoor`; not on bosses | **U** vs Reversal (28 M): about 4 HP vs 10 HP on a 20-HP player, for 8 M less | survive once | Reversal, Cryostasis, Riposte, Foresight |
| lifebloom | III | 16 | heal over time | 4 now + 1 per second x 5 + burst 3 to every ally within 3 blocks = 12 HP (0.75/M) plus 3 per nearby ally | **F** | a heal that ripens | Heal, Regrowth, Stitchtime |
| restore | III | 14 | support | 6 HP, extinguishes fire, mends 5% of every worn and held item; repeats with Zone/Linger/Pulse/Echo | **(shape) O** repair: Zone x6 = 30% durability of all gear for 36 M with no cap; **U** as a heal (0.43 HP/M) | put things back | Heal, Remedy, Stitchtime |
| reversal | IV | 28 | death ward | 30 s: killing blow leaves half of max HP, clears harmful effects, Regen II 5 s, Resistance II 3 s; applies to every ally hit (Burst = party); 60 s lockout | **F** for T4 | be pulled back from death | Second Wind, Cryostasis, Borrowed Time |
| bloomstep | III | 14 | movement | teleport to the landing spot (32 blocks, safe ground as Blink) + Regen I 5 s to allies within 3 blocks + bone meal at both ends | **O** vs Blink (T3, 15 M, 40 blocks): everything Blink does for 1 M less, plus regen and garden; only range is worse | step through a flowering door | Blink, Warp, Shadowstep |
| stitchtime | III | 16 | heal, delayed | 4 now, then 4 s of wounds counted and healed back at once (max 12): up to 16 HP (1.0/M) if hurt | **F** rewards being hit | heal what's about to hurt | Heal, Reckoning (time), Borrowed Time |

#### Arcane (28)

| id | T | cost | role | key numbers (read from code) | power verdict | verb | overlap |
|---|---|---|---|---|---|---|---|
| night_eye | I | 3 | utility buff | Night Vision 60 s (20 s/M); passive-eligible | **F** | see in the dark | none in mod |
| harm | I | 8 | damage, instant | 7 magic damage, armour ignored; Linger 3 hits; passive Orbit aura | **F** 0.88/M, T1 top. With one Linger 1.46/M | plain arcane damage; Unweave's spark | Shock, Ember, Icicle, Pelt, Windcut (other elements' T1 pokes) |
| light | I | 2 | world | light level 15 block for 60 s at the point (fizzles silently if the cell isn't air) | **F** | a lamp for a minute | Glimmer, Glowvine |
| haste | I | 4 | buff | Haste II 30 s (+40% mining, +20% attack speed) | **F** 7.5 s/M; silent (no sound) | quick hands | Swift (wind), Overdrive (blood), Accelerate (time) |
| reveal | I | 3 | control/utility | Glowing 15 s through walls on enemies | **F** | expose | Echolocate (void, area) |
| empower | II | 12 | buff | Strength II 10 s = +6 melee damage per hit; **does nothing for spells**; Amplify adds levels up to IV (+12); passive 1.44 M/s | **O** for melee: 96 extra damage per 10 s at 1.6 swings/s (8/M); passive gives +6 permanently | melee power | Overdrive (blood), Surge (storm), Warcry (blood) |
| smite | III | 16 | damage, instant | 10 magic damage, x2 vs undead (20) | **U** 0.63/M (0.7x Harm) vs non-undead; vs undead 1.25/M but **Ripple T2 (10 M) deals 18 and heals** | holy strike | Ripple (storm), Halo, Harm |
| starfall | IV | 32 | damage, area over time | 8 stars over 2 s at random points within 4 blocks, each 6 damage in a 3.2-wide box | **F** area / **U** single: expected 13.8 on a stationary target (0.43/M, 0.85x median); about 10 per enemy when several stand around; 2/M with 5+ | rain of stars | Cometfall, Meteor, Rain (shape) |
| silence | II | 9 | control | target dropped once (mobs re-acquire within about 10 ticks); Weakness II 6 s = -8 melee damage. No effect on ranged, creepers, casters | **O** vs melee packs: 6 s of "no melee" at 0.67 s/M (2.7x median, x N with Burst/Zone); zero otherwise. **Does not silence** | quiet | Hush (void), Blind, Weigh, Hex |
| resonance | III | 14 | damage, network | 4 damage + mark 10 s; every marked enemy within 16 (max 8) takes half of each later hit | **O in crowds**: N enemies in a Burst = N^2+3N (5: 40 for 27 M, 1.5/M); in a Zone about 340 for 36 M (9.4/M). **U solo**: 4 for 14 M = 0.29/M | link enemies together | Blood Thread (blood innate), Stormweave, Chain modifier |
| decree | II | 10 | control | true hold 2 s (NoAI, no attacks); costs you 2 HP (once per cast); bosses only slowed | **U** 0.2 s/M minus 2 HP (0.8x median control, worse than Jolt 1 s + 4 damage for 9) | command | Jolt, Root, Freeze, Rootsnare |
| reflect | III | 16 | ward | 10 s: 60% (cap 150%) of damage taken is dealt back to the attacker at any range (thorns damage, reduced by their armour); passive-eligible (1.9 M/s) | **F** scales with the blow: vs 3-damage zombies 1.8 back (Bramble gives 3 for 6 M); vs a 20-damage boss 12 | mirror the blow | Bramble, Geode, Stoneform, Riposte, Mirrorfrost |
| swap | II | 6 | movement | you and the first creature (not a boss) trade places instantly, if both fit | **F** cheap | trade places | Warp (void fusion), Shadowstep, Blink |
| barrier | I | 6 | ward | Absorption I (4 HP) 20 s; Amplify adds levels | **F** 0.67 HP/M, equal to Heal | soak a hit | Heal, Frostward, Brace |
| span | II | 8 | world | glass bridge from your feet toward the point (16 blocks), 30 s, at the level of the block under your feet | **F** | build a bridge | Icepath (frost), Rampart (earth) |
| summon | IV | 30 | summon | 3 tamed wolves (vanilla tamed wolf: 40 HP, 4 attack), Speed II, Strength I (+3) = 7 per bite; 20 s; max 6 spirits; players only | **O** on paper: 21 damage/s x 20 s = 420 (14/M at 100% uptime, 5 to 7/M realistic; T4 median 0.5). Not measured | a pack | Shades (void T3), Thunderbird (storm), familiars |
| twin_star | I | 12 (innate) | buff | the next cast within 6 s goes off again 8 ticks later, **free and at full power**; self only; +6% per circle | **O** for expensive spells: 32-M spell = 44 M for 64 M of value (1.45x); 100-M spell 1.8x. Fair at 12 M or less | do it twice | Echo (link, costs the same again), Chorus |
| nullify | III | 14 | control (dispel) | strips an enemy's beneficial effects, or an ally's harmful ones | **F** niche (PvP, witches, Runebound); as an ally cure it is dominated by Cleanse (8 M) | dispel | Cleanse, Timesteal (time), Starmaw (void) |
| treasure_sense | I | 4 | utility buff | Luck II 60 s; glints (every 40 ticks for 60 s) over the first 24 `RandomizableContainerBlockEntity`/`BrushableBlockEntity` found within 24 blocks: **every chest, barrel, hopper, dispenser, dropper, shulker box**, players' included | **F** value 15 s/M; **design bug**: not treasure-specific, not nearest-first | find loot | Prospect (crafted), Echolocate |
| fangs | II | 10 | damage, trap | ring of 5 evoker fangs; target takes 6 after 0.4 s; every other enemy on a fang takes 6 once | **F** 0.6/M (0.8x median) single, more in a pack | teeth from the ground | Pelt, Stalactite, Bonespur |
| starlight_tether | III | 14 | control + damage | 6 s: each target is pulled back if it strays 3 blocks from the anchor, 2 damage per pull (once per 0.5 s: up to 24); bosses only slowed | **O** vs Shackle (T2 9 M, leash without damage): leash + up to 24 damage; world rune (End outer islands) | leash | Shackle, Root, Gravity Well |
| starshard | III | 16 | damage, split | 9 damage + 3 sparks x 3 to the nearest other enemies within 8 blocks (line of sight) | **U** single 0.56/M (0.65x Harm); 1.1/M with 3 others. A world-event-only rune ought to beat the baseline | shatter into sparks | Shock (arc), Chain, Cluster, Prism |
| manaburn | II | 10 | damage, anti-caster | 5 damage; 9 on a Cord-wearing player or a Runebound; drains up to 20 mana | **U** in PvE 0.5/M (0.67x median), 0.9/M vs casters; niche | burn magic | Silence, Nullify |
| manatide | III | 12 | support | +3 mana/s for 10 s to allies hit (max 30), each player once per minute | **U solo**: net +18 mana per minute against a 300-mana-a-minute trickle (+6%); **F** for a party (Burst: each ally +30) | drink mana | Meditation, Siphon enchant, Devour |
| spellbrand | II | 8 | damage, setup | brand 8 s; next spell of yours to hurt it makes it burst for 6 arcane (Amplify scales) | **U** single: 0.75/M and needs a second spell (Harm gives 7 for 8 with no condition); **F** in a pack (6 per enemy) | mark and detonate | Hex, Cinderbrand, Resonance |
| prismatic_burst | III | 16 | damage, payoff | 4 + 3 per elemental mark (burning, frozen, windswept, pulled, soaked, wet, cracked, shadowed, bleeding; capped at 6 = 22), all used up; quiet: never sets off Unweave | **U** 0.25 to 1.38/M; **Harm + Unweave beats it per mana at every mark count** (0 marks 0.88 v 0.25; 2 marks 1.4 v 0.63; 4 marks 1.93 v 1.0; 6 marks 1.93 v 1.38) | cash in every mark | Unweave (the reaction), Spellbrand |
| halo | III | 18 | ward + damage | 8 s crown on an ally: 4 smites (first at 0.5 s, then every 2 s) of 3 magic damage (x3 undead) to the nearest enemy within 6 blocks of them, ally heals 1 each; total 12 damage (36 undead) + 4 HP | **U** 0.67/M (0.27x Thunderbird's 2.5/M: 10 strikes x 5 for 20 M); 2/M vs undead | a guardian crown | Thunderbird, Orbit shape, familiars |
| cometfall | IV | 32 | damage, telegraphed | 1 s delay; 16 falling to 9.6 across 4 blocks + burn 4 s; 5 shards x 4 to other enemies within 10 blocks | **F** 0.5/M single (0.63 with the burn), 2 to 3/M in a pack; about 1.5x Starfall at the same cost | one big comet | Starfall, Meteor, Explode |

### 2b. Presentation and proposals

Presentation verdicts: **Good** (distinct, right scale), **Template** (same building blocks as siblings), **Plain** (bare particles, small), **Silent** (no sound of its own), **Scale** (display doesn't match power), **Mismatch** (looks or sounds like something else). "Signature" is silhouette, motion, palette accent and timing. New mechanics are meant to be about an afternoon each and reuse `Reactions` marks, `Scheduler`, `ShapeRunners.each`, `ElementFx` primitives.

Two rules the proposals follow so arcane and life stop reading as templates: **arcane: the star seal is reserved for a lasting mark or state on someone (Spellbrand, Reveal, Reflect, Halo, Manatide, Exposed); instant strikes use needles, columns and comets, wards use glass, movement uses paired arcs.** **Life: the bloom (flower seal) is reserved for restoration; combat life uses thorn and vine slashes, crowd life uses spores and pollen, world life uses growth ripples and lamps.**

#### Life (27)

| id | presentation today | verdict | proposal |
|---|---|---|---|
| heal | `Vfx.heal`: bloom, 5-step leaf spiral, 4 petals, 3 hearts, one `amethyst_block_resonate` at pitch 1.5 | **Template**: the same bloom, spiral and resonate are reused by Regrowth, Restore, Reversal's mark, Bloom, Lifebloom, Remedy and Nourish | **M:** overheal is kept: whatever a heal can't use becomes Absorption (up to 2 hearts, 10 s), so it never wastes mana; numbers unchanged.<br>**S:** a column of five rising petals and a heart-shaped ring at the feet (the bloom stays; it is Heal's own); when it overheals, a thin gold ring closes round the waist. First 100 ms: a warm flash at the chest.<br>**Snd:** `heal_chime` (rising third, airy tail) instead of the shared amethyst resonate |
| grow | `Vfx.grow`: flash, ground ring, 4 petals, 6 villager sparks; the sound is vanilla bone meal | **Plain** and shares Harvest's effect | **M:** also ages baby animals (and baby villagers) in the 3x3x3 up to adults: "makes things grow up"; bone-meal part unchanged.<br>**S:** a green ripple racing out from the impact with stems popping up ring by ring (40 ms apart); lime accent.<br>**Snd:** `grow_pop` (fast, bright bloops) |
| harvest | reuses `Vfx.grow` plus `crop_break` | **Plain**, not its own effect | **M:** reaps *every ripe plant without killing it*: sweet berries, nether wart, cocoa, melons and pumpkins, cane/cactus/bamboo/kelp tops, as well as crops (a seed still replants).<br>**S:** a flat golden crescent sweeping across the field (`Light.slash` laid level) throwing wheat-gold motes, items hopping toward you.<br>**Snd:** `harvest_sweep` (dry scythe swish plus rustle ticks) |
| glimmer | a soft glow and 3 `GLOW` motes on each new patch; one amethyst place | **Plain**, fine for 2 M | **M:** none needed beyond the cluster split (permanent wall light).<br>**S:** a bioluminescent ripple crossing the wall from the impact, patch by patch 60 ms apart, in cyan-green so it isn't Heal's lime.<br>**Snd:** `grow_pop` (glassy, quiet variant) |
| glowvine | amber glow plus petals per column, `cave_vines_place` | **Plain** | **M:** recasting on the same vines re-ripens their berries (food that comes back).<br>**S:** vines *lowering* from the ceiling (amber drips falling down each column over 1 s), berries lighting one by one.<br>**Snd:** `berry_plip` (descending plips) |
| ancient_seed | bloom, 8-step leaf spiral, egg-crack particles, `sniffer_egg_crack` | **Good** look, weak effect | **M:** the seed becomes a slow garden: it plants the flower and, every 3 s for 15 s, every crop within 4 blocks grows a stage (5 pulses, 32 crops each); Grow stays the instant one.<br>**S:** the seed cracks (kept), then a gold-teal ring rises through the field every 3 s, a visible growth heartbeat.<br>**Snd:** `seed_crack` plus a soft tick per pulse |
| nourish | bread crumbs, small bloom, 4 villager sparks, `generic_eat` | **Plain**; and silent no-op on pets | **M:** on tamed animals heals 6 and puts them in love mode; players unchanged; also removes the Hunger effect.<br>**S:** warm amber ring at the mouth, crumbs (kept).<br>**Snd:** `nourish_munch` (soft double crunch, warm hum) |
| regrowth | leaf spiral, ground ring, petals, `amethyst_block_resonate` 1.7 | **Template**: near-identical to Heal's cue | **M:** a ramp: Regen I 3 s, II 3 s, III 2 s (about 7 HP, same cost); the passive keeps flat Regen II so it doesn't regress.<br>**S:** a vine *winding up the body in three stages* with leaves shedding at each step; deep green to lime.<br>**Snd:** `regrowth_unfurl` (leaf-rustle swell with three rising marimba ticks) |
| cleanse | 4 rings falling down the target, splash drops, bubbles, `brewing_stand_brew` 1.6 | **Good** | **M:** also washes every elemental mark off allies (frozen, soaked, wet, windswept, pulled, shadowed, bleeding, cracked): a defence against reactions in PvP, duels and Runebound fights.<br>**S:** as today plus droplets that rebound off the ground as tiny rings; clear white-blue accent.<br>**Snd:** `purge_pour` (descending filtered noise resolving into a clear bell) |
| venom | two fang slashes, ring, slime, `spider_hurt` 1.4 | **Mismatch**: a spider's hurt sound, a poison rune that does nothing to spiders | **M:** stops depending on vanilla Poison for its damage: 2 now + 1 per second for 6 s as spell damage (lethal, hits undead and spiders; life x1.5 vs undead applies), plus Poison I as the marker for cures, Blight and Elapse; **contagion**: the victim passes Poison I 4 s and 4 damage to up to 3 enemies within 2.5 blocks, once. Total vs a normal mob stays about 12; vs zombies 12 (was 3).<br>**S:** the fangs stay; a thin green thread jumps to each neighbour; sickly yellow-green.<br>**Snd:** `venom_bite` (wet snap and hiss) |
| moonpetal | pale moon disc overhead, star sigil on the ground, 12-step leaf spiral, 30 cherry leaves, `cherry_leaves_break` + chime | **Good**, a little heavy for 12 M (about 45 particles) | **M:** waxes with the moon: damage and healing x1.4 at full moon, x1.0 at the quarters, x0.7 at new moon; +25% at night under open sky. The only rune that reads the moon.<br>**S:** the disc shows the current phase; petals fall as a storm from it.<br>**Snd:** `petal_storm` (airy swirl, descending moon-bell, glitter) |
| sporebloom | red mushroom cap bloom, 14 block bits, 30 spore particles, 20 mycelium, ring, `fungus_break` + `impact_life` | **Good**, noisy (about 65 particles) | **M:** real confusion: for 5 s every enemy in the cloud picks a fight with the nearest other hostile within 6 blocks (players get Nausea); Poison I stays; allies still get 4 hunger. The mod's only crowd-control that makes enemies fight each other.<br>**S:** a ring of spores that drifts out; confused enemies circled by small spore rings; rose-mauve accent.<br>**Snd:** `spore_puff` (muffled fwump, granular hiss, detuned wobble) |
| rootsnare | crack, 10-step leaf spiral, 8 root rays, mangrove-root bits, `roots_break` + `impact_life` | **Good** | **M:** hold 2 s to 1.5 s, then the roots stay 4 s as brambles: Slowness II and 1 damage per 1.5 blocks moved (max 5): a movement tax, so it is no longer just a bigger Root.<br>**S:** the converging ring of roots (kept) leaving thorn tufts on the ground.<br>**Snd:** `root_grip` (creak sweeping down, snap, soil thump) |
| vinelash | green ray from the hand, slash at the target, petals, vine bits, `mangrove_roots_break` + `impact_life` | **Good**, distinct silhouette (a whip) | **M:** marks the target `PULLED` (so Vinelash then Explode = Implode, Repel = Collapse) and ends with a trip: Slowness II 2 s.<br>**S:** whip-crack line (kept) with a petal streak trailing it.<br>**Snd:** `vine_crack` (bullwhip crack) |
| remedy | bloom, grey motes rising off the target, villager sparks, `zombie_villager_cure` 1.6 | **Good** | **M:** transmutes what it cures: Poison to Regeneration I, Slowness to Speed I, Weakness to Strength I, Blindness/Darkness to Night Vision, Mining Fatigue to Haste I, Levitation to Slow Falling, each for half the time left (max 10 s); everything else is just removed. Heal 4 and Regen I 6 s stay.<br>**S:** the grey motes *turn green as they rise*.<br>**Snd:** `purge_pour` variant resolving into a rising chime |
| bramble | 3 thorny arcs curling round the body, dust, `sweet_berry_bush_place`; strike: ray, flash, `player_hurt_sweet_berry_bush` | **Good** | **M:** 5 thorns, not unlimited: each hit taken spends one (3 damage and the shove as now); power scales damage. Caps the swarm case at 15 for 6 M (2.5/M) and makes it countable.<br>**S:** thorns you can see and count orbiting the body; each spent thorn flies at the attacker and leaves a gap.<br>**Snd:** `thorn_snap` (crackle when grown, snap when spent) |
| haven | ground seal, flash, 4 rising rings, 3 meridians, breathing shell, glance ripples, `beacon_activate` + `amethyst_block_resonate` | **Good** (one of the best) | **M:** the edge pushes hostile creatures out (0.9 a second, horizontal) instead of giving Resistance I; projectiles still glance. Same value in a different, readable form.<br>**S:** the dome (kept) with a visible ripple at the boundary on each shove.<br>**Snd:** `dome_pad` (airy chord swell, breathing pulse each second, descending glide on close) |
| drowse | pink pollen cloud, 8 petals, `azalea_leaves_break` 0.7; pale motes over the sleeper's head; wake: angry spark + `panda_sneeze` | **Good** | **M:** a sleeper struck takes x1.75 from that first blow, then wakes: the assassin's opener that makes waking the target the point.<br>**S:** pollen puff (kept), slow `z`-like motes; wake flash on the backstab.<br>**Snd:** `lull_pad` (detuned pad falling a fourth, faint music-box) |
| fortune | star sigil, luck ring, 6 villager sparks, `player_levelup` 1.8; proc: flash, ring, orbit, crit sparks, `amethyst_block_chime` 2.0 + `player_attack_crit` | **Good** proc, no ambient cue | **M:** price and identity: strikes for x2 (not x3), expected +25%; and a kill made under Fortune has a 1 in 4 chance to roll its loot again and drop double XP. Luck is about loot now, and the fight buff is halved.<br>**S:** a gold coin spinning over the target's head while Fortune lasts (the ambient cue), flipping on each proc.<br>**Snd:** `luck_coin` (metallic flutter ring, sparkle; brighter ding on a proc) |
| bloom | flower seal, 10-step leaf spiral, 12 petals, crack ring, `azalea_leaves_place` + `impact_life` | **Template** and heavy for the value | **M:** pollen: allies within 4 blocks of each ally it touches get Regeneration I 5 s (one hop, max 6): a party regen from one cast without Zone pulses; Regen II 6 s stays; cost 16 to 14.<br>**S:** a flower at each ally's feet with a ring of pollen drifting to the next.<br>**Snd:** `grow_pop` bloom-chord variant |
| soulbond | ray tether between the two with flowing pieces, two chimes, snap: `amethyst_block_break` + `chain_break` | **Good** | **M:** keep; adds a warning pulse in the tether at 2 s left.<br>**S:** kept.<br>**Snd:** `soul_link` (two bells a fifth apart, slow beating; dissonant crack on snap) |
| second_wind | hourglass over the ally, mark every 2 s; save: glass break, bell, `totem_use` 1.5 | **Good** | **M:** on the save: 4 HP + Regen II 4 s **and Speed II 4 s and a gust that shoves enemies within 3 blocks away**: it is the "get out" heal, Reversal the "stand and fight" one.<br>**S:** the hourglass shatters into a ring of wind.<br>**Snd:** `second_wind_gasp` (sharp inhale rising to a gust, bell) |
| lifebloom | flower opens, pulse each beat, burst ring, `amethyst_block_resonate` + `pink_petals_place` + `impact_life` | **Good** | **M:** none; burst waits 0.3 s after the last beat for anticipation.<br>**S:** kept.<br>**Snd:** `lifebloom_pulse` (marimba tick, one step higher each beat, chord on the burst) |
| restore | leaf spiral, bloom, hearts, gold ring and wax sparks when it mended, `smithing_table_use`, `amethyst_block_chime` | **Template** heal look; the mending is the interesting half | **M:** heal 6 to 4; mends 8% of each worn and held item, but an item can be mended by Restore only once per 60 s (closes the Zone/Linger repair loop); cost 14 to 12; fire out as now.<br>**S:** a golden thread stitching each equipped item in turn (armour pieces glint head to toe), anvil ring at the end.<br>**Snd:** `mend_tap` (two hollow taps, tiny anvil ring, warm chime) |
| reversal | mark: leaf spiral, life star sigil, totem sparks, `amethyst_block_resonate` 1.2; save: bloom, spiral, 3 rings, 30 totem sparks, **vanilla `totem_use`** | **Mismatch**: the payoff is a vanilla Totem of Undying | **M:** none.<br>**S:** the save: a golden sun-disc rising behind the ally and a door of light closing round them (own silhouette); mark: a slow halo of leaves.<br>**Snd:** `deaths_door` (two low heartbeats, door creak, rising four-voice chord, one bright bell) |
| bloomstep | void seal and petals where you leave, a door of blossom where you arrive (upright petal rings, star seal, butterflies), `blink` + `azalea_leaves_place` | **Good** | **M:** up to 3 allies within 3 blocks of you come along; range 32 to 24; cost 14 to 16. Bloomstep becomes the party mover; Blink stays the solo long jump.<br>**S:** a trail of petals along the route as well as the two doors.<br>**Snd:** `bloomstep_door` (airy shoop, petal flutter, two soft leaf-footsteps) |
| stitchtime | golden thread round the ally, clock face under them, green bloom, golden ticks, `enchantment_table_use` 1.6 + `impact_life` | **Good** | **M:** none.<br>**S:** kept; each counted wound adds a visible stitch.<br>**Snd:** `stitch_tick` (thread-pull zip, clock ticks, tight pluck on close) |

#### Arcane (28)

| id | presentation today | verdict | proposal |
|---|---|---|---|
| night_eye | violet ring closing round the eyes, a spark in each, glow motes, `beacon_power_select` 1.8 | **Good** but quiet | **M:** also outlines invisible things (Veil, Phantom, potions) within 24 blocks as pale motes, only for you: the counter to void's stealth.<br>**S:** kept.<br>**Snd:** `night_eye_open` (airy inhale, two soft chimes) |
| harm | flashes, star seal under the target, orbit of 3 comets, 8 hit sparks, shimmer, **`player_attack_crit`** | **Template + Mismatch**: the baseline damage rune uses the seal template and a melee crit sound | **M:** marks the target **Exposed** for 3 s (new mark: counts as one mark for Unweave, used up by Unweave/Prismatic Burst); a second Harm on a target with one other mark unweaves.<br>**S:** no seal: a thin needle of pink light snapping from hand to target, a small starburst, a tiny eye-shaped seal hovering over the head for 3 s.<br>**Snd:** `harm_needle` (dry "tsk" plus glass thump) |
| light | flash, orb, ring, 8 end-rod motes, `amethyst_cluster_place` 1.6 | **Plain** | **M:** the lamp follows its target: on a creature (Self by default) the light moves with them for 60 s (one per caster); on a block it stays as now; a Light that can't be placed says why.<br>**S:** a small orb that trails behind you like a wisp and gutters in its last 5 s.<br>**Snd:** `light_kindle` (match strike, bell; faint hum while it follows) |
| haste | comets whirl round the arms, star seal, crit sparks; **no sound** | **Template + Silent** | **M:** while Haste lasts, a charged cast fills 30% faster (1.5 s to about 1.05 s): the mage's version of quick hands (Focus of Haste's job, as a buff).<br>**S:** afterimage streaks off the hands, no seal.<br>**Snd:** `haste_flutter` (three fast rising notes) |
| reveal | star seal, 4 rings scanning up, glow motes, `amethyst_cluster_place` 2.0 | **Template** | **M:** the target is Exposed for the whole 15 s and loses Invisibility; homing bolts and wisps prefer Exposed targets. The spotlight.<br>**S:** a sonar ring sweeping outward from the target, an eye seal above the head.<br>**Snd:** `reveal_ping` (sonar ping with a ring) |
| empower | star seal, red ground ring, 4 flame tongues, angry spark, `player_attack_strong` | **Template** | **M:** price: when it ends you are Weak (Weakness I) for 4 s (not for a passive); a passive Empower is capped at Strength I; Strength II for 10 s stays.<br>**S:** a shockwave ring off the fists, and each melee hit under Empower throws a small crescent.<br>**Snd:** `empower_thrum` (sub-bass rise and crackle; a soft exhale on the comedown) |
| smite | ray dropped from 5 above, flash, gold star sigil, ground ring, end-rod burst, `bell_resonate` 1.6 | **Scale**: a T3 finisher with no wind-up, same look as Harm's seal family | **M:** telegraphed: a ring closes at the target's feet for 0.7 s (dodgeable), then a column of light for 15 damage (x2 undead = 30), ignoring armour, and strips Absorption. 0.94/M (1.9/M vs undead, beating Ripple) in exchange for the wind-up.<br>**S:** the ring closing, a tall white-gold column, ground scorch ring.<br>**Snd:** `smite_toll` (rising tone through the telegraph, low bell, sharp crack) |
| starfall | flat star mark, 5-step falling star, 5-pointed burst, end-rod and firework bits, `firework_rocket_twinkle` | **Scale**: same tier and cost as Cometfall, which has a telegraph, shake and huge impact | **M:** stars prefer Exposed/Revealed enemies before random ground; each leaves a starlight pool for 1 s that marks enemies Exposed; numbers unchanged.<br>**S:** the 8 marks drawn as a star polygon on the ground and joined by light threads for 0.5 s before the first star drops; a soft camera shake per star.<br>**Snd:** `star_rain` (sparse twinkles, one falling whistle and pentatonic plink per star) |
| silence | ring closes over the head, inner ring, circle sigil, witch sparks, `illusioner_prepare_blindness` | **Mismatch**: nothing sounds silenced | **M:** does what it says: a Runebound's telegraphed cast is cancelled and it can't cast for 4 s; a player's charge is cancelled and they can't cast for 3 s; Weakness II becomes Weakness I (6 s) so it stops being a free "no melee" button.<br>**S:** the ring closes (kept) and a muzzle-like seal hangs over the head; nearby cast sounds duck.<br>**Snd:** `silence_hush` (a low-pass sweep to silence, a tiny tick) |
| resonance | flash, 2 tilted rings, iron nuggets, threads to linked enemies, `anvil_land` 1.9 + `bell_resonate` 1.6 | **Good** silhouette (nails and threads), instant so it doesn't read as an echo | **M:** the echo travels: it reaches each linked enemy after distance/6 s along a visible thread, at 40% of the hit, to at most 4 enemies per cast; solo damage 4 to 5. Kills the N^2 growth and makes the network visible.<br>**S:** threads that pulse outward; a note rings on each arrival.<br>**Snd:** `resonance_ring` (struck fork with beating overtones; echoes arrive later and softer) |
| decree | three rings from the mouth, `elder_guardian_curse` 1.8; a thread, seal and circle over the target while held | **Good** | **M:** the held target is **Condemned**: your next 2 hits on it within 4 s do +40%; if the decree holds 3 or more, the 2 health isn't taken. A stun and a payoff.<br>**S:** kept (rings, seal); the seal turns crimson while it condemns.<br>**Snd:** `decree_gong` (low gong, choir formant, cut) |
| reflect | mark: three tilted glass rings, star seal, glass bits, `amethyst_block_hit` 1.4; reflection: ray, glass, `amethyst_block_hit` 1.6 | **Good** | **M:** the mirror returns damage as arcane, armour ignored, and cracks with each reflection (each costs 1 s of its 10; at most 6). Removed from passives, or capped at 30% there.<br>**S:** the faceted shell (kept) losing a facet per reflection, shattering on the last.<br>**Snd:** `reflect_mirror` (glass ting, shard tinkle) |
| swap | two star seals and rings, a ray between, `note_block_snare` 1.2 + `enderman_teleport` 1.6 | **Good**, a little template | **M:** swaps danger too: enemies that were after you for the next 3 s turn on your swap partner if it is your ally, or lose you if it's an enemy.<br>**S:** two arcs crossing in mid-air rather than two seals.<br>**Snd:** `swap_snap` (a snap and a reverse swoosh panned left/right) |
| barrier | 3 closing bands, star sigil, flash, `beacon_power_select` + `amethyst_cluster_place` | **Good** | **M:** when the absorption is used up, the light bursts: a 2.5-block ring shoves enemies back 1 block and slows them 1 s. Numbers unchanged.<br>**S:** hex bands (kept); the burst on breaking.<br>**Snd:** `barrier_hum` up, `barrier_pop` on break |
| span | a circle at your feet; a flat ring and glow per glass block, `amethyst_block_place` every third; **shatter: 2 sparkles per block, silent** | **Silent** ending | **M:** none needed; numbers unchanged.<br>**S:** glass tiles fade in one after another as a run of chimes; the shatter cascades away from you.<br>**Snd:** `span_glass_run` (a ladder of pings 60 ms apart) and `span_glass_shatter` (crash cascade) |
| summon | star seal, 1.8-tall ray, flash, ground ring, soul motes, shimmer, `evoker_cast_spell` 1.2 | **Scale**: the most powerful rune of the group opens like a small buff | **M:** price: the wolves lose the free Strength I (4 per bite; +1 Strength level per Amplify), and while any lives your mana regeneration is -25% (you are sustaining them); when a wolf expires or dies it pops in a starlight burst (3 arcane damage, Exposed).<br>**S:** one big circle with three wolf-head sigils and a howl; each wolf steps out of its own sigil.<br>**Snd:** `summon_howl` (ethereal formant howl, bell; a small yip per wolf) |
| twin_star | armed: two orbiting stars, star seal, chime 1.8; second cast: flash at the hand, ring, star, `amethyst_block_resonate` 1.9 | **Good**, though the second cast isn't visibly a second caster | **M:** the second cast is 75% power and comes from a mirrored twin 3 blocks to your side (fired from its origin toward the same aim). Removes the near-2x on big spells.<br>**S:** a translucent silhouette (ring plus star, no entity) mirrors your gesture.<br>**Snd:** `twin_echo` (doubled bell with 150 ms echo; the second cast an octave down) |
| nullify | star seal, ally: shimmer and ring; enemy: implode and witch sparks, `illusioner_cast_spell` | **Good** | **M:** also unmakes magic-made creatures it hits: vex, and spirit wolves or shades that aren't yours dissolve. Strips as now.<br>**S:** rings collapsing inward and losing colour (desaturating motes).<br>**Snd:** `nullify_snuff` (reversed chime, candle-snuff thup) |
| treasure_sense | star seal, gold dust, chime 1.6; glints over containers every 2 s | **Template** and off-fantasy (every chest, hopper, dispenser) | **M:** dowsing: only containers with an unopened loot table and suspicious blocks count, range 24 to 48; a drifting trail of glints (one every 2 s, low along the ground or air) leads to the *nearest*; Luck II as now. Fixes the base-raiding glints and the 24-slot waste.<br>**S:** a thin line of gold motes drifting toward the treasure.<br>**Snd:** `treasure_glint` (coin jingle, one soft ping per pulse) |
| fangs | flat star sigil (2.6), shimmer, `evoker_prepare_attack`; then vanilla fangs (the known silhouette) | **Good** | **M:** the ring stays 2 s as a cage: anything crossing it is bitten (6, once per fang, 1 s apart); numbers otherwise as now.<br>**S:** kept.<br>**Snd:** `fang_snap` (three bone clacks) layered on the vanilla bite |
| starlight_tether | ray anchor to target each 4 ticks, anchor star seal and glow on a pull, `amethyst_block_chime` 1.4 + `impact_arcane` | **Good** | **M:** price: 2 damage per pull at most once a second (max 12), duration 6 to 5 s; bosses stay slowed.<br>**S:** the thread thickens as it tightens; the anchor is a small star that flares on every pull.<br>**Snd:** `tether_pluck` (harp pluck, thread hum) |
| starshard | ray from 8 above (2 layers), arcane impact, star seal, `amethyst_cluster_break` 1.2 + `impact_arcane`; sparks as rays | **Good** | **M:** 9 to 11 damage; 3 sparks of 4; a spark that kills splinters once more (3 sparks of 3). A world-event rune now outclasses Harm.<br>**S:** kept.<br>**Snd:** `starshard_split` (crystal ping then three quick descending ticks) |
| manaburn | violet-pink flame tongues (3, or 6 on a caster), arcane impact, enchant motes, `impact_arcane` (pitch 0.7 on casters) | **Good** | **M:** interrupts: cancels a charging player's charge and a Runebound's telegraphed cast (its next cast delayed 4 s). Silence locks, Manaburn cuts in.<br>**S:** kept, with a vacuum-suck of motes on a caster.<br>**Snd:** `manaburn_hiss` (electric fizz, inward whoosh) |
| manatide | enchant motes flung inward, orbit, star seal, `beacon_power_select` 1.6 | **Template** | **M:** the tide follows your casting: for 10 s each spell you cast returns 25% of its cost (30 total at most); each player still once a minute. Cost 12 to 10.<br>**S:** a small draw of motes into the hand on every cast (instead of a seal).<br>**Snd:** `manatide_inhale` (swelling upward shimmer; a quick inhale tick per refund) |
| spellbrand | star sigil layered over the head, ring, 10 enchant motes, `enchantment_table_use` 1.4; burst: flash, 2 rings, 14 hit sparks, `amethyst_cluster_break` | **Good** | **M:** the burst takes the element of the spell that triggers it (fire spell: 7 fire damage that also ignites and sets off fire reactions; storm: storm; arcane: arcane and Unweave); 6 to 7 damage. The universal reaction amplifier.<br>**S:** the brand is a small sigil; the burst is coloured by the trigger.<br>**Snd:** `brand_stamp` on apply, `brand_shatter` on burst (glass burst pitched by the trigger's element) |
| prismatic_burst | star seal scaled by marks, orbit, an arc per mark in its element's colour, rising chime per mark (`amethyst_block_chime` on a chord), white ring | **Good**: the best presentation in the group, on the weakest rune | **M:** 5 + 4 per mark (cap 5 = 25); and each mark it eats leaps to up to 3 other enemies within 4 blocks (re-applied in its own colour): the crowd setup for Unweave and reactions, not a worse Harm.<br>**S:** kept; the coloured arcs fly out to the neighbours.<br>**Snd:** keep the chime ladder |
| halo | ring over the head with a star seal, petals, glow motes, `amethyst_block_chime` + `impact_arcane`; smite: ray, arcane impact, life ring, `amethyst_cluster_break` | **Good** | **M:** 4 damage every 1.5 s (5 smites, 20 total, 60 vs undead), 1 heal each; and when an enemy hurts the ally, the next smite goes to *that attacker* (a guardian, not a turret).<br>**S:** kept; the halo flares when it answers an attacker.<br>**Snd:** `halo_ring` (angelic bell chorus; ting-zap per smite) |
| cometfall | mark on the ground (star seal + target ring), falling comet with a tail, huge impact, screen shake, `generic_explode` + `amethyst_block_break`; shards as arcs | **Good**: the model of scale following power | **M:** shards prefer Exposed/Revealed enemies; the crater smoulders 3 s. Numbers unchanged.<br>**S:** kept.<br>**Snd:** `comet_fall` (a 1 s rising whistle with a sub rumble, boom, crystal shatter) |

---

## 3. Clusters and how each is broken up

Each cluster lists what the members share now, then how each becomes distinct in mechanics (delivery, targeting, interaction, twist). The proposals are the ones in 2b; this is the map.

### 3.1 "Put health back" (life)
Shared: an instant or ticking heal on one ally, a bloom and leaf spiral, an amethyst sound.

| rune | delivery | who | interaction / twist |
|---|---|---|---|
| Heal | instant | one (shapes multiply) | overheal becomes Absorption: the only heal that shields |
| Regrowth | ramp over 8 s (I to II to III) | one | the sustained one; passive-eligible; a vine winds up the body |
| Restore | instant, small | one | gear: mends 8% per item once a minute, fire out |
| Remedy | cure plus small heal | one | transmutes ailments into boons |
| Cleanse | instant wipe | any | washes elemental marks off allies too |
| Bloom | HoT that spreads | one, then neighbours | pollen: Regen I hops to allies within 4 blocks |
| Lifebloom | HoT then burst | one, then a ring | ripens: burst after the last beat |
| Stitchtime | delayed | one | heals what you are about to suffer |
| Moonpetal | instant, area | enemies and allies | waxes with the moon; the only hybrid |

### 3.2 "Cheat death" (life; cross-element with Cryostasis, Foresight, Riposte, Borrowed Time)
Reversal and Second Wind share `DeathsDoor`'s 60 s rest and the same "a killing blow is turned" rule. Distinct roles: **Reversal = stand and fight** (half health, cleanse, Resistance II 3 s, T4, whole party through Burst); **Second Wind = get out** (4 HP, Regen II, Speed II 4 s and a shove that clears the enemies around you, T3). Soulbond stays the third way (share, not cheat). Cryostasis is the frost version (sealed, unhurtable); Foresight and Riposte dodge; Borrowed Time undoes; each is already distinct by trigger, so only the two life ones needed splitting.

### 3.3 "Hit back" and "shelter" (life and arcane; cross-element with Geode, Stoneform, Stormheart, Ashen Veil, Frostbloom, Riposte, Mirrorfrost)
Bramble (melee thorns, 4.5 blocks), Reflect (percent of any blow, any range), Halo (an ally's guardian) all "hurt whoever hurt you" with slightly different arithmetic. Distinct triggers and payloads:

| rune | trigger | payload | limit |
|---|---|---|---|
| Bramble | you are hit from within 4.5 blocks | 3 damage and a shove | 5 thorns |
| Reflect | you are hit by anyone | 60% of the blow as arcane, armour ignored | cracks: 6 reflections |
| Halo | an ally is hit, or every 1.5 s | a holy smite on the attacker, ally heals 1 | 5 smites |
| Haven | an enemy enters the dome | shoved out; projectiles glance | 8 s |
| Barrier | its absorption is used up | a shoving ring | once |
| Geode/Stoneform/Stormheart/Ashen Veil (other elements) | hit in melee | crystal shards / aftershock / lightning / ignite | for the lead to split the same way: one trigger and one payload each |

### 3.4 "Stop this monster" (life and arcane; cross-element with Root, Shackle, Jolt, Freeze, Glacier, Stasis, Bubble, Weigh)
| rune | what stops | what breaks it | what it leaves |
|---|---|---|---|
| Decree | everything hit, 2 s, true stun | time | a Condemned mark (+40% next 2 hits); costs 2 HP unless it holds 3+ |
| Rootsnare | everything in 3 blocks, 1.5 s, true hold | time | a thorn tax: Slowness II and 1 damage per 1.5 blocks |
| Drowse | one or many, 6 s asleep | **any damage** | a x1.75 backstab on the first blow |
| Silence | casting (Runebound, players) 3 to 4 s; melee damage (Weakness I) | time | nothing; the lock is the point |
| Sporebloom | 5 s of confusion: enemies attack each other | time or death | Poison I and nausea |
| Starlight Tether | a leash: pulled back beyond 3 blocks, 5 s | time | up to 12 damage |
The other elements' holds need the same treatment (each says what it stops, what breaks it, what it leaves); Root in particular is now the plainest.

### 3.5 Arcane damage: strike, split, trap, cut in, mark, cash in (arcane)
| rune | delivery | targeting | interaction |
|---|---|---|---|
| Harm | instant needle | one | marks Exposed (an Unweave mark); the plain strike |
| Smite | telegraphed column 0.7 s | one | strips Absorption; x2 undead |
| Starshard | instant, then sparks | one, then 3 | sparks splinter again on a kill |
| Fangs | 0.4 s delayed from below, ring stays 2 s | one, plus anyone crossing | a cage |
| Manaburn | instant | one | cancels a charge or a telegraphed cast; drains mana |
| Spellbrand | delayed by your next spell | one or many | bursts in the trigger's element |
| Prismatic Burst | instant | one, then neighbours | eats marks and re-applies them to 3 neighbours |
| Resonance | instant, then a travelling echo | a network | the crowd punisher, capped |
| Starfall | 8 stars over 2 s, telegraphed | area | seeks Exposed enemies, leaves pools that expose |
| Cometfall | 1 s telegraph | area plus shards | shards seek Exposed |
Unweave stays arcane's reaction; Exposed is the only new mark, set by Harm, Reveal and (via pools) Starfall, read by Unweave, Prismatic Burst and the sky strikers.

### 3.6 "Make light" (arcane and life)
| rune | where | how long | twist |
|---|---|---|---|
| Light | on a creature or a block | 60 s | follows its target: the only mobile lamp |
| Glimmer | any face | permanent | a cyan-green ripple of lichen over a wall |
| Glowvine | a ceiling | permanent | berries you can eat, re-ripened on a recast |

### 3.7 "Make plants grow" (life)
| rune | verb | twist |
|---|---|---|
| Grow | now | instant bone meal; also ages baby animals up |
| Ancient Seed | slowly | a 15 s garden pulse (+1 stage every 3 s, 4 blocks) and a flower |
| Harvest | reap | every ripe plant without killing it, replanted |
| Nourish | feed | hunger for players; heals and breeds tamed animals |

### 3.8 Movement (arcane and life; cross-element with Blink, Warp, Shadowstep, Zipper, Time Skip)
Swap (trade places and danger), Bloomstep (party door, regen, garden), Span (a bridge you can walk back over). Blink is now the solo long jump; Bloomstep no longer dominates it because it trades range (24) for company.

### 3.9 Buffs (arcane and life)
Haste (charge speed), Empower (melee, with a comedown), Fortune (luck and loot), Twin Star (a twin), Manatide (refund as you cast), Night Eye (sees the hidden), Treasure Sense (dowsing). Every one now changes a different thing: tempo, melee, loot, doubling, mana, sight, direction.

### 3.10 Guardians and summons (arcane)
Summon (bodies, with an upkeep) versus Halo (an angel on an ally); Shades (void) and Thunderbird (storm) are the cross-element peers, and Halo is now the only one that answers attackers.

### 3.11 Poison (life; cross-element with Parasite, Blood Moss, Blight, Bleed)
Venom (lethal, contagious) and Sporebloom (confusing); Parasite (blood) and Blight (reaction) should follow the same rule: **poison never depends on the vanilla effect for its damage**, since vanilla Poison is ignored by undead and spiders and can't kill.

---

## 4. Cross-element and caster-feel observations

**4.1 The cast cue belongs to the element, not the rune.** `SpellCaster` draws `Vfx.castCircle` with the theme of the *first group's first effect* and plays `circle_open` (0.35) and `cast_<element>` (0.55). Every life spell therefore opens with the same 1.6 s soft bell bloom (two variants) and every arcane spell with the same 1.4 s strum; `Self · Heal`, `Self · Regrowth`, `Self · Restore` and `Self · Bramble` are identical for the first 200 ms except for the runes drawn in the circle. The cast pose is chosen from the *first rune* (`CastingPose`: the shape), so it never depends on the effect either. Both cast sounds are also long (1.4 to 1.6 s with reverb), so rapid casting piles them on top of each other.
- Proposal for the lead: add a **role stinger** (0.15 to 0.4 s: strike, restore, ward, mark, world, summon, step) chosen by the first effect's role, layered under the element sound and pitch-shifted by tier: seven stingers per element cover every rune cheaply (section 5). Add an element-specific hand cue (life: a seed of light and a soft exhale; arcane: a star flicker) in place of the plain glow at the hand.

**4.2 What is custom and what is vanilla.** `sounds.json` holds 20 cast/impact events (2 and 3 per element) plus casting, event, boss and familiar sounds; every rune-specific cue in my group is a vanilla sound layered on top, and the two elements lean on amethyst (`amethyst_block_resonate`, `_chime`, `_hit`, `_break`, `amethyst_cluster_place/_break/_hit`): Heal, Regrowth, Light, Reveal, Barrier, Span, Reflect, Restore, Lifebloom, Soulbond, Haven, Halo, Tether, Starshard, Spellbrand, Treasure Sense, Twin Star, Manatide, Second Wind. Where a rune escapes that, it borrows a vanilla sound that means something else: Harm plays the melee crit, Venom a spider's hurt, Reversal the Totem of Undying (Fangs' evoker sounds and Decree's Elder Guardian curse are apt). `tools/sound_art.py` already builds everything on the D major pentatonic, so per-rune phrases on a shared instrument per element will harmonise without extra work.

**4.3 Templates.** 21 of 28 arcane runes open a star seal or star sigil (all but Night Eye, Light, Silence, Resonance, Decree, Span, Manaburn); about 19 of 27 life runes are built from bloom, leaf spiral and petals (the exceptions are Venom, Cleanse, Bramble, Haven, Glimmer, Fortune, Soulbond, Second Wind, Stitchtime). Two grammars in 2b fix it: a seal means a lasting mark on someone, a bloom means restoration.

**4.4 Shared impact helpers.** Every effect ends with `Vfx.touched` (a glow and 3 motes in the element's mote) on each target, and every shape lands with `Vfx.impact`: for arcane those are `ENCHANT` motes and `impact_arcane`; for life `HAPPY_VILLAGER` and `impact_life`. `ScreenFx.punch` fires for *any* spell hit of 8 or more, so a Smite, a Starshard and a Cleave feel the same in the hand. There is no per-rune hit-confirm for the caster and no damage numbers.

**4.5 Multi-target flourishes are uncapped.** `Vfx.harm` (two flashes, a seal, an orbit, 8 hit sparks, a shimmer, a sound) plays for every target; a Zone·Harm on 10 enemies draws 10 seals every second. Sporebloom and Moonpetal draw 45 to 65 particles each. Only passives have a quiet mode (`Fx`). Suggestion: full flourish on the first 3 targets, `Vfx.touched` for the rest.

**4.6 Shapes and Linger multiply per-hit effects; only some runes are guarded.** Heal, Restore, Harm, Resonance, Smite and Venom stack per pulse. Second Wind, Reversal, Manatide, Halo's crown, Stitchtime's stitch, Doomclock and Lifebloom's burst guard themselves (lockouts, "one renewal, never two"). The unguarded per-hit effects are the ones that end up over or degenerate: Zone·Heal (6 HP/M for four allies), Zone·Restore (30% gear repair per cast), Zone·Resonance (9 to 22 dmg/M). Suggestion: a global "diminishing return within one cast": the k-th heal of the same target inside a cast counts 100%, 60%, 40%... Restore gets the per-item lockout in 2b.

**4.7 Passives.** The allow-list in `Passives` includes Empower (1.44 M/s for permanent Strength II), Reflect (1.9 M/s for a permanent 60% mirror), Haste and Regrowth. Empower and Reflect are the two that break the "sustainable" intent; cap Empower at Strength I and Reflect at 30% inside a passive (2b).

**4.8 "Helpful" stops at your team.** `Targets.canHelp` means Heal, Regrowth, Remedy and Nourish never work on villagers, iron golems, wild animals or horses that aren't yours (an owned horse or wolf does count). For the element of living things that is a gap; the two proposals that touch it are Nourish (love mode for pets) and Grow (ages babies). If the lead wants the rest, the change is one predicate for HELPFUL runes tagged life.

**4.9 Fusions are cheap.** Fusion matches element tags only, so Lifebloom comes from any two life effects (Grow + Glimmer, 6 mana of rune costs) and Prismatic Burst from any two arcane ones (Night Eye + Light). A fused T3 rune should beat its ingredients by a clear margin; Bloom and Prismatic Burst currently don't.

**4.10 The T1 damage poke is the same in every element.** Harm 7/8, Shock 4/7, Ember 3+burn/6, Icicle 4 to 6/6, Pelt 4/5, Windcut 4/6, Leech 3/8, Countdown 6/7, Umbra 4 to 8/6: a one-target 3 to 7 damage hit at 5 to 8 mana, each with one tag (a burn, a shove, a heal, a delay). Life has none, which is a real gap: Venom (T2) is its first. The lead may want each element's poke to differ in delivery (instant, delayed, arc, lingering) rather than in a tag.

**4.11 Armour almost never matters for these two elements.** Nearly every arcane and life damage rune is `indirectMagic`, which ignores armour (Harm, Smite, Starfall, Resonance, Fangs, Vinelash, Starshard, Spellbrand, Prismatic Burst, Halo, Cometfall, Tether, Manaburn, Rootsnare, Moonpetal), unlike fire, frost and storm damage. "Ignores armour" is therefore not a Harm-only property: keep it as the arcane baseline and say so in the docs rather than treating it as a perk.

**4.12 Vanilla Poison and Regeneration are ignored by undead** (`IGNORES_POISON_AND_REGEN`, all of `#minecraft:undead`), and spiders are immune to Poison. This hits Venom, Sporebloom, Parasite (blood), Blight's poison, and Remedy/Regrowth/Zephyr on undead allies. Rule for the lead: damage that is meant to be reliable must not lean on the vanilla effect.

**4.13 The climate hooks are the smallest of any element** (life +10% in sun, arcane +15% on a ley line). Moonpetal's lunar scaling and Sporebloom/Ancient Seed reading the biome are the kind of small environmental identity the other elements already have.

**4.14 Innates are tuned against nothing.** Fortune and Twin Star are one of ten innates a player gets at random, growing +6% power per circle. A random gift can't be much stronger or weaker than its siblings: Fortune (+50% expected damage) and Twin Star (a free second cast) should be compared against Stoneform, Stormheart, Kindling, Blood Thread, Mirrorfrost, Phantom, Gale Mantle and Borrowed Time by the same lead pass.

---

## 5. Sound needs

`tools/sound_art.py` already builds everything from `bell`, `glass`, `sparkle`, `sweep`, `thump`, band-limited and moving noise, `chorus` and `reverb`, on the D major pentatonic (D E F# A B), so any new phrase harmonises with the cast and impact sounds. Two layers are needed: **role stingers** that make the first 200 ms differ by *what kind of spell it is*, and **rune voices** that make each rune's landing its own. Nothing below needs a new instrument, only a new phrase; sounds built for one rune may be reused where a row lists several.

### 5.1 Role stingers (0.15 to 0.4 s, layered under `cast_<element>`, chosen by the first effect's role)

| stinger | element | brief | used by |
|---|---|---|---|
| `stinger_restore` | life | two soft marimba-like notes up a third (D to F#), air on the tail, 0.3 s | Heal, Regrowth, Restore, Remedy, Cleanse, Bloom, Lifebloom, Stitchtime, Nourish, Moonpetal |
| `stinger_ward` | life | a low warm chord swell with a soft click, 0.35 s | Haven, Bramble, Soulbond, Reversal, Second Wind |
| `stinger_world` | life | one bright bloop and a wood tick, 0.2 s | Grow, Harvest, Glimmer, Glowvine, Ancient Seed, Bloomstep, Fortune |
| `stinger_thorn` | life | a dry twig snap and rasp, 0.2 s | Venom, Vinelash, Rootsnare, Sporebloom, Drowse |
| `stinger_strike` | arcane | a dry high "tsk" and a short low knock, 0.2 s | Harm, Smite, Fangs, Starshard, Manaburn, Starfall, Cometfall |
| `stinger_mark` | arcane | a page-slam thunk with a shimmer, 0.25 s | Spellbrand, Reveal, Resonance, Decree, Silence, Prismatic Burst, Nullify |
| `stinger_glass` | arcane | a glass-hex click with a bright hum, 0.3 s | Barrier, Reflect, Span, Haste, Empower, Halo |
| `stinger_step` | arcane | a snap and a short swoosh, 0.25 s | Swap, Light, Night Eye |
| `stinger_summon` | arcane | a bell and a breath, 0.4 s | Summon, Twin Star, Manatide |

### 5.2 Rune voices (one per rune or a shared family)

| sound | verb, element | brief | length | used by |
|---|---|---|---|---|
| `heal_chime` | restore, life | two bell notes D to F# (a rising third) with an airy tail and a faint high glint; overheal adds a second glint | 0.6 | Heal |
| `regrowth_unfurl` | grow over time, life | a slow swell of band-limited leaf noise (2.5 to 4.5 kHz) with three marimba ticks D, F#, A marking the ramp stages | 0.9 | Regrowth |
| `mend_tap` | repair, life | two hollow wood taps, a tiny anvil ring and a warm chime | 0.6 | Restore |
| `purge_pour` | cleanse, life | descending band-passed noise like poured water resolving into a clear bell; Remedy version adds a rising glide at the end | 0.7 | Cleanse, Remedy |
| `grow_pop` | grow, life | a rising run of soft sine-glide bloops 40 to 80 ms apart with a wood creak: fast and bright (Grow), glassy and quiet (Glimmer), slower with a crack tick (Ancient Seed), a chord bloom (Bloom) | 0.4 to 0.8 | Grow, Glimmer, Ancient Seed, Bloom |
| `harvest_sweep` | reap, life | a dry scythe swish (band-noise sweep 3 to 1.5 kHz) and three rustle ticks | 0.4 | Harvest |
| `berry_plip` | light, life | descending amber plips, one per vine | 0.5 | Glowvine |
| `seed_crack` | grow, life | a shell crack and a soft tick per pulse | 0.4 | Ancient Seed |
| `nourish_munch` | feed, life | a soft double crunch of filtered noise under a warm low hum | 0.4 | Nourish |
| `venom_bite` | poison, life | a wet snap and a short hiss; the contagion hop is a tiny hiss | 0.4 | Venom |
| `thorn_snap` | punish, life | a crackle of growing twigs when grown; a sharp snap when spent | 0.3 | Bramble |
| `vine_crack` | whip, life | a bullwhip crack with a leaf rustle | 0.35 | Vinelash |
| `root_grip` | entangle, life | a wooden creak sweeping down, a snap, a soil thump | 0.6 | Rootsnare |
| `spore_puff` | confuse, life | a muffled low "fwump" with a granular hiss and a detuned wobble | 0.7 | Sporebloom |
| `lull_pad` | sleep, life | a detuned pad falling a fourth over 1 s with a faint music-box tinkle; the wake hiccup is a short pitch-up | 1.0 | Drowse |
| `dome_pad` | shelter, life | an airy chord swell (D, A, E) with a soft breathing pulse every second and a descending glide on close | 1.2 | Haven |
| `soul_link` | bond, life | two bells a fifth apart with slow beating; a tiny bell tick per shared wound; a dissonant crack on snap | 0.8 | Soulbond |
| `deaths_door` | revive, life | two low heartbeats, a door creak, a rising four-voice chord (chorus), one bright bell | 1.5 | Reversal |
| `second_wind_gasp` | escape, life | a sharp rising inhale (noise burst) into a gust and a bell | 0.7 | Second Wind |
| `luck_coin` | luck, life | a metallic flutter ring (ring-modulated) and sparkle; a brighter ding on a proc | 0.5 | Fortune |
| `lifebloom_pulse` | ripen, life | a marimba tick one pentatonic step higher each beat, a soft chord on the burst | 0.3 per beat | Lifebloom |
| `bloomstep_door` | step, life | an airy "shoop", a petal flutter and two soft leaf footsteps | 0.6 | Bloomstep |
| `stitch_tick` | thread, life | a thread-pull zip, a few clock ticks, a tight pluck on close | 0.5 | Stitchtime |
| `petal_storm` | storm, life | swirling airy noise with a descending moon-bell and glitter | 1.0 | Moonpetal |
| `harm_needle` | strike, arcane | a dry high noise "tsk" and a resonant glass thump | 0.3 | Harm |
| `fang_snap` | bite, arcane | three bone clacks, layered under the vanilla bite | 0.4 | Fangs |
| `smite_toll` | judge, arcane | a rising tone across the 0.7 s telegraph, then a low bell toll and a sharp crack | 1.3 | Smite |
| `star_rain` | fall, arcane | sparse twinkles; per star a falling whistle and a pentatonic plink at a random scale degree | 0.4 per star | Starfall |
| `comet_fall` | fall, arcane | a 1 s rising whistle over a sub rumble, then a boom and a crystal shatter | 1.6 | Cometfall |
| `starshard_split` | split, arcane | a crystal ping then three quick descending ticks | 0.5 | Starshard |
| `resonance_ring` | link, arcane | a struck-fork ring with beating overtones; each echo arrives later and softer | 1.2 | Resonance |
| `decree_gong` | command, arcane | a low gong with a formant "ah" (filtered saw), cut short | 0.8 | Decree |
| `silence_hush` | quiet, arcane | a low-pass sweep down to silence (a vacuum) and a tiny tick; nearby cast sounds duck | 0.6 | Silence |
| `manaburn_hiss` | burn, arcane | electric fizz with an inward whoosh (reversed noise) | 0.5 | Manaburn |
| `manatide_inhale` | drink, arcane | a swelling upward shimmer; a quick inhale tick per refund | 0.9 | Manatide |
| `nullify_snuff` | dispel, arcane | a reversed chime and a candle-snuff "thup" | 0.5 | Nullify |
| `reflect_mirror` | mirror, arcane | a glass "ting" and shard tinkle; a soft crack per reflection | 0.6 | Reflect |
| `swap_snap` | trade, arcane | a snap and a reverse swoosh, panned left and right | 0.4 | Swap |
| `barrier_hum` / `barrier_pop` | ward, arcane | a bright hex hum with a click; on break a crystal crack with a soft pulse | 0.8 / 0.5 | Barrier |
| `span_glass_run` / `span_glass_shatter` | build, arcane | a ladder of glass pings 60 ms apart following the growth; a crash cascade running away from you | 1.0 / 1.0 | Span |
| `twin_echo` | double, arcane | a doubled bell with a 150 ms echo; the second cast an octave lower | 0.7 | Twin Star |
| `treasure_glint` | dowse, arcane | a coin jingle with glitter; one soft ping per pulse | 0.6 | Treasure Sense |
| `tether_pluck` | leash, arcane | a harp pluck on each pull over a thin thread hum | 0.4 | Starlight Tether |
| `haste_flutter` | quicken, arcane | three fast rising notes (A, D, F# high) | 0.25 | Haste |
| `empower_thrum` | power, arcane | a sub-bass rise with crackle; a soft exhale on the comedown | 0.6 | Empower |
| `night_eye_open` | see, arcane | an airy inhale and two soft high chimes | 0.5 | Night Eye |
| `reveal_ping` | expose, arcane | a sonar ping with a ring | 0.5 | Reveal |
| `light_kindle` | light, arcane | a match strike into a bell; a faint hum while it follows | 0.5 | Light |
| `summon_howl` | call, arcane | an ethereal formant-filtered howl over a bell; a small yip per wolf | 1.6 | Summon |
| `halo_ring` | guard, arcane | an angelic bell chorus on opening; a "ting-zap" per smite | 0.8 | Halo |
| `brand_stamp` / `brand_shatter` | mark, arcane | a sealed page thunk on apply; a glass burst pitched to the element of the spell that set it off | 0.4 / 0.6 | Spellbrand |
| (keep) `prism_chord` | cash in, arcane | the existing chime ladder over a chord | | Prismatic Burst |

That is 9 stingers and about 54 voices (pairs counted separately) for 55 runes. Many rows are variants of one instrument (`grow_pop`, `purge_pour`, the thorn family, the glass family), so the authoring is closer to 35 distinct builders.

---

## 6. Proposed implementation order, effort and risks

Effort: **S** under an hour, **M** one to three hours, **L** half a day or more. Nothing depends on the sound kit; land the mechanics first, then the kit, then the visuals.

### Wave 0: broken or off-spec (do regardless)
| item | effort |
|---|---|
| Treasure Sense: only containers with an unopened loot table and suspicious blocks; nearest first (then the dowsing trail in wave 2) | S |
| Restore: per-item lockout so Zone/Linger/Pulse can't repair without limit | S |
| Silence: interrupt a Runebound's telegraphed cast (`Runebound.State.castAt`) and a player's charge (`Charging.FIZZLED`); cast lock 3 to 4 s | M |
| Nourish: heal and love mode on pets (or refuse the cast without spending) | S |
| Light: say why when the cast fizzles (a non-air cell such as tall grass or a snow layer) | S |
| Span shatter and Haste get sounds | S |
| Venom, Sporebloom (and Parasite from the blood audit): stop leaning on vanilla Poison for their damage | M |

### Wave 1: numbers and prices (mostly constants; ship together, then playtest)
Bramble 5 thorns; Fortune x2 and loot re-roll; Empower comedown and passive cap; Reflect crack and passive cap; Halo 4 x 5 and attacker targeting; Bloom 14 M; Starshard 11 and re-splinter; Resonance capped echo and 5 damage solo; Summon price (no free Strength, regen -25%); Tether 1 damage/s; Second Wind flee; Twin Star 75%; Smite telegraph and 15 damage; Restore 4 HP and 12 M.

### Wave 2: new mechanics (independent, one rune each unless noted)
Exposed mark with Harm, Reveal, Starfall pools (**one shared change**: `Reactions.Mark`, `WOVEN`, `FusedVoid.useMarks`, tooltips, `ReactionRules`) (M/L); Prismatic Burst spread (M, after Exposed); Spellbrand trigger element (S); Heal overheal (S); Regrowth ramp (M); Cleanse marks (S); Remedy transmutation (M); Sporebloom confusion (M/L, mob target AI); Drowse backstab (S); Decree Condemned (S); Haven shove (M); Rootsnare thorn tax (M); Vinelash PULLED and trip (S); Moonpetal lunar (S); Light follow (M); Night Eye invisible (M); Grow babies (S); Harvest broader (M); Ancient Seed pulses (S); Glowvine re-ripen (S); Bloomstep party (M); Swap aggro (M); Barrier burst (M); Nullify unsummon (S); Fangs cage (S); Manaburn interrupt (M); Manatide refund (M); Twin Star mirrored twin (M, mostly visual); Treasure Sense dowsing (M).

### Wave 3: presentation and sound
Build the sound kit in `tools/sound_art.py` (stingers first: L for 9, then voices in the order Harm, Heal, Regrowth, Restore, Reversal, Smite, Silence, Summon, Cometfall, then the rest: about 2 days for everything), register events and subtitles (`sounds.json`, `WildercordSounds`, the generator's `NEW_LANG`), then apply the star-seal and bloom rules rune by rune (S/M each), the role stinger and hand cue (M), and the crowd LOD for per-target flourishes (M).

### Risks
- **Balance.** Resonance, Summon, Fortune and Bramble are the numbers most likely to need a second pass after the caps; Exposed adds a mark that Unweave and Prismatic Burst count, so Unweave's "two or more marks" becomes easier to reach (Exposed is a 3 s mark, and Harm can't unweave off its own).
- **Fortune's loot re-roll** touches death loot; it needs an accessor or event and a per-kill guard so a farm can't be built on it.
- **Silence locking players** is PvP-relevant; keep the player lock short (3 s, like Drowse on players) and put it behind the PvP scale.
- **Light following** moves light blocks each few ticks: cap one per caster and move only when the cell changes.
- **Sporebloom infighting** competes with vanilla target goals (they may overwrite the target); test with piglins, endermen and iron golems.
- **Bloomstep party** must reuse `SignatureFusions.landing` per ally.
- **Tests that pin today's numbers or behaviour:** `WildercordFusedLifeTest` (Soulbond, Second Wind, Lifebloom), `WildercordSignatureFusionTest` (Bloomstep, Stitchtime, Halo, Cometfall), `WildercordFusedVoidTest` (Prismatic Burst), `WildercordNewRunesTest` (Fangs, Sporebloom), `WildercordNewRunes2Test` (Spellbrand, Drowse, Reveal, Venom), `WildercordEventsTest` (Manaburn, Manatide), `WildercordWorldMagicTest` (Grow, Harvest, Glimmer), `WildercordReactionsTest` (Venom, Harm), `SignatureRulesTest`, `FusedLifeRulesTest`, `FusedVoidRulesTest`, `ExplorerNumbersTest`, `CraftedRunesTest`, `ReactionRulesTest`, `HeartAndPassivesTest` and `LoadoutRulesTest` (passive lists), `FusionTest`.
- **No functional test exists today** for Cleanse, Empower, Summon, Smite, Silence, Nourish, Twin Star, Fortune, Night Eye, Haste, Light, Decree, Resonance, Reversal, Reflect, Restore, Swap, Barrier, Bramble, Haven, Span, Regrowth, Treasure Sense, Ancient Seed, Glowvine, Remedy, Vinelash, Moonpetal, Rootsnare, Tether or Starshard beyond screenshots; each one changed in phase 2 needs a gametest first.
- **Docs to update per changed rune:** the description string in `spell/Runes.java`, `rune.wildercord.<id>.desc` in `lang/en_us.json` (and other language files), `docs/DESIGN.md` (roster tables and the reaction and passive sections), `docs/features/*` where the rune is described, and the wiki pages `wiki/runes/effects/life.md`, `arcane.md`, `fused.md`, `innate.md`, `world.md`, which are generated by `tools/wiki.py` from the game's data (regenerate, don't hand-edit). A new mark (Exposed) also touches `ReactionRules` (`SHADOWS`/`BLEEDS`-style sets and the tooltip lines), the Cord screen tooltips and `wiki/spellcraft/reactions.md`.

---

## 7. Broken, off-spec or misleading (fix regardless)

1. **Silence doesn't silence.** It calls `Mob.setTarget(null)` once (a monster re-acquires within about 10 ticks) and applies Weakness II 6 s; nothing stops a Runebound or a player from casting. Its real effect is a 6 s melee-damage cancel (Weakness II is -8), strong against zombie packs and useless against archers and creepers. (`Effects.applyEffect` case `"silence"`.)
2. **Treasure Sense lights up every container.** It collects `RandomizableContainerBlockEntity` and `BrushableBlockEntity`, which includes every player's chest, barrel, hopper, dispenser, dropper and shulker box, and keeps the first 24 in chunk order, not the nearest: near a base the 24 slots go to the base's hoppers, hiding real loot, and it works as a base-finding tool. (`ExplorerEffects.treasureSense`.)
3. **Venom's poison is inert on most overworld hostiles.** Undead (`IGNORES_POISON_AND_REGEN`) and spiders are immune to Poison, and Poison can't kill; the description promises "Poison II for 6 seconds". Sporebloom's Poison I has the same limit, and its Nausea does nothing to mobs.
4. **Restore is an unlimited repair loop.** 5% of every worn and held item per pulse, with no per-item limit: a Zone·Restore mends 30% of all gear per cast for 36 mana, more with Linger, Pulse or Echo.
5. **Resonance grows with the square of the crowd** and, in a Zone, again by the number of pulses: 9.4 damage per mana with 5 enemies, 22 with 8 (arithmetic in the appendix). Probably not intended.
6. **Nourish spends mana and does nothing on pets** (`t instanceof Player` only), though pets count as allies.
7. **Light fizzles silently** when the target cell isn't air (tall grass, snow layers); **Span's shatter** and **Haste** have no sound.
8. **Prismatic Burst's tooltip** lists nine marks but counts at most six (22 damage); harmless, but say so.
9. **Bloomstep** is strictly better than Blink at a lower price (a design imbalance rather than a bug, listed with the mechanical fixes).

---

## Appendix A: arithmetic

**Prismatic Burst against Harm + Unweave.** Unweave multiplies by 1 + 0.3 x min(marks, 4) from 2 marks; Prismatic Burst is 4 + 3 x min(marks, 6). Damage per mana (Harm 7/8 M, Prismatic 4/16 M):

| marks | Harm x Unweave | per mana | Prismatic | per mana |
|---|---|---|---|---|
| 0 | 7.0 | 0.88 | 4 | 0.25 |
| 1 | 7.0 | 0.88 | 7 | 0.44 |
| 2 | 11.2 | 1.40 | 10 | 0.63 |
| 3 | 13.3 | 1.66 | 13 | 0.81 |
| 4 | 15.4 | 1.93 | 16 | 1.00 |
| 5 | 15.4 | 1.93 | 19 | 1.19 |
| 6 | 15.4 | 1.93 | 22 | 1.38 |

Prismatic Burst only deals more absolute damage from 5 marks (19 v 15.4) and never more per mana.

**Starfall.** Stars land uniformly in a disc of radius 4 (area 50.3); a star damages any enemy whose box is within 1.6 blocks horizontally: a 3.8 x 3.8 square (14.4) around a 0.6-wide target. Probability per star 0.287; 8 stars: 2.3 hits x 6 = 13.8 damage on a stationary enemy at the centre (0.43/M); 13.5 at 2 blocks out, 9.9 at 3, 6.2 at the rim (Monte Carlo agrees).

**Cometfall.** 16 x (1 - 0.4 x d/4): 16 at the centre, 12.8 at 2 blocks, 9.6 at 4; plus 4 seconds of burning (about 4) and 5 shards x 4 to other enemies within 10 blocks. Single target 16 to 20 for 32 M (0.5 to 0.63/M).

**Resonance.** Each cast on a target does 4, marks it, and hits every already-marked enemy within 16 blocks (at most 8) for 2. A Burst on N fresh enemies: sum over i of (4 + 2(i-1)) = N^2 + 3N; N = 5: 40 for 6 + 14 x 1.5 = 27 M (1.5/M). A Zone (8 + 14 x 2 = 36 M) repeats every second, and from the second pulse every enemy is already marked: per pulse 4N + 2N(N-1) = 60 (N = 5) or 144 (N = 8); over 6 pulses: 40 + 5 x 60 = 340 (9.4/M) and 144 x 5 + 88 = 808 (22/M).

**Heal by shape.** Self 12 M, 8 HP (0.67). Burst 6 + 12 x 1.5 = 24 M; 4 allies x 8 = 32 (1.33/M). Zone 8 + 12 x 2 = 32 M; 6 pulses x 8 x 4 allies = 192 (6.0/M). One Linger x1.8: 24 HP for 21.6 M (1.11/M).

**Linger.** Harm 7/8 = 0.875 base; with one Linger 21/14.4 = 1.46; Smite 10/16 = 0.625 becomes 1.04; Heal 0.67 becomes 1.11: a 1.67x factor on every per-hit rune (three hits for x1.8).

**Bramble.** 3 damage per hit taken from within 4.5 blocks, no cap. Two zombies each hitting once a second for 10 s: 20 procs x 3 = 60 for 6 M = 10/M; a shove of 0.9 (about a second of re-approach) roughly halves the procs: 5/M; against a T1 median of 0.75, 7x to 13x. Reflect (16 M) returns 0.6 x the blow after armour: 1.8 per 3-damage zombie hit (Bramble gives 3), 12 per 20-damage boss hit.

**Fortune.** Expected multiplier 0.25 x 3 + 0.75 = 1.5. A caster spending about 8 mana a second for 10 s spends 80 M for about 64 damage at 0.8 damage per mana; +50% = +32 for a 10 M rune: 3.2 damage/mana against Harm's 0.88 (3.6x).

**Empower.** Strength II = +6 per melee hit. A sword swings every 0.625 s at full attack: 16 swings in 10 s: +96 for 12 M (8/M). As a passive: 12 x 0.12 = 1.44 M/s for +6 forever.

**Summon.** Vanilla tamed wolf: 40 max health (`Wolf.applyTamingSideEffects`), attack 4 (`createAttributes`); Strength I adds 3: 7 per bite at about one bite a second; three wolves 21 damage/s, 420 over 20 s for 30 M (14/M at full uptime). Realistic uptime 35 to 50%: 5 to 7/M against a T4 median of 0.5. Speed II, and Spirits.MAX_SPIRITS = 6.

**Halo against Thunderbird.** Halo: 8 s = 160 ticks; smites at ticks 10, 50, 90, 130 (4), 3 damage each = 12 (36 undead) plus 4 HP for 18 M = 0.67/M (2.0/M undead). Thunderbird: 15 s = 300 ticks, a strike at 30, 60 ... 300 (10) of 5 = 50 for 20 M = 2.5/M. Halo delivers 27% of Thunderbird's damage per mana.

**Silence.** Weakness II = -8 attack damage (4 per level): a zombie's 3 (4.5 on Hard) becomes 0; 6 s of neutered melee for 9 M = 0.67 s/M against a control median of 0.25 (2.7x); nothing against arrows, creepers or spells.

**Venom.** Poison II ticks every 12 ticks (25 >> 1): 120 ticks = 10 damage, plus 2 = 12 for 8 M (1.5/M) on a poisonable mob; on an immune one only the 2 (x1.5 vs undead = 3): 0.38/M.

**Regeneration.** Regen II heals 1 per 25 ticks: 8 s = 6 HP (Regrowth 0.6/M); Regen II 6 s = 4 (Bloom 0.25/M); Regen I 6 s = 2.4 (Remedy's tail).

**Twin Star.** A free second cast at full power for 12 M: total 12 + C for 2C of value: 1.45x at C = 32 and 1.8x at C = 100; at C = 12 it saves nothing.

**Manatide.** 3 mana/s for 10 s = 30, once a minute per player, for 12 M: net +18 a minute; a Twine cord regenerates 300 in a minute, so +6%.

**Restore.** 5% of every item per pulse; Zone x6 = 30% for 8 + 14 x 2 = 36 M.

**Thunderclap, Coldsnap and Flashfire as AoE references (T2, 11 to 12 M, 3 to 5 damage in 3 blocks)** give Moonpetal (4 + 3 heal for 12 M) its "fair in a mixed fight" verdict.

## Appendix B: where each rune lives (for phase 2)

| rune(s) | file |
|---|---|
| Night Eye, Heal, Harm, Haste, Reveal, Regrowth, Cleanse, Empower, Venom, Smite, Starfall, Silence, Nourish, Harvest, Light, Grow, Barrier, Bramble, Haven, Glimmer, Span | `cast/Effects.java` (`applyEffect` switch and the private methods below it); Summon through `cast/Spirits.java` |
| Resonance, Decree, Restore, Swap | `cast/Techniques.java` |
| Reversal, Reflect | `cast/Wards.java`, `cast/DeathsDoor.java`, `TechniqueVfx` |
| Spellbrand, Drowse | `cast/CraftedRunes.java`, `CraftedVfx` |
| Twin Star, Fortune | `cast/Innates.java` (`consumeTwin` in `SpellCaster`, `fortune` in `Effects.hurt`) |
| Vinelash, Remedy, Fangs, Treasure Sense, Ancient Seed, Moonpetal, Sporebloom, Glowvine, Rootsnare, Starlight Tether, Starshard, Manaburn, Manatide | `cast/ExplorerEffects.java`, `ExplorerVfx`, `spell/ExplorerNumbers.java` |
| Bloom, Nullify | `cast/FusedEffects.java`, `FusionVfx` |
| Soulbond, Second Wind, Lifebloom | `cast/FusedLife.java`, `FusedLifeRules`, `FusedLifeVfx` |
| Prismatic Burst | `cast/FusedVoid.java`, `FusedVoidRules`, `FusedVoidVfx` |
| Bloomstep, Halo, Cometfall | `cast/SignatureFusions.java`, `SignatureRules`, `SignatureVfx` |
| Stitchtime | `cast/SignatureWards.java` (`stitch`), `SignatureVfx` |
| Reactions, marks, Unweave, Blight | `cast/Reactions.java`, `spell/ReactionRules.java` |
| Cast cue, circle, poses | `cast/SpellCaster.java`, `Vfx.castCircle`, `client/CastingPose.java`, `cast/Charging.java` |
| Sounds | `content/WildercordSounds.java`, `sounds.json`, `tools/sound_art.py` |
