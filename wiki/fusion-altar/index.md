---
title: The Fusion Altar
nav_order: 6
has_children: true
permalink: /fusion-altar/
---

# The Fusion Altar

Once you know a rune, every further copy used to be useless. The Fusion Altar turns spare runes
into stronger ones, new ones, or a whole spell in a single rune.

**Craft it** from 4 Amethyst Blocks, 4 Deepslate Tiles and a Lodestone (tiles in the corners,
amethyst on the sides, the lodestone in the middle). Right-click it to open it. Put runes in the
three sockets round the middle, and a catalyst in the middle socket: the altar works out which
fusion you mean, shows what it will make and what it costs, and the button does it. Fusions are
paid in XP levels (free in creative). The result waits in the result socket; take it out before the
next fusion.

## Upgrade: three of the same rune

Three copies of one rune, at the same rank and with nothing in the middle, make that rune's next
rank.

| Rank | Power | Costs |
|---|---|---|
| I | as crafted | |
| II | +25% | 2 XP levels |
| III | +50% | 5 XP levels (three rank II runes) |

- Right-click a ranked rune to learn it. That rune is then stronger **in every spell it's
  threaded in**, at the same mana cost. Ranked runes shimmer, and their name says their rank
  ("Fire Rune II").
- Only effects with power have ranks (Fire, Harm, Heal, Shock...). For effects measured in levels,
  like Swift's Speed or Haste, and for mining tiers, rank III counts as one Amplify.
- The Cord screen shows your rank in each rune's tooltip, and the readout names it: "A bolt: Fire II
  (+25% power)".

## Choose the right route

| Inputs | Catalyst | Result | XP levels |
|---|---|---|---|
| Three identical effects at the same rank | None | Next rank | 2 for II; 5 for III |
| Two elemental effects | Amethyst Shard | One of 55 elemental fusions, or a matching one of 40 signature fusions | 3 |
| Two effects or existing exact weaves, at most eight leaves total | Block of Amethyst | An exact weave preserving every ingredient | 3 × (total leaves − 1) |
| One Blank Rune and a selected active spell | String | A Knot storing the sequence in one socket | One per rune, minimum 2 |

Two-leaf weaves need at least their highest component tier. Three or four need Tier III; five to eight need Tier IV. Their base mana is the sum of the leaves. Shapes, modifiers and links belong in Knots, rather than elemental weaves.

## How to finish a fusion

Read the preview, leave the unused third socket empty for a combine (or lay a reagent in it, below), and press **Fuse**. Ingredients and XP are charged when the operation succeeds. Take the output before starting another operation. The native mouse handling is corrected for Minecraft 26.3 in this release.

## Add a reagent

Reagents come from the [residues]({{ '/world/residues/' | relative_url }}) big magic leaves on the world. Lay one in
the free third socket while two effects fuse (shard) or weave (block), and it changes the result in one way of its
element's. It's used up with the fusion.

| Reagent | Does |
|---|---|
| Cinder Ash | Keeps the higher of the two ranks |
| Everfrost Shard | Half the XP levels |
| Fulgurite Shard | A rank I result comes out rank II |
| Bottled Gale | A signature pair makes its elements' fusion instead |
| Geode Grit | The amethyst stays on the altar |
| Wildbloom Petal | Two of the result |
| Hollow Dust | The lower-tier rune stays on the altar |
| Star Dust | One rank higher, for 3 more XP levels |
| Hourglass Sand | No XP for a named fusion you've made before |
| Sanguine Bead | Up to 6 XP levels paid in health instead |

The panel says what it will do, and refuses one that would change nothing, so a reagent is never wasted.

## Imprint an innate

Sneak-use a Blank Rune on the altar to imprint your awakened innate for three XP levels. A failed or unaffordable imprint consumes nothing. A Survival soul weave requires its owner's innate and the appropriate Heart Circle access; creative previews waive ownership/payment. Innates are not ordinary rank-up materials.

## Go deeper

- [Combining and the complete named fusion table]({{ '/fusion-altar/combining/' | relative_url }})
- [Ranks]({{ '/fusion-altar/ranks/' | relative_url }})
- [Knots]({{ '/fusion-altar/knots/' | relative_url }})
- [Physical magic and extended weaving]({{ '/spellcraft/physical-magic/' | relative_url }})
- [Altar troubleshooting]({{ '/troubleshooting/' | relative_url }})


## Field signatures

[Field Signatures]({{ '/fusion-altar/field-signatures/' | relative_url }}) explains six exact pairings for growing watered banks, recovering expedition supplies, helping allies and controlling a short positioning window. Each recipe has its own carved rune, material casting sequence and practical limits.

## Expedition signatures

[Expedition Signatures]({{ '/fusion-altar/expedition-signatures/' | relative_url }}) opens an illustrated field journal for twelve exact pairings: readable projectile counters, willing allied support, tracked chest intake, short voluntary traversal and bounded mineral or passage inspection. It lists altar ingredients, mana, resource limits and the conditions that produce no effect. The chapter identifies its development-build scope and links the remaining acceptance work.
