---
title: The Ember Sanctum
parent: The World
nav_order: 3
---

# The Ember Sanctum

<img src="{{ '/assets/images/ember-sanctum.jpg' | relative_url }}" alt="The Cinder Warden, a hulking figure of magma plates and chains, cracking open in a burst of fire and frost in its forge-lit arena" class="shot">
<span class="caption">A reaction cracks the Cinder Warden's armour open.</span>

## What it is

A forge-temple in the Nether's caves, kept by the **Cinder Warden**. Its armour turns every blow and every plain
spell. Only an **element reaction** breaks through, so this is where you learn reactions for real.

| | |
|---|---|
| **Biomes** | Nether wastes, basalt deltas, crimson forests |
| **How often** | About one in every square 576 blocks across |
| **Doors** | Fire and Earth, then Void and Storm. The vault's is Frost and Fire |
| **Boss** | The Cinder Warden, 300 health |
| **Its own rune** | [Cinderheart]({{ '/runes/world/' | relative_url }}#cinderheart) (Tier IV) |
| **Trophy** | The Cinder Heart |
| **Runes found only here** | [Cinderbrand]({{ '/runes/world/' | relative_url }}#cinderbrand), [Ashen Veil]({{ '/runes/world/' | relative_url }}#ashen_veil), [Kindled]({{ '/runes/world/' | relative_url }}#kindled) |

## How to find it

It sits on a Nether cave floor above the lava sea, or is carved into the rock where there's no floor, so tunnelling
can find one too. Look for **a causeway** of blackstone bricks between two lava trenches, with braziers either side.
A Sanctum never cuts into a fortress or bastion.

## The way in

<img src="{{ '/assets/images/world-ember-gate.jpg' | relative_url }}" alt="A Rune Seal door checkered with fire flames and earth glyphs, framed in polished blackstone with magma blocks above it, braziers burning either side" class="shot">
<span class="caption">The gate: Fire and Earth seals.</span>

| Room | What's in it | Door out |
|---|---|---|
| **The causeway** | A path between two lava trenches | **Fire and Earth** seals |
| **Hall of Chains** | A lava channel crossed by three bridges, magma patches, **a chest**, and four Runebound | **Void and Storm** seals |
| **The Forge** | Blast furnaces, lava cauldrons, an anvil | open |
| **The Arena** | A domed forge-circle with **four lava pools** near the edge and the **forge-altar** in the middle | Side door of **Frost and Fire** |
| **The Vault** | **Two chests** | |

How to open the doors is on [the Archive page]({{ '/world/archive/' | relative_url }}#rune-seal-doors). The vault door is
a hint: Frost and Fire make Shatter.

<img src="{{ '/assets/images/world-ember-hall.jpg' | relative_url }}" alt="The Hall of Chains: a lava channel down the middle crossed by bridges, Runebound wither skeletons and a piglin brute with glowing marks, the Void and Storm door at the far end" class="shot">
<span class="caption">The Hall of Chains and its keepers.</span>

**The hall's guards:** two wither skeletons (Fire Bolt, Fire Arc), a skeleton (Frost Bolt) and a piglin brute
**Adept** (Flashfire Cone). The Frost Bolt and a Fire Bolt together make a Shatter on you, so kill one first. More
wither skeletons, blazes and magma cubes spawn inside, and about a third of the wither skeletons are Runebound. Sneak
over magma blocks.

**The hall chest** holds 2 to 3 Tier II or III fire and earth runes, plus 2 to 3 of: a Torn Page, Blank Runes, lapis,
blaze powder, a Mana Crystal, magma cream, gold, or (rarely) netherite scrap.

## The Cinder Warden

<img src="{{ '/assets/images/world-ember-warden.jpg' | relative_url }}" alt="The Cinder Warden standing on the glowing forge-circle in the middle of its domed arena, a spell circle in its hand, lava pools near the walls" class="shot">
<span class="caption">The Cinder Warden on its forge-circle.</span>

It climbs out of the forge-altar the first time a player comes within **12 blocks**. It's nearly three blocks tall,
walks slowly, can't be knocked back, and never strays more than 22 blocks from its altar. Fire, lava and burning never
hurt it. It's weak to **frost** (+50%) and resists **fire** (half), but a reaction set off on it lands in full. See
[Creature Affinities]({{ '/spellcraft/affinities/' | relative_url }}).

### Its trick: armour only a reaction breaks

| What hits it | What it takes |
|---|---|
| Swords, axes, arrows, tridents, fists | **Nothing** |
| Fire, lava, burning | **Nothing** |
| A plain spell | **A tenth** of its damage |
| A spell that sets off a **reaction** on it | **Full damage**, and its plates **crack open** |
| Any spell while cracked open | **Full damage** |

A crack stays open for **1.5 seconds**, and the boss bar says *cracked open!*. A reaction anyone sets off cracks it for
everyone. How reactions work is on [Reactions]({{ '/spellcraft/reactions/' | relative_url }}).

### How it fights

- **Its fist.** If you're within 4.5 blocks, a **ring of fire glows on the floor** around it (about 4 blocks out) for
  0.8 seconds, then its fist comes down: heavy damage, burning and knockback. Step out of the ring.
- **Its spells** set up reactions on **you**. The boss bar names each one, and the circle lands 1.4 seconds later.

| Phase | Its spells (the reaction it sets up on you) |
|---|---|
| **1** (300 to 200) | Frost Bolt › Fire Bolt (Shatter), Fire Pillar, Ember Wave |
| **2** (200 to 100) | Dash Bolt › Fire Comet (Wildfire), Frost Ring › Fire Ring (Shatter), Fire Rain, Flashfire Cone |
| **3** (100 to 0) | Drawing Bolt+ (Implode), Frost Bolt+ (Shatter), Inferno Zone, Frost Wave › Fire Wave (Shatter) |

**Stoking its forge.** At each phase change it can't be hurt for **3 seconds**, then the heat throws everyone within 8
blocks back. Two Runebound wither skeletons climb out of the fire (Fire Bolt and Frost Arc; Adepts the second time).
No single blow skips a phase.

## Tips and counterplay

- **Bring a one-spell reaction.** The easiest is the Warden's own: `Bolt · Frost · Delay · Bolt · Fire`. The fire half
  sets off Shatter, lands in full and cracks it open. Others that work:

  | Reaction | Example spell |
  |---|---|
  | Shatter | `Bolt · Frost · Delay · Bolt · Fire` |
  | Wildfire | `Bolt · Push · Delay · Bolt · Fire` |
  | Implode | `Bolt · Pull · Delay · Bolt · Explode` |
  | Fracture | `Bolt · Chill · Pelt` |
  | Blight | `Bolt · Hex · Venom` |
  | Rupture | `Bolt · Rend · Windcut` |

  Nothing sets the Warden alight, so fire-based Overload doesn't work. There's no rain in the Nether, so soak it with a
  spell if you want Conduct.
- **Hit hard inside the crack.** For 1.5 seconds every spell does full damage, and frost does 50% more.
- **Fight at range** and its fist never comes. When it does, you have 0.8 seconds to step away.
- **Dodge the second half** of its paired spells. When the bar says *Drawing Bolt+*, get clear of the comet.
- **Fire Resistance** stops its burning (not blasts or the fist). So does the Cinder Heart on your next visit.
- **Kill its keepers** first: Fire Bolt plus Frost Arc is a Shatter on you.
- **Mind the lava pools** when you get knocked back.
- **Don't bother with blades or arrows.** They do nothing.

## When it falls

- **Everyone who fought** and is within 96 blocks of the altar gets a **Tier IV rune** they don't know yet, straight
  into their pack.
- **Its killer** also gets the **Cinder Heart** trophy, the **[Cinderheart]({{ '/runes/world/' | relative_url }}#cinderheart)**
  rune, 3 Mana Crystals, a Torn Page, a fire or earth rune, and a 50% chance of a rune of the Sanctum.
- 220 experience drops on the altar. Anything that doesn't fit is left on the altar, safe from fire, lava and blasts.
- Everyone within 64 blocks earns the **Tempered** feat (1,500 mana toward your next Heart Circle) and advancement. It
  counts as a boss for the 7th Heart Circle.

**The Cinder Heart** is kept, not spent: hold it up for 3 minutes of Fire Resistance, then it rests for 5 minutes.

## The vault

<img src="{{ '/assets/images/world-ember-vault.jpg' | relative_url }}" alt="The Ember Sanctum's vault: two chests either side of a brazier burning on gilded blackstone" class="shot">
<span class="caption">The vault, behind the Frost and Fire door.</span>

Each of its two chests holds a **Tier IV rune**, a **rune of the Sanctum** (35% chance of a second), and 2 to 4 of:
Mana Crystals, a Torn Page, gold, blaze powder, diamonds, magma cream, an echo shard, or netherite scrap.

The runes of the Sanctum:

- **Cinderbrand** (Tier II fire, the most common): brands a target, and your fire burns it hotter for a while.
- **Ashen Veil** (Tier III fire): fire can't hurt the target, and whatever strikes it up close catches fire.
- **Kindled** (Tier III modifier): more power, and the effect sets what it hits alight.

Full details are on [Runes of the World]({{ '/runes/world/' | relative_url }}).
