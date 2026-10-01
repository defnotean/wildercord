# Runebound

![A Runebound zombie at night, glowing orange rune marks on its brow, chest, arms and legs](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/runebound.jpg)
<span>A Runebound zombie. Its Cord is written on its body in its spell's colour.</span>

Some monsters carry a Cord of their own and cast a real spell with it: the same runes, the same engine and the same
magic circles as yours. These are the **Runebound**. They're the first magic you'll meet out in the world, and the
most common source of runes before you can craft many of your own.

## Which monsters, and how often

Four kinds of vanilla monster can be Runebound, and so can every one of the [monsters of the wilds](monsters.md):

| Monster | Includes |
|---|---|
| Zombies | Zombies of every kind: husks, drowned and zombie villagers too |
| Skeletons | Skeletons of every kind: strays, bogged and wither skeletons too |
| Witches | |
| Illagers | Pillagers and vindicators |
| Monsters of the wilds | Bramblewalkers, Gloomstalkers, Thunderwing Harpies, Geode Crawlers, Bog Witch-Frogs and Mana Oozes, each with spells that suit it (a Bramblewalker's roots, a harpy's lightning) |

Each of these is rolled **once**, the first time it appears in the world:

- **Out in the world:** a 2% chance, plus a little more the higher the local difficulty. That's roughly 2.5% in a
  fresh area on Easy, up to about 6% in a long-settled area on Hard.
- **Inside an Archive or a dimension dungeon:** 35%. About a third of the monsters there carry Cords.
- **Never on Peaceful**, and never a monster someone has named with a name tag.
- **One in six** Runebound is an **Adept** (see below).

A server can make Runebound more or less common than this.

If a Runebound turns into something else, it keeps its Cord when the new creature could carry one (a zombie that
drowns stays Runebound as a drowned, a skeleton that freezes stays Runebound as a stray). If the new creature
couldn't, the Cord and its nameplate are gone.

## How to read one

A Runebound tells you exactly what it's about to do, if you know how to look.

![Two Runebound at night with their nameplates showing: a zombie labelled Venom Touch and a vindicator labelled Adept, Greater Arcane Blitz](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-runebound-nameplates.jpg)
<span>Nameplates: a zombie with Venom Touch, and an Adept vindicator with Greater Arcane Blitz.</span>

- **Its nameplate is its spell.** Above its head, always visible: a small icon for each rune on its Cord, then the
  spell's name, in its element's colour. An Adept's name starts with *Adept ·*.
- **Its rune marks.** Its Cord is written on its body: marks glowing in its spell's colour on its chest, back, arms,
  legs and brow, breathing slowly, with a faint haze of the same colour and specks of light drifting up. Everyone
  sees them, in any light. An Adept's marks are brighter.
- **A circle at its feet.** About once a second a faint rune circle turns at its feet, and enchanting glyphs rise over
  its head.
- **The warning.** Before every cast it stops, turns to face its target and opens its spell's magic circle in its
  right hand for **1.1 seconds**. The circle is the spell's own, rune by rune (see
  [Magic Circles](../spellcraft/magic-circles.md)). At the same time its nameplate lights up
  bright white between arrows, *» Frost Bolt «*, its marks flare hot toward white with sparks streaming off them, and
  you hear it prepare. A **Zone**, **Rain**, **Mine** or **Domain** spell also marks the ground where it will land with
  a circle about 3 blocks across.

## Adepts

One Runebound in six is an Adept. An Adept is tougher and hits harder:

| | Runebound | Adept |
|---|---|---|
| Extra health | +60% | +120% |
| Spell power | By difficulty (below) | 15% more |
| Its spell | As listed below | Gains **Split** (three copies) if its shape can split, or **Amplify** (+50% power) if not |
| Its marks | Glowing | Brighter |
| Rune drop | 35% | 60% |
| Torn Page | 6% | 20% |
| Rune of the world | never | 8% |
| Extra experience | 10 | 20 |

So a zombie (20 health) is 32 as a Runebound and 44 as an Adept.

