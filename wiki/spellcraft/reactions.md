---
title: Element Reactions
parent: Spellcraft
nav_order: 8
---

# Element reactions
{: .no_toc }

Effects leave short-lived **marks** on what they hit: frozen, windswept, pulled, wet, shadowed, bleeding.
When a later effect of the right element meets a mark, it sets off a **reaction**, and you see its name
flash up in bold (at most once a second, however many go off). Reactions are the biggest free damage boost
in the game: learn to set them up and a cheap spell hits like an expensive one. There are eleven, and every
one of the ten elements takes part in at least one.

Reactions go hand in hand with [creature affinities]({{ '/spellcraft/affinities/' | relative_url }}): many creatures
are weak to an element or resist one, and **a reaction breaks through a resistance**, so a Shatter lands in full on
a creature that shrugs off plain fire.

1. TOC
{:toc}

## The reactions

| Reaction | Needs | What it does |
|---|---|---|
| **Shatter!** | Fire damage on a **frozen** target | That hit deals **+60%**, the ice bursts and the target thaws. |
| **Conduct!** | Storm damage on a **wet** target | That hit deals **+50%** and arcs to up to **two more** enemies within 5 blocks, 4 damage each. |
| **Wildfire!** | Fire damage on a **windswept** target | Flames leap to **every other enemy within 3 blocks**: each is set alight for 4 seconds and takes 3 fire damage. |
| **Implode!** | Explode, Meteor or Primer blasting where an enemy is still **pulled** | The blast is **50% wider** and hits **30% harder**. |
| **Collapse!** | Repel on an enemy still **pulled** | Repel deals **double damage** there, in a violent burst. |
| **Overload!** | Storm damage on a **burning** target | That hit deals **+30%**, and the flames blow apart: every other enemy within **3 blocks** takes **4 damage** and is thrown back. The fire goes out. No block is harmed. |
| **Fracture!** | Earth damage on a **frozen** target | That hit deals **+40%**, the ice cracks and the target thaws, and it's left **cracked** for 5 seconds: **every spell hits it 20% harder**, whoever casts it. |
| **Blight!** | Life damage on a **shadowed** target | Rot bursts out of it: it and up to **5 more enemies** within **4 blocks** take **3 damage** and are poisoned (Poison I, 5 seconds), and you heal **1** for each one it reaches (once a second at most). |
| **Unweave!** | Arcane damage on a target with **two marks or more** | Every mark on it comes undone at once, and that hit deals **+30% for each** (at most four count: **+120%**). |
| **Rupture!** | Wind damage on a **bleeding** target | That hit deals **+50%**, the wound tears open for **4 more** straight through armour, and you heal **2** (once a second at most). |
| **Elapse!** | Time damage on a **burning, poisoned or withering** target | Their time passes at once: all the damage the fire, poison and withering still had to deal lands now, **half again** as hard (at least 3, at most 16), and they end. |

