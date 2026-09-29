---
title: The Cord Screen
parent: Spellcraft
nav_order: 2
description: "Every part of the Cord screen: the Codex and its search, threading runes, the readout, the magic circle preview, naming spells, spell codes, scrolls, and the Passives, Grimoire and Cosmetics pages."
---

# The Cord Screen
{: .no_toc }

Press **`K`** to open the Cord screen. It's where you thread runes into spells, and it explains every spell
in plain English, with its exact cost and cooldown, before you ever cast it.

1. TOC
{:toc}

---

## Opening and closing it

- **`K`** opens it (you can change the key in the game's Controls options). You need to be wearing a Cord;
  without one the screen just says "You aren't wearing a Cord" and where the slot is.
- It **doesn't pause the game**, even in single player. The world is dimmed behind it, the way the inventory
  dims it.
- **`Esc`** closes it. Because typing searches the Codex, `Esc` first clears the search, then lets go of the
  search box, and only then closes the screen. Pressing `K` again doesn't close it: it types a "k" into the
  search.
- On a small window or a large interface scale, the whole screen is scaled down to fit.

## The layout

<img src="{{ '/assets/images/a-cord-screen.jpg' | relative_url }}" alt="The Cord screen of an Echo Cord: the header with page tabs and three badges, five spell rows (the fifth for the Tome of the Fifth Page), family tabs and a search box, the Codex with rows for Personal, Direct, Projectile and Area shapes, and the readout; the selected spell's magic circle is open beside the window" class="shot">
<span class="caption">An Echo Cord's screen with the Tome of the Fifth Page in hand (so there's a fifth row). The selected spell's magic circle is open beside the window.</span>

From top to bottom:

1. **The header:** your Cord's name, the page tabs (**Spells**, **Passives**, **Grimoire**, **Cosmetics**),
   your Cord's limits ("12 sockets · 4 spells · Tier IV", when there's room) and three badges on the right:
   the **heart**, the **mana** crystal and the **help** mark.
2. **The spell rows:** one row per spell, numbered, with a socket for each rune.
3. **The family tabs and the search box:** All, Shapes, Effects, Modifiers, Links, and a box to type in.
4. **The category chips:** the categories of the chosen family, and how many runes match.
5. **The Codex:** every rune you know, in rows by category.
6. **The readout:** the selected spell explained, with four small tool buttons at its top right.

And beside the window, when there's room for it, the selected spell's **magic circle**.

## The header

Hover the **Cord's name** to see its limits. The **page tabs** switch between the four pages described below.

The three badges:

| Badge | Shows | Hover it for |
|---|---|---|
| **Heart** | How many Heart Circles you've formed (the number in its corner). A small gold light blinks on it when your heart is ready to form the next circle. | Your circles and what they give (max mana, mana a second, spell power), your passive slots, any circles cracked by overcasting and when they mend, your innate rune and leaning, the four perks (lit once you have them), and what the next circle still needs, each part ticked when it's done. |
| **Mana** | | Where your max mana comes from (your Cord, Mana Crystals, Reservoir, Heart Circles, a Focus of the Deep Well), where your regeneration comes from (your Cord, circles, Wellspring, Clarity, meditating, a ley line, a Wellstone), what your passives drain, your Siphon, the casting gear in your hands, and every way to grow your mana. |
| **Help** (the **?**) | | A reminder of how to thread spells and which keys cast, switch and open the screen (it shows your keys, even after you change them). |

See [Heart Circles]({{ '/progression/heart-circles/' | relative_url }}) and [Mana]({{ '/progression/mana/' | relative_url }}).

## The spell rows

Each row is one spell: its number on the left, then the cord threaded through its sockets.

- The **selected** row has a gold frame. **Clicking a row selects it**, and that also makes it the spell your
  cast key casts.
- A row shows as many sockets as your Cord has, and after them, when there's room, a faint note: "more
  sockets on bigger Cords". If a row still holds more runes than your Cord has sockets (kept from a bigger
  Cord), the extra sockets are drawn dim and their runes are quiet.
