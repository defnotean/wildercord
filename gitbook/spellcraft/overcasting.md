# Overcasting and wild magic


Out of mana with a monster in your face? You can still cast. **Overcasting** cracks one of your
[Heart Circles](../progression/heart-circles.md) to pay for the spell. It costs you
that circle's gifts for three minutes, and the spell may twist into **wild magic** on the way out.


## How to overcast

1. Cast a spell you can't afford. Nothing is spent. Instead you're told, above the hotbar: *"Not enough
   mana (38/46). Cast again to overcast: your 3rd Circle cracks for 3 minutes"*, and an amethyst note
   hums.
2. Cast **the same spell again within 2 seconds** to overcast. Waiting longer, or casting a different
   spell, starts over: the next press only asks again.

It always takes that second press, so it never happens by accident.

### When you can't

| Situation | What happens |
|---|---|
| You have no Heart Circles, or every one is already cracked | Nothing to crack: *"Not enough mana"*, and the spell fails. |
| The spell costs more than **twice your full mana** | *"Too costly to overcast"*: a circle pays for a spell, not for anything. |
| The spell is paid in health ([Blood Price](../runes/modifiers.md)) | Overcasting never comes into it: it costs health, not mana. |

## What it costs you

- Your mana goes to **0**. The mana you did have still counts toward your next circle; the rest is
  paid by the crack.
- Your **outermost working circle cracks**. The ring snaps outward in red light and breaks into shards,
  a cracked circle shows on the ground at your feet, and you see *"Overcast! Your 3rd Circle cracks. It
  mends in 3 minutes"*. Your first overcast earns the **Overcast** feat.
- **A cracked circle gives nothing** until it mends: not its +15 max mana, +0.5 mana a second or +3%
  spell power, not its +6% on your innate rune, not its perk (Mana Skin, Flow, Overflow, Archmage), not
  its passive slot. Crack your 5th Circle and you lose Flow and your second passive until it mends.
- It mends **3 minutes** later. Overcasting again before then cracks the **next circle in**, and starts
  the 3 minutes over for all of them. With every circle cracked, you can't overcast any more.
- When they mend you're told *"Your cracked circles have mended."*, with a chime.

