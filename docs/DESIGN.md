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
   **Codex** and survives death. Spare copies are for fusion or trading.
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
| Amplify | I | effect | +50% power (damage, healing, force, blast) | 1.6 |
| Extend | I | effect, Zone, Delay | +100% duration | 1.4 |
| Widen | II | Burst, Zone, Rain, Explode | +50% radius (an effect grows at most 8 times, five Widens) | 1.5 |
| Quicken | II | Bolt, Delay, Zone | Projectiles twice as fast, delays halved | 1.2 |
| Pierce | II | Bolt, Beam | Passes through up to 3 targets | 1.3 |
| Bounce | II | Bolt | Bounces off blocks up to 3 times | 1.3 |
| Split | III | any shape | Three copies (bolts and beams fan out; bursts and zones spread) | 2.4 (whole group) |
| Homing | III | Bolt | Steers toward the nearest enemy within 12 blocks | 1.4 |
| Chain | III | Bolt, Beam, Touch | After a hit, jumps to up to 3 more enemies within 6 blocks | 1.8 |

Modifiers stack: `Amplify · Amplify` = +125% power at ×2.56 cost.

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
| Regrowth | II | life | Regeneration II for 8 s |
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
| Venom | Effect · Damage | II | Poison II for 6 s and 2 damage |
| Smite | Effect · Damage | III | 10 holy damage, doubled against undead |
| Inferno | Effect · Damage | III | Everything within 4 blocks burns: 3 damage/s for 4 s |
| Thunderclap | Effect · Damage | II | A flash, then a crack: 5 damage within 3 blocks, half a second's stun and a forgotten target |
| Starfall | Effect · Damage | IV | Eight falling stars, 6 damage each (Elder Guardian drop) |
| Blind | Effect · Control | I | Blindness and darkness for 5 s (3 on players); a blinded monster lashes out at the creature beside it |
| Chill | Effect · Control | I | Slowness II for 6 s and 1 freeze damage |
| Silence | Effect · Control | II | Monsters forget their target and are weakened for 6 s |
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
| Deflect | Effect · Support | II | 8 s: projectiles coming at the target are turned aside |
| Haven | Effect · Support | II | A 4-block dome for 8 s: allies inside take 20% less damage, projectiles from outside glance off it |
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
| Coldsnap | Effect · Damage | II | 3 freeze damage and Slowness II for 4 s to every enemy within 3 blocks; they're left brittle for Shatter |
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
| Fortune | Life | 10 s: each hit you deal (spells and melee) has a 1 in 4 chance to strike for triple |
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
light, and more (see [features/parry-and-wild-magic.md](features/parry-and-wild-magic.md)).

### Runebound
About 2-6% of zombies and skeletons of every kind (husks, drowned, zombie villagers, strays,
bogged...), witches, pillagers and vindicators spawn **Runebound** (more where the local difficulty
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

### Spell Scrolls and the Training Dummy
- **Spell Scroll:** inscribe any of your spells from the Cord screen (paper, an ink sac and twice the
  spell's mana). Anyone can cast it once, with or without a Cord or its runes, at base power.
- **Training Dummy:** crafted from wool, hay, sticks and a slab. It never dies; every hit floats up
  as a number coloured by damage type, and its name shows DPS over the last 5 s and the burst's
  total. Sneak and punch it to pick it up.

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
| **R** | Cast the selected spell. Hold to charge it, then release |
| **V** | Tap: select the next spell. Hold: the spell wheel; point at a spell and let go to select it. Let go without pointing and it stays open until you click a spell, press `V` or Enter, press a number, or press Esc |
| **K** | Open the Cord screen |
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
  hinders (▼) where you stand.
- Each element has its own particle colour and cast sound.

## Server rules and safety

- **Friendly fire:** harmful effects never hit you, your scoreboard team or your
  tamed pets.
- **PvP:** with PvP off, harmful effects never affect other players or their pets. With PvP on, rune
  damage to players is ×0.6. *(Planned: crowd-control such as Frost, Pull and Launch
  lasting half as long on players.)*
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
- **Validation:** the server checks every cast and every edit (runes learned,
  socket count, cost). The client never decides anything that matters.
- **Config file:** `config/wildercord.json` holds the caps above, whether spells may edit
  blocks, the PvP scale, mana-regen and cost multipliers, Runebound and loot chances, imbue
  limits and feature switches; `/wildercord reload` reads it again (see
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