The mark a reaction needs is used up when it goes off (wet is the exception: being in water or rain
isn't something a spell can take away). Two reactions can want the same mark: frozen is Shatter's and
Fracture's, burning is Overload's and Elapse's, and whichever element reaches it first uses it.

The first time you set off each one, it's written into your
[Grimoire]({{ '/progression/grimoire/' | relative_url }}) and condenses **150 mana** toward your next
[Heart Circle]({{ '/progression/heart-circles/' | relative_url }}). Each also has its own
[advancement]({{ '/progression/advancements/' | relative_url }}) (15 experience), and setting off every one
earns **Chain Reaction**. The 3rd Heart Circle needs 1 reaction, the 4th 3 different ones, and the 6th
**5 different ones** (any five). The Runesmith's [contracts]({{ '/social/contracts/' | relative_url }})
sometimes ask for them too, and the Cinder Warden in the [Ember Sanctum]({{ '/world/ember-sanctum/' | relative_url }})
only yields to them (any of them).

### Every element's part

| Element | Leaves | Sets off |
|---|---|---|
| **Fire** | burning | Shatter, Wildfire, Implode (its blasts) |
| **Frost** | frozen, soaked | |
| **Storm** | | Conduct, Overload |
| **Wind** | windswept | Rupture, and Collapse (Repel) |
| **Earth** | windswept (Tremor, Monolith...), soaked (Mire), bleeding (Bonespur) | Fracture |
| **Life** | poisoned (Venom, Sporebloom) | Blight |
| **Void** | pulled, shadowed, withering (Wither) | |
| **Arcane** | | Unweave (and Prismatic Burst uses every mark) |
| **Time** | | Elapse |
| **Blood** | bleeding | |

You can read it off a rune, too: in the Cord screen, a rune that leaves shadowed or bleeding says so in
its tooltip (*"Leaves foes bleeding: wind damage on them sets off Rupture"*), and a rune whose damage sets
off one of the six newer reactions says which (*"On a frozen foe it sets off Fracture"*), in that
reaction's colour.

## Marks

A mark lasts a few seconds, and while it lasts it **shows**: a small halo in the mark's colour, and a tiny tick
when it's first set. Frozen is a pale ring of frost over the head, Resonant a pink one; Windswept (pale green) and
Pulled (violet) a crescent circling the body; Wet and Soaked (blue) and Bleeding (red) drips; Shadowed (dark violet)
and Ionised (yellow) motes rising off it; Cracked a cracked seal at its feet. A creature shows two marks at a
time, taking turns if it has more. So you can see that the frozen husk is ready to Shatter before you hit it.

| Mark | Lasts | Left by | Used by |
|---|---|---|---|
| **Frozen** | 4 s after Frost (or frost on a wet creature); 2 s after Chill, Coldsnap or Hail; while inside a Blizzard; while a freeze holds, plus 1 s | Frost, Chill, Freeze, Coldsnap; Hoarfrost; Hail, Blizzard, and Glacier, Black Ice, Rime Seal, Frostbite and Absolute Zero when they freeze; Flash Freeze on a wet or soaked creature; **any harmful frost on a wet creature** | Shatter, Fracture, Unweave, Prismatic Burst |
| **Windswept** | 2.5 s (Levitate: while floating, plus 1 s) | Push, Launch, Dash, Levitate, Windcut, Thunderclap, Tremor, Cyclone, Repel, Disarm, a Cushion landing's gust; Tusk Charge, Summit Wind, Basalt Surge, Shulkershell (when it opens); Tempest, Monolith, Updraft, Downdraft, Skyglyph, Recoil | Wildfire, Unweave, Prismatic Burst |
| **Pulled** | 2.5 s | Pull, Gravity Well, Hollow; Vortex, Riftcall; Hellmouth, Magnetize, Singularity | Implode, Collapse, Unweave, Prismatic Burst |
| **Wet** | while in water or rain; 5 s after Tidebreath or steam | Water, rain, Tidebreath (on you and allies), steam clouds | Conduct, Unweave and Prismatic Burst (the 5 s kind only) |
| **Soaked** (counts as wet) | about 5 s (a little longer after Mire and Drowning Word) | A popped Bubble, Tidecall, Undertow, Mire, Drowning Word, Tidewrit | Conduct, Unweave, Prismatic Burst, Flash Freeze (it freezes the water on it solid: soaked becomes frozen) |
| **Burning** | as long as it burns | Any fire that sets a creature alight (a Searing Edge blow too) | Overload, Elapse, Unweave, Prismatic Burst, Conflagration |
| **Shadowed** | as long as the curse: 8 s after Hex or Wither, 6 s after Resonant Shriek or Umbra, 5 s after Blind, 3 s after Echolocate (on what it hits); while Blackflame burns and while Entropy frays; while inside a Hush or under an Eclipse, plus a moment | Hex, Blind, Wither, Blackflame, Umbra; Echolocate, Resonant Shriek, Hush, Eclipse; Entropy | Blight, Unweave, Prismatic Burst |
| **Bleeding** | while Bleed's wound runs (4.5 s); while a Gash keeps it from healing (8 s); 4 s after Rend; 3 s after Cleave or Dismantle; while Bonespur's bleed runs, plus a moment; while inside a Crimson Mist, plus a moment | Bleed, Rend, Cleave, Dismantle, Gash; Crimson Mist, Bonespur | Rupture, Unweave, Prismatic Burst |
| **Cracked** | 5 s | A Fracture | (every spell hits it 20% harder) Unweave, Prismatic Burst |

Elapse also reads two things that aren't marks: **poisoned** (Venom, Sporebloom, a Blight's rot) and
**withering** (Wither), for as long as the effect lasts.

Runes that move **you** (Launch or Dash on Self) never mark you. The runes named after the first
semicolon in each row are [runes of the world]({{ '/runes/world/' | relative_url }}) and
[fused runes]({{ '/runes/fused/' | relative_url }}).

### Which hits count