## Their spells

Each kind of monster picks one spell from its own list when it becomes Runebound. The nameplate shows the name on
the left; an Adept's version is on the right.

**Skeletons** (all kinds) prefer range:

| Nameplate | Runes | Adept |
|---|---|---|
| Frost Bolt | Bolt · Frost | Splitting Frost Bolt |
| Shock Bolt | Bolt · Shock | Splitting Shock Bolt |
| Fire Arc | Arc · Fire | Splitting Fire Arc |
| Arcane Beam | Beam · Harm | Splitting Arcane Beam |
| Venom Bolt | Bolt · Venom | Splitting Venom Bolt |

**Witches** lay out areas and traps:

| Nameplate | Runes | Adept |
|---|---|---|
| Venom Zone | Zone · Venom | Splitting Venom Zone |
| Chill Mine | Mine · Chill | Splitting Chill Mine |
| Arcane Orb | Orb · Harm | Splitting Arcane Orb |
| Shock Rain | Rain · Shock | Splitting Shock Rain |
| Blind Bolt | Bolt · Blind | Splitting Blind Bolt |

**Pillagers and vindicators** fight up close and at mid range:

| Nameplate | Runes | Adept |
|---|---|---|
| Arcane Crescent | Crescent · Harm | Splitting Arcane Crescent |
| Arcane Blitz | Blitz · Harm | Greater Arcane Blitz |
| Fire Cone | Cone · Fire | Greater Fire Cone |
| Arcane Barrage | Barrage · Harm | Greater Arcane Barrage |
| Fire Bolt | Bolt · Fire | Splitting Fire Bolt |

**Zombies** (all kinds) come close:

| Nameplate | Runes | Adept |
|---|---|---|
| Gale-Arcane Burst | Burst · Push · Harm | Splitting Gale-Arcane Burst |
| Venom Touch | Touch · Venom | Greater Venom Touch |
| Frost Wave | Wave · Frost | Greater Frost Wave |
| Shock Ring | Ring · Shock | Greater Shock Ring |
| Swift & Empower | Self · Swift · Empower | Greater Swift & Empower |

A zombie with *Swift & Empower* casts it on itself: Speed and Strength, then it comes for you.

### Their own element

A Runebound **resists the element of its spell**: your spells of that element do **half** to it (you'll see
*Resisted* float over it). A skeleton with *Frost Bolt* shrugs off half your frost; a vindicator with *Fire Cone* half
your fire. This is on top of what its kind already resists or is weak to (skeletons and zombies are undead,
weak to life): see [Creature Affinities](../spellcraft/affinities.md). When its kind is weak to
its own element (a stray, weak to fire, carrying *Fire Arc*), the two cancel out. A reaction set off on it breaks
through the resistance, as on any creature.

The guards placed in the three dimension dungeons, the Runebound their bosses call up, and the Riftcaller carry fixed
spells instead; they're listed on those pages. Those Adepts don't gain Split or Amplify: they're Adepts in health,
power and loot. (The Archive's guards, and the Runebound the Archivist calls up, use the lists above.)

### How they cast

- **It must be hunting you.** A Runebound casts at whatever it's targeting, and only when it can see it.
- **Range depends on the shape.** It waits until you're this close before it starts a cast:

  | Shape | Casts from |
  |---|---|
  | Touch | 3.5 blocks |
  | Burst, Ring, Barrage | 5 blocks |
  | Cone | 6 blocks |
  | Blitz | 8 blocks |
  | Self, Wave | 12 blocks |
  | Crescent | 16 blocks |
  | Zone, Rain, Mine | 18 blocks |
  | Bolt, Arc, Beam, Orb | 22 blocks |

- **Timing.** Its first cast comes 2 to 4 seconds after it arrives, then one every 3.5 to 6 seconds.
- **The spell needs a clear line at the end, too.** When the 1.1-second warning ends, the spell only goes off if its
  target is still alive and still in sight. Step behind a wall or a tree during the warning and nothing happens.
- **Power by difficulty.** A Runebound's spell has 60% of normal power on Easy, 80% on Normal and 100% on Hard
  (Adepts 15% more). A skeleton's Frost Bolt on Normal hits for 4 instead of 5.
- **Who it hits.** Players (whatever the PvP setting, but never in Creative or Spectator), players' pets, iron and snow
  golems, and whatever it's hunting. **Never other monsters.**
- **It never changes blocks.** A Runebound's fire won't light the grass and its frost won't freeze a pond. Its spells
  still do what doesn't change blocks: storm runs through water, earth heaves the ground, wind turns arrows aside (see
  [Magic that Changes the World](world-magic.md)).

