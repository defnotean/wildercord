---
title: Shields and Parrying
parent: Spellcraft
nav_order: 7
---

# Shields and parrying
{: .no_toc }

<img src="{{ '/assets/images/shield-block.jpg' | relative_url }}" alt="A stack of amber magic circles stands in front of a husk and a spell bursts against the front one" class="shot">
<span class="caption">A spell rings off a Shield's circles and stops.</span>

A **Shield** is a one-time block against magic. It sits on you, invisible, until a harmful spell comes
at you. Then its magic circles spring up between you and the spell. A light spell is stopped dead; a
heavier one smashes through. Raise it at the very last moment and you **parry** instead: the spell is
turned back on whoever cast it.

A Shield isn't your only defence: your armour counts against spells, the Warding enchantment and the Potion of
Warding take more off, and your spellguard stops one spell killing you from high health. See
[Defending Against Magic]({{ '/progression/defence/' | relative_url }}).

1. TOC
{:toc}

## The Shield rune

<img src="{{ '/assets/runes/shield.png' | relative_url }}" alt="" width="32" height="32" class="rune-icon"> **Shield** is a Tier II Earth effect that costs 12 mana and needs a
Copper Cord or better. Craft it from a Blank Rune and a Shield, plus 2 Lapis Lazuli and a Gold Ingot
(see [Rune Recipes]({{ '/items/rune-recipes/' | relative_url }})). It also turns up in shipwreck
treasure, buried treasure and trial chamber vaults.