- A row your Cord doesn't have is **locked**: it says which Cord it needs ("Needs a Copper Cord") and, if it
  still holds runes from a bigger Cord, how many ("5 runes kept").
- With the [Tome of the Fifth Page]({{ '/gear/' | relative_url }}) in your offhand, a fifth row with a violet
  number appears for the tome's spell.

**Gold lines** under the sockets show what each modifier is attached to: a line joins the modifier to the
rune it changes. A modifier with **nothing** to change gets a small red mark instead (and the readout warns
you). This is the quickest way to check your order is right. See
[How modifiers find their rune]({{ '/spellcraft/reading-spells/' | relative_url }}#modifiers-find-their-rune).

A threaded rune with a **red corner** is **quiet**: it's kept, but it won't fire. Hover it to see why (too
strong for your Cord, past your Cord's last socket, not learned, or from an add-on mod that's missing). See
[Quiet runes]({{ '/spellcraft/cords/' | relative_url }}#quiet-runes-and-a-smaller-cord).

Hover any threaded rune for its tooltip: its name and rank, family, tier, element and category, what it
does, its rank bonus, what it does to the ground it lands on (burns grass, freezes water) if anything, the
[reaction]({{ '/spellcraft/reactions/' | relative_url }}) it sets up or sets off if it plays a part in one of the six
newer ones, and "Click to remove · drag to move". Long tooltips wrap to fit your window, so none runs off the side
of the screen.

## The Codex

The Codex is every rune you've learned, including any [Knots]({{ '/fusion-altar/knots/' | relative_url }}) you
know. Runes are grouped in rows by category, each row labelled on the left in its family's colour, and sorted
by family, then category, then (inside a category) the runes your Cord can fire first, then by tier and name.
Scroll it with the mouse wheel.

A rune your Cord **can't fire** is dimmed with a small lock, and its tooltip says which Cord it needs
("Needs a Copper Cord (Tier II)"). On the Passives page, runes that can't be part of a passive are dimmed the
same way.

### Family tabs and category chips

The tabs above the Codex show **All** runes, or just the **Shapes**, **Effects**, **Modifiers** or **Links**.
Knots are listed under All.

Pick a family and a row of **chips** appears with its categories. Click one to see only that category, or
**All** to see the whole family. A chip only appears once you know at least one rune in it, so you'll never
see an empty Summon chip before your first summon.

| Family | Categories |
|---|---|
| Shapes | Personal, Direct, Projectile, Area, Lingering |
| Effects | Damage, Control, Support, Movement, Time, World, Summon, Innate |
| Modifiers | Power, Area, Timing, Projectile |
| Links | Timing, Trigger, Reactive, Condition |

The number at the right of the chips row ("249 runes") is how many runes you know that match the tab, chip
and search.

### Searching

<img src="{{ '/assets/images/a-codex-search.jpg' | relative_url }}" alt="The Codex searched for star: five matching runes, with the tooltip of Starshard showing its family, tier, element, category and description, and the spell's magic circle beside the window" class="shot">
<span class="caption">Searching for "star". Hover a rune for its tooltip; "Click to add · drag onto a socket".</span>

**Just start typing** anywhere on the Spells or Passives page and the search box fills in (or press
`Ctrl`+`F`). The Codex narrows as you type.

- A search matches a rune's **name**, its **element** ("frost"), its **category** ("support"), its
  **family** ("modifier", "link") and its **tier** (type "tier3" for Tier III runes).
- **Several words** must all match: "storm control" finds storm runes in the Control category.
- Words of **five letters or more** also match the rune's **description**, so "armour" finds runes that
  mention armour. Shorter words don't, so "fire" finds fire runes, not every rune that fires something.
- `Backspace` deletes a letter, `Ctrl`+`Backspace` clears the search, and **right-clicking** the box clears
  it too. `Esc` clears it.
- If nothing matches, the Codex says so: No runes match "fusion".

## Threading runes

Every change is sent to the game and saved at once. The game checks each one: a change it won't allow is
refused with a low note, and the reason shows in yellow at the top of the readout for a few seconds.

| To | Do this |
|---|---|
| Add a rune to the end of the selected spell | **Click** it in the Codex |
| Put a rune in a particular place | **Drag** it from the Codex onto a socket (the runes after it move along) |
| Move a rune within a spell | **Drag** it along the row |
| Move a rune to another spell | **Drag** it onto the other row (it must fit there and be allowed) |
| Take a rune off | **Click** it, or **drag** it off the Cord |
| Pick a spell to edit | **Click** its row |

Either mouse button works. You only need to learn a rune once to thread it into every spell, and the same
rune can appear several times in one spell (`Amplify · Amplify`, `Push · Push`).

What gets refused, and what it says:

| Refusal | Why |
|---|---|
| Spell 2 needs a Copper Cord | That row is locked for your Cord. |
| Fire is too strong for this Cord: it needs a Copper Cord | The rune's tier is above your Cord's. |
| Your Twine Cord holds 3 runes per spell. Click a rune on the Cord to take it out, or drag it off. | The spell is full. |
| Heal can't be a passive | On the Passives page: only some runes can be passives. |

## The readout

The box at the bottom explains the selected spell, line by line:

1. **The spell's name**, in gold: its automatic name, the name you gave it, or, for a secret spell you've
   found, its true name in its own colour (with a line saying what the secret does).
2. **Its price**, in cyan: "64 mana · 3.2s cooldown · Cord holds 225". This is what the spell costs *you*,
   with every discount you have (Thrift, the Archmage perk, a staff or focus in hand, a mana storm, the
   server's rules) and every cooldown change (Rapid, Vow, Celerity, Flow). A
   [secret spell]({{ '/spellcraft/secret-spells/' | relative_url }}) you've found shows its own, higher price
   and longer cooldown; one you haven't found shows the ordinary spell's, and the cast that finds it costs
   just that. If the spell costs more than your whole mana pool,
   the line turns red and a warning says it "can't be cast" (you'd have to
   [overcast]({{ '/spellcraft/overcasting/' | relative_url }})). A spell with Blood Price shows its price in
   health instead.
3. **Casting gear** in your hands that changes the spell, in violet ("Arcane Staff: Arcane spells: +20%
   power, 10% less mana"), and a line if the server makes spells cost more or less than usual.
4. **What it does**, one line for each group of the spell: what the shape hits, then its effects and what the
   modifiers did to them ("3 bolts (homing): Fire (+50% power)"). After a link, a header line ("On hit:",
   "After 1s:", "When something hurts you:") and the rest indented beneath it.
5. **Warnings**, in yellow, each starting with **!**.

Scroll the readout with the mouse wheel when it's longer than the box; small gold arrows show there's more.
How to read the lines, and every rule behind them, is on
[How a Spell Is Read]({{ '/spellcraft/reading-spells/' | relative_url }}).

The warnings you'll see:

| Warning | What it means |
|---|---|
| Amplify does nothing here: nothing on its left that it can change. | The modifier found no rune it can change. Move it right of the rune you meant. |
| Bolt has no effect after it. | A shape with no effects hits things but does nothing to them. |
| On Hit has nothing after it. | A link with nothing after it. |
| On Hit needs a shape before it to watch. | On Hit, On Kill and Imbue need a shape in front of them. |
| Only 3 Echoes count; the rest are ignored. | A fourth Echo costs mana and does nothing. |
| Vow strengthens only Bolt's effects, and it has none: the cooldown is 4x longer for nothing. | Vow doubles only its own shape's effects. Put it on the shape whose effects you want stronger. |
| An Imbue can't store another Imbue. | See [Imbuing]({{ '/spellcraft/imbuing/' | relative_url }}). |
| Combo never fires in an imbued spell: every release counts as a first cast. | See [Imbuing]({{ '/spellcraft/imbuing/' | relative_url }}). |
| Costs more than this Cord's 100 mana, so it can't be cast. | Trim the spell, or grow your mana. |
| Costs more health than you have (20): trim the spell or drop Blood Price | A Blood Price spell you can't pay for. |

### The magic circle beside the window

When your game window has room to the right of the Cord screen, the selected spell's **magic circle** is
drawn there, laid out just like the circle you'll cast. It opens again, ring by ring, every time you change the
spell, so you can watch what each rune adds. It's the best way to learn to
[read magic circles]({{ '/spellcraft/magic-circles/' | relative_url }}).

## The spell tools

Four small buttons sit at the top right of the readout. Hover one for its name.

### Rename

Click **Rename** (the pencil), type a name, and press `Enter` to save it. `Esc` cancels, `Backspace`
deletes a letter and `Ctrl`+`Backspace` clears the name. Names can be up to 28 characters. Save an **empty**
name to go back to the automatic one. Picking another spell before you press `Enter` drops the name you were
typing, so it never lands on the wrong spell.

Your name shows on your spell panel, on the spell wheel, in the readout, and on scrolls you inscribe from the
spell. It's yours alone: a spell code you share carries the runes, not your name.

**Automatic names** are made from the spell's first group: its first modifier becomes an adjective, its
effects the element, its shape the noun.

| Spell | Automatic name |
|---|---|
| `Bolt · Fire · Split` | Splitting Fire Bolt |
| `Self · Heal · Amplify` | Greater Heal |
| `Burst · Explode · Widen` | Wide Blasting Burst |
| `Bolt · Fire · Burst · Heal` | Fire Bolt & Burst |
| `Bolt · Frost · Delay · Bolt · Fire` | Frost Bolt › Fire Bolt |
| `Bolt · Fire · Split · On Hit · Burst · Explode` | Splitting Fire Bolt+ |

A spell cast on yourself is named after its effects ("Swift", "Stoneskin & Empower"). After a link, the next
group follows a **›**; a second shape in the same segment follows an **&**; and a **+** means the name was too
long to say everything. Some effects have their own word: Heal is "Healing", Push "Gale", Pull "Drawing",
Harm "Arcane", Explode "Blasting", Launch "Rising", Grow "Verdant", Smite "Holy", Root "Rooting" and Break
"Breaking".

### Copy spell code

Copies the selected spell as a **spell code**, a short word that holds the whole spell, and says "Copied
wc:bolt.frost.split" above your hotbar. A code is `wc:` followed by the runes' names in lower case (spaces
become underscores), joined with dots, twelve runes at most:

`wc:bolt.fire.split.on_hit.burst.explode`

Paste a code into a chat message and everyone sees it as a coloured spell card, "[✦ Splitting Fire Bolt+]".
Hovering the card shows the spell's name, its runes, its mana and cooldown and its readout; clicking it
copies the code. See [Playing Together]({{ '/social/playing-together/' | relative_url }}).

### Paste spell code

Loads the first spell code on your clipboard into the selected spell, replacing what's there. Only runes you
know and your Cord can fire are loaded, up to your Cord's sockets. If everything fit you're told "Spell loaded
from the code"; otherwise how many runes were left out ("not known, too strong, or no socket free"). With no
code on the clipboard, it says so and changes nothing.

### Inscribe a scroll

Writes the selected spell onto a **Spell Scroll** that anyone can cast once, with or without a Cord, whether
or not they know its runes. Inscribing takes **a sheet of paper**, **an ink sac** (a glow ink sac works too)
and **twice what the spell costs you to cast**, as the readout shows it (your discounts, the server's cost
setting and a found secret's price all count). The scroll is called "Scroll of"
and the spell's name, and says who inscribed it. Right-click it to cast it; it's used up. It goes off at
plain strength: nobody's Heart Circles or enchantments count. In creative, inscribing is free. See
[Playing Together]({{ '/social/playing-together/' | relative_url }}).

## The Passives page

<img src="{{ '/assets/images/passives.png' | relative_url }}" alt="The Passives page: two passive rows with their upkeep and On switches, the total drain against regeneration, and the Codex with the runes that can't be passives dimmed" class="shot">
<span class="caption">The Passives page (shown here before the Grimoire and Cosmetics tabs were added): each passive's upkeep and switch, and the total drain against your regeneration.</span>

The **Passives** tab swaps the spell rows for your two **passive spells**, always-on spells that cost mana
every second instead of having a cooldown. It works like the Spells page, with a few differences:

- There are two rows. The first opens with your **1st Heart Circle**, the second with your **5th**; until
  then a row says "Opens with the 1st Heart Circle".
- Each row holds up to **5 runes** (3 on a Twine Cord).
- Each row has its upkeep ("0.7/s") and an **On/Off** switch. Click the switch to turn that passive on or off.
  If the runes break a passive rule, a yellow **!** takes the upkeep's place; hover it to see why.
- Under the rows, a line compares what your passives drain with what you regenerate, and turns yellow when
  they drain more.
- In the Codex, runes that can never be part of a passive are dimmed with a lock.
- The readout starts "Passive · 0.7 mana/s · renews every 2s · no cooldown" and has no spell tools.

The rules for what a passive may hold are on [Passive Spells]({{ '/spellcraft/passives/' | relative_url }}).

## The Grimoire page

<img src="{{ '/assets/images/a-grimoire-page.jpg' | relative_url }}" alt="The Grimoire page: your heart with innate rune and leaning, six of the eleven reactions found, and the secret spells as question marks" class="shot">
<span class="caption">The Grimoire page. (This picture is from before the full set of fused runes; the Fusions list now counts all 55.)</span>

The **Grimoire** tab is your book of discoveries. It lists, in order:

- **Your heart:** your innate rune (hover it to read it) and your leaning, or how to get them.
- **Reactions:** the eleven element reactions, "???" until you've set one off.
- **Secret spells:** found ones by name (hover for what they do), ones you've read the riddle of as that
  riddle, and "???" for the rest.
- **Duels:** your wins and losses, once you've fought one.
- **Fusions:** every fused rune you've made at the Fusion Altar, and the ones you haven't as "??? + ???" with a
  hint of one element.
- **Attunements:** the lands you've drawn a rune from, and riddles for the rest.
- **Runes of the world:** every place a rune of the world is found, with the ones you know by name.
- **Feats:** every feat, ticked once you've done it. Hover one for what it asks.

Scroll it with the mouse wheel. Beside the window, the circles of the secret spells you've found turn one
after another, each with its name beneath like a plate in a book. Every new discovery also pops up as a toast
in the corner of your screen. More on [The Grimoire]({{ '/progression/grimoire/' | relative_url }}).

## The Cosmetics page

<img src="{{ '/assets/images/a-cosmetics-page.jpg' | relative_url }}" alt="The Cosmetics page: a turning figure wearing the Cord on the left; Beads, Glow and Cast trail choices on the right; the Amethyst beads' tooltip saying they cost 4 Amethyst Shards" class="shot">

The **Cosmetics** tab changes how your Cord looks to everyone around you. Your figure turns slowly on the
left, wearing your Cord. On the right you choose:

- **Beads:** the material of the beads on your wrist.
- **Glow:** your spells' own colours, or one fixed colour.
- **Cast trail:** a trail your casts leave behind.

**Point at any option, even a locked one**, and your figure shows it. Locked options say how to earn them;
the ones bought with materials are bought with a click, and are yours for good. Your choice is saved at once.
Every option and its price is on [Cosmetics]({{ '/companions/cosmetics/' | relative_url }}).

## Quick reference

| Where | Input | Does |
|---|---|---|
| Anywhere | `K` | Open the screen |
| Anywhere | `Esc` | Clear the search, let go of the search box, then close |
| Codex | Click a rune | Add it to the end of the selected spell |
| Codex | Drag a rune onto a socket | Insert it there |
| Spell row | Click the row | Select that spell (for editing and casting) |
| Spell row | Click a rune | Take it off |
| Spell row | Drag a rune | Move it; drag it off the Cord to take it off |
| Spells or Passives page | Type | Search the Codex |
| Spells or Passives page | `Ctrl`+`F` | Put the cursor in the search box |
| Search box | Right-click | Clear the search |
| Search box | `Ctrl`+`Backspace` | Clear the search |
| Codex, readout, Grimoire | Mouse wheel | Scroll |
| Renaming | `Enter` / `Esc` | Save the name / cancel |
| Passives page | Click On/Off | Switch that passive |
