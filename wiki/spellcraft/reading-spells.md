---
title: How a Spell Is Read
parent: Spellcraft
nav_order: 3
description: "The rules that turn a row of runes into a spell: groups, the implicit Self, how modifiers find their rune, links and segments, friendly fire, many worked examples, cost, cooldown and the limits on a spell."
---

# How a Spell Is Read
{: .no_toc }

A spell is simply the runes on its row, read **from left to right**. The rules are short, and the Cord
screen's readout always shows you the result, so you never have to guess. This page explains every rule in
depth, with worked examples from the simplest to the cleverest, and shows exactly how a spell's mana and
cooldown are worked out.

1. TOC
{:toc}

---

## The four families

| Family | Colour | Job | Examples |
|---|---|---|---|
| [Shape]({{ '/runes/shapes/' | relative_url }}) | teal | *Where* the spell goes and *who* it touches. Starts a group. | Self, Bolt, Beam, Burst, Zone, Orbit, Domain |
| [Effect]({{ '/runes/effects/' | relative_url }}) | its element's | *What happens* to whatever the shape hit. | Fire, Heal, Freeze, Blink, Stasis, Swap |
| [Modifier]({{ '/runes/modifiers/' | relative_url }}) | gold | *Changes* the closest rune on its left that it can change. | Amplify, Extend, Widen, Split, Homing, Rapid |
| [Link]({{ '/runes/links/' | relative_url }}) | violet | Ends a segment: the rest happens *later*, *somewhere else* or *only if*. | On Hit, Delay, Echo, Pulse, On Hurt, Combo |

Everything below comes down to four rules:

1. **A shape starts a group**, and the effects after it land on whatever it hits.
2. **A spell that starts with an effect** is cast on you, as if it began with Self. After a link, effects
   with no shape of their own usually land on whatever set the link off (the creature hit, the attacker),
   or on you after a Delay or a Pulse.
3. **A modifier changes the closest rune on its left that it can change**, and never reaches back past a link.
4. **A link ends the segment**, and everything after it happens later, somewhere else, or only if something
   is true.

## Groups: a shape and its effects

A **shape** starts a **group**. Every effect after it, up to the next shape or link, belongs to that group,
and lands **in order** on everything the shape hits.

