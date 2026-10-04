# Wildercord — Design

A Fabric mod for Minecraft Java 26.3. Thread simple runes onto a Cord you wear,
in any order you like, and fire the whole sequence with one key.

> Status: v1 design, for review. Numbers are starting values for playtesting.

## The pitch in one line

**Find runes → thread them onto your Cord in order → press R → fuse what you
find into stronger runes, or tie whole spells into single Knots.**

Every rune does one simple thing (no fall damage, a fireball, a lightning bolt).
The power comes from the order you put them in.

## Picks

| Question | Pick | Why |
|---|---|---|
| Name and theme | **Wildercord.** Runes are threaded onto a cord like beads, and the order on the cord *is* the spell. | Sequences are the core, so the theme is literally a sequence. It also gives the project folder a meaning. |
| Sealed spells | **Knots.** Tie a finished spell into one rune. | Fits the cord theme better than "Sigil". |
| Resource | **Mana plus an automatic cooldown.** | A 12-rune chain has to cost more than a 2-rune one. Cooldowns alone can't price that. |
| Audience | **Co-op SMP first, PvP supported, single-player works.** | Friendly fire is off by design. PvP numbers are tuned down in config. |
| Relation to Attuned | **Standalone, and safe to install alongside it.** | Attuned covers passive builds and Wildercord covers active spells. They don't compete. |

## Core loop

1. **Find or craft runes.** Early runes come from crafting recipes that unlock when you
   pick up the key ingredient (grab a blaze rod and the Fire rune recipe appears).
   Rarer runes come from structures, bosses and a few world events.
2. **Learn them.** Right-click a rune to learn it forever. It goes into your
   **Codex** and survives death. Spare copies are for fusion or trading. A new rune starts
   **unread** (a hint of what it does) until you've cast it and seen it at work.
3. **Thread spells.** Open the Cord screen (K) and click or drag runes into a spell's
   sockets. A live readout explains exactly what the spell will do.
4. **Cast.** R casts your selected spell. Hold V for the spell wheel.
5. **Grow.** Upgrade your Cord for more sockets and spells. Fuse runes into new
   ones, and tie your best spells into Knots to save sockets or give them away.

## The Cord

Worn in its own extra slot in the inventory screen, next to the offhand. You
have to wear a Cord to cast.

| Cord | Sockets per spell | Spells | Rune tiers | Max mana | Regen /s | How |
|---|---|---|---|---|---|---|
| Twine Cord | 3 | 1 | I | 100 | 5 | 3 string + 1 Blank Rune |
| Copper Cord | 5 | 2 | I–II | 150 | 6 | Twine Cord + 4 copper ingots + 1 amethyst shard |
| Amethyst Cord | 8 | 3 | I–III | 225 | 7 | Copper Cord + 4 amethyst shards + 2 gold ingots |
| Echo Cord | 12 | 4 | I–IV | 300 | 8 | Amethyst Cord + 2 echo shards + 1 netherite scrap |

- **A Cord only holds runes up to its tier.** Stronger runes are locked in the editor,
  and the server refuses them.

- Spells are saved on the **player**, not on the Cord item. Upgrading or swapping
  Cords never loses anything. If you drop to a smaller Cord, the extra sockets
  and spells are kept but go quiet until you have room again.
- The first time you wear a Cord you learn **Self**, **Bolt** and **Push**, so
  you can cast something straight away.

## Growing your mana

Every source stacks. The Cord screen's mana badge shows exactly where your numbers come from.

| Way | What it does | How to get it |
|---|---|---|
| **Mana Crystal** | +10 max mana, forever (up to 10 crystals, +100) | Craft: diamond + 4 amethyst shards + 4 lapis. Found in ancient cities, end cities, stronghold libraries, trial vaults, bastions, mansions and buried treasure |
| **Reservoir** I–III | +25 max mana per level | Cord enchantment: enchanting table, anvil, books, librarians |
| **Wellspring** I–III | +25% mana regeneration per level | Cord enchantment |
| **Siphon** I–II | +2 mana per creature your spells hit, per level (up to 16 per cast) | Cord enchantment (rarer) |
| **Potion of Clarity** | +50% regeneration per level for 3 min (8 min long, II for 1.5 min) | Brew: Awkward + amethyst shard; redstone to lengthen, glowstone to strengthen |
| **Potion of Mana** | Instantly restores 60 mana (II: 120) | Brew: Awkward + lapis lazuli; glowstone to strengthen. Splash and lingering work too |
| **Meditation** | +100% regeneration | Sneak and stand still for a second while wearing a Cord |
| **Ley line** | +100% regeneration, and Heart Circles form twice as fast | Walk onto one (Cord-wearers see its ribbons of violet light) |
| **Wellstone** | +50% regeneration within 12 blocks | Craft one and set it on a ley line |

## Runes

A rune belongs to one of four families, plus Knots. Every family has its own
colour on the item and in the UI.

| Family | Colour | Job |
|---|---|---|
| **Shape** | teal | *Where* it goes: who or what gets hit. |
| **Effect** | its element's colour | *What* happens to whatever got hit. |
| **Modifier** | gold | *Changes* the closest rune to its left that it can affect. |
| **Link** | violet | *When* the rest of the spell fires. |
| **Knot** | the cord's colour | A whole spell tied into one rune. |

### The one rule that makes everything compatible

Every spell passes along one packet of information:

- who cast it;
- what is being hit (creatures, a block, a point);
- which way it faces;
- power, radius, duration and count.

A rune only reads and changes that packet. That is why *any* rune works with
*any* other rune, including runes from add-ons that have never heard of each
other.

### How a spell reads (grammar)

- **Shapes start a group.** A spell that doesn't begin with a shape begins with an
  invisible **Self**.
- **Effects** apply, in order, to everything the group's shape hits.
- **Modifiers** attach to the *closest rune to their left that they can change*.
  For example, Split skips past effects to find a shape. The readout always shows
  what each modifier attached to, and warns when it does nothing.
- **Several shapes without a link** fire at the same time, from the same place.
  `Bolt · Fire · Burst · Heal` = a fire bolt *and* a healing burst.
- **A Link** makes everything after it fire *later*, from the place its condition
  happened. For example, On Hit fires where the bolt hit.
- **Friendly fire is off by design.** Harmful effects never touch you, your team
  or your pets. Helpful effects only touch you and your allies. **Self** always
  means you.

### Examples, from simple to strong

| Spell | What it does | Cord needed |
|---|---|---|
| `Self · Feather Fall · Extend` | 16 seconds of no fall damage | Twine |
| `Bolt · Fire · Split` | a fan of 3 fireballs | Amethyst (Split is Tier III) |
| `Burst · Heal · Widen` | heals you and everyone near you | Copper |
| `Self · Launch · On Land · Burst · Lightning` | rocket up, then lightning on everything where you land | Amethyst |
| `Bolt · On Hit · Zone · Frost · Widen` | a frost bomb that leaves a wide ice field | Amethyst |
| `Beam · Lightning · Chain` | lightning that jumps to 3 more enemies | Amethyst |
| `Bolt · Pierce · On Kill · Blink` | a piercing shot; on a kill you teleport to the body | Amethyst |
| `Bolt · Homing · Fire · Split · On Hit · Burst · Explode · Echo` | 3 homing fireballs that explode, then the whole thing again | Amethyst |

### Mana and cooldown

- Each rune has a base cost.
- Shapes add a small base cost and **multiply** the effects in their group:
  - Self, Touch ×1.0; Bolt ×1.1; Beam ×1.2;
  - Burst ×1.5; Zone ×2.0; Rain ×2.5.
- Modifiers multiply the cost of what they attach to. A modifier on a *shape*
  (Split, Pierce, Homing, Chain…) multiplies that shape's whole group. Rapid, Vow and Blood Price
  sit on a shape but change the whole spell, so they multiply the whole spell's cost wherever they
  sit (Rapid ×1.4; Vow and Blood Price ×1, their price being the cooldown or the health).
- A Link costs 2, and Echo costs as much as everything it repeats. After On Hit or On Kill (which
  fire at every creature), an Echo or a Pulse goes off for the first hit or kill only, as it's paid
  for once.
- **Cooldown** = spell cost × 0.05 s, from 0.5 s up to 20 s, per spell. A
  40-mana spell has a 2-second cooldown.
- If you can't afford a spell you get a clear message ("Not enough mana — 38/46")
  and nothing is spent.

## v1 rune roster (43 runes, plus 12 fusion runes)

Tiers: **I** early game · **II** mid game · **III** late game · **IV** boss.

### Shapes (7)

| Rune | Tier | Hits | Base cost | Effect multiplier |
|---|---|---|---|---|
| Self | I | you | 0 | ×1.0 |
| Touch | I | what you're looking at, within reach | 1 | ×1.0 |
| Bolt | I | a flying projectile, up to 48 blocks | 3 | ×1.1 |
| Beam | II | an instant line, first thing within 24 blocks | 4 | ×1.2 |
| Burst | II | everything within 4 blocks of the current point | 6 | ×1.5 |
| Zone | III | a field (radius 3) at the point you look at; re-applies every 1 s for 6 s | 8 | ×2.0 |
| Rain | III | 5 strikes from the sky over 2 s around the point you look at (radius 4) | 10 | ×2.5 |

### Effects (22)

