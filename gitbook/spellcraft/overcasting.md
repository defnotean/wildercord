# Overcasting and Wild Magic

## What it is

Out of mana with a monster in your face? You can still cast. **Overcasting** cracks one of your
[Heart Circles](../progression/heart-circles.md) to pay for the spell. You lose that circle's
gifts for 3 minutes, and the spell may twist into **wild magic**.

## How to get it

Every player with at least one working Heart Circle can overcast.

1. Cast a spell you can't afford. Nothing is spent. Above your hotbar you see *"Not enough mana (38/46). Cast
   again to overcast: your 3rd Circle cracks for 3 minutes"*.
2. Cast **the same spell again within 2 seconds**. Wait longer or cast another spell and it only asks again.

It always takes that second press, so it never happens by accident.

You can't overcast when:

- every circle is already cracked, or you have none;
- the spell costs more than **twice your full mana** (*"Too costly to overcast"*);
- the spell is paid in health ([Blood Price](../runes/modifiers.md)).

## How to use it

### What it costs

- Your mana drops to **0**, and your **outermost working circle cracks**.
- **A cracked circle gives nothing** until it mends: no extra mana, regeneration or power, no perk (Mana Skin,
  Flow, Overflow, Archmage) and no passive slot. Crack your 5th Circle and you lose Flow and your second
  [passive](passives.md).
- It mends after **3 minutes**. Overcast again before then and the next circle in cracks, and the 3 minutes
  start over for all of them.
- You never lose the circle itself, or mana you've saved toward the next one.

The spell panel shows **✦** and the number of cracked circles. Hover the heart badge in the
[Cord screen](cord-screen.md) to see how long until they mend.

### Wild magic

![Butterflies of light flutter up and away over a group of husks](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/b-wild-butterflies.jpg)

An overcast spell may **surge**. The chance is **20%**, plus 15% for every full mana bar the spell cost beyond
what you had, up to **50%**.

Spells held past full ([overchannelled](casting.md#overchannel-holding-past-full))
can surge too: 7% per stage, less if you traced the glyph well. Hold one too long and it tears loose into a
harmless surge (Butterflies, Heal All, Blink, Levitate or Slow Time).

| Surge | Odds | What happens |
|---|---|---|
| **Twice** | 10.6% | The spell goes off, then again 0.4 s later. |
| **Element** | 9.6% | Harmful effects change to another element. |
| **Grand** | 8.5% | Everything with a size is widened twice over. |
| **Butterflies** | 8.5% | **Nothing is cast.** Harmless butterflies of light and little fireworks. |
| **Free Recast** | 7.4% | The spell goes off, and your next cast within 3 s is free and ignores its cooldown. |
| **Heal All** | 7.4% | **Nothing is cast.** Everyone within 8 blocks, friend and foe, heals 6 health. |
| **Blink** | 7.4% | You jump 3 to 6 blocks aside onto safe ground, then the spell goes off. |
| **Wisps** | 7.4% | The spell goes off, with wisps that chase whatever is near. |
| **Levitate** | 7.4% | The spell goes off, and everything within 6 blocks floats up for 1.2 s. You get Slow Falling for 6 s. |
| **Stray** | 6.4% | The spell flies at a random creature you can see within 16 blocks, whoever it is. |
| **Slow Time** | 6.4% | The spell goes off, and everything within 8 blocks, you too, gets Slowness III for 2 s. |
| **Backfire** | 6.4% | **Nothing is cast**, and you take up to 4 damage. |
| **Ward** | 6.4% | The spell goes off, and a Shield of strength 16 covers you for 10 s. |

- **Wild magic can't kill you.** Backfire never takes you below half a heart.
- **Wild magic never breaks or places blocks** by itself.
- **Secret spells** never get Element or Grand; those become Twice. So does Stray with nobody in sight.
- Levitate never lifts a boss.

## Tips and counterplay

- **Overcast to finish a fight.** A cracked circle for 3 minutes beats dying.
- **Not before a boss.** Losing Overflow or Archmage mid-fight hurts more than one missed spell.
- **Big overcasts gamble more.** The further past your mana, the likelier a surge.
- **Keep a strong spell ready** in another slot in case you roll a Free Recast.
- Your first overcast earns the **Overcast** feat; your first surge earns **Wild Magic**.


A **mana storm** can make spells surge too, without overcasting. See
[World Events](../world/world-events.md).
