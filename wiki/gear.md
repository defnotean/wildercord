---
title: Casting Gear
nav_order: 10
---

# Casting gear

**Casting gear** is what you wear while you cast: a **staff** that favours one element, a **greater
staff** taken from a boss, the **Tome of the Fifth Page** that opens a fifth spell, or a **focus** that bends every
spell a little. Each goes in its own **slot in your inventory**, so you don't have to hold it, and it shows on your
character. Gear works alongside your Cord; you still cast from the Cord.

<img src="{{ '/assets/images/d-gear-staff.jpg' | relative_url }}" alt="A player holding a Fire Staff, surrounded by pillars of flame from a charged fire spell" class="shot">
<span class="caption">A Fire Staff's flourish after a charged Fire spell</span>

## The gear slots

Open your inventory (`E`) and you'll find three slots for casting gear, each with the faint outline of what it takes.
In the survival inventory they sit in a small tray on top of the window, above your armour; in the creative inventory's
Survival Inventory tab they're beside the Cord slot.

<img src="{{ '/assets/images/d-gear-slots-inventory.jpg' | relative_url }}" alt="The survival inventory with a tray on top of the window holding a staff, a focus and the tome, and the player in the paper doll wearing them" class="shot">
<span class="caption">The gear tray, filled; the paper doll wears what's in it</span>

| Slot | Takes |
|---|---|
| **Staff** | any staff, a greater staff too |
| **Focus** | any focus |
| **Tome** | the Tome of the Fifth Page |

A slot holds one piece and only takes what it's for. Click a piece into it, or **shift-click** it (shift-click again to
take it out); a number key swaps it with your hotbar. Hover an empty slot to see what it takes, and an item's tooltip
says which slot it goes in.

The fourth slot on the tray is the **Backpack slot**, for a backpack you wear on your back: see
[Backpacks]({{ '/items/backpacks/' | relative_url }}). A staff in its slot is strapped over the backpack.

What counts is what's in your slots **at the moment you cast**. A piece in its slot works with nothing in your hands.

### Or held, as before

A slot that's empty still lets you hold that kind of gear, exactly as gear always worked:

| Gear | Held in |
|---|---|
| Staffs and greater staffs | **either hand** |
| Tome of the Fifth Page | **off-hand only** |
| Foci | **off-hand only** |

**A piece in its slot takes the place of held pieces of its kind.** With a Fire Staff in the Staff slot, a Frost Staff in
your hand does nothing; with a focus in the Focus slot, a focus in your off-hand is ignored; the same for the tome. The
slots don't affect each other: a staff in its slot and a focus in your off-hand both work.

### On your character

<img src="{{ '/assets/images/d-gear-slots-worn.jpg' | relative_url }}" alt="A player seen from the front and the back wearing a staff across the back, a focus hovering off the left shoulder and the tome hanging from a belt at the hip" class="shot">
<span class="caption">A staff, a focus and the tome, worn</span>

Gear in a slot shows on you, to you in third person, to everyone around you and in the inventory's paper doll:

- a **staff** is strapped diagonally across your back, its head over your right shoulder;
- a **focus** hovers just off your left shoulder, bobbing and slowly turning, with a faint glimmer circling it;
- the **tome** hangs from a belt at your left hip.

Each looks like its item. They keep clear of armour, capes and elytra, follow you when you sneak, and aren't drawn when
you're invisible or already holding that same piece.

### Dying

What's in your gear slots **drops where you die**, like the rest of your inventory (Curse of Vanishing destroys its
own), and stays with you when the server keeps inventory. It never duplicates through dying, respawning or changing
dimension.

## Staffs

There's a staff for each of the ten elements: **Fire, Frost, Storm, Wind, Earth, Life, Void, Arcane, Time** and
**Blood Staff**. Each one:

- gives **effects of its element +20% power**;
- makes **any spell with at least one effect of its element cost 10% less mana** (the whole spell, not just that
  effect);
- leaves a **flourish** when you release a charged spell (charged 20% or more) with an effect of its element: flames,
  a ring of ice, a bolt from the sky, a gust, cracked ground, leaves, a collapsing dark, a star, a clock face or a pulse
  of blood. The more the charge, the bigger it is. It's for show only.

Its tooltip says, for example, *Fire spells: +20% power, 10% less mana*, *Goes in your inventory's Staff slot* and, in
grey, that with the slot empty it works held in either hand.

### Crafting a staff

Every staff is crafted the same way, from two sticks, two of its element's material and a Mana Crystal. The Fire Staff
uses Blaze Rods in place of the sticks.

