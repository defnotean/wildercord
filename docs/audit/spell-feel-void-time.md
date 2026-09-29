# Spell feel audit: the runes of void and time

Phase 1 of the "every spell needs a unique feel" pass. Scope: the 49 effect runes whose element is void (36) or time (13), which includes fused (Singularity, Entropy, Devour, Warp, Chronoshift, Timesteal, Reckoning), signature (Doomclock, Riposte, Malison), innate (Phantom, Borrowed Time) and world runes (Echolocate, Hush, Portalfall, Warp Step, Resonant Shriek, Shulkershell, Eclipse, Riftcall, Starmaw).

Everything below was read from the code on branch `spell-audit-void-time` (a merge of local `main`, `f48ab39`). Nothing was played in game; where a claim rests on vanilla behaviour I could not test (mob AI reacting to Blindness, exact fall damage, damage-type tags) it is marked **(unverified)**.

## The short version

**Over (a rune that is clearly best in class, chainable, or screen-filling for a modest result)**

* **Shades** (T3, 22): two hounds at Strength II bite for about 10 a second for 20 s. Potential 18 damage per mana, 4.5 to 9 at realistic uptime, against 2.5 for Thunderbird and a tier 3 median of 0.61. It out-punches the tier 4 Summon by one Strength level (a code inconsistency).
* **Hex** (T1, 5): a permanent-feeling +25% on everything you cast at the target for 8 s, stacking with Eclipse, Cracked and Execute. It repays itself after 15 damage and is 2.7x better than Amplify at 40.
* **Hush** (T2, 10): a 6 s pocket that makes every non-boss monster in 4 blocks forget you, weakened and blinded; 1.8 s of control per mana against three enemies (2.7x Silence), castable on Self as a panic bubble.
* **Foresight** (T3, 14): dodges two whole blows of any size (6x Heal per mana against a Warden), and recasting refills its charges in a Zone or Pulse (Riposte was fixed against that, Foresight was not).
* **Hollow, Doomclock, Dragon Breath**: fair on one target, but quadratic in a pack (Hollow 180 damage on six for 60 mana) or stacked under a repeating shape (Dragon Breath 150 per enemy for 68 mana in a Zone).
* **Borrowed Time** (free after any kill, and a bug: it can repay damage it never healed), **Umbra in the dark** (1.5x Harm; the dark is the default underground).

**Under (weak per mana, dominated by a sibling, or no reason to choose it)**

* **Sonic Boom** (T4, 35): 16 damage for 0.46 per mana; a tier 3 Sanguine Rite does 12 through armour for 14.
* **Wither** (T4, 25): 16 damage in 8 s (0.64); Blackflame (T3, 18) does 18 in 6 s and spreads. **Starmaw** (0.44), **Entropy** (0.47, but 1.4 with Extend), **Devour** (0.31, 0.83 on a kill).
* **Infinity** (T4, 32): stops projectiles only (a 9-mana Deflect does that), the text says nothing reaches you, and in a passive it is a permanent bubble.
* **Time Skip** (Blink does the same job for one mana more and 5x the range), **Warp** (Swap does the swap for half), **Portalfall**, **Shulkershell**, **Chronoshift**, **Timesteal**, **Malison**.

**The biggest clusters** (section 3): drag-it-in (Pull, Gravity Well, Riftcall, Singularity, Hollow: one mechanic in five colours); move-yourself (eight ways, six sharing the implosion-core-ring look and one teleport sound); lose-sight-and-target (Blind, Hush, Veil, Phantom, Echolocate, Eclipse); deferred damage (Countdown, Reckoning, Doomclock, Stasis, Borrowed Time); sidestep (Foresight, Riposte).

**Cross-cutting patterns** (section 4): the cast cue is identical for every spell of an element and the void and time cast sounds arrive 0.6 to 0.9 s late; nine of the 13 time runes draw the same flat gold disc; the caster cannot see their own self-cast (a 1.1-block eye clearance), so self-cast runes are cued by sound alone; marks (Shadowed, Pulled) that decide the reactions are invisible and silent; there are no end cues; every element's tier 1 damage rune is the same poke; repeating shapes multiply every effect that does not guard itself; damage feedback is vanilla only; and only 20 of the 77 custom sounds are used at the rune level, so most runes borrow a vanilla sound that another rune also uses.

## 1. Method and baselines

### What "power" means here

* **Currency is mana.** A cooldown is only `cost x 0.05 s` (10 to 400 ticks, `SpellNumbers.cooldownTicks`), so a caster is limited by mana, not by cooldown, and every comparison below is per mana. (This is also why a cooldown-only reward is weak: see Chronoshift.)
* **Rune cost, not spell cost.** The shape adds `shape cost + effect cost x multiplier` to every rune alike, so comparisons use the rune's own listed mana at power 1.0 (no Amplify, affinity, circle or gear bonus).
* **Counting rules.** Damage over time and delayed damage count in full. Area effects are shown per enemy and, where useful, for a pack of three. A conditional bonus is shown at its condition and without it. Armour-ignoring damage is flagged, not credited. Healing is compared in HP per mana against Heal (8 HP for 12 mana = 0.67).
* **Fair band** = 0.75x to 1.25x of the median of the rune's role. Outside it the rune is flagged under or over, with the ratio.
* **Cast pressure.** Zone, Totem, Domain, Wall, Orbit, Vortex, Linger, Volley, Pulse and Echo all re-apply an effect; only some effects guard against that (Blackflame, Entropy, Reckoning, Doomclock, Stasis, Singularity do; Dragon Breath, Gravity Well, Riftcall, Hollow, Eclipse, Foresight do not). Where that changes a verdict it is stated.

### Reference numbers (damage per mana of the rune, one target, power 1.0)

| Role | Reference runes (per mana) | Median |
|---|---|---|
| T1 single-target burst | Harm 0.88, Ember 1.0 (3 + 3 burn), Pelt 0.80, Icicle 0.67 (1.0 if slowed), Windcut 0.67, Shock 0.57 (1.0 with its arc), Leech 0.38 + heal, Kindling 0.43 (0.71 over a 5-cast cycle) | 0.67 |
| T2 single-target burst | Fire 1.38 (5 + 6 burn), Venom 1.5, Bleed 1.25, Aftershock 1.11, Dismantle 0.90 (armour-ignoring), Stalactite 0.78, Spellbrand 0.75, Portalfall 0.67, Fangs 0.60, Ripple 0.60, Vinelash 0.56, Manaburn 0.50, Tidehook 0.44, Jolt 0.44 | 0.71 |
| T3 single-target burst | Starshard 1.1, Blackflame 1.0 (DoT), Soulfire 0.94, Sanguine Rite 0.86 (armour-ignoring, costs 3 HP), Lightning 0.80 (12 + 4 burn), Explode 0.67, Resonant Shriek 0.67 (armour-ignoring), Smite 0.63, Meteor 0.58, Frostbite 0.50, Plasma 0.45, Entropy 0.47, Hemomancy 0.44, Cleave 0.36 (20 HP mob), Lifesteal 0.31, Devour 0.31 | 0.61 |
| T4 single-target burst | Dragon Breath 0.83, Wither 0.64, Hollow 0.56, Cometfall 0.50, Sonic Boom 0.46, Starmaw 0.44 (+3 per good effect), Doomclock 0.29 to 0.71, Tidewrit 0.33 | 0.50 |
| Sustained DoT (3 s or more) | Venom 1.5, Fire 1.4, Bleed 1.25, Blackflame 1.0, Soulfire 0.94, Everburn 0.8, Wither 0.64, Blood Moss 0.6, Frostbite 0.5, Entropy 0.47 | 0.87 |
| Armour-ignoring burst | Dismantle 0.90, Sanguine Rite 0.86, Resonant Shriek 0.67, Sonic Boom 0.46 | 0.77 |
| Hard control (seconds of no action per mana) | Root 0.33 s (3 s / 9), Decree 0.20 (2 s / 10), Freeze 0.16 (2.5 s / 16), Stasis 0.15 (5 s / 34), Jolt 0.11 (1 s / 9) | 0.16 |
| Soft control (seconds per mana) | Chill 1.5 (Slowness II 6 s / 4), Blind 1.0, Silence 0.67, Levitate 0.38, Echolocate 0.5 (Slowness II 3 s / 6), Hush 0.6 per target | 0.64 |
| Protection (damage-seconds turned away per mana) | Brace 0.40 (80% x 2 s / 4), Stoneskin 0.33 (40% x 10 s / 12), Anchor 0.6 (about 16% x 15 s / 4, melee only), Shulkershell 0.23 (80% x 4 s / 14) | 0.36 |
| Healing | Heal 0.67 HP/mana, Regrowth 0.64 (Regeneration II 8 s = 6.4 HP / 10), Remedy 0.64 (4 + Regeneration I 6 s = 6.4 / 10) and a cure | 0.65 |

The full arithmetic for each rune is in Appendix A. Two points matter for reading the tables:

1. **Tier is not a power tier for damage.** The medians run 0.67, 0.71, 0.61 and 0.50 from tier 1 to tier 4: nothing rises with tier. A tier 4 damage rune costs a third more per point of damage than a tier 1 one and must justify itself by a property (area, armour, delay). Where a tier 4 rune has none, it is flagged.
2. **Repeating shapes dominate everything.** `Zone` costs 2.0x for 6 applications (about 3x more damage per mana for an instant effect) and each pulse gets a fresh creature budget (`Cast.pulse`). Any effect that does not de-duplicate itself is multiplied again. See section 4.

## 2. Verdict table (all 49 runes)

Columns: id | tier | mana | role | key numbers (power 1.0) | power verdict (ratio to the role median in section 1) | verb | overlap | presentation today | presentation verdict | proposal. In the proposal cell **M** is the mechanic, **P** the presentation signature (silhouette, motion, accent, timing), **S** the sound (a key from section 5). Numbers for "what scales" are in Appendix B.

"Presentation today" was read from the Vfx code by three read-only helpers and spot-checked by me (the clock primitive, the eye-clearance rule, the sound calls); the caster of a self-cast rune sees almost none of its body-centred particles (`Fx.EYE_CLEARANCE` 1.1 blocks), so the cue for those runes has to live in sound, at the feet and on the screen.

### 2.1 Void, tier 1 (6)

