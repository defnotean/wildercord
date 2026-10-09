---
title: Mana
parent: Growing Stronger
nav_order: 4
---

# Mana
{: .no_toc }

<img src="{{ '/assets/images/hud.png' | relative_url }}" alt="The spell HUD beside the hotbar: a circle badge, a row of rune icons, and a violet mana bar with an up-chevron" class="shot">
<span class="caption">Your mana bar sits in the spell HUD. The chevron means your regeneration is boosted.</span>

1. TOC
{:toc}

## What it is

Every spell costs **mana**. You have a pool (your **max mana**) that refills every second (your
**regeneration**). You only have mana, and it only refills, while you wear a
[Cord]({{ '/spellcraft/cords/' | relative_url }}). It belongs to you, not the Cord.

Hover the **mana badge** in the Cord screen for a full breakdown.

## Max mana

| Source | Max mana |
|---|---|
| Twine / Copper / Amethyst / Echo Cord | 100 / 150 / 225 / 300 |
| Each [Mana Crystal](#mana-crystals) absorbed | +10 (up to 100 crystals: +1,000) |
| [Reservoir]({{ '/progression/enchantments/' | relative_url }}) on your Cord | +25 per level (up to +75) |
| Each working [Heart Circle]({{ '/progression/heart-circles/' | relative_url }}) | +15 (up to +300) |
| A **Focus of the Deep Well** | +50 while equipped (see [Casting Gear]({{ '/gear/' | relative_url }})) |

The most you can have is **1,725**. If your maximum drops, mana above it is lost.

## Regeneration

**Base regeneration**: 5 / 6 / 7 / 8 a second from a Twine / Copper / Amethyst / Echo Cord, plus
**+0.5 per working Heart Circle** (up to +10).

Boosts add together, then multiply the base:

| Boost | Extra | How |
|---|---|---|
| [Wellspring]({{ '/progression/enchantments/' | relative_url }}) | +25% per level (up to +75%) | Cord enchantment |
| Clarity | +50% (Clarity II: +100%) | [Potion of Clarity](#potions) |
| Meditating | +100% | [Sneak and stand still](#meditating) |
| On a ley line | +100% | [Ley lines]({{ '/progression/ley-lines/' | relative_url }}) |
| Near an awake Wellstone | +50% | Within 12 blocks |
| Under a mana storm | +100% | [World events]({{ '/world/world-events/' | relative_url }}) |

Example: an Echo Cord (8) with four circles (+2) is a base of 10. With Wellspring II, meditating on a
ley line: 10 × 3.5 = **35 mana a second**. A nearby [familiar]({{ '/companions/familiars/' | relative_url }})
adds a little more.

## Mana Crystals

A **Mana Crystal** raises your max mana by **10, forever**. Hold it and right-click to absorb it. Up to
**100** count. After that, it stays in your hand.

### How to get it

<div class="recipe-gallery">
{% include recipe-card.html id="mana_crystal" name="4 Lapis Lazuli, 4 Amethyst Shards and a Diamond" %}
{% include recipe-card.html id="mana_crystal_from_lumen_antler" name="Or a Lumen Antler in place of the Diamond" %}
</div>

A [Lumen Stag]({{ '/world/creatures/' | relative_url }}#lumen-stag) sheds Lumen Antlers for you.

| Where | How many |
|---|---|
| Ancient city and stronghold library chests | 30% chance |
| End city, bastion treasure and trial chamber rare vault chests | 25% chance |
| Buried treasure and woodland mansion chests | 20% chance |
| Wildercord dungeon chests | often |
| The Archivist, Cinder Warden, Star-Eater, Tide Scribe, Root Guardian and Storm Conductor | 3 each |
| The Riftcaller | 1 to 3 |
| A Fallen Star | 1 |
| Harvesting a Living Greenhouse heart with shears ([expeditions]({{ '/world/expeditions/' | relative_url }})) | 2 |
| The [Research Notebook]({{ '/progression/research/' | relative_url }}) greenhouse experiment | 1, once |
| A [Runesmith]({{ '/social/runesmith/' | relative_url }}) | Expert: 22 emeralds and 4 Amethyst Shards; Master: 20 emeralds |
| The Runesmith's Imbue [contract]({{ '/social/contracts/' | relative_url }}) | 1 |
| [Advancements]({{ '/progression/advancements/' | relative_url }}) | 19 in all |

Chest chances are defaults; a server can change them. Crystals are also used in Wellstones, armour, foci
and other gear.

## Potions

Both brew from an **Awkward Potion**:

<div class="recipe-gallery">
{% include recipe-card.html id="brewing_potion_awkward_amethyst_shard" name="Awkward Potion + Amethyst Shard: Potion of Clarity" %}
{% include recipe-card.html id="brewing_potion_awkward_lapis_lazuli" name="Awkward Potion + Lapis Lazuli: Potion of Mana" %}
{% include recipe-card.html id="brewing_potion_clarity_redstone" name="+ Redstone: lasts longer" %}
{% include recipe-card.html id="brewing_potion_clarity_glowstone_dust" name="+ Glowstone Dust: stronger" %}
{% include recipe-card.html id="brewing_potion_clarity_gunpowder" name="+ Gunpowder: splash" %}
{% include recipe-card.html id="brewing_splash_potion_clarity_dragon_breath" name="Splash + Dragon's Breath: lingering" %}
</div>

| Potion | Effect |
|---|---|
| **Potion of Clarity** | +50% regeneration for 3:00 (long: 8:00) |
| Potion of Clarity II | +100% for 1:30 |
| **Potion of Mana** | Restores 60 mana at once |
| Potion of Mana II | Restores 120 mana |

Mana potions only fill a player wearing a Cord.

## How to use it

### Meditating

**Sneak and stand still** on the ground while wearing a Cord, without using an item. After a second,
lights circle your feet and your regeneration gets **+100%** until you move. Meditating also forms
[Heart Circles]({{ '/progression/heart-circles/' | relative_url }}) and attunes
[runes of the land]({{ '/world/runes-of-the-world/' | relative_url }}).

### Getting mana back in a fight

| Way | How much |
|---|---|
| [Siphon]({{ '/progression/enchantments/' | relative_url }}) | +2 per creature hit, per level, up to 16 a cast |
| **Soulfire** | A little from its damage, up to 5 a cast |
| **Manatide** | 3 a second for 10 seconds, for you and allies it hits; once a minute each |
| **Devour** | 10 mana if it kills, twice a cast at most |
| A Potion of Mana | 60 (II: 120) |
| Forming a Heart Circle | A full refill |

### Spending less

- **Thrift** on your Cord: 7% less per level.
- **Archmage** (8th Circle): 15% less.
- A **staff** of the spell's element: 10% less. A **Focus of Thrift**: 15% less, but 10% weaker spells.
- Under a **mana storm**: 25% less.
- **Frugal** halves the cost of the rune it changes, and weakens it.

Any discount saves at least 1 mana.

## Tips and counterplay

- **A spell you can't afford** doesn't go off: *"Not enough mana"*. Cast it again within 2 seconds to
  [overcast]({{ '/spellcraft/overcasting/' | relative_url }}).
- **Passives** cost mana every second and stop when you can't pay.
- **Mana Skin** (3rd Circle) stops when your mana runs out.
- **Blood Price** spells cost health, so they work on an empty pool.
- **Manaburn** takes up to 20 mana from a player it hits, and a mana storm's backfire spills 10.
