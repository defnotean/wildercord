---
layout: default
title: Rootmolt Striders
parent: The World
nav_order: 23
permalink: /world/rootmolt-striders/
---
# Rootmolt Striders

Rootmolts are broad cave arthropods with six articulated feet, shovel forearms, bark plates and exposed breathing gills. A mature Glowcap draws them to a garden; after eating, they defend that same patch. Raised forearms and a straight line of dirt fragments announce their physical rake.

## Find a living garden

Look below Y48 in Overworld lush and dripstone caves, on covered moss or clay with dim light and an empty, dry standing cell. Natural arrival also requires a mature Glowcap nearby. Fresh buds do not qualify, and nearby Rootmolts limit further arrivals. These conditions permit an encounter; they do not guarantee one. Peaceful difficulty prevents natural arrival.

Follow the [Glowcap nursery guide]({{ '/world/glowcap-nurseries/' | relative_url }}) to understand the plant's living visitor cycle, and meet [Sporeback Snails]({{ '/world/sporeback-snails/' | relative_url }}) before building a collection garden.

A Rootmolt must reach a mature cap and browse uninterrupted before it resets that plant to a bud. It then waits before eating again. A competing consumer cannot spend the same mature cap twice. A successful meal establishes the garden the creature defends.

The meal gives no player materials. The creature has no unique death drops or experience. Its ecological role is competition and a readable physical threat, rather than a renewable loot source.

## Read the rake

![Illustrated top-view warning and sidestep diagram]({{ '/assets/images/rootmolt-rake-illustration.svg' | relative_url }})

*Drawn guide illustration, not a native Minecraft screenshot. The dashed line shows the committed direction, not a continuing beam.*

| Tell | What to do |
|---|---|
| Shovel arms rise; dirt follows a straight line | Step sideways before the rake arrives. The committed line does not rotate after you. |
| One physical rake makes contact | The creature may apply a short Root Tether; it does not repeatedly damage you with that rake. |
| Movement is reduced by Root Tether | Your attack input remains available. Hit the creature, cleanse the harmful effect, or leave its sight/range. |
| Arms lower during recovery | Move out of the defended garden before another warning. |

A real damaging hit releases its current restraint. Cleansing Root Tether, losing the source, breaking line of sight, leaving its close range, changing level or becoming ineligible also releases the owned hold. It does not remove unrelated effects. Do not stand on the line waiting for an attack cooldown to rescue you: react to the warning.

The restraint is transient across a server restart; saved meal and attack deadlines prevent a restart from granting an immediate fresh cycle.

## Observe and record

The Grimoire's Field Guide includes Rootmolts in its Wildlife group. That heading does not make this territorial creature harmless.

Watch a real meal while alive in Survival, within six blocks and with a clear view to record the meal fact. After that observation, an actual direct physical counter-hit during its warning/rake/hold, or a successful sideways miss, can record a second fact. Seeing a creature in the ordinary Field Guide is separate from observing its meal and counter.

These field facts give no immediate items and do not complete [Mara's Three Breathmarks]({{ '/world/glowcap-nurseries/#maras-three-breathmarks' | relative_url }}) by themselves. That investigation still follows its own real observation, authentic marker, harvest, nursery and filter steps. Reading this wiki does not award in-game progress.

A threatened nearby awake Sporeback can retract when the Rootmolt warns. Keep the snail's nursery and walking route accessible. Nursery shelter is covered in the existing garden guide; verify a proposed route with the real creature before relying on a narrow opening to exclude it.

## Server configuration

In `config/wildercord.json`, the `creatures` section controls natural wildlife:

- `rootmolt_strider`: enable this creature's natural spawning.
- `wildlife`: the shared wildlife master switch.
- `wildlife_spawn_multiplier`: shared population adjustment; zero disables natural wildlife spawning.

The settings affect natural admission; a higher multiplier does not remove the cave, footing, light, mature-cap or local-population requirements. Existing creatures and command-spawned fixtures are separate from natural encounter frequency.
