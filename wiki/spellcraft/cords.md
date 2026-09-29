---
title: Cords
parent: Spellcraft
nav_order: 1
description: "The four Cords: sockets, spells, rune tiers, mana, recipes and upgrading, the Cord slot, and the Cord on your wrist."
---

# Cords
{: .no_toc }

A **Cord** is what makes you a caster. You wear it in its own slot, and it decides how many spells you can
keep, how many runes each spell can hold, how strong those runes may be and how much mana you have. You can't
cast, meditate or see ley lines without one.

1. TOC
{:toc}

---

## The four Cords

| Cord | Sockets per spell | Spells | Rune tiers it fires | Max mana | Mana a second |
|---|---|---|---|---|---|
| **Twine Cord** | 3 | 1 | I | 100 | 5 |
| **Copper Cord** | 5 | 2 | I and II | 150 | 6 |
| **Amethyst Cord** | 8 | 3 | I, II and III | 225 | 7 |
| **Echo Cord** | 12 | 4 | I to IV | 300 | 8 |

A Cord's tooltip says the same thing in short: "5 sockets per spell · 2 spell(s) · 150 mana" and "Holds
runes up to Tier II".

- **Sockets per spell** is how many runes one spell can hold. Twelve is the most any spell can have.
- **Spells** is how many spells you can keep ready. You switch between them with `V` (see
  [Casting]({{ '/spellcraft/casting/' | relative_url }}#switching-spells)).
- **Rune tiers** is the strongest rune the Cord will fire. A stronger rune can still sit in the spell, but it
  stays quiet (see [Quiet runes](#quiet-runes-and-a-smaller-cord) below).
- **Max mana** and **mana a second** are your starting pool and how fast it refills. Everything else that
  adds to them (Mana Crystals, Heart Circles, enchantments, potions, ley lines) is added on top: see
  [Mana]({{ '/progression/mana/' | relative_url }}).

A **passive spell** holds **two runes** on any Cord: a lasting buff or a guard, not a whole spell.
See [Passive Spells]({{ '/spellcraft/passives/' | relative_url }}).

## Recipes

Every Cord is crafted, and each one after the first is made from the one before it. None are found in chests.

| Cord | Recipe | Unlocks in the recipe book when you hold |
|---|---|---|
| Twine Cord | 3 String over a Blank Rune (shaped, see below) | a Blank Rune |
| Copper Cord | Twine Cord, 4 Copper Ingots, 1 Amethyst Shard (shapeless) | a Twine Cord |
| Amethyst Cord | Copper Cord, 4 Amethyst Shards, 2 Gold Ingots (shapeless) | a Copper Cord |
| Echo Cord | Amethyst Cord, 2 Echo Shards, 1 Netherite Scrap (shapeless) | an Amethyst Cord |

Each Cord's recipe, as the crafting table shows it:

<div class="recipe-gallery">
{% include recipe-card.html id="twine_cord" name="Twine Cord (shaped: this layout)" %}
{% include recipe-card.html id="copper_cord" name="Copper Cord (any layout)" %}
{% include recipe-card.html id="amethyst_cord" name="Amethyst Cord (any layout)" %}
{% include recipe-card.html id="echo_cord" name="Echo Cord (any layout)" %}
</div>

A **Blank Rune** is 4 Cobblestone around 1 Lapis Lazuli, and makes 4. From scratch, an Echo Cord takes 3
String, a Blank Rune, 4 Copper Ingots, 5 Amethyst Shards, 2 Gold Ingots, 2 Echo Shards and a Netherite Scrap.
Echo Shards come from ancient cities in the deep dark, so the Echo Cord is a late-game goal.

## Upgrading

To upgrade, craft the next Cord from the one you have and put it on. That's all:

- **Your spells stay.** Learned runes and threaded spells are saved on you, not on the Cord, so a new Cord
  starts with every spell you had. The extra sockets and spell rows simply open up.
- **Your mana pool grows** the moment you put the better Cord on. Your current mana doesn't jump up; it
  refills toward the new maximum at the new rate.
- **Enchantments carry over.** The new Cord keeps everything on the old one: its enchantments, a name you gave
  it at an anvil, and its anvil history (so the next anvil job costs what it would have). Enchanting an early Cord
  isn't wasted.

Three [Heart Circles]({{ '/progression/heart-circles/' | relative_url }}) also ask for a better Cord before
they'll form: the 3rd needs a Copper Cord or better, the 5th an Amethyst Cord or better and the 8th an Echo
Cord.

Each Cord has an advancement for wearing it: **Tied On** (Twine), **Copper Wire** (Copper), **Singing
Stone** (Amethyst) and **Echoes of the Deep** (Echo). Wearing a better Cord also counts for the ones before
it.

## Rune tiers

Every rune has a tier from I to IV, shown in its tooltip ("Effect · Tier II · Fire") along with the Cord it
needs ("Needs a Copper Cord or better").

| Tier | Needs | How you usually get it |
|---|---|---|
| I | any Cord | Crafted: a Blank Rune and the rune's own items |
| II | a Copper Cord or better | Crafted, plus 2 Lapis Lazuli and a Gold Ingot |
| III | an Amethyst Cord or better | Crafted, plus a Mana Crystal and a Diamond |
| IV | an Echo Cord | Never crafted: bosses, dungeon vaults and rare structures |

A few kinds of rune follow their own rules whatever their tier: [runes of the world]({{ '/runes/world/' | relative_url }})
are only ever found in their own places, [fused runes]({{ '/runes/fused/' | relative_url }}) (Tier III) are
made at the Fusion Altar, [innate runes]({{ '/runes/innate/' | relative_url }}) (Tier I, so any Cord can hold
yours) wake in your heart, and a [Knot]({{ '/fusion-altar/knots/' | relative_url }}) takes the tier of the
strongest rune tied into it.

In the Cord screen's Codex, runes your Cord can't fire are dimmed with a small lock, and hovering one says
which Cord it needs. Trying to thread one is refused with the same reason.

## Quiet runes and a smaller Cord

Nothing you thread is ever deleted by your Cord. If a rune can't fire right now, it stays in its socket but
goes **quiet**: the spell is cast without it, and the Cord screen marks it with a small red corner. Hover it
to see why:

| Why it's quiet | What the Cord screen says |
|---|---|
| It's too strong for this Cord | Too strong for this Cord: needs a Copper Cord. Kept, but quiet |
| It's past your Cord's last socket | Past the last socket of your Twine Cord (3): kept, but quiet |
| You don't know it | You haven't learned this rune |
| It came from an add-on mod that isn't installed any more | Its add-on is missing, so it stays quiet |

This is what happens if you ever wear a **smaller Cord** than before (swapping to a spare, say):

- Spell rows your Cord doesn't have are locked. The row says which Cord it needs ("Needs a Copper Cord")
  and how many runes it's keeping for you ("5 runes kept").
- In the rows you still have, runes past your last socket stay threaded but quiet. You can still move them
  or take them off, but you can't add new ones past the last socket.
- Put the bigger Cord back on and everything wakes up again, exactly as it was.

If your selected spell is on a locked row, pressing `R` casts the next spell you can use instead.

## The Cord slot

The Cord slot is **just above the offhand slot** in your inventory, marked with the faint outline of a Cord.
Only Cords go in it, one at a time.

- **Shift-click** a Cord anywhere in your inventory and it moves into the slot, if the slot is empty.
  Shift-click the worn Cord to move it back to your inventory.
- In **creative**, the slot is in the Survival Inventory tab, beside your armour.
- The Cord is **kept when you die**. It's never dropped, whatever the game's inventory rules. Your learned
  runes, spells, Heart Circles and cooldowns are kept too. Your **mana** is not: you come back with an empty
  pool, which refills as usual.
- Taking the Cord off stops everything that needs one: you can't cast, a charge you're holding fizzles,
  passive spells stop renewing, and you stop seeing ley lines.

The first time you wear any Cord, you learn **Self**, **Bolt** and **Push**, your mana is filled, and spell 1
is threaded for you as `Bolt · Push`.

## The fifth spell

The **Tome of the Fifth Page**, in its gear slot (or held in your offhand), gives you a fifth spell whatever your Cord. Its row
appears in the Cord screen under your Cord's own rows, with a violet number. You can select it with `V`, or
bind a key to cast it directly. Take the tome out and the fifth spell goes quiet (its runes are kept). See
[Casting Gear]({{ '/gear/' | relative_url }}).

## Enchanting a Cord

Cords can be enchanted at an enchanting table, with an anvil and books, or with books from villagers and
loot. Seven enchantments go on Cords and nothing else:

| Enchantment | Levels | What it does |
|---|---|---|
| Reservoir | I to III | +25 max mana per level |
| Wellspring | I to III | +25% mana regeneration per level |
| Siphon | I and II | +2 mana for every creature your spells hit, per level (up to 16 a cast) |
| Potency | I to III | Spells hit 8% harder per level |
| Celerity | I to III | Cooldowns 8% shorter per level |
| Thrift | I to III | Spells and passives cost 7% less mana per level |
| Persistence | I and II | Spell effects last 20% longer per level |

The Cord screen's mana badge and readout always include them. More on
[Enchantments]({{ '/progression/enchantments/' | relative_url }}).

## The Cord on your wrist

Everyone around you can see your Cord: a band round your **right wrist** in the Cord's own material (twine,
copper, amethyst or echo), with a **bead** for each rune of the spell you have ready, in that rune's colour,
up to eight beads. The beads **glow for a couple of seconds after you put the Cord on** (not when you respawn
wearing it), then fade to their plain material. They **light up while you charge** a spell, brighter as the
charge builds, and **flare** for a moment when you cast. Switch spells and the beads change to match. The band
isn't drawn while you're invisible.

You can change how the beads look (their material, a fixed glow colour, and a trail your casts leave) on
the Cord screen's **Cosmetics** page. See [Cosmetics]({{ '/companions/cosmetics/' | relative_url }}).

## Which Cord for what

| You want | You need at least |
|---|---|
| Fire, Frost, Burst, Beam, Shield, On Hit, Widen, Pierce, Quicken | a Copper Cord (Tier II) |
| A second spell to swap to | a Copper Cord |
| Split, Lightning, Explode, Zone, Rain, Homing, Chain, Echo, Orbit, any fused rune | an Amethyst Cord (Tier III) |
| Spells longer than 5 runes | an Amethyst Cord (8 sockets) |
| Domain, Stasis, Sonic Boom and the other Tier IV runes | an Echo Cord |
| Four spells of twelve runes | an Echo Cord |
