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

These offers are made for you alone and disappear when you close the trade. So they can't be
farmed:

- Only runes a Runesmith sells itself are taken, never Lightning (a lightning rod turns a stack of
  Blank Runes into Lightning runes) and never an innate, fused or found-only rune.
- Only plain rank I runes pay; a ranked rune from the Fusion Altar is refused.
- You can sell back 8 runes a day (across every Runesmith); after that the buybacks close until
  tomorrow. Rerolls aren't limited.
- Neither trade gives you or the Runesmith any experience.

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

A cast only counts (for a ley line or an element contract) once the spell lands on a real creature,
and a reaction only when it's set off on one: casting at nothing or at a Training Dummy earns
nothing. Turning time back doesn't bring a new board (and can't make contracts pay twice); the
board changes only when a later day comes. With the daylight cycle off the board just says new
contracts come at the next dawn.

## Duels

`/duel <player>` challenges someone; they click **[Accept]** or **[Decline]** in chat (a challenge
lasts 30 seconds, and you must be within 40 blocks of each other, in the same world, both when you
challenge and when they accept). You can challenge again after 10 seconds.

- A duel can't start while either of you was hurt in the last 10 seconds, fought another player in
  the last 30, or finished a duel in the last 30.
- A 3-second countdown runs in a circle of light. Nothing about you changes as it starts.
- Then the two of you can hurt each other, even with PvP or friendly fire off. While it lasts
  neither of you can harm any other player (or their pets).
- If anyone else strikes either duellist, the duel is called off (it counts for nobody) and the
  blow lands as usual.
- **Nobody dies.** Brought down by your opponent, you're knocked out at 1 health.
- When the duel ends, however it ends (logging off included), both of you are put back as you were
  when it began: the health your opponent took from you is given back (never more than you had;
  measured from your health as each blow lands, since the damage reported after it is before armour),
  no harmful effects or fire the duel left on you, and the helpful effects you had (not Absorption),
  less the time the duel took. Harm from anything else (a fall, a monster) stays, and mana you spent isn't given back, so
  a duel is never a free heal. A duel still under way when the server stops ends for nobody.
- Walking more than 40 blocks from where the duel began, logging off or dying to something else
  forfeits. A duel with no winner after 5 minutes is a draw.
- Everyone nearby hears the result. `/duel stats [player]` shows wins and losses.

Server operators can turn duels off with `/gamerule wildercord:allow_duels false`.

## Chorus casting

When two or three allied casters cast spells of the **same shape** and the same kind (both
harmful, both helpful, or both neither, like two Bolts of Break), they sing a **chorus**. Each
voice is measured against the voice before it: it must come within a second of it, from a caster
standing within 12 blocks of that caster, aimed at the same foe or at a spot within 5 blocks of
where that voice aimed.

- The last voice's spell gains +50% power for each extra voice (a chorus of three is twice as
  strong, and that's the most), and shapes with a size (Burst, Zone, Rain, Domain...) grow by a
  Widen for each extra voice. It keeps its caster's casting gear.
- Everyone's colours braid together over the target and everyone sees *Chorus!*
- Every caster still pays for their own spell, and the earlier spells go on untouched: nobody can
  cut your spell short or steal its kill by casting after you.
- Allies are casters on the same team, or two who couldn't harm each other anyway (with PvP off,
  for instance). Two players who could fight each other, or two duelling, never sing together, and
  a spell that both harms and heals always sings alone.
- Singing one earns the new **Chorus** feat in the Grimoire.
