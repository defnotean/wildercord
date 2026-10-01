# Passive Spells


A **passive spell** is always on. Instead of a cost and a cooldown, it costs a little mana **every second**,
and keeps itself going: a buff that never runs out, or a ring of orbs that guards you. You can have up to
**two**, and each holds **two runes**.


---

## Unlocking passive slots

Passive slots come from your [Heart Circles](../progression/heart-circles.md):

| Slot | Opens with |
|---|---|
| Passive 1 | the **1st** Heart Circle |
| Passive 2 | the **5th** Heart Circle |

When a slot opens you're told "Passive slots open" and where to thread them. A circle cracked by
[overcasting](overcasting.md) gives nothing until it mends, and that includes
its passive slot: crack your only circle and your passive stops until it heals.

## Threading a passive

Open the Cord screen (`K`) and click the **Passives** tab. You thread a passive exactly like a spell (click or
drag runes from the Codex), with a few differences:

- There are two rows. A row that isn't open yet says which circle opens it.
- Each passive holds **two runes**, whatever your Cord. A passive is a lasting buff or a guard, not a whole spell, so
  make the two count: an effect before any shape is on Self already, so `Stoneskin · Empower` needs no Self rune,
  and a modifier such as Amplify takes one of the two sockets (`Swift · Amplify`). Trying to thread a third is refused
  ("Passives hold 2 runes").
- Runes that can never be part of a passive are dimmed with a lock in the Codex, and trying to thread one is
  refused ("Heal can't be a passive").
- Each row has its **upkeep** ("0.7/s") and an **On/Off** switch.
- Your Cord's tier still applies: a Tier III rune needs an Amethyst Cord here too.

![The Passives page: two passive rows, one with Self and Swift at 0.6 mana a second and one with an Orbit aura at 3.4, both switched On, and a line saying passives drain 4.0 mana a second while you regenerate 15.0](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/passives.png)
<span>Two passives running: their upkeep, their switches, and the total drain against your regeneration.</span>

A new passive starts **on** as soon as it holds runes that make a valid passive.

## What a passive can hold

Passives are for things that make sense to keep up all the time: lasting buffs, and guardian auras. They
can't be a machine gun, a free heal every two seconds or an endless death save.

### Shapes

**Self** or **Orbit**, and only one shape. A passive that starts with an effect is on Self, like any spell, so
an Orbit after that effect would be a second shape: `Swift · Orbit · Shock` is refused. Keep the buff and the
aura in two passives (`Self · Swift` and `Orbit · Shock`).

| Shape | What it does as a passive |
|---|---|
| Self | Keeps its buffs on you. Renewed every 2 seconds. |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/orbit.png) Orbit | Three orbs circle you and strike whatever they touch. When they run out (after 8 seconds) a new ring starts. Orbit is Tier III: it needs an Amethyst Cord. |

### Buffs (Self or Orbit)

| Rune | Tier | As a passive |
|---|---|---|
| Feather Fall | I | Never take fall damage |
| Swift | I | Speed III |
| Night Eye | I | Night vision |
| Haste | I | Haste II |
| Tidebreath | I | Water breathing and faster swimming |
| Leap | I | Jump Boost III |
| Anchor | I | No knockback, and 4 more armour |
| Frostward | I | You can't freeze, not even in powder snow |
| Cushion | I | Falls can't hurt you, and a hard landing throws out a gust |
| Regrowth | II | Regeneration II |
| Stoneskin | II | Resistance II |
| Empower | II | Strength II |
| Fireward | II | Fire resistance |
| Overdrive | II | Strength II, Speed II and Haste II, but you lose 1 health every 2 seconds, all the time |
| Searing Edge | II | Every melee hit you land sets the foe alight and deals 2 more fire damage |
| Reflect | III | Attackers take 60% of their damage back |

### Auras (only with Orbit)

Damage and control effects can only be carried by an **Orbit**: its orbs deal them to whatever they touch.

Harm, Shock, Fire, Frost, Chill, Venom, Dismantle, Ripple, Aftershock, Push, Ember, Icicle, Pelt, Windcut and
Umbra.

`Shock` on its own (on Self) is refused with "Shock needs an Orbit to carry it in a passive."

### Modifiers

**Amplify**, **Extend**, **Frugal**, **Widen**, **Focus** and **Quicken**, following the usual rule: each
changes the closest rune on its left that it can change.

### Never in a passive

**Links** of any kind, **Knots**, and any rune not listed above: heals, Shield, Barrier, Brace, Reversal,
Foresight, summons, big area damage, Stasis, and every other shape. Runes from add-on mods can't be passives
either. The Cord screen tells you exactly what's wrong:

| Message | Means |
|---|---|
| Heal can't be sustained as a passive. | That rune isn't allowed in passives. |
| A passive has one shape at most. | Take all but one shape off (an effect before any shape counts as Self). |
| Shock needs an Orbit to carry it in a passive. | Damage and control need an Orbit. |
| Passives hold 2 runes | A passive is full: take one rune off before you thread another. |

