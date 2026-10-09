---
title: Casting
parent: Spellcraft
nav_order: 4
description: "Tapping and charging, overchannelling, sigil tracing, incantations, cooldowns, mana, rhythm, leaning, switching spells, the spell wheel and the HUD."
---

# Casting

## What it is

You've threaded a spell. This page is about firing it: tapping or charging, pushing a charge past full,
keeping rhythm, switching spells and reading the spell panel beside your hotbar.

## How to use it

### Pressing the cast key

**`R`** casts your **selected** spell. Change keys in Controls, under **Wildercord** (see
[Controls]({{ '/controls/' | relative_url }})).

A cast fails, and nothing is spent, if you have no Cord, the spell's row is locked, it's still recharging, it's
empty, or you can't pay. If you're short of mana but have a Heart Circle, press again within two seconds to
[overcast]({{ '/spellcraft/overcasting/' | relative_url }}).

In **creative**, spells cost no mana, but cooldowns still apply.

### Tap or charge

<img src="{{ '/assets/images/circle-bloom.jpg' | relative_url }}" alt="A player with raised hands charging a spell, its magic circle behind the shoulders" class="shot">

**Tap `R`** and the spell goes off at once.

**Hold `R`** to **charge**. Your hands rise and the spell's magic circle draws itself behind your shoulders.
A **full charge** takes a second and a half and adds **+40% power** (a half charge adds +20%). You walk 40%
slower while charging, and mana is only spent when you let go.

- You can't start a charge while the spell is recharging.
- Held too long without overchannelling (12 seconds), the charge **fizzles**.
- A **Focus of Haste** fills a charge faster. See [Casting Gear]({{ '/gear/' | relative_url }}).
- The direct "Cast spell" keys only tap.
- Your first full-charge release earns the feat **Full Charge**.

While you charge, only you see a **reticle** for the first shape: a ring on the ground for areas, or a dotted
line for things that fly.

### Overchannel: holding past full

<img src="{{ '/assets/images/overchannel.png' | relative_url }}" alt="A caster at the third overchannel stage: the circle cracked and shedding sparks, the screen's edges closing in" class="shot">

Keep holding past full and the charge **overchannels**, climbing a **stage** every 1.2 seconds.

| Stage | Extra power (on top of +40%) | Surge chance |
|---|---|---|
| I | +20% | 7% |
| II | +40% | 14% |
| III | +60% | 21% |

- **Your heart sets the limit.** Anyone can reach stage I, 2 active Heart Circles reach stage II, and 4 reach
  stage III. Cracked circles don't count.
- **It drains spare mana**, about 15% of the spell's price each second, but never the mana the spell needs.
  Short of spare mana, it just waits.
- **A surge** can twist the spell into wild magic. See [Overcasting]({{ '/spellcraft/overcasting/' | relative_url }}).
- **Let go on the beat.** The moment the charge fills, and each time a stage lands, is a beat. Let go within
  about a third of a second for **+10% power**.
- **Don't hold too long.** At your last stage the circle reddens and shakes. Hold 3 more seconds and it
  **tears loose**: the spell fizzles, you're dazed for 1.5 seconds and lose 30% of your mana. It never costs
  health.

### Sigil tracing

<img src="{{ '/assets/images/sigil-trace.png' | relative_url }}" alt="A glyph of violet strokes round the crosshair while a spell charges, with a gold path traced along it" class="shot">

While charging, a faint glyph made from the spell's runes appears round your crosshair. **Hold sneak** to steady
the camera, then trace the glyph with the mouse in one line from the bright dot.

- A good trace adds up to **+8% power**, and a perfect one removes 60% of an overchannel's surge chance.
- Not tracing loses nothing.
- Turn it off or change **Trace assist** in the **Magic visual settings** screen (its key is unbound by default).

### Incantations

<img src="{{ '/assets/images/incantation.png' | relative_url }}" alt="A charging caster with three glowing syllables in a line of script over their shoulders" class="shot">

Every rune has a spoken syllable. While you charge, your spell's syllables rise over your shoulders as its
**incantation**. Anyone near can read it. **A tap is silent**, so only charged spells give away what's coming.
Choose whose incantations you see in the Magic visual settings.

### Cooldowns

