---
title: Element Reactions
parent: Spellcraft
nav_order: 8
---

# Element reactions
{: .no_toc }

Effects leave short-lived **marks** on what they hit: frozen, windswept, pulled, wet. When a later
effect of the right element meets a mark, it sets off a **reaction**, and you see its name flash up in
bold (at most once a second, however many go off). Reactions are the biggest free damage boost in the
game: learn to set them up and a cheap spell hits like an expensive one.

Reactions go hand in hand with [creature affinities]({{ '/spellcraft/affinities/' | relative_url }}): many creatures
are weak to an element or resist one, and **a reaction breaks through a resistance**, so a Shatter lands in full on
a creature that shrugs off plain fire.

1. TOC
{:toc}

## The five reactions

| Reaction | Needs | What it does |
|---|---|---|
| **Shatter!** | Fire damage on a **frozen** target | That hit deals **+60%**, the ice bursts and the target thaws. |
| **Conduct!** | Storm damage on a **wet** target | That hit deals **+50%** and arcs to up to **two more** enemies within 5 blocks, 4 damage each. |
| **Wildfire!** | Fire damage on a **windswept** target | Flames leap to **every other enemy within 3 blocks**: each is set alight for 4 seconds and takes 3 fire damage. |
| **Implode!** | Explode, Meteor or Primer blasting where an enemy is still **pulled** | The blast is **50% wider** and hits **30% harder**. |
| **Collapse!** | Repel on an enemy still **pulled** | Repel deals **double damage** there, in a violent burst. |

