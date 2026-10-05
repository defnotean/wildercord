# Mana


![The spell HUD beside the hotbar: a circle badge, a row of rune icons, and a violet mana bar reading 218/380 with an up-chevron](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/hud.png)
<span>Your mana bar sits in the spell HUD. The little chevron means your regeneration is boosted.</span>

Every spell costs **mana**. You have a pool of it (your **max mana**) that refills a little every
second (your **regeneration**). You only have mana while you wear a [Cord](../spellcraft/cords.md),
and it only refills while you wear one. It belongs to **you**, not the Cord: swap Cords and your mana
stays, up to the new Cord's maximum.


## Where your numbers come from

Hover the **mana badge** in the Cord screen for a full breakdown of your max mana and regeneration,
line by line, and a list of every way to grow them.

### Max mana

Everything adds up:

| Source | Max mana |
|---|---|
| Twine Cord | 100 |
| Copper Cord | 150 |
| Amethyst Cord | 225 |
| Echo Cord | 300 |
| Each [Mana Crystal](#mana-crystals) absorbed (up to 100) | +10 (up to +1,000) |
| [Reservoir](enchantments.md) on your Cord | +25 per level (up to +75) |
| Each working [Heart Circle](heart-circles.md) | +15 (up to +300) |
| A **Focus of the Deep Well** in your off-hand | +50 while you hold it (see [Casting Gear](../gear.md)) |

The most you can have is **1,725**: an Echo Cord, 100 crystals, Reservoir III, twenty circles and a Focus of
the Deep Well. If your maximum drops (you put the focus away, or a circle cracks), any mana above the
new maximum is lost.

### Regeneration

Your **base regeneration** comes from your Cord and your circles:

| Source | Mana a second |
|---|---|
| Twine Cord | 5 |
| Copper Cord | 6 |
| Amethyst Cord | 7 |
| Echo Cord | 8 |
| Each working Heart Circle | +0.5 (up to +4) |

Then every **boost** adds a share of that base. The boosts add together, they don't multiply each
other:

| Boost | Extra regeneration | How |
|---|---|---|
| [Wellspring](enchantments.md) | +25% per level (up to +75%) | A Cord enchantment |
| Clarity | +50% (Clarity II: +100%) | A [Potion of Clarity](#potions) |
| Meditating | +100% | [Sneak and stand still](#meditating) |
| Standing on a ley line | +100% | [Ley lines](ley-lines.md) |
| Near an awake Wellstone | +50% | Within 12 blocks of one set on a ley line |
| Under a mana storm | +100% | A [world event](../world/world-events.md) |

**Regeneration = base × (1 + every boost)**. A server can also scale everyone's regeneration up or
down; the mana badge says so if it does.

Example: an Echo Cord (8) with four circles (+2) is a base of 10 a second. With Wellspring II (+50%),
meditating (+100%) on a ley line (+100%), that's 10 × 3.5 = **35 mana a second**.

A **familiar** out and within 24 blocks gives a little more on top: +10%, +15% or +20% of your whole
regeneration, as it grows. See [Familiars](../companions/familiars.md).

The HUD's mana bar shows a small up-chevron while any boost is on: violet on a ley line or near a
Wellstone, cyan otherwise.

## Mana Crystals

A **Mana Crystal** raises your max mana by **10, forever**. Hold it and use it (right-click) to absorb
it: *"Max mana +10 (3/100 crystals)"*. Up to **100** crystals count (+1,000). After that the crystal won't
absorb (*"Your mana can't grow further with crystals"*) and stays in your hand.

Crystals belong to you, not your Cord.

### Crafting one

![Crafting grid: top row Lapis Lazuli · Amethyst Shard · Lapis Lazuli; middle row Amethyst Shard · Diamond · Amethyst Shard; bottom row Lapis Lazuli · Amethyst Shard · Lapis Lazuli](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/mana_crystal.png)

4 Lapis Lazuli, 4 Amethyst Shards and a Diamond make one.

### Finding them

| Where | Chance |
|---|---|
| Ancient city chests | 30% |
| Stronghold library chests | 30% |
| End city chests | 25% |
| Bastion treasure chests | 25% |
| Trial chamber vaults (their rare rewards) | 25% |
| Buried treasure | 20% |
| Woodland mansion chests | 20% |
| Chests in the Archive, Ember Sanctum, Astral Observatory and Drowned Scriptorium | often, sometimes several |
| The Cinder Warden, the Star-Eater, the Tide Scribe and the Archivist | 3 each, every time |
| A Fallen Star | 1 |
| A closed rift | sometimes |
| The Riftcaller | 1 to 3 |
| A [Runesmith](../social/runesmith.md) | Expert: 22 emeralds and 4 Amethyst Shards; Master: 20 emeralds |
| The Runesmith's Imbue [contract](../social/contracts.md) | 1 |
| [Advancements](advancements.md) | 16 in all |

The chest chances are the defaults; a server can change them. Mana Crystals are also an ingredient:
the Wellstone, Tier III runes and several pieces of casting gear need one.

## Potions

Two potions, both brewed from an **Awkward Potion**:

<div>
![Awkward Potion + Amethyst Shard: Potion of Clarity](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/brewing_potion_awkward_amethyst_shard.png)
![Awkward Potion + Lapis Lazuli: Potion of Mana](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/brewing_potion_awkward_lapis_lazuli.png)
![+ Redstone: lasts longer](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/brewing_potion_clarity_redstone.png)
![+ Glowstone Dust: stronger](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/brewing_potion_clarity_glowstone_dust.png)
![+ Gunpowder: splash](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/brewing_potion_clarity_gunpowder.png)
![Splash + Dragon's Breath: lingering](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/brewing_splash_potion_clarity_dragon_breath.png)
</div>

| Potion | Brew | Effect |
|---|---|---|
| **Potion of Clarity** | Awkward Potion + Amethyst Shard | Clarity: **+50%** mana regeneration for **3:00** |
| Potion of Clarity (long) | + Redstone | +50% for **8:00** |
| Potion of Clarity II | + Glowstone Dust | **+100%** for **1:30** |
| **Potion of Mana** | Awkward Potion + Lapis Lazuli | Instantly restores **60 mana** |
| Potion of Mana II | + Glowstone Dust | Instantly restores **120 mana** |

Add Gunpowder for a **splash** potion and Dragon's Breath for a **lingering** one, as with any potion.
A splash of Mana restores less the further you are from where it breaks, the way vanilla's healing
splashes do. Mana potions only fill the mana of a player wearing a Cord.

## Meditating

**Sneak and stand still** on the ground while wearing a Cord, without using an item. After a second
you're meditating: soft violet lights turn round your feet and glyphs drift in toward you, and your
regeneration gets **+100%** until you move or stand up.

Meditating is also how you form a [Heart Circle](heart-circles.md),
and, with a Blank Rune in hand in the right place, how you attune one to a
[rune of the land](../world/runes-of-the-world.md).

## Getting mana back in a fight

| Way | How much |
|---|---|
| [Siphon](enchantments.md) on your Cord | +2 mana per creature your spell hits, per level, up to 16 a cast |
| **Soulfire** (a rune of the world) | The damage it deals gives back a little mana, up to 5 a cast (a mana storm's echo or Twin Star's second cast shares the same 5) |
| **Manatide** (a rune of the world) | You and the allies it hits regain 3 mana a second for 10 seconds (30 at most: Extend doesn't make it flow longer); once a minute each |
| **Devour** (a fused rune) | 10 mana (and 4 absorption) if it kills; twice a cast at most |
| A Potion of Mana | 60 (II: 120), at once |
| Forming a Heart Circle | Refills you completely |

## Running dry

- **A spell you can't afford** doesn't go off, and nothing is spent: *"Not enough mana (38/46)"*. Cast
  it again within 2 seconds to [overcast](../spellcraft/overcasting.md), cracking a
  Heart Circle to pay.
- **Passives** cost mana every second. Without the mana for a second's upkeep, a passive stops renewing
  until you have it again. See [Passives](../spellcraft/passives.md).
- **Mana Skin** (3rd Circle) pays a fifth of the damage you take with mana, 2 mana per point of health.
  With no mana left, it stops.
- **Blood Price** spells cost health instead of mana, so they still work on an empty pool (but never
  enough to kill you).
- A **Free Recast** from a wild surge costs nothing at all.
- Some magic drains mana: **Manaburn** takes up to 20 from a player it hits, and a mana storm's
  backfire spills 10.

## Spending less

Your mana goes further with:

- **Thrift** on your Cord: spells and passives cost 7% less per level.
- **Archmage** (8th Circle): 15% less.
- A **staff** of the spell's element: 10% less. A **Focus of Thrift**: 15% less (and 10% weaker).
- A **mana storm**: 25% less while you're under it.
- **Frugal**: halves the cost of the rune it changes (and weakens it).

Any discount always saves at least 1 mana, even on a cheap spell.
