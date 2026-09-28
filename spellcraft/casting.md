---
title: Casting
parent: Spellcraft
nav_order: 4
description: "Tapping and charging, the reticle, cooldowns, mana and regeneration, rhythm, leaning, switching spells with V, the spell wheel and direct keys, and the HUD."
---

# Casting
{: .no_toc }

You've threaded a spell. This page is about firing it: tapping or charging, what the game checks when you
press the key, how cooldowns and mana work, the small skills that make a caster stronger (rhythm and
leaning), switching between spells, and reading the spell panel beside your hotbar.

1. TOC
{:toc}

---

## Pressing the cast key

**`R`** casts your **selected** spell. Every key can be changed in the game's Controls options, under
**Wildercord** (see [Controls]({{ '/controls/' | relative_url }})).

When you cast, the game checks, in order:

| Check | If it fails, you see |
|---|---|
| You're wearing a Cord | Wear a Cord first: the slot above your offhand (E) |
| Your Cord has this spell | Spell 2 needs a Copper Cord |
| The spell isn't cooling down | Recharging... 1.2s |
| The spell has runes that can fire | Spell 1 is empty. Press K to thread runes |
| You can pay for it | Not enough mana (12/29), or for a Blood Price spell, Blood Price needs more than 3 health |

Nothing is spent when a cast fails. If you're short of mana but have a Heart Circle, the message offers to
**overcast** instead: press again within two seconds and a circle cracks to pay for it (see
[Overcasting and wild magic]({{ '/spellcraft/overcasting/' | relative_url }})).

When a spell goes off, a copy of its magic circle opens on the ground under you, you move with the spell's
shape (a thrust for a bolt, arms flung up for a zone or a burst, a sweep for a crescent), the beads on your
wrist flare, and everyone nearby hears it. Magic can't be cast while you're dead or spectating, and a cast
key held while a screen is open (chat, the pause menu) simply waits, like a drawn bow.

In **creative** mode spells cost no mana and passives no upkeep, but cooldowns still apply.

## Tap or charge

<img src="{{ '/assets/images/charge.jpg' | relative_url }}" alt="A player with raised hands charging a spell, its magic circle open in front of them" class="shot">

**Tap `R`** and the spell goes off at once, at its normal power.

**Hold `R`** and after a quarter of a second you start to **charge**:

- You raise both hands and the spell's **magic circle** opens in front of them as the charge builds: the
  frame first, then its script and star drawing themselves, then a roundel for each rune in turn. Everyone
  around you sees it. In your own first-person view it's a small seal low on the right, so it never blocks
  your aim.
- A **full charge** takes a second and a half. The circle flares and a chime sounds, and your spell panel
  shows the charge climbing ("40%", "80%") until it blinks **FULL**.
- The beads on your wrist burn brighter, and a hum rises as the charge builds.
- You **walk 40% slower** while you charge.
- **Let go** to cast. The more charge, the more power, up to **+40%** at a full charge:

| Charge | Extra power |
|---|---|
| A tap | none |
| A quarter | +10% |
| Half | +20% |
| Three quarters | +30% |
| Full | +40% |

A release from a fifth of a charge or more leaves your hands with a rush of air and a small kick of the
view. Mana is only spent when you let go, so starting a charge costs nothing.

Some things to know:

- A charge **can't start while the spell is cooling down**. Hold the key anyway and letting go just tells you
  it's recharging.
- Hold a charge longer than **12 seconds** and it **fizzles** ("The charge fizzles"): letting go then does
  nothing, and you can start again.
- Taking your Cord off, dying or spectating ends a charge.
- A **Focus of Haste** in your offhand fills a charge 40% faster (see [Casting Gear]({{ '/gear/' | relative_url }})).
- The direct "Cast spell 1 to 5" keys only tap: they can't charge.
- Releasing a fully charged spell for the first time earns the feat **Full Charge** (and its advancement),
  which condenses mana toward your next [Heart Circle]({{ '/progression/heart-circles/' | relative_url }}).

### The reticle

While you charge, **you** (and only you) see where the spell will go, for the spell's first shape:

