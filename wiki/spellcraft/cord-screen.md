---
title: The Cord Screen
parent: Spellcraft
nav_order: 2
description: "The Cord screen: the Codex and search, threading runes, the readout, naming spells, spell codes, scrolls, loadouts, and the Passives, Grimoire and Cosmetics pages."
---

# The Cord Screen

## What it is

The Cord screen is where you thread runes into spells. It explains each spell in plain words, with its exact
cost and cooldown, before you ever cast it.

## How to get it

Press **`K`** (change it in Controls). The screen doesn't pause the game.

Without a Cord, the screen says "You aren't wearing a Cord" and where the slot is. The **Aura** badge still
works, and if you've copied the Relay Circle lesson you can still read it from here.

## How to use it

<img src="{{ '/assets/images/a-cord-screen.jpg' | relative_url }}" alt="The Cord screen of an Echo Cord: the header with page tabs and badges, five spell rows, family tabs and a search box, the Codex, and the readout; the selected spell's magic circle is open beside the window" class="shot">

From top to bottom:

1. **The header:** your Cord's name, the page tabs (**Spells**, **Passives**, **Grimoire**, **Cosmetics**) and
   the badges.
2. **The spell rows:** one row per spell, with a socket for each rune and a **rank badge** at the end.
3. **Family tabs and search:** All, Shapes, Effects, Modifiers, Links, the search box and the **loadouts** badge.
4. **Category chips** for the chosen family.
5. **The Codex:** every rune you know.
6. **The readout:** the selected spell explained, with four tool buttons.

When there's room, the selected spell's **magic circle** is drawn beside the window. It redraws ring by ring
each time you change the spell, which is a good way to learn
[to read circles]({{ '/spellcraft/magic-circles/' | relative_url }}).

### The badges

| Badge | Shows | Hover or click |
|---|---|---|
| **Aura** (a blade) | Your sword side. | Click to open the Aura screen and your [Master forms]({{ '/masters/master-forms/' | relative_url }}). |
| **Shield** | A dot while your spellguard recharges. | How much less spells hurt you, and why. See [Defending Against Magic]({{ '/progression/defence/' | relative_url }}). |
| **Heart** | Your Heart Circle count. A gold light blinks when the next circle is ready. | What your circles give and what the next one still needs. See [Heart Circles]({{ '/progression/heart-circles/' | relative_url }}). |
| **Mana** | | Where your max mana and regeneration come from. See [Mana]({{ '/progression/mana/' | relative_url }}). |
| **?** | | Which keys cast, switch and open the screen. |

### Threading runes

| To | Do this |
|---|---|
| Add a rune to the end of the selected spell | **Click** it in the Codex |
| Put a rune in a set place | **Drag** it onto a socket |
| Move a rune | **Drag** it along the row, or onto another row |
| Take a rune off | **Click** it, or drag it off the Cord |
| Pick the spell to edit and cast | **Click** its row |

Every change is saved at once. A change that isn't allowed plays a low note and shows why at the top of the
readout, for example "Fire is too strong for this Cord: it needs a Copper Cord".

