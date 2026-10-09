---
title: Cords
parent: Spellcraft
nav_order: 1
description: "The four Cords: sockets, spells, rune tiers, mana, recipes, upgrading, the Cord slot and Cord enchantments."
---

# Cords

## What it is

A **Cord** makes you a caster. You wear it in its own slot. It sets how many spells you keep, how many runes
each spell holds, how strong those runes can be and how much mana you have. Without one you can't cast,
meditate or see ley lines.

| Cord | Sockets per spell | Spells | Rune tiers it fires | Max mana | Mana a second |
|---|---|---|---|---|---|
| **Twine Cord** | 3 | 1 | I | 100 | 5 |
| **Copper Cord** | 5 | 2 | I and II | 150 | 6 |
| **Amethyst Cord** | 8 | 3 | I to III | 225 | 7 |
| **Echo Cord** | 12 | 4 | I to IV | 300 | 8 |

Mana Crystals, Heart Circles, enchantments, potions and ley lines all add on top of the Cord's mana. See
[Mana]({{ '/progression/mana/' | relative_url }}). A [passive spell]({{ '/spellcraft/passives/' | relative_url }})
holds two runes on any Cord.

## How to get it

Every Cord is crafted, and each one after the first is made from the one before. None are found in chests.

| Cord | Recipe |
|---|---|
| Twine Cord | 3 String over a Blank Rune (shaped) |
| Copper Cord | Twine Cord, 4 Copper Ingots, 1 Amethyst Shard |
| Amethyst Cord | Copper Cord, 4 Amethyst Shards, 2 Gold Ingots |
| Echo Cord | Amethyst Cord, 2 Echo Shards, 1 Netherite Scrap |

<div class="recipe-gallery">
{% include recipe-card.html id="twine_cord" name="Twine Cord (shaped: this layout)" %}
{% include recipe-card.html id="copper_cord" name="Copper Cord (any layout)" %}
{% include recipe-card.html id="amethyst_cord" name="Amethyst Cord (any layout)" %}
{% include recipe-card.html id="echo_cord" name="Echo Cord (any layout)" %}
</div>

A **Blank Rune** is 4 Cobblestone around 1 Lapis Lazuli and makes 4. Echo Shards come from ancient cities, so
the Echo Cord is a late-game goal.

**Upgrading** is just crafting the next Cord and putting it on:

- **Your spells stay.** Runes and spells are saved on you, not the Cord. New sockets and rows simply open up.
- **Your mana pool grows** at once. Current mana then refills toward the new maximum.
- **Enchantments, anvil names and anvil history carry over** to the new Cord.

Some [Heart Circles]({{ '/progression/heart-circles/' | relative_url }}) need a better Cord: the 3rd needs Copper
or better, the 5th Amethyst or better, and the 8th an Echo Cord. Lesson spells also need an Echo Cord.

Wearing each Cord earns an advancement: **Tied On** (Twine), **Copper Wire** (Copper), **Singing Stone**
(Amethyst) and **Echoes of the Deep** (Echo).

## How to use it

**The Cord slot** is just above the offhand slot in your inventory. Shift-click a Cord to move it in or out.
In creative, the slot is in the Survival Inventory tab.

- The Cord is **kept when you die**, along with your runes, spells, Heart Circles and cooldowns. You come back
  with an empty mana pool.
- Taking the Cord off stops casting, fizzles a charge you're holding and stops passives.
- The first time you wear any Cord, you learn **Self**, **Bolt** and **Push**, your mana fills, and spell 1 is
  set to `Bolt · Push`.

**Rune tiers.** Every rune has a tier from I to IV, shown in its tooltip.

| Tier | Needs | Usual source |
|---|---|---|
| I | any Cord | Crafted: a Blank Rune plus the rune's own items |
| II | Copper Cord or better | Crafted, plus 2 Lapis Lazuli and a Gold Ingot |
| III | Amethyst Cord or better | Crafted, plus a Mana Crystal and a Diamond |
| IV | Echo Cord | Never crafted: bosses, dungeon vaults and rare places |

[Runes of the world]({{ '/runes/world/' | relative_url }}) are only found in their own places,
[fused runes]({{ '/runes/fused/' | relative_url }}) are made at the Fusion Altar,
[innate runes]({{ '/runes/innate/' | relative_url }}) wake in your heart, and a
[Knot]({{ '/fusion-altar/knots/' | relative_url }}) takes the tier of its strongest rune.

**Quiet runes.** Your Cord never deletes what you've threaded. A rune that can't fire stays in its socket but
goes **quiet**, marked with a small red corner. The spell is cast without it. A rune goes quiet when:

- it's too strong for your Cord,
- it sits past your Cord's last socket,
- you haven't learned it,
- it came from an add-on that's no longer installed.

Wear a smaller Cord and its missing spell rows lock ("Needs a Copper Cord"), keeping their runes. Put the bigger
Cord back on and everything wakes up exactly as it was. If your selected spell is on a locked row, `R` casts
the next spell you can use.

**The fifth spell.** The **Tome of the Fifth Page**, in its gear slot or your offhand, gives you one more spell
on any Cord. Its row has a violet number. See [Casting Gear]({{ '/gear/' | relative_url }}).

**Enchanting.** Cords take enchantments from a table, an anvil or books. Seven enchantments go on Cords only:

| Enchantment | Levels | Effect |
|---|---|---|
| Reservoir | I to III | +25 max mana per level |
| Wellspring | I to III | +25% mana regeneration per level |
| Siphon | I to II | +2 mana per creature your spell hits, per level (up to 16 a cast) |
| Potency | I to III | Spells hit 8% harder per level |
| Celerity | I to III | Cooldowns 8% shorter per level |
| Thrift | I to III | Spells and passives cost 7% less per level |
| Persistence | I to II | Spell effects last 20% longer per level |

More on [Enchantments]({{ '/progression/enchantments/' | relative_url }}).

## Tips and counterplay

- **Read a caster's wrist.** Everyone wears their Cord as a band on the right wrist, with a bead for each rune
  of the ready spell in that rune's colour. The beads light up while they charge and flare when they cast.
  Change the beads' look on the Cord screen's [Cosmetics]({{ '/companions/cosmetics/' | relative_url }}) page.
- **Enchant early.** Enchantments follow the Cord up every upgrade, so nothing is wasted.
- **What each Cord opens:**

| You want | You need at least |
|---|---|
| Fire, Frost, Burst, Beam, Shield, Widen, Pierce, Quicken, a second spell | Copper Cord |
| Split, Lightning, Explode, Zone, Rain, Homing, Chain, Orbit, fused runes, spells longer than 5 runes | Amethyst Cord |
| Domain, Stasis, Sonic Boom and other Tier IV runes, four spells of twelve runes | Echo Cord |