## Where you'll meet them

- **Anywhere** zombies, skeletons, witches and illagers spawn.
- **The Archive and the dimension dungeons**: placed guards in their halls, plus 35% of what spawns inside.
- **Boss fights**: every boss calls up two Runebound at each phase change (Adepts the second time).
- **Fallen stars**: 2 to 4 Runebound rise to guard each one. See [World Events](world-events.md).
- **Rift sieges**: three waves of them, and the Riftcaller. See [World Events](world-events.md).

## What they drop

A Runebound slain by a player (your blow, arrow or spell has to be the one that kills it) drops its usual loot, plus:

| Drop | Runebound | Adept |
|---|---|---|
| **A rune from its Cord**: one of the runes of its spell, picked at random (the shape, an effect, or an Adept's Split or Amplify) | 35% | 60% |
| **A Torn Page** (the riddle of a secret spell; see [Secret Spells](../spellcraft/secret-spells.md)) | 6% | 20% |
| **A rune of the world**: Vinelash, Undertow, Blazecall, Portalfall or Warcry, equally likely | never | 8% |
| **Extra experience** | 10 | 20 |

A skeleton with *Frost Bolt* can drop a Bolt rune or a Frost rune; an Adept with *Greater Arcane Blitz* can drop Blitz,
Harm or Amplify. That makes Runebound a good way to find runes you can't craft yet. The Adept's runes of the world are
on [Runes of the World](runes-of-the-world.md). A server can make rune drops more or less
common.

If it dies some other way (burning after you set it alight, a fall, another monster), it drops only its usual loot.

### Growing stronger with them

- **Runebreaker**: your first Runebound slain writes this feat into your Grimoire (250 mana toward your next Heart
  Circle) and unlocks the bone beads on the [Cosmetics](../companions/cosmetics.md) page.
- **The 6th Heart Circle** needs 8 Runebound slain by you. See [Heart Circles](../progression/heart-circles.md).
- **Advancements**: *Runebreaker* (slay a Runebound) and *Adept's End* (slay an Adept).
- **Contracts**: the Runesmith sometimes posts "defeat 3 to 5 Runebound with spells of one element", for a Tier II
  rune. See [Contracts](../social/contracts.md).

## Tips

- **Read the nameplate before you engage.** A *Venom Touch* zombie has to reach you; a *Shock Rain* witch can hit you
  from 18 blocks behind a crowd.
- **Answer with another element.** The nameplate's colour is its element, and it resists that one. Most Runebound
  are undead: life magic hits them 50% harder.
- **Break line of sight** when the nameplate lights up. The cast is wasted, and the next one is 3.5 to 6 seconds away.
- **Shoot bolts down.** Your own bolt meeting theirs in the air bursts them both. See
  [Playing Together](../social/playing-together.md).
- **Raise a Shield.** A Shield stops any spell that costs no more mana than the spell that raised it. Runebound spells
  cost between about 9 and 28 mana (an Adept's Split multiplies that by 2.4), so `Self · Shield` (12 mana) stops the
  bolts, and a heavier Shield stops the rest. Raised at the last moment, it throws the spell back. See
  [Shields and Parrying](../spellcraft/shields.md).
- **Hunt the Adepts.** A 60% rune, a 20% Torn Page and an 8% rune of the world make them the best loot in the open world.
