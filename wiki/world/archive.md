---
title: The Archive
parent: The World
nav_order: 2
---

# The Archive

<img src="{{ '/assets/images/archivist.jpg' | relative_url }}" alt="The Archivist, a hooded figure in an indigo robe with pale eyes, arms raised over its lectern, its tome open and a spell circle in its hand" class="shot">
<span class="caption">The Archivist, writing a spell in the air.</span>

## What it is

A buried library of magic under the Overworld. Its doors open only to spells, Runebound guard its halls, and the
**Archivist** waits over its lectern. Past the Archivist is a vault of Tier IV runes. It's usually your first dungeon,
and beating it is what the 8th Heart Circle asks for.

## How to find it

<img src="{{ '/assets/images/archive.jpg' | relative_url }}" alt="A ring of broken deepslate-brick pillars in a birch forest clearing, two topped with amethyst clusters, around a stairway going down" class="shot">
<span class="caption">The way in: broken pillars around a stairway going down.</span>

- **Where:** under most land biomes (forests, taigas, jungles, savannas, badlands, plains, snowy plains, deserts,
  meadows, cherry groves, groves). Never under oceans, rivers, swamps or beaches.
- **How often:** about one in every square 704 blocks across, where the biome allows.
- **Look for:** a ring of broken deepslate-brick pillars, some topped with amethyst, around a stairway. An arch with a
  crying obsidian keystone and a soul lantern stands over the steps.
- **Torn Pages point the way.** Reading a Torn Page in the Overworld gives you a riddle and, in the margin, the rough
  distance and direction to the nearest Archive (rounded to 50 blocks). See
  [Secret Spells]({{ '/spellcraft/secret-spells/' | relative_url }}).

Finding one earns the advancement *The Buried Library*.

## The layout

A stairway of 24 steps takes you about 24 blocks down. Then the rooms run in a line:

| Room | What's in it | Guards | Door out |
|---|---|---|---|
| **Hall of Shelves** | A library with bookshelves, reading tables and **a chest** | 3 Runebound (skeleton, witch, pillager) | Seal of **Frost and Storm** |
| **Hall of Braziers** | Four unlit campfires (a fire spell lights them) | 2 Runebound (a vindicator **Adept** and a skeleton) | Seal of **Fire and Wind** |
| **Arena** | A domed circle with eight pillars and the **Archive Lectern** | **The Archivist** | Side seal of **Arcane and Life** |
| **Vault** | **Two chests** and a **Wellstone** | none | |

Skeletons, zombies and witches also spawn in the dark, and about a third of them are Runebound. The arena and vault
are warded: you can't dig, blast or fly into them. See [warded arenas]({{ '/world/' | relative_url }}#warded-arenas).

## Rune Seal doors

<img src="{{ '/assets/images/seal-door.jpg' | relative_url }}" alt="A door of twenty Rune Seals in a deepslate frame, checkered with frost snowflakes and storm lightning bolts" class="shot">
<span class="caption">The Archive's first door: Frost and Storm seals.</span>

Every dungeon door is a **Rune Seal door**: seals 5 wide and 4 high, checkered with two elements.

- Seals can't be broken by tools or blasts. Only your spells open them (never a monster's).
- Hit the door with any effect of one of its elements, within a block of a matching seal. Those seals light up.
- A lit element stays lit for **10 seconds**. Light both elements in that time and the door dissolves for good.
- Opening a door earns the **Sealbreaker** feat and advancement for everyone within 24 blocks, the first time.

The easy way is one spell with both elements: `Bolt · Chill · Shock` opens the first door in one cast. Any Tier I
effect works, so any Cord can do it:

| Element | Cheap effect | Element | Cheap effect |
|---|---|---|---|
| Fire | Ember | Life | Nourish or Heal |
| Frost | Chill | Void | Hex or Blind |
| Storm | Shock | Arcane | Harm |
| Wind | Push | Time | Countdown |
| Earth | Pelt | Blood | Rend |

Recipes are on [Rune Recipes]({{ '/items/rune-recipes/' | relative_url }}). Every dungeon's doors are listed on
[The World]({{ '/world/' | relative_url }}).

## The Archivist

<img src="{{ '/assets/images/archive-arena.jpg' | relative_url }}" alt="The Archive's domed arena, its floor inlaid with a glowing magic circle, the Archivist rising over the lectern at its centre under a purple boss bar" class="shot">
<span class="caption">The Archivist rises. Its boss bar names every spell before it lands.</span>

It rises the first time a player comes within **12 blocks** of the lectern, and only once. If it's ever lost without
being killed, the lectern wakes a new one after players spend about 3 minutes nearby without finding it.