## Upkeep

A passive costs **12% of its cost as a spell, every second**. `Self · Swift` would cost 6 mana as a spell, so
as a passive it costs 0.72 mana a second (shown as 0.7/s).

| Passive | As a spell | Upkeep a second |
|---|---|---|
| `Self · Night Eye` | 3 | 0.36 |
| `Self · Swift` | 6 | 0.72 |
| `Self · Feather Fall` | 6 | 0.72 |
| `Self · Regrowth` | 10 | 1.2 |
| `Stoneskin · Empower` | 24 | 2.88 |
| `Orbit · Shock` | 23 | 2.76 |
| `Orbit · Dismantle` | 29 | 3.48 |
| `Self · Anchor` | 4 | 0.48 |

- **Thrift** on your Cord and the **Archmage** perk lower the upkeep, like they lower a spell's cost (and
  so does the server's own cost setting, if it has one).
- Upkeep is taken **once a second**, for every passive that's on, in its open slot, with valid runes.
- **A passive pays its first second as it starts**: when you switch it on, change its runes, or it's cast afresh after
  you change dimension. Without the mana for that first second it doesn't start (it falters, below), so switching a
  passive on and off can't get its buffs for free.
- **In creative**, passives cost nothing.
- Under the passive rows the Cord screen compares your total drain with your regeneration ("Passives drain
  4.0 mana/s · you regenerate 15.0/s"), and turns yellow when the drain is bigger. On your spell panel, the
  drain shows as "-4.0/s" beside your mana when no cooldown or charge is showing, red when your mana can't
  cover the next second.
- Upkeep doesn't **condense** toward your next Heart Circle; only mana spent casting spells does.

### Faltering

If you don't have the mana for a second's upkeep, that passive **falters**: it stops renewing (an Orbit's
orbs vanish) and takes no mana, until you have enough again. Then it picks up by itself. What it already gave
you wears off within 15 seconds (see below).

With two passives running, keep an eye on the drain line: casting a big spell can leave too little for the
next second's upkeep.

## Renewal

- A **Self** passive is cast again every **2 seconds**, so its buffs never run out. Only the first cast shows
  its light and sound; the renewals after that are quiet.
- An **Orbit** passive starts a new ring of orbs whenever the old one runs out (after 8 seconds; Extend on the
  Orbit makes it 16). Switch it off and its orbs vanish at once.
- A passive is cast afresh after you change dimension, or when you change its runes.
- Passives stop renewing when you take your Cord off, and while you're dead.
- **What a passive gives lasts 15 seconds at most**, however long the rune's own buff would be: a little past the
  next renewal, so it never runs out while the passive is on, and wears off soon after you switch it off or it
  falters. (So switching `Self · Night Eye` on for a moment gives 15 seconds of night vision, not a minute.) The same
  buff from a potion or another spell keeps its own full length.

Because a Self passive is renewed every 2 seconds anyway, **Extend on a Self passive only raises its upkeep**.
**Frugal** halves the upkeep, at 40% less strength.

## Switching passives on and off

Click the **On/Off** switch on a passive's row. You're told "Passive 1 on" or "Passive 1 off". A passive that's
off costs nothing and keeps its runes; its readout starts "Passive (off)" and shows what it would cost.

## How passives count

- Passives are cast with your Heart Circles' power and your Cord's Potency and Persistence.
- They don't use a cooldown, don't count toward rhythm or your affinities, and don't condense mana. Your affinities add
  no power to them either.
- Their buffs are the same effects as the runes' own: `Self · Swift` as a passive gives exactly the Speed
  that `Self · Swift` gives as a spell, kept up forever (each renewal lasting 15 seconds at most).

## Passives to try

| Passive | Needs | Why |
|---|---|---|
| `Self · Night Eye` | 1st Circle, any Cord | Never need a torch to see. Almost free. |
| `Self · Swift` | 1st Circle, any Cord | Speed everywhere, for less than a mana a second. |
| `Self · Feather Fall` | 1st Circle, any Cord | Never die to a fall again. |
| `Self · Tidebreath` | 1st Circle, any Cord | Breathe and swim freely under water. |
| `Stoneskin · Empower` | 1st Circle, a Copper Cord | Tougher and stronger in every fight, in one passive. |
| `Orbit · Shock` | an Amethyst Cord | A crackling guard that hits whatever comes close. |
| `Orbit · Dismantle` | an Amethyst Cord | Orbs that cut straight through armour. |
| `Self · Anchor` | 1st Circle, any Cord | Nothing, not a blow, a blast or a spell, moves you. |


**Overdrive** is allowed as a passive, but it keeps costing you 1 health every 2 seconds for as long as it runs.