What a crack never touches: the circles themselves (you don't lose them) and mana you've condensed toward
the next circle. Everything a cracked circle gave comes back when it mends.

The HUD shows **✦** and the number of cracked circles above your spell panel, and hovering the heart
badge in the Cord screen shows how long until they mend.

## Wild magic

![Butterflies of light flutter up and away over a group of husks](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/b-wild-butterflies.jpg)
<span>A surge of butterflies: nothing cast, but very pretty.</span>

An overcast spell may **surge**, twisting into something unexpected.

### The chance

**20%** at least, plus **15 points for every full mana bar** the spell went past the mana you had, up
to **50%**.

| The spell cost more than you had by... | Chance of a surge |
|---|---|
| a little | 20% |
| a third of your full mana | 25% |
| two thirds of your full mana | 30% |
| your full mana | 35% |
| one and a half times your full mana | 42.5% |
| twice your full mana (the most you can overcast) | 50% |

A surge announces itself with a swirl of its colour winding up round you, a flash over your head, a
star and a ring on the ground, and a coloured line above your hotbar: *"Wild magic! ..."*. Your first
surge earns the **Wild Magic** feat.

### Every surge

When a spell surges, one outcome is picked by weight. Out of every 94 surges, on average:

| Surge | Weight | Odds | What happens |
|---|---|---|---|
| **Twice** | 10 | 10.6% | The spell goes off, and again 0.4 seconds later. |
| **Element** | 9 | 9.6% | Every harmful effect turns to one of a random other element, the one closest in tier (*"The spell turns to Frost"*). |
| **Grand** | 8 | 8.5% | Every part of the spell with a size is widened twice over (+125%). A spell with nothing to widen comes out 50% stronger instead. |
| **Butterflies** | 8 | 8.5% | **Nothing is cast.** A harmless shower of butterflies of light in every colour, with little fireworks overhead. |
| **Free Recast** | 7 | 7.4% | The spell goes off, and your **next cast within 3 seconds** costs no mana and ignores its cooldown (*"Free recast!"* when you use it). |
| **Heal All** | 7 | 7.4% | **Nothing is cast.** Everything within 8 blocks, you, friend and foe, heals 6 health. |
| **Blink** | 7 | 7.4% | You're flung 3 to 6 blocks aside onto safe, solid ground, then the spell goes off from there. |
| **Wisps** | 7 | 7.4% | The spell goes off, and four violet wisps pour out with it, chasing whatever's near (Harm at half power). |
| **Levitate** | 7 | 7.4% | The spell goes off, and gravity flips: everything within 6 blocks, you included, floats up for 1.2 seconds. Players get Slow Falling for 6 seconds. |
| **Stray** | 6 | 6.4% | Your arm is tugged round to a random creature within 16 blocks that you can see, **whoever it is**, and the spell goes off at it. |
| **Slow Time** | 6 | 6.4% | The spell goes off, and everything within 8 blocks, you included, is slowed (Slowness III) for 2 seconds. |
| **Backfire** | 6 | 6.4% | **Nothing is cast**, and you take up to 4 damage in a puff of smoke. |
| **Ward** | 6 | 6.4% | The spell goes off, and a Shield of light (strength 16) settles on you for 10 seconds, unless a stronger [Shield](shields.md) is already up. |

So nearly four surges in five still cast your spell (some of them twice, bigger or with a bonus). The
other three (Butterflies, Heal All and Backfire), a little over one surge in five, cast nothing at
all.

### The fine print

- **Element** only swaps harmful effects that you could craft or find at random; fused runes, runes of
  the world and innate runes stay as they are. If nothing in the spell can be swapped, it goes off
  **Twice** instead.
- **Grand** adds two Widens after everything in the spell that has a size (a shape's radius, a blast,
  a zone), up to the usual limit of what Widen can do.
- **Stray**: with nobody in sight within 16 blocks, it goes off **Twice** instead.
- **Blink** only lands you on solid ground with room to stand, never on magma, in water or lava, or
  more than 3 blocks down. If there's nowhere safe, you stay put, and the spell still goes off.
- **Levitate** never lifts a boss.
- **Twice** and a surge's second go share the first go's payment: they can't Imbue a second time or
  Siphon past the first go's limit.
- **Secret spells** never get Element or Grand (they're their exact runes): those become **Twice**.
  So for a secret spell the odds are out of 77, not 94.
- **Wild magic can't kill you.** Backfire never takes you below half a heart, and it doesn't hurt you
  at all in Creative. Levitate gives players Slow Falling.
- **Wild magic never changes a block by itself.** Your spell's own effects still need permission to
  build, as always.


A **mana storm** can make spells surge too, with no overcasting involved: they swell, echo or kick back.
That's a different thing: see [World Events](../world/world-events.md).

## When to overcast

- **To finish a fight.** A cracked circle for three minutes is cheap next to dying.
- **Not before a boss.** Losing your 7th Circle's Overflow or your 8th's Archmage discount mid-fight
  hurts more than a missed spell.
- **Mind your perks.** Your outermost circle goes first. At the 3rd Circle that's Mana Skin; at the
  5th, Flow and your second passive slot.
- **Big overcasts gamble more.** The further past your mana, the likelier a surge, good or bad.
- **A Free Recast is worth planning for:** keep a strong spell ready in another slot.

## Feats and advancements

| Feat / advancement | How | Reward |
|---|---|---|
| **Overcast** | Crack a Heart Circle to cast beyond your mana | 250 mana toward your next circle; 25 experience |
| **Wild Magic** | Overcast a spell and watch it surge | 250 mana toward your next circle; 25 experience |