| Rune | Tier | Element | Kind | Does | Cost |
|---|---|---|---|---|---|
| Feather Fall | I | wind | helpful | Slow falling and no fall damage for 12 s | 6 |
| Swift | I | wind | helpful | Speed III for 10 s | 6 |
| Night Eye | I | arcane | helpful | Night vision for 60 s | 3 |
| Heal | I | life | helpful | Restores 8 health (4 hearts) | 12 |
| Harm | I | arcane | harmful | 7 magic damage | 8 |
| Push | I | wind | harmful | Knocks targets away from the spell | 4 |
| Light | I | arcane | world | A light source at the point for 60 s | 2 |
| Grow | I | life | world | Bone-meals the block hit and everything around it (3×3×3) | 4 |
| Shield | II | earth | helpful | A one-time spell shield for 30 s: magic circles spawn in front of the next harmful spell and stop it, unless it cost more mana than the one that raised it (then they shatter and it goes through). More strength, more circles. See [Shields](#shields) | 12 |
| Launch | II | wind | harmful | Flings targets upward. On Self it flings *you* up and forward | 8 |
| Dash | II | wind | harmful | Shoves targets 8 blocks the way you're facing. On Self it's a dash | 6 |
| Pull | II | void | harmful | Pulls targets toward the spell, leaving them staggered and pulled for 4 s | 5 |
| Fire | II | fire | harmful | 5 fire damage and sets alight for 6 s | 8 |
| Frost | II | frost | harmful | 5 freeze damage, Slowness III for 4 s, and the frost leaves it brittle for Shatter | 8 |
| Break | II | earth | world | Mines the block (up to iron-pickaxe hardness; Amplify raises it to diamond). Respects claims and spawn protection | 4 |
| Lightning | III | storm | harmful | A 12-damage strike on each target that slows and burns (an enemy takes a cast's strongest strike once, however many land beside it). You and your allies are immune | 20 |
| Blink | III | void | movement | Teleports you to where the spell landed (max 40 blocks, always to a safe spot) | 15 |
| Explode | III | fire | harmful | 12 damage in a 3.5-block blast that throws what it hits; blasts of one cast never stack on one enemy. Never breaks blocks | 18 |
| Sonic Boom | IV | void | harmful | 16 damage that ignores armour, and 8 to everything else on the line to the target, through walls | 35 |
| Wither | IV | void | harmful | Wither IV for 6 s: it spreads to whoever strikes it in melee, and the withered cannot heal | 25 |
| Dragon Breath | IV | void | harmful | A 3-block cloud that rolls on the way you blew it: 5 damage per second for 5 s | 30 |
| Summon | IV | arcane | helpful | 3 vexes fight for you for 15 s *(not in the first playable build)* | 30 |

### Modifiers (9)

| Rune | Tier | Attaches to | Does | Cost × |
|---|---|---|---|---|
| Amplify | I | effect | +50% power (damage, healing, force, blast) | 1.5 |
| Extend | I | effect, Zone, Delay | +100% duration | 1.4 |
| Widen | II | Burst, Zone, Rain, Explode | +50% radius (an effect grows at most 8 times, five Widens) | 1.5 |
| Quicken | II | Bolt, Delay, Zone | Projectiles twice as fast, delays halved | 1.2 |
| Pierce | II | Bolt, Beam | Passes through up to 3 targets | 1.3 |
| Bounce | II | Bolt | Bounces off blocks up to 3 times | 1.3 |
| Split | III | any shape | Three copies (bolts and beams fan out; bursts and zones spread) | 2.4 (whole group) |
| Homing | III | Bolt | Steers toward the nearest enemy within 12 blocks | 1.4 |
| Chain | III | Bolt, Beam, Touch | After a hit, jumps to up to 3 more enemies within 6 blocks | 1.8 |

Modifiers stack: `Amplify · Amplify` = +125% power at ×2.25 cost.

### Links (5)

| Rune | Tier | The rest of the spell fires… | From |
|---|---|---|---|
| Delay | I | 1 s later (Extend makes it 2 s, Quicken 0.5 s) | you, at that moment |
| On Hit | II | each time the group before it hits something | the hit point or creature |
| On Land | II | when you next touch the ground (within 10 s) | where you landed |
| On Kill | III | when the group before it kills a creature | the body |
| Echo | III | again: repeats everything before it once more, 0.5 s later (stack up to 3) | the original place |

## Expansion: more magic

### New shapes
| Rune | Tier | Does |
|---|---|---|
| Arc | I | Lobs a bolt that falls and splashes everything within 2 blocks where it lands |
| Cone | II | Sweeps a 60° cone up to 6 blocks in front of you |
| Trail | II | For 5 s your footsteps leave glowing patches that hit whatever stands on them |
| Wall | III | A 7-block line across where you look; hits whatever crosses it for 5 s |
| Orbit | III | Three orbs circle you for 8 s and hit whatever they touch |

### New effects
| Rune | Tier | Element | Does |
|---|---|---|---|
| Shock | I | storm | 4 damage that arcs to one more enemy (a wet or metal-armoured one first) |
| Haste | I | arcane | Haste II for 30 s |
| Reveal | I | arcane | Targets glow through walls for 15 s |
| Regrowth | II | life | Regeneration that takes hold: I for 3 s, II for 3, III for 2 |
| Cleanse | II | life | Removes harmful effects, fire and freezing |
| Stoneskin | II | earth | Resistance II for 10 s, and Slowness I |
| Root | II | earth | Vines hold targets in place for 3 s |
| Veil | II | void | Invisibility for 12 s; nearby monsters lose track of you; the first damage you deal from it is +50% and ends it |
| Empower | II | arcane | Strength II for 10 s |
| Levitate | II | wind | Targets float helplessly for 3 s (on Self, you float) |
| Freeze | III | frost | Frozen solid for 2.5 s (1.5 on players): mobs stop completely; Shatter or Fracture ends the hold | 14 |
| Meteor | III | fire | A meteor falls on each target (two at most) 1.2 s later: 12 damage in 3.5 blocks, and the crater burns on |
| Tremor | III | earth | The ground erupts: 8 damage within 4 blocks, throwing enemies up |
| Gravity Well | III | void | Drags every enemy within 7 blocks into the point for 2 s, pulls fliers down, leaves them pulled |
| Summon | IV | arcane | Three spirit wolves fight for you for 20 s |

### New modifiers and links
| Rune | Tier | Does |
|---|---|---|
| Frugal | I | Any effect: half the mana, 40% weaker and shorter |
| Linger | II | Damage and healing effects land twice more, a second apart |
| Volley | II | Bolt, Arc or Beam fires three times in quick succession |
| Pulse | II | The rest of the spell fires three times, a second apart, from you |
| On Hurt | II | The rest fires at whatever next hurts you (within 15 s) |

### Batch 3
| Rune | Family · category | Tier | Does |
|---|---|---|---|
| Ring | Shape · Area | II | A ring expands from you out to 7 blocks, hitting everything it passes |
| Pillar | Shape · Area | II | A column erupts where you look (1.5 blocks wide, 6 high) |
| Wave | Shape · Projectile | II | A 3-wide wave rolls 14 blocks forward along the ground |
| Mine | Shape · Lingering | II | A hidden rune that fires when an enemy steps near (30 s) |
| Totem | Shape · Lingering | III | A floating totem that pulses every 2 s for 10 s |
| Venom | Effect · Damage | II | 2 damage, then 0.75 a second for 4 s with Poison I; it passes once to up to 3 enemies within 2.5 blocks |
| Smite | Effect · Damage | III | A ring closes at the target's feet; 0.7 s later 13 holy damage (doubled against undead) that strips Absorption |
| Inferno | Effect · Damage | III | Everything within 4 blocks burns: 3 damage/s for 4 s |
| Thunderclap | Effect · Damage | II | A flash, then a crack: 5 damage within 3 blocks, half a second's stun and a forgotten target |
| Starfall | Effect · Damage | IV | Eight falling stars, 6 damage each (Elder Guardian drop) |
| Blind | Effect · Control | I | Blindness and darkness for 5 s (3 on players); a blinded monster lashes out at the creature beside it |
| Chill | Effect · Control | I | Slowness II for 6 s and 1 freeze damage |
| Silence | Effect · Control | II | Casters can't cast for 4 s (2 on players) and a cast in hand is cut short; monsters forget their target and are weakened for 6 s |
| Fireward | Effect · Support | II | Fire resistance for 30 s |
| Nourish | Effect · Support | I | Restores 6 hunger |
| Tidebreath | Effect · Support | I | Water breathing and faster swimming for 30 s |
| Leap | Effect · Support | I | Jump Boost III for 15 s |
| Grapple | Effect · Movement | II | Pulls you to where the spell hit, and stops you there |
| Harvest | Effect · World | I | Harvests grown crops around the block hit and replants them |
| Icepath | Effect · World | I | Freezes water within 3 blocks into walkable ice |
| Collect | Effect · World | I | Pulls items and experience within 8 blocks to you (Widen reaches up to 24), never out of land you can't build on |
| Excavate | Effect · World | II | Mines a 3x3 area of blocks |
| Focus | Modifier · Area | II | Half the radius, +50% power |
| Overcharge | Modifier · Power | III | +150% power, triple the mana |
| Rapid | Modifier · Timing | II | Halves the whole spell's cooldown |
| If Sneaking | Link · Condition | II | The rest fires only if you're sneaking: two spells in one |
| On Low Health | Link · Reactive | III | The rest fires when your health drops below 30% |

### Batch 4: techniques
Two new elements arrive: **Time** (pale gold) and **Blood** (crimson). Effects gain a **Time** category.

| Rune | Family · category | Tier | Does |
|---|---|---|---|
| Domain | Shape · Lingering | IV | A 9-block dome around you for 6 s: everything inside is struck every second, enemies inside are slowed. Widen grows it up to 24 blocks, and its frame can be seen from up to 512 blocks away |
| Crescent | Shape · Projectile | II | A 5-wide slash flies 16 blocks, cutting each creature once |
| Barrage | Shape · Direct | II | 8 blows in one second on everything right in front of you (35% power each; Quicken adds 4) |
| Orb | Shape · Projectile | III | A slow orb drifts 20 blocks through creatures, striking everything within 2 blocks once a second, and bursts on walls |
| Blitz | Shape · Direct | II | You flash up to 8 blocks forward and strike everything you pass through |
| Cleave | Effect · Damage | III | 6 damage + 10% of the target's max health (up to 20 more); enemies beside it take half |
| Dismantle | Effect · Damage | II | Three slashes 0.1 s apart, 3 damage each, through armour; the last is twice as deep from behind |
| Blackspark | Effect · Damage | III | 8 damage; 1 in 4 hits deal 2.5x and give you Strength and Speed for 6 s |
| Aftershock | Effect · Damage | II | 5 damage, then the same spot struck again half a second later for 5 |
| Resonance | Effect · Damage | III | 4 damage and a 10 s mark; every other marked enemy within 16 blocks takes half |
| Ripple | Effect · Damage | II | 6 damage (x2 on undead), heals you a quarter of what it took; a ripple 0.4 s later hits its neighbours |
| Primer | Effect · Damage | III | The target explodes 2 s later (or the moment it dies): 10 damage within 3 blocks, an enemy takes only the strongest bomb, no block damage |
| Blackflame | Effect · Damage | III | 3 damage a second for 6 s, water can't stop it, spreads if the target dies burning |
| Hollow | Effect · Damage | IV | 20 damage to the creature hit (it is gone for the wind-up), and everything within 4 blocks is dragged in for 8 more (Wither drop, 50%) |
| Repel | Effect · Damage | II | 5 damage and a violent outward blast within 3 blocks (Breeze drop) |
| Decree | Effect · Control | II | Stuns everything hit for 2 s; costs you 2 health per cast (Evoker drop) |
| Weigh | Effect · Control | II | Triple gravity, crippled legs and jumps for 5 s; fliers are dragged down |
| Shackle | Effect · Control | II | Chained to the spot for 5 s: yanked back past 2 blocks, and the chain bites for 2 |
| Bubble | Effect · Control | II | Floats helplessly for 3 s, pops for 4 damage and leaves the target soaked (Witch drop) |
| Infinity | Effect · Support | IV | 6 s: projectiles stop in the air around you, and hostile things are slowed harder the closer they come (Ender Dragon drop) |
| Reversal | Effect · Support | IV | 30 s: one killing blow leaves you at half health instead |
| Reflect | Effect · Support | III | 10 s: attackers take 60% of their damage back |
| Overdrive | Effect · Support | II | Strength II, Speed II, Haste II for 10 s; lose 1 health every 2 s; Strength III under half health, IV under a quarter |
| Foresight | Effect · Support | III | The next 2 attacks within 15 s miss, with a sidestep (12 damage turned away at most; the same spell never refills it) |
| Restore | Effect · Support | III | Heals 6, puts out fire, mends 5% of every worn and held item |
| Swap | Effect · Movement | II | You and the first creature hit trade places |
| Zipper | Effect · Movement | II | Steps you through the wall you face, up to 6 blocks thick (never through bedrock) |
| Shadowstep | Effect · Movement | III | You reappear behind the first creature hit, facing its back; your next blow on it is +50% |
| Stasis | Effect · Time | IV | Stops time on everything hit for 5 s; every hit meanwhile (spells, explosions, arrows, anything) is held and lands all at once afterwards. Stasis always applies before the other effects in its group (Elder Guardian drop, 25%) |
| Rewind | Effect · Time | IV | You return to where you were 5 s ago (if it is safe), with that health if it was more |
| Accelerate | Effect · Time | III | Speed II, Haste III, Jump Boost II and Regeneration for 10 s; your charge fills 40% faster and bolts and arcs fly 50% faster |
| Time Skip | Effect · Time | III | You vanish, reappear up to 8 blocks ahead, monsters lose track of you, and nothing can hurt you for a moment (not again for 5 s) |
| Rampart | Effect · World | II | A 5-wide, 3-high earth wall for 10 s; broken by hand it crumbles, and however it's broken (an explosion too) it drops nothing |
| Shades | Effect · Summon | III | Two frail shadow hounds for 15 s that bite hard only in dim light, each bite leaving a shadow |
| Thunderbird | Effect · Summon | III | A storm bird circles overhead for 12 s; every 2 s it marks the enemy you last hit and dives on that spot for 4.5 (2 at most) |
| Vow | Modifier · Power | III | On a shape: its effects hit twice as hard, the whole spell's cooldown is 5x longer (up to 60 s) |
| Blood Price | Modifier · Power | III | On a shape: the whole spell costs 1 health per 4 mana instead of mana, never lethal |
| Execute | Modifier · Power | II | On an effect: double power against targets under half health |
| If Airborne | Link · Condition | II | The rest fires only while you're in the air |
| Combo | Link · Condition | III | The rest fires only on every third cast of this spell |
| Imbue | Link · Trigger | II | The rest is stored, with 3 charges, in the item in your hand (Self; empty-handed, the block you look at) or any block the shape touches, and paid for three times over. See [Imbuing](#imbuing) |

Shapes that strike again and again (Domain, Zone, Totem, Orbit, Wall, Trail, Rain, Barrage, Orb) get a fresh 64-creature budget for every strike, so a big one keeps working to the end. The Siphon cap still counts for the whole cast.

Also new: one player can have at most **6 spirits** at once (Summon and Shades together), so a Zone or Totem carrying Summon no longer floods the world with wolves.

Combos worth trying:
- `Cone · Dismantle · Execute`: a sweep that finishes off anything below half health.
- `Bolt · Pull · Repel` or `Gravity Well · Delay · Burst · Repel`: sets off **Collapse**.
- `Bolt · Bubble`, then any Shock spell within 5 seconds of the pop: the soaked target sets off **Conduct**.
- `Barrage · Stasis · Harm`: every blow is held, then lands at once when time moves again.
- `Self · Swift · Combo · Blitz · Cleave`: every third cast ends in a dash that cuts through a line of enemies.

### Batch 6: sparks, shields and pickaxes
More simple spells for the early and middle game: energy balls and beams to carry any effect,
protection, mining and building, and a cheap spell for every element. Every one is Tier I or II
and craftable.

| Rune | Family · category | Tier | Does |
|---|---|---|---|
| Spark | Shape · Projectile | I | A quick spark darts 16 blocks to the first thing in its path; cheap (cost 1, effects ×1.0) and quick to recover, but its effects land at 75% power |
| Ray | Shape · Direct | I | An instant 10-block line to the first thing it meets (Pierce and Chain work on it) |
| Nova | Shape · Area | I | Everything within 2.5 blocks of you |
| Wisp | Shape · Projectile | II | A slow wisp that chases the nearest enemy within 16 blocks for up to 4 s |
| Comet | Shape · Projectile | II | A heavy ball that flies 24 blocks and bursts on the first thing it touches: everything within 3 blocks |
| Ricochet | Shape · Projectile | II | An orb that falls, bounces 4 times (Bounce adds 3) and passes through creatures, hitting each once |
| Cluster | Shape · Projectile | II | A ball that breaks into 5 shards where it hits; each strikes everything within 1.5 blocks of where it lands, nothing twice |
| Lance | Shape · Direct | II | A 16-block line of light through every creature in it (Widen makes it wider) |
| Sweep | Shape · Direct | II | A 10-block beam swung across 100° in half a second, hitting each creature once |
| Prism | Shape · Direct | II | A beam that splits into three rays at the first creature it hits; each ray strikes the next creature behind |
| Stream | Shape · Direct | II | A beam that follows your aim for a second: 6 strikes at 35% power (Quicken: 12) on the first thing within 20 blocks |
| Barrier | Effect · Support | I | 2 absorption hearts for 20 s |
| Brace | Effect · Support | I | 80% less damage for 2 s; bracing again takes 6 s |
| Anchor | Effect · Support | I | 15 s: no knockback from blows or blasts, no spell can move you, and 4 armour (8 once you stand still) |
| Bramble | Effect · Support | I | 10 s: whatever hurts you from within 4 blocks takes 3 damage and is shoved away |
| Frostward | Effect · Support | I | 60 s: you can't freeze, not even in powder snow, a frost hold on you lasts a second at most, and frost can't leave you brittle for Shatter |
| Cushion | Effect · Support | I | 30 s: no fall damage, and a hard landing throws a gust at the enemies around you |
| Deflect | Effect · Support | II | 8 s: projectiles coming at the target are sent back at whoever shot them (turned aside if they've been sent back once) |
| Haven | Effect · Support | II | A 4-block dome for 8 s: enemies inside are shoved out once a second, projectiles from outside glance off it |
| Chisel | Effect · World | I | Mines one block at stone-pickaxe strength (Amplify: iron) |
| Glimmer | Effect · World | I | Glow lichen over the block hit and up to 4 around it: light that stays |
| Prune | Effect · World | I | Clears leaves (never ones placed by hand), grass, flowers, vines and cobwebs within 3 blocks |
| Tunnel | Effect · World | II | A 2-high, 4-deep passage into a wall (a 4-deep shaft into a floor or ceiling), iron-pickaxe strength (Amplify: diamond) |
| Vein | Effect · World | II | Mines the block hit and, for an ore, every matching ore touching it (up to 16) |
| Smelt | Effect · World | II | Mines the block hit and drops what a furnace would make of it, with the furnace's experience |
| Fell | Effect · World | II | Fells a tree: the log hit and every log joined to it at its height or above (up to 32). A log with no living leaves around it (a build) comes down alone |
| Span | Effect · World | II | A 1-wide glass bridge from your feet toward the point (up to 16 blocks; Widen: 3 wide) for 30 s. It puts back whatever it replaced, drops nothing when broken, and is taken down if the server stops |
| Ember | Effect · Damage | I | 3 fire damage, alight for 3 s (on a burning target it adds 3 s instead) |
| Icicle | Effect · Damage | I | 4 freeze damage, 6 against a slowed target |
| Pelt | Effect · Damage | I | 4 damage and a hard shove |
| Windcut | Effect · Damage | I | 4 damage and a light shove (sets up Wildfire) |
| Leech | Effect · Damage | I | 3 damage; you heal what it took, and overhealing becomes a shield of up to 4 |
| Hex | Effect · Control | I | 6 s: your spells hit the target 25% harder, and it fixes on you |
| Rend | Effect · Control | I | 10 s: 4 less armour, and what it naturally resists it takes at full strength |
| Countdown | Effect · Damage | I | 1.5 s later: 6 damage (if the mark dies first, on the nearest enemy) |
| Jolt | Effect · Control | II | 4 lightning damage and a 1 s stun (bosses and players are slowed instead) |
| Bleed | Effect · Damage | II | 2 damage, then 1 every half second for 4 s (half as much again while it moves) |
| Coldsnap | Effect · Damage | II | 4 freeze damage and Slowness II for 4 s to every enemy within 3 blocks; they're left brittle for Shatter |
| Flashfire | Effect · Damage | II | 5 fire damage to every enemy within 3 blocks, alight for 4 s; allies in it are thawed and dried |
| Banish | Effect · Control | II | The target reappears up to 8 blocks further from you, dazed, somewhere it fits and can see back to (never a boss) |
| Cyclone | Effect · Control | II | Enemies within 3 blocks whirl around the point for 2 s, then are flung out for 3 damage (bosses are struck, never moved) |

Mining runes break blocks as a player holding a pickaxe of their strength would: nothing
unbreakable, no fluids, drops as that tool would give them (a container spills its contents), and
only where you may build, within the cast's 32-block budget. Monsters' spells never change blocks.

Combos worth trying:
- `Stream · Chisel`: a drill that bores along your aim for a second.
- `Bolt · Hex · Delay · Comet · Flashfire`: the hex makes the burst land 25% harder.
- `Ricochet · Bounce · Pelt`: a stone that skips through a crowd.
- `Self · Brace · On Hurt · Jolt`: take the blow, then stun whatever dealt it.
- `Spark · Volley · Ember`: three quick embers in a row.

### Batch 7: more ways to build a spell
Eighteen crafted runes that give spells new moves rather than more of the same: three shapes that travel
or wait in new ways, three modifiers, two links that answer the reaction and affinity systems, and an
effect for every element with a job its element didn't have. All Tier I-III and craftable; Drowse, Prolong
and On Reaction also turn up in ancient city, end city and trial chamber chests.

| Rune | Family · category | Tier | Cost | Does |
|---|---|---|---|---|
| Glaive | Shape · Projectile | II | 5, effects ×1.8 | Flies out 12 blocks (sooner at a wall, where a world effect lands) and curves back to you, striking each creature on the way out and again on the way back (the way back is a strike of its own, with its own creature budget). Widen, Quicken, Split |
| Imprint | Shape · Lingering | I | 3, effects ×1.3 | An imprint on the ground where you stand; 2 s later (Quicken: 1 s) it erupts on everything within 3 blocks. Kiting in a rune. Widen, Quicken, Split |
| Latch | Shape · Direct | II | 6, effects ×1.9 | A thread latches onto the first creature within 16 blocks of your aim (after a link, the creature that set it off) and strikes it 4 times a second apart at 70% power, snapping if it strays past 24 blocks or out of sight. Extend: 8 strikes; Quicken: 8 in the same time |
| Kindred | Modifier · Area | II | ×1.4 | A helpful effect also lands on you and on the nearest ally within 8 blocks that it missed, at half power. Needs the new share trait: every helpful effect that lands per creature, not summons, domes, horns, death saves, Soulbond, Transfusion, Cryostasis, Shulkershell, Rewind, Overdrive or innate runes |
| Thirst | Modifier · Power | II | ×1.4 | You heal for a quarter of the damage the effect really deals (a half with two, three quarters at most) |
| Belated | Modifier · Timing | II | ×1.25 | The effect, and any Linger after it, lands 1.5 s late on whatever it struck that's still there, 25% stronger (three count: 4.5 s, ×1.95). A Belated kill is too late for On Kill |
| On Reaction | Link · Trigger | III | 2 | Watches the group before it; fires the rest at each creature that group set an element reaction off on as it landed |
| On Weakness | Link · Trigger | II | 2 | Watches the group before it; fires the rest at each creature that group struck with an element it's weak to (creature affinities: players have none) |
| Spellbrand | Effect · Damage (arcane) | II | 8 | Brands each target for 8 s; the next spell damage you deal it, from the next tick on, bursts the brand for 6 arcane |
| Gash | Effect · Control (blood) | II | 9 | 3 damage, then it weeps 0.4 + 1% of its max health a second; for 8 s the target can't heal at all (Regeneration, potions, food, spells) and is bleeding. Setting health outright (a death save) still works |
| Prospect | Effect · World (earth) | I | 3 | Every ore within 12 blocks (16 at most, widened) glows through the rock for 20 s, in the colour of what it gives: block displays only, nothing in the world changes |
| Searing Edge | Effect · Support (fire) | II | 8 | 15 s: each melee hit the target lands sets the foe alight for 4 s and deals 2 more fire damage (who may be burned is the caster's call). Sustainable as a passive |
| Flash Freeze | Effect · Control (frost) | II | 9 | 4 freeze damage; a soaked target (in water, or after a water rune or Bubble) freezes solid for 3 s (1.5 on players), one only rained on for 2 s (1); a dry one is slowed and left brittle |
| Drowse | Effect · Control (life) | III | 14 | Sleep for 6 s (2 on players): no moving or fighting back, but any damage wakes it. Bosses only get Slowness II |
| Galvanize | Effect · World (storm) | I | 3 | A redstone block in the air against the face it struck, for 5 s: it powers what it touches. Only in empty air, where you may build, out of the block budget; written down with its world, drops nothing, pistons can't move it |
| Prolong | Effect · Time | III | 12 | Every good effect on the target lasts 15 s longer, up to 5 minutes; endless ones stay as they are |
| Umbra | Effect · Damage (void) | I | 6 | 4 damage, doubled where the light at the target's eyes is 7 or less (or under an Eclipse); shadowed for 6 s. An Orbit can carry it as a passive |
| Disarm | Effect · Control (wind) | II | 7 | A creature's main-hand item is taken for 5 s and given back (if its hand is still empty); windswept and a small shove. Players and bosses keep hold; nothing ever drops |

Combos worth trying:
- `Imprint · Explode`: cast it, run, and let whatever chases you meet the blast.
- `Bolt · Frost · Fire · On Reaction · Burst · Explode`: the burst goes off only where the Shatter lands.
- `Bolt · Venom · On Weakness · Latch · Harm`: an undead hit by life is held and struck four more times.
- `Beam · Spellbrand · Delay · Glaive · Harm`: the glaive's first pass bursts the brand.
- `Latch · Gash · Belated`: hold a healer down and keep it from mending.
- `Self · Heal · Kindred`: heal yourself and the ally beside you in one cast.

### Flight
One crafted rune that lets a caster (and their allies) fly: real flight, as in creative, for a while.

| Rune | Family · category | Tier | Cost | Does |
|---|---|---|---|---|
| Soar | Effect · Movement (wind) | III | 18 | 20 s of flight (double-tap jump to take off; jump and sneak to rise and sink) at three fifths of creative's flying speed. Extend doubles it, Frugal takes 40% off, 40 s at most; a cast during flight cannot renew it, and the wings rest for 30 s after it ends; cast while falling, it catches you at once. 3 s before the end the wind fades (a chime, wisps off the wings, a line over the hotbar), then it sets you down as Feather Fall does: slow falling, a drift the way you look, no fall damage until you land. Helpful: a shape that reaches allies lifts them too; creatures that aren't players fall slowly for as long instead. Never touches creative or spectator players, or a player who can already fly some other way. A pull (anything that leaves a creature pulled) or a grounding wind (Weigh, Downdraft) ends it and starts the 30 s rest; a dungeon's warded arena or vault won't let it lift anyone and sets down whoever flies in. Logging out saves the player unable to fly and gives back what's left on login; death ends it; another dimension sets you down. Not sustainable as a passive. Phantom Membrane, Feather, Breeze Rod; trial vaults and End cities |

Combos worth trying:
- `Burst · Soar`: you and every ally within 4 blocks take to the air together.
- `Self · Soar · Extend`: 40 seconds in the sky for an Amethyst Cord's scouting trip.
- Against a flier: `Bolt · Pull` or `Beam · Weigh` brings them down, and their wings need 30 seconds of rest.

## Heart Circles

Casters build rings of condensed mana around their heart, from the 1st Circle to the 8th (the Archmage).

- **Condensing:** every point of mana spent casting spells (Blood Price counts 5 mana per health) condenses toward the next circle. Passive upkeep doesn't count. Nothing to farm or craft, but it takes a lot of casting.
- **Breakthroughs:** every circle after the 1st also needs milestones. Early ones ask for knowledge (runes known, a better Cord); later ones for things you've actually done with magic: reactions set off, Runebound slain, secret spells found, feats. A monster counts as "defeated with spells" if it dies within 5 seconds of your spell hurting it.
- **Forming:** once the heart is ready you're told in chat, and the heart badge in the Cord screen blinks. Meditate (sneak and stand still) for 10 seconds (5 on a ley line) without getting hurt (a hit breaks your concentration and starts it over): the rings spin up, mana streams in, and the new circle forms with a title, a burst of light and a full mana refill.
- **The rings:** one per circle around the heart, each on its own tilt, blue on the inside burning to white gold on the outside (tinted toward the element you lean to). They turn while you meditate, for everyone to see. They also spin up whenever you cast, but only the people around you see that: on every cast it would fill the bottom of your own view.

| Circle | Mana condensed | Breakthrough | Opens |
|---|---|---|---|
| 1st | 600 | | Passive slot 1, your **innate rune** |
| 2nd | 2,000 | Know 10 runes | |
| 3rd | 5,000 | Wear a Copper Cord, set off a reaction | **Mana Skin**: a fifth of damage taken is paid with mana (2 mana per health) |
| 4th | 10,000 | Defeat 40 monsters with spells, set off 3 different reactions | |
| 5th | 18,000 | Know 35 runes, wear an Amethyst Cord, *Long Incantation* (slay a monster with a spell of 6+ runes) | Passive slot 2, **Flow**: cooldowns 15% shorter |
| 6th | 30,000 | Defeat 150 monsters with spells, slay 8 Runebound, set off 5 different reactions (any five of the eleven) | |
| 7th | 50,000 | Help slay a boss (Wither, Warden, Elder Guardian, Ender Dragon, the Archivist or a dimension dungeon's boss; everyone within 96 blocks counts), find 2 secret spells, *In Rhythm* (3 casts on the beat) | **Overflow**: spells cast at full mana hit 30% harder |
| 8th | 80,000 | Wear an Echo Cord, find 4 secret spells, *The Last Page* (defeat the Archivist) | **Archmage**: spells and passives cost 15% less mana |

Every circle also adds +15 max mana, +0.5 mana/s and +3% spell power (a circle cracked by overcasting gives none of this until it mends). Hover the heart badge (left of the mana badge) for your circles, perks and what the next one needs. `/wildercord circles <n>` and `/wildercord condense <mana>` set them for testing.

## Batch 5: a world of magic

Everything below came in one batch, built on one change to the engine: **a spell's caster can be
any living thing**, not just a player. Runebound monsters and the Archivist cast real spells
through exactly the same code as players (`cast.Casters` answers the player-only questions:
messages, building rights, reach).

### Charged casting and the aim preview
- **Tap** the cast key to cast at once, as before. **Hold** it and you raise both hands and a magic
  circle opens in front of them as the charge builds: the frame, then its script and star drawing
  themselves, then a roundel for each rune in turn. At a full charge (1.5 s) it flares and chimes.
- **Every spell writes its own magic circle**, built like a classic magic circle from the outside in:
  a heavy frame with rays on the star's points; a band of script, the spell's own rune emblems again
  and again; a band in the first effect's pattern (its element at a glance); a star polygon with a
  point for every rune ({5/2} for five runes, {7/2} for seven, a hexagram or octagram for short
  spells) and on each point a roundel, that rune's own ring pattern around its emblem, in casting
  order around from the top; an inner ring; and the first rune's emblem as the seal in the middle
  (usually the shape's; a spell that starts with an effect, cast on yourself, shows that effect's).
  The bands and the star turn, each its own way.
- **Every rune has its own emblem and ring pattern**, no two alike. The pattern's line tells the
  family (Shape: a double line, Effect: solid, Modifier: dashed, Link: a chain), its motif tells an
  effect's element (flame teeth, crystals, zig-zags, waves, crenels, buds, crescents, stars,
  hourglasses, drops); the emblem's frame tells the family too (square, circle, diamond, octagon).
  Anyone who has learned them can read a spell off its circle, and learn a secret spell by watching
  it cast.
- **The same circle everywhere, never too big:** under every cast on the ground (1 block across the
  frame's radius), in front of a charging caster's hands (0.42), in a Runebound's hand as its
  telegraph (0.4; the Archivist's Sunfall shows Sunfall's own runes) and as the floor of a Domain
  (the Domain's radius). A longer spell gets more star points and smaller roundels, never a bigger
  circle. In your own first-person view your charge circle is a small seal in the lower right.
- Let go to cast: up to **+40% power** at a full charge. You walk 40% slower while charging, and
  a charge held 12 s fizzles.

### Casting as a performance: overchannel, the beat, tracing and incantations
Charging is where a caster performs, and where they take risks. The pure rules are `spell.Overchannel`,
`spell.TraceGlyph` and `spell.Incantation`; `cast.Charging` runs them on the server every tick.

- **Overchannel.** Held past full, a charge climbs a stage every **1.2 s**, up to the stages the heart holds: one
  with fewer than two working Heart Circles, two with two or three, three from the 4th (a cracked circle holds
  nothing). Each stage adds **+20% power** (I +20%, II +40%, III +60%, multiplying the full charge's +40%) and a
  **7% wild surge chance** (7/14/21%), rolled from the overcast table (`WildMagic`, `WildSurge`) when it's let go.
  From stage I the channel drains **15% of the spell's mana price a second**, never the price itself: short of a
  surplus it stops climbing and waits (and the old 12 s fizzle still ends it). Overchannel never cracks a Heart
  Circle, and a spell that would overcast can't overchannel. Each stage lands with a crack sound a step up the
  scale, a swell of the circle, a ring of sparks and a crackle across it; the circle shows a stage's four cracks
  more per stage, trembles harder, throws sparks off its rim, and the hum strains higher with a waver. The
  caster's own screen edges close in with a hairline crack from each corner per stage.
- **The beat.** Filling, and each stage landing, is a beat: a release within **0.3 s after** it (only after: a
  release travels to the server) adds **+10% power** and a chime. The HUD's badge ring closes on the charge's
  beats while charging (red for the tear), instead of the rhythm's.
- **Tearing loose.** Held **3 s** past the heart's last stage, the channel tears loose: the circle bursts, the
  release does nothing, a harmless spell-less surge goes off (`WildMagic.FIZZLES`: Butterflies, Heal All, Blink,
  Levitate, Slow Time; never Backfire), and the caster is dazed (no casting, slowed) for **1.5 s** and loses **30% of
  full mana**. It deals no damage at all. A channel stalled short of its last stage never tears.
- **Sigil tracing.** A spell's glyph is a single line of 3 to 5 strokes (one per rune, at least three) between nine
  points (a ring of eight and its centre), drawn from the rune sequence's hash: always the same, different for
  most spells and for the same runes in another order. It sits faintly round the crosshair while charging;
  holding sneak steadies the hands: the camera holds still and the mouse moves a point along it. The client scores
  coverage times `0.4 + 0.6 × precision` (held back for a path over twice the glyph's length) and sends it just
  before the release; the server clamps it to 0-1, ignores it without half a second of steadying (sneak held while
  charging), and believes at most `steady ticks / 20`. Accuracy takes up to **60%** off the overchannel's surge
  chance and adds up to **+8% power** (nothing below a third). Not tracing loses nothing. The glyph is 16% of the
  screen's short side at every GUI scale; the mouse runs at the player's sensitivity held between 0.04 and 0.3
  degrees a count; client options turn tracing off and choose its assist (how far off the line counts, and how
  strongly the point is pulled back onto it: none, light, strong).
- **Incantations.** Every rune has a syllable: a small generator (an onset, a vowel, an ending, with English words
  and rune names avoided) gives each rune of the roster its own in roster order, with the best known tuned by hand
  (Bolt "vo", Fire "ign", Frost "hrim", Shock "zar", Heal "mae", Split "sei"...). While charging, the syllables rise
  from behind the caster's shoulders as their roundels open and gather into a line over them (world-space text,
  glowing-sign style, readable up close and gone by 18 blocks), with a soft whisper under each rune's note. Every
  nearby client draws them from the synced charge. A tap says nothing: a quick cast keeps its secret, a charged one
  pays for its power with a tell. Client options hide all, others' or your own. They thin away on release, which
  is left to the spell (and its name).
- **Against players**, everything a performance adds (stage, beat, trace) counts inside the ×2.5 bonus cap
  (`SpellDefenceRules.capBonus(bonus, performance, cap)`).
- Server settings: the `channeling` section (`overchannel`, `power_per_stage`, `drain_per_second`,
  `surge_chance_per_stage`, `beat_bonus`, `backfire_stun_seconds`, `backfire_mana_burn`, `sigil_tracing`,
  `trace_power`). With `features.wild_magic` off nothing surges.
- While charging, you (only you) see **where the spell goes**: a reticle on the ground at the spell's
  real radius for Zone, Rain, Pillar, Totem and Mine; around you for Burst, Ring and Domain (under
  your feet for an Imprint); a dotted line for Bolt, Arc, Beam, Crescent, Orb, Wave and the rest that
  fly or reach (a Glaive's as far as it goes before it turns back), with a mark on the creature a
  Latch would hold.
- The circle is drawn by every client from a synced attachment, so it follows the caster's hands
  smoothly. A spell's circle is one particle, `wildercord:spell_circle`, carrying the spell's runes;
  each client builds the whole circle from them every frame, laying lines down as short pieces and
  bands as tiles, so every line keeps its width at any size. The emblems and ring patterns are
  generated by `tools/circle_art.py`.

### How spells look
Every shape is drawn in light: a new particle, `wildercord:light`, draws expanding shockwave rings,
beams (a white-hot core in a coloured halo, always facing you), sweeping crescent slashes (tapered,
brightest at the leading edge) and orbs wrapped in turning rings.
- **Bolt / Arc:** a glowing comet leaving a streak of light; the impact flares and throws a ring.
- **Beam:** fired through a small magic circle at the hand, rings of power racing down it, a flare
  where it ends.
- **Crescent:** a blade of light sweeping forward, a paler echo behind it. **Barrage:** a flurry of
  quick arcs on their own tilts, the last blow landing with a shockwave. **Blitz:** a streak of
  light, afterimages along it and a cross of slashes where you land (on the ground a Blitz now runs
  level, so looking down no longer stops it dead).
- **Burst:** a shell of light (crossed rings racing out) over a ground shockwave. **Ring:** a band
  of light racing along the ground. **Cone:** a fan of beams swept by arcs that open with it.
- **Zone:** its ornate circle on the ground for as long as it lasts, a wave of light across it each
  pulse. **Rain:** a magic circle opens in the sky, facing down; each strike is a streak of light.
  **Pillar:** a circle on the ground and a column of light erupting from it, ringed as it climbs.
- **Domain:** the spell's own magic circle spread across the whole floor, under a dome of light
  (meridians and parallels breathing); each strike pours light down from its crown; it closes by
  falling in on itself and cracking the floor.
- **Wall:** a fence of light. **Wave:** a glowing crest rolling forward. **Orbit / Orb / Totem /
  Mine / Trail:** glowing cores, orbs and seals.
- **Self:** rings of light close in as they climb you.

### How magic feels
- **Glow:** all magic is drawn with additive light, so overlapping light burns brighter; void is
  drawn as darkness (it takes light away) with a thin violet rim. Nothing opens right in front of a
  player's own eyes except a beam leaving their hand.
- **Casting poses:** a caster raises both hands while charging; when a spell goes off they move
  with its shape: a thrust (Bolt, Beam and most shapes), a sweep (Crescent), alternating blows
  (Barrage), arms flung up (Zone, Rain, Ring, Burst, Totem, Mine), spread then clasped (Domain),
  raised and brought down (Pillar), a push (Wall, Wave, Orb), swept back (Blitz), open hands (Self).
- **Screen effects:** a camera shake for everyone near something huge (explosions, bursts, pillars,
  Domains, meteors, tremors, thunderclaps, Sunfall), a field-of-view kick when a charged spell leaves
  your hands, a punch when one of your spells lands a hit of 8 or more, and a tint on the edges of
  the screen inside a Domain. All follow vanilla's Screen Effect Scale.
- **The worn Cord:** everyone sees your Cord on your right wrist, a band in its tier's material with
  a bead for each rune of your ready spell. The beads glow for a couple of seconds after you put
  the Cord on, burn brighter while you charge and flare when you cast; the rest of the time they
  show their material without glowing.
- **Sound:** every cast, impact, circle, beam, orb, shield and Domain has its own synthesised sound,
  all in one key; charging hums, rising as the charge builds.

See [ART.md](ART.md) for the style guide behind all of this.

### Element visual languages
Every effect is drawn in its element's own language, so a hit reads as fire or frost at a glance,
whatever shape carried it: a few strong shapes of light and a handful of particles.
- **Fire:** embers rising, a heat flare (a gold core in an orange bloom) and flame tongues, short
  crescents of light licking upward; big hits burst into whirling flame slashes.
- **Frost:** crystal shards (short spikes of light), a hard white shatter ring, and frost creeping
  over the ground as a glowing frost seal. Freeze closes a frozen creature in ice.
- **Storm:** branching lightning built from short beams, jagged and forking, flickering over two or
  three ticks; strikes throw forks out over the ground.
- **Wind:** crescents swirling on their own tilts, spiralling up round what they touch, and rings of
  air running out along the ground.
- **Earth:** the ground cracking (a cracked seal and a ring of dust), chips of the ground itself
  thrown up, and stone spires jutting up and sinking back.
- **Life:** petals and leaves, a leaf spiral of soft green arcs and a bloom: a gentle glow, a flower
  seal and a slow ring.
- **Void:** darkness instead of light: rings of it imploding and a black core, each edged with a thin
  violet rim, so void reads as a hole in the world.
- **Arcane:** star seals, comets of pink light tracing tilted orbits, and a shimmer of glyphs drawn in.
- **Time:** clock faces of light whose hands are crescents sweeping round, the quarter hours ticked in
  white, and golden flecks. Stasis stops the hands; Rewind turns them backward.
- **Blood:** crimson cuts, heartbeat rings pulsing in pairs, and drops falling.

Reactions are moments of both elements at once: Shatter bursts the ice through a flare of fire,
Wildfire whirls up as a fire tornado, Conduct cages the target in lightning and Implode falls in as
darkness before the blast. Overload blows the flames apart with forks of lightning, Fracture stands a
cracked frost seal over a splitting ground, Blight turns imploding darkness into billowing rot, Unweave
spins every mark off in its own colour round a star seal, Rupture crosses crimson cuts through a slash
of wind, and Elapse races a clock's hands round as what was lingering flares out at once. The secret spells are the grandest of all (a lance of ice that lightning
runs along, a sun that sinks and bursts into a pillar of fire, a crescent cut out to the horizon, a
black star with rings of light, a clock face across the whole of Zero Hour's circle). An effect on
yourself stays out of your own view (you see what reaches your feet), and the buffs a passive
renews stay quiet when they do.

### The spell wheel, names and codes
- **Hold `V`** (a tap still selects the next spell): your spells fan out in a ring with their names,
  runes and cooldowns (the Tome of the Fifth Page's too, while it's held). Point and let go. Let go without pointing and the wheel stays open: click a
  spell, press its number, or point and press `V` again (right-click or Esc closes it).
- **Every spell has a name**, made from its runes ("Splitting Frost Bolt", "Arcane Bolt › Blasting
  Burst"); rename any spell from the Cord screen. The HUD shows it above the panel.
- **Spell codes:** `wc:bolt.frost.split`. Copy one from the Cord screen; paste it in chat and it
  becomes a hoverable card (name, runes, cost, readout) that copies the code when clicked; paste it
  into the Cord screen to load it (only runes you know and your Cord holds).

### Rhythm
Cast again **just as your last spell comes off cooldown** (a window of a quarter of its cooldown,
0.25-0.5 s) and the chain grows, up to 3: **+8% power per step**. Early or late just starts over.
The HUD draws a gold ring closing in on the spell badge as the beat comes, and a note per step.

### Secret spells
Ten exact sequences become unique spells. They're all valid ordinary spells too, so a guess never
looks broken; only the exact order, with nothing before or after, counts. The first cast writes
the secret into the Grimoire with a title. Once found, they cost 1.1-1.5x the ordinary spell's mana
and have a 50% longer cooldown, and the readout, HUD and wheel show that price and their name;
until then everything shows (and the finding cast charges) the ordinary spell, so nothing gives
them away. Their names: Glacial Lance, Sunfall, Horizon Cut, Petal Storm, Tempest Step,
Black Star, Zero Hour, Rebirth, Tectonic Rise and Starlight Cascade. (The sequences are in
`spell/Secrets.java`, for developers; players find them by experimenting or from **Torn Pages**:
read one to learn the riddle of a secret you haven't found, and its margin sketches the way to
the nearest Archive.) Torn Pages turn up in stronghold libraries (45%), ancient cities
and woodland mansions (25%), trial vaults (15%), dungeons and desert pyramids (12%), and in the
Archive; Runebound and the Archivist drop them too.

### Each world's own magic
The secret spells are the same everywhere; the point of these is that they aren't. A world should feel like it has
a nature of its own, and casting should feel like finding magic, not looking it up.

- **Resonances.** At server start each world draws **12** (0-24, `resonances.count`) resonances from its seed, hashed
  through SHA-256 with a label of its own and the owner's `reroll_salt`: the same seed and salt give the same ones, and
  nothing a client sees (the ley seed included) leads back to them. One is an exact sequence of **3 or 4 runes** plus a
  **twist**. Its runes are crafted runes of Tiers I-III (lower tiers drawn 3:2:1), built from a template (a shape, one
  to three effects, perhaps a modifier or an On Hit) that the twist can ride; it must read cleanly as an ordinary spell
  (no warnings, nothing unattached), cost at most **70** mana, and be neither a secret, nor hold both runes of a
  signature fusion, nor repeat another resonance. Each world uses a twist at most once. Its **name** is a twist word, an
  element word and a rite ("the Glasswind Rite"); its **riddle** is the twist's omen and the runes in order (shapes and
  modifiers by what they do, effects by name). The draw is saved with the overworld, so new runes in a later version
  never shift it.
- **Twists** (27, `spell.ResonanceTwists`), each a modest extra with its own visuals and kit sound: Glass Rain (each
  rain strike shatters as glass: 2 damage and Slowness I in 1.8 blocks, ten strikes a cast), Kindly Flame (allies within
  4 of a fire spell's hits mend 2.5, once each, eight a cast), Birds of Light (five birds from the first strike, 2 each
  on the nearest foes), Winter Blossom (flowers bloom where frost lands, Regeneration I for 3 s to allies among them,
  three patches), Upfall (foes within 4.5 fall up, hang and slam down for 3), Pale Steed (a spectral horse for 30 s,
  Self and a helpful effect), Second Voice (the whole spell again from where it landed at half power, a second later,
  once), Slow Hour (6 s: hostile projectiles within 8 crawl), Paper Storm (three whirls of pages, 1.5 and Glowing to foes
  within 5), Star Wake (three small stars, 2.5 each), Crown of Thorns (six foes a cast: Slowness I and 1 damage three
  times), Mirror Shards (three shards for 10 s, 3 damage each to the next foe within 3.5), Soul Lanterns (a foe the
  spell ends mends you 2, three a cast), Aurora (two hearts of absorption to you and allies within 8 for 10 s), Turning
  Tide (a ring of seawater: soaked and shoved, friends put out, twice a cast), Standing Stones (3 damage and a toss in
  3.2), Storm Crown (four foes: a cloud zaps each three times for 1.5), Red Moon (six foes: each mends you 1), Sun Seal
  (3 s seal: alight and 1 a second in 2.5), Falling Blades (the three nearest foes: 3 each, bleeding), Whirling Eddy (2 s
  whirl in 3.5, then flung out for 2), Lantern Flies (Night Vision to allies, Glowing to foes within 12), Tolling Hour
  (four foes: a clock, then 3 damage three seconds later), Rime Steps (6 s of footsteps that slow foes), Crackling Wake
  (four bursts back along the path, 2 each), Sudden Thicket (foes in 3 held a moment and 1 damage) and Great Bell (foes
  within 6 slowed and weakened, allies quickened). Damage goes through `Effects.hurt` (Shields, `SpellDefence`, PvP
  scale), scales with the caster's power, takes the cast's creature budget, and no twist moves a boss. A found
  resonance costs **1.15x** the spell and recharges **1.2x** as long; until then everything shows the ordinary spell.
- **Finding one** (casting its runes, from the Cord): a title, a toast, 400 condensed mana, and, if nobody had found it
  (`announce`), a chat line to the server with its name, never its runes. Torn Pages tell a resonance's name and riddle
  half the time while any are left. The Grimoire lists this world's resonances and quirks.
- **Quirks.** **4** (0-8, `resonances.quirks`) runes get a quirk, drawn on a stream of their own: **x1.2** power, **x1.35**
  duration, or a faint second strike (30% power, half a second later, once a cast), each in a condition that suits the
  element (night, day, rain, snow, a full or new moon, the deep, the heights, underground, the Nether, the End), read where
  the effect lands. Found the first time they matter (100 mana).
- **Privacy.** A client only ever holds `spell.ResonanceLore`'s view: what its player found (everything), read (name
  and riddle) or heard announced (name and finder), the quirks they met, and the count.

### Reading runes
A newly learned rune is **unread** (the Codex shows its name, family, tier, element and cost, and a hint), **glimpsed**
after the first cast (its text, numbers veiled), and **understood** at 5 progress: a cast that holds it is 1, and the
cast landing on something 1 more (three landing casts, or five that don't). Runes known before reading existed, starter runes and
anything learned with `features.unread_runes` off are understood; weaves follow their least read rune, Knots are always
understood, and creative players see everything. The hints come from each rune's element, family, kind and words in its
description, with hand-written ones for about forty common runes.

### The Grimoire
A third page of the Cord screen: your innate rune and leaning, your affinities, the eleven reactions, the secret
spells (found ones in full, hinted ones as riddles), and the feats. Every first discovery
shows a toast, and each new reaction, feat and secret condenses mana toward your next circle: 150
for a reaction, 250 for a feat, 400 for a secret, 2,000 for defeating the Archivist.

### Innate runes
At the 1st Circle one of ten innate runes wakes in your heart, chosen at random. It can't be
crafted, found or learned from an item, and it grows **+6% per circle**. It's a Tier I effect in
the Codex's Innate category, so any Cord can hold it.

| Innate rune | Element | Does |
|---|---|---|
| Blood Thread | Blood | Threads everything hit together for 8 s: 40% of any damage one takes is dealt to each of the rest (4 at most, three shares a second) |
| Kindling | Fire | 3 fire damage and a stack; the fifth stack ignites for 14 in 3 blocks and leaves everything it reaches at two stacks |
| Twin Star | Arcane | Your next spell within 6 s is cast twice |
| Borrowed Time | Time | Heals the damage you took in the last 5 s (what you are missing); it comes back over 10 s with a fifth on top, never lethal, unless you slay a monster |
| Gale Mantle | Wind | 12 s: jump in midair to dash forward (3 dashes) |
| Stoneform | Earth | 8 s: no knockback, Resistance, and each hit you take sends out an aftershock |
| Mirrorfrost | Frost | Casts back the last spell that hit you in the past 30 s, as your own |
| Fortune | Life | 10 s: each hit you deal (spells and melee) has a 1 in 4 chance to strike for double |
| Phantom | Void | An afterimage of you (your skin) draws every monster within 16 blocks for 4 s, then bursts for 8 (1 more for every 6 its decoy soaked) |
| Stormheart | Storm | 10 s: whatever hits you is struck by lightning (once a second) |

### Affinities and leaning
Every player has an **affinity** with each element, grown by what they do: casting it (a tenth of a
point per mana its effects cost), setting off its reactions, finding creatures weak to it, and
everyday things that fit it (smelting for fire, fishing for frost, mining for earth, farming and
breeding for life, the End for void, enchanting and ley lines for arcane, a night watched for time,
melee kills for blood...). Each source has a daily allowance, so nothing can be farmed. Five levels
(100, 600, 1,800, 4,500, 10,000 points): **+3% power a level** with that element's effects, from
III **10/15/20% less** from others' spells of it (a reaction still breaks through), and at V
**10% off** the element's share of a spell's price. The first level of each is a small Grimoire
entry (100 mana). It's gentle on purpose: a mastered element is only 15% stronger, so nobody is
locked in and PvP stays fair. See [features/player-affinity.md](features/player-affinity.md).

Your magic **leans** toward your deepest affinity (level I, and 1.25x the runner-up): your Heart
Circles' rings are tinted toward its colour, and a spell with no effect in it (whose circle would
otherwise charge in gold) charges in its colour. A spell with an effect always takes its first
effect's colour. Leaning adds no power of its own: that's the affinity's.

### Shields
**Shield** guards the target (you with Self, allies with Burst or Nova) for 30 seconds (Extend
doubles it). Raising it opens its magic circles in front of them, then they vanish: a Shield is
invisible until a harmful spell comes. Its strength is the spell's full price before any discount
(a staff, Thrift, a heart perk, a mana storm or the server's mana rules never make it weaker), and
every spell that meets it is weighed the same way. `Self · Shield` is 12; anything else threaded onto
the same spell, Amplify say, makes it stronger because it makes the spell cost more.

- **The circles.** When a spell comes at them, the Shield's magic circles (the spell that raised it,
  written out like any other) spawn in between the spell and them, facing it, stacked one behind
  another: **one circle for every 8 mana** of strength, up to 7. A spell flying at them (a bolt, a
  spark, a comet) makes them appear a moment before it arrives, and strikes the front circle, not the
  creature behind it; a spell that lands at once (a beam, a blast) makes them snap open as it lands.
  A spell that would miss passes by.
- **Stopped.** A harmful spell that cost **as much or less** is stopped. It still shatters as many
  circles from the front as its mana pays for (one circle's worth at a time); the next one holds,
  light flaring at its heart and ripples racing out across it, and the Shield is spent. The rest of
  that spell (its other effects, a Zone's later pulses) is stopped too, at that creature.
- **Broken.** A spell that cost **more** shatters every circle, front to back, the way real glass
  goes: cracks shoot out from the heart across the circle, then it bursts, the rim into curved
  slivers and the rest into shards that fly on the way the spell was going, tumble, fall and glint.
  The spell goes on through and hits what it was aimed at.
- **Parried.** A Shield you raise at the last moment (within **7 ticks** of the spell reaching it, or
  while that spell is already flying at you within 7 blocks) turns the spell whatever it weighs: the
  circles ring gold, a flying spell turns round and flies back at its caster as yours, and anything
  else is negated and answered with a counter-burst (see
  [features/parry-and-wild-magic.md](features/parry-and-wild-magic.md)). The Shield is spent.
- Only spells: arrows, blades and falls pass as if it weren't there. Secret spells weigh what the
  Cord screen shows (more than their runes), and the Archivist's Sunfall breaks any Shield (unless
  it's parried).
- The HUD shows your own Shield's strength and time left above the spell panel.

### Imbuing
**Imbue** is a link: everything after it isn't cast but *stored*, with **3 charges**, in what the
shape before it touched. The stored part is paid for three times over when you cast, so releasing
it costs no mana. An Imbue can't store another Imbue.

Releasing isn't free of time, though, and mana can't be banked:

- **One shared cooldown.** Everything you've imbued (swords, bows, armour, sticks) shares one
  cooldown, as long as the stored spell's own would be for you (Rapid, Vow and heart perks count),
  and never under half a second. A sword, a bow and a helmet can't take turns to release a spell
  faster than your Cord could cast it, and a Vowed spell still waits out its Vow. A bow shot while
  it's cooling is just an arrow, and keeps its charge.
- **Six imbued items at a time.** Imbuing a seventh lets your oldest one's magic fade (the next time
  it would release, it's a plain item again). An item whose charges run out, that you imbue again, or
  that you place as a glyph stops counting. Glyphs are counted separately (twelve).
- **No Siphon.** A release was paid for when it was imbued, so it can't Siphon that mana back.
- **Helpful spells go to the holder.** A stored spell that only helps (a Heal, a Shield) lets go on
  whoever holds the weapon, tool or armour, not on the foe it struck or the block it broke.
- **Inside a stored spell,** an Echo repeats only what was stored, at the same target, and Combo
  never fires (every release counts as a first cast; the Cord screen warns about it).

With **Self** it goes into the item in your hand (one item: from a stack, one is taken off), or, with
both hands empty, into the block you're looking at. How an item lets it go depends on what it is:

| Item | Released |
|---|---|
| Weapons (swords, maces, tridents) | At what they strike |
| Tools (pickaxes, axes, shovels, hoes) | At each block they break, and at what they strike |
| Bows and crossbows | With the next shots, where the arrow lands (a crossbow's triple shot spends one charge, and only one of its arrows carries the spell) |
| Armour and shields | At whatever hurts you |
| Blocks | Placed, the block becomes a glyph holding it |
| Anything else (a stick, a bone, a book) | When used: at what you're looking at, or from you if the stored part starts with a shape |

Imbued items glint and say what they hold. Cords, runes and scrolls can't be imbued.

**Any block** can hold magic. Aimed at one (Touch, Bolt, any shape that hits it), placed from an
imbued block item, or looked at with empty hands, a block becomes a **glyph**: a faint copy of the
stored spell's circle on the face that was struck, sized to the block (small on a button, a torch or
a flower; full on a floor). A glyph goes off at:

- a creature that steps on it or touches it, that the spell is for (enemies for harmful spells, you
  and your allies for helpful ones);
- someone the spell is for **using** the block: opening the door, the chest, the trapdoor, pressing
  the button, pulling the lever;
- anything that **shoots** the block;
- someone else **breaking** it (it goes off at them first);
- the block being **powered** by redstone.

It re-arms no faster than its spell could be cast again (at least a second), and a creature that
stays on it is caught once: it goes off at that creature again only after it steps off and back on.
Glyphs share their maker's imbued cooldown with everything else they've imbued, so a dozen glyphs
on one redstone line go off one at a time, not all at once; picking a glyph up and putting it down
again doesn't reset its re-arm.

Glyphs are kept with the world and cast as their maker (so they sleep while their maker is away).
Break your own glyph and the block you get back still holds its spell and its charges left (if the
block drops itself); anyone else breaking it, or its charges running out, lets it fade. Each caster
keeps at most 12.

Examples: `Self · Imbue · Burst · Fire` (a sword whose next three hits burst into flame),
`Self · Imbue · Break · Widen` (a pickaxe that mines a wide hole three times), `Touch · Imbue ·
Root · Frost` (a trap that holds whoever walks over it), a door imbued with `Shock` that jolts
whoever opens it, a pressure plate that heals your friends, a button that throws lightning, a
plank you carry around and place wherever you need a trap.

### Overcasting
Short on mana? The first press says so; cast the same spell again within 2 s and you
**overcast**: your outermost working Heart Circle cracks to pay for it (mana goes to 0). With no
working circle there's nothing to crack, and the spell simply fails; nor can a circle pay for a
spell costing more than twice your full mana. A cracked circle gives
nothing (mana, regeneration, power, its perk, a passive slot) until it mends **3 minutes** later;
overcasting again cracks the next one in and resets the clock. The HUD shows ✦ and the number
cracked.

An overcast spell may also surge into **wild magic** (20%, up to 50% the further past your mana it
went): it goes off twice, turns to another element, blinks you aside, bursts into butterflies of
light, and more (see [features/parry-and-wild-magic.md](features/parry-and-wild-magic.md)). An
overchannelled spell rolls the same table at 7% a stage, and both together roll as one chance.

### Runebound
About 2-6% of zombies and skeletons of every kind (husks, drowned, zombie villagers, strays,
bogged...), witches, pillagers, vindicators and the [monsters of the wilds](#monsters-of-the-wilds) spawn **Runebound** (more where the local difficulty
is higher, none on Peaceful, and 35% of the monsters inside an Archive), carrying a spell that
suits them; one in six is an **Adept**, whose spell gains a Split or an Amplify. They have 60% more
health (Adepts 120%).
- Their **nameplate** is their spell: its rune icons and name, in its colour.
- Their Cord is written on their bodies: **rune marks** glowing in their spell's colour on the
  chest, back, arms, legs and brow (Adepts brighter), breathing slowly, with a faint haze of the
  same colour around them and specks of light drifting up. Everyone sees them, in any light.
- Before every cast their spell's circle opens for 1.1 s, held out in their right hand so their
  face stays in view, the nameplate lights up (`» … «`) and their marks flare, running hot toward
  white, with sparks streaming off them; Zone, Rain, Mine and Domain spells also mark the ground.
  Plenty of time to dodge or shoot the bolt down.
- Their spells hit players, pets, golems and whatever they're hunting, never other monsters, and
  never change blocks. Power 0.6/0.8/1.0 by difficulty (Adepts ×1.15).
- Slain by a player: a 35% chance (Adepts 60%) of a rune from their Cord, a 6% chance (Adepts
  20%) of a Torn Page, 10 extra experience (Adepts 20), and it counts toward the 6th Circle.

### Monsters of the wilds
Six magical monsters spawn on their own beside the vanilla ones, each with a tell before its signature attack and a
counter a player can learn. They join their biomes' monster lists (inside the vanilla monster cap) with weights of 7 to
14 against a zombie's 95 or a skeleton's 100, so a meeting now and then, never a crowd; all need the dark monsters need
and none spawns on Peaceful.

| Monster | Where | Signature (tell → attack) | Counter | Drop and its use | Affinities |
|---|---|---|---|---|---|
| **Bramblewalker** (30 health, armour 4) | Forest floors at night (dark forest, pale garden ×1.4) | Rears back, vines glowing (0.8 s) → a vine lash along a line at where you stood 0.25 s before it cracks, 7 blocks: 3 damage and rooted (Slowness VII, as the Root rune: 1 / 1.5 / 2 s by difficulty) | Step aside; fire (+50% from any fire), and burning makes it flee until 2 s after the flames | Living Bramble: thrown, roots what it hits for 2 s; composts | weak fire; resists earth, life |
| **Gloomstalker** (24, armour 2) | Dark forest and pale garden surface at night; below 0 anywhere | Hidden in the dark (drawn at 12% opacity, eyes always lit); stalks at 6 blocks, crouches (eyes flare, 0.7 s) → pounces (6 damage), slinks off to the darkest spot near; a miss leaves it sprawled 1.5 s | Light ≥ 8 where it stands, a player within 3 blocks, Glowing, or a fire/storm/arcane/life spell (or Light, Reveal) shows it: 6 s for a light spell, 2 s for any wound | Shadow Pelt: Awkward Potion → Invisibility | weak arcane; resists void |
| **Thunderwing Harpy** (22, armour 1) | Peaks and windswept hills, y ≥ 90, at night or in a storm | Circles 8 up, 7 out; shrieks, wings wide (0.8 s) → dives in a straight line at where you are and are heading (6 damage); a miss into the ground stuns it 2.5 s. In a thunderstorm: a ring under you (1.3 s) → lightning there (5 magic damage within 1.8 blocks, through the spell defences) | Dodge, punish the stun; any earth spell grounds it 3 s; shoot it while it circles | Storm Feather: use for a gust up (and 2 s of slow falling), 1 s cooldown | weak earth, frost; resists storm, wind |
| **Geode Crawler** (26, armour 6) | Caves below 50; away from amethyst only a quarter of its spawns go ahead | Struck, curls for 2 s (a fifth of any blow gets through), rattles (0.6 s) → rolls at its target (6 damage); a wall dazes it 2 s | A mace, a pickaxe, a blast, lightning or a storm spell: ×1.25 and cracks it open, dazed | Geode Grit (the earth reagent) and amethyst | weak storm; resists earth, arcane |
| **Bog Witch-Frog** (32, armour 2) | Swamps and mangrove swamps at night | Throat swells (0.9 s) → lobs a bubble (12 to 34 ticks of flight, 4 to 16 blocks): 3 magic damage and Poison 5 s within 1.7 blocks, and a poisoned mist. Mouth gapes (0.5 s) → tongue yanks a player 3 to 6 blocks off (2 damage). Swallows small, wild, nameless creatures within 6 blocks (heals 6; next bubble ×1.5 wide, ×1.5 harm, Poison II) | Pop the bubble in the air (any hit); a raised shield turns the tongue | Bog Gland: Awkward → Water Breathing; Thick → Leaping | weak frost; resists life |
| **Mana Ooze** (6 / 14 / 24 / 36 by size 1 to 4) | Below 0 anywhere; below 40 under a ley line | Drinks every spell's harm except fire's: 8 × size fills it; full, it grows a size (healed); full at 4, bursts into two size 2 (3 magic damage within 3 blocks). Its touch drains 4 mana from a Cord wearer | Blades, arrows, fire (+50%, never drunk); it never splits on death | Mana Gel: eaten, 15 mana | weak fire |

- **Monster magic is spell damage.** A harpy's lightning, a frog's bubble and an ooze's burst are magic from the monster
  through `SpellDefence` (armour at its spell rate, Warding, Warded, the spellguard); claws, bites, dives, rolls and the
  vine's thorns are ordinary blows. The game scales both by difficulty as it does any monster's harm.
- **Runebound.** Each rolls Runebound like a zombie does (the same chance and Adept odds), with spells that suit it:
  Wave or Ring · Root, Bolt · Venom (Bramblewalker); Bolt · Blind or Wither, Touch · Harm (Gloomstalker); Bolt · Shock or
  Jolt, Crescent · Windcut (harpy); Bolt · Pelt or Shackle, Ring · Tremor (crawler); Zone or Orb · Venom, Bolt · Bleed
  (frog); Orb or Bolt · Harm, Bolt · Silence (ooze).
- **Server settings** (`monsters`): `enabled`, `spawn_rate` (0 to 4, scales their weights when a world loads) and a switch
  for each, read by their spawn rules at once. Spawn eggs, spawners and commands aren't stopped by the switches.

### Collisions, clashes and Unison
- **Spell collision:** a bolt that meets an enemy caster's bolt in the air bursts with it. Different
  elements burst harder (5 damage around the point); a reacting pair (fire/frost, storm/frost,
  fire/wind, void/arcane, fire/storm, earth/frost, life/void, blood/wind, fire/time) sets off a small
  named reaction (8 damage, 4 blocks).
- **Domain clash:** two casters' Domains can't overlap. The two shells push against each other for
  1.5 s, then the weaker one shatters like glass. Strength is power × shape power (Focus, Vow) ×
  (1 + 0.1 per working circle; a monster counts as 4) × √(radius / 9); the incumbent holds a tie.
- **Unison:** hit a foe with a different element than another player did within the last second:
  that hit lands **50% harder**, both colours burst, and both of you see *Unison!*

### Ley lines and the Wellstone
Ley lines are thin, winding veins of world mana in the Overworld, worked out from the world seed
(the server sends clients a one-way hash of it, never the seed itself). Players wearing a Cord see
them as ribbons of pale violet light flowing along the ground, following each line's bends (under
trees, along the forest floor), with now and then a mote lifting off. **On a ley line**, mana
regenerates twice as fast (+100%) and Heart Circles form twice as quickly. The **Wellstone**
(crafted from amethyst blocks, polished deepslate, deepslate tiles and a Mana Crystal) wakes when
set on a ley line: circles turning on the ground around it, a leaning ring of light hanging over
it and slowly swinging round with beads of light running along it, and +50% regeneration for
everyone within 12 blocks.

Lines of one noise field never cross, so the lines run in **two weaves** (two seeds, the second a
little sparser and broader). Where a line of each meets is a **ley crossing**, a place of power a few
hundred blocks from the next: every spell is 10% stronger and 10% cheaper there. See
[A world that remembers magic](#a-world-that-remembers-magic).

### Spell Scrolls and the Training Dummy
- **Spell Scroll:** inscribe any of your spells from the Cord screen (paper, an ink sac and twice the
  spell's mana). Anyone can cast it once, with or without a Cord or its runes, at base power.
- **Training Dummy:** crafted from wool, hay, sticks and a slab. It never dies; every hit floats up
  as a number coloured by damage type, and its name shows DPS over the last 5 s and the burst's
  total. Sneak and punch it to pick it up.

### Spell mastery
Your spells grow with you. A spell is its exact runes in order as they fire (Knots untied), and each player keeps a
record for each spell they've used to some purpose, the 48 most recently used (one threaded on the Cord is never
forgotten). Editing a spell makes it a new one; the old record waits for it to come back.

| Rank | Experience | Brings |
|---|---|---|
| **Kindled** | 0 | the owner's sigil at the heart of the circle |
| **Practised** | 100 | the first trait; a fine ring outside the frame |
| **Adept** | 350 | the second trait; a deeper colour, a brighter sigil; the name spoken when cast; inscribable |
| **Master** | 1,000 | the third trait; a second, ticked ring |
| **Mythic** | 3,000 | the fourth trait; a slow shimmer round the rings |

Tuned so a spell used as a main attack reaches Practised in under half an hour of real play, Master in about three
to four hours and Mythic in about ten: two meaningful casts a minute, each striking one or two monsters for a third of
their health, is about 280 experience an hour.

**Experience comes from outcomes, never from pressing the key:**
- a foe struck: 1 for the strike, up to 2 more for the share of its full health the hit took, 1 more for the kill;
  a Runebound is worth 1.5×, a boss 2×, a player (PvP on) 0.5×; animals and anything not hunting you nothing;
- a heal: 0.25 per point of health truly restored to someone missing it, at most 2 (yourself only in danger, at half);
- an ally helped who needed it (hurt, or in a fight): 1 per ally per cast; yourself in danger: 0.6;
- a spell that moves you or works the world: 0.35 per cast that does its job (double in danger).

**What surrounds the cast:** low health (under 35%) 1.5×, four foes within 16 blocks 1.3× (eight 1.5×), a boss within
48 blocks 1.5×, inside a dungeon (the mod's Archives and dungeons, ancient cities, trial chambers, strongholds,
fortresses, bastions, end cities, mansions, monuments) 1.25×, together at most 3×. A kind of foe the spell hasn't
struck among the last 12 it remembers: 1.25×. **Repetition:** each recent cast at the same kind of foe in the same
16-block cube takes 6% off the next (down to a tenth of its worth), and a place is half forgotten every ten minutes;
boss fights are exempt. One cast (links, pulses and echoes together) earns at most 20. **Practice:** training dummies
and anything in the practice arena teach at half rate, at most 60 in all (just over half of Practised). Scrolls,
imbued releases, glyphs and passives teach nothing, and creative players earn nothing. The server's
`mastery.xp_multiplier` scales it all.

**Traits.** At ranks II to V the caster chooses one of three offered traits; the offer is drawn by weight from a
catalogue of 53, the same every time for the same spell, rank and re-roll:
- filtered by **makeup**: its elements, its shape categories, and whether it harms, only helps, moves its caster or
  works the world;
- weighted by **use**: every counted cast (one that earned real experience) counts the circumstances it was cast in
  (rain, thunder, night, day, underground, deep, water, the Nether, the End, low health, beside allies, against the
  undead, spiders and kin or bosses, in a dungeon, against a crowd, cold, heat, in the air). A trait that asks for a
  circumstance is only offered once at least a seventh of the spell's counted casts (and six at least) were in it, and
  is likelier the more there were. Element traits get a little extra weight; they're the flavourful ones.

What they do, by kind: a price factor (10% off; 15% off in one place: the Nether, the End, water or rain,
underground; 15% off for a spell that works the world), a cooldown factor (10%, or 15% for a movement spell), 15%
faster charging, 20% more reach for bolts, beams and aimed shapes, a damage bonus (6% always, or 10-15% when a
condition holds: a weak or unhurt target, the undead, arthropods, bosses, the Nether or End, a crowd, low health,
night, day, underground, rain), a leap to one more foe (15% chance, 40% strength), a drink of 5% of the damage (at
most 4 health a cast), 4 mana a kill (at most 3 kills a cast), a small mark on a foe struck (slowed, glowing, alight,
pushed, Soaked, Shadowed, Bleeding), a short buff on allies helped (regeneration, speed, resistance, night vision,
or poison and wither cleansed), 3 mana for helping an ally (twice a cast), slow falling after a movement spell, fire at
full strength on the wet, a third of the mana back when cast in danger, looks and sounds (a bell, starlight, a deeper
hue, embers, frost, petals) and a residue left where it lands (the residue system's to fill in).

**Limits:** damage traits multiply into the hit's bonuses in `Effects.hurt`, together at most ×1.2, and against a
player they give half their bonus, all under the spell-defence cap (×2.5); price and cooldown traits together go no
lower than 85%; a leap never leaps again. **Changing your mind:** each rank allows one change for 3 experience levels:
re-roll the waiting offer (excluding what was offered), or unbind a chosen trait (a fresh offer excluding it).

**Sigils.** A deterministic glyph from the spell's key and its owner's UUID: strokes between the points of two rings
(5, 6 or 8 points) and the centre, mirrored left to right, with a dot or two and sometimes a small ring. It replaces the
first rune's seal at the circle's heart. A spell taught by a scroll keeps its teacher's sigil.

**Spoken names.** A custom-named spell at Adept or higher sends its name to every player within 32 blocks (the caster
too) when cast: a brief title over the caster, or a subtitle-like line when they're out of view. Each client can hide
them (Spell titles, in the magic visual settings).

**Inscription.** An Adept (or higher) spell inscribed onto a Spell Scroll carries its earned traits (never borrowed
ones), its sigil and its rank. Read aloud it casts once with them (and teaches nobody). Studied (used while sneaking)
by someone who knows its runes and whose Cord holds them, it threads the spell into an empty row, names it, and starts
the reader's record at Kindled with the traits borrowed: they work at once, and at each rank the borrowed trait is
offered as a fourth card to keep. A reader with any record of the spell can't study it again; no experience travels.
Chosen so a friend gets the feel of your spell straight away but grows it themselves, and nothing can be farmed or
stacked through scrolls.

### How magic looks
- **Your own view stays clear.** Particles that would sit right in front of a player's eyes are
  left out for that player (everyone else still sees them). The circle under every cast is the
  spell's own circle, flat on the ground, and nothing in a cast flies outward across your screen.
- **Flashes are glows.** Impacts and bursts flash with a small glow of their own (a sigil that
  always faces you) instead of vanilla's firework flash, which up close is a pale square.
- **Stone and ice.** Tectonic Rise raises dripstone spires and Glacial Lance closes its targets in
  ice. Both are block displays that grow and shrink away: they never place real blocks, and any
  left over after a restart are removed.
- **Falling stars** (Starfall, Starlight Cascade) mark where each will land.

### The Archive and the Archivist
A buried library (spacing 44 chunks, in most land biomes), found by a ring of broken pillars
around a stairway going down. `/place structure wildercord:archive` builds one.

1. **The Hall of Shelves:** bookshelves, reading tables, Runebound guards and a chest (runes of
   Tier II and III, Torn Pages, a Mana Crystal).
2. **A Rune Seal door (Frost and Storm):** seals are unbreakable. Strike the door with a spell of
   one of its elements and every seal of that element lights; light every element within 10 s and
   the door dissolves.
3. **The Hall of Braziers:** unlit campfires that fire spells light; a Runebound Adept.
4. **A second door (Fire and Wind)**, then the **Arena**: a domed circle with a magic circle inlaid
   in the floor. The **Archivist** rises from its lectern when you come within 12 blocks.
5. **The Vault**, behind a door of Arcane and Life off the arena: two chests (a Tier IV rune each,
   Tier III runes, Mana Crystals, Torn Pages) and a Wellstone.

The Archive is old and not quite asleep: faint shafts of light fall from its lamps with dust
drifting through them, lit braziers flicker and throw up embers (cold ones only glint), the circle
inlaid in the arena floor glows softly (its rings and hexagram; brighter while the Archivist is
abroad), and now and then a page turns among the shelves or something whispers in the dark.

The **Archivist** (400 health, armour 8, immune to fire) is a tall hooded figure in an indigo robe
trimmed with gold, hovering just above the floor, its face an empty dark with two pale eyes. An
open tome floats before it, a page turning now and then, and three loose pages circle it. Every
spell it's about to cast is written on its boss bar (*The Archivist · casting Shock Rain*), with a
telegraph circle: it raises its arms, the tome lifts and riffles and its writing blazes. It blinks
away when you get close and never strays from the arena. At two thirds and one third health it
**rewrites its Cord**: pages tear loose from the tome in a burst, it throws its arms wide with the
book open over its head and the pages whirling far out, untouchable for a moment, summoning two
Runebound (Adepts the second time), then a new phase of spells. Like the dimension bosses, no blow
carries it past the start of its next phase. Slain, it sinks into its robe.

| Phase | Its spells |
|---|---|
| 1 | Splitting Frost Bolt, Shock Rain, Arcane Orb |
| 2 | Volleying Fire Crescent, Wide Venom Zone, Greater Arcane Blitz, Splitting Chill Mine |
| 3 | Arcane-Chill Domain (clash it with yours), Dismantle Barrage, Sonic Boom Beam, Frost-Shock Bolt, and sometimes **Sunfall** (everyone who sees it learns the riddle) |

It drops a Tier IV rune its killer doesn't know yet, two Torn Pages, three Mana Crystals and 200
experience, and counts as a boss for the 7th Circle and as the 8th Circle's feat for everyone
within 64 blocks.

## A world that remembers magic

You're casting your own magic, and the world answers. Big magic leaves lasting marks of its element
where it lands, the marks give reagents that feed back into fusing runes, and some places and hours
make every spell a little stronger. Nothing here is a lot of power: it's texture, something to learn
and seek out, never a must-have.

### Residues
**When.** A spell whose list price (its mana before discounts) reaches **30** leaves a residue of its
element once a cast, at its first landing; an **overcast** always does (strength at least 1, plus
0.5); a **boss's** spell (the Riftcaller, the Archivist, a dungeon's master) counts at least 1.5; and
an **element reaction** has a **35%** chance of leaving a strength-1 residue. Strength is price ÷ 30,
read up to 3. One caster leaves at most one residue every **5 s** (a boss every 10 s). A rune with no
element leaves nothing.

**How much.** Strength 1 leaves 1 block, and each half beyond adds one, **5 at most** (1.5 → 2,
2 → 3, 3 → 5), scattered within 2 to 3 blocks of the impact (the impact's own column first). Each
lasts its kind's lifetime × (0.75 + 0.25 × strength): up to **half again** for the strongest, never
under a minute.

| Residue | Element | Kind | Lifetime | Behaviour | Reagent |
|---|---|---|---|---|---|
| Smouldering Ash | Fire | lies on the ground | 12,000 ticks (half a day) | warm: thaws whatever stands in it; light 6 | Cinder Ash |
| Everfrost | Frost | takes the ground's place | 24,000 (a day) | friction 0.98 (slick as ice); never melts | Everfrost Shard |
| Fulgurite | Storm | lies on the ground | 36,000 | brushing it gives Speed I for 3 s; light 5 | Fulgurite Shard |
| Lingering Eddy | Wind | in the air over the ground | 6,000 (5 min) | an updraft that lifts and cancels falls; bottled, not broken | Bottled Gale |
| Riven Stone | Earth | takes the ground's place | 24,000 | dust and a rumble underfoot; light 3 | Geode Grit |
| Wildbloom | Life | lies on the ground | 24,000 | bees' flower; seeds a seedling within 2 blocks now and then (2 generations, never outliving its parent, only while its caster is online and allowed) | Wildbloom Petal |
| Void Scar | Void | takes the ground's place | 6,000 (5 min) | draws items and orbs in from 5 blocks; animals path round it and are nudged away; narrows through 4 stages | Hollow Dust |
| Star Glyph | Arcane | lies on the ground | 12,000 | a player on it makes invisible creatures within 8 blocks glow; light 7 | Star Dust |
| Stilled Sand | Time | lies on the ground | 12,000 | Slowness I on whatever walks it; items on it never despawn | Hourglass Sand |
| Bloodmoss | Blood | lies on the ground | 12,000 | grows nether wart beside it; animals path round it | Sanguine Bead |

**Rules.** A residue never replaces anything a player built or placed: one that takes the ground's
place takes only natural ground (the `wildercord:residue_ground` block tag: dirt, grass, sand,
gravel, clay, overworld and nether stone, snow, nylium, soul soil, end stone...) with open air above,
and one that lies on the ground takes only open air (or something as easily replaced that grew there)
over sturdy natural ground. Never a block with contents, never a spell's passing block or another
residue, never in a dungeon's ward. Permission is a spell's own (`Casters.mayEdit`: building rights,
spawn protection, claims, `spells_edit_blocks`); a boss's residue follows `spells_edit_blocks` and the
mob-griefing rule. Caps: **12 a chunk**, **6 within 4 blocks**, **1,024 a dimension**, **64 a caster**
(new ones simply aren't left). A residue that took the ground's place gives it back when it fades or is
harvested. Their records are saved and fade on a schedule (never a scan of every residue); one in
unloaded ground waits for its chunk to load. Other spells and pistons leave residues alone. The server
can switch residues off and change the threshold, lifetimes and caps (`residues` settings).

### Reagents
Harvesting a residue gives its reagent (one or two from the ground ones; an eddy only by bottling). Each
has a small use of its own, kept modest:

| Reagent | Use |
|---|---|
| Cinder Ash | Furnace fuel: 800 ticks (4 items) at 1.25× speed |
| Everfrost Shard | Freezes still water into ice, cools still lava into obsidian |
| Fulgurite Shard | Scrapes a stage of weathering off copper |
| Bottled Gale | Drink: Slow Falling for 30 s (the bottle comes back) |
| Geode Grit | Prospect from where you stand: ore within 8 blocks glows for 10 s (2 s cooldown) |
| Wildbloom Petal | Bone meal |
| Hollow Dust | Draws every item and orb within 10 blocks to you (1 s cooldown) |
| Star Dust | A mote of starlight (a light block, level 12) beside the block used on, for 5 minutes, saved as a temporary block |
| Hourglass Sand | A smelting furnace jumps up to 10 s ahead (never finishing it); a young animal ages 2 minutes |
| Sanguine Bead | Grows nether wart a stage |

Every reagent is in the `wildercord:reagents` item tag.

### Reagents at the Fusion Altar
A reagent in the free third rune socket while two effects **fuse** (shard) or **weave** (block) changes
the result one way, and is used up with it. One that would change nothing is refused before anything
is spent.

| Reagent | Effect | Rule |
|---|---|---|
| Cinder Ash | Tempered | the result keeps the higher of the two ranks put in, not the lower |
| Everfrost Shard | Stilled | XP levels halved, rounded up (a weave of eight: 21 → 11) |
| Fulgurite Shard | Charged | a rank I result comes out rank II |
| Bottled Gale | Unbound | a signature pair makes its elements' fusion instead |
| Geode Grit | Grounded | the amethyst stays on the altar |
| Wildbloom Petal | Bountiful | two of the result |
| Hollow Dust | Hollowed | the lower-tier rune stays on the altar (extending a weave, the lone rune) |
| Star Dust | Exalted | one rank higher (up to III), for 3 more XP levels |
| Hourglass Sand | Familiar | a named fusion already in your Grimoire costs no XP |
| Sanguine Bead | Bloodbound | up to 6 XP levels paid as health instead (1 health a level), never below 2 health |

The ranked ones are rare enough (a residue is a strong spell's leftover, five seconds apart at best)
that a rank or two from them doesn't undercut ranking up by copies; the cost ones matter most to big
weaves.

### Places and times of power
Built on the elemental climate (`ClimateRules`), so it goes through the same damage path
(`Affinities.multiplier` in `Effects.hurt`) and the same ×2.5 bonus cap against players, and the HUD
and Grimoire read the same table.

| Condition | When | Shift |
|---|---|---|
| Ley crossing | where both weaves of ley lines run (each at least 0.3 strong) | every element +10%, every spell 10% cheaper |
| Full moon | a clear night under the open sky, moon phase 0 | arcane +15%, void +15% |
| New moon | a clear night under the open sky, moon phase 4 | blood +15%, void +10% |
| Noon | 4,800 to 7,200 on the day clock, open sky, not raining | fire +15% |
| Dawn | 22,800 to 1,200, open sky | time +15% |
| Dusk | 11,800 to 13,400, open sky | time +15% |
| Rain (existing) | rain falling on you | frost +10% (fire -10% as before) |
| Thunderstorm, the Nether, the End (existing) | | storm +25%, fire +20% / frost -25%, void +20% |

The sky's shifts (the moon, the hours, rain's frost) are *celestial*: the server can switch them off or
scale them, and a ley crossing's bonus too (`places_of_power` settings); `elemental_climate` off turns
all of it off. Stepping onto a crossing tells you so (and the first time earns **Crossroads**), and it
shimmers on the client: flat rings and lines of light round its heart, a faint column and rising motes,
seen from 56 blocks. For 8 seconds after a new condition comes into force, the HUD lists every
condition holding and what it shifts ("Full moon: Arcane +15%, Void +15%"); the Grimoire keeps them.

### Hooks for other features
- `Residues.leave(level, element, at, strength, by)`: leave a residue (a mastery trait, an event), every
  rule applying.
- `PowerPlaces.isPlaceOfPower(level, pos)`, `PowerPlaces.at(level, pos)` and `PowerPlaces.of(player)`:
  whether a spot is a ley crossing, and what's favoured there now and why (a resonance that wakes only
  at a place of power, a rite that wants a full moon).
- `#wildercord:reagents`, `Reagents.kindOf(stack)`: whether an item is a reagent, and its element.

## Magical wildlife

The world should feel enchanted between the fights: things that glow in the woods at night, a shape against the
sky over the peaks, tracks in the frost. Six creatures, none of them hostile, each tied to an element and to its
land, each leaving something behind that's worth a little (a brew, a recipe, a stand-in for a rare ingredient) and
none of it power. They spawn on their own, in their biomes, and the rare ones stay rare.

| Creature | Where | What it does | Leaves | Its use |
|---|---|---|---|---|
| **Glimmerwing** | Forests, flower forests, birch woods, dark forests, meadows, sunflower plains, cherry groves; at night | Soft-glowing moths in swarms of 3 to 5 that keep together, circle lamps and anyone who cast a spell in the last 8 seconds, shed a faint glittering dust, and fade one by one at daybreak. A swarm shares one colouring from where it rose: moonlit blue, rose (flower forests, cherry groves) or amber (meadows) | Glimmer Dust (killed) | Brews Night Vision from an Awkward Potion; with an ink sac, a glow ink sac |
| **Lumen Stag** | Old-growth birch, pine and spruce taiga, taiga, cherry groves; rare, always alone | Crystal antlers that burn brighter as the moon fills (full at a full moon, a third at a new moon, faint by day). It bolts from anyone who walks up or runs at it (sneaking or not) and watches a calm, sneaking player; one who stays within 3.5 blocks for 3 seconds is trusted and it bows and sheds an antler at their feet. Once a day: for whoever it trusts first, or on its own (1 in 40 every 10 seconds) while a player is within 48 blocks | Lumen Antler (shed, never dropped) | Stands in for the diamond in a Mana Crystal |
| **Mossback Tortoise** | Swamps, mangrove swamps, jungles, sparse and bamboo jungles; 1 or 2 | A garden on its shell (blue orchids and a mushroom in swamp moss; a propagule, a lily pad and a red mushroom in the mangroves; ferns and a dandelion in the jungle) that grows over to match after 10 minutes in another of those lands. Struck, it hides in its shell for 5 seconds and takes 40%. Follows melon; two fed melon raise a baby (bare shell, big head) | A Mossback Scute every 10 to 20 minutes, one when a baby grows up, 0 to 1 when killed | Brews the Turtle Master from an Awkward Potion; mends a turtle shell |
| **Cinderfox** | Deserts and the three badlands; 1 to 3 | Wild, it keeps its distance (unless you sneak or hold rabbit) and hunts rabbits and chickens. A rabbit, raw or cooked, tames it one time in three; tame, it follows, sits and fights for you, and its bite sets a creature weak to fire alight for 4 seconds and hurts it half again. Breeds when tame (rabbit or chicken). Its ember tail glows always and sparks at night. Fire-immune | An Ember Tuft from a brush, once a day (tame only); 0 to 1 when killed | Brews Fire Resistance from an Awkward Potion; burns in a furnace for eight items |
| **Skyray** | Windswept hills, gravelly hills and forest, meadows, jagged and stony peaks, snowy slopes; rare, at most two in a sky | A manta three and a half blocks across. It climbs from the summit where it spawned to 26-44 blocks over the ground and circles a point (14-26 blocks round, a lap every half minute or so) that drifts slowly across the land; leans into its turns, glides on a ripple and strokes now and then. Its stars glow at night. Struck, it climbs and flies off. Stays while a player is within 96 blocks | A Skyray Membrane every 7.5 to 15 minutes while a player is within 64 blocks; 1 or 2 when killed | Brews Slow Falling from an Awkward Potion; mends an elytra as a phantom's membrane does |
| **Rimehare** | Snowy plains and taiga, ice spikes, snowy slopes, groves; 2 or 3 | Goes in bounds, leaving a pair of frost prints at every landing that fade in about 3 seconds. Bolts from anyone within 10 blocks, faster than a sprint, unless they hold out sweet berries and don't run; then it comes to them, but darts off for a while if they move or turn once it's within 6 blocks (vanilla's skittish tempting). Sits up on alert. Two fed sweet berries raise a leveret | Rime Fur, 0 to 2 when killed | Weaves Rimebound armour in place of packed ice; four make a piece of leather |

**Killing a lumen stag** leaves nothing and brings Bad Luck on whoever did it for five minutes ("The forest falls
silent around you"); every stag within 48 blocks is frightened for five minutes and trusts nobody.

**Spawning.** Each creature joins its biomes' spawn lists through biome modifications, in vanilla's own pools (the two
fliers in the ambient pool with the bats, the rest with the animals), so the mob caps hold. On top of its weight, each
one's spawn rule decides every natural try: its ground (block tags under `wildercord:spawns_on/`), the light, its
rarity (the share of good tries let through: a stag a third, a skyray one in 800, a glimmerwing a quarter, the rest
every one) and how many of its kind are already near (no other stag within 80 blocks; at most two skyrays within 112;
9 glimmerwings within 40; 4 to 6 of the others within 48). Glimmerwings only rise at night, in open air within 6
blocks of the ground; skyrays only from an open summit. Spawn eggs, `/summon` and spawners aren't held to any of it.

**The server's say** (the `creatures` section of `wildercord.json`): `wildlife` switches all their natural spawns,
`wildlife_spawn_multiplier` scales them (below 1 by letting fewer tries through at once; above 1 by raising their
spawn weights too, from the next world load; the crowding limits hold either way), and each creature has a switch of
its own.

**Affinities.** Glimmerwings are weak to fire and wind and resist arcane; a stag is weak to void and resists arcane; a
tortoise resists earth and wind and is weak to frost; a cinderfox resists fire and is weak to frost; a skyray resists
wind and is weak to storm; a rimehare resists frost and is weak to fire.

**The field guide.** A creature of the guide seen up close (within 14 blocks, in sight) goes into the Grimoire the
first time (40 mana toward the next circle, a toast with its spawn egg). The Grimoire lists every creature met with its
short entry, and the rest as a hint of where to look. It's one list, `spell.FieldGuide`, that any creature can join.
## Aura: the swordsman's path

The Cord is the mage's path. **Aura** is the melee counterpart: mana drawn into the body and out along a blade. Both paths
are open to everyone, they combine rather than compete (a mage with a blade, a swordsman with a Cord), and time invested
is the only limit. The pure rules and every number are `aura.AuraRules`; the server's `aura` settings change the main ones.

### The resource
- **Aura** is a small pool that never decays, held by anyone who has learned a **breathing method**. Its capacity grows by
  stage: Glow 20, Flow 40, Edge 70, Form 110, Sovereign 160.
- **It fills from real blows**: a full swing (vanilla's attack strength at 90% or more) of an aura weapon on a real foe
  gives 0.4 aura per point of damage it took, at most 4 a blow. A half swing gives nothing. Repetition fades it (the same
  kind of foe in the same 16-block cube, as spell mastery remembers places; a blow counts a quarter of a cast toward
  it), and a training dummy or the practice arena gives a quarter.
- **And from the breathing stance**: sneak and stand still with an aura weapon in the main hand (not charging a spell).
  After a second the breath takes hold and draws in 1.5 aura a second. A breath comes every two seconds: the HUD's beat
  ring closes on the aura bar's stage marks, and letting sneak up and pressing it again as it closes (a breath on the
  beat, at most half a second up) draws in 3 more at once. Moving, standing up for good, or being hurt breaks the stance.
  Sneaking still is also meditation for mana: you breathe and meditate at once.
- **Spending past empty is backlash**: a technique short of its price spends everything there (a slash goes out
  weakened) and leaves you slowed and weakened for 3 seconds. It never damages.
- **Aura weapons** are the item tag `#wildercord:aura_weapons`: swords, axes, spears, the trident and the mace. Servers and
  add-ons extend it.

### Breathing methods
Each element has a method, learned from its **Breathing Manual** (one item, its method a component; use it to learn).
The method gives the aura its colour (the element's own, burning toward its highlight as the stage climbs) and its
element: a coated blow and the slash meet a creature's affinity and the elemental climate as a spell of that element
would, and set off the reactions waiting on its marks (a frozen foe shatters under an Ember blade, a soaked one conducts
a Thunder blade). Each also has one passive that grows with the stage:

| Method | Element | Passive (by stage) |
|---|---|---|
| Ember Breath | fire | from Flow a coated blow may set its foe alight (15%, 1 s); from Edge it always does (2 s, +1 s a stage) |
| Rime Breath | frost | a coated blow slows its foe (1 s at Glow, +0.5 s a stage) |
| Thunder Breath | storm | a coated blow may throw a spark to the nearest other foe within 5 blocks (8%, +4% a stage) for 30% of the blow |
| Gale Breath | wind | a little speed while in a fight (struck or striking in the last 4 s): +4% a stage |
| Stone Breath | earth | knockback resistance with an aura weapon in hand: +12% a stage |
| Verdant Breath | life | a coated blow mends 0.4 health (+0.15 a stage), at most twice a second |
| Hollow Breath | void | foes within 3.5 blocks of the one struck are drawn toward it (bosses never) |
| Starlit Breath | arcane | aura comes faster: +15% a stage, blows and the stance alike |
| Hourglass Breath | time | a little attack speed with an aura weapon in hand: +2.5% a stage |
| Crimson Breath | blood | a coated blow drinks 10% of what it took (+5% a stage, at most 2 health) for 1 more aura |

**Switching** to another method keeps the stage but costs the road to the next breakthrough: experience goes back to the
start of the stage, and the aura held empties. A player already breathing one way reads the manual twice to be sure.

**Where manuals are found** (`aura.MethodSources`; each place favours its methods three to one): the Archive's library
(10%) and vault (30%); each expedition's vault (25%, its element's method and a neighbour's); the trial chambers' rare
vault (10%) and ominous vault (20%); stronghold libraries (15%); ancient cities (12%). Master weaponsmiths (Ember,
Thunder, Stone, Gale, Crimson) and clerics (Verdant, Starlit, Hourglass, Hollow, Rime) sell one for emeralds and a book.
The rune loot multiplier scales the chests' chances.

### Stages
Aura climbs in leaps. Each stage keeps everything below it.

| Stage | Capacity | Experience | What it brings |
|---|---|---|---|
| **Glow** | 20 | (with the method) | **Aura Coat**: with aura held, every blow of an aura weapon is coated: +10% and the method's element, for 0.5 aura. **Aura Sense**: each breath of the stance outlines the hostile creatures within 16 blocks, for you alone |
| **Flow** | 40 | 150 | **Flowing Cut**: every aura weapon sweeps (vanilla sweeps only with a sword), reaching 1.6 blocks round the struck foe and 3.75 from you (vanilla: 1 and 3), carrying 40% more of the blow. **Aura Guard**: sneak and press the Aura key |
| **Edge** | 70 | 600 | **Crystal Edge**: a blade of solid aura: +1 block of reach, and a quarter of each coated blow goes through armour. **Aura Slash**: tap the Aura key |
| **Form** | 110 | 1,800 | **Aura Step**: double-tap the Aura key. **Aura Armour** and **Intent**, always on. Aura Sense reaches 24 blocks and pulses in a fight |
| **Sovereign** | 160 | 4,500 | **Dominion**: hold the Aura key |

**Aura Guard** (Flow): it holds while sneak is held, two seconds at most, then rests a second. It halves blows and
projectiles from in front, paying 0.6 aura a point it takes off; at nothing it breaks, with backlash. Raised within 7
ticks of a blow (the parry's own moment, `spell.Parry`), it's a **perfect guard**: the blow is turned aside whole and the
attacker staggered (thrown back, Slowness III and Weakness for 2 s; a boss is only slowed), a projectile flies back at
whoever loosed it, now yours, a quarter faster, and a spell is parried as a Shield raised at the last moment parries one
(negated, and answered with a lance of light at its caster). The first perfect guard goes into the Grimoire.

**Aura Slash** (Edge): a crescent of aura flies ahead at chest height, 14 blocks at 1.6 a tick, cutting each foe in its
3-block path once (six at most) for the weapon's damage × 1.2, in the method's element. It costs 12 aura and recharges in
2 seconds; spent past empty it goes out weakened, with backlash. It's the Crescent shape's flight and crescents of
shaped light, without a spell.

**Aura Step** (Form, a double tap): aura carries you 6 blocks in 3 ticks, the way you're moving (the client's movement keys,
`getLastClientMoveIntent`), or ahead and level when you stand still. The path is swept with your whole body in 0.2-block
pieces: it climbs 0.6 (a slab, a stair) as walking would, and stops before anything solid, a dungeon ward's edge (never into
or out of a warded room), the world border, lava or fire. For its first 6 ticks no harm reaches you (anything that bypasses
invulnerability aside). Afterimages of you in your colour are left along the way, for everyone who sees it. 12 aura, every 2
seconds; spent past empty it never forms, with backlash. With a double-tap technique, a lone tap of the Aura key waits out the
double tap's 8 ticks before it goes as a tap, so a step never looses a slash first.

**Aura Armour** (Form, always on): while you hold 20 aura or more, it takes a quarter of what reaches you after the guard
(blows, projectiles, spells, fire, blasts; never a fall, drowning, starving, suffocation or the void), paying 0.5 aura a
point, and only as far as the aura above 20 pays: it fades rather than breaks, so it never brings backlash. It's paid only
when the blow really lands. A faint shell of aura is drawn round the body (the player model again, a little larger, its
light gathered at each face's edges), flaring where a blow strikes.

**Intent** (Form, always on, with an aura weapon in hand and aura to coat a blow): once a second, every hostile creature
within 8 blocks whose whole health is below yours (or, for anything carrying aura of its own, whose stage is below yours) is
slowed for a second and a half (Slowness I) and, one time in four, falters (its path dropped where it stands). Bosses never
feel it. Another player it presses on (PvP on, `intent_pvp`) gets a dark vignette and their speed taken down 5%
(`intent_pvp_slow`, a modifier that lets go when Intent stops reaching them, never the Slowness effect).

**Dominion** (Sovereign, a hold): a circle 3 blocks out from where you raised it (6 across), for 8 seconds. Foes inside
(those you could harm) are slowed (creatures Slowness II, bosses and players Slowness I) and every harm they deal lands 30%
weaker (against a player inside, the weakening is times the aura PvP scale: 18%). Each of your blows on a foe inside chains
once (a sweep's blows once between them) to the nearest other foe inside, for half of what it took as projected aura. While
you stand in it aura comes twice as fast, plus 2 a second. 40 aura, and a 90-second rest that a relog doesn't reset (the
timers are saved in game time). It's raised with a great circle of the aura's colour on the ground, a column of light, a
camera shake and `aura_dominion`; its rim breathes light while it holds and breaks up into motes as it ends.

### Sword strings
Ordinary swings are a language. A **string** is a short run of swings (one to six), each of a kind (a **token**), each within
its window of the one before; played, it sets off an **art**. Nothing new to press: the swordsman swings as always, and
chooses how.

| Token | Written | A swing that... | Weight |
|---|---|---|---|
| a swing | `swing` | any swing at all | 0 |
| a full swing | `full` | the blade had recovered (attack strength 0.9 or more, aura's own "meaningful blow") | 1 |
| a low swing | `low` | struck crouching | 2 |
| a leaping swing | `leap` | struck in the air (not swimming, climbing, riding, flying or gliding) | 2 |
| a running swing | `run` | struck sprinting | 2 |
| a counter | `counter` | the first swing within 16 ticks of a perfect Aura Guard | 4 |
| a step cut | `step` | the first swing within 14 ticks of an Aura Step | 4 |

A swing can be several kinds at once (a full low swing straight after a perfect guard is a full swing, a low swing and a
counter), and a token asks for one kind: any swing that has it fits, and `swing` fits every swing.

**Windows.** Each swing must come within the window of the moment the blade is ready again: half a second
(`aura.string_window_seconds`, 0.1 to 2) plus the weapon's recovery (the ticks to a full swing: a sword 11, an iron axe 20, a
mace 30, a fist 4, at most 60). A sword's string keeps going at a swing every 1.05 seconds or quicker, an axe's every 1.5, a
mace's every 2; quick half-strength swings always keep up. A perfect guard or an Aura Step in the middle of a string keeps it
open at least for its counter's or step cut's moment. A string left past its window lapses quietly.

**Which art.** After each swing, every art whose string the last swings fit is complete. The one that asks the most wins:
the heaviest last token (the release), then the heaviest string, then the longest, then the highest stage, then the first
registered. Of those that can go now (rested, its price held, its condition met) the best goes: so a string whose best art
is resting or unaffordable **falls through** to the next art the same swings spell (three full swings and a low one without a
full pool are the First Art). If none can go, the best is **refused**, with the reason above the hotbar. A completed or refused
string uses its swings up. A swing that would have finished a string of two or more with a deliberate release (anything but a
plain `swing`), had it come in time, and comes no more than 10 ticks late, is a **fumble**. Strings are written so that none of
the five arts' strings is played on the way to another's (`SwordString.cutBy`).

**What counts.** A swing with an aura weapon, aura and strings on, a method learned, no screen open, not a spectator, not
charging a spell or using an item: at a living creature under the crosshair, or at nothing within 4 seconds of a blow given or
taken, a perfect guard or a step. A swing at a block (digging) never counts.

**The arts.** One a stage, the same strings for every method so a player learns them once. Ember, Rime, Thunder, Gale and
Stone have arts of their own ([The methods' arts](#the-methods-arts) below); the other five methods play the five **common
arts** until theirs arrive (the aura overhaul's step 4): projected aura off the blade (the damage type `wildercord:aura`, the
method's element, armour, and against a player the spell defences and the PvP scale, as the slash), at the weapon's damage ×
the art's factor × `damage_scale`, landing through a foe's moment of invulnerability; their hits answer as the slash's do
(the method's passive, aura marks, experience, never aura back).

| Art | Stage | String | Common art | Aura | Rest |
|---|---|---|---|---|---|
| **First Art** | Glow | `swing swing low` | an arc 3.5 blocks out, 130° wide, up to 4 foes (the one the low swing struck first), × 0.6 | 6 | 3 s |
| **Second Art** | Flow | `leap low` | the same arc, rising: × 0.8, and each foe (never a boss) thrown up | 8 | 4 s |
| **Third Art** | Edge | `counter` | a cut on the foe the counter struck (or the nearest in the arc), × 1.0, staggered again | 8 | 4 s |
| **Fourth Art** | Form | `step` | a line 5 blocks ahead, 2.5 wide, up to 5 foes, × 1.0 | 10 | 5 s |
| **Final Art** | Sovereign | `full full full low` | a ring 4 blocks round, up to 8 foes, × 2.0, thrown back (never a boss); only with a full pool (nine tenths or more) until momentum and awakening exist | 40 | 30 s |

An art's price is paid when it goes off, never past empty (no backlash), and its rest is saved in game time
(`aura_arts`), kept through death. The first string played goes into the Grimoire (`aura:sword_string`), and so does each of
a method's own arts the first time it's played (`aura:art_<id>`, 60 toward the next Heart Circle).

**Server-authoritative.** The client reads strings (exact input timing, free of network jitter) and asks; the server checks
aura and strings on, alive, not a spectator, an aura weapon in hand, the art the player's (stage, method), the swings as read
fitting its string, rested, its condition met, its price held, and that the swings happened: at least that many of the
player's own swings (punches and spear thrusts, as the server receives them) within the string's longest span plus a second,
the last within half a second, and the perfect guard or Aura Step a counter or step cut needs. How full a swing was and
whether it crouched, leapt or ran is taken from the client (the network blurs those by a tick, and a lie would only gain an
art a moment sooner). Requests: a burst of 4, then one every 5 ticks.

**How it shows.** A row of 7-pixel marks 19 pixels below the crosshair, clear of vanilla's attack indicator (or above the
aura bar, or hidden: the player's `string_indicator` in `config/wildercord-visuals.json`), one per swing of the string being
played, in the aura's colour; a thin line under them shrinks as the window runs out. A completed string flashes white-gold and fades over a second; a
fumble shakes, turns dull red and drops away; a refusal greys; a lapse fades. After a perfect guard or a step, the counter's
(gold) or step cut's mark breathes faintly where the next swing will land. Sounds, for the player alone: `aura_string_tick`
(climbing the pentatonic scale with each swing), `aura_string_complete` and `aura_string_fumble`. The placeholder arts use
simple shaped light (an arc, a rising arc, a gold ring, a line, a ground ring), nothing drawn at the swordsman's own eyes, and
the shared feel below: each its own trail (a cut, a rising cut, an X, a thrust, a whole turn), an impact on each foe hurt, its
banner, a flare and the method's technique sound.

### The methods' arts
Each of the ten methods answers the five strings its own way (fifty arts), on one rule: **every method's art in a slot costs and
rests the same and is worth about the same.** The string is the same for everyone, so a method is a different answer to the same question, never a
better one. Prices and rests by slot: 6 aura / 3 s, 8 / 4 s, 8 / 4 s, 10 / 5 s, 40 / 30 s (Crackle rests 2.5 s for being
lighter). The Final Arts wait on one shared gate (`AuraApi.FINAL_GATE`: the peak of momentum, below; a full pool where the server
has momentum off).

**Damage** is in the weapon's own damage (W), times `damage_scale` and the server's `aura.art_damage` (0 to 5, default 1), as
projected aura (armour applies; a player's spell defences and the PvP scale apply; a totem saves). Each art's main foe is struck
through its moment of invulnerability.

**The balance model** (`ArtRules.power`, unit-tested): an art's worth is its main foe's damage (fire counted at a seventh of a W
a second, a wound at what it bleeds, a bonus it often earns at about a third), plus three fifths of what it may do to the others
it reaches and of what it mends (in W, a W being about seven health), plus a quarter of a W for each second of control (a hold,
root, slow, throw, silence, a hardening or a speed), plus 0.07 W for each block it reaches past a sword's three, plus a tenth of
a W for each point of aura it gives back, less the health it costs its own swordsman. Each slot's arts sit within 12% of the slot's worth: First 1.3 W, Second 1.8, Third 1.9 (the counter needs a
perfect guard, the hardest thing to time), Fourth 2.15 (the step before it costs 12 aura of its own), Final 4.35. Against the
techniques: Aura Slash is 12 aura every 2 s for 1.2 W down a 14-block line, so it stays the reach weapon; an art does more in
its moment and rests longer. A Final Art lands about what a strong high-circle spell does (around 15 with a diamond blade).
Each method's five arts together sit within 10% of every other's (the fifty: 11.3 to 11.9 W, 5.8% apart).

**Each method leads in its own thing** (`ArtRulesTest` holds it, in the model's sums and in each art's kinds, `ArtRules.Kind`):
Ember the most damage, Rime the most control, Thunder the most to the others, Gale the most reach, Verdant the most mending (and
roots), Hollow the pulls and the only silence, Starlit the only aura given back (and the stars), Hourglass the echoes, rewinds and
moments held still (second in control), Crimson the bleeding and drinking (second in damage and in mending) and the only price
in health. Each method's own kinds are on every one of its arts, and another method borrows one at most once, so a later art that
takes another's signature fails a test.

**Mending and drinking.** Whatever arts mend (Verdant's mending, Crimson's drinking) goes through one bucket a body
(`ArtKit.mend`): at most 10 health at once, and 10 over the 10 s after (it drains a health a second), so arts buy time in a fight
and never out-heal it. Mending is in health, not W: a better blade doesn't mend more and `aura.art_damage` doesn't touch it. A
drink is a share of what an art dealt (a quarter to a half), held to a cap an art.

**Crimson Moon's price** is a quarter of its swordsman's greatest health, taken straight off their health (`setHealth`, never
damage: no armour, no totem, no death message), and never past a heart: a swordsman at a heart or less pays nothing more, so it
can never kill (`ArtRules.moonToll`).

**Fair to players.** One art deals one player at most `PVP_ART_CAP` 8 in all (after the PvP scale, before armour and defences);
holds (a freeze, a stun, a root, a moment stopped) a player at most 15 ticks and no art's hold takes the same player again for
80; sets a player alight at most 60 ticks; throws, lifts or pulls a player at most 0.6 a tick, and a steady drag (a well, a
sphere) pulls one at most 0.08 a tick, under a sprint, so it can always be run out of; silences a player at most 30 ticks and
not again for 100. A player the swordsman can't harm (`canHarmPlayer`, the mod's targeting) is never touched, and is never mended
by a foe's art. Bosses are only ever slowed: never frozen, held, rooted, lifted, thrown, pulled or silenced (a silence on a boss
only interrupts it).

**Silence** (Null Parry): a silenced creature can't cast (the cast lock, `Statuses.silence`), is interrupted, a creeper's fuse
goes out and a drawn bow is lowered; a silenced player can't play an art (`Refusal.SILENCED`, with a line saying so) or use
the Aura key but for the guard (sneak-tap), so they can still defend themselves.

**No griefing.** Fire on the ground, ice paths, mirrors and rising stone are light and block displays (`ArtBlocks`, tagged so a
restart's leftovers are cleared as their chunk loads). The one real block change is Skate's frosted ice over water: through
`WorldMagic.frostWater` (the terrain spells' own path: `Casters.mayEdit`, dungeon wards, `world_changing_magic` and
`spells_edit_blocks`), thawed in time, switched off by `aura.art_terrain`.

| Method | Art | Slot | What it does (numbers in W) |
|---|---|---|---|
| Ember | **Kindling Draw** | I | a draw-cut 3.5 out, 120°, up to 4 foes, 0.55, alight 3 s; a fire line from 1.2 to 6.2 ahead, 1.5 wide, 3 s, setting alight 2 s |
| Ember | **Rising Cinders** | II | a rising cut 3.3 out, 110°, up to 4, 0.7, lift 0.62, alight 3 s; 12 ticks later cinders off each onto others within 2.2, 0.25 |
| Ember | **Backdraft** | III | the attacker: 0.75 + the caught blow (up to 1 W), alight 3 s, thrown 0.7; the rest of a 75° cone 4.5 long half that |
| Ember | **Wildfire Rush** | IV | a second dash 7 blocks over 4 ticks, 1.4 wide, up to 5, 0.85, alight 3 s; the trail burns 4 s |
| Ember | **Sunfall** | V | a leap (0.95 up, 9 ticks), a dive (1.6), the blast 4.5 round: 2.2 at the heart to 1.5 at the edge, up to 10, alight 5 s, thrown 0.9; a ring of fire 5 out, 1 wide, 5 s, 0.15 every half second |
| Rime | **Frostbite** | I | a cut 3.3 out, 120°, up to 4, 0.6, slow 3 s; a crust each, kept 8 s; the third freezes 1.5 s |
| Rime | **Hailfall** | II | a cut 3 out, 0.5; a cloud 2.6 round over the foe struck (or 3.6 ahead): 7 stones over 18 ticks, 0.28 each, at most 3 on one, slowing |
| Rime | **Glacier Mirror** | III | the attacker 0.9, frozen 2.5 s; others within 3 chilled 2 s; a mirror 2.5 s turning projectiles from in front back |
| Rime | **Skate** | IV | a glide 8 blocks over 4 ticks, 1.4 wide, up to 5, 0.7 and a crust; a frozen foe shatters (1.5, shards 0.3 within 2); the path 5 s: allies faster, foes slowed |
| Rime | **Winter's Hush** | V | a cone 7.5 long, 100°, up to 10, 0.8, frozen 2.5 s; 30 ticks later the still-frozen shatter, 1.7, shards 0.35 within 2.2 (a foe cut by shards once an art) |
| Thunder | **Crackle** | I | 3 cuts 2 ticks apart, 0.27 each, each a spark to another within 4 (0.15); the first interrupts |
| Thunder | **Skyfall** | II | a bolt 6 ticks later on the foe struck (or 4 ahead), 1.6 round, 0.95, held 0.5 s; arcs to 2 more within 4.5, 0.4 |
| Thunder | **Static Riposte** | III | the attacker 0.8, then 4 jumps within 5, each 0.85 of the last, each held 0.4 s |
| Thunder | **Bolt Step** | IV | blinks to up to 4 foes within 8 and in sight, 3 ticks apart, 0.75 each, held 0.3 s |
| Thunder | **Heaven's Spear** | V | a 14-tick charge, then a lance 18 long (level unless aimed past 12°, at most 30°), 0.8 each side, up to 8, 2.0, held 1 s; the sky strikes each |
| Gale | **Cutting Breeze** | I | a wind blade 12 out at 2 a tick, 2 wide, up to 4, 0.45, pushed 0.8 |
| Gale | **Updraft** | II | a cut 3.3 out, 120°, up to 4, 0.65, lifted 0.95; the swordsman 0.75 up, floating 1.5 s; coated blows on them while up × 1.25 |
| Gale | **Eye of the Storm** | III | a spin 3.5 round, up to 6, 0.8, thrown 0.75; 3 s of projectiles turned aside and foes within 2.5 blown off |
| Gale | **Tailwind** | IV | a dash 13 blocks over 5 ticks, 1.6 wide, up to 5, 0.6, shoved 0.8; speed 5 s for the swordsman and allies within 5 of the way |
| Gale | **Hundred Winds** | V | 3 s, 5.5 round: every 5 ticks each foe (up to 8) 0.24 and drawn in; at the end 0.5 and lifted 1.0 |
| Stone | **Rockbreaker** | I | a cut 3.3 out, 0.8, held 0.4 s, slowed 1.5 s, cracked 3 s; 0.3 to others within 1.8 |
| Stone | **Avalanche** | II | driven down if airborne; a shockwave 4.2 round over 6 ticks, up to 8, 0.9 to 0.5, thrown 0.5, slowed 2 s |
| Stone | **Unmoved** | III | the attacker 1.0, thrown 1.6, stunned 1 s on landing; the swordsman hardened 4 s (blows a fifth lighter, no knockback) |
| Stone | **Landslide** | IV | a charge 8 blocks over 8 ticks, 1.6 wide, up to 4 carried along; at the end 0.9, thrown 1.0, stunned 0.5 s; into a wall 1.35, stunned 1 s |
| Stone | **Mountain Splitter** | V | a split 15 long at 1.25 a tick, stone every 1.5; within 1.4 of it up to 10, 2.1, lifted 0.75, stunned 0.75 s |
| Verdant | **Thorn Lash** | I | a lash 4.5 out, 70°, up to 3, 0.45; the first rooted 1.5 s and pricked 0.07 every half second, 3 times |
| Verdant | **Blossom Fall** | II | a falling cut 3.3 out, 120°, up to 4, 0.75; a carpet of blossom 3.5 round, 1.8 ahead, 4 s: you and allies mended 2, then 1 a second; foes slowed |
| Verdant | **Rooted Parry** | III | the attacker 0.95, rooted 1.5 s; thorns 2.5 round, 0.3; you mend three quarters of the caught blow (3 to 6 health) |
| Verdant | **Wild Growth** | IV | a rush 7 blocks over 5 ticks, 1.4 wide, up to 5, 0.65, snagged 0.5 s; brambles 5 s: foes slowed and 0.05 a second, allies mended 1 a second |
| Verdant | **Grove's Heart** | V | roots under every foe within 6, up to 10, 1.5, rooted 2.5 s; a grove 8 s: allies mended 1 a second, foes slowed and 0.05 a second |
| Hollow | **Void Cut** | I | a cut 5.5 out, 70°, up to 3, 0.6, each drawn in to 1.6 in front, slowed 1 s, shadowed |
| Hollow | **Collapse** | II | a falling cut; a well 2.5 ahead drags everything within 4.5 in for 16 ticks (0.3 a tick), then collapses 2.2 round: 0.8 to 0.55, up to 8 |
| Hollow | **Null Parry** | III | the attacker 1.0, held 0.4 s, silenced 3 s (a player 1.5 s); others within 3 shoved 0.5 and 0.25 |
| Hollow | **Rift Step** | IV | through a rift to 0.9 past the back of the nearest foe within 8 in a 70° cone ahead, 1.0; the rifts' edges drag in and cut 2.5 round, 0.35, up to 4, slowed 1.5 s |
| Hollow | **Event Horizon** | V | a sphere 4.5 ahead drags everything within 7 in for 2 s (0.3 a tick); within 2.5 slowed hard and 0.12 every 5 ticks; then a crush 3.5 round, 1.4 to 0.9, up to 12 |
| Starlit | **Star Needle** | I | 3 darts 8° apart, 9 out at 1.5 a tick, seeking; 0.22 each, a star on each foe struck, 0.5 aura back a dart |
| Starlit | **Meteor Shower** | II | 5 stars over 18 ticks on a circle 3 round (the foe struck, or 4 ahead): 0.25 within 1.4, at most 3 on one; a great star 0.4 within 2, starring; 1 aura back a foe, up to 4 |
| Starlit | **Constellation Guard** | III | the attacker 0.6, then 4 stars on it burst 1.5 s on, 4 ticks apart, 0.2 and 0.75 aura back each; foes within 6 already starred burst at once, 0.35 |
| Starlit | **Comet Dash** | IV | a rush 8 blocks over 4 ticks, 1.4 wide, up to 5, 0.6 and a star; 12 ticks on the trail bursts, 0.45 within 1.5, 1 aura back a foe, up to 5 |
| Starlit | **Nova** | V | 12 ticks gathering (slowed), then a ring 7 round, 1.9 to 1.3, up to 12, thrown 0.6; a starred foe 0.4 more; 2.5 aura back a foe struck, up to 20 |
| Hourglass | **Echo Cut** | I | a cut 3.3 out, 120°, up to 4, 0.5; 12 ticks later the afterimage cuts again, 0.5, reaching 4 |
| Hourglass | **Rewind Leap** | II | a falling cut 3.3 out, 120°, up to 4, 0.95, slowed hard 2 s; 6 ticks on, you snap back to where the leap left the ground (at most 10 blocks, 2.5 s back) |
| Hourglass | **Stopped Moment** | III | the attacker 0.8, held 2.5 s (a player 0.75 s), then 0.25 and thrown 0.6 as time starts again; others within 3 slowed 1 s |
| Hourglass | **Blur** | IV | a rush 7 blocks in 3 ticks, 1.4 wide, up to 5, 0.7, slowed; 3 s of drag 5 round: foes slowed, projectiles to 35% speed, you quickened |
| Hourglass | **Thousand Moments** | V | everything within 7 (up to 10) held 2.5 s (a player 0.75 s) while 10 cuts gather; then 2.2 each, plus half of each of your blows on them while held (up to 1 W) |
| Crimson | **Bloodletting** | I | a cut 3.3 out, 90°, up to 3, 0.55; a wound bleeding 0.08 every half second for 3 s (half again on the move); drinks a quarter, up to 3 health |
| Crimson | **Red Rain** | II | a falling cut bursting 3.5 round, 2 ahead, up to 6, 0.7; a red rain there 2 s, 0.08 every half second; drinks 30%, up to 4 health |
| Crimson | **Sanguine Parry** | III | the attacker 0.7, held 0.4 s, and the caught blow (up to 1 W, at least 0.3) bled back out over 3 s; others within 2.5 0.25; drinks half, up to 6 health |
| Crimson | **Frenzy** | IV | a rush 7 blocks over 4 ticks, 1.4 wide, up to 5, 0.7; each cut, and each of your blows after, quickens the blade 5% (up to 20%) for 5 s |
| Crimson | **Crimson Moon** | V | costs a quarter of your greatest health (never past a heart); a 180° arc 6 out, up to 10, 2.5, bleeding 0.1 six times; drinks half, up to 10 health |

**The balance pass** (step 4, weighing all fifty together) moved eight of the first twenty-five: Glacier Mirror's freeze 2 s to
2.5 s, Unmoved 0.9 to 1.0, Eye of the Storm 3.2 round to 3.5, Skyfall's arcs 0.35 to 0.4 and Hundred Winds 5 round and 0.22 to
5.5 and 0.24 (each sat 5% to 7% under its slot once the new arts set the standard); Cutting Breeze 10 out to 12 (and 0.5 to 0.45)
and Tailwind 11 blocks to 13, with Heaven's Spear 20 to 18, so Gale leads reach again (44 against Thunder's 43); and Winter's
Hush's shards cut a foe once an art, not once a shattering foe near it.

**How they show.** Every art has its own trail (`AuraFx` strokes), impacts by weight, its banner, a body flare, its method's
technique sound and a voice of its own (`aura_art_<id>`, feel kit; fifty voices, and five more for an art's second beat:
Sunfall's landing, Winter's Hush's shattering, Event Horizon's crush, Comet Dash's bursting trail, Thousand Moments' release). Its shapes are server-sent light: the swordsman's own view
gets only what keeps clear of the middle of their sight (the trail thin and low, marks on foes, light on the ground ahead),
while the big shapes near the body (a lance, a mirror, a near crescent, a gout of flame) go to everyone else and to the
swordsman only in third person (`AuraFx.Shown`). By day the light lays a thin dark rim under itself.

### Momentum and openings
**A fight builds.** Clean play fills a swordsman's **momentum**, momentum wears foes' **stance** faster, a broken stance **opens**
a foe, and an opened foe can be **finished**. All of it is server-side and synced only when it changes: both sides work out the
ebb and the recovery from the same rules (`aura.MomentumRules`, `aura.StanceRules`, unit-tested).

**Momentum** (0 to 100, `aura.Momentum`). It builds from a clean hit (a full swing of a swordsman's blade that hurt a real foe:
3.5, a falling critical 5), an art landing (6, 8, 10, 10 by slot for its first foe, then 1.5 for each of up to three more; the
Final Art builds nothing), a perfect guard (12 against a blow, 6 against a shot or a spell), an Aura Step taken through a real
attack (10, once a step), a stance broken (8), a finisher (14) and a worthy foe felled (3). A hit taken knocks off 12% to 40% of
it by how heavy the blow was (a held guard halves that) and 2 more. It holds four seconds after the last blow given or taken, then
ebbs 8 a second. Its tiers (25, 50, 75, and the peak at 95) make every art cheaper (10%, 15%, 20%, then 25% off) and stronger
(5% a tier, 20% at the peak: another player's art cap still holds) and wear stance faster (×1.1, 1.2, 1.3, 1.45). The peak opens
the Final Art (`Momentum.FINAL_GATE`, or anything step 6 adds through `AuraApi.openFinalArt`), and playing it spends 40. A clean
fight (a full swing every 0.65 s, a foe felled every fourth, an art every 3 s, nothing taken) peaks in ten to fifteen seconds; a
zombie's blow every two seconds keeps it from the peak for good (`MomentumRulesTest` plays both).

**Each method's temper** (`MomentumRules.TEMPERS`): how much each kind of play builds, how much a hit knocks off, how long it holds
and how fast it ebbs, and the foe state its blows feed on (×1.4 on a foe in it, the state its own arts leave):

| Method | Feeds on | Its way |
|---|---|---|
| Ember | burning | the most from blows (×1.15), the fastest ebb (×1.4, holds 3 s): hot, then out |
| Rime | chilled or frozen | perfect guards ×1.25, the slowest ebb (×0.6, holds 5 s) |
| Thunder | ionised | each foe an art reaches past the first counts double; a quick ebb |
| Gale | thrown up | steps through attacks ×1.5; an art from afar counts whole |
| Stone | cracked | a hit knocks off half, a guarded one nothing; perfect guards ×1.4 |
| Verdant | rooted | arts ×1.15, holds 6 s, the slowest ebb |
| Hollow | silenced or shadowed | arts ×1.15 |
| Starlit | starred | arts ×1.2 (the most) |
| Hourglass | stopped in time or held | holds 8 s (the longest) and ebbs slowly |
| Crimson | bleeding | blows ×1.1, a hit knocks off least but Stone's, and a hit taken at half health or under builds 3 |

**Keeping it honest.** Weak swings and a sweep's other blows build nothing (they hold what's there); the slash, bows and spells
build nothing at all. A foe that can't fight back (a mob with no mind of its own that nothing holds, one riding a boat or a cart)
gives nothing. Each foe's blows give only so much (`budget`: 12 to 40 by its health, a boss 60, a player or a dummy 30, forgotten
after twenty seconds) until its stance is broken. An art landing six blocks off or further counts half (Gale's whole). On a
training dummy or anything in the practice arena it builds at 60% and only to 74 (a tier short of the peak); in the arena itself
it may reach the peak, and changing world empties it. Out of a fight it falls from the peak within five seconds and is gone in under
twenty, so it can't be carried to a boss from anywhere.

**Stance** (`aura.Stance`). Every foe a swordsman may fight has one: a creature 0.8 of its health, 20 to 100 (so a weak foe
usually dies to plain blows before it opens: technique opens it); a Runebound, a duelist or a fallen knight a quarter more; a
boss 0.3 of its health, 60 to 150, taking 0.7 of every wear, a quarter steadier after each break (up to twice); a player 30; a
training dummy 40. A full swing wears what it was dealt at (a fall's critical ×1.35, a glance or a sweep's other blows ×0.35, a
blade with no aura ×0.6), an art's strike twice what it deals (×1.6 more for an art that quakes, ×1.2 for one that holds,
freezes, roots, stops or shocks), aura that isn't an art (the slash, a spark) half, a perfect guard 35% of the attacker's stance
at once (a boss 20%); Stone's blades a quarter more on every blow; the striker's momentum; and the server's `stance_damage`. It
comes back after three seconds (a boss's 1.5 s, a player's 2 s) at a fifth of it a second (a boss a tenth, a player a quarter).

**Opened.** Broken, a foe stands opened (a creature 3 s, held still; a boss 2 s, slowed hard, never held; a player 1.5 s, slowed,
their swing spent, shield and Aura Guard broken, never held) and marked (a gold flash and ring as it breaks, then a glint over its
head every half second and a ring at its feet every second, and each client's seal over it). The next full swing of a swordsman's blade on it
is a **finisher**: the blow plus a share of what the foe has lost (a creature 35%, at most four times the blade's damage; a boss
12%, at most 2.5 blades; a player 25%, at most 8 under the PvP scale and a quarter of their health), as part of the blow, so armour,
a totem and every defence have their say; then aura back (6 + 2 a stage, Starlit's half again, a quarter on a practice target),
momentum (14), and the foe stands steady (a creature 3 s, a player 5 s, a boss 10 s) with its stance whole. An opening let pass
ends the same way. Each method's finisher looks and sounds its own (`aura.arts.Finishers`, names in the guide), and leaves a
touch of its element no stronger than its arts would: Ember sets alight, Rime chills, Thunder ionises, Gale lifts, Stone cracks
and slows, Verdant roots and mends (3 health each, in the arts' mending bucket), Hollow draws foes near in, Starlit gives the most
aura back, Hourglass cuts a second time (to look at), Crimson leaves its foe bleeding and drinks (in the bucket).

**Players.** With `pvp_stance` on (the default), duels reward pressure and guarding: no blow (or art's strikes together) wears
more than a third of a player's stance (half over a whole art), so it takes three blows or more; a blow caught on a held Aura
Guard or a raised shield wears the guard's own stance (0.6 of what it caught), so a turtle breaks in the end; a perfect guard
breaks into the attacker's. Teams and the PvP rule are respected (`ArtKit.harmable`). A finisher on a player is never a one-shot:
from full health it adds nothing, and at most a quarter of their health.

**Settings** (`aura` section): `momentum`, `momentum_gain` (0 to 5), `momentum_ebb` (0 to 5, 0 never), `stance`, `stance_damage`
(0 to 5), `finisher_damage` (0 to 3), `pvp_stance`. The client learns whether momentum and stance are on (`Config.Sync.combat`).

### Awakening
**Everything let go at once.** From Edge, a swordsman whose pool is full and whose fight is going well can **awaken**: for a while
they burn at their strongest, and then they pay for it. All of it is server-side (`aura.Awakening`), saved and synced to everyone
near (the body's aura is drawn from it); the numbers are `aura.AwakeningRules` (unit-tested by `AwakeningRulesTest`).

**The input.** The Aura key tapped, then pressed again at once (within the double tap's 8 ticks) and **held 14 ticks**
(`AuraApi.Trigger.TAP_HOLD`, the key's fifth way). A quick second press is still the double tap (the step from Form); a second
press let go after it was clearly held (5 ticks) but before it completed sends nothing; a lone hold is still Dominion; the press
while sneaking is still the guard. The held press shows its charge as a thin ring of the aura's colour filling round the
crosshair (from its 4th tick, so a double tap never flashes it), snapping out as it completes. While an awakening is ready a lone
tap waits out the double tap's moment (as it always does from Form), so the tap of a tap-and-hold never looses a slash that
would spend the pool; when it isn't ready (or below Form when no step waits) taps go at once and a tap-and-hold is refused with
why. Why this and not a new key: it sits inside the key's grammar (tap, double tap, hold, and now tap then hold), needs both a
tap and a deliberate hold, so no swing, guard, step or Dominion of a fight can set it off by accident, and the charge ring lets a
press held by mistake be let go in time.

**What it asks.** Stage Edge or higher; a full pool (95% of capacity, so a coated swing's half point never stands in the way);
momentum at 50 or more (the second tier: `awakening_momentum`; with momentum off on the server, the pool alone); an aura weapon in
hand; not awakened, not spent, and the last one's rest run out. Refused in that order (momentum before the pool: short of it the
lone tap goes as the slash and spends the pool, and the line should name what was really missing).

**While it lasts** (12 s at Edge, 16 at Form, 20 at Sovereign, times `awakening_duration`):
- **arts cost nothing** (`awakening_art_price`, 0 by default; a share of the price written into the state as it begins, so both
  sides price arts alike: `SwordStrings.price` multiplies it in after momentum's discount);
- **momentum is held at the peak** (`Momentum.hold(player, 95, duration)`: no ebb, a hit knocks nothing off below it), so arts are
  at their strongest (×1.2) and wear stance fastest (×1.45), and **the Final Art opens** (`AuraApi.openFinalArt`);
- **coated blows land 15% harder** (`awakening_damage`; against a player half of it, as one more bonus under their `max_bonus` cap
  and the `pvp_scale`: about 5% in the end with the defaults);
- **a tenth faster** on foot (movement speed, base) and with the blade (attack speed, total) (`awakening_speed`);
- **each finisher landed feeds it a second** (20 ticks), four at most (80 ticks), its rest moved on with it;
- aura still comes in from blows, and the techniques (guard, slash, step, Dominion, aura armour) cost as usual.

**Spent.** When it runs out (or ends early through `AuraApi.endAwakening`) whatever aura is left **burns away** (the pool is
emptied), momentum is emptied, and the swordsman is **spent** for 30 s (`spent_seconds`): Slowness I (put back if milk washes it
off) and **no aura comes in at all** (`Aura.gain` and `Aura.giveBack` refuse it: blows, the breathing stance and its beat,
Dominion's flow and trickle, finishers' and Starlit's aura back). With no aura there's no coat, no guard, no slash, no step, no
aura armour and no arts: a real window for a foe or a rival, and for a mob pack. Then the aura stirs again; the next awakening
waits **three minutes from its end** (`awakening_cooldown_seconds`; the spent time runs inside it). A death ends it (the new body
isn't spent; the rest runs from the death). Leaving mid-awakening and coming back later lands you in what's left of it (the times
are game time, saved), momentum held again for what's left; a change of world holds momentum again too.

**The Final Art, finishers and the peak, together.** A Sovereign's awakening opens the Final Art (the hold keeps momentum at the
peak anyway; the opener covers a server with momentum off) and makes it free. Its release still spends 40 momentum, but the hold
lifts it straight back to the peak, so the swordsman stays at full strength after it. Its rest (30 s) outlasts even a fully fed
Sovereign awakening (24 s), so **the Final Art comes at most once per awakening**: awakening is the way to play it on demand from
half momentum, and the price is the spent state after. Finishers come faster while awakened (stance worn ×1.45) and each one feeds
the awakening a second, so a swordsman who keeps opening foes keeps it burning a little longer: up to four seconds. Nothing
multiplies twice: a finisher's extra, the Final Art's damage and the PvP caps are as ever.

**The Sovereign's awakened Dominion.** Dominion raised while awakened is the Sovereign's own (`aura.arts.Awakenings.Ground`): half
again as wide (4.5 blocks) and as long (12 s), its foes 10% weaker still (`dominion_weaken` + 0.1, under the same cap and PvP
scale), its chain leaping to two foes, aura flowing back 2.5 times as fast (Starlit's 3), its own ground drawn in place of the rune
circle, its name in the banner over "Awakened Dominion", and shaped by the method (each beat a second):

| Method | Its name | What it does |
|---|---|---|
| Ember | Throne of Cinders | foes inside set alight each second (a player at most 3 s at a time), a ring of flame round its edge |
| Rime | Court of Winter | raised, every foe inside frozen solid a moment (2 s; a player held to the cap); then chilled hard each second |
| Thunder | Seat of Storms | each second a bolt falls on a foe inside: 0.45 of the weapon, and a shock that interrupts it |
| Gale | Windward Ground | every other second an updraft throws the foes inside up (juggled: coated blows land harder on them); the owner standing in it turns shots aside |
| Stone | Unmoving Mountain | the owner standing in it hardened (Resistance I, unmovable); each second it wears every foe's stance (4) |
| Verdant | Wildwood Court | raised, foes inside rooted (2 s); each second the owner and allies in it mended a health (in the arts' mending bucket) |
| Hollow | Sunken Hall | raised, foes inside silenced (3 s, a player 1.5); every other tick they're dragged toward its heart (a player only leaned on) |
| Starlit | Field of Stars | each second a star falls on a foe inside (0.3 of the weapon, marking it; a marked one's bursts for twice) |
| Hourglass | Stilled Hour | raised, foes inside held still a moment (1.5 s); then slowed hard, and shots foes loose inside slowed to under half |
| Crimson | Crimson Court | every other second foes inside bleed (0.25 of the weapon, marked bleeding); a quarter of every blow the owner lands inside drunk |
| any other | Sovereign Ground | only stronger |

Everything goes through `ArtKit` (holds, throws, drags, silences, burns held to the player caps and the boss rules) and its strikes
through one `ArtKit.Hits` for the whole Dominion, so another player takes no more from all its strikes together than from one art
(8). A Dominion raised awakened keeps its shape to its end even if the awakening ends first.

**The moment.** The server sends the shared stinger and the method's own voice (`aura_awaken`, `aura_awaken_<method>`), its banner
(grand: "Awakening" over "<method> · <stage>"), a long surge of the body's aura, rings closing in over the ground and round the
waist (spectacle) while it gathers, then at 6 ticks the burst: a modest flash and ring at the heart (a whisper low in the owner's
first person), a shockwave in two rings racing out over the ground and a column of light (spectacle: seen by the owner only in
third person), the method's flourish (`Awakenings.flourish`: Ember a ring of flame, Rime ice bursting outward, Thunder three bolts
out of the sky, Gale a rising wind, Stone the ground cracking and slabs heaving, Verdant a flower of light, Hollow a black point
bursting, Starlit a star over the head, Hourglass a clock face on the ground, Crimson crescents of blood), foes within 3.5 blocks
thrown back a step (no harm, the arts' throw caps) and a small shake. Every client draws the rest from the synced state
(`AwakeningRules.form`): motes drawn in to the heart while it gathers; then the body's aura climbs past its stage into its
awakened form, surging at the burst and settling by 26 ticks: eyes burning from Edge (Sovereign's brighter, with a wisp off each),
an Edge swordsman's borrowed mantle and low corona, a Form swordsman's corona, a Sovereign's standing taller, streamers of light
racing up round the body, the pool under it wide and bright, embers streaming off; in its last two seconds it gutters, flickering
down. Spent, it falls to a faint ash-grey haze shedding ash. In the owner's own first person: the edges of the view glow once in
the aura's colour (at most a sixth opaque at the edge, gone within two seconds); then only the bottom-edge whisper and the HUD
(the strip edged in its fire, a light racing along the bar, "Awakened: n s" above; "Spent: n s" after; a small flame after the
stage diamonds: breathing gold when ready, blazing, ash, dark and filling back while it rests). The Aura page writes its state at
the end of the aura line.

**Settings** (`aura` section): `awakening`, `awakening_momentum` (0 to 100), `awakening_duration` (0.25 to 4), `awakening_cooldown_seconds`
(0 to 3600), `spent_seconds` (0 to 300), `awakening_art_price` (0 to 1), `awakening_damage` (0 to 1), `awakening_speed` (0 to 0.5). The
client learns whether it's on and the momentum it asks for (`Config.Sync.combat`: bit 4, and bits 8 to 15).

### Ways
**The path forks at Edge.** At the Edge breakthrough a swordsman chooses a **Way**: the **Blade** (offence), the **Bulwark**
(defence), the **Shadowstep** (movement) or the **Banner** (together). Each has a **node** at Edge, Form and Sovereign, and each node
is a passive and a change to a technique the swordsman already has, so a Way changes how they fight more than how hard. The state is
`aura.Ways` (the `WAY` attachment: saved, synced to everyone near, kept through death); the rules and every number are
`aura.WayRules` (unit-tested by `WayRulesTest`); the choosing is `aura.Crossroads`; the node effects are `aura.WayEffects` and
`aura.WayBanner`; changing is `aura.CrossroadsIncense`.

**The choice is a moment: the crossroads.** 50 ticks after the Edge breakthrough (once its title has been read) a standard of light
rises for each Way (up to six, add-ons' included) on an arc in front of the swordsman: 4.2 blocks out (nearer, 3.2 then 2.4, where
there's no room; anywhere round them as a last resort; refused with a line in a tunnel), 24 degrees apart so all four are in a usual
first-person view, each on ground with two blocks of air above it and in plain sight. They're server-sent shaped light (drawn every
4 ticks, each drawing lasting 9, so they burn steadily), seen by everyone near: the Blade a sword planted point-down, the Bulwark a
kite shield with a cross on its pole, the Shadowstep a pillar of shadow with a bright heart trailing three afterimages under a crescent,
the Banner a swallowtail pennant stirring on its pole, an add-on's Way a plain pillar, each over a ring in its colour. The swordsman
**strikes** one: every swing sends a punch (`mixin.SwordStringsSeenMixin` calls `Crossroads.swung`), and the server tests the look ray
against each standard's axis (`WayRules.strike`: within 0.8 blocks of it, between its foot and head, within 5.5 blocks; of two, the one
nearer the line; nothing solid between). A blade must be in hand. The first strike **leans** (the standard flares, rings rise round it,
the others dim, the line over the hotbar names the Way: 160 ticks to strike again); a strike at another moves the lean; the second
strike on the leaned standard **walks** it: `Ways.choose`, the standard pours into the swordsman over 8 ticks (rays streaming into the
body as spectacle, its own light sinking in the world), the others shatter, a grand banner names the Way over "Your Way", the shared
chord and the Way's own voice, a long body flare, chat lines with the creed and each node (yours now, waking, or yours at a later
stage), Grimoire entries `aura:way` and `aura:way_<id>`. The swordsman's own client names the standard under the crosshair (its Way,
what a strike does now, its three nodes) above the hotbar on a soft backing (`client.WayHud`), from the `CROSSROADS` attachment (where
the standards stand and the lean, told to its swordsman alone). The crossroads stands 60 s; it fades if they walk 12 blocks from where
it rose, change world, die or leave. Why strikes: choosing a Way is choosing how your blade fights, so the blade makes the choice, in
the world, where others can see it; the two strikes keep a swing at a mob from choosing for you, and the Aura page can be read while it
stands.

**Swordsmen past Edge with no Way** (Ways came in after they got there, they unbound theirs, or let the crossroads fade) are told on
joining, and call the crossroads by **holding the breathing stance** 60 ticks after it settles (never while a stillness trial is under
way; 200 ticks' rest after one fades). Their **first choice is free**: every node they've reached is theirs at once. This keeps the moment
for everyone (it's the same crossroads, called by the same meditation the stance always was) without punishing a Sovereign for having
got there before Ways existed.

**Changing Way: a Crossroads Incense, at a place of power, then settling.** Crafted from two Aura Shards, an amethyst shard and blaze
powder (shapeless), held in use for 2 s (smoke curling up in the Way's colour, a ring round the feet) at a ley crossing's heart
(`way_change_at_power`; anywhere else it won't catch and isn't used), it **unbinds** the Way (`Ways.unbind`: its nodes go dark, its
standard rises out of the swordsman and breaks), and 30 ticks later the crossroads rises there. The new Way **settles**: its Edge node
wakes at once, its Form node after half of `way_settle_xp` (240: 120) and its Sovereign node after all of it, counted from the
experience meaningful blows earn (`AuraExperience.earn`, before the stage's cap, so a swordsman waiting at a threshold still settles;
practice doesn't count). About three quarters of an hour of steady fighting at the mod's pace. Why these three: the shards are a fight
(fallen knights), the ley crossing a journey and a deliberate act, and the settling makes a new Way walked into rather than bought,
which still costs a rich Sovereign something. Nothing is lost for good. Operators: `/wildercord aura way <id|none>` and `way crossroads`.

**The nodes** (`WayRules`, the numbers there; "near" is `banner_range`, 12 blocks):

| Way | Edge | Form | Sovereign |
|---|---|---|---|
| **Blade** | **Keen Edge**: clean hits build momentum ×1.35 (an `onMomentum` hook on "hit"); Aura Slash pierces: a held guard doesn't stop it (`Crescents.Flight.pierce`; the guard still takes its share), 4 more foes (10), and it wins a clash, flying on at half its harm | **Cascade**: finishers' extra ×1.4 (`onFinisher.extra`; against a player re-capped at `StanceRules.playerFinisherCap`); a finisher opens the nearest creature within 5 blocks of the finished foe whose stance is half worn or more (`Stance.wear` of what's left; never a player or a boss) | **Storm of Edges**: each finisher feeds the awakening 40 ticks, 160 at most (`Awakening.fed` asks `WayEffects.feed`); awakened, Aura Slash costs nothing and rests 20 ticks |
| **Bulwark** | **Wide Guard**: own stance wear ×0.6 (an `onStance` hook on a Bulwark target), momentum lost to hits ×0.67; Aura Guard covers every side (`AuraGuard.faces`), its perfect moment 10 ticks (7), a held guard throws a third of what it catches back at a melee striker as projected aura (once in 10 ticks a striker) and turns shots back (1.5 aura each) | **Living Wall**: aura armour takes 0.35 (0.25 + 0.10) for 0.8 of its price a point; Intent challenges: creatures within its reach targeting an ally (a player ally, a pet) turn on the Bulwark (never a boss), and a creature striking the raised guard staggers (Slowness II and Weakness 20 ticks, once in 40 a creature) | **Unbroken**: awakened, stance wear 0 and harm a fifth less (`WayBanner.harm`); spent, no slow; Dominion is a bastion: a foe's shots crossing in from outside are turned back at its rim (a reflection off the edge), rings of blue light stand at the rim, creatures inside slowed a level more |
| **Shadowstep** | **Slip**: blows and arts from behind (the foe's rear 120 degrees and more off its front, or a foe that lost them) wear stance ×1.5 (re-capped at a third of a player's stance); a perfect guard against a blow slips behind its striker (`ArtKit.behind`, a clear line from it, never through walls or wards; the stagger then doesn't throw the striker back into them), facing its back, and the striker loses them 30 ticks | **Afterimage**: 40 ticks after an Aura Step every blow counts as from behind; the afterimage left where the step set off lingers 18 ticks more (`AuraStep.Stepped.linger`) and strikes 8 ticks after the step: every foe within 2.2 blocks for 0.6 of the weapon through one `ArtKit.Hits` (stance worn as an art's, the art PvP cap) | **Thousand Shadows**: a finisher from behind readies the step at once; awakened, the step costs nothing, rests 20 ticks, and its afterimage strikes again 8 ticks after the first |
| **Banner** | **Battle Cry**: a third of the momentum built (after its hooks; never a share of a share) builds for each allied swordsman near; each finisher lets out a cry: the Banner and everyone allied near (pets too) steadied a sixth for 100 ticks, allied swordsmen +6 momentum and half the finisher's aura | **Rallying Presence**: a quarter of the aura gathered (through `Aura.gain`) is given to each allied swordsman near (`Aura.giveBack`: refused while spent); while Intent presses on a foe, the Banner and allies within its reach steadied a tenth (30 ticks a press) | **Shelter**: allied swordsmen near lose momentum to hits ×0.67; an awakening holds allied swordsmen's momentum at 50 while it burns (never an ally's own awakening); Dominion shelters: the Banner and allies inside steadied a fifth, allied swordsmen inside gather aura twice as fast with the trickle, their momentum doesn't ebb |

**Allies** (`WayBanner.ally`, the rule chorus casting keeps): never two duelling each other; teammates; or two players who couldn't harm
each other either way (`canHarmPlayer` and the mod's own `Targets.canHarm`, both ways). Pets of the Banner's (or its team's) share in
steadying. **Steadying** from every source together (cries, presence, shelter, an unbroken Bulwark) never takes off more than 0.3, and
against a player's harm only the PvP scale's share of that (0.18 at the defaults); only harm a foe deals (a creature, a player, their
shot or spell), never a fall or the void (`WayBanner.harm`, in `LivingEntityAuraMixin` after a foe's Dominion has weakened the blow).

**Balance.** `WayRules.WORTH` weighs each node in shares of a swordsman's strength (offence, defence, tempo at 0.15, mobility, and
support counted once per ally near), with the reasoning beside each; `WayRulesTest` holds: alone, the Blade, the Bulwark and the
Shadowstep within 15% of each other (about 0.27 to 0.31); alone, the Banner the quietest but no trap (0.17, between half and four
fifths of theirs); with one ally all four within a fifth (0.30 to 0.32); with two the Banner leads (0.48 against 0.35), within half
again; each Way leads in its own term; no single node worth more than 0.16. Every multiplier against a player stays inside the caps
step 5 set (finishers, stance) or the spell defences (anything projected). The Final Art still comes at most once per awakening with
Storm of Edges (400 + 160 ticks < its 600-tick rest).

**Settings** (`aura` section): `ways`, `way_settle_xp` (0 to 10000), `way_change_at_power`, `banner_range` (2 to 48), `banner_share` (0 to 1),
`banner_aura_share` (0 to 1). The client learns whether Ways are on (`Config.Sync.combat` bit 8, `Config.ways`).

### Techniques of your own
**From Edge a swordsman writes their own.** A technique is three **parts**, a **stroke** (how the blade moves), a **release** (how its
force leaves the swordsman) and an **intent** (what it's for), with the method's **element** in all of it, a **name** its writer chose
and a **string** of swings. It's played like an art (it is a string art, the player's own) and ranks up as it lands. The rules and every
number are `aura.TechniqueRules` (pure, unit-tested by `TechniqueRulesTest`); the state, the string source, the writing and the ranks are
`aura.Techniques` (the `aura_techniques` attachment, a `Book`: the parts learned, three slots, and the records of up to twelve techniques
written; saved, synced to its owner, kept through death); the performance is `aura.arts.TechniqueArts`; scrolls are
`aura.world.TechniqueScrollItem` and `aura.ScrollSources`; the page is `client.TechniquePage` inside `client.AuraScreen`.

**The parts.** Six strokes, four releases, seven intents (an add-on can register more intents, `AuraApi.registerTechniqueIntent`):

| Stroke | Shape | Reach | Width | First foe | Foes | Others | Its own |
|---|---|---|---|---|---|---|---|
| **Thrust** | line | 5.5 | 0.7 | ×1.0 | 3 | ×0.7 | long and narrow |
| **Rising Cut** | cone | 3.4 | 100° | ×0.8 | 4 | ×1 | lifts 0.55 |
| **Falling Cut** | cone | 3.6 | 60° | ×1.12 | 2 | ×0.6 | stance ×1.4 |
| **Sweep** | cone | 3.3 | 170° | ×0.62 | 6 | ×1 | |
| **Spin** | ring | 3.0 | 360° | ×0.58 | 8 | ×1 | |
| **Draw** | cone | 3.8 | 110° | ×0.72 | 4 | ×1 | innate |

Releases: **On the Blade** ×1.0 (innate); **Wave** ×0.75, flying 7 blocks past the reach at 1.4 a tick (a spin's a ring racing 5 more),
foes ×1.25, stopped by the first solid block; **Burst** ×0.72, the stroke made a ring round the feet (its reach ×0.85, a spin's +1),
foes ×1.5; **Afterimage** ×0.62 now and an afterimage (`AuraStep.afterimages`, lingering) that strikes the stroke again from where it was
played 12 ticks later for ×0.55 of the stroke (from where the swordsman stood, wherever they've gone since; seen by them too). Intents:
**Pierce** ×0.92, +3 foes and +1.5 reach (half on a ring); **Sunder** ×0.88, stance ×2.5; **Bind** ×0.85, roots 24 ticks
(`ArtKit.root`, the arts' player cap and immunity); **Echo** ×0.85, every foe struck struck again 10 ticks later for half, if within
reach + 2.5; **Ward** ×0.88, the swordsman steadied a fifth for 80 ticks (`WayBanner.steady`, inside the 0.3 cap); **Rally** ×0.88, the
swordsman and allies within 8 steadied a tenth for 100 ticks and allied swordsmen +5 momentum; **Infuse** ×0.95, the element ×2. At
**Tempered** each intent deepens a little (pierce +4 foes, sunder ×2.65, bind 27 ticks, echo 0.54, ward 90 ticks, rally 110 ticks over 9
blocks, infuse ×2.15).

**The element** (`TechniqueRules.Flavour`, by the method's passive): Ember sets foes alight 24 ticks; Rime chills 30; Thunder interrupts
the first foe and, once the stroke has struck all it will, a spark leaps to the nearest foe within 4 blocks it didn't strike for 0.35 of
the blow; Gale throws back 0.45 and reaches a block further; Stone wears a stance ×1.4 and staggers a creature 12 ticks; Verdant mends
the swordsman 1 a foe (3 at most); Hollow draws foes in 0.3; Starlit gives back 1 aura a foe (2.67 at most); Hourglass echoes the first
two foes 6 ticks later for 0.18; Crimson opens a wound (two bleeds of 0.05 of the weapon) and drinks 12% (1.5 at most); an add-on's
method with no element strikes ×1.12. Each is about a seventh of a W in the model, so the methods' techniques sit within 8% of each
other (`TechniqueRulesTest`), a different answer each.

**Worth, price and rest: the arts' own model.** `TechniqueRules.model` turns a technique's profile into an `ArtRules.Art` (its first
foe's blow with the afterimage's and the echo's share, the rest at the stroke's others share times its "fair" share, lift, stance past
an art's, holds, steadying and the element as control, mend and aura) and `ArtRules.power` weighs it, exactly as the fifty arts were
weighed. Price is that worth at **Tempered** (with its temper and edge) times **4.32 aura** a W, rest **43.2 ticks** a W: a shade under
the arts' mean rate across the First to Fourth Arts (4.444 and 44.23), so a Tempered technique is about as good a bargain as an average
art, a Raw one a little worse and a Peerless one a little better. Both are held to 3 to 13 aura and 30 to 130 ticks, and a string's
**effort** (`effortOf`: its tokens' weights and half a point a swing past the first) moves both by at most 2% (cheaper for a demanding
string). `TechniqueRulesTest` holds every combination at every rank, temper, edge, method and string: worth per aura and per tick inside
the arts' own band (slots I to IV: 0.2065 to 0.2536 W an aura), as written from about two thirds of a First Art's worth to a little over
a Fourth Art's, at most 2.7 W, far under the Final Art; never strictly better or worse than an art. Why the arts' model: techniques are
played beside the arts, on the same strings and the same momentum; anything else would make one of the two a trap.

**Strings.** Two to five swings, at least one mark (low, leap, run, counter or step: full swings alone would play themselves in any
fight), weight at least 3 (`SwordString` weights: swing 0, full 1, low/leap/run 2, counter/step 4). A string can't `clash` with any of
the swordsman's arts' strings or another technique's (`AuraApi.conflicts`: one would never be playable as written); one that merely
`overlap`s an art (the same swings could finish both; a low and a running swing never can, `together`) is allowed and the page says
which goes first (`StringReader.compare`: last token's weight, total weight, length), except an overlap only an art's counter or step
would meet in passing (`incidentalCue`).

**Slots** (`TechniqueRules.slots`): one at Edge, two at Form, three at Sovereign; art ids `technique_1` to `technique_3`. A technique is
written into a slot over the `technique_write` payload (`Techniques.write`; both sides refuse from `Techniques.refusal`: off, before
Edge, a closed slot, an unknown part, the string's problems, a clash, and never in a fight); `technique_erase`, `technique_choose`
(temper, edge) and `technique_inscribe` do the rest, all under one `PacketThrottle` (4 in 10 ticks).

**Names.** `TechniqueRules.cleanName`: section signs and the character after them, control and C1 characters, bidirectional controls
and isolates, zero-width characters, line and paragraph separators, the byte-order mark, lone surrogates and every format, private-use
and unassigned code point are dropped; runs of spaces collapse; 24 code points at most. Empty, it's named from its parts (`autoName`:
"Sundering Thrust Wave", "Blazing Draw"). It's shown everywhere as a literal `Component` (`AuraApi.artName`: the banner and the refusal
lines), never parsed, so a name can't carry formatting, a click event or a translation key to another player. The page filters typing
to `nameCharacter`; the server cleans again whatever arrives.

**Where parts come from.** Innate from Edge: the Draw, On the Blade and Infuse (so a first technique can be written the moment Edge
arrives). **Scrolls** (`wildercord:technique_scroll`, the part in the `wildercord:technique_part` component, its art by family): one of
the 12 scrollable parts, drawn by `ScrollSources` (each a loot table, a chance and three favourite parts at three times the weight,
scaled by `technique_scroll_chance`) through the `wildercord:random_technique_part` loot function, added to trial chambers' rare reward
vaults (12%; ominous 25%), ancient cities 14, stronghold libraries 10, bastion treasure 16, jungle temples 10, desert pyramids 8,
woodland mansions 15, pillager outposts 8, every expedition vault 20, and a fallen knight's drops 6. Read with a method; a part already
known refuses (not used up); one learned before Edge waits. **Duelists**: a beaten duelist shows a part the challenger doesn't know, its
method's three favourites three times as likely (`TechniqueRules.duelistWeights`). **Ways** lend one each while walked
(`TechniqueRules.WAY_PARTS`: Blade pierce, Bulwark ward, Shadowstep afterimage, Banner rally); ward and rally are Way-only (never on a
scroll), so a Way's identity reaches into techniques without being something another Way can buy. A technique missing a part (a Way
left) rests: its string plays the arts. The `sword_tomb` source is registered for step 11's tombs (spin, burst, afterimage, sunder and
echo favoured).

**Ranks** (`TechniqueRules.RANKS`): Raw 0, Honed 60, Tempered 200, Keen 520, Peerless 1300 experience, strength ×0.965, 0.98, 1.0,
1.02, 1.04. Experience per foe struck is the foe's worth (`AuraCombat.worth`: a monster 1, stronger more) times (1 + 2 × the share of
its health taken + 1 for a kill), times the moment (`AuraCombat.moment`, more in danger), at most 12 a use, falling with repetition in
the same place (`REPEAT` 0.08 a recent use, floored at spell mastery's floor), and nothing on a helpless foe; training dummies give
half, 40 at most a technique. Why its own and not spell mastery: a technique isn't a spell (no mana, no cast to measure, and its parts
are what change), but it keeps mastery's shape (earned on real foes, more in danger, less for grinding, a capped practice share), so a
player who knows one knows the other. **Honed** opens a temper (**Swift** ×0.8, its holds and burns too, or **Heavy** ×1.25), **Keen**
an edge (**Long**: reach ×1.25, wave ×1.2, ×0.93; **Broad**: +40°, a line ×1.4 wide, a ring ×1.15, fair ×1.3, +2 foes, ×0.9), both
repriced through the model; the first choice is free, changing costs 2 levels. **Peerless** rings its name in gold (a grand banner) and
its scrollable parts can be set down on scrolls (a paper and an Aura Shard each): a master's knowledge passed on, in the world. Records
survive erasing, so writing the same technique again brings its rank back.

**Playing one** (`TechniqueArts`): its voice by release, a stroke's light by stroke (the big shapes as spectacle, the swordsman's own
view keeping a thin low version: a thrust's ground streak, a falling cut's crescent turned about 40° toward the swordsman's back so it
reads from behind, a still ring at a spin's reach), strikes through one `ArtKit.Hits` (`technique_damage` × `damage_scale`, the PvP cap
of 8 a player, stance worn as an art's times its weight), then the intent and the element per foe. Momentum, stance, finishers and
awakening treat it as an art (free while awakened; momentum built as the art slot nearest its worth, `TechniqueRules.momentumSlot`);
its banner names it under "<method> · <rank> technique".

**The writing page.** The Aura page's fourth tab. The Cord is a ring of rune sockets; the writing page borrows its vocabulary (a cord
threaded through three seals, parts as sockets, a lock on an unknown part) but composes left to right like a sentence: stroke, release,
intent, then the readout and a **kata** diagram animating its shape over a grid (foes lit, a wave flying, a burst breaking out, an
afterimage striking), the string composed from the seven swings in the indicator's marks, and the rank strip (temper, edge, inscribe).
The name is inked as it's typed. Everything the server would refuse is said on the page first.

**Settings** (`aura` section): `techniques`, `technique_damage` (0 to 5), `technique_xp_multiplier` (0 to 100), `technique_scroll_chance`
(0 to 10). The client learns whether techniques are on (`Config.Sync.combat` bit 16, `Config.techniques`). Operators:
`/wildercord aura technique learn|forget <part|all>`, `xp <slot> <amount>`, `clear`.

### The bonded blade
**From Edge a swordsman bonds one blade, and it grows with them.** The bond is a ceremony at a place of power; the blade then gathers
**resonance** from every real fight it's used in, rises through four **tiers** (Bonded, Named, Awakened, Soulforged), takes a **name**,
then a **trait** drawn from how it was used, glows in its swordsman's colour (more by tier, in hand and lying on the ground), keeps its
**story**, is kept through death, never breaks, and is only ever its swordsman's. The rules and every number are `aura.BladeRules` (pure,
unit-tested by `BladeRulesTest`); the bond rides on the blade itself (`aura.BladeBond`, the `wildercord:bonded_blade` component); the
world remembers which bonds are live (`aura.BladeRegistry`); the runtime is `aura.BondedBlades`, the ceremonies `aura.BladeCeremony`,
the traits where they act `aura.BladeTraits`; the look is `client.fx.BondGlow`, the tooltip `client.BladeTooltip`, the page
`client.BladePage` (the Aura page's fifth tab).

**Which blades.** Anything in `wildercord:bondable_blades` that wears and stands alone in its slot: `#minecraft:swords`, `#minecraft:axes`,
`#minecraft:spears` and the mace, so aura-forged gear (all swords, axes and spears) bonds too. Not the trident: it's thrown and left
lying, and a blade that leaves the hand by design would make "only ever yours" a fight with the rules every throw. Not bows or tools:
a bonded blade is the one the swordsman fights with.

**The ceremony** (`BladeCeremony`, `BladeRules.BOND_TICKS` 200). Hold the breathing stance at a **ley crossing** (`PowerPlaces`, where
two lines meet; anywhere with `bond_at_power` off) with an unbonded blade in the main hand, from Edge, with no blade bonded: after the
stance settles (`SETTLE_BEFORE` 10 ticks) it begins instead of the crossroads (`Crossroads.breathing` waits while it runs). Three parts:
**kindling** (60 ticks: motes of the aura rise out of the ground along both ley lines into the blade, which kindles from the hilt up),
**joining** (to 140: the two lines (`LeyLines.directions`) light across the ground and run in to the swordsman, a ring turning under
them), **sealing** (to 200: a ring closing in, the blade blazing) and the **seal** (a column of light, a ring racing out, the banner
"Bonded", `aura_bond_seal`, and what it means in chat). The big shapes are spectacle; in first person the lines start a little way out
and the HUD counts it down. Moving, leaving the stance, or the blade leaving the hand breaks it (`aura_bond_fail`). The first bond goes
into the Grimoire (`aura:bond`). Why a ceremony and not a click: the blade is the one thing a swordsman keeps for good; bonding it should
be a place and a moment, and the crossing is already where the stance means something (the Ways' crossroads).

**Tiers** (`THRESHOLD`, `GATE`): Bonded 0, **Named 300**, **Awakened 1200**, **Soulforged 3600** resonance, each also waiting on its
swordsman's stage (Named Edge, Awakened Form, Soulforged Sovereign) and Soulforged on a boss felled by the blade (`SOULFORGED_BOSSES`).
Resonance keeps gathering while a tier waits, so it arrives the moment its swordsman is ready (the page says which it waits on). Each
tier is a moment (`BladeCeremony.tierMoment`: a burst, the body's aura flaring, `aura_bond_tier`, a banner, Soulforged a column) and a
Grimoire entry (`aura:blade_named` and on). What each gives: Named and up draw a little more aura from coated blows (×1.1, Soulforged
×1.2: "it knows the hand", `hitGain`), Awakened its trait, Soulforged the trait half again as strong (`SOULFORGED_TRAIT` 1.5 through
`scaled`) and a free change of trait. The pace (`BladeRulesTest`'s model of an hour of steady fighting: 180 to 320 resonance): Named in
the first evening (0.8 to 2.5 hours), Awakened over a week of play (3 to 8), Soulforged a long road's end (10 to 22, with its
breakthroughs and bosses).

**Resonance** (`BondedBlades.gain`, only with the blade in the main hand, only on worthy foes, scaled by `resonance_gain`): a kill
`KILL` 1 × the foe's worth (`AuraCombat.worth`) × its freshness (`AuraCombat`'s repetition), a boss +30; an art landing (First 0.6,
Second and Third 0.8, Fourth 1, Final 3 on its first foe, 0.2 each for up to three more); a finisher 2 (a boss's 5); a stance broken
0.4; a perfect guard 0.5; an awakening begun 4; a duelist beaten 15; a technique reaching Peerless 25; a breakthrough into Form 120,
Sovereign 240. Everything a foe gives besides its kill is capped per foe (`foeCap`: 4 + 2 × worth, a boss 40), so pounding one patient
foe never grows a blade; a training dummy or the practice room gives nothing; another player's fall counts once a day each
(`PLAYER_KILL_REST`). It's gathered in a buffer and written onto the blade once a second (`flush`), never every hit.

**Its history** (`BladeBond.History`): counts (kills, bosses, strong foes, players, undead, by night, at low health, beside an ally, arts,
finishers, techniques, guards, steps, slashes, stances broken, awakenings, duels), the twelve arts it played most by name (`MAX_ARTS`),
and its last ten notable deeds (`MAX_DEEDS`: a boss, a breakthrough, a tier, a duel, a Peerless technique, a Way chosen, a passing, a
name), with where and when it was bonded (`Origin`) and whose it has been (`lineage`, eight at most). The tooltip and the page tell it.

**Names.** At Named it suggests one of its own (`BladeRules.suggest`), built from its story: a first word from the element of its
swordsman's method (Ember's Cinder, Ash, Pyre...), now and then from their Way, where it was bonded (the biome: Dune, Meadow, the Pines),
what it fells most (Grave, Giant, Moon) or its favourite art's own word (Kindling, Sunfall); then an ending (Cinderwake, Rimesong), a
second word (Ashen Vow, Meteor Oath) or "Oath of the Dunes". Every word is the mod's own; the same blade and the same ask give the same
name; Suggest on the page asks again. Or its swordsman writes one: cleaned exactly as a technique's name (`TechniqueRules.cleanName`, 24
characters, no formatting, controls or invisible marks) and shown everywhere as a literal (`getHoverName` through `ItemStackBondMixin`;
an anvil can't rename it: `BondedBladeSmithingMixin` puts the old name back), so a name can't carry formatting or a click to anyone.

**Traits** (`BladeRules.traits()`, `BladeTraits`): at Awakened the blade offers **three**, the ones its history shows most strongly
(`offer`: each trait's `habit` scores the history 0 to 1, leaned a little by the method (`METHOD_LEAN`) and the Way (`WAY_LEAN`), ties
by the blade's own seed; three that suit any blade if nothing shows). The first choice is free; changing for another it offered costs
10 levels (`RECHOOSE_LEVELS`); at Soulforged it offers again and one change is free. Thirteen:

| Trait | Drawn from | What it does (Awakened; Soulforged ×1.5 of the change) |
|---|---|---|
| **Well-Worn Verse** | its favourite art played 25+ times | that art costs and rests 15% less |
| **Closing Stroke** | 15+ finishers | a finisher gives back half again its aura and 4 more momentum |
| **Sundering Steel** | 25+ stances broken | stances worn 12% harder (half on a player) |
| **Riposte** | 20+ perfect guards | the next coated blow within 2 s of a perfect guard 15% harder (half on a player) |
| **Wind Step** | 30+ Aura Steps | Aura Step 25% cheaper, back 15% sooner |
| **Long Crescent** | 40+ Aura Slashes | Aura Slash 20% cheaper and flies 20% further |
| **Inkbound Steel** | 40+ techniques | techniques 10% cheaper and ranked 20% faster |
| **Second Blaze** | 5+ awakenings | the awakening back a quarter sooner, a quarter less spent after it |
| **Mountainfeller** | 2+ bosses or 15+ strong foes | 10% harder and stances worn 15% faster against bosses and Runebound foes (never a player) |
| **Gravewarden** | 50+ undead, 40% of its kills | coated blows 12% harder against the undead (never a player) |
| **Last Light** | 10+ foes felled below a third of health | below a third of health, 8% less harm |
| **Moonwake** | 60+ foes by night, half its kills | 25% more aura from blows at night |
| **Rallying Steel** | 30+ foes felled beside an ally | each finisher gives allied swordsmen within 10 blocks 5 momentum |

**Balance and PvP fairness** (`Trait.worth`, `pvp`, `BladeRulesTest`): each is worth 2.5% to 4.5% of a swordsman's strength alone, none
more than 1.8 times another; against a player at most 3% and never more than alone. Damage traits stay under +23% at Soulforged
(Riposte and Sundering half on a player, inside the bonus cap: under +12% and +10%), discounts never under two thirds, Last Light never
more than a seventh off. Mountainfeller and Gravewarden never touch a player; Second Blaze gives more awakenings, never a longer one (the
Final Art still once in each). A trait an add-on registers (`AuraApi.registerBladeTrait`, a namespaced id) takes no built-in place; what
it does is the add-on's own. `blade_traits` off: blades still offer and keep their traits, but none acts.

**The look** (`BondGlow`, on the blade itself through `AuraBlade`'s layer hook, in its swordsman's colour): Bonded, a **vein** of light
down the blade's middle beating like a heart (two quick pulses, a rest; quicker while awakened); Named, six **marks** lighting up it one
after another, as if its name ran up the steel; Awakened, its aura **licking off the edges** in flickering tongues; Soulforged, a **ring**
turning about the guard, two sparks winding up the blade and a **corona** round it. In first person all of it is held close, thin and
low in the corner. Lying on the ground: a **pool** of its light under it (wider by tier, a shade of its colour under that by day so it
reads on bright ground), a soft glow turned to whoever looks, motes drifting up, and from a Soulforged one a thin **column** of light to
find it by. In anyone else's hands it's **cold**: a faint grey vein and nothing more. During the ceremony the blade kindles from the hilt.

**Only ever yours** (the decisions, and why):
- **Kept through death**, keepInventory or not, cursed with vanishing or not, on the cursor or in a slot (`BondedBladeDeathMixin`,
  `BondedBladeDropMixin`: the `blade_kept` attachment, copied on death, given back in the slot it was in after respawn, `Aura.AFTER_COPY`).
  Why: losing the blade to a death would make every death a quest to recover it, and the chase would be the whole feature.
- **Never breaks**: wear stops at its last point (`ItemStackBondMixin` on `applyDamage`); it stays notched until mended (Mending, an anvil
  with materials). It can't be eaten by an anvil, a grindstone or the repair recipe as the sacrifice (`BondedBladeSmithingMixin`,
  `BondedBladeMendingMixin`, `BondedBladeRepairMixin`), no crafting recipe takes it as an ingredient (a rune's recipe asks for an
  axe: `BondedBladeIngredientMixin`) and no furnace or stonecutter takes it (`BondedBladeSmeltingMixin`); a smithing table's upgrade
  or forging carries the bond (it copies every component), so a bonded diamond sword made netherite or forged into a Lumenedge is the
  same blade.
- **On the ground** (`ItemEntityBondMixin`): only its swordsman can pick it up (no other player, no mob, no hopper: `HopperBondMixin`), it
  never despawns, burns, melts in lava or breaks in a blast, and out of the world it comes home (`EntityBondBelowWorldMixin`).
- **In a chest**: only its swordsman can take it out (`SlotBondMixin` on `Slot.mayPickup`, every menu, the client agreeing).
- **In anyone else's hands** (inventory, cursor, a shulker box or a bundle inside them: `scan` every tick, `deep` every half second):
  it **slips home** at once, into its swordsman's inventory, or at their feet, or (offline) held by the world until they join
  (`BladeRegistry.homeward`). A stranger holding it has no aura weapon (`Aura.holdsWeapon`) and no trait, and sees it cold. Theft gives
  nothing.
- **No duplicate**: the registry knows the one live copy of each bond (`BladeRegistry`: bond id to owner and whether it's live), and a
  blade is only ever moved, never copied, so there's no recall to race. A second copy (a creative clone) is ended where it's found; a
  released, replaced or passed-on bond's copies lapse to plain steel (`lapse`, `wildercord:former_bond` keeps whose it was).

**Passing it on** (`BladeCeremony`, `PASS_TICKS` 160; step 10's masters and disciples): the master holds the breathing stance with their
blade in hand while a disciple kneels (sneaks) before them, within 2.75 blocks, the two facing each other (`PASS_FACING` 0.5): motes pass
between them, a circle widens round them, the blade's light runs to the disciple, and at the end the blade moves from one inventory to
the other in one go (`BondedBlades.pass`), bonded to the disciple with its tier, name, trait and story, the master in its lineage, a
"passed" deed. Only whom the rules allow (`AuraApi.allowBladePassing`: nobody until step 10 says who), and never to someone already
bonded. A passed blade **sleeps** for a disciple below its tier's stage (`effective`): a faint vein, no gifts, until they grow into it.

**Releasing** (the page, clicked twice): the bond ends; the blade is plain steel that remembers whose it was; another may be bonded.

**The Blade tab** (`BladePage`): before a bond, how to make one (the three steps, whether the blade in hand can be bonded, the ladder of
tiers); bonded, the blade framed in its glow, its name, tier and resonance toward the next and what that waits on, the name to give
(typed, or Suggest), its trait (before Awakened, the habits it reads so far and how strongly; then the three cards with why each is
offered), its story, and Release. Away from it: where it was last with them, and that it's never lost. The tab breathes gold while a
trait waits to be chosen.

**Settings** (`aura` section): `bonded_blades` (off: no new bonds, no resonance, no traits; blades already bonded stay protected),
`resonance_gain` (0 to 100), `bond_at_power`, `blade_traits`. The client learns two (`Config.Sync.combat` bits 32 and 64:
`Config.bonds`, `Config.bladeTraits`). Operators: `/wildercord aura blade` (the report), `bond`, `resonance <amount>`, `tier <1-4>`,
`name <name>`, `trait <id>`, `release`, `pass <player>`.

### The spellblade
From **Edge**, a spell cast **while sneaking** with an aura weapon in hand flows into the blade instead of leaving (the
choice is the sneak: a spell cast standing goes out as usual, sword or not). The next **Aura Slash** within 5 seconds
carries it: the slash takes the place of the spell's first part's shape, and each of its groups that reaches out lands on
the first 4 foes the slash cuts, each at 85% of the one before (On Hit's falloff), or bursts where the slash breaks if it
cut none; groups that act on the caster (Self) go off on the caster as usual, and the first part's own link (Delay, a
condition, Echo...) follows once the slash has flown. Both prices are paid: the spell's mana and cooldown as it's cast (with
everything a cast brings: mastery, affinity, residues), the slash's aura when it's loosed. It runs through the cast engine
(`CastEngine.onHit`), so a Shield, the spell defences, the PvP cap and mastery all have their say. Unused past its time it
slips off and leaves as cast. Secret spells, overchannelled spells (sneak there steadies the charge) and spells that only act
on their caster never ride a blade. While a spell rides it, the blade glows in the spell's colour with two coils of light
winding up it, sparks run off it, and `aura_spellblade` rings as it takes it and again as the slash carries it.

### Aura marks
An elemental aura strike (a coated blow, or the slash) may leave its element's reaction mark, for a mage's spell to set off:
frost leaves frozen (Shatter, Fracture), wind windswept (Wildfire), void shadowed (Blight), blood bleeding (Rupture), arcane
exposed (one of Unweave's marks), fire burning and life a touch of poison (Overload, Elapse). Earth, storm and time leave
none: they set reactions off, and storm's own mark (ionised) is one a Thunder blade would conduct through itself. No method's
mark is set off by its own element. The chance is 15% at Glow and 5% more a stage (35% at Sovereign, times
`mark_chance_multiplier`), the mark lasts 3 seconds (shorter than a spell's), and the same striker marks the same foe again
only after 2 seconds. Another player is never set alight or poisoned by a mark (that would be harm outside the PvP caps);
the marks that do nothing by themselves are left on players as on creatures. Frozen frosts a foe over but never freezes it
solid.

### Experience and breakthroughs
**Experience comes from meaningful melee**, on spell mastery's rules: a full swing on a real foe is worth 0.25, plus 1.0
for the share of its health the blow took, plus 0.5 for the kill (an ordinary monster felled in four swings: about 2.5).
A foe is worth more the stronger it is (the square root of its health over yours, from half to twice); a Runebound at
least 1.5, a boss 2, a player (PvP on) half. The moment multiplies it as for mastery (low health 1.5, a crowd 1.3 or a
horde 1.5, a boss near 1.5, a dungeon 1.25, at most 3); repetition fades it; one blow earns at most 6. Dummies and the
practice arena teach at half rate, 40 in all. Creative players earn nothing. Meaningful fighting earns about 300 an hour:
Flow in about half an hour of play, Edge in about two hours.

Experience fills toward the next stage's threshold and **waits there**: a breakthrough waits for a **trial**. For Flow and
Edge, either:
- **Stillness**: hold the breathing stance unbroken for 30 seconds where ley lines cross (a place of power); or
- **A stronger foe**: fell a boss, a Runebound or a creature with twice your health or more, with your blade (melee and
  the slash) within a minute of your first blow on it, no spell of yours touching it.

Form and Sovereign (about six and fifteen hours of meaningful fighting) ask more, either:
- **The tempest**: hold the breathing stance unbroken at a ley crossing through a thunderstorm, open to the sky: 45 seconds
  for Form, 60 for Sovereign. Lightning that strikes you breaks the stance, as any blow does; or
- **A guardian**: fell a boss (`Spirits.isBoss`: a dungeon's guardian, the Wither, the Warden, the Elder Guardian, the
  Dragon, the Archivist) with your blade within three minutes of your first blow on it, no spell of yours touching it (a
  spell carried on the slash counts as a spell).

A won aura duel against a duelist (`AuraBreakthroughs.DUEL`) is left for the duelists to allow for the stages they teach.

A breakthrough is a moment: a burst of aura in your colour (rings, a column of light, motes), the stage's name as a title,
a rising chord, a full new pool of aura, and a Grimoire entry (150 mana toward your next Heart Circle).

### How it looks
The weapon carries the aura in first and third person, for everyone: at Glow a haze hugging the weapon's own silhouette
(traced from its model) that shimmers along it; at Flow the rim wider and brighter, light running along it from hilt to
tip, and ripples leaving the blade one after another; at Edge a solid, translucent crystal blade, four-faceted, reaching
past the point, bright at its facets and ridge. An empty aura shows only faintly. It's drawn with vanilla's glowing-eyes
type, so it renders the same under shader packs and stays out of their shadows. The stance draws slow
rings at the feet and motes drawn into the body; a breath on the beat, the guard, a perfect guard (the parry's gold), the
sweep, the slash and a breakthrough each have their shaped light. From Form the body wears a faint shell of aura (while
aura armour is up) and a step leaves afterimages, both drawn on every client with vanilla's glowing-eyes and emissive
translucent types, like the blade, so they hold under shader packs. Sounds: `aura_slash`, `aura_guard`,
`aura_perfect_guard`, `aura_breakthrough`, `aura_backlash`, `aura_breath`, `aura_step`, `aura_armour`, `aura_intent`,
`aura_dominion`, `aura_dominion_fade`, `aura_spellblade`, and for sword strings `aura_string_tick`, `aura_string_complete` and
`aura_string_fumble` (the feel kit's `tools/feel/aura.py`).

**One language for every art.** Whatever a swordsman does with aura draws and sounds the same way, so fifty arts read as one
path:
- **Trails.** A coated swing leaves a ribbon of light along the blade's arc in the aura's colour, its shape by the swing (a cut,
  mirrored back in a run; a low sweep across the legs crouching; overhead leaping; a straight lance running, stepping or
  thrusting a spear; an X for a counter; wide and level for a Flow sweep). It grows with the stage: wider, longer, brighter, motes
  from Flow, a bright edge from Edge, an echo from Form, sparks at Sovereign. Never at a block being dug.
- **The body's aura**, by stage: Glow a faint shimmer, Flow wisps from the shoulders and the blade, Edge a haze and a glow
  underfoot, Form a mantle from the shoulders, Sovereign a corona of flame round the whole body and glowing eyes. Calm (35%)
  out of a fight, flaring (80%) for five seconds after any blow given or taken, surging higher as an art goes off; dim while
  aura is too low to coat.
- **Impacts.** A coated blow flashes where it met the foe, by its weight: a light one (a half swing) just a flash; a full one
  sparks and holds the striker's and a struck player's view for 45 ms with a slight nudge of the camera; a heavy one (a
  critical, the slash, the arts) 70 ms and a ring; a grand one (the Final Art) 110 ms and an echo ring.
- **Banners.** An art's name slides in at the left edge of the swordsman's own screen (a third of the way down, never mid-view)
  under its kicker (the method, and which art), in the method's colour; others see it small over the swordsman's head.
- **Sounds.** Each method has its own swing, impact and technique sounds (Ember roars and crackles, Rime rings like ice, Thunder
  snaps, Gale whistles, Stone thuds, Verdant rustles, Hollow pulls, Starlit chimes, Hourglass ticks, Crimson beats; plain steel
  for a method without).
- **Your own view.** In first person your own effects are thin, short and low: a trail a third as wide, half as bright, part
  of its arc only, low and toward the blade hand, tipped with the look and fading out before the middle of the view; your own
  impacts a small flash; a perfect guard a thin gold arc low in the view; no body aura on yourself but a faint band at the bottom
  edge while it flares. Third person and everyone else get the whole spectacle.
- **Settings** (Magic visual settings): blade trails full, subtle (plain ribbons, and others' ordinary swings left out;
  techniques always show) or off; body aura full, calm or off; impact full, soft (half the hit-stop) or off; banners all, your
  own or off; camera motion off also stops the nudge; reduced flash halves the flashes. The performance preset sets subtle,
  calm, soft and your own.

### Fairness
- Aura blows are melee, so armour applies to them as to any blow; only the Edge's quarter goes through.
- Against another player a blow's aura bonuses (the coat, the element) are one factor under the spell-defence cap
  (`defence.max_bonus`), then scaled by `aura.pvp_scale` (0.6).
- **Projected** aura (the slash and a Thunder spark) is the damage type `wildercord:aura`; against players it goes
  through the spell defences (`cast.SpellDefence`): armour, Warding and Warded, the cap, the PvP scale and the spellguard.
- The top stages stay inside the same caps: Intent on a player is a vignette and a 5% slow, and only from a higher stage;
  Dominion's weakening of a player is times the PvP scale and its slow is Slowness I; its chain onto a player is projected
  aura (the spell defences); a carried spell is a spell (Shields, the spell defences, the PvP cap, the spellguard).
- The server's `aura` section: `enabled`, `xp_multiplier`, `gain_multiplier`, `coat_bonus`, `damage_scale`,
  `slash_damage`, `slash_cost`, `slash_cooldown_seconds`, `pvp_scale`, `backlash_seconds` and `guard_share`; and for the top
  stages, the spellblade and marks `step_cost`, `step_cooldown_seconds`, `step_distance`, `armour_share`, `intent_pvp`,
  `intent_pvp_slow`, `dominion_cost`, `dominion_seconds`, `dominion_cooldown_seconds`, `dominion_weaken`,
  `spellblade_seconds` and `mark_chance_multiplier`; and for sword strings `strings` and `string_window_seconds`.
- Sword strings' arts are projected aura (the spell defences and the PvP scale against a player), never spend past empty, and
  are checked by the server against swings it saw itself.

### The world of aura
Swordsmen to meet and learn from, swordsmen to fear, gear forged with aura, and what happens when two slashes meet. The
pure rules and every number are `aura.world.AuraWorldRules`; the server's `aura_world` settings change the main ones.

**Wandering duelists.** A sword master in a travelling cloak with a scabbard at the left hip, one look per breathing method
(the cloak's dye, its trim, the sash, the mask and the scabbard's tassel in the method's colours; its sash's emblem glows
softly in its aura). It harms nobody, nothing harms it outside a duel (a blow is turned aside by the sheathed blade, and
fire, falls and drowning pass it by), and after twenty minutes it moves on in a swirl of its aura.
- **Where**: by day in the overworld, once a minute a 3% chance (times `duelist_spawn_rate`) near a random player: a few
  blocks from the nearest village bell within 96, else on a dirt path 16 to 48 blocks off, else a **small camp** on open
  ground 20 to 40 blocks off, where it lights a campfire (a borrowed block, written down so it goes when the duelist
  does, drops nothing, and is only lit where mobs may change the world and `duelist_camps` allows) and sits by it while
  nobody is close. Never within 160 blocks of another duelist, never more than `max_duelists` (2) loaded. About one an
  hour of daylight for a player out in the world.
- **The duel**: use it and it offers (its method named, the stage it will fight at); use it again within ten seconds to
  accept. A blade in hand is needed. It's the player duel's rules (`duel.DuelRules`): a 3-second countdown in circles of
  light, a 40-block arena, a draw after five minutes, leaving or logging off forfeits. One player and one duelist: the
  duelist cuts nobody else and takes harm from nobody else, and another player striking the challenger calls it off.
- **It meets you at your own stage** (`duelStage`: yours, at least Glow, at most the highest stage open) and fights with
  what that stage brings:

  | Its stage | Health | Its blade | Blows every | What it adds |
  |---|---|---|---|---|
  | Glow | 30 | iron sword | 26 ticks | coated blows (+10%) |
  | Flow | 40 | iron sword | 24 ticks | a guard: raised after a blow lands on it (35% of the time), or now and then when you're close; its first 7 ticks are a perfect guard that turns a blow whole and staggers the striker (thrown back, Slowness II and Weakness for 1.5 s); held it halves a blow; 30 ticks, then it rests 2 s; an axe breaks it |
  | Edge | 50 | diamond sword | 22 ticks | the slash: its blade raised for 18 ticks (the tell; the aim fixes 6 ticks before), a crescent at 1.3 blocks a tick for 12 blocks, its weapon's damage × 0.8, every 5 s, when you're 5.5 to 10 blocks off; guard 45% |
  | Form | 60 | netherite sword | 20 ticks | slash × 0.9 every 4¼ s (16-tick tell); a dash once Aura Step exists: a 6-tick crouch, then 4 blocks at you, every 4 s |
  | Sovereign | 70 | netherite sword | 18 ticks | slash × 1.0 every 3½ s (14-tick tell) |

  Its slash is projected aura through the spell defences, as any monster's magic is. A player's perfect guard staggers it
  (it can't act until the stagger passes) and sends its crescent back.
- **Nobody dies.** A blow that would leave the duelist at 15% of its health or less makes it **yield**, kneeling. A
  challenger brought down by it is **knocked out** on one health. Either way the challenger is put back as they began:
  the health it took given back, the harm it left (its stagger) taken off.
- **Winning**: its method taught outright to a challenger with none (`AuraApi.grantMethod`, source `duelist`); to one who
  already breathes another way, its manual handed over, so switching stays their choice. Then 20 + 15 × its stage aura
  experience (35 at Glow, 65 at Edge), the Grimoire entry `aura:duelist` (a toast, 150 mana), and, if no magic of theirs
  touched it, the **duel trial** (`duel`), allowed for the breakthroughs into Form and Sovereign. It bows and goes.
  **Losing** costs nothing; it stays, resting 30 s before another challenge.

**Fallen knights.** Old plate a swordsman's aura still walks in: a closed helm with a glowing visor slit and a ragged
crest, pauldrons, a torn tabard, a rag of a cloak; the visor, the cracks across its breastplate and its joints glow a dim,
smoky version of its method's colour, and its drawn blade carries a dim Edge of aura.
- **Where**: every 10 s, a 12% chance (times `knight_spawn_rate`) near each player standing in a place in the
  `#wildercord:knight_haunts` structure tag (strongholds, ancient cities, the expeditions) or within 10 blocks of a
  dungeon's spawner on cobblestone: 8 to 20 blocks off, in the dark (light 9 or less), never within 7 blocks of a player,
  inside the same place, never with `max_knights_nearby` (2) within 48 blocks, never on Peaceful.
- **Rank** follows the place: a dungeon 1, a stronghold 2, an ancient city or an expedition 3. Health 40/50/60, armour
  8/10/12, an iron sword (a diamond one at rank 3), its slash 5/6/7 (before the game's difficulty scaling: 4/6/9 on rank
  2), its tell 20/18/16 ticks, every 7/6/5 s. Its method is drawn by the place's manual weights (an ancient city's Hollow,
  Rime and Crimson), any method in a dungeon.
- **The slash** (5 to 13 blocks off, in sight): blade raised high, its visor flaring and a rising hum; 6 ticks before it
  swings, a line of light along the ground marks the crescent's way. A crescent at 1.1 blocks a tick for 14 blocks, 3
  wide, through the spell defences. After it the knight is **open** for a second (no guard). Answers: step off the line,
  meet it with your own slash (a clash), or turn it back with a perfect guard.
- **The guard** (when you're within 4 blocks and its blow rests, 30% each half second; 40% right after it's struck): as
  the duelist's. Answers: wait out its 1.5 s, strike from the side or behind (no guard there), or **break it with an axe**:
  it reels for 2½ s, taking a quarter more.
- **Drops** (its loot table, `entities/fallen_knight`): a **Manual Page** of its method always (one more by luck with
  Looting; the method from the loot function `wildercord:knight_method`), an **Aura Shard** a time in four (killed by a
  player; +8% a Looting level), a few iron nuggets; 15 experience.

**Manual pages** bind four of one method and a book into that method's Breathing Manual (`wildercord:manual_pages`, a
shapeless recipe that only matches pages of one method). About three knights of one method make a manual.

**Aura-forged gear.** A smithing table forges a diamond or netherite weapon with an **Aura Shard** (the template slot)
and a reagent of the world: the weapon keeps its tier, enchantments, damage and durability (the forging is the
`wildercord:aura_forged` component, with its own look through `minecraft:item_model`, its name and rare rarity). Each does
one thing for aura:

| Forging | From | Reagent | Does |
|---|---|---|---|
| **Lumenedge** | diamond or netherite sword | Lumen Antler | a blow's aura × 1.5 |
| **Skyrend Glaive** | diamond or netherite spear | Fulgurite Shard | Aura Slash × 1.6, flying 30% further and wider, through 2 more foes |
| **Bulwark Maul** | diamond or netherite axe | Geode Grit | Aura Guard (raised and held) costs × 0.6 |

The glaive's 1.6 makes up a spear's lower damage: a netherite spear's slash (5 × 1.2 × 1.6 = 9.6) matches a netherite
sword's (8 × 1.2), further and wider. Its bonus counts with the element under the spell-defence cap against players.

**The Breath Sash** is casting gear for the gear tray's tome slot (or held in the off-hand with that slot empty): aura
capacity × 1.25 (Glow 25, Flow 50, Edge 87), the breathing stance settling in 10 ticks instead of 20, and the stance's
breath (steady and on the beat) × 1.5. A swordsman's alternative to the Tome of the Fifth Page. Crafted from four white
wool, two string and an Aura Shard.

**Aura clash.** Every crescent in flight (a player's, a duelist's, a knight's, one sent back) moves first each tick; two of
different owners whose fronts pass within 0.6 × their mean width + 0.4 blocks on the way break together: a white flash,
a ring of each colour, a ring along the ground, sparks; creatures within 2.5 blocks are shoved back; nobody is harmed. A
player's first clash goes into the Grimoire (`aura:clash`).

**Guard and slash, and the PvP review.**
- A held guard (a player's or a mob's) facing a slash takes its share off it and **stops** it: nothing behind the guard is
  cut. A perfect guard **sends it back** at whoever loosed it, as the guard's own, a little faster; it no longer staggers a
  slasher from across a field. A Thunder spark meeting a perfect guard is simply turned.
- Numbers against a player (defaults, before armour): a netherite sword's slash 8 × 1.2 × 0.6 = 5.8 (a diamond one 5.0), a
  Skyrend netherite glaive's 5 × 1.2 × 1.6 × 0.6 = 5.8, every 2 s for 12 aura; a coated netherite blow 8 × (1 + 0.1 × 0.6)
  = 8.5. Five slashes empty an Edge pool (70), so a slash-only fight runs dry in 10 s and backlashes; the guard halves a
  slash at 0.6 aura a point it takes off, the perfect guard reflects it, two slashes clash. Everything aura adds sits under
  `defence.max_bonus` and is scaled by `aura.pvp_scale`, so no forging takes a slash past the cap.

**How it looks**: the duelist's and knight's models are hand-built humanoids (`DuelistModel`, `FallenKnightModel`) on skins
painted by `tools/aura_world_art.py`; their glow layers and the aura on their blades (the player's own `AuraBlade`, from
their render state) are vanilla's glowing-eyes pass, so shader packs draw them. The slash by day: under its crescents of
light a rim of shadow (the mod's darkness, as void magic is drawn) a little wider than its halo, and a brighter edge.
Against a bright sky added light alone washes out; the darkness gives it an edge, and takes nothing from a dark sky.
Sounds: the feel kit's `tools/feel/duelist.py` (`duelist_challenge`, `_sheathe`, `_bow`, `_yield`, `_clash`, and the
knight's `duelist_knight_ambient`, `_hurt`, `_death`, `_windup`, `_step`).

**Server settings** (`aura_world`): `duelists`, `duelist_spawn_rate` (0 to 4), `max_duelists` (0 to 16), `duelist_camps`,
`knights`, `knight_spawn_rate` (0 to 4), `max_knights_nearby` (0 to 8), `forged_gear`, `lumenedge_gain` (1 to 4),
`skyrend_slash` (1 to 4), `bulwark_guard_cost` (0 to 1), `sash_capacity` (1 to 3).

## Passives

Up to two always-on spells, threaded on the Cord screen's **Passives** page. Slots open at the 1st and 5th Circle.

- **No cooldown, no cost per cast:** a passive costs mana every second instead, 0.12 × its cost (Thrift and Archmage lower it). The HUD shows the total drain; the Passives page compares it to your regeneration.
- **Faltering:** without the mana for a second's upkeep, a passive stops renewing until you have it again.
- **Renewal:** a Self passive re-applies every 2 seconds, quietly (no particles after the first time). An Orbit passive restarts whenever its orbs run out, and stops the moment it's switched off.
- **Nothing outlasts it:** whatever a passive's cast sets lasts 15 seconds at most (`Passives.EFFECT_TICKS`, above Night Vision's 10 seconds of flicker), so switching one on for a second can't bank a minute of its buff. The same effect from anywhere else keeps its own length.
- **Each passive has an on/off switch.** Two runes each, on any Cord (a passive is a lasting buff or an aura, not a whole spell); so at most four runes are ever active.
- **Only sustainable runes, so it isn't broken:**
  - Shapes: Self or Orbit.
  - Buffs: Feather Fall, Swift, Night Eye, Haste, Regrowth, Stoneskin, Empower, Fireward, Tidebreath, Leap, Reflect, Overdrive, Anchor, Frostward, Cushion, Searing Edge.
  - Auras (only with Orbit): Harm, Shock, Fire, Frost, Chill, Venom, Dismantle, Ripple, Aftershock, Push, Ember, Icicle, Pelt, Windcut, Umbra.
  - Modifiers: Amplify, Extend, Frugal, Widen, Focus, Quicken.
  - Never: heals, Shield, Barrier (absorption), Brace, Reversal, Foresight, Infinity and Accelerate (a permanent bubble and a permanent tempo), summons, links, big area damage, Stasis.

Examples: `Self · Anchor` (immovable, about 0.5 mana/s), `Orbit · Shock` (a crackling guard), `Orbit · Dismantle` (orbs that cut whatever comes near).

## Spell enchantments

Cords take four more enchantments, from the enchanting table, anvils, villagers and loot:

| Enchantment | Levels | Does |
|---|---|---|
| Potency | I-III | Spells hit 8% harder per level |
| Celerity | I-III | Cooldowns 8% shorter per level |
| Thrift | I-III | Spells and passives cost 7% less mana per level |
| Persistence | I-II | Spell effects last 20% longer per level |

The HUD and the Cord screen always show costs and cooldowns after these (and the Heart's) discounts.

### Finding runes in the Cord screen
- **Search:** just start typing (or Ctrl+F). Several words must all match, against name, element, category and family. Words of 5+ letters also match descriptions. Esc clears.
- **Families and categories:** pick a family tab, then a category chip.
  - Shapes: Personal, Direct, Projectile, Area, Lingering
  - Effects: Damage, Control, Support, Movement, Time, World, Summon, Innate
  - Modifiers: Power, Area, Timing, Projectile
  - Links: Timing, Trigger, Reactive, Condition
- **Codex rows:** the Codex lists one row per category, labelled on the left.
- **Only what you know:** a category chip appears once you've learned at least one rune in it, so a new player never sees an empty Summon chip.

### Element reactions
Effects leave short marks on what they hit. A later effect of the right element that meets a mark sets off a reaction, and the caster sees its name.

| Reaction | Needs | Result |
|---|---|---|
| **Shatter** | Fire damage (Fire, Explode, Meteor) on a target frozen by Frost or Freeze | +60% damage, the ice bursts |
| **Conduct** | Storm damage (Shock, Lightning, Ripple, Thunderbird) on a wet target: in water or rain, or still dripping from Bubble, Tidebreath or steam | +50% damage, arcs to two more enemies |
| **Wildfire** | Fire on a target just thrown by wind (Push, Launch, Dash, Levitate, Tremor) | Flames spread to every enemy within 3 blocks |
| **Implode** | Explode or Meteor where enemies were just pulled (Pull, Gravity Well) | Blast 50% wider and 30% stronger |
| **Collapse** | Repel on enemies just pulled (Pull, Gravity Well, Hollow) | Double damage, a violent burst |
| **Overload** | Storm damage (as for Conduct) on a burning target | +30% damage; the flames burst for 4 on every other enemy within 3 blocks, throwing them back; the fire goes out |
| **Fracture** | Any earth damage on a frozen target | +40% damage, it thaws, and it's left **cracked** for 5 s: every spell hits it 20% harder |
| **Blight** | Any life damage (Venom, Bramble's thorns, Vinelash...) on a **shadowed** target: Hex, Blind, Wither, Blackflame, Umbra, Echolocate, Resonant Shriek, Hush, Eclipse, Entropy | Rot bursts out: 3 damage and Poison I (5 s) to it and up to 5 enemies within 4 blocks; the caster heals 1 for each (once a second at most) |
| **Unweave** | Any arcane damage (Harm, Smite, Resonance...) on a target with two marks or more | Every mark undone: +30% damage for each, at most four |
| **Rupture** | Any wind damage (Windcut, Cyclone, Repel...) on a **bleeding** target: Bleed, Rend, Cleave, Dismantle, Gash, Crimson Mist, Bonespur | +50% damage, 4 more through armour, and the caster heals 2 (once a second at most) |
| **Elapse** | Any time damage (Countdown, Reckoning) on a target that's burning, poisoned or withering | All the damage still to come lands at once, half again (3 to 16); the fire, poison and withering end |

So every element takes part: fire, storm, earth, life, arcane, wind and time set reactions off; frost, wind,
void, blood, fire (burning) and life (poison) leave what they need. Two can want the same mark (frozen:
Shatter or Fracture; burning: Overload or Elapse), and the first to reach it uses it. Numbers are in
`spell.ReactionRules`; a rune's tooltip says which mark it leaves or which newer reaction its damage sets off.

Try `Bolt · Frost · Delay · Bolt · Fire` for Shatter, `Zone · Gravity Well · Delay · Burst · Explode` for Implode,
`Bolt · Chill · Pelt` for Fracture or `Bolt · Rend · Windcut` for Rupture.

**Creature affinities.** Creatures can be weak to an element (+50%), resist one (half) or, rarely, be
immune: the Nether's creatures resist fire and fear frost, the cold's the reverse, the undead burn under
life magic, golems conduct storm, the End's creatures resist void and fear time, and each boss has its
own. A Runebound resists its Cord's element, and a reaction set off on a creature breaks through its
resistance (so the Cinder Warden still yields to Shatter). A weakness struck flashes "Weak!" over the
creature and goes into the Grimoire's Bestiary. The table is entity type tags, so a datapack can change it.

**Elemental climate.** Where you cast nudges the elements, by 10 to 25%: fire +20% and frost -25% in the
Nether, void +20% in the End, storm +25% under a thunderstorm, frost +20% and fire -10% in the snow, fire
+15% and frost -10% in hot dry lands, fire -10% in the rain, void +10% at night under the sky, life +10%
in sunlight, earth +15% deep underground, arcane +15% on a ley line or under a mana storm. The HUD marks
the elements favoured and hindered where you stand. Details in [features/affinities.md](features/affinities.md).

**Wet.** Being wet (in water or rain, or for 5 s after Tidebreath, steam or a popped Bubble) makes storm
conduct (above), fire hit 25% softer (and dries you), and frost freeze you solid at once.

**The world reacts too.** Harmful spells also change the ground they land on: fire lights grass,
leaves, candles and TNT and boils puddles into blinding steam, frost freezes water to walk on, cools
lava into a crust that melts back and puts fires out, storm runs through water to every foe in it,
scrapes copper and pulses lightning rods (and may charge a creeper), wind knocks arrows out of the
air, earth heaves the ground, life makes it bloom and cures a weakened zombie villager, void draws
loose items in and anchors endermen, time ripens crops, weathers copper, grows up young animals and
hurries furnaces, arcane shows the invisible, and blood feeds nether wart. The numbers are in
[features/world-magic.md](features/world-magic.md).

## Getting runes

**Blank Rune:** 4 cobblestone + 1 lapis lazuli → 4 Blank Runes. **Every Tier I-III rune can be
crafted:** a Blank Rune, its themed items, and a cost that grows with the tier (Tier II adds 2 Lapis
Lazuli and a Gold Ingot, Tier III a Mana Crystal and a Diamond). **Tier IV runes are found only**,
from bosses and rare chests. All recipes appear in the crafting recipe book once you hold a Blank
Rune, and every rune's tooltip says how to craft it and where it's found. The full list, generated
from the game's data, is in [RECIPES.md](RECIPES.md).

Chests share out their rune chance by tier: next to each other in a pool, a Tier I rune is 8x as
likely as a Tier IV one (Tier II 5x, Tier III 2x). The table below covers the first runes;
RECIPES.md has every one.

| Rune | Where from |
|---|---|
| Self, Bolt, Push | Learned the first time you wear a Cord |
| Touch | Blank + leather |
| Feather Fall | Blank + 2 feathers |
| Swift | Blank + 2 sugar |
| Night Eye | Blank + glow berries |
| Heal | Blank + glistering melon slice |
| Harm | Blank + fermented spider eye |
| Light | Blank + 2 torches |
| Grow | Blank + 2 bone meal |
| Amplify | Blank + gold ingot |
| Extend | Blank + 2 redstone |
| Delay | Blank + clock |
| Beam | Blank + spyglass |
| Burst | Blank + 2 gunpowder |
| Shield | Blank + shield |
| Launch | Blank + wind charge |
| Dash | Blank + rabbit's foot |
| Pull | Blank + fishing rod |
| Fire | Blank + blaze powder |
| Frost | Blank + powder snow bucket (you keep the bucket) |
| Break | Blank + iron pickaxe |
| Widen | Blank + 2 amethyst shards |
| Quicken | Blank + breeze rod |
| Pierce | Blank + 2 arrows |
| Bounce | Blank + slime block |
| On Hit | Blank + target block |
| On Land | Blank + hay bale |
| Echo | Blank + 2 echo shards |
| Zone | Tier III: Blank + redstone block. Also found in trial chamber vaults (ominous vaults more often) and ancient cities |
| Split | Tier III: Blank + 2 prismarine crystals. Also found in trial chamber vaults and ancient cities |
| Chain | Tier III: Blank + iron chain + redstone. Also found in trial chamber vaults and ancient cities |
| Rain | Tier III: Blank + pointed dripstone + water bucket (you keep the bucket). Also found in end cities and stronghold libraries |
| Homing | Tier III: Blank + compass. Also found in end cities and stronghold libraries, and shulkers drop it 5% of the time |
| On Kill | Tier III: Blank + bone block. Also found in bastions and woodland mansions |
| Explode | Tier III: Blank + TNT + fire charge. Also found in bastions and desert pyramids, and creepers drop it 1% of the time |
| Blink | Tier III: Blank + ender pearl + chorus fruit. Also found in end cities, and endermen drop it 2% of the time |
| **Lightning** | Tier III: Blank + copper block + glowstone. Or a **world event:** drop a Blank Rune next to a lightning rod in a thunderstorm; when the rod is struck, the blank becomes a Lightning rune. Also found in trail ruins |
| Sonic Boom | The Warden always drops one |
| Wither | The Wither always drops one (and Hollow half the time) |
| Dragon Breath | The Ender Dragon drops one (and an Infinity rune) at the feet of whoever killed it, every kill |
| Summon | Evokers drop it 15% of the time |

Every Tier III recipe above also takes a Mana Crystal and a Diamond. Shipwrecks, buried treasure,
dungeons and mineshafts roll the crafted Tier I–II runes, so exploring always pays. Wandering
traders sometimes sell one.

### Runes of the world

Fifty-three more runes can't be crafted at all, whatever their tier: each is found only in its own
places (`spell/RuneSources.java`), so exploring is how a spellbook grows. Vanilla structures' chests
roll their own (an ancient city's Echolocate and Resonant Shriek, a desert pyramid's Sandstorm, a
jungle temple's Vinelash and Snare, an ocean monument's Elder Guardians' Tidecall...); biomes give
theirs by **Attunement** (meditate with a Blank Rune in hand in the right land at the right moment
for 20 seconds: a cherry grove under a full moon gives Moonpetal, the deep dark beside sculk gives
Hush; see `spell/Attunements.java`); Wildercord's dungeons, their bosses and world events have
theirs; and Archive libraries and Runebound Adepts now and then carry a few. The Grimoire lists
attunements (as riddles until found) and every rune of the world by where it's found. The full
list is in [features/new-runes.md](features/new-runes.md).

### Fishing

A rod finds runes too, always in open water (vanilla's rule for treasure), so fishing is a quiet way
to fill a spellbook between adventures:

- **Treasure.** About 4 treasure catches in 11 are a rune, 1 in 11 a Torn Page (they join vanilla's
  single-item treasure pool; Luck of the Sea makes treasure likelier). The runes: 21 crafted runes of
  water, frost and storm and a few a fisher is glad of (Tier I-III, weighted by tier), and two runes
  found nowhere else, at three times the weight of their tier.
- **Magic waters.** A rune tangled in the line on top of the catch: 12% under a mana storm, 5% on or
  near a ley line, 5% in a thunderstorm at the bobber, adding up to 20% at most. The two fishing runes
  weigh six times their tier here, so storms are the time to hunt them.
- **Tidehook** (Tier II frost, 9 mana; Amplify, Linger): a hook of water reels each target in to your
  feet in three tugs, 4 damage, soaked (storm then Conducts). A pull that brings the enemy to you,
  where Pull and Tidecall drag toward the spell. It soaks rather than freezes (no ice where it lands).
- **Current** (Tier II frost movement, 6 mana; Amplify): only in water or rain, a surge of about 15
  blocks the way you look, and no fall damage until you land; dry, it fizzles. Dash works anywhere,
  but water drags it to nothing: Current is the water mage's way to cross a lake, leap a waterfall or
  ride a storm.
- The first rune fished up earns the feat **Reeled In**.

The numbers are in `content/FishingRules.java` and [features/new-runes.md](features/new-runes.md#fishing).

## Fusion Altar

A crafted block (4 amethyst blocks + 1 lodestone + 4 deepslate tiles). Put runes
in, get a stronger rune out. It works out which of the three fusions you mean from
what you put in.

### 1. Upgrade — three of the same rune → the next rank

- Ranks I → II → III give +25% and then +50% power, at the same mana cost.
- The fusion costs 2 XP levels for rank II and 5 for rank III.
- Learning a higher-rank rune upgrades that rune everywhere in your spells.
- Only effects with power have ranks. For effects measured in levels (Swift's
  Speed, Haste, Regrowth...) and mining tiers, rank III counts as one Amplify.
- Three copies at the same rank make the next one, so rank III takes nine runes.

### 2. Combine — two effects of the right elements → a new effect

Costs 1 amethyst shard and 3 XP levels. The first time you make a new fusion it is
recorded in your Codex. Fusions you haven't found yet show as `??? + ???` with
element hints.

| Elements | Result | Does |
|---|---|---|
| fire + wind | **Firestorm** | Sets alight for 6 s, deals 4 damage, and the fire spreads to enemies within 2 blocks |
| fire + frost | **Steam** | 4 damage and Blindness for 3 s |
| fire + earth | **Magma** | The ground under the target burns: 2 damage per second for 4 s to enemies standing on it |
| storm + wind | **Tempest** | A lightning strike (8 damage) plus a huge knockback, and a second bolt (4) where it lands |
| storm + fire | **Plasma** | 10 damage, half of it ignoring armour, and the target is ionised (conducts as if wet) |
| storm + frost | **Hail** | Three 2-damage hits and Slowness II |
| frost + earth | **Glacier** | Frozen in place for 2 s (1 s on players) |
| life + void | **Lifesteal** | 5 damage, and you heal for what it dealt |
| wind + void | **Warp** | Swaps places with the target; an enemy is left with Slowness II for 2 s |
| life + earth | **Bloom** | Regeneration II for 6 s to allies, and plants grow around the first 3 of them |
| life + storm | **Surge** | Allies get Speed I and Strength I for 8 s, and their blows arc on to a neighbour (4 times) |
| arcane + void | **Nullify** | Strips an enemy's good effects, or an ally's bad effects |

Recipes match on **element tags**, not specific runes. If an add-on adds a
"Magma Bolt" effect tagged `fire`, it automatically works in every fire fusion.
Fused runes are Tier III effects that can't be crafted or found. Their elements:
Firestorm and Steam are fire, Magma earth, Tempest, Plasma and Surge storm, Hail
and Glacier frost, Lifesteal blood, Warp void, Bloom life and Nullify arcane.

#### Signature fusions — two particular effects → a rune of their own

Sixteen pairs of *particular* runes have a signature fusion, checked before the
element recipes: Chill with Shock makes **Frostwire**, while any other frost and
storm effects still make Hail. Same shard, same 3 XP levels, same rank rule (the
lower of the two). Each is what its two runes do at once, and each pair has
elements no other signature shares, so every signature's circle braids its own
pairing (with a star added, to tell it from the element fusion). The ingredients
are always runes a caster can come by: crafted ones mostly, four pairs with runes
of the world or Tier IV runes, three of them for grander results (Doomclock,
Cometfall and Dust Devil are Tier IV). The Grimoire lists them apart, as `??? + ???` with their two
runes' elements until found, and two advancements count them (the first, and
five). The full list is in [features/fusion-altar.md](features/fusion-altar.md).

### 3. Tie a Knot — a whole spell → one rune

- Put in 1 Blank Rune + 1 string, pick one of your spells, and it becomes a
  **Knot**.
- It costs 1 XP level per rune inside (minimum 2).
- A Knot takes **one socket** and costs 10% less mana than its contents.
- Knots can hold Knots, up to 2 deep, so you can nest spells inside spells.
- **Anyone can learn a Knot,** even without knowing the runes inside. Knots are
  how players share and trade signature spells.
- The tooltip shows the whole sequence inside.
- A Knot is sealed: modifiers inside it change only its own runes, and modifiers
  outside can't reach in. Its tier is the highest tier inside, so a Cord's tier
  limit still applies. A Knot inside a Knot is discounted twice (19% in all).
- A spell with Imbue can't be tied, and Knots can't be passives.

## Controls

| Key (rebindable) | Action |
|---|---|
| **R** | Cast the selected spell. Hold to charge it, then release; hold past full to overchannel, release on the beat |
| **Sneak** (while charging) | Steady the hands: the camera holds still and the mouse traces the spell's glyph |
| **V** | Tap: select the next spell. Hold: the spell wheel; point at a spell and let go to select it. Let go without pointing and it stays open until you click a spell, press `V` or Enter, press a number, or press Esc |
| **K** | Open the Cord screen |
| **Z** | Aura: tap for Aura Slash (Edge); sneak and press for Aura Guard (Flow). A double tap and a hold are kept for the next wave's techniques |
| (unbound) | Cast spell 1 / 2 / 3 / 4 directly |

### Cord screen

- **Left:** your Codex. It has filter tabs (All / Shape / Effect / Modifier /
  Link; learned Knots are listed under All) and a search box.
- **Right:** your spells, one row of sockets each. You can rename them.
- **Editing:** click a rune in the Codex to add it to the end of the selected spell,
  and click a threaded rune to take it out. Drag a rune onto a socket to insert it
  there, drag threaded runes to reorder them, or drag one off the Cord to remove it.
- **Bottom readout:** the plain-English meaning of the spell, its mana cost and
  cooldown, and warnings. For example: "Fires 3 bolts. On hit: lightning. 46 mana
  · 2.3 s", or "Pierce does nothing here — nothing it can change to its left."
- **Codex tab:** every learned rune with its description, plus discovered
  fusions.

### HUD

- A slim mana bar with a cooldown ring, and the selected spell's name and icons,
  above the hotbar on the right.
- After the spell's name, a small mark for each element the elemental climate favours (▲) or
  hinders (▼) where you stand; for a few seconds after something new comes into force, lines over the
  panel say why ("Full moon: Arcane +15%, Void +15%").
- Each element has its own particle colour and cast sound.
- Once you've learned a breathing method, a slim aura bar on top of the spell panel (or alone in its place without a
  Cord): a diamond for each stage (the next pulses gold while a breakthrough waits), the aura held in the method's colour
  with a gold mark at Aura Slash's price, and in the breathing stance a ring closing on the diamonds with each breath.

## Server rules and safety

- **Friendly fire:** harmful effects never hit you, your scoreboard team or your
  tamed pets.
- **PvP:** with PvP off, harmful effects never affect other players or their pets. With PvP on, rune
  damage to players is ×0.6. *(Planned: crowd-control such as Frost, Pull and Launch
  lasting half as long on players.)*
- **Spell defence (players only):** every spell hit on a player, whoever cast it, goes through
  `SpellDefence`. A hit's bonuses (Execute, reactions, hexes, backstabs, and what an overchannel, a release on
  the beat or a traced glyph added to the cast) multiply to ×2.5 at most;
  armour counts at 55% of its worth against spells that bypass armour; Warding (armour enchantment,
  I-IV, 2 protection points a level, sharing vanilla's 20-point cap with Protection and exclusive
  with it) and Warded (the Potion of Warding, 20% a level) take their share; and the spellguard stops
  one hit taking a player from 80%+ health to dead (one heart left, recharges in 60 s). See the
  `defence` settings in [features/gear-config-api.md](features/gear-config-api.md).
- **Hard caps per cast:** 64 creatures, 32 blocks, 24 live projectiles per player,
  8 links deep, and Knots 2 deep. Nobody can crash a server with
  `Split · Split · Echo · Echo · Echo`.
- **Blocks:** World effects (Break, Excavate, Grow, Harvest, Icepath, Light,
  Rampart, Chisel, Glimmer, Prune, Tunnel, Vein, Smelt, Fell, Span) change blocks, and so do
  the elements where they land (see [world-magic.md](features/world-magic.md)), but only in
  vanilla's own temporary or natural ways: fire (only where fire spreads, so it burns out),
  frosted ice (melts back), a crust of basalt on lava (melts back, saved with the world, drops
  nothing), grass and flowers, growth, and copper weathering or being scraped. Every change needs
  the caster to be allowed to build there (spawn protection and claims are respected) and comes
  out of the cast's block budget, 24 world changes a cast at most. Monsters' spells never change
  blocks (they still shock through water, heave the ground as block displays and blow arrows
  away), and never charge creepers, cure zombie villagers or grow up young animals.
- **Residues** take only natural ground or open air over it, never anything built, placed or holding
  contents, ask a spell's own permission (a boss's: `spells_edit_blocks` and mob griefing), keep out of
  dungeon wards, are capped per chunk, area, dimension and caster, fade on a saved schedule and give back
  the ground they took. See [A world that remembers magic](#a-world-that-remembers-magic).
- **Validation:** the server checks every cast and every edit (runes learned,
  socket count, cost). The client never decides anything that matters.
- **Flight (Soar):** only ever takes back a flight it gave (a saved note says it gave one), never touches
  creative or spectator players, is taken away before a leaving player is saved (and given back on login),
  ends at death, sets you down in another dimension and inside a dungeon's ward, and never gets a player
  kicked on a server with flying turned off (vanilla only counts a player as floating when they may not fly).
- **Aura** (see [Aura](#aura-the-swordsmans-path)): its blows are melee, so armour applies; against players its bonuses sit
  under the spell-defence cap and the aura PvP scale, and the slash meets the spell defences in full, spellguard included.
- **Spell mastery's traits** go through the ordinary cast: their damage bonus is one more factor under the cap
  above (and only half of it against a player), never more than ×1.2 together; prices and cooldowns go no lower than 85%.
- **Config file:** `config/wildercord.json` holds the caps above, whether spells may edit
  blocks, the PvP scale, the spell defences, mana-regen and cost multipliers, Runebound and loot
  chances, the monsters of the wilds (`monsters`: their switches and spawn rate), imbue limits,
  feature switches and the overchannel and tracing tuning (`channeling`); `/wildercord reload`
  reads it again (see
  [features/gear-config-api.md](features/gear-config-api.md)).

## Add-on compatibility contract

This is what "every add-on works with every other add-on, forever" means in
practice:

1. **One shared context.** Shapes produce targets, effects act on targets,
   modifiers change numbers, and links schedule the rest. A new rune only has to
   speak this one interface to combine with everything.
2. **Runes are data.** Every rune, including the built-in ones, is a JSON file in
   `data/<namespace>/wildercord/rune/`. The server syncs them to clients, so
   runes added by a datapack on the server show up with names and icons on every
   client.
3. **Two ways to extend:**
   - **Datapack, no code.** Reuse the built-in rune *types* with new numbers, or
     make a *composite* rune that is a preset sequence (for example "Meteor" =
     `Rain · Fire · Explode`).
   - **Java API.** Register brand-new rune types in the `dev.wildercord.api`
     package (see [API.md](API.md)): runes of every family with their behaviour,
     categories, element reactions and events, from a `wildercord` entrypoint.
4. **Element tags drive fusion.** Fusion recipes are datapack recipes that match
   tags such as `#wildercord:element/fire`, so new runes join old fusions for free.
5. **One rune item.** Every rune is the same `wildercord:rune` item carrying a
   component that stores which rune it is and its rank. An add-on only adds the
   JSON and a texture — no new item registrations needed.
6. **Nothing is ever deleted.** If an add-on is removed, its runes stay in your
   Codex, spells and inventory as **Silent Runes**. They are skipped when casting
   and keep working again if the add-on comes back.
7. **The API only grows.** Within 1.x, public API classes and JSON fields are
   only ever added, never removed or renamed. Every JSON field has a default, so
   old packs keep loading.

## Technical outline

| Piece | Approach |
|---|---|
| Rune definitions | Synced dynamic registry `wildercord:rune`, loaded from datapacks |
| Rune behaviour | Static registry `wildercord:rune_type` (Java), referenced by rune JSON |
| Spell engine | Pure-Java core with no Minecraft imports: parse → attach modifiers → cost → readout. Unit-tested with JUnit. A separate executor runs the spell in the world. |
| Player data | Fabric data attachments: learned runes, spells, selected spell, mana. Saved with the player and synced only to them. |
| Networking | The client sends "cast spell N" and "edit spell". The server validates and runs everything. |
| Extra slot | An inventory-menu slot and screen mixin, in the pattern Attuned already uses |
| Rune items | One item with a `wildercord:rune` component. The item model picks a texture from the component. |
| Fusion | Datapack recipe types `wildercord:fusion_upgrade` and `wildercord:fusion_combine`, with tag ingredients |
| Config | `config/wildercord.json`, with every field defaulted |

## Milestones

| # | Milestone | Done when |
|---|---|---|
| M0 | Project scaffold | Builds against 26.3; the mod loads in a dev client |
| M1 | Spell engine | Parser, modifier attachment, cost and readout pass unit tests. The first 7 shapes, 8 effects, 4 modifiers and 2 links run through a `/wildercord cast` test command |
| M2 | Cord, slot, casting | The Cord slot works; R, V and K work; mana bar and cooldowns work; spells are saved on the player |
| M3 | Cord screen | Drag-and-drop editing with the live readout, and a Codex tab |
| M4 | Full v1 roster and acquisition | All 43 runes; recipes and unlocks; loot tables; boss drops; the Lightning-rod event |
| M5 | Fusion Altar | Upgrade, Combine and Knots, plus Codex discoveries |
| M6 | Art and polish | Rune, Cord and altar textures; particles and sounds per element; balance pass; first release |

## Later ideas (not in v1)

- **Channel link:** keep firing while the key is held.
- **Sneak link:** a different branch while sneaking.
- **Element reactions:** fire on a frozen target shatters it.
- **Cord cosmetics.**
- **More boss runes:** Elder Guardian, Breeze boss variants.
- **Optional Attuned integration:** a Focus that cuts rune costs.


## Blade and spell resonance

A damaging spell and Aura strike on a living foe within 1.2 seconds can produce a short material reaction. Either order, one player or two allies. It costs the swordsman four Aura and gives both swordsman and foe a four-second rest. One payment primes a particular target once, so continuous beams cannot farm reactions. The lesser actual hit contributes 25%, with independent ceilings of three health on creatures and 0.75 on players; normal spell defenses remain effective. The ten spell materials provide capped stance pressure, short mobility/defense buffs, marks or modest creature-only healing/weakening. A full coated ordinary swing or the first answering projected/art hit counts; chain sparks and glancing swings do not. See the guide for the exact durations, permissions and family table. The physical blade incision and three-beat material unfold remain at the target, with no circles or global screen flash.

## Fire preparation identity

Each built-in fire effect has an explicit client preparation recipe in FireFormations. Material sprites and narrow light encode its verb (fuse, grate, shield, jaw, wings, wet crest) rather than sharing one fire-colored scatter. Preparation tightens through two beats; existing server shape delivery and per-effect impact/voice remain authoritative. Exact namespace dispatch, existing event/particle bounds and reduced-detail outlines apply. This is formation-phase coverage; target-aware/linked origins and per-delivery launch/travel remain in the full presentation audit.
