---
title: Creature Affinities and Climate
parent: Spellcraft
nav_order: 9
---

# Creature Affinities and Climate

## What it is

Many creatures are **weak** to an element and take more from it, or **resist** one and take less. Where you
fight matters too: fire burns hotter in the Nether, storm cracks harder in a thunderstorm.

| Affinity | Damage from that element | What you see |
|---|---|---|
| **Weak** | **+50%** | **Weak!** in the element's colour |
| **Resists** | **Half** | **Resisted** in grey |
| **Immune** | **None**, and it doesn't flinch | **Immune** in grey |

- **Only damage changes.** Slows, freezes, knockback, marks and heals work the same on everything.
- **Reactions break resistances.** A hit that sets off a [reaction]({{ '/spellcraft/reactions/' | relative_url }})
  ignores a resistance, but not an immunity. So does a target torn open by **Rend**.
- **Runebound resist their own element**, the one on their Cord. See [Runebound]({{ '/world/runebound/' | relative_url }}).
- **Players** start with no affinities. From [affinity]({{ '/progression/affinity/' | relative_url }}) level III you
  resist that element yourself: 10% at III, 15% at IV, 20% at V.

## How to get it

Your [Grimoire]({{ '/progression/grimoire/' | relative_url }}) keeps a **Bestiary** of every creature with an
affinity that your spells have hit.

- **Hitting a creature** adds it: "Blaze · weak: ? · resists: ?".
- **Finding a weakness** fills it in and gives **25 mana** toward your next
  [Heart Circle]({{ '/progression/heart-circles/' | relative_url }}) and 25 points of affinity with that element.
- Each **?** is something still to find. Try other elements on it.

### Who's weak to what

| Creatures | Weak to | Resists |
|---|---|---|
| **Nether creatures**: blazes, magma cubes, ghasts, striders, wither skeletons, hoglins, zoglins, piglins, piglin brutes, zombified piglins | Frost | Fire |
| **Cold creatures**: strays, polar bears | Fire | Frost |
| **Snow golems** | Fire | Immune to Frost; resist Blood |
| **The undead** | Life | |
| **Skeletons of every kind**, and the wither | | Blood |
| **Water creatures**, and the drowned | | Fire |
| **Arthropods**: spiders, bees, silverfish, endermites | Wind | Life |
| **End creatures**: endermen, endermites, shulkers | Time | Void |
| **Iron and copper golems** | Storm | Earth, Blood |
| **Slimes** | Frost | Earth |
| **Magma cubes** (as well as Nether) | | Earth |
| **Breezes** | Earth | Wind, Blood |
| **Creakings** | Fire | |
| **Creepers** | | Storm |
| **Phantoms** (as well as undead) | Wind | |
| **Witches** | | Life |
| **Evokers, illusioners** | | Arcane |
| **Vexes** | Arcane | Blood |
| **The wither, the warden, the ender dragon** | | Void |

Wildercord's own creatures:

| Creature | Weak to | Resists |
|---|---|---|
| Cinderfox | Frost | Fire |
| Rimehare | Fire | Frost |
| Mossback Tortoise | Frost | Earth, Wind |
| Bramblewalker | Fire | Earth, Life |
| Geode Crawler | Storm | Earth, Arcane |
| Skyray | Storm | Wind |
| Thunderwing Harpy | Earth, Frost | Storm, Wind |
| Glimmerwing | Fire, Wind | Arcane |
| Gloomstalker | Arcane | Void |
| Lumen Stag | Void | Arcane |
| Mana Ooze | Fire | |
| Bog Witch-Frog | Frost | Life |

Bosses:

| Boss | Weak to | Resists |
|---|---|---|
| [The Cinder Warden]({{ '/world/ember-sanctum/' | relative_url }}) | Frost | Fire |
| [The Star-Eater]({{ '/world/astral-observatory/' | relative_url }}) | Life | Void |
| [The Tide Scribe]({{ '/world/drowned-scriptorium/' | relative_url }}) | Life | Frost |
| [The Archivist]({{ '/world/archive/' | relative_url }}) | Void | Arcane |

Everything else takes every element alike. Creatures that can't burn (blazes, the warden, the Star-Eater, the
Archivist and others) show **Immune** to burning spells.

## How to use it

### Elemental climate

Where **you** stand changes how hard each element hits. The spell panel shows a green **▲** or red **▼** mark for
each changed element, and the Grimoire lists it under **Where you stand**.

| Where | Stronger | Weaker |
|---|---|---|
| The Nether | Fire +20% | Frost -25% |
| The End | Void +20% | |
| A thunderstorm over you | Storm +25% | |
| Rain on you | Frost +10% | Fire -10% |
| Snowy lands and cold peaks | Frost +20% | Fire -10% |
| Deserts, badlands, savannas | Fire +15% | Frost -10% |
| Night, under open sky | Void +10% | |
| Sunlight, under open sky | Life +10% | |
| Below y 0 in the Overworld | Earth +15% | |
| A ley line or mana storm | Arcane +15% | |
| A ley crossing | Every element +10%, spells cost 10% less | |
| Full moon, clear night | Arcane +15%, Void +15% | |
| New moon, clear night | Blood +15%, Void +10% | |
| Noon, not raining | Fire +15% | |
| Dawn and dusk | Time +15% | |

- **They stack**, but no element goes above +50% or below half.
- **Only your spells** feel the climate, not monsters'. It counts in duels for both sides.
- Find ley crossings and moon phases on [Places and Times of Power]({{ '/world/places-of-power/' | relative_url }}).

## Tips and counterplay

- **Take frost to the Nether.** Almost everything there is weak to it, even with the -25%.
- **Life for the undead.** Life hits land 50% harder on zombies and skeletons.
- **Resisted? React.** Freeze a piglin, then burn it: the Shatter lands in full.
- **Fight storm in the storm.** Storm is +25%, and everything in the rain is wet, so storm hits Conduct too.
- **Read a Runebound's nameplate** and answer with a different element.

{: .note }
A server can turn off creature affinities or climate in its settings.