- **Gold lines** under the sockets join each modifier to the rune it changes. A red mark means it has nothing
  to change. See [How modifiers find their rune]({{ '/spellcraft/reading-spells/' | relative_url }}#modifiers-find-their-rune).
- A rune with a **red corner** is [quiet]({{ '/spellcraft/cords/' | relative_url }}#how-to-use-it): kept, but
  it won't fire.
- A just-learned rune shows a **?** and a hint instead of its full text. See
  [Reading runes]({{ '/spellcraft/harmonies/' | relative_url }}).

### Finding runes

Runes are grouped by family and category. A rune your Cord can't fire is dimmed with a lock.

| Family | Categories |
|---|---|
| Shapes | Personal, Direct, Projectile, Area, Lingering |
| Effects | Damage, Control, Support, Movement, Time, World, Summon, Innate |
| Modifiers | Power, Area, Timing, Projectile, Circle disciplines |
| Links | Timing, Trigger, Reactive, Condition |

**Just start typing** to search (or press `Ctrl`+`F`). A search matches a rune's name, element, category,
family or tier ("tier3"). Several words must all match. Words of five letters or more also match descriptions.
`Ctrl`+`Backspace` or right-click clears the search, and `Esc` clears it, then leaves the box, then closes the
screen. Searches are capped at 40 characters.

### The readout

The readout explains the selected spell, line by line:

1. **The name**: automatic, yours, or a found secret spell's true name.
2. **The price**: mana, cooldown and your Cord's mana ("64 mana · 3.2s cooldown · Cord holds 225"), with all
   your discounts counted. It turns red if the spell costs more than your whole pool.
3. **Mastery**: the spell's rank and progress. See [Spell Mastery]({{ '/spellcraft/mastery/' | relative_url }}).
4. **Gear and affinity** that change the spell.
5. **What it does**: one line per group, with a header after each link ("On hit:", "After 1s:").
6. **Warnings** in yellow, each starting with **!**.

A lesson spell missing a part says "Unfinished" and the lesson's name, and can't be cast.

| Common warning | Fix |
|---|---|
| Amplify does nothing here: nothing on its left that it can change. | Move the modifier right of the rune you meant. |
| Bolt has no effect after it. | Add an effect after the shape. |
| On Hit needs a shape before it to watch. | On Hit, On Kill, On Reaction, On Weakness and Imbue need a shape before them. |
| Only 3 Echoes count; the rest are ignored. | Remove the extra Echo. |
| Costs more than this Cord's 100 mana, so it can't be cast. | Trim the spell or grow your mana. |

The full rules are on [How a Spell Is Read]({{ '/spellcraft/reading-spells/' | relative_url }}).

### The spell tools

Four buttons sit at the top right of the readout.

- **Rename:** type a name (up to 28 characters) and press `Enter`. Save an empty name to go back to the
  automatic one. Your name is yours alone: a shared code carries only the runes.
- **Copy spell code:** copies a short code such as `wc:bolt.fire.split.on_hit.burst.explode`. Paste it in chat
  and everyone sees a spell card they can hover and click. See
  [Playing Together]({{ '/social/playing-together/' | relative_url }}).
- **Paste spell code:** loads the code on your clipboard into the selected spell. Only runes you know and your
  Cord can fire are loaded. Lesson spells only paste if you meet every lesson requirement.
- **Inscribe a scroll:** writes the spell on a **Spell Scroll** that anyone can cast once, Cord or not. It takes
  a sheet of paper, an ink sac (or glow ink sac) and **twice the spell's mana cost**. Scrolls cast at plain
  strength. A spell of **Adept** rank or higher also carries its traits and sigil, so a friend can
  [study it]({{ '/spellcraft/mastery/' | relative_url }}). Lesson spells can't be inscribed.

**Automatic names** come from the first group: the first modifier becomes an adjective, the effect the
element, the shape the noun. `Bolt · Fire · Split` is "Splitting Fire Bolt". A **›** marks the group after a
link, **&** a second shape and **+** a name too long to say in full.

The Spells page also shows how many builds you've saved in your spell library ("Saved spells: 3 / 24"). See
[Research]({{ '/progression/research/' | relative_url }}).

### Loadouts and mastery

- Click the **list badge** or press `Ctrl`+`L` to open [Loadouts]({{ '/spellcraft/loadouts/' | relative_url }}).
- Click a row's **rank badge** or press `Ctrl`+`M` to open the spell's mastery panel. A gold dot means a trait
  is waiting to be chosen.

### The Passives page

<img src="{{ '/assets/images/passives.png' | relative_url }}" alt="The Passives page: two passive rows with their upkeep and On switches, the total drain against regeneration, and the Codex with the runes that can't be passives dimmed" class="shot">

Two rows for your [passive spells]({{ '/spellcraft/passives/' | relative_url }}). The first opens at your 1st
Heart Circle, the second at your 5th. Each holds 2 runes, shows its upkeep per second and has an **On/Off**
switch. A line underneath compares your total drain with your regeneration.

### The Grimoire page

<img src="{{ '/assets/images/a-grimoire-page.jpg' | relative_url }}" alt="The Grimoire page: your heart with innate rune and leaning, reactions found, and the secret spells as question marks" class="shot">

Your book of discoveries. It holds:

- **Your heart**: innate rune and leaning.
- **Archive lessons**: Relay, Reweave, Excise, Tollgate, Lifeline and Conduit, once copied. Click one to read it.
- **Life journal**, **Aura arts** and **Where you stand** (the elemental climate around you).
- **Bestiary**, **Affinities** and **Reactions**.
- **Secret spells**: found ones by name, riddles you've read, "???" for the rest.
- **Duels**, **Fusions**, **Attunements** and **Feats**.

More on [The Grimoire]({{ '/progression/grimoire/' | relative_url }}).

### The Cosmetics page

<img src="{{ '/assets/images/a-cosmetics-page.jpg' | relative_url }}" alt="The Cosmetics page: a turning figure wearing the Cord on the left; Beads, Glow and Cast trail choices on the right" class="shot">

Change your Cord's **Beads**, **Glow** and **Cast trail**. Point at any option, even a locked one, to preview it.
See [Cosmetics]({{ '/companions/cosmetics/' | relative_url }}).

## Tips and counterplay

- Watch the **gold lines**: they're the quickest way to check your rune order.
- Use the magic circle beside the window to learn how each rune looks, so you can read other players' spells.
- **Quick keys:** `K` open, `Ctrl`+`F` search, `Ctrl`+`L` loadouts, `Ctrl`+`M` mastery, `Esc` back out.