| | |
|---|---|
| **Who it guards** | Only you and your allies: it's a helpful effect. `Self · Shield` guards you. A shape that reaches your friends, such as `Burst · Shield` or `Nova · Shield`, guards everyone it touches, you included. |
| **How long it lasts** | 30 seconds. Each Extend doubles it (60 s, 120 s...). Frugal cuts it to 60%. Persistence on your Cord makes it 20% longer per level. |
| **How strong it is** | The mana the whole spell that raised it is worth. See [Strength](#strength) below. |
| **How many at once** | One per creature. Raising another while one is up keeps whichever is **stronger** and starts the timer again. |
| **Modifiers that work on it** | Amplify, Extend, Frugal, Overcharge, Execute, Trial Key, Kindled, Unstable |

When it goes up, its magic circles open in front of you for a moment, one behind another, then fade
away. From then on nobody can see it until a spell comes. You're told *"Shield up: it stops any spell
of N mana or less"*, and the HUD shows **Shield N · Ns** (its strength and seconds left) above your
spell panel for as long as it holds.

A Shield can't be a [passive]({{ '/spellcraft/passives/' | relative_url }}).

## Strength

A Shield's strength is the **mana the spell that raised it is worth**, and every spell that meets it is
weighed the same way. The weight is the spell's full price before any discount, so the whole spell
counts: every modifier on it, even runes that have nothing to do with the Shield. Thrift, the Archmage
perk, a staff, a mana storm and the server's own mana rules never make your Shield weaker, and they
never make your spells lighter against someone else's.

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

Some spells for comparison: `Bolt · Harm` and `Bolt · Fire` weigh 11.8, so a plain `Self · Shield`
stops them. `Bolt · Fire · Amplify` weighs 16.2, and breaks it.

**Secret spells** weigh more than their runes: their full secret price, even on the cast that finds one (see
[Secret Spells]({{ '/spellcraft/secret-spells/' | relative_url }})). The Archivist's great secret spell
weighs 400: it breaks any Shield unless it's parried.

## The circles

A Shield draws **one circle for every 8 mana** of strength, from 1 up to **7**, stacked one behind
another a short way out from whoever it guards. Each circle is the spell that raised it, written out
like any other [magic circle]({{ '/spellcraft/magic-circles/' | relative_url }}), in warm amber light,
sized to cover the creature behind it.

- **A flying spell** (a bolt, a spark, a comet) makes the circles appear once it's about 7 blocks away
  and on course. It strikes the front circle, not the creature behind.
- **A spell that lands at once** (a beam, a blast, a Zone's pulse) makes the circles snap open as it
  lands.
- **A spell that would miss** passes by, and the Shield stays up.

## Stopped or broken

The first touch of a harmful spell decides everything. Either way, **the Shield is spent**: it's a
one-time block.

| The spell weighs... | What happens |
|---|---|
| **as much as the Shield or less** | **Stopped.** It still shatters as many circles from the front as its weight pays for (one circle's worth of mana each, never the last one). The next circle holds: light flares at its heart, ripples race out across it and sparks glance off. The rest of that spell is stopped at that creature too: its other effects, a Zone's later pulses, anything a Link sets off, and a piercing bolt stops there. Other creatures nearby can still be hit. You see *"Your Shield stopped a spell"*. |
| **more than the Shield** | **Broken.** Every circle shatters, front to back, like glass: cracks shoot out from the heart, then the rim breaks into curved slivers and the rest into shards that fly on the way the spell was going, tumble and glint as they fall. The spell goes through and hits as if the Shield had never been there. You see *"Your Shield shattered!"* and your view jolts. |

<img src="{{ '/assets/images/shield-shatter.jpg' | relative_url }}" alt="A heavier spell smashes through a stack of amber circles and they burst into shards" class="shot">
<span class="caption">A costlier spell shatters every circle and goes on through.</span>

Worked example: `Burst · Shield` (strength 24, three circles of 8) meets `Bolt · Fire` (11.8). One
circle shatters, the second holds, the bolt stops. If `Bolt · Fire · Amplify` (17.1) comes instead,
two circles shatter and the third holds. Anything weighing more than 24 breaks all three and goes
through.

### What a Shield meets

- **Spells cast at you** by anyone you could be hurt by: other players (when PvP is on), Runebound,
  the Archivist, dungeon bosses.
- Any spell that **harms or moves** you: damage, Push, Pull, Launch and the like, Stasis too.
- Damage that **lingers** after a spell landed (a burn or bleed ticking on, a cloud or zone pulsing,
  a clock that strikes again later) meets it like any other spell: stopped or breaking through by
  weight.

### What a Shield ignores

- Anything that isn't a spell: arrows, swords, fists, falls, explosions, lava. It passes as if the
  Shield weren't there, and the Shield stays up.
- Ordinary burning once a fire spell has set you alight: the flames themselves aren't a spell.
- Your own spells, and your allies' spells.
- Helpful spells cast on you.

### Other Shields

- The wild magic surge **Ward** gives you a 16-strength Shield for 10 seconds (see
  [Overcasting and Wild Magic]({{ '/spellcraft/overcasting/' | relative_url }})).
- The Star-Eater in the [Astral Observatory]({{ '/world/astral-observatory/' | relative_url }}) hides
  behind a shield of shards that throws spells back. A Shield of your own also turns its shards back
  at it.

## Parrying

<img src="{{ '/assets/images/b-parry-reflect.jpg' | relative_url }}" alt="Gold magic circles flare in the grass as a turned bolt strikes the husk that cast it" class="shot">
<span class="caption">A parried bolt comes back at the husk that cast it.</span>

Raise a Shield **at the last moment** and it doesn't just stop a spell: it **parries** it.

### The timing

A Shield parries a spell when **either** of these is true:

1. **It went up no more than about a third of a second before the spell reached it.**
2. **The spell was already flying at you, and close, when the Shield went up.** "Close" means inside
   the range where the Shield's circles would show, about 7 blocks. Such a spell is parried when it
   arrives, however long it takes to get there (up to 5 seconds).

The second rule means you can wait until you *see* a bolt coming, then raise your Shield.

- It works for a Shield you raise on an ally too, with `Burst · Shield` or `Nova · Shield`: time it
  to the spell coming at them.
- **Only a player parries.** A Shield on your pet (a wolf caught in your `Burst · Shield`, say) blocks and
  shatters like any Shield, but never parries.
- It needs a Shield raised **by a player's cast**. A Shield from a wild surge, a command or a monster
  never parries.
- **Weight doesn't matter.** A parry turns any spell, even one that would shatter the Shield, the
  Archivist's included. The Shield is spent either way.

### What a parry does

**Flying spells turn round.** Bolt, Arc, Spark, Wisp, Comet, Cluster and a Cluster's shards turn back
where they meet the circle and fly at whoever cast them, **25% faster**, steering after them. A lobbed
Arc comes back straight. The turned spell is **yours** now: it hits the original caster with its own
effects and power, and they can parry it back in turn.

**Everything else is negated and answered.** A beam, a blast, a Zone's pulse, anything that lands at
once, is stopped at you, and a **counter-burst** of your Shield's light lances back at its caster, as
long as they're alive, in the same world and within **32 blocks**, and you're allowed to hurt them.

| The parried spell weighed | Counter-burst damage |
|---|---|
| 10 | 4 |
| 20 | 6 |
| 30 | 8 |
| 40 | 10 |
| 50 or more | 12 (the most it can do) |

It deals 2, plus 0.2 for every mana the parried spell weighed, at most 12, then multiplied by the
parried caster's own power. Between players, the server's PvP damage scale applies too. The
counter-burst is a spell like any other, so a Shield on the caster meets it.

<img src="{{ '/assets/images/parry.jpg' | relative_url }}" alt="Gold circles in the grass send a lance of light back into a husk" class="shot">
<span class="caption">A counter-burst answers a spell that can't be turned.</span>

### How a parry feels

The circles hold and flush **gold**, rings of light race out across them, a bright chime rings and
your view kicks. **"Parried!"** shows above your hotbar, and the caster is told *"Your spell was
parried!"*. Your first parry earns the **Parry** feat and advancement.

<img src="{{ '/assets/images/b-parried.jpg' | relative_url }}" alt="Seen from behind a player, gold rings spread from their circles and Parried! shows above the hotbar" class="shot">

### What can't be parried

- **Damage that lingers** after a spell landed: a burn or bleed ticking on, a zone or cloud pulsing,
  burning or freezing ground (Magma's included), a clock striking again. A Shield timed to one of those
  later hits doesn't turn it; it meets the Shield like any spell, blocked or breaking through by weight.
- **Anything that isn't a spell**: arrows, blades and falls pass straight through a Shield.
- **Your own spells.**

Parrying works against Runebound, the Archivist, dungeon bosses and other players alike.

## Feats and advancements

| Feat / advancement | How |
|---|---|
| **Spellguard** | Your Shield stops a spell cast at you (a parry doesn't count). |
| **Shieldbreaker** | One of your spells shatters someone's Shield. |
| **Parry** | Raise a Shield at the last moment and turn a spell back. |

Each is worth 250 mana toward your next [Heart Circle]({{ '/progression/heart-circles/' | relative_url }})
the first time, and each has an advancement worth 20 to 25 experience (see
[Advancements]({{ '/progression/advancements/' | relative_url }})).

## Tips

- **Watch for the circle.** A Runebound's spell circle opens in its hand for over a second before it
  casts. That's your cue: raise the Shield as the spell leaves, or as soon as you see a bolt within a
  few blocks.
- **Keep a cheap Shield on a spare spell.** `Self · Shield` costs 12 mana and parries anything if it's
  timed right. Weight only matters when you *block*.
- **Heavier is safer, not better at parrying.** If you can't time it, `Self · Shield · Amplify ·
  Amplify` or `Self · Shield · Overcharge` stops much more by weight.
- **Extend for a long walk.** `Self · Shield · Extend` lasts a full minute.
- **Guard your friends** with `Burst · Shield` before a fight, then time a `Nova · Shield` for the big
  hit.
- **Break an enemy's Shield** with one cheap spell first, then follow with the real one: a Shield
  stops only one spell.
