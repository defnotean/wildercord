---
title: FAQ
nav_order: 13
description: "Answers to the questions new and experienced Wildercord players ask most."
---

# Frequently asked questions
{: .no_toc }

1. TOC
{:toc}

---

## Getting going

### How do I start?
Craft **Blank Runes** (4 Cobblestone around 1 Lapis Lazuli), then a **Twine Cord** (3 String over a Blank Rune). Put the Cord in the slot just above your offhand. You learn Self, Bolt and Push, and spell 1 is ready. Tap `R` to cast. See [Getting Started]({{ '/getting-started/' | relative_url }}).

### Does the server need the mod too?
Yes. Install the same Wildercord version, with Fabric API, on the server and every player's game. See [Installation]({{ '/installing/' | relative_url }}).

### Do I have to hold my staff and focus?
No. Your inventory has gear slots for a staff, a focus and the tome. Gear there works with empty hands. See [Casting Gear]({{ '/gear/' | relative_url }}).

### How do I carry more?
Craft a **Backpack** and wear it in the Backpack slot, then press `B`. Containers like shulker boxes can't go inside. See [Backpacks]({{ '/items/backpacks/' | relative_url }}).

### What's a Silent Rune?
A rune your game doesn't recognise. It almost always means your Wildercord version differs from the server's, or the rune came from an add-on you don't have. Nothing is lost: it wakes up once you install the matching version. A chat message tells you which version to install.

