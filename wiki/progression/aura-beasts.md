---
layout: default
title: The Highland Beasts
parent: Growing Stronger
nav_order: 16
---

# The Highland Beasts

The high passes shelter animals whose bodies resist spells. **Stonehorn** and **Galeclaw** have different warnings, movement and feeding habits. Blades and Aura damage cut normally. Supporting magic can still slow or hold them, and their recovery gives damaging spells a better opening.

## Stonehorn: give the grazer room

![Stonehorn's curved horns, rock shoulders and cloven feet in game]({{ '/assets/images/stonehorn-grazer.png' | relative_url }})

Look on dry, open ground in meadows, windswept hills, windswept gravelly hills and stony peaks, at least twelve blocks above sea level. Stonehorn grazes without destroying grass. Its layered rock shoulders, moss and curving horns make it recognizable.

Approach **sneaking**, or hold wheat. Walking within four blocks without either makes it territorial. It stamps for two seconds before charging straight ahead. Its direction stays committed: **step sideways**. A charge ends in a long recovery; colliding with a wall leaves it recovering longer.

Offer wheat to a peaceful Stonehorn to receive a **Stonehorn Plate**. It consumes one wheat and sheds one plate. Its saved two-minute cooldown prevents immediate repeat feeding, and feeding during that cooldown consumes nothing. An animal already retaliating or preparing an attack will not accept food.

Craft a **Bastion Poultice** from one plate, one clay ball and one wheat. Using it grants Resistance I for ten seconds, with **Slowness I** for the same duration. It is consumed and has a thirty-second cooldown: useful preparation with a movement tradeoff.

## Galeclaw: watch the landing

![Galeclaw's hooked beak, banded feathers and talons in game]({{ '/assets/images/galeclaw-ridge-runner.png' | relative_url }})

Look on open windswept hills/forests, jagged or frozen peaks, snowy slopes and groves. Snowy slopes and groves overlap the Rimehare's natural habitat. This feathered ridge runner has a hooked beak, taloned feet and a fan tail. It scavenges dropped raw rabbit or chicken and hunts nearby Rimehares. Recent spellcasting within eighteen blocks attracts its attention.

Its two-second crouch marks a landing in stone dust. **Leave that marked area before it leaps**: the landing stays where preparation began. It spreads its articulated wings during the jump, then rests through an exposed recovery. Terrain can obstruct its leap. Warning and leap have distinct sounds and subtitles.

Offer raw rabbit or chicken before provoking it to collect a **Galeclaw Plume**, with the same saved two-minute shed cooldown. Craft a **Ridge Whistle** from one plume, one copper ingot and one string. Using it distracts visible stalking Galeclaws within sixteen blocks for ten seconds. The whistle is reusable, with a ten-second cooldown.

A whistle cannot stop a prepared leap or an animal retaliating after damage. Use it early, then create distance; it does not tame Galeclaws or make combat harmless.

## Choose the opening

| Damage source | Stonehorn, normally / recovering | Galeclaw, normally / recovering |
| --- | --- | --- |
| Spell damage | 12% / 40% gets through | 20% / 65% gets through |
| Physical blade or Aura | Normal damage | Normal damage |

These values apply before other normal protections. They are damage resistance, not immunity to every spell interaction. A mage can control the encounter while a swordsman closes in, or save damage for recovery. Neither creature is classified as a boss just to block support abilities.

## Habitats and server settings

Natural spawns require open sky, suitable ground, empty dry space and highland elevation. Each species has a local cap of two living animals within ninety-six blocks and uses the existing creature spawn pool. They do not reproduce automatically. Home ranges bound pursuit, and loading resumes in recovery instead of releasing a stale attack.

`aura_world.aura_beasts` controls natural spawning and aggressive behavior. Disabling it leaves existing creatures and materials in the world. Peaceful difficulty suppresses attacks. Bestiary entries appear in the Grimoire through the existing field-guide observation system.

Related: [Aura]({{ '/progression/aura/' | relative_url }}) · [The Sleeping Blade]({{ '/progression/sleeping-blade/' | relative_url }})
