# Element Reactions

## What it is

Effects leave short **marks** on what they hit: frozen, windswept, pulled, wet, shadowed, bleeding and more.
Hit a marked target with the right element and you set off a **reaction**: bonus damage, and its name flashes
up in bold. Reactions are the biggest free damage boost in the game. There are eleven, and every element takes
part in at least one.

**A reaction breaks through a resistance** (not an immunity), so a Shatter lands in full on a creature that
shrugs off plain fire. See [Creature Affinities](affinities.md).

| Reaction | Needs | What it does |
|---|---|---|
| **Shatter!** | Fire on a **frozen** target | That hit deals **+60%**, and the target thaws. |
| **Conduct!** | Storm on a **wet** target | That hit deals **+50%** and arcs to up to two more enemies within 5 blocks for 4 damage each. |
| **Wildfire!** | Fire on a **windswept** target | Every other enemy within 3 blocks catches fire for 4 s and takes 3 damage. |
| **Implode!** | A blast (Explode, Meteor, Primer) where an enemy is **pulled** | The blast is 50% wider and hits 30% harder. |
| **Collapse!** | Repel on a **pulled** enemy | Repel deals double damage. |
| **Overload!** | Storm on a **burning** target | That hit deals **+30%**, and every other enemy within 3 blocks takes 4 damage and is thrown back. The fire goes out. |
| **Fracture!** | Earth on a **frozen** target | That hit deals **+40%**, and the target is **cracked** for 5 s: every spell, anyone's, hits it 20% harder. |
| **Blight!** | Life on a **shadowed** target | It and up to 5 more enemies within 4 blocks take 3 damage and Poison for 5 s. You heal 1 for each. |
| **Unweave!** | Arcane on a target with **two marks or more** | Every mark is used up, and the hit deals **+30% per mark** (up to four: +120%). |
| **Rupture!** | Wind on a **bleeding** target | That hit deals **+50%** plus 4 more through armour, and you heal 2. |
| **Elapse!** | Time on a **burning, poisoned or withering** target | All the damage those still had to deal lands now, half again as hard (3 to 16), and they end. |

Healing from Blight and Rupture comes at most once a second.

## How to get it

Every player can set off reactions. Effects in a spell happen **in the order you thread them**, and a mark
counts the moment it's made. So the easiest reactions happen inside one spell: put the marking effect first,
then the reacting one.

| Reaction | Try |
|---|---|
| Shatter | `Bolt · Chill · Ember` or `Bolt · Frost · Fire` |
| Conduct | Fight in the rain, or `Bolt · Undertow · Shock` |
| Wildfire | `Nova · Windcut · Ember` or `Bolt · Push · Fire` |
| Implode | `Bolt · Pull · Explode` |
| Collapse | `Bolt · Pull · Repel` |
| Overload | `Bolt · Ember · Shock` |
| Fracture | `Bolt · Chill · Pelt` or `Bolt · Frost · Aftershock` |
| Blight | `Bolt · Blind · Venom` or `Bolt · Hex · Venom` |
| Unweave | `Bolt · Chill · Push · Harm` |
| Rupture | `Bolt · Rend · Windcut` or `Bolt · Bleed · Windcut` |
| Elapse | `Bolt · Fire · Countdown` or `Bolt · Venom · Countdown` |

The first time you set off each one, it goes in your [Grimoire](../progression/grimoire.md)
and gives **150 mana** toward your next [Heart Circle](../progression/heart-circles.md).
Each also has an [advancement](../progression/advancements.md).

## How to use it

### Marks

A marked creature shows a small halo or drips in the mark's colour, so you can see a frozen husk is ready to
Shatter before you hit it.

| Mark | Lasts | Left by (for example) | Used by |
|---|---|---|---|
| **Frozen** | about 4 s after Frost, 2 s after Chill | Frost, Chill, Coldsnap, any harmful frost on a wet creature | Shatter, Fracture |
| **Windswept** | 2.5 s | Push, Windcut, Repel, Levitate, Tremor | Wildfire |
| **Pulled** | 2.5 s | Pull, Gravity Well | Implode, Collapse |
| **Wet** | while in water or rain; 5 s after Tidebreath or steam | Water, rain, Tidebreath, steam | Conduct |
| **Soaked** (counts as wet) | about 5 s | A popped Bubble, Undertow, Mire | Conduct, Flash Freeze |
| **Burning** | while it burns | Any fire that sets it alight | Overload, Elapse |
| **Shadowed** | up to 8 s | Hex, Blind, Wither, Umbra | Blight |
| **Bleeding** | about 4 s | Bleed, Rend, Cleave, Dismantle | Rupture |
| **Cracked** | 5 s | A Fracture | (every spell hits 20% harder) |

- Every mark above also counts toward **Unweave**. Harm leaves an **exposed** mark for 3 s that counts too.
- A reaction uses up its mark, except wet from water or rain. Frozen can feed Shatter or Fracture, and burning
  can feed Overload or Elapse: whichever comes first takes it.
- Runes that move **you** never mark you.
- **Blades leave marks too.** A swordsman's elemental strikes can leave their element's mark. See
  [Aura marks](../progression/aura.md#aura-marks).

Each rune's tooltip in the Cord screen says which mark it leaves and which reaction it sets off.

### Being wet

![Billows of white steam rise from a small stone-edged pool](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/b-steam.jpg)

| On a wet creature | |
|---|---|
| **Storm** | conducts: +50%, arcing to two more enemies |
| **Fire** | hits **25% softer** |
| **Frost** | freezes it solid at once, ready for Shatter or Fracture |

Fire in water boils it into steam that blinds and wets everyone inside. Storm landing in water shocks
everything standing in it. More on [Magic in the World](../world/world-magic.md).


Wet works both ways. Tidebreath leaves **you** wet, so an enemy's storm spells conduct on you.

### Spell collisions

When your bolt meets an enemy caster's bolt in the air, both burst. Two different elements make a burst of
**5 damage** within 2.5 blocks. Some pairs set off a reaction instead: **8 damage** within 4 blocks.

| Bolts | Reaction |
|---|---|
| Fire and Frost | Shatter |
| Frost and Storm | Conduct |
| Fire and Wind | Wildfire |
| Arcane and Void | Implode |
| Fire and Storm | Overload |
| Earth and Frost | Fracture |
| Life and Void | Blight |
| Blood and Wind | Rupture |
| Fire and Time | Elapse |

Your first collision earns the **Spell Collision** feat.

## Tips and counterplay

- **Practise on a [Training Dummy](../progression/training-dummy.md)**: its numbers show
  the bonus at once.
- **Marks are short.** Put the reacting effect in the same spell, or follow up fast.
- **Order matters.** `Bolt · Fire · Frost` does nothing special; `Bolt · Frost · Fire` Shatters.
- **Chain them.** Collapse leaves its target windswept, ready for Wildfire. A Fracture cracks the target so
  the next hit lands harder still.
- **Rain helps.** In the rain everything is wet: every storm hit conducts and every frost spell freezes solid.
- **On Reaction** fires the rest of a spell only where a reaction just went off:
  `Bolt · Frost · Fire · On Reaction · Burst · Explode`.
- **Prismatic Burst** (a fused rune) uses up every mark on its target at once for extra damage.
- Fighting with friends? Two casters hitting one foe with two elements can set off **Unison**. See
  [Playing Together](../social/playing-together.md).