<div class="recipe-gallery">
{% include recipe-card.html id="fire_staff" name="Fire Staff" %}
{% include recipe-card.html id="frost_staff" name="Frost Staff" %}
{% include recipe-card.html id="storm_staff" name="Storm Staff" %}
{% include recipe-card.html id="wind_staff" name="Wind Staff" %}
{% include recipe-card.html id="earth_staff" name="Earth Staff" %}
{% include recipe-card.html id="life_staff" name="Life Staff" %}
{% include recipe-card.html id="void_staff" name="Void Staff" %}
{% include recipe-card.html id="arcane_staff" name="Arcane Staff" %}
{% include recipe-card.html id="time_staff" name="Time Staff" %}
{% include recipe-card.html id="blood_staff" name="Blood Staff" %}
</div>

| Staff | Material (2) | Handle (2) |
|---|---|---|
| Fire Staff | Blaze Powder | **Blaze Rods** |
| Frost Staff | Packed Ice | Sticks |
| Storm Staff | Lightning Rod | Sticks |
| Wind Staff | Wind Charge | Sticks |
| Earth Staff | Mossy Cobblestone | Sticks |
| Life Staff | Glistering Melon Slice | Sticks |
| Void Staff | Ender Pearl | Sticks |
| Arcane Staff | Amethyst Shard | Sticks |
| Time Staff | Clock | Sticks |
| Blood Staff | Nether Wart | Sticks |

The pattern is a diagonal: the Mana Crystal in the top right corner, the handle running down to the bottom left.

## Greater staffs

<img src="{{ '/assets/images/d-gear-greater-staff.jpg' | relative_url }}" alt="A player holding a Greater Void Staff with a dark violet head, sparks and smoke around it" class="shot">
<span class="caption">A Greater Void Staff</span>

A **greater staff** is a staff taken from a boss: **Greater Fire Staff**, **Greater Frost Staff** and so on, one for
each element. It works exactly like a staff but stronger:

- effects of its element **+35% power** (instead of +20%);
- the same **10% less mana** for spells with an effect of its element;
- a bigger flourish.

Greater staffs can't be crafted. They're found here:

