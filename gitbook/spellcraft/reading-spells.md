# How a Spell Is Read

## What it is

A spell is the runes on its row, read **from left to right**. The Cord screen's readout always shows the
result, so you never have to guess. This page explains the rules and how mana and cooldown are worked out.

| Family | Colour | Job | Examples |
|---|---|---|---|
| [Shape](../runes/shapes.md) | teal | *Where* the spell goes. Starts a group. | Self, Bolt, Beam, Burst, Zone |
| [Effect](../runes/effects/index.md) | its element's | *What happens* to what the shape hit. | Fire, Heal, Freeze, Blink |
| [Modifier](../runes/modifiers.md) | gold | *Changes* the closest rune on its left that it can change. | Amplify, Extend, Split, Rapid |
| [Link](../runes/links.md) | violet | The rest happens *later*, *somewhere else* or *only if*. | On Hit, Delay, Echo, Pulse |

Every rune, by Codex category, is on the [Rune Codex](../runes/codex.md).

## How to use it

### The four rules

1. **A shape starts a group.** Every effect after it, up to the next shape or link, lands **in order** on
   whatever the shape hits.
2. **A spell that starts with an effect is cast on you**, as if it began with Self. `Heal` is `Self · Heal`.
3. **A modifier changes the closest rune on its left that it can change.** It never reaches back past a link.
4. **A link ends the segment.** Everything after it happens later, somewhere else, or only if something is true.

**Several groups in one segment go off together.** `Bolt · Fire · Burst · Heal` throws a fire bolt and, at the
same moment, heals everything around you. A shape with no effects still flies but does nothing, unless a link
is watching it.

**Stasis** is the one exception to "in order": it always lands first in its group, so every other hit is held
until time moves again.

### After a link, who is "the target"?

| After | Effects with no shape land on | New shapes start from |
|---|---|---|
| Delay, Pulse | You | You, aimed where you look *then* |
| On Hit | Each creature hit (or the block) | The hit, carrying on the same way |
| On Kill | The spot where it died | That spot |
| On Reaction, On Weakness | Each creature that reacted or was hit by its weakness | That creature |
| On Hurt | Whatever hurt you | The attacker |
| On Land, On Low Health | You | Where you are |
| Echo and the conditions | The same as before the link | The same place |

So `Bolt · On Hit · Fire` sets alight the creature the bolt hit, and `Bolt · On Hit · Burst · Fire` bursts into
flame around it.

### Modifiers find their rune

A modifier looks **left**, rune by rune, and attaches to the **closest rune that has what it needs**. A gold
line in the Cord screen shows which.

- **Amplify** needs power. In `Bolt · Fire · Amplify` it finds Fire. In `Bolt · Amplify · Fire` it finds
  nothing and the readout warns you.
- **Split** needs a shape, so `Bolt · Fire · Split` and `Bolt · Split · Fire` are the same spell.
- **Widen** needs a radius. In `Burst · Explode · Widen` it widens the blast; in `Burst · Widen · Explode`, the
  burst.

A modifier on an effect changes that effect. A modifier on a shape changes the whole group and multiplies the
whole group's cost. **Rapid**, **Vow** and **Blood Price** sit on a shape but change the **whole spell**.
Modifiers inside a [Knot](../fusion-altar/knots.md) only change the Knot's own runes.
Modifiers stack and multiply.

| Modifier | Does | Cost × |
|---|---|---|
| Amplify | +50% power | 1.5 |
| Overcharge | +150% power | 2.6 |
| Focus | Half the radius, +50% power (two per rune at most) | 1.2 |
| Extend | Twice as long (three per rune at most) | 1.4 |
| Widen | +50% radius | 1.5 |
| Quicken | Faster projectiles, faster strikes, shorter Delay and Pulse gaps | 1.2 |
| Split | Three copies of the shape | 2.4 |
| Volley | Fires three times, a quarter second apart | 2.4 |
| Pierce | Passes through 3 more targets | 1.3 |
| Bounce | Bounces off blocks 3 more times | 1.3 |
| Chain | Jumps to up to 3 more enemies within 6 blocks | 1.8 |
| Homing | Steers toward the nearest enemy within 12 blocks | 1.4 |
| Linger | The effect lands twice more, a second apart | 1.8 |
| Frugal | Half the mana; weaker and shorter | 0.5 |
| Execute | Double power against targets under half health (one per effect) | 1.3 |
| Trial Key | +60% power against targets at full health (one per effect) | 1.3 |
| Kindred | A helpful effect also lands on you and the nearest ally within 8 blocks, at half power | 1.4 |
| Thirst | You heal for part of the damage dealt | 1.4 |
| Belated | Lands 1.5 seconds late, 25% stronger | 1.25 |
| Rapid | Halves the whole spell's cooldown | 1.4, on the whole spell |
| Vow | That shape's effects hit twice as hard; the whole spell's cooldown is **5 times** longer (one per shape) | 1.0 |
| Blood Price | The whole spell is paid in health | 1.0 |

Extra copies past a limit show a warning and add nothing. Every rune page lists which modifiers work on it.

