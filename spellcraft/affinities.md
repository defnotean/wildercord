---
title: Creature Affinities and Climate
parent: Spellcraft
nav_order: 9
---

# Creature affinities and climate
{: .no_toc }

A fire bolt doesn't hit a blaze the way it hits a zombie. Many creatures are **weak** to an element and take more
from it, or **resist** one and take less. And **where** you fight matters too: fire burns hotter in the Nether, storm
crackles in a thunderstorm, frost bites harder in the snow. Pick the right element and a cheap spell hits like a
dear one; pick the wrong one and you'll see it shrug.

1. TOC
{:toc}

## Weak, resisted, immune

| Affinity | That element's damage | What you see over the creature |
|---|---|---|
| **Weak** | **+50%** | **Weak!** in bold, in the element's colour, and a crack of sparks |
| **Resists** | **half** | **Resisted** in grey, and a dull puff |
| **Immune** | **nothing** (it doesn't even flinch) | **Immune** in grey, and a dull puff |

The words float up over the creature for a moment (everyone nearby sees them), at most once a second however many
creatures your spell strikes, each with a quiet sound. The first time you find a weakness or a resistance on a kind of
creature, it's written into your [Bestiary](#the-bestiary).

- **Only damage changes.** Slows, freezes, knockback, marks and heals work the same on everything.
- **A reaction breaks through a resistance.** A hit that sets off a [reaction]({{ '/spellcraft/reactions/' | relative_url }})
  on a creature ignores what it resists (though not an immunity). Fire on a hoglin is halved; a **Shatter** on a
  frozen hoglin lands in full.
- **Runebound resist their own element.** A [Runebound]({{ '/world/runebound/' | relative_url }}) resists the element
  of the spell on its Cord, on top of what its kind resists: a skeleton with *Frost Bolt* takes half from your frost.
  If its kind is weak to that element, the two cancel out.
- **Players have none.** Duels and PvP are untouched.
- **Monsters' spells feel it too.** A Runebound's storm hits an iron golem 50% harder, just as yours would.

## Who's weak to what

Grouped by kind. A creature can belong to more than one group: a wither skeleton is a creature of the Nether, a
skeleton and undead, all at once.

| Creatures | Weak to | Resists |
|---|---|---|
| **The Nether's creatures**: blazes, magma cubes, ghasts, striders, wither skeletons, hoglins, zoglins, piglins, piglin brutes, zombified piglins | **Frost** | **Fire** |
| **The cold's creatures**: strays, polar bears | **Fire** | **Frost** |
| **Snow golems** | **Fire** | **Frost** (immune: they're made of it), **Blood** |
| **The undead**: zombies, husks, drowned, zombie villagers, skeletons, strays, bogged, wither skeletons, phantoms, zombified piglins, zoglins, the wither, skeleton and zombie horses | **Life** | |
| **Skeletons of every kind**, and the wither | | **Blood** (there's no blood in them to draw on) |
| **Water creatures**: fish, squid and glow squid, dolphins, turtles, axolotls, guardians, elder guardians, tadpoles, nautiluses, and the drowned | | **Fire** |
| **Arthropods**: spiders, cave spiders, bees, silverfish, endermites | **Wind** | **Life** (venomous things shrug off venom) |
| **The End's creatures**: endermen, endermites, shulkers | **Time** | **Void** |
| **Iron golems** and **copper golems** | **Storm** (metal conducts) | **Earth**, **Blood** |
| **Slimes** | **Frost** | **Earth** (they bounce) |
| **Magma cubes** (as well as the Nether's) | | **Earth** |
| **Breezes** | **Earth** | **Wind**, **Blood** |
| **Creakings** | **Fire** | |
| **Creepers** | | **Storm** (lightning only charges them up) |
| **Phantoms** (as well as undead) | **Wind** | |
| **Witches** | | **Life** |
| **Evokers** and **illusioners** | | **Arcane** |
| **Vexes** | **Arcane** | **Blood** |
| **The wither** (as well as undead) | | **Void** |
| **The warden** and **the ender dragon** | | **Void** |

Everything else, from pigs to villagers to pillagers, takes every element alike.

A few things worth knowing:

- **Fireproof creatures take nothing from flames.** Blazes, magma cubes, ghasts, striders, wither skeletons, zoglins
  and zombified piglins can't be burnt at all (nor can shulkers, vexes, the warden, the wither, the ender dragon, the
  Star-Eater or the Archivist), so a burning spell (Fire, Ember, Flashfire...) shows **Immune**. Fire's blasts
  (Explode, Meteor) don't burn, and those get half through a resistance to fire.
- **Frost used to hit blazes, striders and magma cubes five times as hard** (a rule of the game's, meant for powder
  snow). Now it's +50%, like every weakness.
- **Water creatures aren't weak to storm, because they don't need to be.** In water they're wet, so every storm hit
  already sets off **Conduct**: +50%, arcing to two more. Being wet also softens fire by a quarter, but that doesn't
  stack with a creature resisting fire: it takes half either way.

### The bosses

| Boss | Weak to | Resists | How it plays |
|---|---|---|---|
| [The Cinder Warden]({{ '/world/ember-sanctum/' | relative_url }}) | **Frost** | **Fire** | Its armour only yields to reactions, and a reaction breaks through its fire resistance, so a **Shatter** still lands in full. Frost follows up hard while its plates are cracked open. |
| [The Star-Eater]({{ '/world/astral-observatory/' | relative_url }}) | **Life** | **Void** | Burning does nothing to it either. Once its shield is down, Vinelash, Moonpetal or Venom hit 50% harder. |
| [The Tide Scribe]({{ '/world/drowned-scriptorium/' | relative_url }}) | **Life** (it's a drowned sorcerer) | **Frost** | Storm through its flood is still its real weakness (five times the shock). Frost still freezes its water; the frost itself only does half. |
| [The Archivist]({{ '/world/archive/' | relative_url }}) | **Void** (what's written can be unwritten) | **Arcane** | It's immune to burning too. Harm, Smite and the other arcane runes do half; Sonic Boom, Hollow, Blackspark and the other void runes do half as much again. |

## The Bestiary

Your [Grimoire]({{ '/progression/grimoire/' | relative_url }}) keeps a **Bestiary**: every kind of creature your spells
have struck that has an affinity, and what you've learned about it.

- **Meeting a creature** writes it in, quietly: *"Blaze · weak: ? · resists: ?"*.
- **Finding a weakness** fills it in with a toast (*"New in your Grimoire: Blaze: weak to Frost"*) and condenses
  **25 mana** toward your next [Heart Circle]({{ '/progression/heart-circles/' | relative_url }}).
- **Finding a resistance or immunity** fills it in quietly: the callout over the creature already told you.
- Each **?** is one still to find: hit it with other elements. A column reading *none* has nothing to find.
- Hover a creature for what each affinity does. A line turns green once everything about it is known.

A Runebound's resistance comes from its Cord, not its kind, so that one isn't written down.

## Elemental climate

Where you cast nudges how hard each element hits. It's modest on purpose (10 to 25%), and your HUD shows it.

| Where | Favoured | Hindered |
|---|---|---|
| **The Nether** | Fire **+20%** | Frost **-25%** |
| **The End** | Void **+20%** | |
| **A thunderstorm** over you (under the open sky) | Storm **+25%** | |
| **Rain** falling on you | | Fire **-10%** |
| **Snow and frost**: snowy and frozen lands, and high peaks where it's cold enough to snow | Frost **+20%** | Fire **-10%** |
| **Hot, dry land**: deserts, badlands, savannas | Fire **+15%** | Frost **-10%** |
| **Night**, under the open sky | Void **+10%** | |
| **Sunlight**: day, under the open sky, not raining | Life **+10%** | |
| **Deep underground**: below y 0 in the Overworld | Earth **+15%** | |
| **On a ley line**, or under a mana storm | Arcane **+15%** | |

- **They stack.** A thunderstorm at night in the rain: storm +25%, void +10%, fire -10%. However they stack, no
  element goes past +50% or below half.
- **The Nether and the End are their own climate.** No weather, days, depth or lands there (a mana storm still counts).
- **It's about where you stand**, not where the spell lands, and only **your** spells feel it (monsters' spells and
  bosses don't), so no fight changes with the weather. It counts in duels too, the same for both of you.
- **Only damage changes**, as with affinities.
- It stacks with a target's affinity: frost on a blaze in the Nether is +50% for the weakness and -25% for the
  climate, still a little better than plain.

### On the HUD

After your spell's name, beside the hotbar, a small mark appears for each element the climate changes where you
stand, with a green **▲** (it hits harder here) or a red **▼** (softer). The marks:

| Element | Mark | Element | Mark |
|---|---|---|---|
| Fire | a flame | Life | a leaf |
| Frost | a snowflake | Void | a crescent |
| Storm | a lightning bolt | Arcane | a diamond |
| Wind | gusts | Time | an hourglass |
| Earth | a mountain | Blood | a drop |

The Grimoire page says it in words under **Where you stand**: *"The Nether: Fire +20%, Frost -25%"*.

## Tips

- **Take frost to the Nether.** Nearly everything there is weak to it, and even with the Nether's -25% it comes out
  ahead. Fire, which the Nether loves, is the one thing its creatures shrug off.
- **Life magic for the undead.** Venom's poison does nothing to zombies and skeletons, but its hit, Vinelash's lash,
  Moonpetal's petals and Rootsnare's roots all land 50% harder on them.
- **Resisted? React.** A resistance doesn't hold against a reaction. Freeze a piglin, then burn it: the Shatter lands in full.
- **Fight in the storm.** Storm +25% under a thunderstorm, and everything out in the rain is wet, so every storm hit
  Conducts too.
- **Earth for the deep, void for the night.** Mining below y 0, earth hits 15% harder; out under the night sky, void 10%.
- **Watch your Runebound.** A Runebound resists its own element: read its nameplate and answer with another.
- **Fill in the Bestiary.** Each weakness is 25 mana toward your next Heart Circle, and the **?**s tell you where to look.

{: .note }
A server can switch either system off (`creature_affinities` and `elemental_climate` in its config).