| | |
|---|---|
| **Health** | 400 (phases change at 267 and 133) |
| **Armour** | 8, and it barely takes knockback |
| **Immune to** | Burning, falls and other monsters (blasts like Explode still hit) |
| **Weak to** | Void (+50%). Resists arcane (half). See [Creature Affinities]({{ '/spellcraft/affinities/' | relative_url }}) |
| **Boss bar** | Purple; names the spell it's casting. Seen within 48 blocks |

**How it fights:**

- It floats and keeps its distance. Every 8 to 14 seconds it may **blink** about 10 blocks away (always if you're
  within 6 blocks). If it strays more than 22 blocks from its lectern, it blinks back.
- It needs line of sight to start a spell. Each spell shows its circle and lands **1.4 seconds** later.
- It casts its phase's spells in a fixed order, about 3 to 4 seconds apart (2 to 3.5 in the last phase).

| Phase | Its spells |
|---|---|
| **1** | Splitting Frost Bolt, Shock Rain, Arcane Orb |
| **2** | Volleying Fire Crescent, Wide Venom Zone, Greater Arcane Blitz, Splitting Chill Mine |
| **3** | Arcane-Chill Domain, Dismantle Barrage, Sonic Boom Beam, Frost-Shock Bolt, and one time in four **Sunfall** |

**Sunfall** is a [secret spell]({{ '/spellcraft/secret-spells/' | relative_url }}). A wide orange circle marks the ground
around you. A Shield won't hold it unless you [parry]({{ '/spellcraft/shields/' | relative_url }}) at the last moment, so
get out of the circle. Everyone within 40 blocks learns Sunfall's riddle.

**Rewriting its Cord.** At each phase change it can't be hurt for **2.5 seconds** and calls up two Runebound (a
skeleton and a pillager; Adepts the second time). No single blow skips a phase.

## Tips and counterplay

- **Bring void, leave fire.** Sonic Boom, Hollow and Blackspark hit hard. Burning does nothing, and arcane does half.
- **Fight at range.** It blinks when you close in, so bolts, beams and lances beat Touch or Burst.
- **Read the boss bar.** Step out of Zones and marked Rain or Mine spots. Sidestep bolts.
- **Use the pillars** to break line of sight.
- **Shield up** against bolts and orbs. See [Shields and Parrying]({{ '/spellcraft/shields/' | relative_url }}).
- **During a rewrite**, kill the two Runebound instead of wasting a big spell.
- **Answer its Domain**: step out, or break it with a strong Domain of your own. See
  [Playing Together]({{ '/social/playing-together/' | relative_url }}).

## When it falls

It drops a **Tier IV rune** its killer doesn't know yet, **2 Torn Pages**, **3 Mana Crystals**, a 50% chance of a
**greater staff** of any element (see [Casting Gear]({{ '/gear/' | relative_url }})), and 200 experience. Tier IV runes need
an Echo Cord to cast.

Everyone within 64 blocks earns **The Last Page** feat (the one the 8th Heart Circle asks for) and the advancement of
the same name. Everyone nearby also gets the 7th Circle's boss breakthrough. See
[Heart Circles]({{ '/progression/heart-circles/' | relative_url }}).

### The Relay Circle lesson

Once the Archivist has fallen, the quiet Archive Lectern holds a Master lesson, *The Margin Between Places*. If you
have **The Last Page** and an **active 8th Heart Circle**, use the lectern to copy it into your Grimoire. Then read its
three pages, there or later from your Grimoire, to learn the **Relay Circle**. Copying alone doesn't teach it, and you
never need to fight the Archivist again. See [Relay Circle]({{ '/spellcraft/relay-circle/' | relative_url }}).

## The chests

| Chest | What it holds |
|---|---|
| **Hall of Shelves** | 2 to 3 runes (mostly Tier II, some Tier III, rarely a rune of the world), and 1 to 2 of: a Torn Page, Blank Runes, books, lapis, a Mana Crystal, amethyst shards. 20% chance of the Tome of the Fifth Page or a focus |
| **Vault** (each of two) | 1 Tier IV rune, 1 to 2 Tier III runes, and 2 to 3 of: Mana Crystals, a Torn Page, gold, diamonds, an echo shard. 45% chance of casting gear: the Tome, a focus, or a greater staff |

The chest foci are Haste, Thrift, the Deep Well, Echoes and Resolve. See [Casting Gear]({{ '/gear/' | relative_url }}).

<img src="{{ '/assets/images/wellstone.jpg' | relative_url }}" alt="A Wellstone, a dark block banded with violet light, awake on a ley line with circles turning on the ground around it" class="shot">
<span class="caption">A Wellstone, awake on a ley line.</span>

**The Wellstone** is yours: mine it with a pickaxe. Set on a ley line, it quickens mana for everyone nearby. See
[Ley Lines and the Wellstone]({{ '/progression/ley-lines/' | relative_url }}).

A server can make runes, pages, crystals and gear more or less common in chests.
