---
layout: page
title: "The Sword Tombs"
permalink: /progression/sword-tombs/
nav_order: 14
parent: Growing Stronger
description: "Intent gates, the Buried Keeper's physical attacks, and finite technique rewards."
---

# The Sword Tombs

A broken stone arch in open highland country marks a stair into the Marchkeepers' buried blade galleries. Search meadows, windswept hills, windswept gravelly hills, plains and snowy plains. Tombs are rare; their entrances reject flooded ground.

## The two thresholds

Use an **Intent Gate while holding an Aura weapon**. The outer gate asks for **Flow**, and the inner gate asks for **Edge**. Its horizontal band bears two or three marks to show the threshold without relying on colour. A qualified swordsman opens the whole gate for the party. Breathing in place is not required.

The gallery, duel chamber and inner burial room are warded against tunnelling and destruction. The entrance remains an ordinary route out. Gallery chests hold modest supplies; the technique reward belongs to the keeper encounter.

## Challenge the Buried Keeper

At the far side of the chamber, **use the Keeper Reliquary with an Edge blade**. Clear the keeper's central standing place first. Only an authentic generated reliquary can summon it, and the tomb permits one keeper at a time. Peaceful difficulty keeps it asleep.

The keeper is a blindfolded stone effigy with segmented arms, burial cloth and a long bound sword. Its attacks use physical sword strokes, scuffed lanes and ground gouges. It draws no magic circles.

<img src="{{ '/assets/images/sword-tomb-keeper.png' | relative_url }}" alt="The blindfolded stone keeper prepares a broad sword sweep beneath hanging lanterns; its boss bar names the attack and advises leaving the front" class="shot">

| Action | Tell | Response |
| --- | --- | --- |
| Broad sweep | Blade held out to its side; a broad front threatens | Leave the front, flank, or move beyond five blocks |
| Straight thrust | Blade aligned down a narrow lane | Step sideways; the lane is only 1.5 blocks wide |
| Forward guard | Blade braced across the body | Flank it, wait it out, or strike from the front with an axe |
| Recovery | Blade lowered after an attack | Attack during the opening |
| Broken guard | Effigy reels after the axe strike | Use the longer opening |

Both attacks prepare for **1.8 seconds** and lock direction before they land. Walls block hits. The thrust reaches farther than the sweep. Its forward guard reduces damage, while its side and back remain vulnerable. Recovery and broken guard take 25% more damage.

The boss bar names the current action and its answer. Different preparation sounds distinguish sweep and thrust. The keeper stays inside its chamber and returns to the centre if displaced far outside it. Leaving it without a target for ten seconds lets it recover its health.

## Recover the testament

After the keeper falls, participating survival players can **use the reliquary** to claim **two different technique scrolls**, **three Aura Shards**, and **The Keeper's Testament**, a three-page field book. Once the keeper has been hurt by a survival player, other living survival players within the encounter area can participate, including support players.

The encounter clears once per tomb. Each recorded participant can claim once, including after a restart; repeating the interaction cannot produce another reward. Arriving after the encounter has finished does not grant participation. A tomb remembers up to 64 participants. The testament connects the memorials' histories and the keeper's deliberately imperfect defence.

<img src="{{ '/assets/images/sword-tomb-testament.png' | relative_url }}" alt="The awarded Keeper's Testament open on its first page, explaining the Marchkeepers' buried blades and the keeper's blindfold" class="shot">

## Server settings and existing worlds

`aura_world.sword_tombs` controls new tomb generation, gate opening and reliquary interactions. Existing blocks remain when disabled. The current keeper continues its encounter. New tombs appear in newly generated chunks; existing terrain is not retroactively replaced.

The structure set uses 76-chunk spacing and 28-chunk separation. If a keeper disappears without dying, the authentic reliquary can rearm after ten seconds of observation with its arena loaded. This recovery check does not load remote chunks. A slain keeper stays slain.

See [the Marchkeeper battlefields]({{ '/progression/battlefields/' | relative_url }}) for the three outdoor histories, and [Aura]({{ '/progression/aura/' | relative_url }}) for stages and writing techniques.