### I pressed `R` and nothing happened.
Look above your hotbar for the reason: no Cord, an empty spell, still recharging, or not enough mana. See [Casting]({{ '/spellcraft/casting/' | relative_url }}#pressing-the-cast-key).

### How do I learn a rune?
Hold it and use it (right-click). It's used up and yours for good. If you already know it, nothing is used up.

### Where are the rune recipes?
In your recipe book once you've held a Blank Rune, and on [Rune Recipes]({{ '/items/rune-recipes/' | relative_url }}). Each rune's tooltip also says how to get it.

### Which runes can't be crafted?
Tier IV runes (from bosses, vaults and rare places), [runes of the world]({{ '/runes/world/' | relative_url }}) (found only in their own places), [fused runes]({{ '/runes/fused/' | relative_url }}) (made at the Fusion Altar) and the ten [innate runes]({{ '/runes/innate/' | relative_url }}). The [Rune Codex]({{ '/runes/codex/' | relative_url }}) lists every rune.

### Why does a new rune only show a hint?
A rune you've just learned is **unread**. Cast it once to glimpse it, and use it a few times to understand it fully. See [Reading runes]({{ '/spellcraft/harmonies/' | relative_url }}#reading-runes).

### What do I do with a spare rune?
Fuse three into the next rank at the [Fusion Altar]({{ '/fusion-altar/' | relative_url }}), trade it to a [Runesmith]({{ '/social/runesmith/' | relative_url }}), or give it to a friend.

## Building spells

### Does rune order matter?
Yes. A modifier changes the closest rune on its left that it can change. `Bolt · Fire · Amplify` boosts the Fire. `Bolt · Amplify · Fire` does nothing extra. Gold lines under the sockets show what each modifier is attached to. See [How a Spell Is Read]({{ '/spellcraft/reading-spells/' | relative_url }}).

### Why is a threaded rune marked red?
It's **quiet**: kept, but it won't fire. Hover it to see why (too strong for your Cord, past the last socket, not learned, or from a missing add-on). See [Cords]({{ '/spellcraft/cords/' | relative_url }}).

### Do I need a shape in every spell?
No. A spell that starts with an effect is cast on you, so `Heal` alone heals you.

### Can I use the same rune twice?
Yes. `Fire · Amplify · Amplify` is fine. You only learn a rune once.

### How is mana cost worked out?
Shapes and effects have base costs, the shape multiplies its effects, and modifiers multiply what they're attached to. The readout always shows your exact price. See [Cost]({{ '/spellcraft/reading-spells/' | relative_url }}#cost-and-cooldown).

### How long is a cooldown?
About a second for every 20 mana, from half a second up to 20 seconds. Rapid halves it. Each Vow makes it five times longer, up to a minute. Every spell has its own cooldown.

### My spell "can't be cast".
It costs more than your whole mana pool. Trim it, grow your mana, or [overcast]({{ '/spellcraft/overcasting/' | relative_url }}) it.

### How do I share a spell?
Copy its **spell code** from the Cord screen and paste it in chat, inscribe a **scroll**, or tie a **Knot** at the Fusion Altar. See [Playing Together]({{ '/social/playing-together/' | relative_url }}).

## Casting

### Should I tap or hold `R`?
Tap for speed. Hold for power: a full charge takes 1.5 seconds and adds 40%, but you walk slower. See [Casting]({{ '/spellcraft/casting/' | relative_url }}#tap-or-charge).

### What if I keep holding past full?
You **overchannel**. Every 1.2 seconds you climb a stage (up to three, as your Heart Circles allow), each stronger and riskier. Let go right as a stage lands for a small bonus. Hold too long past your last stage and the spell tears loose: it fizzles, you're dazed for 1.5 seconds and lose 30% of your mana, but it never costs health. See [Overchannel]({{ '/spellcraft/casting/' | relative_url }}#overchannel-holding-past-full).

### What's the shape round my crosshair while I charge?
Your spell's **glyph**. Hold sneak while charging to trace it for a little extra power. You can turn it off in Magic visual settings. See [Sigil tracing]({{ '/spellcraft/casting/' | relative_url }}#sigil-tracing).

### How do I switch spells quickly?
Tap `V`, or hold it for the spell wheel. You can also bind the Cast spell 1 to 5 keys. See [Controls]({{ '/controls/' | relative_url }}).

### How do I get more mana?
Better Cords, Heart Circles, Mana Crystals (+10 each, up to 100 crystals), enchantments, potions, meditating and ley lines. See [Mana]({{ '/progression/mana/' | relative_url }}).

### Can I cast without enough mana?
Yes: **overcast** by cracking a Heart Circle, or thread **Blood Price** to pay in health. See [Overcasting]({{ '/spellcraft/overcasting/' | relative_url }}).

### Can my spells hurt my friends?
Harmful effects never touch you, your tamed pets, your party or your team. Other players are only hurt when the server allows PvP, and then for 60% damage by default.

### Why won't my Heal reach my friend?
Helpful spells only reach allies: players in your `/party` or on your team. See [friendly fire]({{ '/spellcraft/reading-spells/' | relative_url }}#friendly-fire).

### Will spells grief my base?
Spells only change blocks where you're allowed to build, and a server can turn block-changing spells off. Monsters' spells never change blocks. Fire spells can still light TNT. See [World Magic]({{ '/world/world-magic/' | relative_url }}).

### My spell left ash or frost on the ground.
That's a **residue**. Strong spells (30 mana or more) and overcasts can leave one. It fades by itself and gives a reagent when broken. Residues only take natural ground, never your builds. See [Residues and Reagents]({{ '/world/residues/' | relative_url }}).

## Cords and progress

### Do I lose anything when I die?
Your Cord, runes, spells, spell ranks and Heart Circles are all kept. Your mana starts empty and refills. Gear in your gear slots drops like the rest of your inventory.

### Do I lose spells when I change Cords?
No. Spells live on you, not the Cord. On a smaller Cord, extra runes stay threaded but quiet until you switch back.

### What's the numeral at the end of a spell row?
Its **rank**. Spells grow from Kindled (I) to Mythic (V) as you use them where it matters. Changing a spell's runes makes a new spell, but the old one's rank comes back if you change it back. See [Spell Mastery]({{ '/spellcraft/mastery/' | relative_url }}).

### How do I get passive spells?
Your 1st Heart Circle opens the first passive slot and your 5th opens the second. See [Passive Spells]({{ '/spellcraft/passives/' | relative_url }}).

### What's my innate rune?
One of ten runes that wakes in you, at random, when you form your 1st Heart Circle. See [Innate Runes]({{ '/runes/innate/' | relative_url }}).

### What are harmonies and secret spells?
**Harmonies** are rune sequences only your world answers, each with a twist. **Secret spells** are exact sequences that become something grander, hinted at by Torn Pages. See [Harmonies]({{ '/spellcraft/harmonies/' | relative_url }}) and [Secret Spells]({{ '/spellcraft/secret-spells/' | relative_url }}).

## Aura and Sword Masters

### What's aura? Do I need a Cord?
Aura is the swordsman's path. You don't need a Cord: read a **Breathing Manual**, then fight with a blade or stand in the breathing stance. See [Aura]({{ '/progression/aura/' | relative_url }}).

### Where do I find a Breathing Manual?
In old chests and vaults, from some villagers, or by beating a Wandering Duelist. See [Where to find manuals]({{ '/progression/aura/' | relative_url }}#where-to-find-manuals).

### My aura bar isn't filling.
Only full swings on real foes count, so wait for your attack to recharge. Hitting the same thing over and over gives less. You can also breathe in the stance.

### I pressed the Aura key and got slowed.
That's backlash: you used a technique without enough aura. It wears off in a few seconds and never hurts.

### My slash comes out a moment late since Form.
From Form, a double tap is Aura Step, so a single tap waits a moment to be sure.

### How do I call a Sword Master?
Reach **Aura Form** or **Heart Circle VIII**. Then sneak and use a Wandering Duelist twice (within 10 seconds), or type `/master challenge <name>` with any of the 16 Masters' names, such as `ember`, `tide` or `venom`. Friends nearby can `/master join`. See [Sword Masters]({{ '/masters/' | relative_url }}).

### What does the `C` key do?
It uses your equipped [Master form]({{ '/masters/master-forms/' | relative_url }}). It does nothing in Creative, so Save Toolbar still works there.

### What are `U`, `Y` and `J`?
The Master's Arts: Spellcut (from Edge), Rising Break and Driving Cut (from Form). See [Controls]({{ '/controls/' | relative_url }}#wildercord-masters-arts).

## The world

### Some monsters glow and cast spells.
They're **Runebound**: monsters with a Cord. Read the circle they hold out before each cast. See [Runebound]({{ '/world/runebound/' | relative_url }}) and [Magic Circles]({{ '/spellcraft/magic-circles/' | relative_url }}).

### Spells keep killing me in one hit.
Wear armour: it counts against spells. Add **Warding** to it, and drink a Potion of Warding before a boss. You also have a **spellguard**: a single spell can't take you from 80% health or more straight to death. See [Defending Against Magic]({{ '/progression/defence/' | relative_url }}).

### I can't see ley lines.
Wear a Cord, in the Overworld. They're thin violet ribbons along the ground. See [Ley Lines]({{ '/progression/ley-lines/' | relative_url }}).

### Why do my spells hit harder some nights?
The sky favours some elements, such as the full moon for arcane and void. Spells are also 10% stronger and cheaper where two ley lines cross. See [Places and Times of Power]({{ '/world/places-of-power/' | relative_url }}).

### Does Wildercord work with shaders?
Yes, with Iris and Sodium. Magic is lit properly by the shader pack. Nothing needs switching on.

## For server owners

### What can a server change?
Settings live in `config/wildercord.json`, written with every default the first time the server starts. Edit it, then run `/wildercord reload`. A missing or broken value falls back to its default, so a bad edit never stops the server.

Sections include `casting`, `mana`, `world`, `loot`, `features`, `travel`, `defence`, `mastery`, `channeling`, `residues`, `places_of_power`, `monsters`, `wildlife` and `aura`.

### I updated Wildercord. Do I need a new settings file?
No. Missing settings are added at their defaults when the server starts or reloads, and your own values are kept.

### Can I add a training pavilion to an old world?
Yes, as an optional, owner-approved step. See [Pavilion Upgrade]({{ '/world/pavilion-upgrade/' | relative_url }}).
