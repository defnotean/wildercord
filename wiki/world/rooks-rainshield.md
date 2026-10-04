---
title: Rook's Rainshield
parent: The World
nav_order: 30
permalink: /world/rooks-rainshield/
---

# Rook's Rainshield

**A mended reed fan for a single dangerous arrow.** Plant your feet, draw the cloth taut, and cover yourself or a teammate who has accepted your offer. The copper hinge and stitched panels are physical equipment; this relic uses no magic circles and spends no mana.

> **New in 0.10.0-alpha.** Native crafting, combat, saved rest and two connected clients have passed focused tests. These pictures are actual game captures from supplied test scenes.

## From a bank to a copper hinge

| Ingredient | How it reaches your crafting grid |
| --- | --- |
| Windreed Braid | Combine a Windreed Tassel, string and sweet berries. |
| Moonreed Floss | Gather an open Moonreed after a Glimmerwing has visited its prepared bud. |
| Copper ingot | Smelt ordinary copper ore. |
| Leather | Use ordinary Minecraft leather. |

Combine **one of each** in any arrangement. The shapeless recipe fits your inventory's 2 x 2 crafting grid. Gathering Moonreed Floss reveals the recipe.

Read [Moonreed Gardens]({{ "/world/moonreed-gardens/" | relative_url }}) for the plant's actual moisture, night growth and visitor requirements. A spell can prepare a damp bud; the living visit still matters.

## Open the fan

### Cover yourself

Hold the Rainshield, face the incoming shot and hold Use while looking into empty space. Keep your feet on dry support and stay still. The fan needs **0.6 seconds** to draw before it can catch an arrow. Release Use after the gesture ends before drawing again.

### Cover a teammate

1. Stand within **three blocks** of a living teammate on the same scoreboard team. Both players must be in Survival, with dry, grounded feet and a clear line of sight.
2. Right-click the teammate with the Rainshield to offer cover.
3. They must begin a **new crouch within two seconds**. Someone already crouching releases and crouches again; holding crouch before the offer does not accept it.
4. Keep the teammate in front of you, then **hold Use on that same teammate** to open the fan. They keep crouching while you prepare and hold it.
5. Hold your position and facing until the arrow reaches them. The fan covers the selected teammate; it does not cover both players or a whole group.

A selected teammate who has not accepted blocks that offer from opening. Let it expire before using the relic for yourself. A team change, separation, lost sight, recipient standing up or disconnect ends cooperative cover.

## A short opening, a real price

| Rule | Value |
| --- | --- |
| Draw time | 12 ticks / 0.6 seconds |
| Protection after drawing | At most 40 ticks / 2 seconds |
| Capacity | One eligible frontal arrow |
| Cost of a catch | Four durability |
| Rest after a catch | 300 ticks / 15 seconds for carrier and recipient |
| Heavy hands after a catch | Mining Fatigue I for 40 ticks / 2 seconds |
| Original durability | 65; drawing needs at least five points remaining |

Times assume normal 20-tick server operation. The last pivot point is reserved, so a fully used fan can make sixteen catches. The spent arrow disappears; it is not reflected, redirected or returned as loot. A miss or cancelled opening does not grant resources.

The rest belongs to each player. Switching to another Rainshield, logging out or dying does not reset it. Release Use between drawings; keeping the button held does not extend the opening.

## Read the counterplay

| Attack or action | What happens |
| --- | --- |
| Ordinary, tipped or spectral vanilla arrow from the front | One qualifying collision within the 70-degree total cone can be caught after the draw. |
| Arrow from behind or outside the frontal cone | It continues into ordinary collision. |
| Second arrow after a catch | The spent opening offers no protection. |
| Imbued arrow, other magic, trident or melee | It bypasses this fan. |
| Carrier walks, jumps, sprints or turns away | The opening ends. |
| Carrier or recipient takes actual damage | Their current cover ends. |

Against a fan, change your angle, rush the carrier or send a second shot. With a fan, protect a retreat or one exposed crossing rather than standing still indefinitely. It offers a deliberate opening for a teammate, with position and timing that opponents can read.

## Visual review

The intended sequence is a copper-pivot click, staggered reed ribs and mended cloth drawing into a short fan, then one physical catch with cloth and splinter accents. Full and Minimal settings keep the same gameplay limits.

### Folded and prepared

<img src="{{ '/assets/rainshield/rainshield_full_actual_supplied_held_firstperson.png' | relative_url }}" alt="The folded reed and linen fan held to the right of the clear crosshair" class="shot">

<img src="{{ '/assets/rainshield/rainshield_full_actual_prepared_firstperson.png' | relative_url }}" alt="The actual prepared fan beneath the aiming area in first person" class="shot">

### Full and Minimal settings

<img src="{{ '/assets/rainshield/rainshield_full_actual_unfolded_cloth.png' | relative_url }}" alt="Full physical cloth and ribs during actual held preparation" class="shot">

<img src="{{ '/assets/rainshield/rainshield_minimal_actual_unfolded_cloth.png' | relative_url }}" alt="Minimal physical cue retains fewer ribs and one cloth section" class="shot">

### After one catch

<img src="{{ '/assets/rainshield/rainshield_full_actual_arrow_catch.png' | relative_url }}" alt="The carrier after an actual eligible arrow catch, with the fan lowered" class="shot">

The opened cue partly overlaps the raised hand and mouth in this third-person angle; eyes and nose remain visible. The prepared first-person aiming area is clear. These still frames establish the shown views, not all cameras, shaders or the entire unfolding animation. Two-process interaction tests independently verify fresh consent, the catch, respawn, saved rest and disconnect cancellation.