| Shape | What you see |
|---|---|
| Zone, Rain, Pillar, Totem, Mine | A ring on the ground where you're looking, as wide as the spell |
| Burst, Ring, Nova, Domain | A ring on the ground around you, as wide as the spell |
| Bolt, Arc, Crescent, Orb, Wave, Wisp, Ricochet, Comet, Cluster, Beam, Spark, Ray, Lance, Prism, Sweep, Stream | A faint dotted line as far as it can reach, and a mark where it meets a block |

## Cooldowns

After a spell goes off it has to recharge before you can cast **that spell** again. Each spell has its own
cooldown, so on a Cord with several spells you can cast one while another recharges.

- The cooldown comes from the spell's **cost**: about **a second for every 20 mana**, never less than half a
  second and never more than twenty.
- **Rapid** halves it and **Vow** makes it four times longer; **Celerity** and the 5th Heart Circle's
  **Flow** shorten it. It's never under a quarter of a second.
- [Secret spells]({{ '/spellcraft/secret-spells/' | relative_url }}) take half as long again.
- The readout, the spell panel and the spell wheel all show your real cooldown.
- Cooldowns are **kept** when you log out or die, so leaving the game doesn't reset a long one.

The exact rules are on [How a Spell Is Read]({{ '/spellcraft/reading-spells/' | relative_url }}#cooldown).

## Mana

Every Cord gives you a pool of mana and refills it steadily:

| Cord | Max mana | Mana a second |
|---|---|---|
| Twine | 100 | 5 |
| Copper | 150 | 6 |
| Amethyst | 225 | 7 |
| Echo | 300 | 8 |

- Mana refills all the time, a little every quarter of a second, whatever you're doing.
- **Meditate** to refill it twice as fast: sneak and stand still on the ground for about a second while
  wearing your Cord (not while using an item). A ring of soft lights turns round your feet. Move, stand up or
  start using something and it stops.
- **Every source stacks**: Mana Crystals, Heart Circles, Reservoir and Wellspring on your Cord, Clarity and
  Mana potions, ley lines, a Wellstone, a mana storm, a familiar, a Focus of the Deep Well. Hover the mana
  badge in the Cord screen to see exactly where yours comes from. All of it is on
  [Mana]({{ '/progression/mana/' | relative_url }}).
- A spell that costs more than your whole pool can't be cast normally (the readout turns red); it can only
  be overcast.
- After you die you come back with an **empty** pool, which refills as usual.
- Every point of mana you spend on spells **condenses** toward your next
  [Heart Circle]({{ '/progression/heart-circles/' | relative_url }}). Passive upkeep doesn't.

## Rhythm

Cast again **right as your last spell comes off cooldown** and you're casting **on the beat**. Each cast on
the beat adds a step, up to three, and each step makes that cast stronger:

| Step | Extra power |
|---|---|
| 1 | +8% |
| 2 | +16% |
| 3 | +24% |

- The beat opens the moment the spell you just cast is ready again, and lasts a quarter of its cooldown,
  but never less than a quarter of a second or more than half a second.
- The next cast can be **any** spell, as long as it lands inside the beat.
- **Early or late starts over.** Pressing a spell that's still recharging before the beat, casting something
  else too soon, or letting the beat pass all drop you back to nothing. Nothing else is lost.
- On your spell panel, a **gold ring closes in** on the spell badge as the beat comes, and glows while it's
  open. Each step shows a note (♪) above the panel, and you hear a chime that rises with each step, with
  "♪ Rhythm x2 (+16% power)" above your hotbar.
- Three on the beat is the feat **In Rhythm**, which the 7th Heart Circle asks for.

Short spells are easiest to keep in rhythm, because their beat comes round quickly.

## Leaning

Every cast counts toward the elements of the effects in it (each element once per cast, and the runes inside
a Knot count too). Once one element has **40 or more casts** and **a quarter more than any other**, your magic
**leans** toward it:

- effects of that element hit **10% harder**,
- your Heart Circles turn toward its colour, and a charging circle for a spell with no effect of its own takes
  its colour,
- you're told "Your magic leans toward Fire..." and earn the feat **Leaning**.

Your leaning can change: if another element pulls far enough ahead, your magic leans toward that one
instead. The Grimoire page and the heart badge show your leaning, or how close you are.

## Switching spells

A Copper Cord holds 2 spells, an Amethyst Cord 3 and an Echo Cord 4, and the
[Tome of the Fifth Page]({{ '/gear/' | relative_url }}) adds a fifth while it's in your offhand. There are four
ways to pick one:

- **Tap `V`** to move to the next spell. It steps through every spell you can use, the tome's included, and
  back round to the first. The line above your hotbar shows the spell's number and its runes.
- **Hold `V`** for the **spell wheel** (below).
- **Click a spell's row** in the Cord screen: the spell you're editing is also the one you'll cast.
- **Bind the "Cast spell 1" to "Cast spell 5" keys** to cast a spell directly, without selecting it. They're
  unbound until you set them, and they only tap (no charge).

The spell you have selected is the one your wrist beads show.

### The spell wheel

<img src="{{ '/assets/images/spell-wheel.png' | relative_url }}" alt="The spell wheel: four spells in a ring around a centre showing the pointed spell's mana and cooldown, each with its name and rune icons outside the ring" class="shot">

Hold `V` for a quarter of a second and your spells fan out in a ring (you need a Cord with at least two
spells). Each shows its number, its name and runes, a coloured bar for its first element, and a dark shade
while it's still cooling down. The centre shows the pointed spell's mana and cooldown.

- **Point and let go:** move the mouse toward a spell and let go of `V` to select it.
- **Let go without pointing** and the wheel stays open. Then click a spell, press its **number**, or point
  at one and press `V` or `Enter` again.
- **Right-click** or press **`Esc`** to close it without choosing.

The wheel shows the Cord's own spells; the tome's fifth spell is reached with `V` taps or its own key.

## The HUD

<img src="{{ '/assets/images/hud.png' | relative_url }}" alt="The spell panel to the right of the hotbar: a badge with the spell's number, twelve rune icons and a cost of 168, a mana bar with a gold mark, and 218/380 mana with an upward chevron" class="shot">

The **spell panel** sits to the right of your hotbar (it moves aside for an offhand item, and tucks into the
corner on a narrow window). It's hidden while you aren't wearing a Cord.

| Part | Shows |
|---|---|
| **The badge** | The selected spell's number: gold when you can cast it, red when you can't afford it, dim when the spell is empty. A dark shade drains out of it as the spell recharges. |
| **Dots under the badge** | One for each spell you can use, the selected one in gold (the tome's in violet). |
| **Top row** | The spell's rune icons (with "+2" and so on when there are too many to fit) and, on the right, its cost in mana, or in health with a heart for Blood Price. The cost turns red when you can't afford it. |
| **The mana bar** | Your mana. A **gold mark** shows how much this spell will take; it turns red when you haven't enough. A soft shine runs along the bar while you're meditating or under Clarity. |
| **Bottom row** | Your mana ("218/380"). It turns bright, with a small up-arrow, while your regeneration is boosted (violet on a ley line or near a Wellstone). On the right: your **charge** ("60%", "FULL"), or the **cooldown** left ("1.2s"), or, when neither, what your **passives drain** ("-3.4/s", red if you can't keep them up). |
| **Above the panel** | The spell's **name** in its colour, **♪** notes for your rhythm steps, **✦** and a number in red for Heart Circles cracked by overcasting, and, when you're wearing a Shield, its strength and time left ("Shield 12 · 28s"). |

If the selected spell is empty, the panel just shows a **K**, a reminder that the Cord screen is where you
thread it.

## What other players see

Casting is meant to be read by everyone around you:

- the Cord on your wrist, with a bead for each rune of your ready spell,
- your hands raised and your spell's circle opening in front of them while you charge,
- the circle under your feet and the shape's pose when a spell goes off,
- your Heart Circles' rings turning round you as you cast and meditate.

Anyone who knows the runes can read what you're about to cast from your circle. See
[Magic Circles]({{ '/spellcraft/magic-circles/' | relative_url }}).

Big impacts (explosions, bursts, pillars, Domains, meteors) shake the camera of everyone nearby, a charged
release kicks your own view, and one of your spells landing a heavy hit gives a small punch. Inside a Domain
the edges of your screen are tinted. All of these follow the game's own Screen Effect Scale setting.