| id | T | mana | role | key numbers | power | verb | overlap | presentation today | pres. verdict | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| anchor | 1 | 4 | defence | no knockback from blows or blasts, +4 armour, 15 s; passive-capable (0.5 mana/s) | fair+ : 0.60 protection-s/mana vs 0.36, 1.7x on paper but melee-only | immovable | Stoneform (earth), Brace, Stoneskin | ground circle, four chains rising to the waist, waist ring; CHAIN_PLACE + HEAVY_CORE_PLACE at t0; ring and star drawn in 0x3A1060 (near invisible); nothing when a push is refused; no end cue | distinct silhouette, loud for a T1, the actual effect is never shown | M: also immune to spell displacement (Pull, Banish, Swap, Warp, Gravity Well, Singularity, Riftcall, Portalfall); +4 more armour once you have stood still 1 s. P: a clank flash and dust puff at the feet each time a push is refused; chains slacken at the end. S: anchor_clank |
| blind | 1 | 5 | soft control | Blindness + Darkness 5 s, mob target cleared once, Shadowed 5 s | fair: 1.0 s/mana on paper (1.5x) but vs mobs it is one aggro reset (unverified whether mob AI honours Blindness); strong in PvP (black screen, no sprint) | put out their lights | Hush (area), Silence (arcane), Veil, Echolocate | dark disc closing on the eyes, black orb, violet ring, 6 SQUID_INK; SQUID_SQUIRT once; nothing at the end | distinct, right scale | M: blinded mobs lash out at the nearest other monster within 6 blocks for the duration (infighting) instead of just forgetting you; players' blackout 5 s to 3 s. P: black veil ring at eye height, ink drip; the caster hears the gulp. S: blind_gulp |
| collect | 1 | 3 | utility | pulls items and xp within 8 blocks (Widen up to 24) to you; no item cap | fair (only rune that fetches) | fetch | none | one violet streak per item (one packet each, uncapped); ITEM_PICKUP once; silent if nothing moved; streaks lag the teleport by 10 to 17 ticks | minimal; packet spike on big piles | M: cap 48 items per cast (stacks merged first), a short fizzle when nothing is there. P: a ring shrinking on the point, then streaks that arrive as the items do. S: collect_suck |
| hex | 1 | 5 | debuff / amplifier | your spells hit the target +25% for 8 s (multiplies with Eclipse 1.2, Cracked 1.2, Execute 2x); leaves Shadowed | OVER: 5 mana returns 0.05 x (damage in the window); beats Amplify (0.73 dmg/mana) once you deal 15 damage in 8 s, and is 2.7x better at 40 | mark for harm | Malison, Eclipse, Cinderbrand, Spellbrand, Trial Key | turning STAR sigil over the head (60 ticks of a 160-tick hex), ring, 6 WITCH, EVOKER_CAST_SPELL; every later hurt call plays a bite (glow + 2 WITCH); no end cue | distinct as a mark, sound shared with Malison, bite can spam on multi-hit | M: the hexed creature fixates on its hexer (mobs target you): the price of +25%; 6 s. P: star lasts the whole hex with one pip per second left, bite at most every 0.5 s. S: hex_mark, hex_bite |
| phantom | 1 (innate) | 12 | decoy | Mannequin with your skin (60 HP) draws every monster within 16 blocks for 4 s, then bursts for 8 x scale within 3 blocks; you are unseen 1.5 s | fair: 0.67/mana single, area + aggro control | decoy that detonates | none (Veil/Time Skip only drop aggro) | implosion, ground ring, souls; ring + dust every 10 ticks; burst about 38 packets; ILLUSIONER_MIRROR_MOVE (also Veil, Mirrorfrost); ENDER_EYE_DEATH + GLASS_BREAK at the burst | distinct (the only rune with a body), well scaled | M: bait and revenge: the burst gains +1 damage for every 6 the decoy soaked (max +8), so the harder they hit it the harder it pays them back. P: a ring around the afterimage shrinking over 4 s, pulses that quicken. S: phantom_tick (accelerating), phantom_burst |
| umbra | 1 | 6 | damage | 4, x2 when light at the target's eyes is 7 or less; Shadowed | fair in light (0.67 = median); OVER in the dark (1.33 = 2.0x median, 1.5x Harm), and dark is the norm underground and at night | bite of darkness | Harm, Ember, Icicle, Pelt, Windcut, Leech (the T1 poke crowd) | black orb, two rings, 4 to 8 ink; SCULK_CATALYST_BLOOM (lower if dim); ring tinted with the bright violet not the dark accent; silent Shadowed mark | small, generic black-and-ink | M: keep 4/8; make the light web real: Eclipse, Shades and night under open sky count as dim, so dark is something you build. P: dim: three dark claw crescents; lit: one pale flicker. S: shadow_bite |

### 2.2 Void, tier 2 (9)

