# Conduit: Circle XVIII traversal

Third lesson of the authored pack, using the same architecture as Tollgate.

## Use case
Prepare an exit: plant a rod on a ledge or behind cover, fight, then arrive on it. Blink is instant and goes where the spell lands. Conduit commits to a spot in advance, warns everyone with a visible rod, takes a 0.4 s spark, and can be denied.

## Discovery and lesson
- Feat **Grounded** (Storm Conductor) makes *Notes on a Grounded Storm* retrievable.
- A three-page live reading with active Circle XVIII teaches the rune. Old saves are covered by the feat.
- Practice is recorded on the first arrival.

## Rules
- The row must be exactly Pillar + Conduit.
- The first press plants a rod on visible, buildable ground within 20 blocks, in your own dungeon ward. The rod lasts 20 s and costs 32 base mana once, with a 15 s shared rest.
- A second fresh press sparks for 8 ticks, then teleports you onto the rod. The arrival is free.
- Arrival needs all of these:
  - the rod within 32 blocks
  - a clear line of sight
  - a safe landing
  - claim permission
  - the same ward
- A refused arrival keeps the rod.

## Counterplay
- Any hostile standing within 1.5 blocks grounds the rod and refuses arrival.
- Damage during the spark cancels it, using the same interrupt threshold as Masters arts.
- So do a cast lock, an edited book or becoming unavailable.
- Breaking the line of sight or leaving the 32-block range also stops it.

## Delayed ownership
Bound to the original connected caster body. The rod retires on death, a world change, respawn, reconnect or server stop. Rest is saved and never renewed or refunded.

## Presentation
- Pillar pose.
- Lightning crack when the rod is planted.
- A thin rod light, which brightens with sparks around you during the spark.
- Beacon chime on the spark and beacon fade when the spark is cancelled or grounded.
- On arrival: a trident-thunder strike and an arrival ray.
- Storm seal signature.
- HUD reads "press <key> to arrive".

## Verification
The native test plants a rod, checks that a zombie beside it grounds the rod (no movement, rod kept), then clears it and arrives, checking the free arrival and practice. Hit-interrupt and two-player checks are left for CI.
