# Loadouts


A Cord set up for a boss fight is no good in a mine. A **loadout** is your whole Cord saved under a name, so
you can switch between setups in a moment instead of threading every rune again: one for fighting, one for
mining, one for exploring, one for keeping your friends alive.

![The loadouts panel over the Cord screen: three saved loadouts named Fighting, Mining and Helping friends, each with its first runes and Load, save, rename and delete buttons, and a row to save the current Cord as a new one](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/loadouts-panel.jpg)
<span>The loadouts panel: three saved, room for three more.</span>


---

## What a loadout keeps

Everything you thread on the Cord screen:

- every spell's runes, in order, the tome's fifth spell included;
- every spell's name, if you gave it one;
- your passives, and which of them are switched on;
- which spell is selected (the one your cast key casts).

You can keep **six** loadouts. They're yours alone, and they stay with you when you die, like your spells.

A loadout remembers **which runes went where**, not the runes themselves. Loading one never gives you a rune:
see [What loading does](#what-loading-does).

## The loadouts panel

Open the [Cord screen](cord-screen.md) (`K`) and click the little **list
badge** at the right end of the row of family tabs, just under the **?** badge (or press `Ctrl`+`L`). A panel
opens over the window with your loadouts, one to a row:

- its number and **name** (the number is cyan for the one you loaded last);
- the first few **runes** of its selected spell;
- four buttons: **Load**, **Save current here** (⇩), **Rename** (✎) and **Delete** (✕).

Hover a loadout's name for everything in it: each spell with its name and runes, each passive (and whether
it's on), and what loading does. A rune you don't know is struck through.

The first empty row holds **+ Save current as new**. Click it, type a name (it offers "Loadout 3" and so on),
and press `Enter`.

| To | Do this |
|---|---|
| Save your Cord as a new loadout | **+ Save current as new**, type a name, `Enter` |
| Put a loadout on your Cord | **Load** |
| Save your Cord over a loadout, keeping its name | **Save current here** (⇩), then click it again to be sure |
| Rename a loadout | **Rename** (✎), type, `Enter` (`Esc` cancels) |
| Forget a loadout | **Delete** (✕), then click it again to be sure |
| Close the panel | `Esc`, the **×**, or click outside it |

Saving over a loadout and deleting one both ask for a second click within three seconds (the row says "Click
again to save over Fighting"), so a stray click never loses a setup. Deleting a loadout never touches your Cord: it keeps what it
holds now.

Names can be up to 24 letters, spaces included, and two loadouts can't share a name (whatever the capitals).

### With the keyboard

| Key | Does |
|---|---|
| `Ctrl`+`L` | Open or close the panel |
| `↑` / `↓` | Pick a loadout (it gets a gold frame) |
| `Enter` | Load it |
| `Ctrl`+`R` | Rename it |
| `Delete` | Delete it (press twice) |
| `Ctrl`+`S` | Save your Cord over it (press twice) |
| `Ctrl`+`N` | Save your Cord as a new loadout |
| `Esc` | Stop typing a name, or close the panel |

While the panel is open, typing never searches the Codex behind it.

## What loading does

Loading puts the loadout's spells, names, passives, switches and selected spell on your Cord, replacing what
was there. You're told *"Loaded Fighting"* above your hotbar, and the Cord screen shows the loaded spells.

**Runes you don't know or your Cord can't hold stay quiet.** They're put back where they were, but they
don't fire, exactly as when you [change to a smaller Cord](cords.md#quiet-runes-and-a-smaller-cord):

- a rune you **don't know** any more (or one from an add-on that's been removed);
- a rune **too strong** for the Cord you're wearing (a Tier III rune on a Copper Cord);
- runes past your Cord's **last socket**, and whole spells your Cord **doesn't have** (spell 4 on an Amethyst
  Cord).

They show with a red corner, as quiet runes always do, and wake up by themselves once you learn the rune or wear
a bigger Cord. When any stay quiet you're told how many: *"Loaded Fighting. 3 runes stay quiet: not learned,
too strong for this Cord, or past its sockets"*.

So a loadout saved on your Echo Cord still loads on a Twine Cord: only what the Twine Cord can fire fires, and
nothing is lost.

### Cooldowns

**Every spell that changes starts its cooldown**, as if you'd just cast it, unless it was cooling down already
(then it keeps the time it had). A spell that stays the same keeps its cooldown, or its readiness, as it was.
So swapping loadouts mid-fight never gets you a fresh spell to fire straight away, and never cuts a cooldown
short.

A passive whose runes change pays its first second of upkeep as it starts, as it always does when it changes.

### When you can't

Loading (from the panel, the key or `/loadout load`) is refused, and you're told why:

| Situation | Message |
|---|---|
| You're charging a spell | You can't change loadouts while charging a spell |
| You're in a duel, countdown included | You can't change loadouts during a duel |
| You're sealed in a [Cryostasis](../runes/fused.md) | You can't change loadouts while sealed in ice |
| You aren't wearing a Cord | You aren't wearing a Cord |

Saving, renaming and deleting always work.

## Switching with a key

The **Next loadout** key loads your next loadout, round and round in the order you saved them, and names it
above your hotbar: *"Loadout: Mining (2 of 4)"*. It has no key to begin with: set one in **Options → Controls →
Key Binds**, under **Wildercord**. The same rules apply: changed spells start their cooldowns, and it won't
switch while you charge, duel or sit in the ice.

## From chat

| Command | Does |
|---|---|
| `/loadout save <name>` | Save your Cord as a loadout. A name you already use is saved over. |
| `/loadout load <name>` | Load a loadout. |
| `/loadout delete <name>` | Forget a loadout. |
| `/loadout list` (or `/loadout`) | List your loadouts: click one to load it. The one you loaded last has a dot. |

Names may have spaces (`/loadout save boss fight`), capitals don't matter, and your loadouts are suggested as you
type.

## Ideas

| Loadout | Spells |
|---|---|
| Fighting | Your strongest attack selected, a Shield spell, a heal, and a Stoneskin passive. |
| Mining | `Beam · Break` selected, `Self · Night Eye · Haste`, and `Self · Swift` as a passive. |
| Exploring | Movement (Leap, Blink, Feather Fall, and Soar to fly over what's in the way) and Night Eye as a passive. |
| Helping friends | Healing and buffs that reach your allies, and a Regrowth passive. |
