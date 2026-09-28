---
title: Imbuing and Glyphs
parent: Spellcraft
nav_order: 9
---

# Imbuing and glyphs
{: .no_toc }

<img src="{{ '/assets/images/imbue-sword.jpg' | relative_url }}" alt="A player holds an iron sword over a teal magic circle; the hotbar line reads Iron Sword holds Fire (3 charges), released at what it strikes" class="shot">
<span class="caption">Imbuing a sword with Fire.</span>

**Imbue** stores a spell instead of casting it. Put it in a sword and your next three hits burst into
flame. Put it in a bow and your next three arrows carry lightning. Write it onto a door, a pressure
plate or a patch of floor and it becomes a **glyph**: a trap that goes off at whoever steps on it,
opens it or breaks it.

1. TOC
{:toc}

## The Imbue rune

<img src="{{ '/assets/runes/imbue.png' | relative_url }}" alt="" width="32" height="32" class="rune-icon"> **Imbue** is a Tier II **link** that costs 3 mana and needs a Copper Cord or
better. Craft it from a Blank Rune and an Experience Bottle, plus 2 Lapis Lazuli and a Gold Ingot (see
[Rune Recipes]({{ '/items/rune-recipes/' | relative_url }})).

Like every link, it splits the spell in two:

- **Before Imbue**: a shape that decides **what** gets the magic. `Self` means the item in your hand
  (or, with both hands empty, the block you're looking at). Any other shape means the block it
  touches.
- **After Imbue**: the part that's **stored**. It isn't cast now. It goes off later, when the item or
  glyph is set off, from wherever that happens.

`Self · Imbue · Burst · Fire` stores `Burst · Fire` in the item in your hand.

## Charges and price

- Everything imbued holds **3 charges**: it lets the stored part go three times, then the magic is
  spent.
- You pay for all three up front: the stored part costs **three times** its usual mana, plus the
  Imbue rune itself. The Cord screen's readout shows the full price, headed *"Stored in the item in
  your hand (3 charges), then:"*.
- **Releasing costs no mana.** That's the point: you can fight on while your mana refills.
- A release **never Siphons** mana back: it was paid for when you imbued it.
- A released spell hits with your strength at the moment it goes off: your
  [Heart Circles]({{ '/progression/heart-circles/' | relative_url }}), your Cord's enchantments and your
  leaning count, charging and rhythm don't.

## The shared cooldown

Stored magic isn't a way around cooldowns:

- **Everything you've imbued shares one cooldown.** After any of your items or glyphs lets go, none
  of them can let go again until the stored spell's own cooldown has passed, as long as it would be
  for you to cast (Rapid, Vow, Celerity and the Flow perk all count), and never less than **half a
  second**. A sword, a bow and a helmet can't take turns to cast faster than your Cord could.
- A bow shot while the magic is recharging is just an arrow, and keeps its charge.
- Using an imbued "anything else" item while it recharges tells you how long is left
  (*"Your imbued magic is still recharging"*).

## Items

With `Self`, the magic goes into the item in your **main hand** (or your off-hand if your main hand is
empty). If you're holding a stack, one item is taken off it and imbued, so charges can't be copied by
splitting stacks. Imbuing an item that already holds magic replaces it.

How the item lets its spell go depends on what it is:

| Item | When it lets go | Where |
|---|---|---|
| **Weapons** (swords, maces, tridents...) | When you strike a creature with it in your main hand | At what you struck |
| **Tools** (pickaxes, axes, shovels, hoes...) | Each block you break with it, and each creature you strike | At the block, or at what you struck |
| **Bows and crossbows** | The next shots it fires (from either hand) | Where the arrow lands, in a creature or a block |
| **Armour and shields** | Whenever a creature hurts you (a blocked hit counts) | At whoever hurt you |
| **Blocks** | When you place it | It becomes a [glyph](#glyphs) holding the magic |
| **Anything else** (a stick, a bone, a book, a feather...) | When you use it (right-click) | At the creature or block you're looking at (up to 24 blocks away); or, if the stored part starts with a shape, from you like a normal cast |

- **Helpful magic goes to the holder.** If the stored part only helps (a Heal, a Shield, a Swift), a
  weapon, tool or piece of armour lets it go on **you**, not on the foe you struck or the block you
  broke. A sword imbued with `Heal` heals you as you fight.
- **Bows:** each shot that carries the spell uses a charge. A crossbow's triple shot (Multishot) spends one
  charge, and **only one of its three arrows carries the spell**; the other two are plain arrows. Imbued arrows
  trail motes of the spell's colour.
- **Armour:** you only need one imbued piece. If you wear several, the first one found (head, chest,
  legs, feet, then your hands) lets go.
- An item whose stored part starts with an effect, used while you look at nothing, tells you *"Look at
  something to release it at"* and keeps its charge.

Imbued items glint like enchanted ones, and their tooltip says what they hold (*"Imbued: Fire"*), how
many charges are left, and how they let go. When the last charge is used you're told *"The magic in
your ... is spent"* and the glint goes.

**Cords, runes, Knots and Spell Scrolls can't be imbued** (*"That can't hold a spell"*).

### Six items at a time

You can keep up to **6 imbued items** charged at once. Imbuing a seventh lets your **oldest** one's
magic fade: you're told *"Your oldest imbued item's magic fades"*, and the next time that item would
let go it's a plain item again (*"The magic in ... has faded"*). An item stops counting when its
charges run out, when you imbue it again, or when you place it as a glyph. This keeps anyone from
filling a chest with charged swords.

## Glyphs

<img src="{{ '/assets/images/imbue-glyph.jpg' | relative_url }}" alt="A teal glyph circle flares on the grass under a husk, and ice shards burst up around it" class="shot">
<span class="caption">A husk steps onto a frost glyph.</span>

**Any block at all** can hold magic. A block with a spell in it is a **glyph**: a faint copy of the
stored spell's magic circle on the face that was struck, sized to fit (small on a button, a torch or a
flower; full size on a floor).

### Making one

| How | Example |
|---|---|
| A shape that touches a block | `Touch · Imbue · Root · Frost` on the floor in front of you |
| A shape that hits a creature: the glyph goes into the block under it | `Bolt · Imbue · Shock` at a zombie writes a glyph where it stands |
| `Self` with **both hands empty**: the block you're looking at | `Self · Imbue · Shock`, looking at a door |
| An imbued **block item**, placed | Imbue an oak plank with `Self`, then place it anywhere |

You need the right to build where the glyph goes: spawn protection and land claims stop it (*"You
can't imbue a block here"*). A block full of water can't take one.

### What sets a glyph off

| Trigger | Who it goes off at |
|---|---|
| A creature **steps on it or touches** the face it's on | That creature |
| Someone **uses** the block: opens the door, chest or trapdoor, presses the button, pulls the lever | Whoever used it |
| Anything **shoots** the block | From the block itself |
| Someone else **breaks** it | Them, first |
| The block is **powered** by redstone (the moment the power comes on) | From the block itself |

A glyph only answers to the creatures its spell is for: **enemies** for a harmful spell, **you and your
allies** for a helpful one (a spell with both kinds of effect answers to both). A pressure plate
holding `Heal` heals your friends and ignores zombies; a floor holding `Frost` freezes zombies and lets
your friends walk over it.

### Re-arming

- A glyph re-arms no faster than its spell could be cast again, and never in under **1 second**.
- A creature that **stays** on a glyph is caught only once: it goes off at that creature again only
  after it steps off and back on. Another creature stepping on sets it off as soon as it's re-armed.
- Glyphs share your **imbued cooldown** with everything else you've imbued, so a dozen glyphs on one
  redstone line go off one at a time, not all at once.
- Picking a glyph up and putting it down again doesn't reset its re-arm.

### Twelve glyphs, kept with the world

- Each caster keeps at most **12 glyphs**. Writing a 13th lets your oldest fade.
- Glyphs are saved with the world and cast **as you**: your power, your allies. So they **sleep while
  you're away**: offline, dead, spectating or in another dimension. Everyone within 24 blocks sees a
  glyph's faint circle, fainter still while it sleeps.
- If the block it's written on is **replaced or destroyed** (by anything other than you breaking it),
  the glyph fades.

### Portable traps

Break **your own** glyph and the block you get back **still holds its spell and the charges it had
left**, and glints (as long as the block drops itself: glass or leaves broken by hand won't). Carry it
and place it wherever you need a trap. While you carry it, it counts as one of your 6 imbued items;
placed, it's one of your 12 glyphs again. Its re-arm wait comes with it.

Anyone else breaking your glyph sets it off at them first (if it's for them), and then its magic
fades. So does a glyph whose last charge is used.

<img src="{{ '/assets/images/glyph.gif' | relative_url }}" alt="A glyph's faint circle on the ground flares as a creature walks over it" class="shot">

## Inside a stored spell

- An **Echo** in the stored part repeats only what was stored, at the same target.
- **Combo** never fires in a stored spell: every release counts as a first cast. The Cord screen warns
  you.
- An Imbue can't store another Imbue (*"An Imbue can't store another Imbue"*).
- A spell cast once can Imbue once: a mana storm's echo, Twin Star's second go, a Focus of Echoes or a
  wild surge's second go never imbues a second item for the same price.
- A stored part that starts with an **effect** lands on whatever set it off. One that starts with a
  **shape** is cast from there like a new spell: `Self · Imbue · Burst · Explode` in a sword bursts
  around the creature you struck.

## Ideas

| Spell | What you get |
|---|---|
| `Self · Imbue · Burst · Fire` (holding a sword) | Your next three hits burst into flame around what you strike. |
| `Self · Imbue · Break · Widen` (holding a pickaxe) | Three blocks you mine each open a wide hole. |
| `Self · Imbue · Lightning` (holding a bow) | Three arrows that call lightning where they land. |
| `Self · Imbue · Push` (holding a chestplate, then wear it) | Whatever hits you is thrown back, three times. |
| `Self · Imbue · Heal` (holding a helmet) | Wear it: every time you're hurt, you heal, three times. |
| `Touch · Imbue · Root · Frost` | A trap on the floor that holds and freezes whoever walks over it. |
| `Self · Imbue · Shock` (empty hands, looking at a door) | A door that jolts whoever opens it. |
| `Self · Imbue · Heal` (empty hands, looking at a pressure plate) | A plate that heals your friends. |
| `Self · Imbue · Lightning` (empty hands, looking at a button) | A button that throws lightning at whoever presses it. |
| `Self · Imbue · Root · Shock` (holding a plank) | A plank you carry around and place wherever you need a trap. |

## Feats, advancements and contracts

- **Imbuer**: imbue a spell into an item or a block (a Grimoire feat, 250 mana toward your next Heart
  Circle; advancement, 20 experience).
- **Tripwire**: have one of your glyphs go off (advancement, 25 experience).
- The Runesmith sometimes offers a [contract]({{ '/social/contracts/' | relative_url }}): *"Imbue a
  weapon and use all its charges"*, for a Mana Crystal.