### Links

| Link | Tier | Mana | The rest of the spell... |
|---|---|---|---|
| Delay | I | 2 | fires 1 second later, from you. Extend doubles the wait, Quicken halves it. |
| On Hit | II | 2 | fires at each creature the shape before it hits. |
| On Land | II | 2 | fires the next time you touch the ground. |
| Pulse | II | 2 | fires three times, a second apart. The part after it costs three times over. |
| On Hurt | II | 2 | fires at whatever next hurts you. |
| If Sneaking | II | 1 | fires only if you're sneaking. |
| If Airborne | II | 1 | fires only if you're in the air. |
| If Wounded | II | 1 | fires only if you're below half health. |
| If Wet | II | 1 | fires only if you're in water or rain. |
| On Weakness | II | 2 | fires at each creature struck with an element it's weak to. |
| Imbue | II | 3 | is stored with 3 charges in an item or block. See [Imbuing](imbuing.md). |
| On Kill | III | 2 | fires where each creature the shape kills died. |
| Echo | III | 2 | carries on, and *everything before it* fires again half a second later. |
| On Low Health | III | 2 | fires when your health drops below 30%. |
| Combo | III | 2 | fires only on every third cast of this spell. |
| If Outnumbered | III | 1 | fires only if 3 or more enemies are within 8 blocks. |
| On Reaction | III | 2 | fires at each creature the shape sets off a [reaction](reactions.md) on. |

- **On Hit, On Kill, On Reaction, On Weakness and Imbue watch the group just before them**, and need a shape
  there.
- **On Hit fires for every hit**: each pierced creature, each split bolt, each Chain jump (at most 8 from one hit).
- **An Echo or Pulse after On Hit or On Kill** is paid once, so it goes off for the first hit only.
- **Conditions check once**, the moment the spell reaches them. Everything before them has already happened.
- **Echoes double.** `Bolt · Fire · Echo · Echo` throws four bolts. Only 3 Echoes count.
- **A cast goes at most 8 links deep.**

### Friendly fire

- **Harmful effects** never touch you, your **party**, your pets or your **team**. Other players are only harmed
  if the server allows PvP, and then rune damage to players is scaled down (to 60% by default).
- **Helpful effects** only touch you and those same allies. To heal a friend with `Burst · Heal`, join a party
  with `/party` or share a team.
- **Movement and world effects** only work where you're allowed to build.

So `Burst · Fire · Heal` burns every enemy around you and heals you and your allies at once.

## Cost and cooldown

A spell's cost is worked out from its runes in one go, then **rounded up**:

- A **group** costs its shape's cost, plus each effect's cost times the **shape's multiplier**.
- A modifier on an effect multiplies that effect's cost. A modifier on a shape multiplies the whole group.
  Rapid, Vow and Blood Price multiply the whole spell.
- A **link** adds its own cost plus everything after it. Pulse and Imbue count what's after them three times.
  An Echo adds the cost of everything it repeats.

### Shapes

| Shape | Cost | Effects × | | Shape | Cost | Effects × |
|---|---|---|---|---|---|---|
| Self | 0 | 1.0 | | Ring | 6 | 1.7 |
| Touch | 1 | 1.0 | | Pillar | 5 | 1.4 |
| Spark | 1 | 1.0 | | Wave | 6 | 1.5 |
| Ray | 2 | 1.15 | | Mine | 5 | 1.3 |
| Bolt | 3 | 1.1 | | Snare | 4 | 1.4 |
| Arc | 3 | 1.25 | | Cone | 5 | 1.4 |
| Beam | 3 | 1.2 | | Trail | 7 | 2.85 |
| Nova | 3 | 1.3 | | Zone | 8 | 2.75 |
| Imprint | 3 | 1.3 | | Constellation | 8 | 2.4 |
| Wisp | 4 | 1.3 | | Orbit | 9 | 3.05 |
| Crescent | 5 | 1.4 | | Orb | 9 | 2.0 |
| Lance | 5 | 1.5 | | Vortex | 9 | 2.8 |
| Prism | 5 | 1.5 | | Rain | 10 | 2.5 |
| Ricochet | 5 | 1.5 | | Wall | 10 | 2.95 |
| Comet | 5 | 1.6 | | Totem | 10 | 3.15 |
| Sweep | 5 | 1.8 | | Domain | 20 | 3.9 |
| Barrage | 5 | 1.85 | | Burst | 6 | 1.5 |
| Stream | 5 | 1.95 | | Blitz | 6 | 1.5 |
| Glaive | 5 | 1.8 | | Cluster | 6 | 1.5 |
| Latch | 6 | 2.15 | | | | |

Each effect's cost is on its rune page.

### A worked price

`Bolt · Fire · Split · On Hit · Burst · Explode`:

| Part | Sum | Mana |
|---|---|---|
| Bolt and its Fire | 3 + (8 × 1.1) | 11.8 |
| Split on the Bolt | 11.8 × 2.4 | 28.32 |
| On Hit | 2 | 2 |
| Burst and its Explode | 6 + (18 × 1.5) | 33 |
| **Total** | 63.32, rounded up | **64** |

