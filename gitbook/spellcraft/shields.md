# Shields and Parrying

## What it is

![A stack of amber magic circles stands in front of a husk and a spell bursts against the front one](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/shield-block.jpg)

A **Shield** is a one-time block against magic. It sits on you, unseen, until a harmful spell comes at you.
Then its magic circles spring up in the way. A light spell is stopped; a heavier one smashes through. Raise it
at the very last moment and you **parry**: the spell is turned back on its caster.

Armour, the Warding enchantment and your spellguard also help against spells. See
[Defending Against Magic](../progression/defence.md).

## How to get it

![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/shield.png) **Shield**
is a Tier II Earth effect that costs 12 mana and needs a Copper Cord or better. Craft it from a Blank Rune, a
Shield, 2 Lapis Lazuli and a Gold Ingot. See [Rune Recipes](../items/rune-recipes.md).

- **Who it guards:** you and your allies. `Self · Shield` guards you; `Burst · Shield` or `Nova · Shield`
  guards everyone it touches.
- **How long:** 30 seconds. Extend doubles it; Frugal shortens it; Persistence on your Cord lengthens it.
- **One at a time.** Raising another keeps the stronger one and restarts the timer.
- The spell panel shows **Shield N · Ns**: its strength and seconds left.
- A Shield can't be a [passive](passives.md).

## How to use it

### Strength

A Shield's strength is the **mana the whole spell that raised it is worth**, before any discount. Every spell
that meets it is weighed the same way. Discounts like Thrift never make your Shield weaker, or your spells
lighter.

| Spell | Strength | Circles | Lasts |
|---|---|---|---|
| `Self · Shield · Frugal` | 6 | 1 | 18 s |
| `Self · Shield` | 12 | 2 | 30 s |
| `Self · Shield · Extend` | 16.8 | 3 | 60 s |
| `Self · Shield · Amplify` | 18 | 3 | 30 s |
| `Nova · Shield` | 18.6 | 3 | 30 s |
| `Burst · Shield` | 24 | 3 | 30 s |
| `Self · Shield · Amplify · Amplify` | 27 | 4 | 30 s |
| `Self · Shield · Overcharge` | 31.2 | 4 | 30 s |

For comparison, `Bolt · Fire` and `Bolt · Harm` weigh 11.8, so a plain `Self · Shield` stops them.
`Bolt · Fire · Amplify` weighs 16.2 and breaks it.

A Shield draws **one circle per 8 strength**, up to 7.

### Stopped or broken

The first harmful spell that touches it decides. Either way **the Shield is spent**.

| The spell weighs... | What happens |
|---|---|
| **the same or less** | **Stopped.** It cracks some front circles, the next one holds, and the rest of that spell stops at you. *"Your Shield stopped a spell"*. |
| **more** | **Broken.** Every circle shatters and the spell hits as if the Shield wasn't there. *"Your Shield shattered!"* |

![A heavier spell smashes through a stack of amber circles and they burst into shards](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/shield-shatter.jpg)

- **It meets** any spell that hurts or moves you, from players (with PvP on), Runebound, the Archivist and
  bosses, and damage that lingers after a spell, such as a burn or a zone's pulse.
- **It ignores** anything that isn't a spell (arrows, swords, falls, lava), ordinary burning, your own
  spells, your allies' spells and helpful spells.
- **Secret spells** weigh their full secret price. See [Secret Spells](secret-spells.md).

### Parrying

![Gold magic circles flare in the grass as a turned bolt strikes the husk that cast it](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/b-parry-reflect.jpg)

A Shield **parries** when either:

1. it went up no more than about **a third of a second** before the spell hit, or
2. the spell was already flying at you, within about **7 blocks**, when you raised it.

So you can wait until you *see* a bolt coming, then raise your Shield. **Weight doesn't matter** for a parry:
it turns any spell, even one that would break the Shield.

- **Flying spells turn round** and fly back at their caster **25% faster**. The turned spell is yours now,
  and they can parry it back.
- **Everything else is stopped**, and a **counter-burst** lances back at the caster if they're within
  **32 blocks**. It deals 2, plus 0.2 per mana the spell weighed, up to 12.
- **"Parried!"** shows above your hotbar, and the caster is told.

![Gold circles in the grass send a lance of light back into a husk](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/parry.jpg)

Parrying needs a player's Shield. A Shield on your pet, or from wild magic, only blocks. Lingering damage
(a burn ticking, a zone pulsing) can't be parried.

## Tips and counterplay

- **Watch for the circle.** A Runebound holds its spell circle in its hand before it casts. Raise your
  Shield as the spell leaves.
- **Keep a cheap Shield ready.** `Self · Shield` parries anything if your timing is right.
- **Can't time it?** Go heavier: `Self · Shield · Overcharge` blocks far more by weight.
- **Guard friends** with `Burst · Shield` before a fight.
- **Break an enemy's Shield** with one cheap spell, then send the real one. A Shield stops only one spell.
- The feats **Spellguard** (block a spell), **Shieldbreaker** (shatter a Shield) and **Parry** each give
  250 mana toward your next [Heart Circle](../progression/heart-circles.md).