| Where | Chance | Which elements |
|---|---|---|
| **The Archivist** (the Archive's boss) | 50% | any of the ten |
| **The Ender Dragon** | 50%, each time it's killed | Void, Arcane or Time. It lands at the killer's feet, glowing, and never despawns. |
| **The Wither** | 35% | Void or Blood |
| **The Warden** | 35% | Earth or Storm |
| **An Elder Guardian** | 35% | Frost or Life |
| **Archive vault chests** | part of a 45% chance of one piece of gear (see below) | any of the ten |

See [The Archive]({{ '/world/archive/' | relative_url }}) for the Archivist and the vault.

## The Tome of the Fifth Page

<img src="{{ '/assets/images/d-gear-tome-row.jpg' | relative_url }}" alt="The Cord screen with five spell rows; the fifth row is highlighted, and the readout mentions the Arcane Staff's bonus" class="shot">
<span class="caption">With the tome in your off-hand, the Cord screen shows a fifth spell row</span>

The **Tome of the Fifth Page** gives you a **fifth spell**, whatever your Cord.

- **Put it in the Tome slot** (or hold it in your off-hand) and a fifth row appears on the Cord screen: *The tome's
  spell: it casts while the tome is in its slot or your off-hand*. Thread it like any other spell. It has as many sockets as your Cord's other rows, and holds the
  same tiers.
- **Select it** by tapping **V** to step on past your Cord's own spells, or on the spell wheel (hold **V**), where it
  shows as spell 5 while the tome counts (press **5** there to pick it). The wheel opens even on a Twine Cord then,
  with the Cord's spell and the tome's. You can also bind a key to **Cast spell 5 (the tome's)** in Minecraft's
  controls.
- **Take the tome out** and spell 5 locks, but its runes are kept for next time. The **Cast spell 5** key then says
  *Spell 5 is the tome's: put the Tome of the Fifth Page in its slot (or hold it in your off-hand)*. If spell 5 was still selected, the cast
  key moves on to your first spell instead: a tap casts it and a hold charges it.
- The fifth spell can be inscribed onto a [Spell Scroll]({{ '/social/playing-together/' | relative_url }}#spell-scrolls)
  while the tome counts, but it can't be tied into a [Knot]({{ '/fusion-altar/knots/' | relative_url }}).

The tome can't be crafted. It's found in **Archive vaults and libraries** and in **stronghold libraries** (see the
table below). See [Controls]({{ '/controls/' | relative_url }}) for the keys.

## Foci

<img src="{{ '/assets/images/d-gear-foci.jpg' | relative_url }}" alt="First-person view holding a staff in one hand and a gold Focus of Thrift with a green gem in the other" class="shot">
<span class="caption">A staff in one hand and a focus in the other</span>

A **focus** goes in the **Focus slot** (or your off-hand) and changes **every** spell you cast.

| Focus | What it does | Recipe |
|---|---|---|
| **Focus of Haste** | Charged casts fill **40% faster** | Feather on top; Sugar, Mana Crystal, Sugar across the middle; Gold Ingot below |
| **Focus of Thrift** | Spells cost **15% less mana**, but every effect hits **10% softer** | Emerald on top; Gold Ingot, Mana Crystal, Gold Ingot across the middle; Gold Ingot below |
| **Focus of the Deep Well** | **+50 max mana** | Block of Lapis Lazuli on top; Polished Deepslate, Mana Crystal, Polished Deepslate across the middle; Polished Deepslate below |
| **Focus of Echoes** | A **10% chance** that a spell **echoes**: it goes off again half a second later, for free | Echo Shard on top; Amethyst Shard, Mana Crystal, Amethyst Shard across the middle; Amethyst Shard below |
| **Focus of Resolve** | Incoming spells hurt **20% less**, but your spell effects are **15% weaker** | Iron Ingot on top and below; Amethyst Shard, Mana Crystal, Amethyst Shard across the middle |

Each focus is a plus shape around a Mana Crystal:

<div class="recipe-gallery">
{% include recipe-card.html id="focus_of_haste" name="Focus of Haste" %}
{% include recipe-card.html id="focus_of_thrift" name="Focus of Thrift" %}
{% include recipe-card.html id="focus_of_the_deep_well" name="Focus of the Deep Well" %}
{% include recipe-card.html id="focus_of_echoes" name="Focus of Echoes" %}
{% include recipe-card.html id="focus_of_resolve" name="Focus of Resolve" %}
</div>

Foci can also be found (see the table below).

## Where gear is found

Chests that hold casting gear roll once for **one piece**, shared evenly among the pieces listed:

| Chest | Chance of a piece | Pieces it can be |
|---|---|---|
| **Archive vault** | 45% | the Tome, any of the five foci, or any of the ten greater staffs |
| **Archive library** | 20% | the Tome or any of the five foci |
| **Stronghold library** | 10% | the Tome or any of the five foci |
| **Ancient city** | 12% | any of the five foci |
| **Woodland mansion** | 8% | any of the five foci |

So an Archive library chest has about a 3.3% chance of the Tome, and each of the five foci the same. Plain staffs are never
found: craft them.

## How gear stacks

Everything that counts works together: your slots, and the hands where a slot is empty. The rules:

- **The same piece twice counts once.** Two Fire Staffs are one Fire Staff.
- **Two staffs never discount one spell twice.** A spell with Fire and Frost effects, cast with a Fire Staff and a Frost
  Staff, costs 10% less, not 20%. Each staff still boosts its own element's effects.
- **The better staff counts.** A Fire Staff and a Greater Fire Staff together give Fire effects +35%, not more.
- **A staff and a focus both work.** A Fire Staff with a Focus of Thrift gives Fire
  effects +20% then 10% softer (+8% in all), and a Fire spell costs 10% less then 15% less (about 23.5% less in all).
- **The tome and a focus work together** when each is in its slot. Held, they share the off-hand, so you hold one of
  them at a time.
- **Gear comes last.** Gear is its own step on cost and power, applied after everything else: Heart Circles, Cord
  enchantments, charge and [ranks]({{ '/fusion-altar/ranks/' | relative_url }}).

### Where you see it

- **The Cord screen's readout** shows what your gear does to the spell you're editing. Its cost line already includes
  it, and a line such as *Arcane Staff: Arcane spells: +20% power, 10% less mana* explains it.
- **The mana badge** on the Cord screen lists everything that counts under *Casting gear*, with the extra max
  mana from a Focus of the Deep Well.
- **Each piece's tooltip** says what it does, which slot it goes in and that it works held while the slot is empty.

### Gear and other things

- A [chorus]({{ '/social/chorus/' | relative_url }}) keeps the last caster's gear behind it.
- A [Spell Scroll]({{ '/social/playing-together/' | relative_url }}#spell-scrolls) ignores gear: it goes off at base
  strength.
- A staff boosts effects of its element inside a [Knot]({{ '/fusion-altar/knots/' | relative_url }}) too, and
  [fused runes]({{ '/runes/fused/' | relative_url }}) count as the element shown in their entry.

All the gear recipes are also listed in [Items and Crafting]({{ '/items/' | relative_url }}#casting-gear).

## New in 0.7

- [Elemental Armour]({{ '/progression/elemental-armour/' | relative_url }})
- [Defensive Foci]({{ '/progression/defensive-foci/' | relative_url }})