**Fire damage that can Shatter and set off Wildfire:** Fire, Ember, Explode, Meteor, Inferno,
Flashfire, Primer, a Searing Edge blow's flames; Blazecall, Sunscorch, Cinderbrand, Soulfire (its first burn); Firestorm, Starfire's
motes, Conflagration, Everburn (its first hit), Hellmouth (its first pulse); and the innate rune
Kindling.

**Storm damage that can Conduct and Overload:** Shock, Jolt, Lightning, Thunderclap, Ripple, Thunderbird's
strikes; Plasma, Tempest, Riftbolt, Stormweave, Stormclock, Heartstopper, Thunderhead, and Magnetize's
shocks.

The other five newer reactions go off for **any** spell damage of their element, from the first hit to the
last tick of something that lingers:

- **Earth (Fracture):** Pelt, Aftershock, Tremor; Stalactite, Infest, Sandstorm, Basalt Surge, Tusk Charge;
  Magma, Sinkhole, Fossilize, Bonespur, Monolith. Root, Weigh, Shackle and Mire hold without hurting.
- **Life (Blight):** Venom, and Bramble's thorns (they hurt whatever strikes you); Vinelash, Moonpetal,
  Rootsnare. Sporebloom poisons and Drowse lulls to sleep without hurting.
- **Arcane (Unweave):** Harm, Smite, Resonance, Starfall, a Spellbrand's burst; Fangs, Starshard, Starlight Tether, Manaburn.
  Prismatic Burst uses every mark up itself instead.
- **Wind (Rupture):** Windcut, Cyclone, Repel; Summit Wind; Updraft, Downdraft, Recoil. Push, Launch, Dash,
  Levitate and Disarm throw without hurting.
- **Time (Elapse):** Countdown (when the moment catches up) and Reckoning (when it comes due).

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

### Overload

Set them alight, then strike with storm.

| Spell | Cord | Notes |
|---|---|---|
| `Bolt · Ember · Shock` | Twine | Three Tier I runes: Ember lights it, Shock blows the flames apart. |
| `Bolt · Fire · Jolt` | Copper | Jolt's 4 becomes 5.2, and everything round the target takes 4 and is thrown clear. |
| `Nova · Flashfire · Thunderclap` | Copper | Light everything within 3 blocks of you, then blast it: each burning one Overloads. |
| `Bolt · Lightning`, twice | Amethyst | Lightning sets what it strikes alight, so the second bolt Overloads. |

The fire goes out when it Overloads, so you trade the rest of the burn for the blast: best in a crowd.

### Fracture

Freeze it, then hit it with earth. It competes with Shatter for the same frost: Shatter hits harder
once, Fracture leaves the target cracked for everything that follows.

| Spell | Cord | Notes |
|---|---|---|
| `Bolt · Chill · Pelt` | Twine | Three Tier I runes: the Pelt cracks the ice. |
| `Bolt · Frost · Aftershock` | Copper | Aftershock's first impact cracks it, and its second lands on a cracked target, 20% harder. |
| `Nova · Coldsnap · Tremor` | Amethyst | Freeze everything around you, then heave the ground: every frozen one standing on it Fractures. |
| `Bolt · Chill · Pelt`, then any spell | Twine | Crack it, then hit it again within 5 seconds: 20% harder. |

A cracked target takes 20% more from **every** spell for 5 seconds, a friend's included: crack it, then
let everyone pile in.

### Blight

Shadow it with void, then strike with life.

| Spell | Cord | Notes |
|---|---|---|
| `Bolt · Blind · Venom` | Copper | Blind shadows it for 5 seconds; Venom's damage turns the shadow to rot. |
| `Bolt · Hex · Venom` | Copper | The same, and the Hex makes your spells 25% harder on it for 8 seconds. |
| `Zone · Hex · Venom` | Amethyst | Hex and poison everything in the zone: each one Blights, and the rot spreads to the rest. |

**Bramble** counts too: a shadowed monster that hits you while your thorns are up gets the rot. Blight
heals you a little for every creature the rot reaches (once a second at most), so it's at its best in a crowd.

### Unweave

Stack two marks or more, then strike with arcane.

| Spell | Cord | Notes |
|---|---|---|
| `Bolt · Chill · Push · Harm` | Copper | Frozen and windswept: Harm's 7 becomes 11.2. |
| `Bolt · Ember · Windcut · Harm` | Copper | Burning and windswept: +60%. |
| `Bolt · Ember · Windcut · Rend · Harm` | Copper | Burning, windswept and bleeding: +90%. (Windcut before Rend: on a bleeding target it would Rupture instead.) |
| `Bolt · Ember · Chill · Pull · Push · Smite` | Amethyst | Four marks: Smite hits 2.2 times as hard. |