The mark a reaction needs is used up when it goes off (wet is the exception: being in water or rain
isn't something a spell can take away).

The first time you set off each one, it's written into your
[Grimoire]({{ '/progression/grimoire/' | relative_url }}) and condenses **150 mana** toward your next
[Heart Circle]({{ '/progression/heart-circles/' | relative_url }}). Each also has its own
[advancement]({{ '/progression/advancements/' | relative_url }}) (15 experience). The 3rd Heart Circle
needs 1 reaction, the 4th 3 different ones, and the 6th all five. The Runesmith's
[contracts]({{ '/social/contracts/' | relative_url }}) sometimes ask for them too, and the Cinder Warden
in the [Ember Sanctum]({{ '/world/ember-sanctum/' | relative_url }}) only yields to them.

## Marks

A mark lasts a few seconds. Nothing on screen names it, but you can see most of them: frozen creatures
are frosted over and slowed, windswept ones are flying through the air, pulled ones are being dragged,
wet ones are in water or dripping.

| Mark | Lasts | Left by | Used by |
|---|---|---|---|
| **Frozen** | 4 s after Frost (or frost on a wet creature); 2 s after Chill, Coldsnap or Hail; while inside a Blizzard; while a freeze holds, plus 1 s | Frost, Chill, Freeze, Coldsnap; Hoarfrost; Hail, Blizzard, and Glacier, Black Ice, Rime Seal, Frostbite and Absolute Zero when they freeze; **any harmful frost on a wet creature** | Shatter, Prismatic Burst |
| **Windswept** | 2.5 s (Levitate: while floating, plus 1 s) | Push, Launch, Dash, Levitate, Windcut, Thunderclap, Tremor, Cyclone, Repel, a Cushion landing's gust; Tusk Charge, Summit Wind, Basalt Surge, Shulkershell (when it opens); Tempest, Monolith, Updraft, Downdraft, Skyglyph, Recoil | Wildfire, Prismatic Burst |
| **Pulled** | 2.5 s | Pull, Gravity Well, Hollow; Vortex, Riftcall; Hellmouth, Magnetize, Singularity | Implode, Collapse, Prismatic Burst |
| **Wet** | while in water or rain; 5 s after Tidebreath or steam | Water, rain, Tidebreath (on you and allies), steam clouds | Conduct, Prismatic Burst |
| **Soaked** (counts as wet) | about 5 s (a little longer after Mire and Drowning Word) | A popped Bubble, Tidecall, Undertow, Mire, Drowning Word, Tidewrit | Conduct, Prismatic Burst |
| **Burning** | as long as it burns | Any fire that sets a creature alight | Prismatic Burst, Conflagration |

Runes that move **you** (Launch or Dash on Self) never mark you. The runes named after the first
semicolon in each row are [runes of the world]({{ '/runes/world/' | relative_url }}) and
[fused runes]({{ '/runes/fused/' | relative_url }}).

### Which hits count as fire and storm

**Fire damage that can Shatter and set off Wildfire:** Fire, Ember, Explode, Meteor, Inferno,
Flashfire, Primer; Blazecall, Sunscorch, Cinderbrand, Soulfire (its first burn); Firestorm, Starfire's
motes, Conflagration, Everburn (its first hit), Hellmouth (its first pulse); and the innate rune
Kindling.

**Storm damage that can Conduct:** Shock, Jolt, Lightning, Thunderclap, Ripple, Thunderbird's strikes;
Plasma, Tempest, Riftbolt, Stormweave, Stormclock, Heartstopper, Thunderhead, and Magnetize's shocks.

## Setting each one off

Effects in a spell happen **in the order they're threaded**, and a mark counts the moment it's made. So
the easiest reactions happen inside a single spell: put the effect that marks first, then the one that
reacts.

### Shatter

Freeze it, then burn it.

| Spell | Cord | Notes |
|---|---|---|
| `Bolt · Chill · Ember` | Twine | Three Tier I runes. Chill's frost lasts 2 seconds, long enough. |
| `Bolt · Frost · Fire` | Copper | Frost's 5 damage, then a Fire hit at +60%. |
| `Nova · Coldsnap · Flashfire` | Copper | Freeze and burn everything within 3 blocks of you at once: each one Shatters. |
| `Bolt · Frost · Delay · Bolt · Fire` | Copper | Frost now, the fire bolt a moment later. |

In the **rain**, or on anything standing in water, *any* harmful frost freezes solid at once: even a
light Chill. Fight by a lake and every frost spell sets up a Shatter.

### Conduct

Get them wet, then strike with storm.

- Fight **in the rain**, or when they're **standing in water**. Every storm hit conducts.
- **Bubble**: trap them in a bubble. It pops 3 seconds later and leaves them soaked for 5 seconds.
  Then Shock them.
- **Soak them in one spell** with a runes-of-the-world effect: `Bolt · Undertow · Shock`, or `Zone ·
  Tidecall · Shock`.
- **Boil a puddle**: a fire spell landing in water raises a cloud of steam that blinds and wets every
  enemy inside it for 5 seconds.
- **Strike the water**: a storm spell that lands in water runs through all the water joined to it and
  shocks everything standing in it. That counts as Conduct too (see
  [Magic in the World]({{ '/world/world-magic/' | relative_url }})).

{: .warning }
Being wet works both ways. Tidebreath leaves **you** wet for 5 seconds, so an enemy's storm spells
conduct on you. On the bright side, fire hurts you 25% less while you're wet.

### Wildfire

Throw them with wind, then set them alight while they're still flying.

| Spell | Cord | Notes |
|---|---|---|
| `Nova · Windcut · Ember` | Twine | Everything within 2.5 blocks is cut, shoved and lit, and each one spreads the flames. |
| `Bolt · Push · Fire` | Copper | Push marks the target, Fire sets off Wildfire on everyone around it. |
| `Burst · Repel · Flashfire` | Copper | Blow a crowd away from you and set the lot alight. |

Wildfire is at its best in a crowd: every enemy within 3 blocks of the windswept one catches fire.

### Implode

Pull them together, then blast them.

| Spell | Cord | Notes |
|---|---|---|
| `Bolt · Pull · Explode` | Amethyst | One spell: Pull marks the target, Explode goes off 50% wider and 30% harder. |
| `Zone · Gravity Well · Delay · Burst · Explode` | Amethyst | Gravity Well drags everything in for 2 seconds, then the blast. |

Only one enemy in the blast needs to be marked for the whole blast to Implode.

### Collapse

Pull them in, then Repel them.

| Spell | Cord | Notes |
|---|---|---|
| `Bolt · Pull · Repel` | Copper | Repel's 5 damage becomes 10. |
| `Zone · Gravity Well` then `Burst · Repel` | Amethyst and Copper | Drag a crowd in, then blow it apart. |
| `Bolt · Pull · Repel · Fire` | Copper | Collapse, and Repel leaves the target windswept, so the Fire sets off **Wildfire** too. |

## Wet, steam and the world

<img src="{{ '/assets/images/b-steam.jpg' | relative_url }}" alt="Billows of white steam rise from a small stone-edged pool" class="shot">
<span class="caption">Fire in water boils it into steam: anyone caught inside is blinded and left wet.</span>

Being wet changes more than Conduct:

| On a wet creature | |
|---|---|
| **Storm** | conducts: +50%, arcing to two more enemies |
| **Fire** | hits **25% softer**, and dries it (unless it's standing in water) |
| **Frost** | freezes it **solid at once** (for at least 3 seconds), and marks it frozen for Shatter |

Harmful spells change the ground they land on too: fire lights grass and boils puddles into steam,
frost freezes water to walk on, storm runs through water, wind knocks arrows out of the air, and more.
All of it is on [Magic in the World]({{ '/world/world-magic/' | relative_url }}).

## Spell collisions

When your bolt meets an enemy caster's bolt in the air, both burst. What happens depends on the first
effect's element in each:

| The two bolts | Result |
|---|---|
| Same element | Both simply burst. |
| Different elements | A burst of **5 damage** within 2.5 blocks. |
| Fire and Frost | **Shatter!**: **8 damage** within 4 blocks |
| Frost and Storm | **Conduct!**: 8 damage within 4 blocks |
| Fire and Wind | **Wildfire!**: 8 damage within 4 blocks |
| Arcane and Void | **Implode!**: 8 damage within 4 blocks |

A collision reaction counts for your Grimoire like any other. Your first collision earns the **Spell
Collision** feat.

## Other marks and named bursts

- **Prismatic Burst** (a fused rune) uses up **every** mark on its target, burning, frozen, windswept,
  pulled, soaked and wet: 4 damage, plus 3 for each mark (up to 22).
- **Resonance** leaves a cursed mark for 10 seconds, and every Resonance hit also deals half its damage
  to each other marked enemy within 16 blocks (up to 8 of them).
- **Cinderbrand** brands a target for 6 seconds: your fire spells burn it 50% hotter.
- Some other bursts flash their name the same way but aren't element reactions and don't go in the
  Grimoire's list: **Unison!** (two casters, two elements, one foe: see
  [Playing Together]({{ '/social/playing-together/' | relative_url }})), **Collision!**, **Combo!**,
  **Chorus!**, **Blackspark!** and **Ignite!** (the innate Kindling's fifth stack).

## Tips

- **Practise on a [Training Dummy]({{ '/progression/training-dummy/' | relative_url }})**: its floating
  numbers show the reaction's extra damage at once.
- **Marks are short.** Frozen lasts 4 seconds at most after Frost, windswept and pulled only 2.5. Put
  the reacting effect in the same spell, or cast the follow-up quickly.
- **Order matters inside a spell.** `Bolt · Fire · Frost` does nothing special; `Bolt · Frost · Fire`
  Shatters.
- **Chain them.** Collapse leaves its target windswept, ready for Wildfire. A soaked target hit by
  frost freezes solid, ready for Shatter.
