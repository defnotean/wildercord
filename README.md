<div align="center">

# Wildercord

**Thread simple runes onto a Cord, in any order, and cast the whole sequence with one key.**

A spell-crafting magic mod for Minecraft Java **26.3** on **Fabric**.

[![Build](https://github.com/defnotean/wildercord/actions/workflows/build.yml/badge.svg)](https://github.com/defnotean/wildercord/actions/workflows/build.yml)
![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-3C8527)
![Fabric](https://img.shields.io/badge/loader-Fabric-DBD0B4)
![Java 25](https://img.shields.io/badge/Java-25-E76F00)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

**[Read the Wildercord Player Guide](https://defnotean.github.io/wildercord/)** · **[Download 0.11.1-alpha](https://github.com/defnotean/wildercord/releases/tag/v0.11.1-alpha)** · [GitBook export](gitbook/SUMMARY.md)

<img src="docs/images/hero.gif" alt="A caster raises their hands and a magic circle opens ring by ring, then a nova of fire bursts out; then a Domain's spell circle spreads across the ground under a dome of light" width="720">

<img src="docs/images/cord-screen.png" alt="The Cord screen: four spells threaded with runes, the searchable Codex below, and the spell explained in plain English" width="640">

</div>

---

## Contents

- [What it is](#what-it-is)
- [Features](#features)
- [How spells are read](#how-spells-are-read)
- [Screenshots](#screenshots)
- [Installing (players)](#installing-players)
- [Controls and commands](#controls-and-commands)
- [Building from source](#building-from-source)
- [Project layout](#project-layout)
- [How it works (the short version)](#how-it-works-the-short-version)
- [Extending Wildercord](#extending-wildercord)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## What it is

You wear a **Cord** in a new inventory slot (just above your offhand). A Cord has sockets, and
you thread **runes** into them. Each rune is tiny and simple (a *shape*, an *effect*, a
*modifier* or a *link*), but they combine in order, so a handful of runes gives you an enormous
number of spells:

```
Bolt · Fire · Split · On Hit · Burst · Explode
```

> Three bolts of fire. Wherever one lands, an explosion goes off around it.

The Cord screen explains every spell in plain English as you build it, shows exactly what each
modifier attaches to, and prices it in mana and cooldown before you ever cast it.

## Features

| | |
|---|---|
| **756 named runes** | 82 shapes, 568 effects, 72 modifiers and 34 links across four tiers. **571 are craftable**, **59 runes of the world** come from their particular places, and the remaining ordinary Tier IV runes come from bosses and rare treasure. The Fusion Altar makes **55 elemental fusions** and **40 signature fusions**; exact weaves of two to eight effects extend these further. Ten effects are **innate runes**: one wakes in each caster's heart. Each named rune has its own icon; Tier III, Tier IV and innate icons are animated. See [docs/RECIPES.md](docs/RECIPES.md). |
| **Charged casting** | Tap to cast, or hold to charge: you raise your hands and a magic circle assembles behind your shoulders and a shape-specific focus forms ahead, for up to 40% more power. While you charge, a reticle shows where the spell will land. |
| **Magic circles you can read and build around** | Twelve animated mechanisms give different shapes their own casting circles, preserving rune script and illustrated roundels. Twelve craftable [circle disciplines](docs/features/circle-disciplines.md) change coverage, flight speed, effect duration, support power, elemental fusion, movement, stance, weather or night bonuses with explicit costs. Circles follow the caster's rear plane; the outgoing spell retains its shape and materials. |
| **Spells drawn in light** | All magic glows: light adds to what's behind it, and void magic is drawn as darkness. Beams with a white-hot core fired through a magic circle, bolts that glide as comets, crescents that sweep like blades, bursts that throw shells of light, rain from a circle in the sky, and a Domain whose floor is the spell's own circle under a dome of light. Every element has its own visual language, from branching lightning to imploding darkness. |
| **Magic you can feel** | Casters raise their hands to charge and move with each shape when it goes off; big impacts shake the camera, a charged release kicks the view, heavy hits land with a punch, and a Domain tints the edges of your screen. The Cord shows on every player's wrist, a bead for each rune of the spell they have ready, glowing as they put it on, charge and cast. |
| **Its own sound** | Every cast, impact, circle, beam, shield and Domain has its own synthesised sound, all in one key so they harmonise, and charging hums higher as it builds. |
| **Spell shields** | Shield is invisible until a spell comes at you; then its magic circles spawn in front of the spell, stacked one behind another (the stronger the Shield, the more circles), and stop it. A spell that cost more mana than yours cracks them and shatters them like glass, front to back, and goes through. |
| **Imbue items and blocks** | Store a spell in your sword (it goes off at what you strike), your pickaxe (at each block it breaks), your bow (with its arrows), your armour (at whatever hurts you) or any item you use, with three charges. Or put it into any block as a glyph that fires at whoever steps on, opens, shoots or breaks it, or on a redstone signal; carry an imbued block and place it where you want the trap. |
| **Secret spells** | Ten exact rune sequences become something new (a sun that falls from the sky, a lance of ice, a black star that swallows everything). Nothing lists them: experiment, or read the riddles on Torn Pages. |
| **The Grimoire** | Every reaction, secret, feat and riddle you discover, written into a page of the Cord screen. Each new reaction, secret and feat condenses mana toward your next Heart Circle. |
| **Runebound and the Archive** | Some monsters carry Cords and cast real spells, glowing with rune marks in their spell's colour and telegraphed by the spell's own circle and a readable nameplate. Deep underground, the Archive holds Rune Seal doors, the Archivist (a hooded, hovering three-phase boss with a floating tome, who rewrites its Cord) and a vault of Tier IV runes. |
| **Runes of the world** | Fifty-nine runes nobody can craft: in vanilla structures' chests, on bosses, in fallen-star craters and rift sieges, or taken from the land itself by **Attunement** (meditate with a Blank Rune in the right biome at the right moment). The Grimoire lists where each is found, with a riddle for the ones you haven't learned. See [docs/features/new-runes.md](docs/features/new-runes.md). |
| **Dungeons** | The Ember Sanctum in the Nether, the Astral Observatory in the End and the Drowned Scriptorium on the deep sea floor each have a three phase boss. The Rootbound Maze in swamps and Storm Spire on peaks add custom bosses, guarded side caches, sealed vaults and reusable relics. Clockwork Crypt, Living Greenhouse and Moving Sky Ruin each add three layouts and a distinct handling relic. See [docs/features/dungeons.md](docs/features/dungeons.md). |
| **World events** | Mana storms over the ley lines (faster mana, cheaper spells, wild surges), fallen stars that leave a guarded crater, and rift sieges that end with the Riftcaller. See [docs/features/world-events.md](docs/features/world-events.md). |
| **Magic that changes the world** | Fire lights the grass and boils water into blinding steam, frost freezes a pond you can walk on, storm arcs through water to everyone in it, wind knocks arrows back, earth heaves the ground and life makes flowers bloom. Real temporary walls, borrowed water attacks and wind stepping stones add terrain interactions. See [physical magic](docs/features/physical-magic.md) and [world magic](docs/features/world-magic.md). |
| **The Fusion Altar** | Three of a rune make its next rank, any two elemental effects fuse into one of 55 fused runes (every pair of elements, and each element with itself) or, for forty particular pairs, a **signature fusion** of their own, and a Blank Rune and string tie a whole spell into one **Knot** that takes a single socket. Amethyst blocks weave up to eight exact elemental effects, including the owner’s imprinted innate at Tier IV. See [docs/features/fusion-altar.md](docs/features/fusion-altar.md). |
| **Familiars** | Wisps of light rise from the ley lines at night. Tame one with its own element and it floats at your shoulder, quickens your mana, casts a little spell of its own and levels up as you fight together. Dress your Cord in beads and glow colours on the Cosmetics page. See [docs/features/familiars-and-cosmetics.md](docs/features/familiars-and-cosmetics.md). |
| **Cinnamon** | A small, immortal companion with her own model and skin. The server chooses her one owner in `config/wildercord-cinnamon.json`; she arrives already tamed and follows or sits on that player's click. See [docs/features/cinnamon.md](docs/features/cinnamon.md). |
| **Parry and wild magic** | A Shield raised at the last moment turns a flying spell back at its caster. An overcast may surge into something wild. See [docs/features/parry-and-wild-magic.md](docs/features/parry-and-wild-magic.md). |
| **The Runesmith, duels and chorus** | A villager who sells runes, buys your duplicates or swaps them for runes you don't know, and posts daily contracts; formal duels that put everything back afterwards; and allies casting together in a chorus. See [docs/features/runesmith-duels-chorus.md](docs/features/runesmith-duels-chorus.md). |
| **Travel commands** | For servers: homes, public warps, personal waypoints with an arrow on screen and a beam only you can see, teleport requests with clickable answers, `/back`, `/spawn` and `/rtp`. Every teleport is a short warmup in a forming magic circle, with a cooldown and a safe landing. See [docs/features/travel.md](docs/features/travel.md). |
| **Casting gear** | Elemental and greater staffs, the Tome of the Fifth Page (a fifth spell) and seven foci, each worn in its own inventory slot (staff, focus, tome) and shown on your character, or held as before while its slot is empty. See [docs/features/gear-config-api.md](docs/features/gear-config-api.md). |
| **Advancements** | A Wildercord tab from your first Blank Rune to the 20th Heart Circle. See [docs/features/advancements.md](docs/features/advancements.md). |
| **Ley lines** | Veins of world mana, visible to Cord-wearers as flowing ribbons of violet light: mana flows twice as fast on them. A Wellstone set on one becomes a well for everyone nearby. |
| **Play together** | Paste a spell code (`wc:bolt.frost.split`) in chat and it becomes a readable spell card; inscribe spells onto scrolls anyone can cast; hit the foe another player just hit, with a different element, for **Unison**; win **domain clashes**; shoot enemy bolts out of the air. |
| **Four Cords** | Twine → Copper → Amethyst → Echo: more sockets, more spells, higher rune tiers, more mana. |
| **Ten elements** | Fire, Frost, Storm, Wind, Earth, Life, Void, Arcane, Time and Blood, each with its own look and sound. |
| **Element reactions** | Eleven of them, from Shatter and Conduct to Overload, Fracture, Blight, Unweave, Rupture and Elapse: the right element on the right mark sets off a bonus, and every element takes part. |
| **Creature affinities and climate** | Blazes fear frost, the undead burn under life magic, golems conduct storm: creatures are weak to some elements and resist others (a datapack can change which), and your Grimoire's Bestiary records what you find. Where you fight matters too: fire burns hotter in the Nether, storm in a thunderstorm, frost in the snow. See [docs/features/affinities.md](docs/features/affinities.md). |
| **Heart Circles** | Condense mana by casting, earn breakthroughs (mostly feats: set off five different reactions, find secret spells, defeat the Archivist), and meditate to form rings of mana around your heart, from the 1st Circle to the 20th (Master Heart), with Archmage at the 8th. In a pinch, **overcast**: crack a circle to cast beyond your mana. |
| **Rhythm** | Cast again right as your last spell comes off cooldown and the chain builds power. |
| **Your affinities** | An affinity with each of the ten elements that grows with what you do: casting it, its reactions, and everyday things that fit it (smelting for fire, fishing for frost, mining for earth, farming for life...), each with a daily allowance so nothing can be farmed. Levels I to V: +3% power a level with that element, a resistance to it from III, cheaper spells at V. Your magic leans toward your deepest one: your Heart Circles are tinted toward its colour, and a spell with no effect of its own charges in it. See [docs/features/player-affinity.md](docs/features/player-affinity.md). |
| **Passive spells** | Up to two always-on spells (a buff, or an Orbit aura) that drain mana every second instead of having a cooldown. |
| **Mana that grows** | Mana Crystals, Cord enchantments (Reservoir, Wellspring, Siphon), Clarity and Mana potions, meditation, Heart Circles. |
| **Spell enchantments** | Potency, Celerity, Thrift and Persistence make your spells stronger, faster, cheaper and longer. |
| **A readable Cord screen** | Type-to-search Codex, family tabs and category chips, drag and drop, a live plain-English readout, the spell's magic circle opening beside the window as you build it, spell names you can change, and a spell wheel (hold `V`). |
| **A practice dummy** | Place a Training Dummy to try spells on: every hit floats up as a number, and it shows your damage per second. |
| **Crafting and loot** | Every Tier I-III rune is craftable (costlier by tier) and shows in the recipe book; Tier IV runes come from bosses and rare structures. |
| **Server-authoritative** | The client only asks; the server validates every edit and cast. Friendly fire is off, every cast has hard budgets, and spells that change blocks respect spawn protection and claim mods. |
| **Built to extend** | Runes are plain data with traits, so new runes slot into the reading rules without special cases. Server owners get a config file (`config/wildercord.json`, `/wildercord reload`), and other mods get an add-on API for runes, effects and cast events ([docs/API.md](docs/API.md)). |

## How spells are read

Runes come in four families. The rules are short, and the Cord screen always shows you the result:

| Family | Colour | What it does | Examples |
|---|---|---|---|
| **Shape** | Teal | Decides *where* and *who*. Starts a group. | Self, Bolt, Beam, Burst, Zone, Orbit, Domain |
| **Effect** | Its element | Decides *what happens* to whatever the shape hit. | Fire, Heal, Freeze, Blink, Stasis, Swap |
| **Modifier** | Gold | Changes the closest rune on its left that it *can* change. | Amplify, Extend, Widen, Split, Homing, Rapid |
| **Link** | Purple | Ends the segment; everything after it fires *later*. | On Hit, Delay, Echo, Pulse, On Hurt, Combo |

1. **A shape starts a group.** Effects before any shape use an implicit shape: Self at the start
   of a spell, or *whatever triggered it* right after a link.
2. **A modifier attaches to the closest rune on its left that has the trait it needs.** Split
   needs something that can split, so in `Bolt · Fire · Split` it skips Fire and doubles up the
   Bolt. It never reaches back past a link.
3. **A link splits the spell.** `On Hit`, `On Kill` and `On Hurt` fire the rest at the thing that
   triggered them; `Delay` and `Pulse` fire it from you later; `Echo` repeats everything before it.

Cost is the sum of each group's shape and effects, scaled by the shape's multiplier and the
modifiers on them. Cooldown is one tick per mana, between 0.5 and 20 seconds. The full design,
with every rune's numbers, is in [docs/DESIGN.md](docs/DESIGN.md).

## Screenshots

### New in 0.4: a fused rune for every pair of elements

| Singularity (Void + Void): a black hole that bursts | Stormclock (Storm + Time): lightning on the hour |
|---|---|
| <img src="docs/images/fused-singularity.jpg" alt="At night, a black sphere inside a tilted disk of white and violet rings, pulling husks into it" width="420"> | <img src="docs/images/fused-stormclock.jpg" alt="A gold clock face hanging in the sky over a husk, lightning falling from it onto a target mark" width="420"> |
| Riftbolt (Storm + Void): a black bolt that tears its target away | Cryostasis (Frost + Time): an ally sealed safe in ice |
| <img src="docs/images/fused-riftbolt.jpg" alt="A black bolt with a violet core striking a husk, jagged cracks torn in the air at both ends" width="420"> | <img src="docs/images/fused-cryostasis.jpg" alt="A clear cocoon of ice standing over the caster, a tamed wolf beside it" width="420"> |

### New in 0.3

| The Ember Sanctum: a reaction cracks the Cinder Warden's plates | The Astral Observatory: the Star-Eater throws a spell back |
|---|---|
| <img src="docs/images/ember-sanctum.jpg" alt="A domed forge arena of blackstone and magma; the Cinder Warden, a hulk of iron and magma, bursts with fire and frost light as a reaction goes off on it" width="420"> | <img src="docs/images/astral-observatory.jpg" alt="A purpur dome over a floor of starry tiles; the Star-Eater, a knot of void with star shards round it, hangs in the air turning a spell back" width="420"> |
| The Drowned Scriptorium: storm magic in the Tide Scribe's flood | A mana storm: violet sky, arcs over the ley line |
| <img src="docs/images/drowned-scriptorium.jpg" alt="A flooded prismarine arena; a ring of lightning spreads across the water round the Tide Scribe" width="420"> | <img src="docs/images/mana-storm.jpg" alt="Open grassland under a violet-tinted sky, branching arcs of light crackling overhead" width="420"> |
| A fallen star, its beam and its guards | Wild wisps of every element |
| <img src="docs/images/fallen-star.jpg" alt="At night a column of pale light rises from a star lying in a scorched crater, a guard beside it" width="420"> | <img src="docs/images/wisps.jpg" alt="At night, a row of small glowing orbs of light in different colours drifting over the grass" width="420"> |
| Attunement: the land's circle and its rune | Frost freezes a pond you can walk on |
| <img src="docs/images/attunement.jpg" alt="A player meditating on a turning green magic circle while motes of light rise from the ground into the blank rune in their hand" width="420"> | <img src="docs/images/frozen-pond.jpg" alt="A small pond frozen over in frosted ice, a frost seal of light on the ice" width="420"> |
| Storm through water shocks everything in it | The Fusion Altar at work |
| <img src="docs/images/storm-water.jpg" alt="Arcs of light skittering across a pond from a strike to each creature standing in the water" width="420"> | <img src="docs/images/fusion-altar.jpg" alt="An amethyst and deepslate altar with a magic circle turning over it, rings of light racing out and sparks rising" width="420"> |
| The Runesmith at its Scribing Desk | A parry: the Shield's counter-burst |
| <img src="docs/images/runesmith.jpg" alt="A villager in a violet hooded robe with a gold band standing beside a wooden desk set with a rune" width="420"> | <img src="docs/images/parry.jpg" alt="A golden magic circle held in front of the player and a beam of light striking back at the husk that cast at them" width="420"> |

### In motion

| A Shield's circles spawn in and stop a bolt | A heavier spell shatters every circle, and hits |
|---|---|
| <img src="docs/images/shield-block.gif" alt="Animated: a bolt flies at a husk; amber magic circles open one behind another in front of it, the front one breaks and the next stops the bolt in a flare" width="420"> | <img src="docs/images/shield-shatter.gif" alt="Animated: a heavy bolt hits a husk's stacked circles and they burst into glinting glass shards, and the husk falls" width="420"> |
| A Frost glyph going off under a husk | Ten shapes, one after another |
| <img src="docs/images/glyph.gif" alt="Animated: a glyph's magic circle on the ground flares as a husk steps onto it, and rings of frost light rise round the husk" width="420"> | <img src="docs/images/spells.gif" alt="Animated: beam, crescent, burst, pillar, rain, nova, prism, comet, lance and a Domain, each fading into the next" width="420"> |

### In the world

**Reading a spell circle.** The star has a point for every rune, and each point's roundel carries
that rune's emblem: read them around from the top. This one is Nova, Flashfire, Widen, Shock,
Amplify, On Hit, Burst: a nova of fire and lightning round the caster, the Flashfire widened (Widen
attaches to it, since it has a radius) and the Shock amplified. The Burst after On Hit carries no
effect, so it would do nothing (the Cord screen warns "Burst has no effect after it"). The band of
script around it repeats the runes, and the first rune's emblem (Nova's) is the seal in the middle.

<img src="docs/images/spell-circle.jpg" alt="A player holding out a charging magic circle: a frame, a band of script, a seven-pointed star with a coloured roundel on each point, and a seal in the middle" width="760">

| A Domain: the spell's circle as its floor | Beam |
|---|---|
| <img src="docs/images/domain.jpg" alt="A Domain seen from the side: the spell's magic circle spread across the ground under a dome of light, light pouring down from its crown" width="420"> | <img src="docs/images/beam.jpg" alt="A beam with a hot core fired through a small magic circle into a line of husks, rings of light along it" width="420"> |
| A Shield: its circles spawn in, stacked, and stop the spell | A stronger spell shatters them like glass, and goes through |
| <img src="docs/images/shield-block.jpg" alt="Seven amber magic circles stacked one behind another in front of a husk, a bolt flaring white against the front one" width="420"> | <img src="docs/images/shield-shatter.jpg" alt="The circles bursting into glinting shards and curved slivers of their rims as the spell hits the husk" width="420"> |
| Imbue: a sword that holds Fire for three strikes | A Frost glyph going off under a husk that stepped on it |
| <img src="docs/images/imbue-sword.jpg" alt="A player holding a glinting iron sword over the magic circle of the Imbue spell, a message saying the sword holds Fire" width="420"> | <img src="docs/images/imbue-glyph.jpg" alt="A glyph's magic circle on the ground flaring as a husk stands on it, rings of frost light rising round the husk" width="420"> |
| Crescent | Burst |
| <img src="docs/images/crescent.jpg" alt="Blades of fiery light sweeping forward into a group of husks" width="420"> | <img src="docs/images/burst.jpg" alt="A shell of light, crossed rings racing out over a shockwave on the ground" width="420"> |
| Pillar | Rain |
| <img src="docs/images/pillar.jpg" alt="A column of light erupting from a magic circle on the ground beneath a husk" width="420"> | <img src="docs/images/rain.jpg" alt="A magic circle open in the sky above a target circle, a streak of light falling from it" width="420"> |
| Nova, a new Tier I shape | Prism, a new Tier II beam |
| <img src="docs/images/nova.jpg" alt="A dome of light bursting round the caster over a spell circle, catching husks inside it" width="420"> | <img src="docs/images/prism.jpg" alt="A beam that splits into three rays at the first husk it hits" width="420"> |
| Charging a spell | A Runebound, glowing with its spell's marks |
| <img src="docs/images/charge.jpg" alt="A player with raised hands charging a spell, its magic circle open in front of them" width="420"> | <img src="docs/images/runebound.jpg" alt="A zombie at night with glowing orange rune marks on its brow, arms and legs" width="420"> |
| **Glacial Lance**, a secret spell | **Tectonic Rise**, a secret spell |
| <img src="docs/images/glacial-lance.jpg" alt="A lance of ice pierces a line of husks and closes each one in a block of ice" width="420"> | <img src="docs/images/tectonic-rise.jpg" alt="A line of stone spires bursts out of the ground toward a group of husks" width="420"> |
| A domain clash | The Training Dummy |
| <img src="docs/images/domain-clash.jpg" alt="Two domains meet: the player's dome of light pressing against a witch's, their floor circles overlapping" width="420"> | <img src="docs/images/training-dummy.jpg" alt="Arcane bolts striking a training dummy, a damage number rising over its DPS readout" width="420"> |
| The mouth of an Archive | The Archivist |
| <img src="docs/images/archive.jpg" alt="Broken deepslate pillars crowned with amethyst around a stairway leading underground" width="420"> | <img src="docs/images/archivist.jpg" alt="The Archivist up close: a hooded robed figure with glowing eyes, arms raised, a floating open tome and pages circling it, holding out its spell's circle" width="420"> |
| A Rune Seal door: only Frost and Storm spells open it | The Archivist's arena |
| <img src="docs/images/seal-door.jpg" alt="A door of Rune Seal blocks in a deepslate hall, each seal marked with a snowflake or a lightning bolt" width="420"> | <img src="docs/images/archive-arena.jpg" alt="The domed arena under the Archive: the Archivist rising from its lectern at the centre of a glowing floor circle" width="420"> |
| A Wellstone on a ley line | The spell wheel |
| <img src="docs/images/wellstone.jpg" alt="A Wellstone block on a ley line, rings of light turning around it" width="420"> | <img src="docs/images/spell-wheel.png" alt="The radial spell wheel: four spells around a centre showing cost and cooldown" width="420"> |

**The Grimoire:** your innate rune, leaning and affinities, and every reaction, secret spell and feat
you've found, with the riddles you've read.

<img src="docs/images/grimoire-reactions.jpg" alt="The Grimoire page of the Cord screen: your heart, six of the eleven reactions found (Shatter, Conduct, Wildfire, Overload, Fracture and Rupture), and ten secret spells still unknown" width="740">

### Screens

| The Codex, searched | The HUD |
|---|---|
| <img src="docs/images/search.png" alt="Searching the Codex for fire" width="420"> | <img src="docs/images/hud.png" alt="The spell HUD beside the hotbar" width="420"> |

**Passive spells:** up to two always-on spells, each with an on/off switch and its upkeep.

<img src="docs/images/passives.png" alt="The Passives page of the Cord screen, with upkeep and on/off switches" width="760">

**Heart Circles:** hover the heart for your circles, perks and what the next one needs.

<img src="docs/images/heart.png" alt="The heart tooltip at the 8th Circle, listing every perk" width="760">

<details>
<summary><b>Every rune's emblem and ring pattern</b></summary>

Learn these and you can read any spell being cast. Each rune's emblem (on its roundel, in the band
of script and as a seal) and ring pattern, generated by `tools/circle_art.py`.

<img src="docs/images/rune-rings.png" alt="The craftable runes' emblems and ring patterns, each drawn as a ring with its emblem, labelled with the rune's name" width="900">

</details>

<details>
<summary><b>Every rune icon</b></summary>

<img src="docs/images/rune-icons.png" alt="All rune icons at true size on light and dark backgrounds" width="720">

</details>

## Installing (players)

1. Install **Minecraft Java 26.3** with **Fabric Loader 0.19.5+**.
2. Put **Fabric API 0.161.0+26.3** (or newer for 26.3) in your `mods` folder.
3. Download the Wildercord jar from [Releases](https://github.com/defnotean/wildercord/releases)
   or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/wildercord) and put it in your
   `mods` folder (or install it with the CurseForge app, which fetches Fabric API for you).

The mod must be on both the server and every client.

**Getting started in survival:** craft a Blank Rune (4 cobblestone around 1 lapis lazuli), then a
Twine Cord (3 string over a Blank Rune), and put it on in the new slot above your offhand. You
learn Self, Bolt and Push, and your first spell is threaded for you. Right-click any rune item to
learn it.

## Controls and commands

| Key | Action |
|---|---|
| `R` | Cast the selected spell. **Hold** to charge it, then let go |
| `V` | Select the next spell. **Hold** for the spell wheel: point and let go, or let go first and it stays open (click a spell, press its number, or Esc) |
| `K` | Open the Cord screen |
| *(unbound)* | "Cast spell 1-4": bind these to cast a spell directly |
| Sneak + stand still | Meditate: double mana regeneration, and form a Heart Circle when ready |

In the Cord screen: click or drag runes from the Codex onto a spell, drag runes to reorder them,
click a threaded rune to remove it, **just type to search** (Ctrl+F also works, Esc clears). The
small buttons over the readout rename the spell, copy or paste its spell code, and inscribe it
onto a scroll. The **Grimoire** tab lists everything you've discovered.

Out of mana? Cast the same spell again within two seconds to **overcast**: your outermost Heart
Circle cracks for three minutes to pay for it (for a spell costing up to twice your full mana).

Commands (operators, permission level 2):

| Command | Does |
|---|---|
| `/wildercord learnall` | Learn every rune |
| `/wildercord learn <rune>` | Learn one rune, e.g. `stasis` or `wildercord:stasis` (an add-on's runes by their full id) |
| `/wildercord spell <1-4> <runes...>` | Thread a spell directly |
| `/wildercord mana` | Refill your mana |
| `/wildercord circles <0-20>` | Set your Heart Circles |
| `/wildercord condense <mana>` | Add condensed mana toward the next circle |
| `/wildercord innate <rune>` | Choose your innate rune |
| `/wildercord runebound` | Bind the nearest monster to a Cord |
| `/place structure wildercord:archive` | Build an Archive where you stand (vanilla command) |
| `/wildercord reset` | Forget every rune and spell |

## Building from source

Requirements: **JDK 25** and Python 3.11+. **Pillow** is needed only for regenerating art and data.
Gradle comes with the wrapper.
Full/sharded client test resources and launch preflight also require Python on PATH
as `python3` on macOS/Linux or `python` on Windows. The owned CI launcher runs on Linux;
focused selectors retain their existing platform behavior. See
[the explicit full-client plan](docs/FULL_CLIENT_SHARD_PLAN.md) for its fail-closed contract.

```bash
git clone https://github.com/defnotean/wildercord.git
cd wildercord
./gradlew build
```

The jar is in `build/libs/`. Useful tasks:

| Task | What it does |
|---|---|
| `./gradlew runClient` | Starts a dev client with the mod loaded |
| `./gradlew test` | Runs the spell-engine unit tests (pure Java, no Minecraft needed, a few seconds) |
| `./gradlew runClientGameTest` | Starts a real client, builds a world, checks the mechanics and saves screenshots |
| `WILDERCORD_CINNAMON_SUITE=1 ./gradlew -PcinnamonSuite runClientGameTest` | Runs Cinnamon's focused in-world checks and saves `cinnamon_front.png` and `cinnamon_summoned.png` |
| `./gradlew -PaltBuild -PmagicSuite runClientGameTest` | Casts the eight motion families in a real client and saves formation, first-person, and release screenshots as `magic_*.png` |
| `python tools/generate_assets.py` | Regenerates textures, models, language, recipes, enchantments and `docs/RECIPES.md` |
| `./gradlew -PaltBuild compileJava` | Compiles into `build-alt/`, so you can check code while a dev client is running |

> **Heads up:** Loom's dev client loads classes straight from `build/`. Use `-PaltBuild` for
> compile checks or focused game tests while a dev client is open, then restart the client to
> load the new classes.

## Project layout

```
src/
├── main/java/dev/wildercord/
│   ├── spell/      The spell engine: pure Java, no Minecraft imports.
│   │                 Runes (the roster), RuneDef, Trait, RuneCategories, SpellCompiler,
│   │                 SpellPlan, SpellNumbers, Passives, Circles, Secrets, Feats, Leaning, PlayerAffinity,
│   │                 SpellNames, SpellCodes, SpellSigil
│   ├── cast/       Running spells in the world: SpellCaster (the gate), CastEngine, Cast,
│   │                 Casters, Targets, ShapeRunners, Effects, Techniques, Wards, Reactions,
│   │                 Scheduler, Spirits, RuneBolt, HeartCircles, PassiveCaster, Charging,
│   │                 Rhythm, Overcast, Unison, DomainClash, SecretSpells, Innates, Grimoire,
│   │                 Shields, Imbuing,
│   │                 Runebound, Archivist, RuneSeals, LeyWalker, TrainingDummy, SpellChat,
│   │                 CordLook, ScreenFx, Vfx, TechniqueVfx, ExpansionVfx, ElementFx, Light,
│   │                 Sigils, BlockFx, Fx
│   ├── player/     Per-player state: Spellbook, Mana, Heart, WildercordAttachments
│   ├── content/    Items, blocks (Wellstone, Rune Seal, Archive Lectern), Cord tiers, data
│   │                 components, the sigil, spell circle and light particles, sounds, potions,
│   │                 loot
│   ├── world/      The Archive structure, and ley lines (pure maths shared by both sides)
│   ├── menu/       The Cord slot and the gear slots
│   ├── mixin/      Cord and gear slots in the inventory menu, creative sync, gear dropped on death, lightning rods,
│   │                 Phantom's afterimage, pistons (temporary blocks stay put), imbued arrows
│   │                 and placed imbued blocks
│   ├── net/        Packets: cast, charge, select, edit, rename, passives, scrolls; and the
│   │                 notices the server sends (discoveries, the ley seed, screen effects)
│   └── command/    /wildercord
├── client/java/dev/wildercord/client/
│                   CordScreen (with the Grimoire page and GuiSpellCircle), SpellHud,
│                   SpellWheelScreen, GrimoireToast, key bindings; mixins (inventory screens,
│                   casting poses, camera shake, the glow pipelines);
│                   fx/ (glow blending, magic circles, shaped light, comets, charging, aim
│                   preview, screen effects, ley ribbons, Archive ambience, shield circles and
│                   their shattering) and
│                   render/ (the worn Cord, Runebound marks, the Archivist, the Training Dummy)
├── main/resources/ Generated assets and data (textures, models, lang, recipes, ...)
├── test/           JUnit tests for the spell engine (and ley lines)
└── gametest/       Client game tests: mechanics checks, screenshots and the feature tour
tools/
├── generate_assets.py   Reads Runes.java and writes every asset and data file
├── item_art.py          Hand-tuned pixel art for runes, cords and badges
├── gui_art.py           GUI and HUD sprites
├── sigil_art.py         Magic circles, and the wheel, beat ring and toast sprites
├── shield_art.py        The glass shards and crack lines of a Shield's breaking circles
├── circle_art.py        Every rune's own ring pattern and emblem for magic circles
├── world_art.py         Scroll, page, dummy, Wellstone, seals, lectern, entity skins, rune marks
├── wear_art.py          The Cord players wear on the wrist
├── sound_art.py         Every sound, synthesised: casts, impacts, charging, circles and the UI
└── make_gif.py          The moving header at the top of this README, from the feature tour
docs/
├── DESIGN.md            The full design: every rune, number and rule
├── ARCHITECTURE.md      How the code fits together, for developers
├── ART.md               The Wildercord look: colour, circles, light, motion, icons and sound
├── ADDING_RUNES.md      Step by step: adding a shape, effect, modifier or link
├── RECIPES.md           Every recipe and drop (generated)
└── images/              The screenshots in this README
```

## How it works (the short version)

```
 client                              server
┌──────────────┐  EditSpell   ┌──────────────────────────────────────────────┐
│  CordScreen  │ ───────────▶ │ SpellCaster.edit   validates, saves Spellbook │
│  SpellHud    │  CastSpell   │ SpellCaster.cast   Cord? learned? cooldown?   │
│  (key R)     │ ───────────▶ │                    mana? then:                │
└──────────────┘              │   SpellCompiler.compile(runes) → SpellPlan    │
                              │   CastEngine.cast(plan)                       │
                              │     ├─ shapes find hits (ShapeRunners)        │
                              │     ├─ effects apply (Effects, Techniques)    │
                              │     ├─ links schedule the rest (Scheduler)    │
                              │     └─ visuals (Vfx, TechniqueVfx → Fx)       │
                              └──────────────────────────────────────────────┘
```

- **`spell/` is pure data and logic.** `Runes` lists every rune as a `RuneDef` record; the
  compiler turns a list of runes into a `SpellPlan`, prices it and explains it. Because none of it
  touches Minecraft, the readout the player sees and the spell the server runs can never disagree,
  and it's all unit-tested.
- **`cast/` runs a plan in the world.** A `Cast` carries hard budgets (64 creatures, 32 blocks,
  8 link depth, 128 parts in all) shared by everything it chains into, so no spell can run away. Its caster can be
  any living thing, so Runebound monsters and the Archivist cast through the same engine.
- **Player state lives in Fabric data attachments**: saved, synced to the owner, and kept
  through death where it should be.

The full tour, with the reasoning behind each piece, is in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Extending Wildercord

Adding a rune is usually a few lines: a `RuneDef` in `Runes`, its behaviour in `Effects` (or a
runner in `ShapeRunners`), a visual, a glyph, and a recipe. Modifiers find what they can change
through **traits**, not lists, so a new effect that declares `POWER` is amplified by Amplify with
no other changes. Walk through it in [docs/ADDING_RUNES.md](docs/ADDING_RUNES.md).

## Testing

- **Unit tests** (`./gradlew test`): the compiler's reading rules, costs, cooldowns, Blood Price,
  Vow, passive rules, Heart Circle and enchantment maths; spell names and codes, secret spells,
  leaning, innate runes, ley lines and the breakthrough table.
- **Client game tests** (`./gradlew runClientGameTest`): starts a real client and world, then
  - screenshots the HUD and Cord screen at every GUI scale and three window sizes,
  - drives the Cord screen with the real mouse for all four Cords: selecting spells, threading and
    taking off runes with either button, dragging, and being refused (with the reason) for a locked
    spell, a rune too strong for the Cord, a full spell and a rune that can't be a passive, checking
    what the server saved each time, and that each Cord casts exactly the spells it holds
    (`WILDERCORD_CORDS_ONLY=1` runs just this),
  - casts every newer rune at test mobs (any exception fails the run),
  - checks mechanics for real: Stasis holds Sonic Boom, Primer, Meteor and more until time moves
    again; Reflect, Foresight, Reversal, Blood Price and Combo work; passives drain mana; forming a
    Heart Circle raises max mana; the Cord screen turns on text input for its search box; a Shield
    stops a spell that costs no more than it and breaks under one that costs more; an imbued sword,
    a glyph and a placed imbued block release what they hold, and a broken glyph comes back imbued;
  - tours the world features on a cleared stage (charging, the wheel, every secret spell,
    Runebound, the dummy, a domain clash, overcasting, a ley line, and a placed Archive with its
    Archivist), asserting what it can and screenshotting the rest. Set `WILDERCORD_TOUR_ONLY=1` to
    run just the tour; the images in [Screenshots](#screenshots) come from it.

Screenshots land in `build/run/clientGameTest/screenshots/`. CI runs the build and unit tests on
every push, checks generated resources, and runs the client game suites under Xvfb with screenshots and logs retained as artifacts.

## 0.9.0: the wilds and the sword

Twelve new creatures, six monsters with tells and counterplay and six enchanted animals, fill the world, and a field guide
in the Grimoire records the ones you meet. **Aura** opens a swordsman's path beside the Cord: ten breathing methods, five
stages from a haze on the blade to a Dominion, breakthroughs, the spellblade, wandering duelists who teach their methods,
fallen knights and aura-forged gear. See the [changelog](CHANGELOG.md#090-alpha---2026-10-01) and
[What's New](https://defnotean.github.io/wildercord/whats-new/).

## 0.8.0: magic of your own

Every world draws its own hidden harmonies and rune quirks from its seed, and new runes have to be read before
they're understood. Spells grow with their casters through five ranks of mastery, with a chosen trait at each rank, a
personal sigil, spoken names and inscribed scrolls. Casting is a performance: overchannel past full charge, trace the
spell's glyph, and watch its incantation rise. Big magic leaves residues that give Fusion Altar reagents, and ley
crossings, the moon, the hour and the weather favour different elements. See the
[changelog](CHANGELOG.md#080-alpha---2026-10-01) and [What's New](https://defnotean.github.io/wildercord/whats-new/).

## September 30 expansion and audit fixes

The 0.7.1-alpha release adds three expeditions (Clockwork Crypt, Living Greenhouse and Moving Sky Ruin), Root Guardian and Storm Conductor encounters, three elemental armour sets, two defensive foci, Mirror-thread Mantle and three dungeon handling charms. Cinnamon now retains her sitting preference and has sleeping, greeting, petting and toy behaviour.

Formation circles sit behind the caster while each of the 39 shapes assembles its own release. Named and exact woven fusions carry their ingredient materials into formations. Client visual settings include three presets, reduced flashing, camera motion controls and a local frame sampler.

New player tools are available through `/runelab`:

* `practice enter` / `practice leave`: enter a separate practice dimension and return.
* `research`: permanent experiments and a learned-ingredient fusion hint.
* `builds save|load|delete "Name" <slot>`: a 24-build library; deletion takes only the name.
* `familiar companion|scout|guardian|gardener`: choose elemental assistance or a utility job.
* `trial precision|variety|fusion`: optional Survival practice challenges.

A craftable Runic Hearth supports a reading lantern, bloom planter, fall-protection chime and a cooperative ritual with a solo completion path. Completed world events leave temporary saved echoes to explore.

Feature guides: [expeditions](docs/features/expeditions.md), [elemental armour](docs/features/elemental-armor.md), [defensive foci](docs/features/defensive-foci.md), [research and library](docs/features/research-library.md), [home projects](docs/features/home-projects.md), [familiar jobs, echoes and trials](docs/features/familiars-events-trials.md), [visual presets](profiles/README.md). Verification and remaining limits are recorded in the [implementation ledger](docs/audit/implementation-progress.md).

## Roadmap

The expanded combat, magic, progression and lore update is tracked in the [Masters of Tomorrow development roadmap](docs/MASTERS_OF_TOMORROW_ROADMAP.md). Its [release checklist](docs/reviews/RELEASE_READINESS.md) distinguishes tested checkpoints from unfinished features and remaining runtime acceptance.

- More dungeons, bosses and runes of the world.
- More reactions and secret spells.
- Balance passes from what players find on real servers.

## Contributing

Issues and pull requests are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md): it covers
setting up, the code style, the asset pipeline, and what a good PR includes.

## License

[MIT](LICENSE). Use it, learn from it, build on it.
