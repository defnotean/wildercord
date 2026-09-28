# The Runesmith, duels and chorus casting

## The Runesmith

A new villager trade. Any villager without a job takes it from a **Scribing Desk**
(crafted like a lectern: three wooden slabs over a Blank Rune, a bookshelf and a Blank Rune,
over one more slab). Runesmiths wear violet robes with a gold band and a hood.

What a Runesmith sells, by level (each level stocks two of its trades, and every rune on the
shelf is picked at random, so no two Runesmiths sell quite the same runes):

| Level | Trades |
|---|---|
| Novice | 4 Blank Runes for an emerald; buys lapis and paper; a Tier I rune |
| Apprentice | Tier I and Tier II runes; buys amethyst shards; paper for Spell Scrolls |
| Journeyman | a Tier II rune; a Torn Page (emeralds and a book); ink sacs |
| Expert | a Mana Crystal; a Tier I-II rune; a Torn Page |
| Master | a **Tier III rune** (emeralds and a diamond); a Mana Crystal |

Wandering traders now and then have a rune for sale too.

### Duplicates always have value

When you trade with a Runesmith it also looks at the runes you're carrying that you already know:

- **Buyback:** it buys any of them for emeralds (1, 2, 5 or 10 by tier).
- **Reroll:** two known runes of the same tier (the same rune twice, or two different ones) swap
  for one rune of that tier you haven't learned yet. The trade shows which rune you'll get.

These offers are made for you alone and disappear when you close the trade.

### Daily contracts

Right-click a Scribing Desk to read your contract board: three contracts, new every dawn, such as

- Defeat 4 Runebound with Frost spells
- Set off 3 Conduct reactions
- Cast 20 spells on a ley line
- Imbue a weapon and use all its charges
- Slay 15 monsters with spells
- Cast 25 Fire spells

Progress shows above your hotbar as you go, and you're told when one is done. Right-click any
Scribing Desk to hand finished contracts in for their reward: emeralds, Blank Runes, a Mana
Crystal or a random Tier II rune.

## Duels

`/duel <player>` challenges someone; they click **[Accept]** or **[Decline]** in chat (a challenge
lasts 30 seconds, and you must be within 40 blocks of each other).

- Both duellists are healed to full health and mana, and a 3-second countdown runs in a circle
  of light.
- Then only the two of you can hurt each other, even with PvP or friendly fire off, and nobody
  else's blows or spells land on either of you.
- **Nobody dies.** Brought down by your opponent, you're knocked out at 1 health: the duel ends and
  both of you are restored.
- Walking more than 40 blocks from where the duel began, logging off or dying to something else
  forfeits. A duel with no winner after 5 minutes is a draw.
- Everyone nearby hears the result. `/duel stats [player]` shows wins and losses.

Server operators can turn duels off with `/gamerule wildercord:allow_duels false`.

## Chorus casting

When two or three casters cast spells of the **same shape** within a second of each other,
standing within 12 blocks and aimed at the same foe or the same spot (within 5 blocks), their
spells merge into one **chorus spell**:

- +50% power for each extra voice (a chorus of three is twice as strong, and that's the most),
  and shapes with a size (Burst, Zone, Rain, Domain...) grow by a Widen for each extra voice.
- Everyone's colours braid together over the target and everyone sees *Chorus!*
- Every caster still pays for their own spell. What's left of the earlier spells in the air folds
  into the chorus.
- Singing one earns the new **Chorus** feat in the Grimoire.
