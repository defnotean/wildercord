---
title: Duels
parent: Friends and Rivals
nav_order: 3
---

# Duels

## What it is

A **duel** is a fair fight between two players where **nobody dies**. When it ends, the harm you did each other is
undone. Duels work even with PvP off, and between members of the same
[party]({{ '/social/playing-together/' | relative_url }}#parties).

## How to get it

Type `/duel <player>`, for example `/duel Alex`.

- They must be in the same world and within **40 blocks**.
- They hear a bell and see **[Accept]** and **[Decline]** buttons in chat. You can also type `/duel accept <player>`
  or `/duel decline <player>`.
- The challenge lasts **30 seconds**. You can challenge again after **10 seconds**.

### When a duel can't start

Neither of you may have been:

| Doing this | In the last |
|---|---|
| Hurt by anything | 10 seconds |
| Fighting another player | 30 seconds |
| In a duel | 30 seconds |

You're told why, and the challenge still stands until its 30 seconds run out. This stops anyone using a duel to
escape a real fight or to heal.

## How to use it

1. **Countdown.** A gold **3, 2, 1** counts down. No blow counts yet.
2. **Fight!** Now the two of you can hurt each other with anything: spells, weapons, arrows, pets.
3. **The end.** The winner sees **Victory!**, the loser **Defeated**, and everyone within 64 blocks reads the result.

### The rules

- **The arena** reaches 40 blocks out from the point halfway between you. Leave it, or change worlds, and you
  forfeit.
- **Nobody dies.** A blow from your opponent that would kill you leaves you at **1 health**, and you lose.
- **Only each other.** Neither of you can harm any other player or their pets.
- **No interference.** If anyone else hits either of you, the duel is called off and counts for nobody.
- **Five minutes.** No winner after 5 minutes is a draw.

| What happens | Result | Counts in your record? |
|---|---|---|
| Your opponent brings you down | You lose | Yes |
| You leave the arena or the world | You forfeit | Yes |
| You log off | You forfeit | Yes |
| Something else kills you | You forfeit | Yes |
| 5 minutes pass | Draw | No |
| Someone else joins in | Called off | No |
| The server shuts down | Ends for nobody | No |

### What's undone

When a duel ends, however it ends:

- **Health** your opponent took from you comes back, but never more than you had at the start.
- **Fire** and **harmful effects** your opponent put on you are removed.
- **Helpful effects** you had at the start come back, minus the time the duel took. Absorption doesn't come back.
- **Mana** you spent stays spent.

Anything your opponent didn't cause stays: a monster's poison, a fall, lava. Nothing is dropped.

## Tips and counterplay

- `/duel stats` shows your record; `/duel stats <player>` shows someone else's. Your Grimoire has a **Duels** line too.
- Your record is kept for good.
- Duellists never [chorus]({{ '/social/chorus/' | relative_url }}) together.
- Server operators can turn duels off with the game rule `wildercord:allow_duels`.
