---
title: Imbuing and Glyphs
parent: Spellcraft
nav_order: 10
---

# Imbuing and Glyphs

## What it is

<img src="{{ '/assets/images/imbue-sword.jpg' | relative_url }}" alt="A player holds an iron sword over a teal magic circle; the hotbar line reads Iron Sword holds Fire (3 charges), released at what it strikes" class="shot">

**Imbue** stores a spell instead of casting it. Put it in a sword and your next three hits burst into flame. Write
it onto a door or a patch of floor and it becomes a **glyph**: a trap that goes off when someone steps on it,
opens it or breaks it.

## How to get it

<img src="{{ '/assets/runes/imbue.png' | relative_url }}" alt="" width="32" height="32" class="rune-icon"> **Imbue**
is a Tier II **link** that costs 3 mana and needs a Copper Cord or better. Craft it from a Blank Rune, an
Experience Bottle, 2 Lapis Lazuli and a Gold Ingot. See [Rune Recipes]({{ '/items/rune-recipes/' | relative_url }}).

## How to use it

Imbue splits the spell in two:

- **Before Imbue:** a shape that picks **what** holds the magic. `Self` means the item in your hand, or, with both
  hands empty, the block you're looking at. Any other shape means the block it touches.
- **After Imbue:** the part that's **stored** and goes off later.

`Self · Imbue · Burst · Fire` stores `Burst · Fire` in the item in your hand.

### Charges and price

- Everything imbued holds **3 charges**. You pay for all three up front: three times the stored part's mana,
  plus Imbue itself.
- **Releasing costs nothing.** It hits with your Heart Circles, Cord enchantments and affinities, but not
  charging or rhythm.
- **One shared cooldown.** After any imbued item or glyph goes off, none can go off again until that spell's
  cooldown has passed (at least half a second). It's kept when you log out.

### Items

| Item | Goes off when | Where |
|---|---|---|
| **Weapons** | You hit a creature | At what you hit |
| **Tools** | You break a block or hit a creature | At the block or creature |
| **Bows and crossbows** | You shoot | Where the arrow lands |
| **Armour and shields** | Something hurts you | At whoever hurt you |
| **Blocks** | You place it | It becomes a glyph |
| **Anything else** | You use it | At what you're looking at, up to 24 blocks away |

- **Helpful magic goes to you.** A sword imbued with `Heal` heals you as you fight.
- A crossbow's Multishot spends one charge, and only one arrow carries the spell.
- From a stack, only one item is imbued. Imbuing an item again replaces its magic.
- **Up to 6 imbued items.** A seventh makes your oldest one fade.
- Cords, runes, Knots and Spell Scrolls can't be imbued.

### Glyphs

<img src="{{ '/assets/images/imbue-glyph.jpg' | relative_url }}" alt="A teal glyph circle flares on the grass under a husk, and ice shards burst up around it" class="shot">

**Any block** can hold a glyph. Make one with a shape that touches a block (`Touch · Imbue · Root · Frost`), a
shape that hits a creature (the glyph goes under it), `Self` with empty hands, or by placing an imbued block.
You need build rights there.

A glyph goes off when a creature steps on or touches it, someone uses the block (door, button, lever), something
shoots it, someone else breaks it, or redstone powers it.

- **It knows friend from foe.** A harmful glyph only answers enemies, a helpful one only you and your allies.
- **It re-arms** no faster than its spell's cooldown, and never in under 1 second. A creature standing on it is
  caught once, until it steps off.
- **Up to 12 glyphs**, across all dimensions. A 13th makes your oldest fade.
- **Glyphs sleep while you're away**: offline, dead, spectating or in another dimension.
- **Portable traps.** Break your own glyph and the block keeps its spell and charges, so you can place it
  elsewhere.

### Inside a stored spell

- A stored part starting with an **effect** lands on whatever set it off. One starting with a **shape** casts
  from there: `Self · Imbue · Burst · Explode` in a sword bursts around what you hit.
- **Echo** repeats only the stored part. **Combo** never fires. An Imbue can't store another Imbue.
- A spell imbues once, even if a mana storm, Focus of Echoes or wild surge repeats it.

## Tips and counterplay

| Spell | What you get |
|---|---|
| `Self · Imbue · Burst · Fire` (holding a sword) | Three hits that burst into flame. |
| `Self · Imbue · Lightning` (holding a bow) | Three arrows that call lightning. |
| `Self · Imbue · Heal` (holding a helmet) | Heal each time you're hurt, three times. |
| `Touch · Imbue · Root · Frost` | A floor trap that holds and freezes. |
| `Self · Imbue · Shock` (empty hands, at a door) | A door that jolts whoever opens it. |

- **Watch the ground.** Everyone within 24 blocks sees a glyph's faint circle.
- Imbuing earns the feat **Imbuer** and an advancement. Having a glyph go off earns **Tripwire**.