`Burst · Fire · Heal` is one group: everything within 4 blocks of you is struck by Fire, then by Heal. (Who
actually feels each one is decided by [friendly fire](#friendly-fire-who-a-spell-touches): the Fire burns
your enemies and the Heal mends you and your allies.)

**Several groups in one segment go off together**, at the same moment and from the same place.
`Bolt · Fire · Burst · Heal` is two groups: a fire bolt flies at what you're looking at, and at the same time
a healing burst goes off around you. The readout gives each group its own line:

> A bolt: Fire<br>
> Everything within 4 blocks: Heal

A shape with no effects after it still flies, bursts or strikes, but does nothing to what it hits; the readout
says "Bolt has no effect after it". The one exception is a shape that a link is watching (see
[On Hit](#links-and-segments)), which is there to set the link off.

{: .note }
One effect breaks the "in order" rule on purpose: **Stasis** always lands first in its group, wherever it's
threaded, so every other hit in the group is held until time moves again. `Barrage · Stasis · Harm` and
`Barrage · Harm · Stasis` both hold every blow.

## The implicit Self

A spell that begins with an **effect** has no shape of its own at the start, so the Cord reads it as if it
began with **Self**. `Heal` is `Self · Heal`: it heals you, costs the same (Self costs nothing), and saves a
socket. The readout says "You: Heal".

After a link, effects with no shape of their own (before the next shape) land on an implicit shape too, and
which one depends on the link. The readout calls it "The target".

| After | Effects with no shape land on | Shapes after it start from |
|---|---|---|
| The start of the spell | You | You, aimed where you look |
| Delay, Pulse | You | You, aimed where you look *at that moment* |
| On Hit | The creature that was hit (each one), or the block if it hit only a block | The hit, carrying on the way the spell was going |
| On Kill | The spot where the creature died (it's gone, so nothing is there to touch) | That spot |
| On Hurt | Whatever hurt you | The attacker, carrying on away from you |
| On Land | You | You, where you landed |
| On Low Health | You | You |
| Echo, and the conditions (If Sneaking, If Airborne, Combo, If Wounded, If Outnumbered, If Wet) | The same as before the link | The same place as before the link |
| Imbue | Whatever the stored spell is released at | See [Imbuing]({{ '/spellcraft/imbuing/' | relative_url }}) |

So `Bolt · On Hit · Fire` sets the creature the bolt hit alight, and `Bolt · On Hit · Burst · Fire` bursts
into flame around the point where it hit. **Self** is always you, wherever it appears.

## Modifiers find their rune

A modifier doesn't just change the rune in front of it. It looks **left**, rune by rune, and attaches to the
**closest rune that has what it needs**, skipping everything that doesn't. In the Cord screen, a gold line
under the sockets joins each modifier to the rune it found.

- **Amplify** needs power. In `Bolt · Fire · Amplify` it finds Fire. In `Bolt · Amplify · Fire` the only rune
  on its left is Bolt, which has no power, so Amplify does nothing and the readout warns you: "Amplify does
  nothing here: nothing on its left that it can change."
- **Split** needs a shape that can make copies. In `Bolt · Fire · Split` it skips Fire and finds Bolt, so
  `Bolt · Fire · Split` and `Bolt · Split · Fire` are the same spell.
- **Widen** needs a radius, and the closest rune with one wins. In `Burst · Explode · Widen` it finds
  Explode (a wider blast); in `Burst · Widen · Explode` it finds Burst (a wider burst).

Some more rules:

- **A modifier on an effect** changes just that effect. **A modifier on a shape** changes the whole group
  (three copies, a wider area, a faster bolt), and multiplies the cost of the whole group. Rapid, Vow and
  Blood Price sit on a shape too, but they change the **whole spell**, so they multiply the whole spell's
  cost, whichever shape they sit on.
- **Modifiers stack.** `Fire · Amplify · Amplify` is +125% power (1.5 × 1.5), at 1.6 × 1.6 = 2.56 times the
  cost. Most stacks multiply like that; the [limits](#the-limits-on-a-spell) below say where they stop.
- **A modifier never reaches back past a link.** In `Bolt · Fire · On Hit · Amplify`, Amplify can't reach
  the Fire. It *can* change the link itself: `Delay · Extend` waits two seconds instead of one, and
  `Pulse · Quicken` pulses twice as often.
- **A Knot is sealed.** Modifiers inside a [Knot]({{ '/fusion-altar/knots/' | relative_url }}) only change
  the Knot's own runes, and modifiers outside can't reach in.

### What each modifier can change

| Modifier | Needs | Can change |
|---|---|---|
| Amplify, Overcharge, Execute, Trial Key, Kindled, Unstable | power | Effects with power (damage, healing, force, a buff's strength). The rune pages list which. |
| Extend | duration | Effects that last; the shapes Zone, Trail, Wall, Orbit, Totem, Domain and Vortex; the link Delay |
| Widen, Focus | radius | Effects with a radius (Explode, Meteor, Inferno...); the shapes Burst, Zone, Rain, Cone, Wall, Ring, Pillar, Wave, Mine, Totem, Domain, Crescent, Orb, Blitz, Nova, Comet, Cluster, Lance, Sweep, Vortex, Snare and Constellation |
| Quicken | speed | The shapes Bolt, Arc, Spark, Wisp, Comet, Ricochet, Cluster, Crescent, Orb, Wave, Zone, Wall, Totem, Domain, Barrage, Sweep and Stream; the links Delay and Pulse |
| Split | copies | The shapes Bolt, Beam, Arc, Spark, Wisp, Comet, Ricochet, Cluster, Crescent, Orb, Lance, Prism, Burst, Zone, Rain, Pillar, Mine and Orbit |
| Volley | repeat shots | Bolt, Beam, Arc, Crescent, Spark, Comet |
| Pierce | passing through | Bolt, Beam, Ray |
| Bounce | bouncing | Bolt, Arc, Ricochet |
| Chain | jumping on | Touch, Bolt, Beam, Ray |
| Homing | steering | Bolt |
| Linger | landing again | Effects that can land again (most damage and control effects) |
| Frugal | | Any effect |
| Rapid, Vow, Blood Price | a shape | Any shape (they change the whole spell: see [cost](#cost) and [cooldown](#cooldown)) |

Every rune's page lists exactly which modifiers work on it: see [Shapes]({{ '/runes/shapes/' | relative_url }}),
[Effects]({{ '/runes/effects/' | relative_url }}) and [Modifiers]({{ '/runes/modifiers/' | relative_url }}).

### What the common modifiers do

| Modifier | Does | Cost × |
|---|---|---|
| Amplify | +50% power | 1.6 |
| Extend | Twice as long | 1.4 |
| Widen | +50% radius | 1.5 |
| Focus | Half the radius, +50% power | 1.2 |
| Quicken | Bolts, sparks, crescents twice as fast (other projectiles a little less); fields, walls, totems and Domains strike twice as often; Barrage 4 more blows; Stream twice the strikes; Delay and Pulse gaps halved | 1.2 |
| Split | Three copies: bolts, beams and other things that fly fan out; bursts, fields, pillars and mines spread into a ring around the first; Rain drops five more strikes per copy; Orbit gets three more orbs | 2.4 |
| Volley | Fires three times, a quarter second apart | 2.4 |
| Pierce | Passes through 3 more targets | 1.3 |
| Bounce | Bounces off blocks 3 more times | 1.3 |
| Chain | After a hit, jumps to up to 3 more enemies within 6 blocks | 1.8 |
| Homing | Steers toward the nearest enemy within 12 blocks | 1.4 |
| Linger | The effect lands twice more, a second apart | 1.8 |
| Frugal | Half the mana; 40% weaker and shorter | 0.5 |
| Overcharge | +150% power | 3.0 |
| Execute | Double power against targets under half health | 1.3 |
| Rapid | Halves the whole spell's cooldown | 1.4, on the whole spell |
| Vow | That shape's effects hit twice as hard; the whole spell's cooldown is four times longer | 1.0 |
| Blood Price | The whole spell is paid for in health, 1 per 5 mana | 1.0 |

## Links and segments

A **link** ends a **segment**. Everything after it is a new segment that happens later, somewhere else, or
only if something is true. In the readout, a link is a header line and the rest of the spell sits indented
beneath it.

| Link | Tier | Mana | The rest of the spell... |
|---|---|---|---|
| Delay | I | 2 | fires 1 second later, from you. Extend doubles the wait, Quicken halves it. |
| On Hit | II | 2 | fires wherever the shape before it hits: at each creature it hits, or at the block if it hit only a block. |
| On Land | II | 2 | fires the next time you touch the ground (within 10 seconds), where you land. |
| Pulse | II | 2 | fires three times, a second apart, from you. Quicken halves the gap. The part after it costs three times over. After On Hit or On Kill, it goes off for the first hit or kill only. |
| On Hurt | II | 2 | fires at whatever next hurts you (within 15 seconds). |
| If Sneaking | II | 1 | fires only if you're sneaking when you cast. |
| If Airborne | II | 1 | fires only if you're in the air (not standing, not in water). |
| Imbue | II | 3 | isn't cast: it's stored, with 3 charges, in an item or block. See [Imbuing]({{ '/spellcraft/imbuing/' | relative_url }}). |
| On Kill | III | 2 | fires at each creature the shape before it kills, where it died. |
| Echo | III | 2 | carries straight on, and *everything before the Echo* fires again half a second later, from you. After On Hit or On Kill, it goes off for the first hit or kill only. |
| On Low Health | III | 2 | fires when your health drops below 30% (within 30 seconds). |
| Combo | III | 2 | fires only on every third cast of this spell. |
| If Wounded | II | 1 | fires only if you're below half health. |
| If Outnumbered | III | 1 | fires only if 3 or more enemies are within 8 blocks of you. |
| If Wet | II | 1 | fires only if you're in water or rain. |

If Wounded, If Outnumbered and If Wet are [runes of the world]({{ '/runes/world/' | relative_url }}). Every
link is described on [Links]({{ '/runes/links/' | relative_url }}).

Things worth knowing about links:

- **On Hit, On Kill and Imbue watch the group just before them.** In `Bolt · Fire · Burst · Heal · On Hit · Explode`,
  On Hit watches the Burst, not the Bolt. They need a shape in front of them in the same segment ("On Hit
  needs a shape before it to watch").
- **On Hit fires once for every hit.** A bolt that pierces three creatures, three split bolts, a Chain's
  every jump or each pulse of a Zone all set it off again, at each creature hit (at most eight from any one
  hit). On Kill does the same for every kill.
- **A repeat after On Hit or On Kill goes off once.** An **Echo** or a **Pulse** there is paid for once, so
  it goes off for the first hit (or kill) only, not for every creature. The readout says so: "(first hit
  only)". A Pulse's three runs are each paid for, so an Echo after a Pulse goes off in every run.
- **Shapes after a link start where the link fired.** After On Hit, a Bolt flies on from the point of impact
  the way the first one was going, a Burst goes off around it, and a Zone or a Rain lands on the ground there.
  After Delay, the new Bolt leaves your hands a second later, aimed wherever you're looking then.
- **After On Kill, use an area.** The creature is gone, so an effect on "the target" has nothing to land on.
  `On Kill · Burst · Explode` blows up everything around the body. Movement effects still work: in
  `Bolt · Pierce · On Kill · Blink` you blink to the body.
- **Conditions don't wait.** If Sneaking, If Airborne, Combo, If Wounded, If Outnumbered and If Wet check
  once, the moment the spell reaches them, and either carry on at once or stop there. Everything before them
  has already happened. `Bolt · Fire · If Sneaking · Bolt · Frost` always throws the fire bolt, and adds a
  frost bolt when you sneak.
- **Links chain.** `Bolt · On Hit · Bolt · On Hit · Bolt · Fire` is a bolt that becomes a bolt that becomes a
  fire bolt. A cast can go at most eight links deep.
- **Combo counts casts of this spell** since you joined the world. The third, sixth, ninth cast and so on
  fire what's after it.

### Echo, in full

Echo repeats **everything before it** (from the very start of the spell) half a second later, from you, and
at the same moment carries on with whatever is after it.

- `Bolt · Fire · Echo` throws two fire bolts, half a second apart.
- **Echoes stack by doubling.** Each Echo repeats everything before it, earlier Echoes included, so
  `Bolt · Fire · Echo · Echo` throws four bolts (one, then two together, then one more) and three Echoes
  eight. Only three Echoes count: a fourth costs mana and does nothing.
- **An Echo after a link is part of that link's segment**, so it happens when that segment does. After On
  Hit or On Kill, which fire for every creature, it goes off only for the first: in
  `Bolt · Fire · Split · On Hit · Burst · Explode · Echo`, the Echo sits under "On hit:" in the readout
  ("0.5s later, everything before this fires again (first hit only).") and the first bolt to hit sets off the
  whole spell again from you, half a second later. Three fireballs that explode, then the whole thing
  once more, just as it's paid for.
- Inside an [imbued]({{ '/spellcraft/imbuing/' | relative_url }}) spell, an Echo repeats only what was
  stored.

## Friendly fire: who a spell touches

Friendly fire is **off**, always:

- **Harmful effects** (damage, control, knockback) never touch **you**, your **tamed pets** or anyone on your
  **team**. Other players and their pets are only harmed if the server allows PvP, and then rune damage to a
  player is scaled down (to 60% unless the server changes it). Players in creative or spectator are never harmed.
- **Helpful effects** (healing, buffs, shields) only touch **you and your allies**: your pets and players on
  your team. A player who isn't on your team isn't an ally, so to heal a friend with `Burst · Heal`, join the
  same team (an operator can set one up with the game's team command).
- **Movement effects** move you (Blink, Grapple, Dash on Self), and **world effects** work on blocks
  (Break, Grow, Light), only where you're allowed to build.

That's why one group can hold both: `Burst · Fire · Heal` burns every enemy within 4 blocks and heals you and
your allies in the same breath, and `Nova · Push` throws back everything around you except you.

## Worked examples

From simple to clever. Mana and cooldown are the plain numbers, before any discounts of your own; the Cord
is the smallest one that can hold the spell.

### 1. One effect: `Heal`
{: .no_toc }

> You: Heal

The implicit Self: it heals you 8 health. **12 mana, 0.6 s. Twine Cord.**

### 2. A shape and an effect: `Bolt · Fire`
{: .no_toc }

> A bolt: Fire

A bolt flies up to 48 blocks; what it hits takes 5 fire damage and burns for 6 seconds. **12 mana, 0.6 s.
Copper Cord** (Fire is Tier II).

### 3. Where the modifier goes: `Bolt · Fire · Amplify`
{: .no_toc }

> A bolt: Fire (+50% power)

Amplify looks left, finds Fire, and makes it 50% stronger. **18 mana, 0.85 s.** Swap the last two runes
(`Bolt · Amplify · Fire`) and Amplify only has Bolt to its left, which has no power: it does nothing, costs
nothing, and the readout warns you.

### 4. A modifier that skips: `Bolt · Fire · Split`
{: .no_toc }

> 3 bolts: Fire

Split skips Fire (an effect can't split) and finds Bolt: three fire bolts in a fan. `Bolt · Split · Fire` is
the same spell. Because Split is on the shape, it multiplies the whole group's cost by 2.4. **29 mana, 1.4 s.
Amethyst Cord** (Split is Tier III).

### 5. The closest one wins: `Burst · Explode · Widen` and `Burst · Widen · Explode`
{: .no_toc }

| Spell | Readout | Mana | Cooldown |
|---|---|---|---|
| `Burst · Explode · Widen` | Everything within 4 blocks: Explode (+50% radius) | 47 | 2.35 s |
| `Burst · Widen · Explode` | Everything within 6 blocks: Explode | 50 | 2.5 s |

Both Burst and Explode have a radius. In the first, Explode is closer, so each creature in the 4-block burst
explodes with a blast 50% wider. In the second, only Burst is on Widen's left, so the burst itself reaches 6
blocks. On a shape, Widen multiplies the whole group, so it costs a little more.

### 6. Extend the field or the fire: `Zone · Fire · Extend` and `Zone · Extend · Fire`
{: .no_toc }

| Spell | Readout | Mana |
|---|---|---|
| `Zone · Fire · Extend` | A field (3 blocks, 6s, every 1s): Fire (2x duration) | 31 |
| `Zone · Extend · Fire` | A field (3 blocks, 12s, every 1s): Fire | 34 |

The first makes what the field sets alight burn twice as long. The second makes the field itself last twice
as long, striking 12 times instead of 6.

### 7. Two groups at once: `Bolt · Fire · Burst · Heal`
{: .no_toc }

> A bolt: Fire<br>
> Everything within 4 blocks: Heal

A fire bolt at your target and a healing burst around you, both at the same moment. **36 mana, 1.8 s.**

### 8. Waiting for the hit: `Bolt · On Hit · Burst · Fire`
{: .no_toc }

> A bolt: nothing yet<br>
> On hit:<br>
> &nbsp;&nbsp;Everything within 4 blocks: Fire

The bolt carries nothing itself; where it lands, a burst of fire goes off. **23 mana, 1.15 s.** Compare
`Bolt · On Hit · Fire` (13 mana), which only sets alight the one creature the bolt hit.

### 9. The classic: `Bolt · Fire · Split · On Hit · Burst · Explode`
{: .no_toc }

> 3 bolts: Fire<br>
> On hit:<br>
> &nbsp;&nbsp;Everything within 4 blocks: Explode

Three fire bolts; wherever each one lands, an explosion goes off around it. **64 mana, about 3.2 s. Amethyst
Cord.** Its cost is worked out step by step [below](#a-worked-price).

### 10. Setting up a reaction: `Bolt · Frost · Delay · Bolt · Fire`
{: .no_toc }

> A bolt: Frost<br>
> After 1s:<br>
> &nbsp;&nbsp;A bolt: Fire

A frost bolt freezes the target; a second later a fire bolt leaves your hands, aimed wherever you're looking
then. Fire on a frozen target sets off **Shatter** (see [Reactions]({{ '/spellcraft/reactions/' | relative_url }})).
**26 mana, 1.3 s.** Add Extend after the Delay (`Bolt · Frost · Delay · Extend · Bolt · Fire`) and the wait
becomes two seconds: Extend changes the link, not the Frost (27 mana).

### 11. Up and down: `Self · Launch · On Land · Burst · Lightning`
{: .no_toc }

> You: Launch<br>
> When you land:<br>
> &nbsp;&nbsp;Everything within 4 blocks: Lightning

Launch on Self rockets you up and forward. The next time you touch the ground (within 10 seconds),
lightning strikes everything within 4 blocks of where you land. **46 mana, 2.3 s. Amethyst Cord.**

### 12. Answering a blow: `Self · Brace · On Hurt · Jolt`
{: .no_toc }

> You: Brace<br>
> When something hurts you:<br>
> &nbsp;&nbsp;The target: Jolt

Brace cuts the damage you take by 80% for 2 seconds; the next thing that hurts you (within 15 seconds) is
stunned by a Jolt. **15 mana, 0.75 s. Copper Cord.**

### 13. A safety net: `Self · Heal · On Low Health · Heal`
{: .no_toc }

> You: Heal<br>
> When your health drops below 30%:<br>
> &nbsp;&nbsp;The target: Heal

A heal now, and another if you fall below 30% health in the next 30 seconds. After On Low Health "the target"
is you. **26 mana, 1.3 s. Amethyst Cord.**

### 14. Two spells in one: `Bolt · Fire · If Sneaking · Bolt · Frost`
{: .no_toc }

> A bolt: Fire<br>
> If you're sneaking:<br>
> &nbsp;&nbsp;A bolt: Frost

A fire bolt every time, and a frost bolt too when you cast while sneaking. **25 mana, 1.25 s.** Put the part
you want only sometimes after the condition.

### 15. The link watches the last group: `Bolt · Fire · Burst · Heal · On Hit · Explode`
{: .no_toc }

> A bolt: Fire<br>
> Everything within 4 blocks: Heal<br>
> On hit:<br>
> &nbsp;&nbsp;The target: Explode

On Hit watches the group right before it, the healing Burst, not the Bolt. To make the bolt explode on hit,
put On Hit straight after the bolt's group: `Burst · Heal · Bolt · Fire · On Hit · Explode`.

### 16. Doubling up: `Bolt · Fire · Echo · Echo`
{: .no_toc }

> A bolt: Fire<br>
> 0.5s later, everything before this fires again.<br>
> 0.5s later, everything before this fires again.

Four fire bolts in all. **54 mana, 2.65 s.** A single Echo (`Bolt · Fire · Echo`) is two bolts for 26 mana.

### 17. Rapid pays for the whole spell
{: .no_toc }

| Spell | Mana | Cooldown |
|---|---|---|
| `Bolt · Fire · On Hit · Burst · Explode` | 47 | 2.35 s |
| `Bolt · Fire · Rapid · On Hit · Burst · Explode` | 66 | 1.65 s |
| `Bolt · Fire · On Hit · Burst · Explode · Rapid` | 66 | 1.65 s |
| `Self · Rapid · Bolt · Fire · On Hit · Burst · Explode` | 66 | 1.65 s |

Rapid halves the cooldown of the **whole** spell, so it multiplies the cost of the whole spell by 1.4: on
the cheap Bolt group, on the Burst, or on an empty Self, it's the same spell at the same price. (Since a
cooldown comes from the cost, the dearer spell's cooldown is halved from a longer one: it ends up about
seven tenths of the plain spell's.)

### 18. Where stacking stops: `Bolt · Fire · Split · Split`
{: .no_toc }

> 9 bolts: Fire

Two Splits make nine bolts for **68 mana**. A third Split still makes nine (that's the most) but multiplies
the cost again, to 164. The readout shows the copies, so it's easy to spot.

## Cost

Every rune has a base cost, and the spell's cost is worked out from them in one go:

- A **group** costs its **shape's** own cost, plus each **effect's** cost times the **shape's multiplier**.
- A **modifier on an effect** multiplies that effect's cost. A **modifier on a shape** multiplies the whole
  group's cost, except **Rapid**, **Vow** and **Blood Price**, which change the whole spell and so multiply
  the **whole spell's** cost, wherever they sit (Rapid by 1.4; Vow and Blood Price by 1, as you pay for them
  in cooldown and in health).
- A **link** adds its own cost (times any modifier on it), plus the whole of the spell after it. **Pulse**
  and **Imbue** count what's after them **three times** over; an **Echo** adds the cost of everything it
  repeats.
- The total is **rounded up** to whole mana at the very end.

### Shapes

| Shape | Cost | Effects cost × | | Shape | Cost | Effects cost × |
|---|---|---|---|---|---|---|
| Self | 0 | 1.0 | | Ring | 6 | 1.5 |
| Touch | 1 | 1.0 | | Pillar | 5 | 1.4 |
| Spark | 1 | 1.0 | | Wave | 6 | 1.5 |
| Ray | 2 | 1.0 | | Mine | 5 | 1.3 |
| Bolt | 3 | 1.1 | | Snare | 4 | 1.4 |
| Arc | 3 | 1.1 | | Cone | 5 | 1.4 |
| Nova | 3 | 1.3 | | Trail | 7 | 1.8 |
| Beam | 4 | 1.2 | | Zone | 8 | 2.0 |
| Wisp | 4 | 1.3 | | Constellation | 8 | 2.4 |
| Crescent | 5 | 1.4 | | Orbit | 9 | 2.0 |
| Lance | 5 | 1.5 | | Orb | 9 | 2.2 |
| Prism | 5 | 1.5 | | Vortex | 9 | 2.2 |
| Ricochet | 5 | 1.5 | | Rain | 10 | 2.5 |
| Barrage | 5 | 1.6 | | Wall | 10 | 2.2 |
| Comet | 5 | 1.6 | | Totem | 10 | 2.4 |
| Sweep | 5 | 1.6 | | Domain | 20 | 3.0 |
| Cluster | 6 | 1.7 | | Stream | 5 | 1.8 |
| Burst | 6 | 1.5 | | Blitz | 6 | 1.5 |

The implicit Self (a spell or segment with no shape) costs nothing and multiplies by 1. Every effect's cost
is on its rune page, and the modifiers' multipliers are in the table [above](#what-the-common-modifiers-do).
Links cost 2, except If Sneaking, If Airborne, If Wounded, If Outnumbered and If Wet (1) and Imbue (3).

### A worked price

`Bolt · Fire · Split · On Hit · Burst · Explode`:

| Part | Sum | Mana |
|---|---|---|
| Bolt and its Fire | 3 + (8 × 1.1) | 11.8 |
| Split on the Bolt, so the whole group | 11.8 × 2.4 | 28.32 |
| On Hit | 2 | 2 |
| Burst and its Explode | 6 + (18 × 1.5) | 33 |
| **Total** | 28.32 + 2 + 33 = 63.32, rounded up | **64** |

And `Bolt · Fire · Amplify` is 3 + (8 × 1.6 × 1.1) = 17.08, so **18** mana.

### What you actually pay

The readout and your spell panel always show your own price, after:

- **Thrift** on your Cord: 7% less per level ([Enchantments]({{ '/progression/enchantments/' | relative_url }})),
- the **Archmage** perk of the 8th Heart Circle: 15% less ([Heart Circles]({{ '/progression/heart-circles/' | relative_url }})),
- **casting gear** in your hands: a staff of the spell's element, or a Focus of Thrift ([Casting Gear]({{ '/gear/' | relative_url }})),
- a **mana storm** overhead: 25% less ([World Events]({{ '/world/world-events/' | relative_url }})),
- and the **server's** own cost setting, if it has one.

A discount always saves at least 1 mana, even where rounding would swallow it. A
[Knot]({{ '/fusion-altar/knots/' | relative_url }}) costs 10% less than the runes inside it (19% for a Knot
inside a Knot), and a higher [rank]({{ '/fusion-altar/ranks/' | relative_url }}) makes an effect stronger at
the same cost.

### Blood Price

With **Blood Price** on any shape, the **whole spell** is paid for in **health** instead of mana: 1 health for
every 5 mana of your price, rounded up, at least 1. It never kills you: if you don't have more health than it
costs, the cast is refused ("Blood Price needs more than 3 health"). `Bolt · Fire · Blood Price` costs 3
health. The readout shows the price in health ("3 health (Blood Price)") and the spell panel as a number
with a small heart, and the health you spend still counts toward
your next Heart Circle (5 mana for every point of health).

## Cooldown

A spell's cooldown comes from its **cost**:

- **one second for every 20 mana** of the spell's plain cost (before your discounts),
- never less than **half a second** and never more than **20 seconds**,
- then **halved for every Rapid** anywhere in the spell, and **four times longer for every Vow** (up to a minute),
- then shortened by **Celerity** (8% per level) and the **Flow** perk of the 5th Heart Circle (15%),
- and never under a **quarter of a second** in the end.

A [secret spell]({{ '/spellcraft/secret-spells/' | relative_url }}) you've found takes half as long again to
recharge (and costs more: see its page).

**Vow strengthens only its own shape's effects**, but lengthens the whole spell's cooldown, so put it on the
shape whose effects you want stronger. On a shape with no effects (an empty Self, or a Bolt that's only
there to set off an On Hit) it's a longer cooldown for nothing, and the readout warns you.

| Spell | Plain cost | Cooldown |
|---|---|---|
| `Bolt · Push` | 7.4 | 0.5 s (the least) |
| `Bolt · Fire` | 11.8 | 0.6 s |
| `Bolt · Fire · Split` | 28.3 | 1.4 s |
| `Bolt · Fire · Split · On Hit · Burst · Explode` | 63.3 | about 3.2 s |
| `Bolt · Fire · Rapid` | 16.5 | 0.45 s |
| `Bolt · Vow · Fire` | 11.8 | 2.4 s (and its fire hits twice as hard) |

Every spell has its **own** cooldown, so while one recharges you can cast another. Cooldowns are kept when
you log out or die. See [Casting]({{ '/spellcraft/casting/' | relative_url }}#cooldowns).

## The limits on a spell

Some things stop growing, so no spell can run away with the world:

| Limit | |
|---|---|
| Runes in a spell | Your Cord's sockets: 3, 5, 8 or 12 |
| Rune tier | Your Cord's: I, II, III or IV |
| Copies from Split | 9 at most (two Splits) |
| Echoes that count | 3 |
| Volley | 9 shots at most |
| Linger | 6 more landings at most (three Lingers) |
| Widen on an effect | 8 times its radius at most (five Widens make it about 7.6 times; a sixth reaches 8) |
| Domain | a radius of 24 blocks at most (other shapes keep growing with every Widen) |
| Barrage | 20 blows at most; Stream 12 strikes at most |
| Quicken on a flying shape | 8 times as fast at most (three Quickens on a bolt); a bolt still flies 48 blocks at most, a wave 14 |
| Links deep | 8 |
| Creatures one cast can touch | 64 (a server can change this). Shapes that strike again and again (Domain, Zone, Totem, Orbit, Wall, Trail, Rain, Barrage, Orb, Stream, Vortex, Snare) get a fresh 64 for every strike. |
| Blocks one cast can change | 32 (a server can change this) |
| Parts of one cast | 128 in all (links, pulses, echoes and repeats together) |
| On Hit from one hit | fires at 8 creatures at most |
| Your spell projectiles in flight | 24 at once |
| Spirits you command | 6 at once (Summon and Shades together) |
| Knots inside Knots | 2 deep |
| Cooldown | from a quarter of a second to a minute |
| Mana cost | anything, but a spell that costs more than your whole pool can only be [overcast]({{ '/spellcraft/overcasting/' | relative_url }}), and only up to twice your pool |
