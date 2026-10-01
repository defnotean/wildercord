---
title: Familiar Jobs and Event Echoes
nav_order: 3
parent: Companions and Style
---

# Familiar jobs, event echoes and spell trials

## Choose a familiar job

`/runelab familiar <companion|scout|guardian|gardener>` saves the active familiar's job preference on its owner.

* **Companion:** the familiar's existing magic for its element, including offensive or support assistance.
* **Scout:** reveal the nearest nearby hostile mob with a short glow and a pointing ray.
* **Guardian:** grant brief Absorption when the owner was recently attacked by a living mob. It does not refresh an existing Absorption effect.
* **Gardener:** mark a ripe nearby crop with happy particles. It never harvests or replaces farm blocks.

The selected utility job replaces elemental assistance. Searches run at bounded assistance intervals; the gardener visits at most a 9×3×9 set of loaded blocks. Role selection persists through logout and death. Cinnamon has her own owner, sitting and personality system; these jobs belong to bonded wisps.

## World event aftermath

A looted star, won/sealed rift or ended mana storm leaves a ten-minute saved echo. Nearby players see a small particle landmark; crouching within four blocks discovers its boon once per player. Star echoes give night vision and slow falling, rift echoes give brief Resistance, and storm echoes give Regeneration.

The ledger holds at most 32 echoes and 128 claimants per echo. Echoes do not alter terrain, force chunk loading or award repeatable items. Existing crater restoration and event-monster cleanup still run. An absent player can return and discover an unexpired echo later.
