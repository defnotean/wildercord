# Passive Spells

## What it is

A **passive spell** is always on. Instead of a cost and a cooldown, it takes a little mana **every second**: a
buff that never runs out, or a ring of orbs that guards you. You can have up to **two**, and each holds **two
runes**.

## How to get it

Passive slots come from your [Heart Circles](../progression/heart-circles.md):

| Slot | Opens with |
|---|---|
| Passive 1 | the **1st** Heart Circle |
| Passive 2 | the **5th** Heart Circle |

A circle cracked by [overcasting](overcasting.md) loses its slot until it mends.

## How to use it

Open the [Cord screen](cord-screen.md) (`K`) and click the **Passives** tab.
Thread runes just like a spell. Each row shows its **upkeep** ("0.7/s") and an **On/Off** switch. A new
passive turns on as soon as its runes make a valid passive. Your Cord's tier still applies.

![The Passives page: two passive rows with their upkeep and On switches, and a line comparing total drain with your regeneration](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/passives.png)

### What a passive can hold

- **Shape:** Self or Orbit, and only one. An effect with no shape before it is already on Self, so
  `Stoneskin · Empower` works, but `Swift · Orbit · Shock` has two shapes and is refused.
- **Buffs** (Self or Orbit): Feather Fall, Swift, Night Eye, Haste, Tidebreath, Leap, Anchor, Frostward,
  Cushion, Regrowth, Stoneskin, Empower, Fireward, Overdrive, Searing Edge and Reflect.
- **Auras** (Orbit only): Harm, Shock, Fire, Frost, Chill, Venom, Dismantle, Ripple, Aftershock, Push, Ember,
  Icicle, Pelt, Windcut and Umbra. Orbit's three orbs hit whatever they touch, and a new ring starts every
  8 seconds. Orbit needs an Amethyst Cord.
- **Modifiers:** Amplify, Extend, Frugal, Widen, Focus and Quicken.
- **Never:** links, Knots, heals, Shield and every other rune. Locked runes are dimmed in the Codex, and the
  Cord screen says what's wrong.

### Upkeep

A passive costs **12% of its spell cost every second**. `Self · Swift` costs 6 mana as a spell, so it costs
about 0.7 mana a second as a passive.

- Thrift on your Cord and the Archmage perk lower it. In Creative it's free.
- A passive pays its first second when it starts, so flicking it on and off gives nothing for free.
- Upkeep doesn't count toward your next Heart Circle.
- Under the rows, the Cord screen compares your total drain with your regeneration. It turns yellow when
  you drain more than you regain.

If you can't pay a second's upkeep, the passive **falters**: it stops renewing and takes no mana until you can
pay again.

### Renewal

- A Self passive recasts every **2 seconds**, quietly, so its buffs never run out.
- Whatever a passive gives lasts **15 seconds at most**, so it wears off soon after you switch it off or it
  falters.
- Passives stop while you're dead or not wearing your Cord.
- They don't use a cooldown, don't count for rhythm, and don't gain power from your affinities.

## Tips and counterplay

| Passive | Needs | Why |
|---|---|---|
| `Self · Night Eye` | 1st Circle | See in the dark, almost free. |
| `Self · Swift` | 1st Circle | Speed everywhere for under a mana a second. |
| `Self · Feather Fall` | 1st Circle | Never die to a fall. |
| `Stoneskin · Empower` | Copper Cord | Tougher and stronger in one passive. |
| `Orbit · Shock` | Amethyst Cord | A crackling guard that hits whatever comes close. |

- **Extend on a Self passive only raises its upkeep**, since it renews every 2 seconds anyway.
- **Frugal** cuts the upkeep but weakens the passive.
- With two passives on, watch the drain: a big spell can leave too little for the next second.


**Overdrive** works as a passive, but it keeps costing you 1 health every 2 seconds while it runs.