| id | T | mana | role | key numbers | power | verb | overlap | presentation today | pres. verdict | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| banish | 2 | 8 | control | target reappears 8 x power (max 16) blocks further away on ground it fits, in sight; bosses resist | under 0.8x: about 1 block/mana against Push at 1.2 (4.8 blocks for 4 mana) | send away | Push, Launch, Dash, Recoil, Cyclone | ring folds the target in, 16 REVERSE_PORTAL, streak, flash + ring at the landing (about 19 packets); ENDERMAN_TELEPORT + CHORUS_FRUIT_TELEPORT | distinct A to B, sound is generic enderman | M: it arrives dazed (Slowness II 2 s) and forgets you. P: the target dissolves into dots that stream to the landing and re-form. S: banish_dissolve |
| echolocate | 2 (world) | 6 | reveal / soft control | everything hostile within 16 (cap 32) glows 10 s through walls; those hit: Slowness II 3 s, Shadowed 3 s | fair (utility; Reveal reaches only a Nova) | sonar reveal | Reveal, Treasure Sense, Prospect | three teal shock rings + a ring pinged round each creature (up to 77 packets); SCULK_CLICKING + AMETHYST_BLOCK_RESONATE; teal not void | distinct, good scale, palette deliberately sculk | M: the echo colours by distance and marks what is hidden (behind walls, invisible) in red; the count goes to the action bar. P: expanding shell with returning pings. S: sonar_ping |
| grapple | 2 | 8 | movement | sets your velocity toward the point: min(3.2, 0.8 + 0.12 d) x sqrt(power) per tick, 1 to 48 blocks, fall reset | fair | reel yourself in | Dash, Launch, Tusk Charge, Blink | thin violet line hand to anchor + black core + ring (about 14 packets); FISHING_BOBBER_THROW; silent when out of range | distinct, small enough | M: momentum is cancelled at the point (you arrive, not overshoot). P: a dotted rope that tightens and retracts. S: grapple_reel |
| hush | 2 (world) | 10 | area control | 4-block field 6 s (cap 32): every 0.5 s non-boss mobs forget their target, Weakness I, Darkness, Blindness, Shadowed | OVER: 0.6 s/mana per enemy, 1.8 for a pack of three (2.7x Silence); works on Self as a panic bubble | silence a room | Silence (arcane), Blind, Eclipse, Sandstorm | dark ring on the ground for its whole life, implosion, teal rim + souls + ink each second; SCULK_CLICKING once (shared with Echolocate) | distinct (dome rim), silent after the opening | M: a true anti-magic pocket: enemy players and Runebound inside cannot cast; drop its Blindness (Blind's job), keep Weakness and forgetting. P: a dome of dark glass rings; sound drops out. S: hush_dome |
| portalfall | 2 (world) | 9 | control / damage | portal under the target, drops it from up to 7 blocks (tries 7 to 3), 2 magic + fall (about 4 at 7 blocks); bosses take 2 | under: 0.67/mana in open air, about 0.2 under a low ceiling, nothing on fliers or into water | drop through a portal | Downdraft (wind), Launch, Meteor | implosion + dark circle below, ring + REVERSE_PORTAL above (about 8 packets); PORTAL_TRIGGER + ENDERMAN_TELEPORT; no cue for the fall or the landing | distinct (two portals), no payoff | M: the landing slams: enemies within 2 blocks take 3 and stagger; the fall is at least 5 blocks or it swaps to a sink instead. P: exit portal high, a streak down, a dust ring at the landing. S: portal_drop, landing thud |
| pull | 2 | 5 | control | velocity toward the spell min(2.6, 0.5 + 0.22 d) x power; PULLED 2.5 s (Implode, Collapse) | fair | hook | Vinelash (life), Tidehook (frost), Gravity Well, Grapple | two rings collapsing along the pull axis + 4 streaks + 8 PORTAL; ENDER_EYE_DEATH (also the Phantom burst) | distinct (axis-aligned rings) | M: the pulled creature lands staggered (Slowness III 1 s) and PULLED lasts 4 s so Implode and Collapse setups are reliable. P: a taut tether line, a thud and dust where it lands. S: void_hook |
| veil | 2 | 10 | stealth | Invisibility 12 s; monsters within 16 targeting you drop it once | fair | vanish | Time Skip, Shadowstep (0.75 s), Phantom, Blind, Hush | implosion, ring, 4 grey clouds, 8 REVERSE_PORTAL (about 14 packets); ILLUSIONER_MIRROR_MOVE; no end cue | generic void (same recipe as Wither, Blink, Phantom) | M: ambush: the first damage you deal while veiled is +50% and breaks the veil. P: you fade into rising grey ash; the ambush is a dark crescent flash. S: veil_fade |
| warp_step | 2 (world) | 8 | movement | teleport up to 24 blocks to the spell's point; 3 s later you are pulled back unless sneaking | fair | there and back | Blink (it copies Blink's look and sound), Time Skip | implosion + ray + ring, 16 WARPED_SPORE each end; teal ray (the comment says violet); custom BLINK at the start, ENDERMAN_TELEPORT at the return; silent when it stays | too close to a tier 3 rune | M: a visible return sigil on the ground at your origin counts down (3 ticks); you may cast freely meanwhile. P: teal tether + a shrinking arc; the return is a snap, not a repeat. S: warpstep_tick |
| zipper | 2 | 7 | movement | phase through up to 6 blocks of solid wall (not bedrock); no claim check | fair | unzip the wall | Blink, Excavate and Tunnel (earth) | gold seam of teeth on the wall at entry and exit (56 packets on one tick); CHAIN_BREAK + CHAIN_PLACE (Anchor's sound); failures are chat only | the most distinct visual of the tier, gold rather than void | M: the seam stays open 3 s (allies can follow, so can enemies); refuse walls in claims. P: the teeth animate open then zip shut. S: unzip |

### 2.3 Void, tier 3 (15)

| id | T | mana | role | key numbers | power | verb | overlap | presentation today | pres. verdict | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| blackflame | 3 | 18 | DoT | 3/s for 6 s = 18, armour-ignoring, unquenchable, Shadowed; if the target dies burning with 1 s or more left it spreads to the nearest enemy within 6 (chains) | fair: 1.0/mana = 1.15x the DoT median (top of tier 3, in line with Soulfire) | unquenchable spreading fire | Soulfire, Everburn, Inferno (fire) | Fire's five flame-tongue crescents recoloured black, SQUID_INK, pale grey smoke each second; SOUL_ESCAPE at ignition, FIRE_AMBIENT each pulse; no end cue | "dark fire" by palette only; the grey smoke and fire crackle undercut it | M: none needed; keep the spread as its identity. P: black fire with a white-hot core, embers that fall inward, a slow heartbeat tick per burn. S: black_fire |
| blackspark | 3 | 16 | damage / gamble | 8; one hit in four sparks: x2.5 and you gain Strength I + Speed I for 6 s (average 11) | fair: 0.69/mana vs 0.61 (1.1x) | gamble | Fortune (life), Kindling | 75%: one dark ring + 6 CRIT with PLAYER_ATTACK_STRONG (a vanilla crit); 25%: three crimson bolts + implosion + blood pulse (about 110 packets), TRIDENT_THUNDER + ATTACK_CRIT; nothing marks your buff; blood colours | split personality, the common branch is plain | M: a spark also arcs to a second enemy within 4 blocks; your zone shows as dark flame at the hands. P: base hit a small black flicker, the spark a red-black bolt and a bass hit with a screen punch. S: spark_crit |
| blink | 3 | 15 | movement | teleport to the spell's landing point, up to 40 blocks, safe spot search | fair (the reference teleport) | point to point | Warp Step, Shadowstep, Time Skip, Bloomstep, Thunderstep | implosion, dark streak, black core, ring, 12 REVERSE_PORTAL (about 24 packets); custom blink sound at both ends; failures silent or chat only | clean and proportionate; the mod's teleport signature | M: none. Fix: a fizzle cue when no safe spot or beyond 40. P: unchanged, it is the reference the others must differ from. S: blink (exists), blink_fizzle |
| devour | 3 (fused) | 16 | finisher | 5; if it kills: +10 mana and +4 absorption (twice per cast) | under on damage: 0.31/mana (0.5x); net of the refund 6 mana for a kill = 0.83 | feed on the kill | Leech and Lifesteal (blood), Thirst, Siphon | fanged maw snaps three times, bite flash, feed motes into you; up to 66 packets per target (about 530 for 8); WARDEN_HEARTBEAT, EVOKER_FANGS_ATTACK, GENERIC_EAT | distinct, correctly scaled, heavy spike on crowds | M: the bite grows with what the prey is missing (5 + 1 per 10% health missing, up to +5) so it finishes. P: full animation for the first 3 targets only. S: devour_chomp |
| eclipse | 3 (world) | 18 | area debuff | 4-block disc 5 s: Blindness, Shadowed, 2/s (10), your damage on them +20% | fair: 0.56/enemy plus the amplifier | darken a place | Sandstorm (earth), Hush, Hex | hairline line-art disc 4.5 blocks up, thin ring, ground ring + ink each second; BEACON_DEACTIVATE + cast_void; no close cue; in the screenshot it is a thin outline | unique silhouette, under-signalled | M: under an eclipse light counts as dim (Umbra doubles, Shades hit harder) and its players' screens darken (ScreenFx.tint). P: a filled dark disc with a corona and a shadow patch on the ground. S: eclipse_hum |
| entropy | 3 (fused) | 16 | DoT | 0.5, 1, 1.5, 2, 2.5 = 7.5 over 5 s, armour-ignoring; Extend lengthens the top step (10 s = 20) | under at base: 0.47/mana (0.54x); Extend x2 gives 1.43 (1.6x), so it is an Extend scaler | ramping decay | Blackflame, Wither, Venom, Bleed | gold dial with five pips over the head, frayed threads, per-second tick + rising SOUL_ESCAPE, finale black-core pop (about 25 packets per target per second) | the clearest anticipation and payoff in the set; busy | M: 1, 1.5, 2, 2.5, 3 (10, 0.63/mana); each wound also strips 1 armour point until it ends (team value, Rend-like). P: keep; one tick sound per cast, not per target. S: entropy_tick |
| gravity_well | 3 | 16 | area control | radius 7 drag 2 s (pull every 2 ticks), 4 at the end; PULLED | fair as a corral: 0.25/enemy, 0.125 s/mana per target x N | corral | Singularity, Riftcall, Hollow, Vortex, Sinkhole | 0.35-block black orb, converging violet comets, tilted rings (about 115 packets); WARDEN_SONIC_CHARGE once; the crush has no cue | good convergence read, tiny orb for a 7-block pull, weakest ending | M: it also drags fliers down and pins them (Weigh) so nothing leaves, and refreshes PULLED (3 s) at the end so Implode lands. P: a wide ground disc of inward-spiralling dust with lifting stones. S: void_corral, void_snap |
| malison | 3 (signature) | 16 | curse | 3 damage + hex (+25% 8 s) + Shadowed; if it dies cursed the curse passes to up to 3 enemies within 6 for the time left | under: 0.19 dmg/mana, worth about half of Hex + Harm (13 mana) except in packs | curse that travels | Hex (it contains it), Resonance (arcane) | dark seal + pink star + implosion + ring plus Hex's own star and sound; two EVOKER_CAST_SPELL (pitch 0.7 and 1.4) on one tick; heirs replay it; star ends at 60 of 160 ticks | rich but pink, doubled sound | M: each hand-me-down is +5% stronger (25, 30, 35, max 45%): it grows as it travels; 3 to 4 damage. P: the star walks along dark threads to the heirs. S: hex_mark (low), curse_pass |
| resonant_shriek | 3 (world) | 18 | damage / debuff | 8 + echo 4 after 1 s, sonic (armour-ignoring), Darkness 6 s, Shadowed | fair: 0.67/mana vs 0.77 (0.87x) | shriek and echo | Sonic Boom, Hollow, Echolocate, Hush | teal rings stamped along the path all on one tick, shrieker ring, implosion, sonic ring; SCULK_SHRIEKER_SHRIEK + WARDEN_SONIC_BOOM; quieter echo | distinct by colour and voice; boom shared | M: the shriek interrupts: cancels a charge or cast in progress (Charging.cancel, Runebound) and staggers 0.5 s. P: the rings travel out along the path. S: shriek, shriek_echo |
| riftcall | 3 (world) | 18 | area DoT | rift radius 5 for 3 s: drag + 2/s (6), then a snap for 6 within half the radius: 12 inner, 6 outer | fair: 0.67 inner, 0.33 outer per enemy | tear that gnaws then snaps | Gravity Well, Singularity, Hollow | flat cracked sigil at waist height + one tilted ring per 5 ticks (2 packets each); DOMAIN_CLOSE at the snap; silent between; the screenshot shows almost nothing | under-signalled | M: it gapes: radius +0.5 and snap +1 per enemy held (max +2.5, +5): packs are rewarded. P: an upright jagged slit with a lit rim. S: riftcall_open, void_snap |
| shades | 3 | 22 | summon | two black wolves 20 s, Strength II (about 10 per bite, 1 bite/s), Speed II, 40 HP; up to 6 spirits | OVER: potential 18 dmg/mana (2 x 10 x 20 / 22), 4.5 to 9 at 25 to 50% uptime = 7x to 15x the tier 3 median (0.61) and 1.8x to 3.6x Thunderbird (2.5) | hounds from your shadow | Summon (arcane T4, Strength I), Thunderbird, familiars | per hound: ground ring, circle, implosion, ink, clouds (about 17 packets), aura every 0.5 s for 20 s; WOLF_GROWL_BABY + SOUL_ESCAPE | distinct, modest, baby growl | M: no Strength by default (I in dim light, the Umbra rule), 15 s, 20 HP, bites leave Shadowed. P: hounds trail a dark smear and dissolve into ink. S: hound_growl |
| shadowstep | 3 | 12 | movement | reappear behind the first creature hit, facing its back, invisible 0.75 s; can target allies | fair | appear behind | Blink, Warp, Thunderstep | implosion + ink at both ends, black core, ring, black dust silhouette (about 46 packets); ENDERMAN_TELEPORT | distinct by ink and silhouette; sound generic | M: a backstab charge for 3 s: your next hit on it (spell or melee) is +50%. P: a stretched smear where you left. S: shadow_cut |
| shulkershell | 3 (world) | 14 | defence | target: Resistance IV (80%), no knockback, Slowness VII 4 s; on opening enemies within 3 float 2 s | under: 0.23 protection-s/mana vs 0.36 (0.6x) | close the shell | Brace, Cryostasis, Stoneskin | three tilted rings cage + ground circle; SHULKER_CLOSE/OPEN + SHOOT; a self-caster sees only the ground circle | sounds carry it, visuals thin | M: the damage the shell turns aside is stored and, on opening, leaves as up to 3 seeking bullets (stored / 3, max 6 each, plus Levitation). P: a shell of rings that thickens per hit absorbed. S: shell_close, shell_open |
| singularity | 3 (fused) | 22 | area control / burst | hole 2.5 s radius 5, line-of-sight pull (12 max), then 6 and a fling; 3 holes per caster | under on damage: 0.27/enemy; fair as control; vs Gravity Well +6 mana for +2 damage and a fling | black hole | Gravity Well, Riftcall, Hollow, Vortex, the secret spell of the same name | orb in a four-ring accretion disk + photon rings + spiral motes; burst about 55 packets; cast_void + BEACON_ACTIVATE + PORTAL_TRIGGER, GENERIC_EXPLODE + sonic boom + impact_void | the best set piece of the group | M: it swallows projectiles that enter it (arrows, bolts); the burst gains +1 damage each (max +5). Rename the secret spell to Black Star. P: unchanged. S: void_corral, void_snap |
| warp | 3 (fused) | 12 | movement / control | swap places with the first creature hit; an enemy gets Slowness II + Nausea 2 s (Nausea is not in the text) | under: Swap (6 mana) does the swap | swap through the void | Swap (arcane), Shadowstep, Blink | Swap's whole show (pink seals, NOTE_BLOCK_SNARE, ENDERMAN_TELEPORT) under Warp's own (implosion, core, three crescents, blink at both ends): about 33 packets, four sounds at t0 | over-layered, no void identity | M: the enemy is left PULLED (Implode and Collapse setups) and you keep 1 s of invisibility. P: drop the Swap visuals; two spirals that cross and exchange colours. S: warp_ping |

### 2.4 Void, tier 4 (6)

| id | T | mana | role | key numbers | power | verb | overlap | presentation today | pres. verdict | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| dragon_breath | 4 | 30 | area DoT | 3-block cloud, 5 pulses x 5 = 25 per enemy over 5 s; no de-duplication: Zone x6 gives 150 per enemy for 68 mana | fair alone (0.83/enemy vs T4 0.50); OVER under any repeating shape (2.2/enemy/mana) | lingering cloud | Inferno, Blizzard, Sandstorm, Eclipse | DRAGON_BREATH x22 per pulse, spherical (half underground, wider than the damage radius), two ground rings; ENDER_DRAGON_SHOOT once (nothing is launched) | unique material, thin for T4 | M: the breath drifts about 1.2 blocks/s along your facing (a lane), one breath per caster at a time. P: a rolling cloud front with a dark core. S: breath_roar |
| hollow | 4 | 36 | burst | 20 to the target and 8 to everything within 4 (dragged in); up to 3 centres per cast: a pack of 6 takes 180 (3.0/mana under Burst) | fair single (0.56, 1.1x T4); OVER in crowds (quadratic) and under repeat shapes | erasure | Singularity, Gravity Well, Sonic Boom | red and blue orbs spiral in over 6 ticks, black core, rings, sonic ring, drag (about 35 packets); BEACON_DEACTIVATE + WARDEN_SONIC_BOOM; damage lands 1 tick before the boom | the best-choreographed T4; the 0.3 s wind-up is easy to miss | M: one centre (the creature hit): 20 + 8 per other; the erased creature is gone from the fight for the wind-up (invisible, no AI) and returns with the damage. P: a cut-out sphere where colours invert. S: void_snap (heavy) |
| infinity | 4 | 32 | defence | projectiles freeze within 2.4 blocks and slow within 4; enemies near you are nudged 0.45 per 4 ticks; passive-capable (3.84 mana/s, permanent) | under as an active: 0.19 s/mana vs Deflect 0.89 (0.2x); OVER as a passive; melee, beams and blasts are unaffected although the text says nothing reaches you | nothing reaches you (in fact: freezes projectiles) | Deflect (wind), Anchor, Brace, Shulkershell | three hairline great circles at radius 1.7 (catch radius 4.0), a ring per caught projectile; BEACON_ACTIVATE + AMETHYST_BLOCK_HIT; the wearer never sees the shell; no end cue | hairline for a 32-mana ward | M: a Zeno bubble: enemies and projectiles within 5 blocks slow in proportion to closeness (Slowness II at 5 blocks to VI at 2), projectiles freeze as now; take it out of the passive list or triple its upkeep. P: glass-clear concentric ripples that tighten inward. S: zeno_heartbeat |
| sonic_boom | 4 | 35 | armour-ignoring burst | 16 to each target (no cap: N beams and booms) | under: 0.46/mana vs 0.77 for the armour-ignoring role (0.6x); a T3 Sanguine Rite does 12 through armour for 14 | hitscan crack | Resonant Shriek, Hollow, Beam and Lance | beam of darkness + hairline core, vanilla SONIC_BOOM rings every 2.5 blocks all on one tick, voidImpact; WARDEN_SONIC_BOOM 1.2 at the origin | right for a T4 hitscan, particles do not travel, crowds stack | M: it pierces: every creature on the line from you to the target takes 8 (the target 16), through walls; knockback along the line. P: rings that travel down the line. S: sonic_crack |
| starmaw | 4 (world) | 32 | damage / anti-buff | 14 + 3 per good effect (strips them) | under: 0.44/mana (0.9x); strong only on buffed foes | eat the light | Nullify (arcane), Timesteal, Prolong | black ball + implosion + a gold spoke per swallowed effect (about 12 packets, 0.6 s); WITHER_SPAWN 0.35 + impact_void | reads well, short for T4 | M: +4 per good effect, +1 per absorption heart, and it devours mod wards on the target (Shield, Foresight, Riposte, Reflect, Infinity, Anchor) at +4 each. P: spokes in each effect's own colour. S: starmaw_gulp |
| wither | 4 | 25 | DoT | Wither III 8 s = 16 damage (armour-ignoring), Shadowed; Elapse fuel (up to 16 at once); Amplify does nothing | under: 0.64/mana = 0.74x the DoT median and 0.64x of its T3 sibling Blackflame (18 for 18) | rot | Blackflame, Entropy, Venom | implosion, tiny core, smoke, 4 souls (about 12 packets, once); WITHER_SHOOT; nothing during the 8 s but the vanilla swirl | generic and small; looks like Veil or Blind | M: Wither IV for 6 s (24, 0.96/mana), contagious (a melee attacker gets Wither I 3 s), the withered cannot regain health. P: creeping black veins and rot drip under it. S: wither_rot |

### 2.5 Time (13)

| id | T | mana | role | key numbers | power | verb | overlap | presentation today | pres. verdict | proposal |
|---|---|---|---|---|---|---|---|---|---|---|
| borrowed_time | 1 (innate) | 14 | heal / loan | heals what you took in the last 5 s (40 hits); repaid over 10 s as magic damage unless you slay a monster | OVER in PvE: 1.07 HP/mana at 15 damage taken (1.6x Heal) and free after any kill; a net loss if you were already healed (bug, section 7) | loan | Heal, Stitchtime (life), Rewind | Rewind's cue without the trails: backward clock at the waist, gold flecks, a contracting ground ring; TRIDENT_RETURN + BELL_RESONATE + BELL_BLOCK; payments have no cue | generic (Rewind-lite), the repayment is unreadable | M: debt = HP actually restored; repayment can never kill (leaves 1 HP); 20% interest unless forgiven. P: a coin-tick of gold falls from you each second with a low tock. S: debt_tock |
| countdown | 1 | 7 | damage / fuse | 6 at 1.5 s; time damage: sets off Elapse on burning, poisoned or withering foes; Extend ignored | fair: 0.86/mana vs 0.67 (1.3x), dodgeable | fuse | Primer, Imprint, Belated, Delay, Stormclock, Doomclock | gold circle sigil over the head three times + white ring, three rising NOTE_BLOCK_HAT ticks, a sky beam and END_ROD burst at 1.5 s, BELL_BLOCK + chime; about 19 packets per target, up to 64 targets | the best anticipation, strike big for a T1, spam risk | M: if the mark dies first the strike finds the nearest enemy within 6. P: a vertical clock face with a sweeping second hand; strike effects for at most 8 targets. S: clock_tick, clock_strike |
| accelerate | 3 | 14 | buff | Speed III, Haste III, Jump II, Regeneration I 10 s; passive-capable (1.7 mana/s) | fair+: 14 vs 24 for the four separate runes (1.7x), the best passive | hurry | Swift, Haste, Leap, Regrowth, Chronoshift | horizontal clock at the waist, one tilted hand, ground ring, 6 END_ROD; BEACON_POWER_SELECT; the caster sees only the ground ring; no buff cue | generic (the same clock as eight runes) | M: Speed II, and your charge fills 40% faster and your Bolts and Arcs fly 1.5x (a tempo buff, not a potion). P: a gold ghost trail of your last positions and speed lines. S: tick_run |
| chronoshift | 3 (fused) | 18 | support | ally: every other spell's cooldown 3 s sooner (x0.5 to 2), Haste I + Speed I 5 s; once per window | under, conditional: cooldown is cost/20 s so it clears most cooldowns but mana is the limit; about one extra cast per use, 0.3x to 0.6x | turn the clock forward | Accelerate, Rapid, Twin Star | three meshing cogs on the ground + waist ring + three quarter-turn hand jumps; cast_time + VAULT_OPEN_SHUTTER + three rising ticks; the only time rune with its own silhouette and the only one a self-caster sees | distinct, proportionate | M: also refunds 30% of the mana the ally spent in the last 5 s (max 30): a real support role. P: keep the cogs. S: chrono_wind |
| foresight | 3 | 14 | defence / dodge | the next round(2 x power) blows with an attacker within 15 s are sidestepped, whatever their size, bosses too | OVER vs big hitters: a 30-damage blow x 2 for 14 mana is 4.3 HP/mana (6x Heal); 0.6x vs chip damage; recasting refills the charges | sidestep the next blows | Riposte (dominates it), Brace, Deflect, Cryostasis | small clock over the head + 4 END_ROD, ILLUSIONER_PREPARE_MIRROR; on a dodge a gold afterimage, stopped clock and a 1 to 1.6 block sidestep, ILLUSIONER_MIRROR_MOVE; nothing for 15 s; no expiry cue | the dodge is distinct, the cast generic | M: each dodge stops at most 12 damage (a bigger blow is only softened by 12) and spent charges are never refilled (Riposte's rule). P: a faint gold eye above the head with one pip per charge. S: dodge_whip |
| prolong | 3 | 12 | utility | every good effect on the target +15 s (up to 5 min); endless ones untouched | fair (utility; Extend is cheaper on a rune buff, Prolong works on potions and allies) | stretch buffs | Extend, Timesteal, Starmaw, Nullify | slow waist clock (20 ticks) + gold flecks; BELL_RESONATE pitch 1.5 or 0.9 if nothing extended; the caster sees 4 ticks of it | generic, nothing shows what was extended | M: none (numbers are fine). P: an hourglass ring: a thread of sand in each extended effect's colour. S: sand_pour |
| reckoning | 3 (fused) | 18 | damage / ledger | 4 s ledger: every wound counted after armour, from anything; half comes due at once, at most 12 | under to fair: at most 0.67/mana, usually 0.3 to 0.5 (needs 24 damage counted) | ledger | Doomclock, Stasis, Stitchtime, Countdown | bronze book-seal with a second hand, a mote + ring + tally per wound, the seal folds shut like a book, a stamp and blood drip + shake at settle; BOOK_PAGE_TURN, cartographer tick per wound, ANVIL_LAND + BELL_BLOCK + impact_blood | the most bespoke of the 13, lavish for 12 damage | M: a share of what comes due heals you (half, max 6): the tax is collected. P: keep. S: reuse; cap long-lived packets |
| riposte | 3 (signature) | 16 | defence / counter | next 2 blows within 10 s sidestepped and answered with 6 x power time damage at up to 24 blocks | over vs Foresight: +2 mana buys 12 damage at range; fair as the fused upgrade | dodge and answer | Foresight (parent), Reflect, Bramble | waist clock + pink ring + two orbiting comets, chime + impact_time; on a dodge a gold ring, 5 glows, pink slash, ray; ATTACK_NODAMAGE + ATTACK_CRIT | generic at cast, distinct on the dodge | M: the answer returns the blow's damage clamped 4 to 12 instead of a flat 6. P: a mirror flash and a blade of light from ally to striker. S: dodge_whip, parry_ring |
| time_skip | 3 | 14 | movement | vanish, reappear up to 8 blocks ahead on safe ground, invisible 1.5 s, monsters within 16 drop you | under: Blink does 40 blocks, aimed, for 15; Dash does 8 for 6 | hop and vanish | Blink, Dash, Veil, Shadowstep | three crimson dust silhouettes at the origin + stopped clock, crimson flash + ring at the arrival, gold ground ring (about 60 packets, the heaviest burst of the 13); crimson (0xB02030) reads as blood; MIRROR_MOVE + BELL_BLOCK; the caster does not see the arrival | red reads as blood or void, not time | M: you skip a second of time: 0.75 s of Resistance V on arrival (nothing hurts you), with a fixed 5 s lockout. P: three flickering gold after-images one tick apart. S: skip_stutter |
| timesteal | 3 (fused) | 16 | steal | up to 2 good effects (strongest, then longest), at most 30 s each, go to you; a boss keeps the effect, loses the time | under in PvE (few mobs carry buffs), strong in PvP | steal buffs | Nullify, Starmaw, Prolong | 12-mark dial with a backward star, hands point at the thief, each stolen effect flies across in its own colour; BELL_RESONATE always | distinct, the theft reads clearly | M: with nothing to steal it steals a moment: the target Slowness II 2 s, you Speed I + Haste I 2 s (never blank). P: a thread of sand joins target and thief. S: sand_pour, chime |
| doomclock | 4 (signature) | 28 | damage / bomb | up to 3 clocks, 3 s; each blow (4 a second at most) winds +2 (max 12); bursts for 8 + wind to its bearer and every enemy within 3 blocks | fair single: 0.29 to 0.71/mana; OVER in packs (an enemy near three clocks takes three bursts) | winding bomb | Reckoning, Stasis, Primer, Countdown | horizontal clock over the head + amber ring + ground reticle, tick every 10 ticks, wind ticks rise in pitch, TNT_PRIMED + impact_time, burst = timeImpact + fireImpact + GENERIC_EXPLODE + BELL_BLOCK | distinct, the best-scaled T4 cue in the group | M: an enemy takes at most one Doomclock burst per cast. P: the clock face stands upright facing the caster, its hand quickening. S: doom_tick, clock_strike |
| rewind | 4 | 26 | heal / movement | return to where you were 5 s ago (snapshots every 0.5 s, 7 s kept) with that health if higher; clears fire; players only; each cast reaches further back | fair: 0.65 HP/mana plus the reposition (Heal + Blink) | go back | Borrowed Time, Blink, Heal | gold trail motes and a ray back, dust afterimage at the old place, backward clock at the arrival; the view snaps to the old yaw and pitch (a strong cue); TRIDENT_RETURN + BELL_RESONATE; silent no-op with no snapshot | partly distinct, modest for a T4 | M: the destination is checked for lava, void, blocks and claims. P: the recorded path lights up in reverse and you slide back along it. S: rewind_whoosh |
| stasis | 4 | 34 | hard control | holds 5 s (mobs frozen in AI, players anchored and slowed, bosses only Slowness V); every blow is stored and lands at once; applies before the group's other effects | fair: 0.147 s/mana vs Freeze 0.156; huge with area shapes (no cap but 64) and Zone (holds for the zone + 5 s) | pause and bank | Freeze, Glacier, Cryostasis, Drowse, Doomclock | generic clock (radius 0.7 to 0.9), stopped clock + grey dust every 4 ticks (about 100 packets per target), 4 rays + ring per held blow (uncapped); BELL_BLOCK + BEACON_DEACTIVATE at the caster; NODAMAGE per held blow; the screenshot shows nothing legible | generic and under-scaled for T4, the biggest packet risk of the time set | M: none needed (the verb is excellent); cap the release knockback and the stored-hit packets. P: a column of frozen gold sand around the target, each held blow a shard hanging in it; release shatters outward. S: stasis_stop, time_resumes |

## 3. Clusters and how each is broken up

A cluster is a set of runes that answer the same question ("how do I drag things in?") with the same shape of outcome. For each, the axes below are the ones the proposals separate them on: **delivery** (instant, delayed, pursuing, lingering), **targeting** (one, line, arc, ring, self-centred), **interaction** (marks, terrain, conditions, modifiers) and a **signature twist**.

### C1. Drag it in (void, 5 runes; 5 more elsewhere)

Pull, Gravity Well, Riftcall, Singularity and Hollow are one mechanic (move creatures toward a point, then hit them) in five colours, and the mod adds Vortex (shape), Sinkhole (earth), Hellmouth (fire), Magnetize (storm), Tidecall and Tidehook (frost) and Vinelash (life). Nothing in the pulls tells you which one you cast.

| rune | delivery | targeting | interaction | twist | silhouette | sound |
|---|---|---|---|---|---|---|
| Pull (5) | instant tug | one, along the line | leaves PULLED 4 s, arrival stagger | the hook | a taut tether line | void_hook |
| Gravity Well (16) | 2 s field, no burst | wide (radius 7), ground disc | pins fliers to the ground, refreshes PULLED | the corral: it sets up the blast you follow with | flat disc of inward-spiralling dust, stones lifting | void_corral (a bed) |
| Riftcall (18) | 3 s tear, gnaws then snaps | radius 5, inner half takes the snap | gapes wider per enemy held | the maw that grows with its meal | an upright jagged slit with a lit rim | riftcall_open, void_snap |
| Singularity (22) | 2.5 s sphere, then a burst | radius 5, line of sight | swallows projectiles, each adds to the burst | the black hole (accretion disk, already excellent) | sphere + disk + photon rings | void_corral, void_snap |
| Hollow (36) | 0.3 s wind-up, then a cut | one centre (the target) + 8 to the rest | the target is erased for the wind-up and returns with the damage | the erasure: precision, not a crowd pull | a cut-out sphere where colours invert | void_snap (heavy) |

The effects then differ in **what you do next**: Pull, Gravity Well and Warp leave PULLED so Implode (any blast) and Collapse (Repel) want them; Riftcall and Singularity pay off on their own; Hollow is a finisher on the thing you aimed at.

### C2. Move yourself (void 6, time 2; 6 more elsewhere)

Blink, Warp Step, Warp, Shadowstep, Time Skip, Zipper, Grapple (and Rewind) are eight ways to change position. Blink, Warp Step, Shadowstep, Warp and Bloomstep/Thunderstep share the implosion-core-ring look and the one custom teleport sound.

| rune | delivery | targeting | interaction | twist | signature | sound |
|---|---|---|---|---|---|---|
| Blink (15) | instant, 40 blocks | the spell's landing point | safe-spot search | the reference: a clean ping | unchanged | blink |
| Warp Step (8) | there, then back after 3 s | the spell's point, 24 | sneak to stay | the yo-yo with a visible return sigil | teal tether, a shrinking return arc | warpstep_tick |
| Warp (12) | instant swap | first creature hit | leaves it PULLED and you unseen 1 s | swap through the void | two spirals that cross and trade colours | warp_ping |
| Shadowstep (12) | instant | behind the first creature hit | +50% backstab for 3 s | the opener | a stretched smear, black dust | shadow_cut |
| Time Skip (14) | instant hop, 8 blocks | forward | 0.75 s of Resistance V, 5 s lockout | skip a second: the dodge | gold flicker frames | skip_stutter |
| Zipper (7) | instant, 6 blocks of wall | through the wall in front | seam stays open 3 s, respects claims | the door you leave | teeth that open and zip shut | unzip |
| Grapple (8) | a reel over a second | the spell's point | momentum cancelled at arrival | the tether | a dotted rope | grapple_reel |
| Rewind (26) | instant, back in time | your own past | checks the destination | the undo | the path lights in reverse | rewind_whoosh |

### C3. Take away their sight and their target (void 6; arcane Silence)

Blind, Hush, Veil, Phantom, Echolocate, Eclipse all end with "monsters lose you" or "you cannot see" and share the darkness look; Umbra and Shades belong to the same fantasy without using it. The proposal turns darkness into one web instead of six copies:

* **Blind** makes the blinded fight each other (chaos). **Hush** becomes an anti-magic pocket (silence, literally). **Veil** becomes the ambush (the unseen first strike). **Phantom** stays the decoy that detonates. **Echolocate** stays the scan and gains a readable echo. **Eclipse** makes the place dark: light counts as dim inside it.
* **The dark web:** Umbra doubles in dim light (as now), Eclipse and night under open sky count as dim, Shades hit harder in dim light and leave Shadowed, and Shadowed (Blight, life) is what all of them leave. Darkness becomes something a void caster builds and then spends.

### C4. Sculk and sound (void 4)

Sonic Boom, Resonant Shriek, Echolocate and Hush share teal, SCULK_CLICKING and the Warden's boom.

| rune | verb | delivery | twist | sound |
|---|---|---|---|---|
| Sonic Boom | crack | instant hitscan through walls and armour | pierces everything on the line | sonic_crack |
| Resonant Shriek | shriek and echo | hit, then an echo 1 s later | interrupts a cast in progress | shriek, shriek_echo |
| Echolocate | sonar | expanding shell | returns distance and hidden things | sonar_ping |
| Hush | silence | a pocket for 6 s | enemy casters cannot cast | hush_dome |

### C5. Deferred damage and the ledger (time 5; life, storm, fire elsewhere)

Countdown, Reckoning, Doomclock, Stasis and Borrowed Time all do "not now, later, and more", with Primer, Imprint, Belated, Stormclock and Stitchtime doing the same in other elements. What separates them:

| rune | what is deferred | trigger | who profits | twist |
|---|---|---|---|---|
| Countdown | one hit | a fixed fuse | the caster | finds another target if its mark dies |
| Reckoning | half of every wound (max 12) | the ledger's end | the caster, who is healed by a share | the tax that is collected |
| Doomclock | a burst that winds with each blow | zero of a 3 s clock | the caster (once per enemy per cast) | the bomb the whole party winds |
| Stasis | every blow, in full | the hold's end | whoever hit it | the pause that banks damage |
| Borrowed Time | your own damage | you slay something | you | a loan with interest |

### C6. Sidestep (time 2; wind, earth, frost, life elsewhere)

Foresight and Riposte are the same ward (the next blows are dodged). Riposte dominates Foresight for two mana. Foresight becomes the plain, capped dodge (12 damage per blow, no refill); Riposte becomes the counter (the answer returns the blow's damage, 4 to 12, at range).

### C7. Buff manipulation (time 2, void 1; arcane Nullify)

Prolong (extend), Timesteal (steal), Starmaw (eat for damage) and Nullify (strip) are a healthy tetrad: four verbs on one object. They need feedback (what was moved is not shown) and power fixes, not new mechanics; Timesteal gets a fallback so it is never blank, and Starmaw the anti-ward niche.

### C8. Damage over time (void 3; fire, life, blood elsewhere)

| rune | curve | identity |
|---|---|---|
| Blackflame | 3/s x 6 s, spreads | the plague: a kill sets the next target burning |
| Entropy | ramps 1 to 3 over 5 s (Extend keeps it at the top) | the grind: strips armour as it goes |
| Wither | Wither IV 6 s | the rot: contagious, no healing |

### C9. Curses and amplifiers (void 3)

Hex (a mark that turns mobs on you), Malison (a curse that grows as it travels) and Eclipse (a darkness that lowers everyone's guard in a place) are three ways to say "your spells hit harder": single, travelling, area. The price of Hex (aggro) and the growth of Malison keep them apart.

### C10. Defence (void 3, time 3)

Anchor (immovable: also against spells), Infinity (Zeno: the closer, the slower), Shulkershell (the shell that turns blows into bullets), Foresight (the sidestep), Riposte (the answer) and Rewind (the undo) all defend, and each now protects against a different thing: displacement, approach, one heavy hit, blows, the health you lost.

### C11. Tempo (time 2)

Accelerate becomes a tempo buff (charge and projectile speed, not a potion pack); Chronoshift becomes the ally's second wind for mana and cooldowns. Rapid, Quicken and Twin Star stay the modifier answers.

### C12. Summons and the T1 poke crowd

* **Shades** is a summon and should be priced like one (section 2). It shares the wolf entity and the Strength-per-power rule with Summon; the two rules differ by one level.
* **The T1 damage pokes** (Harm, Ember, Icicle, Pelt, Windcut, Umbra, Leech, Countdown, Kindling) are one 3 to 4 damage poke on one target with a different colour. Umbra (light), Countdown (delay) and Kindling (stacks) are the only pokes that already have a twist; the other elements' auditors should give each poke its own delivery, and Umbra's light web is the model.

## 4. Cross-element and caster-feel observations (patterns for the lead to treat globally)

**4.1 The cast cue is the same for every spell of an element.** On press the server does the same six things for every spell (`SpellCaster.java:174-197`): a vanilla arm swing, one magic circle at the feet (same size and timing, only the emblems inside differ), 6 `ENCHANT` glyphs, a hand glow the caster cannot see, `CIRCLE_OPEN` at 0.35 and `cast_<element of the first effect>` at 0.55, then the heart rings. Nothing varies by rune, and nothing varies by role (an Anchor, a Blink and a Hollow are the same first 200 ms). The cast pose depends on the first rune's id, but is only visible to other players. There is no pitch or volume variation anywhere in the cast path, despite two variants per element. Proposal for a global layer: (a) an onset sting per role (damage snap, control inhale, movement blip, buff chime, world thunk, summon call; section 5) played at the hand at t0 under the element sound; (b) the circle's behaviour by role (damage circles snap outward, control circles contract, movement circles spin then blink out, buffs rise), a client parameter of `SpellCircleParticle`; (c) 5% pitch spread on the element sounds.

**4.2 The element cast sounds arrive late.** `cast_time`'s clock chime lands about 0.57 s after the press and `cast_void`'s thump about 0.94 s; `impact_void` has a 0.32 to 0.36 s reversed pre-roll before its boom, so the sound trails the visual by about 7 ticks. In the first 200 ms a void or time caster hears only a glass arpeggio (`CIRCLE_OPEN`, identical for every spell) and a reversed toll or two quiet ticks. Fire, storm and arcane get their attack inside 160 ms. Void and time need an early transient (a 20 ms tick or a low click) and their payoff sound on the effect, not the cast.

**4.3 Bolts play the cast sound twice** (`castCircle` and `RuneBolt.launch`), and Malison plays two cast sounds an octave apart on one tick. Both are the same bug in different clothes.

**4.4 Shared impact helpers make runes of one element look alike.** Every effect hit gets the same `Vfx.touched` (one glow and 3 motes, no sound), including movement and world runes that do nothing to the target (Collect, Phantom with a non-self shape, Blink's bystander). `implode` + `blackCore` is the void fingerprint at about 50 call sites (mod-wide, including fused flame, frost and storm), and `ElementFx.clock` is the time fingerprint: nine of the 13 time runes draw the same flat gold disc at different radii, and only Chronoshift, Countdown, Reckoning and Timesteal have a silhouette of their own. Advice: keep the fingerprints as the element's ground colour but give every rune one silhouette that is not the fingerprint (section 2, P), and make time faces stand upright facing the viewer (they are horizontal turntables at the waist now).

**4.5 The caster cannot see their own self-cast.** `Fx.send` drops any particle within 1.1 blocks of the viewer's eyes plus its spread, and `Sigils`/`Light` within 1.25 (deliberate, so spells never block the view). Every self-cast rune (Accelerate, Foresight, Prolong, Rewind, Veil, Anchor, Infinity, Shulkershell, Borrowed Time) therefore shows the caster only its ground ring; the only cue left is sound. Rules: for self-cast runes design the cue for the ears (an onset sting and a verb sound), the feet (ground rings, footprints, a floor sigil) and the screen (a brief `ScreenFx.tint` or field-of-view change), and keep the body-centred silhouette for observers.

**4.6 Marks are invisible and silent.** Shadowed, Pulled, Bleeding, Cracked and Resonant decide whether Blight, Implode, Collapse, Rupture and Unweave go off, and nothing on the creature shows them. A player cannot learn the reaction system from what they see. Add a mark tell: at most 3 dim particles every 10 ticks in the mark's own colour, and a tiny tick when a mark is set.

**4.7 No end cues, and per-target spam.** Blind, Hex, Veil, Anchor, Echolocate, Accelerate, Foresight, Infinity, Prolong and Hush all end silently (Hex's star ends at 60 of 160 ticks). A soft 1 s "fading" cue on the important timed effects would make buffs and debuffs readable. In the other direction Countdown sends about 19 packets per target (64 targets), Stasis about 100 per target over 5 s, Devour about 530 for 8 targets; a global "at most N fully animated targets per cast" helper (the fused runes already have `MAX_TARGETS` 8) would keep the show and drop the cost.

**4.8 Hit feedback is vanilla.** The caster gets the target's ordinary hurt sound and flash. The `DAMAGE_INDICATOR` particle appears only for Execute and Blood Price; there are no damage numbers or hit markers; `ScreenFx.punch` fires only for a hit of 8 or more and only through `Effects.hurt` (so not for Stasis's release). A per-element hit sting (`impact_<element>` at 0.25 to the caster only) and a punch for any crit-like event (Blackspark's spark, a reaction) would make "this hit was special" audible.

**4.9 Repeating shapes multiply every effect that does not guard itself.** Zone costs 2.0x for 6 applications (about 2.4x to 2.7x cheaper per application at tier 3 and 4 costs), Totem, Domain, Wall, Orbit, Vortex, Linger, Volley, Pulse and Echo do the same, and each pulse gets a fresh 64-creature budget. Blackflame, Entropy, Reckoning, Doomclock, Stasis, Singularity (3 holes) and Riposte guard against it. Dragon Breath, Gravity Well, Riftcall, Hollow, Eclipse and Foresight do not. Either every effect gets a per-target-per-rune-per-cast rule (the fused runes' `unstacked` helper is the model) or shapes are priced by pulses; otherwise per-mana comparisons of instant effects are meaningless.

**4.10 The tier 1 poke crowd.** Harm, Ember, Icicle, Pelt, Windcut, Leech, Umbra and Countdown are one 3 to 4 damage poke on one target in eight colours. Only Umbra (light), Countdown (delay) and Kindling (stacks) have a twist. Every element's tier 1 damage rune needs its own delivery before the palette is the only difference.

**4.11 Tier is a price, not a power step, for damage.** Median damage per mana runs 0.67, 0.71, 0.61 and 0.50 from tier 1 to 4, and a T4 with no property (Sonic Boom, Wither, Starmaw) loses to two casts of Harm. The lead should decide whether T4 is supposed to be better per mana (then re-price) or better per press (then every T4 needs a visible reason).

**4.12 Palette and sound bookkeeping.** `Vfx.Theme` void secondary is 0x3A1060 (near black, dim as additive light) but `ElementFx.VOID.secondary` is 0xE0B0FF; shape deliveries that use the theme (`Vfx.impact`, `Vfx.beam`) draw dim rings while shockwaves draw pale ones. The custom element sounds have no `attenuation_distance` (default 16 blocks) while visuals reach 32 or 128. `impact_life` is finished with the "cast" role. There are 77 custom sound events, not 30, but only the 20 element ones are used at the rune level: every other rune sound is vanilla, most of them reused across elements (ENDERMAN_TELEPORT for six teleports, EVOKER_CAST_SPELL for Hex, Malison, Summon and the Archivist, BELL_BLOCK at the same pitch for Countdown and Borrowed Time).

**4.13 Where two runes feel the same to cast.** In the first 200 ms, casting Anchor, Blink and Hollow is one experience (the same circle, the same arpeggio, a reversed toll that peaks 0.9 s later); so is casting Countdown, Foresight and Stasis (the same circle, two quiet ticks). The differences arrive at the effect, and for a self-cast rune the caster sees none of them. What the caster gets today and what is proposed:

| group | first 200 ms today | proposed |
|---|---|---|
| self buffs and wards (Anchor, Veil, Infinity, Shulkershell, Accelerate, Foresight, Prolong) | circle + arpeggio + a late element sound; nothing of the body-centred effect is visible to the caster | onset_chime, then the rune's own verb sound; a floor sigil and a brief screen tint; a pip display for counted wards |
| damage (Umbra, Blackspark, Devour, Sonic Boom, Hollow, Wither) | circle + arpeggio + a late element sound; the hit is the vanilla hurt sound | onset_snap; the verb sound at the target; a punch for a crit-like hit |
| pulls and control (Pull, Gravity Well, Riftcall, Hush, Blind, Banish) | circle + arpeggio + `cast_void` inhale (peaking at 0.94 s) | onset_inhale, then the verb sound (hook, corral, tear, dome) |
| teleports (Blink, Warp Step, Warp, Shadowstep, Time Skip, Zipper, Grapple) | the same circle, then the same custom blink at arrival; the caster sees the arrival ring only if it is beyond 1.1 blocks | onset_blip and a brief field-of-view pulse at departure; a different arrival silhouette and sound per rune |
| time (Countdown, Stasis, Rewind, Reckoning, Doomclock) | circle + two quiet ticks, the chime 0.57 s later | a clock_tick at t0 (the fuse audibly starts), then the rune's own sound |

---

## 5. Sound needs

Every sound below is a short synthesis brief in `tools/sound_art.py` vocabulary (`bell`, `glass`, `clock_bell`, `tick`, `thump`, `moving_band`, `grains`, `sparkle`, `reverse`, `reverb`, `sweep`, `fm`, `soft_saw`, `chopper`), in the shared D-major pentatonic where tonal, 2 or 3 variants each, played with about 5% pitch spread. The first transient must fall inside 100 ms for anything the caster triggers.

### 5.1 Onset stings (global, one per role, played at the hand at t0, under the element cast sound)

| key | character | used by |
|---|---|---|
| onset_snap | a 25 ms dry crack (noise burst + short thump) | every damage rune |
| onset_inhale | a 120 ms rising breath (`moving_band` 400 to 1800) | control and pulls |
| onset_blip | a 60 ms rising chirp | movement runes |
| onset_chime | one soft glass note | buffs and wards |
| onset_thunk | a low 50 ms knock | world runes |
| onset_call | a 150 ms two-note call | summons |

### 5.2 Void sounds

| key | brief | used by |
|---|---|---|
| anchor_clank | tuned inharmonic metal partials (ratios 1, 2.76, 5.4, tau 0.25) + a low thump, 0.45 s; a 0.15 s version each time a push is refused | anchor |
| blind_gulp | a 50 ms noise burst that a lowpass closes from 6 kHz to 500 Hz over 0.15 s, then a 5 kHz tinnitus sine fading over 0.7 s | blind |
| collect_suck | a rising band sweep (300 to 2500 Hz) with a soft pop, then a glass sparkle whose note count grows with the items, 0.5 s | collect |
| hex_mark, hex_bite | mark: two detuned glass notes a minor second apart, slow tremolo, a downward glide, 0.6 s (a lower version for Malison); bite: one 2.6 kHz tick and a low blip, 0.1 s | hex, malison |
| phantom_form, phantom_tick, phantom_burst | form: reversed glass shimmer into a pop; tick: dry ticks whose gap shrinks from 0.5 s to 0.1 s over 4 s with rising pitch; burst: glass break + a low whump | phantom |
| shadow_bite | 0.35 s of bandpassed noise (300 to 1500 Hz) "kchh" with a breathy exhale and a sub thump; a tone lower in dim light | umbra, blackspark base hit |
| banish_dissolve | fine grains sliding from 4 kHz to 1 kHz then a 0.1 s chirp (300 to 1200 Hz) at the landing, 0.7 s | banish |
| sonar_ping | a 1.2 kHz sine ping (0.12 s) then three echoes, each 6 dB softer and 200 Hz lower, spaced by distance | echolocate |
| grapple_reel | a plucked twang (220 Hz, tau 0.15), then a ratchet of 6 ticks that speeds up and a soft thud, 0.6 s | grapple |
| hush_dome | a swell (band noise 200 to 900 Hz, 0.5 s) that drops out to near silence for 0.6 s over a faint low hum; one soft chime when it lifts | hush |
| portal_drop | a reversed whoosh in (band 3000 to 400, 0.25 s), a breathy pop, a whoosh out from above 0.25 s later, a landing thud (thump 90 to 45) | portalfall |
| void_hook | a thin inhale sweep (600 to 3000 Hz, 0.3 s) ending in a dry "thock", 0.5 s | pull |
| veil_fade | a reversed breath and a soft cloth flutter (lowpassed grains) fading to nothing, 1.0 s | veil |
| warpstep_tick | three quiet ticks at 1, 2 and 2.8 s (2.2, 2.6, 3.0 kHz), then the existing blink played reversed | warp_step |
| unzip | 20 to 30 ratchet ticks (3.2 kHz, gaps 45 to 8 ms) over a ripping grain band (500 to 5000 Hz), 0.6 s | zipper |
| black_fire | a lowpassed crackle plus a soft heartbeat thump per burn tick, 0.4 s; the ignite is a hollow roar swell (150 to 600 Hz, 0.6 s) | blackflame |
| spark_crit, spark_plain | crit: a bass hit (thump 80 to 40, driven) + a red crackle (grains 1 to 6 kHz), 0.6 s; plain: a small dry tick | blackspark |
| blink_fizzle | the blink ping cut short with a soft "phft", 0.3 s | blink |
| devour_chomp | two wet crunches (lowpassed grains, 50 ms each), a gulp (sine 180 to 90), and on a feed a rising three-note chime, 0.8 s | devour |
| eclipse_hum, eclipse_lift | a slow beating drone (two detuned saws at D2, lowpass 500 Hz) fading in, 5 s; a bright shimmer as the light returns, 0.6 s | eclipse |
| entropy_tick | a dry tick that rises a semitone each second, ending in a snap; one per cast, not per target | entropy |
| void_corral | a 2 s bed: inward band noise (1400 to 300 Hz) over a D1 drone with slow tremolo | gravity_well, singularity (hold) |
| void_snap | a reversed toll into a hard 8 ms crack and a low whump (thump 110 to 40), tail 0.4 s; a heavy 1.0 s version with a sub for Hollow | riftcall (close), singularity (burst), hollow, gravity_well (crush, light) |
| curse_pass | a descending glass arpeggio panned across three notes, 0.7 s | malison |
| shriek, shriek_echo | an FM scream (carrier 1.4 kHz, ratio 1.5, index 6, 40 Hz vibrato) with a bone crack, 0.9 s; the echo lowpassed, 9 dB down, with reverb | resonant_shriek |
| riftcall_open | a tear-rip of grains (400 to 4500 Hz) rising over a D1 drone, 0.7 s (a short cut of `rift_open`) | riftcall |
| hound_growl | a low saw (90 to 70 Hz) through a moving formant lowpass (300 to 800 Hz) with a chopper, 0.7 s; the bite is a tick and a wet chomp | shades |
| shadow_cut | a reversed whisper (highpass band 3 to 8 kHz) then a sharp 25 ms "shk", 0.3 s | shadowstep |
| shell_close, shell_open | a heavy stone-wood slam (thump 100 to 50 + knock), 0.5 s; a dull gong with three light bullet pings (2.0, 2.4, 2.8 kHz) sent away, 0.9 s | shulkershell |
| warp_ping | two glass pings crossing in pitch (A rising, E falling) panned opposite, with a reversed sparkle, 0.45 s | warp |
| breath_roar | a quiet roar band (180 to 900 Hz) with breathing modulation, hiss and crackle, 5 s at low level | dragon_breath |
| zeno_heartbeat | a 1 s loop of a two-note 55 Hz thump over a glassy hum; the tempo slows as enemies close; a 4 kHz "freeze" click when a projectile halts | infinity |
| sonic_crack | a 6 ms broadband crack, a descending sine (3.5 kHz to 400 Hz over 0.35 s), a short 1.8 kHz ring and a long tail, 0.6 s | sonic_boom |
| starmaw_gulp | an inhale sweep, a reversed glass-shard swallow and a low thump; a chime per swallowed effect, 0.8 s | starmaw |
| wither_rot | slow creaks (chopper on a low saw), wet lowpassed crackle and a fading moan, 1.0 s; a small pop for contagion | wither |

### 5.3 Time sounds

| key | brief | used by |
|---|---|---|
| clock_tick | one dry wooden tick, three pitches (2.2, 2.6, 3.0 kHz), 50 ms | countdown, doomclock, chronoshift, entropy |
| clock_strike | a low clock bell (D3, tau 0.9) + a short thud + a winding-down tick tail, 1.2 s (lower than `impact_time`) | countdown, doomclock |
| tick_run | 12 ticks whose gaps shrink from 100 to 15 ms with rising pitch, ending in a whirr and a soft chime, 0.8 s | accelerate |
| chrono_wind | a clockwork winding ratchet with a click-back, rising, then a released spring and a soft chime, 0.9 s | chronoshift |
| dodge_whip | a cloth whip (band 800 to 6000 Hz in 0.12 s) + a reversed glass chime + a faint ring, 0.35 s (Riposte adds `shield_parry`) | foresight, riposte |
| sand_pour | a soft hiss of falling grains (band 3 to 9 kHz, slow flutter) ending in a glass ping, 0.9 s | prolong, timesteal |
| ledger_scratch | a quill scratch (short grains 2 to 6 kHz) per wound counted; the settle is `clock_strike` and a stamp thud | reckoning |
| rewind_whoosh | a reversed tick train and a downward glide (2400 to 300 Hz) with a reversed reverb tail, 0.8 s; an arrival chime | rewind, borrowed_time (short) |
| debt_tock | a low wooden tock (thump 200 to 120 + tick), 0.2 s, one per payment | borrowed_time |
| skip_stutter | a 40 ms slice of a tick and a bell repeated three times a semitone-third higher each time, then silence and a soft thud, 0.35 s | time_skip |
| stasis_stop, time_resumes | a bell, then all sound gates out (a lowpass slam and a drop to near silence for 0.5 s) over a faint sustained tone, 1.0 s; the resume is the reverse plus a shockwave crack, 0.7 s | stasis |
| doom_tick | a heavier tick with a sub thump whose gap shrinks toward zero, 0.2 s | doomclock |

The twelve that would change the game's character most, in order: `void_hook`, `void_corral` + `void_snap`, `unzip`, `shadow_cut` + `shadow_bite`, `sonic_crack` + `shriek`, `hush_dome`, `clock_tick` + `clock_strike`, `rewind_whoosh`, `stasis_stop`, `dodge_whip`, `sand_pour`, `black_fire`.

---

## 6. Proposed implementation order, effort and risks

Effort is for one developer who knows the code, code and tests only; the sound and particle work is separate (wave 3).

### Wave 0: fix regardless (about 1.5 days in all)

| # | change | effort | notes |
|---|---|---|---|
| 1 | Borrowed Time: debt = health actually restored; a repayment can never kill (leaves 1 HP) | 0.5 h | `Innates.borrowTime`, `Innates.tick`; covered by `WildercordParryTest` |
| 2 | Foresight: never refill spent charges (copy Riposte's rule), cap each dodge at 12 damage | 1 h | `Wards.foresight`, `Wards.allowDamage` |
| 3 | Hollow: one centre (the creature hit), 8 to each other creature | 1 h | `Techniques.hollow`; `WildercordOitTest` |
| 4 | Doomclock: an enemy takes one burst per cast | 1 h | `SignatureWards.burst`; `WildercordSignatureFusionTest` |
| 5 | A per-caster, per-rune, per-target repeat guard for Dragon Breath, Gravity Well, Riftcall, Eclipse | 2 h | one shared helper, modelled on `FusedEffects.unstacked` |
| 6 | Shades: Strength 0 (I in dim light), 15 s, 20 HP; make Summon and Shades use the same offset | 0.5 h | `Spirits.spawnSpirits` |
| 7 | Infinity: correct the text, and out of `Passives.BUFFS` until the rework | 0.5 h | `Passives`, lang, wiki |
| 8 | Collect: 48-item cap, merge stacks, fizzle cue | 0.5 h | `Effects.collect` |
| 9 | Rewind: hazard and claim check on the destination | 1 h | `Wards.rewind` |
| 10 | Rename the secret spell Singularity | 0.5 h | `Secrets`, `SecretSpells`, lang, wiki |
| 11 | Bookkeeping: Umbra ring tint, `Theme` void secondary, Bolt double cast sound, Malison double sound, sound attenuation | 2 h | `CraftedVfx`, `Vfx`, `RuneBolt`, `sounds.json` |
| 12 | Descriptions that do not match the code (Warp's Nausea, Hollow, Doomclock, Infinity) | 0.5 h | `Runes.java` and `en_us.json` |

### Wave 1: small mechanics (half a day each)

Hex fixate; Blind infighting; Veil ambush; Shadowstep backstab; Time Skip's skipped second; Countdown transfer; Umbra light web (Eclipse, night, Shades); Devour missing-health; Wither IV + contagion + no healing; Entropy numbers + armour strip; Malison generations; Banish daze; Pull stagger and 4 s mark; Warp mark; Timesteal fallback; Reckoning tax; Anchor displacement immunity; Riftcall gape; Dragon Breath drift; Sonic Boom line; Starmaw wards. Most are 20 to 60 lines in the existing effect functions and reuse existing systems (`Reactions.mark`, `Spirits.hold`, `ShapeRunners.nearestEnemy`, `Wards` maps, `TemporaryBlocks`).

### Wave 2: about a day each

Hush anti-cast (a hook in `SpellCaster.cast` and `Runebound`), Shulkershell bullets (AFTER_DAMAGE store, `Vfx.stream` bullets), Infinity Zeno (per-tick Slowness by distance in `Wards.tickInfinity`), Singularity swallow (`Projectile` scan in `FusedVoid.draw`), Chronoshift mana refund (a per-player 5 s spend log in `Mana`), Accelerate tempo (`Charging` full-charge time, `RuneBolt` speed), Portalfall landing slam (a landing watch, `Reactions`-safe), Zipper seam (`TemporaryBlocks`), Gravity Well pin (Weigh reuse), Rewind path (the snapshots already exist).

### Wave 3: presentation (parallel, mostly asset work)

1. The 12 sounds at the end of section 5, then the rest, then the onset stings.
2. A clock family: an upright face facing the viewer, a gear, an hourglass, a bell, so nine runes stop sharing the flat disc (`ElementFx.clock` stays as the base).
3. Void silhouettes: the upright slit (Riftcall), the ground disc (Gravity Well), the sphere and disk (Singularity, unchanged), the cut-out (Hollow), the hook line (Pull).
4. Teleport looks for Warp Step, Warp, Shadowstep, Time Skip, Zipper, Grapple (section 3, C2), each with a different silhouette from the implosion-core-ring.
5. Mark tells, end cues, eye-safe self-cast cues (ground, sound, screen tint), per-role circle behaviour, per-cast animated-target cap.

### Risks

* **Balance.** Hex's fixate changes party play (it becomes a taunt); Hush's anti-magic matters in PvP and vs Runebound; Blind's infighting could be farmed (a mob-kills-mob loop costs the player drops? unverified); Accelerate's charge speed stacks with Focus of Haste; Infinity Zeno as a passive would be a permanent bubble; the Shades nerf is large by design and needs a playtest, not a formula; Chronoshift's refund between two casters must not loop (it cannot refund more than was spent).
* **Tests to update.** Gametests: `WildercordFusedVoidTest` (Reckoning, Devour, Timesteal, Chronoshift, Entropy, Singularity), `WildercordSignatureFusionTest` (Doomclock, Riposte, Malison), `WildercordNewRunesTest` and `WildercordNewRunes2Test` (Eclipse, Portalfall, Shriek, Shulkershell, Starmaw, Prolong, Umbra), `WildercordWorldMagicTest` (Blink, Banish), `WildercordWorldMagic2Test` (Countdown, Blind), `WildercordReactionsTest` (Countdown, Blind), `WildercordAffinitiesTest` (Sonic Boom), `WildercordParryTest` (Borrowed Time), `WildercordOitTest` (Hollow), `WildercordFeatureTour`, `WildercordScreenshots` (most runes). Unit tests: `FusionTest`, `ExpansionRunesTest`, `CraftedRunesTest`, `ExplorerNumbersTest`, `ReactionRulesTest`, `HeartAndPassivesTest` (passives list), and any that count runes. Runes with no gametest today and worth one: Collect, Phantom, Grapple, Veil, Dragon Breath.
* **Docs and wiki.** Rune descriptions live in `Runes.java` and `rune.wildercord.<id>.desc` in `en_us.json`; `tools/wiki.py` generates `wiki/runes/effects/void.md`, `time.md`, `fused.md`, `innate.md` and the world pages from them (regenerate, do not hand edit). Also `wiki/spellcraft/passives.md`, `reactions.md` (Elapse lists Countdown and Reckoning), `secret-spells.md` (Singularity, and Zero Hour uses Stasis + Accelerate), `docs/DESIGN.md` (rune tables around lines 180 to 190, 239 to 245, 321 to 324, 429 to 430, 628 to 633), `docs/RECIPES.md` if a recipe moves, and `CHANGELOG.md`.
* **Passives.** `infinity`, `accelerate` and `anchor` are in `Passives.BUFFS`; changing them changes what a passive can carry (and `Passives.EFFECT_TICKS` caps their lifetimes at 300 ticks).
* **Particle cost.** Several proposals add per-tick emitters (Infinity, Eclipse's filled disc, Dragon Breath's drifting cloud, the rope of Grapple): give each a packet budget before it ships (at most about 40 particles per tick and about 400 per second per effect).

---

## 7. Broken, or not doing what it says (fix regardless)

1. **Borrowed Time can hurt you for nothing.** `Innates.borrowTime` heals with `player.heal(owed)` but records the whole `owed` as debt. If your health was already back (a potion, regeneration, a second heal) the heal does nothing and the debt is still repaid as magic damage over 10 s. The debt must be capped at what was actually restored. Separately, killing any monster forgives the whole debt, so in PvE the loan is free; that is a balance call, not a bug.
2. **Foresight refills.** `Wards.foresight` replaces the ward, so `Zone`, `Pulse`, `Linger`, `Echo` and `Volley` refill the charges again and again (an ally in a Foresight Zone can dodge about once a second for 6 s for 36 mana). Riposte has an explicit rule against exactly this, and dodges are excluded from passives for the same reason.
3. **Hollow does more than it says.** The text says 20 damage and 8 more within 4 blocks; the code makes up to three creatures each a centre (20 to itself, 8 to everyone within 4 blocks of each centre), so a pack of six takes 180. Under a repeating shape it multiplies again.
4. **Doomclock over-bursts.** Every clock's burst hits every enemy within 3 blocks; an enemy beside three clocks takes three bursts (24 to 60), though the text reads as one burst per bearer.
5. **Lingering effects stack under repeating shapes** (Dragon Breath 150 per enemy for 68 mana in a Zone, Gravity Well, Riftcall, Eclipse); Blackflame, Entropy, Reckoning, Doomclock and Stasis guard against it, these do not.
6. **Infinity does not do what it says.** "Nothing reaches you": only `Projectile` entities are held and melee attackers are nudged; beams, blasts, spell shapes and melee are untouched. The drawn shell is radius 1.7 while projectiles freeze 2.4 to 4.0 blocks out, and the wearer cannot see it (eye clearance). It is also in the passive list, where it is a permanent 3.84 mana/s anti-projectile bubble.
7. **Shades out-punch Summon.** `Spirits.spawnSpirits` gives Strength `round(power)` to hounds and `round(power) - 1` to Summon's wolves, so a 22-mana tier 3 hound bites harder (Strength II) than a 30-mana tier 4 wolf (Strength I).
8. **Warp's text omits the Nausea it applies**, and it plays Swap's whole visual and sound show under its own.
9. **Collect is unbounded.** No item cap, one streak packet per item on one tick, silent when there is nothing to take.
10. **Rewind teleports to a stale point unchecked**: no lava, void, block or claim check; and it is a silent no-op with no snapshot or a non-player target.
11. **Two different "Singularity"s**: the fused void rune and the secret spell (Orb, Gravity Well, Pull, Explode).
12. **Zipper (and Blink, Warp Step, Time Skip, Rewind) ignore claims and spawn protection.** Zipper phases through a claimed wall; whether that is intended (ender pearls do the same) is a design call. **(unverified whether claims are meant to stop it)**
13. **Stasis release loses damage types.** Every stored blow is dealt at the end as one `hurtServer` of the *last* blow's source; if that was fire and the target is fire immune, or magic and it resists, all the stored damage of other types is lost (edge case, unverified in play). Its release also skips `ScreenFx.punch`.
14. **Visual and sound slips:** Malison plays two cast sounds an octave apart on one tick and its star ends at 60 of 160 ticks; Umbra's dark ring uses the bright violet as its darkness tint; `Vfx.Theme` void secondary (0x3A1060) is near black while `ElementFx.VOID.secondary` is 0xE0B0FF; Bolts play the cast sound twice; Dragon Breath's cloud is a sphere half underground and wider than its damage radius, with a launch sound for nothing launched; Sonic Boom's sound plays at the origin (silent beyond 16 blocks) and its rings do not travel; Warp Step's comment says violet and it is teal; Riposte's clock is gold although its javadoc says arcane light; Hollow's damage lands one tick before its boom; Shades queues 39 scheduler tasks per hound at cast and its aura stops after a reload; `cast_void` plays twice on Eclipse, Riftcall, Singularity and Entropy (caster cue plus effect).
15. **Blackspark's callout is filed as a reaction.** `Reactions.callout(cast, "blackspark", ...)` also runs `Contracts.onReaction` with a string that is not a reaction (it is not in `Feats.REACTIONS`, so it does not count toward Heart Circle reactions); harmless if contracts ignore unknown names. **(unverified)**

---

## Appendix A. The arithmetic

Damage per mana = the outcome at power 1.0 divided by the rune's own cost.

| rune | arithmetic | value |
|---|---|---|
| anchor | +4 armour is about 16 points of base damage (4% each until the 20-point cap); 16% x 15 s / 4 | 0.60 protection-s/mana |
| blind | 5 s / 5 mana | 1.0 s/mana (soft) |
| hex | Hex returns 0.25 x D for 5 mana; Amplify on Harm returns 0.5 x 7 / (0.6 x 8) = 0.73; break-even D = 0.73 x 5 / 0.25 = 15; at D = 40 Hex gives 2.0/mana | over |
| umbra | 4 / 6 = 0.67; dim 8 / 6 = 1.33 (1.5x Harm at 7/8 = 0.875) | 0.67 / 1.33 |
| banish | 8 blocks / 8 mana; Push impulse 2.2 gives about 2.2 / (1 - 0.546) = 4.8 blocks on the ground for 4 mana | 1.0 vs 1.2 blocks/mana |
| hush | 6 s of lost target and Weakness per enemy / 10 mana = 0.6; x 3 enemies = 1.8; Silence 6 s / 9 = 0.67 | 2.7x Silence |
| portalfall | 2 + fall damage (7 blocks up: 7 - 3 = 4) = 6 / 9; with 3 blocks free: 2 + 0 | 0.67 / 0.22 |
| blackflame | pulses at +20, +40, ..., +120 = 6 x 3 = 18 / 18 (the 7th pulse at +140 is past `until`) | 1.0 |
| blackspark | 8 x (0.75 + 0.25 x 2.5) = 11 / 16; base 8 / 16 | 0.69 / 0.50 |
| devour | 5 / 16; on a kill 10 mana back: 5 / 6 | 0.31 / 0.83 |
| eclipse | 5 pulses x 2 = 10 / 18 per enemy; +20% to every hit on it, stacks x1.25 with Hex | 0.56 + amp |
| entropy | 0.5 + 1 + 1.5 + 2 + 2.5 = 7.5 / 16; Extend x1 (10 waves) = 20 / (16 x 1.4); Extend x2 (20 waves) = 45 / (16 x 1.96) | 0.47 / 0.89 / 1.43 (median 0.87) |
| gravity_well | crush at tick 40 only: 4 / 16; 2 s / 16 mana per target | 0.25 / 0.125 s |
| malison | 3 / 16; Hex (5) + Harm-equivalent of 3 damage (3.4) = 8.4 / 16 | 0.19 / 0.52 |
| resonant_shriek | (8 + 4) / 18 | 0.67 |
| riftcall | pulses at 0, 20, 40 = 3 x 2 = 6, snap 6 within half the radius: outer 6 / 18, inner 12 / 18 | 0.33 / 0.67 |
| shades | 2 hounds x (4 + 3 x 2 Strength II) = 10 x 1 bite/s x 20 s = 400 / 22; at 25 to 50% uptime 100 to 200 / 22; Thunderbird 10 strikes x 5 = 50 / 20 | 18 / 4.5 to 9 / 2.5 |
| shulkershell | 80% x 4 s / 14 | 0.23 |
| singularity | 6 / 22 per enemy | 0.27 |
| dragon_breath | 5 pulses x 5 = 25 / 30 per enemy; Zone: 6 applications x 5 pulses x 5 = 150 / (8 + 30 x 2.0) | 0.83 / 2.2 |
| hollow | 20 / 36 alone; Burst on a tight pack of 6: 3 centres x (20 + 2 x 8) + 3 others x (3 x 8) = 108 + 72 = 180 / (6 + 36 x 1.5) | 0.56 / 3.0 |
| infinity | 6 s / 32 vs Deflect 8 s / 9 | 0.19 vs 0.89 s/mana |
| sonic_boom | 16 / 35; Sanguine Rite 12 / 14 (costs 3 HP) | 0.46 vs 0.86 |
| starmaw | 14 / 32; with two good effects 20 / 32 | 0.44 / 0.63 |
| wither | Wither III interval 40 >> 2 = 10 ticks, 1 damage: 16 in 8 s / 25; Wither IV interval 5: 4/s x 6 s = 24 / 25 | 0.64 / 0.96 |
| borrowed_time | heals what you took: at 15 damage 15 / 14; at 6 damage 6 / 14; Heal 8 / 12 | 1.07 / 0.43 vs 0.67 |
| countdown | 6 / 7 | 0.86 |
| accelerate | Swift 6 + Haste 4 + Leap 4 + Regrowth 10 = 24 vs 14; passive upkeep 14 x 0.12 | 1.7x; 1.7 mana/s |
| chronoshift | cooldown = cost / 20 s (10 to 400 ticks), so 3 s clears any spell of 60 mana or less, but only costs mana to use; value is about one extra cast when cooldown-limited, none when mana-limited | conditional |
| foresight | 2 blows x 30 (Warden melee on Normal) = 60 / 14 = 4.3 HP/mana; vs a zombie 2 x 3 = 6 / 14 = 0.43; Heal 0.67 | 6x / 0.6x |
| reckoning | 12 cap / 18 needs 24 damage counted in 4 s | at most 0.67 |
| riposte | 2 x 6 = 12 counter damage for 2 more mana than Foresight | over its parent |
| stasis | 5 s / 34 vs Freeze 2.5 s / 16 | 0.147 vs 0.156 s/mana |
| doomclock | (8 + 0 to 12) / 28 per bearer; an enemy within 3 blocks of three clocks takes three bursts | 0.29 to 0.71 (x3 in packs) |
| rewind | up to (health 5 s ago - now), 17 HP in the example / 26 | 0.65 |

Passive upkeep is `cost x 0.12` mana per second (`Passives.UPKEEP_PER_COST`): Infinity 32 x 0.12 = 3.84, Anchor 0.48, Accelerate 1.68.

## Appendix B. What Amplify, Extend and Widen do

From the trait list and the code (`Runes.java`, `SpellNumbers`); Frugal, Linger and Overcharge follow the generic rules. "Nothing" means the rune has no such trait, so the modifier is dead weight on it.

| rune | Amplify | Extend | Widen |
|---|---|---|---|
| anchor, blind, hex, veil, infinity, shulkershell | nothing | duration | nothing |
| collect | nothing | nothing | radius 8 (max 24) |
| phantom | scales the burst through your innate scale | delay 4 s | nothing |
| umbra, devour, blackspark, sonic_boom, starmaw | damage | nothing | nothing |
| banish, grapple, pull | distance or impulse | nothing | nothing |
| portalfall | 2 damage only (the drop is fixed) | nothing | nothing |
| echolocate | nothing | glow 10 s, slow 3 s | radius 16 (5 Widens reach 121 blocks, capped at 32 creatures) |
| hush | nothing | 6 s | radius 4 |
| eclipse, riftcall, gravity_well, dragon_breath | damage | duration (dragon breath: more pulses) | radius |
| malison | damage | curse 8 s | pass-on reach 6 |
| entropy | damage | the top step lasts longer | nothing |
| blackflame | damage | burn time | nothing |
| resonant_shriek | damage | Darkness 6 s | nothing |
| shades | Strength level (round(power)) | lifetime 20 s | nothing |
| singularity | burst and fling | nothing | radius 5 |
| hollow | damage | nothing | radius 4 |
| wither | nothing (fixed Wither III) | duration | nothing |
| warp | nothing | debuff 2 s | nothing |
| warp_step, zipper, blink, shadowstep, time_skip, rewind, borrowed_time | nothing | nothing | nothing |
| countdown | damage | nothing (the fuse is fixed) | nothing |
| accelerate | Speed and Haste level (capped at IV) | 10 s | nothing |
| chronoshift | Haste and Speed level (up to +2), cooldown shift x0.5 to 2 | 5 s | nothing |
| foresight | charges round(2 x power) (3 with one Amplify) | 15 s | nothing |
| riposte | answer 6 x power | 10 s | nothing |
| prolong | nothing | +30 s | nothing |
| reckoning | the 12 cap x power | window 4 s to 8 s | nothing |
| timesteal | nothing | cap 30 s to 60 s | nothing |
| doomclock | damage | fuse 3 s to 6 s | burst radius 3 |
| stasis | nothing | hold 5 s to 10 s | nothing |

## Limits of this audit

* Nothing was played in the game. Every number is from the code; every "feel" statement is from reading the visual and sound calls and the two existing screenshots that cover these runes (`fx_barrage_stasis.png`, `fx_hollow.png`, `fx_eclipse.png`, `fx_riftcall.png`), which are captures taken a few ticks in and show little of the effect itself.
* Unverified vanilla behaviour: whether mob AI honours Blindness; the exact fall damage of Portalfall on every mob; tamed wolf attack damage and attack rate in 26.3 (I assumed 4 damage and one bite per second, the vanilla `MeleeAttackGoal` cooldown); the damage-type tags of `dragon_breath` and `sonic_boom` on every target; how Mannequins are targeted by monsters (Phantom is untested).
* Real playtest values will move every ratio in section 2; the ordering and the clusters are the durable part. The Shades ratio in particular depends on how long hounds really survive and fight.
* The three presentation reads (void T1 and T2, void T3 and T4, time and caster cues) were done by read-only helpers; I checked the load-bearing claims (the clock primitive, the eye-clearance rule, the crimson Time Skip colour, the late cast sounds, the shared sounds) against the code myself.