### What you actually pay

The readout always shows your own price after **Thrift** (7% less per level), the **Archmage** perk of the 8th
Heart Circle (15% less), [casting gear](../gear.md), a **mana storm** overhead (25% less), an
[affinity](../progression/affinity.md) at level V, and the server's cost setting. A
[Knot](../fusion-altar/knots.md) costs 10% less than the runes inside it.

**Blood Price** pays the whole spell in **health**: 1 health per 4 mana, rounded up, at least 1. It never kills
you: if you don't have enough health, the cast is refused. `Bolt · Fire · Blood Price` costs 3 health.

### Cooldown

- **One second for every 20 mana** of the plain cost, from **half a second** to **20 seconds**.
- **Halved** for every Rapid, **five times longer** for every Vow.
- Then shortened by **Celerity** (8% per level) and the **Flow** perk of the 5th Heart Circle (15%).
- In the end, from a **quarter of a second** to **a minute**.

A [secret spell](secret-spells.md) you've found takes 1.5 times as long.
[Lesson spells](relay-circle.md) use their own fixed rest instead. Every spell
has its own cooldown, kept when you log out or die.

## Worked examples

Plain numbers, before your own discounts.

| Spell | What it does | Mana | Cooldown |
|---|---|---|---|
| `Heal` | Heals you 8 health | 12 | 0.6 s |
| `Bolt · Push` | A bolt that knocks back | 8 | 0.5 s |
| `Bolt · Fire` | Fire bolt: 5 fire damage, burns 6 s | 12 | 0.6 s |
| `Bolt · Fire · Amplify` | Fire bolt, +50% power | 17 | 0.8 s |
| `Bolt · Fire · Split` | Three fire bolts in a fan | 29 | 1.4 s |
| `Burst · Explode · Widen` | Each creature in a 4-block burst explodes, blast 50% wider | 47 | 2.35 s |
| `Burst · Widen · Explode` | A 6-block burst of explosions | 50 | 2.5 s |
| `Zone · Fire · Extend` | A field; what it sets alight burns twice as long | 39 | 1.95 s |
| `Zone · Extend · Fire` | A field that lasts twice as long | 42 | 2.1 s |
| `Bolt · Fire · Burst · Heal` | A fire bolt and a healing burst at once | 36 | 1.8 s |
| `Bolt · On Hit · Burst · Fire` | A burst of fire where the bolt lands | 23 | 1.15 s |
| `Bolt · Fire · Split · On Hit · Burst · Explode` | Three fire bolts that each explode | 64 | about 3.2 s |
| `Bolt · Frost · Delay · Bolt · Fire` | Frost, then a fire bolt a second later for **Shatter** | 26 | 1.3 s |
| `Self · Launch · On Land · Burst · Lightning` | Rocket up; lightning where you land | 46 | 2.3 s |
| `Self · Brace · On Hurt · Jolt` | Brace, then stun whatever hits you next | 15 | 0.75 s |
| `Bolt · Fire · If Sneaking · Bolt · Frost` | Fire bolt, plus a frost bolt when sneaking | 25 | 1.25 s |
| `Bolt · Fire · Echo · Echo` | Four fire bolts | 54 | 2.65 s |
| `Bolt · Fire · On Hit · Burst · Explode · Rapid` | Exploding fire bolt, half cooldown | 66 | 1.65 s |
| `Bolt · Fire · Split · Split` | Nine fire bolts | 68 | 3.4 s |
| `Bolt · Vow · Fire` | Fire bolt that hits twice as hard | 12 | 3 s |
| `Self · Heal · Kindred` | Heals you 8 and the nearest ally 4 | 17 | 0.85 s |

## Tips and counterplay

- **Put On Hit right after the group you want watched.** In `Bolt · Fire · Burst · Heal · On Hit · Explode`, On
  Hit watches the Burst, not the Bolt.
- **After On Kill, use an area.** The creature is gone, so `On Kill · Burst · Explode` works and `On Kill ·
  Explode` has nothing to hit.
- **Put Vow on the shape whose effects you want stronger.** On an empty shape it's a longer cooldown for nothing.
- **A third Split adds nothing.** Two Splits already make 9 copies, the most allowed, so a third only raises the cost.

### Limits

| Limit | |
|---|---|
| Runes in a spell | Your Cord's sockets: 3, 5, 8 or 12 |
| Copies from Split | 9 |
| Echoes that count | 3 |
| Volley | 9 shots |
| Linger | 6 more landings |
| Widen on an effect | 8 times its radius |
| Domain radius | 24 blocks |
| Quicken | 8 times as fast |
| Links deep | 8 |
| Creatures one cast can touch | 64 (servers can change this) |
| Blocks one cast can change | 32 (servers can change this) |
| Parts of one cast | 128 |
| Your spell projectiles in flight | 24 |
| Spirits you command | 6 |
| Knots inside Knots | 2 deep |
| Mana cost | anything, but above your pool you must [overcast](overcasting.md) |
