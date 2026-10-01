# The Training Dummy


![A straw training dummy stands in a magic circle, a pink number 8.3 floating above it and its name reading DPS 16.5 and a running total](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/training-dummy.jpg)
<span>Every hit floats up as a number; the nameplate keeps score.</span>

A straw **Training Dummy** to try your spells on. It never dies, it stands firm, and it shows you
exactly what every hit did.


## Crafting

![Crafting grid: top row empty · White Wool · empty; middle row Stick · Hay Bale · Stick; bottom row empty · Smooth Stone Slab · empty](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/training_dummy.png)

A White Wool, 2 Sticks, a Hay Bale and a Smooth Stone Slab make one. Dummies stack to 16.

## Placing and picking up

- **Place it** by using it on the top or side of a block. It stands on the block beside the face you
  clicked, turned to face you. It needs room to stand (about 2 blocks tall).
- **Pick it up** by **sneaking and punching it**. It drops back as an item (in Creative it simply goes).
  A spell you cast while sneaking still hits it, so you can test **If Sneaking** spells on it.

## What it shows

### Every hit

Every hit pops a **number** out of the dummy, which drifts up and fades after a second. Numbers under
10 show one decimal (8.3); bigger ones are rounded (24). The bigger the hit, the bigger the number.
Numbers show through walls, so you can read them from anywhere nearby.

The colour says what kind of damage it was:

| Colour | Damage |
|---|---|
| Orange | Fire |
| Pale blue | Freezing |
| Yellow | Lightning |
| Amber | Explosions |
| Pink | Magic |
| White | Anything else (blades, arrows, fists...) |

A reaction's extra damage shows in the number: Shatter a frozen dummy and you'll see the fire hit jump
by 60%.

### The nameplate

While you're hitting it, its name reads **DPS 16.5 · 170 total**:

- **DPS** is the damage per second over the **last 5 seconds** of the current burst (over the time
  since your first hit, if that's shorter, but never less than a second).
- **Total** is everything dealt since the burst began.

A burst ends when **3 seconds** pass with no hits: the next hit starts counting from zero. After **4
seconds** of quiet the nameplate disappears.

## How it behaves

- **It never dies.** It has 1,000 health and heals back to full after every hit.
- **It stands firm.** Knockback and pushes don't shift it (Push, Pull, Launch and the like), and
  walking into it doesn't shove it.
- **It takes damage like any creature.** Your spells, blades and arrows hit it. It wears no armour.
  Marks and reactions work on it: freeze it and burn it, wet it and shock it.
- **It isn't a monster.** Slaying doesn't come into it, so it never counts toward Heart Circle
  breakthroughs, feats that need a kill, or the Runesmith's [contracts](../social/contracts.md).

## Uses

- **Compare spells.** Cast one spell for five seconds, read the DPS, then try another.
- **Test modifiers.** See what Amplify, Split or Focus really add.
- **Learn reactions.** Try `Bolt · Chill · Ember` and watch for the Shatter. See
  [Element Reactions](../spellcraft/reactions.md).
- **Check your gear and perks.** Hit it with and without a staff, at full mana (Overflow) and not, with
  a charged cast and a tap, on the beat and off.
- **Practise combos**: several dummies in a row make a fine crowd for Chain, Burst and Wildfire.
