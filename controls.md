---
title: Controls
nav_order: 12
description: "Every Wildercord key with its default, the controls inside the Cord screen and the spell wheel, and the commands players and server operators use."
---

# Controls
{: .no_toc }

1. TOC
{:toc}

---

## Keys

Wildercord's keys are in the game's **Options → Controls → Key Binds**, under their own **Wildercord**
heading, and every one can be changed. The help badge (**?**) in the Cord screen always shows your current
keys.

| Key | Default | What it does |
|---|---|---|
| Cast spell (hold to charge) | `R` | **Tap** to cast your selected spell at once. **Hold** to charge it (up to +40% power at a full charge), then let go to cast. See [Casting]({{ '/spellcraft/casting/' | relative_url }}#tap-or-charge). |
| Next spell (hold for the wheel) | `V` | **Tap** to select your next spell. **Hold** for the spell wheel (with two or more spells). See [Switching spells]({{ '/spellcraft/casting/' | relative_url }}#switching-spells). |
| Open Cord | `K` | Opens the [Cord screen]({{ '/spellcraft/cord-screen/' | relative_url }}). |
| Cast spell 1 | not set | Casts spell 1 straight away, without selecting it. Taps only: no charging. |
| Cast spell 2 | not set | The same for spell 2. |
| Cast spell 3 | not set | The same for spell 3. |
| Cast spell 4 | not set | The same for spell 4. |
| Cast spell 5 (the tome's) | not set | The same for the fifth spell, while the Tome of the Fifth Page is in your offhand. |

A key press counts as a **hold** once it's been down for a quarter of a second. Shorter than that is a tap.

## Other things you do with the usual keys

| Do | To |
|---|---|
| Put a Cord in the slot above your offhand (inventory, `E`), or shift-click it | Wear it |
| Use (right-click) a rune | Learn it for good |
| Use a Knot | Learn it (anyone can, even without the runes inside) |
| Use a Mana Crystal | Absorb it: +10 max mana, up to 10 crystals |
| Use a Spell Scroll | Cast the spell on it, once |
| Use a Torn Page | Read the riddle of a secret spell |
| Sneak and stand still, wearing a Cord | Meditate: mana comes back twice as fast. When your heart is ready, meditate for 10 seconds without getting hurt to form a Heart Circle |
| Meditate with a Blank Rune in hand, in the right land at the right moment | Attune it (see [Runes of the World]({{ '/runes/world/' | relative_url }})) |
| Sneak and punch a Training Dummy | Pick it back up |
| Sneak and use your familiar | Tell it to stay, or to follow again |
| Use a Wisp Lantern / sneak and use it | Send your familiar home or call it out / call out the next one |

## In the Cord screen

| Input | Does |
|---|---|
| Click a rune in the Codex | Add it to the end of the selected spell |
| Drag a rune from the Codex onto a socket | Insert it there |
| Click a threaded rune | Take it off |
| Drag a threaded rune | Move it within the spell, onto another spell, or off the Cord to take it off |
| Click a spell's row | Select it for editing and for casting |
| Just type, or `Ctrl`+`F` | Search the Codex |
| `Backspace` / `Ctrl`+`Backspace` | Delete a letter / clear the search |
| Right-click the search box | Clear the search |
| Mouse wheel | Scroll the Codex, the readout or the Grimoire |
| Click a family tab or a category chip | Filter the Codex |
| Click the page tabs | Spells, Passives, Grimoire, Cosmetics |
| Click a passive's On/Off | Switch that passive |
| The four buttons over the readout | Rename the spell, copy its spell code, paste a spell code, inscribe a scroll |
| `Enter` / `Esc` while renaming | Save the name / cancel |
| `Esc` | Clear the search, then let go of the search box, then close the screen |

Either mouse button works for threading. Full details on [The Cord Screen]({{ '/spellcraft/cord-screen/' | relative_url }}).

## In the spell wheel

| Input | Does |
|---|---|
| Hold `V`, point at a spell, let go | Select it |
| Let go of `V` without pointing | The wheel stays open |
| Click a spell | Select it |
| Press a spell's number (`1` to `4`, or `5` for the tome's) | Select it |
| Point, then press `V` or `Enter` | Select the one you're pointing at |
| Right-click, or `Esc` | Close without choosing |

## Commands for players

Anyone can use these.

| Command | Does |
|---|---|
| `/duel <player>` | Challenge another player to a duel. They have 30 seconds to answer, and must be within 40 blocks. |
| `/duel accept <player>` | Accept that player's challenge. (Clicking **[Accept]** in the challenge message does the same.) |
| `/duel decline <player>` | Turn the challenge down. (Or click **[Decline]**.) |
| `/duel stats` | Your wins and losses in duels. |
| `/duel stats <player>` | Another player's. |

Nobody dies in a duel, and afterwards both duellists are put back as they were. The rules are on
[Duels]({{ '/social/duels/' | relative_url }}). Pasting a spell code (`wc:bolt.frost.split`) into chat isn't a
command, but everyone sees it as a spell card: see [Playing Together]({{ '/social/playing-together/' | relative_url }}).

## Commands for operators

These need operator permission (permission level 2): a server's operators, or anyone in a single-player world
with cheats on. They're meant for testing, events and fixing things up.

| Command | Does |
|---|---|
| `/wildercord learnall` | Learn every rune. |
| `/wildercord learn <rune>` | Learn one rune, by its name in lower case with underscores: `stasis`, `on_hit`, `feather_fall`. |
| `/wildercord spell <1-5> <runes...>` | Thread a whole spell at once, replacing what's there: `/wildercord spell 1 bolt fire split`. Runes named here are learned too. Your Cord's sockets and tier still apply. Spell 5 is the tome's. |
| `/wildercord mana` | Fill your mana. |
| `/wildercord circles <0-8>` | Set how many Heart Circles you have. |
| `/wildercord condense <mana>` | Add condensed mana toward your next Heart Circle. |
| `/wildercord innate <rune>` | Choose your innate rune (and learn it): `phantom`, `twin_star`... |
| `/wildercord runebound` | Bind the nearest monster (within 16 blocks) to a Cord, making it a Runebound. |
| `/wildercord event mana_storm` | Start a mana storm over the nearest ley line (within 48 blocks). |
| `/wildercord event starfall` | Send a fallen star down 60 to 150 blocks away. |
| `/wildercord event rift` | Open a rift siege a little way off. Only one rift can be open in a world at a time. |
| `/wildercord event <kind> here` | Start any of the three right where you stand. |
| `/wildercord reset` | Forget every rune and spell (you learn the three starter runes again the next time you wear a Cord). Heart Circles and everything else are kept. |
| `/wildercord reload` | Read the server's Wildercord settings again, and say what, if anything, was wrong with them. |

Fallen stars and rift sieges need a difficulty above Peaceful, and none of the three events starts on a server
whose settings switch world events off. Tab completion suggests rune names for
`learn` and `innate`.

The game's own commands work with Wildercord's structures and rules too:

| Command | Does |
|---|---|
| `/place structure wildercord:archive` | Build an [Archive]({{ '/world/archive/' | relative_url }}) where you stand. |
| `/place structure wildercord:ember_sanctum` | Build an [Ember Sanctum]({{ '/world/ember-sanctum/' | relative_url }}). |
| `/place structure wildercord:astral_observatory` | Build an [Astral Observatory]({{ '/world/astral-observatory/' | relative_url }}). |
| `/place structure wildercord:drowned_scriptorium` | Build a [Drowned Scriptorium]({{ '/world/drowned-scriptorium/' | relative_url }}). |
| `/gamerule wildercord:allow_duels false` | Turn duels off in this world (`true` turns them back on). It's also listed with the other game rules, as **Allow duels**. |
| `/team` | Players on the same team count as allies, so your helpful spells (Heal, Shield, buffs) reach them. See [friendly fire]({{ '/spellcraft/reading-spells/' | relative_url }}#friendly-fire-who-a-spell-touches). |