At most four marks count. Unweave uses up every mark, so it ends any other reaction waiting on them.

### Rupture

Cut it with blood, then strike with wind.

| Spell | Cord | Notes |
|---|---|---|
| `Bolt · Rend · Windcut` | Twine | Two Tier I runes: Rend tears its armour and leaves it bleeding; Windcut tears the wound open. |
| `Bolt · Bleed · Windcut` | Copper | The bleed keeps ticking afterwards, too. |
| `Bolt · Bleed · Cyclone` | Copper | Cyclone flings everything round the point out for 3: whatever was bleeding Ruptures. |
| `Burst · Dismantle · Repel` | Copper | Three slashes on everything round you, then a blast out: every bleeding one it reaches Ruptures. |

Each Rupture heals you 2, once a second at most.

### Elapse

Leave something lingering on it (fire, poison, withering), then strike with time.

| Spell | Cord | Notes |
|---|---|---|
| `Bolt · Fire · Countdown` | Copper | Fire burns for 6 seconds; a second and a half later the countdown lands, and the last 4.5 seconds of the burn land with it, as 6.75. |
| `Bolt · Ember · Countdown` | Twine | Ember's burn is short: the rest lands for 3 at least. |
| `Bolt · Venom · Countdown` | Copper | Poison II's last 4.5 seconds would deal 7: they land as 10.5. Not on undead (they can't be poisoned). |
| `Bolt · Wither · Countdown` | Echo | Wither III's last 6.5 seconds would deal 13: they land as 16, the most Elapse deals. |

Elapse ends the fire, poison and withering it hurried along, so it's a way to have it all now, not more
of it.

## Wet, steam and the world

<img src="{{ '/assets/images/b-steam.jpg' | relative_url }}" alt="Billows of white steam rise from a small stone-edged pool" class="shot">
<span class="caption">Fire in water boils it into steam: anyone caught inside is blinded and left wet.</span>

Being wet changes more than Conduct:

| On a wet creature | |
|---|---|
| **Storm** | conducts: +50%, arcing to two more enemies |
| **Fire** | hits **25% softer**, and dries it (unless it's standing in water) |
| **Frost** | freezes it **solid at once** (for at least 3 seconds), and marks it frozen for Shatter or Fracture |

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
| Fire and Storm | **Overload!**: 8 damage within 4 blocks |
| Earth and Frost | **Fracture!**: 8 damage within 4 blocks |
| Life and Void | **Blight!**: 8 damage within 4 blocks |
| Blood and Wind | **Rupture!**: 8 damage within 4 blocks |
| Fire and Time | **Elapse!**: 8 damage within 4 blocks |

A collision reaction counts for your Grimoire like any other. Your first collision earns the **Spell
Collision** feat.

## Other marks and named bursts

- **Prismatic Burst** (a fused rune) uses up **every** mark on its target, burning, frozen, windswept,
  pulled, soaked, wet, cracked, shadowed and bleeding: 4 damage, plus 3 for each mark (up to 22).
- **Resonance** leaves a cursed mark for 10 seconds, and every Resonance hit also deals half its damage
  to each other marked enemy within 16 blocks (up to 8 of them). Unweave doesn't count this one.
- **Cinderbrand** brands a target for 6 seconds: your fire spells burn it 50% hotter.
- **Spellbrand** brands a target for 8 seconds: the next time your magic hurts it (from a moment later on),
  the brand bursts for 6 arcane damage, which can Unweave what's on it.
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
  frost freezes solid, ready for Shatter. A Fracture leaves a crack that makes the next reaction's hit
  bigger still. A Blight poisons everything it reaches, ready for Elapse.
- **Pick what to spend.** Frozen can Shatter or Fracture, burning can Overload or Elapse, and Unweave
  takes everything at once. Thread the one you want first.
- **Rain helps frost.** In the rain everything is wet, so **Flash Freeze** freezes whatever it hits solid (in
  the dry, only something soaked: after a Bubble pops, say).
- **Follow up only when it works.** **On Reaction** fires the rest of a spell only at a creature a reaction
  just went off on: `Bolt · Frost · Fire · On Reaction · Burst · Explode` blows up round the Shatter, and
  nowhere else.