Each spell has its own cooldown, so you can cast one while another recharges. It's about **a second per 20
mana**, from half a second to twenty, and **Rapid**, **Vow**, **Celerity** and **Flow** change it. Cooldowns are
kept when you log out or die. Full rules: [How a Spell Is Read]({{ '/spellcraft/reading-spells/' | relative_url }}#cooldown).

### Mana

Your Cord sets your starting pool and refill rate (see [Cords]({{ '/spellcraft/cords/' | relative_url }})).

- **Meditate** to refill twice as fast: sneak and stand still on the ground while wearing your Cord.
- Mana Crystals, Heart Circles, enchantments, potions, ley lines and more all stack. See
  [Mana]({{ '/progression/mana/' | relative_url }}).
- You come back from death with an **empty** pool.
- Mana you spend on spells **condenses** toward your next [Heart Circle]({{ '/progression/heart-circles/' | relative_url }}).

### Damage limit per cast

A player's spell deals at most **96 raw damage per hit**. Against each target, one cast can deal at most **12 +
twice its mana price**, up to **512**. Burns, echoes, pulses and linked hits all share that allowance. Each
target has its own, so area spells still work on crowds. A new paid cast starts fresh.

### Rhythm

Cast again **right as your last spell comes off cooldown** to cast **on the beat**. Each beat adds a step, up to
three, worth **+8% power** each (+24% at three).

- The beat lasts a quarter of the spell's cooldown, between a quarter and half a second.
- The next cast can be any spell.
- Too early, too late, or missing the beat drops you back to nothing.
- A gold ring closes in on your spell badge as the beat comes.
- Three on the beat earns the feat **In Rhythm**, which the 7th Heart Circle asks for.

### Leaning

Spells grow your [affinity]({{ '/progression/affinity/' | relative_url }}) with their elements. Once one element
reaches **level I** and has **a quarter more points than any other**, your magic **leans** toward it: your Heart
Circles take its colour and you earn the feat **Leaning**. Leaning is just a label; the power comes from each
affinity's level. It can change if another element pulls ahead.

### Switching spells

A Copper Cord holds 2 spells, Amethyst 3, Echo 4, and the [Tome of the Fifth Page]({{ '/gear/' | relative_url }})
adds one more.

- **Tap `V`** to move to the next spell.
- **Hold `V`** for the **spell wheel**.
- **Click a spell's row** in the Cord screen.
- **Bind "Cast spell 1" to "Cast spell 5"** to cast a spell directly. They're unbound by default.

<img src="{{ '/assets/images/spell-wheel.png' | relative_url }}" alt="The spell wheel: four spells in a ring around a centre showing the pointed spell's mana and cooldown" class="shot">

On the wheel, point at a spell and let go of `V` to pick it. Let go without pointing and the wheel stays open:
click a spell or press its number. Right-click or `Esc` closes it.

### The HUD

<img src="{{ '/assets/images/hud.png' | relative_url }}" alt="The spell panel to the right of the hotbar: a badge with the spell's number, rune icons and a cost, a mana bar with a gold mark, and the mana count" class="shot">

The **spell panel** sits beside your hotbar while you wear a Cord.

| Part | Shows |
|---|---|
| **Badge** | The spell's number: gold if you can cast it, red if you can't afford it. It shades while recharging, and a ring shows your rhythm or charge beat. |
| **Dots** | One per spell; the selected one is gold, the tome's violet. |
| **Top row** | The spell's runes and its cost (red if you can't afford it, a heart for Blood Price). |
| **Mana bar** | Your mana, with a **gold mark** for this spell's cost. |
| **Bottom row** | Your mana, then your charge ("FULL", "✦I" to "✦III"), the cooldown left, or your passives' drain. |
| **Above** | The spell's name, ♪ rhythm notes, cracked circles, your Shield, and the [climate]({{ '/spellcraft/affinities/' | relative_url }}) where you stand (▲ stronger, ▼ weaker). |

## Tips and counterplay

- **Read other casters.** A charging caster shows their circle and speaks their incantation. A cracking,
  trembling circle means they're overchannelling, and a Jolt stops a caster mid-charge.
- **Tap to hide your spell**, charge for power. You can't have both.
- **Spells show their element as they form.** Area spells prepare where they'll land, so watch the ground.
  See [Magic Circles]({{ '/spellcraft/magic-circles/' | relative_url }}).

<img src="{{ '/assets/group-formations/group_formation_zone.png' | relative_url }}" alt="A Zone preparing at the ground point the caster aims at" class="shot">

- **Short spells keep rhythm best**, because their beat comes round quickly.
- Big impacts shake the camera. That follows the game's **Screen Effect Scale** setting.
