---
title: Duels
parent: Friends and Rivals
nav_order: 3
---

# Duels

A **duel** is a fair fight between two players where **nobody dies** and **nobody loses anything**. When it's over,
however it ends, you're both put back as you were when it began. Duels work even on servers with PvP turned off.

## Challenging someone

Type **/duel** followed by the player's name, for example `/duel Alex`.

- **They must be close:** in the same world as you and within **40 blocks**.
- **They hear a bell** and see your challenge in chat with two buttons: **[Accept]** and **[Decline]**.
- **The challenge lasts 30 seconds.** After that it runs out.
- **You can challenge again after 10 seconds.** Challenging the same player again renews the challenge (without
  ringing the bell again).
- You can't challenge yourself, or anyone who's already in a duel.

To answer by typing instead of clicking: `/duel accept Alex` or `/duel decline Alex`.

### When a duel can't start

When the challenge is sent, and again when it's accepted, **both** players must be ready. A duel can't start while
either of you:

| Was... | In the last | The game says |
|---|---|---|
| hurt by anything | 10 seconds | *(Player) was hurt too recently to start a duel* |
| fighting another player (hitting them or being hit) | 30 seconds | *(Player) was fighting another player too recently to start a duel* |
| in a duel that ended | 30 seconds | *(Player) duelled too recently: wait a little before the next one* |

When accepting, you must also still be in the same world, within 40 blocks of each other, and both alive.

This stops anyone using a duel to escape a real fight, or to heal up.

## How a duel goes

1. **The countdown.** Both of you read *Duel with (player)! Nobody dies, and afterwards you're both as you were before
   it. Stay within 40 blocks.* A gold **3, 2, 1** counts down on screen, each second with a click and a circle of
   light around each of you. Nothing about you changes as it starts, and no blow between you counts yet.
2. **Fight!** A bell rings, *Fight!* fills the screen and a ring of light flashes at your feet. Now the two of you can
   hurt each other with anything (spells, weapons, arrows, pets) even if PvP or friendly fire is off.
3. **The end.** The winner sees **Victory!** and the loser **Defeated**, each with the other's name, and everyone
   within 64 blocks reads how it ended in chat.

### The arena

The arena is a circle reaching **40 blocks** out in every direction from the point halfway between you when the duel
was accepted (only the distance across the ground counts, not height). Walk out of it, or change worlds, and you
forfeit.

## The rules

- **Nobody dies.** A blow from your opponent that would kill you (or their harm in the last five seconds, like fire or
  a fall they caused) leaves you at **1 health** instead, and you lose.
- **Only each other.** While the duel lasts (countdown included), neither duellist can harm any other player, or any
  other player's pets.
- **No interference.** If anyone else strikes either duellist, the duel is **called off** and counts for nobody. That
  blow still lands. Everyone nearby reads *The duel was called off: someone else joined the fight*.
- **Five minutes.** A fight with no winner after five minutes is a **draw**.
- **No singing together.** Two duellists never [chorus]({{ '/social/chorus/' | relative_url }}) together.

### How a duel ends

| What happens | Result | Counts in your record? |
|---|---|---|
| Your opponent brings you down | You lose: *(winner) defeated (loser) in a duel* | Yes |
| You leave the arena, or go to another world | You forfeit: *(winner) won the duel: (loser) left the arena* | Yes |
| You log off | You forfeit: *(winner) won the duel: (loser) left the game* | Yes |
| Something else kills you (a monster, lava, the void) | You forfeit: *(winner) won the duel: (loser) fell to something else* | Yes |
| Five minutes pass | Draw: *The duel ended in a draw* | No |
| Someone else joins the fight | Called off | No |

## What's restored

When a duel ends, **however it ends**, both duellists are put back as they were when it began. A duel is never a free
heal, and never costs you anything:

- **Health:** back to what you had when the duel began, or what you have now if that's more (never above your maximum).
- **Mana:** the same: what you had, or what you have now if that's more.
- **Fire:** if you weren't burning when the duel began, the flames go out.
- **Harmful effects** the duel left on you (poison, slowness and so on that you didn't have before) are removed.
- **Effects you had** when it began come back, less the time the duel took. A potion with 3 minutes left when a
  one-minute duel began has 2 minutes left afterwards.

Anything the duel didn't cause is kept: helpful effects you gained during it stay, and nothing is taken from your
inventory. Nothing is dropped, because nobody dies to the other.

A player who dies to something else or logs off during a duel isn't put back.

## Your record

Every duel with a winner adds a win or a loss to both players' records.

- Your **Grimoire** has a **Duels** line: *(wins) won, (losses) lost*, and says how to challenge someone. See
  [the Grimoire]({{ '/progression/grimoire/' | relative_url }}).
- **/duel stats** shows your own record in chat; **/duel stats (player)** shows someone else's.

Your record is kept for good, through death.

## Turning duels off

Server operators can turn duels off for a world with the game rule `wildercord:allow_duels`. With it off, challenges
and answers are refused with *Duels are turned off here*. See [Controls]({{ '/controls/' | relative_url }}) for the
few commands a server operator uses.
